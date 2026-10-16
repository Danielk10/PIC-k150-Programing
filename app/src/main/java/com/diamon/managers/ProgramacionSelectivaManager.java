package com.diamon.managers;

import android.app.AlertDialog;
import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.pic.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de Programación Selectiva y Verificación de Borrado Diferenciada (Blank Check Dual).
 *
 * Ofrece al usuario:
 * 1. Selección modal de estrategia de grabación:
 *    - "🚀 Grabación Completa" (ROM + EEPROM + Fuses) [Por defecto]
 *    - "💾 Grabar Solo Flash ROM" (usa programRomOnly)
 *    - "📦 Grabar Solo EEPROM" (usa programEepromOnly)
 *    - "⚙️ Grabar Solo Fusibles de Configuración" (usa programConfigOnly)
 * 2. Reporte dual detallado de borrado de memoria (ROM y EEPROM separadas).
 */
public class ProgramacionSelectivaManager {

    /**
     * Modalidades de programación selectiva disponibles.
     */
    public enum TipoProgramacion {
        COMPLETA(
                R.string.programar_todo,
                R.string.prog_type_all_desc
        ),
        SOLO_ROM(
                R.string.programar_solo_rom,
                R.string.prog_type_rom_desc
        ),
        SOLO_EEPROM(
                R.string.programar_solo_eeprom,
                R.string.prog_type_eeprom_desc
        ),
        SOLO_CONFIG(
                R.string.programar_solo_config,
                R.string.prog_type_config_desc
        );

        private final int tituloResId;
        private final int descripcionResId;

        TipoProgramacion(int tituloResId, int descripcionResId) {
            this.tituloResId = tituloResId;
            this.descripcionResId = descripcionResId;
        }

        public int getTituloResId() {
            return tituloResId;
        }

        public int getDescripcionResId() {
            return descripcionResId;
        }

        public String getTitulo(Context context) {
            return context != null ? context.getString(tituloResId) : getTitulo();
        }

        public String getDescripcion(Context context) {
            return context != null ? context.getString(descripcionResId) : getDescripcion();
        }

        public String getTitulo() {
            switch (this) {
                case COMPLETA: return "Grabar todo";
                case SOLO_ROM: return "Grabar ROM";
                case SOLO_EEPROM: return "Grabar EEPROM";
                case SOLO_CONFIG: return "Grabar configuración";
                default: return name();
            }
        }

        public String getDescripcion() {
            switch (this) {
                case COMPLETA: return "ROM, EEPROM y Configuración";
                case SOLO_ROM: return "Memoria Flash";
                case SOLO_EEPROM: return "Datos EEPROM";
                case SOLO_CONFIG: return "Fusibles y bits de configuración";
                default: return name();
            }
        }
    }

    /**
     * Listener para recibir la opción elegida por el usuario.
     */
    public interface OnProgramacionSeleccionadaListener {
        void onOpcionSeleccionada(TipoProgramacion tipo);
    }

    private final Context context;

    public ProgramacionSelectivaManager(Context context) {
        this.context = context;
    }

    /**
     * Calcula la lista de modalidades de programación disponibles según las regiones
     * presentes en el HEX/BIN cargado y la configuración del usuario.
     *
     * @param chipPIC            Microcontrolador actual (para validar si soporta EEPROM).
     * @param datosPicProcesados Datos procesados del HEX/BIN (opcional).
     * @param fusesConfigured    Indica si el usuario configuró fuses/ID manualmente.
     * @return Lista de opciones disponibles (siempre incluye COMPLETA al inicio).
     */
    public List<TipoProgramacion> obtenerOpcionesDisponibles(
            ChipPic chipPIC,
            DatosPicProcesados datosPicProcesados,
            boolean fusesConfigured) {

        boolean hasRom = true;
        boolean hasEeprom = (chipPIC == null || chipPIC.isTamanoValidoDeEEPROM());
        boolean hasConfig = true;

        if (datosPicProcesados != null) {
            hasRom = datosPicProcesados.tieneRomEnHex() || datosPicProcesados.tieneRomData();
            hasEeprom = (chipPIC == null || chipPIC.isTamanoValidoDeEEPROM())
                    && (datosPicProcesados.tieneEepromEnHex() || datosPicProcesados.tieneEepromData());
            hasConfig = datosPicProcesados.tieneConfigEnHex() || datosPicProcesados.tieneConfigData() || fusesConfigured;
        }

        List<TipoProgramacion> opciones = new ArrayList<>();
        // Grabación Completa SIEMPRE de primero
        opciones.add(TipoProgramacion.COMPLETA);

        if (hasRom) {
            opciones.add(TipoProgramacion.SOLO_ROM);
        }
        if (hasEeprom) {
            opciones.add(TipoProgramacion.SOLO_EEPROM);
        }
        if (hasConfig) {
            opciones.add(TipoProgramacion.SOLO_CONFIG);
        }

        return opciones;
    }

