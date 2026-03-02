# AGENTS.md - Reglas de Desarrollo para PIC k150 Programming

Este documento establece las reglas, estándares y convenciones para el desarrollo del proyecto **PIC k150 Programming**, una aplicación Android para programar microcontroladores PIC. Todas las contribuciones de código deben seguir estas directrices.

## 📱 Información del Proyecto

- **Nombre**: PIC k150 Programming
- **Lenguaje**: Java
- **Plataforma**: Android (API 23-36)
- **Arquitectura**: Programación Orientada a Objetos con Patrón Manager
- **Protocolo**: P018 de KITSRUS
- **Compilación**: Java 11 (Compatibilidad Android Marshmallow/16)
- **Build Tools**: Gradle (SDK 36)

## 🏗️ Arquitectura del Proyecto

### Estructura de Paquetes

El proyecto sigue una arquitectura modular y delegada mediante managers:

```
com.diamon/
├── audio/           # Manejo de sonido y música de la aplicación
├── chip/            # Lógica específica de chips PIC y configuraciones
├── datos/           # Procesamiento de datos, archivos HEX y configuraciones
├── excepciones/     # Excepciones personalizadas para manejo robusto de errores
├── graficos/        # Renderizado gráfico 2D y texturas
├── managers/        # Lógica de negocio extraída de las Activities (Delegación)
├── nucleo/          # Interfaces abstractas y contratos del núcleo
├── pic/             # Activities principales y punto de entrada
├── politicas/       # Políticas de privacidad y términos
├── protocolo/       # Implementaciones específicas de protocolos
├── publicidad/      # Integración con servicios de publicidad
├── tutorial/        # Sistema de guías y ayuda al usuario
└── utilidades/      # Clases de utilidad, helpers y PantallaCompleta
```

### Principios Arquitectónicos

1. **Separación de Responsabilidades**: Cada paquete tiene una función específica y bien definida
2. **Abstracciones en Núcleo**: Todas las funcionalidades principales se definen como interfaces y clases abstractas en `nucleo`
3. **Implementaciones Específicas**: Las implementaciones concretas se ubican en paquetes especializados
4. **Modularidad**: Componentes intercambiables y extensibles

## 🎯 Programación Orientada a Objetos

### Principios Fundamentales

#### 1. Encapsulación
```java
public class ChipPic {
    private Map<String, Object> variablesDeChip;
    private HashMap<String, String> variablesProgramacion;
    
    // Getters y setters públicos para acceso controlado
    public HashMap<String, String> getVariablesDeProgramacion() {
        // Lógica de procesamiento antes de retornar
        variablesProgramacion.put("rom_size", "" + variablesDeChip.get("rom_size"));
        return variablesProgramacion;
    }
}
```

#### 2. Herencia y Polimorfismo
```java
// Clase abstracta base
public abstract class Protocolo {
    protected Context contexto;
    protected UsbSerialPort usbSerialPort;
    
    // Métodos abstractos que deben implementar las subclases
    public abstract boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firmware);
}

// Implementación específica
public class ProtocoloP18A extends Protocolo {
    @Override
    public boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firmware) {
        // Implementación específica del protocolo P018
    }
}
```

#### 3. Interfaces para Contratos
```java
public interface Datos {
    public InputStream leerDatoExterno(String nombre) throws IOException;
    public OutputStream escribirDatoExterno(String nombre) throws IOException;
    public InputStream leerDatoInterno(String nombre) throws IOException;
}
```

### Patrones de Diseño Utilizados

#### 1. Strategy Pattern
- **Ubicación**: Protocolos de comunicación
- **Uso**: Diferentes implementaciones de protocolos intercambiables

#### 2. Template Method Pattern
- **Ubicación**: Clase `Protocolo`
- **Uso**: Métodos base con implementaciones específicas en subclases

#### 3. Factory Pattern
- **Ubicación**: Creación de texturas y recursos
- **Uso**: `Recurso.cargarTextura()`, `Recurso.cargarSonido()`

#### 4. Singleton Pattern (implícito)
- **Ubicación**: Clases de utilidades
- **Uso**: Gestión de recursos únicos

