package com.poopycobblemon.cobblemonext.species;

import com.poopycobblemon.cobblemonext.CobblemonExt;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 自定义种族注册表：附属模组在初始化阶段调用 {@link #register} 交出
 * {@link SpeciesDefinition}；服务器数据包仓储创建时（每次启动与 /reload）
 * 本库把它们打包成一个内存数据包提交，由 Cobblemon 原生加载链路拾取。
 *
 * <p>注册须发生在服务器首次拉取数据包之前（模组构造期即满足）；
 * 服务器运行中新注册的定义需要 /reload 才会生效。
 */
@EventBusSubscriber(modid = CobblemonExt.MOD_ID)
public final class ExtSpeciesRegistry {

    private static final List<SpeciesDefinition> DEFINITIONS = new CopyOnWriteArrayList<>();

    private ExtSpeciesRegistry() {
    }

    /**
     * 登记一个种族定义；同路径（命名空间 + 文件名）重复注册会覆盖旧定义。
     * 返回 false 表示覆盖了同路径旧值。
     */
    public static boolean register(SpeciesDefinition definition) {
        if (definition == null) {
            return false;
        }
        String path = definition.resourcePath();
        for (int i = 0; i < DEFINITIONS.size(); i++) {
            if (DEFINITIONS.get(i).resourcePath().equals(path)) {
                DEFINITIONS.set(i, definition);
                CobblemonExt.LOGGER.info("[cobblemon-ext] 覆盖种族定义：{}", path);
                return false;
            }
        }
        DEFINITIONS.add(definition);
        CobblemonExt.LOGGER.info("[cobblemon-ext] 登记种族定义：{}", path);
        return true;
    }

    /** 已登记的定义快照（只读） */
    public static List<SpeciesDefinition> definitions() {
        return List.copyOf(DEFINITIONS);
    }

    /** 服务器数据包仓储创建时注入内存包（模组总线事件，静态注册） */
    @SubscribeEvent
    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || DEFINITIONS.isEmpty()) {
            return;
        }
        Pack pack = VirtualDataPack.buildPack(definitions());
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
