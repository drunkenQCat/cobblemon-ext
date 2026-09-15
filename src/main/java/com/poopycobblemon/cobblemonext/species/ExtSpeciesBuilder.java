package com.poopycobblemon.cobblemonext.species;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义宝可梦种族构建器：用 Java 代码拼出一份合法的种族数据包 JSON
 * （{@code data/<命名空间>/species/<种族>.json}），不要求调用方手写几百行 JSON。
 *
 * <p>字段模型与 Cobblemon 1.7.3 自带种族文件一致（参见
 * {@code data/cobblemon/species/generation1/abra.json}）。常用项都有默认值，
 * 只有命名空间、种族 id、主属性与种族值组必须显式给出。
 *
 * <p>本类是纯 Java 实现（含 JSON 转义），不引用 Minecraft 类型，可在无头环境测试。
 *
 * <p>典型用法：
 * <pre>{@code
 * ExtSpeciesRegistry.register(ExtSpecies.create("poopy_cobblemon", "dungemon")
 *         .name("Dungemon").dexNumber(1026)
 *         .primaryType("poison").secondaryType("dark")
 *         .baseStats(60, 80, 50, 90, 60, 100)
 *         .abilities("stench", "innerfocus").hiddenAbility("sticky_hold")
 *         .move("1:tackle", "9:poison_gas", "egg:scary_face")
 *         .custom("poopsky:poop_type", "toxic")   // 玩法扩展字段，原样透传
 *         .build());
 * }</pre>
 *
 * <p>构建结果经 {@link ExtSpeciesRegistry#register} 注册后，由库注入的内存数据包
 * 在（重）加载时被 Cobblemon 自己的种族加载器拾取，并自动同步到客户端。
 */
public final class ExtSpeciesBuilder {

    private final String namespace;
    private final String speciesId;

    private String displayName;
    private Integer dexNumber;
    private String primaryType;
    private String secondaryType;
    private double maleRatio = 0.5D;
    private double height = 1.0D;
    private double weight = 1.0D;
    private final List<String> pokedex = new ArrayList<>();
    private final List<String> labels = new ArrayList<>(List.of("custom"));
    private final List<String> aspects = new ArrayList<>();
    private final List<String> abilities = new ArrayList<>();
    private final List<String> hiddenAbilities = new ArrayList<>();
    private final List<String> eggGroups = new ArrayList<>(List.of("undiscovered"));
    private final LinkedHashMap<String, Integer> baseStats = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> evYield = new LinkedHashMap<>();
    private int baseExperienceYield = 60;
    private String experienceGroup = "medium_fast";
    private int catchRate = 45;
    private int eggCycles = 20;
    private int baseFriendship = 50;
    private double baseScale = 1.0D;
    private double hitboxWidth = 0.8D;
    private double hitboxHeight = 0.8D;
    private boolean hitboxFixed = false;
    private final List<String> moves = new ArrayList<>();
    private boolean implemented = true;
    private final LinkedHashMap<String, Object> customFields = new LinkedHashMap<>();

    private ExtSpeciesBuilder(String namespace, String speciesId) {
        this.namespace = namespace;
        this.speciesId = speciesId;
        this.displayName = defaultDisplayName(speciesId);
    }

    /** 以数据包命名空间 + 种族 id 开始构建（两者同时决定资源路径） */
    public static ExtSpeciesBuilder create(String namespace, String speciesId) {
        return new ExtSpeciesBuilder(namespace, speciesId);
    }

    /** 显示名（JSON 的 name 字段；缺省时由种族 id 首字母大写得到） */
    public ExtSpeciesBuilder name(String displayName) {
        this.displayName = requireText(displayName, "name");
        return this;
    }

    /** 全国图鉴编号 */
    public ExtSpeciesBuilder dexNumber(int nationalPokedexNumber) {
        this.dexNumber = nationalPokedexNumber;
        return this;
    }

    /** 主属性（小写元素名，如 {@code poison}）——必填 */
    public ExtSpeciesBuilder primaryType(String type) {
        this.primaryType = requireText(type, "primaryType");
        return this;
    }

    /** 副属性（不需要则不调） */
    public ExtSpeciesBuilder secondaryType(String type) {
        this.secondaryType = requireText(type, "secondaryType");
        return this;
    }

    /** 雄性比例 0.0～1.0（-1 表示无性别） */
    public ExtSpeciesBuilder maleRatio(double ratio) {
        this.maleRatio = ratio;
        return this;
    }

    /** 图鉴身高（dm）与体重（kg） */
    public ExtSpeciesBuilder dexMetrics(double height, double weight) {
        this.height = height;
        this.weight = weight;
        return this;
    }

    /** 图鉴描述的翻译键（可多行） */
    public ExtSpeciesBuilder pokedex(String... translationKeys) {
        pokedex.addAll(List.of(translationKeys));
        return this;
    }

    /** 标签（默认 {@code ["custom"]}，追加会保留默认项） */
    public ExtSpeciesBuilder labels(String... labels) {
        this.labels.addAll(List.of(labels));
        return this;
    }

    /** 方面（aspects，供渲染/特性系统使用） */
    public ExtSpeciesBuilder aspects(String... aspects) {
        this.aspects.addAll(List.of(aspects));
        return this;
    }

    /** 普通特性池（1～2 个） */
    public ExtSpeciesBuilder abilities(String... abilityNames) {
        abilities.addAll(List.of(abilityNames));
        return this;
    }

    /** 隐藏特性（自动加 {@code h:} 前缀，可多个） */
    public ExtSpeciesBuilder hiddenAbility(String... abilityNames) {
        for (String abilityName : abilityNames) {
            hiddenAbilities.add(requireText(abilityName, "hiddenAbility"));
        }
        return this;
    }

    /** 蛋组 */
    public ExtSpeciesBuilder eggGroups(String... groups) {
        if (eggGroups.size() == 1 && "undiscovered".equals(eggGroups.get(0))) {
            eggGroups.clear(); // 显式设置时替换默认值
        }
        eggGroups.addAll(List.of(groups));
        return this;
    }

    /**
     * 种族值（顺序：hp, attack, defence, special_attack, special_defence, speed）——必填。
     */
    public ExtSpeciesBuilder baseStats(int hp, int attack, int defence, int specialAttack,
                                       int specialDefence, int speed) {
        baseStats.put("hp", hp);
        baseStats.put("attack", attack);
        baseStats.put("defence", defence);
        baseStats.put("special_attack", specialAttack);
        baseStats.put("special_defence", specialDefence);
        baseStats.put("speed", speed);
        return this;
    }

    /** 单项努力值产出（stat 用 {@code hp/attack/…/speed}） */
    public ExtSpeciesBuilder evYield(String stat, int value) {
        evYield.put(requireText(stat, "evYield.stat"), value);
        return this;
    }

    /** 基础经验产出 */
    public ExtSpeciesBuilder baseExperienceYield(int yield) {
        this.baseExperienceYield = yield;
        return this;
    }

    /** 经验组（medium_fast / slow / fast / …） */
    public ExtSpeciesBuilder experienceGroup(String group) {
        this.experienceGroup = requireText(group, "experienceGroup");
        return this;
    }

    /** 捕获率 */
    public ExtSpeciesBuilder catchRate(int catchRate) {
        this.catchRate = catchRate;
        return this;
    }

    /** 孵化周期 */
    public ExtSpeciesBuilder eggCycles(int cycles) {
        this.eggCycles = cycles;
        return this;
    }

    /** 基础亲密度 */
    public ExtSpeciesBuilder baseFriendship(int friendship) {
        this.baseFriendship = friendship;
        return this;
    }

    /** 基础渲染缩放 */
    public ExtSpeciesBuilder baseScale(double scale) {
        this.baseScale = scale;
        return this;
    }

    /** 世界碰撞箱（宽、高，方块单位） */
    public ExtSpeciesBuilder hitbox(double width, double height) {
        this.hitboxWidth = width;
        this.hitboxHeight = height;
        return this;
    }

    /** 碰撞箱是否固定（不随缩放变化） */
    public ExtSpeciesBuilder hitboxFixed(boolean fixed) {
        this.hitboxFixed = fixed;
        return this;
    }

    /**
     * 可学习招式，格式与种族 JSON 一致：{@code "等级:招式"}（如 {@code "1:tackle"}）、
     * {@code "egg:招式"}、{@code "tm:招式"}。
     */
    public ExtSpeciesBuilder move(String... levelColonMove) {
        for (String move : levelColonMove) {
            moves.add(requireText(move, "move"));
        }
        return this;
    }

    /** 是否为完整实现（影响图鉴过滤；默认 true） */
    public ExtSpeciesBuilder implemented(boolean implemented) {
        this.implemented = implemented;
        return this;
    }

    /**
     * 玩法扩展字段：原样写进 JSON 顶层，供附属自己的数据加载/战斗逻辑读取。
     * value 支持 String / Number / Boolean / Map / List（嵌套同理）。
     */
    public ExtSpeciesBuilder custom(String key, Object value) {
        customFields.put(requireText(key, "custom.key"), value);
        return this;
    }

    /** 生成定义；缺必填项（主属性/种族值）或命名空间非法时抛 IllegalStateException */
    public SpeciesDefinition build() {
        requireText(namespace, "namespace");
        requireText(speciesId, "speciesId");
        if (primaryType == null) {
            throw new IllegalStateException("primaryType 未设置: " + speciesId);
        }
        if (baseStats.isEmpty()) {
            throw new IllegalStateException("baseStats 未设置: " + speciesId);
        }

        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("implemented", implemented);
        root.put("nationalPokedexNumber", dexNumber == null ? -1 : dexNumber);
        root.put("name", displayName);
        root.put("primaryType", primaryType);
        if (secondaryType != null) {
            root.put("secondaryType", secondaryType);
        }
        root.put("maleRatio", maleRatio);
        root.put("height", height);
        root.put("weight", weight);
        root.put("pokedex", List.copyOf(pokedex));
        root.put("labels", List.copyOf(labels));
        root.put("aspects", List.copyOf(aspects));
        List<String> abilityPool = new ArrayList<>(abilities);
        for (String hidden : hiddenAbilities) {
            abilityPool.add("h:" + hidden);
        }
        if (abilityPool.isEmpty()) {
            throw new IllegalStateException("abilities 未设置（至少一个普通特性）: " + speciesId);
        }
        root.put("abilities", List.copyOf(abilityPool));
        root.put("eggGroups", List.copyOf(eggGroups));
        root.put("baseStats", copyOf(baseStats));
        root.put("evYield", copyOf(evYield));
        root.put("baseExperienceYield", baseExperienceYield);
        root.put("experienceGroup", experienceGroup);
        root.put("catchRate", catchRate);
        root.put("eggCycles", eggCycles);
        root.put("baseFriendship", baseFriendship);
        root.put("baseScale", baseScale);
        root.put("hitbox", Map.of("width", hitboxWidth, "height", hitboxHeight, "fixed", hitboxFixed));
        if (!moves.isEmpty()) {
            root.put("moves", List.copyOf(moves));
        }
        root.putAll(customFields);

        return new SpeciesDefinition(namespace, speciesId + ".json", JsonWriter.write(root));
    }

    private static Map<String, Object> copyOf(LinkedHashMap<String, Integer> source) {
        return new LinkedHashMap<>(source);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(field + " 不能为空");
        }
        return value;
    }

    private static String defaultDisplayName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) {
            return speciesId;
        }
        return Character.toUpperCase(speciesId.charAt(0)) + speciesId.substring(1);
    }

    /**
     * 极简 JSON 输出：固定按插入顺序（LinkedHashMap），支持 String / Number /
     * Boolean / Map / List，转义规则与 JSON 规范一致。不用 Gson 是为了让
     * 构建器在无 Minecraft 类路径的环境（纯 JVM 测试）下也能编译运行。
     */
    private static final class JsonWriter {

        private static String write(Map<String, Object> root) {
            StringBuilder sb = new StringBuilder();
            writeValue(sb, root);
            return sb.toString();
        }

        private static void writeValue(StringBuilder sb, Object value) {
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String s) {
                writeString(sb, s);
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map<?, ?> map) {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    writeString(sb, String.valueOf(entry.getKey()));
                    sb.append(':');
                    writeValue(sb, entry.getValue());
                }
                sb.append('}');
            } else if (value instanceof Iterable<?> list) {
                sb.append('[');
                boolean first = true;
                for (Object item : list) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    writeValue(sb, item);
                }
                sb.append(']');
            } else {
                throw new IllegalArgumentException("不支持的 JSON 值类型: " + value.getClass());
            }
        }

        private static void writeString(StringBuilder sb, String s) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
        }
    }
}