#### 5. Manager Pattern (Core)
- **Ubicación**: Paquete `com.diamon.managers`
- **Uso**: Delegar lógica compleja fuera de `MainActivity`. Ejemplos: `UsbConnectionManager`, `PicProgrammingManager`, `FileManager`.
- **Regla**: Las Activities NO deben contener lógica de protocolos o archivos; deben delegarla a un Manager.

## 📱 Desarrollo Android Moderno

### Soporte Android 15/16 y Edge-to-Edge
El proyecto está optimizado para las últimas versiones de Android.

1. **Edge-to-Edge**: Obligatorio para Android 15+. Se usa `PantallaCompleta.habilitarEdgeToEdge()` antes de `setContentView`.
2. **Window Insets**: Manejo de paddings dinámicos para evitar que el contenido quede bajo la barra de navegación o estado.
3. **Inmersión**: Uso de `controller.hide(Type.systemBars())` para modo de programación ininterrumpido.

### Gestión Segura de Vistas y Contexto
```java
// Ejemplo de inicialización en MainActivity
pantallaCompleta = new PantallaCompleta(this);
pantallaCompleta.habilitarEdgeToEdge();
pantallaCompleta.ocultarBotonesVirtuales();
```

## 📝 Convenciones de Nomenclatura

### Variables
```java
// Variables de instancia - camelCase, descriptivas en español
private HashMap<String, String> variablesProgramacion;
private Map<String, Object> variablesDeChip;
private boolean procesoGrabado;
private StringBuffer firware;

// Variables locales - camelCase, nombres descriptivos
int numeroRegistro = 0;
String modelosPic = "PIC16F84A";
boolean respuesta = protocolo.borrarMemoriasDelPic();
```

### Métodos
```java
// Métodos públicos - camelCase, descriptivos, verbos en español
public boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firware)
public String obtenerVersionOModeloDelProgramador()
public void iniciarProcesamientoDeDatos()
public boolean detectarPicEnElSocket()

// Métodos privados - camelCase, descriptivos
private void cargarDatos()
private void mostrarInformacionPic(String modelo)
private DatosFuses parseLine(String line)
```

### Clases
```java
// PascalCase, nombres descriptivos en español
public class ChipPic { }
public class ProtocoloP18A { }
public class DatosPicProcesados { }
public class MostrarPublicidad { }
public class ChipinfoReader { }
```

### Constantes
```java
// UPPER_CASE con guiones bajos
private static final String ACTION_USB_PERMISSION = "com.diamon.pic.USB_PERMISSION";
private static final int REQUEST_CODE_OPEN_FILE = 1;
private static final int COLOR_PRIMARY = Color.parseColor("#003366");
private static final int NUMERO_DE_REGISTROS_DATOS = 6617;
```

### Interfaces
```java
// Nombres descriptivos de capacidades
public interface Protocolo { }
public interface Datos { }
public interface Graficos { }
public interface Publicidad { }
```

## 🛠️ Manejo de Excepciones

### Excepciones Personalizadas
```java
public class InvalidRecordException extends Exception {
    public InvalidRecordException(String message) {
        super(message);
    }
}

public class InvalidChecksumException extends Exception {
    public InvalidChecksumException(String message) {
        super(message);
    }
}
```

### Patrón de Manejo
```java
// Patrón típico: Try-catch con delegación a UI thread
new Thread(() -> {
    try {
        boolean success = programmingManager.programChip(currentChip, firmware, idToUse, fusesToUse);
        runOnUiThread(() -> dialogManager.updateProgrammingResult(success));
    } catch (Exception e) {
        runOnUiThread(() -> processStatusTextView.setText("Error: " + e.getMessage()));
    }
}).start();
```

### Resiliencia y Estabilidad del Sistema
Para errores globales o del sistema fuera de nuestro control (como fallos en el SDK de anuncios o broadcasts del sistema), se implementa un handler global en `PicApplication`:

```java
// En PicApplication.java
private void setupUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
        if (isSystemException(throwable)) {
            Log.w(TAG, "Absorbed system exception: " + throwable.getClass().getSimpleName());
            return; // Absorber sin crashear
        }
        // ... handler por defecto para el resto
    });
}
```

### Comunicación Segura (SafeBroadcastManager)
Para Android 13+ (API 33+), se debe usar `SafeBroadcastManager` para asegurar el cumplimiento de exportación de receivers:

