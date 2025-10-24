package com.diamon.pruebas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChipinfoEntry - Representa una entrada individual de información de un chip PIC
 *
 * Esta clase es la migración de la clase Python "Chipinfo_Entry" y contiene todos
 * los datos de configuración de un microcontrolador PIC específico, incluyendo:
 * - Información del chip (nombre, tamaño ROM/EEPROM, tipo de núcleo)
 * - Configuración de programación (secuencia de energía, delays, reintentos)
 * - Configuración de fusibles (fuses) con sus valores y opciones
 *
 * Propósito: Almacenar y proveer métodos para manipular los datos de un chip PIC
 * que serán usados para configurar el protocolo de programación.
 */
public class ChipinfoEntry {

    // Excepción personalizada para errores relacionados con fusibles
    public static class FuseError extends Exception {
        public FuseError(String message) {
            super(message);
        }
    }

    // Diccionario de mapeo: PowerSequence string -> código numérico
    // Indica el orden en que se aplican Vcc y Vpp durante la programación
    private static final Map<String, Integer> POWER_SEQUENCE_DICT = new HashMap<>();
    static {
        POWER_SEQUENCE_DICT.put("Vcc", 0);           // Solo Vcc
        POWER_SEQUENCE_DICT.put("VccVpp1", 1);       // Vcc primero, luego Vpp en pin 1
        POWER_SEQUENCE_DICT.put("VccVpp2", 2);       // Vcc primero, luego Vpp en pin 2
        POWER_SEQUENCE_DICT.put("Vpp1Vcc", 3);       // Vpp primero en pin 1, luego Vcc
        POWER_SEQUENCE_DICT.put("Vpp2Vcc", 4);       // Vpp primero en pin 2, luego Vcc
        POWER_SEQUENCE_DICT.put("VccFastVpp1", 1);   // Vcc rápido, luego Vpp en pin 1
        POWER_SEQUENCE_DICT.put("VccFastVpp2", 2);   // Vcc rápido, luego Vpp en pin 2
    }

    // Diccionario de mapeo: PowerSequence string -> flag de delay rápido
    // True si se usa secuencia rápida, False si usa delay normal
    private static final Map<String, Boolean> VCC_VPP_DELAY_DICT = new HashMap<>();
    static {
        VCC_VPP_DELAY_DICT.put("Vcc", false);
        VCC_VPP_DELAY_DICT.put("VccVpp1", false);
        VCC_VPP_DELAY_DICT.put("VccVpp2", false);
        VCC_VPP_DELAY_DICT.put("Vpp1Vcc", false);
        VCC_VPP_DELAY_DICT.put("Vpp2Vcc", false);
        VCC_VPP_DELAY_DICT.put("VccFastVpp1", true);  // Modo rápido
        VCC_VPP_DELAY_DICT.put("VccFastVpp2", true);  // Modo rápido
    }

    // Diccionario de mapeo: tipo de socket -> ubicación del pin 1
    // Indica dónde está el pin 1 en el socket del programador
    private static final Map<String, String> SOCKET_IMAGE_DICT = new HashMap<>();
    static {
        SOCKET_IMAGE_DICT.put("8pin", "socket pin 13");
        SOCKET_IMAGE_DICT.put("14pin", "socket pin 13");
        SOCKET_IMAGE_DICT.put("18pin", "socket pin 2");
        SOCKET_IMAGE_DICT.put("28Npin", "socket pin 1");
        SOCKET_IMAGE_DICT.put("40pin", "socket pin 1");
    }

    // Mapa de variables del chip - almacena todos los parámetros
    private final Map<String, Object> vars;

