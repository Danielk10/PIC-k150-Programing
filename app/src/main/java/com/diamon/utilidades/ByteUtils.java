package com.diamon.utilidades;

import com.diamon.excepciones.UsbCommunicationException;
import com.diamon.utilidades.LogManager.Categoria;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidades seguras para operaciones con bytes en la aplicación PIC K150.
 *
 * <p>Esta clase proporciona métodos seguros para manipulación de bytes, incluyendo validación,
 * conversión, verificación de integridad y logging automático de operaciones críticas.
 *
 * <p>Características principales:
 *
 * <ul>
 *   <li>Validación automática de parámetros de entrada
 *   <li>Logging integrado de operaciones de bytes
 *   <li>Verificación de integridad con checksums
 *   <li>Conversiones seguras entre tipos de datos
 *   <li>Manejo de endianness para comunicación USB
 *   <li>Utilidades para formateo y visualización
 * </ul>
 *
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public final class ByteUtils {

    // ========== CONSTANTES ==========

    /** Tamaño máximo de buffer para operaciones seguras */
    private static final int MAX_BUFFER_SIZE = 64 * 1024; // 64KB

    /** Valor para bytes no válidos o de relleno */
    public static final byte BYTE_INVALIDO = (byte) 0xFF;

    /** Caracteres hexadecimales válidos */
    private static final String HEX_CHARS = "0123456789ABCDEF";

    /** Prefijo para logging de operaciones de bytes */
    private static final String LOG_PREFIX = "ByteOp";

    // Constructor privado para clase utilitaria
    private ByteUtils() {
        throw new AssertionError("Clase utilitaria - no instanciar");
    }

    // ========== VALIDACIÓN DE PARÁMETROS ==========

    /**
     * Valida que un array de bytes no sea null y tenga el tamaño esperado.
     *
     * @param datos Array a validar
     * @param tamanoEsperado Tamaño esperado (-1 para no validar tamaño)
     * @param nombreParametro Nombre del parámetro para mensajes de error
     * @throws IllegalArgumentException Si la validación falla
     */
    public static void validarArray(byte[] datos, int tamanoEsperado, String nombreParametro) {
        if (datos == null) {
            String mensaje = String.format("Parámetro '%s' no puede ser null", nombreParametro);
            LogManager.e(Categoria.DATA, LOG_PREFIX, mensaje);
            throw new IllegalArgumentException(mensaje);
        }

        if (tamanoEsperado >= 0 && datos.length != tamanoEsperado) {
            String mensaje =
                    String.format(
                            "Parámetro '%s' debe tener %d bytes, pero tiene %d",
                            nombreParametro, tamanoEsperado, datos.length);
            LogManager.e(Categoria.DATA, LOG_PREFIX, mensaje);
            throw new IllegalArgumentException(mensaje);
        }

        if (datos.length > MAX_BUFFER_SIZE) {
            String mensaje =
                    String.format(
                            "Parámetro '%s' excede el tamaño máximo permitido (%d bytes)",
                            nombreParametro, MAX_BUFFER_SIZE);
            LogManager.e(Categoria.DATA, LOG_PREFIX, mensaje);
            throw new IllegalArgumentException(mensaje);
        }

        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format("Array '%s' validado: %d bytes", nombreParametro, datos.length));
    }

    /**
     * Valida que los índices de un array estén dentro del rango válido.
     *
     * @param array Array de referencia
     * @param offset Índice de inicio
     * @param longitud Longitud de datos
     * @throws IndexOutOfBoundsException Si los índices están fuera de rango
     */
    public static void validarRango(byte[] array, int offset, int longitud) {
        if (offset < 0 || longitud < 0 || offset + longitud > array.length) {
            String mensaje =
                    String.format(
                            "Rango inválido: offset=%d, longitud=%d, arraySize=%d",
                            offset, longitud, array.length);
            LogManager.e(Categoria.DATA, LOG_PREFIX, mensaje);
            throw new IndexOutOfBoundsException(mensaje);
        }
    }

    // ========== CONVERSIONES SEGURAS ==========

    /**
     * Convierte un array de bytes a string hexadecimal de forma segura.
     *
     * @param datos Array de bytes a convertir
     * @return String hexadecimal en mayúsculas
     */
    public static String bytesToHex(byte[] datos) {
        validarArray(datos, -1, "datos");

        StringBuilder sb = new StringBuilder(datos.length * 2);
        for (byte b : datos) {
            sb.append(HEX_CHARS.charAt((b >> 4) & 0xF));
            sb.append(HEX_CHARS.charAt(b & 0xF));
        }

        String resultado = sb.toString();
        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Convertidos %d bytes a hex: %s",
                        datos.length,
                        resultado.length() > 32 ? resultado.substring(0, 32) + "..." : resultado));
        return resultado;
    }

    /**
     * Convierte un string hexadecimal a array de bytes de forma segura.
     *
     * @param hex String hexadecimal (puede contener espacios)
     * @return Array de bytes
     * @throws IllegalArgumentException Si el string hexadecimal es inválido
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("String hexadecimal no puede ser null");
        }

        // Limpiar espacios y normalizar
        hex = hex.replaceAll("\\s+", "").toUpperCase();

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("String hexadecimal debe tener longitud par");
        }

        // Validar caracteres hexadecimales
        for (char c : hex.toCharArray()) {
            if (HEX_CHARS.indexOf(c) == -1) {
                throw new IllegalArgumentException("Carácter hexadecimal inválido: " + c);
            }
        }

        byte[] resultado = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = HEX_CHARS.indexOf(hex.charAt(i));
            int low = HEX_CHARS.indexOf(hex.charAt(i + 1));
            resultado[i / 2] = (byte) ((high << 4) | low);
        }

        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format("Convertido hex a %d bytes: %s", resultado.length, hex));
        return resultado;
    }

    /**
     * Convierte un entero a array de bytes con endianness especificado.
     *
     * @param valor Valor entero a convertir
     * @param bigEndian true para big-endian, false para little-endian
     * @return Array de 4 bytes
     */
    public static byte[] intToBytes(int valor, boolean bigEndian) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(valor);

        byte[] resultado = buffer.array();
        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Int %d convertido a bytes (%s): %s",
                        valor, bigEndian ? "BE" : "LE", bytesToHex(resultado)));
        return resultado;
    }

    /**
     * Convierte un short a array de bytes con endianness especificado.
     *
     * @param valor Valor short a convertir
     * @param bigEndian true para big-endian, false para little-endian
     * @return Array de 2 bytes
     */
    public static byte[] shortToBytes(short valor, boolean bigEndian) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(valor);

        byte[] resultado = buffer.array();
        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Short %d convertido a bytes (%s): %s",
                        valor, bigEndian ? "BE" : "LE", bytesToHex(resultado)));
        return resultado;
    }

    /**
     * Convierte array de bytes a entero con endianness especificado.
     *
     * @param datos Array de bytes (debe tener exactamente 4 bytes)
     * @param bigEndian true para big-endian, false para little-endian
     * @return Valor entero
     */
    public static int bytesToInt(byte[] datos, boolean bigEndian) {
        validarArray(datos, 4, "datos");

        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        int resultado = buffer.getInt();
        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Bytes %s convertidos a int (%s): %d",
                        bytesToHex(datos), bigEndian ? "BE" : "LE", resultado));
        return resultado;
    }

    /**
     * Convierte array de bytes a short con endianness especificado.
     *
     * @param datos Array de bytes (debe tener exactamente 2 bytes)
     * @param bigEndian true para big-endian, false para little-endian
     * @return Valor short
     */
    public static short bytesToShort(byte[] datos, boolean bigEndian) {
        validarArray(datos, 2, "datos");

        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        short resultado = buffer.getShort();
        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Bytes %s convertidos a short (%s): %d",
                        bytesToHex(datos), bigEndian ? "BE" : "LE", resultado));
        return resultado;
    }

    // ========== OPERACIONES DE MANIPULACIÓN ==========

    /**
     * Copia de forma segura un rango de bytes entre arrays.
     *
     * @param origen Array origen
     * @param offsetOrigen Índice de inicio en origen
     * @param destino Array destino
     * @param offsetDestino Índice de inicio en destino
     * @param longitud Número de bytes a copiar
     * @return Número de bytes copiados efectivamente
     */
    public static int copiarBytes(
            byte[] origen, int offsetOrigen, byte[] destino, int offsetDestino, int longitud) {
        validarArray(origen, -1, "origen");
        validarArray(destino, -1, "destino");
        validarRango(origen, offsetOrigen, longitud);
        validarRango(destino, offsetDestino, longitud);

        System.arraycopy(origen, offsetOrigen, destino, offsetDestino, longitud);

        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Copiados %d bytes desde pos %d a pos %d",
                        longitud, offsetOrigen, offsetDestino));
        return longitud;
    }

    /**
     * Rellena un array con un valor específico.
     *
     * @param array Array a rellenar
     * @param valor Valor para rellenar
     * @param offset Índice de inicio
     * @param longitud Número de bytes a rellenar
     */
    public static void rellenarArray(byte[] array, byte valor, int offset, int longitud) {
        validarArray(array, -1, "array");
        validarRango(array, offset, longitud);

        for (int i = offset; i < offset + longitud; i++) {
            array[i] = valor;
        }

        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Array rellenado con 0x%02X desde pos %d por %d bytes",
                        valor & 0xFF, offset, longitud));
    }

    /**
     * Intercambia bytes de un array (swab operation).
     *
     * @param datos Array original
     * @return Nuevo array con bytes intercambiados de a pares
     */
    public static byte[] intercambiarBytes(byte[] datos) {
        validarArray(datos, -1, "datos");

        if (datos.length % 2 != 0) {
            throw new IllegalArgumentException("Array debe tener longitud par para intercambio");
        }

        byte[] resultado = new byte[datos.length];
        for (int i = 0; i < datos.length; i += 2) {
            resultado[i] = datos[i + 1];
            resultado[i + 1] = datos[i];
        }

        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format("Intercambiados %d pares de bytes", datos.length / 2));
        return resultado;
    }

    // ========== VERIFICACIÓN DE INTEGRIDAD ==========

    /**
     * Calcula checksum simple (suma de bytes módulo 256).
     *
     * @param datos Array de bytes
     * @param offset Índice de inicio
     * @param longitud Número de bytes a incluir
     * @return Checksum calculado
     */
    public static int calcularChecksum(byte[] datos, int offset, int longitud) {
        validarArray(datos, -1, "datos");
        validarRango(datos, offset, longitud);

        int checksum = 0;
        for (int i = offset; i < offset + longitud; i++) {
            checksum = (checksum + (datos[i] & 0xFF)) % 256;
        }

        LogManager.v(
                Categoria.DATA,
                LOG_PREFIX,
                String.format("Checksum calculado para %d bytes: 0x%02X", longitud, checksum));
        return checksum;
    }

    /**
     * Verifica checksum simple de un array.
     *
     * @param datos Array incluyendo checksum al final
     * @return true si el checksum es válido
     */
    public static boolean verificarChecksum(byte[] datos) {
        if (datos.length < 2) {
            return false;
        }

        int checksumCalculado = calcularChecksum(datos, 0, datos.length - 1);
        int checksumEsperado = datos[datos.length - 1] & 0xFF;

        boolean valido = checksumCalculado == checksumEsperado;
        LogManager.d(
                Categoria.DATA,
                LOG_PREFIX,
                String.format(
                        "Verificación checksum: calculado=0x%02X, esperado=0x%02X, válido=%s",
                        checksumCalculado, checksumEsperado, valido));
        return valido;
    }

    /**
     * Calcula hash MD5 de un array de bytes.
     *
     * @param datos Array de bytes
     * @return Hash MD5 como string hexadecimal
     */
    public static String calcularHashMD5(byte[] datos) {
        validarArray(datos, -1, "datos");

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(datos);
            String resultado = bytesToHex(hash);

            LogManager.d(
                    Categoria.DATA,
                    LOG_PREFIX,
                    String.format("Hash MD5 calculado para %d bytes: %s", datos.length, resultado));
            return resultado;
        } catch (NoSuchAlgorithmException e) {
            LogManager.e(Categoria.DATA, LOG_PREFIX, "Error calculando MD5", e);
            throw new RuntimeException("MD5 no disponible", e);
        }
    }

    // ========== UTILIDADES USB ESPECÍFICAS ==========

    /**
     * Valida respuesta USB esperada con logging detallado.
     *
     * @param respuesta Respuesta recibida
     * @param esperado Valor esperado
     * @param comando Comando que generó la respuesta
     * @return true si la respuesta es correcta
     * @throws UsbCommunicationException Si la respuesta no coincide
     */
    public static boolean validarRespuestaUSB(byte[] respuesta, byte esperado, String comando)
            throws UsbCommunicationException {
        validarArray(respuesta, -1, "respuesta");

        if (respuesta.length == 0) {
            LogManager.e(
                    Categoria.USB,
                    LOG_PREFIX,
                    String.format("Respuesta vacía para comando '%s'", comando));
            throw UsbCommunicationException.crearRespuestaInesperada(
                    String.format("0x%02X", esperado), "VACIO", comando);
        }

        byte recibido = respuesta[0];
        boolean valida = recibido == esperado;

        if (valida) {
            LogManager.v(
                    Categoria.USB,
                    LOG_PREFIX,
                    String.format("Respuesta USB válida para '%s': 0x%02X", comando, recibido));
        } else {
            LogManager.w(
                    Categoria.USB,
                    LOG_PREFIX,
                    String.format(
                            "Respuesta USB inválida para '%s': esperado=0x%02X, recibido=0x%02X",
                            comando, esperado, recibido));
            throw UsbCommunicationException.crearRespuestaInesperada(
                    String.format("0x%02X", esperado), String.format("0x%02X", recibido), comando);
        }

        return valida;
    }

    /**
     * Prepara datos para envío USB con validación y logging.
     *
     * @param datos Datos a enviar
     * @param comando Nombre del comando para logging
     * @return Array validado listo para envío
     */
    public static byte[] prepararDatosUSB(byte[] datos, String comando) {
        validarArray(datos, -1, "datos");

        // Crear copia para evitar modificaciones accidentales
        byte[] copia = new byte[datos.length];
        copiarBytes(datos, 0, copia, 0, datos.length);

        LogManager.d(
                Categoria.USB,
                LOG_PREFIX,
                String.format("Datos USB preparados para '%s': %d bytes", comando, copia.length));
        LogManager.logDatosUSB("ENVÍO", copia, copia.length);

        return copia;
    }

    // ========== UTILIDADES DE FORMATEO ==========

    /**
     * Formatea array de bytes para visualización con agrupación.
     *
     * @param datos Array de bytes
     * @param bytesPorLinea Número de bytes por línea
     * @param mostrarAscii true para mostrar representación ASCII
     * @return String formateado para visualización
     */
    public static String formatearParaVisualizacion(
            byte[] datos, int bytesPorLinea, boolean mostrarAscii) {
        validarArray(datos, -1, "datos");

        if (bytesPorLinea <= 0) {
            bytesPorLinea = 16;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < datos.length; i += bytesPorLinea) {
            // Dirección
            sb.append(String.format("%04X: ", i));

            // Bytes hexadecimales
            int bytesEnLinea = Math.min(bytesPorLinea, datos.length - i);
            for (int j = 0; j < bytesEnLinea; j++) {
                sb.append(String.format("%02X ", datos[i + j]));
            }

            // Relleno si es necesario
            for (int j = bytesEnLinea; j < bytesPorLinea; j++) {
                sb.append("   ");
            }

            // Representación ASCII
            if (mostrarAscii) {
                sb.append(" |");
                for (int j = 0; j < bytesEnLinea; j++) {
                    byte b = datos[i + j];
                    char c = (b >= 32 && b <= 126) ? (char) b : '.';
                    sb.append(c);
                }
                sb.append("|");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Obtiene un resumen estadístico de un array de bytes.
     *
     * @param datos Array de bytes
     * @return String con estadísticas del array
     */
    public static String obtenerEstadisticas(byte[] datos) {
        validarArray(datos, -1, "datos");

        if (datos.length == 0) {
            return "Array vacío";
        }

        int ceros = 0, unos = 0;
        byte min = datos[0], max = datos[0];

        for (byte b : datos) {
            if (b == 0) ceros++;
            if (b == (byte) 0xFF) unos++;
            if (b < min) min = b;
            if (b > max) max = b;
        }

        return String.format(
                "Tamaño: %d bytes, Min: 0x%02X, Max: 0x%02X, Ceros: %d, 0xFF: %d",
                datos.length, min & 0xFF, max & 0xFF, ceros, unos);
    }

    public static int bytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN); // o LITTLE_ENDIAN según tu caso
        return buffer.getInt();
    }
}
