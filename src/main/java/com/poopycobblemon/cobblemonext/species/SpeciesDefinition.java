package com.poopycobblemon.cobblemonext.species;

/**
 * 一个已生成的自定义宝可梦种族定义：内容即数据包 JSON 文本。
 * <p>刻意不引用任何 Minecraft 类型，保证构建器链路可在纯 JVM 环境下测试。
 * Minecraft 侧的落地（内存数据包路径等）由 {@link ExtSpeciesRegistry} 负责。
 *
 * @param namespace 数据包命名空间（对应 {@code data/<namespace>/species/}）
 * @param fileName  文件名（不含目录，如 {@code dungemon.json}）
 * @param json      种族 JSON 文本（构建器产出的最终内容）
 */
public record SpeciesDefinition(String namespace, String fileName, String json) {

    /** 数据包内该定义的完整路径（供 VirtualDataPack 与日志使用） */
    public String resourcePath() {
        return "data/" + namespace + "/species/" + fileName;
    }
}
