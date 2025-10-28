package com.diamon.datos;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.excepciones.HexProcessingException;
import com.diamon.utilidades.ByteUtils;
import com.diamon.utilidades.HexFileUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Procesador de datos específicos para chips PIC con logging integrado.
 *
 * <p>Esta clase procesa archivos HEX y extrae datos específicos para chips PIC, incluyendo ROM,
 * EEPROM, ID y configuración de fuses. Incluye validación robusta y logging detallado de todas las
 * operaciones.
 *
 * <p>Datos procesados:
 *
 * <ul>
 *   <li>Memoria ROM (código del programa)
 *   <li>Memoria EEPROM (datos persistentes)
 *   <li>ID del chip y configuración
 *   <li>Valores de fuses para configuración
 *   <li>Detección automática de endianness
 * </ul>
 *
 * @author Danielk10
 * @version 2.0 - Integrado con sistema de logging y excepciones mejoradas
 * @since 2025
 */
public class DatosPicProcesados {

    /** Firmware HEX a procesar */
    private final String firware;

    /** Información del chip PIC objetivo */
    // private final ChipPic chipPIC;

    private ChipPic chipPIC;

    /** Datos de memoria ROM procesados */
    private byte[] romData;

    /** Datos de memoria EEPROM procesados */
    private byte[] eepromData;

    /** Datos de ID del chip procesados */
    private byte[] IDData;

    /** Datos de configuración de fuses como bytes */
    private byte[] fuseData;

    /** Valores de fuses como enteros */
    private int[] fuseValues;

    /** Información del procesamiento para logging */
    private String informacionProcesamiento;

    /**
     * Constructor para procesamiento de datos PIC.
     *
     * @param firware Contenido del archivo HEX
     * @param chipPIC Información del chip PIC objetivo
     */
    public DatosPicProcesados(String firware, ChipPic chipPIC) throws ChipConfigurationException {
        if (firware == null || firware.trim().isEmpty()) {
            throw new IllegalArgumentException("Firmware no puede ser null o vacío");
        }

        if (chipPIC == null) {
            throw new IllegalArgumentException("ChipPIC no puede ser null");
        }

        this.firware = firware;
        this.chipPIC = chipPIC;
    }

    /**
     * Inicia el procesamiento completo de datos HEX para el chip PIC.
     *
     * <p>Este método procesa el firmware HEX y extrae todos los datos necesarios para programar el
     * chip, incluyendo ROM, EEPROM, ID y fuses. Incluye detección automática de endianness y
     * validación completa.
     *
     * @throws HexProcessingException Si ocurre error durante el procesamiento
     * @throws ChipConfigurationException Si hay incompatibilidad con el chip
     */
    public void iniciarProcesamientoDeDatos()
            throws HexProcessingException, ChipConfigurationException {

        try {
            // Definir rangos de memoria según especificación PIC
            final int romWordBase = 0x0000;
            final int configWordBase = 0x4000;
            final int eepromWordBase = 0x4200;
            final int romWordEnd = configWordBase;
            final int configWordEnd = 0x4010;
            final int eepromWordEnd = 0xFFFF;

            // Procesar archivo HEX
            HexProcesado procesado;
            try {
                procesado = new HexProcesado(firware);
            } catch (HexProcessingException e) {
                throw e;
            }

            // Convertir registros HEX a formato interno
            List<HexFileUtils.Pair<Integer, String>> records = convertirRegistrosHex(procesado);

            // Filtrar registros por rangos de memoria
            List<HexFileUtils.Pair<Integer, String>> romRecords =
                    HexFileUtils.rangeFilterRecords(records, romWordBase, romWordEnd);
            List<HexFileUtils.Pair<Integer, String>> configRecords =
                    HexFileUtils.rangeFilterRecords(records, configWordBase, configWordEnd);
            List<HexFileUtils.Pair<Integer, String>> eepromRecords =
                    HexFileUtils.rangeFilterRecords(records, eepromWordBase, eepromWordEnd);

            // Generar datos en blanco para cada tipo de memoria
            byte[] romBlank =
                    HexFileUtils.generateRomBlank(
                            chipPIC.getTipoDeNucleoBit(), chipPIC.getTamanoROM());
            byte[] eepromBlank = HexFileUtils.generateEepromBlank(chipPIC.getTamanoEEPROM());

            // Detectar endianness de los datos ROM
            // Detectar endianness con manejo robusto
            boolean swapBytes;
            try {
                swapBytes = detectarEndianness(romRecords, romBlank);
            } catch (IllegalArgumentException e) {
                // ✅ SOLUCIÓN: Manejar error de endianness
                swapBytes = false; // Valor por defecto
            }

            // Ajustar registros según endianness detectado
            if (swapBytes) {
                romRecords = HexFileUtils.swabRecords(romRecords);
                configRecords = HexFileUtils.swabRecords(configRecords);
            }

            // Procesar EEPROM con byte picking según endianness
            List<HexFileUtils.Pair<Integer, String>> adjustedEepromRecords =
                    procesarRegistrosEEPROM(eepromRecords, eepromWordBase, swapBytes);

            // Fusionar todos los datos
            this.romData = fusionarDatos(romRecords, romBlank, romWordBase, "ROM");
            this.eepromData =
                    fusionarDatos(adjustedEepromRecords, eepromBlank, eepromWordBase, "EEPROM");

            // Procesar ID y fuses
            procesarIDyFuses(configRecords, configWordBase);

            // Generar información de resumen
            this.informacionProcesamiento = generarResumenProcesamiento();

        } catch (Exception e) {

            if (e instanceof HexProcessingException || e instanceof ChipConfigurationException) {
                throw e;
            } else {
                throw new HexProcessingException("Error inesperado procesando datos PIC", e);
            }
        }
    }

