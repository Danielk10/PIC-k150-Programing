# 📚 INSTALACIÓN DE SDCC 4.5.0 EN TERMUX PARA ANDROID
## Compilador Small Device C Compiler para PIC

**Versión para Termux Google Play**

---

## ⚠️ REQUISITO OBLIGATORIO ANTES DE EMPEZAR

### 🔴 GPUTILS 1.5.2 ES OBLIGATORIO

Este tutorial requiere que tengas **GPUTILS 1.5.2 instalado y funcionando correctamente** en tu Termux. SDCC depende completamente de GPUTILS como base para compilar código para PIC.

**Si ya instalaste GPUTILS 1.5.2 siguiendo el tutorial anterior:** Puedes **saltar directamente al Paso 1 de este tutorial**.

**Si NO lo instalaste aún:** Debes **instalar GPUTILS 1.5.2 primero** siguiendo el tutorial específico o completando los pasos iniciales de este documento.

#### Verificar si tienes GPUTILS instalado:

Ejecuta estos comandos en Termux:

```bash
gpasm --version
gplink --version
gplib --version
```

Si ves algo como:

```
gpasm-1.5.2 #1325 (Jan 25 2026)
gplink-1.5.2 #1325 (Jan 25 2026)
gplib-1.5.2 #1325 (Jan 25 2026)
```

✅ **¡GPUTILS está instalado correctamente! Continúa con el Paso 1 de este tutorial.**

Si ves "command not found" o similar, debes instalar GPUTILS primero.

---

## 📋 CONTENIDO DEL TUTORIAL

1. Actualizar Repositorios (si es necesario)
2. Instalar Dependencias Base
3. Instalar Dependencias Avanzadas
4. Verificar Prerequisites
5. Descargar Código Fuente de SDCC
6. Extraer Archivos
7. Configurar SDCC
8. Compilar SDCC (⏱️ PROCEDIMIENTO LARGO)
9. Instalar SDCC
10. Verificar Instalación
11. Compilar Primer Programa C para PIC16F628A
12. Copiar Archivo HEX a Descargas
13. Solución de Problemas

---

# 🔧 INSTALACIÓN DE GPUTILS 1.5.2 (PARA USUARIOS QUE NO LO INSTALARON)

Si ya completaste la instalación de GPUTILS con el tutorial anterior, **SALTA ESTA SECCIÓN** y ve al **PASO 1** del tutorial de SDCC.

## 📦 PASO A.1: Actualizar Repositorios de Termux

Abre la aplicación Termux y actualiza los paquetes del sistema:

```bash
pkg update && pkg upgrade -y
```

**ℹ️ Notas Importantes:**
- Este comando descarga e instala las actualizaciones más recientes
- El parámetro `-y` responde automáticamente "sí" a confirmaciones
- Esto garantiza seguridad y compatibilidad

🔐 **Configurar Acceso al Almacenamiento:**

```bash
termux-setup-storage
```

Necesitarás aceptar el permiso de almacenamiento en tu dispositivo Android.

---

## 🔧 PASO A.2: Instalar Dependencias de GPUTILS

Instala las herramientas básicas de compilación:

```bash
pkg install build-essential clang make wget tar bzip2 -y
```

**📋 Paquetes Instalados:**
- `build-essential`: Herramientas básicas de compilación (gcc, g++, etc.)
- `clang`: Compilador moderno de C/C++
- `make`: Automatización de compilación
- `wget`: Descarga de archivos
- `tar/bzip2`: Descompresión de archivos

Luego instala utilidades binarias:

```bash
pkg install binutils -y
```

---

## 📥 PASO A.3: Descargar GPUTILS 1.5.2

```bash
wget https://sourceforge.net/projects/gputils/files/gputils/1.5.0/gputils-1.5.2.tar.bz2/download -O gputils-1.5.2.tar.bz2
```

---

## 📂 PASO A.4: Extraer GPUTILS

```bash
tar -xjf gputils-1.5.2.tar.bz2
cd gputils-1.5.2
```

---

## ⚙️ PASO A.5: Configurar GPUTILS

```bash
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie"
```

---

## 🔨 PASO A.6: Compilar GPUTILS

```bash
make -j$(nproc)
```

---

## 💾 PASO A.7: Instalar GPUTILS

```bash
make install
```

---

## ✅ PASO A.8: Verificar GPUTILS

```bash
gpasm --version
gplink --version
gplib --version
```

Deberías ver las versiones de cada herramienta.

---

# 📚 TUTORIAL DE INSTALACIÓN DE SDCC 4.5.0

## 📖 ¿QUÉ ES SDCC?

**SDCC** (Small Device C Compiler) es un compilador C estándar retargetable y optimizado que soporta múltiples arquitecturas:

