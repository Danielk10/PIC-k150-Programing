# Notas de Lanzamiento - PIC-k150-Programing v2.8.4 (versionCode 51)

Fecha de lanzamiento / Release Date: 5 de Octubre de 2026 / October 5, 2026

---

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- Importantes mejoras de estabilidad y rendimiento en Android 11 y versiones superiores.
- Exportación binaria optimizada con compatibilidad dual para almacenamiento local y en la nube.
- Mayor robustez y detección inmediata ante interrupciones de conexión USB OTG.
- Correcciones generales en la gestión de archivos y almacenamiento del sistema.

### English (`en-US`)
- Critical stability and performance improvements on Android 11 and newer versions.
- Optimized binary export with dual compatibility for local and cloud storage.
- Enhanced robustness and immediate detection for USB OTG connection interruptions.
- General file management and system storage handling improvements.

---

## 🛠️ Detalle Técnico de Cambios

1. **Parche Nativo C++ (`libfdsan_bypass.so`)**:
   - Integración de `fdsan_bypass.cpp` con constructor dinámico `__attribute__((constructor))` para invocar `android_fdsan_set_error_level(ANDROID_FDSAN_ERROR_LEVEL_DISABLED)`.
   - Carga anticipada en el bloque estático de `PicApplication.java` protegiendo todo el ciclo de vida del proceso de la aplicación (incluyendo mediadores de anuncios, WebView y E/S de archivos).
   - Configuración de `CMakeLists.txt` con CMake 4.1.2 y soporte para arquitecturas `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`.

2. **Sincronización Dual y Fallback en Exportación (`HexExportManager.java`)**:
   - Preservación del canal de alta velocidad `FileChannel.write()` con sincronización explícita `channel.force(true)` para `FileOutputStream`.
   - Fallback resiliente a `BufferedOutputStream` tradicional de 8KB en caso de fallos en descriptores virtuales o pipes remotos de SAF.

3. **Protección contra Desconexión USB (`Protocolo.java`)**:
   - Control de `bytesRead < 0` en el bucle de `readBytes()`, deteniendo de inmediato intentos recurrentes de lectura `bulkTransfer` sobre descriptores USB desconectados o cerrados por el kernel.

4. **Gestión de Recursos y Fugas de Descriptores (`FileManager.java`)**:
   - Uso de `try-with-resources` en `readBinaryAsIntelHex()` garantizando el cierre atómico del `InputStream`.
   - Adición de respaldo de streaming en `copyFileZeroCopy()` ante fallos de `transferTo()` en sistemas de archivos virtuales.

5. **Configuración y Manifiesto (`app/build.gradle`)**:
   - Incremento a `versionCode 51` y `versionName "2.8.4"`.
   - Integración de `externalNativeBuild` con CMake 4.1.2 y filtros ABI NDK.
   - Preservación de `android:largeHeap="true"`.