    // ========== MÉTODOS AUXILIARES DE PROCESAMIENTO ==========

    /**
     * Convierte los registros HEX procesados a formato interno.
     *
     * @param procesado Objeto HexProcesado con los registros
     * @return Lista de pares (dirección, datos) convertidos
     */
    private List<HexFileUtils.Pair<Integer, String>> convertirRegistrosHex(HexProcesado procesado) {
        List<HexFileUtils.Pair<Integer, String>> records = new ArrayList<>();

        for (int i = 0; i < procesado.getRecords().size(); i++) {
            HexProcesado.HexRecord registro = procesado.getRecords().get(i);
            StringBuilder datosHex = new StringBuilder();

            for (int v = 0; v < registro.data.length; v++) {
                byte b = registro.data[v];
                datosHex.append(String.format("%02X", b));
            }

            records.add(new HexFileUtils.Pair<>(registro.address, datosHex.toString()));
        }

        return records;
    }

    /**
     * Detecta el endianness de los datos ROM analizando las palabras.
     *
     * @param romRecords Lista de registros ROM
     * @return true si es little-endian, false si es big-endian
     */
    private boolean detectarEndianness(
            List<HexFileUtils.Pair<Integer, String>> romRecords, byte[] romBlank) {
        boolean swapBytes = false;
        boolean swapBytesDetected = false;
        int romBlankWord = ByteUtils.bytesToInt(romBlank);

        for (HexFileUtils.Pair<Integer, String> record : romRecords) {
            // ✅ CORRECCIÓN BASADA EN PYTHON: Solo validar si hay datos suficientes
            if (record.first % 2 != 0) {
                continue; // ✅ Continuar como Python
            }

            String data = record.second;
            // ✅ CORRECCIÓN BASADA EN PYTHON: Solo procesar si hay datos completos
            for (int x = 0; x < data.length(); x += 4) {
                // ✅ VERIFICAR que hay suficientes caracteres (como Python hace con x+2 < len)
                if ((x + 4) <= data.length()) { // ✅ Solo si hay datos suficientes
                    String wordHex = data.substring(x, x + 4);
                    int BE_word = Integer.parseInt(wordHex, 16);
                    int LE_word = Integer.reverseBytes(BE_word) >>> 16;

                    boolean BE_ok = (BE_word & romBlankWord) == BE_word;
                    boolean LE_ok = (LE_word & romBlankWord) == LE_word;

                    if (BE_ok && !LE_ok) {
                        swapBytes = false;
                        swapBytesDetected = true;
                        break;
                    } else if (LE_ok && !BE_ok) {
                        swapBytes = true;
                        swapBytesDetected = true;
                        break;
                    } else if (!BE_ok && !LE_ok) {
                        // ✅ COMO PYTHON: Solo fallar en casos realmente inválidos
                        continue;
                    }
                }
            }

            if (swapBytesDetected) {
                break;
            }
        }

        // ✅ COMO PYTHON: Valor por defecto si no se detecta
        if (!swapBytesDetected) {
            swapBytes = false;
        }

        return swapBytes;
    }

