package com.diamon.chip;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

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

        StringBuffer mensaje = new StringBuffer();

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

    public String iniciarVariablesDeProgramacion(InformacionPic chipPIC) {

        enviarComando("3");

        StringBuilder respuesta = new StringBuilder();

        // Parámetros de inicialización
        int romSize = chipPIC.getTamanoROM(); // Tamaño de ROM
        int eepromSize = chipPIC.getTamanoEEPROM(); // Tamaño de EEPROM
        int coreType = chipPIC.getTipoNucleoPic(); // Core Type: 16F7x 16F7x7
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

        try {

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

            usbSerialPort.write(payload.array(), 100); // Enviar datos

        } catch (NumberFormatException e) {

            respuesta.append(e.toString());
            return respuesta.toString();

        } catch (IOException e) {

            respuesta.append(e.toString());
            return respuesta.toString();
        }

        try {

            respuesta.append(new String(readBytes(1, 100)));

            researComandos();

        } catch (Exception err) {

            respuesta.append(err.toString());
        }

        if (respuesta.equals("I")) {

            return respuesta.toString();

        } else {

            return respuesta.toString();
        }
    }

    public boolean activarVoltajesDeProgramacion() {

        enviarComando("4");

        StringBuffer respuesta = new StringBuffer();

        try {

            respuesta.append(new String(readBytes(1, 100)));

            if (respuesta.equals("V")) {

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

            respuesta.append(new String(readBytes(1, 100)));

            if (respuesta.equals("v")) {

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

            respuesta.append(new String(readBytes(1, 100)));

            if (respuesta.equals("V")) {

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

    public boolean programarROMPic() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {7};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean programarEEPROMPic() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {8};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean programarFusesIDPic() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {9};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
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
            usbSerialPort.write(new byte[] {0x0B}, 10); // Comando 11 en hexadecimal (0x0B)

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
            // desactivarVoltajesDeProgramacion();

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
            usbSerialPort.write(new byte[] {0x0C}, 10); // Comando 12 en hexadecimal (0x0C)

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
            // desactivarVoltajesDeProgramacion();

            // Retornar los datos leídos en formato hexadecimal
            return datos.toString().trim();

        } catch (NumberFormatException e) {
            return "Error al leer Memoria ROM: " + e.toString();
        } catch (IOException e) {
            return "Error al leer Memoria ROM: " + e.toString();
        }
    }

    public boolean leerCalibracionPic() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {14};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean borrarMemoriaPic() {

        enviarComando("15");

        StringBuffer respuesta = new StringBuffer();

        try {

            respuesta.append(new String(readBytes(1, 100)));

            if (respuesta.equals("Y")) {

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

    public boolean verificarBorradoMemoriaROMPic() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {16};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean verificarBorradoMemoriaEEPROMPic() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {17};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean programarFuses18F() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {18};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean detectarPicEnSoket() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {19};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean detectarPicFueraDelSoket() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {20};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean obtenerVersionDelProgramador() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {21};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean obtenerProtocoloDelProgramador() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {22};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean vectorDepuracionDelPrograma() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {23};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean vectorDepuracionDeLectura() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {24};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

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
