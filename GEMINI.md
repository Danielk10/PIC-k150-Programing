# PIC k150 Programming

An Android professional application that allows programming PIC microcontrollers using the PIC k150 programmer directly from a mobile device using a USB OTG cable.

## 📋 Project Overview

- **Purpose**: Program PIC microcontrollers via Android using the PIC k150 programmer.
- **Protocol**: Implements the **Protocol P018** of **KITSRUS Programmer Firmware Protocol** (August 2004).
- **Core Library**: Uses `usb-serial-for-android` for reliable USB-Serial communication.
- **Target Platform**: Android (API 23 to 37).
- **Current Version**: 2.8.0 (Code 47).
- **Language**: Java 11.
- **Architecture**: Native Android application.

## 🏗️ Architecture & Technologies

- **Android SDK**: Target SDK 37, Minimum SDK 23.
- **Languages**: Java 11.
- **Build System**: Gradle 8.x (v9.5.1 wrapper).
- **USB Communication**: Android USB Host API + `usb-serial-for-android`.
- **Versions**: API 37.0, Build Tools 37.0.0, NDK 30.0.14904198 (rc1).
- **Services**:
    - **Firebase**: Crashlytics, Analytics and Cloud Messaging.
    - **App Center**: Analytics and Crashes (v5.0.6).
    - **Google AdMob**: Monetization and Mediation.
- **Supported Architectures**: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`.

## 🚀 Building and Running

### Prerequisites
- Java 11 JDK.
- Android SDK & NDK (can be configured using `setup-sdk.sh`).
- **Note**: The SDK is configured by default in `/tmp/android-sdk` to avoid space issues in the home directory.

### Setup
Run the included script to configure the Android SDK and NDK environment:
```bash
./setup-sdk.sh
```

### Build Commands
- **Assemble Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Install Debug APK**:
  ```bash
  ./gradlew installDebug
  ```
- **Run Tests**:
  ```bash
  ./gradlew test
  ```

### GitHub Releases (Prerelease)
To create a new pre-release on GitHub:
```bash
gh release create v0.1.0-beta app/build/outputs/apk/debug/app-debug.apk --title "Versión Alfa 0.1.0" --notes "Primera compilación de prueba del juego." --prerelease
```
*Note: Always increment the version tag and title before running.*

## 📂 Project Structure

- `app/src/main/java/com/diamon/pic/`:
    - `MainActivity.java`: Contains the core application logic, UI handling, and programming protocol implementation.
    - `PicApplication.java`: Custom application class for initialization (Firebase, App Center, etc.).
- `app/src/main/assets/`:
    - `chipinfo.cid`: Comprehensive database of PIC microcontrollers and their programming specifications.
    - `softprotocol5.txt`: Internal protocol definitions.
    - `tutorial_*.md/txt`: User tutorials for SDCC and GPUTILS in multiple languages.
- `app/src/main/res/`: Android resources (layouts, drawables, values).
- `setup-sdk.sh`: Script to automate Android development environment setup.

## 🔧 Development Conventions

- **Coding Style**: Standard Android/Java coding conventions.
- **UI**: Material Design components.
- **Compatibility**: Ensure changes maintain compatibility with API Level 23 (Android 6.0).
- **Testing**: Unit tests are located in `app/src/test/`, and instrumentation tests in `app/src/androidTest/`.

## 📝 Key Documentation Files

- `README.md`: Comprehensive project documentation, feature list, and developer guide.
- `guia_uso_sdk.md`: Reference guide for using Android SDK command-line tools (`sdkmanager`, `adb`, `avdmanager`).
- `LICENSE.txt`: GPL-3.0 License details.
- `resumen_instalacion.md`: Brief installation notes.
- `AGENT_INSTRUCTIONS.md`: Mandatory workflow guidelines for AI agents working on this project.
