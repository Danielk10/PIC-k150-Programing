package com.diamon.datos;

import android.content.Context;
import android.content.res.AssetManager;

import com.diamon.chip.ChipinfoEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChipinfoReader - Lee y parsea archivos de información de chips PIC
 *
 * <p>VERSIÓN FINAL CORREGIDA: 100% Compatible con Android API < 26 - Sin getOrDefault() (requiere
 * API 24) - Sin computeIfAbsent() (requiere API 24) - Sin named groups en regex (requiere API 26) -
 * Compatible con Android 4.0+ (API 14+)
 *
 * <p>Esta clase es la migración de la clase Python "Chipinfo_Reader" y se encarga de: - Leer
 * archivos chipinfo.txt/cid desde assets o filesystem - Parsear el formato de texto con expresiones
 * regulares - Convertir los datos a estructuras Java - Crear objetos ChipinfoEntry para cada chip
 *
 * <p>El archivo chipinfo contiene información de configuración de múltiples chips PIC, con sus
 * parámetros de programación y configuración de fusibles.
 *
 * <p>Formato del archivo: - Líneas de asignación: Campo=Valor - Líneas de fusibles: LISTn FUSEm
 * "nombre" "opcion1"=valor1 "opcion2"=valor2 - Cada chip inicia con CHIPname=XXXX
 */
public class ChipFusesReader {

    /** Excepción personalizada para errores de formato en el archivo */
    public static class FormatError extends Exception {
        public FormatError(String message) {
            super(message);
        }
    }

    // Diccionario de mapeo: string booleano -> valor boolean
    // Convierte las representaciones 'Y'/'N' y '1'/'0' a booleanos
    private static final Map<String, Boolean> BOOLEAN_DICT = new HashMap<>();

    static {
        BOOLEAN_DICT.put("y", true);
        BOOLEAN_DICT.put("1", true);
        BOOLEAN_DICT.put("n", false);
        BOOLEAN_DICT.put("0", false);
    }

    // Diccionario de mapeo: tipo de núcleo string -> código numérico
    // Identifica el tipo de arquitectura del PIC
    private static final Map<String, Integer> CORE_TYPE_DICT = new HashMap<>();

    static {
        CORE_TYPE_DICT.put("bit16_a", 1); // PIC18F con acceso a panel simple
        CORE_TYPE_DICT.put("bit16_b", 2); // PIC18F estándar
        CORE_TYPE_DICT.put("bit14_g", 3); // PIC16F mejorado
        CORE_TYPE_DICT.put("bit12_a", 4); // PIC12 original
        CORE_TYPE_DICT.put("bit14_a", 5); // PIC16 clásico
        CORE_TYPE_DICT.put("bit14_b", 6); // PIC16F con EEPROM
        CORE_TYPE_DICT.put("bit14_c", 7); // PIC16F moderno
        CORE_TYPE_DICT.put("bit14_d", 8); // PIC16F variante D
        CORE_TYPE_DICT.put("bit14_e", 9); // PIC16F variante E
        CORE_TYPE_DICT.put("bit14_f", 10); // PIC16F variante F
        CORE_TYPE_DICT.put("bit12_b", 11); // PIC12 mejorado
        CORE_TYPE_DICT.put("bit14_h", 12); // PIC16F variante H
        CORE_TYPE_DICT.put("bit16_c", 13); // PIC18F variante C
        // Agregado para compatibilidad con chipinfo.txt
        CORE_TYPE_DICT.put("newf12b", 11); // Alias para bit12_b
    }

    // Mapa que almacena todas las entradas de chips leídas
    // Key: nombre del chip en minúsculas, Value: datos del chip
    private final Map<String, Map<String, Object>> chipEntries;

    // Expresiones regulares compiladas para parsear el archivo

    // Patrón para líneas de asignación: CAMPO=VALOR
    private static final Pattern ASSIGNMENT_REGEXP = Pattern.compile("^(\\w+)\\s*=\\s*(.*)\\s*$");

    // Patrón para valores de fusible: "nombre"=valor
    // Soporta valores múltiples separados por &: valor1&valor2&valor3
    private static final Pattern FUSE_VALUE_REGEXP =
            Pattern.compile("\"([^\"]*)\"\\s*=\\s*([0-9a-fA-F]+(?:&[0-9a-fA-F]+)*)");

