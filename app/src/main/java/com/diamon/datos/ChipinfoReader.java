package com.diamon.datos;

import android.app.Activity;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee y procesa el archivo chipinfo.cid creando objetos ChipPic.
 *
 * <p>
 * Formato del archivo (un bloque por chip, separado por linea vacia):
 * </p>
 * 
 * <pre>
 *   CHIPname=16F628A
 *   INCLUDE=Y
 *   SocketImage=18pin
 *   EraseMode=3
 *   FlashChip=Y
 *   PowerSequence=Vpp2Vcc
 *   ProgramDelay=80
 *   ProgramTries=1
 *   OverProgram=0
 *   CoreType=bit14_B
 *   ROMsize=000800
 *   EEPROMsize=00000080
 *   FUSEblank=3FFF               (o "2700 0F0F ..." para PIC18 con multiples palabras)
 *   CPwarn=N
 *   CALword=N
 *   BandGap=N
 *   ICSPonly=N
 *   ChipID=07E0
 *   LIST1 FUSE1 "WDT" "Enabled"=3FFF "Disabled"=3FF7
 *   LIST2 FUSE1 "PWRTE" "Disabled"=3FFF "Enabled"=3FEF
 *   LIST3 FUSE2 "BOR" "On"=FFFF "Off"=FFFD    (FUSE2 = segunda palabra config, solo PIC18)
 * </pre>
 *
 * <p>
 * ChipPic.java recibe todos los campos como String y hace sus propias
 * conversiones.
 * </p>
 * <p>
 * La unica excepcion es FUSEblank (String[]) y fusesMap (Map estructurado).
 * </p>
 */
public class ChipinfoReader {

    // Regex para lineas de asignacion: CAMPO=valor
    // Equivalente a assignment_regexp del parser Python de referencia.
    private static final Pattern ASSIGNMENT_REGEXP = Pattern.compile("^(\\S+)\\s*=\\s*(.*)\\s*$");

    // Regex para lineas LIST de fusibles:
    // LIST<n> FUSE<w> "nombre" "opcion1"=HHHH "opcion2"=HHHH ...
    // Grupo 1: indice de la palabra de configuracion (1..7)
    // Grupo 2: nombre del fusible
    // Grupo 3: resto de la linea con pares "opcion"=valor
    private static final Pattern FUSE_LIST_REGEXP = Pattern.compile("^LIST\\d+\\s+FUSE(\\d)\\s+\"([^\"]*)\"\\s*(.*)$");

    // Regex para pares "opcion"=HHHH dentro de una linea LIST
    // El valor puede ser HHHH&HHHH para fusibles que afectan multiples palabras de
    // config
    private static final Pattern FUSE_VALUE_REGEXP = Pattern
            .compile("\"([^\"]*)\"\\s*=\\s*([0-9a-fA-F]+(?:&[0-9a-fA-F]+)*)");

    // Nombres de campo en chipinfo.cid que se mapean al constructor de ChipPic
    // Los campos desconocidos se ignoran silenciosamente
    private static final Map<String, String> KEY_MAP;
    static {
        KEY_MAP = new HashMap<>();
        KEY_MAP.put("CHIPname", "CHIPname");
        KEY_MAP.put("INCLUDE", "INCLUDE");
        KEY_MAP.put("SocketImage", "SocketImage");
        KEY_MAP.put("EraseMode", "EraseMode");
        KEY_MAP.put("FlashChip", "FlashChip");
        KEY_MAP.put("PowerSequence", "PowerSequence");
        KEY_MAP.put("ProgramDelay", "ProgramDelay");
        KEY_MAP.put("ProgramTries", "ProgramTries");
        KEY_MAP.put("OverProgram", "OverProgram");
        KEY_MAP.put("CoreType", "CoreType");
        KEY_MAP.put("ROMsize", "ROMsize");
        KEY_MAP.put("EEPROMsize", "EEPROMsize");
        KEY_MAP.put("FUSEblank", "FUSEblank");
        KEY_MAP.put("CPwarn", "CPwarn");
        KEY_MAP.put("CALword", "CALword");
        KEY_MAP.put("BandGap", "BandGap");
        KEY_MAP.put("ICSPonly", "ICSPonly");
        KEY_MAP.put("ChipID", "ChipID");
        // Alias que aparece en algunas versiones del archivo
        KEY_MAP.put("KITSRUS.COM", "SocketImage");
        // Campos ignorados (presentes en algunas versiones del archivo)
        KEY_MAP.put("ProgramFlag2", null);
        KEY_MAP.put("PanelSizing", null);
    }

    private final Map<String, ChipPic> chipEntries;
    private final ArrayList<String> modelosPic;