    /**
     * Constructor - Inicializa una entrada de chip con todos sus parámetros
     *
     * @param CHIPname Nombre del chip (ej: "16F877A")
     * @param INCLUDE Si está incluido en la base de datos (Y/N)
     * @param SocketImage Tipo de socket/encapsulado (8pin, 14pin, etc)
     * @param EraseMode Modo de borrado del chip
     * @param FlashChip Si es memoria Flash (true) o EEPROM (false)
     * @param PowerSequence Secuencia de aplicación de voltajes
     * @param ProgramDelay Delay en microsegundos durante programación
     * @param ProgramTries Número de reintentos de programación
     * @param OverProgram Sobre-programación adicional
     * @param CoreType Tipo de núcleo del PIC (bit12A, bit14B, bit16A, etc)
     * @param ROMsize Tamaño de la memoria de programa en palabras
     * @param EEPROMsize Tamaño de la EEPROM en bytes
     * @param FUSEblank Lista de valores en blanco de los fusibles
     * @param CPwarn Advertencia de protección de código
     * @param CALword Palabra de calibración en ROM
     * @param BandGap Fusible de banda prohibida (bandgap)
     * @param ICSPonly Solo programación ICSP (In-Circuit Serial Programming)
     * @param ChipID ID del chip para identificación
     * @param fuses Mapa de fusibles con sus opciones y valores
     */
    public ChipinfoEntry(
            String CHIPname,
            String INCLUDE,
            String SocketImage,
            int EraseMode,
            boolean FlashChip,
            String PowerSequence,
            int ProgramDelay,
            int ProgramTries,
            int OverProgram,
            int CoreType,
            int ROMsize,
            int EEPROMsize,
            List<Integer> FUSEblank,
            boolean CPwarn,
            boolean CALword,
            boolean BandGap,
            boolean ICSPonly,
            int ChipID,
            Map<String, Map<String, List<FuseValue>>> fuses) {

        // Crear el mapa de variables y almacenar todos los parámetros
        this.vars = new HashMap<>();
        this.vars.put("CHIPname", CHIPname);
        this.vars.put("INCLUDE", INCLUDE);
        this.vars.put("SocketImage", SocketImage);
        this.vars.put("erase_mode", EraseMode);
        this.vars.put("FlashChip", FlashChip);

        // Convertir PowerSequence string a su código numérico
        this.vars.put("power_sequence", POWER_SEQUENCE_DICT.get(PowerSequence));
        this.vars.put("power_sequence_str", PowerSequence);

        this.vars.put("program_delay", ProgramDelay);
        this.vars.put("program_tries", ProgramTries);
        this.vars.put("over_program", OverProgram);
        this.vars.put("core_type", CoreType);
        this.vars.put("rom_size", ROMsize);
        this.vars.put("eeprom_size", EEPROMsize);
        this.vars.put("FUSEblank", FUSEblank);
        this.vars.put("CPwarn", CPwarn);
        this.vars.put("flag_calibration_value_in_ROM", CALword);
        this.vars.put("flag_band_gap_fuse", BandGap);
        this.vars.put("ICSPonly", ICSPonly);
        this.vars.put("ChipID", ChipID);
        this.vars.put("fuses", fuses);
    }

    /**
     * Clase auxiliar para representar un valor de fusible
     * Contiene el índice del fusible y su valor hexadecimal
     */
    public static class FuseValue {
        public final int index;      // Índice del fusible (0, 1, 2...)
        public final int value;      // Valor hexadecimal del fusible

        public FuseValue(int index, int value) {
            this.index = index;
            this.value = value;
        }
    }

    /**
     * Obtiene las variables de programación necesarias para configurar
     * el protocolo de comunicación con el PIC
     *
     * @return Mapa con todos los parámetros de programación
     */
    public Map<String, Object> getProgrammingVars() {
        Map<String, Object> result = new HashMap<>();

        result.put("rom_size", vars.get("rom_size"));
        result.put("eeprom_size", vars.get("eeprom_size"));
        result.put("core_type", vars.get("core_type"));
        result.put("flag_calibration_value_in_ROM", vars.get("flag_calibration_value_in_ROM"));
        result.put("flag_band_gap_fuse", vars.get("flag_band_gap_fuse"));

        // Según T.Nixon: flag es true solo para core_type bit16_a (tipo 1)
        int coreType = (Integer) vars.get("core_type");
        result.put("flag_18f_single_panel_access_mode", (coreType == 1));

        // Obtener flag de delay desde el diccionario
        String powerSeqStr = (String) vars.get("power_sequence_str");
        result.put("flag_vcc_vpp_delay", VCC_VPP_DELAY_DICT.get(powerSeqStr));

        result.put("program_delay", vars.get("program_delay"));
        result.put("power_sequence", vars.get("power_sequence"));
        result.put("erase_mode", vars.get("erase_mode"));
        result.put("program_retries", vars.get("program_tries"));
        result.put("over_program", vars.get("over_program"));

        return result;
    }