    // ✅ CORRECCIÓN CRÍTICA: Usar grupos numerados en lugar de named groups
    // Patrón para líneas LIST de fusibles: LISTn FUSEm "nombre" valores...
    // ANTES:
    // Pattern.compile("^LIST\\d+\\s+FUSE(?<fuse>\\d)\\s+\"(?<name>[^\"]*)\"\\s*(?<values>.*)$");
    // AHORA: Grupos numerados (1=fuse, 2=name, 3=values)
    private static final Pattern FUSE_LIST_REGEXP =
            Pattern.compile("^LIST\\d+\\s+FUSE(\\d)\\s+\"([^\"]*)\"\\s*(.*)$");

    // Patrón para líneas no vacías (para validación)
    private static final Pattern NONBLANK_REGEXP = Pattern.compile(".*\\S.*$");

    /**
     * Constructor desde archivo en el filesystem
     *
     * @param fileName Ruta completa del archivo chipinfo
     * @throws IOException Si hay error al leer el archivo
     * @throws FormatError Si el formato del archivo es inválido
     */
    public ChipFusesReader(String fileName) throws IOException, FormatError {
        this(new File(fileName));
    }

    /**
     * Constructor desde objeto File
     *
     * @param file Objeto File apuntando al chipinfo
     * @throws IOException Si hay error al leer el archivo
     * @throws FormatError Si el formato del archivo es inválido
     */
    public ChipFusesReader(File file) throws IOException, FormatError {
        this(new BufferedReader(new FileReader(file)));
    }

