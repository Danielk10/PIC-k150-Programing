# 🧠 Guía de Mejoras Técnicas Futuras: Autodetección de Chip PIC y Control Manual de Conexión USB en K150

> **Estado:** Documento de Diseño e Implementación Futura  
> **Fecha de Certificación:** 7 de Octubre de 2026  
> **Aplicación Target:** `PIC k150 Programming` (`com.diamon.pic`)  
> **Microcontrolador Validado en Pruebas Físicas:** Microchip PIC16F628A (Device ID: `0x6810` / `0x1060`)

---

## 📌 1. Resumen Ejecutivo y Objetivo

El objetivo de este documento de diseño es estructurar dos mejoras críticas de experiencia de usuario y robustez de hardware para futuras versiones:

1. **Detectar automáticamente la presencia física de un microcontrolador** en el zócalo ZIF de 40 pines del programador K150.
2. **Identificar el modelo exacto del chip (Device ID)** leyendo el registro de configuración del silicio a través del protocolo P18A.
3. **Sincronizar automáticamente la interfaz de usuario**:
   - Mover el selector (`Spinner`) al microcontrolador detectado (ej. `16F628A`).
   - Cargar inmediatamente sus variables de programación (`iniciarVariablesDeProgramacion`).
   - Actualizar el diagrama visual de orientación de pines en el zócalo (ej. *Pin 1 del chip en Pin 2 del ZIF*).
   - Ajustar el interruptor de *Modo ICSP* y mapa de fusibles correspondientes.
4. **Conservar siempre la selección manual**: El usuario mantiene el control absoluto para cambiar manualmente de chip en el desplegable en cualquier momento o forzar un modelo si el chip no cuenta con Device ID legible.
5. **Conectar y desconectar el programador K150 con un botón en la UI**:
   - Permitir encender/apagar la comunicación lógica con el dispositivo a voluntad.
   - **Reconexión transparente sin solicitar permisos USB de nuevo** mientras el cable permanezca físicamente enchufado.
   - Operación desacoplada del reset DTR gracias a la secuencia determinista de rescate por software (`0x01 -> 'Q' -> 'P' -> 'P'`).

---

## 🔬 2. Viabilidad Técnica y Fundamento del Hardware

### A. ¿Se puede saber si hay o no un chip en el zócalo?
**SÍ, 100% FACTIBLE.**  
El firmware maestro del K150 (protocolo Kitsrus P18A) implementa por hardware el comando de test de zócalo:
* **Comando:** `cmdDetectarEnSocket` (`0x12` / `18` decimal en P18A).
* **Comportamiento:** El K150 aplica una señal de sensado en las líneas del zócalo ZIF para medir la presencia de carga.
* **Respuesta del Programador:**
  - Si hay chip: Retorna el byte `'A'` (`0x41`) seguido de `'Y'` (`0x59`).
  - Si el zócalo está vacío o la palanca levantada: No retorna `'Y'` o falla el timeout.
