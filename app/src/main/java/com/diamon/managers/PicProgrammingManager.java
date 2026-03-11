package com.diamon.managers;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.protocolo.ProtocoloP18A;
import com.diamon.pic.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de operaciones de programacion de microcontroladores PIC. Encapsula
 * todas las operaciones
 * relacionadas con la programacion, lectura, borrado y verificacion de memoria
 * de chips PIC.
 */
public class PicProgrammingManager {

    private final Context context;
    private ProtocoloP18A protocolo;

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

    /** Resultado de la verificación de borrado del chip. */
    public static class ResultadoVerificacionBorrado {
        public final boolean romEnBlanco;
        public final boolean eepromEnBlanco;
        public final boolean fallbackUsado;
        public final String metodoUtilizado;
        public final String error;

        public ResultadoVerificacionBorrado(
                boolean romEnBlanco,
                boolean eepromEnBlanco,
                boolean fallbackUsado,
                String metodoUtilizado,
                String error) {
            this.romEnBlanco = romEnBlanco;
            this.eepromEnBlanco = eepromEnBlanco;
            this.fallbackUsado = fallbackUsado;
            this.metodoUtilizado = metodoUtilizado;
            this.error = error;
        }

        public boolean chipEnBlanco() {
            return romEnBlanco && eepromEnBlanco;
        }
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
    public void setProtocolo(ProtocoloP18A protocolo) {
        this.protocolo = protocolo;
    }

