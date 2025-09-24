package com.diamon.chip;

import com.diamon.datos.DatosFuses;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.utilidades.LogManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase para manejar la configuración y propiedades de chips PIC.
 * Proporciona métodos para acceder a las características del chip,
 * configuraciones de programación y valores de fuses.
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
            Map<String, Object> fuses) throws ChipConfigurationException {
        
        LogManager.d(LogManager.Categoria.CHIP, "Creando instancia de ChipPic: " + CHIPname);
        
        // Validar parámetros obligatorios
        if (CHIPname == null || CHIPname.trim().isEmpty()) {
            LogManager.e(LogManager.Categoria.CHIP, "Nombre de chip no puede ser nulo o vacío");
            throw new ChipConfigurationException("Nombre de chip no puede ser nulo o vacío");
        }
        
        if (ROMsize == null || ROMsize.trim().isEmpty()) {
            LogManager.e(LogManager.Categoria.CHIP, "Tamaño de ROM no puede ser nulo o vacío para chip: " + CHIPname);
            throw new ChipConfigurationException("Tamaño de ROM no puede ser nulo o vacío");
        }
        
        if (CoreType == null || CoreType.trim().isEmpty()) {
            LogManager.e(LogManager.Categoria.CHIP, "Tipo de núcleo no puede ser nulo o vacío para chip: " + CHIPname);
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
        LogManager.d(LogManager.Categoria.CHIP, "Obteniendo tipo de núcleo en bits");
        
        try {
            String coreTypeStr = variablesDeChip.get("core_type").toString().toLowerCase();
            LogManager.d(LogManager.Categoria.CHIP, "Tipo de núcleo: " + coreTypeStr);
            
            Integer nucleoObj = tipoDeNucleo.get(coreTypeStr);
            if (nucleoObj == null) {
                LogManager.e(LogManager.Categoria.CHIP, "Tipo de núcleo no encontrado: " + coreTypeStr);
                throw new ChipConfigurationException("Tipo de núcleo no encontrado: " + coreTypeStr);
            }
            
            int nucleo = nucleoObj;
            LogManager.d(LogManager.Categoria.CHIP, "Valor numérico de núcleo: " + nucleo);

            if (nucleo == 1 || nucleo == 2 || nucleo == 13) {
                nucleo = 16;
            } else if (nucleo == 3 || nucleo == 5 || nucleo == 6 || nucleo == 7 || nucleo == 8 || nucleo == 9 || nucleo == 10 || nucleo == 12) {
                nucleo = 14;
            } else if (nucleo == 4 || nucleo == 11) {
                nucleo = 12;
            } else if (nucleo == 0) {
                nucleo = 14;
            } else {
                LogManager.e(LogManager.Categoria.CHIP, "Tipo de núcleo inválido: " + nucleo);
                throw new ChipConfigurationException("Tipo de núcleo inválido: " + nucleo);
            }
            
            LogManager.d(LogManager.Categoria.CHIP, "Tipo de núcleo en bits: " + nucleo);
            return nucleo;
            
        } catch (NumberFormatException e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al procesar tipo de núcleo: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al procesar tipo de núcleo: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si el chip solo puede ser programado mediante ICSP.
     *
     * @return true si el chip solo usa ICSP, false en caso contrario
     * @throws ChipConfigurationException Si hay un error al procesar la configuración ICSP
     */
    public boolean isICSPonly() throws ChipConfigurationException {
        LogManager.d(LogManager.Categoria.CHIP, "Verificando si el chip solo usa ICSP");
        
        try {
            String icspOnlyStr = variablesDeChip.get("ICSPonly").toString().toLowerCase();
            LogManager.d(LogManager.Categoria.CHIP, "Valor ICSPonly: " + icspOnlyStr);
            
            Boolean valor = respuestas.get(icspOnlyStr);
            if (valor == null) {
                LogManager.e(LogManager.Categoria.CHIP, "Valor ICSPonly inválido: " + icspOnlyStr);
                throw new ChipConfigurationException("Valor ICSPonly inválido: " + icspOnlyStr);
            }
            
            LogManager.d(LogManager.Categoria.CHIP, "ICSPonly: " + valor);
            return valor;
            
        } catch (Exception e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al verificar ICSPonly: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al verificar ICSPonly: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene las variables de programación del chip.
     *
     * @return Mapa con las variables de programación
     * @throws ChipConfigurationException Si hay un error al obtener las variables
     */
    public HashMap<String, String> getVariablesDeProgramacion() throws ChipConfigurationException {
        LogManager.d(LogManager.Categoria.CHIP, "Obteniendo variables de programación");
        
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
            variablesProgramacion.put("flag_calibration_value_in_ROM",
                    "" + variablesDeChip.get("flag_calibration_value_in_ROM"));
            variablesProgramacion.put("flag_band_gap_fuse",
                    "" + variablesDeChip.get("flag_band_gap_fuse"));
            
            // Flag 18F single panel access mode
            String coreType = variablesDeChip.get("core_type").toString().toLowerCase();
            Integer coreTypeValue = tipoDeNucleo.get(coreType);
            Integer bit16aValue = tipoDeNucleo.get("bit16_a");
            
            if (coreTypeValue != null && bit16aValue != null) {
                variablesProgramacion.put("flag_18f_single_panel_access_mode",
                        "" + coreTypeValue.equals(bit16aValue));
            } else {
                LogManager.w(LogManager.Categoria.CHIP, "No se pudo determinar flag_18f_single_panel_access_mode");
                variablesProgramacion.put("flag_18f_single_panel_access_mode", "false");
            }
            
            // Flag VCC VPP delay
            String powerSequence = variablesDeChip.get("power_sequence_str").toString().toLowerCase();
            Boolean vccVppDelay = vccVppTiempo.get(powerSequence);
            
            if (vccVppDelay != null) {
                variablesProgramacion.put("flag_vcc_vpp_delay", "" + vccVppDelay);
            } else {
                LogManager.w(LogManager.Categoria.CHIP, "No se pudo determinar flag_vcc_vpp_delay para secuencia: " + powerSequence);
                variablesProgramacion.put("flag_vcc_vpp_delay", "false");
            }
            
            LogManager.d(LogManager.Categoria.CHIP, "Variables de programación obtenidas: " + variablesProgramacion.size());
            return variablesProgramacion;
            
        } catch (Exception e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al obtener variables de programación: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al obtener variables de programación: " + e.getMessage(), e);
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
        LogManager.d(LogManager.Categoria.CHIP, "Obteniendo retardo de programación");
        
        try {
            String delayStr = variablesDeChip.get("program_delay").toString();
            int valor = Integer.parseUnsignedInt(delayStr, 10);
            
            LogManager.d(LogManager.Categoria.CHIP, "Retardo de programación: " + valor);
            return valor;
            
        } catch (NumberFormatException e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al procesar retardo de programación: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al procesar retardo de programación: " + e.getMessage(), e);
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

    /**
     * Obtiene los valores blank de fuses como array de enteros.
     *
     * @return Array con los valores blank de fuses
     * @throws ChipConfigurationException Si hay un error al procesar los fuses
     */
    public int[] getFuseBlack() {
        LogManager.d(LogManager.Categoria.CHIP, "Obteniendo valores blank de fuses");
        
        try {
            String[] fusesTexto = (String[]) variablesDeChip.get("FUSEblank");
            
            if (fusesTexto == null) {
                LogManager.w(LogManager.Categoria.CHIP, "No hay fuses configurados");
                return new int[0];
            }
            
            int[] fuseBlank = new int[fusesTexto.length];
            
            for (int i = 0; i < fuseBlank.length; i++) {
                try {
                    fuseBlank[i] = Integer.parseUnsignedInt(fusesTexto[i], 16);
                    LogManager.d(LogManager.Categoria.CHIP, "Fuse[" + i + "]: 0x" + Integer.toHexString(fuseBlank[i]));
                } catch (NumberFormatException e) {
                    LogManager.e(LogManager.Categoria.CHIP, "Error al procesar fuse[" + i + "]: " + fusesTexto[i]);
                    throw new ChipConfigurationException("Error al procesar fuse[" + i + "]: " + fusesTexto[i], e);
                }
            }
            
            LogManager.d(LogManager.Categoria.CHIP, "Valores blank de fuses obtenidos: " + fuseBlank.length + " fuses");
            return fuseBlank;
            
        } catch (ClassCastException e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error de tipo al obtener fuses: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error de tipo al obtener fuses: " + e.getMessage(), e);
        }
    }

    private Map<String, ArrayList<String[]>> getFuses() {

        Map<String, ArrayList<String[]>> fusess =
                (Map<String, ArrayList<String[]>>) variablesDeChip.get("fuses");

        return fusess;
    }

    private ArrayList<String> getListaDeFuses() {

        List<String> lista = new ArrayList<String>();

        StringBuffer listaFuse = new StringBuffer();

        for (int i = 0; i < getFuses().get("FUSES").size(); i++) {

            for (int j = 0; j < getFuses().get("FUSES").get(i).length; j++) {

                listaFuse.append(getFuses().get("FUSES").get(i)[j].toString() + " ");
            }

            lista.add(new String(listaFuse.toString()));

            listaFuse.setLength(0);
        }

        return (ArrayList<String>) lista;
    }

    public ArrayList<DatosFuses> getValoresDeFuses() {

        ArrayList<DatosFuses> datosProsesados = new ArrayList<DatosFuses>();

        for (String line : getListaDeFuses()) {

            datosProsesados.add(parseLine(line));
        }

        return datosProsesados;
    }

    /**
     * Parsea una línea de configuración de fuses.
     *
     * @param line Línea de configuración de fuses
     * @return Objeto DatosFuses con la información parseada
     * @throws ChipConfigurationException Si hay un error al parsear la línea
     */
    private DatosFuses parseLine(String line) {
        LogManager.d(LogManager.Categoria.CHIP, "Parseando línea de fuses: " + line);
        
        if (line == null || line.trim().isEmpty()) {
            LogManager.w(LogManager.Categoria.CHIP, "Línea de fuses vacía");
            return new DatosFuses();
        }
        
        try {
            DatosFuses datosFuses = new DatosFuses();

            // Expresión regular para capturar el texto dentro de comillas y valores hexadecimales
            Pattern pattern = Pattern.compile("\"([^\"]+)\"=([A-F0-9]+)");
            Matcher matcher = pattern.matcher(line);

            // Buscar el título principal entre las primeras comillas
            int firstQuote = line.indexOf("\"");
            int secondQuote = line.indexOf("\"", firstQuote + 1);
            
            if (firstQuote == -1 || secondQuote == -1) {
                LogManager.e(LogManager.Categoria.CHIP, "Formato de línea inválido, no se encontraron comillas: " + line);
                throw new ChipConfigurationException("Formato de línea inválido: " + line);
            }
            
            String mainTitle = line.substring(firstQuote + 1, secondQuote);
            datosFuses.setTitulo(mainTitle);
            LogManager.d(LogManager.Categoria.CHIP, "Título de fuse: " + mainTitle);

            // Iterar sobre los pares "Texto"=HEX
            while (matcher.find()) {
                String description = matcher.group(1);
                String hexValue = matcher.group(2);

                datosFuses.setDescription(description);
                datosFuses.setValor(Integer.parseUnsignedInt(hexValue, 16));
                
                LogManager.d(LogManager.Categoria.CHIP, "Fuse encontrado: " + description + " = 0x" + hexValue);
            }

            return datosFuses;
            
        } catch (NumberFormatException e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al parsear valor hexadecimal: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al parsear valor hexadecimal: " + e.getMessage(), e);
        } catch (Exception e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al parsear línea de fuses: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al parsear línea de fuses: " + e.getMessage(), e);
        }
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

    public int getSecuenciaDeEncendido() {

        int secuencia = Integer.parseInt("" + variablesDeChip.get("power_sequence"));

        if (icsp) {

            if (secuencia == 2) {

                secuencia = 1;

            } else if (secuencia == 4) {

                secuencia = 3;
            }

        } else {

            secuencia = Integer.parseInt("" + variablesDeChip.get("power_sequence"));
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
    public int getTamanoROM() {
        LogManager.d(LogManager.Categoria.CHIP, "Obteniendo tamaño de ROM");
        
        try {
            String romSizeStr = variablesDeChip.get("rom_size").toString();
            int tamano = Integer.parseUnsignedInt(romSizeStr, 16);
            
            LogManager.d(LogManager.Categoria.CHIP, "Tamaño de ROM: 0x" + romSizeStr + " = " + tamano + " bytes");
            return tamano;
            
        } catch (NumberFormatException e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al procesar tamaño de ROM: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al procesar tamaño de ROM: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el tamaño de la EEPROM del chip.
     *
     * @return Tamaño de la EEPROM en bytes
     * @throws ChipConfigurationException Si hay un error al procesar el tamaño
     */
    public int getTamanoEEPROM() {
        LogManager.d(LogManager.Categoria.CHIP, "Obteniendo tamaño de EEPROM");
        
        try {
            String eepromSizeStr = variablesDeChip.get("eeprom_size").toString();
            int tamano = Integer.parseUnsignedInt(eepromSizeStr, 16);
            
            LogManager.d(LogManager.Categoria.CHIP, "Tamaño de EEPROM: 0x" + eepromSizeStr + " = " + tamano + " bytes");
            return tamano;
            
        } catch (NumberFormatException e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al procesar tamaño de EEPROM: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al procesar tamaño de EEPROM: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el ID del chip PIC.
     *
     * @return ID del chip
     * @throws ChipConfigurationException Si hay un error al procesar el ID
     */
    public int getIDPIC() {
        LogManager.d(LogManager.Categoria.CHIP, "Obteniendo ID del PIC");
        
        try {
            String chipIdStr = variablesDeChip.get("ChipID").toString();
            int id = Integer.parseUnsignedInt(chipIdStr, 16);
            
            LogManager.d(LogManager.Categoria.CHIP, "ID del PIC: 0x" + chipIdStr + " = " + id);
            return id;
            
        } catch (NumberFormatException e) {
            LogManager.e(LogManager.Categoria.CHIP, "Error al procesar ID del PIC: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al procesar ID del PIC: " + e.getMessage(), e);
        }
    }

    public String getUbicacionPin1DelPic() {

        String ubicacion = "" + socketImagen.get("" + variablesDeChip.get("SocketImage"));

        return ubicacion;
    }
}