- **STM8, MCS-51, DS390, HC08, S08, Z80, Z180, R800, Rabbit, SM83, eZ80**
- **Microchip PIC16 y PIC18** ✅ (Lo que usaremos)
- **Padauk PDK13, PDK14, PDK15**
- **MOS 6502 y 6502**

### 🎯 ¿Por qué usar SDCC para PIC?

- **Desarrollo más rápido** en C vs Ensamblador
- **Código más legible y mantenible**
- **Fácil portabilidad** entre diferentes PICs
- **Genera código eficiente** para microcontroladores
- **Compatible con GPUTILS** para ensamblaje final

---

## 📦 PASO 1: Actualizar Repositorios de Termux

Abre Termux y actualiza todos los paquetes:

```bash
pkg update && pkg upgrade -y
```

**ℹ️ Explicación:**
- Asegura que tengas las versiones más recientes de las dependencias
- Aplica parches de seguridad
- Evita conflictos de compatibilidad

---

## 🔐 Configurar Acceso al Almacenamiento

Si no lo hiciste previamente:

```bash
termux-setup-storage
```

Acepta el permiso cuando se te solicite.

---

## 🔧 PASO 2: Instalar Dependencias Base

Instala los compiladores y herramientas esenciales:

```bash
pkg install wget tar bzip2 -y
```

Luego:

```bash
pkg install clang make binutils build-essential cmake -y
```

**📋 Explicación de paquetes:**
- `wget`: Descargar archivos desde Internet
- `tar/bzip2`: Descomprimir archivos
- `clang`: Compilador C/C++ moderno
- `make`: Sistema de compilación
- `binutils`: Herramientas de manipulación de binarios
- `build-essential`: Compiladores GCC
- `cmake`: Sistema de construcción alternativo

Verifica que binutils esté instalado:

```bash
pkg install binutils -y
```

---

## 🔧 PASO 3: Instalar Dependencias Avanzadas

SDCC requiere bibliotecas adicionales para compilación avanzada:

```bash
pkg install libandroid-execinfo -y
```

**ℹ️ `libandroid-execinfo`:** Soporte para seguimiento de pilas (stack tracing) en Android.

Instala herramientas de parsing y análisis:

```bash
pkg install bison flex boost zlib texinfo -y
```

**📋 Explicación:**
- `bison`: Generador de analizadores sintácticos (parser generator)
- `flex`: Generador de analizadores léxicos (lexer generator)
- `boost`: Biblioteca de utilidades C++ necesaria para SDCC
- `zlib`: Compresión de datos
- `texinfo`: Documentación y manuales

Instala encabezados de Boost:

```bash
pkg install boost-headers -y
```

Instala la librería estándar de C++:

```bash
pkg install libc++ -y
```

**ℹ️ `libc++`:** Implementación de la Standard Library de C++ compatible con Android/Termux.

---

## 📥 PASO 4: Descargar Código Fuente de SDCC 4.5.0

Descarga el archivo comprimido desde SourceForge:

```bash
wget https://sourceforge.net/projects/sdcc/files/sdcc/4.5.0/sdcc-src-4.5.0.tar.bz2
```

**⏱️ Tiempo de descarga:** 5-15 minutos dependiendo de tu velocidad de conexión.

---

## 📂 PASO 5: Extraer el Código Fuente

Una vez descargado, extrae el archivo:

```bash
tar -jxvf sdcc-src-4.5.0.tar.bz2
```

Navega al directorio extraído:

```bash
cd sdcc-4.5.0
```

**📋 Explicación del comando:**
- `tar`: Comando de manipulación de archivos empaquetados
- `-jxvf`: Opciones combinadas:
  - `-j`: Descomprimir usando bzip2
  - `-x`: Extraer archivos
  - `-v`: Modo verboso (muestra progreso)
  - `-f`: Especifica el archivo a procesar

---

## ⚙️ PASO 6: Configurar SDCC

Este paso prepara SDCC para compilación específicamente optimizado para Termux/Android:

```bash
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            CXXFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie" \
            gcc_cv_c_no_fpie=no \
            gcc_cv_no_pie=no
```

**🔍 Explicación detallada de las opciones:**

- `./configure`: Script que prepara el proyecto
- `AR=ar`: Especifica el archivador para bibliotecas estáticas
- `RANLIB=ranlib`: Genera índices de bibliotecas
- `--prefix=$PREFIX`: Instala en `/data/data/com.termux/files/usr`

**Opciones de compilación importantes:**

- `CFLAGS="-fPIC -fPIE"`: 
  - `-fPIC`: Position Independent Code (código independiente de posición)
  - `-fPIE`: Position Independent Executable (ejecutable independiente de posición)
  - Necesario para seguridad en Android moderno

- `CXXFLAGS="-fPIC -fPIE"`: Lo mismo para código C++

- `LDFLAGS="-pie"`: Enlazador configurado para PIE (ejecutables independientes de posición)

