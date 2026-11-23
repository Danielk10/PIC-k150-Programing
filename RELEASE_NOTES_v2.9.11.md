# PIC k150 Programming v2.9.11 (versionCode 65) — Reubicación del anuncio nativo

Fecha de lanzamiento / Release Date: 23 de noviembre de 2026 / November 23, 2026

## 🇪🇸 Español

### 📌 Resumen
El anuncio nativo de la pantalla principal sube de posición: ahora aparece
justo debajo de «Estado de fusibles» y «Modo ICSP», antes de los botones de
operación, en lugar de quedar pegado al registro.

### 🛠️ Cambios
• **Nueva posición:** tarjeta nativa compacta entre la fila de fusibles/ICSP y
  el bloque de operaciones.
• **Mismo formato:** icono, distintivo «Anuncio», titular, texto, anunciante y
  botón de acción.
• **Sin relleno, sin hueco:** la tarjeta queda oculta si no hay anuncio, si los
  anuncios están desactivados o si el consentimiento UMP aún no se resolvió.
• **Sin cambios** en compilador, grabador PIC, terminal, visor de memoria ni
  pase Pro.

### ✅ Validación
• `:app:bundleRelease` correcto con `compileSdk 37`, `buildTools 37.0.0`,
  `targetSdk 37` y `minSdk 24`.
• AAB firmado con `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 65` y `versionName 2.9.11`; ABIs `arm64-v8a`, `armeabi-v7a`,
  `x86` y `x86_64`.

---

## 🇺🇸 English

### 📌 Overview
The main-screen native ad moves up: it now sits right below “Fuse status” and
“ICSP mode”, above the operation buttons, instead of next to the log.

### 🛠️ Changes
• **New position:** compact native card between the fuse/ICSP row and the
  operations block.
• **Same format:** icon, “Ad” badge, headline, body, advertiser and
  call-to-action button.
• **No fill, no gap:** the card stays hidden when there is no ad, when ads are
  disabled, or when UMP consent is not resolved yet.
• **No changes** to the compiler, PIC programmer, terminal, memory viewer or
  Pro pass.

### ✅ Validation
• `:app:bundleRelease` succeeds with `compileSdk 37`, `buildTools 37.0.0`,
  `targetSdk 37` and `minSdk 24`.
• AAB signed with `firma_pic_k150_programing.jks` (alias `pick150`).
• `versionCode 65` and `versionName 2.9.11`; ABIs `arm64-v8a`, `armeabi-v7a`,
  `x86` and `x86_64`.
