package com.diamon.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.diamon.chip.ChipPic;
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
                "🚀 Grabación Completa",
                "Borra, graba ROM, EEPROM y Fusibles de Configuración"
        ),
        SOLO_ROM(
                "💾 Grabar Solo Flash ROM",
                "Graba la memoria Flash sin alterar EEPROM ni Fusibles"
        ),
        SOLO_EEPROM(
                "📦 Grabar Solo EEPROM",
                "Escribe datos persistentes sin reescribir la memoria de código"
        ),
        SOLO_CONFIG(
                "⚙️ Grabar Solo Fusibles de Configuración",
                "Actualiza fuses y oscilador sin tocar memorias ROM/EEPROM"
        );

        private final String titulo;
        private final String descripcion;

        TipoProgramacion(String titulo, String descripcion) {
            this.titulo = titulo;
            this.descripcion = descripcion;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getDescripcion() {
            return descripcion;
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
     * Muestra el menú modal con las opciones de programación selectiva.
     * Garantiza que la Grabación Completa aparezca siempre en primer lugar.
     *
     * @param context  Contexto de la actividad.
     * @param chipPIC  Microcontrolador actual (opcional, para omitir EEPROM si no dispone).
     * @param listener Callback invocado al seleccionar una opción.
     */
    public void mostrarMenuSeleccion(Context context, ChipPic chipPIC, OnProgramacionSeleccionadaListener listener) {
        if (context == null || listener == null) return;

        List<TipoProgramacion> opciones = new ArrayList<>();
        // Grabación Completa SIEMPRE de primero
        opciones.add(TipoProgramacion.COMPLETA);
        opciones.add(TipoProgramacion.SOLO_ROM);
        if (chipPIC == null || chipPIC.isTamanoValidoDeEEPROM()) {
            opciones.add(TipoProgramacion.SOLO_EEPROM);
        }
        opciones.add(TipoProgramacion.SOLO_CONFIG);

        CharSequence[] items = new CharSequence[opciones.size()];
        for (int i = 0; i < opciones.size(); i++) {
            items[i] = opciones.get(i).getTitulo() + "\n   " + opciones.get(i).getDescripcion();
        }

        new AlertDialog.Builder(context)
                .setTitle("🎛️ " + context.getString(R.string.seleccionar_operacion))
                .setItems(items, (dialog, which) -> {
                    if (which >= 0 && which < opciones.size()) {
                        listener.onOpcionSeleccionada(opciones.get(which));
                    }
                })
                .setNegativeButton(context.getString(R.string.cancelar), null)
                .show();
    }

    /**
     * Muestra el menú modal con las 4 opciones de programación selectiva.
     *
     * @param context  Contexto de la actividad.
     * @param listener Callback invocado al seleccionar una opción.
     */
    public void mostrarMenuSeleccion(Context context, OnProgramacionSeleccionadaListener listener) {
        mostrarMenuSeleccion(context, null, listener);
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
     * @param resultado Resultado de la verificación de borrado de PicProgrammingManager.
     * @param chip      Microcontrolador objetivo.
     * @return Lista de líneas formateadas para el log.
     */
    public List<String> formatearReporteBlankCheckDual(
            PicProgrammingManager.ResultadoVerificacionBorrado resultado,
            ChipPic chip) {

        List<String> lineas = new ArrayList<>();
        if (resultado == null) {
            lineas.add("❌ Error en verificación de borrado: resultado nulo");
            return lineas;
        }

        if (resultado.error != null && !resultado.error.isEmpty()) {
            lineas.add("❌ Error verificando borrado: " + resultado.error);
            return lineas;
        }

        lineas.add("📋 Verificación de Borrado Diferenciada (Blank Check Dual):");

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
            lineas.add("  ✓ ROM limpia al 100% (" + blankHex + ")");
        } else {
            lineas.add("  ⚠ ROM con datos previos detectados (No está en blanco)");
        }

        // 2. Diagnóstico de Memoria EEPROM
        boolean hasEeprom = (chip != null && chip.isTamanoValidoDeEEPROM());
        if (hasEeprom) {
            if (resultado.eepromEnBlanco) {
                lineas.add("  ✓ EEPROM limpia al 100% (0xFF)");
            } else {
                lineas.add("  ⚠ EEPROM con datos previos detectados (No está en blanco)");
            }
        }

        // 3. Método y protocolo utilizado
        if (resultado.metodoUtilizado != null && !resultado.metodoUtilizado.isEmpty()) {
            lineas.add("  ℹ Método: " + resultado.metodoUtilizado);
        }

        // 4. Conclusión global
        if (resultado.chipEnBlanco()) {
            lineas.add("✓ Microcontrolador listo para grabar (100% Borrado)");
        } else {
            lineas.add("⚠ Se requiere borrado previo antes de grabar");
        }

        return lineas;
    }
}