    /**
     * Obtiene el protocolo de comunicación actual.
     *
     * @return Instancia del protocolo, o null si no está configurado
     */
    public ProtocoloP18A getProtocolo() {
        return protocolo;
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
     * @param chipPIC  Chip PIC a programar
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
            boolean eraseOk = protocolo.borrarMemoriasDelPic();
            if (!eraseOk) {
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
     * Programa solo la memoria ROM del chip PIC con borrado previo
     *
     * @param chipPIC  Chip PIC a programar
     * @param firmware Contenido del archivo HEX
     * @return true si la programacion fue exitosa, false en caso contrario
     */
    public boolean programRomOnly(ChipPic chipPIC, String firmware) {
        if (protocolo == null || chipPIC == null || firmware == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return false;
        }

        try {
            notifyStarted();

            // Paso 1: Borrar chip (siempre requerido para escribir ROM)
            notifyProgress(context.getString(R.string.borrando_memorias), 10);
            if (!protocolo.borrarMemoriasDelPic()) {
                notifyError(context.getString(R.string.error_borrando_memoria));
                return false;
            }

            // Paso 2: Programar ROM
            notifyProgress(context.getString(R.string.programando_memoria_rom), 50);
            if (!protocolo.programarMemoriaROMDelPic(chipPIC, firmware)) {
                notifyError(context.getString(R.string.error_programando_rom));
                return false;
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
     * Programa solo la memoria EEPROM del chip PIC sin borrar la ROM
     *
     * @param chipPIC  Chip PIC a programar
     * @param firmware Contenido del archivo HEX
     * @return true si la programacion fue exitosa, false en caso contrario
     */
    public boolean programEepromOnly(ChipPic chipPIC, String firmware) {
        if (protocolo == null || chipPIC == null || firmware == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return false;
        }

        if (!chipPIC.isTamanoValidoDeEEPROM()) {
            notifyError("Chip no tiene memoria EEPROM");
            return false;
        }

        try {
            notifyStarted();

            // Programar EEPROM
            notifyProgress(context.getString(R.string.programando_memoria_eeprom), 50);
            if (!protocolo.programarMemoriaEEPROMDelPic(chipPIC, firmware)) {
                notifyError(context.getString(R.string.error_programando_eeprom));
                return false;
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
     * Programa solo la configuración (Fuses e ID) del chip PIC sin borrar la ROM
     *
     * @return true si la programacion fue exitosa, false en caso contrario
     */
    public boolean programConfigOnly(ChipPic chipPIC, String firmware, byte[] IDPic, List<Integer> fusesUsuario) {
        if (protocolo == null || chipPIC == null || firmware == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return false;
        }

        try {
            notifyStarted();

            // Programar Fuses e ID
            notifyProgress(context.getString(R.string.programando_fuses_id), 50);
            if (!protocolo.programarFusesIDDelPic(chipPIC, firmware, IDPic, fusesUsuario)) {
                notifyError(context.getString(R.string.error_programando_fuses));
                return false;
            }

            // Programar Fuses adicionales para PIC18F
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
     * Lee la memoria de configuración del chip PIC
     *
     * @param chipPIC Chip PIC del cual leer
     * @return Contenido de la memoria de configuración como string
     */
    public String readConfigData(ChipPic chipPIC) {
        if (protocolo == null || chipPIC == null) {
            notifyError(context.getString(R.string.protocolo_no_inicializado));
            return "";
        }

        try {
            return protocolo.leerDatosDeConfiguracionDelPic();
        } catch (Exception e) {
            notifyError("Error leyendo datos de configuración: " + e.getMessage());
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

    /**
     * Verifica si el chip está realmente en blanco.
     *
     * Estrategia actual:
     * 1) Siempre realiza lectura comparativa independiente (ROM/EEPROM/FUSEblank).
     * 2) Ejecuta blank-check nativo solo como diagnóstico, sin afectar el veredicto.
     *
     * Nota: este flujo no persiste ni mezcla datos con la lectura de memoria para UI.
     */
    public ResultadoVerificacionBorrado verificarBorradoCompleto(ChipPic chipPIC) {
        if (protocolo == null) {
            String error = context.getString(R.string.protocolo_no_inicializado);
            notifyError(error);
            return new ResultadoVerificacionBorrado(false, false, true, "Sin protocolo", error);
        }

        if (chipPIC == null) {
            String error = context.getString(R.string.no_hay_chip_seleccionado);
            notifyError(error);
            return new ResultadoVerificacionBorrado(false, false, true, "Sin chip", error);
        }

        // Veredicto principal: lectura comparativa independiente.
        boolean romBlankLectura = verificarRomVaciaPorLectura(chipPIC);
        boolean eepromBlankLectura = true;
        if (chipPIC.isTamanoValidoDeEEPROM()) {
            eepromBlankLectura = verificarEepromVaciaPorLectura(chipPIC);
        }
        boolean configBlankLectura = verificarConfiguracionVaciaPorLectura(chipPIC);
        boolean romFinal = romBlankLectura && configBlankLectura;

        // Diagnóstico opcional del blank-check nativo (no bloqueante).
        String diagnosticoNativo = "nativo no ejecutado";
        try {
            boolean romBlankNativo = protocolo.verificarSiEstaBarradaLaMemoriaROMDelDelPic(chipPIC);
            boolean eepromBlankNativo = true;
            if (chipPIC.isTamanoValidoDeEEPROM()) {
                eepromBlankNativo = protocolo.verificarSiEstaBarradaLaMemoriaEEPROMDelDelPic();
            }
            diagnosticoNativo = (romBlankNativo && eepromBlankNativo)
                    ? "nativo=blanco"
                    : "nativo=con_datos";
        } catch (Exception e) {
            diagnosticoNativo = "nativo_error=" + e.getMessage();
        }

        String metodo = "Lectura comparativa aislada (ROM/EEPROM/FUSEblank), " + diagnosticoNativo;
        return new ResultadoVerificacionBorrado(romFinal, eepromBlankLectura, true, metodo, null);
    }

    /** Verifica ROM vacía por lectura comparando palabra blank por núcleo. */
    private boolean verificarRomVaciaPorLectura(ChipPic chipPic) {
        try {
            String romHex = protocolo.leerMemoriaROMDelPic(chipPic);
            if (romHex == null || romHex.startsWith("Error") || romHex.isEmpty()) {
                return false;
            }

            int coreBits = chipPic.getTipoDeNucleoBit();
            int blankWord = (~(0xFFFF << coreBits)) & 0xFFFF;
            String blankWordHex = String.format("%04X", blankWord);

            String romNormalizada = romHex.replaceAll("\\s+", "").toUpperCase();
            int max = romNormalizada.length() - (romNormalizada.length() % 4);
            for (int i = 0; i < max; i += 4) {
                String palabra = romNormalizada.substring(i, i + 4);
                if (!blankWordHex.equals(palabra)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Verifica EEPROM vacía por lectura comprobando bytes 0xFF. */
    private boolean verificarEepromVaciaPorLectura(ChipPic chipPic) {
        try {
            String eepromHex = protocolo.leerMemoriaEEPROMDelPic(chipPic);
            if (eepromHex == null || eepromHex.startsWith("Error") || eepromHex.isEmpty()) {
                return false;
            }

            String eepromNormalizada = eepromHex.replaceAll("\\s+", "").toUpperCase();
            int max = eepromNormalizada.length() - (eepromNormalizada.length() % 2);
            for (int i = 0; i < max; i += 2) {
                String byteHex = eepromNormalizada.substring(i, i + 2);
                if (!"FF".equals(byteHex)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Verifica que configuración/fuses coincidan con FUSEblank del chip. */
    private boolean verificarConfiguracionVaciaPorLectura(ChipPic chipPic) {
        try {
            String configData = protocolo.leerDatosDeConfiguracionDelPic();
            if (configData == null || configData.startsWith("Error") || configData.length() < 24) {
                return false;
            }

            int[] fusesBlank = chipPic.getFuseBlank();
            if (fusesBlank == null || fusesBlank.length == 0) {
                return true;
            }

            String configNormalizada = configData.replaceAll("\\s+", "").toUpperCase();
            int maxFuses = Math.min(fusesBlank.length, 7);
            for (int i = 0; i < maxFuses; i++) {
                int inicio = 20 + (i * 4);
                if (inicio + 4 > configNormalizada.length()) {
                    break;
                }
                String fuseLeidoLE = configNormalizada.substring(inicio, inicio + 4);
                String fuseLeido = fuseLeidoLE.substring(2, 4) + fuseLeidoLE.substring(0, 2);
                String fuseBlankHex = String.format("%04X", fusesBlank[i]);
                if (!fuseBlankHex.equals(fuseLeido)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
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
     * @param message  Mensaje descriptivo del paso actual
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
