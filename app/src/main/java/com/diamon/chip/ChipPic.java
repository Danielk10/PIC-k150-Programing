package com.diamon.chip;

import com.diamon.excepciones.ChipConfigurationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clase para manejar la configuración y propiedades de chips PIC. Proporciona
 * métodos para acceder
 * a las características del chip, configuraciones de programación y valores de
 * fuses.
 */
public class ChipPic {

    // --- Clases internas para manejo de fuses ---

    /** Error en operacion de fuses */
    public static class FuseError extends Exception {
        public FuseError(String message) {
            super(message);
        }
    }

    /**
     * Par (indice, valor) que representa el efecto de una opcion de fuse
     * sobre la palabra de configuracion del PIC.
     */
    public static class FuseValue {
        public final int index;
        public final int value;

        public FuseValue(int index, int value) {
            this.index = index;
            this.value = value;
        }
    }

    // --- Campos internos ---

    private Map<String, Object> variablesDeChip;

    private HashMap<String, String> variablesProgramacion;

    private HashMap<String, Integer> secuenciaEncedido;

    private HashMap<String, Boolean> vccVppTiempo;

    private HashMap<String, String> socketImagen;

    private HashMap<String, Integer> tipoDeNucleo;

    private HashMap<String, Boolean> respuestas;

    private boolean icsp;

