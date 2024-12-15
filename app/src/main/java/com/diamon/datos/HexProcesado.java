package com.diamon.datos;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexProcesado {

    // Excepciones personalizadas
    public class InvalidRecordException extends Exception {
        public InvalidRecordException(String message) {
            super(message);
        }
    }

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

    private final List<HexRecord> records = new ArrayList<>();

    public HexProcesado(String fileContent)
            throws InvalidRecordException, InvalidChecksumException {

        String line;
        boolean eof = false;
        int extendedAddress = 0;

        String lineas[] = fileContent.split("\n");

        // Expresiones regulares para validar y dividir registros HEX
        Pattern hexRecordPattern = Pattern.compile("^:[0-9A-Fa-f]+$");
        Pattern hexRecordChopper =
                Pattern.compile(
                        "^:([0-9A-Fa-f]{2})([0-9A-Fa-f]{4})([0-9A-Fa-f]{2})([0-9A-Fa-f]*)([0-9A-Fa-f]{2})$");

        for (int j = 0; j < lineas.length; j++) {

            line = lineas[j];

            if (line.trim().isEmpty()) {
                continue;
            }

            Matcher matcher = hexRecordPattern.matcher(line);
            if (!matcher.matches()) {
                throw new InvalidRecordException("Registro no válido: " + line);
            }

            if (eof) {
                throw new InvalidRecordException("Registro extra después del EOF.");
            }

            Matcher chop = hexRecordChopper.matcher(line);
            if (chop.matches()) {
                int length = Integer.parseInt(chop.group(1), 16);
                int address = Integer.parseInt(chop.group(2), 16);
                int type = Integer.parseInt(chop.group(3), 16);
                String dataStr = chop.group(4);
                int checksum = Integer.parseInt(chop.group(5), 16);

                byte[] data = hexStringToByteArray(dataStr);
                if (length != data.length) {
                    throw new InvalidRecordException(
                            "Longitud incorrecta: " + length + " != " + data.length);
                }

                // Verificar checksum
                int checksumTest = 0;
                for (int i = 1; i < line.length() - 2; i += 2) {
                    checksumTest =
                            (checksumTest + Integer.parseInt(line.substring(i, i + 2), 16)) % 256;
                }
                checksumTest = (256 - checksumTest) % 256;
                if (checksumTest != checksum) {
                    throw new InvalidChecksumException(
                            "Checksum incorrecto: " + checksumTest + " != " + checksum);
                }

                // Procesar registros según su tipo
                if (type == 0) {
                    records.add(new HexRecord(address | extendedAddress, data));
                } else if (type == 1) {
                    eof = true;
                } else if (type == 2) {
                    extendedAddress = (data[0] << 8 | data[1]) << 16;
                } else if (type == 4) {
                    extendedAddress = (data[0] << 8 | data[1]) << 4;
                } else {
                    throw new InvalidRecordException("Tipo de registro desconocido: " + type);
                }
            } else {
                throw new InvalidRecordException("Registro malformado: " + line);
            }
        }
    }

    public List<HexRecord> getRecords() {
        return records;
    }

    public byte[] merge(byte[] dataBuffer) throws IndexOutOfBoundsException {
        for (HexRecord record : records) {
            int address = record.address;
            byte[] data = record.data;

            if ((address + data.length) > dataBuffer.length) {
                throw new IndexOutOfBoundsException("Registro fuera de rango.");
            }

            System.arraycopy(data, 0, dataBuffer, address, data.length);
        }
        return dataBuffer;
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte)
                            ((Character.digit(s.charAt(i), 16) << 4)
                                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
