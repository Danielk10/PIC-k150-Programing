package com.diamon.datos;

import android.widget.Toast;
import com.diamon.chip.ChipPic;
import com.diamon.excepciones.HexProcessingException;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.utilidades.ByteUtils;
import com.diamon.utilidades.HexFileUtils;
import com.diamon.utilidades.LogManager;
import com.diamon.utilidades.LogManager.Categoria;

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
    private final ChipPic chipPIC;

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

        LogManager.i(
                Categoria.DATA,
                "DatosPicProcesados",
                String.format(
                        "Inicializado procesador para chip ROM:%d, EEPROM:%d",
                        chipPIC.getTamanoROM(), chipPIC.getTamanoEEPROM()));
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
        long inicioOperacion = LogManager.logInicioOperacion(Categoria.DATA, "procesarDatosPIC");

        try {
            LogManager.i(
                    Categoria.DATA,
                    "procesarDatos",
                    String.format(
                            "Iniciando procesamiento de datos para chip ROM:%d words, EEPROM:%d bytes",
                            chipPIC.getTamanoROM(), chipPIC.getTamanoEEPROM()));

            // Definir rangos de memoria según especificación PIC
            final int romWordBase = 0x0000;
            final int configWordBase = 0x4000;
            final int eepromWordBase = 0x4200;
            final int romWordEnd = configWordBase;
            final int configWordEnd = 0x4010;
            final int eepromWordEnd = 0xFFFF;

            LogManager.d(
                    Categoria.DATA,
                    "procesarDatos",
                    String.format(
                            "Rangos de memoria - ROM: 0x%04X-0x%04X, CONFIG: 0x%04X-0x%04X, EEPROM: 0x%04X-0x%04X",
                            romWordBase,
                            romWordEnd - 1,
                            configWordBase,
                            configWordEnd - 1,
                            eepromWordBase,
                            eepromWordEnd));

            // Procesar archivo HEX
            HexProcesado procesado;
            try {
                procesado = new HexProcesado(firware);
            } catch (HexProcessingException e) {
                LogManager.e(Categoria.DATA, "procesarDatos", "Error procesando archivo HEX", e);
                LogManager.logFinOperacion(
                        Categoria.DATA, "procesarDatosPIC", inicioOperacion, false);
                throw e;
            }

            // Convertir registros HEX a formato interno
            List<HexFileUtils.Pair<Integer, String>> records = convertirRegistrosHex(procesado);

            LogManager.d(
                    Categoria.DATA,
                    "procesarDatos",
                    String.format("Convertidos %d registros HEX", records.size()));

            // Filtrar registros por rangos de memoria
            List<HexFileUtils.Pair<Integer, String>> romRecords =
                    HexFileUtils.rangeFilterRecords(records, romWordBase, romWordEnd);
            List<HexFileUtils.Pair<Integer, String>> configRecords =
                    HexFileUtils.rangeFilterRecords(records, configWordBase, configWordEnd);
            List<HexFileUtils.Pair<Integer, String>> eepromRecords =
                    HexFileUtils.rangeFilterRecords(records, eepromWordBase, eepromWordEnd);

            LogManager.d(
                    Categoria.DATA,
                    "procesarDatos",
                    String.format(
                            "Registros filtrados - ROM: %d, CONFIG: %d, EEPROM: %d",
                            romRecords.size(), configRecords.size(), eepromRecords.size()));

            // Generar datos en blanco para cada tipo de memoria
            byte[] romBlank =
                    HexFileUtils.generateRomBlank(
                            chipPIC.getTipoDeNucleoBit(), chipPIC.getTamanoROM());
            byte[] eepromBlank = HexFileUtils.generateEepromBlank(chipPIC.getTamanoEEPROM());

            LogManager.v(
                    Categoria.DATA,
                    "procesarDatos",
                    String.format(
                            "Datos en blanco generados - ROM: %d bytes, EEPROM: %d bytes",
                            romBlank.length, eepromBlank.length));

            // Detectar endianness de los datos ROM
            boolean swapBytes = detectarEndianness(romRecords, romBlank);
            LogManager.d(
                    Categoria.DATA,
                    "procesarDatos",
                    String.format(
                            "Endianness detectado: %s",
                            swapBytes ? "Little-Endian" : "Big-Endian"));

            // Ajustar registros según endianness detectado
            if (swapBytes) {
                LogManager.v(
                        Categoria.DATA,
                        "procesarDatos",
                        "Aplicando intercambio de bytes (swab) a registros");
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

            LogManager.i(Categoria.DATA, "procesarDatos", informacionProcesamiento);
            LogManager.logFinOperacion(Categoria.DATA, "procesarDatosPIC", inicioOperacion, true);

        } catch (Exception e) {
            LogManager.e(
                    Categoria.DATA, "procesarDatos", "Error durante procesamiento de datos", e);
            LogManager.logFinOperacion(Categoria.DATA, "procesarDatosPIC", inicioOperacion, false);

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

        LogManager.v(
                Categoria.DATA,
                "convertirRegistros",
                String.format("Convirtiendo %d registros HEX", procesado.getRecords().size()));

        for (int i = 0; i < procesado.getRecords().size(); i++) {
            HexProcesado.HexRecord registro = procesado.getRecords().get(i);
            StringBuilder datosHex = new StringBuilder();

            for (int v = 0; v < registro.data.length; v++) {
                byte b = registro.data[v];
                datosHex.append(String.format("%02X", b));
            }

            records.add(new HexFileUtils.Pair<>(registro.address, datosHex.toString()));

            LogManager.v(
                    Categoria.DATA,
                    "convertirRegistros",
                    String.format(
                            "Registro %d: addr=0x%06X, %d bytes",
                            i + 1, registro.address, registro.data.length));
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
    LogManager.v(Categoria.DATA, "detectarEndianness", "Iniciando detección de endianness");
    boolean swapBytes = false;
    boolean swapBytesDetected = false;
    int romBlankWord = ByteUtils.bytesToInt(romBlank);
    
    for (HexFileUtils.Pair<Integer, String> record : romRecords) {
        // ✅ CORRECCIÓN BASADA EN PYTHON: Solo validar si hay datos suficientes
        if (record.first % 2 != 0) {
            LogManager.w(Categoria.DATA, "detectarEndianness", 
                String.format("⚠️ ADVERTENCIA: Registro ROM en dirección impar: 0x%06X", record.first));
            continue; // ✅ Continuar como Python
        }
        
        String data = record.second;
        // ✅ CORRECCIÓN BASADA EN PYTHON: Solo procesar si hay datos completos
        for (int x = 0; x < data.length(); x += 4) {
            // ✅ VERIFICAR que hay suficientes caracteres (como Python hace con x+2 < len)
            if ((x + 4) <= data.length()) {  // ✅ Solo si hay datos suficientes
                String wordHex = data.substring(x, x + 4);
                int BE_word = Integer.parseInt(wordHex, 16);
                int LE_word = Integer.reverseBytes(BE_word) >>> 16;
                
                boolean BE_ok = (BE_word & romBlankWord) == BE_word;
                boolean LE_ok = (LE_word & romBlankWord) == LE_word;
                
                if (BE_ok && !LE_ok) {
                    swapBytes = false;
                    swapBytesDetected = true;
                    LogManager.v(Categoria.DATA, "detectarEndianness", "Detectado Big-Endian");
                    break;
                } else if (LE_ok && !BE_ok) {
                    swapBytes = true;
                    swapBytesDetected = true;
                    LogManager.v(Categoria.DATA, "detectarEndianness", "Detectado Little-Endian");
                    break;
                } else if (!BE_ok && !LE_ok) {
                    // ✅ COMO PYTHON: Solo fallar en casos realmente inválidos
                    LogManager.w(Categoria.DATA, "detectarEndianness", 
                        String.format("⚠️ ADVERTENCIA: Palabra ROM no válida: %s", wordHex));
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
        LogManager.i(Categoria.DATA, "detectarEndianness", 
            "No se pudo detectar endianness, usando Big-Endian por defecto");
        swapBytes = false;
    }
    
    LogManager.d(Categoria.DATA, "detectarEndianness", 
        String.format("Endianness detectado: %s", swapBytes ? "Little-Endian" : "Big-Endian"));
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

        LogManager.v(
                Categoria.DATA,
                "procesarEEPROM",
                String.format(
                        "Procesando %d registros EEPROM con byte picking", eepromRecords.size()));

        // EEPROM está almacenado en el archivo HEX con un byte por palabra
        // Seleccionar el byte apropiado según el endianness detectado
        int pickByte = swapBytes ? 0 : 1;

        LogManager.v(
                Categoria.DATA,
                "procesarEEPROM",
                String.format("Usando byte %d para extracción EEPROM", pickByte));

        List<HexFileUtils.Pair<Integer, String>> adjustedEepromRecords = new ArrayList<>();

        for (HexFileUtils.Pair<Integer, String> record : eepromRecords) {
            int baseAddress = eepromWordBase + (record.first - eepromWordBase) / 2;
            StringBuilder filteredData = new StringBuilder();

            for (int x = pickByte * 2; x < record.second.length(); x += 4) {
                filteredData.append(record.second.substring(x, x + 2));
            }

            adjustedEepromRecords.add(
                    new HexFileUtils.Pair<>(baseAddress, filteredData.toString()));

            LogManager.v(
                    Categoria.DATA,
                    "procesarEEPROM",
                    String.format(
                            "Registro EEPROM ajustado: addr=0x%06X, datos=%s",
                            baseAddress, filteredData.toString()));
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
        LogManager.v(
                Categoria.DATA,
                "fusionarDatos",
                String.format("Fusionando %d registros de %s", records.size(), tipoMemoria));

        try {
            byte[] resultado = HexFileUtils.mergeRecords(records, blankData, baseAddress);

            LogManager.d(
                    Categoria.DATA,
                    "fusionarDatos",
                    String.format("Memoria %s fusionada: %d bytes", tipoMemoria, resultado.length));

            // Generar estadísticas de los datos fusionados
            String estadisticas = ByteUtils.obtenerEstadisticas(resultado);
            LogManager.v(
                    Categoria.DATA,
                    "fusionarDatos",
                    String.format("Estadísticas %s: %s", tipoMemoria, estadisticas));

            return resultado;
        } catch (Exception e) {
            LogManager.e(
                    Categoria.DATA,
                    "fusionarDatos",
                    String.format("Error fusionando memoria %s", tipoMemoria),
                    e);
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
        LogManager.v(Categoria.DATA, "procesarIDFuses", "Procesando ID y fuses del chip");

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

                LogManager.v(
                        Categoria.DATA,
                        "procesarIDFuses",
                        String.format(
                                "ID ajustado para chip de %d bits: %d bytes",
                                chipPIC.getTipoDeNucleoBit(), IDData.length));
            }

            // Procesar fuses (0x400E-0x4010)
            List<HexFileUtils.Pair<Integer, String>> configRecordsFuses =
                    HexFileUtils.rangeFilterRecords(configRecords, 0x400E, 0x4010);

            byte[] fusesBytes = HexFileUtils.encodeToBytes(chipPIC.getFuseBlack());
            this.fuseData = HexFileUtils.mergeRecords(configRecordsFuses, fusesBytes, 0x400E);
            this.fuseValues = HexFileUtils.decodeFromBytes(fuseData);

            LogManager.d(
                    Categoria.DATA,
                    "procesarIDFuses",
                    String.format(
                            "ID procesado: %d bytes, Fuses: %d valores",
                            IDData.length, fuseValues.length));

        } catch (Exception e) {
            LogManager.e(Categoria.DATA, "procesarIDFuses", "Error procesando ID y fuses", e);
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
}
