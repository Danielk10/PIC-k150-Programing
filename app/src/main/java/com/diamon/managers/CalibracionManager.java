package com.diamon.managers;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.diamon.chip.ChipPic;
import com.diamon.protocolo.ProtocoloP18A;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestor integral de Calibración de Oscilador (OSCCAL), arquitectura PIC10F
 * y Vector de Depuración ICD (In-Circuit Debugger).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Detección de necesidad de calibración por silicio y familia de chip.</li>
 *   <li>Lectura y respaldo preventivo de OSCCAL antes de borrado masivo.</li>
 *   <li>Restauración de OSCCAL tras borrado masivo (específica para PIC10F y PIC12F/16F).</li>
 *   <li>Diálogo interactivo de inspección, edición y reprogramación de OSCCAL.</li>
 *   <li>Diálogo interactivo de lectura y escritura del vector de depuración ICD (24 bits).</li>
 * </ul>
 */
public class CalibracionManager {

    private final Context context;
    private ProtocoloP18A protocolo;
    private Integer ultimoOsccalPreservado = null;
    private Integer ultimoVectorIcd = null;

    /**
     * Resultado del proceso de borrado seguro con preservación de OSCCAL.
     */
    public static class ResultadoBorradoSeguro {
        public final boolean borradoExitoso;
        public final boolean requiereCalibracion;
        public final Integer osccalPreservado;
        public final boolean restauracionExitosa;
        public final String mensaje;

        public ResultadoBorradoSeguro(boolean borradoExitoso, boolean requiereCalibracion,
                                     Integer osccalPreservado, boolean restauracionExitosa,
                                     String mensaje) {
            this.borradoExitoso = borradoExitoso;
            this.requiereCalibracion = requiereCalibracion;
            this.osccalPreservado = osccalPreservado;
            this.restauracionExitosa = restauracionExitosa;
            this.mensaje = mensaje;
        }
    }

    public CalibracionManager(Context context) {
        this.context = context;
    }

    public CalibracionManager(Context context, ProtocoloP18A protocolo) {
        this.context = context;
        this.protocolo = protocolo;
    }

    public void setProtocolo(ProtocoloP18A protocolo) {
        this.protocolo = protocolo;
    }

    public ProtocoloP18A getProtocolo() {
        return protocolo;
    }

    public Integer getUltimoOsccalPreservado() {
        return ultimoOsccalPreservado;
    }

    public void setUltimoOsccalPreservado(Integer cal) {
        this.ultimoOsccalPreservado = cal;
    }

    public Integer getUltimoVectorIcd() {
        return ultimoVectorIcd;
    }

    public void setUltimoVectorIcd(Integer vec) {
        this.ultimoVectorIcd = vec;
    }

    // -------------------------------------------------------------------------
    // Verificación de Familias y Detección de Calibración
    // -------------------------------------------------------------------------

    /**
     * Determina si un chip pertenece a la familia PIC10F (10F200..10F222).
     */
    public static boolean isFamilia10F(ChipPic chip) {
        if (chip == null || chip.getNombreDelPic() == null) {
            return false;
        }
        String nombre = chip.getNombreDelPic().toUpperCase();
        return nombre.startsWith("10F") || nombre.startsWith("PIC10F");
    }

    /**
     * Determina si un chip requiere gestión y preservación de valor de calibración (OSCCAL).
     */
    public static boolean requiereCalibracion(ChipPic chip) {
        if (chip == null) {
            return false;
        }
        if (isFamilia10F(chip)) {
            return true;
        }
        if (chip.isFlagCalibration()) {
            return true;
        }
        // Verificación de respaldo para chips conocidos que usan OSCCAL en última palabra de ROM
        String nombre = (chip.getNombreDelPic() != null) ? chip.getNombreDelPic().toUpperCase() : "";
        return nombre.startsWith("12F629") || nombre.startsWith("12F675")
                || nombre.startsWith("16F630") || nombre.startsWith("16F676")
                || nombre.startsWith("12C508") || nombre.startsWith("12C509")
                || nombre.startsWith("12F508") || nombre.startsWith("12F509");
    }

    // -------------------------------------------------------------------------
    // Parseo y Formateo Hexadecimal
    // -------------------------------------------------------------------------