```java
safeManager.registerReceiver(receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
```

## 💬 Estilo de Comentarios

### Comentarios de Clase
```java
/**
 * Clase para procesar y manejar datos específicos de chips PIC.
 * Maneja la conversión de archivos HEX y configuraciones de memoria.
 */
public class DatosPicProcesados {
```

### Comentarios de Método
```java
/**
 * Filtra registros de un archivo HEX por rango de direcciones.
 *
 * @param records Lista de registros HEX como pares (dirección, datos en string).
 * @param lowerBound Límite inferior del rango.
 * @param upperBound Límite superior del rango.
 * @return Nueva lista de registros dentro del rango especificado.
 */
public static List<HexFileUtils.Pair<Integer, String>> rangeFilterRecords(...)
```

### Comentarios Explicativos
```java
// Comando para programar ROM
usbSerialPort.write(new byte[] {0x07}, 10);

// Detectar si los datos ROM son big-endian o little-endian
boolean swapBytes = false;
boolean swapBytesDetected = false;

// Parámetros de inicialización del chip
int romSize = chipPIC.getTamanoROM(); // Tamaño de ROM
int eepromSize = chipPIC.getTamanoEEPROM(); // Tamaño de EEPROM
```

### Comentarios TODO y FIXME
```java
// TODO: Implementar verificación adicional de checksums
// FIXME: Optimizar el algoritmo de detección de endianness
// No va - para código que debe ser revisado
```

## 🔧 Estándares de Codificación

### Formato de Código
```java
// Llaves en nueva línea para clases y métodos
public class ChipPic 
{
    public boolean programarMemoria() 
    {
        if (condicion) 
        {
            // Código
        } 
        else 
        {
            // Código alternativo
        }
    }
}
```

### Manejo de Strings
```java
// Usar StringBuilder/StringBuffer para concatenaciones múltiples
StringBuffer datos = new StringBuffer();
for (int i = 0; i < leidos; i++) {
    datos.append(String.format("%02X", buffer[i]));
}

// Usar String.format para formateo
String addressHex = String.format("%04X", address);
```

### Validación de Parámetros
```java
public boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firware) {
    // Validación temprana
    if (wordCount > chipPIC.getTamanoROM()) {
        return false;
    }
    
    if ((wordCount * 2) % 32 != 0) {
        return false;
    }
    
    // Lógica principal
}
```

## 🎨 Interfaz de Usuario

### Creación Programática de UI
```java
// Usar createIconButton para botones consistentes
private Button createIconButton(String text, @DrawableRes int iconRes) {
    Button button = new Button(this, null, android.R.attr.buttonStyle);
    button.setText(text);
    button.setBackgroundResource(R.drawable.button_background);
    button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
    // ... configuración adicional
    return button;
}
```

### Colores y Temas
```java
// Definir constantes para colores del tema
private static final int COLOR_PRIMARY = Color.parseColor("#003366");
private static final int COLOR_SECONDARY = Color.parseColor("#FF6600");
private static final int COLOR_BACKGROUND = Color.parseColor("#1A1A2E");
private static final int COLOR_TEXT = Color.parseColor("#E0E0E0");
```

## 📊 Manejo de Datos

### Procesamiento de Archivos HEX
```java
// Usar clases específicas para procesamiento
public class HexProcesado {
    public class HexRecord {
        public int address;
        public byte[] data;
    }
    
    // Validación de checksums
    int checksumTest = (256 - checksumTest) % 256;
    if (checksumTest != checksum) {
        throw new InvalidChecksumException("Checksum incorrecto");
    }
}
```

### Configuración de Chips
```java
// Usar Maps para configuraciones flexibles
private Map<String, Object> variablesDeChip;
private HashMap<String, String> variablesProgramacion;

// Métodos de acceso controlado
public HashMap<String, String> getVariablesDeProgramacion() {
    variablesProgramacion.put("rom_size", "" + variablesDeChip.get("rom_size"));
    return variablesProgramacion;
}
```

## 🔗 Comunicación USB