- `gcc_cv_c_no_fpie=no`: No desactiva PIE
- `gcc_cv_no_pie=no`: No desactiva compilación PIE

**⚠️ Importante:**

Si ves mensajes de error, revisa el archivo de configuración:

```bash
cat config.log | grep -i error
```

---

## 🔨 PASO 7: Compilar SDCC

⏱️ **⚠️ ADVERTENCIA IMPORTANTE:** 

La compilación de SDCC **TARDA MÁS DE 5 HORAS** en un dispositivo móvil típico.

### 📋 Instrucciones Críticas:

1. **NO CANCELES EL PROCESO** - Aunque parezca que se colgó, es normal
2. **CONECTA TU TELÉFONO AL CARGADOR** - La batería debe mantenerse al 100%
3. **TEN PACIENCIA** - El código es muy grande, esto es normal
4. **NO CIERRES TERMUX** durante la compilación

Inicia la compilación con:

```bash
make -j$(nproc)
```

**📋 Explicación del comando:**
- `make`: Lee Makefile y ejecuta instrucciones de compilación
- `-j$(nproc)`: Compilación paralela usando todos los núcleos
  - `-j`: Paralelización
  - `$(nproc)`: Número automático de núcleos disponibles
  - Esto acelera significativamente el proceso

**⏱️ Tiempo esperado:**
- Dispositivos de 4 núcleos: 5-8 horas
- Dispositivos de 8 núcleos: 3-5 horas
- Varía según el modelo del teléfono y RAM disponible

**🎯 Qué esperar durante la compilación:**

- Verás líneas de salida normales durante 30-60 minutos iniciales
- Luego puede parecer que "se colgó" sin mostrar nada nuevo
- **ESTO ES COMPLETAMENTE NORMAL** - El compilador está trabajando
- Puede haber períodos de 30-60 minutos sin salida
- El proceso está funcionando, solo está compilando código complejo

### ⚠️ Si recibes Error: `[Process completed (signal 9)]`

Este error ocurre cuando Android (el kernel) fuerza el cierre de Termux. Las causas son:

1. **Phantom Process Killer** (Android 12+): El sistema cierra apps que consumen muchos recursos
2. **Falta de memoria (RAM)**: El proceso intentó usar más RAM de la disponible

#### Soluciones (Ordenadas por efectividad):

**Solución 1: Configuración de Batería (RÁPIDA)**

1. Ve a **Ajustes → Aplicaciones → Termux**
2. Selecciona **Batería** o **Uso de Batería**
3. Cambia a **"Sin restricciones"** o **"No optimizar"**
4. En aplicaciones recientes, mantén presionada la ventana de Termux
5. Selecciona el icono de **candado** para evitar cierre automático

**Solución 2: Desactivar Phantom Process Killer (ANDROID 12+)**

Si tienes Android 12, 13 o 14, este es el paso más importante.

**Método A: Vía ADB desde PC (Si tienes disponible):**

```bash
adb shell "/system/bin/device_config set_sync_disabled_for_tests persistent"
adb shell "/system/bin/device_config put activity_manager max_phantom_processes 2147483647"
adb shell settings put global settings_enable_monitor_phantom_procs false
```

**Método B: Vía LADB (Sin PC - En tu teléfono):**

1. Descarga la app **LADB** desde Play Store o GitHub
2. En Termux, activa **Opciones de Desarrollador** → **Depuración Inalámbrica**
3. Abre LADB y conecta con Depuración Inalámbrica
4. Ejecuta los mismos 3 comandos de arriba

**Método C: Feature Flags (Algunos teléfonos)**

1. Ve a **Ajustes → Opciones de Desarrollador**
2. Busca **"Feature Flags"** o **"Banderas de características"**
3. Encuentra `settings_enable_monitor_phantom_procs`
4. Ponlo en **False** (desactivado)

### ✅ Si la compilación se cancela:

**Buena noticia:** Puedes continuar donde se paró.

Simplemente ejecuta nuevamente:

```bash
make -j$(nproc)
```

El sistema detectará qué ya se compiló y continuará desde ese punto. **NO necesitas empezar de cero**.

---

## 💾 PASO 8: Instalar SDCC

Una vez completada la compilación (después de 5+ horas):

```bash
make install
```

**ℹ️ Nota:** La instalación es mucho más rápida (2-5 minutos).

**📍 Ubicaciones de instalación:**
- Binarios: `/data/data/com.termux/files/usr/bin/`
  - `sdcc`: Compilador SDCC principal
  - `sdcpp`: Preprocesador
  - `cppstm8`: Preprocesador para STM8
  
- Librerías: `/data/data/com.termux/files/usr/lib/`
  - Librerías SDCC para diferentes arquitecturas
  
- Includes: `/data/data/com.termux/files/usr/share/sdcc/`
  - Headers para PIC, STM8, Z80, etc.

