package com.diamon.pruebas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChipinfoEntry - Representa una entrada individual de información de un chip PIC
 *
 * VERSIÓN CORREGIDA: Búsqueda flexible de fusibles para PICs 18F
 * - Búsqueda exacta
 * - Búsqueda case-insensitive
 * - Búsqueda por coincidencia parcial
 * - Mensajes de error útiles con fusibles disponibles
 * - Compatible con Android API 14+
 *
 * Esta clase es la migración de la clase Python "Chipinfo_Entry" y contiene todos
 * los datos de configuración de un microcontrolador PIC específico.
 */
public class ChipinfoEntry {

    public static class FuseError extends Exception {
        public FuseError(String message) {
            super(message);
        }
    }

    private static final Map<String, Integer> POWER_SEQUENCE_DICT = new HashMap<>();
    static {
        POWER_SEQUENCE_DICT.put("Vcc", 0);
        POWER_SEQUENCE_DICT.put("VccVpp1", 1);
        POWER_SEQUENCE_DICT.put("VccVpp2", 2);
        POWER_SEQUENCE_DICT.put("Vpp1Vcc", 3);
        POWER_SEQUENCE_DICT.put("Vpp2Vcc", 4);
        POWER_SEQUENCE_DICT.put("VccFastVpp1", 1);
        POWER_SEQUENCE_DICT.put("VccFastVpp2", 2);
    }

    private static final Map<String, Boolean> VCC_VPP_DELAY_DICT = new HashMap<>();
    static {
        VCC_VPP_DELAY_DICT.put("Vcc", false);
        VCC_VPP_DELAY_DICT.put("VccVpp1", false);
        VCC_VPP_DELAY_DICT.put("VccVpp2", false);
        VCC_VPP_DELAY_DICT.put("Vpp1Vcc", false);
        VCC_VPP_DELAY_DICT.put("Vpp2Vcc", false);
        VCC_VPP_DELAY_DICT.put("VccFastVpp1", true);
        VCC_VPP_DELAY_DICT.put("VccFastVpp2", true);
    }

    private static final Map<String, String> SOCKET_IMAGE_DICT = new HashMap<>();
    static {
        SOCKET_IMAGE_DICT.put("8pin", "socket pin 13");
        SOCKET_IMAGE_DICT.put("14pin", "socket pin 13");
        SOCKET_IMAGE_DICT.put("18pin", "socket pin 2");
        SOCKET_IMAGE_DICT.put("28Npin", "socket pin 1");
        SOCKET_IMAGE_DICT.put("40pin", "socket pin 1");
    }

    private final Map<String, Object> vars;

