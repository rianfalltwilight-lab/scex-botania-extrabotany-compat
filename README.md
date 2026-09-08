# SCEX Botania / ExtraBotany 森林法杖兼容层

**[1.4.0：直接下载运行 JAR / Download JAR](https://github.com/rianfalltwilight-lab/scex-botania-extrabotany-compat/releases/download/v1.4.0/SCEX-Botania-ExtraBotany-Compat-1.21.1-1.4.0.jar)** · [更新说明 / Release notes](docs/releases/1.4.0.md)

> 这是 Space Creator EX（SCEX）维护的非官方、双端 NeoForge 兼容模组，用于补齐 Botania 456 与 SCEX ExtraBotany 1.21.1 移植版之间的森林法杖绑定行为。

[English](README_en.md)

当前版本为 **1.4.0**。它与 [SCEX ExtraBotany `2.0-scex.6-dev`](https://github.com/rianfalltwilight-lab/scex-extrabotany) 是两个独立仓库：ExtraBotany 提供本体移植，本仓库只提供版本锁定的法杖兼容层，两者不互相内置。

## 适配矩阵

| 组件 | 精确版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Botania | 456-20260822.093314-4（运行时显示 456-SNAPSHOT） |
| ExtraBotany | [`2.0-scex.6-dev`](https://github.com/rianfalltwilight-lab/scex-extrabotany/releases/tag/v2.0-scex.6-dev) |
| 本兼容层 | 1.4.0 |
| Java | 21 |

安装时应在客户端和服务端放入同一份兼容层 JAR，并同时安装上表的 Botania 与 ExtraBotany。不要将 `1.4.0` 与其他 ExtraBotany 版本混用。

## 解决的问题

1. 向 Botania 456 的 NeoForge `WandBindable` 能力系统暴露实际实现该接口的 ExtraBotany 方块实体，使产能花能被选中并绑定至魔力发射器。
2. 在 ExtraBotany 的魔力池点击拦截器消费交互前，先完成 Botania 原生花到魔力池的绑定，并保持原生选择清除语义。
3. 在逻辑客户端与服务端同步完成魔力池到 Manalink 的链接，并标记方块实体已变更，使绑定可在区块保存后存续。

将兼容层保持为独立模组，可以避免将 SCEX 整合包特定的交互补丁混入 ExtraBotany 本体移植。该层依赖 ExtraBotany 当前的法杖选择和 Manalink 方法签名；ExtraBotany 升级时必须重新编译并重跑交互、保存重载和专服验收。

## 验证摘要

- JUnit 契约测试 2/2 通过。
- 真实客户端交互包的服务端断言 7/7 通过。
- ExtraBotany 花→发射器、Botania 花→魔力池、魔力池→Manalink 三条绑定在保存、关闭和重开世界后仍保持。
- 本版未重跑 236-JAR 整包专服；该项旧验收只适用于 1.3.0。
- 发布 JAR 不包含开发专用的物理客户端探针。

详细边界与哈希见 [VALIDATION.md](VALIDATION.md)。

## 构建

```powershell
$env:JAVA_HOME = '<Java 21 JDK>'
.\gradlew.bat --no-daemon clean test build
```

Linux/macOS 使用 `./gradlew`。Botania 从公开 Maven 坐标获取；仓库不重新分发 Botania 或 ExtraBotany 依赖 JAR。运行物理客户端探针前，需将精确版本的 ExtraBotany JAR 放入开发运行目录。

## 许可证与来源

本项目以 [MIT License](LICENSE) 发布。第三方名称、代码和素材仍归各自权利人；依赖关系与 AI 参与范围见 [NOTICE](NOTICE) 和 [AI-GENERATED.md](AI-GENERATED.md)。
