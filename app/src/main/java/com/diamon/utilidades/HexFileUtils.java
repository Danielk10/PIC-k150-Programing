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
    public static List<HexFileUtils.Pair<Integer, String>> rangeFilterRecords(
            List<HexFileUtils.Pair<Integer, String>> records, int lowerBound, int upperBound) {

        List<Pair<Integer, String>> result = new ArrayList<>();

        for (HexFileUtils.Pair<Integer, String> record : records) {
            int address = record.first;
            String data = record.second;
            int recordEnd = address + data.length();

            if (address >= lowerBound && address < upperBound) {
                if (recordEnd < upperBound) {
                    // Caso 3: Registro completamente dentro del rango.
                    result.add(record);
                } else {
                    // Caso 4: Registro parcialmente fuera por encima del límite superior.
                    int sliceLength = upperBound - address;
                    result.add(
                            new HexFileUtils.Pair<Integer, String>(
                                    address, data.substring(0, sliceLength)));
                }
            } else if (address < lowerBound && recordEnd > lowerBound) {
                // Caso 2: Registro parcialmente fuera por debajo del límite inferior.
                int slicePos = lowerBound - address;
                result.add(
                        new HexFileUtils.Pair<Integer, String>(
                                lowerBound, data.substring(slicePos)));
            }
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

    /*public static List<Pair<Integer, String>> swabRecords(List<Pair<Integer, String>> records) {
    	List<Pair<Integer, String>> swappedRecords = new ArrayList<>();

    	for (Pair<Integer, String> record : records) {
    		StringBuilder swappedData = new StringBuilder();

    		for (int i = 0; i < record.second.length(); i += 4) {
    			// Intercambiar bytes adyacentes
    			swappedData.append(record.second.charAt(i + 2)).append(record.second.charAt(i + 3))
    				.append(record.second.charAt(i)).append(record.second.charAt(i + 1));
    		}

    		swappedRecords.add(new Pair<Integer, String>(record.first, swappedData.toString()));
    	}

    	return swappedRecords;
    }*/

    /**
     * Intercambia los bytes de cada palabra en los registros para convertir entre endianess.
     *
     * @param records Lista de pares (dirección, datos) donde los datos están en formato
     *     hexadecimal.
     * @return Nueva lista de registros con bytes intercambiados.
     */
    public static List<HexFileUtils.Pair<Integer, String>> swabRecords(
            List<HexFileUtils.Pair<Integer, String>> records) {
        List<HexFileUtils.Pair<Integer, String>> swabbedRecords = new ArrayList<>();

        for (HexFileUtils.Pair<Integer, String> record : records) {
            StringBuilder swappedData = new StringBuilder();

            // Procesar cada palabra (2 bytes -> 4 caracteres hex)
            for (int i = 0; i < record.second.length(); i += 4) {
                String word = record.second.substring(i, i + 4); // Obtener una palabra
                // Intercambiar bytes (big-endian <-> little-endian)
                swappedData.append(word.substring(2, 4)); // Byte bajo
                swappedData.append(word.substring(0, 2)); // Byte alto
            }

            // Añadir el registro con los datos intercambiados
            swabbedRecords.add(
                    new HexFileUtils.Pair<Integer, String>(record.first, swappedData.toString()));
        }

        return swabbedRecords;
    }
}
