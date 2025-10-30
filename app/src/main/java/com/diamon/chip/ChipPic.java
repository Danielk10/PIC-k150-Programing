package com.diamon.chip;

import com.diamon.excepciones.ChipConfigurationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase para manejar la configuración y propiedades de chips PIC. Proporciona métodos para acceder
 * a las características del chip, configuraciones de programación y valores de fuses.
 */
public class ChipPic {

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
     * @param CHIPname Nombre del chip PIC
     * @param INCLUDEr Archivo de inclusión
     * @param SocketImage Imagen del socket
     * @param EraseMode Modo de borrado
     * @param FlashChip Indica si es un chip flash
     * @param PowerSequence Secuencia de encendido
     * @param ProgramDelay Retardo de programación
     * @param ProgramTries Intentos de programación
     * @param OverProgram Sobrecarga de programación
     * @param CoreType Tipo de núcleo
     * @param ROMsize Tamaño de ROM en hexadecimal
     * @param EEPROMsize Tamaño de EEPROM en hexadecimal
     * @param FUSEblank Valores blank de fuses
     * @param CPwarn Advertencia de protección de código
     * @param CALword Palabra de calibración
     * @param BandGap Band gap
     * @param ICSPonly Indica si solo usa ICSP
     * @param ChipID ID del chip en hexadecimal
     * @param fuses Mapa de fuses del chip
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
     * @throws ChipConfigurationException Si hay un error al procesar la configuración ICSP
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
            String powerSequence =
                    variablesDeChip.get("power_sequence_str").toString().toLowerCase();
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
        boolean valor =
                respuestas.get(
                        variablesDeChip
                                .get("flag_calibration_value_in_ROM")
                                .toString()
                                .toLowerCase());

        return valor;
    }

    public boolean isFlagBandGap() {
        boolean valor =
                respuestas.get(variablesDeChip.get("flag_band_gap_fuse").toString().toLowerCase());

        return valor;
    }

    public boolean isFlag18fSingle() {

        String dato =
                ""
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

        boolean valor =
                vccVppTiempo.get(("" + variablesDeChip.get("power_sequence_str")).toLowerCase());

        return valor;
    }

    public int getTipoDeNucleoDelPic() {

        int nucleo =
                Integer.parseInt(
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

        boolean valido = (Integer.parseInt("" + variablesDeChip.get("eeprom_size")) != 0);

        return valido;
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
    public int[] getFuseBlack() throws ChipConfigurationException {

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
     * Verifica si el chip es compatible SOLO con ICSP. Si retorna true: el usuario NO puede cambiar
     * el modo (switch deshabilitado) Si retorna false: el usuario puede cambiar entre ICSP y
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
}
