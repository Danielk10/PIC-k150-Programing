package com.diamon.protocolo;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.nucleo.Protocolo;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ProtocoloP018 extends Protocolo {

    public ProtocoloP018(Context contexto, UsbSerialPort usbSerialPort) {
        super(contexto, usbSerialPort);
    }

    @Override
    public String hacerUnEco() {

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

    @Override
    public boolean iniciarVariablesDeProgramacion(ChipPic chipPIC) {

        enviarComando("3");

        StringBuilder respuesta = new StringBuilder();

        try {

            // Parámetros de inicialización
            int romSize = chipPIC.getTamanoROM(); // Tamaño de ROM
            int eepromSize = chipPIC.getTamanoEEPROM(); // Tamaño de EEPROM
            int coreType = chipPIC.getTipoDeNucleoVerdaderoDelPic(); // Core Type: 16F7x 16F7x7
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firware) {

        // Procesar datos HEX
        DatosPicProcesados datosPic = new DatosPicProcesados(firware, chipPIC);
        datosPic.iniciarProcesamientoDeDatos();
        byte[] romData = datosPic.obtenerBytesHexROMPocesado();

        int wordCount = romData.length / 2; // Cantidad de palabras (2 bytes por palabra)

        // Validación: Verificar que el tamaño no exceda el límite del PIC
        if (wordCount > chipPIC.getTamanoROM()) {

            return false;
        }

        // Validación: Verificar que el tamaño sea múltiplo de 32 bytes
        if ((wordCount * 2) % 32 != 0) {
            return false;
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

                return false;
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
                    return false;
                }
            }

            // Validar respuesta final 'P'
            if (!leerRespuesta(
                    response, 'P', "Error: No se recibió la confirmación final de programación.")) {
                return false;
            }

            // Desactivar voltajes y limpiar comandos
            desactivarVoltajesDeProgramacion();
            researComandos();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean programarMemoriaEEPROMDelPic(ChipPic chipPIC, String firware) {

        DatosPicProcesados datosPic = new DatosPicProcesados(firware, chipPIC);

        datosPic.iniciarProcesamientoDeDatos();

        byte[] eepromData = datosPic.obtenerBytesHexEEPROMPocesado();

        int byteCount = eepromData.length;

        // Validación: Verificar que no exceda el tamaño de la EEPROM
        if (byteCount > chipPIC.getTamanoEEPROM()) {
            return false;
        }

        // Validación: Verificar que el tamaño sea múltiplo de 2 bytes
        if (byteCount % 2 != 0) {
            return false;
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
                return false;
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
                    return false;
                }
            }

            // Enviar 2 bytes adicionales al final (relleno)
            usbSerialPort.write(new byte[] {0x00, 0x00}, 10);

            // Validar respuesta final 'P'
            if (!leerRespuesta(
                    response, 'P', "Error: No se recibió la confirmación final de programación.")) {
                return false;
            }

            // Desactivar voltajes y limpiar comandos
            desactivarVoltajesDeProgramacion();
            researComandos();

            return true;
        } catch (IOException e) {

            return false;
        }
    }

    @Override
    public boolean programarFusesIDDelPic(ChipPic chipPIC, String firware) {

        try {
            // Procesar datos
            DatosPicProcesados datosPic = new DatosPicProcesados(firware, chipPIC);
            datosPic.iniciarProcesamientoDeDatos();

            // Obtener tipo de núcleo, ID y valores de FUSES
            int tipoNucleo = chipPIC.getTipoDeNucleoBit();
            byte[] id = datosPic.obtenerVsloresBytesHexIDPocesado();
            int[] fuses = datosPic.obtenerValoresIntHexFusesPocesado();

            // Validar datos según tipo de núcleo
            if (tipoNucleo == 16) {
                if (id.length != 8) {
                    return false;
                }
                if (fuses.length != 7) {
                    return false;
                }
            } else if (tipoNucleo == 14) {
                if (id.length != 4) {
                    return false;
                }
                if (fuses.length < 1 || fuses.length > 2) {
                    return false;
                }
            } else {
                return false;
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

                return true;

            } else if (response[0] == 'N') {

                return false;
            } else {
                return false;
            }

        } catch (IOException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean programarCalibracionDelPic(ChipPic chipPIC, String firware) {

        return false;
    }

    @Override
    public String leerMemoriaROMDelPic(ChipPic chipPIC) {

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
                        datos.append(String.format("%02X", buffer[i]));
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

    @Override
    public String leerMemoriaEEPROMDelPic(ChipPic chipPIC) {

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
                        datos.append(String.format("%02X", buffer[i]));
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
            return "Error al leer Memoria EEPROM: " + e.toString();
        } catch (IOException e) {
            return "Error al leer Memoria EEPROM: " + e.toString();
        }
    }

    @Override
    public String leerDatosDeConfiguracionDelPic() {
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

                        datos.append(String.format("%02X", buffer[i]));
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

    @Override
    public String leerDatosDeCalibracionDelPic() {

        return null;
    }

    @Override
    public boolean borrarMemoriasDelPic() {

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

                return true;

            } else {

                return false;
            }

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean verificarSiEstaBarradaLaMemoriaROMDelDelPic(ChipPic chipPIC) {

        try {
            // Resetear comandos
            researComandos();

            // Comando ERASE CHECK ROM
            usbSerialPort.write(
                    new byte[] {0x10, (byte) 0x3F},
                    10); // 0x10 es 16 en decimal, y 0x3F es el valor del high_byte

            while (true) {
                byte[] buffer = new byte[1];
                int leidos = usbSerialPort.read(buffer, 100);

                if (leidos > 0) {
                    switch (buffer[0]) {
                        case (byte) 0xFF: // ROM aún no revisada por completo
                            continue;
                        case 'Y': // ROM está en blanco
                            researComandos();
                            return true;
                        case 'N': // ROM no está en blanco
                        case 'C': // ROM no está en blanco (alternativa)
                            researComandos();
                            return false;
                        default: // Error inesperado
                            researComandos();
                            return false;
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean verificarSiEstaBarradaLaMemoriaEEPROMDelDelPic() {

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

                return true;

            } else if (new String(bytes, "US-ASCII").equals("N")) {

                return false;

            } else {

                return false;
            }

        } catch (IOException e) {

            return false;
        }
    }

    @Override
    public boolean programarFusesDePics18F() {

        try {
            // Resetear comandos previos
            researComandos();

            // Enviar comando para obtener versión
            usbSerialPort.write(new byte[] {Byte.parseByte("17")}, 10); 

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

                return true;

            } else if (new String(bytes, "US-ASCII").equals("B")) {

                return false;

            } else {

                return false;
            }

        } catch (IOException e) {

            return false;
        }
    }

    @Override
    public boolean detectarPicEnElSocket() {

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

                return true;

            } else {

                return false;
            }

        } catch (IOException e) {

            return false;
        }
    }

    @Override
    public boolean detectarSiEstaFueraElPicDelSocket() {

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

                return true;

            } else {

                return false;
            }

        } catch (IOException e) {

            return false;
        }
    }

    @Override
    public String obtenerVersionOModeloDelProgramador() {

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

    @Override
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

                        datos.append(String.format("%02X", buffer[i]));
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

    @Override
    public boolean programarVectorDeDepuracionDelPic(ChipPic chipPIC) {

        int address = 0; // No va
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

    @Override
    public String leerVectorDeDepuracionDelPic() {

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

    @Override
    public boolean programarDatosDeCalibracionDePics10F() {

        return false;
    }
}