---

## ✅ PASO 9: Verificar la Instalación

Verifica que SDCC se instaló correctamente:

```bash
sdcc -v
```

**Resultado esperado (o similar):**

```
SDCC : mcs51/z80/z180/r2k/r2ka/r3ka/sm83/tlcs90/ez80_z80/z80n/r800/ds390/pic16/pic14/TININative/ds400/hc08/s08/stm8/pdk13/pdk14/pdk15/mos6502/mos65c02/f8 TD- 4.5.0 #15242 (Linux)
published under GNU General Public License (GPL)
```

✅ Si ves la versión de SDCC, **¡la instalación fue exitosa!**

---

## 🔄 (OPCIONAL) Limpiar Archivos de Instalación

Si deseas liberar espacio, puedes eliminar las carpetas extraídas y los archivos comprimidos:

```bash
cd ~
rm -rf gputils-1.5.2
rm -rf sdcc-4.5.0
rm -f gputils-1.5.2.tar.bz2
rm -f sdcc-src-4.5.0.tar.bz2
```

**Advertencia:** Haz esto solo si confirmaste que SDCC y GPUTILS funcionan correctamente.

---

# 💻 COMPILAR CÓDIGO C PARA PIC16F628A CON SDCC

Ahora que tienes SDCC instalado, compila código C para PIC.

## PASO 10: Crear el Archivo C

Crea un archivo con tu editor favorito:

```bash
nano prueba_led.c
```

---

## 📝 CÓDIGO C: Parpadeo de LED con PIC16F628A

Copia el siguiente código en el editor:

```c
#include <pic16f628a.h>
#include <stdint.h>

// ============================================================
// CONFIGURACIÓN DE FUSIBLES PARA PIC16F628A
// ============================================================
// En SDCC 4.5.0 para PIC14, la configuración se define así:
// - _INTRC_OSC_NOCLKOUT: Usar oscilador interno sin salida en RA6
// - _WDT_OFF: Desactivar el watchdog timer
// - _LVP_OFF: Desactivar programación en bajo voltaje (seguridad)
// - _MCLRE_ON: Pin MCLR activo (Reset maestro)

__code uint16_t __at (0x2007) _conf = _INTRC_OSC_NOCLKOUT & _WDT_OFF & _LVP_OFF & _MCLRE_ON;

// ============================================================
// FUNCIÓN DE RETARDO (DELAY)
// ============================================================
// Genera un retardo aproximado para lograr una frecuencia
// visible de parpadeo de LED
// 
// La palabra clave 'volatile' es CRÍTICA aquí:
// Sin ella, el compilador SDCC podría "optimizar" (eliminar)
// este bucle vacío como código innecesario.
// Con 'volatile', SDCC sabe que el bucle tiene efecto secundario
// y debe ejecutarse completamente.

void delay(void) {
    // Crear un retardo aproximado de ~500ms a 4MHz
    // El valor 10000 se ajusta según tu prueba experimental
    for (volatile uint16_t i = 0; i < 10000; i++);
}

// ============================================================
// FUNCIÓN PRINCIPAL (MAIN)
// ============================================================
// Esta función se ejecuta una sola vez al iniciar el PIC

void main(void) {
    // Desactivar los comparadores analógicos
    // El PIC16F628A tiene comparadores en PORTB que interfieren
    // con la salida digital si no se desactivan.
    // 0x07 desactiva todos los comparadores
    CMCON = 0x07;
    
    // Configurar PORTB como salidas digitales
    // 0x00 = todos los pines de PORTB como salidas
    // 1 = entrada, 0 = salida
    TRISB = 0x00;
    
    // ============================================================
    // BUCLE PRINCIPAL (LOOP INFINITO)
    // ============================================================
    // Este bucle se repite indefinidamente mientras el PIC esté
    // encendido, alternando entre encender y apagar el LED
    
    while(1) {
        // Encender el LED
        // RB0 = 1 establece el pin RB0 en nivel alto (5V)
        RB0 = 1;
        
        // Esperar 500ms aproximadamente
        delay();
        
        // Apagar el LED
        // RB0 = 0 establece el pin RB0 en nivel bajo (0V)
        RB0 = 0;
        
        // Esperar 500ms aproximadamente
        delay();
        
        // El bucle se repite, encendiendo y apagando el LED
        // Resultado: LED parpadea con período de ~1 segundo
    }
}

// ============================================================
// NOTA TÉCNICA: DIFERENCIAS CON ASSEMBLER
// ============================================================
// 
// En Ensamblador (ASM):
// - Cada instrucción controla exactamente qué hace el PIC
// - Necesitas manejar manualmente: bancos, registros, bits
// - Más líneas de código (50-100 líneas para este programa)
// - Compilación rápida pero difícil de mantener
//
// En C con SDCC:
// - Escribes lógica de alto nivel
// - SDCC genera el código ensamblador automáticamente
// - Solo 40 líneas de código C con comentarios
// - Más fácil de leer, mantener y depurar
// - El compilador optimiza el código generado
//
// ============================================================
```