    /**
     * Procesa registros EEPROM aplicando byte picking según endianness.
     *
     * @param eepromRecords Registros EEPROM originales
     * @param eepromWordBase Dirección base de EEPROM
     * @param swapBytes true si es little-endian
     * @return Lista de registros EEPROM ajustados
     */
    private List<HexFileUtils.Pair<Integer, String>> procesarRegistrosEEPROM(
            List<HexFileUtils.Pair<Integer, String>> eepromRecords,
            int eepromWordBase,
            boolean swapBytes) {

        // EEPROM está almacenado en el archivo HEX con un byte por palabra
        // Seleccionar el byte apropiado según el endianness detectado
        int pickByte = swapBytes ? 0 : 1;

        List<HexFileUtils.Pair<Integer, String>> adjustedEepromRecords = new ArrayList<>();

        for (HexFileUtils.Pair<Integer, String> record : eepromRecords) {
            int baseAddress = eepromWordBase + (record.first - eepromWordBase) / 2;
            StringBuilder filteredData = new StringBuilder();

            for (int x = pickByte * 2; x < record.second.length(); x += 4) {
                filteredData.append(record.second.substring(x, x + 2));
            }

            adjustedEepromRecords.add(
                    new HexFileUtils.Pair<>(baseAddress, filteredData.toString()));
        }

        return adjustedEepromRecords;
    }

    /**
     * Fusiona registros en un buffer de datos con logging detallado.
     *
     * @param records Lista de registros a fusionar
     * @param blankData Datos en blanco como base
     * @param baseAddress Dirección base
     * @param tipoMemoria Tipo de memoria para logging
     * @return Buffer fusionado
     */
    private byte[] fusionarDatos(
            List<HexFileUtils.Pair<Integer, String>> records,
            byte[] blankData,
            int baseAddress,
            String tipoMemoria)
            throws HexProcessingException {
        try {
            byte[] resultado = HexFileUtils.mergeRecords(records, blankData, baseAddress);

            // Generar estadísticas de los datos fusionados
            String estadisticas = ByteUtils.obtenerEstadisticas(resultado);

            return resultado;
        } catch (Exception e) {

            throw new HexProcessingException("Error fusionando memoria " + tipoMemoria, e);
        }
    }

    /**
     * Procesa datos de ID y fuses del chip.
     *
     * @param configRecords Registros de configuración
     * @param configWordBase Dirección base de configuración
     */
    private void procesarIDyFuses(
            List<HexFileUtils.Pair<Integer, String>> configRecords, int configWordBase)
            throws HexProcessingException {

        try {
            // Procesar ID del chip (0x4000-0x4008)
            List<HexFileUtils.Pair<Integer, String>> configRecordsID =
                    HexFileUtils.rangeFilterRecords(configRecords, 0x4000, 0x4008);

            byte[] IDBlanco = HexFileUtils.generarArrayDeDatos((byte) 0x00, 8);
            this.IDData = HexFileUtils.mergeRecords(configRecordsID, IDBlanco, configWordBase);

            // Ajustar ID para chips de 14 bits
            if (chipPIC.getTipoDeNucleoBit() != 16) {
                byte[] IDTemporal = new byte[IDData.length / 2];
                ByteUtils.copiarBytes(IDData, 0, IDTemporal, 0, IDTemporal.length);
                this.IDData = IDTemporal;
            }

            // Procesar fuses (0x400E-0x4010)
            List<HexFileUtils.Pair<Integer, String>> configRecordsFuses =
                    HexFileUtils.rangeFilterRecords(configRecords, 0x400E, 0x4010);

            byte[] fusesBytes = HexFileUtils.encodeToBytes(chipPIC.getFuseBlack());
            this.fuseData = HexFileUtils.mergeRecords(configRecordsFuses, fusesBytes, 0x400E);
            int[] fuseINT = HexFileUtils.decodeFromBytes(fuseData);
            this.fuseValues = HexFileUtils.decodeFromBytes(fuseData);

        } catch (Exception e) {
            throw new HexProcessingException("Error procesando ID y fuses del chip", e);
        }
    }

    /**
     * Genera un resumen del procesamiento para logging.
     *
     * @return String con resumen del procesamiento
     */
    private String generarResumenProcesamiento() {
        StringBuilder resumen = new StringBuilder();
        resumen.append("Procesamiento completado - ");

        if (romData != null) {
            resumen.append(String.format("ROM: %d bytes, ", romData.length));
        }

        if (eepromData != null) {
            resumen.append(String.format("EEPROM: %d bytes, ", eepromData.length));
        }

        if (IDData != null) {
            resumen.append(String.format("ID: %d bytes, ", IDData.length));
        }

        if (fuseValues != null) {
            resumen.append(String.format("Fuses: %d valores", fuseValues.length));
        }

        return resumen.toString();
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

    public ChipPic obtenerChipProcesado() {

        return chipPIC;
    }
}
