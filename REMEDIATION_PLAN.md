# Remediation Plan: Security & Compatibility Modernization

This remediation plan details the step-by-step actions required to secure passcode verification and storage, fix runtime service crashes on newer Android APIs, upgrade deprecated dependencies, and resolve state serialization bugs in **AppLockFlutter**.

---

## 1. Upgrades & Build Configuration Changes
To ensure compatibility with modern Android OS versions and newer Dart/Flutter runtimes:

*   **Dart SDK Target**: Update `sdk` in `pubspec.yaml` to `"sdk: >=3.0.0 <4.0.0"`.
*   **Android 14 Foreground Service Compliance**: Add `android:foregroundServiceType="specialUse"` to `.ForegroundService` inside `AndroidManifest.xml` to prevent OS startup crashes.
*   **Permissions Audit**: Delete unnecessary Bluetooth permission tags (`BLUETOOTH`, `BLUETOOTH_CONNECT`) in `AndroidManifest.xml` to follow security best practices.

---

## 2. Secure Passcode Architecture (Remediation)
The current plaintext passcode storage will be refactored using cryptographic security:

```
[Plaintext PIN] ──> [Generate Random Salt] ──> [Hash with SHA-256] ──> [Store Hash & Salt]
```

1.  **Secure Storage (Flutter)**:
    *   Add the `flutter_secure_storage` package to `pubspec.yaml`.
    *   Update [password_controller.dart](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/lib/executables/controllers/password_controller.dart) to write the passcode to secure storage rather than standard SharedPreferences.
2.  **Encrypted Storage (Android Native)**:
    *   In [MainActivity.kt](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/android/app/src/main/kotlin/com/applockFlutter/MainActivity.kt) and [Window.kt](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/android/app/src/main/java/com/appmanager/etherium/switch_up/Window.kt), read/write preferences using `EncryptedSharedPreferences` to ensure hardware-backed file encryption.
3.  **Cryptographic Hashing**:
    *   Convert passcode storage to store a **SHA-256 hash** and a unique **salt** instead of the plaintext PIN.
    *   Perform comparisons by hashing inputs with the salt and comparing hashes.
4.  **Brute-Force Rate Limiting**:
    *   Add attempt counters in [Window.kt](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/android/app/src/main/java/com/appmanager/etherium/switch_up/Window.kt). Lock keypad inputs for 30 seconds after 5 failed passcode inputs.

---

## 3. Background Services & Serialization Bug Fixes

1.  **BootUp Receiver Crash Fix**:
    *   In [BootUpReceiver.kt](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/android/app/src/main/java/com/appmanager/etherium/switch_up/BootUpReceiver.kt), replace `context.startService(...)` with `ContextCompat.startForegroundService(context, intent)` to prevent background service launch failures on Android 8.0+.
2.  **App Icon Serialization Bug Fix**:
    *   Rewrite `toJson()` and `fromJson()` for `ApplicationData` in [application_model.dart](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/lib/models/application_model.dart) to use direct base64 codecs (`base64Encode` and `base64Decode`) instead of string conversion.

---

## 4. Flutter API Modernization

1.  **WillPopScope Deprecation**:
    *   Replace `WillPopScope` with `PopScope` in `unlocked_apps.dart` and `set_passcode.dart`.
2.  **Theme and Typography Deprecations**:
    *   Migrate theme configurations in `themes.dart` to modern Material 3 styling tokens (`bodyLarge`, `bodyMedium`, `titleMedium`).
3.  **Package Migration**:
    *   Migrate custom application fetching logic from `device_apps` to `installed_apps` to leverage a maintained dependency.

---

## 5. Dead Code Cleanup
Remove the following unused assets to clean up decompiled source layout:
*   [extra_methods.dart](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/lib/services/extra_methods.dart) (duplicates constants)
*   [PinCodeActivity.kt](file:///C:/Users/mohit/OneDrive/Desktop/AppLockFlutter/android/app/src/main/java/com/appmanager/etherium/switch_up/PinCodeActivity.kt) (broken and unused class)
*   `NativeActivity.kt` & `activity_native.xml` (unused native templates)

---

## 6. Verification and Acceptance
*   Compile on Flutter 3.x with zero compile errors.
*   Validate overlay window performance on Android emulator instances (API level 26 to 34).
*   Verify that preference storage files do not contain cleartext passcode entries.
