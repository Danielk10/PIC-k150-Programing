package com.diamon.pruebas;

import java.util.Arrays;

/**
 * HexRecord - Representa un registro de archivo Intel HEX
 *
 * En archivos HEX, cada registro contiene:
 * - Una dirección de memoria (address)
 * - Datos binarios (data)
 *
 * Esta clase reemplaza las tuplas (address, data_string) de Python
 * y proporciona métodos para manipular registros HEX.
 *
 * Propósito: Almacenar un bloque de datos con su dirección de memoria asociada,
 * típicamente usado para leer/escribir memoria de programa y configuración de PICs.
 */
public class HexRecord {

    // Dirección de memoria donde comienzan estos datos
    private final int address;

    // Datos binarios del registro
    private final byte[] data;

    /**
     * Constructor - Crea un registro HEX
     *
     * @param address Dirección de memoria (típicamente 0x0000-0xFFFF para PICs)
     * @param data Datos binarios asociados
     */
    public HexRecord(int address, byte[] data) {
        this.address = address;
        // Crear copia defensiva para inmutabilidad
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * Constructor desde String (para compatibilidad con código Python)
     *
     * En Python, los datos eran strings: "\\x12\\x34\\xAB"
     * Este constructor convierte de String ISO-8859-1 a bytes
     *
     * @param address Dirección de memoria
     * @param dataString String con datos binarios
     */
    public HexRecord(int address, String dataString) {
        this.address = address;
        // Convertir string a bytes usando ISO-8859-1 (1 byte por carácter)
        this.data = dataString.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    /**
     * Obtiene la dirección de memoria
     *
     * @return Dirección de inicio del registro
     */
    public int getAddress() {
        return address;
    }

    /**
     * Obtiene los datos del registro
     *
     * @return Copia de los datos (para mantener inmutabilidad)
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Obtiene la longitud de los datos
     *
     * @return Número de bytes en este registro
     */
    public int getLength() {
        return data.length;
    }

    /**
     * Obtiene la dirección final del registro (exclusiva)
     *
     * @return address + length (primer byte después de este registro)
     */
    public int getEndAddress() {
        return address + data.length;
    }

    /**
     * Verifica si este registro contiene una dirección específica
     *
     * @param addr Dirección a verificar
     * @return true si addr está en el rango [address, address+length)
     */
    public boolean containsAddress(int addr) {
        return (addr >= address) && (addr < getEndAddress());
    }

    /**
     * Verifica si este registro se superpone con un rango de direcciones
     *
     * @param lowerBound Límite inferior del rango (inclusivo)
     * @param upperBound Límite superior del rango (exclusivo)
     * @return true si hay superposición
     */
    public boolean overlapsRange(int lowerBound, int upperBound) {
        return (address < upperBound) && (getEndAddress() > lowerBound);
    }

    /**
     * Crea un nuevo registro con un subset de los datos
     *
     * @param startOffset Offset desde el inicio de data
     * @param length Número de bytes a incluir
     * @return Nuevo HexRecord con datos cortados
     */
    public HexRecord slice(int startOffset, int length) {
        if (startOffset < 0 || startOffset + length > data.length) {
            throw new IndexOutOfBoundsException(
                    "Slice out of bounds: offset=" + startOffset +
                            ", length=" + length + ", data.length=" + data.length);
        }

        byte[] slicedData = Arrays.copyOfRange(data, startOffset, startOffset + length);
        int newAddress = address + startOffset;
        return new HexRecord(newAddress, slicedData);
    }

    @Override
    public String toString() {
        return String.format("HexRecord[addr=0x%04X, len=%d]", address, data.length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HexRecord)) return false;
        HexRecord other = (HexRecord) obj;
        return address == other.address && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return 31 * address + Arrays.hashCode(data);
    }
}
