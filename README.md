# Left Handed
Mirror your character mesh for a left-handed appearance

A [RuneLite](https://runelite.net) plugin generated from the official
[plugin-hub template](https://github.com/runelite/plugin-hub-tooling) via
`create_new_plugin.py`.

## How it works

The plugin wraps the GPU plugin's draw callbacks and mirrors the local player's model
(vertex X, face winding and per-corner colors) only for the duration of its `drawTemp` upload. Requires the
GPU plugin (default); the CPU renderer has no equivalent hook point, so nothing changes
there. Modes: OFF, ALWAYS, BOW_ONLY (mirrors while a bow is wielded).

## Local toolchain (fully self-contained)

Everything needed to build and run lives in this folder — no system-wide installs:

| Path | Contents |
|---|---|
| `tools/jdk` | Eclipse Temurin JDK 21 (builds with `--release 11`, matching RuneLite CI) |
| `.gradle-home/` | Gradle distribution + dependency cache (created on first run) |
| `vendor/` | Reference clones: `runelite`, `example-plugin`, `plugin-hub`, `plugin-hub-tooling` |

## Building & running

```sh
. ./env.sh          # sets JAVA_HOME and GRADLE_USER_HOME to this folder
./gradlew build     # compile + package
./gradlew run       # launch the RuneLite dev client with this plugin loaded
```

The dev client logs in through the
[Jagex Account plugin](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
Only a human can verify plugin behavior in-game; `build` passing is not a passing test.

## Development guidelines

See [AGENTS.md](AGENTS.md) — the official RuneLite plugin development rules
(threading, config migrations, hub restrictions, packaging).

## Submitting to the hub

Follow the "Submitting a plugin" section of the
[plugin-hub README](https://github.com/runelite/plugin-hub#submitting-a-plugin):
fork plugin-hub, add `plugins/left-handed` with `repository=` and
`commit=`, and open a PR.
