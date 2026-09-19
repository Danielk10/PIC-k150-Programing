# Emulador de Programador K150 (PIC-k150-Programing)

Este entorno de emulación permite realizar pruebas locales para la aplicación `PIC-k150-Programing` y clientes como `picpro` simulando la conexión física de un programador K150 (usando el protocolo P18A) y microcontroladores PIC (PIC12, PIC16 y PIC18), todo de manera virtual.

## 🛠️ ¿Cómo funciona?

1. **`emulador_k150.cpp` / `emulador_k150.py`**: Servidores en C++ y Python que crean un par de terminales virtuales (pseudo-terminal o PTY) mediante `openpty()`. Configuran el puerto esclavo en modo raw (sin eco ni buffer de línea, sin control de flujo por software XON/XOFF) y simulan fielmente el protocolo P18A del programador K150 respondiendo a comandos de lectura, escritura, borrado y consulta de configuración.
2. **`picpro_patched.py`**: Dado que las PTYs en Linux no soportan llamadas a ioctls de líneas de control de módem (como DTR/RTS) que realiza `pyserial`, este wrapper monkey-patchea en tiempo de ejecución las funciones de actualización de estados DTR/RTS de `pyserial` y el método de reinicio de la conexión (`IConnection.reset()`), dando además prioridad al código local de `~/picpro`.
3. **`probar_emulacion.sh`**: Script bash de automatización que arranca el emulador en segundo plano (`cpp` o `python`), realiza las pruebas de ciclo de vida completo (lectura de config, borrado, programación de ROM/EEPROM/fuses, verificación independiente y volcado dump) utilizando el archivo HEX `pwmc_main107_628A.HEX` ubicado en el home, y finalmente limpia los procesos.

## 🚀 Ejecutar las pruebas

Simplemente corre el script de prueba automatizado:

```bash
./probar_emulacion.sh cpp      # Usando motor C++
./probar_emulacion.sh python   # Usando motor Python
```

## 💻 Uso manual interactivo

Si quieres usar el emulador de forma manual interactiva en tu terminal para probar comandos con la app o con `picpro`:

1. Inicia el emulador:
   ```bash
   ./emulador_k150_cpp
   # o: python3 emulador_k150.py
   ```
   *Esto generará un enlace simbólico en `~/emulador_pic_k150/vtty` apuntando al pseudo-terminal activo (ej. `/dev/pts/3`).*

2. En otra terminal, ejecuta los comandos de `picpro` usando el wrapper parchado y el puerto virtual:
   ```bash
   # Leer configuración
   python3 picpro_patched.py read_chip_config -p ./vtty -t 16F628A
   
   # Borrar chip
   python3 picpro_patched.py erase -p ./vtty -t 16F628A
   
   # Grabar archivo hex
   python3 picpro_patched.py program -p ./vtty -i ~/pwmc_main107_628A.HEX -t 16F628A

   # Verificar grabación
   python3 picpro_patched.py verify -p ./vtty -i ~/pwmc_main107_628A.HEX -t 16F628A
   ```
