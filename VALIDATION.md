# SCEX Botania / ExtraBotany compatibility 1.3.0 validation

Validated on 2026-09-05 (Asia/Shanghai) against Minecraft 1.21.1,
NeoForge 21.1.248, Botania 456-20260822.093314-4, and
[SCEX ExtraBotany 2.0-scex.5-dev](https://github.com/rianfalltwilight-lab/scex-extrabotany/releases/tag/v2.0-scex.5-dev).

## Frozen artifacts

- ExtraBotany runtime JAR: 6,474,768 bytes; SHA-256
  `8d385776211f1a449f1c7c25c480286c1cb46c8d050145b9d5c7014216a6218a`.
- Compatibility runtime JAR: 7,409 bytes; SHA-256
  `c593f0ae2328a52136bae925b3460b6db19b4b761e731092ffa11ba88d34d479`.
- The compatibility metadata requires exactly `2.0-scex.5-dev`.
- The runtime compatibility JAR contains zero development-client probe classes.

## Results

- Java 21 / Gradle 9.2.1 clean test and build completed successfully.
- JUnit contract tests: 2/2 passed, with zero failures, errors, or skips.
- A physical client used real `useItemOn` interaction packets and passed seven
  server-side assertions covering ExtraBotany flower selection, flower-to-spreader
  binding, Botania flower-to-pool binding, pool selection for Manalink, Manalink
  binding, and persistence of all three links after save and reopen.
- A minimal dedicated server loaded the exact versions, reached ready state with
  zero ERROR-level entries, stopped normally, saved all dimensions, and exited 0.
- A 236-JAR SCEX acceptance server loaded the exact pair and introduced zero new
  errors compared with its established 164-entry baseline.

The original machine-local logs are intentionally not published because they
contain environment paths and instance details. Their frozen SHA-256 values are:

- Physical client: `a0f7d6d4b83a3199c7a54b1308fc4e7d3a0affae5001099873ba05f593c5de5f`
- Minimal dedicated server: `6c0aa1009806747d8f685fce009dd2d84bc4c534a3519fc3324faf80d0baa9b8`
- Full-pack dedicated server: `5c5c22110c4d2d16cf78cf5140b4fb9ff1919687fed01295ed9628800b346c57`

## Limits

This is bounded evidence, not proof of universal compatibility. The bridge is
coupled to ExtraBotany's current wand-selection and `ManalinkBlockEntity`
signatures. Optional-mod combinations, long-running load, every multiplayer
timing case, and future ExtraBotany versions require renewed validation.

Publication does not deploy the module to a production server.
