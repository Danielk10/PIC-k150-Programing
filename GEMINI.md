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
13. **Robustez de Autodetección y Sincronización de Spinner en UI:**
   * Se solucionó la limitación nativa de Android donde `Spinner.setSelection(index)` no dispara `onItemSelected` si el índice coincide con el valor previo, invocando explícitamente `selectChip(modelo)` para sincronizar modelo, visualización del zócalo ZIF y parámetros de memoria.
   * Se implementó un fallback automático en `PicProgrammingManager`: si `detectarPicEnElSocket()` o `leerDeviceIDDelSocket()` fallan tras haber seleccionado manualmente un chip con pines/parámetros incompatibles en el Spinner, se reconfiguran las variables al perfil estándar de 14-bit y se reintenta la lectura de Device ID.
   * Se extendió el timeout de espera del ack de VPP (`'C'`) de 100ms a 500ms (`TIMEOUT_EXTENDED`) con apagado fail-safe de voltajes en bloque `finally`.
14. **Reconexión USB en Vivo sin Desconectar Cable OTG:**
   * `iniciarProtocolo()` incorpora una secuencia determinista de 3 reintentos (`clearBuffer() -> 0x01 -> 'Q' -> 'P' -> 'P'`) que recupera la máquina de estados del firmware K150 aun cuando el microcontrolador permanezca encendido vía el bus de alimentación VBUS del puerto USB-OTG.
   * Las operaciones de apertura, handshake y cierre del puerto USB en `UsbConnectionManager` se trasladaron a un hilo en segundo plano (`UsbWorkerThread`), evitando cualquier bloqueo en la interfaz de usuario.
15. **Persistencia Híbrida del Pase Pro de 12 Horas (Firebase Firestore + Respaldo Local Firmado):**
   * Sincronización en la nube con **Cloud Firestore** (`pro_passes/{deviceId}`) dentro del plan gratuito Spark de Firebase (50.000 lecturas y 20.000 escrituras diarias).
   * Al desinstalar y reinstalar la app, el pase activo de 12 horas se consulta y restaura automáticamente desde Firestore en el arranque mediante `OnPassRestoredListener`, ocultando banners publicitarios y habilitando el menú Pro de inmediato.
   * Cuenta con doble salvaguarda offline: archivo firmado con hash HMAC-SHA256 (`Documents/.k150_pro_pass.dat`) y Google Cloud Auto Backup (`pro_pass_prefs.xml`).
16. **Resolución de Bloqueo y Desincronización en Diagnóstico de Hardware K150:**
   * Se identificó que el Comando 19 (`detectarSiEstaFueraElPicDelSocket()`) en el firmware K150 es un bucle bloqueante que espera la extracción física del microcontrolador; al ejecutarse con un chip insertado bloqueaba el microcontrolador y rompía la máquina de estados serial. Se desacopló de la rutina diagnóstica automática.
   * Se robusteció `obtenerProtocoloDelProgramador()` (Comando 21) sanitizando respuestas no-ASCII (`0xFF`) de clones físicos para evitar desbordes y cadenas corruptas.
   * Se añadieron purgas de búfer (`clearBuffer()`) previas y posteriores a eco/diagnóstico y se implementó un cerrojo atómico (`AtomicBoolean voltajesActivos`) en `DiagnosticoHardwareManager`, eliminando colisiones por hilos concurrentes al cerrar el diálogo.
   * Se validó en hardware real: la prueba de eco responde en ~420ms, el diagnóstico completo finaliza con éxito y las operaciones posteriores en la UI (como "Detectar PIC" reconociendo el `16F628A`) operan con total normalidad sin requerir reconexión OTG.
