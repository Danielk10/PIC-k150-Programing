package com.diamon.datos;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.excepciones.HexProcessingException;
import com.diamon.utilidades.ByteUtils;
import com.diamon.utilidades.HexFileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Procesador de datos específicos para chips PIC con logging integrado.
 *
 * <p>
 * Esta clase procesa archivos HEX y extrae datos específicos para chips PIC,
 * incluyendo ROM,
 * EEPROM, ID y configuración de fuses. Incluye validación robusta y logging
 * detallado de todas las
 * operaciones.
 *
 * <p>
 * Datos procesados:
 *
 * <ul>
 * <li>Memoria ROM (código del programa)
 * <li>Memoria EEPROM (datos persistentes)
 * <li>ID del chip y configuración
 * <li>Valores de fuses para configuración
 * <li>Detección automática de endianness
 * </ul>
 *
 * @author Danielk10
 * @version 2.0 - Integrado con sistema de logging y excepciones mejoradas
 * @since 2025
 */
public class DatosPicProcesados {

    /** Contexto para localización */
    private final android.content.Context context;

    /** Firmware HEX a procesar */
    private final String firware;

    /** Información del chip PIC objetivo */
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

    /** Indican si el HEX fuente trae registros de cada región (aunque sean blank). */
    private boolean romPresenteEnHex;
    private boolean eepromPresenteEnHex;
    private boolean configPresenteEnHex;

    /**
     * Constructor para procesamiento de datos PIC.
     *
     * @param firware Contenido del archivo HEX
     * @param chipPIC Información del chip PIC objetivo
     */
    public DatosPicProcesados(android.content.Context context, String firware, ChipPic chipPIC) throws ChipConfigurationException {
        if (context == null) {
            throw new IllegalArgumentException("Context no puede ser null");
        }
        if (firware == null || firware.trim().isEmpty()) {
            throw new IllegalArgumentException("Firmware no puede ser null o vacío");
        }

        if (chipPIC == null) {
            throw new IllegalArgumentException("ChipPIC no puede ser null");
        }

        this.context = context;
        this.firware = firware;
        this.chipPIC = chipPIC;
    }

