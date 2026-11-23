# PIC k150 Programming v2.9.10 (versionCode 64) — Anuncio nativo en la pantalla principal

Fecha de lanzamiento / Release Date: 23 de noviembre de 2026 / November 23, 2026

## 🇪🇸 Español

### 📌 Resumen
Se traslada el anuncio nativo del visor HEX a la pantalla principal, con un
formato compacto integrado entre los botones de operación y el registro.

### 🛠️ Cambios
• **Anuncio nativo en la pantalla principal:** nueva tarjeta compacta con
  icono, distintivo «Anuncio», titular, texto, anunciante y botón de acción.
• **Visor HEX limpio:** se elimina el contenedor y la llamada al anuncio nativo
  del visor de memoria.
• **Sin relleno, sin hueco:** la tarjeta queda oculta si no hay anuncio, si los
  anuncios están desactivados o si el consentimiento UMP aún no se resolvió.
• **Sin cambios** en compilador, grabador PIC, terminal, visor de memoria ni
  pase Pro.

### ✅ Validación
• `:app:bundleRelease` correcto con `compileSdk 37`, `buildTools 37.0.0`,
  `targetSdk 37` y `minSdk 24`.
• AAB firmado con `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 64` y `versionName 2.9.10`; ABIs `arm64-v8a`, `armeabi-v7a`,
  `x86` y `x86_64`.

### 📦 Artefacto
• `app-release.aab` — 16200942 bytes
• SHA-256: `f7d3aa986c6d7780df0c65872f9a4bf304eb9afe222e46a7a731a84280f56ad0`
• Publicado en Google Play, track `production`, estado `completed` (100 %).

---

## 🇺🇸 English

### 📌 Overview
The native ad moves from the HEX viewer to the main screen as a compact card
placed between the operation buttons and the log.

### 🛠️ Changes
• **Native ad on the main screen:** compact card with icon, “Ad” badge,
  headline, body, advertiser and call-to-action button.
• **Cleaner HEX viewer:** the native ad container and call are removed from the
  memory viewer.
• **No fill, no gap:** the card stays hidden when there is no ad, when ads are
  disabled, or when UMP consent is not resolved yet.
• **No changes** to the compiler, PIC programmer, terminal, memory viewer or
  Pro pass.

### ✅ Validation
• `:app:bundleRelease` succeeds with `compileSdk 37`, `buildTools 37.0.0`,
  `targetSdk 37` and `minSdk 24`.
• AAB signed with `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 64` and `versionName 2.9.10`; ABIs `arm64-v8a`, `armeabi-v7a`,
  `x86` and `x86_64`.
