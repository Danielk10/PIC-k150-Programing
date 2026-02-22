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
 * Clase para leer y procesar información de chips PIC desde archivos de
 * configuración. Parsea
 * archivos de configuración de chips y crea objetos ChipPic con la información.
 */
public class ChipinfoReader {

    private static final int NUMERO_DE_REGISTROS_DATOS = 6617;

    // Patron para lineas LIST de fusibles: LIST\d+ FUSE\d "nombre" valores...
    // Grupos: (1)=numero_fuse, (2)=nombre_fuse, (3)=valores
    private static final Pattern FUSE_LIST_REGEXP = Pattern.compile("^LIST\\d+\\s+FUSE(\\d)\\s+\"([^\"]*)\"\\s*(.*)$");

    // Patron para pares "opcion"=valor hexadecimal (puede tener & entre valores)
    private static final Pattern FUSE_VALUE_REGEXP = Pattern
            .compile("\"([^\"]*)\"\\s*=\\s*([0-9a-fA-F]+(?:&[0-9a-fA-F]+)*)");

    private Map<String, ChipPic> chipEntries;

    private String CHIPname;

    private String INCLUDEr;

    private String SocketImage;

    private String EraseMode;

    private String FlashChip;

    private String PowerSequence;

    private String ProgramDelay;

    private String ProgramTries;

    private String OverProgram;

    private String CoreType;

    private String ROMsize;

    private String EEPROMsize;

    private String[] FUSEblank;

    private String CPwarn;

    private String CALword;

    private String BandGap;

    private String ICSPonly;

    private String ChipID;

    // Fuses estructurados para el chip actual
    // Map: nombre_fuse -> (opcion -> List<FuseValue>)
    private Map<String, Map<String, List<ChipPic.FuseValue>>> currentFusesMap;

    private ArrayList<String> modelosPic;

