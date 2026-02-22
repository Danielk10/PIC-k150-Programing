# Análisis comparativo: `picpro` Python 3 vs implementación Java Android

## Alcance de la revisión
- Se comparó el protocolo de referencia en Python (`app/picpro-0.3.0.zip`, paquete `picpro`) con la implementación Java (`Protocolo` + `ProtocoloP018`).
- El objetivo fue identificar qué partes de la lógica ya están bien implementadas, qué falta, y qué conviene portar para soportar completamente los protocolos en tu clase `Protocolo`.
- **No se realizaron cambios funcionales al código Java**, solo este documento de análisis.

## Lo que tu implementación Java ya tiene bien (fortalezas)
1. **Diseño por abstracción/protocolo**
   - Tu clase `Protocolo` define un contrato amplio con métodos para prácticamente todos los comandos relevantes (ROM, EEPROM, fuses, configuración, borrado, blank check, debug vector, etc.).
2. **Separación de utilidades USB y parsing HEX**
   - Hay utilidades de bajo nivel como `readBytes`, `expectResponse`, `escribirDatosUSB`, más clases dedicadas de procesamiento (`DatosPicProcesados`, `HexProcesado`, `HexFileUtils`).
3. **Cobertura de comandos base P018**
   - Se observa implementación para: eco, init vars, voltajes, program ROM, program EEPROM, program ID/fuses, lectura ROM/EEPROM/config, erase, checks de blank, versión/protocolo y debug vector.
4. **Validaciones tempranas en operaciones críticas**
   - En ROM y EEPROM ya validas tamaño y casos inválidos al inicio.
5. **Alineación parcial con flujo del protocolo oficial**
   - En varios métodos haces secuencia de `reset` + voltajes + comando + respuesta + limpieza.

## Brechas principales contra Python de referencia

### 1) Falta una máquina de estados de sesión equivalente a Python
En Python hay estado explícito:
- `chip_info` cargado,
- `fuses_set`,
- chequeos `_need_chip_info()` y `_need_fuses()` antes de comandos dependientes.

En Java actualmente no hay un estado equivalente consolidado en `Protocolo` para impedir secuencias inválidas (ej. programar fuse commit sin haber cargado fuses).

**Impacto:** mayor probabilidad de comandos fuera de secuencia y fallos intermitentes difíciles de depurar.

### 2) Diferencia importante en `programarMemoriaROMDelPic`
Referencia Python exige que ROM sea múltiplo de 32 bytes; si no, lanza error y no programa.
Tu Java **auto-rellena con `0xFF`** hasta múltiplo de 32.

**Esto puede ser bueno o malo según firmware/chip**:
- bueno para tolerancia,
- riesgoso si el flujo esperado requiere tamaño exacto de entrada y validación estricta.

### 3) Inconsistencias en construcción/envío de comandos
En `Protocolo.enviarComando`, el parseo del `String comando` usa `Byte.parseByte(comando)` dentro del loop, en vez de parsear carácter por carácter o un único byte una sola vez.

**Impacto:** posibilidad de comportamiento no intencional cuando cambie el formato del comando.

### 4) Métodos con implementación pendiente o incompleta
Detectados stubs directos:
- `programarCalibracionDelPic(...)` devuelve `false`.
- `leerDatosDeCalibracionDelPic()` devuelve `null`.
- `programarDatosDeCalibracionDePics10F()` devuelve `false`.

**Impacto:** no hay paridad con la referencia para calibración/10F.

### 5) Diferencias en comandos de blank-check ROM
En Python `rom_is_blank` usa comando `15` y espera secuencia de bytes (`B`, `Y`, `N`, `C`) con conteo esperado de `B`.
En Java `verificarSiEstaBarradaLaMemoriaROMDelDelPic` envía bytes `{0x10, 0x3F}` y evalúa `0xFF`, `Y`, `N`, `C`.

**Posible desalineación:** comando/semántica distinta al protocolo documentado por la referencia.

### 6) Lecturas sin timeout acumulado robusto en algunos métodos
Python usa `_read(count, timeout)` acumulando hasta completar bytes o timeout real.
En Java, algunos métodos sí usan `readBytes(...)` (bien), pero otros leen con `usbSerialPort.read(buffer, 100)` en bucle sin control temporal total estricto.

