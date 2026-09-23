# Match-3

A match-3 game in JavaFX with a Material 3 interface, online play, and a colour scheme
that follows the wallpaper. Originally a CS109 course project.

[中文](../../README.md)

## Playing

Single-player / host a room / join a room; four difficulties (custom edits the goal
score, move limit and time limit); save & load; auto-go and auto-confirm; music;
light and dark; seven UI languages.

## Build

Needs JDK 26 (pinned by the toolchain block in `build.gradle.kts`). Use the wrapper; it
downloads Gradle itself.

```shell
./gradlew run        # launch
./gradlew fatJar     # build/libs/match3.jar, runnable on its own
./gradlew smokeTest  # model checks
./gradlew dist       # Windows installer
```

The UI is JavaFX + [MaterialFX](https://github.com/palexdev/MaterialFX) +
[MonetFX](https://github.com/Glavo/MonetFX); dependencies come from Maven Central.
`dist` also needs [WiX](https://wixtoolset.org/) on Windows (from v7 on, run
`wix eula accept wix7` first).

## Releases

A push to main publishes the jar. Running the Release workflow by hand with `pack_exe`
also builds the installer. The tag is `r{1000 + commits}`; the installer's internal
version is `1.{commits / 100}.{commits % 100}`.

## Language

The UI language follows the system locale and can be switched in Settings.
`-Dmatch3.lang=<tag>` overrides the detection. Strings live in
`resource/i18n/messages_<tag>.properties` (UTF-8); to add a language, copy the English
file, translate it, and register the locale in `I18n.SUPPORTED`.
