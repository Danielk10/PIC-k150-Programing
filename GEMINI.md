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

---

## 🧪 4. Pruebas de Integración de Lógica Java con Emulador
Se implementó un entorno de pruebas integradas para validar la lógica del protocolo Java (`ProtocoloP18A.java`) contra el emulador K150 en local usando puertos virtuales (PTY):
* **Clase de Prueba:** [`ProtocoloP18AIntegrationTest.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/test/java/com/diamon/protocolo/ProtocoloP18AIntegrationTest.java)
  - Valida el ciclo completo de vida (handshake, eco, lectura/escritura de ROM, borrado del chip y detección en socket).
* **Script de Ejecución Automatizado:** [`run_java_emulator_tests.sh`](file:///home/danielpdiamon/PIC-k150-Programing/run_java_emulator_tests.sh)
  - Levanta el emulador en segundo plano, realiza las pruebas en Gradle (`testDebugUnitTest`) y detiene el emulador al finalizar de forma segura.
* **Comando para Ejecutar:**
  ```bash
  ./run_java_emulator_tests.sh
  ```

---

## 🛠️ 5. Resumen de Cambios y Correcciones Realizadas
Recientemente se aplicaron correcciones críticas para mejorar la robustez de la lógica Java y la fidelidad de las pruebas con el emulador virtual:

1. **Compatibilidad con formatos Windows (`\r\n`):** Se modificó [`HexProcesado.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/main/java/com/diamon/datos/HexProcesado.java) para limpiar/recortar las líneas (`.trim()`), lo que permite procesar archivos HEX generados en Windows sin fallar por caracteres no hexadecimales (retornos de carro `\r`).
2. **Corrección de bugs en comunicación serial (`ProtocoloP18A.java`):**
   * Se solucionó la extensión de signo implícita en Java al formatear bytes en hexadecimal (ej. `0xFF` leyéndose como `FFFFFFFF` en lugar de `FF`) usando la máscara `& 0xFF`.
   * Se corrigió la sobrescritura del índice del búfer en lecturas seriales fragmentadas (reemplazando índices estáticos con `bytesLeidos + i`).
3. **Nuevos tests de integración:** Se agregaron pruebas de ciclo completo de vida para la programación/lectura de **EEPROM** y **Fuses/Configuraciones** (validando que el ID del chip coincida) en [`ProtocoloP18AIntegrationTest.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/test/java/com/diamon/protocolo/ProtocoloP18AIntegrationTest.java).
4. **Desactivación de Software Flow Control (XON/XOFF):** Se modificaron [`emulador_k150.py`](file:///home/danielpdiamon/emulador_picpro/emulador_k150.py) y [`emulador_k150.cpp`](file:///home/danielpdiamon/emulador_picpro/emulador_k150.cpp) para deshabilitar las banderas `IXON`, `IXOFF` e `IXANY` en la terminal virtual (PTY). Esto evita que el sistema operativo intercepte y descarte silenciosamente bytes de datos seriales con valor `0x11` o `0x13`.
5. **Selector del motor de emulación:** El script [`run_java_emulator_tests.sh`](file:///home/danielpdiamon/PIC-k150-Programing/run_java_emulator_tests.sh) ahora acepta un parámetro (`cpp` o `python`) para validar las pruebas integradas de Java contra cualquiera de los dos emuladores:
   * `./run_java_emulator_tests.sh cpp` (Compila si es necesario e inicia el emulador en C++).
   * `./run_java_emulator_tests.sh python` (Inicia el emulador en Python).

