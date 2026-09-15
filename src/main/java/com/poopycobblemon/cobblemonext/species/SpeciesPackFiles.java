package com.poopycobblemon.cobblemonext.species;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 种族数据包的文件布局（纯 Java，无 Minecraft 依赖，可在无头环境测试）：
 * {@code pack.mcmeta} + {@code data/<命名空间>/species/<种族>.json}。
 * {@link VirtualDataPack} 把这份映射包成标准 {@code PackResources}。
 */
final class SpeciesPackFiles {

    /** 定义列表为空时 create 返回 null，调用方应跳过注入空包 */
    static Map<String, byte[]> create(List<SpeciesDefinition> definitions, int packFormat) {
        if (definitions == null || definitions.isEmpty()) {
            return null;
        }
        LinkedHashMap<String, byte[]> files = new LinkedHashMap<>();
        String meta = "{\"pack\":{\"description\":\"cobblemon-ext generated species\","
                + "\"pack_format\":" + packFormat + "}}";
        files.put("pack.mcmeta", meta.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (SpeciesDefinition definition : definitions) {
            files.put(definition.resourcePath(),
                    definition.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return files;
    }

    private SpeciesPackFiles() {
    }
}
