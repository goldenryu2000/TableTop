# Tabletop

Curated mini games for Android. Ad-free, offline, open source.

## Games

- **Klondike**: classic solitaire with draw 1 or 3, unlimited undo, hints and auto-finish.

## Build

Requires JDK 21 and the Android SDK.

    ./gradlew assembleDebug

Release builds are signed when `keystore.properties` exists (see `app/build.gradle.kts`).

    ./gradlew assembleRelease

## License

GPL-3.0
