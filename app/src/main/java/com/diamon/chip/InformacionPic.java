package com.diamon.chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InformacionPic {

    private Map<String, Object> variablesDeChip;

    private HashMap<String, String> variablesProgramacion;

    private HashMap<String, Integer> secuenciaEncedido;

    private HashMap<String, Boolean> vccVppTiempo;

    private HashMap<String, String> socketImagen;

    private HashMap<String, Integer> tipoDeNucleo;

    private HashMap<String, Boolean> respuestas;

    public InformacionPic(
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
            Map<String, Object> fuses) {

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

        respuestas = new HashMap<String, Boolean>();

        respuestas.put("y", true);

        respuestas.put("1", true);

        respuestas.put("n", false);

        respuestas.put("0", false);
    }

    public HashMap<String, String> getVariablesProgramacion() {

        variablesProgramacion.put("rom_size", "" + variablesDeChip.get("rom_size"));

        variablesProgramacion.put("eeprom_size", "" + variablesDeChip.get("eeprom_size"));

        variablesProgramacion.put("core_type", "" + variablesDeChip.get("core_type"));

        variablesProgramacion.put(
                "flag_calibration_value_in_ROM",
                "" + variablesDeChip.get("flag_calibration_value_in_ROM"));

        variablesProgramacion.put(
                "flag_band_gap_fuse", "" + variablesDeChip.get("flag_band_gap_fuse"));

        variablesProgramacion.put(
                "flag_18f_single_panel_access_mode",
                ""
                        + tipoDeNucleo
                                .get(variablesDeChip.get("core_type").toString().toLowerCase())
                                .toString()
                                .equals("" + tipoDeNucleo.get("bit16_a")));

        variablesProgramacion.put(
                "flag_vcc_vpp_delay",
                ""
                        + vccVppTiempo.get(
                                ("" + variablesDeChip.get("power_sequence_str")).toLowerCase()));

        variablesProgramacion.put("program_delay", "" + variablesDeChip.get("program_delay"));

        variablesProgramacion.put("power_sequence", "" + variablesDeChip.get("power_sequence"));

        variablesProgramacion.put("erase_mode", "" + variablesDeChip.get("erase_mode"));

        variablesProgramacion.put("program_retries", "" + variablesDeChip.get("program_tries"));

        variablesProgramacion.put("over_program", "" + variablesDeChip.get("over_program"));

        return variablesProgramacion;
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

    public int getProgramDelay() {
        int valor = Integer.parseUnsignedInt("" + variablesDeChip.get("program_delay"), 10);

        return valor;
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

    public String[] getFuseBlack() {

        String[] fuseBlank = (String[]) variablesDeChip.get("FUSEblank");

        return fuseBlank;
    }

    public Map<String, ArrayList<String[]>> getFuses() {

        Map<String, ArrayList<String[]>> fusess =
                (Map<String, ArrayList<String[]>>) variablesDeChip.get("fuses");

        return fusess;
    }

    public int getTipoNucleoBit() {

        int nucleo =
                Integer.parseInt(
                        ""
                                + tipoDeNucleo.get(
                                        ""
                                                + variablesDeChip
                                                        .get("core_type")
                                                        .toString()
                                                        .toLowerCase()));

        if (nucleo == 1 || nucleo == 2) {

            nucleo = 16;

        } else if (nucleo == 3
                || nucleo == 5
                || nucleo == 6
                || nucleo == 7
                || nucleo == 8
                || nucleo == 9
                || nucleo == 10) {

            nucleo = 14;

        } else if (nucleo == 4) {

            nucleo = 12;
        }

        return nucleo;
    }

    public int getTipoNucleoPic() {

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

    public int getSecuenciaEncendido() {
        int secuencia = Integer.parseInt("" + variablesDeChip.get("power_sequence"));

        return secuencia;
    }

    public boolean isTamanoValidoEEPROM() {

        boolean valido = (Integer.parseInt("" + variablesDeChip.get("eeprom_size")) != 0);

        return valido;
    }

    public int getTamanoROM() {

        int tamano = Integer.parseUnsignedInt("" + variablesDeChip.get("rom_size"), 16);

        return tamano;
    }

    public int getTamanoEEPROM() {

        int tamano = Integer.parseUnsignedInt("" + variablesDeChip.get("eeprom_size"), 16);

        return tamano;
    }

    public int getIDPIC() {

        int id = Integer.parseUnsignedInt("" + variablesDeChip.get("ChipID"), 16);

        return id;
    }

    public String getUbicacionPin1() {
        String ubicacion = "" + socketImagen.get("" + variablesDeChip.get("SocketImage"));

        return ubicacion;
    }

    public Map<String, String> decodeFuseData(int[] fuseValues) throws Exception {
        Map<String, String> decoded = new HashMap<>();
        Map<String, Map<String, int[][]>> fuses =
                (Map<String, Map<String, int[][]>>) variablesDeChip.get("fuses");
        int[] fuseBlank = (int[]) variablesDeChip.get("FUSEblank");

        for (String fuse : fuses.keySet()) {
            Map<String, int[][]> settings = fuses.get(fuse);
            int[] bestValue = fuseBlank.clone();
            boolean fuseIdentified = false;

            for (String settingName : settings.keySet()) {
                int[][] settingValues = settings.get(settingName);

                // Comparamos el resultado de indexwiseAnd con fuseValues
                if (arraysEqual(indexwiseAnd(fuseValues, settingValues), fuseValues)) {
                    // Actualizamos el mejor valor si también coincide
                    if (arraysEqual(indexwiseAnd(bestValue, settingValues), fuseValues)) {
                        decoded.put(fuse, settingName);
                        bestValue = indexwiseAnd(bestValue, settingValues);
                        fuseIdentified = true;
                    }
                }
            }

            if (!fuseIdentified) {
                throw new Exception("Could not identify fuse setting for " + fuse);
            }
        }
        return decoded;
    }

    public static int[] indexwiseAnd(int[] values, int[][] settings) {
        int[] result =
                values.clone(); // Clonamos el array original para no modificarlo directamente
        for (int[] setting : settings) {
            int index = setting[0]; // Primer valor es el índice
            int value = setting[1]; // Segundo valor es el valor a aplicar
            result[index] &= value; // Operación bit a bit AND
        }
        return result;
    }

    private boolean arraysEqual(int[] array1, int[] array2) {
        if (array1.length != array2.length) return false;
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) return false;
        }
        return true;
    }
}
