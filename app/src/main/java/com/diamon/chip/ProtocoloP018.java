package com.diamon.chip;

import android.content.Context;

import com.diamon.datos.HexFileListo;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ProtocoloP018 {

    private Context contexto;

    private UsbSerialPort usbSerialPort;

    public ProtocoloP018(Context contexto, UsbSerialPort usbSerialPort) {

        this.contexto = contexto;

        this.usbSerialPort = usbSerialPort;
    }

    public boolean iniciarProtocolo() {

        if (usbSerialPort == null) {
            return false;
        }

        String comando = "P";

        try {

            byte[] data = comando.getBytes(StandardCharsets.US_ASCII);

            usbSerialPort.write(data, 100);

            byte[] respuesta = readBytes(1, 100);

            if (respuesta.length > 1) {

                clearBuffer();

                respuesta[0] = 'P';
            }

            if (respuesta[0] == 'P') {

                return true;
            } else {
                return false;
            }

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    private void clearBuffer() throws IOException {
        byte[] tmpBuffer = new byte[1024];

        while (usbSerialPort.read(tmpBuffer, 100) > 0) {
            // Consumir todos los datos pendientes

        }
    }

    private void expectResponse(byte[] expected, int timeoutMillis) throws IOException {
        byte[] response = readBytes(expected.length, timeoutMillis);
        if (!Arrays.equals(response, expected)) {
            throw new IOException(
                    "Expected: "
                            + Arrays.toString(expected)
                            + ", but received: "
                            + Arrays.toString(response));
        }
    }

    // Método para leer bytes del puerto serie
    private byte[] readBytes(int count, int timeoutMillis) throws IOException {

        if (usbSerialPort == null) {
            return null;
        }

        // Verificar que el número de bytes solicitados sea válido
        if (count <= 0) {
            throw new IllegalArgumentException("El número de bytes a leer debe ser mayor que 0.");
        }

        // Crear un buffer para almacenar los datos leídos
        ByteBuffer byteBuffer = ByteBuffer.allocate(count);
        long startTime = System.currentTimeMillis();

        // Mientras no se hayan leído todos los bytes y el tiempo de espera no haya expirado
        while (byteBuffer.position() < count
                && (System.currentTimeMillis() - startTime) < timeoutMillis) {
            // Leer los bytes restantes
            int remaining = count - byteBuffer.position();
            byte[] tmpBuffer = new byte[remaining];
            int bytesRead = usbSerialPort.read(tmpBuffer, timeoutMillis);

            if (bytesRead > 0) {
                // Añadir los bytes leídos al buffer
                byteBuffer.put(tmpBuffer, 0, bytesRead);
            } else if (bytesRead == 0) {
                // Si no se reciben datos, esperar brevemente antes de reintentar
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Hilo interrumpido durante la operación de lectura.", e);
                }
            }
        }

        // Si no se alcanzaron los bytes esperados dentro del tiempo de espera, lanzar una excepción
        if (byteBuffer.position() < count) {
            throw new IOException("Tiempo de espera agotado mientras se esperaban los datos.");
        }

        // Devolver los datos leídos como un arreglo de bytes
        return byteBuffer.array();
    }

    private void enviarComando(String comando) {

        if (usbSerialPort == null) {

            return;
        }
        try {
            byte[] data = new byte[comando.length()];

            for (int i = 0; i < comando.length(); i++) {

                data[i] = Byte.parseByte(comando);
            }

            usbSerialPort.write(new byte[] {0x01}, 100);

            expectResponse(new byte[] {'Q'}, 500);

            // Enviar 'P' para ir a la tabla de salto.
            usbSerialPort.write(new byte[] {'P'}, 100);

            byte[] ack = readBytes(1, 100);

            if (ack[0] != 'P') {

                throw new IOException("No se recibio P");
            }

            // Enviar el número del comando, si es necesario.
            if (data[0] != 0) {

                usbSerialPort.write(data, 100);
            }

        } catch (NumberFormatException e) {

        } catch (IOException e) {
        }
    }

    public boolean esperarInicioDeNuevoComando() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {0};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    private boolean researComandos() {

        enviarComando("0");

        return true;
    }

    public String hacerEco() {

        enviarComando("2");

        StringBuilder response = new StringBuilder();

        try {

            usbSerialPort.write(new byte[] {(byte) 2}, 100);

            response.append((char) readBytes(1, 500)[0]);

            researComandos();

            return response.toString();

        } catch (NumberFormatException e) {

            return response.toString();

        } catch (IOException e) {

            return response.toString();
        }
    }

    public boolean iniciarVariablesDeProgramacion(InformacionPic chipPIC) {

        enviarComando("3");

        StringBuilder respuesta = new StringBuilder();

        try {

            // Parámetros de inicialización
            int romSize = chipPIC.getTamanoROM(); // Tamaño de ROM
            int eepromSize = chipPIC.getTamanoEEPROM(); // Tamaño de EEPROM
            int coreType = chipPIC.getTipoNucleoVerdaderoPic(); // Core Type: 16F7x 16F7x7
            boolean flagCalibrationValueInROM = chipPIC.isFlagCalibration(); // Flag 0
            boolean flagBandGapFuse = chipPIC.isFlagBandGap(); // Flag 1
            boolean flagSinglePanelAccessMode = chipPIC.isFlag18fSingle(); // Flag 2
            boolean flagVccVppDelay = chipPIC.isFlagVccVppDelay(); // Flag 3
            int programDelay = chipPIC.getProgramDelay(); // 50 * 100µs = 5ms
            int powerSequence = chipPIC.getPowerSequence(); // Power Sequence: VPP2 ->
            // VCC
            int eraseMode = chipPIC.getEraseMode(); // Erase Mode: 16F7x7
            int programRetries = chipPIC.getProgramTries(); // Intentos de programación
            int overProgram = chipPIC.getOverProgram(); // Over Program
            // Construir los flags
            int flags = 0;
            // Crear el payload según el protocolo
            ByteBuffer payload = ByteBuffer.allocate(11);

            flags |= (flagCalibrationValueInROM ? 1 : 0); // Bit 0
            flags |= (flagBandGapFuse ? 2 : 0); // Bit 1
            flags |= (flagSinglePanelAccessMode ? 4 : 0); // Bit 2
            flags |= (flagVccVppDelay ? 8 : 0); // Bit 3
            payload.order(ByteOrder.BIG_ENDIAN); // Big-endian como el protocolo
            // requiere
            payload.putShort((short) romSize); // Bytes 1 y 2: ROM Size High y Low
            payload.putShort((short) eepromSize); // Bytes 3 y 4: EEPROM Size High y
            // Low
            payload.put((byte) coreType); // Byte 5: Core Type
            payload.put((byte) flags); // Byte 6: Flags
            payload.put((byte) programDelay); // Byte 7: Program Delay
            payload.put((byte) powerSequence); // Byte 8: Power Sequence
            payload.put((byte) eraseMode); // Byte 9: Erase Mode
            payload.put((byte) programRetries); // Byte 10: Program Tries
            payload.put((byte) overProgram); // Byte 11: Over Program

            usbSerialPort.write(payload.array(), 100);

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }

        try {

            respuesta.append(new String(readBytes(1, 100), StandardCharsets.US_ASCII));

            researComandos();

        } catch (IOException e) {

            return false;
        }

        if (respuesta.toString().equals("I")) {

            return true;

        } else {

            return false;
        }
    }

    public boolean activarVoltajesDeProgramacion() {

        enviarComando("4");

        StringBuffer respuesta = new StringBuffer();

        try {

            respuesta.append(new String(readBytes(1, 100), StandardCharsets.US_ASCII));

            if (respuesta.toString().equals("V")) {

                return true;

            } else {

                return false;
            }

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean desactivarVoltajesDeProgramacion() {

        enviarComando("5");

        StringBuffer respuesta = new StringBuffer();

        try {

            respuesta.append(new String(readBytes(1, 100), StandardCharsets.US_ASCII));

            if (respuesta.toString().equals("v")) {

                return true;

            } else {

                return false;
            }

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean reiniciarVoltajesDeProgramacion() {

        enviarComando("6");

        StringBuffer respuesta = new StringBuffer();

        try {

            respuesta.append(new String(readBytes(1, 100), StandardCharsets.US_ASCII));

            if (respuesta.toString().equals("V")) {

                return true;

            } else {

                return false;
            }

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public String programarROMPic(InformacionPic chipPIC, String datos) {
        // Procesar datos HEX
        HexFileListo hexProsesado = new HexFileListo(datos, chipPIC);
        hexProsesado.iniciarProcesamientoDatos();
        byte[] romData = hexProsesado.obtenerBytesHexROMPocesado();

        int wordCount = romData.length / 2; // Cantidad de palabras (2 bytes por palabra)

        // Validación: Verificar que el tamaño no exceda el límite del PIC
        if (wordCount > chipPIC.getTamanoROM()) {
            return "Error: Los datos exceden el tamaño máximo de ROM permitido.";
        }

        // Validación: Verificar que el tamaño sea múltiplo de 32 bytes
        if ((wordCount * 2) % 32 != 0) {
            return "Error: Los datos de la ROM deben ser múltiplos de 32 bytes.";
        }

        // Preparar mensaje para enviar el tamaño de palabras
        byte[] wordCountMessage = ByteBuffer.allocate(2).putShort((short) wordCount).array();

        try {
            // Inicializar comunicación y activar voltajes de programación
            researComandos();
            activarVoltajesDeProgramacion();

            // Comando para programar ROM
            usbSerialPort.write(new byte[] {0x07}, 10);

            // Enviar cantidad de palabras
            usbSerialPort.write(wordCountMessage, 100);

            // Validar respuesta inicial 'Y'
            byte[] response = new byte[1];
            if (!leerRespuesta(
                    response,
                    'Y',
                    "Error: No se recibió la respuesta esperada después de enviar el tamaño de palabras.")) {
                return "Error al programar";
            }

            // Enviar datos en bloques de 32 bytes
            for (int i = 0; i < romData.length; i += 32) {
                // Extraer un bloque de 32 bytes
                byte[] chunk = Arrays.copyOfRange(romData, i, Math.min(i + 32, romData.length));

                // Enviar el bloque y validar respuesta
                usbSerialPort.write(chunk, 10);
                if (!leerRespuesta(
                        response,
                        'Y',
                        "Error: No se recibió la respuesta esperada después de enviar un bloque de datos.")) {
                    return "Error al programar";
                }
            }

            // Validar respuesta final 'P'
            if (!leerRespuesta(
                    response, 'P', "Error: No se recibió la confirmación final de programación.")) {
                return "Error al programar";
            }

            // Desactivar voltajes y limpiar comandos
            desactivarVoltajesDeProgramacion();
            researComandos();

            return "ROM programada exitosamente.";
        } catch (IOException e) {
            return "Error: Ocurrió un error de comunicación durante la programación.";
        }
    }

    public String programarEEPROMPic(InformacionPic chipPIC, String datos) {

        HexFileListo hexProsesado = new HexFileListo(datos, chipPIC);

        hexProsesado.iniciarProcesamientoDatos();

        byte[] eepromData = hexProsesado.obtenerBytesHexEEPROMPocesado();

        int byteCount = eepromData.length;

        // Validación: Verificar que no exceda el tamaño de la EEPROM
        if (byteCount > chipPIC.getTamanoEEPROM()) {
            return "Error: Los datos exceden el tamaño máximo de la EEPROM.";
        }

        // Validación: Verificar que el tamaño sea múltiplo de 2 bytes
        if (byteCount % 2 != 0) {
            return "Error: Los datos de la EEPROM deben ser múltiplos de 2 bytes.";
        }

        // Preparar mensaje con la cantidad de bytes
        byte[] byteCountMessage = ByteBuffer.allocate(2).putShort((short) byteCount).array();

        try {
            // Inicializar comunicación y activar voltajes de programación
            researComandos();
            activarVoltajesDeProgramacion();

            // Comando para programar EEPROM
            usbSerialPort.write(new byte[] {0x08}, 10);

            // Enviar cantidad de bytes
            usbSerialPort.write(byteCountMessage, 100);

            // Validar respuesta inicial 'Y'
            byte[] response = new byte[1];
            if (!leerRespuesta(
                    response,
                    'Y',
                    "Error: No se recibió la respuesta esperada después de enviar el tamaño de bytes.")) {
                return "Error al programar EEPROM";
            }

            // Enviar datos en bloques de 2 bytes
            for (int i = 0; i < eepromData.length; i += 2) {
                byte[] chunk = Arrays.copyOfRange(eepromData, i, i + 2);

                // Enviar el bloque de 2 bytes y validar respuesta
                usbSerialPort.write(chunk, 10);
                if (!leerRespuesta(
                        response,
                        'Y',
                        "Error: No se recibió la respuesta esperada después de enviar un bloque de datos.")) {
                    return "Error al programar EEPROM";
                }
            }

            // Enviar 2 bytes adicionales al final (relleno)
            usbSerialPort.write(new byte[] {0x00, 0x00}, 10);

            // Validar respuesta final 'P'
            if (!leerRespuesta(
                    response, 'P', "Error: No se recibió la confirmación final de programación.")) {
                return "Error al programar EEPROM";
            }

            // Desactivar voltajes y limpiar comandos
            desactivarVoltajesDeProgramacion();
            researComandos();

            return "EEPROM programada exitosamente.";
        } catch (IOException e) {
            return "Error: Ocurrió un error de comunicación durante la programación.";
        }
    }

    // Método para leer respuesta y validar contra un carácter esperado
    private boolean leerRespuesta(byte[] response, char esperado, String mensajeError)
            throws IOException {
        usbSerialPort.read(response, 100);
        if (response[0] != (byte) esperado) {
            System.err.println(mensajeError);
            return false;
        }
        return true;
    }

    public String programarFusesIDPic(InformacionPic chipPIC, String datos) {
        try {
            // Procesar datos
            HexFileListo hexProcesado = new HexFileListo(datos, chipPIC);
            hexProcesado.iniciarProcesamientoDatos();

            // Obtener tipo de núcleo, ID y valores de FUSES
            int tipoNucleo = chipPIC.getTipoNucleoBit();
            byte[] id = hexProcesado.obtenerVoloresBytesHexIDPocesado();
            int[] fuses = hexProcesado.obtenerVoloresIntHexFusesPocesado();

            // Validar datos según tipo de núcleo
            if (tipoNucleo == 16) {
                if (id.length != 8) {
                    return "Error: El ID debe tener 8 bytes para núcleo de 16 bits.";
                }
                if (fuses.length != 7) {
                    return "Error: Debe haber exactamente 7 valores de FUSES para núcleo de 16 bits.";
                }
            } else if (tipoNucleo == 14) {
                if (id.length != 4) {
                    return "Error: El ID debe tener 4 bytes para núcleo de 14 bits.";
                }
                if (fuses.length < 1 || fuses.length > 2) {
                    return "Error: Debe haber 1 o 2 valores de FUSES para núcleo de 14 bits.";
                }
            } else {
                return "Error: Tipo de núcleo desconocido.";
            }

            // Reiniciar comandos y activar voltajes de programación
            researComandos();
            activarVoltajesDeProgramacion();

            // Enviar comando para programar FUSES e ID
            usbSerialPort.write(new byte[] {0x09}, 10);

            // Preparar cuerpo del comando
            ByteArrayOutputStream commandBody = new ByteArrayOutputStream();
            commandBody.write(new byte[] {0x30, 0x30}); // '0' '0' en ASCII

            if (tipoNucleo == 16) {
                commandBody.write(id);
                for (int fuse : fuses) {
                    commandBody.write(
                            ByteBuffer.allocate(2)
                                    .order(ByteOrder.LITTLE_ENDIAN)
                                    .putShort((short) fuse)
                                    .array());
                }
            } else { // tipoNucleo == 14
                commandBody.write(id);
                commandBody.write(new byte[] {'F', 'F', 'F', 'F'}); // 'FFFF' en ASCII
                commandBody.write(
                        ByteBuffer.allocate(2)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .putShort((short) fuses[0])
                                .array());
                for (int i = 0; i < 6; i++) {
                    commandBody.write(new byte[] {(byte) 0xFF, (byte) 0xFF});
                }
            }

            // Enviar comando preparado
            usbSerialPort.write(commandBody.toByteArray(), 100);

            // Leer respuesta
            byte[] response = new byte[1];
            usbSerialPort.read(response, 100);

            // Desactivar voltajes y limpiar comandos
            desactivarVoltajesDeProgramacion();
            researComandos();

            // Validar respuesta
            if (response[0] == 'Y') {

                return "ID y FUSES programados exitosamente.";

            } else if (response[0] == 'N') {

                return "Error: Falló la programación de ID y FUSES.";
            } else {
                return "Error: Respuesta inesperada del dispositivo.";
            }

        } catch (IOException e) {
            return "Error: Ocurrió un error de comunicación durante la programación.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public boolean calibracionDelPrograma() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {10};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public String leerMemoriaROMPic(InformacionPic chipPIC) {

        StringBuffer datos = new StringBuffer();

        try {
            // Tamaño total de la memoria ROM esperada
            int romSize = chipPIC.getTamanoROM() * 2; // Convertir palabras a bytes
            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques
            int bytesLeidos = 0;

            // Resetear y activar los voltajes de programación
            researComandos();

            activarVoltajesDeProgramacion();

            // Enviar el comando para leer ROM (11)
            usbSerialPort.write(
                    new byte[] {Byte.parseByte("11")}, 10); // Comando 11 en hexadecimal (0x0B)

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < romSize) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {
                        datos.append(String.format("%02X ", buffer[i]));
                    }
                    bytesLeidos += leidos;
                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }

            // Desactivar los voltajes de programación
            desactivarVoltajesDeProgramacion();

            researComandos();

            // Retornar los datos leídos en formato hexadecimal
            return datos.toString().trim();

        } catch (NumberFormatException e) {
            return "Error al leer Memoria ROM: " + e.toString();
        } catch (IOException e) {
            return "Error al leer Memoria ROM: " + e.toString();
        }
    }

    public String leerMemoriaEEPROMPic(InformacionPic chipPIC) {

        StringBuffer datos = new StringBuffer();

        try {
            // Tamaño total de la memoria ROM esperada
            int romSize = chipPIC.getTamanoEEPROM(); // Convertir palabras a bytes
            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques
            int bytesLeidos = 0;

            // Resetear y activar los voltajes de programación
            researComandos();
            activarVoltajesDeProgramacion();

            // Enviar el comando para leer ROM (12)
            usbSerialPort.write(
                    new byte[] {Byte.parseByte("12")}, 10); // Comando 12 en hexadecimal (0x0C)

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < romSize) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {
                        datos.append(String.format("%02X ", buffer[i]));
                    }
                    bytesLeidos += leidos;
                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }

            // Desactivar los voltajes de programación
            desactivarVoltajesDeProgramacion();

            researComandos();

            // Retornar los datos leídos en formato hexadecimal
            return datos.toString().trim();

        } catch (NumberFormatException e) {
            return "Error al leer Memoria ROM: " + e.toString();
        } catch (IOException e) {
            return "Error al leer Memoria ROM: " + e.toString();
        }
    }

    public String leerConfiguracionPic() {

        StringBuilder datos = new StringBuilder();

        try {
            // Resetear y activar los voltajes de programación
            researComandos();

            activarVoltajesDeProgramacion();

            // Comando para leer la configuración
            usbSerialPort.write(new byte[] {Byte.parseByte("13")}, 10); // 0x0D es 13 en hexadecimal

            int size = 26; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            // Leer el acknowledgment ('C')
            byte[] ack = new byte[1];
            int bytesRead = usbSerialPort.read(ack, 100);

            if (bytesRead != 1 || ack[0] != 'C') {
                return "Error: No se recibió el acknowledgment ('C')";
            }

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        datos.append(String.format("%02X ", buffer[i]));
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }

            // Desactivar voltajes y resetear comandos
            desactivarVoltajesDeProgramacion();

            researComandos();

            return datos.toString();
        } catch (IOException e) {
            return "Error al leer Datos de configuración: " + e.toString();
        }
    }

    public String leerCalibracionPic() {

        StringBuilder datos = new StringBuilder();

        try {
            // Resetear y activar los voltajes de programación
            researComandos();

            activarVoltajesDeProgramacion();

            // Comando para leer la configuración
            usbSerialPort.write(new byte[] {Byte.parseByte("14")}, 10); // 0x0E es 14 en hexadecimal

            int size = 2; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            byte[] bytes = new byte[size];

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        datos.append(String.format("%02X ", buffer[i]));

                        bytes[i] = buffer[i];
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }

            // Desactivar voltajes y resetear comandos
            desactivarVoltajesDeProgramacion();

            researComandos();

            return new String(bytes, "US-ASCII") + " " + datos.toString();

        } catch (IOException e) {
            return "Error al leer Datos de calibración: " + e.toString();
        }
    }

    public String borrarMemoriaPic() {

        try {
            // Resetear y activar los voltajes de programación
            researComandos();

            activarVoltajesDeProgramacion();

            // Comando para leer la configuración
            usbSerialPort.write(new byte[] {Byte.parseByte("14")}, 10);

            int size = 1; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            byte[] bytes = new byte[size];

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        bytes[i] = buffer[i];
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }

            // Desactivar voltajes y resetear comandos
            desactivarVoltajesDeProgramacion();

            researComandos();

            if (new String(bytes, "US-ASCII").equals("Y")) {

                return "PIC Borrado correctamente";

            } else {

                return "Error al Borrar PIC";
            }

        } catch (IOException e) {
            return "Error al Borrar " + e.toString();
        }
    }

    public String verificarBorradoMemoriaEEPROMPic() {

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener versión
            usbSerialPort.write(new byte[] {Byte.parseByte("16")}, 10); // 0x10 es 16 en decimal

            int size = 1; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            byte[] bytes = new byte[size];

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        bytes[i] = buffer[i];
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }
            researComandos();

            if (new String(bytes, "US-ASCII").equals("Y")) {

                return "La Memoria EEPROM se encuetra Vacia";

            } else if (new String(bytes, "US-ASCII").equals("N")) {

                return "La Memoria EEPROM contine datos";

            } else {

                return "Error al verificar memoria EEPROM";
            }

        } catch (IOException e) {

            return "Error al verificar memoria EEPROM: " + e.getMessage();
        }
    }

    public String programarFuses18F() {

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener versión
            usbSerialPort.write(new byte[] {Byte.parseByte("17")}, 10); // 0x10 es 16 en decimal

            int size = 1; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            byte[] bytes = new byte[size];

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        bytes[i] = buffer[i];
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }
            researComandos();

            if (new String(bytes, "US-ASCII").equals("Y")) {

                return "Fuses programdos correctamente";

            } else if (new String(bytes, "US-ASCII").equals("B")) {

                return "Error al programar fuses B";

            } else {

                return "Error al programar fuses";
            }

        } catch (IOException e) {

            return "Error al programar fuses: " + e.getMessage();
        }
    }

    public String detectarPicEnSoket() {

        StringBuffer datos = new StringBuffer();

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener versión
            usbSerialPort.write(new byte[] {Byte.parseByte("18")}, 10); // 0x10 es 16 en decimal

            byte[] response = new byte[1];

            if (leerRespuesta(response, 'A', "No se recibio la respuesta esperada")) {

                datos.append("El PIC esta dentro del Soket");
            }

            if (leerRespuesta(response, 'Y', "No se recibio la respuesta esperada")) {

                researComandos();

                return datos.toString();

            } else {

                enviarComando("1");

                return "El PIC esta fuera del Socket";
            }

        } catch (IOException e) {

            return "Error al detectar PIC: " + e.getMessage();
        }
    }

    public String detectarPicFueraDelSoket() {

        StringBuffer datos = new StringBuffer();

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener versión
            usbSerialPort.write(new byte[] {Byte.parseByte("19")}, 10); // 0x10 es 16 en decimal

            byte[] response = new byte[1];

            if (leerRespuesta(response, 'A', "No se recibio la respuesta esperada")) {

                datos.append("El PIC esta fuera del Soket");
            }

            if (leerRespuesta(response, 'Y', "No se recibio la respuesta esperada")) {

                researComandos();

                return datos.toString();

            } else {

                return "El PIC  esta dentro del Soket";
            }

        } catch (IOException e) {

            return "Error al detectar PIC: " + e.getMessage();
        }
    }

    public String obtenerVersionDelProgramador() {

        StringBuffer datos = new StringBuffer();

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener versión
            usbSerialPort.write(new byte[] {Byte.parseByte("20")}, 10); // 0x15 es 21 en decimal

            int size = 1; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            byte[] bytes = new byte[size];

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        bytes[i] = buffer[i];
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }
            researComandos();

            if (bytes[0] == Byte.parseByte("0")) {

                datos.append(String.format("K128: %02X", bytes[0] & 0xFF));

            } else if (bytes[0] == Byte.parseByte("1")) {

                datos.append(String.format("K149-A: %02X", bytes[0] & 0xFF));

            } else if (bytes[0] == Byte.parseByte("2")) {

                datos.append(String.format("K149-B: %02X", bytes[0] & 0xFF));

            } else if (bytes[0] == Byte.parseByte("3")) {

                datos.append(String.format("K150: %02X", bytes[0] & 0xFF));
            }

            return datos.toString();

        } catch (IOException e) {
            return "Error al obtener la versión del programador: " + e.getMessage();
        }
    }

    public String obtenerProtocoloDelProgramador() {

        StringBuffer datos = new StringBuffer();

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener protocolo
            usbSerialPort.write(new byte[] {Byte.parseByte("21")}, 10); // 0x16 es 22 en decimal

            int size = 4; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            byte[] bytes = new byte[size];

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        bytes[i] = buffer[i];

                        datos.append(String.format("%02X ", buffer[i]));
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }

            researComandos();

            // Convertir los bytes a una cadena ASCII
            return new String(bytes, "US-ASCII") + " " + datos.toString();

        } catch (IOException e) {
            return "Error al obtener el protocolo del programador: " + e.getMessage();
        }
    }

    public boolean programarVectorDeDepuracion(int address) {
        try {
            // Comando 22 (0x16)
            byte cmd = 0x16;

            // Dividir la dirección en bytes
            byte[] BE4_address = ByteBuffer.allocate(4).putInt(address).array();

            // Enviar comando
            usbSerialPort.write(new byte[] {cmd}, 10);

            // Enviar los 3 bytes de la dirección
            usbSerialPort.write(new byte[] {BE4_address[1], BE4_address[2], BE4_address[3]}, 10);

            // Leer respuesta (1 byte)
            byte[] response = new byte[1];
            usbSerialPort.read(response, 100);

            // Validar la respuesta
            if (response[0] == 'Y') {
                return true;
            } else if (response[0] == 'N') {
                return false;
            } else {

                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error en programarVectorDeDepuracion(): " + e.getMessage(), e);
        }
    }

    public String programarVectorDeDepuracion(int highAddress, int midAddress, int lowAddress) {
        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando (22 en decimal o 0x16 en hexadecimal)
            usbSerialPort.write(new byte[] {0x16}, 10);

            // Enviar los bytes de dirección en orden
            usbSerialPort.write(
                    new byte[] {(byte) highAddress, (byte) midAddress, (byte) lowAddress}, 10);

            // Leer la respuesta (1 byte)
            byte[] response = new byte[1];
            usbSerialPort.read(response, 100);

            // Validar la respuesta
            if (response[0] == 'Y') {
                return "Vector de depuración programado correctamente.";
            } else if (response[0] == 'N') {
                return "Fallo al programar el vector de depuración.";
            } else {
                return "Respuesta inesperada: " + String.format("%02X", response[0]);
            }
        } catch (IOException e) {
            return "Error al programar el vector de depuración: " + e.getMessage();
        }
    }

    public int leerVectorDeDepuracion() {
        try {
            // Comando 23 (0x17)
            byte cmd = 0x17;

            // Enviar comando
            usbSerialPort.write(new byte[] {cmd}, 10);

            // Leer respuesta (4 bytes)
            byte[] response = new byte[4];
            usbSerialPort.read(response, 100);

            // Validar el primer byte
            if (response[0] != (byte) 0xEF) {
                return 0;
            }

            // Combinar los siguientes 3 bytes para obtener la dirección
            ByteBuffer buffer = ByteBuffer.allocate(4).put((byte) 0x00).put(response, 1, 3);
            buffer.flip();
            return buffer.getInt();
        } catch (IOException e) {
            throw new RuntimeException("Error en leerVectorDeDepuracion(): " + e.getMessage(), e);
        }
    }

    public String leerVectorDeDepuracion(int dato) {
        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando (23 en decimal o 0x17 en hexadecimal)
            usbSerialPort.write(new byte[] {0x17}, 10);

            // Leer la respuesta (4 bytes)
            byte[] response = new byte[4];
            usbSerialPort.read(response, 100);

            // Verificar si el primer byte es 0xEF y extraer la dirección
            if (response[0] == (byte) 0xEF) {
                int highAddress = response[1] & 0xFF;
                int midAddress = response[2] & 0xFF;
                int lowAddress = response[3] & 0xFF;

                return String.format(
                        "Vector de depuración leído: 0x%02X%02X%02X",
                        highAddress, midAddress, lowAddress);
            } else {
                return "Respuesta inesperada: " + String.format("%02X", response[0]);
            }
        } catch (IOException e) {
            return "Error al leer el vector de depuración: " + e.getMessage();
        }
    }

    /* public String leerVectorDeDepuracion() {

        StringBuffer datos = new StringBuffer();

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener protocolo
            usbSerialPort.write(new byte[] {0x18}, 10); // 0x18 es 24 en decimal

            int size = 4; // Convertir palabras a bytes

            byte[] buffer = new byte[64]; // Búfer temporal para leer datos en bloques

            int bytesLeidos = 0;

            byte[] bytes = new byte[size];

            // Leer los datos en múltiples iteraciones
            while (bytesLeidos < size) {
                int leidos = usbSerialPort.read(buffer, 100); // Leer hasta 64 bytes
                if (leidos > 0) {
                    for (int i = 0; i < leidos; i++) {

                        bytes[i] = buffer[i];

                        datos.append(String.format("%02X ", buffer[i]));
                    }
                    bytesLeidos += leidos;

                } else {
                    // Si no se reciben datos, salir del bucle para evitar un bloqueo infinito
                    break;
                }
            }

            researComandos();

            // Convertir los bytes a una cadena ASCII
            return new String(bytes, "US-ASCII") + " " + datos.toString();

        } catch (IOException e) {
            return "Error al obtener el protocolo del programador: " + e.getMessage();
        }
    }*/

    public boolean datosDeCalibracionPics10F() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {25};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }
}
