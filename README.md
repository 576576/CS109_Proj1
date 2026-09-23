# 消消乐 (Match-3)

Material 3 风格的 JavaFX 消消乐，支持 TCP 联机，配色由当前壁纸动态生成。原为 CS109 课程项目。

English documentation: [docs/en/README.md](docs/en/README.md)

## 功能

- **三种玩法**：单机、创建房间、按地址加入房间。
- **四种难度**：简单 / 普通 / 困难，以及自定义——目标分数、步数上限、限时三项都可编辑。
- **存档与读档**：进行中的对局可写入文件，之后接着玩。
- **自动模式**：auto-go 自己走子，auto-confirm 自动落定，无需每次确认。
- **音乐**：通过 `javax.sound.sampled` SPI 播放 FLAC 与 MP3。
- **主题**：MonetFX 从壁纸主色生成整套 Material 3 配色，换一张壁纸界面跟着变色；深浅色可切。
- **七种界面语言**：启动时按系统区域自动选择，运行时也能切换。

## 环境要求

JDK 26 工具链——版本由 `build.gradle.kts` 的 toolchain 指定。用 wrapper 即可，Gradle 它会自己下载。

界面基于 JavaFX，配 [MaterialFX](https://github.com/palexdev/MaterialFX) 与
[MonetFX](https://github.com/Glavo/MonetFX)。

## 构建

```shell
./gradlew run        # 启动应用
./gradlew fatJar     # build/libs/match3.jar，可直接运行
./gradlew smokeTest  # 不启动界面的模型层检查
./gradlew dist       # Windows 安装包
```

`dist` 先把主 jar、依赖和 `resource/` 收进 `build/package-input`，用 `jlink` 裁一份运行时，
再由 `jpackage` 打成单个 `.exe`。裁出来的运行时保留
`java.base, java.desktop, java.logging, java.prefs, java.sql, jdk.localedata,
jdk.unsupported, jdk.zipfs` 加上 JavaFX 三个模块，启动器传 `--enable-native-access`
以加载 JavaFX 的本地库。

`dist` 在 Windows 上需要 [WiX](https://wixtoolset.org/)：

```shell
dotnet tool install --global wix
wix eula accept wix7
wix extension add --global WixToolset.Util.wixext
wix extension add --global WixToolset.UI.wixext
```

依赖全部来自 Maven Central，没有需要入库的 `lib/`。音频格式靠 SPI 发现，新增一种格式
就是往 `build.gradle.kts` 里加一个 provider jar。

## 持续集成

两个 workflow，`Release` 是唯一入口：

| Workflow | 触发 | 职责 |
|---|---|---|
| `release.yml` | 推送 main、或手动触发 | 解析版本号，调用 `Build`，创建 GitHub Release |
| `build.yml` | 仅 `workflow_call` | 出 jar；调用方要求时才出安装包 |

推送只构建并发布 jar。手动触发并勾选 `pack_exe`，才会一并构建 Windows 安装包。

版本号由提交数推出：Release 标签是 `r{1000 + 提交数}`，安装包内部版本是
`1.{提交数 / 100}.{提交数 % 100}`——第 234 个提交即 `r1234` 和 `1.2.34`。安装包版本必须是数字，
所以不能直接沿用标签。

## 国际化

界面语言在启动时按系统区域自动选择，都没命中就回退英文。`-Dmatch3.lang=<tag>` 可覆盖自动检测
（`zh-CN`、`ja`、`ru` 等，`zh_CN` 也认）。设置里可运行时切换，下拉框第一项是「自动」，
选它即重新跟随系统。

新增一门语言：

1. 复制 `resource/i18n/messages_en.properties` 为 `messages_<tag>.properties`，
   例如 `messages_de.properties`，翻译各条 value。键保持不变；value 里的 `\n` 会变成换行。
2. 在 `I18n.SUPPORTED`（`src/ui/I18n.java`）里注册该 Locale，它才会出现在设置的下拉框中。

bundle 按 UTF-8 读取。回退链是 `<语言>_<地区>` → `<语言>` → 英文，某条 key 缺失时从英文文件取。

## 目录结构

| 包 | 内容 |
|---|---|
| `model` | 棋盘、棋子、匹配规则、存档编解码 |
| `controller` | 游戏流程、动画、输入处理 |
| `ui` | 界面、主题、弹窗、i18n |
| `net` | 联机的创建房间与加入 |
| `config` | 设置、玩法模式、难度预设 |
| `player` | 音乐播放 |
| `listener` | 视图到控制器的回调 |
| `smoke` | `smokeTest` 跑的模型层检查，以及开发期用的界面探针 |