    public ChipinfoEntry(
            String CHIPname, String INCLUDE, String SocketImage,
            int EraseMode, boolean FlashChip, String PowerSequence,
            int ProgramDelay, int ProgramTries, int OverProgram,
            int CoreType, int ROMsize, int EEPROMsize,
            List<Integer> FUSEblank, boolean CPwarn, boolean CALword,
            boolean BandGap, boolean ICSPonly, int ChipID,
            Map<String, Map<String, List<FuseValue>>> fuses) {

        this.vars = new HashMap<>();
        this.vars.put("CHIPname", CHIPname);
        this.vars.put("INCLUDE", INCLUDE);
        this.vars.put("SocketImage", SocketImage);
        this.vars.put("erase_mode", EraseMode);
        this.vars.put("FlashChip", FlashChip);

        Integer powerSeq = POWER_SEQUENCE_DICT.get(PowerSequence);
        this.vars.put("power_sequence", (powerSeq != null) ? powerSeq : 0);
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

    public static class FuseValue {
        public final int index;
        public final int value;

        public FuseValue(int index, int value) {
            this.index = index;
            this.value = value;
        }
    }

    public Map<String, Object> getProgrammingVars() {
        Map<String, Object> result = new HashMap<>();
        result.put("rom_size", vars.get("rom_size"));
        result.put("eeprom_size", vars.get("eeprom_size"));
        result.put("core_type", vars.get("core_type"));
        result.put("flag_calibration_value_in_ROM", vars.get("flag_calibration_value_in_ROM"));
        result.put("flag_band_gap_fuse", vars.get("flag_band_gap_fuse"));

        int coreType = (Integer) vars.get("core_type");
        result.put("flag_18f_single_panel_access_mode", (coreType == 1));

        String powerSeqStr = (String) vars.get("power_sequence_str");
        Boolean delayFlag = VCC_VPP_DELAY_DICT.get(powerSeqStr);
        result.put("flag_vcc_vpp_delay", (delayFlag != null) ? delayFlag : false);

        result.put("program_delay", vars.get("program_delay"));
        result.put("power_sequence", vars.get("power_sequence"));
        result.put("erase_mode", vars.get("erase_mode"));
        result.put("program_retries", vars.get("program_tries"));
        result.put("over_program", vars.get("over_program"));

        return result;
    }

    public Integer getCoreBits() {
        int coreType = (Integer) vars.get("core_type");

        if (coreType == 1 || coreType == 2 || coreType == 13) {
            return 16;
        } else if (coreType == 3 || coreType == 5 || coreType == 6 ||
                coreType == 7 || coreType == 8 || coreType == 9 ||
                coreType == 10 || coreType == 12) {
            return 14;
        } else if (coreType == 4 || coreType == 11) {
            return 12;
        }

        return null;
    }

    /**
     * ✅ MÉTODO NUEVO: Búsqueda flexible de fusibles
     * Compatible con Android API 14+
     */
    private String findFuseName(Map<String, Map<String, List<FuseValue>>> fuseConfigs,
                                String fuseName) {
        // 1. Búsqueda exacta
        if (fuseConfigs.containsKey(fuseName)) {
            return fuseName;
        }

        // 2. Búsqueda case-insensitive
        for (String key : fuseConfigs.keySet()) {
            if (key.equalsIgnoreCase(fuseName)) {
                return key;
            }
        }

        // 3. Búsqueda por coincidencia parcial
        String searchTerm = fuseName.toLowerCase();
        for (String key : fuseConfigs.keySet()) {
            String keyLower = key.toLowerCase();
            if (keyLower.contains(searchTerm) || searchTerm.contains(keyLower)) {
                return key;
            }
        }

        return null;
    }

    /**
     * Decodifica una lista de valores de fusibles a su representación simbólica
     *
     * ✅ VERSIÓN FINAL CORREGIDA: Matching tolerante para valores imperfectos
     *
     * @param fuseValues Lista de valores hexadecimales de fusibles
     * @return Mapa de nombre_fusible -> valor_simbólico
     * @throws FuseError Si no se puede identificar la configuración del fusible
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> decodeFuseData(List<Integer> fuseValues) throws FuseError {
        Map<String, Map<String, List<FuseValue>>> fuseParamList =
                (Map<String, Map<String, List<FuseValue>>>) vars.get("fuses");

        if (fuseParamList == null || fuseParamList.isEmpty()) {
            throw new FuseError("No hay configuraciones de fusibles disponibles");
        }

        Map<String, String> result = new HashMap<>();

        // Iterar sobre cada parámetro de fusible
        for (Map.Entry<String, Map<String, List<FuseValue>>> fuseParamEntry : fuseParamList.entrySet()) {
            String fuseParam = fuseParamEntry.getKey();
            Map<String, List<FuseValue>> fuseSettings = fuseParamEntry.getValue();

            String bestSetting = null;
            int bestScore = -1;

            // Probar cada configuración posible del fusible
            for (Map.Entry<String, List<FuseValue>> settingEntry : fuseSettings.entrySet()) {
                String setting = settingEntry.getKey();
                List<FuseValue> settingValue = settingEntry.getValue();

                // Calcular score de coincidencia
                int score = calculateMatchScore(fuseValues, settingValue);

                if (score > bestScore) {
                    bestScore = score;
                    bestSetting = setting;
                }
            }

            if (bestSetting != null) {
                result.put(fuseParam, bestSetting);
            } else {
                // Si no hay match, usar valor "Unknown"
                result.put(fuseParam, "Unknown");
            }
        }

        return result;
    }

    /**
     * Calcula score de coincidencia entre valores leídos y configuración
     * Retorna número de bits que coinciden
     */
    private int calculateMatchScore(List<Integer> fuseValues, List<FuseValue> settingValues) {
        int score = 0;

        for (FuseValue fv : settingValues) {
            if (fv.index < fuseValues.size()) {
                int actual = fuseValues.get(fv.index);
                int expected = fv.value;

                // Contar bits que coinciden
                int andResult = actual & expected;
                score += Integer.bitCount(andResult);
            }
        }

        return score;
    }


    /**
     * ✅ MÉTODO CORREGIDO: Usa búsqueda flexible
     */
    @SuppressWarnings("unchecked")
    public List<Integer> encodeFuseData(Map<String, String> fuseDict) throws FuseError {
        List<Integer> result = new ArrayList<>((List<Integer>) vars.get("FUSEblank"));

        Map<String, Map<String, List<FuseValue>>> fuseParamList =
                (Map<String, Map<String, List<FuseValue>>>) vars.get("fuses");

        if (fuseParamList == null || fuseParamList.isEmpty()) {
            throw new FuseError("No hay configuraciones de fusibles disponibles");
        }

        for (Map.Entry<String, String> entry : fuseDict.entrySet()) {
            String fuse = entry.getKey();
            String fuseValue = entry.getValue();

            // ✅ CORRECCIÓN: Búsqueda flexible del fusible
            String actualFuseName = findFuseName(fuseParamList, fuse);

            if (actualFuseName == null) {
                StringBuilder availableFuses = new StringBuilder();
                availableFuses.append("Fusibles disponibles: ");
                int count = 0;
                for (String key : fuseParamList.keySet()) {
                    if (count > 0) availableFuses.append(", ");
                    availableFuses.append(key);
                    if (++count >= 10) {
                        availableFuses.append("...");
                        break;
                    }
                }
                throw new FuseError("Fusible desconocido: \"" + fuse + "\". " +
                        availableFuses.toString());
            }

            Map<String, List<FuseValue>> fuseSettings = fuseParamList.get(actualFuseName);

            if (!fuseSettings.containsKey(fuseValue)) {
                StringBuilder availableValues = new StringBuilder();
                availableValues.append("Valores disponibles para '").append(actualFuseName).append("': ");
                int count = 0;
                for (String key : fuseSettings.keySet()) {
                    if (count > 0) availableValues.append(", ");
                    availableValues.append(key);
                    count++;
                }
                throw new FuseError("Configuración de fusible inválida: \"" +
                        actualFuseName + "\" = \"" + fuseValue + "\". " +
                        availableValues.toString());
            }

            List<FuseValue> settingValues = fuseSettings.get(fuseValue);
            result = indexwiseAnd(result, settingValues);
        }

        return result;
    }

    private List<Integer> indexwiseAnd(List<Integer> fuses, List<FuseValue> settingValues) {
        List<Integer> result = new ArrayList<>(fuses);

        for (FuseValue fv : settingValues) {
            if (fv.index < result.size()) {
                result.set(fv.index, result.get(fv.index) & fv.value);
            }
        }

        return result;
    }

    public boolean hasEeprom() {
        int eepromSize = (Integer) vars.get("eeprom_size");
        return eepromSize != 0;
    }

    public String getPin1LocationText() {
        String socketImage = (String) vars.get("SocketImage");
        String location = SOCKET_IMAGE_DICT.get(socketImage);
        return (location != null) ? location : "ubicación desconocida";
    }

    @SuppressWarnings("unchecked")
    public String getFuseDoc() {
        StringBuilder result = new StringBuilder();

        Map<String, Map<String, List<FuseValue>>> fuseParamList =
                (Map<String, Map<String, List<FuseValue>>>) vars.get("fuses");

        if (fuseParamList == null || fuseParamList.isEmpty()) {
            return "No hay fusibles disponibles para este chip";
        }

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

    public Object getVar(String key) {
        return vars.get(key);
    }

    public String getChipName() {
        return (String) vars.get("CHIPname");
    }
}
