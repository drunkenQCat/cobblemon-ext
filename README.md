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

Subscribe once during mod initialization. Handlers should check the holder's health, item and animation readiness. Call `ExtBridge.ensurePatched()` and confirm `ExtBridge.isPatched()` before sending requests.

The JavaScript patch wraps `BattleStream._writeLine` inside the running GraalJS context. It handles `>cobblemonext_damage` and `>cobblemonext_boost` without overwriting Cobblemon files, and preserves Showdown's private/public HP messages.

## Build

Install JDK 21, Node.js 22 and Python 3.11+, then run from this repository:

```sh
python scripts/build.py
```

This downloads hash-verified Cobblemon, builds the library, tests the Showdown bridge, and writes the JAR and `SHA256SUMS.txt` to `dist/`. No parent repository or modpack installation is needed. For incremental builds, prepare dependencies once with `python scripts/prepare_dependencies.py`, then use `./gradlew build` (`./gradlew.bat build` on Windows).

## Install and release

Install `cobblemon-ext-1.2.jar` on client and server alongside Cobblemon and Kotlin for Forge. The library can be used independently of [Poopy Cobblemon](https://github.com/drunkenQCat/Poopy-Cobblemon), which pins it as a Git submodule.

This repository has its own [`VERSION`](VERSION). For a release, update it and add notes under `releases/`, run `python scripts/build.py --tag v1.2` with the intended version, push to `main`, and wait for CI. Push the matching `v<version>` tag to publish after Ubuntu and Windows builds pass. The workflow uploads a draft, verifies downloaded assets, then publishes; manual runs only validate.

Java and JavaScript code use [MIT](LICENSE); original non-code assets use [CC BY-NC 4.0](LICENSE-ASSETS.md).