    /**
     * Constructor: lee el archivo chipinfo.cid desde los assets de Android
     * y construye el mapa de chips.
     *
     * @param actividad Actividad Android para acceder a los assets
     * @throws ChipConfigurationException si ocurre un error critico al leer el
     *                                    archivo
     */
    public ChipinfoReader(Activity actividad) throws ChipConfigurationException {
        if (actividad == null) {
            throw new ChipConfigurationException("La actividad no puede ser nula");
        }

        chipEntries = new HashMap<>();
        modelosPic = new ArrayList<>();

        try {
            final List<String> lines = new DatosDePic(actividad).getInformacionPic();
            final int totalLines = lines.size();

            // 'block' acumula los campos del chip actual como Strings
            // Se crea al encontrar la primera linea no-vacia de un bloque
            Map<String, String> block = null;
            // 'fusesBlock' acumula los fusibles estructurados del chip actual
            Map<String, Map<String, List<ChipPic.FuseValue>>> fusesBlock = null;

            for (int i = 0; i < totalLines; i++) {
                final String raw = lines.get(i);
                final String line = (raw != null) ? raw.trim() : "";
                final boolean isLast = (i == totalLines - 1);

                if (!line.isEmpty()) {
                    // Primera linea de un bloque nuevo
                    if (block == null) {
                        block = new HashMap<>();
                        fusesBlock = new HashMap<>();
                    }
                    parsearLinea(block, fusesBlock, line);
                }

                // Fin del bloque: linea vacia o ultima linea
                if (block != null && (line.isEmpty() || isLast)) {
                    guardarBloque(block, fusesBlock);
                    block = null;
                    fusesBlock = null;
                }
            }

        } catch (Exception e) {
            throw new ChipConfigurationException(
                    "Error al inicializar ChipinfoReader: " + e.getMessage(), e);
        }
    }

    /**
     * Parsea una linea y actualiza el bloque.
     * Las lineas de asignacion (CAMPO=valor) actualizan el mapa de Strings.
     * Las lineas LIST actualizan el mapa de fusibles estructurado.
     */
    private void parsearLinea(
            Map<String, String> block,
            Map<String, Map<String, List<ChipPic.FuseValue>>> fusesBlock,
            String line) {

        // Intento 1: linea de asignacion CAMPO=valor
        Matcher assignMatcher = ASSIGNMENT_REGEXP.matcher(line);
        if (assignMatcher.matches()) {
            String rawKey = assignMatcher.group(1);
            String value = assignMatcher.group(2).trim();

            String key = KEY_MAP.get(rawKey);
            if (key != null) {
                // key == null solo para campos ignorados (ProgramFlag2, PanelSizing)
                block.put(key, value);
            }
            return;
        }

        // Intento 2: linea LIST de fusibles
        Matcher listMatcher = FUSE_LIST_REGEXP.matcher(line);
        if (listMatcher.matches()) {
            // fuseWordIndex es 0-based: FUSE1 → 0, FUSE2 → 1, etc.
            int fuseWordIndex = Integer.parseInt(listMatcher.group(1)) - 1;
            String fuseName = listMatcher.group(2);
            String valoresStr = listMatcher.group(3);

            if (fuseName == null || fuseName.isEmpty())
                return;

            // Obtener o crear el mapa de opciones para este fusible
            Map<String, List<ChipPic.FuseValue>> fuseOptions = fusesBlock.get(fuseName);
            if (fuseOptions == null) {
                fuseOptions = new HashMap<>();
                fusesBlock.put(fuseName, fuseOptions);
            }

            // Parsear cada par "opcion"=HHHH en la linea
            Matcher valueMatcher = FUSE_VALUE_REGEXP.matcher(
                    valoresStr != null ? valoresStr : "");
            while (valueMatcher.find()) {
                String opcion = valueMatcher.group(1);
                String rawValues = valueMatcher.group(2);

                // Un valor puede ser HHHH&HHHH para opciones que afectan
                // multiples palabras de configuracion consecutivas.
                // Equivalente al split('&') del parser Python.
                String[] partes = rawValues.split("&");
                List<ChipPic.FuseValue> fuseValueList = new ArrayList<>();
                for (int i = 0; i < partes.length; i++) {
                    try {
                        int wordValue = Integer.parseInt(partes[i].trim(), 16);
                        fuseValueList.add(
                                new ChipPic.FuseValue(fuseWordIndex + i, wordValue));
                    } catch (NumberFormatException ignored) {
                    }
                }
                fuseOptions.put(opcion, fuseValueList);
            }
        }
        // Lineas no reconocidas se ignoran silenciosamente
    }

