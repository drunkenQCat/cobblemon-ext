# cobblemon-ext

NeoForge 扩展库，提供 Cobblemon 战斗事件，以及修改伤害和能力等级的 Showdown 桥接。供 Poopy Cobblemon 使用，本身不添加物品或玩法。

[English](README.md) · [下载](https://github.com/drunkenQCat/cobblemon-ext/releases) · [CI](https://github.com/drunkenQCat/cobblemon-ext/actions/workflows/ci.yml)

已测试 Minecraft 1.21.1、NeoForge 21.1.240、Cobblemon 1.7.3 和 Java 21。扩展库使用 Cobblemon 内部接口，升级前置后需要复测。

## API

Java 包名为 `com.poopycobblemon.cobblemonext`；使用旧包名的附属模组需要使用本库重新编译。

| 事件或方法 | 行为 |
| --- | --- |
| `ExtEvents.MOVE_USED` | 在 `MoveInstruction.invoke` 返回后发射，包含使用者、可空目标和招式；动画可能尚未完成。 |
| `BATTLE_ACTIVE_READY` | 初始出战位分配完成后，每场战斗发射一次。 |
| `ACTIVE_POKEMON_CHANGED` | 出战位的 `BattlePokemon` 变化时发射，包括变为 null。 |
| `BATTLE_TURN` | 在 `PokemonBattle.turn(int)` 执行后发射，忽略重复或倒退的回合号。 |
| `BATTLE_ENDED` | 清理战斗记录并通知订阅者。 |
| `currentTurn(battleId)` | 最新已执行回合号；首回合前和清理后为 0。 |
| `ExtBridge.applyDamage(battle, uuid, amount)` | 通过 Showdown 请求伤害，至少保留 1 HP。 |
| `ExtBridge.applyBoost(battle, uuid, stat, stages)` | 通过 Showdown 请求能力等级变化。 |

在模组初始化时订阅一次。处理器需检查携带者的存活、道具和动画就绪状态。发送请求前调用 `ExtBridge.ensurePatched()`，再用 `ExtBridge.isPatched()` 确认补丁已加载。

JavaScript 补丁在运行中的 GraalJS 上下文包装 `BattleStream._writeLine`，处理 `>cobblemonext_damage` 和 `>cobblemonext_boost`，保留 Showdown 的私有／公开 HP 消息，不覆盖 Cobblemon 文件。

## 构建

安装 JDK 21、Node.js 22 和 Python 3.11+，在本仓库运行：

```sh
python scripts/build.py
```

脚本下载并校验 Cobblemon，构建本库，测试 Showdown 桥接，在 `dist/` 生成 JAR 和 `SHA256SUMS.txt`，不需要父仓库或整合包。增量构建时，先运行一次 `python scripts/prepare_dependencies.py`，再用 `./gradlew build`（Windows 使用 `./gradlew.bat build`）。

## 安装与发布

客户端和服务端均安装 `cobblemon-ext-1.2.jar`，并安装 Cobblemon 和 Kotlin for Forge。本库可独立于 [Poopy Cobblemon](https://github.com/drunkenQCat/Poopy-Cobblemon) 使用；后者通过 Git 子模块固定引用本库。

本仓库使用自己的 [`VERSION`](VERSION)。发布时更新版本号，在 `releases/` 添加发行说明，用目标版本运行 `python scripts/build.py --tag v1.2`，推送到 `main` 并等待 CI 通过，再推送对应的 `v<版本>` 标签。Ubuntu、Windows 构建通过后，工作流上传草稿、下载复验产物，再公开发布；手动触发只验证。

Java 和 JavaScript 代码使用 [MIT](LICENSE)，原创非代码资源使用 [CC BY-NC 4.0](LICENSE-ASSETS.md)。
