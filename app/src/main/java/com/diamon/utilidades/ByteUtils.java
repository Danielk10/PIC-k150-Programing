package com.diamon.utilidades;

import com.diamon.excepciones.UsbCommunicationException;
import com.diamon.pic.R;

import android.content.Context;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidades seguras para operaciones con bytes en la aplicación PIC K150.
 *
 * <p>
 * Esta clase proporciona métodos seguros para manipulación de bytes, incluyendo
 * validación,
 * conversión, verificación de integridad y logging automático de operaciones
 * críticas.
 *
 * <p>
 * Características principales:
 *
 * <ul>
 * <li>Validación automática de parámetros de entrada
 * <li>Logging integrado de operaciones de bytes
 * <li>Verificación de integridad con checksums
 * <li>Conversiones seguras entre tipos de datos
 * <li>Manejo de endianness para comunicación USB
 * <li>Utilidades para formateo y visualización
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
     * @param datos           Array a validar
     * @param tamanoEsperado  Tamaño esperado (-1 para no validar tamaño)
     * @param nombreParametro Nombre del parámetro para mensajes de error
     * @throws IllegalArgumentException Si la validación falla
     */
    public static void validarArray(android.content.Context context, byte[] datos, int tamanoEsperado, String nombreParametro) {
        if (datos == null) {
            String mensaje = context.getString(R.string.error_argumento_null, nombreParametro);
            throw new IllegalArgumentException(mensaje);
        }

        if (tamanoEsperado >= 0 && datos.length != tamanoEsperado) {
            String mensaje = context.getString(R.string.error_tamano_esperado, nombreParametro, tamanoEsperado, datos.length);
            throw new IllegalArgumentException(mensaje);
        }

        if (datos.length > MAX_BUFFER_SIZE) {
            String mensaje = context.getString(R.string.error_tamano_maximo, nombreParametro, MAX_BUFFER_SIZE);
            throw new IllegalArgumentException(mensaje);
        }
    }

    /**
     * Valida que los índices de un array estén dentro del rango válido.
     *
     * @param array    Array de referencia
     * @param offset   Índice de inicio
     * @param longitud Longitud de datos
     * @throws IndexOutOfBoundsException Si los índices están fuera de rango
     */
    public static void validarRango(android.content.Context context, byte[] array, int offset, int longitud) {
        if (offset < 0 || longitud < 0 || offset + longitud > array.length) {
            String mensaje = context.getString(R.string.error_rango_invalido, offset, longitud, array.length);
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
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Convierte un array de bytes a string hexadecimal de forma ultra rápida.
     *
     * @param datos Array de bytes a convertir
     * @return String hexadecimal en mayúsculas
     */
    public static String bytesToHex(byte[] datos) {
        if (datos == null)
            return "";
        char[] hexChars = new char[datos.length * 2];
        for (int j = 0; j < datos.length; j++) {
            int v = datos[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Añade la representación hexadecimal de un array de bytes a un StringBuilder
     * existente.
     * Evita la creación de objetos string intermedios.
     *
     * @param data   Array de bytes
     * @param length Cantidad de bytes a procesar
     * @param sb     StringBuilder donde añadir los datos
     */
    public static void appendHexToBuilder(byte[] data, int length, StringBuilder sb) {
        if (data == null || sb == null)
            return;
        for (int j = 0; j < length; j++) {
            int v = data[j] & 0xFF;
            sb.append(HEX_ARRAY[v >>> 4]);
            sb.append(HEX_ARRAY[v & 0x0F]);
        }
    }

    /**
     * Convierte un string hexadecimal a array de bytes de forma segura.
     *
     * @param hex String hexadecimal (puede contener espacios)
     * @return Array de bytes
     * @throws IllegalArgumentException Si el string hexadecimal es inválido
     */
    public static byte[] hexToBytes(android.content.Context context, String hex) {
        if (hex == null) {
            throw new IllegalArgumentException(context.getString(R.string.error_hex_null));
        }

        // Limpiar espacios y normalizar
        hex = hex.replaceAll("\\s+", "").toUpperCase();

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException(context.getString(R.string.error_hex_longitud));
        }

        // Validar caracteres hexadecimales
        for (char c : hex.toCharArray()) {
            if (HEX_CHARS.indexOf(c) == -1) {
                throw new IllegalArgumentException(context.getString(R.string.error_hex_caracter, String.valueOf(c)));
            }
        }

        byte[] resultado = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = HEX_CHARS.indexOf(hex.charAt(i));
            int low = HEX_CHARS.indexOf(hex.charAt(i + 1));
            resultado[i / 2] = (byte) ((high << 4) | low);
        }

        return resultado;
    }

    /**
     * Convierte un entero a array de bytes con endianness especificado.
     *
     * @param valor     Valor entero a convertir
     * @param bigEndian true para big-endian, false para little-endian
     * @return Array de 4 bytes
     */
    public static byte[] intToBytes(int valor, boolean bigEndian) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(valor);

        byte[] resultado = buffer.array();
        return resultado;
    }

    /**
     * Convierte un short a array de bytes con endianness especificado.
     *
     * @param valor     Valor short a convertir
     * @param bigEndian true para big-endian, false para little-endian
     * @return Array de 2 bytes
     */
    public static byte[] shortToBytes(short valor, boolean bigEndian) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(valor);

        byte[] resultado = buffer.array();
        return resultado;
    }

    /**
     * Convierte array de bytes a entero con endianness especificado.
     *
     * @param datos     Array de bytes (debe tener exactamente 4 bytes)
     * @param bigEndian true para big-endian, false para little-endian
     * @return Valor entero
     */
    public static int bytesToInt(android.content.Context context, byte[] datos, boolean bigEndian) {
        validarArray(context, datos, 4, "datos");

        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        int resultado = buffer.getInt();
        return resultado;
    }

    /**
     * Convierte array de bytes a short con endianness especificado.
     *
     * @param datos     Array de bytes (debe tener exactamente 2 bytes)
     * @param bigEndian true para big-endian, false para little-endian
     * @return Valor short
     */
    public static short bytesToShort(android.content.Context context, byte[] datos, boolean bigEndian) {
        validarArray(context, datos, 2, "datos");

        ByteBuffer buffer = ByteBuffer.wrap(datos);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        short resultado = buffer.getShort();
        return resultado;
    }

    // ========== OPERACIONES DE MANIPULACIÓN ==========

    /**
     * Copia de forma segura un rango de bytes entre arrays.
     *
     * @param origen        Array origen
     * @param offsetOrigen  Índice de inicio en origen
     * @param destino       Array destino
     * @param offsetDestino Índice de inicio en destino
     * @param longitud      Número de bytes a copiar
     * @return Número de bytes copiados efectivamente
     */
    public static int copiarBytes(
            android.content.Context context, byte[] origen, int offsetOrigen, byte[] destino, int offsetDestino, int longitud) {
        validarArray(context, origen, -1, "origen");
        validarArray(context, destino, -1, "destino");
        validarRango(context, origen, offsetOrigen, longitud);
        validarRango(context, destino, offsetDestino, longitud);

        System.arraycopy(origen, offsetOrigen, destino, offsetDestino, longitud);

        return longitud;
    }

    /**
     * Rellena un array con un valor específico.
     *
     * @param array    Array a rellenar
     * @param valor    Valor para rellenar
     * @param offset   Índice de inicio
     * @param longitud Número de bytes a rellenar
     */
    public static void rellenarArray(android.content.Context context, byte[] array, byte valor, int offset, int longitud) {
        validarArray(context, array, -1, "array");
        validarRango(context, array, offset, longitud);

        for (int i = offset; i < offset + longitud; i++) {
            array[i] = valor;
        }
    }

    /**
     * Intercambia bytes de un array (swab operation).
     *
     * @param datos Array original
     * @return Nuevo array con bytes intercambiados de a pares
     */
    public static byte[] intercambiarBytes(android.content.Context context, byte[] datos) {
        validarArray(context, datos, -1, "datos");

        if (datos.length % 2 != 0) {
            throw new IllegalArgumentException(context.getString(R.string.error_intercambio_par));
        }

        byte[] resultado = new byte[datos.length];
        for (int i = 0; i < datos.length; i += 2) {
            resultado[i] = datos[i + 1];
            resultado[i + 1] = datos[i];
        }

        return resultado;
    }

    // ========== VERIFICACIÓN DE INTEGRIDAD ==========

    /**
     * Calcula checksum simple (suma de bytes módulo 256).
     *
     * @param datos    Array de bytes
     * @param offset   Índice de inicio
     * @param longitud Número de bytes a incluir
     * @return Checksum calculado
     */
    public static int calcularChecksum(android.content.Context context, byte[] datos, int offset, int longitud) {
        validarArray(context, datos, -1, "datos");
        validarRango(context, datos, offset, longitud);

        int checksum = 0;
        for (int i = offset; i < offset + longitud; i++) {
            checksum = (checksum + (datos[i] & 0xFF)) % 256;
        }

        return checksum;
    }

    /**
     * Verifica checksum simple de un array.
     *
     * @param datos Array incluyendo checksum al final
     * @return true si el checksum es válido
     */
    public static boolean verificarChecksum(android.content.Context context, byte[] datos) {
        if (datos.length < 2) {
            return false;
        }

        int checksumCalculado = calcularChecksum(context, datos, 0, datos.length - 1);
        int checksumEsperado = datos[datos.length - 1] & 0xFF;

        boolean valido = checksumCalculado == checksumEsperado;
        return valido;
    }

    /**
     * Calcula hash MD5 de un array de bytes.
     *
     * @param datos Array de bytes
     * @return Hash MD5 como string hexadecimal
     */
    public static String calcularHashMD5(android.content.Context context, byte[] datos) {
        validarArray(context, datos, -1, "datos");

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(datos);
            String resultado = bytesToHex(hash);

            return resultado;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(context.getString(R.string.error_md5_no_disponible), e);
        }
    }

    // ========== UTILIDADES USB ESPECÍFICAS ==========

    /**
     * Valida respuesta USB esperada con logging detallado.
     *
     * @param respuesta Respuesta recibida
     * @param esperado  Valor esperado
     * @param comando   Comando que generó la respuesta
     * @return true si la respuesta es correcta
     * @throws UsbCommunicationException Si la respuesta no coincide
     */
    public static boolean validarRespuestaUSB(android.content.Context context, byte[] respuesta, byte esperado, String comando)
            throws UsbCommunicationException {
        validarArray(context, respuesta, -1, "respuesta");

        if (respuesta.length == 0) {
            throw UsbCommunicationException.crearRespuestaInesperada(
                    String.format("0x%02X", esperado), "VACIO", comando);
        }

        byte recibido = respuesta[0];
        boolean valida = recibido == esperado;

        if (valida) {
        } else {
            throw UsbCommunicationException.crearRespuestaInesperada(
                    String.format("0x%02X", esperado), String.format("0x%02X", recibido), comando);
        }

        return valida;
    }

    /**
     * Prepara datos para envío USB con validación y logging.
     *
     * @param datos   Datos a enviar
     * @param comando Nombre del comando para logging
     * @return Array validado listo para envío
     */
    public static byte[] prepararDatosUSB(android.content.Context context, byte[] datos, String comando) {
        validarArray(context, datos, -1, "datos");

        // Crear copia para evitar modificaciones accidentales
        byte[] copia = new byte[datos.length];
        copiarBytes(context, datos, 0, copia, 0, datos.length);

        return copia;
    }

    // ========== UTILIDADES DE FORMATEO ==========

    /**
     * Formatea array de bytes para visualización con agrupación.
     *
     * @param datos         Array de bytes
     * @param bytesPorLinea Número de bytes por línea
     * @param mostrarAscii  true para mostrar representación ASCII
     * @return String formateado para visualización
     */
    public static String formatearParaVisualizacion(
            android.content.Context context, byte[] datos, int bytesPorLinea, boolean mostrarAscii) {
        validarArray(context, datos, -1, "datos");

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
    public static String obtenerEstadisticas(android.content.Context context, byte[] datos) {
        validarArray(context, datos, -1, "datos");

        if (datos.length == 0) {
            return context.getString(R.string.error_vacio);
        }

        int ceros = 0, unos = 0;
        byte min = datos[0], max = datos[0];

        for (byte b : datos) {
            if (b == 0)
                ceros++;
            if (b == (byte) 0xFF)
                unos++;
            if (b < min)
                min = b;
            if (b > max)
                max = b;
        }

        return context.getString(R.string.stats_format,
                datos.length, min & 0xFF, max & 0xFF, ceros, unos);
    }

    public static int bytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN); // o LITTLE_ENDIAN según tu caso
        return buffer.getInt();
    }
}
