package com.diamon.pic;

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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ScrollView;
import android.graphics.Typeface;
import java.util.LinkedHashMap;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.managers.ChipSelectionManager;
import com.diamon.managers.FileManager;
import com.diamon.managers.FuseConfigPopup;
import com.diamon.managers.HexExportManager;
import com.diamon.managers.MemoryDisplayManager;
import com.diamon.managers.PicProgrammingManager;
import com.diamon.managers.ProgrammingDialogManager;
import com.diamon.managers.UsbConnectionManager;
import com.diamon.protocolo.TipoProtocolo;
import com.diamon.politicas.Politicas;
import com.diamon.publicidad.MostrarPublicidad;
import com.diamon.tutorial.TutorialGputilsActivity;
import com.diamon.utilidades.PantallaCompleta;
import com.diamon.utilidades.Recurso;

import com.diamon.graficos.Graficos2D;
import com.diamon.graficos.Textura2D;
import com.diamon.nucleo.Graficos;
import com.diamon.nucleo.Textura;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.ArrayList;
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

    private TextView connectionStatusTextView;
    private TextView processStatusTextView;
    private TextView chipInfoTextView;
    private TextView fuseStatusTextView; // NUEVO
    private LinearLayout romDataContainer;
    private LinearLayout eepromDataContainer;
    private Spinner chipSpinner;
    private android.widget.ImageView chipSocketImageView; // NUEVO
    private androidx.appcompat.widget.SwitchCompat swModeICSP;

    private android.widget.Button btnSelectHex;
    private android.widget.Button btnProgramarPic;
    private android.widget.Button btnLeerMemoriaDeLPic;
    private android.widget.Button btnVerificarMemoriaDelPic;
    private android.widget.Button btnBorrarMemoriaDeLPic;
    private android.widget.Button btnDetectarPic;
    private android.widget.Button btnConfigureFuses; // NUEVO
    private android.widget.Button btnBlankCheck; // Verificar Borrado

    private UsbConnectionManager usbManager;
    private PicProgrammingManager programmingManager;
    private FileManager fileManager;
    private ChipSelectionManager chipSelectionManager;
    private MemoryDisplayManager memoryDisplayManager;
    private ProgrammingDialogManager dialogManager;
    private MostrarPublicidad publicidad;
    private Recurso recurso;
    private PowerManager.WakeLock wakeLock;

    private FuseConfigPopup fuseConfigPopup; // NUEVO
    private HexExportManager hexExportManager; // NUEVO: Export manager

    private String firmware = "";
    private String lastReadRomData = ""; // Últimos datos ROM leídos
    private String lastReadEepromData = ""; // Últimos datos EEPROM leídos
    private String lastReadConfigData = ""; // Últimos datos Config leídos
    private ChipPic currentChip;

    // NUEVAS VARIABLES PARA FUSES
    private boolean fusesConfigured = false;
    private List<Integer> configuredFuses = new ArrayList<>();
    private byte[] configuredID = new byte[] { 0 };
    private Map<String, String> lastFuseConfiguration = null;
    private DatosPicProcesados datosPicProcesados = null;

    private PantallaCompleta pantallaCompleta;

    @SuppressLint({ "InvalidWakeLockTag", "UnspecifiedRegisterReceiverFlag" })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Habilitar Edge-to-Edge ANTES de setContentView (requerido por Android 15)
        pantallaCompleta = new PantallaCompleta(this);
        pantallaCompleta.habilitarEdgeToEdge();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        recurso = new Recurso(this);
        publicidad = new MostrarPublicidad(this);
    }

    private void findViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        processStatusTextView = findViewById(R.id.processStatusTextView);
        chipInfoTextView = findViewById(R.id.chipInfoTextView);
        chipSocketImageView = findViewById(R.id.chipSocketImageView); // NUEVO
        fuseStatusTextView = findViewById(R.id.fuseStatusTextView); // NUEVO
        chipSpinner = findViewById(R.id.chipSpinner);

        btnSelectHex = findViewById(R.id.btnSelectHex);
        btnProgramarPic = findViewById(R.id.btnProgramarPic);
        btnLeerMemoriaDeLPic = findViewById(R.id.btnLeerMemoriaDeLPic);
        btnVerificarMemoriaDelPic = findViewById(R.id.btnVerificarMemoriaDelPic);
        btnBorrarMemoriaDeLPic = findViewById(R.id.btnBorrarMemoriaDeLPic);
        btnDetectarPic = findViewById(R.id.btnDetectarPic);
        btnConfigureFuses = findViewById(R.id.btnConfigureFuses); // NUEVO
        btnBlankCheck = findViewById(R.id.btnBlankCheck); // Verificar Borrado

        romDataContainer = findViewById(R.id.romDataContainer);
        eepromDataContainer = findViewById(R.id.eepromDataContainer);
    }

    private void setupBanner() {
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
                chipInfoTextView.setText(chipSelectionManager.getSelectedChipInfoColored());

                updateSwitchColors();
                updateICSPSwitchState();
                updateChipImage(chip);
                clearFuseConfiguration();

                if (usbManager.isConnected()) {
                    try {
                        usbManager.getProtocolo().iniciarVariablesDeProgramacion(chip);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, getString(R.string.error_inicializando_chip),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onChipSelectionError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
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
        dialogManager = new ProgrammingDialogManager(this);

        // NUEVO: Inicializar export manager
        hexExportManager = new HexExportManager(this);
        hexExportManager.initialize();
        hexExportManager.setExportListener(new HexExportManager.ExportListener() {
            @Override
            public void onExportSuccess(String fileName) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        getString(R.string.exportacion_exitosa) + ": " + fileName,
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onExportError(String errorMessage) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        getString(R.string.error_exportando) + ": " + errorMessage,
                        Toast.LENGTH_LONG).show());
            }
        });

        // NUEVO: Diferir la precarga para evitar ANR durante la inicialización
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (publicidad != null) {
                publicidad.precargarNativeAd(com.diamon.publicidad.MostrarPublicidad.KEY_NATIVE_MEMORY);
                publicidad.precargarNativeAd(com.diamon.publicidad.MostrarPublicidad.KEY_NATIVE_PROGRAMMING);
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
                        // Se usa literal para evitar errores de compilación en otros idiomas hasta que
                        // se traduzca R.string.fusibles_aplicados_correctamente
                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.fusibles_aplicados_correctamente),
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onFusesCancelled() {
                        // No hacer nada
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
        // setupChipSelectionListeners(); // ELIMINADO: Se maneja en initializeManagers
        setupFileManagerListeners();
        setupButtonListeners();
        setupPersistentLayoutListener(); // Registramos el vigilante de tamaño desde el inicio
    }

    // IMPLEMENTACIÓN DE UsbConnectionListener
    @Override
    public void onConnected() {
        Analytics.trackEvent("USB: Connected");
        runOnUiThread(() -> {
            connectionStatusTextView.setTextColor(Color.GREEN);
            connectionStatusTextView.setText(getString(R.string.conectado));
            programmingManager.setProtocolo(usbManager.getProtocolo());
            Toast.makeText(this, getString(R.string.conectado_al_programador), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDisconnected() {
        Analytics.trackEvent("USB: Disconnected");
        runOnUiThread(() -> {
            connectionStatusTextView.setTextColor(Color.RED);
            connectionStatusTextView.setText(getString(R.string.desconectado));
        });
    }

    @Override
    public void onConnectionError(String errorMessage) {
        Analytics.trackEvent("USB: Error", crearMapaAnalitica("Message", errorMessage));
        runOnUiThread(() -> {
            connectionStatusTextView.setTextColor(Color.RED);
            connectionStatusTextView.setText(getString(R.string.desconectado));
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    // IMPLEMENTACIÓN DE ProgrammingListener
    @Override
    public void onProgrammingStarted() {
        Analytics.trackEvent("Prog: Started");
        runOnUiThread(() -> processStatusTextView.setText(getString(R.string.iniciando_programacion)));
    }

    @Override
    public void onProgrammingProgress(String message, int progress) {
        runOnUiThread(() -> processStatusTextView.setText(message + " (" + progress + "%)"));
    }

    @Override
    public void onProgrammingCompleted(boolean success) {
        Analytics.trackEvent("Prog: Completed", crearMapaAnalitica("Success", String.valueOf(success)));
        runOnUiThread(() -> {
            if (success) {
                processStatusTextView.setText(getString(R.string.pic_programado_exitosamente));
            } else {
                processStatusTextView.setText(getString(R.string.error_programando_pic));
            }
        });
    }

    @Override
    public void onProgrammingError(String errorMessage) {
        Analytics.trackEvent("Prog: Error", crearMapaAnalitica("Message", errorMessage));
        runOnUiThread(() -> {
            processStatusTextView.setText("Error: " + errorMessage);
            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
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

                    } catch (ChipConfigurationException e) {
                        swModeICSP.setChecked(false);
                        Toast.makeText(
                                MainActivity.this,
                                "Error al cambiar modo: " + e.getMessage(),
                                Toast.LENGTH_SHORT)
                                .show();
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

        if (isIcspOnly || isIcspActive || numPines == 0) {
            chipSocketImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            dibujarICSP();
            return;
        }

        // Para ZIF Sockets usamos el mismo ratio que ICSP y mantenemos el dibujo nitido
        chipSocketImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        dibujarSocketZIF(chip);
    }

    private void dibujarSocketZIF(ChipPic chip) {
        int width = chipSocketImageView.getWidth();
        int height = chipSocketImageView.getHeight();

        if (width <= 0 || height <= 0)
            return;

        Textura textura = new Textura2D(width, height, Graficos.FormatoTextura.ARGB8888);
        Graficos g = new Graficos2D(textura);

        // Colores originales del vector
        int colorTeal = Color.parseColor("#005F5F");
        int colorBlueFrame = Color.parseColor("#1565C0");
        int colorInnerRecess = Color.parseColor("#0D47A1");
        int colorPinGreen = Color.parseColor("#4CAF50");
        int colorPinGold = Color.parseColor("#FFD700");

        // Fondo Teal
        g.limpiar(colorTeal);

        float scaleX = width / 300f;
        float scaleY = height / 360f;

        // 1. Marco del Socket (BLUE)
        g.dibujarRectangulo(40 * scaleX, 10 * scaleY, 220 * scaleX, 340 * scaleY, colorBlueFrame);
        Paint lapiz = g.getLapiz();
        lapiz.setStyle(Paint.Style.STROKE);
        lapiz.setStrokeWidth(1 * scaleX);
        lapiz.setColor(Color.WHITE);
        g.getCanvas().drawRect(40 * scaleX, 10 * scaleY, 260 * scaleX, 350 * scaleY, lapiz);

        // 2. Hueco Central (Inner Recess)
        g.dibujarRectangulo(90 * scaleX, 20 * scaleY, 120 * scaleX, 320 * scaleY, colorInnerRecess);

        // 3. Pines (Grid de 20x2)
        lapiz.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 20; i++) {
            float rowY = (30 + i * 16) * scaleY;

            // Columna Izquierda
            g.dibujarRectangulo(50 * scaleX, rowY, 30 * scaleX, 10 * scaleY, colorPinGreen);
            g.dibujarRectangulo(70 * scaleX, rowY + 2 * scaleY, 6 * scaleX, 6 * scaleY, colorPinGold);

            // Columna Derecha
            g.dibujarRectangulo(220 * scaleX, rowY, 30 * scaleX, 10 * scaleY, colorPinGreen);
            g.dibujarRectangulo(224 * scaleX, rowY + 2 * scaleY, 6 * scaleX, 6 * scaleY, colorPinGold);
        }

        // 4. Indicadores (Numero y Flecha)
        String pinLocation = chip.getUbicacionPin1DelPic();
        int pinStartRow = 0;
        String indicatorText = "1";

        if ("socket pin 2".equalsIgnoreCase(pinLocation)) {
            pinStartRow = 1;
            indicatorText = "2";
        } else if ("socket pin 13".equalsIgnoreCase(pinLocation)) {
            pinStartRow = 12;
            indicatorText = "13";
        }

        float indicatorY = (30 + pinStartRow * 16) * scaleY; // Y coord of the first pin row
        float rowCenterY = indicatorY + (5 * scaleY); // Center of the 10-unit high pin

        // Flecha Blanca - Tamaño moderado para evitar solape
        lapiz.setColor(Color.WHITE);
        android.graphics.Path path = new android.graphics.Path();
        float arrowWidth = 12 * scaleX;
        float arrowHeight = 10 * scaleY;
        path.moveTo(22 * scaleX, rowCenterY - arrowHeight);
        path.lineTo(40 * scaleX, rowCenterY);
        path.lineTo(22 * scaleX, rowCenterY + arrowHeight);
        path.close();
        g.getCanvas().drawPath(path, lapiz);

        // Texto del indicador - Resuelto solapamiento
        lapiz.setTextSize(24 * scaleY);
        lapiz.setFakeBoldText(true);
        // Desplazado para evitar la flecha si es de dos dígitos
        g.dibujarTexto(indicatorText, 2 * scaleX, rowCenterY + 10 * scaleY, Color.WHITE);

        // Brillo sutil en el texto
        lapiz.setStyle(Paint.Style.STROKE);
        lapiz.setStrokeWidth(0.5f * scaleX);
        lapiz.setColor(Color.LTGRAY);
        g.getCanvas().drawText(indicatorText, 2 * scaleX, rowCenterY + 10 * scaleY, lapiz);
        lapiz.setStyle(Paint.Style.FILL);

        // 5. Cuerpo del Chip (Negro)
        int numPines = chip.getNumeroDePines();
        if (numPines > 0) {
            float left = 90 * scaleX;
            float right = 210 * scaleX;

            // Calculo exacto para que el chip coincida con los pines (2 unidades de margen
            // arriba/abajo)
            float top = (30 + pinStartRow * 16 - 2) * scaleY;
            int numFilas = numPines / 2;
            float chipHeight = ((numFilas - 1) * 16 + 10 + 4) * scaleY;
            float bottom = top + chipHeight;

            int colorChipBody = Color.parseColor("#151515");
            int colorChipBorder = Color.parseColor("#404040");
            int colorNotch = Color.parseColor("#8B4513"); // Marrón
            int colorLegs = Color.parseColor("#BDBDBD"); // Plateado metálico

            // A. PATAS del Chip (debajo del cuerpo)
            lapiz.setStyle(Paint.Style.FILL);
            for (int i = 0; i < numFilas; i++) {
                float legY = (30 + (pinStartRow + i) * 16 + 3) * scaleY;
                // Pata Izquierda
                g.dibujarRectangulo(80 * scaleX, legY, 12 * scaleX, 4 * scaleY, colorLegs);
                // Pata Derecha
                g.dibujarRectangulo(208 * scaleX, legY, 12 * scaleX, 4 * scaleY, colorLegs);
            }

            // B. Cuerpo
            g.dibujarRectangulo(left, top, right - left, bottom - top, colorChipBody);

            // C. Modelo del Chip (Texto grabado) - Horizontal y más grande
            String chipName = chip.getNombreDelPic();
            lapiz.setStyle(Paint.Style.FILL);
            lapiz.setColor(Color.parseColor("#D0D0D0")); // Gris claro láser
            lapiz.setTextSize(24 * scaleY); // Aumentado
            lapiz.setFakeBoldText(true);

            float chipCenterX = (left + right) / 2f;
            float chipCenterY = (top + bottom) / 2f;
            float textWidth = lapiz.measureText(chipName);

            // Dibujar centrado horizontalmente y verticalmente
            g.getCanvas().drawText(chipName, chipCenterX - textWidth / 2f, chipCenterY + (8 * scaleY), lapiz);

            // Borde del chip
            lapiz.setStyle(Paint.Style.STROKE);
            lapiz.setStrokeWidth(1.2f * scaleX);
            lapiz.setColor(colorChipBorder);
            g.getCanvas().drawRect(left, top, right, bottom, lapiz);

            // D. Muesca (Notch)
            lapiz.setStyle(Paint.Style.FILL);
            lapiz.setColor(colorNotch);
            float notchWidth = 40 * scaleX;
            float notchHeight = 18 * scaleY;
            g.getCanvas().drawArc(
                    (300 / 2f - notchWidth / 2f) * scaleX,
                    top - notchHeight / 2f,
                    (300 / 2f + notchWidth / 2f) * scaleX,
                    top + notchHeight / 2f,
                    0, 180, true, lapiz);

            // Sombra interna notch
            lapiz.setStyle(Paint.Style.STROKE);
            lapiz.setStrokeWidth(0.8f * scaleX);
            lapiz.setColor(Color.BLACK);
            g.getCanvas().drawArc(
                    (300 / 2f - notchWidth / 2f) * scaleX,
                    top - notchHeight / 2f,
                    (300 / 2f + notchWidth / 2f) * scaleX,
                    top + notchHeight / 2f,
                    0, 180, false, lapiz);
        }

        chipSocketImageView.setImageBitmap(textura.getBipmap());
    }

    private void dibujarICSP() {
        // Obtenemos el tamaño del ImageView
        int width = chipSocketImageView.getWidth();
        int height = chipSocketImageView.getHeight();

        // IMPORTANTE: Si las dimensiones son 0 (durante el inflado), NO dibujamos.
        // El listener registrado en setupListeners se encargara de llamar
        // a este metodo cuando el layout sea real.
        if (width <= 0 || height <= 0) {
            return;
        }

        Textura textura = new Textura2D(width, height, Graficos.FormatoTextura.ARGB8888);
        Graficos g = new Graficos2D(textura);

        // 1. Fondo PURPURA
        g.limpiar(Color.parseColor("#800080"));

        float scaleX = width / 200f;
        float scaleY = height / 240f;

        // 2. Conector Gris
        // Añadimos un margen vertical interno (padding) para evitar que el texto
        // superior (VPP1) se corte
        float vPadding = height * 0.05f; // 5% de margen
        float effectiveHeight = height - (2 * vPadding);

        float rectWidth = 35 * scaleX;
        float rectX = 10 * scaleX;
        float rectY = vPadding;
        float rectHeight = effectiveHeight;
        g.dibujarRectangulo(rectX, rectY, rectWidth, rectHeight, Color.parseColor("#808080"));

        // Borde del conector
        Paint lapiz = g.getLapiz();
        lapiz.setStyle(Paint.Style.STROKE);
        lapiz.setStrokeWidth(2f * scaleX);
        g.getCanvas().drawRect(rectX, rectY, rectX + rectWidth, rectY + rectHeight, lapiz);

        // 3. Cables y Etiquetas
        String[] labels = { "VPP1", "LOW", "DAT", "CLK", "VCC", "GND" };
        int[] colors = {
                Color.WHITE,
                Color.BLUE,
                Color.parseColor("#008000"), // Verde oscuro
                Color.RED,
                Color.BLACK,
                Color.YELLOW
        };

        lapiz.setStyle(Paint.Style.FILL);
        lapiz.setAntiAlias(true);
        lapiz.setFakeBoldText(true);

        float lineStartX = rectX + rectWidth;
        float lineEndX = width - (2 * scaleX);

        // Distribucion sobre el alto EFECTIVO (con padding)
        float lineSpacing = effectiveHeight / labels.length;

        for (int i = 0; i < labels.length; i++) {
            float currentY = rectY + (i * lineSpacing) + (lineSpacing / 2f);

            // Dibujar Cable
            lapiz.setColor(colors[i]);
            float strokeWidth = effectiveHeight / (labels.length * 4f); // Un poco mas fino para dar aire
            lapiz.setStrokeWidth(strokeWidth);
            g.dibujarLinea(lineStartX, currentY, lineEndX, currentY, colors[i]);

            // Dibujar Etiqueta CLARAMENTE arriba del cable (evita solapamiento y clipping)
            lapiz.setColor(Color.WHITE);
            lapiz.setTextSize(strokeWidth * 1.8f);
            float textX = lineStartX + (6 * scaleX);
            float textY = currentY - (strokeWidth / 1.2f); // Mas separacion
            g.dibujarTexto(labels[i], textX, textY, Color.WHITE);
        }

        chipSocketImageView.setImageBitmap(textura.getBipmap());
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
                        processStatusTextView.setText(
                                getString(R.string.archivo_cargado) + ": " + fileName);
                        enableOperationButtons(true);

                        // NUEVO: Habilitar botón de configuración de fusibles
                        enableFuseConfigButton(true);

                        // NUEVO: Resetear configuración de fusibles previa al cargar nuevo archivo
                        clearFuseConfiguration();

                        // NUEVO: Procesar datos del HEX
                        procesarDatosHex();

                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.archivo_hex_cargado_exitosamen),
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onFileLoadError(String errorMessage) {
                        processStatusTextView.setText(getString(R.string.error_cargando_archivo));
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupButtonListeners() {
        btnSelectHex.setOnClickListener(v -> fileManager.openFilePicker());
        btnConfigureFuses.setOnClickListener(v -> openFuseConfiguration()); // NUEVO
        btnProgramarPic.setOnClickListener(v -> executeProgram());
        btnLeerMemoriaDeLPic.setOnClickListener(v -> executeReadMemory());
        btnBorrarMemoriaDeLPic.setOnClickListener(v -> executeEraseMemory());
        btnVerificarMemoriaDelPic.setOnClickListener(v -> executeVerifyMemory());
        btnDetectarPic.setOnClickListener(v -> executeDetectChip());
        btnBlankCheck.setOnClickListener(v -> ejecutarBlankCheck());
    }

    /** NUEVO: Procesa los datos del archivo HEX */
    private void procesarDatosHex() {
        if (currentChip == null || firmware.isEmpty()) {
            return;
        }

        new Thread(
                () -> {
                    try {
                        datosPicProcesados = new DatosPicProcesados(firmware, currentChip);
                        datosPicProcesados.iniciarProcesamientoDeDatos();

                        runOnUiThread(
                                () -> {
                                    processStatusTextView.setText(
                                            getString(R.string.hex_procesado_correctamente));
                                });

                    } catch (Exception e) {
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(
                                            MainActivity.this,
                                            getString(R.string.error_procesando_hex)
                                                    + ": "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG)
                                            .show();
                                });
                    }
                })
                .start();
    }

    /** NUEVO: Abre el popup de configuración de fusibles */
    private void openFuseConfiguration() {
        if (currentChip == null) {
            Toast.makeText(this, getString(R.string.selecciona_chip_primero), Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentChip.getFusesMap() == null || currentChip.getFusesMap().isEmpty()) {
            Toast.makeText(this, getString(R.string.no_hay_fusibles_para_chip), Toast.LENGTH_SHORT)
                    .show();
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
        if (configured) {
            fuseStatusTextView.setText("✓ " + getString(R.string.fuses_configurados));
            fuseStatusTextView.setTextColor(Color.WHITE);
            fuseStatusTextView.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            fuseStatusTextView.setText(getString(R.string.fuses_no_configurados));
            fuseStatusTextView.setTextColor(Color.parseColor("#757575"));
            fuseStatusTextView.setBackgroundColor(Color.parseColor("#3A3A3A"));
        }
    }

    /** NUEVO: Habilita/deshabilita el botón de configuración de fusibles */
    private void enableFuseConfigButton(boolean enabled) {
        btnConfigureFuses.setEnabled(enabled);
        if (enabled) {
            btnConfigureFuses.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#9C27B0")));
            btnConfigureFuses.setTextColor(Color.WHITE);
        } else {
            btnConfigureFuses.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#555555")));
            btnConfigureFuses.setTextColor(Color.parseColor("#AAAAAA"));
        }
    }

    private void enableOperationButtons(boolean enabled) {
        android.widget.Button[] buttons = {
                btnProgramarPic,
                btnLeerMemoriaDeLPic,
                btnVerificarMemoriaDelPic,
                btnBorrarMemoriaDeLPic,
                btnDetectarPic,
                btnBlankCheck
        };

        for (android.widget.Button btn : buttons) {
            btn.setEnabled(enabled);
            if (enabled) {
                if (btn == btnProgramarPic) {
                    btn.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#FF6600")));
                } else if (btn == btnLeerMemoriaDeLPic || btn == btnVerificarMemoriaDelPic) {
                    btn.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#2196F3")));
                } else if (btn == btnBorrarMemoriaDeLPic) {
                    btn.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#F44336")));
                } else {
                    btn.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#9C27B0")));
                }
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#555555")));
                btn.setTextColor(Color.parseColor("#AAAAAA"));
            }
        }
    }

    /**
     * MODIFICADO: Ahora usa los fusibles configurados y permite programación
     * parcial
     */
    private void executeProgram() {
        if (currentChip == null || firmware.isEmpty()) {
            Toast.makeText(
                    this,
                    getString(R.string.seleccione_un_chip_y_cargue_un),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (datosPicProcesados == null) {
            Toast.makeText(this, getString(R.string.debe_procesar_hex_primero), Toast.LENGTH_SHORT).show();
            return;
        }

        // Determinar qué regiones tienen datos
        boolean hasRom = datosPicProcesados.tieneRomData();
        boolean hasEeprom = datosPicProcesados.tieneEepromData();
        boolean hasConfig = datosPicProcesados.tieneConfigData();

        java.util.List<String> options = new java.util.ArrayList<>();

        // Siempre ofrecemos Programar Todo (comportamiento clásico)
        options.add(getString(R.string.programar_todo));

        if (hasRom) {
            options.add(getString(R.string.programar_solo_rom));
        }
        if (hasEeprom) {
            options.add(getString(R.string.programar_solo_eeprom));
        }
        if (hasConfig || fusesConfigured) {
            options.add(getString(R.string.programar_solo_config));
        }

        // Si solo hay una opción (Programar Todo) o el usuario no configuró
        // fuses/regiones, lo hacemos directo
        if (options.size() <= 1) {
            doProgrammingFlow(getString(R.string.programar_todo));
            return;
        }

        String[] items = options.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.seleccionar_operacion))
                .setItems(items, (dialog, which) -> {
                    String selected = items[which];
                    doProgrammingFlow(selected);
                })
                .setNegativeButton(getString(R.string.cancelar), null)
                .show();
    }

    private void doProgrammingFlow(String operationType) {
        publicidad.ocultarBanner();

        final byte[] idToUse = fusesConfigured ? configuredID : new byte[] { 0 };
        final List<Integer> fusesToUse = fusesConfigured ? new ArrayList<>(configuredFuses) : new ArrayList<>();

        dialogManager.showProgrammingDialog(
                () -> {
                    new Thread(
                            () -> {
                                boolean success = false;

                                if (operationType.equals(getString(R.string.programar_solo_rom))) {
                                    success = programmingManager.programRomOnly(currentChip, firmware);
                                } else if (operationType.equals(getString(R.string.programar_solo_eeprom))) {
                                    success = programmingManager.programEepromOnly(currentChip, firmware);
                                } else if (operationType.equals(getString(R.string.programar_solo_config))) {
                                    success = programmingManager.programConfigOnly(currentChip, firmware, idToUse,
                                            fusesToUse);
                                } else {
                                    // Default: Programar todo
                                    success = programmingManager.programChip(currentChip, firmware, idToUse,
                                            fusesToUse);
                                }

                                final boolean finalSuccess = success;
                                runOnUiThread(
                                        () -> {
                                            dialogManager.updateProgrammingResult(finalSuccess);
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
            Toast.makeText(this, getString(R.string.seleccione_un_chip), Toast.LENGTH_SHORT).show();
            return;
        }

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

                                    processStatusTextView.setText(
                                            getString(R.string.memoria_leida_exitosamente));

                                    // Mostrar banner de nuevo
                                    publicidad.mostrarBanner();
                                } catch (ChipConfigurationException e) {
                                    Toast.makeText(
                                            MainActivity.this,
                                            getString(R.string.error_obteniendo_datos_del_chi)
                                                    + ": " + e.getMessage(),
                                            Toast.LENGTH_LONG)
                                            .show();
                                    publicidad.mostrarBanner();
                                }
                            });
                })
                .start();
    }

    private void executeEraseMemory() {
        new Thread(
                () -> {
                    boolean success = programmingManager.eraseMemory();
                    runOnUiThread(
                            () -> {
                                if (success) {
                                    processStatusTextView.setText(
                                            getString(
                                                    R.string.memoria_borrada_exitosamente));
                                } else {
                                    processStatusTextView.setText(
                                            getString(R.string.error_borrando_memoria));
                                }
                            });
                })
                .start();
    }

    private void executeVerifyMemory() {
        if (currentChip == null) {
            Toast.makeText(this, getString(R.string.seleccione_un_chip), Toast.LENGTH_SHORT).show();
            return;
        }

        processStatusTextView.setText(getString(R.string.verificando_memoria));

        new Thread(
                () -> {
                    try {
                        // Procesar el HEX cargado (firmware) a bytes si es necesario
                        if (datosPicProcesados == null || firmware == null || firmware.isEmpty()) {
                            runOnUiThread(() -> processStatusTextView.setText(
                                    getString(R.string.error_verificando_memoria) + ": No hay firmware válido"));
                            return;
                        }

                        // Usar VerificationManager para verificación real con bytes procesados
                        byte[] expectedRomBytes = datosPicProcesados.obtenerBytesHexROMPocesado();
                        byte[] expectedEepromBytes = datosPicProcesados.obtenerBytesHexEEPROMPocesado();

                        com.diamon.managers.VerificationManager.VerificationResult result = com.diamon.managers.VerificationManager
                                .verify(
                                        programmingManager.getProtocolo(),
                                        currentChip,
                                        expectedRomBytes,
                                        expectedEepromBytes);

                        runOnUiThread(
                                () -> {
                                    StringBuilder statusMsg = new StringBuilder();
                                    statusMsg.append(getString(R.string.verificacion_completa));
                                    statusMsg.append("\n");

                                    // ROM
                                    if (result.romVerified) {
                                        statusMsg.append(getString(R.string.verificacion_rom_ok));
                                    } else if (result.romMaybeLocked) {
                                        statusMsg.append(getString(R.string.verificacion_rom_fallo))
                                                .append(" — ")
                                                .append(getString(R.string.rom_posible_locked));
                                    } else {
                                        statusMsg.append(getString(R.string.verificacion_rom_fallo));
                                    }

                                    // Chip config info
                                    if (result.chipIdHex != null) {
                                        statusMsg.append("\nChip ID: ").append(result.chipIdHex);
                                    }

                                    // Fuses decodificados
                                    if (result.decodedFuses != null && !result.decodedFuses.isEmpty()) {
                                        statusMsg.append("\n").append(getString(R.string.fuses_decodificados))
                                                .append(":");
                                        for (java.util.Map.Entry<String, String> fuse : result.decodedFuses
                                                .entrySet()) {
                                            statusMsg.append("\n  ").append(fuse.getKey())
                                                    .append(" = ").append(fuse.getValue());
                                        }
                                    }

                                    processStatusTextView.setText(statusMsg.toString());
                                });

                    } catch (Exception e) {
                        runOnUiThread(
                                () -> processStatusTextView.setText(
                                        getString(R.string.error_verificando_memoria)
                                                + ": " + e.getMessage()));
                    }
                })
                .start();
    }

    private void executeDetectChip() {
        new Thread(
                () -> {
                    boolean detected = programmingManager.detectChipInSocket();
                    runOnUiThread(
                            () -> {
                                if (detected) {
                                    processStatusTextView.setText(
                                            getString(R.string.pic_detectado_en_socket));
                                } else {
                                    processStatusTextView.setText(
                                            getString(
                                                    R.string.no_se_detecto_pic_en_socket));
                                }
                            });
                })
                .start();
    }

    /** Ejecuta la verificación de borrado (Blank Check) */
    private void ejecutarBlankCheck() {
        if (currentChip == null) {
            Toast.makeText(this, getString(R.string.seleccione_un_chip), Toast.LENGTH_SHORT).show();
            return;
        }

        processStatusTextView.setText(getString(R.string.verificando_borrado) + "...");

        new Thread(() -> {
            try {
                PicProgrammingManager.ResultadoVerificacionBorrado resultado = programmingManager
                        .verificarBorradoCompleto(currentChip);

                if (resultado.error != null && !resultado.error.isEmpty()) {
                    runOnUiThread(() -> processStatusTextView
                            .setText(getString(R.string.error_verificando_borrado) + ": " + resultado.error));
                    return;
                }

                runOnUiThread(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(getString(R.string.resultado_verificacion_borrado)).append(":\n");
                    sb.append("ROM: ").append(
                            resultado.romEnBlanco ? getString(R.string.rom_ok_blank) : getString(R.string.rom_not_blank))
                            .append("\n");

                    if (currentChip.isTamanoValidoDeEEPROM()) {
                        sb.append("EEPROM: ")
                                .append(resultado.eepromEnBlanco ? getString(R.string.eeprom_ok_blank)
                                        : getString(R.string.eeprom_not_blank))
                                .append("\n");
                    }

                    sb.append(getString(R.string.protocolo)).append(": ").append(resultado.metodoUtilizado);
                    processStatusTextView.setText(sb.toString());

                    if (resultado.chipEnBlanco()) {
                        Toast.makeText(MainActivity.this, R.string.chip_esta_borrado, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.chip_no_esta_borrado, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> processStatusTextView
                        .setText(getString(R.string.error_verificando_borrado) + ": " + e.getMessage()));
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
        menu.add(Menu.NONE, 1, 1, getString(R.string.modelo_programador));
        menu.add(Menu.NONE, 2, 2, getString(R.string.protocolo));
        menu.add(Menu.NONE, 6, 3, "💾 " + getString(R.string.exportar_memoria));
        menu.add(Menu.NONE, 7, 4, "📋 " + getString(R.string.chip_info_json));
        menu.add(Menu.NONE, 3, 5, "📚 " + getString(R.string.gputils_termux_asm));
        menu.add(Menu.NONE, 5, 6, "📚 " + getString(R.string.sdcc_termux_tutorial));
        menu.add(Menu.NONE, 4, 7, getString(R.string.politica_de_privacidad));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
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
                startActivity(new Intent(this, Politicas.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** NUEVO: Muestra diálogo para exportar memoria leída */
    private void showExportDialog() {
        if (lastReadRomData.isEmpty() && lastReadEepromData.isEmpty() && lastReadConfigData.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_hay_datos_para_exportar),
                    Toast.LENGTH_LONG).show();
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
                        int eepromAddr = (coreBits == 16) ? 0xF00000 : 0x4200;
                        byte[] eepromBytes = stringHexToByteArray(lastReadEepromData);
                        if (eepromBytes != null && eepromBytes.length > 0) {
                            eepromBytes = HexExportManager.formatForHexExport(eepromBytes, coreBits, true);
                            hexExportManager.exportAsHexWithAddress(eepromBytes, eepromAddr, chipName + "_EEPROM");
                        }
                    } else if (selected.equals(getString(R.string.exportar_eeprom_bin))) {
                        hexExportManager.exportBinStringAsFile(lastReadEepromData, chipName + "_EEPROM");

                    } else if (selected.equals(getString(R.string.exportar_config_hex))) {
                        int configAddr = (coreBits == 16) ? 0x300000 : 0x4000;
                        byte[] configBytes = stringHexToByteArray(lastReadConfigData);
                        if (configBytes != null && configBytes.length > 0) {
                            configBytes = HexExportManager.formatForHexExport(configBytes, coreBits, false);
                            hexExportManager.exportAsHexWithAddress(configBytes, configAddr, chipName + "_CONFIG");
                        }
                    } else if (selected.equals(getString(R.string.exportar_config_bin))) {
                        byte[] configBytes = stringHexToByteArray(lastReadConfigData);
                        if (configBytes != null && configBytes.length > 0) {
                            hexExportManager.exportAsBinary(configBytes, chipName + "_CONFIG");
                        }

                    } else if (selected.equals(getString(R.string.exportar_dump_completo))) {
                        byte[] romBytes = stringHexToByteArray(lastReadRomData);
                        byte[] eepromBytes = stringHexToByteArray(lastReadEepromData);
                        byte[] configBytes = stringHexToByteArray(lastReadConfigData);

                        int eepromAddr = (coreBits == 16) ? 0xF00000 : 0x4200;
                        int configAddr = (coreBits == 16) ? 0x300000 : 0x4000;

                        romBytes = HexExportManager.formatForHexExport(romBytes, coreBits, false);
                        eepromBytes = HexExportManager.formatForHexExport(eepromBytes, coreBits, true);
                        configBytes = HexExportManager.formatForHexExport(configBytes, coreBits, false);

                        hexExportManager.exportFullDumpAsHex(romBytes, eepromBytes, configBytes, eepromAddr, configAddr,
                                chipName + "_FULL");
                    }
                })
                .setNegativeButton(getString(R.string.cancelar), null)
                .show();
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

    /**
     * MODIFICADO: Muestra la información completa del chip en un formato de tabla profesional.
     */
    private void showChipInfoJson() {
        if (currentChip == null) {
            Toast.makeText(this, getString(R.string.selecciona_chip_primero), Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear contenedor principal con scroll
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#1A1A1A")); // Fondo oscuro premium
        scrollView.setPadding(40, 40, 40, 40);

        // Crear tabla
        TableLayout tableLayout = new TableLayout(this);
        tableLayout.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
        tableLayout.setStretchAllColumns(true);

        // Estilo de celdas
        int paddingSide = 20;
        int paddingTopBottom = 16;
        int textColorLabel = Color.parseColor("#BBBBBB");
        int textColorValue = Color.WHITE;
        int textColorHeader = Color.parseColor("#9C27B0"); // Púrpura para secciones
        float textSize = 15f;
        float textSizeHeader = 17f;

        Map<String, Object> allData = currentChip.toDict();
        
        // Función auxiliar para añadir filas
        java.util.function.BiConsumer<String, String> addRow = (labelStr, valueStr) -> {
            TableRow row = new TableRow(this);
            row.setPadding(0, 8, 0, 8);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView label = new TextView(this);
            label.setText(labelStr);
            label.setTextColor(textColorLabel);
            label.setTextSize(textSize);
            label.setPadding(paddingSide, paddingTopBottom, paddingSide, paddingTopBottom);
            label.setTypeface(null, Typeface.BOLD);

            TextView value = new TextView(this);
            value.setText(valueStr);
            value.setTextColor(textColorValue);
            value.setTextSize(textSize);
            value.setPadding(paddingSide, paddingTopBottom, paddingSide, paddingTopBottom);
            value.setGravity(Gravity.END);

            row.addView(label);
            row.addView(value);

            View divider = new View(this);
            divider.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.parseColor("#444444"));

            tableLayout.addView(row);
            tableLayout.addView(divider);
        };

        // Función auxiliar para añadir encabezados de sección
        java.util.function.Consumer<String> addHeader = (title) -> {
            TextView header = new TextView(this);
            header.setText(title);
            header.setTextColor(textColorHeader);
            header.setTextSize(textSizeHeader);
            header.setTypeface(null, Typeface.BOLD);
            header.setPadding(paddingSide, 40, paddingSide, 10);
            tableLayout.addView(header);
            
            View divider = new View(this);
            divider.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(textColorHeader);
            tableLayout.addView(divider);
        };

        // --- SECCIÓN: IDENTIFICACIÓN ---
        addHeader.accept("IDENTIFICACIÓN");
        addRow.accept(getString(R.string.modelo) + ":", String.valueOf(allData.get("chip_name")));
        addRow.accept("Chip ID:", "0x" + String.valueOf(allData.get("chip_id")));
        addRow.accept("Incluye:", String.valueOf(allData.get("include")));

        // --- SECCIÓN: MEMORIA ---
        addHeader.accept("MEMORIA");
        try {
            addRow.accept("Memoria ROM:", String.format("%,d Bytes", currentChip.getTamanoROM()));
            addRow.accept("Memoria EEPROM:", currentChip.isTamanoValidoDeEEPROM() ? 
                    String.format("%,d Bytes", currentChip.getTamanoEEPROM()) : "N/A");
        } catch (Exception e) {}
        addRow.accept("Flash Chip:", String.valueOf(allData.get("flash_chip")));

        // --- SECCIÓN: HARDWARE ---
        addHeader.accept("HARDWARE");
        addRow.accept(getString(R.string.tipo_de_nucleo) + ":", allData.get("core_bits") + " bits (" + allData.get("core_type") + ")");
        addRow.accept("Pines:", currentChip.getNumeroDePines() + " pines");
        addRow.accept("Ubicación Pin 1:", String.valueOf(allData.get("pin1_location")));
        addRow.accept("Imagen Socket:", String.valueOf(allData.get("socket_image")));
        addRow.accept("Modo ICSP Only:", String.valueOf(allData.get("icsp_only")));

        // --- SECCIÓN: PROGRAMACIÓN ---
        addHeader.accept("PARÁMETROS DE GRABACIÓN");
        addRow.accept("Erase Mode:", String.valueOf(allData.get("erase_mode")));
        addRow.accept("Power Sequence:", String.valueOf(allData.get("power_sequence")));
        addRow.accept("Program Delay:", String.valueOf(allData.get("program_delay")));
        addRow.accept("Program Tries:", String.valueOf(allData.get("program_tries")));
        addRow.accept("Over Program:", String.valueOf(allData.get("over_program")));
        addRow.accept("CP Warn:", String.valueOf(allData.get("cp_warn")));

        // --- SECCIÓN: FUSES ---
        Map<String, ?> fuses = (Map<String, ?>) allData.get("fuses");
        if (fuses != null && !fuses.isEmpty()) {
            addHeader.accept("FUSIBLES (CONFIGURACIÓN)");
            for (Map.Entry<String, ?> entry : fuses.entrySet()) {
                String fuseName = entry.getKey();
                String options = "";
                if (entry.getValue() instanceof List) {
                    options = String.join(", ", (List<String>) entry.getValue());
                }
                addRow.accept(fuseName, options);
            }
        }

        scrollView.addView(tableLayout);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.chip_info_json))
                .setView(scrollView)
                .setPositiveButton(getString(R.string.aceptar), null)
                .show();
    }

    private void openTutorialGputils() {
        try {
            Intent intent = new Intent(MainActivity.this, TutorialGputilsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_abriendo_tutorial) + ": " + e.getMessage(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void openTutorialSdcc() {
        try {
            Intent intent = new Intent(MainActivity.this, com.diamon.tutorial.TutorialSdccActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_abriendo_tutorial) + ": " + e.getMessage(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void showProgrammerModelDialog() {
        if (usbManager.isConnected()) {
            String model = usbManager.getProtocolo().obtenerVersionOModeloDelProgramador();
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.modelo_del_programador))
                    .setMessage(model)
                    .setPositiveButton(getString(R.string.aceptar), null)
                    .show();
        }
    }

    private void showProtocolDialog() {
        String currentProto = usbManager.getTipoProtocolo().getNombre();
        String detected = "";
        if (usbManager.isConnected()) {
            detected = usbManager.getProtocolo().obtenerProtocoloDelProgramador();
        }

        String[] protocolos = TipoProtocolo.getNombres();
        int currentIndex = 0;
        for (int i = 0; i < protocolos.length; i++) {
            if (protocolos[i].equals(currentProto)) {
                currentIndex = i;
                break;
            }
        }

        final int[] selectedIndex = { currentIndex };
        String title = getString(R.string.protocolo_del_programador);
        if (!detected.isEmpty()) {
            title += " (" + detected.trim() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(protocolos, currentIndex, (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton(getString(R.string.aceptar), (dialog, which) -> {
                    TipoProtocolo selected = TipoProtocolo.values()[selectedIndex[0]];
                    usbManager.setTipoProtocolo(selected);
                    Toast.makeText(this,
                            getString(R.string.protocolo) + ": " + selected.getNombre(),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancelar), null)
                .show();
    }

    @Override
    protected void onPause() {
        publicidad.pausarBanner();
        super.onPause();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        publicidad.resumenBanner();
        if (wakeLock != null) {
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    @SuppressLint("Wakelock")
    @Override
    protected void onDestroy() {
        try {
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

    public MostrarPublicidad getPublicidad() {
        return publicidad;
    }
}
