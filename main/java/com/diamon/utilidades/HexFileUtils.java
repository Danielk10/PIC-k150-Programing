package com.diamon.utilidades;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HexFileUtils {

    /**
     * Filtra registros de un archivo HEX por rango de direcciones.
     *
     * @param records Lista de registros HEX como pares (dirección, datos en string).
     * @param lowerBound Límite inferior del rango.
     * @param upperBound Límite superior del rango.
     * @return Nueva lista de registros dentro del rango especificado.
     */

    // Este metodo esta corregido
    public static List<HexFileUtils.Pair<Integer, String>> rangeFilterRecords(
            List<HexFileUtils.Pair<Integer, String>> records, int lowerBound, int upperBound) {

        List<HexFileUtils.Pair<Integer, String>> result = new ArrayList<>();

        for (HexFileUtils.Pair<Integer, String> record : records) {
            int address = record.first; // Dirección del registro
            String data = record.second; // Datos del registro en formato string
            int recordEnd =
                    address
                            + (data.length()
                                    / 2); // Final del registro (cada par de caracteres es 1 byte)

            // Caso 2: Registro parcialmente por debajo del límite inferior
            if (address < lowerBound && recordEnd > lowerBound) {
                int slicePos =
                        (lowerBound - address) * 2; // Calcular posición de corte en caracteres
                String slicedData = data.substring(slicePos); // Cortar los datos desde slicePos
                result.add(new HexFileUtils.Pair<>(lowerBound, slicedData));
            }
            // Caso 3: Registro dentro del rango
            else if (address >= lowerBound && address < upperBound) {
                if (recordEnd <= upperBound) {
                    // Registro completamente dentro del rango
                    result.add(record);
                } else {
                    // Caso 4: Registro parcialmente fuera por encima del límite superior
                    int sliceLength =
                            (upperBound - address) * 2; // Calcular longitud de corte en caracteres
                    String slicedData =
                            data.substring(0, sliceLength); // Cortar los datos hasta sliceLength
                    result.add(new HexFileUtils.Pair<>(address, slicedData));
                }
            }
            // Caso 1 y 5: Registro fuera del rango (no hacer nada)
        }

        return result;
    }

    public static byte[] mergeRecords(
            List<HexFileUtils.Pair<Integer, String>> records, byte[] defaultData, int baseAddress) {

        // Clonamos el arreglo para no modificar el original
        byte[] dataArray = defaultData.clone();

        for (HexFileUtils.Pair<Integer, String> record : records) {
            int address = record.first;
            String data = record.second;

            // Calcula el índice inicial en el arreglo según la dirección base
            int startIndex = address - baseAddress;
            if (startIndex < 0 || startIndex >= dataArray.length) {
                throw new IndexOutOfBoundsException(
                        "Record address " + address + " is outside the allowed range.");
            }

            // Escribe los datos en la posición correcta
            for (int i = 0;
                    i < data.length();
                    i += 2) { // Procesamos de 2 en 2 caracteres (1 byte en hexadecimal)
                int dataIndex = startIndex + i / 2; // Calcula el índice en bytes
                if (dataIndex >= dataArray.length) {
                    break; // Evita sobrescribir fuera del arreglo
                }

                // Convierte los caracteres hexadecimales a un byte
                String byteHex = data.substring(i, i + 2);
                dataArray[dataIndex] = (byte) Integer.parseInt(byteHex, 16);
            }
        }

        return dataArray;
    }

    /** Clase Pair para representar pares de valores (dirección, datos). */
    public static class Pair<F, S> {
        public final F first;
        public final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }

    /**
     * Genera datos en blanco para la ROM en base al tamaño.
     *
     * @param coreBits Número de bits del núcleo del chip.
     * @param romSize Tamaño de la ROM en palabras (no en bytes).
     * @return Datos en blanco para la ROM como arreglo de bytes.
     */
    public static byte[] generateRomBlank(int coreBits, int romSize) {
        // Calcula la palabra ROM en blanco.
        int romBlankWord = 0xFFFF << coreBits;
        romBlankWord = (~romBlankWord) & 0xFFFF;

        // Convierte la palabra en un arreglo de bytes (big-endian).
        byte[] romBlankBytes = ByteBuffer.allocate(2).putShort((short) romBlankWord).array();

        // La ROM total se mide en bytes, por lo tanto, romSize * 2.
        int totalSizeInBytes = romSize * 2; // Cada palabra tiene 2 bytes.

        // Crea el arreglo final con el tamaño total.
        byte[] romBlank = new byte[totalSizeInBytes];

        // Llena el arreglo con las palabras en blanco.
        for (int i = 0; i < romSize; i++) {
            System.arraycopy(romBlankBytes, 0, romBlank, i * 2, 2);
        }

        return romBlank;
    }

    /**
     * Genera datos en blanco para la EEPROM en base al tamaño.
     *
     * @param eepromSize Tamaño de la EEPROM.
     * @return Datos en blanco para la EEPROM como arreglo de bytes.
     */
    public static byte[] generateEepromBlank(int eepromSize) {
        byte eepromBlankByte = (byte) 0xFF;
        byte[] eepromBlank = new byte[eepromSize];
        Arrays.fill(eepromBlank, eepromBlankByte);
        return eepromBlank;
    }

    public static List<HexFileUtils.Pair<Integer, String>> swabRecords(
            List<HexFileUtils.Pair<Integer, String>> records) {
        List<HexFileUtils.Pair<Integer, String>> swabbedRecords = new ArrayList<>();

        for (HexFileUtils.Pair<Integer, String> record : records) {
            StringBuilder swappedData = new StringBuilder();

            // Rellenar datos con '3F' si son más cortos de 4 caracteres
            String paddedData = record.second;
            if (paddedData.length() % 4 != 0) {
                int paddingLength =
                        4 - (paddedData.length() % 4); // Calcular cuántos caracteres faltan
                paddedData =
                        String.format(
                                        "%-" + (paddedData.length() + paddingLength) + "s",
                                        paddedData)
                                .replace(' ', '3');
                paddedData =
                        paddedData.replaceAll("3{2}", "3F"); // Asegurar que el padding sea '3F'
            }

            // Procesar cada palabra (2 bytes -> 4 caracteres hex)
            for (int i = 0; i < paddedData.length(); i += 4) {
                String word = paddedData.substring(i, i + 4); // Obtener una palabra
                swappedData.append(word.substring(2, 4)); // Byte bajo
                swappedData.append(word.substring(0, 2)); // Byte alto
            }

            // Añadir el registro con los datos intercambiados
            swabbedRecords.add(
                    new HexFileUtils.Pair<Integer, String>(record.first, swappedData.toString()));
        }

        return swabbedRecords;
    }

    public static byte[] generarArrayDeDatos(byte dato, int numeroDeDatos) {

        byte[] datos = new byte[numeroDeDatos];

        for (int i = 0; i < datos.length; i++) {

            datos[i] = dato;
        }

        return datos;
    }

    public static byte[] encodeToBytes(int[] integers) {
        ByteBuffer buffer = ByteBuffer.allocate(integers.length * 2); // Cada entero ocupa 2 bytes
        buffer.order(java.nio.ByteOrder.BIG_ENDIAN); // Configurar como big-endian
        for (int value : integers) {
            buffer.putShort((short) value); // Escribir cada entero como 2 bytes
        }
        return buffer.array(); // Devolver el arreglo de bytes
    }

    public static int[] decodeFromBytes(byte[] bytes) {
        if (bytes.length % 2 != 0) {
            throw new IllegalArgumentException("El número de bytes no es múltiplo de 2");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(java.nio.ByteOrder.BIG_ENDIAN); // Leer como big-endian
        int[] integers = new int[bytes.length / 2];
        for (int i = 0; i < integers.length; i++) {
            integers[i] = buffer.getShort() & 0xFFFF; // Leer 2 bytes y convertir a entero sin signo
        }
        return integers;
    }
}