---

## 💾 Guardar el Archivo

Presiona:
1. **CTRL + X** para salir del editor
2. **Y** para confirmar que quieres guardar
3. **ENTER** para confirmar el nombre del archivo

---

## 🔨 PASO 11: Compilar con SDCC

Compila el código C para el PIC16F628A:

```bash
sdcc -mpic14 -p16f628a --use-non-free prueba_led.c
```

**🔍 Explicación de las opciones de compilación:**

- `sdcc`: Compilador SDCC
- `-mpic14`: Especifica que compilamos para la familia PIC14
  - `pic14` = PIC16F628A, PIC16F877, etc. (arquitectura de 14 bits)
  - Nota: Existe también `-mpic16` para PIC18 (arquitectura de 16 bits)
- `-p16f628a`: Especifica el modelo exacto de PIC
  - Permite optimizaciones específicas del modelo
  - Configura automáticamente la cantidad de RAM disponible
- `--use-non-free`: Usa bibliotecas y scripts no gratuitos de GPUTILS
  - Necesario para obtener el archivo HEX final optimizado
  - Sin esta opción, no generaría .hex compilable

**📂 Archivos Generados:**

Verifica los archivos creados:

```bash
ls -la prueba_led.*
```

**Deberías ver:**

- `prueba_led.asm`: Código ensamblador generado por SDCC
  - Puedes verlo para entender qué genera SDCC
- `prueba_led.lst`: Listado con referencias cruzadas
  - Mapea instrucciones C a ensamblador
- `prueba_led.cod`: Archivo de depuración COD
  - Usado por simuladores y depuradores
- `prueba_led.hex`: ✅ **EL ARCHIVO QUE NECESITAS**
  - Código máquina en formato hexadecimal para programar el PIC
  - Este es el archivo que cargarás en tu programador

**⚠️ Mensajes de Compilación (Son normales):**

Puedes ver mensajes como:

```
prueba_led.asm:91:Message[1304] Page selection not needed for this device. No code generated.
warning: Relocation symbol "_cinit" [0x0000] has no section.
```

Estos son **advertencias normales** en SDCC para PIC14. No afectan el funcionamiento del programa compilado.

---

## 📋 PASO 12: Copiar Archivo HEX a Descargas

Copia el archivo HEX a tu carpeta de Descargas para poder descargarlo desde la aplicación:

```bash
cp prueba_led.hex ~/storage/downloads/
```

**Verificar la copia:**

```bash
ls -l ~/storage/downloads/prueba_led.hex
```

Deberías ver:

```
-rw-r--r-- 1 u0_a123 u0_a123  1234 Jan 26 10:30 ~/storage/downloads/prueba_led.hex
```

---

## 🚀 PASO 13: Programar tu PIC16F628A

Ahora tienes el archivo `.hex` listo para programar en tu PIC.

### 📝 Pasos Finales:

1. **Conecta tu programador PIC K150** a tu dispositivo Android
   - Si usas USB-OTG, conecta: Teléfono → Adaptador USB-OTG → Programador
   
2. **Abre la aplicación "PIC K150 Programming"**
   
3. **Selecciona el modelo de PIC:**
   - Busca `PIC16F628A` en la lista de modelos
   
4. **Carga el archivo HEX:**
   - Haz clic en "Cargar archivo" o "Load file"
   - Navega a: `Almacenamiento → Downloads → prueba_led.hex`
   
5. **Verifica la conexión:**
   - Asegúrate de que el PIC esté correctamente insertado en el programador
   - La aplicación debería detectar el dispositivo
   
6. **Inicia la programación:**
   - Haz clic en "Programar" o "Program"
   - Espera a que se complete (generalmente 10-30 segundos)

7. **Verificación exitosa:**
   - La aplicación debería mostrar un mensaje de éxito
   - Algunos programadores tienen un LED verde cuando finaliza

---

## ✨ Resultado Esperado

Una vez programado correctamente, tu LED **parpadeará continuamente**:
- **Encendido:** ~500 milisegundos
- **Apagado:** ~500 milisegundos
- **Período total:** ~1 segundo

---

# 📚 COMPARACIÓN: ENSAMBLADOR vs C CON SDCC

Para que entiendas las ventajas de usar C, aquí comparamos ambos enfoques:

## 📊 Tabla Comparativa

