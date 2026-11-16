# PIC k150 Programming v2.9.9 (versionCode 63) — Actualización de dependencias / Dependency update

Fecha de lanzamiento / Release Date: 15 de noviembre de 2026 / November 15, 2026

> **Sin cambios de código.** Esta versión solo publica las actualizaciones de
> dependencias y la limpieza de documentación que ya estaban en `main` desde el
> 11 de noviembre, para que Google Play y GitHub queden alineados con el
> repositorio.

---

## 🇪🇸 Español

### 📌 Resumen
Versión de mantenimiento: no se modifica ninguna funcionalidad de la app. Se
actualizan las herramientas de compilación y las librerías para recibir
correcciones de estabilidad y compatibilidad, y se elimina una guía de
consentimiento que ya estaba obsoleta.

### 🛠️ Cambios
• **Android Gradle Plugin:** 9.4.1 → 9.5.1.
• **Gradle:** 9.6.0 → 9.9.1.
• **Firebase BOM:** 35.0.2 → 35.2.0.
• **Firebase Crashlytics:** 21.0.0 → 21.1.0.
• **Google Services:** 4.5.0 → 4.6.0.
• **usb-serial-for-android:** 3.11.0 → 3.12.0.
• **Documentación:** se elimina `CONSENTIMIENTO_PRIVACIDAD.md`, obsoleto desde
  que el consentimiento UMP quedó integrado en la app.
• **Sin cambios funcionales:** el compilador, el grabador PIC, la terminal, el
  visor de memoria, la publicidad (banner, nativo, rewarded) y el pase Pro
  siguen exactamente igual.

### ✅ Validación
• `:app:bundleRelease` y `:app:assembleRelease` correctos con `compileSdk 37`,
  `buildTools 37.0.0` y `targetSdk 37`.
• APK y AAB firmados con `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 63` y `versionName 2.9.9`; ABIs `arm64-v8a`, `armeabi-v7a`,
  `x86` y `x86_64`.

### 📦 Artefactos
• `PIC-k150-Programing-v2.9.9-release.apk` — 15945867 bytes
  • SHA-256: `b50a8b33cc4848583866795e3ab629e17ca2df51a2d984276ed7f482d5d3dffb`
• El AAB se publicó en Google Play Console; no se adjunta a este release.

---

## 🇺🇸 English

### 📌 Overview
Maintenance release: no app functionality is changed. Build tools and libraries
are updated to pick up stability and compatibility fixes, and an obsolete
consent guide is removed.

### 🛠️ Changes
• **Android Gradle Plugin:** 9.4.1 → 9.5.1.
• **Gradle:** 9.6.0 → 9.9.1.
• **Firebase BOM:** 35.0.2 → 35.2.0.
• **Firebase Crashlytics:** 21.0.0 → 21.1.0.
• **Google Services:** 4.5.0 → 4.6.0.
• **usb-serial-for-android:** 3.11.0 → 3.12.0.
• **Documentation:** `CONSENTIMIENTO_PRIVACIDAD.md` removed; it had been
  obsolete since UMP consent was integrated into the app.
• **No functional changes:** the compiler, PIC programmer, terminal, memory
  viewer, ads (banner, native, rewarded) and Pro pass remain exactly the same.

### ✅ Validation
• `:app:bundleRelease` and `:app:assembleRelease` succeeded with
  `compileSdk 37`, `buildTools 37.0.0` and `targetSdk 37`.
• APK and AAB signed with `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 63` and `versionName 2.9.9`; ABIs `arm64-v8a`, `armeabi-v7a`,
  `x86` and `x86_64`.

### 📦 Artifacts
• `PIC-k150-Programing-v2.9.9-release.apk` — 15945867 bytes
  • SHA-256: `b50a8b33cc4848583866795e3ab629e17ca2df51a2d984276ed7f482d5d3dffb`
• The AAB was published to Google Play Console (not attached to this release).

---

## 🔗 Enlaces / Links
• **Repositorio / Repository:** https://github.com/Danielk10/PIC-k150-Programing
• **Release:** https://github.com/Danielk10/PIC-k150-Programing/releases/tag/v2.9.9
