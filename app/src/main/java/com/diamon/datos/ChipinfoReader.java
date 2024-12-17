package com.diamon.datos;

import android.app.Activity;

import com.diamon.chip.InformacionPic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChipinfoReader {

    private static final int NUMERO_DE_REGISTROS_DATOS = 3448;

    private Map<String, InformacionPic> chipEntries;

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

    public ChipinfoReader(Activity actividad) {

        chipEntries = new HashMap<String, InformacionPic>();

        modelosPic = new ArrayList<String>();

        listaFuses = new ArrayList<String[]>();

        final DatosDePic datos = new DatosDePic(actividad);

        int numeroRegistro = 0;

        for (String te : datos.getInformacionPic()) {

            numeroRegistro++;

            if (te.length() >= 8) {

                if (te.substring(0, 8).equals("CHIPname")) {

                    CHIPname = te.substring(9);
                }
            }

            if (te.length() >= 7) {

                if (te.substring(0, 7).equals("INCLUDE")) {

                    INCLUDEr = te.substring(8);
                }
            }

            if (te.length() >= 11) {

                if (te.substring(0, 11).equals("SocketImage")) {

                    SocketImage = te.substring(12);
                }
            }

            if (te.length() >= 9) {

                if (te.substring(0, 9).equals("EraseMode")) {

                    EraseMode = te.substring(10);
                }
            }

            if (te.length() >= 9) {

                if (te.substring(0, 9).equals("FlashChip")) {

                    FlashChip = te.substring(10);
                }
            }

            if (te.length() >= 13) {

                if (te.substring(0, 13).equals("PowerSequence")) {

                    PowerSequence = te.substring(14);
                }
            }

            if (te.length() >= 12) {

                if (te.substring(0, 12).equals("ProgramDelay")) {

                    ProgramDelay = te.substring(13);
                }
            }

            if (te.length() >= 12) {

                if (te.substring(0, 12).equals("ProgramTries")) {

                    ProgramTries = te.substring(13);
                }
            }

            if (te.length() >= 11) {

                if (te.substring(0, 11).equals("OverProgram")) {

                    OverProgram = te.substring(12);
                }
            }

            if (te.length() >= 8) {

                if (te.substring(0, 8).equals("CoreType")) {

                    CoreType = te.substring(9);
                }
            }

            if (te.length() >= 7) {

                if (te.substring(0, 7).equals("ROMsize")) {

                    ROMsize = te.substring(8);
                }
            }

            if (te.length() >= 10) {

                if (te.substring(0, 10).equals("EEPROMsize")) {

                    EEPROMsize = te.substring(11);
                }
            }

            if (te.length() >= 9) {

                if (te.substring(0, 9).equals("FUSEblank")) {

                    StringBuffer letra = new StringBuffer();

                    ArrayList<String> dato = new ArrayList<String>();

                    for (int con = 0; con < te.substring(10).length(); con++) {

                        if (!("" + te.substring(10).charAt(con)).equals(" ")) {

                            letra.append(te.substring(10).charAt(con));

                        } else {

                            dato.add(letra.toString());

                            letra.setLength(0);
                        }

                        if (con == (te.substring(10).length() - 1)) {

                            dato.add(letra.toString());
                        }
                    }

                    String[] datosProcesados = new String[dato.size()];

                    for (int da = 0; da < datosProcesados.length; da++) {

                        datosProcesados[da] = dato.get(da).toString();
                    }

                    FUSEblank = datosProcesados;
                }
            }

            if (te.length() >= 6) {

                if (te.substring(0, 6).equals("CPwarn")) {

                    CPwarn = te.substring(7);
                }
            }

            if (te.length() >= 7) {

                if (te.substring(0, 7).equals("CALword")) {

                    CALword = te.substring(8);
                }
            }

            if (te.length() >= 7) {

                if (te.substring(0, 7).equals("BandGap")) {

                    BandGap = te.substring(8);
                }
            }

            if (te.length() >= 8) {

                if (te.substring(0, 8).equals("ICSPonly")) {

                    ICSPonly = te.substring(9);
                }
            }

            if (te.length() >= 6) {

                if (te.substring(0, 6).equals("ChipID")) {

                    ChipID = te.substring(7);
                }
            }

            if (te.length() >= 4) {

                if (te.substring(0, 4).equals("LIST")) {

                    ArrayList<String> dato = new ArrayList<String>();

                    StringBuffer letra = new StringBuffer();
                    

                    for (int con = 0; con < te.length(); con++) {

                        if (!("" + te.charAt(con)).equals(" ")) {

                            
                            letra.append(te.charAt(con));
                            
                            

                        } else {
                            

                            dato.add(letra.toString());

                            letra.setLength(0);
                        }

                        if (con == (te.length() - 1)) {

                            dato.add(letra.toString());
                        }
                    }

                    String[] datosFuses = new String[dato.size()];

                    for (int i = 0; i < datosFuses.length; i++) {

                        datosFuses[i] = dato.get(i).toString();
                    }

                    listaFuses.add(datosFuses);
                }
            }

            // Para las posiciones
            if (te.equals("")) {

                cargarDatos();
            }

            // Ultimo registro
            if (numeroRegistro == ChipinfoReader.NUMERO_DE_REGISTROS_DATOS) {

                cargarDatos();
            }
        }
    }

    private void cargarDatos() {

        // Se vuelve a construir un objeto cada ciclo
        fuses = new HashMap<String, Object>();

        fuses.put("FUSES", listaFuses);

        // Se vuelve a construir un objeto para llenar de nuevo
        listaFuses = new ArrayList<String[]>();

        modelosPic.add(CHIPname);

        chipEntries.put(
                CHIPname,
                new InformacionPic(
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
                        fuses));
    }

    public InformacionPic getChipEntry(String chipName) {
        return chipEntries.get(chipName);
    }

    public ArrayList<String> getModelosPic() {
        return this.modelosPic;
    }
}
