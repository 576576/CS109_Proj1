# Match-3

A match-3 puzzle game with a Material 3 JavaFX interface, online play over TCP, and a
colour scheme derived from the current wallpaper. Originally a CS109 course project.

## Features

- **Three ways to play**: single-player, host a room, or join a room by address.
- **Four difficulties**: easy / normal / hard, plus a custom one where the goal score,
  move limit and time limit are editable.
- **Save & load**: a running game can be written to a file and resumed later.
- **Auto mode**: auto-go plays moves on its own, auto-confirm settles the board
  without asking.
- **Music**: FLAC and MP3 playback through the `javax.sound.sampled` SPI.
- **Theming**: MonetFX derives the Material 3 palette from the background image, so
  changing the wallpaper recolours the whole UI. Light and dark are switchable.
- **Seven UI languages**, detected from the system locale and switchable at runtime.

## Requirements

A JDK 26 toolchain — the version is pinned by the Gradle toolchain block in
`build.gradle.kts`. Use the wrapper; it downloads Gradle itself.

The UI is built on JavaFX with [MaterialFX](https://github.com/palexdev/MaterialFX)
and [MonetFX](https://github.com/Glavo/MonetFX).

## Build

```shell
./gradlew run        # launch the app
./gradlew fatJar     # build/libs/match3.jar, runnable on its own
./gradlew smokeTest  # headless model checks
./gradlew dist       # Windows installer
```

`dist` collects the main jar, the dependencies and `resource/` into
`build/package-input`, trims a runtime image with `jlink`, then runs `jpackage` to
produce a single `.exe`. The trimmed runtime keeps
`java.base, java.desktop, java.logging, java.prefs, java.sql, jdk.localedata,
jdk.unsupported, jdk.zipfs` plus the JavaFX modules, and the launcher passes
`--enable-native-access` so JavaFX's native libraries load.

`dist` needs [WiX](https://wixtoolset.org/) on Windows:

```shell
dotnet tool install --global wix
wix eula accept wix7
wix extension add --global WixToolset.Util.wixext
wix extension add --global WixToolset.UI.wixext
```

All dependencies come from Maven Central — there is no `lib/` to check in. Audio
formats are discovered through the SPI, so adding one is a matter of adding its
provider jar to `build.gradle.kts`.

## Continuous integration

Two workflows, with `Release` as the only entry point:

| Workflow | Trigger | Job |
|---|---|---|
| `release.yml` | push to `main`, or manual dispatch | resolves the version, calls `Build`, creates the GitHub release |
| `build.yml` | `workflow_call` only | `jar`, and `exe` when the caller asks for it |

A push builds and publishes the jar only. Running the workflow by hand with
`pack_exe` enabled also builds the Windows installer.

Versions come from the commit count: the release tag is `r{1000 + commits}`, and the
installer's internal version is `1.{commits / 100}.{commits % 100}` — commit 234
becomes `r1234` and `1.2.34`. The installer version must be numeric, which is why it
cannot reuse the tag.

## Internationalization

The UI language is detected from the system locale at startup and falls back to
English when nothing matches. `-Dmatch3.lang=<tag>` overrides the detection
(`zh-CN`, `ja`, `ru`, ...; `zh_CN` is also accepted). Settings switches languages at
runtime, and its dropdown starts with an "auto" entry that follows the system again.

To add a language:

1. Copy `resource/i18n/messages_en.properties` to `messages_<tag>.properties` — for
   example `messages_de.properties` — and translate the values. Keep the keys; `\n`
   inside a value becomes a line break.
2. Register the locale in `I18n.SUPPORTED` (`src/ui/I18n.java`) so it shows up in the
   Settings dropdown.

Bundles are read as UTF-8. The fallback chain is `<language>_<region>` →
`<language>` → English, and any key missing from a bundle is taken from the English
file.

## Layout

| Package | Contents |
|---|---|
| `model` | board, pieces, match rules, save codec |
| `controller` | game flow, animations, input handling |
| `ui` | screens, theming, dialogs, i18n |
| `net` | host/join networking |
| `config` | settings, play modes, difficulty presets |
| `player` | music playback |
| `listener` | view-to-controller callbacks |
| `smoke` | model checks run by `smokeTest`, plus development-time UI probes |
