# Resumen de Instalación del SDK de Android

En la sesión anterior se realizaron las siguientes acciones para configurar el entorno de desarrollo:

1.  **Directorio del SDK:** Se creó la carpeta `~/android-sdk`.
2.  **Herramientas de línea de comandos:**
    *   Se descargó `commandlinetools-linux-14742923_latest.zip`.
    *   Se descomprimió en `~/android-sdk/cmdline-tools/latest`.
3.  **Variables de Entorno:** Se configuraron en `~/.bashrc`:
    *   `ANDROID_HOME=$HOME/android-sdk`
    *   Se añadieron al `PATH` las carpetas `cmdline-tools/latest/bin` y `platform-tools`.
4.  **Platform-Tools:** Se instaló el paquete `platform-tools` (incluye `adb`, `fastboot`) usando `sdkmanager`.
5.  **Verificación:** Se confirmó el funcionamiento de `sdkmanager` (v20.0) y `adb` (v1.0.41).

---
*Generado por Gemini CLI*
