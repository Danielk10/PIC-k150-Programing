package com.diamon.datos;

import com.diamon.excepciones.HexProcessingException;
import com.diamon.utilidades.ByteUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Procesador de archivos HEX Intel con logging integrado y manejo robusto de errores.
 *
 * <p>Esta clase procesa archivos en formato Intel HEX, validando checksums, formatos de registro y
 * integridad de datos. Incluye logging detallado de todas las operaciones y manejo de excepciones
 * específicas del dominio.
 *
 * <p>Características principales:
 *
 * <ul>
 *   <li>Validación completa de formato Intel HEX
 *   <li>Verificación automática de checksums
 *   <li>Logging detallado de procesamiento
 *   <li>Manejo robusto de errores con contexto
 *   <li>Soporte para direcciones extendidas
 *   <li>Validación de integridad de datos
 * </ul>
 *
 * @author Danielk10
 * @version 2.0 - Integrado con sistema de logging y excepciones mejoradas
 * @since 2025
 */
public class HexProcesado {

    // ========== EXCEPCIONES DEPRECADAS (Usar HexProcessingException en su lugar) ==========

    /**
     * @deprecated Usar HexProcessingException.crearErrorFormato() en su lugar
     */
    @Deprecated
    public class InvalidRecordException extends Exception {
        public InvalidRecordException(String message) {
            super(message);
        }
    }

    /**
     * @deprecated Usar HexProcessingException.crearErrorChecksum() en su lugar
     */
    @Deprecated
    public class InvalidChecksumException extends Exception {
        public InvalidChecksumException(String message) {
            super(message);
        }
    }

    // Clase para representar un registro HEX
    public class HexRecord {
        public int address;
        public byte[] data;

        public HexRecord(int address, byte[] data) {
            this.address = address;
            this.data = data;
        }
    }

    /** Lista de registros HEX procesados */
    private final List<HexRecord> records = new ArrayList<>();

    /** Información del archivo procesado para logging */
    private final String informacionArchivo;

    /**
     * Constructor que procesa un archivo HEX completo con logging integrado.
     *
     * @param fileContent Contenido del archivo HEX como string
     * @throws HexProcessingException Si ocurre error durante el procesamiento
     */
    public HexProcesado(String fileContent) throws HexProcessingException {
        if (fileContent == null || fileContent.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Contenido del archivo HEX no puede ser null o vacío");
        }

        String[] lineas = fileContent.split("\n");
        this.informacionArchivo =
                String.format("%d líneas, %d caracteres", lineas.length, fileContent.length());

        boolean eof = false;
        int extendedAddress = 0;
        int lineasProcesadas = 0;
        int registrosValidos = 0;

        // Expresiones regulares optimizadas para validación
        Pattern hexRecordPattern = Pattern.compile("^:[0-9A-Fa-f]+$");
        Pattern hexRecordChopper =
                Pattern.compile(
                        "^:([0-9A-Fa-f]{2})([0-9A-Fa-f]{4})([0-9A-Fa-f]{2})([0-9A-Fa-f]*)([0-9A-Fa-f]{2})$");

        try {
            for (int numeroLinea = 0; numeroLinea < lineas.length; numeroLinea++) {
                String line = lineas[numeroLinea];
                lineasProcesadas++;

                // Ignorar líneas vacías
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Validar formato básico del registro
                Matcher matcher = hexRecordPattern.matcher(line);
                if (!matcher.matches()) {
                    throw HexProcessingException.crearErrorFormato(
                            numeroLinea + 1,
                            line,
                            "Caracteres no hexadecimales o formato incorrecto");
                }

                // Verificar registros después del EOF
                if (eof) {
                    throw HexProcessingException.crearErrorFormato(
                            numeroLinea + 1, line, "Registro extra después del EOF");
                }

                // Parsear componentes del registro
                Matcher chop = hexRecordChopper.matcher(line);
                if (!chop.matches()) {
                    throw HexProcessingException.crearErrorFormato(
                            numeroLinea + 1, line, "Estructura del registro incorrecta");
                }

                // Extraer campos del registro
                int length = Integer.parseInt(chop.group(1), 16);
                int address = Integer.parseInt(chop.group(2), 16);
                int type = Integer.parseInt(chop.group(3), 16);
                String dataStr = chop.group(4);
                int checksum = Integer.parseInt(chop.group(5), 16);

                // Convertir datos a bytes
                byte[] data = hexStringToByteArraySeguro(dataStr);

                // Validar longitud
                if (length != data.length) {
                    String razon =
                            String.format(
                                    "Longitud declarada (%d) no coincide con datos (%d)",
                                    length, data.length);
                    throw HexProcessingException.crearErrorFormato(numeroLinea + 1, line, razon);
                }

                // Verificar checksum con logging detallado
                if (!verificarChecksumRegistro(line, checksum, numeroLinea + 1)) {
                    // La excepción ya fue lanzada en verificarChecksumRegistro
                    return;
                }

                // Procesar registros según su tipo
                switch (type) {
                    case 0: // Registro de datos
                        int direccionCompleta = address | extendedAddress;
                        records.add(new HexRecord(direccionCompleta, data));
                        registrosValidos++;
                        break;

                    case 1: // EOF
                        eof = true;
                        break;

                    case 2: // Extended Segment Address
                        extendedAddress = ((data[0] & 0xFF) << 8 | (data[1] & 0xFF)) << 4;
                        break;

                    case 4: // Extended Linear Address
                        extendedAddress = ((data[0] & 0xFF) << 8 | (data[1] & 0xFF)) << 16;
                        break;

                    default:
                        throw HexProcessingException.crearTipoDesconocido(
                                numeroLinea + 1, line, type);
                }
            }

            // Verificar que se encontró EOF
            if (!eof) {}

        } catch (NumberFormatException e) {
            throw new HexProcessingException("Error de formato numérico en archivo HEX", e);
        } catch (Exception e) {
            if (e instanceof HexProcessingException) {
                throw e;
            } else {
                throw new HexProcessingException("Error inesperado procesando archivo HEX", e);
            }
        }
    }

