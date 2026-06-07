# Guía de Uso del SDK de Android (Terminal)

Esta guía explica cómo gestionar y usar las herramientas del SDK desde la línea de comandos.

## 1. Gestión de Paquetes con `sdkmanager`

El `sdkmanager` es la herramienta principal para instalar, actualizar y listar componentes.

*   **Listar paquetes instalados y disponibles:**
    ```bash
    sdkmanager --list
    ```
*   **Instalar un paquete:**
    ```bash
    sdkmanager "nombre_del_paquete"
    ```
    *(Ejemplo: `sdkmanager "platforms;android-37.0" "build-tools;37.0.0"`)
*   **Actualizar todos los paquetes:**
    ```bash
    sdkmanager --update
    ```

## 2. Depuración con `adb` (Android Debug Bridge)

`adb` permite interactuar con dispositivos físicos o emuladores.

*   **Listar dispositivos conectados:**
    ```bash
    adb devices
    ```
*   **Instalar una APK:**
    ```bash
    adb install mi_aplicacion.apk
    ```
*   **Entrar al shell del dispositivo:**
    ```bash
    adb shell
    ```

## 3. Gestión de Emuladores con `avdmanager`

*   **Listar dispositivos virtuales (AVDs):**
    ```bash
    avdmanager list avd
    ```
*   **Crear un nuevo AVD:**
    ```bash
    avdmanager create avd -n nombre_avd -k "system-images;android-37.0;google_apis;x86_64"
    ```

---
*Generado por Gemini CLI*
