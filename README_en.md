# SCEX Botania / ExtraBotany Forest-Wand Compatibility

最新冻结交付与验证边界 / Current frozen delivery: [release notes](docs/releases/1.4.2.md).
**[1.4.2：直接下载运行 JAR / Download JAR](https://github.com/rianfalltwilight-lab/scex-botania-extrabotany-compat/releases/download/v1.4.2/SCEX-Botania-ExtraBotany-Compat-1.21.1-1.4.2.jar)** · [更新说明 / Release notes](docs/releases/1.4.2.md)

> An unofficial BOTH-side NeoForge compatibility mod maintained by Space Creator EX (SCEX). It restores forest-wand binding behavior between Botania 456 and the SCEX ExtraBotany 1.21.1 port.

[Chinese](README.md)

The current version is **1.4.2**. This repository and [SCEX ExtraBotany `2.0-scex.8-dev`](https://github.com/rianfalltwilight-lab/scex-extrabotany) are separate projects: ExtraBotany provides the main port, while this repository provides a interaction bridge. Neither project bundles the other.

## Compatibility matrix

| Component | Validated version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Botania | 456-20260822.093314-4 (reports 456-SNAPSHOT at runtime) |
| ExtraBotany | [`2.0-scex.8-dev`](https://github.com/rianfalltwilight-lab/scex-extrabotany/releases/tag/v2.0-scex.8-dev) |
| This bridge | 1.4.2 |
| Java | 21 |

Install the same compatibility JAR on client and server alongside the exact Botania and ExtraBotany versions above. Metadata accepts ExtraBotany `[2.0-scex.7-dev,3)`, Botania `[456-SNAPSHOT,457)` and NeoForge `[21.1.248,)`; other combinations have not been individually validated.

## What it fixes

1. Exposes ExtraBotany block entities that implement `WandBindable` through Botania 456's NeoForge capability, allowing generating flowers to bind to mana spreaders.
2. Completes normal Botania flower-to-pool binding before ExtraBotany's pool interceptor consumes the click, while preserving Botania's selection-clearing behavior.
3. Applies pool-to-Manalink links on both logical sides and marks the block entity changed so the link survives a chunk save.

Keeping this bridge separate prevents SCEX-pack-specific interaction patches from being mixed into the main ExtraBotany port. The bridge depends on the current ExtraBotany wand-selection and Manalink method signatures; every ExtraBotany update therefore requires a rebuild and renewed interaction, save/reload, and dedicated-server validation.

## Historical validation (1.4.0) summary (see current release notes for scope)

- 2/2 JUnit contract tests passed.
- 7/7 server assertions driven by real client interaction packets passed.
- ExtraBotany flower→spreader, Botania flower→pool, and pool→Manalink bindings all survived save, close, and reopen.
- The 236-JAR server result belongs to 1.3.0; that full-pack test was not repeated for 1.4.2.
- The release JAR contains no development-only physical-client harness classes.

See [VALIDATION.md](VALIDATION.md) for scope and hashes.

## Build

```powershell
$env:JAVA_HOME = '<Java 21 JDK>'
.\gradlew.bat --no-daemon clean test build
```

Use `./gradlew` on Linux/macOS. Botania is resolved from its public Maven coordinate; this repository does not redistribute Botania or ExtraBotany dependency JARs. Place the exact ExtraBotany release JAR in the development run directory before executing the physical-client probe.

## License and provenance

Released under the [MIT License](LICENSE). Third-party names, code, and assets remain with their respective owners. Dependency relationships and AI involvement are documented in [NOTICE](NOTICE) and [AI-GENERATED.md](AI-GENERATED.md).