    /**
     * Obtiene el número de bits del núcleo del PIC según su tipo
     *
     * @return Número de bits (12, 14, o 16) o null si el tipo es desconocido
     */
    public Integer getCoreBits() {
        int coreType = (Integer) vars.get("core_type");

        // Núcleos de 16 bits
        if (coreType == 1 || coreType == 2) {
            return 16;
        }
        // Núcleos de 14 bits
        else if (coreType == 3 || coreType == 5 || coreType == 6 ||
                coreType == 7 || coreType == 8 || coreType == 9 || coreType == 10) {
            return 14;
        }
        // Núcleos de 12 bits
        else if (coreType == 4) {
            return 12;
        }

        return null;  // Tipo desconocido
    }

    /**
     * Decodifica una lista de valores de fusibles a su representación simbólica
     *
     * Este método toma los valores hexadecimales de los fusibles leídos del chip
     * y los convierte a nombres legibles (ejemplo: "WDT" = "Enabled")
     *
     * @param fuseValues Lista de valores hexadecimales de fusibles
     * @return Mapa de nombre_fusible -> valor_simbólico
     * @throws FuseError Si no se puede identificar la configuración del fusible
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> decodeFuseData(List<Integer> fuseValues) throws FuseError {
        Map<String, Map<String, List<FuseValue>>> fuseParamList =
                (Map<String, Map<String, List<FuseValue>>>) vars.get("fuses");

        Map<String, String> result = new HashMap<>();

        // Iterar sobre cada parámetro de fusible (WDT, Code Protect, etc)
        for (Map.Entry<String, Map<String, List<FuseValue>>> fuseParamEntry : fuseParamList.entrySet()) {
            String fuseParam = fuseParamEntry.getKey();
            Map<String, List<FuseValue>> fuseSettings = fuseParamEntry.getValue();

            // Inicializar mejor valor con todos bits en 1 (sin bits limpiados)
            List<Integer> bestValue = new ArrayList<>();
            for (int i = 0; i < fuseValues.size(); i++) {
                bestValue.add(0xFFFF);
            }

            boolean fuseIdentified = false;

            // Probar cada configuración posible del fusible
            for (Map.Entry<String, List<FuseValue>> settingEntry : fuseSettings.entrySet()) {
                String setting = settingEntry.getKey();
                List<FuseValue> settingValue = settingEntry.getValue();

                // Realizar AND indexado: (fuseValues & settingValue)
                List<Integer> andResult = indexwiseAnd(fuseValues, settingValue);

                // Si el resultado del AND es igual a fuseValues, esta configuración aplica
                if (andResult.equals(fuseValues)) {
                    // Verificar si esta configuración limpia más bits que la mejor actual
                    List<Integer> bestAndSetting = indexwiseAnd(bestValue, settingValue);

                    if (!bestAndSetting.equals(bestValue)) {
                        // Esta configuración limpia más bits, es mejor
                        bestValue = bestAndSetting;
                        result.put(fuseParam, setting);
                        fuseIdentified = true;
                    }
                }
            }

            // Si no se pudo identificar el fusible, lanzar error
            if (!fuseIdentified) {
                throw new FuseError("No se pudo identificar la configuración del fusible: " + fuseParam);
            }
        }

        return result;
    }

    /**
     * Codifica un diccionario de fusibles simbólicos a sus valores hexadecimales
     *
     * Este método toma configuraciones legibles (ejemplo: "WDT" = "Enabled")
     * y las convierte a los valores hexadecimales correspondientes para programar
     *
     * @param fuseDict Mapa de nombre_fusible -> valor_simbólico
     * @return Lista de valores hexadecimales de fusibles
     * @throws FuseError Si el fusible o su valor son inválidos
     */
    @SuppressWarnings("unchecked")
    public List<Integer> encodeFuseData(Map<String, String> fuseDict) throws FuseError {
        // Iniciar con los valores en blanco de los fusibles
        List<Integer> result = new ArrayList<>((List<Integer>) vars.get("FUSEblank"));

        Map<String, Map<String, List<FuseValue>>> fuseParamList =
                (Map<String, Map<String, List<FuseValue>>>) vars.get("fuses");

        // Procesar cada fusible del diccionario
        for (Map.Entry<String, String> entry : fuseDict.entrySet()) {
            String fuse = entry.getKey();
            String fuseValue = entry.getValue();

            // Verificar que el fusible existe
            if (!fuseParamList.containsKey(fuse)) {
                throw new FuseError("Fusible desconocido: \"" + fuse + "\"");
            }

            Map<String, List<FuseValue>> fuseSettings = fuseParamList.get(fuse);

            // Verificar que el valor del fusible es válido
            if (!fuseSettings.containsKey(fuseValue)) {
                throw new FuseError("Configuración de fusible inválida: \"" +
                        fuse + "\" = \"" + fuseValue + "\"");
            }

            // Aplicar el valor del fusible mediante AND indexado
            List<FuseValue> settingValues = fuseSettings.get(fuseValue);
            result = indexwiseAnd(result, settingValues);
        }

        return result;
    }

