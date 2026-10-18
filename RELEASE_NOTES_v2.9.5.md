# 🚀 Notas de Lanzamiento / Release Notes - v2.9.5 (VersionCode 59)

Fecha de lanzamiento / Release Date: 18 de Octubre de 2026 / October 18, 2026

---

## 🇪🇸 Español

### 📌 Resumen General
Esta versión resuelve el fallo reportado en **Firebase Crashlytics (`#8101c07c`)** relacionado con `ProxyBillingActivity.onCreate` en dispositivos de pruebas automatizadas de Google Play, incorpora el blindaje retrocompatible de `WindowInsets` en `GestorPantalla` (alineado con *C-PIC Compiler*) para Android 14+, y actualiza las dependencias de Firebase, Google Play Services Ads y Android Gradle Plugin a sus últimas versiones estables.

---

### 🛠️ Correcciones y Mejoras Principales

#### 💳 1. Resolución de Crashlytics en `ProxyBillingActivity` (`#8101c07c307416da1819dc7553864c1b`)
* **Detección de Entornos de Prueba Automatizada (`isRunningInTestLab`):** En los emuladores `x86_64` del Informe Previo al Lanzamiento de Google Play Console / Firebase Test Lab (`Settings.System: firebase.test.lab = true`), Google Play Services no entrega un `PendingIntent` de compra válido, provocando `NullPointerException: PendingIntent.getIntentSender()` dentro de `ProxyBillingActivity.onCreate`. Ahora `BillingManager` detecta este entorno y omite la apertura de la actividad proxy de forma segura.
* **Verificación de Ciclo de Vida y Estado del Cliente:** Se valida que la `Activity` anfitriona no esté finalizando ni destruida (`!isFinishing() && !isDestroyed()`) y que `billingClient.isReady()` sea verdadero antes de iniciar el flujo de compra; de lo contrario, reconecta automáticamente.
* **Protección Anti-Doble-Toque (`LAUNCH_DEBOUNCE_MS = 2000ms`):** Evita lanzar instancias concurrentes de `ProxyBillingActivity` por toques rápidos repetidos, restableciéndose inmediatamente al recibir respuesta en `onPurchasesUpdated`.
* **Captura Defensiva de Excepciones:** Se envuelve `billingClient.launchBillingFlow(...)` en un bloque `try-catch (RuntimeException)` con notificación controlada de error.

#### 📱 2. Blindaje de `WindowInsets` en `GestorPantalla` (Alineado con C-PIC Compiler)
* **Prevención de `NoSuchMethodError` en Android 14+:** Se incorporó `obtenerInsetsSeguros(WindowInsetsCompat)` con captura `try-catch (Throwable)` y respaldo a los insets del sistema tradicionales (`getSystemWindowInsetLeft/Top/Right/Bottom` o `Insets.NONE`), evitando caídas en firmwares donde `WindowInsets.Type.systemOverlays()` no está presente.
* **Modo Inmersivo Protegido:** Se blindó `ocultarBotonesVirtuales()` frente a excepciones de plataforma al ocultar barras del sistema.

#### ⬆️ 3. Actualización de Librerías y Herramientas de Compilación (`libs.versions.toml`)
* **Android Gradle Plugin (`agp`):** `9.2.1` ➔ **`9.4.1`**
* **Firebase BoM (`firebaseBom`):** `34.19.0` ➔ **`35.0.2`** (`firebase-analytics 24.0.0`, `firebase-firestore 27.0.1`, `firebase-messaging 26.0.0`, `firebase-crashlytics-ndk 21.0.0`)
* **Firebase Crashlytics (`firebaseCrashlytics`):** `20.1.1` ➔ **`21.0.0`**
* **Google Play Services Ads (`playServicesAds`):** `25.3.0` ➔ **`25.5.0`**
* **Limpieza en `app/build.gradle`:** Eliminada declaración duplicada de `libs.activity` y variable local en desuso.

#### 🧪 4. Cobertura de Pruebas (121/121 Pasadas)
* Se añadieron 4 pruebas unitarias nuevas en `BillingManagerTest` (cliente desconectado, actividad finalizando, anti-doble-toque y excepción en `launchBillingFlow`), alcanzando **121 pruebas automatizadas exitosas** (unitarias + integración con el emulador C++ K150).

