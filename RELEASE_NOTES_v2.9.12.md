# PIC k150 Programming v2.9.12 (versionCode 66) — Pase Pro de 15 minutos

Fecha de lanzamiento / Release Date: 24 de noviembre de 2026 / November 24, 2026

> **Publicado / Published:** compilado y firmado con el SDK de Android. Publicado en la pista de producción de Google Play Store.

---

## 🇪🇸 Español

### 📌 Resumen
El Pase Pro temporal pasa de 12 horas a 15 minutos. Los pases que ya estaban activos conservan su vencimiento original: actualizar la app no recorta el tiempo restante. La activación usa `max(vencimiento actual, ahora + 15 min)`, así que si quedan menos de 15 minutos el siguiente video reinicia el contador a 15; si quedan más (por ejemplo, un pase heredado de 12 h), se conserva. Firestore registra el campo `app: "pic"` para distinguir el origen de cada pase.

### 🛠️ Cambios
• **Duración de 15 minutos:** `DURACION_PASE_MS = 15 * 60 * 1000L`.
• **Activación sin recortes:** `activarPasePro()` conserva el vencimiento mayor entre el pase actual y `ahora + 15 min`.
• **Compatibilidad:** `activarPase12Horas()` se mantiene como envoltorio obsoleto para no romper llamadas antiguas.
• **Firestore:** los pases nuevos guardan `app: "pic"` en la colección compartida `pro_passes`.
• **Traducciones:** todas las cadenas del botón y del aviso del Pase Pro se actualizaron de 12 horas a 15 minutos.
• **Pruebas:** `ProPassManagerTest` actualizado a la nueva duración y a `activarPasePro()`.

### ✅ Validación
• `:app:assembleRelease` y `:app:bundleRelease` correctos con `compileSdk 37`, `buildTools 37.0.0`, `targetSdk 37` y `minSdk 24`.
• APK y AAB firmados con `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 66` y `versionName 2.9.12`; ABIs `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`.
• Pruebas unitarias `ProPassManagerTest`: correctas.
• AAB publicado en Google Play, track `production`, estado `completed` (100 %).
• Sin cambios en reglas de Firestore ni en documentos existentes.

### 📦 Artefactos
• `PIC-k150-Programing-v2.9.12-release.apk` — 15949271 bytes
  • SHA-256: `7f5714253bddc8aecb2e0487b10379f19a2c775b8e037fe9594e8e80e26e16a3`
• `PIC-k150-Programing-v2.9.12-release.aab` — 16200896 bytes
  • SHA-256: `c12fa88ed84714aac2af009121e8208a7a612a90e717489a2fbba9bab4ade513`

---

## 🇺🇸 English

### 📌 Overview
The temporary Pro Pass changes from 12 hours to 15 minutes. Passes that were already active keep their original expiry: updating the app does not shorten the remaining time. Activation uses `max(current expiry, now + 15 min)`, so if less than 15 minutes remain, the next video resets the counter to 15; if more time remains (for example, a legacy 12-hour pass), it is preserved. Firestore records the `app: "pic"` field to identify the source of each pass.

### 🛠️ Changes
• **15-minute duration:** `DURACION_PASE_MS = 15 * 60 * 1000L`.
• **Non-shortening activation:** `activarPasePro()` keeps the later expiry between the current pass and `now + 15 min`.
• **Compatibility:** `activarPase12Horas()` remains as a deprecated wrapper so old calls keep working.
• **Firestore:** new passes store `app: "pic"` in the shared `pro_passes` collection.
• **Translations:** every Pro Pass button and toast string was updated from 12 hours to 15 minutes.
• **Tests:** `ProPassManagerTest` updated for the new duration and `activarPasePro()`.

### ✅ Validation
• `:app:assembleRelease` and `:app:bundleRelease` succeed with `compileSdk 37`, `buildTools 37.0.0`, `targetSdk 37` and `minSdk 24`.
• APK and AAB signed with `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 66` and `versionName 2.9.12`; ABIs `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`.
• `ProPassManagerTest` unit tests: passing.
• AAB published to Google Play, `production` track, `completed` status (100%).
• No changes to Firestore rules or existing documents.

### 📦 Artifacts
• `PIC-k150-Programing-v2.9.12-release.apk` — 15949271 bytes
  • SHA-256: `7f5714253bddc8aecb2e0487b10379f19a2c775b8e037fe9594e8e80e26e16a3`
• `PIC-k150-Programing-v2.9.12-release.aab` — 16200896 bytes
  • SHA-256: `c12fa88ed84714aac2af009121e8208a7a612a90e717489a2fbba9bab4ade513`