    /**
     * Función auxiliar: Realiza operación AND indexada
     *
     * Toma una lista de valores y aplica AND con valores en posiciones específicas
     * Ejemplo: fuses = fuses & settingValues[i].value (donde index=2)
     *
     * @param fuses Lista de valores actuales
     * @param settingValues Lista de pares (index, value) a aplicar
     * @return Nueva lista con AND aplicado
     */
    private List<Integer> indexwiseAnd(List<Integer> fuses, List<FuseValue> settingValues) {
        // Crear copia de la lista original
        List<Integer> result = new ArrayList<>(fuses);

        // Aplicar AND en cada índice especificado
        for (FuseValue fv : settingValues) {
            if (fv.index < result.size()) {
                result.set(fv.index, result.get(fv.index) & fv.value);
            }
        }

        return result;
    }

    /**
     * Verifica si el chip tiene memoria EEPROM
     *
     * @return true si tiene EEPROM, false si no
     */
    public boolean hasEeprom() {
        int eepromSize = (Integer) vars.get("eeprom_size");
        return eepromSize != 0;
    }

    /**
     * Obtiene la descripción textual de la ubicación del pin 1
     *
     * @return String describiendo dónde está el pin 1 en el socket
     */
    public String getPin1LocationText() {
        String socketImage = (String) vars.get("SocketImage");
        return SOCKET_IMAGE_DICT.get(socketImage);
    }

    /**
     * Genera documentación de los fusibles disponibles
     *
     * @return String con la documentación de todos los fusibles y sus opciones
     */
    @SuppressWarnings("unchecked")
    public String getFuseDoc() {
        StringBuilder result = new StringBuilder();

        Map<String, Map<String, List<FuseValue>>> fuseParamList =
                (Map<String, Map<String, List<FuseValue>>>) vars.get("fuses");

        // Generar documentación para cada fusible
        for (Map.Entry<String, Map<String, List<FuseValue>>> entry : fuseParamList.entrySet()) {
            String fuse = entry.getKey();
            Map<String, List<FuseValue>> fuseSettings = entry.getValue();

            result.append("'").append(fuse).append("' : (");

            boolean first = true;
            for (String setting : fuseSettings.keySet()) {
                if (!first) {
                    result.append(", ");
                }
                result.append("'").append(setting).append("'");
                first = false;
            }

            result.append(")\n");
        }

        return result.toString();
    }

    /**
     * Obtiene el valor de una variable del chip
     *
     * @param key Nombre de la variable
     * @return Valor de la variable o null si no existe
     */
    public Object getVar(String key) {
        return vars.get(key);
    }

    /**
     * Obtiene el nombre del chip
     *
     * @return Nombre del chip (ejemplo: "16F877A")
     */
    public String getChipName() {
        return (String) vars.get("CHIPname");
    }
}