* **Implementación actual:** Ya existe en [`ProtocoloP18A.java`](app/src/main/java/com/diamon/protocolo/ProtocoloP18A.java#L1042):
  ```java
  public boolean detectarPicEnElSocket() {
      resetearComandos();
      usbSerialPort.write(new byte[] { (byte) tipoProtocolo.getCmdDetectarEnSocket() }, 10);
      byte[] response = new byte[1];
      if (leerRespuesta(response, 'A', "...")) { ... }
      if (leerRespuesta(response, 'Y', "...")) {
          resetearComandos();
          return true;
      }
      return false;
  }
  ```

---

### B. ¿Se puede saber qué chip específico es (Device ID)?
**SÍ, PARA LA GRAN MAYORÍA DE CHIPS FLASH (PIC12F, PIC16F, PIC18F).**

#### 1. Arquitectura de Identificación de Silicio en Microchip PIC
En los microcontroladores Microchip con memoria Flash, existe una dirección de memoria reservada en el bloque de configuración de fábrica:
* **Familia 14-bit Flash (PIC16F628A, PIC16F877A, PIC16F88, PIC16F627A, PIC16F84A, etc.):**
  - Dirección: `0x2006`.
  - Longitud: 14 bits.
  - Formato: `[13:5] Device ID` + `[4:0] Revision ID`.
* **Familia 16-bit Flash (PIC18F2550, PIC18F4550, PIC18F2455, etc.):**
  - Direcciones: `0x3FFFFE` (`DEVID1`) y `0x3FFFFF` (`DEVID2`).

#### 2. Evidencia Experimental de Nuestras Pruebas en Hardware Real
Durante la validación física del **PIC16F628A** en el zócalo ZIF:
1. Se invocó la lectura del bloque de configuración (Comando `0x0D` / `13`).
2. El hardware devolvió los primeros bytes correspondientes al Device ID: **`0x6810`**.
3. **Análisis de endianness y máscara de revisión:**
   - Bytes leídos: `0x68` (byte bajo) y `0x10` (byte alto).
   - En orden Big-Endian de palabra PIC: `0x1068`.
   - Máscara de silicio (descartando los 5 bits de revisión `0x1F`):
     $$\text{Device Code} = \text{Word} \ \& \ \text{0xFFE0} = \text{0x1068} \ \& \ \text{0xFFE0} = \mathbf{0x1060}$$
4. En el catálogo oficial [`chipinfo.cid`](app/src/main/assets/chipinfo.cid#L1848), la definición para el PIC16F628A especifica textualmente:
   ```text
   CHIPname=16F628A
   ChipID=1060
   ```
   **¡La coincidencia entre el silicio real y la base de datos es del 100%!**

#### 3. Casos Excepcionales (Chips OTP antiguos)
* Microcontroladores antiguos basados en EPROM/OTP (como PIC12C508, PIC16C54, etc.) carecen de registro Device ID en silicio o requieren secuencias de alta tensión propietarias.
* Para estos modelos, la detección automática indicará: *"Chip detectado en socket, pero el modelo no reporta Device ID. Por favor selecciónelo manualmente en la lista."*

---

## 🔄 3. Diagrama de Flujo del Algoritmo Propuesto

```mermaid
flowchart TD
    A["Usuario pulsa 'Detectar PIC' (o evento USB Connect)"] --> B{"¿Hay programador K150 conectado?"}
    B -- No --> C["Mostrar error: Conecte el programador USB"]
    B -- Sí --> D["Enviar Comando 18 (Sensado de Zócalo)"]
    D --> E{"¿Retorna 'A' e 'Y'?"}
    E -- No --> F["⚠️ Advertencia: No se detecta chip en el zócalo ZIF (revise palanca)"]
    E -- Sí --> G["✓ Chip presente en el zócalo"]
    G --> H["Enviar Comando 13 (Leer Bloque de Configuración)"]
    H --> I["Extraer Bytes 0-1 (Device ID en bruto)"]
    I --> J["Aplicar normalización Big-Endian y Máscara de Revisión (& 0xFFE0)"]
    J --> K{"¿Device ID coincide con algún chip en chipinfo.cid?"}
    
    K -- Sí --> L["🎯 Chip Identificado: ej. '16F628A'"]
    L --> M["Sincronizar Spinner: chipSpinner.setSelection(posicion)"]
    M --> N["Disparar onChipSelected():\n1. Cargar variables de programación (Cmd 3)\n2. Actualizar imagen de zócalo (Pin 1 en Pin 2)\n3. Actualizar Switch ICSP y Fuses"]
    N --> O["Notificar en Log: '✓ PIC detectado e identificado: 16F628A (ID: 0x6810)'"]
    
    K -- No --> P["⚠️ Chip detectado en zócalo, pero Device ID (0xXXXX) desconocido o no legible"]
    P --> Q["Mantener selección manual actual en el Spinner y solicitar al usuario seleccionar"]
```

---

## 🛠️ 4. Plan de Implementación Paso a Paso en el Código

### Paso 1: Base de Datos de Chips (`ChipinfoReader.java`)
Crear un índice inverso en memoria `Map<Integer, String> deviceIdToChipName` al inicializar la base de datos:

```java
public class ChipinfoReader {
    private final Map<Integer, String> deviceIdMap = new HashMap<>();

    // Dentro de procesarBaseDeDatos():
    if (linea.startsWith("ChipID=")) {
        String idHex = linea.substring(7).trim();
        try {
            int chipId = Integer.parseInt(idHex, 16);
            deviceIdMap.put(chipId, currentChipName);
        } catch (NumberFormatException ignored) {}
    }

    /**
     * Busca el modelo de PIC correspondiente a un Device ID en bruto.
     * Soporta coincidencia directa y con máscara de revisión (0xFFE0).
     */
    public String buscarModeloPorDeviceID(int rawWord) {
        // 1. Probar valor directo
        if (deviceIdMap.containsKey(rawWord)) {
            return deviceIdMap.get(rawWord);
        }
        // 2. Probar enmascarando los 5 bits de revisión de Microchip
        int maskedWord = rawWord & 0xFFE0;
        if (deviceIdMap.containsKey(maskedWord)) {
            return deviceIdMap.get(maskedWord);
        }
        // 3. Probar con bytes invertidos (Little-Endian a Big-Endian)
        int swappedWord = ((rawWord & 0xFF) << 8) | ((rawWord >> 8) & 0xFF);
        if (deviceIdMap.containsKey(swappedWord)) {
            return deviceIdMap.get(swappedWord);
        }
        int swappedMasked = swappedWord & 0xFFE0;
        if (deviceIdMap.containsKey(swappedMasked)) {
            return deviceIdMap.get(swappedMasked);
        }
        return null;
    }
}
```

---

### Paso 2: Capa de Protocolo (`ProtocoloP18A.java`)
Añadir el método específico para extraer el Device ID sin necesidad de invocar una verificación completa:

```java
public int leerDeviceIDDelSocket() throws UsbCommunicationException {
    String configData = leerDatosDeConfiguracionDelPic();
    if (configData == null || configData.startsWith("Error") || configData.length() < 4) {
        return -1;
    }
    try {
        // Primeros 4 caracteres hexadecimales corresponden a los 2 bytes del Device ID
        String idHex = configData.substring(0, 4);
        return Integer.parseInt(idHex, 16);
    } catch (NumberFormatException e) {
        return -1;
    }
}
```

---

### Paso 3: Gestor de Programación (`PicProgrammingManager.java`)
Implementar la estructura de resultado de autodetección:

```java
public static class ResultadoAutodeteccion {
    public final boolean chipEnSocket;
    public final int rawDeviceId;
    public final String modeloIdentificado;

    public ResultadoAutodeteccion(boolean enSocket, int deviceId, String modelo) {
        this.chipEnSocket = enSocket;
        this.rawDeviceId = deviceId;
        this.modeloIdentificado = modelo;
    }
}

public ResultadoAutodeteccion autodectarChip(ChipinfoReader reader) {
    if (protocolo == null) return new ResultadoAutodeteccion(false, -1, null);
    
    boolean enSocket = protocolo.detectarPicEnElSocket();
    if (!enSocket) {
        return new ResultadoAutodeteccion(false, -1, null);
    }
    
    int rawId = protocolo.leerDeviceIDDelSocket();
    String modelo = (rawId > 0 && reader != null) ? reader.buscarModeloPorDeviceID(rawId) : null;
    
    return new ResultadoAutodeteccion(true, rawId, modelo);
}
```

---

### Paso 4: Interfaz de Usuario y Sincronización (`MainActivity.java`)
Modificar la acción de `executeDetectChip()`:

```java
private void executeDetectChip() {
    new Thread(() -> {
        runOnUiThread(() -> appendLog("🔍 Detectando chip en el zócalo..."));
        
        PicProgrammingManager.ResultadoAutodeteccion res = 
            programmingManager.autodectarChip(chipSelectionManager.getChipReader());
            
        runOnUiThread(() -> {
            if (!res.chipEnSocket) {
                appendLog("⚠ No se detectó ningún PIC en el zócalo ZIF (verifique orientación y palanca)");
                return;
            }
            
            appendLog("✓ PIC detectado físicamente en el zócalo");
            
            if (res.modeloIdentificado != null) {
                String hexStr = String.format("0x%04X", res.rawDeviceId);
                appendLog("🎯 Identificado automáticamente: " + res.modeloIdentificado + " (ID: " + hexStr + ")");
                
                // Sincronizar el Spinner al modelo detectado
                chipSelectionManager.seleccionarModeloEnSpinner(chipSpinner, res.modeloIdentificado);
                
                Toast.makeText(MainActivity.this, 
                    "Chip detectado: " + res.modeloIdentificado, Toast.LENGTH_SHORT).show();
            } else {
                String hexStr = (res.rawDeviceId > 0) ? String.format("0x%04X", res.rawDeviceId) : "Desconocido";
                appendLog("ℹ Chip presente (ID: " + hexStr + "). Seleccione el modelo en la lista manual.");
            }
        });
    }).start();
}
```

---

### Paso 5: Desacoplamiento de Botones de Hardware (Mejora Crítica de UX)
En la versión actual (`MainActivity.java:642`), todos los botones de operaciones comienzan deshabilitados (`enabled=false`) y sólo se activan cuando se carga un archivo HEX.

**Refactorización Recomendada:**
1. **Estado `HardwareConectado`:**  
   Al conectar el K150 y seleccionar chip (manual o automáticamente), habilitar inmediatamente:
   - `btnDetectarPic` (Detectar PIC)
   - `btnLeerMemoriaDeLPic` (Leer Memoria / Dump)
   - `btnBorrarMemoriaDeLPic` (Borrar Memoria)
   - `btnBlankCheck` (Verificar Borrado)
2. **Estado `FirmwareCargado`:**  
   Al cargar un archivo `.HEX` o `.BIN`, habilitar adicionalmente:
   - `btnProgramarPic` (Programar PIC)
   - `btnVerificarMemoriaDelPic` (Verificar Memoria contra HEX)
   - `btnConfigureFuses` (Configurar Fusibles)

---

## 🎯 5. Beneficios para el Usuario Final

| Característica | Comportamiento Actual | Con la Autodetección Implementada |
| :--- | :--- | :--- |
| **Detección de Chip** | Solo responde si hay o no chip en socket, no identifica cuál. | Detecta presencia **e identifica el modelo exacto** (ej. `16F628A`). |
| **Configuración de UI** | El usuario debe buscar manualmente entre más de 100 chips en el Spinner. | **El Spinner salta automáticamente al modelo detectado** y carga todos los parámetros. |
| **Carga de Variables K150** | Requiere selección manual del desplegable. | Se transmiten las variables de programación de forma transparente al conectar/detectar. |
| **Diagrama de Zócalo** | Se actualiza tras selección manual. | **Muestra instantáneamente la posición correcta** (ej. *Pin 1 en Pin 2*). |
| **Flexibilidad Manual** | Disponible. | **100% conservada** (el usuario puede anular la autodetección y elegir manualmente). |
| **Volcado previo de memoria** | Requiere cargar un archivo HEX ficticio para habilitar botones. | **Directo e intuitivo:** inserta el chip, pulsa "Leer Memoria" y obtiene el dump. |

---

## 🔌 6. Propuesta Técnica: Botón de Conexión y Desconexión USB en la UI

### A. Objetivo
Permitir al usuario conectar y desconectar el programador K150 de forma explícita y manual mediante un botón en la interfaz de usuario (UI), sin depender únicamente de la inicialización automática al arrancar la app ni de desenchufar físicamente el cable USB-OTG.

---

### B. Análisis de Viabilidad: Permisos USB en Android
> **Pregunta Clave:** ¿Es posible reconectar con el botón **SIN solicitar permisos de nuevo** al usuario?  
> **Respuesta Técnica:** **SÍ, 100% FACTIBLE.**

#### Fundamento del Framework de Android (`UsbManager`):
1. **Asignación de Permisos:** En Android ([`android.hardware.usb.UsbManager`](https://developer.android.com/reference/android/hardware/usb/UsbManager)), la concesión de permisos otorgada por el usuario en el modal del sistema se asocia al identificador del objeto físico `UsbDevice` en el subsistema del kernel de Linux mientras el hardware permanezca energizado y conectado al bus USB.
2. **Desconexión Lógica vs Física:**
   * Al presionar el botón **"Desconectar"**, la app ejecuta:
     ```java
     usbSerialPort.close();
     connection.close();
     ```
   * Esto libera el descriptor de archivo (`fd`) del puerto serie en espacio de usuario y finaliza los hilos de escucha, **pero NO destruye el objeto `UsbDevice` del sistema ni revoca el permiso concedido**.
   * Por tanto, `usbManager.hasPermission(driver.getDevice())` **continúa devolviendo `true`**.
3. **Reconexión Silenciosa e Inmediata:**
   * Al presionar el botón **"Conectar"**, el gestor verifica inmediatamente `if (usbManager.hasPermission(device))`.
   * Al ser afirmativo, abre el dispositivo directamente con `usbManager.openDevice(device)` sin disparar ningún diálogo emergente (`PendingIntent`).
   * El diálogo de permisos solo reaparecerá si el usuario desenchufa físicamente el cable USB-OTG del teléfono (`ACTION_USB_DEVICE_DETACHED`).

---

### C. Análisis con respecto al DTR en Android (vs PC)

En el documento de ingeniería [`sincronizacion_protocolo_k150_android.md`](file:///home/danielpdiamon/PIC-k150-Programing/sincronizacion_protocolo_k150_android.md), se explica que en PC (`picpro`), conectar o reconectar se basa en forzar un reset físico por **DTR** en el microcontrolador del programador (pin MCLR) para esperar la trama de Fase 1 (`'B\x03'`).

**¿Por qué en tu app Android conectar/desconectar por botón es completamente seguro y diferente?**
1. **Alimentación VBUS Continua:** Al estar conectado por USB-OTG, el teléfono alimenta de forma ininterrumpida al programador (5V continuos en VBUS). El microcontrolador PIC16F628A del K150 nunca se apaga cuando la app cierra el puerto serie.
2. **Independencia de DTR:** Tu app no requiere conmutar DTR ni forzar un reset eléctrico por hardware (que en chips clones PL-2303 / CH340 suele ser ineficaz).
3. **Resincronización Determinista por Software:** Al presionar "Conectar", la app no espera `'B\x03'`, sino que ejecuta la secuencia de rescate por software en la **Command Jump Table**:
   $$\text{App} \xrightarrow{0x01} \text{K150 responde 'Q'} \xrightarrow{\text{'P'}} \text{K150 responde 'P'}$$
   Esto garantiza que cada reconexión por software sea 100% determinista, limpia y sin timeouts, sin importar en qué estado haya quedado el buffer previo.

---

### D. Diseño de UI y Código Propuesto

#### 1. Ubicación en la Toolbar (`activity_main.xml`):
Complementar el actual `connectionIndicator` estático con un botón interactivo o chip de acción:
```xml
<LinearLayout
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="end"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:layout_marginEnd="8dp">

    <View
        android:id="@+id/connectionIndicator"
        android:layout_width="12dp"
        android:layout_height="12dp"
        android:layout_marginEnd="8dp"
        android:background="@drawable/connection_indicator" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnConnectUsb"
        style="@style/Widget.MaterialComponents.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="36dp"
        android:paddingHorizontal="8dp"
        android:text="Conectar"
        android:textColor="#FFFFFF"
        android:textSize="12sp" />
</LinearLayout>
```

#### 2. Métodos en `UsbConnectionManager.java`:
```java
/**
 * Conecta o reconecta al programador USB si ya se cuenta con permisos.
 */
public void connect() {
    if (isConnected()) {
        return;
    }
    drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
    if (drivers == null || drivers.isEmpty()) {
        notifyError(context.getString(R.string.dispositivo_sin_puertos_usb_di));
        return;
    }
    // Si ya fue autorizado previamente en esta sesión física, conecta inmediatamente sin diálogos
    requestPermissionsIfNeeded();
}

/**
 * Cierra la conexión lógica con el programador liberando el puerto serie.
 */
public void disconnect() {
    cleanupConnection();
    if (connectionListener != null) {
        connectionListener.onDisconnected();
    }
}
```

#### 3. Controlador en `MainActivity.java`:
```java
private void setupConnectionButton() {
    btnConnectUsb = findViewById(R.id.btnConnectUsb);
    btnConnectUsb.setOnClickListener(v -> {
        if (usbManager.isConnected()) {
            usbManager.disconnect();
        } else {
            appendLog("🔌 Conectando al programador...");
            usbManager.connect();
        }
    });
}

@Override
public void onConnected() {
    runOnUiThread(() -> {
        connectionIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        btnConnectUsb.setText("Desconectar");
        btnConnectUsb.setTextColor(Color.parseColor("#FF5252"));
        // Habilitar botones de hardware según estado
    });
}

@Override
public void onDisconnected() {
    runOnUiThread(() -> {
        connectionIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        btnConnectUsb.setText("Conectar");
        btnConnectUsb.setTextColor(Color.parseColor("#4CAF50"));
        // Deshabilitar botones de hardware
    });
}
```

---

## 🏁 Conclusión y Hoja de Ruta

Tanto la **autodetección de chip en el zócalo** como el **botón de conexión/desconexión manual sin re-solicitud de permisos** son adiciones 100% viables, seguras y de alto valor para la experiencia de usuario:
1. No requieren modificaciones de hardware ni flasheo del microcontrolador interno del K150.
2. Respetan plenamente la arquitectura determinista del protocolo P18A en Android USB-OTG sin dependencia del DTR.
3. Se integran limpiamente en la arquitectura de managers existente (`UsbConnectionManager`, `PicProgrammingManager`, `MainActivity`).