    /**
     * Constructor para inicializar un objeto ChipPic con todas las configuraciones.
     *
     * @param CHIPname      Nombre del chip PIC
     * @param INCLUDEr      Archivo de inclusión
     * @param SocketImage   Imagen del socket
     * @param EraseMode     Modo de borrado
     * @param FlashChip     Indica si es un chip flash
     * @param PowerSequence Secuencia de encendido
     * @param ProgramDelay  Retardo de programación
     * @param ProgramTries  Intentos de programación
     * @param OverProgram   Sobrecarga de programación
     * @param CoreType      Tipo de núcleo
     * @param ROMsize       Tamaño de ROM en hexadecimal
     * @param EEPROMsize    Tamaño de EEPROM en hexadecimal
     * @param FUSEblank     Valores blank de fuses
     * @param CPwarn        Advertencia de protección de código
     * @param CALword       Palabra de calibración
     * @param BandGap       Band gap
     * @param ICSPonly      Indica si solo usa ICSP
     * @param ChipID        ID del chip en hexadecimal
     * @param fuses         Mapa de fuses del chip
     * @throws ChipConfigurationException Si los parámetros son inválidos
     */
    public ChipPic(
            String CHIPname,
            String INCLUDEr,
            String SocketImage,
            String EraseMode,
            String FlashChip,
            String PowerSequence,
            String ProgramDelay,
            String ProgramTries,
            String OverProgram,
            String CoreType,
            String ROMsize,
            String EEPROMsize,
            String[] FUSEblank,
            String CPwarn,
            String CALword,
            String BandGap,
            String ICSPonly,
            String ChipID,
            Map<String, Object> fuses)
            throws ChipConfigurationException {

        // Validar parámetros obligatorios
        if (CHIPname == null || CHIPname.trim().isEmpty()) {
            throw new ChipConfigurationException("Nombre de chip no puede ser nulo o vacío");
        }

        if (ROMsize == null || ROMsize.trim().isEmpty()) {
            throw new ChipConfigurationException("Tamaño de ROM no puede ser nulo o vacío");
        }

        if (CoreType == null || CoreType.trim().isEmpty()) {
            throw new ChipConfigurationException("Tipo de núcleo no puede ser nulo o vacío");
        }

        secuenciaEncedido = new HashMap<String, Integer>();

        secuenciaEncedido.put("vcc", 0);

        secuenciaEncedido.put("vccvpp1", 1);

        secuenciaEncedido.put("vccvpp2", 2);

        secuenciaEncedido.put("vpp1vcc", 3);

        secuenciaEncedido.put("vpp2vcc", 4);

        secuenciaEncedido.put("vccfastvpp1", 1);

        secuenciaEncedido.put("vccfastvpp2", 2);

        vccVppTiempo = new HashMap<String, Boolean>();

        vccVppTiempo.put("vcc", false);

        vccVppTiempo.put("vccvpp1", false);

        vccVppTiempo.put("vccvpp2", false);

        vccVppTiempo.put("vpp1vcc", false);

        vccVppTiempo.put("vpp2vcc", false);

        vccVppTiempo.put("vccfastvpp1", true);

        vccVppTiempo.put("vccfastvpp2", true);

        socketImagen = new HashMap<String, String>();

        socketImagen.put("0pin", "ICSP"); // PIC10xxx y chips solo-ICSP

        socketImagen.put("8pin", "socket pin 13");

        socketImagen.put("14pin", "socket pin 13");

        socketImagen.put("18pin", "socket pin 2");

        socketImagen.put("28Npin", "socket pin 1");

        socketImagen.put("40pin", "socket pin 1");

        variablesProgramacion = new HashMap<String, String>();

        variablesDeChip = new HashMap<String, Object>();

        variablesDeChip.put("CHIPname", "" + CHIPname);

        variablesDeChip.put("INCLUDE", "" + INCLUDEr);

        variablesDeChip.put("SocketImage", "" + SocketImage);

        variablesDeChip.put("erase_mode", "" + EraseMode);

        variablesDeChip.put("FlashChip", "" + FlashChip);

        variablesDeChip.put(
                "power_sequence", "" + secuenciaEncedido.get(PowerSequence.toLowerCase()));

        variablesDeChip.put("power_sequence_str", "" + PowerSequence);

        variablesDeChip.put("program_delay", "" + ProgramDelay);

        variablesDeChip.put("program_tries", "" + ProgramTries);

        variablesDeChip.put("over_program", "" + OverProgram);

        variablesDeChip.put("core_type", "" + CoreType);

        variablesDeChip.put("rom_size", "" + ROMsize);

        variablesDeChip.put("eeprom_size", "" + EEPROMsize);

        variablesDeChip.put("FUSEblank", FUSEblank);

        variablesDeChip.put("CPwarn", "" + CPwarn);

        variablesDeChip.put("flag_calibration_value_in_ROM", "" + CALword);

        variablesDeChip.put("flag_band_gap_fuse", "" + BandGap);

        variablesDeChip.put("ICSPonly", "" + ICSPonly);

        variablesDeChip.put("ChipID", "" + ChipID);

        variablesDeChip.put("fuses", fuses);

        tipoDeNucleo = new HashMap<String, Integer>();

        tipoDeNucleo.put("bit16_a", 1);

        tipoDeNucleo.put("bit16_b", 2);

        tipoDeNucleo.put("bit14_g", 3);

        tipoDeNucleo.put("bit12_a", 4);

        tipoDeNucleo.put("bit14_a", 5);

        tipoDeNucleo.put("bit14_b", 6);

        tipoDeNucleo.put("bit14_c", 7);

        tipoDeNucleo.put("bit14_d", 8);

        tipoDeNucleo.put("bit14_e", 9);

        tipoDeNucleo.put("bit14_f", 10);

        tipoDeNucleo.put("bit12_b", 11);

        tipoDeNucleo.put("bit14_h", 12);

        tipoDeNucleo.put("bit16_c", 13);

        tipoDeNucleo.put("newf12b", 0); // No esta en la documentacion

        respuestas = new HashMap<String, Boolean>();

        respuestas.put("y", true);

        respuestas.put("1", true);

        respuestas.put("n", false);

        respuestas.put("0", false);

        icsp = false;
    }