| Aspecto | Ensamblador (ASM) | C con SDCC |
|---------|-------------------|-----------|
| **Líneas de código** | 80-120 líneas | 30-40 líneas |
| **Curva de aprendizaje** | Muy difícil | Moderada |
| **Velocidad de desarrollo** | Muy lenta | Rápida |
| **Legibilidad** | Difícil | Fácil |
| **Mantenibilidad** | Compleja | Simple |
| **Portabilidad a otro PIC** | Requiere reescritura mayor | Cambiar `-p16f628a` a otro modelo |
| **Riesgo de errores** | Alto | Bajo |
| **Control de hardware** | Total | Alto (suficiente para 99% de casos) |
| **Velocidad de ejecución** | Máxima | 95-98% de máxima |
| **Tamaño de código (HEX)** | Mínimo | Mínimo-Pequeño |

---

## 🔍 Ejemplo: El mismo programa en ASM

Para comparación, el parpadeo de LED en **Ensamblador** sería así:

```asm
; Parpadeo de LED en PIC16F628A - Ensamblador
; Este es equivalente al código C anterior

    LIST P=16F628A
    #include <p16f628a.inc>

; Configuración de fusibles
    __CONFIG _CP_OFF & _WDT_OFF & _PWRTE_ON & _INTRC_OSC_NOCLKOUT & _LVP_OFF & _BODEN_OFF & _MCLRE_ON

; Variables para retardo (3 bytes de RAM)
    CBLOCK 0x20
        d1
        d2
        d3
    ENDC

; Vector de reset
    ORG 0x00
    goto Inicio

; Programa principal
Inicio:
    ; Configurar PORTB como salidas
    banksel TRISB
    movlw   b'00000000'
    movwf   TRISB

    ; Desactivar comparadores
    banksel CMCON
    movlw   0x07
    movwf   CMCON

    banksel PORTB

; Bucle principal
Bucle:
    bsf     PORTB, 0        ; Encender LED (RB0 = 1)
    call    Retardo         ; Esperar 500ms
    bcf     PORTB, 0        ; Apagar LED (RB0 = 0)
    call    Retardo         ; Esperar 500ms
    goto    Bucle

; Subrutina de retardo (~500ms a 4MHz)
Retardo:
    movlw   0x03
    movwf   d1
    movlw   0x18
    movwf   d2
    movlw   0x02
    movwf   d3

Retardo_Loop:
    decfsz  d1, f
    goto    $+2
    decfsz  d2, f
    goto    $+2
    decfsz  d3, f
    goto    Retardo_Loop

    return

    END
```

**Análisis:**
- **120 líneas de código** vs 40 líneas de C
- **Mucha gestión manual** de bancos de memoria
- **Cálculos complejos** para retardos exactos
- **Difícil de modificar** (cambiar tiempo de parpadeo requiere recalcular números)
- **Mayor riesgo de errores** en direccionamiento de bancos

---

## 🎯 ¿Cuándo usar cada uno?

**Usa ENSAMBLADOR cuando:**
- Necesites máxima velocidad en secciones críticas
- Tienes restricciones muy ajustadas de memoria/espacio
- Realizas acceso directo a hardware muy específico
- Ya tienes código ASM probado

**Usa C con SDCC cuando (99% de los casos):**
- ✅ Desarrollo rápido
- ✅ Código más legible y mantenible
- ✅ Cambios y depuración más fácil
- ✅ Código más portable
- ✅ Menos propenso a errores
- ✅ Ideal para aprendizaje

---

# 🎓 EXPLICACIÓN DETALLADA DE FLAGS DE COMPILACIÓN

## 🔨 Opciones Principales de SDCC

### 1. **Selección de Arquitectura**

```bash
-mpic14      # PIC de 14 bits (16F628A, 16F877, etc.)
-mpic16      # PIC de 16 bits (18F2550, 18F4550, etc.)
-mstm8       # STM8 (microcontroladores STMicroelectronics)
-mz80        # Z80 (procesador Zilog)
```

### 2. **Selección de Dispositivo Específico**

```bash
-p16f628a    # PIC específico
-p16f877a    # PIC específico
-p18f2550    # PIC específico
```

SDCC adapta el código para las características exactas del chip.

### 3. **Opciones de Optimización**

```bash
-O2          # Optimización de velocidad
-O3          # Máxima optimización
-Os          # Optimizar para tamaño (menor .hex)
```

**Recomendación para PIC:** `-O2` es generalmente lo mejor.

### 4. **Opciones de Salida**

```bash
--out-fmt-ihx  # Generar archivo .hex (Intel HEX) - Por defecto
--out-fmt-s19  # Generar archivo .s19 (Motorola S-Record)
```

### 5. **Librerías y Scripts**

```bash
--use-non-free  # Usar librerías no gratuitas (gputils)
                # NECESARIO para generar .hex correctamente
```

---

## 📚 EXPLICACIÓN DEL CONFIGURE DE SDCC

Recuerda que en la instalación usamos:

```bash
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            CXXFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie" \
            gcc_cv_c_no_fpie=no \
            gcc_cv_no_pie=no
```

