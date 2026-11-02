# 🚀 Notas de Lanzamiento / Release Notes - v2.9.6 (VersionCode 60)

Fecha de lanzamiento / Release Date: 2 de Noviembre de 2026 / November 2, 2026

---

## 🇪🇸 Español

### 📌 Resumen General
Esta versión corrige el `Resources$NotFoundException` que cerraba la app al inflar el `SwitchCompat` de la pantalla principal y el ANR de AdMob que ocurría con la aplicación en segundo plano. También fija Play Billing en 8.3.0 y HSDP en 2.2.0, y evita que el manejador global absorba excepciones del hilo principal.

### 🛠️ Correcciones Principales

#### 1. `Resources$NotFoundException` en `MainActivity.onCreate` (Crashlytics `857da16195dd070f687c07a80e369d06`)
• **Causa raíz:** el resource shrinker de AGP eliminó los PNG `abc_btn_switch_to_on_mtrl_00001` y `abc_btn_switch_to_on_mtrl_00012` (ID `0x7f070036`), que siguen referenciados por el selector `abc_switch_thumb_material` de AppCompat. Al inflar `activity_main.xml` línea 197, `SwitchCompat` intentaba cargar el drawable inexistente y lanzaba la excepción.
• **Corrección:** se desactivó `shrinkResources` en el build de release y se añadió [`app/src/main/res/raw/keep.xml`](app/src/main/res/raw/keep.xml) con reglas `tools:keep` para los drawables de AppCompat.
• **Verificación:** el APK release contiene `abc_btn_switch_to_on_mtrl_00001` (`0x7f070035`) y `abc_btn_switch_to_on_mtrl_00012` (`0x7f070036`) en mdpi, hdpi, xhdpi, xxhdpi y xxxhdpi; el selector `abc_switch_thumb_material` resuelve ambos IDs.

#### 2. ANR de AdMob con la app en segundo plano (Crashlytics `169756c0f0c89a39b0971fa8fb8d6a07`)
• **Causa raíz:** `GestorPublicidad` seguía reintentando la precarga de anuncios rewarded/native cuando la actividad ya no era visible. AdMob creaba un WebView en el hilo principal con el proceso en background, superando el tiempo de ANR.
• **Corrección:** no se cargan anuncios si la actividad no está al menos en estado `STARTED`; la precarga de rewarded se reanuda en `onResume`. También se omite la carga para usuarios Pro.

#### 3. Dependencias de terceros
• **Play Billing 9.1.0 ➔ 8.3.0:** evita el `NoSuchMethodError` de `ActivityOptions.setPendingIntentBackgroundActivityStartMode` en compilaciones de Android 14 que no exponen ese método.
• **HSDP 2.0.1 ➔ 2.2.0:** evita el `IllegalStateException: targetPackageName is null` de `HsdpShimActivity`.
• **R8:** se añadió una regla `-keep` para `com.google.android.play.core.hsdp.**`.

#### 4. Protección del hilo principal
• `PicApplication` solo absorbe excepciones de sistema en hilos de fondo. En el hilo principal siempre delega al manejador por defecto, evitando dejar el looper muerto y terminar en un ANR de `DestroyJavaVM`.

### ✅ Validación
• **Tests:** 121 pruebas unitarias y de integración, 0 fallos, 0 errores, 0 omitidas.
• **Emulador K150:** suite completa ejecutada contra el emulador C++ (`emulador_pic_k150`) mediante `ejecutar_pruebas_con_emulador.sh cpp`.
• **Build release:** `:app:assembleRelease` y `:app:bundleRelease` correctos; APK y AAB firmados con la clave de producción.

### 📦 Artefactos
• **APK Release:** `PIC-k150-Programing-v2.9.6-release.apk`  
  **SHA-256:** `dccd887041a76ce84a4bbb4013da48e8cbc59edfe8db3400d69657c15e42a9a3`
• **AAB Release (Google Play Bundle):** `PIC-k150-Programing-v2.9.6-release.aab`  
  **SHA-256:** `903edf738d92eb65dcb15e181ae757cbe53beebaddb38dc6f5539cde07ee6b03`

---

## 🇺🇸 English

### 📌 Overview
This release fixes the `Resources$NotFoundException` that crashed the app while inflating the main screen `SwitchCompat`, and the AdMob ANR that occurred while the app was in the background. It also pins Play Billing to 8.3.0 and HSDP to 2.2.0, and stops the global handler from swallowing main-thread exceptions.

