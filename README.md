# Android NFC Reader

A simple Android application that reads NFC tags and displays their contents on-screen.

## Features

- Reads **NDEF-formatted tags** and displays each record's type and payload
- Reads **raw (non-NDEF) tags** and shows tag ID and technology information
- Supports **NFC-A, NFC-B, NFC-F, NFC-V, ISO-DEP, MIFARE Classic, and MIFARE Ultralight**
- Uses **foreground dispatch** so the app always intercepts tags while open
- Gracefully handles devices without NFC hardware

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1.1+ |
| Gradle | 8.4 |
| Android Gradle Plugin | 8.2.2 |
| Kotlin | 1.9.22 |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 34 (Android 14) |

## Building

1. Clone the repository.
2. Open the project in **Android Studio**.
3. Let Gradle sync finish automatically.
4. Connect an Android device (or use an emulator with NFC support).
5. Click **Run ▶** or execute:

```bash
./gradlew assembleDebug
```

The resulting APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Deploying

### Debug (sideload)
```bash
./gradlew installDebug
```

### Release (Google Play)
1. Generate a signing keystore:
   ```bash
   keytool -genkey -v -keystore nfc-reader.jks -alias nfc-reader -keyalg RSA -keysize 2048 -validity 10000
   ```
2. In `app/build.gradle`, add a `signingConfigs` block referencing the keystore.
3. Build:
   ```bash
   ./gradlew assembleRelease
   ```
4. Upload `app/build/outputs/apk/release/app-release.apk` to the Google Play Console.

## Permissions

| Permission | Reason |
|-----------|--------|
| `android.permission.NFC` | Required to read NFC tags |
| `android.hardware.nfc` | Declared as required hardware feature |

## License

MIT
