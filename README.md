# Medication Log

Medication Log is an offline Android app for organizing medication schedules and keeping a personal record of actual use. You can set reminders, explicitly record or skip a scheduled dose, undo a skip, add an independent record, review history, and view totals based on saved records. A reminder does not mean a dose was taken.

- Application ID: `cn.medreminder.app`
- Android 8.0 (API 26) or newer; target SDK 35
- Works without Google Mobile Services
- Schedules and records are stored locally on the device

The app uses English as its baseline language. Chinese is selected when the Android system language is Chinese. Dose formats the app does not recognize remain in the saved record but are excluded from numeric totals. The app is a record-keeping tool and does not provide medical advice. Enter medication and dose details as directed by your healthcare professional. Android notification and exact alarm settings can affect delivery. Background behavior depends on the device and manufacturer and has not been verified on every device.

## Build

Install Android SDK platform 35, build-tools 35.0.0, and JDK 21, then run `./build.sh`. The default output is an unsigned developer APK at `dist/yongyaoji-debug-unsigned.apk`; it cannot be installed until it has been signed. A third-party release build must be signed with a keystore you control by setting `SIGNING_KEYSTORE`, `KEYSTORE_PASSWORD`, and `KEY_PASSWORD` in the environment. The official update signing key is not included. An APK signed with a different key cannot update an installation signed with the official key.

The project includes the MIT license in `LICENSE` and packages a copy in the app at `res/raw/license_mit.txt`.