### 🔍 Explicación de cada flag:

#### **CFLAGS="-fPIC -fPIE"**

- `-fPIC` (Position Independent Code):
  - Genera código que puede ejecutarse desde cualquier dirección de memoria
  - Necesario para cargar dinámicamente librerías compartidas
  - En Android, es casi obligatorio para seguridad

- `-fPIE` (Position Independent Executable):
  - Genera ejecutables independientes de posición
  - Proporciona protección ASLR (Address Space Layout Randomization)
  - Seguridad contra exploits

#### **LDFLAGS="-pie"**

- Instrucciones al enlazador (linker) que genere ejecutables PIE
- Complementa el `-fPIE` del compilador
- Asegura que todo el ejecutable final sea independiente de posición

#### **gcc_cv_c_no_fpie=no**

- Variable de configuración del script `configure`
- `-no`: No desactives la compilación PIE
- Asegura que la detección automática de soporte PIE se haga correctamente

#### **gcc_cv_no_pie=no**

- Similar al anterior pero para la fase final de enlazado
- Asegura que se use `-pie` en el enlazador

---

## 🎯 Caso de Uso: Ejemplo con diferentes flags

### Compilar con optimización de velocidad:

```bash
sdcc -mpic14 -p16f628a -O2 --use-non-free prueba_led.c
```

### Compilar con optimización de tamaño:

```bash
sdcc -mpic14 -p16f628a -Os --use-non-free prueba_led.c
```

### Compilar sin optimización (para depuración):

```bash
sdcc -mpic14 -p16f628a --use-non-free prueba_led.c
```

---

# 💡 LIMITACIONES DE SDCC PARA PIC

Es importante que conozcas las limitaciones al usar SDCC con PIC:

## ⚠️ Limitaciones Técnicas

### 1. **Soporte Parcial de PIC16**

- SDCC tiene soporte **incompleto** para PIC16 (pic14)
- Muchas características funcionan bien, pero algunas están limitadas
- El soporte es **mejor** en MCS-51 y STM8

### 2. **Limitaciones de Memoria**

| Aspecto | Limitación |
|---------|-----------|
| **PIC16F628A** | 2KB de ROM, 224 bytes de RAM |
| **Código C complejo** | Puede no caber en chips muy pequeños |
| **Strings constantes** | Ocupan espacio de programa |
| **Arreglos grandes** | RAM limitada hace difícil usarlos |

### 3. **Acceso a Hardware**

- No todos los periféricos están soportados via librerías
- Algunos requieren acceso manual via registros
- PWM, UART, ADC: Funcionan pero requieren conocimiento de registros

### 4. **Compilación de Punto Flotante**

- Operaciones con `float` generan código muy grande
- En PIC16, mejor usar `int` o `unsigned int`
- Usa librerías de aritmética fija si necesitas decimales

### 5. **Recursión**

- Evita funciones recursivas (llaman a sí mismas)
- El stack de PIC16 es muy limitado (~8 niveles)
- Mejor reescribir como bucles iterativos

---

## 📋 Compatibilidad de Familias PIC

**SDCC soporta (con soporte completo a bueno):**

- ✅ PIC16F628A, 16F877A (pic14 - 14 bits)
- ✅ PIC18F2550, 18F4550, 18F4620 (pic16 - 16 bits)

**SDCC soporta (con soporte limitado o experimental):**

- ⚠️ PIC24 (24 bits) - Soporte muy limitado
- ⚠️ dsPIC30/33 - Soporte muy limitado
- ⚠️ PIC32 - Mejor usar XC32 de Microchip

**Para otros microcontroladores:**

- ✅ STM8: Soporte excelente (mejor que PIC)
- ✅ Z80: Soporte excelente
- ✅ MCS-51: Soporte muy bueno

---

## 🎯 Alternativas a SDCC

Si necesitas mejor soporte para PIC:

| Compilador | Ventajas | Desventajas |
|-----------|----------|------------|
| **SDCC** | Gratuito, opensource, multiplataforma | Soporte parcial en PIC |
| **XC8 (Microchip)** | Oficial de Microchip, soporte completo | Propietario, limitado en versión free |
| **CCS C** | Excelente soporte PIC | Muy caro, propietario |
| **PICC18** | Bueno para PIC18 | Propietario, caro |

---

# 🆘 SOLUCIÓN DE PROBLEMAS

## ❌ Error: "command not found: sdcc"

**Causa:** SDCC no se instaló en PATH de Termux.

**Solución:**

```bash
export PATH=$PREFIX/bin:$PATH
echo 'export PATH=$PREFIX/bin:$PATH' >> ~/.bashrc
```

Luego reinicia Termux.

---

## ❌ Error: "configure: error: C compiler cannot create executables"

**Causa:** Falta compilador C o librerías.

**Solución:**

