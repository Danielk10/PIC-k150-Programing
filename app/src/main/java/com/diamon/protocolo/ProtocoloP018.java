package com.diamon.protocolo;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.excepciones.UsbCommunicationException;
import com.diamon.nucleo.Protocolo;
import com.diamon.utilidades.ByteUtils;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Implementación del protocolo P018 para programadores PIC K150.
 *
 * <p>Esta clase implementa el protocolo de comunicación P018 específico para programadores KITSRUS,
 * incluyendo todas las operaciones de programación, lectura, borrado y verificación de chips PIC.
 *
 * <p>Operaciones soportadas:
 *
 * <ul>
 *   <li>Programación de memoria ROM y EEPROM
 *   <li>Configuración de fuses e ID del chip
 *   <li>Lectura de memorias y configuración
 *   <li>Borrado y verificación de memorias
 *   <li>Detección de chips en socket
 *   <li>Manejo de voltajes de programación
 * </ul>
 *
 * @author Danielk10
 * @version 2.0 - Integrado con sistema de logging y excepciones mejoradas
 * @since 2025
 */
public class ProtocoloP018 extends Protocolo {

    /** Versión del protocolo P018 implementado */
    private static final String VERSION_PROTOCOLO = "P018 v2.0";

    /** Timeout por defecto para operaciones USB en milisegundos */
    private static final int TIMEOUT_DEFAULT = 100;

    /** Timeout extendido para operaciones largas en milisegundos */
    private static final int TIMEOUT_EXTENDED = 500;

    /**
     * Constructor del protocolo P018.
     *
     * @param contexto Contexto de la aplicación Android
     * @param usbSerialPort Puerto serie USB configurado para el programador
     */
    public ProtocoloP018(Context contexto, UsbSerialPort usbSerialPort) {
        super(contexto, usbSerialPort);
    }