    /**
     * Constructor desde assets de Android
     *
     * @param context Contexto de Android para acceder a assets
     * @param assetFileName Nombre del archivo en la carpeta assets (ej: "chipinfo.cid")
     * @throws IOException Si hay error al leer el asset
     * @throws FormatError Si el formato del archivo es inválido
     */
    public ChipFusesReader(Context context, String assetFileName) throws IOException, FormatError {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(assetFileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        this.chipEntries = parseChipinfo(reader);
        reader.close();
    }

    /**
     * Constructor desde BufferedReader (método privado de inicialización)
     *
     * @param reader BufferedReader con el contenido del archivo
     * @throws IOException Si hay error al leer
     * @throws FormatError Si el formato es inválido
     */
    private ChipFusesReader(BufferedReader reader) throws IOException, FormatError {
        this.chipEntries = parseChipinfo(reader);
        reader.close();
    }

    /**
     * Método principal de parseo del archivo chipinfo
     *
     * <p>Lee línea por línea y construye el mapa de chips con sus datos
     *
     * @param reader BufferedReader con el contenido del archivo
     * @return Mapa de nombre_chip -> datos_chip
     * @throws IOException Si hay error de lectura
     * @throws FormatError Si hay error de formato
     */
    private Map<String, Map<String, Object>> parseChipinfo(BufferedReader reader)
            throws IOException, FormatError {

        Map<String, Map<String, Object>> entries = new HashMap<>();

        String chipName = ""; // Nombre del chip actual
        Map<String, Object> entry = null; // Entrada actual siendo procesada
        int lineNumber = 0; // Número de línea (para mensajes de error)

        String line;
        // Procesar cada línea del archivo
        while ((line = reader.readLine()) != null) {
            lineNumber++;

            // Intentar parsear como línea de asignación
            Matcher assignmentMatcher = ASSIGNMENT_REGEXP.matcher(line);
            if (assignmentMatcher.matches()) {
                String lhs = assignmentMatcher.group(1); // Lado izquierdo
                String rhs = assignmentMatcher.group(2); // Lado derecho

                // ✅ CORRECCIÓN: Usar "literal".equals(variable) para evitar NPE
                if ("CHIPname".equals(lhs)) {
                    // Validar que rhs no sea null o vacío
                    if (rhs == null || rhs.trim().isEmpty()) {
                        throw new FormatError("CHIPname vacío en línea " + lineNumber);
                    }

                    chipName = rhs.toLowerCase().trim();
                    entry = new HashMap<>();
                    entries.put(chipName, entry);
                    entry.put("CHIPname", chipName);
                } else {
                    // Caso general: agregar campo a la entrada actual
                    if (entry == null) {
                        throw new FormatError(
                                "Asignación fuera de definición de chip en línea "
                                        + lineNumber
                                        + ": "
                                        + line);
                    }

                    // Aplicar manejadores especiales según el tipo de campo
                    Object value = handleSpecialField(lhs, rhs);
                    entry.put(lhs, value);
                }
            } else {
                // Intentar parsear como línea de fusible (LIST)
                Matcher fuseListMatcher = FUSE_LIST_REGEXP.matcher(line);
                if (fuseListMatcher.matches()) {
                    if (entry == null) {
                        throw new FormatError(
                                "Definición de fusible fuera de chip en línea " + lineNumber);
                    }

                    // ✅ CORRECCIÓN: Usar grupos numerados (1, 2, 3) en lugar de named groups
                    String fuse = fuseListMatcher.group(1); // Número de fusible
                    String name = fuseListMatcher.group(2); // Nombre del fusible
                    String valuesString = fuseListMatcher.group(3); // Valores

                    // Validar que los valores extraídos no sean null
                    if (fuse == null || name == null || valuesString == null) {
                        throw new FormatError("Error parseando fusible en línea " + lineNumber);
                    }

                    // ✅ CORRECCIÓN: No usar computeIfAbsent()
                    // Obtener o crear el mapa de fusibles
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, List<ChipinfoEntry.FuseValue>>> fuses =
                            (Map<String, Map<String, List<ChipinfoEntry.FuseValue>>>)
                                    entry.get("fuses");

                    if (fuses == null) {
                        fuses = new HashMap<>();
                        entry.put("fuses", fuses);
                    }

                    // ✅ CORRECCIÓN: No usar computeIfAbsent()
                    // Obtener o crear el mapa de configuraciones para este fusible
                    Map<String, List<ChipinfoEntry.FuseValue>> fuseSettings = fuses.get(name);
                    if (fuseSettings == null) {
                        fuseSettings = new HashMap<>();
                        fuses.put(name, fuseSettings);
                    }

                    // Parsear todos los valores del fusible
                    Matcher valueMatcher = FUSE_VALUE_REGEXP.matcher(valuesString);
                    while (valueMatcher.find()) {
                        String setting = valueMatcher.group(1); // Nombre de la opción
                        String valuesStr = valueMatcher.group(2); // Valores hex (puede tener &)

                        // Validar valores extraídos
                        if (setting == null || valuesStr == null) {
                            continue; // Saltar valores inválidos
                        }

                        // Procesar valores múltiples separados por &
                        // Ejemplo: 3FFF&2FFF significa fuse=0x3FFF, fuse=0x2FFF
                        String[] valuesParts = valuesStr.split("&");
                        List<ChipinfoEntry.FuseValue> fuseValues = new ArrayList<>();

                        try {
                            int fuseNumber = Integer.parseInt(fuse) - 1; // Índice base 0

                            for (int i = 0; i < valuesParts.length; i++) {
                                int value = Integer.parseInt(valuesParts[i], 16);
                                fuseValues.add(new ChipinfoEntry.FuseValue(fuseNumber + i, value));
                            }

                            fuseSettings.put(setting, fuseValues);
                        } catch (NumberFormatException e) {
                            // Ignorar valores inválidos y continuar
                        }
                    }
                } else if (NONBLANK_REGEXP.matcher(line).matches()) {
                    // Línea no vacía que no coincide con ningún patrón = error
                    throw new FormatError(
                            "Formato de línea no reconocido en línea " + lineNumber + ": " + line);
                }
                // Las líneas vacías se ignoran silenciosamente
            }
        }

        return entries;
    }

    /**
     * Maneja la conversión de campos especiales con tipos específicos
     *
     * <p>✅ VERSIÓN CORREGIDA - Compatible con Android API < 24
     *
     * @param fieldName Nombre del campo
     * @param value Valor como string
     * @return Valor convertido al tipo apropiado
     */
    private Object handleSpecialField(String fieldName, String value) {
        // Protección contra valores null
        if (value == null) {
            return "";
        }

        value = value.toLowerCase().trim();

        switch (fieldName) {
            case "BandGap":
            case "CALword":
            case "CPwarn":
            case "FlashChip":
            case "ICSPonly":
                // ✅ CORRECCIÓN: No usar getOrDefault()
                Boolean boolValue = BOOLEAN_DICT.get(value);
                return (boolValue != null) ? boolValue : false;

            case "ChipID":
            case "EEPROMsize":
            case "ROMsize":
                try {
                    return Integer.parseInt(value, 16);
                } catch (NumberFormatException e) {
                    return 0;
                }

            case "EraseMode":
            case "OverProgram":
            case "ProgramDelay":
            case "ProgramTries":
                try {
                    return Integer.parseInt(value, 10);
                } catch (NumberFormatException e) {
                    return 0;
                }

            case "CoreType":
                // ✅ CORRECCIÓN: No usar getOrDefault()
                Integer coreType = CORE_TYPE_DICT.get(value);
                return (coreType != null) ? coreType : 0;

            case "FUSEblank":
                String[] parts = value.split("\\s+");
                List<Integer> fuseBlank = new ArrayList<>();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        try {
                            fuseBlank.add(Integer.parseInt(part, 16));
                        } catch (NumberFormatException e) {
                            // Ignorar valores inválidos
                        }
                    }
                }
                return fuseBlank;

            default:
                return value;
        }
    }

    /**
     * Obtiene una entrada de chip por su nombre
     *
     * @param name Nombre del chip (case-insensitive)
     * @return Objeto ChipinfoEntry con los datos del chip
     * @throws IllegalArgumentException Si el chip no existe en la base de datos
     */
    @SuppressWarnings("unchecked")
    public ChipinfoEntry getChip(String name) {
        // ✅ CORRECCIÓN: Validar parámetro null
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de chip inválido: null o vacío");
        }

        String chipName = name.toLowerCase().trim();

        if (!chipEntries.containsKey(chipName)) {
            throw new IllegalArgumentException("Chip no encontrado: " + name);
        }

        Map<String, Object> data = chipEntries.get(chipName);

        // ✅ CORRECCIÓN: Manejo seguro de nulls con valores por defecto
        String CHIPname = (String) data.get("CHIPname");
        String INCLUDE = data.containsKey("INCLUDE") ? (String) data.get("INCLUDE") : null;
        String SocketImage =
                data.containsKey("SocketImage") ? (String) data.get("SocketImage") : null;

        Integer eraseModeObj = (Integer) data.get("EraseMode");
        int EraseMode = (eraseModeObj != null) ? eraseModeObj : 0;

        Boolean flashChipObj = (Boolean) data.get("FlashChip");
        boolean FlashChip = (flashChipObj != null) ? flashChipObj : false;

        String PowerSequence =
                data.containsKey("PowerSequence") ? (String) data.get("PowerSequence") : null;

        Integer programDelayObj = (Integer) data.get("ProgramDelay");
        int ProgramDelay = (programDelayObj != null) ? programDelayObj : 0;

        Integer programTriesObj = (Integer) data.get("ProgramTries");
        int ProgramTries = (programTriesObj != null) ? programTriesObj : 0;

        Integer overProgramObj = (Integer) data.get("OverProgram");
        int OverProgram = (overProgramObj != null) ? overProgramObj : 0;

        Integer coreTypeObj = (Integer) data.get("CoreType");
        int CoreType = (coreTypeObj != null) ? coreTypeObj : 0;

        Integer romSizeObj = (Integer) data.get("ROMsize");
        int ROMsize = (romSizeObj != null) ? romSizeObj : 0;

        Integer eepromSizeObj = (Integer) data.get("EEPROMsize");
        int EEPROMsize = (eepromSizeObj != null) ? eepromSizeObj : 0;

        List<Integer> FUSEblank = (List<Integer>) data.get("FUSEblank");

        Boolean cpWarnObj = (Boolean) data.get("CPwarn");
        boolean CPwarn = (cpWarnObj != null) ? cpWarnObj : false;

        Boolean calWordObj = (Boolean) data.get("CALword");
        boolean CALword = (calWordObj != null) ? calWordObj : false;

        Boolean bandGapObj = (Boolean) data.get("BandGap");
        boolean BandGap = (bandGapObj != null) ? bandGapObj : false;

        Boolean icspOnlyObj = (Boolean) data.get("ICSPonly");
        boolean ICSPonly = (icspOnlyObj != null) ? icspOnlyObj : false;

        Integer chipIdObj = (Integer) data.get("ChipID");
        int ChipID = (chipIdObj != null) ? chipIdObj : 0;

        Map<String, Map<String, List<ChipinfoEntry.FuseValue>>> fuses =
                (Map<String, Map<String, List<ChipinfoEntry.FuseValue>>>) data.get("fuses");

        // Crear y retornar el objeto ChipinfoEntry
        return new ChipinfoEntry(
                CHIPname,
                INCLUDE,
                SocketImage,
                EraseMode,
                FlashChip,
                PowerSequence,
                ProgramDelay,
                ProgramTries,
                OverProgram,
                CoreType,
                ROMsize,
                EEPROMsize,
                FUSEblank,
                CPwarn,
                CALword,
                BandGap,
                ICSPonly,
                ChipID,
                fuses);
    }

    /**
     * Obtiene la lista de todos los chips disponibles en la base de datos
     *
     * @return Lista con los nombres de todos los chips
     */
    public List<String> getAvailableChips() {
        return new ArrayList<>(chipEntries.keySet());
    }

    /**
     * Verifica si un chip existe en la base de datos
     *
     * @param name Nombre del chip (case-insensitive)
     * @return true si el chip existe, false si no
     */
    public boolean hasChip(String name) {
        if (name == null) {
            return false;
        }
        return chipEntries.containsKey(name.toLowerCase().trim());
    }

    /**
     * Obtiene el número total de chips en la base de datos
     *
     * @return Número de chips
     */
    public int getChipCount() {
        return chipEntries.size();
    }
}