17. **Implementación de Comandos de Zócalo 18 y 19, Feedback Háptico y Notificaciones con Sonido:**
   * Se creó [`NotificacionVibracionManager.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/main/java/com/diamon/managers/NotificacionVibracionManager.java) con soporte para vibración háptica (`vibrarAlerta()`, `vibrarExito()`) y notificaciones con sonido del sistema en canal de alta prioridad.
   * **Protección contra grabación en vacío:** Antes de iniciar la programación (en modo ZIF), se valida con el Comando 18 si el microcontrolador está físicamente en el zócalo. Si está ausente, vibra con alerta, emite sonido y muestra modal avisando que inserte el PIC y trabe la palanca.
   * **Finalización de programación:** Al terminar con éxito, vibra con patrón de éxito, emite notificación con sonido y en el diálogo de fin de proceso monitorea con el Comando 19 (`esperarExtraccionDelSocket`) para confirmar la extracción segura del microcontrolador.
   * **Seguridad de voltajes en Diagnóstico:** Al pulsar `VPP (13V)`, si hay un chip en el zócalo, solicita su extracción previa y espera confirmación con Comando 19 antes de activar 13V para no dañar el componente.
   * **Asistente de sensores ZIF:** Se añadió la opción "🧪 Probar Sensores ZIF" en Diagnóstico para probar interactivamente la inserción (Cmd 18) y extracción (Cmd 19).
18. **Optimización de Latencia y Corrección de Borrado Masivo en Silicio:**
   * **Causa raíz identificada:** En `borrarMemoriasDelPic()`, el timeout para esperar `'Y'` tras el pulso de borrado físico (`0x0E`) estaba configurado en 500ms (`TIMEOUT_EXTENDED`). En microcontroladores físicos reales (que borran simultáneamente Flash, EEPROM y Fuses) con la latencia del enlace USB-OTG, la respuesta suele demorar entre 550ms y 800ms. Esto provocaba una excepción de timeout prematuro en el primer intento (`❌ Error al borrar memoria`), a pesar de que el chip sí se borraba físicamente, obligando al usuario a pulsar varias veces.
   * **Solución de timeout para borrado:** Se definió una constante dedicada `TIMEOUT_ERASE = 3000` (3000ms) para la confirmación de borrado masivo. Al ser no-bloqueante en recepción, la operación retorna inmediatamente en cuanto llega `'Y'` en el primer intento sin falsos errores.
   * **Tolerancia en voltajes VPP:** Se configuraron `activarVoltajesDeProgramacion()` y `desactivarVoltajesDeProgramacion()` con `TIMEOUT_EXTENDED` (500ms) en lugar de 100ms para admitir transitorios y estabilización de la línea de 13V, y se agregó un bloque `finally` en `borrarMemoriasDelPic()` para garantizar que los voltajes siempre se apaguen y los comandos se reseteen ante cualquier eventualidad.
   * **Eliminación de latencia en drenado de búfer:** En `clearBuffer()`, se ajustó el timeout de lectura de 100ms a 10ms. Dado que `enviarComando()` invoca `clearBuffer()` al inicio de cada instrucción, un búfer vacío bloqueaba 100ms en cada invocación, sumando más de 400ms de retraso acumulado por ciclo de borrado. Con 10ms, el drenado es instantáneo y se recupera la velocidad máxima del protocolo.
19. **Mejoras de UI, Detección de Inserción Automática, Diálogo de Grabación Clásico y Pase Pro:**
   * **Reorganización de UI:** El botón Conectar USB y el indicador de conexión se integraron dentro de la sección de opciones para despejar la Toolbar, permitiendo mostrar el nombre completo de la aplicación. Los botones «Programar PIC» y «Cargar Archivo HEX» se ubicaron uno al lado del otro manteniendo una distribución compacta y balanceada.
   * **Reseteo en Selección de Chip:** Al cambiar de microcontrolador en el Spinner (incluyendo re-seleccionar el mismo chip mediante `ReselectableSpinner`) o al ejecutar la autodetección por silicio, se limpian el HEX cargado y se deshabilitan inmediatamente los botones de programar y verificar.
   * **Detección y Espera Automática en Zócalo ZIF:** Para todas las operaciones de hardware (Programar, Leer, Borrar, Verificar, Blank Check), se verifica la presencia del PIC en el zócalo ZIF (Cmd 18). Si está ausente, se alerta con vibración y sonido, esperando automáticamente la inserción física del chip para iniciar la operación sin requerir clics adicionales.
   * **Sonido de Fin de Grabación:** Se reemplazaron las notificaciones de la barra de estado de Android por reproducción de sonido directo del sistema (`reproducirSonidoNotificacion()`) y vibración háptica de éxito.
   * **Diálogo de Grabación Selectiva Clásico:** Se garantizó que el diálogo modal se muestre siempre al pulsar programar, presentando «🚀 Grabación Completa» siempre en primer lugar con sus descripciones detalladas.
   * **Seguridad en Diagnóstico:** La prueba de voltajes (13V VPP) detecta si hay chip presente y bloquea la tensión hasta que el usuario retira el microcontrolador (Cmd 19).
20. **Internacionalización Completa (53 Idiomas), Sincronización Global y Cierre de Issues (v2.9.1):**
   * **Internacionalización total:** Se estandarizó el inglés como recurso predeterminado en `values/strings.xml` y `values-en/strings.xml`, certificando el español nativo en `values-es`, `values-es-rES` y `values-es-rVE`. Se coordinaron 3 subagentes de IA en lotes de 5 para traducir y sincronizar los 48 idiomas restantes, logrando paridad del 100% (450 claves en cada uno de los 53 archivos `strings.xml`) y eliminando 17 claves obsoletas.
   * **Corrección de ArrayIndexOutOfBoundsException en `Protocolo.readBytes` (Issue #68):** Se acotó la cantidad de bytes a copiar en `System.arraycopy` mediante `Math.min(bytesRead, Math.min(targetBuf.length, remaining))`, garantizando que buffers o paquetes mayores del endpoint USB nunca provoquen desbordamiento de índice.
   * **Revisión y Cierre de Issues en GitHub:** Se comentaron y cerraron los issues #68 (error en `readBytes`), #67 (timeout transitorio al detectar), #66 (reporte sobre protocolo obsoleto v1.5.8) y #10 (implementación del protocolo P18A), dejando únicamente el issue #11 abierto como roadmap de expansión de microcontroladores.
   * **Compilación y Lanzamiento:** Verificación con 104/104 pruebas JUnit contra el emulador C++ K150, compilación limpia de Debug (APK) y producción (APK y AAB) con `versionCode 54` y `versionName "2.9.1"`, y publicación en GitHub Releases (`v2.9.1`).
21. **Blindaje de Detección No Bloqueante en Zócalo ZIF, Grabación Parcial Dinámica y Copiado de Log C-PIC (v2.9.2):**
   * **Sonda No Bloqueante en Zócalo ZIF (`sondaPicPresenteEnSocket`):** Se erradicó el bloqueo del firmware K150 (Comando 18) cuando el zócalo está vacío o el chip no hace contacto; la app interroga primero el Device ID en silicio o el Comando 19 (<15ms) evitando que el microcontrolador K150 se congele en bucles infinitos en ensamblador.
   * **Prueba Dinámica de Sensores ZIF Segura:** Se actualizaron `esperarInsercionEnElSocket()` y `esperarExtraccionDelSocket()` para utilizar sondeo no bloqueante en bucle con cancelación inmediata sin riesgo de colgar el programador ni la app.
   * **Restauración de Grabación Parcial Dinámica:** Basada en la rama `backup-v2.8.5-estable`, el modal de programación selectiva filtra dinámicamente las opciones disponibles en función de las regiones presentes en el archivo HEX/BIN cargado (Flash ROM, EEPROM, Configuración).
   * **Copiado de Log al Portapapeles (Estilo C-PIC Compiler):** Se eliminó el copiado accidental por toques bruscos (`setOnClickListener`), reemplazándolo por pulsación prolongada (`setOnLongClickListener`), selección de texto (`textIsSelectable`) y protección de foco en el ScrollView (`blocksDescendants`), con 100% de paridad de cadenas en los 53 idiomas.

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
* **Galería de Capturas de Pantalla:** [`docs/capturas_hardware_k150/`](file:///home/danielpdiamon/PIC-k150-Programing/docs/capturas_hardware_k150/) (Evidencias visuales del ciclo completo de validación física: permisos USB, detección en socket con Device ID 0x6810, volcado ROM/EEPROM, diagnóstico de hardware sin desincronización, seguridad de voltajes VPP 13V con Comando 19 y prueba interactiva de sensores ZIF).
* **Guía de Mejoras Técnicas:** [`PROPUESTA_AUTODETECCION_CHIP_K150.md`](file:///home/danielpdiamon/PIC-k150-Programing/PROPUESTA_AUTODETECCION_CHIP_K150.md) (arquitectura de autodetección de modelo PIC por Device ID, control USB, diagnóstico de hardware y calibración, 100% implementada en v2.9.0).
* **Notas de Lanzamiento v2.9.0:** [`RELEASE_NOTES_v2.9.0.md`](file:///home/danielpdiamon/PIC-k150-Programing/RELEASE_NOTES_v2.9.0.md) (detalle técnico bilingüe y registro de cambios para Google Play Console).
* **Notas de Lanzamiento v2.9.1:** [`RELEASE_NOTES_v2.9.1.md`](file:///home/danielpdiamon/PIC-k150-Programing/RELEASE_NOTES_v2.9.1.md) (internacionalización global a 53 idiomas, resolución de issues de GitHub y blindaje de buffers USB).

