# Guía de Pruebas de Integración con el Emulador Virtual K150 (GUIA_PRUEBAS.md)

Esta guía detalla el entorno de pruebas unitarias y de integración del programador K150 (Protocolo P18A) desarrollado para este proyecto, validando el funcionamiento en múltiples familias de microcontroladores (PIC12, PIC16 y PIC18).

---

## 🛠️ 1. Configuración Inicial del Entorno

Antes de correr las pruebas, es necesario preparar las herramientas del SDK de Android. Ejecuta el script de configuración en la raíz del repositorio:
```bash
./setup-sdk.sh
```
*Este script descarga automáticamente el compilador NDK, las herramientas de Gradle y define el archivo `local.properties` apuntando a `/tmp/android-sdk` para evitar consumo de almacenamiento persistente.*

La firma para producción está configurada mediante el archivo `keystore.properties` en la raíz del proyecto, el cual se lee dinámicamente en el build.gradle (método basado en el repositorio `Flash-EEPROM-Tool` para mantener las credenciales de firma seguras fuera del control de versiones).

---

## 🏗️ 2. Estructura de Compilación y Ejecución Automatizada

El ciclo completo de pruebas levanta el emulador K150 de hardware virtualizado en segundo plano, abre puertos virtuales seriales (PTY/TTY) y ejecuta las pruebas de Gradle:

*   **Comando para ejecutar las pruebas con el emulador en C++ (Recomendado/Optimizado):**
    ```bash
    ./run_java_emulator_tests.sh cpp
    ```
*   **Comando para ejecutar las pruebas con el emulador en Python:**
    ```bash
    ./run_java_emulator_tests.sh python
    ```

El script detiene automáticamente el emulador al finalizar las pruebas y reporta los resultados.

---

## 🔬 3. Pruebas de Integración Implementadas Paso a Paso

Las pruebas se encuentran en [`ProtocoloP18AIntegrationTest.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/test/java/com/diamon/protocolo/ProtocoloP18AIntegrationTest.java). Estas son las validaciones cubiertas en el suite:

### A. Handshake y Protocolo Base (`testHandshakeYComandosBasicos`)
*   Valida la inicialización del programador enviando el byte de control `P`.
*   Comprueba el canal de eco serial para verificar la integridad física/emulada de la comunicación.
*   Obtiene la versión del firmware (reportando `K150`) y el protocolo (`P18A`).

### B. Borrado y Socket (`testDeteccionSocketYBorrado`)
*   Comprueba la detección del microcontrolador insertado en el ZIF Socket del programador.
*   Inicializa los registros de memoria y ejecuta el comando de borrado de chip (`cmd 14`), devolviendo la memoria ROM y EEPROM a sus estados limpios (`0xFF`).

### C. Grabado Parcial e Intercalado (`testImportacionYProgramacionIntercaladaParcial`)
Valida la independencia de la memoria de datos y de programa:
1.  Graba la memoria ROM con un archivo HEX (`main.hex`).
2.  Lee y verifica la ROM grabada, asegurándose de que la EEPROM permanezca vacía (`0xFF`).
3.  Graba únicamente la memoria EEPROM con un segundo archivo HEX (`pwmc_main107_628A.HEX`).
4.  Verifica que la EEPROM contenga los nuevos datos y que la ROM **no haya sido borrada o modificada** al escribir la EEPROM.
5.  Graba Fuses e ID de forma independiente usando un tercer archivo (`nuevoled.hex`) y verifica que ni la ROM ni la EEPROM sufran alteraciones.

### D. Grabado Completo a través del Administrador (`testManagerProgramacionCompleta`)
*   Instancia `PicProgrammingManager` y realiza el ciclo completo automatizado: Borrado -> Grabado ROM -> Grabado EEPROM -> Grabado Fuses e ID.
*   Verifica que los listeners de progreso y estados (`onProgrammingProgress`, `onProgrammingCompleted`) reporten las fases correspondientes en tiempo real.

### E. Soporte Multigeneracional de Familias (PIC12, PIC16, PIC18)
*   **PIC16 (Núcleo de 14 bits):** Valida lectura, escritura y fusión de registros con `pwmc_main107_628A.HEX` para PIC16F628A (ID de Chip `1060`).
*   **PIC18 (Núcleo de 16 bits):** Valida el algoritmo de direccionamiento lineal y grabado de fuses multigrupo (7 palabras de fuse y 8 bytes de ID) usando el archivo real `waw_pic18f2550.hex` para PIC18F2550 (ID de Chip `1240`).
*   **PIC12 (Núcleo de 14 bits / 8 Pines):** Valida lectura, grabado de ROM y fuses para chips de bajo pinout usando el archivo real `32x-autohz_12f675.hex` para PIC12F675 (ID de Chip `0FC0`).

### F. Formatos de Exportación y Round-Trip (`testExportacionYFormatosHexBin`)
*   Toma la memoria leída por el emulador y la formatea para exportación aplicando *Little Endian swabbing* y *padding* de Microchip mediante `HexExportManager`.
*   Genera el archivo Intel HEX resultante en memoria.
*   Importa de vuelta el HEX generado a través de `DatosPicProcesados` y comprueba que coincida al 100% byte por byte con la memoria física original, asegurando que cualquier exportación es compatible para importación posterior.

---

## 📦 4. Publicación en Producción

Para compilar la versión definitiva y firmada para Play Store de la APK y el Bundle (AAB):
```bash
chmod +x gradlew && ./gradlew bundleRelease assembleRelease
```
Los compilados se generarán redirigidos a la ruta `/tmp/k150/outputs/bundle/release/` y `/tmp/k150/outputs/apk/release/`.

Para publicar el bundle AAB en la Play Store, usa el script copiado de producción `upload_play_store.py`:
```bash
python3 upload_play_store.py \
  --package_name com.diamon.pic \
  --aab_path /tmp/k150/outputs/bundle/release/app-release.aab \
  --service_account_json /home/danielpdiamon/pc-api-6650547003605444910-569-9d23413fdc95.json \
  --track production \
  --release_notes "Mejoras de rendimiento y compatibilidad con emulación de PIC12 y PIC18."
```
