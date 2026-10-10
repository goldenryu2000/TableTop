# Tabletop

Curated mini games for Android. Ad-free, offline, open source.

Download the APK from [Releases](https://github.com/goldenryu2000/TableTop/releases).

## Games

- **Klondike**: classic solitaire with draw 1 or 3, unlimited undo, hints and auto-finish.

## Build

Requires JDK 21 and the Android SDK.

    ./gradlew assembleDebug

Release builds are signed when `keystore.properties` exists (see `app/build.gradle.kts`).

    ./gradlew assembleRelease

## Releasing

Pushing a `v*` tag builds a signed APK and attaches it to the GitHub Release. It needs these repo secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## License

GPL-3.0
