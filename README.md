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
- art design *not done*
- darkMode (or allow skin custom?) *part*
## Bonus part
- music
- network game *576-planned*
- autoMode(autoGo&autoConfirm)
- ...

## Build

Requires a JDK 27 toolchain. Use the wrapper; it fetches Gradle itself.

```shell
./gradlew fatJar          # build/libs/match3.jar, runnable on its own
./gradlew smokeTest       # pure-main smoke checks
./gradlew packageInput    # build/package-input, laid out for jpackage
```

Dependencies (jflac, mp3spi, tritonus-share, jlayer) come from Maven Central —
there is no `lib/` to check in. Audio formats are discovered through the
`javax.sound.sampled` SPI, so supporting another format is a matter of adding
one provider jar to `build.gradle.kts`.
