package com.diamon.pruebas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChipinfoUtils - Funciones auxiliares para manipulación de datos de chips
 *
 * Esta clase contiene métodos estáticos utilitarios que son usados por
 * ChipinfoEntry y ChipinfoReader para operaciones comunes.
 *
 * Incluye funciones para:
 * - Conversión de valores hexadecimales
 * - Operaciones bit a bit indexadas
 * - Formateo de datos
 */
public class ChipinfoUtils {

    /**
     * Convierte un entero a string hexadecimal con formato 0x
     *
     * @param value Valor entero a convertir
     * @return String en formato "0xXXXX"
     */
    public static String toHexString(int value) {
        if (value >= 0) {
            return "0x" + Integer.toHexString(value).toUpperCase();
        } else {
            return "-0x" + Integer.toHexString(-value).toUpperCase();
        }
    }

    /**
     * Convierte un entero a string hexadecimal con longitud fija
     *
     * @param value Valor entero a convertir
     * @param minLength Longitud mínima (se rellena con ceros)
     * @return String hexadecimal con longitud especificada
     */
    public static String toHexString(int value, int minLength) {
        String hex = Integer.toHexString(value).toUpperCase();
        while (hex.length() < minLength) {
            hex = "0" + hex;
        }
        return "0x" + hex;
    }

    /**
     * Realiza operación AND indexada entre una lista y valores específicos
     *
     * Toma una lista de valores y aplica AND con valores en índices específicos.
     * Ejemplo:
     *   fuses = [0x3FFF, 0x0FFF, 0x00FF]
     *   settingValues = [(0, 0x3FFB), (2, 0x00F0)]
     *   resultado = [0x3FFB, 0x0FFF, 0x00F0]
     *
     * @param fuses Lista de valores base
     * @param settingValues Lista de pares (índice, valor) a aplicar
     * @return Nueva lista con operaciones AND aplicadas
     */
    public static List<Integer> indexwiseAnd(List<Integer> fuses,
                                             List<ChipinfoEntry.FuseValue> settingValues) {
        // Crear copia de la lista original
        List<Integer> result = new ArrayList<>(fuses);

        // Aplicar AND en cada índice especificado
        for (ChipinfoEntry.FuseValue fv : settingValues) {
            // Verificar que el índice esté dentro de rango
            if (fv.index >= 0 && fv.index < result.size()) {
                // Realizar AND bit a bit: result[index] = result[index] & value
                int currentValue = result.get(fv.index);
                int newValue = currentValue & fv.value;
                result.set(fv.index, newValue);
            }
        }

        return result;
    }

    /**
     * Convierte una lista de enteros a array de bytes
     *
     * Útil para enviar datos al programador de PICs
     *
     * @param values Lista de valores enteros (16 bits cada uno)
     * @return Array de bytes (little-endian)
     */
    public static byte[] toByteArray(List<Integer> values) {
        byte[] result = new byte[values.size() * 2];

        for (int i = 0; i < values.size(); i++) {
            int value = values.get(i);
            // Little-endian: byte bajo primero
            result[i * 2] = (byte) (value & 0xFF);
            result[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
        }

        return result;
    }

    /**
     * Convierte un array de bytes a lista de enteros
     *
     * Útil para recibir datos del programador de PICs
     *
     * @param bytes Array de bytes (little-endian)
     * @return Lista de valores enteros (16 bits cada uno)
     */
    public static List<Integer> fromByteArray(byte[] bytes) {
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < bytes.length; i += 2) {
            if (i + 1 < bytes.length) {
                // Little-endian: byte bajo primero
                int low = bytes[i] & 0xFF;
                int high = bytes[i + 1] & 0xFF;
                int value = (high << 8) | low;
                result.add(value);
            }
        }

        return result;
    }

    /**
     * Formatea una lista de fusibles para mostrar en UI
     *
     * @param fuseDict Diccionario de fusibles nombre -> valor
     * @return String formateado para mostrar al usuario
     */
    public static String formatFuseDict(Map<String, String> fuseDict) {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuración de Fusibles:\n");
        sb.append("==========================\n\n");

        for (Map.Entry<String, String> entry : fuseDict.entrySet()) {
            sb.append(String.format("%-25s: %s\n", entry.getKey(), entry.getValue()));
        }

        return sb.toString();
    }

    /**
     * Formatea una lista de valores hexadecimales para depuración
     *
     * @param values Lista de valores
     * @param name Nombre descriptivo
     * @return String formateado
     */
    public static String formatHexList(List<Integer> values, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": [");

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(toHexString(values.get(i), 4));
        }

        sb.append("]");
        return sb.toString();
    }
}