    /**
     * Inicia el procesamiento completo de datos HEX para el chip PIC.
     *
     * <p>
     * Este método procesa el firmware HEX y extrae todos los datos necesarios para
     * programar el
     * chip, incluyendo ROM, EEPROM, ID y fuses. Incluye detección automática de
     * endianness y
     * validación completa.
     *
     * @throws HexProcessingException     Si ocurre error durante el procesamiento
     * @throws ChipConfigurationException Si hay incompatibilidad con el chip
     */
    public void iniciarProcesamientoDeDatos()
            throws HexProcessingException, ChipConfigurationException {

        try {
            // Definir rangos de memoria según arquitectura del PIC
            final int coreBits = chipPIC.getTipoDeNucleoBit();
            final int romWordBase = 0x0000;
            final int romWordEnd = chipPIC.getTamanoROM() * 2;

            final int eepromWordBase = (coreBits == 16) ? 0xF000 : 0x4200;
            final int eepromWordEnd = 0xFFFF;

            final int idWordBase;
            final int idWordEnd;
            final int fuseWordBase;
            final int fuseWordEnd;

            if (coreBits == 16) {
                idWordBase = 0x200000;
                idWordEnd = 0x200010;
                fuseWordBase = 0x300000;
                fuseWordEnd = 0x30000E;
            } else if (coreBits == 12) {
                // Alineado con referencia picpro: para 12-bit el ID se toma desde
                // el inicio del bloque config (justo después de ROM).
                idWordBase = romWordEnd;
                idWordEnd = Math.min(romWordEnd + 8, 0x2000);
                // El área de fuse de programación sigue usándose en 0x400E para el flujo K150.
                fuseWordBase = 0x400E;
                fuseWordEnd = 0x4010;
            } else {
                idWordBase = 0x4000;
                idWordEnd = 0x4008;
                fuseWordBase = 0x400E;
                fuseWordEnd = 0x4010;
            }

            // Procesar archivo HEX
            HexProcesado procesado;
            try {
                procesado = new HexProcesado(context, firware);
            } catch (HexProcessingException e) {
                throw e;
            }

            // Convertir registros HEX a formato interno
            List<HexFileUtils.Pair<Integer, String>> records = convertirRegistrosHex(procesado);

            // Filtrar registros por rangos de memoria
            List<HexFileUtils.Pair<Integer, String>> romRecords = HexFileUtils.rangeFilterRecords(records, romWordBase,
                    romWordEnd);
            List<HexFileUtils.Pair<Integer, String>> eepromRecords = HexFileUtils.rangeFilterRecords(records,
                    eepromWordBase, eepromWordEnd);
            List<HexFileUtils.Pair<Integer, String>> idRecords = HexFileUtils.rangeFilterRecords(records,
                    idWordBase, idWordEnd);
            List<HexFileUtils.Pair<Integer, String>> fuseRecords = HexFileUtils.rangeFilterRecords(records,
                    fuseWordBase, fuseWordEnd);

            this.romPresenteEnHex = !romRecords.isEmpty();
            this.eepromPresenteEnHex = !eepromRecords.isEmpty();
            this.configPresenteEnHex = !idRecords.isEmpty() || !fuseRecords.isEmpty();

            // Generar datos en blanco para cada tipo de memoria
            byte[] romBlank = HexFileUtils.generateRomBlank(
                    chipPIC.getTipoDeNucleoBit(), chipPIC.getTamanoROM());
            byte[] eepromBlank = HexFileUtils.generateEepromBlank(chipPIC.getTamanoEEPROM());

            // Detectar endianness de los datos ROM con manejo robusto.
            // Alineado con referencia picpro:
            // - core 16 bits: little-endian por defecto (swap=true)
            // - core 12/14 bits: big-endian por defecto (swap=false) si no se puede detectar.
            boolean defaultSwap = (coreBits == 16);
            boolean swapBytes;
            try {
                swapBytes = detectarEndianness(romRecords, romBlank, defaultSwap);
            } catch (IllegalArgumentException e) {
                swapBytes = defaultSwap; // Valor por defecto si hay corrupción
            }

            // Ajustar registros según endianness detectado
            if (swapBytes) {
                romRecords = HexFileUtils.swabRecords(romRecords);
                if (coreBits == 16) {
                    eepromRecords = HexFileUtils.swabRecords(eepromRecords);
                    idRecords = HexFileUtils.swabRecords(idRecords);
                    fuseRecords = HexFileUtils.swabRecords(fuseRecords);
                }
            }

            // Procesar EEPROM con byte picking según endianness
            List<HexFileUtils.Pair<Integer, String>> adjustedEepromRecords = procesarRegistrosEEPROM(eepromRecords,
                    eepromWordBase, swapBytes, coreBits);

            // Fusionar todos los datos
            this.romData = fusionarDatos(romRecords, romBlank, romWordBase, "ROM");
            this.eepromData = fusionarDatos(adjustedEepromRecords, eepromBlank, eepromWordBase, "EEPROM");

            // Procesar ID y fuses
            procesarIDyFuses(idRecords, idWordBase, fuseRecords, fuseWordBase, coreBits);

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
            // Convertir a hexadecimal para reutilizar la infraestructura de fusión.
            String datosHex = ByteUtils.bytesToHex(registro.data);
            records.add(new HexFileUtils.Pair<>(registro.address, datosHex));
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
            List<HexFileUtils.Pair<Integer, String>> romRecords, byte[] romBlank, boolean defaultSwap) {
        boolean swapBytes = defaultSwap;
        boolean swapBytesDetected = false;
        int romBlankWord = ByteUtils.bytesToInt(romBlank);

        for (HexFileUtils.Pair<Integer, String> record : romRecords) {
            // Ignorar direcciones impares, equivalente al parser de referencia.
            if (record.first % 2 != 0) {
                continue;
            }

            String data = record.second;
            // Procesar palabras completas de 16 bits (4 caracteres hex).
            for (int x = 0; x < data.length(); x += 4) {
                if ((x + 4) <= data.length()) {
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
                        continue;
                    }
                }
            }

            if (swapBytesDetected) {
                break;
            }
        }

        if (!swapBytesDetected) {
            swapBytes = defaultSwap;
        }

        return swapBytes;
    }

