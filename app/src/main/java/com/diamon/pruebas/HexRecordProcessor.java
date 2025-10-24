package com.diamon.pruebas;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * HexRecordProcessor - Procesamiento de registros HEX para PICs
 *
 * Esta clase contiene métodos estáticos para:
 * - Filtrar registros por rango de direcciones
 * - Fusionar registros en un buffer de datos
 * - Manipular datos de memoria de PICs
 *
 * Migrado del código Python 2 que trabaja con archivos Intel HEX.
 *
 * Propósito: Proporcionar operaciones sobre colecciones de registros HEX,
 * como filtrado por direcciones y fusión con datos por defecto.
 */
public class HexRecordProcessor {

    /**
     * Filtra registros HEX por rango de direcciones
     *
     * Dado una lista de registros HEX, retorna una nueva lista que contiene
     * solo los datos dentro del rango especificado [lowerBound, upperBound).
     *
     * Maneja 5 casos posibles:
     * 1. Registro completamente debajo del límite inferior → ignorar
     * 2. Registro parcialmente debajo del límite inferior → cortar y agregar
     * 3. Registro completamente dentro del rango → agregar completo
     * 4. Registro parcialmente arriba del límite superior → cortar y agregar
     * 5. Registro completamente arriba del límite superior → ignorar
     *
     * @param records Lista de registros HEX a filtrar
     * @param lowerBound Límite inferior del rango (inclusivo)
     * @param upperBound Límite superior del rango (exclusivo)
     * @return Nueva lista con solo los registros en el rango
     *
     * Ejemplo:
     * records = [HexRecord(0x3FFE, [1,2,3,4]), HexRecord(0x4002, [5,6,7,8])]
     * rangeFilterRecords(records, 0x4000, 0x4008)
     * → [HexRecord(0x4000, [3,4]), HexRecord(0x4002, [5,6,7,8])]
     */
    public static List<HexRecord> rangeFilterRecords(
            List<HexRecord> records,
            int lowerBound,
            int upperBound) {

        List<HexRecord> result = new ArrayList<>();

        for (HexRecord record : records) {
            int recordStart = record.getAddress();
            int recordEnd = record.getEndAddress();

            // Caso 1 y 5: Registro completamente fuera del rango
            if (recordEnd <= lowerBound || recordStart >= upperBound) {
                continue;  // Ignorar este registro
            }

            // Caso 2: Registro parcialmente debajo del límite inferior
            if (recordStart < lowerBound && recordEnd > lowerBound) {
                // Calcular cuántos bytes cortar del inicio
                int sliceOffset = lowerBound - recordStart;
                int sliceLength = Math.min(recordEnd - lowerBound,
                        upperBound - lowerBound);

                result.add(record.slice(sliceOffset, sliceLength));
            }
            // Caso 3: Registro completamente dentro del rango
            else if (recordStart >= lowerBound && recordEnd <= upperBound) {
                result.add(record);
            }
            // Caso 4: Registro parcialmente arriba del límite superior
            else if (recordStart >= lowerBound && recordStart < upperBound) {
                // Calcular cuántos bytes incluir
                int sliceLength = upperBound - recordStart;
                result.add(record.slice(0, sliceLength));
            }
        }

        return result;
    }

    /**
     * Fusiona registros HEX en un buffer de datos por defecto
     *
     * Dado una lista de registros HEX y un buffer con datos por defecto,
     * crea un nuevo buffer donde los registros HEX sobrescriben los datos
     * por defecto en sus posiciones correspondientes.
     *
     * Los huecos entre registros se llenan con defaultData.
     *
     * @param records Lista de registros HEX a fusionar
     * @param defaultData Buffer con datos por defecto
     * @param baseAddress Dirección base del buffer defaultData
     * @return Nuevo array de bytes con datos fusionados
     * @throws IndexOutOfBoundsException Si algún registro está fuera de rango
     *
     * Ejemplo:
     * defaultData = [0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]  (base: 0x4000)
     * records = [HexRecord(0x4001, [0x12, 0x34])]
     * mergeRecords(records, defaultData, 0x4000)
     * → [0xFF, 0x12, 0x34, 0xFF, 0xFF, 0xFF]
     *
     * Algoritmo:
     * 1. Recorrer registros en orden
     * 2. Copiar defaultData desde última posición hasta inicio del registro
     * 3. Copiar datos del registro
     * 4. Marcar nueva posición
     * 5. Al final, copiar resto de defaultData
     */
    public static byte[] mergeRecords(
            List<HexRecord> records,
            byte[] defaultData,
            int baseAddress) {

        // Crear lista dinámica para construir el resultado
        List<Byte> resultList = new ArrayList<>();

        int mark = 0;  // Marca en defaultData donde terminamos la última copia

        for (HexRecord record : records) {
            int recordAddress = record.getAddress();
            int recordLength = record.getLength();

            // Validar que el registro está dentro del rango del buffer
            if (recordAddress < baseAddress ||
                    (recordAddress + recordLength) > (baseAddress + defaultData.length)) {
                throw new IndexOutOfBoundsException(
                        String.format("Registro fuera de rango: addr=0x%04X, len=%d, " +
                                        "buffer=[0x%04X, 0x%04X)",
                                recordAddress, recordLength,
                                baseAddress, baseAddress + defaultData.length));
            }

            // Calcular posición en el buffer (relativo a baseAddress)
            int point = recordAddress - baseAddress;

            // Si hay hueco entre mark y point, llenarlo con defaultData
            if (mark != point) {
                for (int i = mark; i < point; i++) {
                    resultList.add(defaultData[i]);
                }
                mark = point;
            }

            // Agregar datos del registro
            byte[] recordData = record.getData();
            for (byte b : recordData) {
                resultList.add(b);
            }

            // Actualizar marca
            mark += recordLength;
        }

        // Llenar el resto con defaultData si es necesario
        if (mark < defaultData.length) {
            for (int i = mark; i < defaultData.length; i++) {
                resultList.add(defaultData[i]);
            }
        }

        // Convertir List<Byte> a byte[]
        byte[] result = new byte[resultList.size()];
        for (int i = 0; i < resultList.size(); i++) {
            result[i] = resultList.get(i);
        }

        return result;
    }

    /**
     * Sobrecarga de mergeRecords con baseAddress = 0 por defecto
     *
     * @param records Lista de registros HEX
     * @param defaultData Buffer con datos por defecto
     * @return Array de bytes fusionado
     */
    public static byte[] mergeRecords(List<HexRecord> records, byte[] defaultData) {
        return mergeRecords(records, defaultData, 0);
    }
}
