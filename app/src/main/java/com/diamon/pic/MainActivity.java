package com.diamon.pic;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.graphics.Typeface;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.billing.BillingManager;
import com.diamon.managers.CalibracionManager;
import com.diamon.managers.ChipSelectionManager;
import com.diamon.managers.DiagnosticoHardwareManager;
import com.diamon.managers.FileManager;
import com.diamon.managers.FuseConfigPopup;
import com.diamon.managers.HexEditorManager;
import com.diamon.managers.HexExportManager;
import com.diamon.managers.MemoryDisplayManager;
import com.diamon.managers.PicProgrammingManager;
import com.diamon.managers.ProAccessDialog;
import com.diamon.managers.ProPassManager;
import com.diamon.managers.ProgramacionSelectivaManager;
import com.diamon.managers.ProgramacionSelectivaManager.TipoProgramacion;
import com.diamon.managers.NotificacionVibracionManager;
import com.diamon.managers.ProgrammingDialogManager;
import com.diamon.managers.UsbConnectionManager;
import com.diamon.managers.SocketDrawingManager;
import com.diamon.nucleo.Protocolo;
import com.diamon.protocolo.TipoProtocolo;
import com.diamon.politicas.PoliticaPrivacidadActivity;
import com.diamon.publicidad.GestorPublicidad;
import com.diamon.tutorial.TutorialGputilsActivity;
import com.diamon.utilidades.GestorPantalla;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * MainActivity COMPLETAMENTE ACTUALIZADA
 *
 * <p>
 * Nueva funcionalidad: - Configuración de fusibles mediante PopupWindow -
 * Indicador visual de
 * estado de fusibles - Restauración de fusibles desde PIC o HEX - ID
 * personalizado del usuario -
 * Integración completa con DatosPicProcesados y ChipinfoEntry
 */