    /**
     * Procesa registros EEPROM aplicando byte picking según endianness.
     *
     * @param eepromRecords  Registros EEPROM originales
     * @param eepromWordBase Dirección base de EEPROM
     * @param swapBytes      true si es little-endian
     * @return Lista de registros EEPROM ajustados
     */
    private List<HexFileUtils.Pair<Integer, String>> procesarRegistrosEEPROM(
            List<HexFileUtils.Pair<Integer, String>> eepromRecords,
            int eepromWordBase,
            boolean swapBytes,
            int coreBits) {

        if (coreBits == 16) {
            return eepromRecords;
        }

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
     * @param records     Lista de registros a fusionar
     * @param blankData   Datos en blanco como base
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
            byte[] resultado = HexFileUtils.mergeRecords(context, records, blankData, baseAddress);

            return resultado;
        } catch (Exception e) {

            throw new HexProcessingException("Error fusionando memoria " + tipoMemoria, e);
        }
    }

    /**
     * Procesa datos de ID y fuses del chip.
     *
     * @param configRecords  Registros de configuración
     * @param configWordBase Dirección base de configuración
     */
    private void procesarIDyFuses(
            List<HexFileUtils.Pair<Integer, String>> idRecords,
            int idWordBase,
            List<HexFileUtils.Pair<Integer, String>> fuseRecords,
            int fuseWordBase,
            int coreBits)
            throws HexProcessingException {

        try {
            byte[] IDBlanco = HexFileUtils.generarArrayDeDatos((byte) 0x00, 8);
            this.IDData = HexFileUtils.mergeRecords(context, idRecords, IDBlanco, idWordBase);

            // Ajustar ID para chips de 14 bits
            if (coreBits != 16) {
                // En 12/14-bit, User ID útil está en el byte alto de cada palabra de 16 bits
                // (índices impares), alineado con referencia picpro (bytes 1,3,5,7).
                byte[] IDTemporal = new byte[IDData.length / 2];
                for (int i = 0, j = 1; i < IDTemporal.length && j < IDData.length; i++, j += 2) {
                    IDTemporal[i] = IDData[j];
                }
                this.IDData = IDTemporal;
            }

            byte[] fusesBytes = HexFileUtils.encodeToBytes(chipPIC.getFuseBlank());
            this.fuseData = HexFileUtils.mergeRecords(context, fuseRecords, fusesBytes, fuseWordBase);
            this.fuseValues = HexFileUtils.decodeFromBytes(context, fuseData);

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
            resumen.append("ROM: ").append(romData.length).append(" bytes, ");
        }

        if (eepromData != null) {
            resumen.append("EEPROM: ").append(eepromData.length).append(" bytes, ");
        }

        if (IDData != null) {
            resumen.append("ID: ").append(IDData.length).append(" bytes, ");
        }

        if (fuseValues != null) {
            resumen.append("Fuses: ").append(fuseValues.length).append(" valores");
        }

        return resumen.toString();
    }

    /**
     * Verifica si la PROM procesada contiene datos no en blanco.
     * 
     * @return true si tiene código a programar, false si está vacía.
     */
    public boolean tieneRomData() {
        if (romData == null || chipPIC == null)
            return false;
        try {
            byte[] romBlank = HexFileUtils.generateRomBlank(chipPIC.getTipoDeNucleoBit(), chipPIC.getTamanoROM());
            return !Arrays.equals(romData, romBlank);
        } catch (Exception e) {
            return false;
        }
    }

    /** Retorna true si el archivo HEX contiene registros en región ROM. */
    public boolean tieneRomEnHex() {
        return romPresenteEnHex;
    }

    /**
     * Verifica si la EEPROM procesada contiene datos no en blanco.
     * 
     * @return true si tiene datos, false si está vacía.
     */
    public boolean tieneEepromData() {
        if (eepromData == null || chipPIC == null || !chipPIC.isTamanoValidoDeEEPROM())
            return false;
        try {
            byte[] eepromBlank = HexFileUtils.generateEepromBlank(chipPIC.getTamanoEEPROM());
            return !Arrays.equals(eepromData, eepromBlank);
        } catch (Exception e) {
            return false;
        }
    }

    /** Retorna true si el archivo HEX contiene registros en región EEPROM. */
    public boolean tieneEepromEnHex() {
        return eepromPresenteEnHex;
    }

    /**
     * Verifica si la configuración (Fuses o ID) procesada no es igual a su valor en
     * blanco.
     * 
     * @return true si tiene Fuses/ID reales.
     */
    public boolean tieneConfigData() {
        if (fuseValues == null || IDData == null || chipPIC == null)
            return false;

        // Verificar ID (esperamos no todos 0s si hubo data)
        boolean hasID = false;
        for (byte b : IDData) {
            if (b != 0) {
                hasID = true;
                break;
            }
        }

        // Verificar Fuses
        boolean hasFuses = false;
        try {
            int[] blankFuses = chipPIC.getFuseBlank();
            if (fuseValues.length == blankFuses.length) {
                for (int i = 0; i < fuseValues.length; i++) {
                    if (fuseValues[i] != blankFuses[i]) {
                        hasFuses = true;
                        break;
                    }
                }
            }
        } catch (com.diamon.excepciones.ChipConfigurationException e) {
            hasFuses = true; // Si hay error, asumimos que tiene fuses para evitar bloquear al usuario
        }

        return hasID || hasFuses;
    }

    /** Retorna true si el archivo HEX contiene registros de ID/Fuses. */
    public boolean tieneConfigEnHex() {
        return configPresenteEnHex;
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
