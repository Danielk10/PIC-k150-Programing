# Notas de Lanzamiento - PIC-k150-Programing v2.9.1 (versionCode 54)

Fecha de lanzamiento / Release Date: 15 de Octubre de 2026 / October 15, 2026

---

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- 🌍 **Internacionalización Completa y Sincronización Global**: Sincronización exhaustiva del 100% de las cadenas de recursos (450 claves) en los 53 idiomas soportados por la aplicación.
- 🗣️ **Traducciones Técnicas Nativas**: Localización precisa y adaptada al ámbito de sistemas embebidos, microcontroladores PIC y electrónica para todas las herramientas Pro (Diagnóstico de Hardware, Editor Hexadecimal en Vivo, Calibración OSCCAL, Vector ICD, Control de Voltajes VPP/VDD y Sensores ZIF).
- 🧹 **Depuración y Limpieza de Recursos**: Eliminación definitiva de claves obsoletas, recursos huérfanos y optimización del árbol de internacionalización en Android XML.
- 🧪 **Certificación de Silicio y Emulación**: Validación al 100% de la suite de pruebas unitarias y de integración contra el emulador virtual del programador PIC K150.
- ⚡ **Compilación de Producción Optimizada**: Generación de paquetes firmados APK y Android App Bundle (AAB) con soporte nativo multiarquitectura (ARM64, ARMv7, x86, x86_64).

### English (`en-US`)
- 🌍 **Full Internationalization & Global Synchronization**: Exhaustive 100% synchronization of all string resources (450 keys) across all 53 supported application languages.
- 🗣️ **Native Technical Localization**: High-precision terminology tailored to embedded electronics, PIC microcontrollers, and programmer hardware for all Pro suite features (Hardware Diagnostics, Live Hex Editor, OSCCAL Calibration, ICD Vector, VPP/VDD Voltage Controls, and ZIF Sensors).
- 🧹 **Resource Cleanup**: Deprecated keys cleanly removed, XML entity formatting sanitized, and internationalization architecture streamlined.
- 🧪 **Emulation & Protocol Certification**: 100% test pass rate across the full unit and integration test suite targeting the virtual PIC K150 hardware emulator.
- ⚡ **Optimized Production Builds**: Signed multi-architecture production APK and Google Play Android App Bundle (AAB) targeting SDK 37 (Android 15+).

---

## 🛠️ Detalle Técnico de Cambios

1. **Sincronización Exhaustiva de Recursos de Texto (53 Idiomas)**:
   - Idioma base por defecto configurado en inglés (`values/strings.xml` y `values-en/strings.xml`).
   - Español nativo certificado en variantes base y regionales (`values-es`, `values-es-rES`, `values-es-rVE`).
   - 48 idiomas adicionales sincronizados al 100% con 450 claves cada uno: af, am, ar, be, bg, ca, cs, da, de, el, et, fa, fi, fr, hi, hr, hu, hy, in, is, it, iw, ja, kk, ko, lt, lv, ms, nb, nl, pl, pt, ro, ru, sk, sl, sr, sv, sw, th, tl, tr, uk, vi, zh, zh-rHK, zh-rTW, zu.
   - Preservación estricta de especificadores de formato (`%1$s`, `%2$s`, `%1$d`, `100%%`) y escapeo seguro de entidades XML (`\'`, `&amp;`, `&lt;`, `&gt;`).

2. **Validación y Certificación**:
   - 104/104 pruebas unitarias y de integración Java aprobadas contra el emulador virtual K150 en C++.
   - Verificación de compilación Debug (`./gradlew assembleDebug`).
   - Compilación Release de producción verificada (`./gradlew assembleRelease bundleRelease`).
