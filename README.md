# 消消乐 (Match-3)

JavaFX 消消乐，Material 3 界面，支持联机，配色随壁纸变化。原为 CS109 课程项目。

[English](docs/en/README.md)

## 玩法

单机 / 创建房间 / 加入房间；四种难度（自定义可改目标分数、步数、限时）；
存档读档；自动走子与自动落定；音乐；深浅色切换；七种界面语言。

## 构建

需要 JDK 26（版本在 `build.gradle.kts` 的 toolchain 里）。用 wrapper 即可，Gradle 会自己下载。

```shell
./gradlew run        # 启动
./gradlew fatJar     # build/libs/match3.jar，可直接运行
./gradlew smokeTest  # 模型层检查
./gradlew dist       # Windows 安装包
```

界面基于 JavaFX + [MaterialFX](https://github.com/palexdev/MaterialFX) +
[MonetFX](https://github.com/Glavo/MonetFX)，依赖全部来自 Maven Central。
`dist` 在 Windows 上还需 [WiX](https://wixtoolset.org/)（v7 起要先 `wix eula accept wix7`）。

## 发布

推送 main 自动发布 jar；手动触发 Release 并勾选 `pack_exe` 才额外构建安装包。
标签为 `r{1000 + 提交数}`，安装包内部版本为 `1.{提交数 / 100}.{提交数 % 100}`。

## 语言

界面语言按系统区域自动选择，设置里可切换。`-Dmatch3.lang=<tag>` 覆盖自动检测。
文案在 `resource/i18n/messages_<tag>.properties`（UTF-8），新增语言时复制英文文件翻译，
再到 `I18n.SUPPORTED` 注册该 Locale。