    /**
     * Obtiene el tipo de núcleo en bits (16, 14 o 12).
     *
     * @return Tipo de núcleo en bits
     * @throws ChipConfigurationException Si el tipo de núcleo es inválido
     */
    public int getTipoDeNucleoBit() throws ChipConfigurationException {

        try {
            String coreTypeStr = variablesDeChip.get("core_type").toString().toLowerCase();
            Integer nucleoObj = tipoDeNucleo.get(coreTypeStr);
            if (nucleoObj == null) {
                throw new ChipConfigurationException(
                        "Tipo de núcleo no encontrado: " + coreTypeStr);
            }

            int nucleo = nucleoObj;

            if (nucleo == 1 || nucleo == 2 || nucleo == 13) {
                nucleo = 16;
            } else if (nucleo == 3
                    || nucleo == 5
                    || nucleo == 6
                    || nucleo == 7
                    || nucleo == 8
                    || nucleo == 9
                    || nucleo == 10
                    || nucleo == 12) {
                nucleo = 14;
            } else if (nucleo == 4 || nucleo == 11) {
                nucleo = 12;
            } else if (nucleo == 0) {
                nucleo = 14;
            } else {
                throw new ChipConfigurationException("Tipo de núcleo inválido: " + nucleo);
            }

            return nucleo;

        } catch (NumberFormatException e) {
            throw new ChipConfigurationException(
                    "Error al procesar tipo de núcleo: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si el chip solo puede ser programado mediante ICSP.
     *
     * @return true si el chip solo usa ICSP, false en caso contrario
     * @throws ChipConfigurationException Si hay un error al procesar la
     *                                    configuración ICSP
     */
    public boolean isICSPonly() throws ChipConfigurationException {

        try {
            String icspOnlyStr = variablesDeChip.get("ICSPonly").toString().toLowerCase();

            Boolean valor = respuestas.get(icspOnlyStr);
            if (valor == null) {
                throw new ChipConfigurationException("Valor ICSPonly inválido: " + icspOnlyStr);
            }

            return valor;

        } catch (Exception e) {
            throw new ChipConfigurationException(
                    "Error al verificar ICSPonly: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene las variables de programación del chip.
     *
     * @return Mapa con las variables de programación
     * @throws ChipConfigurationException Si hay un error al obtener las variables
     */
    public HashMap<String, String> getVariablesDeProgramacion() throws ChipConfigurationException {

        try {
            // Limpiar el mapa antes de llenarlo
            variablesProgramacion.clear();

            // Variables básicas
            variablesProgramacion.put("rom_size", "" + variablesDeChip.get("rom_size"));
            variablesProgramacion.put("eeprom_size", "" + variablesDeChip.get("eeprom_size"));
            variablesProgramacion.put("core_type", "" + variablesDeChip.get("core_type"));
            variablesProgramacion.put("program_delay", "" + variablesDeChip.get("program_delay"));
            variablesProgramacion.put("power_sequence", "" + variablesDeChip.get("power_sequence"));
            variablesProgramacion.put("erase_mode", "" + variablesDeChip.get("erase_mode"));
            variablesProgramacion.put("program_retries", "" + variablesDeChip.get("program_tries"));
            variablesProgramacion.put("over_program", "" + variablesDeChip.get("over_program"));

            // Flags
            variablesProgramacion.put(
                    "flag_calibration_value_in_ROM",
                    "" + variablesDeChip.get("flag_calibration_value_in_ROM"));
            variablesProgramacion.put(
                    "flag_band_gap_fuse", "" + variablesDeChip.get("flag_band_gap_fuse"));

            // Flag 18F single panel access mode
            String coreType = variablesDeChip.get("core_type").toString().toLowerCase();
            Integer coreTypeValue = tipoDeNucleo.get(coreType);
            Integer bit16aValue = tipoDeNucleo.get("bit16_a");

            if (coreTypeValue != null && bit16aValue != null) {
                variablesProgramacion.put(
                        "flag_18f_single_panel_access_mode",
                        "" + coreTypeValue.equals(bit16aValue));
            } else {
                variablesProgramacion.put("flag_18f_single_panel_access_mode", "false");
            }

            // Flag VCC VPP delay
            String powerSequence = variablesDeChip.get("power_sequence_str").toString().toLowerCase();
            Boolean vccVppDelay = vccVppTiempo.get(powerSequence);

            if (vccVppDelay != null) {
                variablesProgramacion.put("flag_vcc_vpp_delay", "" + vccVppDelay);
            } else {
                variablesProgramacion.put("flag_vcc_vpp_delay", "false");
            }

            return variablesProgramacion;

        } catch (Exception e) {
            throw new ChipConfigurationException(
                    "Error al obtener variables de programación: " + e.getMessage(), e);
        }
    }

    public boolean isFlagCalibration() {
        Object raw = variablesDeChip.get("flag_calibration_value_in_ROM");
        if (raw == null)
            return false;
        Boolean valor = respuestas.get(raw.toString().toLowerCase());
        return valor != null && valor;
    }

    public boolean isFlagBandGap() {
        Object raw = variablesDeChip.get("flag_band_gap_fuse");
        if (raw == null)
            return false;
        Boolean valor = respuestas.get(raw.toString().toLowerCase());
        return valor != null && valor;
    }

    public boolean isFlag18fSingle() {

        String dato = ""
                + tipoDeNucleo
                        .get(variablesDeChip.get("core_type").toString().toLowerCase())
                        .toString()
                        .equals("" + tipoDeNucleo.get("bit16_a"));

        boolean valor = Boolean.parseBoolean(dato);

        return valor;
    }

    /**
     * Obtiene el retardo de programación.
     *
     * @return Retardo de programación
     * @throws ChipConfigurationException Si hay un error al procesar el valor
     */
    public int getProgramDelay() throws ChipConfigurationException {

        try {
            String delayStr = variablesDeChip.get("program_delay").toString();
            int valor = Integer.parseUnsignedInt(delayStr, 10);

            return valor;

        } catch (NumberFormatException e) {
            throw new ChipConfigurationException(
                    "Error al procesar retardo de programación: " + e.getMessage(), e);
        }
    }

    public int getPowerSequence() {
        int valor = Integer.parseUnsignedInt("" + variablesDeChip.get("power_sequence"), 10);

        return valor;
    }

    public int getEraseMode() {
        int valor = Integer.parseUnsignedInt("" + variablesDeChip.get("erase_mode"), 10);

        return valor;
    }

    public int getProgramTries() {
        int valor = Integer.parseUnsignedInt("" + variablesDeChip.get("program_tries"), 10);

        return valor;
    }

    public int getOverProgram() {
        int valor = Integer.parseUnsignedInt("" + variablesDeChip.get("over_program"), 10);

        return valor;
    }

    public boolean isFlagVccVppDelay() {

        boolean valor = vccVppTiempo.get(("" + variablesDeChip.get("power_sequence_str")).toLowerCase());

        return valor;
    }

    public int getTipoDeNucleoDelPic() {

        int nucleo = Integer.parseInt(
                ""
                        + tipoDeNucleo.get(
                                ""
                                        + variablesDeChip
                                                .get("core_type")
                                                .toString()
                                                .toLowerCase()));

        return nucleo;
    }

    public void setActivarICSP(boolean activar) {

        this.icsp = activar;
    }

    public boolean isISCPModo() {

        return icsp;
    }

    public int getSecuenciaDeEncendido() {

        int secuencia = Integer.parseUnsignedInt("" + variablesDeChip.get("power_sequence"), 10);

        if (icsp) {

            if (secuencia == 2) {

                secuencia = 1;

            } else if (secuencia == 4) {

                secuencia = 3;
            }

        } else {

            secuencia = Integer.parseUnsignedInt("" + variablesDeChip.get("power_sequence"), 10);
        }

        return secuencia;
    }

    public boolean isTamanoValidoDeEEPROM() {

        try {
            // EEPROMsize en chipinfo.cid esta en hexadecimal: 00000080 = 128 bytes
            int tamano = Integer.parseUnsignedInt(
                    variablesDeChip.get("eeprom_size").toString().trim(), 16);
            return tamano != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Obtiene el tamaño de la ROM del chip.
     *
     * @return Tamaño de la ROM en bytes
     * @throws ChipConfigurationException Si hay un error al procesar el tamaño
     */
    public int getTamanoROM() throws ChipConfigurationException {

        try {
            String romSizeStr = variablesDeChip.get("rom_size").toString();
            int tamano = Integer.parseUnsignedInt(romSizeStr, 16);

            return tamano;

        } catch (NumberFormatException e) {
            throw new ChipConfigurationException(
                    "Error al procesar tamaño de ROM: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el tamaño de la EEPROM del chip.
     *
     * @return Tamaño de la EEPROM en bytes
     * @throws ChipConfigurationException Si hay un error al procesar el tamaño
     */
    public int getTamanoEEPROM() throws ChipConfigurationException {

        try {
            String eepromSizeStr = variablesDeChip.get("eeprom_size").toString();
            int tamano = Integer.parseUnsignedInt(eepromSizeStr, 16);

            return tamano;

        } catch (NumberFormatException e) {
            throw new ChipConfigurationException(
                    "Error al procesar tamaño de EEPROM: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene los valores blank de fuses como array de enteros.
     *
     * @return Array con los valores blank de fuses
     * @throws ChipConfigurationException Si hay un error al procesar los fuses
     */
    public int[] getFuseBlank() throws ChipConfigurationException {
        try {
            String[] fusesTexto = (String[]) variablesDeChip.get("FUSEblank");

            if (fusesTexto == null) {
                return new int[0];
            }

            int[] fuseBlank = new int[fusesTexto.length];

            for (int i = 0; i < fuseBlank.length; i++) {
                try {
                    fuseBlank[i] = Integer.parseUnsignedInt(fusesTexto[i], 16);
                } catch (NumberFormatException e) {
                    throw new ChipConfigurationException(
                            "Error al procesar fuse[" + i + "]: " + fusesTexto[i], e);
                }
            }

            return fuseBlank;

        } catch (ClassCastException e) {
            throw new ChipConfigurationException(
                    "Error de tipo al obtener fuses: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el ID del chip PIC.
     *
     * @return ID del chip
     * @throws ChipConfigurationException Si hay un error al procesar el ID
     */
    public int getIDPIC() throws ChipConfigurationException {

        try {
            String chipIdStr = variablesDeChip.get("ChipID").toString();
            int id = Integer.parseUnsignedInt(chipIdStr, 16);

            return id;

        } catch (NumberFormatException e) {
            throw new ChipConfigurationException(
                    "Error al procesar ID del PIC: " + e.getMessage(), e);
        }
    }

    public String getUbicacionPin1DelPic() {

        String ubicacion = "" + socketImagen.get("" + variablesDeChip.get("SocketImage"));

        return ubicacion;
    }

    /**
     * Verifica si el chip es compatible SOLO con ICSP. Si retorna true: el usuario
     * NO puede cambiar
     * el modo (switch deshabilitado) Si retorna false: el usuario puede cambiar
     * entre ICSP y
     * programación normal
     *
     * @return true si el chip es SOLO ICSP, false si es compatible con ambos modos
     * @throws ChipConfigurationException Si hay error al procesar
     */
    public boolean isICSPOnlyCompatible() throws ChipConfigurationException {
        return isICSPonly();
    }

    /** Obtiene el estado actual del modo ICSP */
    public boolean getICSPModoActual() {
        return icsp;
    }

    public String getNombreDelPic() {
        String chipIdStr = variablesDeChip.get("CHIPname").toString();

        return chipIdStr;
    }

    /**
     * Obtiene el numero de pines del chip basado en la propiedad SocketImage.
     *
     * @return Numero de pines (8, 14, 18, 28, 40) o 0 si no se reconoce.
     */
    public int getNumeroDePines() {
        String socketImage = (String) variablesDeChip.get("SocketImage");
        if (socketImage == null)
            return 0;

        String lowerValue = socketImage.toLowerCase();
        if (lowerValue.contains("40pin"))
            return 40;
        if (lowerValue.contains("28npin"))
            return 28;
        if (lowerValue.contains("18pin"))
            return 18;
        if (lowerValue.contains("14pin"))
            return 14;
        if (lowerValue.contains("8pin"))
            return 8;

        return 0;
    }

    // -------------------------------------------------------------------------
    // Metodos de fuses (unificados desde ChipinfoEntry)
    // -------------------------------------------------------------------------

    /**
     * Devuelve el conjunto de nombres de fuses disponibles para este chip.
     * Requiere que los fuses hayan sido cargados como mapa estructurado
     * por ChipinfoReader.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getFuseNames() {
        Object fusesObj = variablesDeChip.get("fuses");
        if (!(fusesObj instanceof Map))
            return new java.util.HashSet<>();
        Map<String, Map<String, List<FuseValue>>> fuses = (Map<String, Map<String, List<FuseValue>>>) fusesObj;
        return fuses.keySet();
    }

    /**
     * Devuelve un mapa estructurado de fuses: nombre → (opcion → List<FuseValue>).
     * Puede ser null si los fuses no se cargaron con el formato estructurado.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, List<FuseValue>>> getFusesMap() {
        Object fusesObj = variablesDeChip.get("fuses");
        if (!(fusesObj instanceof Map))
            return null;
        return (Map<String, Map<String, List<FuseValue>>>) fusesObj;
    }

    /**
     * Establece el mapa estructurado de fuses (lo llama ChipinfoReader).
     */
    public void setFusesMap(Map<String, Map<String, List<FuseValue>>> fusesMap) {
        variablesDeChip.put("fuses", fusesMap);
    }

    /**
     * Codifica un mapa nombre→opcion en una lista de palabras de configuracion
     * listas para enviar al programador.
     *
     * @param fuseDict Mapa con la configuracion deseada: nombre_fuse → opcion
     * @return Lista de enteros (palabras de config con los bits de fuse aplicados)
     * @throws FuseError Si algun fusible o valor es desconocido
     */
    @SuppressWarnings("unchecked")
    public List<Integer> encodeFuseData(Map<String, String> fuseDict) throws FuseError {
        // Base: copiar los valores blank de fuses
        int[] fuseBlankArr;
        try {
            fuseBlankArr = getFuseBlank();
        } catch (ChipConfigurationException e) {
            throw new FuseError("No se pudo obtener FUSEblank: " + e.getMessage());
        }
        List<Integer> result = new ArrayList<>();
        for (int v : fuseBlankArr)
            result.add(v);

        Map<String, Map<String, List<FuseValue>>> fuseParamList = getFusesMap();
        if (fuseParamList == null || fuseParamList.isEmpty()) {
            throw new FuseError("No hay configuraciones de fusibles disponibles para " + getNombreDelPic());
        }

        for (Map.Entry<String, String> entry : fuseDict.entrySet()) {
            String fuse = entry.getKey();
            String fuseValue = entry.getValue();

            String actualFuseName = findFuseName(fuseParamList, fuse);
            if (actualFuseName == null) {
                StringBuilder sb = new StringBuilder("Fusible desconocido: \"").append(fuse)
                        .append("\". Fusibles disponibles: ");
                int count = 0;
                for (String key : fuseParamList.keySet()) {
                    if (count++ > 0)
                        sb.append(", ");
                    sb.append(key);
                    if (count >= 10) {
                        sb.append("...");
                        break;
                    }
                }
                throw new FuseError(sb.toString());
            }

            Map<String, List<FuseValue>> fuseSettings = fuseParamList.get(actualFuseName);
            if (!fuseSettings.containsKey(fuseValue)) {
                StringBuilder sb = new StringBuilder("Valor invalido '").append(fuseValue)
                        .append("' para fuse '").append(actualFuseName)
                        .append("'. Valores disponibles: ");
                int count = 0;
                for (String key : fuseSettings.keySet()) {
                    if (count++ > 0)
                        sb.append(", ");
                    sb.append(key);
                }
                throw new FuseError(sb.toString());
            }

            result = indexwiseAnd(result, fuseSettings.get(fuseValue));
        }

        return result;
    }

    /**
     * Decodifica una lista de palabras de configuracion a su representacion
     * simbolica (nombre_fuse → opcion_activa).
     *
     * @param fuseValues Lista de enteros leidos del PIC
     * @return Mapa nombre_fuse → opcion_activa
     * @throws FuseError Si no hay fuses disponibles
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> decodeFuseData(List<Integer> fuseValues) throws FuseError {
        Map<String, Map<String, List<FuseValue>>> fuseParamList = getFusesMap();
        if (fuseParamList == null || fuseParamList.isEmpty()) {
            throw new FuseError("No hay configuraciones de fusibles disponibles para " + getNombreDelPic());
        }

        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Map<String, List<FuseValue>>> fuseParamEntry : fuseParamList.entrySet()) {
            String fuseParam = fuseParamEntry.getKey();
            Map<String, List<FuseValue>> fuseSettings = fuseParamEntry.getValue();

            String bestSetting = null;
            int bestScore = -1;

            for (Map.Entry<String, List<FuseValue>> settingEntry : fuseSettings.entrySet()) {
                int score = calculateMatchScore(fuseValues, settingEntry.getValue());
                if (score > bestScore) {
                    bestScore = score;
                    bestSetting = settingEntry.getKey();
                }
            }

            result.put(fuseParam, bestSetting != null ? bestSetting : "Unknown");
        }

        return result;
    }

    /**
     * Devuelve documentacion de los fuses disponibles para este chip.
     */
    @SuppressWarnings("unchecked")
    public String getFuseDoc() {
        Map<String, Map<String, List<FuseValue>>> fuseParamList = getFusesMap();
        if (fuseParamList == null || fuseParamList.isEmpty()) {
            return "No hay fusibles disponibles para " + getNombreDelPic();
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, List<FuseValue>>> entry : fuseParamList.entrySet()) {
            sb.append("'").append(entry.getKey()).append("' : (");
            boolean first = true;
            for (String setting : entry.getValue().keySet()) {
                if (!first)
                    sb.append(", ");
                sb.append("'").append(setting).append("'");
                first = false;
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    /** Busqueda flexible de nombre de fuse: exacta, case-insensitive, parcial. */
    private String findFuseName(
            Map<String, Map<String, List<FuseValue>>> fuseConfigs,
            String fuseName) {
        if (fuseConfigs.containsKey(fuseName))
            return fuseName;
        for (String key : fuseConfigs.keySet()) {
            if (key.equalsIgnoreCase(fuseName))
                return key;
        }
        String search = fuseName.toLowerCase();
        for (String key : fuseConfigs.keySet()) {
            String kl = key.toLowerCase();
            if (kl.contains(search) || search.contains(kl))
                return key;
        }
        return null;
    }

    /** Calcula cuantos bits coinciden entre fuseValues y settingValues. */
    private int calculateMatchScore(List<Integer> fuseValues, List<FuseValue> settingValues) {
        int score = 0;
        for (FuseValue fv : settingValues) {
            if (fv.index < fuseValues.size()) {
                score += Integer.bitCount(fuseValues.get(fv.index) & fv.value);
            }
        }
        return score;
    }

    /**
     * Aplica un AND por posicion entre la lista de fuses y los valores de la
     * opcion.
     */
    private List<Integer> indexwiseAnd(List<Integer> fuses, List<FuseValue> settingValues) {
        List<Integer> result = new ArrayList<>(fuses);
        for (FuseValue fv : settingValues) {
            if (fv.index < result.size()) {
                result.set(fv.index, result.get(fv.index) & fv.value);
            }
        }
        return result;
    }
}
