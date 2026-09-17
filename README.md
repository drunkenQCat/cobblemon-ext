# cobblemon-ext

A NeoForge library exposing Cobblemon battle events and a Showdown bridge for damage and stat changes. Used by Poopy Cobblemon; it adds no items or gameplay on its own.

[简体中文](README.zh-CN.md) · [Downloads](https://github.com/drunkenQCat/cobblemon-ext/releases) · [CI](https://github.com/drunkenQCat/cobblemon-ext/actions/workflows/ci.yml)

Tested with Minecraft 1.21.1, NeoForge 21.1.240, Cobblemon 1.7.3 and Java 21. The library hooks into Cobblemon internals, so dependency upgrades require retesting.

## API

Java package: `com.poopycobblemon.cobblemonext`. Addons compiled against the previous package must be rebuilt with this library.

| Event or method | Behavior |
| --- | --- |
| `ExtEvents.MOVE_USED` | After `MoveInstruction.invoke` returns; includes the user, nullable target and move. Animations may still be pending. |
| `BATTLE_ACTIVE_READY` | Once per battle after initial active slots are assigned. |
| `ACTIVE_POKEMON_CHANGED` | When a slot's `BattlePokemon` changes, including becoming null. |
| `BATTLE_TURN` | After `PokemonBattle.turn(int)` executes; ignores repeated or backward turn numbers. |
| `BATTLE_ENDED` | Clears tracked battle state and notifies subscribers. |
| `currentTurn(battleId)` | Latest executed turn, or 0 before the first turn and after cleanup. |
| `ExtBridge.applyDamage(battle, uuid, amount)` | Requests damage through Showdown, leaving at least 1 HP. |
| `ExtBridge.applyBoost(battle, uuid, stat, stages)` | Requests a stat-stage change through Showdown. |
| `ExtHandlers.subscribe(bus, filter, action)` | Declarative registration: runs only when the filter matches; returns a `Registration` handle that can be revoked anytime. |
| `ExtHandlers.subscribeOnce(bus, filter, action)` | One-shot subscription: auto-unregisters after the first match. |
| `ExtHandlers.unregisterAll()` | Removes every registration made through this registry (for shutdown-style cleanup). |

Subscribe once during mod initialization. Handlers should check the holder's health, item and animation readiness. Call `ExtBridge.ensurePatched()` and confirm `ExtBridge.isPatched()` before sending requests.

The JavaScript patch wraps `BattleStream._writeLine` inside the running GraalJS context. It handles `>cobblemonext_damage` and `>cobblemonext_boost` without overwriting Cobblemon files, and preserves Showdown's private/public HP messages.

## Build

Install JDK 21, Node.js 22 and Python 3.11+, then run from this repository:

```sh
python scripts/build.py
```

Gradle resolves Cobblemon from Modrinth Maven using the fixed version ID in [gradle.properties](gradle.properties), verifies [committed dependency checksums](gradle/verification-metadata.xml), and extracts its simulator into `build/showdown` for bridge tests. The script writes the JAR and `SHA256SUMS.txt` to `dist/`. No parent repository or modpack installation is needed. For incremental builds, use `./gradlew build` (`./gradlew.bat build` on Windows); dependency preparation and all checks run automatically.

When updating a Maven dependency, change its version ID, regenerate `gradle/verification-metadata.xml` with `./gradlew --write-verification-metadata sha256 build`, and review the new checksums against the publisher before committing. CI only verifies committed checksums. When Ext dependencies change, update verification metadata in both repositories; the parent file governs composite builds.

## Install and release

Install `cobblemon-ext-1.2.jar` on client and server alongside Cobblemon and Kotlin for Forge. The library can be used independently of [Poopy Cobblemon](https://github.com/drunkenQCat/Poopy-Cobblemon), which pins it as a Git submodule.

This repository has its own [`VERSION`](VERSION). For a release, update it and add notes under `releases/`, run `python scripts/build.py --tag v1.2` with the intended version, push to `main`, and wait for CI. Push the matching `v<version>` tag to publish after Ubuntu and Windows builds pass. [GitHub Actions](.github/workflows/ci.yml) builds, tests and previews publishing without credentials on Ubuntu and Windows, then compares SHA-256 hashes of both packages. A version tag publishes to GitHub and then CurseForge only after both builds pass and the artifacts match. Branch pushes, pull requests and manual runs only validate.

The CurseForge project ID is **1693831**; upload settings live in [publishing/curseforge.json](publishing/curseforge.json). Add `CURSEFORGE_TOKEN` under this repository's **Settings → Secrets and variables → Actions**, using a CurseForge API token with upload permission for this project. Configure each repository separately. GitHub releases use the automatically supplied Actions `GITHUB_TOKEN`; no extra personal token is needed.

This repository uploads only `cobblemon-ext-<version>.jar` to CurseForge, with Cobblemon and Kotlin for Forge marked as required dependencies.

After building, inspect the proposed file, version, dependencies and changelog:

```sh
./gradlew -p publishing curseforgePreview -PreleaseTag=v1.2
```

Use `./gradlew.bat` on Windows. The preview never reads a token or calls the CurseForge API; it does not validate remote project permissions, dependency slugs or moderation status. Gradle may still download plugin dependencies on the first run. The real `curseforge` task requires an explicit `-PreleaseTag` matching `VERSION`; CI runs it against the downloaded and reverified build artifact.

A successful CurseForge upload may still await moderation. If a connection fails during upload, inspect the project's file list before retrying to avoid duplicates. Choose **Re-run failed jobs** in Actions so successful publishing jobs are not repeated. A CurseForge failure does not retract an already published GitHub release.

[Dependabot](.github/dependabot.yml) checks Gradle dependencies and GitHub Actions weekly; updates require review and passing CI. The publishing plugin is pinned to Java 21-compatible 1.1.28. To update it, run `./gradlew -p publishing --write-verification-metadata sha256 curseforgePreview`, verify the new checksums and commit the separate `publishing/gradle/verification-metadata.xml`. Do not accept unverified checksums just to clear a failed build.

Java and JavaScript code use [MIT](LICENSE); original non-code assets use [CC BY-NC 4.0](LICENSE-ASSETS.md).
