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
import com.diamon.managers.MemoryDisplayManager;
import com.diamon.managers.PicProgrammingManager;
import com.diamon.managers.ProgrammingDialogManager;
import com.diamon.managers.UsbConnectionManager;
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
    private Switch swModeICSP;

    private android.widget.Button btnSelectHex;
    private android.widget.Button btnProgramarPic;
    private android.widget.Button btnLeerMemoriaDeLPic;
    private android.widget.Button btnVerificarMemoriaDelPic;
    private android.widget.Button btnBorrarMemoriaDeLPic;
    private android.widget.Button btnDetectarPic;
    private android.widget.Button btnConfigureFuses; // NUEVO

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

    private String firmware = "";
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

        // Inicializar el Switch de ICSP (Agregar esta línea)
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
        // CORRECCIÓN PARA ANR en inicio:
        // Mover la inicialización de USB (escaneo de drivers y apertura de protocolo)
        // a un hilo secundario para no bloquear el onCreate.
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
                                            "HEX procesado correctamente");
                                });

                    } catch (Exception e) {
                        runOnUiThread(
                                () -> {
                                    Toast.makeText(
                                            MainActivity.this,
                                            "Error procesando HEX: "
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
            Toast.makeText(this, "Selecciona un chip primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentChip.getFusesMap() == null || currentChip.getFusesMap().isEmpty()) {
            Toast.makeText(this, "No hay datos de fusibles para este chip", Toast.LENGTH_SHORT)
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
            fuseStatusTextView.setText("✓ Configurados");
            fuseStatusTextView.setTextColor(Color.WHITE);
            fuseStatusTextView.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            fuseStatusTextView.setText("No configurados");
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
                btnDetectarPic
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

    /** MODIFICADO: Ahora usa los fusibles configurados */
    private void executeProgram() {
        if (currentChip == null || firmware.isEmpty()) {
            Toast.makeText(
                    this,
                    getString(R.string.seleccione_un_chip_y_cargue_un),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        publicidad.ocultarBanner();

        // MODIFICADO: Usar fusibles y ID configurados si existen
        final byte[] idToUse = fusesConfigured ? configuredID : new byte[] { 0 };
        final List<Integer> fusesToUse = fusesConfigured ? new ArrayList<>(configuredFuses) : new ArrayList<>();

        dialogManager.showProgrammingDialog(
                () -> {
                    new Thread(
                            () -> {
                                boolean success = programmingManager.programChip(
                                        currentChip, firmware, idToUse, fusesToUse);
                                runOnUiThread(
                                        () -> {
                                            dialogManager.updateProgrammingResult(success);
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

                    runOnUiThread(
                            () -> {
                                try {
                                    int romSize = currentChip.getTamanoROM();
                                    int eepromSize = currentChip.isTamanoValidoDeEEPROM()
                                            ? currentChip.getTamanoEEPROM()
                                            : 0;
                                    boolean hasEeprom = currentChip.isTamanoValidoDeEEPROM()
                                            && !eepromData.isEmpty();

                                    // Actualizar popup con los datos leídos
                                    memoryDisplayManager.updateWithData(
                                            romData != null ? romData : "",
                                            romSize,
                                            eepromData != null ? eepromData : "",
                                            eepromSize,
                                            hasEeprom);

                                    processStatusTextView.setText(
                                            getString(R.string.memoria_leida_exitosamente));

                                    // Mostrar banner de nuevo
                                    publicidad.mostrarBanner();
                                } catch (ChipConfigurationException e) {
                                    Toast.makeText(
                                            MainActivity.this,
                                            "Error: " + e.getMessage(),
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
        new Thread(
                () -> {
                    boolean isEmpty = programmingManager.verifyMemoryErased();
                    runOnUiThread(
                            () -> {
                                if (isEmpty) {
                                    processStatusTextView.setText(
                                            getString(R.string.memoria_vacia));
                                } else {
                                    processStatusTextView.setText(
                                            getString(R.string.memoria_contiene_datos));
                                }
                            });
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
        menu.add(Menu.NONE, 3, 3, "📚 " + getString(R.string.gputils_termux_asm));
        // Aquí va el recurso string: R.string.sdcc_termux_tutorial
        menu.add(Menu.NONE, 5, 4, "📚 " + "SDCC en Termux Compilador C Android");
        menu.add(Menu.NONE, 4, 5, getString(R.string.politica_de_privacidad));
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

    private void openTutorialGputils() {
        try {
            Intent intent = new Intent(MainActivity.this, TutorialGputilsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir tutorial: " + e.getMessage(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void openTutorialSdcc() {
        try {
            Intent intent = new Intent(MainActivity.this, com.diamon.tutorial.TutorialSdccActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir tutorial: " + e.getMessage(), Toast.LENGTH_LONG)
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
        if (usbManager.isConnected()) {
            String protocol = usbManager.getProtocolo().obtenerProtocoloDelProgramador();
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.protocolo_del_programador))
                    .setMessage(protocol)
                    .setPositiveButton(getString(R.string.aceptar), null)
                    .show();
        }
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
