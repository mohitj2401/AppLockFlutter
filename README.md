# App Lock

A robust, high-performance App Lock application built with Flutter for Android. This project leverages native Android background services and method channels to provide secure application protection.

## 🚀 Key Features
- **Modern Android Support**: Fully compatible with Android 16 (API 36).
- **Biometric / Fingerprint Unlock**: Securely unlock protected applications using your device's fingerprint or biometric credentials, with custom transparent activity hosting and passcode fallback.
- **Native Background Service**: Highly optimized `ForegroundService` written in Kotlin for real-time app monitoring.
- **Persistence**: Enhanced background stability using `START_STICKY` and `onTaskRemoved` handling to ensure the lock works even after the app is cleared from Recent Apps.
- **Smart Security**: Automatically locks sensitive device pages (like Settings, Admin, and Permissions) if the device's main Settings app is protected.
- **Enhanced UI**: Modernized search interfaces, crisp UI elements, and intuitive drawer-based settings.
- **Landscape Handling**: Custom orientation locking ensures the PIN screen remains portrait and easily usable regardless of the underlying app's rotation.
- **Security**: 
  - Encrypted storage for passcodes using `EncryptedSharedPreferences`.
  - Android 14+ `RECEIVER_EXPORTED` security compliance.
  - Native Usage Access monitoring for real-time detection.
- **State Management**: Uses `GetX` for clean and reactive UI updates.
- **Battery Optimization**: Built-in support for requesting Battery Optimization exemption to prevent the OS from killing the background protection service.
- **Uninstall Protection**: Optional **Device Administrator** integration that prevents the app from being uninstalled by the user or other apps while active.

## 🛠 Tech Stack
- **Flutter**: UI and high-level logic.
- **Kotlin**: Native Android services, device monitoring, and Device Admin.
- **Method Channels**: Seamless communication between Flutter and Native layers.
- **Gradle 8.14 & AGP 8.11.1**: Latest build tools for performance and security.
- **NDK 28**: Support for 16KB memory page sizes required by newer Android hardware.

## 📱 Permissions Required
- **Overlay Permission**: To show the lock screen on top of other apps.
- **Usage Stats Access**: To monitor which app is currently in the foreground.
- **Notification Access**: For persistent foreground service notifications.
- **Ignore Battery Optimizations**: To ensure 24/7 protection.
- **Device Administrator**: (Optional) To prevent app uninstallation.

## 📸 Screenshots
<img src="https://user-images.githubusercontent.com/56929825/210130743-d30f5155-3b4e-4d49-9c73-718f20901592.jpg" width="150" height="320"> <img src="https://user-images.githubusercontent.com/56929825/210130744-7f2e208a-5447-49c8-bc35-1c6ccfe48a48.jpg" width="150" height="320"> 
<img src="https://user-images.githubusercontent.com/56929825/210130749-59f770c6-2304-489b-b56f-3533b8baa39d.jpg" width="150" height="320">
<img src="https://user-images.githubusercontent.com/56929825/210130753-61431b86-5d3a-4909-88c9-0ebb16e2b1cf.jpg" width="150" height="320">
<img src="https://user-images.githubusercontent.com/56929825/210130756-4498aabd-cf84-4bc6-a777-64323f732d33.jpg" width="150" height="320">

## 🎥 Demo
https://user-images.githubusercontent.com/56929825/210130784-6cc095a8-af96-4ce6-a5db-025b91e4e45c.mp4
