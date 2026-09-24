<div align="center">
<img src=".idea/icon.svg" width="120" alt="Time Gem Logo">

# Time Gem
De-clutter your mind, structure your day, FOSS style!
</div>

Time Gem is an open-source Android app for notes and, over time, planning your day. It uses Jetpack Compose and Material 3.

## Development demo: 0.1.0-demo

This is an early, usable text-note demo. The broader life-management tools are still in development.

Available now:

- Create, edit, search and delete text notes stored on your device.
- Save when leaving a note, or enable a save confirmation in Settings. Empty drafts are ignored. Clearing a saved note does not delete its previous version; use Delete to remove it.
- Switch between a note grid and a list, with a navigation rail on wider screens.
- Choose Time Gem colors, wallpaper-based Material You colors on Android 12+, or a preset palette. Follow device brightness or choose light/dark mode.
- Customize visible workspaces, the themed search logo and subtle haptics.
- First-run setup and offline Google Sans Flex typography.

Not implemented yet: checklists, drawing, reminders, calendar events, weekly planning, routines, sleep/focus tools, reflections, sync and export. Their current entries describe the planned features.

There are no accounts or analytics. Notes and preferences use app storage; Android system backup is currently enabled and follows device backup settings. This demo does not provide its own sync or backup service.

## Build

Requires Android 8.0 (API 26) or newer. Open the project in Android Studio and let Gradle sync. On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Use Android Studio's Run button or install it with `adb install -r` to preserve existing app data.

## Tests

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.dragonpi.timegem.test/androidx.test.runner.AndroidJUnitRunner
```

The instrumentation tests cover note persistence, updates/deletion, setup layout, saving on exit, empty drafts, optional confirmation, cancelled back gestures, save retries and repeat-tap protection. Repository tests use a separate test database.

## License

Time Gem is licensed under [GPL-3.0](LICENSE).

Google Sans Flex is bundled under the [SIL Open Font License 1.1](app/src/main/assets/licenses/google_sans_flex_ofl.txt). Its [trademark notice](app/src/main/assets/licenses/google_sans_flex_trademarks.md) is included. The original font comes from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/googlesansflex).
