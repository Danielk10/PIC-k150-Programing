package com.diamon.managers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.diamon.protocolo.ProtocoloP18A;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestor de la Suite de Diagnóstico de Hardware para el programador PIC K150.
 *
 * <p>Proporciona herramientas técnicas avanzadas para validar:
 * <ul>
 *   <li>Identificación del modelo de hardware y revisión del protocolo de firmware.</li>
 *   <li>Prueba de eco serial bidireccional y medición de latencia en milisegundos.</li>
 *   <li>Lectura de los sensores de presencia e inserción del zócalo ZIF de 40 pines.</li>
 *   <li>Control manual de voltajes VPP (13V) y VDD (5V) para diagnóstico con multímetro.</li>
 * </ul>
 */
public class DiagnosticoHardwareManager {

    /** Callback para notificar eventos al log principal de la aplicación. */
    public interface LogCallback {
        void onLog(String message);
    }

    /** Resultado estructurado de una ejecución de diagnóstico completa. */
    public static class ResultadoDiagnostico {
        public final String modeloProgramador;
        public final String protocolo;
        public final boolean ecoExitoso;
        public final long latenciaEcoMs;
        public final boolean picEnSocket;
        public final boolean picFueraSocket;
        public final long timestamp;

        public ResultadoDiagnostico(
                String modeloProgramador,
                String protocolo,
                boolean ecoExitoso,
                long latenciaEcoMs,
                boolean picEnSocket,
                boolean picFueraSocket) {
            this.modeloProgramador = modeloProgramador;
            this.protocolo = protocolo;
            this.ecoExitoso = ecoExitoso;
            this.latenciaEcoMs = latenciaEcoMs;
            this.picEnSocket = picEnSocket;
            this.picFueraSocket = picFueraSocket;
            this.timestamp = System.currentTimeMillis();
        }

        public String getEstadoSocketDescripcion() {
            if (picEnSocket) {
                return "✓ PIC Detectado en el zócalo ZIF";
            } else if (picFueraSocket) {
                return "ℹ Zócalo vacío (Sin chip detectado)";
            } else {
                return "⚠ No se detectó contacto firme en el zócalo";
            }
        }
    }

    public DiagnosticoHardwareManager() {
    }

    /**
     * Ejecuta una serie completa de pruebas de diagnóstico sobre el programador K150.
     *
     * @param protocolo Instancia del protocolo P18A conectada
     * @return ResultadoDiagnostico con todos los parámetros medidos
     */
    public ResultadoDiagnostico ejecutarDiagnostico(ProtocoloP18A protocolo) {
        if (protocolo == null) {
            return new ResultadoDiagnostico("Desconectado", "N/A", false, -1, false, false);
        }

        // 1. Modelo de hardware (Cmd 20)
        String modelo;
        try {
            modelo = protocolo.obtenerVersionOModeloDelProgramador();
            if (modelo == null || modelo.isEmpty()) {
                modelo = "Desconocido";
            }
        } catch (Exception e) {
            modelo = "Error: " + e.getMessage();
        }

        // 2. Protocolo de firmware (Cmd 21)
        String protoNombre;
        try {
            protoNombre = protocolo.obtenerProtocoloDelProgramador();
            if (protoNombre == null || protoNombre.isEmpty()) {
                protoNombre = "Desconocido";
            }
        } catch (Exception e) {
            protoNombre = "Error: " + e.getMessage();
        }

        // 3. Medición de latencia y eco serial (Cmd 2)
        long latenciaMs = -1;
        boolean ecoOk = false;
        try {
            long t0 = System.currentTimeMillis();
            String ecoRes = protocolo.hacerUnEco();
            latenciaMs = System.currentTimeMillis() - t0;
            ecoOk = (ecoRes != null && !ecoRes.isEmpty());
        } catch (Exception ignored) {
        }

        // 4. Sensor de presencia en el zócalo ZIF (Cmd 18)
        boolean picEnSocket = false;
        try {
            picEnSocket = protocolo.detectarPicEnElSocket();
        } catch (Exception ignored) {
        }
        boolean picFueraSocket = !picEnSocket;

        return new ResultadoDiagnostico(
                modelo,
                protoNombre,
                ecoOk,
                latenciaMs,
                picEnSocket,
                picFueraSocket
        );
    }

