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
* **Carpeta del Emulador:** [`/home/danielpdiamon/emulador_pic_k150/`](file:///home/danielpdiamon/emulador_pic_k150/) (disponible también dentro del repositorio en `emulador_pic_k150/`)
* **Archivos Disponibles:**
  - `emulador_k150.py`: Emulador de hardware en Python.
  - `emulador_k150.cpp` / `emulador_k150_cpp`: Emulador de hardware en C++ y su binario optimizado.
  - `picpro_patched.py`: Wrapper de `picpro` que da prioridad al repositorio local `~/picpro` y parcha llamadas ioctl PTY (DTR/RTS).
  - `probar_emulacion.sh`: Script bash de prueba para validar el ciclo de vida completo (`./probar_emulacion.sh cpp` o `python`).
* **Guía Detallada de Emulación y Familias (PIC12/16/18):**
  - Consultar: [`/home/danielpdiamon/emulador_pic_k150/guia_emulacion_k150.md`](file:///home/danielpdiamon/emulador_pic_k150/guia_emulacion_k150.md)

---

## 🧪 4. Pruebas de Integración de Lógica Java con Emulador
Se implementó un entorno de pruebas integradas para validar la lógica del protocolo Java (`ProtocoloP18A.java`) contra el emulador K150 en local usando puertos virtuales (PTY):
* **Clase de Prueba:** [`ProtocoloP18AIntegrationTest.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/test/java/com/diamon/protocolo/ProtocoloP18AIntegrationTest.java)
  - Valida el ciclo completo de vida (handshake, eco, lectura/escritura de ROM, borrado del chip y detección en socket).
* **Script de Ejecución Automatizado:** [`ejecutar_pruebas_con_emulador.sh`](file:///home/danielpdiamon/PIC-k150-Programing/ejecutar_pruebas_con_emulador.sh) (alias: `run_java_emulator_tests.sh`)
  - Levanta el emulador de hardware (en C++ o Python) en segundo plano, ejecuta las pruebas integradas de la app en Gradle (`testDebugUnitTest`) y detiene el emulador al finalizar de forma segura.
* **Comando para Ejecutar:**
  ```bash
  ./ejecutar_pruebas_con_emulador.sh cpp
  ```

---

## 🛠️ 5. Resumen de Cambios y Correcciones Realizadas
Recientemente se aplicaron correcciones críticas para mejorar la robustez de la lógica Java y la fidelidad de las pruebas con el emulador virtual:

1. **Compatibilidad con formatos Windows (`\r\n`):** Se modificó [`HexProcesado.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/main/java/com/diamon/datos/HexProcesado.java) para limpiar/recortar las líneas (`.trim()`), lo que permite procesar archivos HEX generados en Windows sin fallar por caracteres no hexadecimales (retornos de carro `\r`).
2. **Corrección de bugs en comunicación serial (`ProtocoloP18A.java`):**
   * Se solucionó la extensión de signo implícita en Java al formatear bytes en hexadecimal (ej. `0xFF` leyéndose como `FFFFFFFF` en lugar de `FF`) usando la máscara `& 0xFF`.
   * Se corrigió la sobrescritura del índice del búfer en lecturas seriales fragmentadas (reemplazando índices estáticos con `bytesLeidos + i`).
3. **Nuevos tests de integración:** Se agregaron pruebas de ciclo completo de vida para la programación/lectura de **EEPROM** y **Fuses/Configuraciones** (validando que el ID del chip coincida) en [`ProtocoloP18AIntegrationTest.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/test/java/com/diamon/protocolo/ProtocoloP18AIntegrationTest.java).
4. **Desactivación de Software Flow Control (XON/XOFF):** Se modificaron [`emulador_k150.py`](file:///home/danielpdiamon/emulador_pic_k150/emulador_k150.py) y [`emulador_k150.cpp`](file:///home/danielpdiamon/emulador_pic_k150/emulador_k150.cpp) para deshabilitar las banderas `IXON`, `IXOFF` e `IXANY` en la terminal virtual (PTY). Esto evita que el sistema operativo intercepte y descarte silenciosamente bytes de datos seriales con valor `0x11` o `0x13`.
5. **Persistencia de memoria en emulador y corrección de fuses en borrado:**
   * Se evitó el reseteo indiscriminado de buffers de ROM/EEPROM al ejecutar `init programming vars` (`cmd == 3`), permitiendo verificar o volcar la memoria tras programar en comandos CLI independientes.
   * Se ajustó el valor por defecto de fuses en borrado de chips 14-bit a `0x3FFF` (en lugar de `0xFFFF`), evitando errores de validación de fuses en clientes como `picpro`.
6. **Renombrado coherente del entorno del emulador:**
   * Se renombró y estructuró la carpeta del emulador como [`emulador_pic_k150/`](file:///home/danielpdiamon/emulador_pic_k150/) (coherente con `PIC-k150-Programing`), integrándolo tanto en la raíz del repositorio como en `~/emulador_pic_k150`.
   * Se integró el soporte para utilizar el repositorio clonado [`/home/danielpdiamon/picpro`](file:///home/danielpdiamon/picpro) directamente en las pruebas.
7. **Selector del motor de emulación:** El script [`run_java_emulator_tests.sh`](file:///home/danielpdiamon/PIC-k150-Programing/run_java_emulator_tests.sh) acepta un parámetro (`cpp` o `python`) para validar las pruebas integradas de Java contra cualquiera de los dos emuladores:
   * `./run_java_emulator_tests.sh cpp` (Compila si es necesario e inicia el emulador en C++).
   * `./run_java_emulator_tests.sh python` (Inicia el emulador en Python).
8. **Arquitectura de Sincronización del Protocolo K150 en Android (USB-OTG):**
   * Documento técnico completo en [`sincronizacion_protocolo_k150_android.md`](file:///home/danielpdiamon/PIC-k150-Programing/sincronizacion_protocolo_k150_android.md).
   * Explica por qué la aplicación en Android omite la Fase 1 (Power-up `B\x03`) y entra directamente a la *Command Jump Table* mediante la secuencia determinista `0x01 -> 'Q' -> 'P' -> 'P'`, superando el desfase temporal de VBUS en USB-OTG y la ineficacia del reset por DTR en chips clones seriales.
9. **Resolución de Crash en Crashlytics y Actualización de Dependencias (v2.8.5):**
   * Se eliminó la dependencia innecesaria `firebase-auth` que inyectaba `SignInHubActivity` (`com.google.android.gms:play-services-auth`) provocando `NullPointerException` en dispositivos Huawei (`HRY-LX1T`).
   * Se actualizaron todas las dependencias principales a sus últimas versiones estables (`firebase-bom: 34.19.0`, `appcompat: 1.8.0`, `constraintlayout: 2.2.2`, `mockito-core: 5.24.0`, plugins de Google Services 4.5.0 y Crashlytics 3.0.8).
   * Se configuró `systemProp.java.net.preferIPv4Stack=true` en `gradle.properties` para asegurar descargas inmediatas sin timeouts por IPv6.
10. **Validación Exitosa en Hardware Real (K150 + PIC16F628A):**
   * Validación al 100% de las 7 fases del protocolo en silicio real sobre Android 12 (detección de Device ID 0x6810, volcado ROM/EEPROM, borrado masivo, verificación en blanco, programación completa y verificación de fuses), corroborando la robustez de la pila en producción.
11. **Lanzamiento Mayor v2.9.0 (K150 Master Suite):**
   * Implementación completa de [`PROPUESTA_AUTODETECCION_CHIP_K150.md`](file:///home/danielpdiamon/PIC-k150-Programing/PROPUESTA_AUTODETECCION_CHIP_K150.md):
     - Autodetección automática de PIC por Device ID en silicio (Flash).
     - Botón Conectar/Desconectar USB en Toolbar con reconexión transparente sin re-pedir permisos OTG.
     - Desacoplamiento de operaciones de hardware (Dump, Borrado, Blank Check y Detección disponibles sin requerir archivo HEX previo).
     - Rediseño visual 3D de alta fidelidad en `PicAnimationView` (zócalo verde Textool K150 `#0F7A4D`, palanca metálica cerrada, Pin 1 dinámico, microcontrolador proporcional, llenado de memoria y barrido láser).
     - Preservación automática de calibración de fábrica OSCCAL (`RETLW xx`) para PIC12F/PIC16F y soporte PIC10F (Comando `0x18`).
     - Vector de Depuración ICD (24 bits) para inspección y parcheo.
     - Diálogo de Diagnóstico Hardware K150 (modelo, protocolo, test de eco/latencia y control manual de voltajes VPP 13V / VDD 5V con fail-safe para técnicos).
     - Programación selectiva (ROM, EEPROM, Fuses), Blank Check Dual y Editor Hexadecimal interactivo en vivo.
12. **Monetización Híbrida Ética (In-App Purchase & Pase Pro de 12 Horas con Video Recompensado):**
   * Integración de `BillingManager` con Google Play Billing 9.1.0 para compra in-app permanente (`remove_ads_pro`).
   * `GestorPublicidad` y `ProPassManager`: desbloqueo del Pase Pro temporal de 12 horas mediante AdMob Rewarded Video (`ca-app-pub-5141499161332805/7291519778`, unidad *"Pase Pro 12 Horas K150"*), persistido en SharedPreferences con formateo dinámico (`11h 45m`) y ocultamiento del banner publicitario.
   * `ProAccessDialog`: diálogo modal oscuro temático para acceder al pase o a la compra permanente.
   * Protección estricta de funciones gratuitas: Grabación de silicio completa/selectiva (ROM, EEPROM, Fuses), Detección en zócalo, Borrado masivo, Blank Check Dual y Exportación de Dumps (HEX/BIN) son 100% libres y sin costo. Solo 4 herramientas avanzadas (Editor Hexadecimal en Vivo, Diagnóstico Hardware y test VPP/VDD, Vector ICD y calibración manual OSCCAL) están protegidas por el Pase Pro.
   * Guía paso a paso para Google Play Console y AdMob en [`GUIA_CONFIGURACION_PLAY_CONSOLE_MONETIZACION.md`](file:///home/danielpdiamon/PIC-k150-Programing/GUIA_CONFIGURACION_PLAY_CONSOLE_MONETIZACION.md).

---

## 🔌 6. Pruebas y Validación en Hardware Real (K150 + PIC16F628A por USB-OTG y ADB)
La suite de comunicación y el protocolo P18A implementado en [`ProtocoloP18A.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/main/java/com/diamon/protocolo/ProtocoloP18A.java) han sido formalmente certificados sobre hardware físico real en un microcontrolador **Microchip PIC16F628A** montado en el programador **PIC K150**.

### 🌐 Topología del Entorno de Hardware
```text
[Google Cloud Shell]
        ↕ (Túnel SSH Reverso: ssh -R 5555:localhost:PUERTO_MOVIL)
  [ADB Server / Client (localhost:5555)]
        ↕ (Depuración USB inalámbrica / Termux)
[Teléfono Físico: TECNO BF7 / SPARK Go 2023 (Android 12, ARM64)]
        ↕ (Conexión física USB-OTG Host Mode)
[Programador PIC K150 (Chip USB-Serial Prolific PL2303 - VID: 067b, PID: 2303)]
        ↕ (Zócalo ZIF de 40 pines - Pin 1 en Pin 2 del ZIF, palanca trabada)
[Microcontrolador Target: Microchip PIC16F628A (18 Pines DIP, Device ID 0x6810)]
```

### ⚡ Ciclo de Comandos Rápidos para Agentes
Para inspeccionar, depurar y operar el dispositivo físico desde Cloud Shell o terminal:
1. **Comprobar estado del teléfono y batería:**
   ```bash
   adb-phone status
   # o: adb -s localhost:5555 devices -l
   ```
2. **Conectar/Reconectar túnel ADB:**
   ```bash
   adb-phone connect
   ```
3. **Verificar enumeración del programador USB (PL2303):**
   ```bash
   adb -s localhost:5555 shell dumpsys usb | grep -E "mName|mVendorId|mProductId"
   ```
4. **Monitorear tramas de comunicación y logs del protocolo:**
   ```bash
   adb -s localhost:5555 logcat -s ProtocoloP18A:V USB_SERIAL:V UsbService:V
   ```
5. **Inspección de UI y Automatización Headless (sin display):**
   ```bash
   # Volcar jerarquía de vistas XML
   adb -s localhost:5555 shell uiautomator dump /sdcard/window_dump.xml
   adb -s localhost:5555 pull /sdcard/window_dump.xml /tmp/

   # Tocar botón o elemento por coordenadas calculadas [X, Y]
   adb -s localhost:5555 shell input tap <X> <Y>
   ```
6. **Capturas de pantalla remotas:**
   ```bash
   adb-phone screenshot docs/capturas_hardware_k150/nueva_captura.png
   ```

### 📚 Documentación de Referencia y Evidencias
* **Guía Operativa Exhaustiva:** [`GUIA_PRUEBAS_HARDWARE_REAL_K150.md`](file:///home/danielpdiamon/PIC-k150-Programing/GUIA_PRUEBAS_HARDWARE_REAL_K150.md) (manual completo para reproducir las 7 pruebas de hardware sin pantalla física).
* **Reporte Técnico de Validación:** [`REPORTE_VALIDACION_HARDWARE_PIC16F628A.md`](file:///home/danielpdiamon/PIC-k150-Programing/REPORTE_VALIDACION_HARDWARE_PIC16F628A.md) (certificación formal de tramas, Device ID 0x6810, lectura/escritura y fuses).
* **Galería de Capturas de Pantalla:** [`docs/capturas_hardware_k150/`](file:///home/danielpdiamon/PIC-k150-Programing/docs/capturas_hardware_k150/) (10 evidencias visuales del ciclo completo de validación física: permisos USB, detección de socket con Device ID 0x6810, volcado ROM/EEPROM, borrado, verificación post-erase, programación de firmware y verificación bit a bit de fuses/código).
* **Guía de Mejoras Técnicas:** [`PROPUESTA_AUTODETECCION_CHIP_K150.md`](file:///home/danielpdiamon/PIC-k150-Programing/PROPUESTA_AUTODETECCION_CHIP_K150.md) (arquitectura de autodetección de modelo PIC por Device ID, control USB, diagnóstico de hardware y calibración, 100% implementada en v2.9.0).
* **Notas de Lanzamiento v2.9.0:** [`RELEASE_NOTES_v2.9.0.md`](file:///home/danielpdiamon/PIC-k150-Programing/RELEASE_NOTES_v2.9.0.md) (detalle técnico bilingüe y registro de cambios para Google Play Console).