    public List<HexRecord> getRecords() {
        return records;
    }

    /**
     * Fusiona todos los registros HEX en un buffer de datos con validación y logging.
     *
     * @param dataBuffer Buffer de destino donde fusionar los datos
     * @return Buffer actualizado con todos los datos fusionados
     * @throws HexProcessingException Si los datos exceden el buffer o hay conflictos
     */
    public byte[] merge(byte[] dataBuffer) throws HexProcessingException {
        ByteUtils.validarArray(dataBuffer, -1, "dataBuffer");

        try {

            int registrosFusionados = 0;
            int bytesFusionados = 0;

            for (HexRecord record : records) {
                int address = record.address;
                byte[] data = record.data;

                // Validar que el registro cabe en el buffer
                if ((address + data.length) > dataBuffer.length) {
                    String mensaje =
                            String.format(
                                    "Registro fuera de rango: addr=0x%06X, len=%d, bufferSize=%d",
                                    address, data.length, dataBuffer.length);
                    throw HexProcessingException.crearErrorDireccion(
                            -1, address, 0, dataBuffer.length - 1, "BUFFER");
                }

                // Fusionar datos usando ByteUtils
                ByteUtils.copiarBytes(data, 0, dataBuffer, address, data.length);
                registrosFusionados++;
                bytesFusionados += data.length;
            }

            return dataBuffer;

        } catch (Exception e) {
            if (e instanceof HexProcessingException) {
                throw e;
            } else {
                throw new HexProcessingException("Error fusionando registros HEX", e);
            }
        }
    }

    /**
     * Verifica el checksum de un registro HEX con logging detallado.
     *
     * @param registro Línea completa del registro
     * @param checksumEsperado Checksum esperado del registro
     * @param numeroLinea Número de línea para logging
     * @return true si el checksum es válido
     * @throws HexProcessingException Si el checksum es inválido
     */
    private boolean verificarChecksumRegistro(
            String registro, int checksumEsperado, int numeroLinea) throws HexProcessingException {

        int checksumCalculado = 0;

        // Calcular checksum excluyendo el ':' inicial y los 2 últimos caracteres (checksum)
        for (int i = 1; i < registro.length() - 2; i += 2) {
            String byteHex = registro.substring(i, i + 2);
            checksumCalculado = (checksumCalculado + Integer.parseInt(byteHex, 16)) % 256;
        }
        checksumCalculado = (256 - checksumCalculado) % 256;

        boolean valido = (checksumCalculado == checksumEsperado);

        if (!valido) {
            throw HexProcessingException.crearErrorChecksum(
                    numeroLinea, registro, checksumCalculado, checksumEsperado);
        }

        return true;
    }

    /**
     * Convierte string hexadecimal a array de bytes de forma segura.
     *
     * @param hexStr String hexadecimal
     * @return Array de bytes
     * @throws HexProcessingException Si el string no es válido
     */
    private byte[] hexStringToByteArraySeguro(String hexStr) throws HexProcessingException {
        try {
            return ByteUtils.hexToBytes(hexStr);
        } catch (IllegalArgumentException e) {
            throw new HexProcessingException("Error en datos hexadecimales: " + hexStr, e);
        }
    }

    /**
     * Convierte string hexadecimal a array de bytes (método legacy).
     *
     * @deprecated Usar hexStringToByteArraySeguro() en su lugar
     */
    @Deprecated
    private byte[] hexStringToByteArray(String s) {
        try {
            return hexStringToByteArraySeguro(s);
        } catch (HexProcessingException e) {
            // Comportamiento legacy: retornar array vacío en caso de error
            return new byte[0];
        }
    }
}
