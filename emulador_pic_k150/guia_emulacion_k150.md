# Guía Completa del Emulador de K150 (picpro) y Simulación de Familias PIC

Esta guía detalla paso a paso cómo utilizar, ejecutar y configurar el **emulador virtual del programador K150** (protocolo P18A) desarrollado en este proyecto, tanto en su versión en **Python** como en **C++**. 

El objetivo principal es permitirte probar en tu máquina local todo el ciclo de vida de programación de microcontroladores utilizando el archivo de prueba [`pwmc_main107_628A.HEX`](file:///home/danielpdiamon/pwmc_main107_628A.HEX) para el chip **PIC16F628A**, además de enseñarte cómo configurar el emulador para **simular otras familias de PIC (PIC12, PIC16, PIC18)** que no poseas físicamente.

---

## 📋 1. Arquitectura del Emulador K150

El emulador replica el comportamiento del firmware del programador físico K150. Para interactuar con herramientas locales en tu PC:
1. El emulador abre un par de terminales virtuales (**PTY**).
2. Expone un puerto virtual mapeado al enlace simbólico [`~/emulador_pic_k150/vtty`](file:///home/danielpdiamon/emulador_pic_k150/vtty).
3. Responde a los comandos binarios enviados por el cliente (`picpro`) simulando el comportamiento del microcontrolador seleccionado.

Dado que las PTYs en Linux no soportan llamadas a ioctls de control físico (como DTR/RTS) que realiza `pyserial`, se utiliza el wrapper [`picpro_patched.py`](file:///home/danielpdiamon/emulador_pic_k150/picpro_patched.py) para ignorar estas señales y forzar el handshake del programador de forma virtual.

---

## 🚀 2. Ejecutar Pruebas Automatizadas en Local

El script de automatización [`probar_emulacion.sh`](file:///home/danielpdiamon/emulador_pic_k150/probar_emulacion.sh) levanta el emulador en segundo plano, realiza las pruebas completas usando tu archivo `.HEX` local y limpia el entorno al finalizar.

### Pruebas con la versión en Python:
```bash
cd ~/emulador_pic_k150
./probar_emulacion.sh python
```

### Pruebas con la versión en C++:
```bash
cd ~/emulador_pic_k150
./probar_emulacion.sh cpp
```

---

## 💻 3. Guía de Pruebas Manuales con `picpro` (Paso a Paso)

Si deseas interactuar manualmente con el emulador para inspeccionar el flujo de bytes o depurar comandos específicos:

### Paso 1: Iniciar el Emulador en una terminal
Ejecuta el emulador en el lenguaje de tu elección para crear el puerto virtual `./vtty`:

**En Python:**
```bash
python3 ~/emulador_pic_k150/emulador_k150.py
```

**En C++ (compilar y ejecutar):**
```bash
g++ -O2 ~/emulador_pic_k150/emulador_k150.cpp -o ~/emulador_pic_k150/emulador_k150_cpp
~/emulador_pic_k150/emulador_k150_cpp
```

### Paso 2: Lanzar Comandos desde otra terminal
Usa el wrapper parchado [`picpro_patched.py`](file:///home/danielpdiamon/emulador_pic_k150/picpro_patched.py) apuntando al puerto virtual `-p ~/emulador_pic_k150/vtty` y especificando el tipo de chip `-t 16F628A`:

1. **Leer la Configuración del Chip (ID y Fuses actuales):**
   ```bash
   python3 ~/emulador_pic_k150/picpro_patched.py read_chip_config -p ~/emulador_pic_k150/vtty -t 16F628A
   ```
   *Debe responder con ID 4192 (0x1060) y fuses en 0x3FFF.*

2. **Borrar la Memoria del Chip:**
   ```bash
   python3 ~/emulador_pic_k150/picpro_patched.py erase -p ~/emulador_pic_k150/vtty -t 16F628A
   ```

3. **Grabar tu Archivo HEX en el Microcontrolador Emulado:**
   ```bash
   python3 ~/emulador_pic_k150/picpro_patched.py program -p ~/emulador_pic_k150/vtty -i ~/pwmc_main107_628A.HEX -t 16F628A
   ```
   *El emulador escribirá los bloques en sus arrays de ROM, EEPROM y Fuses simulados en RAM.*

4. **Verificar la Grabación contra el Archivo HEX:**
   ```bash
   python3 ~/emulador_pic_k150/picpro_patched.py verify -p ~/emulador_pic_k150/vtty -i ~/pwmc_main107_628A.HEX -t 16F628A
   ```
   *Deberá reportar éxito de validación bit por bit tanto en ROM como en EEPROM.*

5. **Hacer un Volcado (Dump) de la Memoria a un archivo local:**
   ```bash
   python3 ~/emulador_pic_k150/picpro_patched.py dump rom -p ~/emulador_pic_k150/vtty -o /tmp/volcado_pic16.hex -t 16F628A
   ```

---

## 🧬 4. Cómo Simular Otras Familias de PIC (PIC12 / PIC16 / PIC18)

Puedes simular el comportamiento de cualquier chip soportado modificando las constantes de tamaño de memoria y registros en el código del emulador antes de ejecutarlo o compilarlo.

### A. Parámetros de Configuración por Familia:

| Característica | Familia PIC12 (ej. 12F675) | Familia PIC16 (ej. 16F628A) | Familia PIC18 (ej. 18F2550) |
| :--- | :--- | :--- | :--- |
| **`chip_id`** (ID decimal) | `2960` (`0x0B90`) | `4192` (`0x1060`) | `4640` (`0x1220`) |
| **ROM Size (Palabras)** | `1024` palabras | `2048` palabras | Dirección por bytes (ver abajo) |
| **ROM Size (Bytes)** | `rom_size_bytes = 2048` | `rom_size_bytes = 4096` | `rom_size_bytes = 32768` (32 KB) |
| **EEPROM Size (Bytes)**| `eeprom_size_bytes = 128` | `eeprom_size_bytes = 128` | `eeprom_size_bytes = 256` |
| **Fuses (Cantidad)** | `1` fusible (`fuses[0]`) | `1` fusible (`fuses[0]`) | `7` fusibles (`fuses[0..6]`) |
| **ID del Pic (Tamaño)**| `4` bytes | `4` bytes | `8` bytes |

---

### B. Modificar el Código Fuente para Simular PIC18F2550:

#### 🐍 Modificaciones en Python (`emulador_k150.py`):
Modifica el inicio de la función `run_emulator()` (líneas 37-45 aprox.):
```python
    # Configuración de memoria para PIC18F2550
    rom_size_bytes = 32768
    eeprom_size_bytes = 256
    chip_id = 4640
    fuses = [0xffff] * 7  # 7 palabras fuse (14 bytes)
    pic_id = bytearray(b'\xff' * 8) # ID de 8 bytes
```
Y ajusta el empaquetado del comando `13` (read config) en `get_config_bytes()`:
```python
    def get_config_bytes():
        # Estructura: chip_id (2B), id (8B), 7 fuses (14B), calibración (2B) = 26 bytes
        return struct.pack('<H8s7HH',
            chip_id,
            pic_id,
            *fuses,
            calibrate
        )
```

#### 🏢 Modificaciones en C++ (`emulador_k150.cpp`):
Modifica las variables al principio de `run_emulator()` (líneas 43-52 aprox.):
```cpp
    const size_t rom_size_bytes = 32768;
    const size_t eeprom_size_bytes = 256;
    uint16_t chip_id = 4640;
    std::vector<uint8_t> pic_id(8, 0xFF);
    std::vector<uint16_t> fuses(7, 0xFFFF);
```
Y en el bloque de procesamiento del comando `13` (read config), adapta la asignación en `config_bytes`:
```cpp
    // Copiar ID de 8 bytes
    std::memcpy(&config_bytes[2], pic_id.data(), 8);
    // Copiar 7 fuses (14 bytes)
    for (int i = 0; i < 7; ++i) {
        config_bytes[10 + i * 2] = fuses[i] & 0xFF;
        config_bytes[11 + i * 2] = (fuses[i] >> 8) & 0xFF;
    }
```
*Recompila el binario:*
```bash
g++ -O2 emulador_k150.cpp -o emulador_k150_cpp
```

---

## ⚡ 5. Tabla de Correspondencia del Protocolo K150

El emulador implementa y responde exactamente a los comandos mapeados por la lógica Java de tu aplicación Android (`ProtocoloP18A.java`):

| Comando (Dec) | Comando (Hex) | Acción en el Emulador | Respuesta del Emulador | Significado / Mapeo Java |
| :---: | :---: | :--- | :--- | :--- |
| `1` | `0x01` | Sincronización / Reset | `Q` (WAITING_FOR_COMMAND) | `enviarComando()` (Paso 1) |
| `80` (`'P'`) | `0x50` | Ir a Tabla de Saltos | `P` (AT_COMMAND_JUMP_TABLE) | `enviarComando()` (Paso 3) |
| `21` | `0x15` | Leer Protocolo | `P18A` (4 bytes ASCII) | `obtenerProtocoloDelProgramador()` |
| `20` | `0x14` | Leer Versión | `0x03` (K150) | `obtenerVersionOModeloDelProgramador()` |
| `18` | `0x12` | Detectar Chip en Socket | `A` (WAITING_FOR_ACTION) + `Y` | `detectarPicEnElSocket()` |
| `19` | `0x13` | Detectar Chip fuera de Socket | `A` (WAITING_FOR_ACTION) + `Y` | `detectarSiEstaFueraElPicDelSocket()` |
| `3` | `0x03` | Cargar variables de chip | Lee 11 bytes + responde `I` | `iniciarVariablesDeProgramacion()` |
| `4` | `0x04` | Encender Voltaje VPP | `V` (ENABLED) | `activarVoltajesDeProgramacion()` |
| `5` | `0x05` | Apagar Voltaje VPP | `v` (DISABLED) | `desactivarVoltajesDeProgramacion()` |
| `6` | `0x06` | Ciclar Voltaje VPP | `V` (ENABLED) | `reiniciarVoltajesDeProgramacion()` |
| `14` | `0x0E` | Borrado total del PIC | Borra memoria local + responde `Y` | `borrarMemoriaCompletaDelPic()` |
| `7` | `0x07` | Programar ROM | Graba bloques de 32 bytes + `Y`/`P` | `programarMemoriaROMDelPic()` |
| `8` | `0x08` | Programar EEPROM | Graba bloques de 2 bytes + `Y`/`P` | `programarMemoriaEEPROMDelPic()` |
| `9` | `0x09` | Programar Fuses e ID | Lee payload de 24 bytes + responde `Y` | `programarFusesIDDelPic()` |
| `11` | `0x0B` | Leer ROM | Devuelve la ROM simulada | `leerMemoriaROMDelPic()` |
| `12` | `0x0C` | Leer EEPROM | Devuelve la EEPROM simulada | `leerMemoriaEEPROMDelPic()` |
| `13` | `0x0D` | Leer Configuración | Responde `C` + 26 bytes de fuses/ID | `leerDatosDeConfiguracionDelPic()` |
