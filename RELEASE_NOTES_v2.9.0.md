# Notas de Lanzamiento - PIC-k150-Programing v2.9.0 (versionCode 53)

Fecha de lanzamiento / Release Date: 10 de Octubre de 2026 / October 10, 2026

---

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- 🎯 Autodetección automática de microcontroladores PIC por Device ID en silicio con sincronización instantánea de zócalo y parámetros.
- 🔌 Control manual de conexión/desconexión USB en la barra superior con reconexión transparente sin re-solicitar permisos OTG.
- 🔓 Desacoplamiento de operaciones de hardware: Lectura (Dump), Borrado, Blank Check y Detección ahora disponibles directamente sin requerir archivo HEX previo.
- 🎨 Rediseño visual 3D de alta fidelidad: Zócalo verde Textool K150, palanca de bloqueo, conteo dinámico de pines, llenado lumínico y haz láser de barrido.
- 🛡️ Preservación automática de calibración interna de fábrica OSCCAL (PIC12F/PIC16F) y soporte completo para la familia PIC10F (Comando 0x18).
- 🛠️ Vector de Depuración ICD (24 bits) para inspección y parcheo en gamas medias y PIC18F.
- 🩺 Suite de Diagnóstico de Hardware K150: Telemetría de firmware/protocolo, test de latencia de eco y control de voltajes VPP (13V) / VDD (5V) con fail-safe para técnicos.
- 🎛️ Grabación selectiva (Completa, Solo ROM, Solo EEPROM, Solo Fuses), Blank Check Dual detallado y Editor Hexadecimal interactivo en vivo.

### English (`en-US`)
- 🎯 Automatic silicon Device ID chip detection with instantaneous socket mapping and parameter configuration.
- 🔌 Manual USB Connect/Disconnect button on toolbar with seamless reconnection without re-prompting OTG permissions.
- 🔓 Decoupled hardware workflow: Read Dump, Bulk Erase, Blank Check, and Socket Detect now accessible without loading a HEX file first.
- 🎨 High-fidelity 3D visual redesign: Official Textool green ZIF socket, locked lever, dynamic pin count, memory fill glow, and laser scan.
- 🛡️ Automatic factory OSCCAL calibration word preservation for PIC12F/PIC16F and full PIC10F 6-pin family support (Command 0x18).
- 🛠️ In-Circuit Debug (ICD) 24-bit vector support for mid-range and PIC18F microcontrollers.
- 🩺 K150 Hardware Diagnostic Suite: Firmware/protocol telemetry, serial echo latency benchmark, and manual VPP (13V)/VDD (5V) voltage testing with fail-safe shutoff.
- 🎛️ Modular Selective Programming (Full, ROM-Only, EEPROM-Only, Fuses-Only), Dual Blank Check report, and Live Interactive Hex Code Editor.

---

## 🛠️ Detalle Técnico de Cambios

1. **Autodetección de Silicio por Device ID (`ChipinfoReader`, `ProtocoloP18A`, `PicProgrammingManager`)**:
   - Lectura de bloque de configuración con Comando 13 (`0x0D`).
   - Normalización Big-Endian y enmascaramiento de los 5 bits de revisión de Microchip (`rawWord & 0xFFE0`).
   - Índice inverso en memoria `deviceIdMap` construido durante la carga de `chipinfo.cid`.
   - Sincronización automática del `Spinner`, imagen de socket y variables de programación.

2. **Control de Conexión USB y Desacoplamiento de Hardware (`UsbConnectionManager`, `MainActivity`)**:
   - Botón `btnConnectUsb` en Toolbar.
   - Reconexión transparente sin relanzar `PendingIntent` de permisos mientras el dispositivo siga conectado físicamente.
   - Operaciones de hardware no destructivas y de borrado disponibles de inmediato tras enchufar el programador.

3. **Rediseño 3D de Alta Fidelidad (`PicAnimationView`, `ProgrammingDialogManager`)**:
   - Paleta de color oficial verde Textool K150 (`#0F7A4D`), relieves 3D y ranuras niqueladas.
   - Palanca mecánica cromada trabada en posición cerrada con perilla plástica esférica.
   - Proporción dinámica de encapsulado y número de pines según el chip (8, 14, 18, 28, 40 pines).
   - Efecto visual de llenado de memoria y haz láser móvil durante el progreso (0-100%).
   - Halo verde resplandeciente de éxito tras completar la programación.

4. **Preservación de OSCCAL y Familia PIC10F (`CalibracionManager`)**:
   - Lectura previa del valor `RETLW xx` de fábrica antes de cualquier comando de borrado masivo.
   - Restauración automática del valor de calibración tras el ciclo de borrado.
   - Soporte dedicado para arquitectura PIC10F (`CoreType=NewF12B`, 10F200..222) con registros de calibración y respaldo mediante Comando 24 (`0x18`).
   - Diálogo modal interactivo para consultar, editar y reprogramar valores OSCCAL.

5. **Vector de Depuración ICD (`CalibracionManager`, `ProtocoloP18A`)**:
   - Lectura (Comando 23) y escritura (Comando 22) del vector de depuración de 24 bits.
   - Diálogo modal accesible desde el menú de la Toolbar.

6. **Suite de Diagnóstico de Hardware K150 (`DiagnosticoHardwareManager`)**:
   - Consulta al firmware: Modelo (Comando 20), Protocolo (Comando 21), prueba de eco con latencia (Comando 2) y sensor de zócalo ZIF (Comando 17/18).
   - Control de voltajes VPP (13V) y VDD (5V) para diagnóstico con multímetro, con apagado de seguridad automático al cerrar el diálogo.

7. **Programación Selectiva, Blank Check Dual y Editor Hexadecimal (`ProgramacionSelectivaManager`, `HexEditorManager`)**:
   - Selector de grabación: Completa, Solo ROM, Solo EEPROM, Solo Fuses.
   - Blank Check Dual detallado reportando ROM y EEPROM por separado.
   - Editor hexadecimal en vivo interactivo al hacer clic sobre cualquier fila del visor de memoria, con recálculo de checksum en formato Intel HEX.

8. **Monetización Híbrida Ética (Pase Pro 12 Horas y Google Play Billing)**:
   - Integración de Google Play Billing 9.1.0 para compra in-app permanente (`remove_ads_pro`) a $4.99 / $5.00 USD.
   - Pase Pro temporal de 12 horas desbloqueable con AdMob Rewarded Video (`ca-app-pub-5141499161332805/7291519778`).
   - Cero anuncios durante el tiempo Pro activo (ocultamiento dinámico del banner).
   - Protección estricta de funciones gratuitas: Grabación de silicio (ROM, EEPROM, Fuses), Detección, Borrado masivo, Blank Check y Exportación de Dumps son 100% libres. Solo 4 herramientas avanzadas (Editor Hexadecimal, Diagnóstico de Hardware y test de voltajes, Vector ICD y calibración manual OSCCAL) requieren Pase Pro.

9. **Certificación y Cobertura de Pruebas**:
   - 100% de la suite de pruebas unitarias y de integración contra el emulador virtual K150 en C++ y Python (`./ejecutar_pruebas_con_emulador.sh`).
