# Notas de Lanzamiento - PIC-k150-Programing v2.9.3 (versionCode 57)

Fecha de lanzamiento / Release Date: 16 de Octubre de 2026 / October 16, 2026

---

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- 🛡️ **Protección Automática de Play y API 24+**: Actualización de la versión mínima de Android a Android 7.0 (API Level 24 - Nougat) para habilitar la Protección automática contra manipulaciones de Google Play (*Play Automatic Protection*).
- ⚡ **Optimización R8 y Ofuscación DEX (Android Vitals)**: Activación de optimización completa R8 (`minifyEnabled true` y `shrinkResources true`) con reglas ProGuard dedicadas, reduciendo el tamaño del APK de 21 MB a 15 MB y superando el umbral de ofuscación de Google Play Vitals.
- 🔒 **Aislamiento Estricto de Memoria**: La edición y reprogramación en el visor hexadecimal operan de manera 100% aislada, preservando inalterado el archivo HEX/BIN cargado desde disco y manteniendo el botón principal deshabilitado si no hay un archivo cargado.
- 💡 **Avisos de Compatibilidad en Funciones Pro**: Al pulsar «Leer» o «Grabar» en Calibración OSCCAL o Vector ICD, se valida de forma no invasiva la compatibilidad del microcontrolador notificando mediante Toast si el chip no lo admite.
- 🔢 **Enmascaramiento de Silicio por Arquitectura**: Formateo dinámico exacto de palabras de calibración (14-bit con `0x3FFF` en vez de `0xFFFF`, 12-bit con `0x0FFF` y 16-bit con `0xFFFF`).
- 🌍 **Sincronización Total en 53 Idiomas (474 Claves)**: 100% de paridad y traducción nativa en las 53 variantes de idiomas soportadas.

### English (`en-US`)
- 🛡️ **Play Automatic Protection & API 24+**: Updated minimum SDK to Android 7.0 (API Level 24 - Nougat) to support Google Play Automatic Protection anti-tamper and piracy defenses.
- ⚡ **R8 Optimization & DEX Obfuscation (Android Vitals)**: Enabled full R8 optimization (`minifyEnabled true` and `shrinkResources true`) with dedicated ProGuard rules, slashing APK size from 21 MB down to 15 MB while exceeding Google Play Vitals obfuscation requirements.
- 🔒 **Strict Memory Isolation**: Hex editing and reprogramming operate in complete isolation, leaving original disk-loaded HEX/BIN firmware untouched and keeping the main program button safely disabled when no disk file is loaded.
- 💡 **Pro Feature Compatibility Feedback**: Clicking Read or Write in OSCCAL calibration or ICD debug vector dialogs triggers non-intrusive Toast feedback if the selected microcontroller is incompatible.
- 🔢 **Architecture-Aware Core Masking**: Accurate silicon word masking for calibration (14-bit words masked to `0x3FFF` instead of `0xFFFF`, 12-bit to `0x0FFF`, and 16-bit to `0xFFFF`).
- 🌍 **Full 53-Language Parity (474 Keys)**: Complete 100% resource synchronization and native translation across all 53 supported locales.

---

## 🛠️ Detalle Técnico de Cambios

1. **Cumplimiento de Seguridad y Google Play Automatic Protection**:
   - `minSdkVersion` elevado a 24 en `app/build.gradle` y `README.md`.
   - `versionCode` incrementado a 57 y `versionName` a `2.9.3`.

2. **Optimización de Bytecode DEX y Reglas R8 (`proguard-rules.pro`)**:
   - `minifyEnabled true` y `shrinkResources true` en la variante `release`.
   - Reglas de preservación para controladores USB serial (`com.hoho.android.usbserial.**`), modelos de datos PIC (`com.diamon.chip.**`, `com.diamon.datos.**`, `com.diamon.protocolo.**`), Google Play Billing Library (`com.android.billingclient.**`), Firebase y vistas personalizadas (`PicAnimationView`, `ReselectableSpinner`).
   - Reducción del APK de 20.6 MB a 15.1 MB.

3. **Aislamiento de Firmware y Memoria de Trabajo**:
   - Desacoplamiento de `originalLoadedFirmware` y `originalLoadedDatosPic` en `MainActivity.java` durante la edición hexadecimal.

4. **Compatibilidad y Enmascaramiento por Núcleo en `CalibracionManager.java`**:
   - Método `isCompatibleConIcd(ChipPic chip)`.
   - Validación y aviso Toast en diálogos de OSCCAL e ICD.
   - Enmascaramiento por arquitectura de núcleo (`0x3FFF` para 14 bits, `0x0FFF` para 12 bits, `0xFFFF` para 16 bits).

5. **Pruebas y Certificación**:
   - 115/115 pruebas unitarias y de integración Java aprobadas contra el emulador virtual K150 en C++.
   - Verificación de compilación Release (`assembleRelease` y `bundleRelease`).
