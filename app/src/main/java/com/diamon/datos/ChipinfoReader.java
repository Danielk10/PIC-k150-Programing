package com.diamon.datos;

import android.app.Activity;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.utilidades.LogManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase para leer y procesar información de chips PIC desde archivos de configuración.
 * Parsea archivos de configuración de chips y crea objetos ChipPic con la información.
 */
public class ChipinfoReader {

    private static final int NUMERO_DE_REGISTROS_DATOS = 6617;

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

    private Map<String, Object> fuses;

    private ArrayList<String[]> listaFuses;

    private ArrayList<String> modelosPic;

    /**
     * Constructor que inicializa el lector de información de chips.
     *
     * @param actividad Actividad Android para acceder a recursos
     * @throws ChipConfigurationException Si hay un error al leer la información de los chips
     */
    public ChipinfoReader(Activity actividad) throws ChipConfigurationException {
        LogManager.d(LogManager.Categoria.DATA, "Inicializando ChipinfoReader");
        
        if (actividad == null) {
            LogManager.e(LogManager.Categoria.DATA, "La actividad no puede ser nula");
            throw new ChipConfigurationException("La actividad no puede ser nula");
        }
        
        chipEntries = new HashMap<String, ChipPic>();
        modelosPic = new ArrayList<String>();
        listaFuses = new ArrayList<String[]>();

        try {
            final DatosDePic datos = new DatosDePic(actividad);
            LogManager.i(LogManager.Categoria.DATA, "DatosDePic inicializado correctamente");

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
                        LogManager.d(LogManager.Categoria.DATA, "Chip procesado: " + CHIPname + " (" + chipsProcesados + ")");
                    }

                    // Ultimo registro
                    if (numeroRegistro == ChipinfoReader.NUMERO_DE_REGISTROS_DATOS) {
                        cargarDatos();
                        chipsProcesados++;
                        LogManager.d(LogManager.Categoria.DATA, "Último chip procesado: " + CHIPname + " (" + chipsProcesados + ")");
                    }
                    
                } catch (Exception e) {
                    LogManager.e(LogManager.Categoria.DATA, "Error al procesar línea " + numeroRegistro + ": " + te + " - " + e.getMessage(), e);
                    // Continuamos con la siguiente línea
                }
            }
            
            LogManager.i(LogManager.Categoria.DATA, "Procesamiento completado. Total de chips: " + chipEntries.size());
            
        } catch (Exception e) {
            LogManager.e(LogManager.Categoria.DATA, "Error al inicializar ChipinfoReader: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al inicializar ChipinfoReader: " + e.getMessage(), e);
        }
    }
    
    /**
     * Procesa una línea del archivo de configuración de chips.
     *
     * @param te Línea a procesar
     * @param numeroRegistro Número de registro actual
     */
    private void procesarLinea(String te, int numeroRegistro) throws ChipConfigurationException {
        if (te == null) {
            LogManager.w(LogManager.Categoria.DATA, "Línea nula en registro " + numeroRegistro);
            return;
        }
        
        LogManager.v(LogManager.Categoria.DATA, "Procesando línea " + numeroRegistro + ": " + te);

        if (te.length() >= 8 && te.substring(0, 8).equals("CHIPname")) {
            CHIPname = te.substring(9);
            LogManager.v(LogManager.Categoria.DATA, "CHIPname: " + CHIPname);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("INCLUDE")) {
            INCLUDEr = te.substring(8);
            LogManager.v(LogManager.Categoria.DATA, "INCLUDE: " + INCLUDEr);
        }

        if (te.length() >= 11 && te.substring(0, 11).equals("SocketImage")) {
            SocketImage = te.substring(12);
            LogManager.v(LogManager.Categoria.DATA, "SocketImage: " + SocketImage);
        }

        if (te.length() >= 9 && te.substring(0, 9).equals("EraseMode")) {
            EraseMode = te.substring(10);
            LogManager.v(LogManager.Categoria.DATA, "EraseMode: " + EraseMode);
        }

        if (te.length() >= 9 && te.substring(0, 9).equals("FlashChip")) {
            FlashChip = te.substring(10);
            LogManager.v(LogManager.Categoria.DATA, "FlashChip: " + FlashChip);
        }

        if (te.length() >= 13 && te.substring(0, 13).equals("PowerSequence")) {
            PowerSequence = te.substring(14);
            LogManager.v(LogManager.Categoria.DATA, "PowerSequence: " + PowerSequence);
        }

        if (te.length() >= 12 && te.substring(0, 12).equals("ProgramDelay")) {
            ProgramDelay = te.substring(13);
            LogManager.v(LogManager.Categoria.DATA, "ProgramDelay: " + ProgramDelay);
        }

        if (te.length() >= 12 && te.substring(0, 12).equals("ProgramTries")) {
            ProgramTries = te.substring(13);
            LogManager.v(LogManager.Categoria.DATA, "ProgramTries: " + ProgramTries);
        }

        if (te.length() >= 11 && te.substring(0, 11).equals("OverProgram")) {
            OverProgram = te.substring(12);
            LogManager.v(LogManager.Categoria.DATA, "OverProgram: " + OverProgram);
        }

        if (te.length() >= 8 && te.substring(0, 8).equals("CoreType")) {
            CoreType = te.substring(9);
            LogManager.v(LogManager.Categoria.DATA, "CoreType: " + CoreType);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("ROMsize")) {
            ROMsize = te.substring(8);
            LogManager.v(LogManager.Categoria.DATA, "ROMsize: " + ROMsize);
        }

        if (te.length() >= 10 && te.substring(0, 10).equals("EEPROMsize")) {
            EEPROMsize = te.substring(11);
            LogManager.v(LogManager.Categoria.DATA, "EEPROMsize: " + EEPROMsize);
        }

        if (te.length() >= 9 && te.substring(0, 9).equals("FUSEblank")) {
            procesarFUSEblank(te.substring(10));
        }

        if (te.length() >= 6 && te.substring(0, 6).equals("CPwarn")) {
            CPwarn = te.substring(7);
            LogManager.v(LogManager.Categoria.DATA, "CPwarn: " + CPwarn);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("CALword")) {
            CALword = te.substring(8);
            LogManager.v(LogManager.Categoria.DATA, "CALword: " + CALword);
        }

        if (te.length() >= 7 && te.substring(0, 7).equals("BandGap")) {
            BandGap = te.substring(8);
            LogManager.v(LogManager.Categoria.DATA, "BandGap: " + BandGap);
        }

        if (te.length() >= 8 && te.substring(0, 8).equals("ICSPonly")) {
            ICSPonly = te.substring(9);
            LogManager.v(LogManager.Categoria.DATA, "ICSPonly: " + ICSPonly);
        }

        if (te.length() >= 6 && te.substring(0, 6).equals("ChipID")) {
            ChipID = te.substring(7);
            LogManager.v(LogManager.Categoria.DATA, "ChipID: " + ChipID);
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
        LogManager.v(LogManager.Categoria.DATA, "Procesando FUSEblank: " + fuseblankData);
        
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
            LogManager.v(LogManager.Categoria.DATA, "FUSEblank procesado: " + datosProcesados.length + " valores");
            
        } catch (Exception e) {
            LogManager.e(LogManager.Categoria.DATA, "Error al procesar FUSEblank: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al procesar FUSEblank: " + e.getMessage());
        }
    }
    
    /**
     * Procesa la línea LIST.
     *
     * @param listData Datos de LIST
     */
    private void procesarLIST(String listData) throws ChipConfigurationException {
        LogManager.v(LogManager.Categoria.DATA, "Procesando LIST: " + listData);
        
        try {
            ArrayList<String> dato = new ArrayList<String>();
            StringBuffer letra = new StringBuffer();

            for (int con = 0; con < listData.length(); con++) {
                if (!("" + listData.charAt(con)).equals(" ")) {
                    letra.append(listData.charAt(con));
                } else {
                    if (letra.length() > 0) {
                        dato.add(letra.toString());
                        letra.setLength(0);
                    }
                }

                if (con == (listData.length() - 1) && letra.length() > 0) {
                    dato.add(letra.toString());
                }
            }

            String[] datosFuses = new String[dato.size()];
            for (int i = 0; i < datosFuses.length; i++) {
                datosFuses[i] = dato.get(i);
            }

            listaFuses.add(datosFuses);
            LogManager.v(LogManager.Categoria.DATA, "LIST procesado: " + datosFuses.length + " elementos");
            
        } catch (Exception e) {
            LogManager.e(LogManager.Categoria.DATA, "Error al procesar LIST: " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al procesar LIST: " + e.getMessage());
        }
    }

    /**
     * Carga los datos procesados en un nuevo objeto ChipPic.
     *
     * @throws ChipConfigurationException Si hay un error al crear el objeto ChipPic
     */
    private void cargarDatos() throws ChipConfigurationException {
        LogManager.d(LogManager.Categoria.DATA, "Cargando datos para chip: " + CHIPname);
        
        try {
            // Validar datos obligatorios
            if (CHIPname == null || CHIPname.trim().isEmpty()) {
                LogManager.e(LogManager.Categoria.DATA, "CHIPname no puede ser nulo o vacío");
                throw new ChipConfigurationException("CHIPname no puede ser nulo o vacío");
            }
            
            if (ROMsize == null || ROMsize.trim().isEmpty()) {
                LogManager.e(LogManager.Categoria.DATA, "ROMsize no puede ser nulo o vacío para chip: " + CHIPname);
                throw new ChipConfigurationException("ROMsize no puede ser nulo o vacío para chip: " + CHIPname);
            }
            
            if (CoreType == null || CoreType.trim().isEmpty()) {
                LogManager.e(LogManager.Categoria.DATA, "CoreType no puede ser nulo o vacío para chip: " + CHIPname);
                throw new ChipConfigurationException("CoreType no puede ser nulo o vacío para chip: " + CHIPname);
            }

            // Se vuelve a construir un objeto cada ciclo
            fuses = new HashMap<String, Object>();
            fuses.put("FUSES", listaFuses);

            // Se vuelve a construir un objeto para llenar de nuevo
            listaFuses = new ArrayList<String[]>();

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
                    fuses);

            chipEntries.put(CHIPname, chipPic);
            
            LogManager.d(LogManager.Categoria.DATA, "Chip cargado exitosamente: " + CHIPname);
            
        } catch (Exception e) {
            LogManager.e(LogManager.Categoria.DATA, "Error al cargar datos para chip " + CHIPname + ": " + e.getMessage(), e);
            throw new ChipConfigurationException("Error al cargar datos para chip " + CHIPname + ": " + e.getMessage());
        }
    }

    /**
     * Obtiene la entrada de un chip específico.
     *
     * @param chipName Nombre del chip a buscar
     * @return Objeto ChipPic con la información del chip, o null si no se encuentra
     */
    public ChipPic getChipEntry(String chipName) {
        LogManager.d(LogManager.Categoria.DATA, "Buscando chip: " + chipName);
        
        if (chipName == null || chipName.trim().isEmpty()) {
            LogManager.w(LogManager.Categoria.DATA, "Nombre de chip no puede ser nulo o vacío");
            return null;
        }
        
        ChipPic chipPic = chipEntries.get(chipName);
        
        if (chipPic == null) {
            LogManager.w(LogManager.Categoria.DATA, "Chip no encontrado: " + chipName);
        } else {
            LogManager.d(LogManager.Categoria.DATA, "Chip encontrado: " + chipName);
        }
        
        return chipPic;
    }

    /**
     * Obtiene la lista de modelos PIC disponibles.
     *
     * @return Lista con los nombres de los modelos PIC
     */
    public ArrayList<String> getModelosPic() {
        LogManager.d(LogManager.Categoria.DATA, "Obteniendo lista de modelos PIC. Total: " + modelosPic.size());
        return this.modelosPic;
    }
}
