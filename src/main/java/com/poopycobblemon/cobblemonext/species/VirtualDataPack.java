package com.poopycobblemon.cobblemonext.species;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内存数据包：把 {@link SpeciesDefinition} 集合暴露成标准 PackResources，
 * 通过 {@code AddPackFindersEvent} 注入后，Cobblemon 的种族加载器
 * （{@code PokemonSpecies}，{@code PackType.SERVER_DATA}）会像读取普通数据包
 * 一样拾取其中的 {@code data/<ns>/species/*.json}，解析、校验与客户端同步
 * 全部走原生链路。
 */
final class VirtualDataPack implements PackResources {

    private static final Logger LOGGER = LogUtils.getLogger();

    static final String PACK_ID = "cobblemon_ext/generated_species";

    private final PackLocationInfo location;
    private final Map<String, byte[]> files;

    VirtualDataPack(PackLocationInfo location, Map<String, byte[]> files) {
        this.location = location;
        this.files = files;
    }

    /**
     * 组装可提交给 {@code AddPackFindersEvent#addRepositorySource} 的内置数据包。
     * 空定义列表返回 null（调用方应跳过注入，避免凭空多出一个空包）。
     */
    static Pack buildPack(List<SpeciesDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return null;
        }
        int packFormat = SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA);
        Map<String, byte[]> files = SpeciesPackFiles.create(definitions, packFormat);
        PackLocationInfo location = new PackLocationInfo(PACK_ID,
                Component.literal("cobblemon-ext species"), PackSource.BUILT_IN, Optional.empty());
        Pack pack = Pack.readMetaAndCreate(location, new ResourcesSupplier(files),
                PackType.SERVER_DATA, new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
        LOGGER.info("[cobblemon-ext] 已生成种族数据包：{} 个定义（{}）",
                definitions.size(), files.keySet().stream().filter(k -> k.endsWith(".json"))
                        .map(k -> k.substring("data/".length())).collect(Collectors.joining(", ")));
        return pack;
    }

    private static String keyFor(PackType type, ResourceLocation id) {
        return type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath();
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        return resource(String.join("/", segments));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        return resource(keyFor(type, id));
    }

    private IoSupplier<InputStream> resource(String key) {
        byte[] bytes = files.get(key);
        if (bytes == null) {
            return null;
        }
        return () -> new ByteArrayInputStream(bytes);
    }

    /**
     * NeoForge 1.21.1 契约：第二参为命名空间，prefix 为该命名空间下的路径前缀；
     * 枚举 {@code <typeDir>/<namespace>/<prefix…>} 下的文件，输出 id 为
     * {@code <namespace>:<相对路径>}（路径含 species/ 目录，与原版一致）。
     */
    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        String root = type.getDirectory() + "/" + namespace + "/";
        String[] prefixParts = prefix.isEmpty() ? new String[0] : prefix.split("/");
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(root)) {
                continue;
            }
            String path = key.substring(root.length());
            if (!pathStartsWith(path, prefixParts)) {
                continue;
            }
            byte[] bytes = entry.getValue();
            output.accept(ResourceLocation.fromNamespaceAndPath(namespace, path),
                    () -> new ByteArrayInputStream(bytes));
        }
    }

    private static boolean pathStartsWith(String path, String[] prefixParts) {
        if (prefixParts.length == 0) {
            return true;
        }
        String[] segments = path.split("/");
        if (segments.length < prefixParts.length) {
            return false;
        }
        for (int i = 0; i < prefixParts.length; i++) {
            if (!segments[i].equals(prefixParts[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        String root = type.getDirectory() + "/";
        return files.keySet().stream()
                .filter(key -> key.startsWith(root))
                .map(key -> key.substring(root.length()))
                .filter(rest -> rest.contains("/"))
                .map(rest -> rest.substring(0, rest.indexOf('/')))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        byte[] bytes = files.get(PackResources.PACK_META);
        if (bytes == null) {
            return null;
        }
        // 与原版 AbstractPackResources.getMetadataFromStream 一致：fromJson 收到的是
        // pack.mcmeta 里 <节名> 对应的子对象，而非根对象
        JsonObject root;
        try {
            root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
        String name = serializer.getMetadataSectionName();
        if (!root.has(name)) {
            return null;
        }
        return serializer.fromJson(root.getAsJsonObject(name));
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
    }

    /** 每次打开返回同一份只读内容的供应商 */
    private record ResourcesSupplier(Map<String, byte[]> files) implements Pack.ResourcesSupplier {

        @Override
        public PackResources openPrimary(PackLocationInfo location) {
            return new VirtualDataPack(location, files);
        }

        @Override
        public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            return new VirtualDataPack(location, files);
        }
    }
}