```bash
pkg install build-essential clang -y
cd ~/sdcc-4.5.0
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            CXXFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie" \
            gcc_cv_c_no_fpie=no \
            gcc_cv_no_pie=no
make distclean
make -j$(nproc)
```

---

## ❌ Error: "[Process completed (signal 9)]" durante compilación

Ver sección **"Si recibes Error: [Process completed (signal 9)]"** en PASO 7.

**En resumen:**
1. Configurar batería sin restricciones
2. Desactivar Phantom Process Killer (Android 12+)
3. Ejecutar `make -j$(nproc)` nuevamente para continuar

---

## ❌ Error: "gputils: command not found"

**Causa:** GPUTILS no está instalado o no en PATH.

**Solución:**

Verifica que GPUTILS está en el sistema:

```bash
ls -la $PREFIX/bin/gpasm
```

Si no existe, debes instalar GPUTILS primero.

---

## ❌ Compilación muy lenta o se congela

**Causa:** Dispositivo con pocos recursos o Termux optimizado.

**Soluciones:**

1. Reduce paralelización:
```bash
make -j2        # Solo 2 núcleos
```

2. Usa un solo núcleo (más lento pero más estable):
```bash
make            # Sin -j
```

3. Cierra otras aplicaciones para liberar RAM

---

# 📚 REFERENCIAS Y RECURSOS

## 🔗 Sitios Oficiales

- **SDCC Official:** https://sourceforge.net/projects/sdcc/
- **GPUTILS Official:** https://sourceforge.net/projects/gputils/
- **Termux Wiki:** https://wiki.termux.com/
- **Microchip PIC16F628A Datasheet:** https://www.microchip.com/

## 📖 Documentación

- **SDCC Manual:** https://sdcc.sourceforge.io/
- **SDCC User Guide:** https://sdcc.sourceforge.io/doc/sdccman.pdf
- **Termux Package Management:** https://wiki.termux.com/wiki/Package_Management

## 📱 Aplicaciones

- **Termux:** https://play.google.com/store/apps/details?id=com.termux
- **PIC K150 Programming:** Play Store
- **LADB (ADB desde Android):** https://github.com/RikkaApps/LADB

## 🆘 Problemas y Soluciones

- **Phantom Process Killer:** https://docs.andronix.app/android-12/andronix-on-android-12-and-beyond
- **Termux Issues:** https://github.com/termux/termux-app/issues
- **SDCC Issues:** https://sourceforge.net/projects/sdcc/

---

# ✅ CHECKLIST DE ÉXITO

Marca cada paso completado:

- [ ] GPUTILS 1.5.2 instalado y verificado
- [ ] Repositorios de Termux actualizados
- [ ] Todas las dependencias instaladas
- [ ] Archivo SDCC 4.5.0 descargado
- [ ] Código fuente extraído sin errores
- [ ] Configure completado sin errores
- [ ] Compilación iniciada y completada
- [ ] Make install completado exitosamente
- [ ] `sdcc -v` muestra versión 4.5.0
- [ ] Archivo `prueba_led.c` creado
- [ ] Compilación de prueba_led.c exitosa
- [ ] Archivo `prueba_led.hex` generado
- [ ] Archivo HEX copiado a Descargas
- [ ] PIC programado correctamente
- [ ] LED parpadea como se espera

---

# 🎉 ¡FELICIDADES!

Has instalado exitosamente **SDCC 4.5.0** en Termux y compilado tu primer programa en C para PIC.

**Ahora puedes:**

✅ Compilar código C complejo para PIC
✅ Usar todas las capacidades de un compilador moderno
✅ Desarrollar aplicaciones más complejas
✅ Compartir y reutilizar código fácilmente
✅ Programar múltiples PICs desde tu Android

**Próximos pasos sugeridos:**

1. Experimenta con diferentes valores de `delay()`
2. Usa múltiples LED en diferentes pines
3. Implementa entrada con botones (PORTB como entrada)
4. Usa PWM para controlar intensidad de LED
5. Implementa UART para comunicación serial
6. Explora interrupciones en SDCC

---

## 📝 NOTAS FINALES

**Importante:**
- SDCC es software libre pero algunas características requieren GPUTILS (no libre)
- La compilación es larga pero solo hay que hacerlo una vez
- Una vez instalado, SDCC seguirá disponible para futuros proyectos
- Puedes crear múltiples programas sin reinstalar SDCC

**Recursos del usuario:**
- Mantén copias de backup de `prueba_led.hex` y otros programas
- Documenta cualquier modificación que hagas a los comandos
- Crea una carpeta de proyectos en `~/storage/downloads/` para organizarte

---

**Versión:** 1.0
**Fecha:** 26 de Enero de 2026
**Plataforma:** Termux Google Play en Android
**Compilador:** SDCC 4.5.0
**Dependencia:** GPUTILS 1.5.2

---