    /**
     * Prueba individual de eco serial con cálculo de tiempo de ida y vuelta (Round-trip time).
     *
     * @param protocolo Instancia del protocolo P18A
     * @return Latencia en milisegundos si es exitoso, o -1 si falla
     */
    public long probarEco(ProtocoloP18A protocolo) {
        if (protocolo == null) {
            return -1;
        }
        try {
            long t0 = System.currentTimeMillis();
            String res = protocolo.hacerUnEco();
            long delta = System.currentTimeMillis() - t0;
            if (res != null && !res.isEmpty()) {
                return delta;
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /**
     * Activa manualmente los voltajes de programación VPP (13V) y VDD (5V).
     * <p>Comando K150: '4' (0x04) -> espera 'V'.
     */
    public boolean activarVoltajes(ProtocoloP18A protocolo) {
        if (protocolo == null) {
            return false;
        }
        try {
            return protocolo.activarVoltajesDeProgramacion();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Desactiva los voltajes de programación regresando las líneas a 0V.
     * <p>Comando K150: '5' (0x05) -> espera 'v'.
     */
    public boolean desactivarVoltajes(ProtocoloP18A protocolo) {
        if (protocolo == null) {
            return false;
        }
        try {
            return protocolo.desactivarVoltajesDeProgramacion();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reinicia el ciclo de voltajes de programación en el hardware.
     * <p>Comando K150: '6' (0x06) -> espera 'V'.
     */
    public boolean reiniciarVoltajes(ProtocoloP18A protocolo) {
        if (protocolo == null) {
            return false;
        }
        try {
            return protocolo.reiniciarVoltajesDeProgramacion();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Muestra el modal interactivo de diagnóstico de hardware y control de voltajes.
     *
     * @param activity Actividad host
     * @param protocolo Protocolo conectado al programador
     * @param log Callback para reportar eventos a la consola
     */
    public void showDiagnosticoDialog(
            Activity activity,
            ProtocoloP18A protocolo,
            LogCallback log) {

        if (activity == null || activity.isFinishing()) {
            return;
        }

        final float density = activity.getResources().getDisplayMetrics().density;
        final int dp8 = Math.round(8 * density);
        final int dp12 = Math.round(12 * density);
        final int dp16 = Math.round(16 * density);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setBackgroundColor(Color.parseColor("#1A1A2E"));

        LinearLayout rootLayout = new LinearLayout(activity);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dp16, dp16, dp16, dp16);
        scrollView.addView(rootLayout);

        // --- TARJETA 1: ESTADO DEL HARDWARE ---
        LinearLayout cardEstado = crearCardContainer(activity, dp12, dp16);
        rootLayout.addView(cardEstado);

        TextView tvTituloEstado = crearSectionHeader(activity, "📋 Estado del Programador");
        cardEstado.addView(tvTituloEstado);

        final TextView tvModelo = crearFilaInfo(activity, "Modelo:", "Consultando...");
        cardEstado.addView(tvModelo);

        final TextView tvProtocolo = crearFilaInfo(activity, "Protocolo:", "Consultando...");
        cardEstado.addView(tvProtocolo);

        final TextView tvEco = crearFilaInfo(activity, "Prueba de Eco:", "Pendiente");
        cardEstado.addView(tvEco);

        final TextView tvSocket = crearFilaInfo(activity, "Sensor ZIF:", "Consultando...");
        cardEstado.addView(tvSocket);

        final ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleSmall);
        progressBar.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pbParams.gravity = Gravity.CENTER_HORIZONTAL;
        pbParams.topMargin = dp8;
        cardEstado.addView(progressBar, pbParams);

        // Botones de acción de la tarjeta de estado
        LinearLayout layoutBotonesEstado = new LinearLayout(activity);
        layoutBotonesEstado.setOrientation(LinearLayout.HORIZONTAL);
        layoutBotonesEstado.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lbeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lbeParams.topMargin = dp12;
        cardEstado.addView(layoutBotonesEstado, lbeParams);

        Button btnProbarEco = crearBoton(activity, "⚡ Probar Eco", Color.parseColor("#00B0FF"));
        Button btnActualizarTodo = crearBoton(activity, "🔄 Diagnóstico Completo", Color.parseColor("#3F51B5"));

        LinearLayout.LayoutParams btnHalfParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnHalfParams.setMargins(dp8 / 2, 0, dp8 / 2, 0);
        layoutBotonesEstado.addView(btnProbarEco, btnHalfParams);
        layoutBotonesEstado.addView(btnActualizarTodo, btnHalfParams);

        Button btnProbarSensores = crearBoton(activity, "🧪 Probar Sensores ZIF (Inserción/Extracción)", Color.parseColor("#7B1FA2"));
        LinearLayout.LayoutParams btnFullWidthParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnFullWidthParams.topMargin = dp8;
        cardEstado.addView(btnProbarSensores, btnFullWidthParams);

        // Separador
        View spacer = new View(activity);
        LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp16);
        rootLayout.addView(spacer, spParams);

        // --- TARJETA 2: CONTROL MANUAL DE VOLTAJES ---
        LinearLayout cardVoltajes = crearCardContainer(activity, dp12, dp16);
        rootLayout.addView(cardVoltajes);

        TextView tvTituloVoltajes = crearSectionHeader(activity, "⚡ Control de Voltajes VPP / VDD");
        cardVoltajes.addView(tvTituloVoltajes);

        // Cuadro de Advertencia Crítica de Seguridad
        LinearLayout warningBox = new LinearLayout(activity);
        warningBox.setOrientation(LinearLayout.VERTICAL);
        warningBox.setPadding(dp12, dp12, dp12, dp12);
        GradientDrawable warningBg = new GradientDrawable();
        warningBg.setColor(Color.parseColor("#3E2723")); // Marrón muy oscuro
        warningBg.setCornerRadius(6 * density);
        warningBg.setStroke(Math.round(1.5f * density), Color.parseColor("#FF5722")); // Borde naranja alerta
        warningBox.setBackground(warningBg);

        TextView tvWarning = new TextView(activity);
        tvWarning.setText("⚠️ ATENCIÓN: Retire el microcontrolador del zócalo ZIF antes de activar o medir voltajes con multímetro.");
        tvWarning.setTextColor(Color.parseColor("#FFCC80"));
        tvWarning.setTextSize(13f);
        tvWarning.setTypeface(null, Typeface.BOLD);
        warningBox.addView(tvWarning);

        LinearLayout.LayoutParams wbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wbParams.topMargin = dp8;
        wbParams.bottomMargin = dp12;
        cardVoltajes.addView(warningBox, wbParams);

        final TextView tvEstadoVoltaje = new TextView(activity);
        tvEstadoVoltaje.setText("Estado: Voltajes apagados (0V)");
        tvEstadoVoltaje.setTextColor(Color.parseColor("#B0BEC5"));
        tvEstadoVoltaje.setTextSize(13f);
        tvEstadoVoltaje.setGravity(Gravity.CENTER);
        cardVoltajes.addView(tvEstadoVoltaje);

        // Botones de Voltaje
        LinearLayout layoutBotonesVoltajes = new LinearLayout(activity);
        layoutBotonesVoltajes.setOrientation(LinearLayout.HORIZONTAL);
        layoutBotonesVoltajes.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lbvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lbvParams.topMargin = dp12;
        cardVoltajes.addView(layoutBotonesVoltajes, lbvParams);

        final Button btnActivarVpp = crearBoton(activity, "VPP (13V)", Color.parseColor("#FF6600"));
        final Button btnDesactivarV = crearBoton(activity, "Apagar (0V)", Color.parseColor("#4CAF50"));
        final Button btnReiniciarV = crearBoton(activity, "Reiniciar", Color.parseColor("#78909C"));

        LinearLayout.LayoutParams btnThirdParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnThirdParams.setMargins(dp8 / 2, 0, dp8 / 2, 0);

        layoutBotonesVoltajes.addView(btnActivarVpp, btnThirdParams);
        layoutBotonesVoltajes.addView(btnDesactivarV, btnThirdParams);
        layoutBotonesVoltajes.addView(btnReiniciarV, btnThirdParams);

        final AtomicBoolean voltajesActivos = new AtomicBoolean(false);

        Runnable apagarVoltajesSiActivos = () -> {
            if (voltajesActivos.compareAndSet(true, false)) {
                new Thread(() -> {
                    try {
                        desactivarVoltajes(protocolo);
                    } catch (Exception ignored) {
                    }
                }, "DiagVoltajesCleanup").start();
            }
        };

        // Diálogo AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("🩺 Diagnóstico de Hardware K150")
                .setView(scrollView)
                .setPositiveButton("Cerrar", (d, which) -> {
                    apagarVoltajesSiActivos.run();
                })
                .create();

        dialog.setOnDismissListener(d -> {
            apagarVoltajesSiActivos.run();
        });

        // --- LÓGICA DE ACTUALIZACIÓN ASÍNCRONA ---
        Runnable refrescarDiagnosticoCompleto = () -> {
            progressBar.setVisibility(View.VISIBLE);
            tvModelo.setText("Modelo: Consultando...");
            tvProtocolo.setText("Protocolo: Consultando...");
            tvEco.setText("Prueba de Eco: Midiendo...");
            tvSocket.setText("Sensor ZIF: Leyendo...");

            new Thread(() -> {
                ResultadoDiagnostico res = ejecutarDiagnostico(protocolo);
                activity.runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvModelo.setText("Modelo: " + res.modeloProgramador);
                    tvProtocolo.setText("Protocolo: " + res.protocolo);
                    if (res.ecoExitoso) {
                        tvEco.setText("Prueba de Eco: ✓ OK (" + res.latenciaEcoMs + " ms)");
                        tvEco.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        tvEco.setText("Prueba de Eco: ❌ Sin respuesta");
                        tvEco.setTextColor(Color.parseColor("#FF5252"));
                    }
                    tvSocket.setText("Sensor ZIF: " + res.getEstadoSocketDescripcion());
                    if (res.picEnSocket) {
                        tvSocket.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        tvSocket.setTextColor(Color.parseColor("#ECEFF1"));
                    }

                    if (log != null) {
                        log.onLog("🩺 Diagnóstico K150: " + res.modeloProgramador +
                                " | Proto: " + res.protocolo +
                                " | Eco: " + (res.ecoExitoso ? res.latenciaEcoMs + "ms" : "Fallo") +
                                " | Socket: " + (res.picEnSocket ? "Chip Detectado" : "Vacío"));
                    }
                });
            }).start();
        };

        btnActualizarTodo.setOnClickListener(v -> refrescarDiagnosticoCompleto.run());

        btnProbarEco.setOnClickListener(v -> {
            tvEco.setText("Prueba de Eco: Midiendo...");
            new Thread(() -> {
                long latencia = probarEco(protocolo);
                activity.runOnUiThread(() -> {
                    if (latencia >= 0) {
                        tvEco.setText("Prueba de Eco: ✓ OK (" + latencia + " ms)");
                        tvEco.setTextColor(Color.parseColor("#4CAF50"));
                        if (log != null) {
                            log.onLog("⚡ Eco serial K150 exitoso: " + latencia + " ms");
                        }
                    } else {
                        tvEco.setText("Prueba de Eco: ❌ Falló");
                        tvEco.setTextColor(Color.parseColor("#FF5252"));
                        if (log != null) {
                            log.onLog("❌ Prueba de eco K150 no respondió");
                        }
                    }
                });
            }).start();
        });

        btnProbarSensores.setOnClickListener(v -> {
            mostrarDialogoPruebaSensoresZIF(activity, protocolo, log);
        });

        btnActivarVpp.setOnClickListener(v -> {
            btnActivarVpp.setEnabled(false);
            new Thread(() -> {
                boolean chipEnSocket = protocolo.detectarPicEnElSocket();
                if (chipEnSocket) {
                    activity.runOnUiThread(() -> {
                        btnActivarVpp.setEnabled(true);
                        mostrarAlertaSeguridadVpp(activity, protocolo, () -> {
                            btnActivarVpp.setEnabled(false);
                            new Thread(() -> {
                                boolean ok = activarVoltajes(protocolo);
                                activity.runOnUiThread(() -> {
                                    btnActivarVpp.setEnabled(true);
                                    if (ok) {
                                        voltajesActivos.set(true);
                                        tvEstadoVoltaje.setText("Estado: ⚡ VPP (13V) / VDD (5V) ACTIVADOS");
                                        tvEstadoVoltaje.setTextColor(Color.parseColor("#FF6600"));
                                        if (log != null) {
                                            log.onLog("⚡ Voltajes VPP/VDD activados (13V/5V) para medición");
                                        }
                                        Toast.makeText(activity, "Chip retirado: Voltajes activados para medición segura", Toast.LENGTH_SHORT).show();
                                    } else {
                                        tvEstadoVoltaje.setText("Estado: ❌ Error activando voltajes");
                                        tvEstadoVoltaje.setTextColor(Color.parseColor("#FF5252"));
                                    }
                                });
                            }).start();
                        });
                    });
                    return;
                }

                boolean ok = activarVoltajes(protocolo);
                activity.runOnUiThread(() -> {
                    btnActivarVpp.setEnabled(true);
                    if (ok) {
                        voltajesActivos.set(true);
                        tvEstadoVoltaje.setText("Estado: ⚡ VPP (13V) / VDD (5V) ACTIVADOS");
                        tvEstadoVoltaje.setTextColor(Color.parseColor("#FF6600"));
                        if (log != null) {
                            log.onLog("⚡ Voltajes VPP/VDD activados (13V/5V) para medición");
                        }
                        Toast.makeText(activity, "Voltajes activados: mida con multímetro en VPP y VDD", Toast.LENGTH_SHORT).show();
                    } else {
                        tvEstadoVoltaje.setText("Estado: ❌ Error activando voltajes");
                        tvEstadoVoltaje.setTextColor(Color.parseColor("#FF5252"));
                        if (log != null) {
                            log.onLog("❌ Error al activar voltajes de programación");
                        }
                    }
                });
            }).start();
        });

        btnDesactivarV.setOnClickListener(v -> {
            btnDesactivarV.setEnabled(false);
            new Thread(() -> {
                boolean ok = desactivarVoltajes(protocolo);
                activity.runOnUiThread(() -> {
                    btnDesactivarV.setEnabled(true);
                    if (ok) {
                        voltajesActivos.set(false);
                        tvEstadoVoltaje.setText("Estado: Voltajes apagados (0V)");
                        tvEstadoVoltaje.setTextColor(Color.parseColor("#4CAF50"));
                        if (log != null) {
                            log.onLog("✓ Voltajes desactivados (0V)");
                        }
                        Toast.makeText(activity, "Voltajes apagados (0V)", Toast.LENGTH_SHORT).show();
                    } else {
                        tvEstadoVoltaje.setText("Estado: ❌ Error apagando voltajes");
                        tvEstadoVoltaje.setTextColor(Color.parseColor("#FF5252"));
                    }
                });
            }).start();
        });

        btnReiniciarV.setOnClickListener(v -> {
            btnReiniciarV.setEnabled(false);
            new Thread(() -> {
                boolean chipEnSocket = protocolo.detectarPicEnElSocket();
                if (chipEnSocket) {
                    activity.runOnUiThread(() -> {
                        btnReiniciarV.setEnabled(true);
                        mostrarAlertaSeguridadVpp(activity, protocolo, () -> {
                            btnReiniciarV.setEnabled(false);
                            new Thread(() -> {
                                boolean ok = reiniciarVoltajes(protocolo);
                                activity.runOnUiThread(() -> {
                                    btnReiniciarV.setEnabled(true);
                                    if (ok) {
                                        voltajesActivos.set(true);
                                        tvEstadoVoltaje.setText("Estado: ⚡ Voltajes reiniciados (Activos)");
                                        tvEstadoVoltaje.setTextColor(Color.parseColor("#00B0FF"));
                                        if (log != null) {
                                            log.onLog("⚡ Ciclo de voltajes de programación reiniciado");
                                        }
                                        Toast.makeText(activity, "Chip retirado: Voltajes reiniciados para medición", Toast.LENGTH_SHORT).show();
                                    } else {
                                        tvEstadoVoltaje.setText("Estado: ❌ Error al reiniciar voltajes");
                                        tvEstadoVoltaje.setTextColor(Color.parseColor("#FF5252"));
                                    }
                                });
                            }).start();
                        });
                    });
                    return;
                }

                boolean ok = reiniciarVoltajes(protocolo);
                activity.runOnUiThread(() -> {
                    btnReiniciarV.setEnabled(true);
                    if (ok) {
                        voltajesActivos.set(true);
                        tvEstadoVoltaje.setText("Estado: ⚡ Voltajes reiniciados (Activos)");
                        tvEstadoVoltaje.setTextColor(Color.parseColor("#00B0FF"));
                        if (log != null) {
                            log.onLog("⚡ Ciclo de voltajes de programación reiniciado");
                        }
                    } else {
                        tvEstadoVoltaje.setText("Estado: ❌ Error al reiniciar voltajes");
                        tvEstadoVoltaje.setTextColor(Color.parseColor("#FF5252"));
                    }
                });
            }).start();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            GradientDrawable winBg = new GradientDrawable();
            winBg.setColor(Color.parseColor("#1A1A2E"));
            winBg.setCornerRadius(12 * density);
            dialog.getWindow().setBackgroundDrawable(winBg);
        }

        Button btnPos = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btnPos != null) {
            btnPos.setTextColor(Color.parseColor("#4CAF50"));
            btnPos.setTypeface(null, Typeface.BOLD);
        }

        // Iniciar diagnóstico automático inmediatamente al abrir
        refrescarDiagnosticoCompleto.run();
    }

    private void mostrarAlertaSeguridadVpp(
            Activity activity,
            ProtocoloP18A protocolo,
            Runnable accionAlRetirar) {

        final AtomicBoolean cancelToken = new AtomicBoolean(false);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("⚠️ Microcontrolador en el Zócalo")
                .setMessage("Para evitar dañar el PIC con 13V de VPP, por favor retire el microcontrolador del zócalo ZIF antes de activar voltajes de medición.\n\nEsperando a que retire el chip...")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("Cancelar", (d, which) -> cancelToken.set(true))
                .create();

        dialog.setOnDismissListener(d -> cancelToken.set(true));
        dialog.show();

        new Thread(() -> {
            boolean extraido = protocolo.esperarExtraccionDelSocket(30000, cancelToken);
            activity.runOnUiThread(() -> {
                if (extraido && !cancelToken.get()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {}
                    if (accionAlRetirar != null) {
                        accionAlRetirar.run();
                    }
                } else if (!cancelToken.get()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {}
                    Toast.makeText(activity, "Tiempo agotado. Retire el chip e intente nuevamente.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void mostrarDialogoPruebaSensoresZIF(Activity activity, ProtocoloP18A protocolo, LogCallback log) {
        final AtomicBoolean cancelToken = new AtomicBoolean(false);

        final float density = activity.getResources().getDisplayMetrics().density;
        final int p16 = Math.round(16 * density);
        final int p12 = Math.round(12 * density);
        final int p8 = Math.round(8 * density);

        LinearLayout rootLayout = new LinearLayout(activity);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(p16, p16, p16, p16);
        rootLayout.setBackgroundColor(Color.parseColor("#1A1A2E"));

        // Tarjeta con fondo oscuro contrastado #2A2A3E, esquinas redondeadas y borde sutil
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(p16, p16, p16, p16);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#2A2A3E"));
        cardBg.setCornerRadius(10 * density);
        cardBg.setStroke(Math.round(1 * density), Color.parseColor("#4A4A6A"));
        card.setBackground(cardBg);
        rootLayout.addView(card);

        TextView tvTitulo = new TextView(activity);
        tvTitulo.setText("🧪 Prueba de Sensores ZIF");
        tvTitulo.setTextSize(16f);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        tvTitulo.setTextColor(Color.WHITE);
        tvTitulo.setPadding(0, 0, 0, p8);
        card.addView(tvTitulo);

        TextView tvInfo = new TextView(activity);
        tvInfo.setText("Prueba de sensores de hardware del zócalo ZIF (Kitsrus Comandos 18 y 19):\n");
        tvInfo.setTextSize(13f);
        tvInfo.setTextColor(Color.parseColor("#ECEFF1"));
        card.addView(tvInfo);

        TextView tvPaso1 = new TextView(activity);
        tvPaso1.setText("⏳ Paso 1: Inserte un microcontrolador en el zócalo y trabe la palanca (Comando 18)...");
        tvPaso1.setTextSize(13f);
        tvPaso1.setTextColor(Color.parseColor("#FFD54F"));
        tvPaso1.setPadding(0, p8, 0, p8);
        card.addView(tvPaso1);

        TextView tvPaso2 = new TextView(activity);
        tvPaso2.setText("⚪ Paso 2: Pendiente (Extracción con Comando 19)");
        tvPaso2.setTextSize(13f);
        tvPaso2.setTextColor(Color.parseColor("#B0BEC5"));
        tvPaso2.setPadding(0, p8, 0, p8);
        card.addView(tvPaso2);

        ProgressBar pb = new ProgressBar(activity, null, android.R.attr.progressBarStyleSmall);
        pb.setPadding(0, p8, 0, 0);
        card.addView(pb);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setNegativeButton("Cancelar", (d, w) -> cancelToken.set(true))
                .create();

        dialog.setOnDismissListener(d -> cancelToken.set(true));
        dialog.show();

        if (dialog.getWindow() != null) {
            GradientDrawable winBg = new GradientDrawable();
            winBg.setColor(Color.parseColor("#1A1A2E"));
            winBg.setCornerRadius(12 * density);
            dialog.getWindow().setBackgroundDrawable(winBg);
        }

        Button btnCancel = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (btnCancel != null) {
            btnCancel.setTextColor(Color.parseColor("#FF5252"));
            btnCancel.setTypeface(null, Typeface.BOLD);
        }

        new Thread(() -> {
            // Paso 1: Esperar inserción (Cmd 18)
            boolean insercionOk = protocolo.esperarInsercionEnElSocket(25000, cancelToken);
            if (cancelToken.get()) return;

            activity.runOnUiThread(() -> {
                if (insercionOk) {
                    tvPaso1.setText("✓ Paso 1: Sensor de Inserción OK (Comando 18)");
                    tvPaso1.setTextColor(Color.parseColor("#4CAF50"));
                    tvPaso2.setText("⏳ Paso 2: Ahora levante la palanca y retire el chip del zócalo (Comando 19)...");
                    tvPaso2.setTextColor(Color.parseColor("#FFD54F"));
                    if (log != null) log.onLog("✓ Sensor de inserción ZIF respondido (Comando 18)");
                } else {
                    tvPaso1.setText("❌ Paso 1: Tiempo agotado sin detectar chip");
                    tvPaso1.setTextColor(Color.parseColor("#FF5252"));
                    pb.setVisibility(View.GONE);
                }
            });

            if (!insercionOk || cancelToken.get()) return;

            // Paso 2: Esperar extracción (Cmd 19)
            boolean extraccionOk = protocolo.esperarExtraccionDelSocket(25000, cancelToken);
            if (cancelToken.get()) return;

            activity.runOnUiThread(() -> {
                pb.setVisibility(View.GONE);
                if (extraccionOk) {
                    tvPaso2.setText("✓ Paso 2: Sensor de Extracción OK (Comando 19)");
                    tvPaso2.setTextColor(Color.parseColor("#4CAF50"));
                    tvInfo.setText("🎉 ¡Ambos sensores físicos del zócalo ZIF funcionan perfectamente!\n");
                    if (log != null) log.onLog("✓ Sensor de extracción ZIF respondido (Comando 19)");
                } else {
                    tvPaso2.setText("❌ Paso 2: Tiempo agotado sin detectar extracción");
                    tvPaso2.setTextColor(Color.parseColor("#FF5252"));
                }
            });
        }).start();
    }

    private LinearLayout crearCardContainer(Context context, int padding, int margin) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(padding, padding, padding, padding);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#2A2A3E"));
        gd.setCornerRadius(8 * context.getResources().getDisplayMetrics().density);
        card.setBackground(gd);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = margin;
        card.setLayoutParams(params);
        return card;
    }

    private TextView crearSectionHeader(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(Color.parseColor("#FFFFFF"));
        tv.setTextSize(15f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, Math.round(8 * context.getResources().getDisplayMetrics().density));
        return tv;
    }

    private TextView crearFilaInfo(Context context, String label, String value) {
        TextView tv = new TextView(context);
        tv.setText(label + " " + value);
        tv.setTextColor(Color.parseColor("#ECEFF1"));
        tv.setTextSize(13f);
        tv.setPadding(0, Math.round(3 * context.getResources().getDisplayMetrics().density), 0, Math.round(3 * context.getResources().getDisplayMetrics().density));
        return tv;
    }

    private Button crearBoton(Context context, String text, int color) {
        Button btn = new Button(context);
        btn.setText(text);
        btn.setTextSize(12f);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundTintList(ColorStateList.valueOf(color));
        return btn;
    }
}