    @Override
    public String hacerUnEco() {
        try {

            // Enviar comando 2 para eco
            enviarComando("2");

            StringBuilder response = new StringBuilder();

            // Enviar byte de eco
            byte[] byteEco = ByteUtils.prepararDatosUSB(new byte[] {(byte) 2}, "eco");
            escribirDatosUSB(byteEco, TIMEOUT_DEFAULT, "byte_eco");

            // Leer respuesta
            byte[] respuestaBytes = readBytes(1, TIMEOUT_EXTENDED);
            response.append((char) respuestaBytes[0]);

            // Resetear comandos
            if (!researComandos()) {}

            String resultado = response.toString();

            return resultado;

        } catch (UsbCommunicationException e) {
            return ""; // Retornar vacío en caso de error
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public boolean iniciarVariablesDeProgramacion(ChipPic chipPIC)
            throws ChipConfigurationException {
        if (chipPIC == null) {
            throw new IllegalArgumentException("ChipPIC no puede ser null");
        }

        StringBuilder respuesta = new StringBuilder();

        try {

            // Enviar comando 3
            enviarComando("3");

            // Obtener parámetros de inicialización del chip
            int romSize = chipPIC.getTamanoROM();
            int eepromSize = chipPIC.getTamanoEEPROM();
            int coreType = chipPIC.getTipoDeNucleoDelPic();
            boolean flagCalibrationValueInROM = chipPIC.isFlagCalibration();
            boolean flagBandGapFuse = chipPIC.isFlagBandGap();
            boolean flagSinglePanelAccessMode = chipPIC.isFlag18fSingle();
            boolean flagVccVppDelay = chipPIC.isFlagVccVppDelay();
            int programDelay = chipPIC.getProgramDelay();
            int powerSequence = chipPIC.getPowerSequence();
            int eraseMode = chipPIC.getEraseMode();
            int programRetries = chipPIC.getProgramTries();
            int overProgram = chipPIC.getOverProgram();

            // Construir los flags según el protocolo
            int flags = 0;
            flags |= (flagCalibrationValueInROM ? 1 : 0); // Bit 0
            flags |= (flagBandGapFuse ? 2 : 0); // Bit 1
            flags |= (flagSinglePanelAccessMode ? 4 : 0); // Bit 2
            flags |= (flagVccVppDelay ? 8 : 0); // Bit 3

            // Crear payload según el protocolo P018
            ByteBuffer payload = ByteBuffer.allocate(11);
            payload.order(ByteOrder.BIG_ENDIAN);

            payload.putShort((short) romSize); // Bytes 1-2: ROM Size
            payload.putShort((short) eepromSize); // Bytes 3-4: EEPROM Size
            payload.put((byte) coreType); // Byte 5: Core Type
            payload.put((byte) flags); // Byte 6: Flags
            payload.put((byte) programDelay); // Byte 7: Program Delay
            payload.put((byte) powerSequence); // Byte 8: Power Sequence
            payload.put((byte) eraseMode); // Byte 9: Erase Mode
            payload.put((byte) programRetries); // Byte 10: Program Tries
            payload.put((byte) overProgram); // Byte 11: Over Program

            // Enviar payload de configuración
            escribirDatosUSB(payload.array(), TIMEOUT_DEFAULT, "configuracion_chip");

            // Leer respuesta de confirmación
            byte[] respuestaBytes = readBytes(1, TIMEOUT_DEFAULT);
            respuesta.append(new String(respuestaBytes, StandardCharsets.US_ASCII));

            // Resetear comandos
            if (!researComandos()) {}

            boolean exitoso = respuesta.toString().equals("I");

            if (exitoso) {
            } else {
            }

            return exitoso;

        } catch (UsbCommunicationException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean activarVoltajesDeProgramacion() {

        try {

            enviarComando("4");

            byte[] respuestaBytes = readBytes(1, TIMEOUT_DEFAULT);
            String respuesta = new String(respuestaBytes, StandardCharsets.US_ASCII);

            boolean exitoso = respuesta.equals("V");

            if (exitoso) {
            } else {
            }

            return exitoso;

        } catch (UsbCommunicationException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean desactivarVoltajesDeProgramacion() {

        try {

            enviarComando("5");

            byte[] respuestaBytes = readBytes(1, TIMEOUT_DEFAULT);
            String respuesta = new String(respuestaBytes, StandardCharsets.US_ASCII);

            boolean exitoso = respuesta.equals("v");

            if (exitoso) {
            } else {
            }

            return exitoso;

        } catch (UsbCommunicationException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean reiniciarVoltajesDeProgramacion() {

        try {

            enviarComando("6");

            byte[] respuestaBytes = readBytes(1, TIMEOUT_DEFAULT);
            String respuesta = new String(respuestaBytes, StandardCharsets.US_ASCII);

            boolean exitoso = respuesta.equals("V");

            if (exitoso) {
            } else {
            }

            return exitoso;

        } catch (UsbCommunicationException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firware)
            throws ChipConfigurationException {
        // Validaciones de entrada
        if (chipPIC == null) {
            throw new IllegalArgumentException("ChipPIC no puede ser null");
        }

        if (firware == null || firware.trim().isEmpty()) {
            throw new IllegalArgumentException("Firmware no puede ser null o vacío");
        }

        try {

            // ✅ CORRECCIÓN: Manejo robusto de excepciones como Python
            DatosPicProcesados datosPic = new DatosPicProcesados(firware, chipPIC);

            try {
                datosPic.iniciarProcesamientoDeDatos();
            } catch (Exception e) {
                return false;
            }

            byte[] romData = datosPic.obtenerBytesHexROMPocesado();

            if (romData == null || romData.length == 0) {
                return false;
            }

            int wordCount = romData.length / 2;
            // Validación crítica de tamaño
            if (wordCount > chipPIC.getTamanoROM()) {
                String mensaje =
                        String.format(
                                "Datos ROM exceden capacidad del chip: %d > %d words",
                                wordCount, chipPIC.getTamanoROM());
                return false;
            }

            // ✅ CORRECCIÓN BASADA EN PYTHON: Padding automático
            int totalSize = wordCount * 2;
            if (totalSize % 32 != 0) {
                int paddingNeeded = 32 - (totalSize % 32);

                // Crear nuevo array con padding
                byte[] paddedRomData = new byte[romData.length + paddingNeeded];
                System.arraycopy(romData, 0, paddedRomData, 0, romData.length);
                // Llenar padding con 0xFF (valor por defecto para ROM vacía)
                Arrays.fill(paddedRomData, romData.length, paddedRomData.length, (byte) 0xFF);

                romData = paddedRomData;
                wordCount = romData.length / 2;
            }

            // ✅ SIGUIENDO EL PROTOCOLO PYTHON EXACTO
            if (!researComandos()) {
                return false;
            }

            if (!activarVoltajesDeProgramacion()) {
                return false;
            }

            // Comando para programar ROM (0x07) - IGUAL QUE PYTHON
            escribirDatosUSB(new byte[] {0x07}, 10, "comando_programar_ROM");

            // Enviar cantidad de palabras - IGUAL QUE PYTHON
            byte[] wordCountMessage = ByteUtils.shortToBytes((short) wordCount, true);
            escribirDatosUSB(wordCountMessage, TIMEOUT_DEFAULT, "tamaño_palabras_ROM");

            // Validar respuesta inicial 'Y' - IGUAL QUE PYTHON
            byte[] response = new byte[1];
            if (!leerRespuesta(
                    response, 'Y', "Error: No se recibió confirmación después de enviar tamaño")) {
                desactivarVoltajesDeProgramacion();
                researComandos();
                return false;
            }

            // ✅ ENVIAR DATOS EN BLOQUES DE 32 BYTES - EXACTAMENTE COMO PYTHON
            int bloquesEnviados = 0;
            try {
                for (int i = 0; i < romData.length; i += 32) {
                    byte[] chunk = Arrays.copyOfRange(romData, i, Math.min(i + 32, romData.length));
                    bloquesEnviados++;

                    escribirDatosUSB(chunk, 10, String.format("bloque_ROM_%d", bloquesEnviados));

                    // ✅ TIMEOUT EXTENDIDO COMO PYTHON (timeout=20)
                    if (!leerRespuesta(
                            response,
                            'Y',
                            "Error: No se recibió confirmación de bloque",
                            TIMEOUT_EXTENDED)) {
                        desactivarVoltajesDeProgramacion();
                        researComandos();
                        return false;
                    }
                }

                // ✅ TIMEOUT EXTENDIDO COMO PYTHON para respuesta final
                if (!leerRespuesta(
                        response,
                        'P',
                        "Error: No se recibió confirmación final de programación",
                        TIMEOUT_EXTENDED)) {
                    desactivarVoltajesDeProgramacion();
                    researComandos();
                    return false;
                }

            } catch (Exception e) {
                // ✅ MANEJO DE EXCEPCIONES DURANTE PROGRAMACIÓN COMO PYTHON
                desactivarVoltajesDeProgramacion();
                researComandos();
                return false;
            }

            // Finalizar secuencia
            if (!desactivarVoltajesDeProgramacion()) {}

            if (!researComandos()) {}

            // ✅ AGREGAR VERIFICACIÓN COMO PYTHON
            try {
                String romLeida = leerMemoriaROMDelPic(chipPIC);
                if (romLeida != null && !romLeida.startsWith("Error")) {
                } else {
                }
            } catch (Exception e) {
                // No fallar por esto, solo advertir
            }

            return true;

        } catch (UsbCommunicationException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean programarMemoriaEEPROMDelPic(ChipPic chipPIC, String firware)
            throws ChipConfigurationException {
        // Validaciones de entrada
        if (chipPIC == null) {
            throw new IllegalArgumentException("ChipPIC no puede ser null");
        }

        if (firware == null || firware.trim().isEmpty()) {
            throw new IllegalArgumentException("Firmware no puede ser null o vacío");
        }

        try {

            // Procesar datos HEX
            DatosPicProcesados datosPic = new DatosPicProcesados(firware, chipPIC);
            datosPic.iniciarProcesamientoDeDatos();
            byte[] eepromData = datosPic.obtenerBytesHexEEPROMPocesado();

            if (eepromData == null) {
                return true; // No es error si no hay datos EEPROM
            }

            int byteCount = eepromData.length;

            // Validaciones críticas
            if (byteCount > chipPIC.getTamanoEEPROM()) {
                String mensaje =
                        String.format(
                                "Datos EEPROM exceden capacidad del chip: %d > %d bytes",
                                byteCount, chipPIC.getTamanoEEPROM());
                return false;
            }

            // Validar que el tamaño sea múltiplo de 2 bytes
            if (byteCount % 2 != 0) {
                String mensaje =
                        String.format(
                                "Tamaño de datos EEPROM debe ser múltiplo de 2: %d", byteCount);
                return false;
            }

            // Preparar secuencia de programación
            if (!researComandos()) {
                return false;
            }

            if (!activarVoltajesDeProgramacion()) {
                return false;
            }

            // Comando para programar EEPROM (0x08)
            escribirDatosUSB(new byte[] {0x08}, 10, "comando_programar_EEPROM");

            // Enviar cantidad de bytes
            byte[] byteCountMessage = ByteUtils.shortToBytes((short) byteCount, true);
            escribirDatosUSB(byteCountMessage, TIMEOUT_DEFAULT, "tamaño_bytes_EEPROM");

            // Validar respuesta inicial 'Y'
            byte[] response = new byte[1];
            if (!leerRespuesta(
                    response, 'Y', "Error: No se recibió confirmación después de enviar tamaño")) {
                desactivarVoltajesDeProgramacion();
                researComandos();
                return false;
            }

            // Enviar datos en bloques de 2 bytes
            int bloquesEnviados = 0;
            for (int i = 0; i < eepromData.length; i += 2) {
                byte[] chunk = Arrays.copyOfRange(eepromData, i, i + 2);
                bloquesEnviados++;

                escribirDatosUSB(chunk, 10, String.format("bloque_EEPROM_%d", bloquesEnviados));

                if (!leerRespuesta(response, 'Y', "Error: No se recibió confirmación de bloque")) {
                    desactivarVoltajesDeProgramacion();
                    researComandos();
                    return false;
                }
            }

            // Enviar 2 bytes adicionales al final (relleno según protocolo)
            escribirDatosUSB(new byte[] {0x00, 0x00}, 10, "relleno_final_EEPROM");

            // Validar respuesta final 'P'
            if (!leerRespuesta(response, 'P', "Error: No se recibió confirmación final")) {
                desactivarVoltajesDeProgramacion();
                researComandos();
                return false;
            }

            // Finalizar secuencia
            if (!desactivarVoltajesDeProgramacion()) {}

            if (!researComandos()) {}

            return true;

        } catch (UsbCommunicationException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    
       @Override
    public boolean programarFusesIDDelPic(
            ChipPic chipPIC, String firware, byte[] IDPic, List<Integer> fusesUsuario) {

        try {
            // Procesar datos
            DatosPicProcesados datosPic = new DatosPicProcesados(firware, chipPIC);
            datosPic.iniciarProcesamientoDeDatos();

            // Obtener tipo de núcleo
            int tipoNucleo = chipPIC.getTipoDeNucleoBit();
            
            // ============================================================
            // NUEVO: Determinar si usar datos del usuario o del HEX
            // ============================================================
            byte[] id;
            int[] fuses;
            
            // Verificar si el usuario configuró fusibles
            boolean usuarioConfiguroFuses = fusesUsuario != null 
                && !fusesUsuario.isEmpty() 
                && fusesUsuario.size() > 0;
            
            boolean usuarioConfiguroID = IDPic != null 
                && IDPic.length > 1 
                && !(IDPic.length == 1 && IDPic[0] == 0);
            
            if (usuarioConfiguroFuses || usuarioConfiguroID) {
                // ============================================================
                // USAR DATOS DEL USUARIO
                // ============================================================
                
                // ID: Usar del usuario si existe, sino del HEX
                if (usuarioConfiguroID) {
                    id = IDPic;
                } else {
                    id = datosPic.obtenerVsloresBytesHexIDPocesado();
                }
                
                // FUSES: Usar del usuario si existe, sino del HEX
                if (usuarioConfiguroFuses) {
                    // Convertir List<Integer> a int[]
                    fuses = new int[fusesUsuario.size()];
                    for (int i = 0; i < fusesUsuario.size(); i++) {
                        fuses[i] = fusesUsuario.get(i);
                    }
                } else {
                    fuses = datosPic.obtenerValoresIntHexFusesPocesado();
                }
                
            } else {
                // ============================================================
                // USAR DATOS DEL HEX (comportamiento original)
                // ============================================================
                id = datosPic.obtenerVsloresBytesHexIDPocesado();
                fuses = datosPic.obtenerValoresIntHexFusesPocesado();
            }
            
            // ============================================================
            // Validar datos según tipo de núcleo
            // ============================================================
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

    /*@Override
    public boolean programarFusesIDDelPic(
            ChipPic chipPIC, String firware, byte[] IDPic, List<Integer> fusesUsuario) {

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
    }*/

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
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        } catch (ChipConfigurationException e) {
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
            // Preparar secuencia de borrado
            if (!researComandos()) {
                return false;
            }

            if (!activarVoltajesDeProgramacion()) {
                return false;
            }

            // Comando para borrar memoria (0x0E = 14 decimal)
            escribirDatosUSB(new byte[] {0x0E}, 10, "comando_borrar_memorias");

            // Leer respuesta de confirmación
            byte[] bytes = readBytes(1, TIMEOUT_EXTENDED); // Usar timeout extendido para borrado

            // Finalizar secuencia
            if (!desactivarVoltajesDeProgramacion()) {}

            if (!researComandos()) {}

            // Validar respuesta
            String respuesta = new String(bytes, StandardCharsets.US_ASCII);
            boolean exitoso = respuesta.equals("Y");

            if (exitoso) {
            } else {
            }

            return exitoso;

        } catch (UsbCommunicationException e) {
            return false;
        } catch (Exception e) {
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
        } catch (UsbCommunicationException e) {
            throw new RuntimeException(e);
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
        } catch (UsbCommunicationException e) {
            throw new RuntimeException(e);
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

                datos.append(String.format("K128", bytes[0] & 0xFF));

            } else if (bytes[0] == Byte.parseByte("1")) {

                datos.append(String.format("K149-A", bytes[0] & 0xFF));

            } else if (bytes[0] == Byte.parseByte("2")) {

                datos.append(String.format("K149-B", bytes[0] & 0xFF));

            } else if (bytes[0] == Byte.parseByte("3")) {

                datos.append(String.format("K150", bytes[0] & 0xFF));
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