    /**
     * Crea un objeto ChipPic con los datos del bloque y lo registra en el mapa.
     * El constructor de ChipPic recibe los valores como Strings y hace sus
     * propias conversiones internas.
     */
    private void guardarBloque(
            Map<String, String> block,
            Map<String, Map<String, List<ChipPic.FuseValue>>> fusesBlock) {

        String chipName = block.get("CHIPname");
        if (chipName == null || chipName.isEmpty())
            return;

        String romSize = block.get("ROMsize");
        String coreType = block.get("CoreType");
        if (romSize == null || romSize.isEmpty())
            return;
        if (coreType == null || coreType.isEmpty())
            return;

        // FUSEblank: separar por espacios en blanco → String[]
        // ChipPic.getFuseBlack() los parsea como hex internamente.
        String fuseBlankRaw = block.get("FUSEblank");
        String[] fuseBlankArr = null;
        if (fuseBlankRaw != null && !fuseBlankRaw.isEmpty()) {
            fuseBlankArr = fuseBlankRaw.trim().split("\\s+");
        }

        // El constructor de ChipPic recibe todo como String.
        // El parametro 'fuses' es un Map<String,Object> legado que se usa
        // solo como contenedor; el mapa estructurado se establece via setFusesMap().
        Map<String, Object> fusesLegacy = new HashMap<>();

        try {
            ChipPic chipPic = new ChipPic(
                    chipName,
                    block.getOrDefault("INCLUDE", "Y"),
                    block.getOrDefault("SocketImage", "0pin"),
                    block.getOrDefault("EraseMode", "0"),
                    block.getOrDefault("FlashChip", "N"),
                    block.getOrDefault("PowerSequence", "VccVpp1"),
                    block.getOrDefault("ProgramDelay", "1"),
                    block.getOrDefault("ProgramTries", "1"),
                    block.getOrDefault("OverProgram", "0"),
                    coreType,
                    romSize,
                    block.getOrDefault("EEPROMsize", "00000000"),
                    fuseBlankArr,
                    block.getOrDefault("CPwarn", "N"),
                    block.getOrDefault("CALword", "N"),
                    block.getOrDefault("BandGap", "N"),
                    block.getOrDefault("ICSPonly", "N"),
                    block.getOrDefault("ChipID", "FFFF"),
                    fusesLegacy);

            // Inyectar el mapa estructurado de fusibles para encode/decode
            if (!fusesBlock.isEmpty()) {
                chipPic.setFusesMap(new HashMap<>(fusesBlock));
            }

            modelosPic.add(chipName);
            chipEntries.put(chipName, chipPic);

        } catch (Exception e) {
            // Si el chip tiene datos invalidos, simplemente se omite
        }
    }

    // -------------------------------------------------------------------------
    // API publica
    // -------------------------------------------------------------------------

    /**
     * Obtiene el objeto ChipPic para el chip indicado.
     *
     * @param chipName Nombre del chip (exacto, tal como aparece en el archivo)
     * @return ChipPic o null si no existe
     */
    public ChipPic getChipEntry(String chipName) {
        if (chipName == null || chipName.isEmpty())
            return null;
        return chipEntries.get(chipName);
    }

    /**
     * Lista de modelos PIC disponibles, ordenada alfabetica y numericamente.
     * Orden: primero por familia (PIC10 &lt; PIC12 &lt; PIC16 &lt; PIC18),
     * luego por numero de modelo de menor a mayor.
     *
     * @return Lista con los nombres de los modelos PIC disponibles
     */
    public ArrayList<String> getModelosPic() {
        Collections.sort(modelosPic, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return comparePicNames(a, b);
            }
        });
        return modelosPic;
    }

    /**
     * Comparador alfabetico-numerico para nombres de PIC.
     * Cuando se encuentra una secuencia de digitos, se compara como numero entero
     * para evitar que "PIC16F10" aparezca despues de "PIC16F9".
     */
    private int comparePicNames(String a, String b) {
        int ia = 0, ib = 0;
        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia);
            char cb = b.charAt(ib);
            boolean aDigit = Character.isDigit(ca);
            boolean bDigit = Character.isDigit(cb);

            if (aDigit && bDigit) {
                int startA = ia, startB = ib;
                while (ia < a.length() && Character.isDigit(a.charAt(ia)))
                    ia++;
                while (ib < b.length() && Character.isDigit(b.charAt(ib)))
                    ib++;
                int numA = Integer.parseInt(a.substring(startA, ia));
                int numB = Integer.parseInt(b.substring(startB, ib));
                if (numA != numB)
                    return Integer.compare(numA, numB);
            } else {
                int cmp = Character.compare(
                        Character.toUpperCase(ca),
                        Character.toUpperCase(cb));
                if (cmp != 0)
                    return cmp;
                ia++;
                ib++;
            }
        }
        return Integer.compare(a.length(), b.length());
    }
}
