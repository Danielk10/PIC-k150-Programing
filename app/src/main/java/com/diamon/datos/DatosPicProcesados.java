package com.diamon.datos;

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
 * <p>Esta clase procesa archivos HEX y extrae datos específicos para chips PIC,
 * incluyendo ROM, EEPROM, ID y configuración de fuses. Incluye validación
 * robusta y logging detallado de todas las operaciones.</p>
 *
 * <p>Datos procesados:</p>
 * <ul>
 *   <li>Memoria ROM (código del programa)</li>
 *   <li>Memoria EEPROM (datos persistentes)</li>
 *   <li>ID del chip y configuración</li>
 *   <li>Valores de fuses para configuración</li>
 *   <li>Detección automática de endianness</li>
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
    public DatosPicProcesados(String firware, ChipPic chipPIC) {
        if (firware == null || firware.trim().isEmpty()) {
            throw new IllegalArgumentException("Firmware no puede ser null o vacío");
        }
        
        if (chipPIC == null) {
            throw new IllegalArgumentException("ChipPIC no puede ser null");
        }

        this.firware = firware;
        this.chipPIC = chipPIC;
        
        LogManager.i(Categoria.DATA, "DatosPicProcesados",
                    String.format("Inicializado procesador para chip ROM:%d, EEPROM:%d",
                                chipPIC.getTamanoROM(), chipPIC.getTamanoEEPROM()));
    }

    /**
     * Inicia el procesamiento completo de datos HEX para el chip PIC.
     *
     * <p>Este método procesa el firmware HEX y extrae todos los datos necesarios
     * para programar el chip, incluyendo ROM, EEPROM, ID y fuses. Incluye
     * detección automática de endianness y validación completa.</p>
     *
     * @throws HexProcessingException Si ocurre error durante el procesamiento
     * @throws ChipConfigurationException Si hay incompatibilidad con el chip
     */
    public void iniciarProcesamientoDeDatos() throws HexProcessingException, ChipConfigurationException {
        long inicioOperacion = LogManager.logInicioOperacion(Categoria.DATA, "procesarDatosPIC");
        
        try {
            LogManager.i(Categoria.DATA, "procesarDatos",
                        String.format("Iniciando procesamiento de datos para chip ROM:%d words, EEPROM:%d bytes",
                                    chipPIC.getTamanoROM(), chipPIC.getTamanoEEPROM()));

            // Definir rangos de memoria según especificación PIC
            final int romWordBase = 0x0000;
            final int configWordBase = 0x4000;
            final int eepromWordBase = 0x4200;
            final int romWordEnd = configWordBase;
            final int configWordEnd = 0x4010;
            final int eepromWordEnd = 0xFFFF;

            LogManager.d(Categoria.DATA, "procesarDatos",
                        String.format("Rangos de memoria - ROM: 0x%04X-0x%04X, CONFIG: 0x%04X-0x%04X, EEPROM: 0x%04X-0x%04X",
                                    romWordBase, romWordEnd-1, configWordBase, configWordEnd-1, eepromWordBase, eepromWordEnd));

            // Procesar archivo HEX
            HexProcesado procesado;
            try {
                procesado = new HexProcesado(firware);
            } catch (HexProcessingException e) {
                LogManager.e(Categoria.DATA, "procesarDatos", "Error procesando archivo HEX", e);
                LogManager.logFinOperacion(Categoria.DATA, "procesarDatosPIC", inicioOperacion, false);
                throw e;
            }

            // Convertir registros HEX a formato interno
            List<HexFileUtils.Pair<Integer, String>> records = convertirRegistrosHex(procesado);
            
            LogManager.d(Categoria.DATA, "procesarDatos",
                        String.format("Convertidos %d registros HEX", records.size()));

            // Filtrar registros por rangos de memoria
            List<HexFileUtils.Pair<Integer, String>> romRecords =
                    HexFileUtils.rangeFilterRecords(records, romWordBase, romWordEnd);
            List<HexFileUtils.Pair<Integer, String>> configRecords =
                    HexFileUtils.rangeFilterRecords(records, configWordBase, configWordEnd);
            List<HexFileUtils.Pair<Integer, String>> eepromRecords =
                    HexFileUtils.rangeFilterRecords(records, eepromWordBase, eepromWordEnd);

            LogManager.d(Categoria.DATA, "procesarDatos",
                        String.format("Registros filtrados - ROM: %d, CONFIG: %d, EEPROM: %d",
                                    romRecords.size(), configRecords.size(), eepromRecords.size()));

            // Generar datos en blanco para cada tipo de memoria
            byte[] romBlank = HexFileUtils.generateRomBlank(chipPIC.getTipoDeNucleoBit(), chipPIC.getTamanoROM());
            byte[] eepromBlank = HexFileUtils.generateEepromBlank(chipPIC.getTamanoEEPROM());

            LogManager.v(Categoria.DATA, "procesarDatos",
                        String.format("Datos en blanco generados - ROM: %d bytes, EEPROM: %d bytes",
                                    romBlank.length, eepromBlank.length));

            // Detectar endianness de los datos ROM
            boolean swapBytes = detectarEndianness(romRecords);
            LogManager.d(Categoria.DATA, "procesarDatos",
                        String.format("Endianness detectado: %s", swapBytes ? "Little-Endian" : "Big-Endian"));

            // Ajustar registros según endianness detectado
            if (swapBytes) {
                LogManager.v(Categoria.DATA, "procesarDatos", "Aplicando intercambio de bytes (swab) a registros");
                romRecords = HexFileUtils.swabRecords(romRecords);
                configRecords = HexFileUtils.swabRecords(configRecords);
            }

            // Procesar EEPROM con byte picking según endianness
            List<HexFileUtils.Pair<Integer, String>> adjustedEepromRecords =
                    procesarRegistrosEEPROM(eepromRecords, eepromWordBase, swapBytes);

            // Fusionar todos los datos
            this.romData = fusionarDatos(romRecords, romBlank, romWordBase, "ROM");
            this.eepromData = fusionarDatos(adjustedEepromRecords, eepromBlank, eepromWordBase, "EEPROM");

            // Procesar ID y fuses
            procesarIDyFuses(configRecords, configWordBase);

            // Generar información de resumen
            this.informacionProcesamiento = generarResumenProcesamiento();
            
            LogManager.i(Categoria.DATA, "procesarDatos", informacionProcesamiento);
            LogManager.logFinOperacion(Categoria.DATA, "procesarDatosPIC", inicioOperacion, true);

        } catch (Exception e) {
            LogManager.e(Categoria.DATA, "procesarDatos", "Error durante procesamiento de datos", e);
            LogManager.logFinOperacion(Categoria.DATA, "procesarDatosPIC", inicioOperacion, false);
            
            if (e instanceof HexProcessingException || e instanceof ChipConfigurationException) {
                throw e;
            } else {
                throw new HexProcessingException("Error inesperado procesando datos PIC", e);
            }
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
