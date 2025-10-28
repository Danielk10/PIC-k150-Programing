package com.diamon.managers;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.protocolo.ProtocoloP018;
import com.diamon.pic.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de operaciones de programacion de microcontroladores PIC. Encapsula todas las operaciones
 * relacionadas con la programacion, lectura, borrado y verificacion de memoria de chips PIC.
 */
public class PicProgrammingManager {

    private final Context context;
    private ProtocoloP018 protocolo;

    // Interfaz para notificar el progreso de operaciones
    private ProgrammingListener programmingListener;

    /** Interfaz para manejar eventos de programacion */
    public interface ProgrammingListener {
        void onProgrammingStarted();

        void onProgrammingProgress(String message, int progress);

        void onProgrammingCompleted(boolean success);

        void onProgrammingError(String errorMessage);
    }

    /** Estados del proceso de programacion */
    public enum ProgrammingStep {
        ERASING_MEMORY,
        PROGRAMMING_ROM,
        PROGRAMMING_EEPROM,
        PROGRAMMING_FUSES,
        PROGRAMMING_FUSES_18F,
        COMPLETED
    }

    /**
     * Constructor del gestor de programacion
     *
     * @param context Contexto de la aplicacion
     */
    public PicProgrammingManager(Context context) {
        this.context = context;
    }

    /**
     * Establece el protocolo de comunicacion
     *
     * @param protocolo Instancia del protocolo P018
     */
    public void setProtocolo(ProtocoloP018 protocolo) {
        this.protocolo = protocolo;
    }

    /**
     * Establece el listener para eventos de programacion
     *
     * @param listener Listener que sera notificado de eventos
     */
    public void setProgrammingListener(ProgrammingListener listener) {
        this.programmingListener = listener;
    }

    /**
     * Programa completamente un chip PIC con el firmware especificado
     *
     * @param chipPIC Chip PIC a programar
     * @param firmware Contenido del archivo HEX
     * @return true si la programacion fue exitosa, false en caso contrario
     */
    public boolean programChip(
            ChipPic chipPIC, String firmware, byte[] IDPic, List<Integer> fusesUsuario) {
        if (protocolo == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return false;
        }

        if (chipPIC == null || firmware == null || firmware.isEmpty()) {
            notifyError(context.getString(R.string.datos_invalidos_para_programac));
            return false;
        }

        notifyStarted();

        try {
            // Paso 1: Borrar memorias
            notifyProgress(context.getString(R.string.borrando_memorias), 10);
            if (!protocolo.borrarMemoriasDelPic()) {
                notifyError(context.getString(R.string.error_borrando_memorias));
                return false;
            }

            // Paso 2: Programar ROM
            notifyProgress(context.getString(R.string.programando_memoria_rom), 30);
            if (!protocolo.programarMemoriaROMDelPic(chipPIC, firmware)) {
                notifyError(context.getString(R.string.error_programando_rom));
                return false;
            }

            // Paso 3: Programar EEPROM si es necesario
            if (chipPIC.isTamanoValidoDeEEPROM()) {
                notifyProgress(context.getString(R.string.programando_memoria_eeprom), 50);
                if (!protocolo.programarMemoriaEEPROMDelPic(chipPIC, firmware)) {
                    notifyError(context.getString(R.string.error_programando_eeprom));
                    return false;
                }
            }

            // Paso 4: Programar Fuses
            notifyProgress(context.getString(R.string.programando_fuses_id), 70);
            if (!protocolo.programarFusesIDDelPic(chipPIC, firmware, IDPic, fusesUsuario)) {
                notifyError(context.getString(R.string.error_programando_fuses));
                return false;
            }

            // Paso 5: Programar Fuses adicionales para PIC18F
            if (chipPIC.getTipoDeNucleoBit() == 16) {
                notifyProgress(context.getString(R.string.programando_fuses_18f), 90);
                if (!protocolo.programarFusesDePics18F()) {
                    notifyError(context.getString(R.string.error_programando_fuses_18f));
                    return false;
                }
            }

            // Completado
            notifyProgress(context.getString(R.string.programacion_completada), 100);
            notifyCompleted(true);
            return true;

        } catch (Exception e) {
            notifyError(context.getString(R.string.error_inesperado) + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Lee la memoria ROM del chip PIC
     *
     * @param chipPIC Chip PIC del cual leer
     * @return Contenido de la memoria ROM como string
     */
    public String readRomMemory(ChipPic chipPIC) {
        if (protocolo == null || chipPIC == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return "";
        }

        try {
            return protocolo.leerMemoriaROMDelPic(chipPIC);
        } catch (Exception e) {
            notifyError(
                    context.getString(R.string.error_leyendo_memoria_rom) + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Lee la memoria EEPROM del chip PIC
     *
     * @param chipPIC Chip PIC del cual leer
     * @return Contenido de la memoria EEPROM como string
     */
    public String readEepromMemory(ChipPic chipPIC) {
        if (protocolo == null || chipPIC == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return "";
        }

        try {
            if (chipPIC.isTamanoValidoDeEEPROM()) {
                return protocolo.leerMemoriaEEPROMDelPic(chipPIC);
            }
            return "";
        } catch (Exception e) {
            notifyError(
                    context.getString(R.string.error_leyendo_memoria_eeprom)
                            + ": "
                            + e.getMessage());
            return "";
        }
    }

    /**
     * Borra todas las memorias del chip PIC
     *
     * @return true si el borrado fue exitoso, false en caso contrario
     */
    public boolean eraseMemory() {
        if (protocolo == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return false;
        }

        try {
            return protocolo.borrarMemoriasDelPic();
        } catch (Exception e) {
            notifyError(context.getString(R.string.error_borrando_memoria) + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si la memoria EEPROM esta borrada
     *
     * @return true si la memoria esta borrada, false si contiene datos
     */
    public boolean verifyMemoryErased() {
        if (protocolo == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return false;
        }

        try {
            return protocolo.verificarSiEstaBarradaLaMemoriaEEPROMDelDelPic();
        } catch (Exception e) {
            notifyError(
                    context.getString(R.string.error_verificando_memoria) + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Detecta si hay un chip PIC en el socket del programador
     *
     * @return true si se detecta un chip, false en caso contrario
     */
    public boolean detectChipInSocket() {
        if (protocolo == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return false;
        }

        try {
            return protocolo.detectarPicEnElSocket();
        } catch (Exception e) {
            notifyError(context.getString(R.string.error_detectando_chip) + ": " + e.getMessage());
            return false;
        }
    }

    /** Notifica el inicio de la programacion */
    private void notifyStarted() {
        if (programmingListener != null) {
            programmingListener.onProgrammingStarted();
        }
    }

    /**
     * Notifica el progreso de la programacion
     *
     * @param message Mensaje descriptivo del paso actual
     * @param progress Progreso en porcentaje (0-100)
     */
    private void notifyProgress(String message, int progress) {
        if (programmingListener != null) {
            programmingListener.onProgrammingProgress(message, progress);
        }
    }

    /**
     * Notifica la finalizacion de la programacion
     *
     * @param success true si fue exitosa, false en caso contrario
     */
    private void notifyCompleted(boolean success) {
        if (programmingListener != null) {
            programmingListener.onProgrammingCompleted(success);
        }
    }

    /**
     * Notifica un error durante la programacion
     *
     * @param errorMessage Mensaje de error
     */
    private void notifyError(String errorMessage) {
        if (programmingListener != null) {
            programmingListener.onProgrammingError(errorMessage);
        }
    }
}