public class MainActivity extends AppCompatActivity
        implements UsbConnectionManager.UsbConnectionListener,
        PicProgrammingManager.ProgrammingListener {

    private View connectionIndicator;
    private TextView processStatusTextView;
    private TextView chipInfoTextView;
    private TextView fuseStatusTextView; // NUEVO
    private LinearLayout romDataContainer;
    private LinearLayout eepromDataContainer;
    private Spinner chipSpinner;
    private android.widget.ImageView chipSocketImageView; // NUEVO
    private androidx.appcompat.widget.SwitchCompat swModeICSP;
    private ScrollView logScrollView;
    private FloatingActionButton btnLimpiarLog;

    private android.widget.Button btnSelectHex;
    private android.widget.Button btnProgramarPic;
    private android.widget.Button btnLeerMemoriaDeLPic;
    private android.widget.Button btnVerificarMemoriaDelPic;
    private android.widget.Button btnBorrarMemoriaDeLPic;
    private android.widget.Button btnDetectarPic;
    private android.widget.Button btnConfigureFuses; // NUEVO
    private android.widget.Button btnBlankCheck; // Verificar Borrado
    private android.widget.Button btnConnectUsb;
    private TextView tvConnectionStatus;

    private UsbConnectionManager usbManager;
    private PicProgrammingManager programmingManager;
    private FileManager fileManager;
    private ChipSelectionManager chipSelectionManager;
    private MemoryDisplayManager memoryDisplayManager;
    private ProgrammingDialogManager dialogManager;
    private GestorPublicidad publicidad;
    private PowerManager.WakeLock wakeLock;

    private FuseConfigPopup fuseConfigPopup; // NUEVO
    private HexExportManager hexExportManager; // NUEVO: Export manager
    private DiagnosticoHardwareManager diagnosticoHardwareManager; // NUEVO: Diagnóstico K150
    private CalibracionManager calibracionManager; // NUEVO: Calibración OSCCAL e ICD
    private ProgramacionSelectivaManager programacionSelectivaManager; // NUEVO: Programación selectiva y Dual Blank Check
    private HexEditorManager hexEditorManager; // NUEVO: Editor hexadecimal interactivo en vivo
    private BillingManager billingManager; // NUEVO: Facturación Google Play (Pro Permanente)
    private ProPassManager proPassManager; // NUEVO: Pase temporal Pro de 12 Horas

    private String firmware = "";
    private String originalLoadedFirmware = "";
    private DatosPicProcesados originalLoadedDatosPic = null;
    private String lastProgrammedFirmware = "";
    private DatosPicProcesados lastProgrammedDatosPic = null;
    private String lastReadRomData = ""; // Últimos datos ROM leídos
    private String lastReadEepromData = ""; // Últimos datos EEPROM leídos
    private String lastReadConfigData = ""; // Últimos datos Config leídos
    private ChipPic currentChip;
    private SocketDrawingManager socketDrawingManager; // NUEVO: Gestor de dibujo de zócalo/ICSP

    // NUEVAS VARIABLES PARA FUSES
    private boolean fusesConfigured = false;
    private List<Integer> configuredFuses = new ArrayList<>();
    private byte[] configuredID = new byte[] { 0 };
    private Map<String, String> lastFuseConfiguration = null;
    private DatosPicProcesados datosPicProcesados = null;
    private final List<String> logHistory = new ArrayList<>();

    private GestorPantalla pantallaCompleta;
    private NotificacionVibracionManager notificacionVibracionManager;

    @SuppressLint({ "InvalidWakeLockTag", "UnspecifiedRegisterReceiverFlag" })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Habilitar Edge-to-Edge ANTES de setContentView (requerido por Android 15)
        pantallaCompleta = new GestorPantalla(this);
        pantallaCompleta.habilitarEdgeToEdge();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Aplicar insets del sistema al layout raíz para evitar solapes
        View vistaPrincipal = findViewById(R.id.main);
        if (vistaPrincipal != null) {
            pantallaCompleta.aplicarWindowInsets(vistaPrincipal);
        }

        // Ocultar barras de navegación para modo inmersivo
        pantallaCompleta.ocultarBotonesVirtuales();

        // Inicializar el switch de modo ICSP
        swModeICSP = findViewById(R.id.swModeICSP);

        updateSwitchColors();
        // Configurar listener para el switch
        setupICSPSwitchListener();

        initializeAppCenter();
        initializeBasicComponents();
        findViews();
        setupBanner();
        initializeManagers();
        setupListeners();
        setupToolbar();
        // Inicializar USB en un hilo secundario para no bloquear el onCreate.
        new Thread(() -> {
            try {
                if (usbManager != null) {
                    usbManager.initialize();
                }
            } catch (Exception e) {
                Analytics.trackEvent("USB: Init Error",
                        crearMapaAnalitica("Message", e.getMessage() != null ? e.getMessage() : "unknown"));
            }
        }, "UsbInitializer").start();

        setupWakeLock();
    }

    private void initializeAppCenter() {
        AppCenter.start(
                getApplication(),
                "c9a1ef1a-bbfb-443a-863e-1c1d77e49c18",
                Analytics.class,
                Crashes.class);
    }

    private void initializeBasicComponents() {
        Analytics.trackEvent("Init: Basic Components");
        publicidad = new GestorPublicidad(this);
        appendLog("⚙ " + getString(R.string.esperando_operacion));
    }

    private static final SimpleDateFormat LOG_DATE_FORMAT =
            new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault());

    private void appendLog(String message) {
        runOnUiThread(() -> {
            String timestamp = "[" + LOG_DATE_FORMAT.format(new Date()) + "] ";
            String fullMessage = timestamp + message;

            logHistory.add(fullMessage);
            updateLogUI();
        });
    }

    private void clearLog() {
        runOnUiThread(() -> {
            logHistory.clear();
            if (processStatusTextView != null) {
                processStatusTextView.setText("");
            }
        });
    }

    private void updateLogUI() {
        if (processStatusTextView == null) return;
        StringBuilder sb = new StringBuilder();
        for (String log : logHistory) {
            sb.append(log).append("\n");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        processStatusTextView.setText(sb.toString());
        // Auto-scroll al final del log
        if (logScrollView != null) {
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private void findViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectionIndicator = findViewById(R.id.connectionIndicator);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        if (connectionIndicator != null) {
            connectionIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        }
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setText(getString(R.string.desconectado));
            tvConnectionStatus.setTextColor(Color.parseColor("#B0BEC5"));
        }
        processStatusTextView = findViewById(R.id.processStatusTextView);
        chipInfoTextView = findViewById(R.id.chipInfoTextView);
        chipSocketImageView = findViewById(R.id.chipSocketImageView);
        fuseStatusTextView = findViewById(R.id.fuseStatusTextView);
        chipSpinner = findViewById(R.id.chipSpinner);
        logScrollView = findViewById(R.id.logScrollView);
        btnLimpiarLog = findViewById(R.id.btnLimpiarLog);

        btnSelectHex = findViewById(R.id.btnSelectHex);
        btnProgramarPic = findViewById(R.id.btnProgramarPic);
        btnLeerMemoriaDeLPic = findViewById(R.id.btnLeerMemoriaDeLPic);
        btnVerificarMemoriaDelPic = findViewById(R.id.btnVerificarMemoriaDelPic);
        btnBorrarMemoriaDeLPic = findViewById(R.id.btnBorrarMemoriaDeLPic);
        btnDetectarPic = findViewById(R.id.btnDetectarPic);
        btnConfigureFuses = findViewById(R.id.btnConfigureFuses);
        btnBlankCheck = findViewById(R.id.btnBlankCheck);

        romDataContainer = findViewById(R.id.romDataContainer);
        eepromDataContainer = findViewById(R.id.eepromDataContainer);

        btnConnectUsb = findViewById(R.id.btnConnectUsb);
        if (btnConnectUsb != null) {
            btnConnectUsb.setOnClickListener(v -> {
                if (usbManager != null && usbManager.isConnected()) {
                    appendLog("🔌 " + getString(R.string.desconectado));
                    usbManager.disconnect();
                } else if (usbManager != null) {
                    appendLog(getString(R.string.log_connecting_to_programmer));
                    usbManager.connect();
                }
            });
            btnConnectUsb.setText(getString(R.string.btn_connect_usb));
            btnConnectUsb.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            btnConnectUsb.setTextColor(Color.WHITE);
        }

        actualizarEstadoBotones(false, false);
    }

    private void setupBanner() {
        if (proPassManager != null && proPassManager.isProActive(billingManager)) {
            return;
        }
        FrameLayout bannerContainer = findViewById(R.id.bannerContainer);
        if (bannerContainer != null && publicidad != null) {
            publicidad.cargarBanner(bannerContainer);
        }
    }

    private void setupToolbar() {
        // Ya configurado en findViews()
    }

    private void initializeManagers() {
        Analytics.trackEvent("Init: Managers");
        usbManager = new UsbConnectionManager(this);
        programmingManager = new PicProgrammingManager(this);
        calibracionManager = new CalibracionManager(this);
        socketDrawingManager = new SocketDrawingManager(this, chipSocketImageView);

        // Configurar listeners
        usbManager.setConnectionListener(this);
        programmingManager.setProgrammingListener(this);

        fileManager = new FileManager(this);
        fileManager.initialize();

        chipSelectionManager = new ChipSelectionManager(this);
        chipSelectionManager.setSelectionListener(new ChipSelectionManager.ChipSelectionListener() {
            @Override
            public void onChipSelected(ChipPic chip, String model) {
                currentChip = chip;
                if (dialogManager != null) {
                    dialogManager.setChip(chip);
                }
                fileManager.setCurrentChip(chip);
                if (fileManager != null) {
                    fileManager.clearLoadedFile();
                }
                firmware = "";
                originalLoadedFirmware = "";
                datosPicProcesados = null;
                originalLoadedDatosPic = null;
                lastProgrammedFirmware = "";
                lastProgrammedDatosPic = null;
                lastReadRomData = "";
                lastReadEepromData = "";
                lastReadConfigData = "";
                chipInfoTextView.setText(chipSelectionManager.getSelectedChipInfoColored());
                appendLog("⚙ " + getString(R.string.pic_seleccionado) + ": " + model);

                updateSwitchColors();
                updateICSPSwitchState();
                updateChipImage(chip);
                clearFuseConfiguration();

                if (usbManager.isConnected()) {
                    try {
                        usbManager.getProtocolo().iniciarVariablesDeProgramacion(chip);
                    } catch (Exception e) {
                        appendLog("❌ " + getString(R.string.error_inicializando_chip));
                    }
                }

                actualizarEstadoBotones(usbManager != null && usbManager.isConnected(), false);
            }

            @Override
            public void onChipSelectionError(String errorMessage) {
                appendLog("❌ " + errorMessage);
                swModeICSP.setEnabled(false);
                swModeICSP.setChecked(false);
            }

            @Override
            public void onDatabaseLoaded() {
                if (chipSpinner != null) {
                    chipSelectionManager.setupSpinner(chipSpinner);
                }
                Analytics.trackEvent("Chips Loaded: Success");
            }
        });

        // Iniciar carga asíncrona de chips
        chipSelectionManager.initializeAsync();

        memoryDisplayManager = new MemoryDisplayManager(this);
        memoryDisplayManager.setOnMemoryRowClickListener(this::abrirEditorHexadecimal);
        memoryDisplayManager.setOnReprogramClickListener(this::executeReprogramFromMemoryDisplay);
        programacionSelectivaManager = new ProgramacionSelectivaManager(this);
        hexEditorManager = new HexEditorManager(this);
        proPassManager = new ProPassManager(this);
        proPassManager.setOnPassRestoredListener(expireTime -> {
            actualizarEstadoProUI();
            invalidateOptionsMenu();
        });
        billingManager = new BillingManager(this, new BillingManager.BillingListener() {
            @Override
            public void onAdsRemovedChanged(boolean adsRemoved) {
                MainActivity.this.actualizarEstadoProUI();
            }

            @Override
            public void onPriceLoaded(String formattedPrice) {
            }

            @Override
            public void onBillingError(String errorMessage) {
                appendLog(getString(R.string.log_pro_purchase_error, errorMessage));
            }
        });
        billingManager.startConnection();

        if (publicidad != null) {
            publicidad.setAdsDisabledChecker(() -> proPassManager != null && proPassManager.isProActive(billingManager));
        }

        dialogManager = new ProgrammingDialogManager(this);
        notificacionVibracionManager = new NotificacionVibracionManager(this);
        diagnosticoHardwareManager = new DiagnosticoHardwareManager();

        // NUEVO: Inicializar export manager
        hexExportManager = new HexExportManager(this);
        hexExportManager.initialize();
        hexExportManager.setExportListener(new HexExportManager.ExportListener() {
            @Override
            public void onExportSuccess(String fileName) {
                appendLog("✓ " + getString(R.string.exportacion_exitosa) + ": " + fileName);
            }

            @Override
            public void onExportError(String errorMessage) {
                appendLog("❌ " + getString(R.string.error_exportando) + ": " + errorMessage);
            }
        });

        // NUEVO: Diferir la precarga para evitar ANR durante la inicialización
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (publicidad != null) {
                publicidad.precargarNativeAd(com.diamon.publicidad.GestorPublicidad.KEY_NATIVE_MEMORY);
                publicidad.precargarNativeAd(com.diamon.publicidad.GestorPublicidad.KEY_NATIVE_PROGRAMMING);
                publicidad.precargarRewardedAd();
            }
        }, 3000); // Esperar 3 segundos para asegurar que MobileAds esté listo

        // NUEVO: Inicializar popup de fusibles
        fuseConfigPopup = new FuseConfigPopup(
                this,
                new FuseConfigPopup.FuseConfigListener() {
                    @Override
                    public void onFusesApplied(
                            List<Integer> fuses,
                            byte[] idData,
                            Map<String, String> configuration) {
                        // Guardar configuración en las variables de la actividad
                        configuredFuses = new ArrayList<>(fuses);
                        configuredID = idData;
                        lastFuseConfiguration = configuration;
                        fusesConfigured = true;

                        updateFuseStatus(true);
                        appendLog("✓ " + getString(R.string.fusibles_aplicados_correctamente));
                    }

                    @Override
                    public void onFusesCancelled() {
                        // No hacer nada
                    }

                    @Override
                    public void onLogMessage(String message) {
                        appendLog(message);
                    }
                });
    }

    /** Crea un mapa simple para eventos de analítica compatible con API 23+. */
    private Map<String, String> crearMapaAnalitica(String clave, String valor) {
        java.util.HashMap<String, String> propiedades = new java.util.HashMap<>();
        propiedades.put(clave, valor);
        return propiedades;
    }

    private void setupListeners() {
        setupFileManagerListeners();
        setupButtonListeners();
        setupPersistentLayoutListener(); // Registramos el vigilante de tamaño desde el inicio
    }

    // IMPLEMENTACIÓN DE UsbConnectionListener
    @Override
    public void onConnected() {
        Analytics.trackEvent("USB: Connected");
        runOnUiThread(() -> {
            if (connectionIndicator != null) {
                connectionIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            }
            if (tvConnectionStatus != null) {
                tvConnectionStatus.setText(getString(R.string.status_connected));
                tvConnectionStatus.setTextColor(Color.parseColor("#4CAF50"));
            }
            if (btnConnectUsb != null) {
                btnConnectUsb.setText(getString(R.string.btn_disconnect_usb));
                btnConnectUsb.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C62828")));
                btnConnectUsb.setTextColor(Color.WHITE);
            }
            boolean tieneArchivo = originalLoadedFirmware != null && !originalLoadedFirmware.isEmpty() && originalLoadedDatosPic != null;
            actualizarEstadoBotones(true, tieneArchivo);
            programmingManager.setProtocolo(usbManager.getProtocolo());
            calibracionManager.setProtocolo(usbManager.getProtocolo());
            appendLog("🔌 " + getString(R.string.conectado_al_programador));

            if (currentChip != null && usbManager.getProtocolo() != null) {
                new Thread(() -> {
                    try {
                        usbManager.getProtocolo().iniciarVariablesDeProgramacion(currentChip);
                    } catch (Exception ignored) {
                    }
                }, "InitChipOnConnect").start();
            }
        });
    }

    @Override
    public void onDisconnected() {
        Analytics.trackEvent("USB: Disconnected");
        runOnUiThread(() -> {
            if (connectionIndicator != null) {
                connectionIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            }
            if (tvConnectionStatus != null) {
                tvConnectionStatus.setText(getString(R.string.desconectado));
                tvConnectionStatus.setTextColor(Color.parseColor("#B0BEC5"));
            }
            if (btnConnectUsb != null) {
                btnConnectUsb.setText(getString(R.string.btn_connect_usb));
                btnConnectUsb.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                btnConnectUsb.setTextColor(Color.WHITE);
            }
            boolean tieneArchivo = originalLoadedFirmware != null && !originalLoadedFirmware.isEmpty() && originalLoadedDatosPic != null;
            actualizarEstadoBotones(false, tieneArchivo);
            appendLog("❌ " + getString(R.string.desconectado));
        });
    }

    @Override
    public void onConnectionError(String errorMessage) {
        Analytics.trackEvent("USB: Error", crearMapaAnalitica("Message", errorMessage));
        runOnUiThread(() -> {
            if (connectionIndicator != null) {
                connectionIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            }
            if (tvConnectionStatus != null) {
                tvConnectionStatus.setText(getString(R.string.status_error));
                tvConnectionStatus.setTextColor(Color.parseColor("#FF5252"));
            }
            if (btnConnectUsb != null) {
                btnConnectUsb.setText(getString(R.string.btn_connect_usb));
                btnConnectUsb.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                btnConnectUsb.setTextColor(Color.WHITE);
            }
            boolean tieneArchivo = originalLoadedFirmware != null && !originalLoadedFirmware.isEmpty() && originalLoadedDatosPic != null;
            actualizarEstadoBotones(false, tieneArchivo);
            appendLog("❌ " + errorMessage);
        });
    }

    // IMPLEMENTACIÓN DE ProgrammingListener
    @Override
    public void onProgrammingStarted() {
        Analytics.trackEvent("Prog: Started");
        appendLog("⏳ " + getString(R.string.iniciando_programacion));
    }

    @Override
    public void onProgrammingProgress(String message, int progress) {
        appendLog("⏳ " + message + " (" + progress + "%)");
        if (dialogManager != null) {
            dialogManager.updateProgress(progress, message);
        }
    }

    @Override
    public void onProgrammingCompleted(boolean success) {
        Analytics.trackEvent("Prog: Completed", crearMapaAnalitica("Success", String.valueOf(success)));
        runOnUiThread(() -> {
            if (success) {
                appendLog("✓ " + getString(R.string.pic_programado_exitosamente));
            } else {
                appendLog("❌ " + getString(R.string.error_programando_pic));
            }
        });
    }

    @Override
    public void onProgrammingError(String errorMessage) {
        Analytics.trackEvent("Prog: Error", crearMapaAnalitica("Message", errorMessage));
        runOnUiThread(() -> {
            appendLog("❌ " + errorMessage);
        });
    }

    /** Configura el listener para el switch de ICSP */
    private void setupICSPSwitchListener() {
        swModeICSP.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (currentChip == null) {
                        swModeICSP.setChecked(false);
                        return;
                    }

                    updateSwitchColors();

                    try {
                        // Si el chip es ONLY ICSP, no permitir desactivar
                        if (currentChip.isICSPOnlyCompatible()) {
                            swModeICSP.setChecked(true);
                        } else {
                            // Si el chip soporta ambos modos, permitir cambio
                            currentChip.setActivarICSP(isChecked);
                        }

                        // ACTUALIZAR IMAGEN SEGUN ESTADO DEL SWITCH
                        updateChipImage(currentChip);
                        if (isChecked) {
                            appendLog("⚙ " + getString(R.string.modo_icsp_activado));
                        } else {
                            appendLog("⚙ " + getString(R.string.modo_socket_activado));
                        }

                    } catch (ChipConfigurationException e) {
                        swModeICSP.setChecked(false);
                        appendLog("❌ " + getString(R.string.error_al_cambiar_modo) + e.getMessage());
                    }
                });
    }

    private void updateSwitchColors() {

        try {
            // Obtén el chip actual seleccionado
            boolean isICSPCompatible = currentChip != null && currentChip.isICSPOnlyCompatible();
            boolean isDualMode = currentChip != null && (!currentChip.isICSPonly());

            if (isICSPCompatible && !isDualMode) {
                // PIC SOLO COMPATIBLE CON ICSP
                // Tanto en ON como en OFF, usa el mismo verde claro
                swModeICSP.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#90EE90")));
                swModeICSP.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                swModeICSP.setEnabled(true);
            } else if (isDualMode) {
                // PIC COMPATIBLE CON AMBOS MODOS
                boolean isChecked = swModeICSP.isChecked();

                if (isChecked) {
                    // Switch ACTIVADO - Verde claro
                    swModeICSP.setTrackTintList(
                            ColorStateList.valueOf(Color.parseColor("#90EE90")));
                    swModeICSP.setThumbTintList(
                            ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                } else {
                    // Switch DESACTIVADO - Gris oscuro
                    swModeICSP.setTrackTintList(
                            ColorStateList.valueOf(Color.parseColor("#90EE90")));
                    swModeICSP.setThumbTintList(
                            ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                }
                swModeICSP.setEnabled(true);
            } else {
                // PIC NO COMPATIBLE CON ICSP
                swModeICSP.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#666666")));
                swModeICSP.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#999999")));
                swModeICSP.setEnabled(false);
            }
        } catch (ChipConfigurationException e) {
        }
    }

    /** Actualiza el estado del switch cuando se selecciona un nuevo chip */
    private void updateICSPSwitchState() {
        if (currentChip == null) {
            swModeICSP.setEnabled(false);
            swModeICSP.setChecked(false);
            return;
        }

        try {
            boolean isIcspOnly = currentChip.isICSPOnlyCompatible();
            boolean currentMode = currentChip.getICSPModoActual();

            // Desactivar listener temporalmente para evitar recursión
            swModeICSP.setOnCheckedChangeListener(null);

            if (isIcspOnly) {
                // ICSP only: switch habilitado pero siempre activado y deshabilitado
                swModeICSP.setChecked(true);
                swModeICSP.setEnabled(false);
                swModeICSP.setAlpha(0.5f); // Visual de deshabilitado
            } else {
                // Compatible con ambos: switch habilitado y usuario puede controlar
                swModeICSP.setChecked(currentMode);
                swModeICSP.setEnabled(true);
                swModeICSP.setAlpha(1.0f);
            }

            // Restaurar listener
            setupICSPSwitchListener();

        } catch (ChipConfigurationException e) {
            swModeICSP.setEnabled(false);
            swModeICSP.setChecked(false);
        }
    }

    /** NUEVO: Actualiza la imagen del socket segun el chip */
    private void updateChipImage(ChipPic chip) {
        if (chip == null) {
            chipSocketImageView.setVisibility(View.GONE);
            return;
        }

        chipSocketImageView.setVisibility(View.VISIBLE);
        int numPines = chip.getNumeroDePines();

        boolean isIcspOnly = false;
        try {
            isIcspOnly = chip.isICSPOnlyCompatible();
        } catch (ChipConfigurationException e) {
            e.printStackTrace();
        }

        // MOSTRAR ICSP SI: Es ICSP-only O el switch esta activado manualmente
        boolean isIcspActive = swModeICSP != null && swModeICSP.isChecked();

        chipSocketImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        if (socketDrawingManager != null) {
            socketDrawingManager.updateChipImage(chip, isIcspOnly || isIcspActive || numPines == 0);
        }
    }

    private void setupPersistentLayoutListener() {
        chipSocketImageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                // Si el tamaño real cambió y es valido, redibujamos
                if ((right - left) > 0 && (bottom - top) > 0 &&
                        (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom)) {
                    updateChipImage(currentChip);
                }
            }
        });
    }

    private void setupFileManagerListeners() {
        fileManager.setFileLoadListener(
                new FileManager.FileLoadListener() {
                    @Override
                    public void onFileLoaded(String content, String fileName) {
                        firmware = content;
                        originalLoadedFirmware = content;
                        lastProgrammedFirmware = "";
                        lastProgrammedDatosPic = null;

                        // Detectar tipo de archivo para mensaje coherente
                        boolean esBin = fileName != null && fileName.toLowerCase().endsWith(".bin");
                        String tipoArchivo = esBin ? ".BIN" : ".HEX";
                        appendLog(getString(R.string.log_loading_file, tipoArchivo, fileName));

                        actualizarEstadoBotones(usbManager != null && usbManager.isConnected(), true);

                        // Resetear configuración de fusibles al cargar nuevo archivo
                        clearFuseConfiguration();

                        // Procesar datos del archivo
                        appendLog(getString(R.string.log_processing_file, tipoArchivo));
                        procesarDatosHex();
                    }

                    @Override
                    public void onFileLoadError(String errorMessage) {
                        appendLog("❌ " + getString(R.string.error_cargando_archivo) + ": " + errorMessage);
                    }
                });
    }

    private void setupButtonListeners() {
        btnSelectHex.setOnClickListener(v -> fileManager.openFilePicker());
        btnConfigureFuses.setOnClickListener(v -> openFuseConfiguration());
        btnProgramarPic.setOnClickListener(v -> executeProgram());
        btnProgramarPic.setOnLongClickListener(v -> {
            executeProgram();
            return true;
        });
        btnLeerMemoriaDeLPic.setOnClickListener(v -> executeReadMemory());
        btnBorrarMemoriaDeLPic.setOnClickListener(v -> executeEraseMemory());
        btnVerificarMemoriaDelPic.setOnClickListener(v -> executeVerifyMemory());
        btnDetectarPic.setOnClickListener(v -> executeDetectChip());
        btnBlankCheck.setOnClickListener(v -> ejecutarBlankCheck());
        if (btnLimpiarLog != null) {
            btnLimpiarLog.setOnClickListener(v -> clearLog());
        }

        setupLogCopySupport();
    }

    private void setupLogCopySupport() {
        if (processStatusTextView == null) {
            return;
        }
        processStatusTextView.setTextIsSelectable(true);
        processStatusTextView.setLongClickable(true);
        processStatusTextView.setOnLongClickListener(v -> {
            String logs = processStatusTextView.getText() == null ? "" : processStatusTextView.getText().toString();
            if (logs.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_no_log_text), Toast.LENGTH_SHORT).show();
                return true;
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) {
                Toast.makeText(this, getString(R.string.toast_clipboard_fail), Toast.LENGTH_SHORT).show();
                return true;
            }

            clipboard.setPrimaryClip(ClipData.newPlainText("logs", logs));
            Toast.makeText(this, getString(R.string.toast_logs_copied), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    /** NUEVO: Procesa los datos del archivo HEX */
    private void procesarDatosHex() {
        if (currentChip == null || firmware.isEmpty()) {
            return;
        }

        new Thread(
                () -> {
                    try {
                        datosPicProcesados = new DatosPicProcesados(MainActivity.this, firmware, currentChip);
                        datosPicProcesados.iniciarProcesamientoDeDatos();
                        if (firmware != null && firmware.equals(originalLoadedFirmware)) {
                            originalLoadedDatosPic = datosPicProcesados;
                        }

                        runOnUiThread(
                                () -> {
                                    byte[] romBytes = datosPicProcesados.obtenerBytesHexROMProcesado();
                                    byte[] eepromBytes = datosPicProcesados.obtenerBytesHexEEPROMProcesado();
                                    int romLen = (romBytes != null) ? romBytes.length : 0;
                                    int eepromLen = (eepromBytes != null) ? eepromBytes.length : 0;

                                    lastReadRomData = romBytes != null ? com.diamon.utilidades.ByteUtils.bytesToHex(romBytes) : "";
                                    lastReadEepromData = eepromBytes != null ? com.diamon.utilidades.ByteUtils.bytesToHex(eepromBytes) : "";

                                    popularContenedoresMemoria(romBytes, eepromBytes);

                                    String detailMsg = getString(R.string.hex_procesado_correctamente)
                                            + "\n  ROM: " + romLen + " B, EEPROM: " + eepromLen + " B";
                                    appendLog("✓ " + detailMsg);

                                    boolean tieneArchivoValido = originalLoadedFirmware != null && !originalLoadedFirmware.isEmpty() && originalLoadedDatosPic != null;
                                    actualizarEstadoBotones(usbManager != null && usbManager.isConnected(), tieneArchivoValido);
                                });

                    } catch (Exception e) {
                        runOnUiThread(
                                () -> {
                                    originalLoadedDatosPic = null;
                                    actualizarEstadoBotones(usbManager != null && usbManager.isConnected(), false);
                                    appendLog("❌ " + getString(R.string.error_procesando_hex) + ": " + e.getMessage());
                                });
                    }
                })
                .start();
    }

    /** Actualiza visibilidad de publicidad y Toolbar según el estado Pro. */
    private void actualizarEstadoProUI() {
        runOnUiThread(() -> {
            boolean isPro = proPassManager != null && proPassManager.isProActive(billingManager);
            if (isPro) {
                if (publicidad != null) {
                    publicidad.ocultarBanner();
                }
            }
            invalidateOptionsMenu();
        });
    }

    /** Muestra el diálogo modal de Acceso K150 Pro. */
    private void showProAccessDialog(Runnable onUnlocked) {
        if (billingManager != null && billingManager.isProPurchased()) {
            mostrarDialogoProYaAdquirido();
            if (onUnlocked != null) {
                onUnlocked.run();
            }
            return;
        }
        ProAccessDialog.show(this, publicidad, billingManager, proPassManager, () -> {
            actualizarEstadoProUI();
            if (onUnlocked != null) {
                onUnlocked.run();
            }
        });
    }

    /**
     * Muestra información de confirmación cuando el usuario ya ha adquirido la versión Pro permanente.
     */
    private void mostrarDialogoProYaAdquirido() {
        float density = getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setBackgroundColor(0xFF181824);

        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setPadding(pad, pad, pad, pad);
        textView.setTextSize(14);
        textView.setTextColor(0xFFE0E0E0);
        textView.setLineSpacing(0, 1.3f);

        String htmlText = "<h3><b><font color=\"#FFD700\">⭐ " + getString(R.string.pro_permanent_active_title) + "</font></b></h3>" +
                "<p>" + getString(R.string.pro_permanent_active_desc) + "</p>";

        textView.setText(androidx.core.text.HtmlCompat.fromHtml(htmlText, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));
        scrollView.addView(textView);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.pro_permanent_active_title))
                .setView(scrollView)
                .setPositiveButton(getString(R.string.aceptar), null)
                .show();
    }

    /**
     * Editor Hexadecimal en Vivo: Abre el diálogo modal para modificar
     * una palabra ROM o byte EEPROM directamente.
     */
    private void abrirEditorHexadecimal(boolean isRom, int address, TextView rowView) {
        if (proPassManager != null && !proPassManager.isProActive(billingManager)) {
            showProAccessDialog(() -> abrirEditorHexadecimal(isRom, address, rowView));
            return;
        }
        if (hexEditorManager == null) return;
        int valorActual = HexEditorManager.obtenerValorActualEnMemoria(
                isRom ? lastReadRomData : lastReadEepromData,
                isRom,
                address,
                datosPicProcesados,
                currentChip);

        hexEditorManager.mostrarDialogoEdicion(
                this,
                isRom,
                address,
                valorActual,
                currentChip,
                datosPicProcesados,
                firmware,
                rowView,
                (isRomEdited, addr, oldVal, newVal, updatedFirmware) -> {
                    if (isRomEdited) {
                        lastReadRomData = HexEditorManager.actualizarCeldaEnHexLeido(
                                lastReadRomData, true, addr, newVal, currentChip);
                        int romSize = 0;
                        try {
                            if (currentChip != null) {
                                romSize = currentChip.getTamanoROM();
                            }
                        } catch (Exception ignored) {
                        }
                        if (memoryDisplayManager != null) {
                            memoryDisplayManager.refreshRow(true, addr, lastReadRomData, romSize);
                        }
                    } else {
                        lastReadEepromData = HexEditorManager.actualizarCeldaEnHexLeido(
                                lastReadEepromData, false, addr, newVal, currentChip);
                        int eepromSize = 0;
                        try {
                            if (currentChip != null && currentChip.isTamanoValidoDeEEPROM()) {
                                eepromSize = currentChip.getTamanoEEPROM();
                            }
                        } catch (Exception ignored) {
                        }
                        if (memoryDisplayManager != null) {
                            memoryDisplayManager.refreshRow(false, addr, lastReadEepromData, eepromSize);
                        }
                    }
                    appendLog(getString(R.string.log_memoria_editada,
                            isRomEdited ? "ROM" : "EEPROM", addr, oldVal, newVal));
                }
        );
    }

    /**
     * Llena los contenedores de memoria interactivos con filas clickeables
     * para permitir edición en vivo celda por celda.
     */
    private void popularContenedoresMemoria(byte[] romBytes, byte[] eepromBytes) {
        if (romDataContainer != null) {
            romDataContainer.removeAllViews();
            if (romBytes != null && romBytes.length > 0) {
                int wordsPerRow = 8;
                int totalWords = romBytes.length / 2;
                for (int w = 0; w < totalWords; w += wordsPerRow) {
                    final int addr = w;
                    TextView tv = new TextView(this);
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format(Locale.getDefault(), "0x%04X: ", addr));
                    for (int i = 0; i < wordsPerRow && (w + i) < totalWords; i++) {
                        int idx = (w + i) * 2;
                        int wordVal = (romBytes[idx] & 0xFF) | ((romBytes[idx + 1] & 0xFF) << 8);
                        sb.append(String.format(Locale.getDefault(), "%04X ", wordVal));
                    }
                    tv.setText(sb.toString().trim());
                    tv.setTextColor(Color.parseColor("#4CAF50"));
                    tv.setPadding(0, 4, 0, 4);
                    tv.setOnClickListener(v -> abrirEditorHexadecimal(true, addr, tv));
                    romDataContainer.addView(tv);
                }
            }
        }

        if (eepromDataContainer != null) {
            eepromDataContainer.removeAllViews();
            if (eepromBytes != null && eepromBytes.length > 0) {
                int bytesPerRow = 16;
                for (int b = 0; b < eepromBytes.length; b += bytesPerRow) {
                    final int addr = b;
                    TextView tv = new TextView(this);
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format(Locale.getDefault(), "0x%04X: ", addr));
                    for (int i = 0; i < bytesPerRow && (b + i) < eepromBytes.length; i++) {
                        int val = eepromBytes[b + i] & 0xFF;
                        sb.append(String.format(Locale.getDefault(), "%02X ", val));
                    }
                    tv.setText(sb.toString().trim());
                    tv.setTextColor(Color.parseColor("#00BCD4"));
                    tv.setPadding(0, 4, 0, 4);
                    tv.setOnClickListener(v -> abrirEditorHexadecimal(false, addr, tv));
                    eepromDataContainer.addView(tv);
                }
            }
        }
    }

    /** NUEVO: Abre el popup de configuración de fusibles */
    private void openFuseConfiguration() {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.selecciona_chip_primero));
            return;
        }

        if (currentChip.getFusesMap() == null || currentChip.getFusesMap().isEmpty()) {
            appendLog("⚠ " + getString(R.string.no_hay_fusibles_para_chip));
            return;
        }

        // Mostrar popup con ultima configuracion si existe
        fuseConfigPopup.show(currentChip, datosPicProcesados, lastFuseConfiguration);
    }

    /** NUEVO: Limpia la configuración de fusibles */
    private void clearFuseConfiguration() {
        fusesConfigured = false;
        configuredFuses = new ArrayList<>();
        configuredID = new byte[] { 0 };
        lastFuseConfiguration = null;
        datosPicProcesados = null;
        updateFuseStatus(false);
    }

    /** NUEVO: Actualiza el indicador de estado de fusibles */
    private void updateFuseStatus(boolean configured) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setCornerRadius(dpToPx(4));
        
        if (configured) {
            fuseStatusTextView.setText("✓ " + getString(R.string.fuses_configurados));
            fuseStatusTextView.setTextColor(Color.WHITE);
            gd.setColor(Color.parseColor("#4CAF50"));
        } else {
            fuseStatusTextView.setText(getString(R.string.fuses_no_configurados));
            fuseStatusTextView.setTextColor(Color.parseColor("#757575"));
            gd.setColor(Color.parseColor("#3A3A3A"));
        }
        fuseStatusTextView.setBackground(gd);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setButtonState(android.widget.Button btn, boolean enabled, int activeColor) {
        if (btn == null) return;
        btn.setEnabled(enabled);
        if (enabled) {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(activeColor));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#35354D")));
            btn.setTextColor(Color.parseColor("#6D6D8A"));
        }
    }

    /**
     * Desacoplamiento de botones de hardware y firmware:
     * - Operaciones de hardware (Detectar, Leer, Borrar, Blank Check) habilitadas si hardwareConectado == true
     * - Operación «Programar PIC» habilitada ÚNICAMENTE si hardwareConectado == true Y se ha cargado
     *   explícitamente un archivo HEX o BIN desde almacenamiento (originalLoadedFirmware).
     * - Operación «Verificar Memoria» habilitada si hardwareConectado == true Y existe firmware válido
     *   para comparar (bien sea el último grabado exitosamente, original o modificado, o el archivo cargado).
     * - Configuración de fusibles habilitada si currentChip != null
     */
    private void actualizarEstadoBotones(boolean hardwareConectado, boolean firmwareCargado) {
        // Operaciones de hardware (independientes del archivo HEX)
        setButtonState(btnDetectarPic, hardwareConectado, Color.parseColor("#00B0FF"));
        setButtonState(btnLeerMemoriaDeLPic, hardwareConectado, Color.parseColor("#00B0FF"));
        setButtonState(btnBorrarMemoriaDeLPic, hardwareConectado, Color.parseColor("#D32F2F"));
        setButtonState(btnBlankCheck, hardwareConectado, Color.parseColor("#00B0FF"));

        // Programar PIC requiere estrictamente hardware conectado y archivo original cargado desde disco
        boolean archivoRealCargado = originalLoadedFirmware != null && !originalLoadedFirmware.isEmpty() && originalLoadedDatosPic != null;
        boolean progHabilitado = hardwareConectado && firmwareCargado && archivoRealCargado;
        setButtonState(btnProgramarPic, progHabilitado, Color.parseColor("#FF6600"));

        // Verificar Memoria requiere hardware conectado y datos para verificar (último grabado o archivo cargado)
        boolean tieneDatosParaVerificar = (lastProgrammedDatosPic != null) || (firmwareCargado && archivoRealCargado);
        boolean verificarHabilitado = hardwareConectado && tieneDatosParaVerificar;
        setButtonState(btnVerificarMemoriaDelPic, verificarHabilitado, Color.parseColor("#00B0FF"));

        // Configuración de fusibles (requiere chip seleccionado)
        boolean fusesHabilitado = currentChip != null;
        setButtonState(btnConfigureFuses, fusesHabilitado, Color.parseColor("#00B0FF"));
    }

    /**
     * Verifica la presencia del microcontrolador en el zócalo ZIF antes de realizar
     * operaciones que lo requieran (Programar, Leer, Borrar, Verificar, Blank Check).
     *
     * Si el chip ya está en el zócalo, ejecuta la operación de inmediato.
     * Si no está en el zócalo, muestra la advertencia y monitorea la inserción;
     * tan pronto como se inserte el chip, ejecuta la operación automáticamente.
     */
    private void verificarChipEnSocketYEjecutar(Runnable operacion, String nombreOperacion) {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip));
            return;
        }

        if (usbManager == null || !usbManager.isConnected()) {
            appendLog(getString(R.string.log_connect_programmer_first));
            return;
        }

        // Si está en modo ICSP, la conexión es externa a los pines del circuito; omitir sensor de socket ZIF
        if (swModeICSP != null && swModeICSP.isChecked()) {
            operacion.run();
            return;
        }

        Protocolo proto = usbManager.getProtocolo();
        if (proto == null) {
            operacion.run();
            return;
        }

        appendLog(getString(R.string.log_verifying_pic_in_socket));
        new Thread(() -> {
            boolean chipEnSocket = proto.detectarPicEnElSocket();
            if (chipEnSocket) {
                // El microcontrolador ya está en el zócalo; proceder normalmente
                runOnUiThread(operacion);
            } else {
                // No hay chip en el zócalo; alertar y esperar inserción automática
                runOnUiThread(() -> mostrarAlertaYEspanarInsercion(operacion, nombreOperacion, proto));
            }
        }, "VerificarSocketThread").start();
    }

    private void mostrarAlertaYEspanarInsercion(Runnable operacion, String nombreOperacion, Protocolo proto) {
        if (isFinishing() || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }

        if (notificacionVibracionManager != null) {
            notificacionVibracionManager.vibrarAlerta();
            notificacionVibracionManager.reproducirSonidoNotificacion();
        }

        String chipName = (currentChip != null ? currentChip.getNombreDelPic() : "");
        appendLog(getString(R.string.log_no_chip_in_socket, chipName, nombreOperacion));

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_insert_microcontroller_title))
                .setMessage(getString(R.string.dialog_insert_microcontroller_message, chipName, nombreOperacion))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.aceptar, null)
                .create();

        dialog.show();
    }

    /**
     * Programación Selectiva: Ofrece modal con las 4 opciones
     * (Completa, Solo Flash ROM, Solo EEPROM, Solo Configuración).
     * Siempre y únicamente programa el archivo HEX/BIN original cargado desde el selector de archivos.
     * Si no se ha cargado un archivo desde disco, nunca programa volcados de lectura ni memoria modificada en visor.
     */
    private void executeProgram() {
        if (originalLoadedFirmware == null || originalLoadedFirmware.isEmpty() || originalLoadedDatosPic == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip_y_cargue_un));
            return;
        }

        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip));
            return;
        }

        firmware = originalLoadedFirmware;
        datosPicProcesados = originalLoadedDatosPic;

        verificarChipEnSocketYEjecutar(
                () -> iniciarFlujoSeleccionProgramacion(originalLoadedFirmware, originalLoadedDatosPic),
                getString(R.string.programar_pic));
    }

    /**
     * Reprograma el microcontrolador utilizando los datos leídos de la memoria y
     * potencialmente modificados a través del editor hexadecimal interactivo en vivo.
     * Esta operación es independiente del archivo HEX/BIN cargado y no altera
     * el estado de los botones principales de la pantalla ni el archivo original.
     */
    private void executeReprogramFromMemoryDisplay() {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip));
            return;
        }

        if (usbManager == null || !usbManager.isConnected()) {
            appendLog(getString(R.string.log_connect_programmer_first));
            return;
        }

        if (lastReadRomData.isEmpty() && lastReadEepromData.isEmpty() && lastReadConfigData.isEmpty()) {
            appendLog("⚠ " + getString(R.string.no_hay_datos_para_exportar));
            return;
        }

        String hexReprogramar = HexEditorManager.construirFirmwareIntelHexDesdeLectura(
                lastReadRomData, lastReadEepromData, lastReadConfigData, currentChip);
        if (hexReprogramar == null || hexReprogramar.isEmpty()) {
            appendLog("❌ " + getString(R.string.error_procesando_hex));
            return;
        }

        DatosPicProcesados datosReprogramar;
        try {
            datosReprogramar = new DatosPicProcesados(this, hexReprogramar, currentChip);
            datosReprogramar.iniciarProcesamientoDeDatos();
        } catch (Exception e) {
            appendLog("❌ " + getString(R.string.error_procesando_hex) + ": " + e.getMessage());
            return;
        }

        verificarChipEnSocketYEjecutar(
                () -> iniciarFlujoSeleccionProgramacion(hexReprogramar, datosReprogramar),
                getString(R.string.reprogramar));
    }

    private void iniciarFlujoSeleccionProgramacion(String hexToProgram, DatosPicProcesados datosToProgram) {
        if (programacionSelectivaManager != null) {
            programacionSelectivaManager.mostrarMenuSeleccion(
                    this,
                    currentChip,
                    datosToProgram,
                    fusesConfigured,
                    tipo -> doProgrammingFlow(tipo, hexToProgram, datosToProgram));
        } else {
            doProgrammingFlow(TipoProgramacion.COMPLETA, hexToProgram, datosToProgram);
        }
    }

    private void doProgrammingFlow(TipoProgramacion tipo, String hexToProgram, DatosPicProcesados datosToProgram) {
        publicidad.ocultarBanner();

        final byte[] idToUse = fusesConfigured ? configuredID : new byte[] { 0 };
        final List<Integer> fusesToUse = fusesConfigured ? new ArrayList<>(configuredFuses) : new ArrayList<>();

        dialogManager.setChip(currentChip);
        dialogManager.showProgrammingDialog(
                () -> {
                    new Thread(
                            () -> {
                                boolean success;
                                if (tipo == TipoProgramacion.SOLO_ROM) {
                                    success = programmingManager.programRomOnly(currentChip, hexToProgram);
                                } else if (tipo == TipoProgramacion.SOLO_EEPROM) {
                                    success = programmingManager.programEepromOnly(currentChip, hexToProgram);
                                } else if (tipo == TipoProgramacion.SOLO_CONFIG) {
                                    success = programmingManager.programConfigOnly(currentChip, hexToProgram, idToUse, fusesToUse);
                                } else {
                                    // Default: Grabación Completa
                                    success = programmingManager.programChip(currentChip, hexToProgram, idToUse, fusesToUse);
                                }

                                final boolean finalSuccess = success;
                                runOnUiThread(
                                        () -> {
                                            dialogManager.updateProgrammingResult(finalSuccess);
                                            if (finalSuccess) {
                                                lastProgrammedFirmware = hexToProgram;
                                                lastProgrammedDatosPic = datosToProgram;

                                                if (notificacionVibracionManager != null) {
                                                    notificacionVibracionManager.vibrarExito();
                                                    notificacionVibracionManager.reproducirSonidoNotificacion();
                                                }
                                                appendLog(getString(R.string.log_programming_verified_success));
                                            } else {
                                                if (notificacionVibracionManager != null) {
                                                    notificacionVibracionManager.vibrarAlerta();
                                                }
                                                appendLog(getString(R.string.log_programming_failed));
                                            }

                                            // Asegurar que el estado de los botones refleje el archivo cargado y la disponibilidad de verificación
                                            boolean archivoRealCargado = originalLoadedFirmware != null && !originalLoadedFirmware.isEmpty() && originalLoadedDatosPic != null;
                                            actualizarEstadoBotones(usbManager != null && usbManager.isConnected(), archivoRealCargado);
                                        });
                            })
                            .start();
                },
                () -> {
                    publicidad.mostrarBanner();
                });
    }

    private void executeReadMemory() {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip));
            return;
        }
        verificarChipEnSocketYEjecutar(this::doReadMemoryOperation, getString(R.string.leer_memoria));
    }

    private void doReadMemoryOperation() {
        // Ocultar banner mientras se muestra el popup
        publicidad.ocultarBanner();

        // Mostrar popup con estado de carga ANTES de leer (no bloquea UI)
        memoryDisplayManager.showLoadingState();

        // Leer memoria en hilo secundario
        new Thread(
                () -> {
                    String romData = programmingManager.readRomMemory(currentChip);
                    String eepromData = programmingManager.readEepromMemory(currentChip);
                    String configData = programmingManager.readConfigData(currentChip);

                    // NUEVO: Guardar datos para exportación posterior
                    final String romResult = romData;
                    final String eepromResult = eepromData;
                    final String configResult = configData;

                    runOnUiThread(
                            () -> {
                                try {
                                    // Guardar datos leídos para exportación
                                    lastReadRomData = romResult != null ? romResult : "";
                                    lastReadEepromData = eepromResult != null ? eepromResult : "";
                                    lastReadConfigData = configResult != null ? configResult : "";

                                    int romSize = currentChip.getTamanoROM();
                                    int eepromSize = currentChip.isTamanoValidoDeEEPROM()
                                            ? currentChip.getTamanoEEPROM()
                                            : 0;
                                    boolean hasEeprom = currentChip.isTamanoValidoDeEEPROM()
                                            && !lastReadEepromData.isEmpty();

                                    // Actualizar popup con los datos leídos
                                    memoryDisplayManager.updateWithData(
                                            lastReadRomData,
                                            romSize,
                                            lastReadEepromData,
                                            eepromSize,
                                            hasEeprom);

                                    appendLog("✓ " + getString(R.string.memoria_leida_exitosamente));

                                    // Mostrar banner de nuevo
                                    publicidad.mostrarBanner();
                                } catch (ChipConfigurationException e) {
                                    appendLog("❌ " + getString(R.string.error_obteniendo_datos_del_chi)
                                            + ": " + e.getMessage());
                                    publicidad.mostrarBanner();
                                }
                            });
                })
                .start();
    }

    private void executeEraseMemory() {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip));
            return;
        }
        verificarChipEnSocketYEjecutar(this::doEraseMemoryOperation, getString(R.string.borrar_memoria));
    }

    private void doEraseMemoryOperation() {
        new Thread(
                () -> {
                    if (currentChip != null && CalibracionManager.requiereCalibracion(currentChip)) {
                        runOnUiThread(() -> appendLog(getString(R.string.log_backing_up_osccal)));
                    }

                    CalibracionManager.ResultadoBorradoSeguro res = (calibracionManager != null && currentChip != null)
                            ? calibracionManager.borrarConPreservacion(currentChip, programmingManager)
                            : new CalibracionManager.ResultadoBorradoSeguro(programmingManager.eraseMemory(), false, null, false, "");

                    runOnUiThread(
                            () -> {
                                if (res.borradoExitoso) {
                                    appendLog("✓ " + getString(
                                            R.string.memoria_borrada_exitosamente));
                                    if (res.requiereCalibracion) {
                                        if (res.restauracionExitosa && res.osccalPreservado != null) {
                                            String calHex = CalibracionManager.formatearCalibracionHex(
                                                    res.osccalPreservado, CalibracionManager.isFamilia10F(currentChip));
                                            appendLog(getString(R.string.log_osccal_restored, calHex));
                                        } else if (res.osccalPreservado != null) {
                                            appendLog(getString(R.string.log_osccal_restore_failed_warning));
                                        }
                                    }
                                } else {
                                    appendLog("❌ " + getString(R.string.error_borrando_memoria));
                                }
                            });
                })
                .start();
    }

    private void executeVerifyMemory() {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip));
            return;
        }

        DatosPicProcesados datosParaVerificar = (lastProgrammedDatosPic != null)
                ? lastProgrammedDatosPic
                : originalLoadedDatosPic;

        if (datosParaVerificar == null) {
            appendLog("❌ " + getString(R.string.error_verificando_memoria) + ": " + getString(R.string.no_hay_firmware_valido));
            return;
        }

        datosPicProcesados = datosParaVerificar;
        firmware = (lastProgrammedFirmware != null && !lastProgrammedFirmware.isEmpty())
                ? lastProgrammedFirmware
                : originalLoadedFirmware;

        verificarChipEnSocketYEjecutar(this::doVerifyMemoryOperation, getString(R.string.verificar));
    }

    private void doVerifyMemoryOperation() {
        appendLog("⏳ " + getString(R.string.verificando_memoria));

        new Thread(
                () -> {
                    try {
                        // Procesar el HEX cargado (firmware) a bytes si es necesario
                        byte[] expectedRomBytes = datosPicProcesados.obtenerBytesHexROMProcesado();
                        byte[] expectedEepromBytes = datosPicProcesados.obtenerBytesHexEEPROMProcesado();

                        com.diamon.managers.VerificationManager.VerificationResult result = com.diamon.managers.VerificationManager
                                .verify(
                                        MainActivity.this,
                                        programmingManager.getProtocolo(),
                                        currentChip,
                                        expectedRomBytes,
                                        expectedEepromBytes);

                        runOnUiThread(
                                () -> {
                                    StringBuilder statusMsg = new StringBuilder();
                                    statusMsg.append("✓ ").append(getString(R.string.verificacion_completa));
                                    statusMsg.append("\n");

                                    // ROM
                                    if (result.romVerified) {
                                        statusMsg.append("  ").append(getString(R.string.verificacion_rom_ok));
                                    } else if (result.romMaybeLocked) {
                                        statusMsg.append("  ").append(getString(R.string.verificacion_rom_fallo))
                                                .append(" — ")
                                                .append(getString(R.string.rom_posible_locked));
                                    } else {
                                        statusMsg.append("  ").append(getString(R.string.verificacion_rom_fallo));
                                    }

                                    // Chip config info
                                    if (result.chipIdHex != null) {
                                        statusMsg.append("\n  ").append(getString(R.string.chip_id_label)).append(" ").append(result.chipIdHex);
                                    }

                                    // Fuses decodificados
                                    if (result.decodedFuses != null && !result.decodedFuses.isEmpty()) {
                                        statusMsg.append("\n  ").append(getString(R.string.fuses_decodificados))
                                                .append(":");
                                        for (java.util.Map.Entry<String, String> fuse : result.decodedFuses
                                                .entrySet()) {
                                            statusMsg.append("\n    ").append(fuse.getKey())
                                                    .append(" = ").append(fuse.getValue());
                                        }
                                    }

                                    statusMsg.append("\n  ").append(getString(R.string.protocolo)).append(": ").append(getString(R.string.protocolo_descripcion));
                                    appendLog(statusMsg.toString());
                                });

                    } catch (Exception e) {
                        runOnUiThread(
                                () -> appendLog("❌ " +
                                        getString(R.string.error_verificando_memoria)
                                                + ": " + e.getMessage()));
                    }
                })
                .start();
    }

    private void executeDetectChip() {
        new Thread(() -> {
            runOnUiThread(() -> appendLog(getString(R.string.log_detecting_chip_in_socket)));

            PicProgrammingManager.ResultadoAutodeteccion res =
                    programmingManager.autodectarChip(chipSelectionManager.getChipReader());
            runOnUiThread(() -> {
                if (!res.chipEnSocket) {
                    appendLog(getString(R.string.log_no_pic_detected_in_zif));
                    return;
                }
                appendLog(getString(R.string.log_pic_physically_detected));
                if (res.modeloIdentificado != null) {
                    String hexStr = String.format("0x%04X", res.rawDeviceId);
                    appendLog(getString(R.string.log_chip_auto_identified, res.modeloIdentificado, hexStr));
                    firmware = "";
                    originalLoadedFirmware = "";
                    datosPicProcesados = null;
                    originalLoadedDatosPic = null;
                    lastProgrammedFirmware = "";
                    lastProgrammedDatosPic = null;
                    if (fileManager != null) {
                        fileManager.clearLoadedFile();
                    }
                    actualizarEstadoBotones(usbManager != null && usbManager.isConnected(), false);
                    chipSelectionManager.seleccionarModeloEnSpinner(chipSpinner, res.modeloIdentificado);
                    Toast.makeText(MainActivity.this, getString(R.string.toast_chip_detected, res.modeloIdentificado), Toast.LENGTH_SHORT).show();
                } else {
                    String hexStr = (res.rawDeviceId > 0) ? String.format("0x%04X", res.rawDeviceId) : getString(R.string.unknown_device_id);
                    appendLog(getString(R.string.log_chip_present_select_manually, hexStr));
                    firmware = "";
                    originalLoadedFirmware = "";
                    datosPicProcesados = null;
                    originalLoadedDatosPic = null;
                    lastProgrammedFirmware = "";
                    lastProgrammedDatosPic = null;
                    if (fileManager != null) {
                        fileManager.clearLoadedFile();
                    }
                    actualizarEstadoBotones(usbManager != null && usbManager.isConnected(), false);
                }
            });
        }).start();
    }

    /** Ejecuta la verificación de borrado (Blank Check) */
    private void ejecutarBlankCheck() {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.seleccione_un_chip));
            return;
        }

        verificarChipEnSocketYEjecutar(this::doBlankCheckOperation, getString(R.string.verificar_borrado));
    }

    private void doBlankCheckOperation() {
        appendLog("⏳ " + getString(R.string.verificando_borrado) + "...");

        new Thread(() -> {
            try {
                PicProgrammingManager.ResultadoVerificacionBorrado resultado = programmingManager
                        .verificarBorradoCompleto(currentChip);

                runOnUiThread(() -> {
                    if (programacionSelectivaManager != null) {
                        List<String> lineasReporte = programacionSelectivaManager
                                .formatearReporteBlankCheckDual(MainActivity.this, resultado, currentChip);
                        for (String linea : lineasReporte) {
                            appendLog(linea);
                        }
                    } else {
                        if (resultado.error != null && !resultado.error.isEmpty()) {
                            appendLog("❌ " + getString(R.string.error_verificando_borrado) + ": " + resultado.error);
                            return;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("✓ ").append(getString(R.string.resultado_verificacion_borrado)).append(":\n");
                        sb.append("  ROM: ").append(
                                resultado.romEnBlanco ? getString(R.string.rom_ok_blank) : getString(R.string.rom_not_blank))
                                .append("\n");

                        if (currentChip.isTamanoValidoDeEEPROM()) {
                            sb.append("  EEPROM: ")
                                     .append(resultado.eepromEnBlanco ? getString(R.string.eeprom_ok_blank)
                                            : getString(R.string.eeprom_not_blank))
                                    .append("\n");
                        }

                        sb.append("  ").append(getString(R.string.protocolo)).append(": ").append(resultado.metodoUtilizado);
                        appendLog(sb.toString());

                        if (resultado.chipEnBlanco()) {
                            appendLog("✓ " + getString(R.string.chip_esta_borrado));
                        } else {
                            appendLog("⚠ " + getString(R.string.chip_no_esta_borrado));
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> appendLog("❌ " + getString(R.string.error_verificando_borrado) + ": " + e.getMessage()));
            }
        }).start();
    }

    @SuppressLint("InvalidWakeLockTag")
    private void setupWakeLock() {
        // CORREGIDO: FULL_WAKE_LOCK está deprecado.
        // Usamos FLAG_KEEP_SCREEN_ON para la pantalla y PARTIAL_WAKE_LOCK para la CPU.
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PICProgramming:Proceso");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        String proTitle = getString(R.string.menu_pro_mode);
        if (proPassManager != null && proPassManager.isProActive(billingManager)) {
            if (billingManager != null && billingManager.isProPurchased()) {
                proTitle = getString(R.string.menu_pro_permanent);
            } else {
                proTitle = getString(R.string.menu_pro_timer, proPassManager.formatearTiempoRestante());
            }
        }
        menu.add(Menu.NONE, 12, 0, proTitle).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(Menu.NONE, 9, 1, getString(R.string.menu_hardware_diagnostic)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 10, 2, getString(R.string.menu_osccal_calibration)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 11, 3, getString(R.string.menu_debug_vector_icd)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 1, 4, "🔌 " + getString(R.string.modelo_programador)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 2, 5, "⚡ " + getString(R.string.seleccionar_protocolo)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 6, 6, "💾 " + getString(R.string.exportar_memoria)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 7, 7, "📋 " + getString(R.string.chip_info_json)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 3, 8, "📚 " + getString(R.string.gputils_termux_asm)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 5, 9, "📚 " + getString(R.string.sdcc_termux_tutorial)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 4, 10, "🛡️ " + getString(R.string.politica_de_privacidad)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 8, 11, "ℹ️ " + getString(R.string.acerca_de_licencias)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 12:
                if (billingManager != null && billingManager.isProPurchased()) {
                    mostrarDialogoProYaAdquirido();
                } else {
                    showProAccessDialog(null);
                }
                return true;
            case 9:
                if (proPassManager != null && !proPassManager.isProActive(billingManager)) {
                    showProAccessDialog(this::abrirDiagnosticoHardware);
                    return true;
                }
                abrirDiagnosticoHardware();
                return true;
            case 10:
                if (proPassManager != null && !proPassManager.isProActive(billingManager)) {
                    showProAccessDialog(() -> {
                        if (calibracionManager != null) {
                            calibracionManager.showCalibrationDialog(this, currentChip);
                        }
                    });
                    return true;
                }
                if (calibracionManager != null) {
                    calibracionManager.showCalibrationDialog(this, currentChip);
                }
                return true;
            case 11:
                if (proPassManager != null && !proPassManager.isProActive(billingManager)) {
                    showProAccessDialog(() -> {
                        if (calibracionManager != null) {
                            calibracionManager.showDebugVectorDialog(this, currentChip);
                        }
                    });
                    return true;
                }
                if (calibracionManager != null) {
                    calibracionManager.showDebugVectorDialog(this, currentChip);
                }
                return true;
            case 1:
                showProgrammerModelDialog();
                return true;
            case 2:
                showProtocolDialog();
                return true;
            case 6:
                showExportDialog();
                return true;
            case 7:
                showChipInfoJson();
                return true;
            case 3:
                openTutorialGputils();
                return true;
            case 5:
                openTutorialSdcc();
                return true;
            case 4:
                startActivity(new Intent(this, PoliticaPrivacidadActivity.class));
                return true;
            case 8:
                showAboutLicensesDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void abrirDiagnosticoHardware() {
        if (proPassManager != null && !proPassManager.isProActive(billingManager)) {
            showProAccessDialog(this::abrirDiagnosticoHardware);
            return;
        }
        // Igual que Calibración OSCCAL y Vector ICD: la ventana se abre siempre y
        // la verificación de conexión se realiza al ejecutar cada prueba.
        boolean conectado = usbManager != null && usbManager.isConnected();
        diagnosticoHardwareManager.showDiagnosticoDialog(
                this,
                conectado ? usbManager.getProtocolo() : null,
                this::appendLog);
    }

    /** NUEVO: Muestra diálogo para exportar memoria leída */
    private void showExportDialog() {
        if (lastReadRomData.isEmpty() && lastReadEepromData.isEmpty() && lastReadConfigData.isEmpty()) {
            appendLog("⚠ " + getString(R.string.no_hay_datos_para_exportar));
            return;
        }

        String chipName = (currentChip != null) ? currentChip.getNombreDelPic() : "PIC";
        java.util.List<String> options = new java.util.ArrayList<>();

        if (!lastReadRomData.isEmpty()) {
            options.add(getString(R.string.exportar_rom_hex));
            options.add(getString(R.string.exportar_rom_bin));
        }
        if (!lastReadEepromData.isEmpty()) {
            options.add(getString(R.string.exportar_eeprom_hex));
            options.add(getString(R.string.exportar_eeprom_bin));
        }
        if (!lastReadConfigData.isEmpty()) {
            options.add(getString(R.string.exportar_config_hex));
            options.add(getString(R.string.exportar_config_bin));
        }

        // Si hay al menos ROM y (EEPROM o Config), ofrecer un volcado completo
        if (!lastReadRomData.isEmpty() && (!lastReadEepromData.isEmpty() || !lastReadConfigData.isEmpty())) {
            options.add(getString(R.string.exportar_dump_completo));
            options.add(getString(R.string.exportar_dump_completo_bin));
        }

        String[] items = options.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.exportar_memoria))
                .setItems(items, (dialog, which) -> {
                    String selected = items[which];
                    int coreBits = 14;
                    if (currentChip != null) {
                        try {
                            coreBits = currentChip.getTipoDeNucleoBit();
                        } catch (com.diamon.excepciones.ChipConfigurationException e) {
                        }
                    }

                    if (selected.equals(getString(R.string.exportar_rom_hex))) {
                        byte[] romBytes = stringHexToByteArray(lastReadRomData);
                        if (romBytes != null && romBytes.length > 0) {
                            romBytes = HexExportManager.formatForHexExport(romBytes, coreBits, false);
                            hexExportManager.exportAsHex(romBytes, chipName + "_ROM");
                        }
                    } else if (selected.equals(getString(R.string.exportar_rom_bin))) {
                        hexExportManager.exportBinStringAsFile(lastReadRomData, chipName + "_ROM");

                    } else if (selected.equals(getString(R.string.exportar_eeprom_hex))) {
                        int eepromAddr = (coreBits == 16) ? 0xF000 : 0x4200;
                        byte[] eepromBytes = stringHexToByteArray(lastReadEepromData);
                        if (eepromBytes != null && eepromBytes.length > 0) {
                            eepromBytes = HexExportManager.formatForHexExport(eepromBytes, coreBits, true);
                            hexExportManager.exportAsHexWithAddress(eepromBytes, eepromAddr, chipName + "_EEPROM");
                        }
                    } else if (selected.equals(getString(R.string.exportar_eeprom_bin))) {
                        hexExportManager.exportBinStringAsFile(lastReadEepromData, chipName + "_EEPROM");

                    } else if (selected.equals(getString(R.string.exportar_config_hex))) {
                        byte[] rawConfig = stringHexToByteArray(lastReadConfigData);
                        ConfigSplitData configSplitData = splitRawConfigForHex(rawConfig, coreBits);
                        if (configSplitData != null) {
                            hexExportManager.exportConfigAsHexSplit(
                                    configSplitData.idBytes,
                                    configSplitData.idAddress,
                                    configSplitData.fuseBytes,
                                    configSplitData.fuseAddress,
                                    chipName + "_CONFIG");
                        }
                    } else if (selected.equals(getString(R.string.exportar_config_bin))) {
                        byte[] configBytes = stringHexToByteArray(lastReadConfigData);
                        if (configBytes != null && configBytes.length > 0) {
                            hexExportManager.exportAsBinary(configBytes, chipName + "_CONFIG");
                        }

                    } else if (selected.equals(getString(R.string.exportar_dump_completo))) {
                        byte[] romBytes = stringHexToByteArray(lastReadRomData);
                        byte[] eepromBytes = stringHexToByteArray(lastReadEepromData);
                        byte[] rawConfig = stringHexToByteArray(lastReadConfigData);

                        int eepromAddr = (coreBits == 16) ? 0xF000 : 0x4200;

                        romBytes = HexExportManager.formatForHexExport(romBytes, coreBits, false);
                        eepromBytes = HexExportManager.formatForHexExport(eepromBytes, coreBits, true);

                        ConfigSplitData configSplitData = splitRawConfigForHex(rawConfig, coreBits);
                        if (configSplitData != null) {
                            hexExportManager.exportFullDumpAsHexWithSplitConfig(
                                    romBytes, eepromBytes,
                                    configSplitData.idBytes,
                                    configSplitData.idAddress,
                                    configSplitData.fuseBytes,
                                    configSplitData.fuseAddress,
                                    eepromAddr, chipName + "_FULL");
                        } else {
                            // Fallback: sin datos de config, exportar solo ROM y EEPROM
                            hexExportManager.exportFullDumpAsHex(
                                    romBytes, eepromBytes, null,
                                    eepromAddr, 0x4000, chipName + "_FULL");
                        }
                    } else if (selected.equals(getString(R.string.exportar_dump_completo_bin))) {
                        byte[] romBytes = stringHexToByteArray(lastReadRomData);
                        byte[] eepromBytes = stringHexToByteArray(lastReadEepromData);
                        byte[] rawConfig = stringHexToByteArray(lastReadConfigData);

                        byte[] fullBinData = buildFullBinaryDump(romBytes, eepromBytes, rawConfig, coreBits);
                        if (fullBinData != null && fullBinData.length > 0) {
                            hexExportManager.exportAsBinary(fullBinData, chipName + "_FULL");
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancelar), null)
                .show();
    }

    private byte[] buildFullBinaryDump(byte[] romBytes, byte[] eepromBytes, byte[] rawConfig, int coreBits) {
        int romLen = (romBytes != null) ? romBytes.length : 0;
        int eepromLen = (eepromBytes != null) ? eepromBytes.length : 0;
        int configLen = 26;

        if (romLen == 0) {
            return new byte[0];
        }

        byte[] fullBin = new byte[romLen + configLen + eepromLen];

        System.arraycopy(romBytes, 0, fullBin, 0, romLen);

        if (rawConfig != null && rawConfig.length == 26) {
            System.arraycopy(rawConfig, 0, fullBin, romLen, 26);
        } else {
            byte[] dummyConfig = new byte[26];
            Arrays.fill(dummyConfig, (byte) 0xFF);
            if (currentChip != null) {
                try {
                    int chipId = currentChip.getIDPIC();
                    dummyConfig[0] = (byte) (chipId & 0xFF);
                    dummyConfig[1] = (byte) ((chipId >> 8) & 0xFF);
                    int[] fuseBlank = currentChip.getFuseBlank();
                    for (int i = 0; i < Math.min(fuseBlank.length, 7); i++) {
                        int fb = fuseBlank[i];
                        dummyConfig[10 + i * 2] = (byte) (fb & 0xFF);
                        dummyConfig[11 + i * 2] = (byte) ((fb >> 8) & 0xFF);
                    }
                } catch (Exception ignored) {}
            }
            System.arraycopy(dummyConfig, 0, fullBin, romLen, 26);
        }

        if (eepromBytes != null && eepromLen > 0) {
            System.arraycopy(eepromBytes, 0, fullBin, romLen + configLen, eepromLen);
        }

        return fullBin;
    }

    private byte[] stringHexToByteArray(String s) {
        if (s == null || s.isEmpty())
            return new byte[0];
        try {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                        + Character.digit(s.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private ConfigSplitData splitRawConfigForHex(byte[] rawConfig, int coreBits) {
        if (rawConfig == null || rawConfig.length == 0) {
            return null;
        }

        int idLen = (coreBits == 16) ? 8 : 4;
        int idStart = 2; // read_config devuelve primero chip_id (2 bytes).
        int fuseCount = 1;
        if (currentChip != null) {
            try {
                fuseCount = Math.max(1, currentChip.getFuseBlank().length);
            } catch (Exception ignored) {
            }
        }

        int fuseByteLen = fuseCount * 2;
        int fuseStart = 10; // 2 bytes chip_id + 8 bytes user ID.
        if (rawConfig.length < (idStart + idLen) || rawConfig.length <= fuseStart) {
            return null;
        }

        byte[] idBytes = Arrays.copyOfRange(rawConfig, idStart, Math.min(idStart + idLen, rawConfig.length));
        byte[] fuseBytes = Arrays.copyOfRange(rawConfig, fuseStart,
                Math.min(fuseStart + fuseByteLen, rawConfig.length));

        idBytes = HexExportManager.formatForHexExport(idBytes, coreBits, false);
        fuseBytes = HexExportManager.formatForHexExport(fuseBytes, coreBits, false);

        int idAddr = (coreBits == 16) ? 0x200000 : 0x4000;
        int fuseAddr = (coreBits == 16) ? 0x300000 : 0x400E;

        return new ConfigSplitData(idBytes, idAddr, fuseBytes, fuseAddr);
    }

    private static class ConfigSplitData {
        final byte[] idBytes;
        final int idAddress;
        final byte[] fuseBytes;
        final int fuseAddress;

        ConfigSplitData(byte[] idBytes, int idAddress, byte[] fuseBytes, int fuseAddress) {
            this.idBytes = idBytes;
            this.idAddress = idAddress;
            this.fuseBytes = fuseBytes;
            this.fuseAddress = fuseAddress;
        }
    }

    /**
     * MODIFICADO: Muestra la información completa del chip en un formato de tabla profesional.
     */
    private void showChipInfoJson() {
        if (currentChip == null) {
            appendLog("⚠ " + getString(R.string.selecciona_chip_primero));
            return;
        }

        // Crear contenedor principal con scroll vertical
        ScrollView verticalScrollView = new ScrollView(this);
        verticalScrollView.setBackgroundColor(Color.parseColor("#1A1A1A")); // Fondo oscuro premium
        verticalScrollView.setPadding(30, 30, 30, 30);

        // Crear scroll horizontal para evitar que las tablas anchas se corten
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // Crear tabla
        TableLayout tableLayout = new TableLayout(this);
        tableLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        // No estiramos columnas para que el scroll horizontal tenga sentido
        tableLayout.setStretchAllColumns(false);

        // Estilo de celdas
        int paddingSide = 25;
        int paddingTopBottom = 12;
        int textColorLabel = Color.parseColor("#A0A0A0"); // Gris medio para etiquetas
        int textColorValue = Color.WHITE;
        int textColorHeader = Color.parseColor("#E91E63"); // Rosa vibrante para secciones
        float textSize = 14f;
        float textSizeHeader = 16f;

        Map<String, Object> allData = currentChip.toDict();

        // Helper local compatible con minSdk 23 (sin java.util.function.*)
        class ChipInfoTableHelper {
            void addRow(String labelStr, String valueStr) {
                if (valueStr == null || valueStr.isEmpty() || valueStr.equalsIgnoreCase("null")) {
                    valueStr = getString(R.string.not_available);
                }

                TableRow row = new TableRow(MainActivity.this);
                row.setPadding(0, 4, 0, 4);
                row.setGravity(Gravity.CENTER_VERTICAL);

                TextView label = new TextView(MainActivity.this);
                label.setText(labelStr);
                label.setTextColor(textColorLabel);
                label.setTextSize(textSize);
                label.setPadding(paddingSide, paddingTopBottom, paddingSide, paddingTopBottom);
                label.setTypeface(null, Typeface.BOLD);

                TextView value = new TextView(MainActivity.this);
                value.setText(valueStr);
                value.setTextColor(textColorValue);
                value.setTextSize(textSize);
                value.setPadding(paddingSide, paddingTopBottom, paddingSide, paddingTopBottom);
                value.setSingleLine(false);
                value.setGravity(Gravity.START);

                row.addView(label);
                row.addView(value);

                View divider = new View(MainActivity.this);
                divider.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.parseColor("#333333"));

                tableLayout.addView(row);
                tableLayout.addView(divider);
            }

            void addHeader(String title) {
                TextView header = new TextView(MainActivity.this);
                header.setText(title);
                header.setTextColor(textColorHeader);
                header.setTextSize(textSizeHeader);
                header.setTypeface(null, Typeface.BOLD);
                header.setPadding(paddingSide, 50, paddingSide, 15);
                tableLayout.addView(header);

                View divider = new View(MainActivity.this);
                divider.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 3));
                divider.setBackgroundColor(textColorHeader);
                tableLayout.addView(divider);
            }

            boolean isTrue(Object val) {
                if (val == null) {
                    return false;
                }
                String s = String.valueOf(val).trim().toLowerCase(java.util.Locale.ROOT);
                return s.equals("y") || s.equals("1") || s.equals("true");
            }

            String formatFuseBlank(Object val) {
                if (val instanceof String[]) {
                    return String.join(" ", (String[]) val);
                }
                return String.valueOf(val);
            }
        }

        ChipInfoTableHelper helper = new ChipInfoTableHelper();

        // --- SECCIÓN: IDENTIFICACIÓN ---
        helper.addHeader(getString(R.string.seccion_identificacion));
        helper.addRow(getString(R.string.modelo) + ":", String.valueOf(allData.get("chip_name")));
        Object chipId = allData.get("chip_id");
        String hexId = getString(R.string.not_available);
        if (chipId != null) {
            try {
                long idVal = (chipId instanceof Number) ? ((Number)chipId).longValue() : Long.parseLong(String.valueOf(chipId), 16);
                hexId = "0x" + Long.toHexString(idVal).toUpperCase();
            } catch (Exception e) {
                hexId = String.valueOf(chipId);
            }
        }
        helper.addRow(getString(R.string.chip_id_label), hexId);
        helper.addRow(getString(R.string.include_file_label), String.valueOf(allData.get("include")));

        // --- SECCIÓN: MEMORIA ---
        helper.addHeader(getString(R.string.seccion_memoria));
        try {
            int words = currentChip.getTamanoROM();
            helper.addRow(getString(R.string.rom_capacity_label), getString(R.string.rom_words_bytes, words, words * 2));
            helper.addRow(getString(R.string.eeprom_capacity_label), currentChip.isTamanoValidoDeEEPROM() ? 
                    getString(R.string.eeprom_bytes, currentChip.getTamanoEEPROM()) : getString(R.string.not_available));
            helper.addRow(getString(R.string.fuse_blank_value_label), helper.formatFuseBlank(allData.get("fuse_blank")));
        } catch (Exception e) {
            helper.addRow(getString(R.string.memory_error_label), e.getMessage());
        }
        helper.addRow(getString(R.string.flash_technology_label), helper.isTrue(allData.get("flash_chip")) ? getString(R.string.yes) : getString(R.string.no_label));

        // --- SECCIÓN: HARDWARE ---
        helper.addHeader(getString(R.string.seccion_hardware));
        helper.addRow(getString(R.string.core_architecture_label), getString(R.string.core_bits_val, (int)allData.get("core_bits")));
        helper.addRow(getString(R.string.core_type_id_label), String.valueOf(allData.get("core_type")));
        helper.addRow(getString(R.string.pines_totales_label), getString(R.string.pines_val, currentChip.getNumeroDePines()));
        helper.addRow(getString(R.string.pin_1_location_label), String.valueOf(allData.get("pin1_location")));
        helper.addRow(getString(R.string.socket_image_id_label), String.valueOf(allData.get("socket_image")));
        helper.addRow(getString(R.string.icsp_required_label), helper.isTrue(allData.get("icsp_only")) ? getString(R.string.yes) : getString(R.string.no_label));

        // --- SECCIÓN: PROGRAMACIÓN ---
        helper.addHeader(getString(R.string.seccion_programacion));
        helper.addRow(getString(R.string.erase_algorithm_label), String.valueOf(allData.get("erase_mode")));
        helper.addRow(getString(R.string.power_sequence_label), String.valueOf(allData.get("power_sequence")));
        helper.addRow(getString(R.string.prog_delay_label), String.valueOf(allData.get("program_delay")));
        helper.addRow(getString(R.string.prog_retries_label), String.valueOf(allData.get("program_tries")));
        helper.addRow(getString(R.string.over_prog_factor_label), String.valueOf(allData.get("over_program")));
        helper.addRow(getString(R.string.cp_warn_label), helper.isTrue(allData.get("cp_warn")) ? getString(R.string.yes) : getString(R.string.no_label));

        // --- SECCIÓN: FUSES ---
        Object fusesObj = allData.get("fuses");
        Map<?, ?> fuses = fusesObj instanceof Map ? (Map<?, ?>) fusesObj : null;
        if (fuses != null && !fuses.isEmpty()) {
            helper.addHeader(getString(R.string.seccion_fuses));
            for (Map.Entry<?, ?> entry : fuses.entrySet()) {
                String fuseName = String.valueOf(entry.getKey());
                String options = getString(R.string.not_available);
                if (entry.getValue() instanceof List) {
                    List<?> list = (List<?>) entry.getValue();
                    List<String> stringList = new ArrayList<>();
                    for (Object item : list) {
                        stringList.add(String.valueOf(item));
                    }
                    options = String.join(" | ", stringList);
                }
                helper.addRow(fuseName, options);
            }
        }

        horizontalScrollView.addView(tableLayout);
        verticalScrollView.addView(horizontalScrollView);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chip_info_titulo))
                .setView(verticalScrollView)
                .setPositiveButton(getString(R.string.aceptar), null)
                .show();
    }

    private void openTutorialGputils() {
        try {
            Intent intent = new Intent(MainActivity.this, TutorialGputilsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            appendLog("❌ " + getString(R.string.error_abriendo_tutorial) + ": " + e.getMessage());
        }
    }

    private void openTutorialSdcc() {
        try {
            Intent intent = new Intent(MainActivity.this, com.diamon.tutorial.TutorialSdccActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            appendLog("❌ " + getString(R.string.error_abriendo_tutorial) + ": " + e.getMessage());
        }
    }

    private void showProgrammerModelDialog() {
        if (usbManager != null && usbManager.isConnected() && usbManager.getProtocolo() != null) {
            String model = usbManager.getProtocolo().obtenerVersionOModeloDelProgramador();
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.modelo_del_programador))
                    .setMessage(model)
                    .setPositiveButton(getString(R.string.aceptar), null)
                    .show();
        } else {
            Toast.makeText(this, getString(R.string.diag_programmer_not_connected), Toast.LENGTH_SHORT).show();
            appendLog(getString(R.string.log_connect_programmer_first));
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.modelo_del_programador))
                    .setMessage(getString(R.string.diag_programmer_not_connected))
                    .setPositiveButton(getString(R.string.aceptar), null)
                    .show();
        }
    }

    private void showProtocolDialog() {
        String currentProto = usbManager.getTipoProtocolo().getNombre();

        String[] protocolos = TipoProtocolo.getNombres();
        int currentIndex = 0;
        for (int i = 0; i < protocolos.length; i++) {
            if (protocolos[i].equals(currentProto)) {
                currentIndex = i;
                break;
            }
        }

        final int[] selectedIndex = { currentIndex };
        String title = getString(R.string.seleccionar_protocolo);

        float density = getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);
        int padItemH = Math.round(14 * density);
        int padItemV = Math.round(12 * density);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setBackgroundColor(0xFF181824);

        android.widget.RadioGroup radioGroup = new android.widget.RadioGroup(this);
        radioGroup.setOrientation(android.widget.RadioGroup.VERTICAL);
        radioGroup.setPadding(pad, pad, pad, pad);

        // ColorStateList de alto contraste para los RadioButtons:
        // Seleccionado: Verde neón vibrante (#00E676)
        // No seleccionado: Gris plateado claro (#B0BEC5), claramente distinguible del fondo oscuro (#181824 / #222234)
        ColorStateList radioColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        0xFF00E676,
                        0xFFB0BEC5
                }
        );

        for (int i = 0; i < protocolos.length; i++) {
            androidx.appcompat.widget.AppCompatRadioButton rb = new androidx.appcompat.widget.AppCompatRadioButton(this);
            rb.setId(View.generateViewId());

            String proto = protocolos[i];
            rb.setText(proto);
            rb.setTextColor(0xFFFFFFFF);
            rb.setTextSize(15f);
            rb.setTypeface(null, Typeface.BOLD);
            rb.setSupportButtonTintList(radioColorStateList);
            rb.setPadding(padItemH, padItemV, padItemH, padItemV);
            rb.setMinimumHeight(Math.round(48 * density));

            android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
            cardBg.setColor(0xFF222234);
            cardBg.setCornerRadius(8 * density);
            cardBg.setStroke(Math.round(1.5f * density), (i == currentIndex) ? 0xFF00E676 : 0xFF35354A);
            rb.setBackground(cardBg);

            android.widget.RadioGroup.LayoutParams lp = new android.widget.RadioGroup.LayoutParams(
                    android.widget.RadioGroup.LayoutParams.MATCH_PARENT,
                    android.widget.RadioGroup.LayoutParams.WRAP_CONTENT
            );
            if (i < protocolos.length - 1) {
                lp.bottomMargin = Math.round(8 * density);
            }
            rb.setLayoutParams(lp);

            if (i == currentIndex) {
                rb.setChecked(true);
            }

            radioGroup.addView(rb);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                boolean isChecked = (child.getId() == checkedId);
                if (isChecked) {
                    selectedIndex[0] = i;
                }
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(0xFF222234);
                bg.setCornerRadius(8 * density);
                bg.setStroke(Math.round(1.5f * density), isChecked ? 0xFF00E676 : 0xFF35354A);
                child.setBackground(bg);
            }
        });

        scrollView.addView(radioGroup);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                    TipoProtocolo selected = TipoProtocolo.values()[selectedIndex[0]];
                    usbManager.setTipoProtocolo(selected);
                    appendLog("ℹ " + getString(R.string.protocolo) + ": " + selected.getNombre());
                })
                .setNegativeButton(getString(R.string.cancelar), null)
                .show();
    }

    private void showAboutLicensesDialog() {
        float density = getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setBackgroundColor(0xFF181824);

        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setPadding(pad, pad, pad, pad);
        textView.setTextSize(13);
        textView.setTextColor(0xFFE0E0E0);
        textView.setLinkTextColor(0xFF00B0FF);
        textView.setLineSpacing(0, 1.25f);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

        String htmlText = "<h3><b><font color=\"#00E676\">PIC k150 Programming</font></b></h3>" +
                "<p><b>" + getString(R.string.desarrollador) + ":</b> Danielk10</p>" +
                "<p><b>" + getString(R.string.repositorio) + ":</b> <a href=\"https://github.com/Danielk10/PIC-k150-Programing\">GitHub - Danielk10/PIC-k150-Programing</a></p>" +
                "<hr>" +
                "<p>" + getString(R.string.about_description) + "</p>" +
                "<hr>" +
                "<h4><b><font color=\"#00B0FF\">" + getString(R.string.about_features) + "</font></b></h4>" +
                "<ul>" +
                "  <li>" + getString(R.string.about_feature_1) + "</li>" +
                "  <li>" + getString(R.string.about_feature_2) + "</li>" +
                "  <li>" + getString(R.string.about_feature_3) + "</li>" +
                "  <li>" + getString(R.string.about_feature_4) + "</li>" +
                "</ul>" +
                "<hr>" +
                "<h4><b><font color=\"#FFB300\">" + getString(R.string.about_more_apps_title) + "</font></b></h4>" +
                "<ul>" +
                "  <li>💻 <b>" + getString(R.string.about_app_ptc) + ":</b> <a href=\"https://play.google.com/store/apps/details?id=com.diamon.ptc\">" + getString(R.string.about_view_on_play_store) + "</a></li>" +
                "  <li>💾 <b>" + getString(R.string.about_app_curso) + ":</b> <a href=\"https://play.google.com/store/apps/details?id=com.diamon.curso\">" + getString(R.string.about_view_on_play_store) + "</a></li>" +
                "  <li>🔌 <b>" + getString(R.string.about_app_mini) + ":</b> <a href=\"https://play.google.com/store/apps/details?id=com.diamon.mini\">" + getString(R.string.about_view_on_play_store) + "</a></li>" +
                "</ul>" +
                "<hr>" +
                "<h4><b><font color=\"#00B0FF\">" + getString(R.string.licencia_de_aplicacion) + "</font></b></h4>" +
                "<p>GNU General Public License v3.0 - <a href=\"https://www.gnu.org/licenses/gpl-3.0.html\">GPL-3.0</a></p>" +
                "<hr>" +
                "<h4><b><font color=\"#00B0FF\">" + getString(R.string.licencias_de_terceros) + "</font></b></h4>" +
                "<ul>" +
                "  <li><b>usb-serial-for-android:</b> MIT License - <a href=\"https://github.com/mik3y/usb-serial-for-android\">mik3y/usb-serial-for-android</a></li>" +
                "  <li><b>picpro:</b> LGPL-3.0 - <a href=\"https://github.com/Salamek/picpro\">Salamek/picpro</a></li>" +
                "  <li><b>picprogrammer:</b> LGPL-2.0 - <a href=\"https://github.com/almost/picprogrammer\">almost/picprogrammer</a></li>" +
                "  <li><b>Termux:</b> GPL-3.0 - <a href=\"https://github.com/termux/termux-app\">termux/termux-app</a></li>" +
                "  <li><b>KITSRUS P018:</b> <a href=\"https://www.kitsrus.com/pic.html\">kitsrus.com</a></li>" +
                "  <li><b>Android Code Studio:</b> <a href=\"https://github.com/AndroidCSOfficial/android-code-studio\">AndroidCSOfficial/android-code-studio</a></li>" +
                "</ul>" +
                "<hr>" +
                "<h4><b><font color=\"#00B0FF\">" + getString(R.string.about_acknowledgements_title) + "</font></b></h4>" +
                "<p>" + getString(R.string.about_acknowledgements_desc) + "</p>";

        textView.setText(androidx.core.text.HtmlCompat.fromHtml(htmlText, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));
        scrollView.addView(textView);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.acerca_de_licencias))
                .setView(scrollView)
                .setPositiveButton(getString(R.string.aceptar), null)
                .show();
    }

    @Override
    protected void onPause() {
        publicidad.pausarBanner();
        super.onPause();

        // NUEVO: Liberar recursos gráficos pesados al pausar para evitar ANR en otras actividades
        if (socketDrawingManager != null) {
            chipSocketImageView.setImageBitmap(null);
            socketDrawingManager.recycleTextura();
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (proPassManager != null && proPassManager.isProActive(billingManager)) {
            if (publicidad != null) {
                publicidad.ocultarBanner();
            }
        } else {
            if (publicidad != null) {
                publicidad.resumenBanner();
            }
        }
        actualizarEstadoProUI();

        // NUEVO: Recrear la imagen del chip al volver
        if (currentChip != null) {
            updateChipImage(currentChip);
        }

        if (wakeLock != null) {
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    @SuppressLint("Wakelock")
    @Override
    protected void onDestroy() {
        try {
            if (billingManager != null) {
                billingManager.destroy();
            }

            if (publicidad != null) {
                publicidad.destruirPublicidad();
            }

            if (usbManager != null) {
                usbManager.release();
            }

            if (dialogManager != null) {
                dialogManager.dismiss();
            }

            if (memoryDisplayManager != null) {
                memoryDisplayManager.dismissAllPopups();
            }

            // NUEVO: Limpiar popup de fusibles
            if (fuseConfigPopup != null) {
                fuseConfigPopup.dismiss();
            }

            if (socketDrawingManager != null) {
                socketDrawingManager.recycleTextura();
            }

            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }

        } catch (Exception e) {
            // Manejo de excepciones
        }

        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {

            pantallaCompleta.ocultarBotonesVirtuales();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        pantallaCompleta.ocultarBotonesVirtuales();

        return super.onKeyUp(keyCode, event);
    }

    public GestorPublicidad getPublicidad() {
        return publicidad;
    }
}