### Protocolo de Comunicación
```java
// Envío de comandos con validación
protected void enviarComando(String comando) {
    usbSerialPort.write(new byte[] {0x01}, 100);
    expectResponse(new byte[] {'Q'}, 500);
    usbSerialPort.write(new byte[] {'P'}, 100);
    // ... resto de la lógica
}

// Lectura con timeout
protected byte[] readBytes(int count, int timeoutMillis) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.allocate(count);
    long startTime = System.currentTimeMillis();
    
    while (byteBuffer.position() < count && 
           (System.currentTimeMillis() - startTime) < timeoutMillis) {
        // Lógica de lectura con manejo de timeout
    }
}
```

## 📱 Desarrollo Android

### Activities y Lifecycle
```java
@Override
protected void onPause() {
    publicidad.pausarBanner();
    super.onPause();
    wakeLock.release();
}

@Override
protected void onResume() {
    super.onResume();
    publicidad.resumenBanner();
    wakeLock.acquire();
}
```

### Permisos y Compatibilidad
```java
// Manejo de permisos por versión de API
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    openFilePicker();
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
    }
}
```

## 🧵 Programación Concurrente

### Uso de Hilos
```java
// Operaciones de programación en hilo separado
hiloGrabado = new Thread(new Runnable() {
    @Override
    public void run() {
        // Lógica de programación de chip
        
        // Actualización de UI en hilo principal
        runOnUiThread(() -> {
            proceso.setText(getString(R.string.memory_erased_successfully));
        });
    }
});
hiloGrabado.start();
```

## 🛡️ Seguridad y Validación

### Validación de Datos
```java
// Validación de rangos de memoria
if (wordCount > chipPIC.getTamanoROM()) {
    return false;
}

// Validación de formato
if ((wordCount * 2) % 32 != 0) {
    return false;
}

// Validación de direcciones
if (address % 2 != 0) {
    throw new IllegalArgumentException("ROM record starts on odd address.");
}
```

## 📖 Documentación

### Documentación de Context7
Para documentación actualizada de librerías, utilizar el servidor MCP `context7`:

```markdown
# Documentación Actualizada
Utilizar `resolve-library-id` seguido de `get-library-docs` para obtener documentación actualizada de cualquier librería utilizada en el proyecto.
```

## 🌐 Internacionalización

### Recursos Multiidioma
```xml
<!-- Estructura de recursos -->
res/values/strings.xml           # Español por defecto
res/values-en/strings.xml        # Inglés
res/values-de/strings.xml        # Alemán
res/values-ja/strings.xml        # Japonés
res/values-zh/strings.xml        # Chino
```

### Uso de Recursos
```java
// Siempre usar recursos para textos
mensaje.setText(getString(R.string.conectado));
proceso.setText(getString(R.string.memory_erased_successfully));
Toast.makeText(this, getString(R.string.select_valid_binary_file), Toast.LENGTH_SHORT).show();
```

## 🎯 Métricas y Calidad

### Tracking y Analytics
```java
// Inicialización de AppCenter para métricas
AppCenter.start(getApplication(), "c9a1ef1a-bbfb-443a-863e-1c1d77e49c18", Analytics.class, Crashes.class);
```

### Testing
```java
// Estructura de testing
app/src/test/java/                    # Unit tests
app/src/androidTest/java/            # Integration tests
```

## ⚠️ Reglas Importantes de Cumplimiento

1. **TODOS los comentarios de código DEBEN estar en español**
2. **TODOS los nombres de métodos DEBEN ser descriptivos en español**
3. **TODAS las respuestas y mensajes al usuario DEBEN estar en español**
4. **TODA la documentación técnica DEBE estar en español**
5. **El código DEBE seguir el Patrón Manager (Separación de Actividad y Lógica)**
6. **Las validaciones DEBEN hacerse al inicio de los métodos**
7. **El soporte Edge-to-Edge es obligatorio para todas las nuevas Activities**
8. **El manejo de excepciones DEBE ser resiliente y evitar crash por errores de sistema**
9. **La estructura de paquetes DEBE respetarse estrictamente**
10. **Context7 DEBE utilizarse para documentación actualizada de librerías externas**

---

**Versión**: 2.6.2  
**Última Actualización**: 2026  
**Mantenedor**: Danielk10  
**Licencia**: GPL-2.0 (Verificada en README)