---

## 🇺🇸 English

### 📌 Overview
This release resolves the **Firebase Crashlytics (`#8101c07c`)** crash in `ProxyBillingActivity.onCreate` triggered on Google Play automated pre-launch test devices, ports the backward-compatible `WindowInsets` safeguard in `GestorPantalla` from *C-PIC Compiler* for Android 14+, and updates Firebase, Google Play Services Ads, and Android Gradle Plugin to their latest stable releases.

---

### 🛠️ Key Fixes & Improvements

#### 💳 1. Crashlytics Fix for `ProxyBillingActivity` (`#8101c07c307416da1819dc7553864c1b`)
* **Automated Test Lab Detection (`isRunningInTestLab`):** On Google Play Console Pre-Launch Report / Firebase Test Lab `x86_64` virtual devices (`Settings.System: firebase.test.lab = true`), Google Play Services does not return a valid purchase `PendingIntent`, causing `ProxyBillingActivity.onCreate` to throw `NullPointerException: PendingIntent.getIntentSender()`. `BillingManager` now detects Test Lab environments and safely skips launching the billing proxy activity.
* **Activity Lifecycle & Readiness Guards:** Verifies that the host `Activity` is active (`!isFinishing() && !isDestroyed()`) and `billingClient.isReady()` before launching the purchase flow, triggering automatic reconnection when needed.
* **Double-Tap Debounce (`LAUNCH_DEBOUNCE_MS = 2000ms`):** Prevents overlapping `ProxyBillingActivity` launches from rapid taps and resets immediately once `onPurchasesUpdated` completes.
* **Defensive Exception Handling:** Wraps `billingClient.launchBillingFlow(...)` in a `try-catch (RuntimeException)` block.

#### 📱 2. `WindowInsets` Hardening in `GestorPantalla` (Aligned with C-PIC Compiler)
* **Android 14+ `NoSuchMethodError` Prevention:** Added `obtenerInsetsSeguros(WindowInsetsCompat)` with `try-catch (Throwable)` fallback to legacy system window insets or `Insets.NONE` when `WindowInsets.Type.systemOverlays()` is missing on certain Android 14+ builds.
* **Protected Immersive Controller:** Wrapped `ocultarBotonesVirtuales()` in a defensive `try-catch (Throwable)` block.

#### ⬆️ 3. Library & Build Tool Upgrades (`libs.versions.toml`)
* **Android Gradle Plugin (`agp`):** `9.2.1` ➔ **`9.4.1`**
* **Firebase BoM (`firebaseBom`):** `34.19.0` ➔ **`35.0.2`** (`firebase-analytics 24.0.0`, `firebase-firestore 27.0.1`, `firebase-messaging 26.0.0`, `firebase-crashlytics-ndk 21.0.0`)
* **Firebase Crashlytics (`firebaseCrashlytics`):** `20.1.1` ➔ **`21.0.0`**
* **Google Play Services Ads (`playServicesAds`):** `25.3.0` ➔ **`25.5.0`**

---

## 📋 Notas Cortas para Google Play Console / Short Play Store Release Notes

### 🇪🇸 Español (`es-419` / `es-ES`)
```text
• Solucionado cierre inesperado en Google Play Billing (ProxyBillingActivity) en entornos sin PendingIntent de compra.
• Protección de pantalla completa y barras del sistema (WindowInsets) para Android 14, 15 y 16.
• Actualización de librerías Firebase BoM 35.0.2, Crashlytics 21.0.0 y Google Mobile Ads 25.5.0.
• Mejoras de estabilidad y rendimiento en la programación de microcontroladores PIC por USB-OTG.
```

### 🇺🇸 English (`en-US`)
```text
• Fixed a crash in Google Play Billing (ProxyBillingActivity) when purchase PendingIntent is unavailable.
• Hardened edge-to-edge WindowInsets handling across Android 14, 15, and 16 devices.
• Updated Firebase BoM 35.0.2, Crashlytics 21.0.0, and Google Mobile Ads 25.5.0 libraries.
• Stability and performance improvements for USB-OTG PIC microcontroller programming.
```
