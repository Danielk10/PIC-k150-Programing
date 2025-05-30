package com.diamon.datos;

import com.diamon.chip.ChipPic;
import com.diamon.utilidades.HexFileUtils;

import java.util.ArrayList;
import java.util.List;

public class DatosPicProcesados {

    private String firware;

    private ChipPic chipPIC;

    private byte[] romData;

    private byte[] eepromData;

    private byte[] IDData;

    private byte[] fuseData;

    private int[] fuseValues;

    public DatosPicProcesados(String firware, ChipPic chipPIC) {

        this.firware = firware;

        this.chipPIC = chipPIC;
    }

    public void iniciarProcesamientoDeDatos() {

        // Rangos de memoria.
        int romWordBase = 0x0000;

        int configWordBase = 0x4000;

        int eepromWordBase = 0x4200;

        int romWordEnd = configWordBase;

        int configWordEnd = 0x4010;

        int eepromWordEnd = 0xffff;

        List<HexFileUtils.Pair<Integer, String>> records =
                new ArrayList<HexFileUtils.Pair<Integer, String>>();

        try {
            HexProcesado procesado = new HexProcesado(firware);

            for (int i = 0; i < procesado.getRecords().size(); i++) {
                StringBuffer es = new StringBuffer();

                for (int v = 0; v < procesado.getRecords().get(i).data.length; v++) {
                    byte b = procesado.getRecords().get(i).data[v];

                    es.append(String.format("%02X", b));
                }

                records.add(
                        new HexFileUtils.Pair<Integer, String>(
                                procesado.getRecords().get(i).address, es.toString()));
            }

            // Filtrar registros
            List<HexFileUtils.Pair<Integer, String>> romRecords =
                    HexFileUtils.rangeFilterRecords(records, romWordBase, romWordEnd);

            List<HexFileUtils.Pair<Integer, String>> configRecords =
                    HexFileUtils.rangeFilterRecords(records, configWordBase, configWordEnd);

            List<HexFileUtils.Pair<Integer, String>> eepromRecords =
                    HexFileUtils.rangeFilterRecords(records, eepromWordBase, eepromWordEnd);

            // Generar datos en blanco
            byte[] romBlank =
                    HexFileUtils.generateRomBlank(
                            chipPIC.getTipoDeNucleoBit(), chipPIC.getTamanoROM());

            byte[] eepromBlank = HexFileUtils.generateEepromBlank(chipPIC.getTamanoEEPROM());

            // Detectar si los datos ROM son big-endian o little-endian
            boolean swapBytes = false;
            boolean swapBytesDetected = false;
            int romBlankWord = 0xFFFF; // Palabra ROM en blanco

            for (HexFileUtils.Pair<Integer, String> record : romRecords) {
                if (record.first % 2 != 0) {
                    throw new IllegalArgumentException("ROM record starts on odd address.");
                }

                if (record.second.length() % 4 != 0) {
                    throw new IllegalArgumentException(
                            "Data length in record must be a multiple of 4: " + record.second);
                }

                String data = record.second;
                for (int x = 0;
                        x < data.length();
                        x += 4) { // Procesamos en bloques de 2 bytes (4 caracteres hex)
                    String wordHex = data.substring(x, x + 4); // Extraer palabra
                    int BE_word = Integer.parseInt(wordHex, 16); // Interpretar como big-endian
                    int LE_word =
                            Integer.reverseBytes(BE_word) >>> 16; // Interpretar como little-endian

                    boolean BE_ok = (BE_word & romBlankWord) == BE_word;
                    boolean LE_ok = (LE_word & romBlankWord) == LE_word;

                    // No va

                    BE_ok = false;

                    if (BE_ok && !LE_ok) {
                        swapBytes = false;
                        swapBytesDetected = true;
                        break;
                    } else if (LE_ok && !BE_ok) {
                        swapBytes = true;
                        swapBytesDetected = true;
                        break;
                    } else if (!BE_ok && !LE_ok) {
                        throw new IllegalArgumentException(
                                "Invalid ROM word: "
                                        + wordHex
                                        + ", ROM blank word: "
                                        + String.format("%04X", romBlankWord));
                    }
                }
                if (swapBytesDetected) {
                    break;
                }
            }

            // Si es necesario, ajustar los registros (swabRecords)
            if (swapBytes) {
                romRecords = HexFileUtils.swabRecords(romRecords);
                configRecords = HexFileUtils.swabRecords(configRecords);
            }

            // EEPROM está almacenado en el archivo HEX con un byte por palabra.
            // Seleccionamos el byte apropiado según el endianess detectado.
            int pickByte = swapBytes ? 0 : 1;

            List<HexFileUtils.Pair<Integer, String>> adjustedEepromRecords = new ArrayList<>();
            for (HexFileUtils.Pair<Integer, String> record : eepromRecords) {
                int baseAddress = eepromWordBase + (record.first - eepromWordBase) / 2;
                StringBuilder filteredData = new StringBuilder();

                for (int x = pickByte * 2; x < record.second.length(); x += 4) {
                    filteredData.append(
                            record.second.substring(x, x + 2)); // Extraer byte seleccionado
                }

                adjustedEepromRecords.add(
                        new HexFileUtils.Pair<Integer, String>(
                                baseAddress, filteredData.toString()));
            }

            // Crear datos finales fusionando los registros ajustados con los datos en blanco
            byte[] romData = HexFileUtils.mergeRecords(romRecords, romBlank, romWordBase);

            byte[] eepromData =
                    HexFileUtils.mergeRecords(adjustedEepromRecords, eepromBlank, eepromWordBase);

            List<HexFileUtils.Pair<Integer, String>> configRecordsID =
                    HexFileUtils.rangeFilterRecords(configRecords, 0x4000, 0x4008);

            byte[] IDBlanco = HexFileUtils.generarArrayDeDatos((byte) 0x00, 8);

            IDData = HexFileUtils.mergeRecords(configRecordsID, IDBlanco, configWordBase);

            if (chipPIC.getTipoDeNucleoBit() != 16) {

                byte[] IDTemporal = new byte[IDData.length / 2];

                for (int i = 0; i < IDTemporal.length; i++) {

                    IDTemporal[i] = IDData[i];
                }

                IDData = IDTemporal;
            }

            List<HexFileUtils.Pair<Integer, String>> configRecordsFuses =
                    HexFileUtils.rangeFilterRecords(configRecords, 0x400E, 0x4010);

            byte[] fusesBytes = HexFileUtils.encodeToBytes(chipPIC.getFuseBlack());

            this.fuseData = HexFileUtils.mergeRecords(configRecordsFuses, fusesBytes, 0x400E);

            this.fuseValues = HexFileUtils.decodeFromBytes(fuseData);

            this.romData = romData;

            this.eepromData = eepromData;

        } catch (HexProcesado.InvalidRecordException e) {
        } catch (HexProcesado.InvalidChecksumException e) {
        }
    }

    public byte[] obtenerBytesHexROMPocesado() {

        return this.romData;
    }

    public byte[] obtenerBytesHexEEPROMPocesado() {

        return this.eepromData;
    }

    public int[] obtenerValoresIntHexFusesPocesado() {

        return this.fuseValues;
    }

    public byte[] obtenerVsloresBytesHexIDPocesado() {

        return this.IDData;
    }
}