    /**
     * Muestra el menú modal con las opciones de programación selectiva filtradas dinámicamente
     * de acuerdo con las regiones presentes en el archivo HEX o binario cargado (ROM, EEPROM, Config).
     * Si el archivo solo contiene una región o no hay opciones diferenciadas, ejecuta directamente
     * sin mostrar el diálogo modal.
     *
     * @param context            Contexto de la actividad.
     * @param chipPIC            Microcontrolador actual.
     * @param datosPicProcesados Datos procesados del archivo HEX/BIN.
     * @param fusesConfigured    Si el usuario configuró fuses/ID manualmente.
     * @param listener           Callback invocado al seleccionar una opción.
     */
    public void mostrarMenuSeleccion(
            Context context,
            ChipPic chipPIC,
            DatosPicProcesados datosPicProcesados,
            boolean fusesConfigured,
            OnProgramacionSeleccionadaListener listener) {
        if (context == null || listener == null) return;

        List<TipoProgramacion> opciones = obtenerOpcionesDisponibles(chipPIC, datosPicProcesados, fusesConfigured);

        // Si solo hay una opción (Programar Todo) o el firmware no tiene partes diferenciadas,
        // ejecutamos directo sin desplegar diálogo
        if (opciones.size() <= 1) {
            listener.onOpcionSeleccionada(TipoProgramacion.COMPLETA);
            return;
        }

        CharSequence[] items = new CharSequence[opciones.size()];
        for (int i = 0; i < opciones.size(); i++) {
            items[i] = opciones.get(i).getTitulo(context);
        }

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.seleccionar_operacion))
                .setItems(items, (dialog, which) -> {
                    if (which >= 0 && which < opciones.size()) {
                        listener.onOpcionSeleccionada(opciones.get(which));
                    }
                })
                .setNegativeButton(context.getString(R.string.cancelar), null)
                .show();
    }

    /**
     * Sobrecarga de compatibilidad: muestra opciones de programación selectiva para un chip.
     *
     * @param context  Contexto de la actividad.
     * @param chipPIC  Microcontrolador actual.
     * @param listener Callback invocado al seleccionar una opción.
     */
    public void mostrarMenuSeleccion(Context context, ChipPic chipPIC, OnProgramacionSeleccionadaListener listener) {
        mostrarMenuSeleccion(context, chipPIC, null, false, listener);
    }

    /**
     * Muestra el menú modal con las opciones de programación selectiva por defecto.
     *
     * @param context  Contexto de la actividad.
     * @param listener Callback invocado al seleccionar una opción.
     */
    public void mostrarMenuSeleccion(Context context, OnProgramacionSeleccionadaListener listener) {
        mostrarMenuSeleccion(context, null, null, false, listener);
    }

    /**
     * Ejecuta la programación seleccionada delegando en PicProgrammingManager.
     *
     * @param tipo               Tipo de programación a ejecutar.
     * @param programmingManager Gestor de programación.
     * @param chipPIC            Microcontrolador seleccionado.
     * @param firmware           Contenido del archivo HEX.
     * @param idPic              Bytes del ID.
     * @param fusesUsuario       Lista de fusibles configurados.
     * @return true si la operación finalizó con éxito.
     */
    public boolean ejecutarProgramacion(
            TipoProgramacion tipo,
            PicProgrammingManager programmingManager,
            ChipPic chipPIC,
            String firmware,
            byte[] idPic,
            List<Integer> fusesUsuario) {

        if (programmingManager == null || chipPIC == null || firmware == null || firmware.isEmpty()) {
            return false;
        }

        switch (tipo) {
            case SOLO_ROM:
                return programmingManager.programRomOnly(chipPIC, firmware);

            case SOLO_EEPROM:
                return programmingManager.programEepromOnly(chipPIC, firmware);

            case SOLO_CONFIG:
                return programmingManager.programConfigOnly(chipPIC, firmware, idPic, fusesUsuario);

            case COMPLETA:
            default:
                return programmingManager.programChip(chipPIC, firmware, idPic, fusesUsuario);
        }
    }

    /**
     * Genera un reporte detallado e individualizado de la verificación de borrado dual (ROM + EEPROM).
     *
     * @param context   Contexto para recursos (opcional).
     * @param resultado Resultado de la verificación de borrado de PicProgrammingManager.
     * @param chip      Microcontrolador objetivo.
     * @return Lista de líneas formateadas para el log.
     */
    public List<String> formatearReporteBlankCheckDual(
            Context context,
            PicProgrammingManager.ResultadoVerificacionBorrado resultado,
            ChipPic chip) {

        List<String> lineas = new ArrayList<>();
        if (resultado == null) {
            lineas.add(context != null ? context.getString(R.string.blank_check_err_null_result) : "❌ Error en verificación de borrado: resultado nulo");
            return lineas;
        }

        if (resultado.error != null && !resultado.error.isEmpty()) {
            lineas.add(context != null ? context.getString(R.string.blank_check_err_verifying, resultado.error) : ("❌ Error verificando borrado: " + resultado.error));
            return lineas;
        }

        lineas.add(context != null ? context.getString(R.string.blank_check_dual_header) : "📋 Verificación de Borrado Diferenciada:");

        // 1. Diagnóstico de Memoria ROM
        int coreBits = 14;
        if (chip != null) {
            try {
                coreBits = chip.getTipoDeNucleoBit();
            } catch (com.diamon.excepciones.ChipConfigurationException ignored) {
            }
        }
        int blankWord = (~(0xFFFF << coreBits)) & 0xFFFF;
        String blankHex = String.format("0x%04X", blankWord);

        if (resultado.romEnBlanco) {
            lineas.add(context != null ? context.getString(R.string.blank_check_rom_clean, blankHex) : ("  ✓ ROM limpia al 100% - " + blankHex));
        } else {
            lineas.add(context != null ? context.getString(R.string.blank_check_rom_has_data) : "  ⚠ ROM con datos previos detectados - No está en blanco");
        }

        // 2. Diagnóstico de Memoria EEPROM
        boolean hasEeprom = (chip != null && chip.isTamanoValidoDeEEPROM());
        if (hasEeprom) {
            if (resultado.eepromEnBlanco) {
                lineas.add(context != null ? context.getString(R.string.blank_check_eeprom_clean) : "  ✓ EEPROM limpia al 100% - 0xFF");
            } else {
                lineas.add(context != null ? context.getString(R.string.blank_check_eeprom_has_data) : "  ⚠ EEPROM con datos previos detectados - No está en blanco");
            }
        }

        // 3. Método y protocolo utilizado
        if (resultado.metodoUtilizado != null && !resultado.metodoUtilizado.isEmpty()) {
            lineas.add(context != null ? context.getString(R.string.blank_check_method_label, resultado.metodoUtilizado) : ("  ℹ Método: " + resultado.metodoUtilizado));
        }

        // 4. Conclusión global
        if (resultado.chipEnBlanco()) {
            lineas.add(context != null ? context.getString(R.string.blank_check_ready_to_program) : "✓ Microcontrolador listo para grabar - 100% Borrado");
        } else {
            lineas.add(context != null ? context.getString(R.string.blank_check_erase_required) : "⚠ Se requiere borrado previo antes de grabar");
        }

        return lineas;
    }

    public List<String> formatearReporteBlankCheckDual(
            PicProgrammingManager.ResultadoVerificacionBorrado resultado,
            ChipPic chip) {
        return formatearReporteBlankCheckDual(null, resultado, chip);
    }
}
