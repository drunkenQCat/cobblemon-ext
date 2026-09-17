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
| `ExtHandlers.subscribe(bus, filter, action)` | 声明式注册：过滤器匹配才执行；返回 `Registration` 句柄，可随时注销。 |
| `ExtHandlers.subscribeOnce(bus, filter, action)` | 一次性订阅：首次匹配后自动注销。 |
| `ExtHandlers.unregisterAll()` | 注销通过本表登记的全部订阅（服务器关闭等收尾场景）。 |

在模组初始化时订阅一次。处理器需检查携带者的存活、道具和动画就绪状态。发送请求前调用 `ExtBridge.ensurePatched()`，再用 `ExtBridge.isPatched()` 确认补丁已加载。

JavaScript 补丁在运行中的 GraalJS 上下文包装 `BattleStream._writeLine`，处理 `>cobblemonext_damage` 和 `>cobblemonext_boost`，保留 Showdown 的私有／公开 HP 消息，不覆盖 Cobblemon 文件。

## 构建

安装 JDK 21、Node.js 22 和 Python 3.11+，在本仓库运行：

```sh
python scripts/build.py
```

Gradle 根据 [gradle.properties](gradle.properties) 中固定的版本 ID 从 Modrinth Maven 解析 Cobblemon，校验[已提交的依赖哈希](gradle/verification-metadata.xml)，并将其模拟器解压到 `build/showdown` 用于桥接测试。脚本在 `dist/` 生成 JAR 和 `SHA256SUMS.txt`，不需要父仓库或整合包。增量构建直接使用 `./gradlew build`（Windows 使用 `./gradlew.bat build`），自动准备依赖并运行所有检查。

更新 Maven 依赖时，修改版本 ID，运行 `./gradlew --write-verification-metadata sha256 build` 生成校验文件，并对照发布方核实新增哈希后提交。CI 只验证已提交的哈希。Ext 的依赖变化时，两仓库都需要更新校验文件；组合构建使用父仓库的校验文件。

## 安装与发布

客户端和服务端均安装 `cobblemon-ext-1.2.jar`，并安装 Cobblemon 和 Kotlin for Forge。本库可独立于 [Poopy Cobblemon](https://github.com/drunkenQCat/Poopy-Cobblemon) 使用；后者通过 Git 子模块固定引用本库。

本仓库使用自己的 [`VERSION`](VERSION)。发布时更新版本号，在 `releases/` 添加发行说明，用目标版本运行 `python scripts/build.py --tag v1.2`，推送到 `main` 并等待 CI 通过，再推送对应的 `v<版本>` 标签。[GitHub Actions](.github/workflows/ci.yml) 在推送、PR 和手动触发时运行 Ubuntu、Windows 构建、测试和免凭据发布预览，并比较两平台产物的 SHA-256。只有构建通过且产物一致，版本标签才会触发 GitHub Release，随后上传 CurseForge；普通分支、PR 和手动运行只验证。

CurseForge 项目 ID 为 **1693831**，发布设置保存在 [publishing/curseforge.json](publishing/curseforge.json)。在本仓库的 **Settings → Secrets and variables → Actions** 添加 `CURSEFORGE_TOKEN`，值为具有此项目上传权限的 CurseForge API token。两个仓库分别设置；GitHub Release 使用 Actions 自动提供的 `GITHUB_TOKEN`，无需额外个人 token。

本仓库只向 CurseForge 上传 `cobblemon-ext-<版本>.jar`，声明 Cobblemon 和 Kotlin for Forge 为必需依赖。

构建后可单独检查待上传文件、版本、依赖和发行说明：

```sh
./gradlew -p publishing curseforgePreview -PreleaseTag=v1.2
```

Windows 使用 `./gradlew.bat`。预览不会读取 token，也不会调用 CurseForge API；它不验证远端项目权限、依赖 slug 或审核状态。Gradle 首次运行仍需下载插件依赖。正式上传任务为 `curseforge`，必须显式传入与 `VERSION` 一致的 `-PreleaseTag`，CI 会下载并复验已经构建的 JAR 后执行它。

CurseForge 上传成功后可能仍需审核。如果上传时网络中断，先检查项目文件列表再重试，以免重复上传；在 Actions 中选择 **Re-run failed jobs**，避免重新执行已成功的发布任务。上传失败不会撤回已经成功发布的 GitHub Release。

[Dependabot](.github/dependabot.yml) 每周检查 Gradle 依赖和 GitHub Actions；更新需要人工审查和 CI 通过。发布插件固定在兼容 Java 21 的 1.1.28。升级它时运行 `./gradlew -p publishing --write-verification-metadata sha256 curseforgePreview`，核实并提交独立的 `publishing/gradle/verification-metadata.xml`。不要仅为消除校验失败而接受未核实的哈希。

Java 和 JavaScript 代码使用 [MIT](LICENSE)，原创非代码资源使用 [CC BY-NC 4.0](LICENSE-ASSETS.md)。