    /**
     * Constructor que inicializa el lector de información de chips.
     *
     * @param actividad Actividad Android para acceder a recursos
     * @throws ChipConfigurationException Si hay un error al leer la información de
     *                                    los chips
     */
    public ChipinfoReader(Activity actividad) throws ChipConfigurationException {

        if (actividad == null) {
            throw new ChipConfigurationException("La actividad no puede ser nula");
        }

        chipEntries = new HashMap<String, ChipPic>();
        modelosPic = new ArrayList<String>();
        currentFusesMap = new HashMap<>();

        try {
            final DatosDePic datos = new DatosDePic(actividad);

            int numeroRegistro = 0;
            int chipsProcesados = 0;

            for (String te : datos.getInformacionPic()) {
                numeroRegistro++;

                try {
                    procesarLinea(te, numeroRegistro);

                    // Para las posiciones
                    if (te.equals("")) {
                        cargarDatos();
                        chipsProcesados++;
                        // Reiniciar fuses para el proximo chip
                        currentFusesMap = new HashMap<>();
                    }

                    // Ultimo registro
                    if (numeroRegistro == ChipinfoReader.NUMERO_DE_REGISTROS_DATOS) {
                        cargarDatos();
                        chipsProcesados++;
                        currentFusesMap = new HashMap<>();
                    }

                } catch (Exception e) {
                    // Continuamos con la siguiente línea
                }
            }

        } catch (Exception e) {
            throw new ChipConfigurationException(
                    "Error al inicializar ChipinfoReader: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa una línea del archivo de configuración de chips.
     *
     * @param te             Línea a procesar
     * @param numeroRegistro Número de registro actual
     */
    private void procesarLinea(String te, int numeroRegistro) throws ChipConfigurationException {
        if (te == null) {
            return;
        }

        if (te.length() >= 8 && te.substring(0, 8).equals("CHIPname")) {
            CHIPname = te.substring(9);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("INCLUDE")) {
            INCLUDEr = te.substring(8);
        }

        if (te.length() >= 11 && te.substring(0, 11).equals("SocketImage")) {
            SocketImage = te.substring(12);
        }

        if (te.length() >= 9 && te.substring(0, 9).equals("EraseMode")) {
            EraseMode = te.substring(10);
        }

        if (te.length() >= 9 && te.substring(0, 9).equals("FlashChip")) {
            FlashChip = te.substring(10);
        }

        if (te.length() >= 13 && te.substring(0, 13).equals("PowerSequence")) {
            PowerSequence = te.substring(14);
        }

        if (te.length() >= 12 && te.substring(0, 12).equals("ProgramDelay")) {
            ProgramDelay = te.substring(13);
        }

        if (te.length() >= 12 && te.substring(0, 12).equals("ProgramTries")) {
            ProgramTries = te.substring(13);
        }

        if (te.length() >= 11 && te.substring(0, 11).equals("OverProgram")) {
            OverProgram = te.substring(12);
        }

        if (te.length() >= 8 && te.substring(0, 8).equals("CoreType")) {
            CoreType = te.substring(9);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("ROMsize")) {
            ROMsize = te.substring(8);
        }

        if (te.length() >= 10 && te.substring(0, 10).equals("EEPROMsize")) {
            EEPROMsize = te.substring(11);
        }

        if (te.length() >= 9 && te.substring(0, 9).equals("FUSEblank")) {
            procesarFUSEblank(te.substring(10));
        }

        if (te.length() >= 6 && te.substring(0, 6).equals("CPwarn")) {
            CPwarn = te.substring(7);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("CALword")) {
            CALword = te.substring(8);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("BandGap")) {
            BandGap = te.substring(8);
        }

        if (te.length() >= 8 && te.substring(0, 8).equals("ICSPonly")) {
            ICSPonly = te.substring(9);
        }

        if (te.length() >= 6 && te.substring(0, 6).equals("ChipID")) {
            ChipID = te.substring(7);
        }

        if (te.length() >= 4 && te.substring(0, 4).equals("LIST")) {
            procesarLIST(te);
        }
    }

    /**
     * Procesa la línea FUSEblank.
     *
     * @param fuseblankData Datos de FUSEblank
     */
    private void procesarFUSEblank(String fuseblankData) throws ChipConfigurationException {
        try {
            StringBuffer letra = new StringBuffer();
            ArrayList<String> dato = new ArrayList<String>();

            for (int con = 0; con < fuseblankData.length(); con++) {
                if (!("" + fuseblankData.charAt(con)).equals(" ")) {
                    letra.append(fuseblankData.charAt(con));
                } else {
                    if (letra.length() > 0) {
                        dato.add(letra.toString());
                        letra.setLength(0);
                    }
                }

                if (con == (fuseblankData.length() - 1) && letra.length() > 0) {
                    dato.add(letra.toString());
                }
            }

            String[] datosProcesados = new String[dato.size()];
            for (int da = 0; da < datosProcesados.length; da++) {
                datosProcesados[da] = dato.get(da);
            }

            FUSEblank = datosProcesados;

        } catch (Exception e) {
            throw new ChipConfigurationException("Error al procesar FUSEblank: " + e.getMessage());
        }
    }

    /**
     * Procesa la linea LIST y construye el mapa estructurado de fuses.
     * Formato: LIST\d+ FUSE\d "nombre" "opcion1"=val1 "opcion2"=val2 ...
     * Compatible con el formato chipinfo.txt/chipinfo.cid.
     */
    private void procesarLIST(String listData) throws ChipConfigurationException {
        try {
            Matcher listMatcher = FUSE_LIST_REGEXP.matcher(listData);
            if (!listMatcher.matches()) {
                // Formato no reconocido, ignorar silenciosamente
                return;
            }

            String fuseIndexStr = listMatcher.group(1); // numero del fuse (1, 2...)
            String fuseName = listMatcher.group(2); // nombre del fuse
            String valuesStr = listMatcher.group(3); // resto con "opcion"=valor

            if (fuseName == null || fuseName.isEmpty())
                return;

            // Obtener o crear el mapa de opciones para este fuse
            Map<String, List<ChipPic.FuseValue>> fuseSettings = currentFusesMap.get(fuseName);
            if (fuseSettings == null) {
                fuseSettings = new HashMap<>();
                currentFusesMap.put(fuseName, fuseSettings);
            }

            int fuseIndex = Integer.parseInt(fuseIndexStr) - 1; // base 0

            // Parsear todos los pares "opcion"=valor en la linea
            Matcher valueMatcher = FUSE_VALUE_REGEXP.matcher(valuesStr != null ? valuesStr : "");
            while (valueMatcher.find()) {
                String setting = valueMatcher.group(1);
                String valPart = valueMatcher.group(2);
                if (setting == null || valPart == null)
                    continue;

                // Valores multiples separados por & (multiples palabras de config)
                String[] parts = valPart.split("&");
                List<ChipPic.FuseValue> fuseValues = new ArrayList<>();
                for (int i = 0; i < parts.length; i++) {
                    try {
                        int val = Integer.parseInt(parts[i].trim(), 16);
                        fuseValues.add(new ChipPic.FuseValue(fuseIndex + i, val));
                    } catch (NumberFormatException ignored) {
                    }
                }

                fuseSettings.put(setting, fuseValues);
            }

        } catch (Exception e) {
            throw new ChipConfigurationException("Error al procesar LIST: " + e.getMessage());
        }
    }

    /**
     * Carga los datos procesados en un nuevo objeto ChipPic.
     *
     * @throws ChipConfigurationException Si hay un error al crear el objeto ChipPic
     */
    private void cargarDatos() throws ChipConfigurationException {

        try {
            // Validar datos obligatorios
            if (CHIPname == null || CHIPname.trim().isEmpty()) {
                throw new ChipConfigurationException("CHIPname no puede ser nulo o vacio");
            }

            if (ROMsize == null || ROMsize.trim().isEmpty()) {
                throw new ChipConfigurationException(
                        "ROMsize no puede ser nulo o vacio para chip: " + CHIPname);
            }

            if (CoreType == null || CoreType.trim().isEmpty()) {
                throw new ChipConfigurationException(
                        "CoreType no puede ser nulo o vacio para chip: " + CHIPname);
            }

            // Construir el mapa de fuses legado (compatibilidad con el constructor)
            Map<String, Object> fusesLegacy = new HashMap<String, Object>();
            fusesLegacy.put("FUSES", new ArrayList<String[]>()); // vacio, se usa setFusesMap

            modelosPic.add(CHIPname);

            ChipPic chipPic = new ChipPic(
                    CHIPname,
                    INCLUDEr,
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
                    fusesLegacy);

            // Inyectar el mapa estructurado de fuses (para encode/decode)
            if (!currentFusesMap.isEmpty()) {
                chipPic.setFusesMap(new HashMap<>(currentFusesMap));
            }

            chipEntries.put(CHIPname, chipPic);

        } catch (Exception e) {
            throw new ChipConfigurationException(
                    "Error al cargar datos para chip " + CHIPname + ": " + e.getMessage());
        }
    }

    /**
     * Obtiene la entrada de un chip específico.
     *
     * @param chipName Nombre del chip a buscar
     * @return Objeto ChipPic con la información del chip, o null si no se encuentra
     */
    public ChipPic getChipEntry(String chipName) {

        if (chipName == null || chipName.trim().isEmpty()) {
            return null;
        }

        ChipPic chipPic = chipEntries.get(chipName);

        if (chipPic == null) {
        } else {
        }

        return chipPic;
    }

    /**
     * Obtiene la lista de modelos PIC disponibles, ordenada alfabetica y
     * numericamente.
     * Orden: primero por familia (PIC10 < PIC12 < PIC16 < PIC18),
     * luego por numero de modelo de menor a mayor.
     *
     * @return Lista con los nombres de los modelos PIC
     */
    public ArrayList<String> getModelosPic() {
        Collections.sort(modelosPic, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return comparePicNames(a, b);
            }
        });
        return this.modelosPic;
    }

    /**
     * Compara dos nombres de PICs para ordenamiento alfabetico-numerico.
     * Estrategia: comparar caracter a caracter; cuando se encuentra una
     * secuencia de digitos, compararla como numero entero.
     */
    private int comparePicNames(String a, String b) {
        int ia = 0, ib = 0;
        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia);
            char cb = b.charAt(ib);
            boolean aDigit = Character.isDigit(ca);
            boolean bDigit = Character.isDigit(cb);

            if (aDigit && bDigit) {
                // Extraer los numeros completos y compararlos numericamente
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
                // Comparar caracteres textuales (case-insensitive)
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
