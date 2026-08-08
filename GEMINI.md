# Guía de Desarrollo y Compilación para Agentes (GEMINI.md)

Este archivo sirve como referencia puntual y resumida para que cualquier agente de IA o desarrollador entienda la estructura de compilación y el entorno de emulación del proyecto.

---

## 📦 1. Configuración de Android SDK e NDK
Para configurar las herramientas de compilación de Android en este entorno sin consumir almacenamiento persistente en el home:
* Corre el script de configuración incluido en el repositorio:
  ```bash
  ./setup-sdk.sh
  ```
  *Esto descargará e instalará el SDK, NDK y dependencias automáticamente en la ruta temporal `/tmp/android-sdk` y generará el archivo `local.properties`.*

---

## 🏗️ 2. Redirección de Compilación (Build Outputs)
Toda la salida de compilación se redirige a `/tmp` para ahorrar espacio de disco persistente:
* **Directorio raíz del build:** `/tmp/k150/`
* **Ruta de la APK (Debug):** `/tmp/k150/outputs/apk/debug/app-debug.apk`
* **Ruta del Bundle AAB (Debug):** `/tmp/k150/outputs/bundle/debug/`
* **Comando para compilar la APK:**
  ```bash
  chmod +x gradlew && ./gradlew assembleDebug
  ```

---

## 🔌 3. Entorno de Emulación K150 Local
El programador K150 (protocolo P18A) y el microcontrolador PIC16F628A están emulados de forma virtual en:
* **Carpeta del Emulador:** `/home/danielpdiamon/emulador_picpro/`
* **Archivos Disponibles:**
  - `emulador_k150.py`: Emulador de hardware en Python.
  - `emulador_k150.cpp` / `emulador_k150_cpp`: Emulador de hardware en C++ y su binario optimizado.
  - `picpro_patched.py`: Wrapper de `picpro` con parches para compatibilidad de terminales virtuales PTY (ignora DTR/RTS).
  - `probar_emulacion.sh`: Script bash de prueba para validar el ciclo de vida completo (`./probar_emulacion.sh cpp` o `python`).
* **Guía Detallada de Emulación y Familias (PIC12/16/18):**
  - Consultar: [`/home/danielpdiamon/emulador_picpro/guia_emulacion_k150.md`](file:///home/danielpdiamon/emulador_picpro/guia_emulacion_k150.md)