**Impacto:** riesgo de lecturas parciales y respuestas truncadas tratadas como válidas.

### 7) Falta de verificación fuerte post-programación al nivel de `picpro`
Python en `program_pic`:
- lee config,
- parchea calibración si aplica,
- borra,
- programa,
- verifica ROM/EEPROM byte a byte,
- si core=16, hace `program_18fxxxx_fuse` (commit final).

Java tiene una verificación posterior más limitada (por ejemplo ROM lectura informativa) y no replica todo el pipeline de verificación completa.

### 8) Falta de manejo explícito de DTR/arranque de programador
Python hace reset y autodetección de polaridad DTR en `reset()`.
Java no refleja este handshake de forma equivalente en la capa de protocolo.

**Impacto:** algunos programadores/firmwares pueden ser más sensibles al arranque de sesión.

### 9) Manejo de errores: Python falla rápido con excepciones semánticas
Python diferencia `InvalidValueError`, `InvalidResponseError`, `InvalidCommandSequenceError`.
Java tiende a capturar `Exception` y retornar `false` en muchos puntos.

**Impacto:** se pierde granularidad diagnóstica para soporte técnico.

## Qué tiene mejor tu Java frente al Python (en el contexto Android)
1. **Arquitectura integrada con app Android (Managers + UI thread)**
   - Está adaptado al entorno móvil, cosa que la CLI Python no necesita.
2. **Procesamiento HEX desacoplado en clases utilitarias internas**
   - `DatosPicProcesados` y `HexFileUtils` ya te dan base para reutilizar lógica en distintos protocolos.
3. **Contrato abstracto amplio (`Protocolo`)**
   - Facilita agregar nuevas implementaciones (Strategy Pattern por protocolo).

## Recomendación técnica para implementar “todos los protocolos” en tu `Protocolo`

### Fase 1 (estabilización de base)
1. Consolidar en `Protocolo` una **máquina de estado mínima**:
   - `chipInicializado`, `fusesCargados`, `firmwareTipo`, etc.
2. Estandarizar I/O de todos los comandos con:
   - `readBytes(count, timeoutTotal)` + validaciones exactas de longitud.
3. Eliminar rutas de lectura manual dispersas y usar solo helpers centralizados.

### Fase 2 (paridad funcional con Python)
4. Portar fielmente semántica de comandos 7..23 con tablas de:
   - payload,
   - ack esperado,
   - timeout por comando,
   - precondiciones (`need_chip_info`, `need_fuses`).
5. Implementar por completo calibración y 10F.
6. Replicar verificación completa post-programación:
   - comparar ROM/EEPROM con buffers esperados,
   - commit de fuses 18F al final cuando corresponda.

### Fase 3 (robustez y mantenibilidad)
7. Introducir excepciones semánticas finas por comando en lugar de `catch(Exception)` general.
8. Añadir pruebas unitarias/instrumentadas por comando (mock USB).
9. Añadir trazas de sesión (comando, bytes TX/RX, timeout, resultado).

## Checklist concreto de “faltantes” detectados
- [ ] Implementar calibración real (`cmd 10`) y lectura/uso de calibración.
- [ ] Revisar `rom_is_blank` para alinear número de comando y protocolo de bytes.
- [ ] Revisar parseo de comando en `enviarComando` para evitar ambigüedad.
- [ ] Uniformar manejo timeout/lectura completa en todos los métodos.
- [ ] Añadir estado de protocolo (`chip info`/`fuses`) con precondiciones.
- [ ] Replicar verificación fuerte tipo `program_pic` (ROM+EEPROM+commit fuses 18F).
- [ ] Reducir `catch(Exception)` y propagar errores de dominio.

## Verificación de SDK/compilación
- Se ejecutó `setup-sdk.sh` correctamente y generó `local.properties` con `sdk.dir`.
- La compilación `:app:assembleDebug` se detuvo en `processDebugGoogleServices` por falta de `google-services.json` en rutas esperadas.
- Con esto, el SDK quedó instalado/configurado, pero no se pudo completar el build final por ese archivo local faltante en el entorno actual.