### 🛠️ Key Fixes

#### 1. `Resources$NotFoundException` in `MainActivity.onCreate` (Crashlytics `857da16195dd070f687c07a80e369d06`)
• **Root cause:** the AGP resource shrinker removed the `abc_btn_switch_to_on_mtrl_00001` and `abc_btn_switch_to_on_mtrl_00012` PNGs (ID `0x7f070036`), still referenced by AppCompat's `abc_switch_thumb_material` selector. Inflating `activity_main.xml` line 197 made `SwitchCompat` load the missing drawable and throw.
• **Fix:** disabled `shrinkResources` for release and added [`app/src/main/res/raw/keep.xml`](app/src/main/res/raw/keep.xml) with `tools:keep` rules for the AppCompat drawables.
• **Verification:** the release APK contains `abc_btn_switch_to_on_mtrl_00001` (`0x7f070035`) and `abc_btn_switch_to_on_mtrl_00012` (`0x7f070036`) in mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi; the `abc_switch_thumb_material` selector resolves both IDs.

#### 2. AdMob ANR while in the background (Crashlytics `169756c0f0c89a39b0971fa8fb8d6a07`)
• **Root cause:** `GestorPublicidad` kept retrying rewarded/native preloads when the activity was no longer visible. AdMob created a WebView on the main thread while the process was in the background, exceeding the ANR timeout.
• **Fix:** ads are not loaded unless the activity is at least `STARTED`; rewarded preload resumes in `onResume`. Loading is also skipped for Pro users.

#### 3. Third-party dependencies
• **Play Billing 9.1.0 ➔ 8.3.0:** avoids the `NoSuchMethodError` from `ActivityOptions.setPendingIntentBackgroundActivityStartMode` on Android 14 builds where the method is absent.
• **HSDP 2.0.1 ➔ 2.2.0:** avoids the `IllegalStateException: targetPackageName is null` from `HsdpShimActivity`.
• **R8:** added a `-keep` rule for `com.google.android.play.core.hsdp.**`.

#### 4. Main-thread protection
• `PicApplication` only absorbs system exceptions on background threads. On the main thread it always delegates to the default handler, avoiding a dead looper and a `DestroyJavaVM` ANR.

### ✅ Validation
• **Tests:** 121 unit and integration tests, 0 failures, 0 errors, 0 skipped.
• **K150 emulator:** full suite executed against the C++ emulator (`emulador_pic_k150`) through `ejecutar_pruebas_con_emulador.sh cpp`.
• **Release build:** `:app:assembleRelease` and `:app:bundleRelease` succeeded; APK and AAB signed with the production key.

### 📦 Artifacts
• **APK Release:** `PIC-k150-Programing-v2.9.6-release.apk`  
  **SHA-256:** `dccd887041a76ce84a4bbb4013da48e8cbc59edfe8db3400d69657c15e42a9a3`
• **AAB Release (Google Play Bundle):** `PIC-k150-Programing-v2.9.6-release.aab`  
  **SHA-256:** `903edf738d92eb65dcb15e181ae757cbe53beebaddb38dc6f5539cde07ee6b03`

---

## 📋 Notas Cortas para Google Play Console / Short Play Store Release Notes

### 🇪🇸 Español (`es-419` / `es-ES`)
```text
• Corregido el cierre al iniciar la app por un recurso de SwitchCompat eliminado en el build.
• Corregido ANR de AdMob al cargar anuncios con la app en segundo plano.
• Actualizadas Play Billing 8.3.0 y HSDP 2.2.0 para evitar fallos en Android 14.
• Mejoras de estabilidad en la programación PIC por USB-OTG.
```

### 🇺🇸 English (`en-US`)
```text
• Fixed the startup crash caused by a SwitchCompat resource removed from the release build.
• Fixed the AdMob ANR when ads were loaded while the app was in the background.
• Updated Play Billing 8.3.0 and HSDP 2.2.0 to avoid Android 14 crashes.
• Stability improvements for USB-OTG PIC programming.
```

---

## 🔗 Enlaces / Links
• **Repositorio / Repository:** https://github.com/Danielk10/PIC-k150-Programing
• **Lanzamiento / Release:** https://github.com/Danielk10/PIC-k150-Programing/releases/tag/v2.9.6