    /**
     * Parsea un string hexadecimal de 4 caracteres proveniente del comando de lectura de config
     * y reconstruye la palabra de calibración de 12 o 14 bits.
     *
     * @param rawHex Cadena hex de 4 caracteres (ej. "4834" en LE o "3448")
     * @return Valor numérico de la palabra de calibración o -1 si es inválido
     */
    public static int parsearCalibracionHex(String rawHex) {
        if (rawHex == null || rawHex.length() < 4) {
            return -1;
        }
        String hex = rawHex.trim();
        if (hex.length() > 4) {
            hex = hex.substring(hex.length() - 4);
        }
        try {
            int b0 = Integer.parseInt(hex.substring(0, 2), 16);
            int b1 = Integer.parseInt(hex.substring(2, 4), 16);

            // Detección de opcode RETLW (0x34xx en PIC12F/PIC16F)
            if ((b1 & 0xFE) == 0x34) {
                return (b1 << 8) | b0; // LE: b0 es el byte bajo, b1 es el opcode 0x34
            } else if ((b0 & 0xFE) == 0x34) {
                return (b0 << 8) | b1; // BE directo
            }

            // Para PIC10F (12 bits: 0x000 a 0x0FFF), reconstruir desde LE
            return (b1 << 8) | b0;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parsea una dirección de 24 bits para el vector de depuración ICD.
     *
     * @param rawInput Texto ingresado por el usuario (ej. "0x000020", "20", "000020")
     * @return Dirección entera en el rango 0..0xFFFFFF o -1 si es inválida
     */
    public static int parsearDireccionIcdHex(String rawInput) {
        if (rawInput == null) {
            return -1;
        }
        String clean = rawInput.trim();
        if (clean.toLowerCase().startsWith("0x")) {
            clean = clean.substring(2);
        }
        if (clean.isEmpty() || clean.length() > 6) {
            return -1;
        }
        try {
            int addr = Integer.parseInt(clean, 16);
            if (addr >= 0 && addr <= 0xFFFFFF) {
                return addr;
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Formatea un valor de calibración numérico a representación legible.
     */
    public static String formatearCalibracionHex(int valor, boolean es10F) {
        if (valor < 0) {
            return "No disponible";
        }
        if (es10F) {
            return String.format("0x%04X", valor & 0x0FFF);
        }
        int lowByte = valor & 0xFF;
        return String.format("0x%04X (RETLW 0x%02X)", valor & 0x3FFF, lowByte);
    }

    /**
     * Formatea una dirección de vector ICD a 24 bits hexadecimal.
     */
    public static String formatearDireccionIcdHex(int address) {
        if (address < 0) {
            return "0x000000";
        }
        return String.format("0x%06X", address & 0xFFFFFF);
    }

    // -------------------------------------------------------------------------
    // Preservación Automática y Borrado Seguro
    // -------------------------------------------------------------------------

    /**
     * Lee la calibración actual del microcontrolador y la almacena en el respaldo de memoria.
     *
     * @param chip Microcontrolador objetivo
     * @return Valor numérico de calibración leído o null en caso de error
     */
    public Integer leerYPreservarCalibracion(ChipPic chip) {
        if (protocolo == null) {
            return null;
        }
        String calHex = protocolo.leerDatosDeCalibracionDelPic();
        if (calHex == null || calHex.startsWith("Error")) {
            return null;
        }
        int calVal = parsearCalibracionHex(calHex);
        if (calVal > 0) {
            this.ultimoOsccalPreservado = calVal;
            return calVal;
        }
        return null;
    }

    /**
     * Restaura un valor de calibración específico en el silicio físico.
     *
     * @param chip Microcontrolador objetivo
     * @param cal Valor de calibración a programar
     * @return true si la operación en silicio fue exitosa
     */
    public boolean restaurarCalibracion(ChipPic chip, int cal) {
        if (protocolo == null || chip == null) {
            return false;
        }
        if (isFamilia10F(chip)) {
            // PIC10F: programa los dos registros (Calibración activa y Respaldo)
            return protocolo.programarDatosDeCalibracionDePics10F(cal, cal);
        } else {
            // PIC12F / PIC16F: programa calibración en la última palabra ROM
            return protocolo.programarCalibracionDelPic(chip, cal);
        }
    }

    /**
     * Restaura el último valor de calibración respaldado en RAM si está disponible.
     */
    public boolean restaurarCalibracionPreservada(ChipPic chip) {
        if (ultimoOsccalPreservado == null) {
            return false;
        }
        return restaurarCalibracion(chip, ultimoOsccalPreservado);
    }

    /**
     * Realiza un ciclo de borrado masivo seguro: si el chip utiliza OSCCAL,
     * lee y respalda la calibración previa antes del borrado y la restaura
     * inmediatamente después.
     *
     * @param chip Microcontrolador objetivo
     * @param programmingManager Gestor de programación
     * @return ResultadoBorradoSeguro con el estado detallado de cada fase
     */
    public ResultadoBorradoSeguro borrarConPreservacion(ChipPic chip, PicProgrammingManager programmingManager) {
        if (programmingManager == null) {
            return new ResultadoBorradoSeguro(false, false, null, false, "Gestor de programación nulo");
        }

        boolean reqCal = requiereCalibracion(chip);

        // Si no requiere calibración, borrado estándar directo
        if (!reqCal) {
            boolean borrado = programmingManager.eraseMemory();
            return new ResultadoBorradoSeguro(borrado, false, null, false,
                    borrado ? "Memoria borrada exitosamente" : "Error al borrar memoria");
        }

        // 1. Lectura preventiva de OSCCAL
        Integer osccalLeido = leerYPreservarCalibracion(chip);

        // 2. Ejecutar borrado masivo físico
        boolean borrado = programmingManager.eraseMemory();
        if (!borrado) {
            return new ResultadoBorradoSeguro(false, true, osccalLeido, false,
                    "Fallo durante el borrado físico del chip");
        }

        // 3. Restauración de OSCCAL si se pudo leer el valor de fábrica
        boolean restaurado = false;
        if (osccalLeido != null) {
            restaurado = restaurarCalibracion(chip, osccalLeido);
        }

        String msg;
        if (osccalLeido != null && restaurado) {
            msg = "Memoria borrada exitosamente y OSCCAL restaurado: " +
                    formatearCalibracionHex(osccalLeido, isFamilia10F(chip));
        } else if (osccalLeido != null) {
            msg = "Memoria borrada pero falló la restauración de OSCCAL";
        } else {
            msg = "Memoria borrada. No se pudo leer OSCCAL previo de fábrica";
        }

        return new ResultadoBorradoSeguro(true, true, osccalLeido, restaurado, msg);
    }

    // -------------------------------------------------------------------------
    // Diálogos de Usuario (UI)
    // -------------------------------------------------------------------------

    /**
     * Muestra el diálogo modal para visualización, edición y programación de OSCCAL.
     */
    public void showCalibrationDialog(Activity activity, ChipPic chip) {
        if (activity == null) {
            return;
        }

        final boolean es10F = isFamilia10F(chip);
        final String chipNombre = (chip != null) ? chip.getNombreDelPic() : "PIC";

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(activity, 20), dpToPx(activity, 15),
                dpToPx(activity, 20), dpToPx(activity, 15));
        layout.setBackgroundColor(Color.parseColor("#1E1E2C"));

        // Cabecera descriptiva
        TextView tvSubtitulo = new TextView(activity);
        tvSubtitulo.setText("Microcontrolador: " + chipNombre +
                (es10F ? " (Familia PIC10F - 2 registros CAL)" : " (Palabra OSCCAL en última dirección ROM)"));
        tvSubtitulo.setTextColor(Color.parseColor("#B0B0C0"));
        tvSubtitulo.setTextSize(13);
        layout.addView(tvSubtitulo);

        // Estado del valor en silicio
        TextView tvValorActual = new TextView(activity);
        String valStr = (ultimoOsccalPreservado != null) ?
                formatearCalibracionHex(ultimoOsccalPreservado, es10F) : "No leído aún del chip";
        tvValorActual.setText("\nValor en Silicio: " + valStr);
        tvValorActual.setTextColor(Color.WHITE);
        tvValorActual.setTextSize(15);
        tvValorActual.setTypeface(null, Typeface.BOLD);
        layout.addView(tvValorActual);

        // Campo de entrada hexadecimal
        TextView tvPrompt = new TextView(activity);
        tvPrompt.setText("\nIngrese valor hexadecimal (ej. " + (es10F ? "0C2A" : "3448") + "):");
        tvPrompt.setTextColor(Color.parseColor("#00B0FF"));
        tvPrompt.setTextSize(13);
        layout.addView(tvPrompt);

        EditText etValorHex = new EditText(activity);
        if (ultimoOsccalPreservado != null) {
            etValorHex.setText(String.format(es10F ? "%03X" : "%04X", ultimoOsccalPreservado));
        } else {
            etValorHex.setHint(es10F ? "0FFF" : "3448");
        }
        etValorHex.setTextColor(Color.WHITE);
        etValorHex.setHintTextColor(Color.parseColor("#5A5A70"));
        etValorHex.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        layout.addView(etValorHex);

        // Contenedor de botones de acción
        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setPadding(0, dpToPx(activity, 15), 0, 0);

        Button btnLeer = new Button(activity);
        btnLeer.setText("📥 Leer del Chip");
        btnLeer.setTextColor(Color.WHITE);
        btnLeer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00B0FF")));
        LinearLayout.LayoutParams lpLeer = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpLeer.setMargins(0, 0, dpToPx(activity, 8), 0);
        btnLeer.setLayoutParams(lpLeer);
        btnLayout.addView(btnLeer);

        Button btnGrabar = new Button(activity);
        btnGrabar.setText("💾 Grabar Silicio");
        btnGrabar.setTextColor(Color.WHITE);
        btnGrabar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        LinearLayout.LayoutParams lpGrabar = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnGrabar.setLayoutParams(lpGrabar);
        btnLayout.addView(btnGrabar);

        layout.addView(btnLayout);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("⏱️ Calibración OSCCAL")
                .setView(scrollView)
                .setNeutralButton("Cerrar", null)
                .create();

        btnLeer.setOnClickListener(v -> {
            if (protocolo == null) {
                Toast.makeText(activity, "Programador no conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                Integer cal = leerYPreservarCalibracion(chip);
                activity.runOnUiThread(() -> {
                    if (cal != null) {
                        tvValorActual.setText("\nValor en Silicio: " + formatearCalibracionHex(cal, es10F));
                        etValorHex.setText(String.format(es10F ? "%03X" : "%04X", cal));
                        Toast.makeText(activity, "✓ OSCCAL leído: " + formatearCalibracionHex(cal, es10F), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "❌ Error al leer calibración del chip", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        btnGrabar.setOnClickListener(v -> {
            if (protocolo == null) {
                Toast.makeText(activity, "Programador no conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            String input = etValorHex.getText().toString().trim();
            int valorAGrabar = parsearCalibracionHex(input.length() == 4 ? input : ("0" + input));
            if (valorAGrabar < 0) {
                try {
                    valorAGrabar = Integer.parseInt(input, 16);
                } catch (NumberFormatException e) {
                    valorAGrabar = -1;
                }
            }
            if (valorAGrabar < 0) {
                Toast.makeText(activity, "Valor hexadecimal no válido", Toast.LENGTH_SHORT).show();
                return;
            }

            final int valorFinal = valorAGrabar;
            new Thread(() -> {
                boolean ok = restaurarCalibracion(chip, valorFinal);
                activity.runOnUiThread(() -> {
                    if (ok) {
                        ultimoOsccalPreservado = valorFinal;
                        tvValorActual.setText("\nValor en Silicio: " + formatearCalibracionHex(valorFinal, es10F));
                        Toast.makeText(activity, "✓ OSCCAL grabado exitosamente en el chip", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "❌ Fallo al grabar calibración en silicio", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        dialog.show();
    }

    /**
     * Muestra el diálogo modal para lectura y programación del Vector de Depuración ICD (24 bits).
     */
    public void showDebugVectorDialog(Activity activity, ChipPic chip) {
        if (activity == null) {
            return;
        }

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(activity, 20), dpToPx(activity, 15),
                dpToPx(activity, 20), dpToPx(activity, 15));
        layout.setBackgroundColor(Color.parseColor("#1E1E2C"));

        TextView tvSubtitulo = new TextView(activity);
        tvSubtitulo.setText("Depuración en Circuito (ICD) - Vector de Dirección de 24 bits\n(Rango válido: 0x000000 - 0xFFFFFF)");
        tvSubtitulo.setTextColor(Color.parseColor("#B0B0C0"));
        tvSubtitulo.setTextSize(13);
        layout.addView(tvSubtitulo);

        TextView tvValorActual = new TextView(activity);
        String valStr = (ultimoVectorIcd != null) ?
                formatearDireccionIcdHex(ultimoVectorIcd) : "No leído aún del chip";
        tvValorActual.setText("\nVector Actual: " + valStr);
        tvValorActual.setTextColor(Color.WHITE);
        tvValorActual.setTextSize(15);
        tvValorActual.setTypeface(null, Typeface.BOLD);
        layout.addView(tvValorActual);

        TextView tvPrompt = new TextView(activity);
        tvPrompt.setText("\nDirección de depuración en hex (ej. 000020):");
        tvPrompt.setTextColor(Color.parseColor("#00B0FF"));
        tvPrompt.setTextSize(13);
        layout.addView(tvPrompt);

        EditText etDireccionHex = new EditText(activity);
        if (ultimoVectorIcd != null) {
            etDireccionHex.setText(String.format("%06X", ultimoVectorIcd));
        } else {
            etDireccionHex.setHint("000020");
        }
        etDireccionHex.setTextColor(Color.WHITE);
        etDireccionHex.setHintTextColor(Color.parseColor("#5A5A70"));
        etDireccionHex.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        layout.addView(etDireccionHex);

        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setPadding(0, dpToPx(activity, 15), 0, 0);

        Button btnLeer = new Button(activity);
        btnLeer.setText("📥 Leer Vector");
        btnLeer.setTextColor(Color.WHITE);
        btnLeer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00B0FF")));
        LinearLayout.LayoutParams lpLeer = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpLeer.setMargins(0, 0, dpToPx(activity, 8), 0);
        btnLeer.setLayoutParams(lpLeer);
        btnLayout.addView(btnLeer);

        Button btnGrabar = new Button(activity);
        btnGrabar.setText("💾 Grabar Vector");
        btnGrabar.setTextColor(Color.WHITE);
        btnGrabar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        LinearLayout.LayoutParams lpGrabar = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnGrabar.setLayoutParams(lpGrabar);
        btnLayout.addView(btnGrabar);

        layout.addView(btnLayout);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🛠️ Vector de Depuración (ICD)")
                .setView(scrollView)
                .setNeutralButton("Cerrar", null)
                .create();

        btnLeer.setOnClickListener(v -> {
            if (protocolo == null) {
                Toast.makeText(activity, "Programador no conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                String resp = protocolo.leerVectorDeDepuracionDelPic();
                activity.runOnUiThread(() -> {
                    if (resp != null && resp.contains("0x")) {
                        int idx = resp.indexOf("0x");
                        String hex = resp.substring(idx + 2).trim();
                        int addr = parsearDireccionIcdHex(hex);
                        if (addr >= 0) {
                            ultimoVectorIcd = addr;
                            tvValorActual.setText("\nVector Actual: " + formatearDireccionIcdHex(addr));
                            etDireccionHex.setText(String.format("%06X", addr));
                            Toast.makeText(activity, "✓ Vector leído: " + formatearDireccionIcdHex(addr), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    Toast.makeText(activity, "Respuesta: " + resp, Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        btnGrabar.setOnClickListener(v -> {
            if (protocolo == null) {
                Toast.makeText(activity, "Programador no conectado", Toast.LENGTH_SHORT).show();
                return;
            }
            String input = etDireccionHex.getText().toString().trim();
            int addr = parsearDireccionIcdHex(input);
            if (addr < 0) {
                Toast.makeText(activity, "Dirección hexadecimal de 24 bits inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                boolean ok = protocolo.programarVectorDeDepuracionDelPic(addr);
                activity.runOnUiThread(() -> {
                    if (ok) {
                        ultimoVectorIcd = addr;
                        tvValorActual.setText("\nVector Actual: " + formatearDireccionIcdHex(addr));
                        Toast.makeText(activity, "✓ Vector ICD programado: " + formatearDireccionIcdHex(addr), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "❌ Fallo al programar vector ICD", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });

        dialog.show();
    }

    private static int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
