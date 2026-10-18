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
   * Implementación completa de la suite avanzada K150:
     - Autodetección automática de PIC por Device ID en silicio (Flash).
     - Botón Conectar/Desconectar USB en UI con reconexión transparente sin re-pedir permisos OTG.
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
   * **Seguridad de voltajes en Diagnóstico:** Al pulsar `13V/5V`, si hay un chip en el zócalo, solicita su extracción previa y espera confirmación con Comando 19 antes de activar 13V para no dañar el componente.
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
   * **Seguridad en Diagnóstico:** La prueba de voltajes (`13V/5V`) detecta si hay chip presente y bloquea la tensión hasta que el usuario retira el microcontrolador (Cmd 19).
20. **Internacionalización Completa (53 Idiomas), Sincronización Global y Cierre de Issues (v2.9.1):**
   * **Internacionalización total:** Se estandarizó el inglés como recurso predeterminado en `values/strings.xml` y `values-en/strings.xml`, certificando el español nativo en `values-es`, `values-es-rES` y `values-es-rVE`. Se coordinaron 3 subagentes de IA en lotes de 5 para traducir y sincronizar los 48 idiomas restantes, logrando paridad del 100%.
   * **Corrección de ArrayIndexOutOfBoundsException en `Protocolo.readBytes` (Issue #68):** Se acotó la cantidad de bytes a copiar en `System.arraycopy` mediante `Math.min(bytesRead, Math.min(targetBuf.length, remaining))`, garantizando que buffers o paquetes mayores del endpoint USB nunca provoquen desbordamiento de índice.
   * **Revisión y Cierre de Issues en GitHub:** Se comentaron y cerraron los issues #68 (error en `readBytes`), #67 (timeout transitorio al detectar), #66 (reporte sobre protocolo obsoleto v1.5.8) y #10 (implementación del protocolo P18A), dejando únicamente el issue #11 abierto como roadmap de expansión de microcontroladores.
21. **Blindaje de Detección No Bloqueante en Zócalo ZIF, Grabación Parcial Dinámica y Copiado de Log C-PIC (v2.9.2):**
   * **Sonda No Bloqueante en Zócalo ZIF (`sondaPicPresenteEnSocket`):** Se erradicó el bloqueo del firmware K150 (Comando 18) cuando el zócalo está vacío o el chip no hace contacto; la app interroga primero el Device ID en silicio o el Comando 19 (<15ms) evitando que el microcontrolador K150 se congele en bucles infinitos en ensamblador.
   * **Prueba Dinámica de Sensores ZIF Segura:** Se actualizaron `esperarInsercionEnElSocket()` y `esperarExtraccionDelSocket()` para utilizar sondeo no bloqueante en bucle con cancelación inmediata sin riesgo de colgar el programador ni la app.
   * **Restauración de Grabación Parcial Dinámica:** Basada en la rama `backup-v2.8.5-estable`, el modal de programación selectiva filtra dinámicamente las opciones disponibles en función de las regiones presentes en el archivo HEX/BIN cargado (Flash ROM, EEPROM, Configuración).
   * **Copiado de Log al Portapapeles (Estilo C-PIC Compiler):** Se eliminó el copiado accidental por toques bruscos (`setOnClickListener`), reemplazándolo por pulsación prolongada (`setOnLongClickListener`), selección de texto (`textIsSelectable`) y protección de foco en el ScrollView (`blocksDescendants`), con 100% de paridad de cadenas en los 53 idiomas.
22. **Rediseño Profesional de UI/Diálogos, Editor Hexadecimal en Tarjetas, Limpieza de Paréntesis y Enlaces de Ecosistema:**
   * **Rediseño del Editor Hexadecimal (`HexEditorManager.java`):** Se transformó el diálogo en una interfaz estructurada y moderna basada en 3 tarjetas sobre fondo oscuro (`#181824`): métricas de memoria (Dirección, Valor Actual, Límite Máximo), desglose técnico con badges para Byte Bajo y Byte Alto (orden Little-Endian) y campo de entrada hexadecimal centrado 20sp monospace con borde `#00E676`.
   * **Erradicación de Paréntesis en UI, Diagnóstico y Pase Pro:** Se eliminaron paréntesis informales en todas las etiquetas de la app (`VPP 13V`, `Apagar 0V`, `Voltajes VPP 13V y VDD 5V activados`, `Palabra`, `Blank`, `24 bits`, etc.) logrando una apariencia estética profesional.
   * **Diálogo «Acerca de / Licencias» y README:** Se integraron enlaces directos a las tres aplicaciones del desarrollador en Google Play (**C PIC Compiler** `com.diamon.ptc`, **Flash SPI Tool** `com.diamon.curso` y **EEPROM Flasher** `com.diamon.mini`) y se alinearon todas las referencias y licencias (GPL-3.0, Apache-2.0, MIT) con el `README.md`.
   * **Paridad del 100% en 53 Idiomas:** Se sincronizaron exactamente 470 claves en todas las 53 variantes de `strings.xml`.
   * **SDK/NDK de Android:** Instalado y verificado en `/tmp/android-sdk` mediante `setup-sdk.sh`.
23. **Sincronización Total Multilingüe (53 Idiomas x 470 Claves), Botón 13V/5V, Selector de Protocolo y Modelo del Programador:**
   * **Selector de Protocolo con RadioButtons de Alto Contraste:** Rediseño del modal en tarjetas oscuras `#222234` con RadioButtons tintados `#00E676` (seleccionado) y `#B0BEC5` (no seleccionado), mostrando únicamente el nombre limpio del protocolo (`P18A`, `P018`, `P016`, `P014`).
   * **Aviso en «Modelo del Programador» cuando está desconectado:** `showProgrammerModelDialog()` informa explícitamente `Programador no conectado` (`diag_programmer_not_connected`) mediante diálogo, Toast y consola cuando no hay un programador conectado por USB, consistente con el resto del menú.
   * **Renombrado de Botón de Diagnóstico a `13V/5V`:** Actualización en las 53 variantes de recursos para reflejar con precisión la alimentación simultánea de VPP (13V) y VDD (5V).
   * **Cero Cadenas Hardcodeadas y Paridad Total:** Eliminación de cadenas literales en Java (`MainActivity.java`), unificando todos los mensajes a través de `getString(R.string.*)` con 470 claves sincronizadas al 100% en los 53 idiomas.
24. **Auditoría C++/Java, Blindaje Defensivo y Limpieza del Repositorio:**
   * **Emulador C++ (`emulador_k150.cpp`):** Incorporación de `write_all()` para escrituras PTY completas sin advertencias (`-Wall -Wextra`), validación de límites antes de `std::memcpy` en ROM/EEPROM y cierre seguro de `slave_fd` ante errores de symlink.
   * **Blindaje Java (`UsbConnectionManager.java` y `ProPassManager.java`):** Validación nula sobre lista de drivers y retorno de `usbManager.openDevice()`, y uso de `try-with-resources` en lectura/escritura del respaldo local firmado.
   * **Limpieza de Archivos Obsoletos:** Eliminación de notas de versión antiguas (`RELEASE_NOTES_v2.8.3` a `v2.9.1`), propuestas ya implementadas (`PROPUESTA_AUTODETECCION_CHIP_K150.md`), arnés manual obsoleto (`pruebas_locales/`), scripts de pre-lanzamiento en desuso y paquete muerto `com.diamon.tutorial.utilidadestutorial`.
25. **Resolución de Crash en Editor Hexadecimal (Issue #69), Reprogramación Desacoplada y Estado Pro Permanente:**
   * **Corrección de Crash en Editor Hexadecimal (#69):** Se corrigió la discrepancia en el formato de recursos `hex_editor_byte_high` sustituyendo `%2$s` por `%1$s` y saneando las descripciones y apóstrofes sin escapar en todos los idiomas, alcanzando 473 claves con 100% de paridad en las 53 variantes de `strings.xml`.
   * **Detección Táctil por Columnas y Refresco Visual en Tiempo Real:** `MemoryDisplayManager` ahora detecta con precisión el toque sobre cada palabra ROM o byte EEPROM según su posición X (0 a 7) y refresca de forma inmediata la fila con sus colores de sintaxis y columna ASCII tras guardar la edición.
   * **Botón «Reprogramar» y Desacoplamiento de Flujos de Grabación:** Se agregó el botón naranja «Reprogramar» dentro de la ventana de lectura de memoria para grabar el volcado editado. Se blindó la separación de datos (`originalLoadedFirmware` y `originalLoadedDatosPic`) garantizando que pulsar el botón principal «Programar PIC» programe siempre el archivo HEX/BIN original cargado desde el almacenamiento sin sufrir alteraciones por las ediciones realizadas en el lector.
   * **Manejo Exclusivo de K150 Pro Permanente:** Cuando el usuario ya dispone de la compra permanente ($4.99), tocar el componente en la barra superior ya no muestra ofertas de compra ni anuncios; ahora despliega un diálogo informativo confirmando su estado permanente activo (`pro_permanent_active_title` y `pro_permanent_active_desc`).
   * **Validación Total con Emulador Virtual y Lanzamiento:** Las 114 pruebas unitarias y de integración Java pasaron al 100% contra el emulador virtual K150 en C++ (`./ejecutar_pruebas_con_emulador.sh cpp`), compilando y actualizando los binarios firmados (`.apk` y `.aab`) en el release `v2.9.2` y cerrando formalmente el Issue #69 en GitHub.
26. **Aislamiento Estricto de Reprogramación y Botón Principal de Grabación:**
   * **Desacoplamiento total entre volcado de memoria y firmware original:** Se eliminó la sobreescritura accidental de la variable `firmware` en lecturas de memoria y ediciones en el visor (eliminando la función `sincronizarFirmwareDesdeMemoriaLeida`).
   * **Comportamiento determinista del botón principal «Programar PIC»:**
     - Si NO hay archivo HEX/BIN cargado desde disco: Los botones «Programar PIC» y «Verificar Memoria» permanecen estrictamente deshabilitados e inactivos al salir del lector de memoria, y cualquier intento de invocación es rechazado defensivamente.
     - Si SÍ hay un archivo HEX/BIN cargado desde disco: Los botones principales se mantienen activos y programan **única y exclusivamente el archivo original cargado**, sin ser contaminados ni reemplazados por lecturas del microcontrolador ni modificaciones manuales realizadas en el visor de memoria.
   * **Flujo dedicado para «Reprogramar»:** El botón naranja dentro del lector de memoria genera dinámicamente su propio volcado independiente (`hexReprogramar` y `datosReprogramar`) para programar el chip en el zócalo, restaurando al finalizar el estado legítimo de los botones de la pantalla principal según la presencia del archivo original cargado.
   * **Seguimiento Dinámico de Verificación (`lastProgrammedDatosPic`):** El botón «Verificar Memoria» detecta y compara automáticamente contra el último firmware efectivamente grabado en el chip (bien sea la memoria modificada reprogramada o el archivo original cargado). Si se vuelve a grabar el archivo original, la verificación conmuta automáticamente a dicho archivo original, permitiendo verificar en todo momento lo que realmente se grabó en el silicio.
27. **Resolución de Error de Borrado tras Edición Hexadecimal y Corrección de Endianness de Fuses/User ID:**
   * **Causa raíz del fallo de borrado (10%):** Al reentrar en el flujo de programación tras editar celdas en el visor hexadecimal, las variables del firmware K150 en memoria RAM (`EraseMode` y `PowerSequence`) no estaban inicializadas en el microcontrolador del programador, provocando que el comando de borrado masivo (Comando 14) fuera rechazado o fallara con timeout en silicio real. Se incorporó la inicialización explícita `protocolo.iniciarVariablesDeProgramacion(chipPIC)` (Comando 3) previo a `borrarMemoriasDelPic()` en `PicProgrammingManager` (`programChip`, `programRomOnly`, `programEepromOnly`, `programConfigOnly`, y en la sobrecarga `eraseMemory(chip)`).
   * **Formateo de Fuses y User ID en Reconstrucción Intel HEX:** En `HexEditorManager.java` y `MainActivity.java`, se corrigió la generación de bloques de fuses y User ID: los bytes de fuses leídos con el Comando 13 ya se encuentran en orden Little-Endian conforme al estándar INHX32 y no deben invertirse con `formatForHexExport` (lo que alteraba la protección de código), y para núcleos de 12/14 bits el User ID se estructura a 8 bytes INHX32 (`[LSB = rawId[i], MSB = 0x00]`).
   * **Aislamiento Estricto de Memoria Editada:** Al guardar una celda editada en el visor hexadecimal, se actualiza el volcado en memoria de trabajo (`firmware` y `datosPicProcesados`) para reprogramación desde el visor, pero se preservan estrictamente intactos `originalLoadedFirmware` y `originalLoadedDatosPic`. Esto garantiza que el archivo HEX/BIN cargado desde disco nunca se altere y que el botón principal «Programar PIC» permanezca deshabilitado si no se ha cargado un archivo externo.
   * **Pruebas de Integración y Emulación:** Se expandieron los tests unitarios e integrados (`CalibracionManagerTest`, `HexEditorManagerTest`, `ProtocoloP18AIntegrationTest`), validando el ciclo completo de lectura, edición de celda ROM/EEPROM, reprogramación en silicio emulado y verificación final con 115/115 pruebas exitosas contra el emulador virtual en C++.
28. **Compatibilidad y Enmascaramiento por Arquitectura en Funciones Pro (OSCCAL e ICD):**
   * **Aviso Toast de Incompatibilidad:** Al pulsar «Leer» o «Grabar» en el diálogo de Calibración OSCCAL o en el de Vector de Depuración ICD (24 bits), se valida la compatibilidad del microcontrolador en zócalo. Si el chip no admite la función (ej. PIC16F628A para OSCCAL o PIC10F/núcleos de 12 bits para ICD), se dispara un Toast explicativo (`chip_no_compatible_funcion`) indicando que el microcontrolador no es compatible con dicha función. Si es compatible, la operación procede con normalidad.
   * **Enmascaramiento por Arquitectura de Núcleo:** Los valores de calibración (OSCCAL) leídos o ingresados se enmascaran automáticamente según la arquitectura de la palabra de instrucción del núcleo:
     - **Núcleo de 14 bits (PIC12F/PIC16F):** Enmascarado con `0x3FFF` (evita mostrar erróneamente `0xFFFF` en chips como el PIC16F628A, reflejando fielmente la palabra de 14 bits `0x3FFF`).
     - **Núcleo de 12 bits (PIC10F / Baseline):** Enmascarado con `0x0FFF`.
     - **Núcleo de 16 bits (PIC18F):** Enmascarado con `0xFFFF`.
   * **Sincronización Total en 53 Idiomas (474 Claves):** Integración de `chip_no_compatible_funcion` en las 53 variantes de `strings.xml` con paridad del 100% en todas las claves.
29. **Lanzamiento Mayor v2.9.3 (versionCode 57, minSdkVersion 24, Optimización R8 DEX):**
   * **Play Automatic Protection:** Actualización de `minSdkVersion` a 24 para dar soporte nativo a las defensas automáticas de Google Play contra piratería y manipulación.
   * **Optimización DEX y Ofuscación R8 (Android Vitals):** Habilitación de `minifyEnabled true` y `shrinkResources true` con reglas exhaustivas en `proguard-rules.pro`, reduciendo el peso del APK de 21 MB a 15 MB y superando el umbral de ofuscación del 25% de Google Play Vitals.
   * **Versionado:** Incremento a `versionCode 57` y `versionName "2.9.3"`.
30. **Lanzamiento Mayor v2.9.4 (versionCode 58, Blindaje de Facturación Google Play Billing 9.1.0 y Reintentos Dinámicos):**
   * **Compatibilidad Dual de Productos In-App:** Soporte simultáneo para `remove_ads_pro` y `remove_ads`, asegurando coincidencia total con la configuración de Google Play Console.
   * **Consultas Silenciosas en Segundo Plano (Estilo C-PIC):** Se eliminó la notificación de errores de sondeo en la consola del programador (`Failed to load product details` / `Product details not available`), manteniéndola exclusivamente dedicada a operaciones de microcontroladores PIC.
   * **Reintento Dinámico en UI (`ProAccessDialog`):** Ante retrasos de propagación de Google Play, el diálogo Pro no se cierra prematuramente; reintenta la consulta en segundo plano y muestra un aviso Toast amigable sin interrumpir al usuario.
   * **Permiso Explícito:** Declaración de `com.android.vending.BILLING` en el AndroidManifest.
   * **Sincronización Total y Validación:** Superadas 116/116 pruebas automatizadas con el emulador en C++ (`ejecutar_pruebas_con_emulador.sh cpp`) y publicación directa a Google Play Store (producción) y GitHub Releases.
31. **Lanzamiento v2.9.5 (versionCode 59, Resolución de Crashlytics #8101c07c en ProxyBillingActivity, Blindaje WindowInsets y Actualización de Librerías):**
   * **Resolución de Crashlytics `#8101c07c307416da1819dc7553864c1b` (`ProxyBillingActivity.onCreate` NPE):** Se blindó `BillingManager.launchPurchaseFlow()` con detección de dispositivos de prueba automatizada de Google Play / Firebase Test Lab (`isRunningInTestLab`), validación de ciclo de vida de la `Activity` y `billingClient.isReady()` con reconexión automática, filtro anti-doble-toque (`LAUNCH_DEBOUNCE_MS = 2000ms`) y bloque `try-catch (RuntimeException)`.
   * **Blindaje de `WindowInsets` en `GestorPantalla.java` (Guía C-PIC):** Se incorporó `obtenerInsetsSeguros(WindowInsetsCompat)` con respaldo retrocompatible y captura de `Throwable` para evitar `NoSuchMethodError: WindowInsets.Type.systemOverlays()` en firmwares Android 14+.
   * **Actualización de Dependencias (`gradle/libs.versions.toml`):** Actualización a las últimas versiones estables: `agp 9.4.1`, `firebaseBom 35.0.2` (`firebase-analytics 24.0.0`, `firebase-firestore 27.0.1`, `firebase-messaging 26.0.0`, `firebase-crashlytics-ndk 21.0.0`), `firebaseCrashlytics 21.0.0` y `playServicesAds 25.5.0`.
   * **Validación Completa:** 121/121 pruebas automatizadas superadas (incluyendo 23 pruebas en `BillingManagerTest` y 22 pruebas de integración con el emulador K150 en C++).

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
* **Guía Operativa Exhaustiva:** [`GUIA_PRUEBAS_HARDWARE_REAL_K150.md`](file:///home/danielpdiamon/PIC-k150-Programing/GUIA_PRUEBAS_HARDWARE_REAL_K150.md) (manual completo para reproducir las pruebas de hardware sin pantalla física).
* **Guía de Pruebas con Emulador Virtual:** [`GUIA_PRUEBAS.md`](file:///home/danielpdiamon/PIC-k150-Programing/GUIA_PRUEBAS.md) (documentación de las 121 pruebas automatizadas JUnit + Emulador C++/Python).
* **Reporte Técnico de Validación:** [`REPORTE_VALIDACION_HARDWARE_PIC16F628A.md`](file:///home/danielpdiamon/PIC-k150-Programing/REPORTE_VALIDACION_HARDWARE_PIC16F628A.md) (certificación formal de tramas, Device ID 0x6810, lectura/escritura y fuses).
* **Galería de Capturas de Pantalla:** [`docs/capturas_hardware_k150/`](file:///home/danielpdiamon/PIC-k150-Programing/docs/capturas_hardware_k150/) (Evidencias visuales del ciclo completo de validación física: permisos USB, detección en socket con Device ID 0x6810, volcado ROM/EEPROM, diagnóstico de hardware sin desincronización, seguridad de voltajes 13V/5V con Comando 19 y prueba interactiva de sensores ZIF).
* **Notas de Lanzamiento Vigentes (v2.9.5):** [`RELEASE_NOTES_v2.9.5.md`](file:///home/danielpdiamon/PIC-k150-Programing/RELEASE_NOTES_v2.9.5.md) (versionCode 59, resolución de Crashlytics en ProxyBillingActivity, blindaje WindowInsets y actualización de librerías).


