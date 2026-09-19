# 消消乐 (Match-3)
这是一份CS109课程项目，目的为实现消消乐游戏。

为了方便我的国际合作者Grigory，以下说明为英文。
## Basic game system
- Initialize the game
- Item swap & match
- save & load from files
## GUI design
- window adaptation *part*
- scoreboard
- art design *done — MaterialFX + MonetFX, project icon*
- darkMode (or allow skin custom?) *part*
## Bonus part
- music
- network game *576-planned*
- autoMode(autoGo&autoConfirm)
- ...

## Build

Requires a JDK 26 toolchain. Use the wrapper; it fetches Gradle itself.

The UI is built on JavaFX 26 with [MaterialFX](https://github.com/palexdev/MaterialFX)
and [MonetFX](https://github.com/Glavo/MonetFX) (the latter derives the dynamic
Material-3 colour scheme from the background image).

```shell
./gradlew fatJar      # build/libs/match3.jar, runnable on its own
./gradlew smokeTest   # pure-main smoke checks
./gradlew dist        # Windows installer: packageInput + trimmed runtime + jpackage
```

`dist` lays the app out under `build/package-input`, trims a runtime image that
includes the JavaFX modules, then runs `jpackage` to produce a single `.exe`.

CI (`.github/workflows/build.yml`) publishes two artifacts:

- `match3-jar` — the fat jar.
- `match3-exe` — a Windows installer built by `jpackage` on top of a `jlink`
  runtime trimmed to
  `java.base,java.desktop,java.logging,java.prefs,java.sql,jdk.localedata,jdk.unsupported,jdk.zipfs`
  plus the JavaFX modules
  `javafx.base,javafx.graphics,javafx.controls,javafx.fxml`
  (~60 MB instead of a full JDK). The launcher passes
  `--enable-native-access=ALL-UNNAMED` so JavaFX's native code loads.

Dependencies (jflac, mp3spi, tritonus-share, jlayer, JavaFX, MaterialFX,
MonetFX) come from Maven Central — there is no `lib/` to check in. Audio
formats are discovered through the `javax.sound.sampled` SPI, so supporting
another format is a matter of adding one provider jar to `build.gradle.kts`.

## i18n

The UI language is auto-detected from the system locale at startup and falls
back to English when nothing matches. `-Dmatch3.lang=<tag>` overrides the
detection (`zh-CN`, `ja`, `ru`, ... — `zh_CN` also accepted). All strings live
in `resource/i18n/messages_<tag>.properties` (UTF-8); the language can be
switched at runtime from Settings, whose dropdown starts with an "auto"
entry that goes back to following the system.

Adding a language:

1. Copy `resource/i18n/messages_en.properties` to
   `messages_<tag>.properties` (e.g. `messages_de.properties`) and translate
   the values. Keep the keys; `\n` inside a value becomes a line break.
2. Register the locale in `I18n.SUPPORTED` (`src/ui/I18n.java`) so it appears
   in the Settings dropdown.
3. Done — the fallback chain is `<language>_<region>` → `<language>` →
   English, and any key missing from a bundle is read from the English file.
