package com.diamon.pic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.diamon.chip.ChipPic;
import com.diamon.chip.ChipinfoEntry;
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
import com.diamon.utilidades.Recurso;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MainActivity COMPLETAMENTE ACTUALIZADA
 *
 * <p>Nueva funcionalidad: - Configuración de fusibles mediante PopupWindow - Indicador visual de
 * estado de fusibles - Restauración de fusibles desde PIC o HEX - ID personalizado del usuario -
 * Integración completa con DatosPicProcesados y ChipinfoEntry
 */
public class MainActivity extends AppCompatActivity {

    private TextView connectionStatusTextView;
    private TextView processStatusTextView;
    private TextView chipInfoTextView;
    private TextView fuseStatusTextView; // NUEVO
    private LinearLayout romDataContainer;
    private LinearLayout eepromDataContainer;
    private Spinner chipSpinner;
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
    private ChipinfoEntry currentChipFuses;

    // NUEVAS VARIABLES PARA FUSES
    private boolean fusesConfigured = false;
    private List<Integer> configuredFuses = new ArrayList<>();
    private byte[] configuredID = new byte[] {0};
    private Map<String, String> lastFuseConfiguration = null;
    private DatosPicProcesados datosPicProcesados = null;

    @SuppressLint({"InvalidWakeLockTag", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        usbManager.initialize();
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
        recurso = new Recurso(this);
        publicidad = new MostrarPublicidad(this);
        publicidad.cargarBanner();
    }

    private void findViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        processStatusTextView = findViewById(R.id.processStatusTextView);
        chipInfoTextView = findViewById(R.id.chipInfoTextView);
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
            com.google.android.gms.ads.AdView banner = publicidad.getBanner();
            if (banner != null) {
                if (banner.getParent() != null) {
                    ((android.view.ViewGroup) banner.getParent()).removeView(banner);
                }
                FrameLayout.LayoutParams params =
                        new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                banner.setLayoutParams(params);
                bannerContainer.addView(banner);
            }
        }
    }

    private void setupToolbar() {
        // Ya configurado en findViews()
    }

    private void initializeManagers() {
        usbManager = new UsbConnectionManager(this);
        programmingManager = new PicProgrammingManager(this);
        fileManager = new FileManager(this);
        fileManager.initialize();
        chipSelectionManager = new ChipSelectionManager(this);
        memoryDisplayManager = new MemoryDisplayManager(this);
        dialogManager = new ProgrammingDialogManager(this);

        // NUEVO: Inicializar popup de fusibles
        fuseConfigPopup =
                new FuseConfigPopup(
                        this,
                        new FuseConfigPopup.FuseConfigListener() {
                            @Override
                            public void onFusesApplied(
                                    List<Integer> fuses,
                                    byte[] idData,
                                    Map<String, String> configuration) {
                                // Guardar configuración de fusibles
                                fusesConfigured = true;
                                configuredFuses = new ArrayList<>(fuses);
                                configuredID = idData;
                                lastFuseConfiguration = configuration;

                                // Actualizar UI
                                updateFuseStatus(true);

                                Toast.makeText(
                                                MainActivity.this,
                                                "Fusibles configurados correctamente",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }

                            @Override
                            public void onFusesCancelled() {
                                // No hacer nada, mantener configuración anterior
                            }
                        });
    }

    private void setupListeners() {
        setupUsbConnectionListeners();
        setupChipSelectionListeners();
        setupFileManagerListeners();
        setupProgrammingListeners();
        setupButtonListeners();
    }

    private void setupUsbConnectionListeners() {
        usbManager.setConnectionListener(
                new UsbConnectionManager.UsbConnectionListener() {
                    @Override
                    public void onConnected() {
                        runOnUiThread(
                                () -> {
                                    connectionStatusTextView.setTextColor(Color.GREEN);
                                    connectionStatusTextView.setText(getString(R.string.conectado));
                                    programmingManager.setProtocolo(usbManager.getProtocolo());
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    getString(R.string.conectado_al_programador),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                });
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(
                                () -> {
                                    connectionStatusTextView.setTextColor(Color.RED);
                                    connectionStatusTextView.setText(
                                            getString(R.string.desconectado));
                                });
                    }

                    @Override
                    public void onConnectionError(String errorMessage) {
                        runOnUiThread(
                                () -> {
                                    connectionStatusTextView.setTextColor(Color.RED);
                                    connectionStatusTextView.setText(
                                            getString(R.string.desconectado));
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    errorMessage,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                });
                    }
                });
    }

    private void setupChipSelectionListeners() {
        chipSelectionManager.setSelectionListener(
                new ChipSelectionManager.ChipSelectionListener() {
                    @Override
                    public void onChipSelected(ChipPic chip, String model) {

                        currentChip = chip;
                        String info = chipSelectionManager.getSelectedChipInfo();
                        chipInfoTextView.setText(info);

                        // Cargar fusibles del chip
                        currentChipFuses = chipSelectionManager.getSelectedChipFuses();

                        // Actualizar información del chip
                        chipInfoTextView.setText(chipSelectionManager.getSelectedChipInfoColored());

                        updateSwitchColors();

                        // IMPORTANTE: Actualizar estado del switch ICSP
                        updateICSPSwitchState();

                        // NUEVO: Limpiar configuración de fusibles cuando se selecciona nuevo chip
                        clearFuseConfiguration();

                        if (usbManager.isConnected()) {
                            try {
                                usbManager.getProtocolo().iniciarVariablesDeProgramacion(chip);
                            } catch (Exception e) {
                                Toast.makeText(
                                                MainActivity.this,
                                                getString(R.string.error_inicializando_chip),
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }

                    @Override
                    public void onChipSelectionError(String errorMessage) {
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

                        // Desabilitar switch y botones
                        swModeICSP.setEnabled(false);
                        swModeICSP.setChecked(false);
                    }
                });
        chipSelectionManager.setupSpinner(chipSpinner);
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

                            return;
                        }

                        // Si el chip soporta ambos modos, permitir cambio
                        currentChip.setActivarICSP(isChecked);

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

    private void setupProgrammingListeners() {
        programmingManager.setProgrammingListener(
                new PicProgrammingManager.ProgrammingListener() {
                    @Override
                    public void onProgrammingStarted() {
                        runOnUiThread(
                                () ->
                                        processStatusTextView.setText(
                                                getString(R.string.iniciando_programacion)));
                    }

                    @Override
                    public void onProgrammingProgress(String message, int progress) {
                        runOnUiThread(
                                () ->
                                        processStatusTextView.setText(
                                                message + " (" + progress + "%)"));
                    }

                    @Override
                    public void onProgrammingCompleted(boolean success) {
                        runOnUiThread(
                                () -> {
                                    if (success) {
                                        processStatusTextView.setText(
                                                getString(R.string.pic_programado_exitosamente));
                                    } else {
                                        processStatusTextView.setText(
                                                getString(R.string.error_programando_pic));
                                    }
                                });
                    }

                    @Override
                    public void onProgrammingError(String errorMessage) {
                        runOnUiThread(
                                () -> {
                                    processStatusTextView.setText("Error: " + errorMessage);
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    errorMessage,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                });
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

        if (currentChipFuses == null) {
            Toast.makeText(this, "No hay datos de fusibles para este chip", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Mostrar popup con última configuración si existe
        fuseConfigPopup.show(
                currentChip, currentChipFuses, datosPicProcesados, lastFuseConfiguration);
    }

    /** NUEVO: Limpia la configuración de fusibles */
    private void clearFuseConfiguration() {
        fusesConfigured = false;
        configuredFuses = new ArrayList<>();
        configuredID = new byte[] {0};
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
        final byte[] idToUse = fusesConfigured ? configuredID : new byte[] {0};
        final List<Integer> fusesToUse =
                fusesConfigured ? new ArrayList<>(configuredFuses) : new ArrayList<>();

        dialogManager.showProgrammingDialog(
                () -> {
                    new Thread(
                                    () -> {
                                        boolean success =
                                                programmingManager.programChip(
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

        new Thread(
                        () -> {
                            String romData = programmingManager.readRomMemory(currentChip);
                            String eepromData = programmingManager.readEepromMemory(currentChip);

                            runOnUiThread(
                                    () -> {
                                        try {
                                            int romSize = currentChip.getTamanoROM();
                                            int eepromSize =
                                                    currentChip.isTamanoValidoDeEEPROM()
                                                            ? currentChip.getTamanoEEPROM()
                                                            : 0;
                                            boolean hasEeprom =
                                                    currentChip.isTamanoValidoDeEEPROM()
                                                            && !eepromData.isEmpty();

                                            memoryDisplayManager.showMemoryDataPopup(
                                                    romData != null ? romData : "",
                                                    romSize,
                                                    eepromData != null ? eepromData : "",
                                                    eepromSize,
                                                    hasEeprom);

                                            processStatusTextView.setText(
                                                    getString(R.string.memoria_leida_exitosamente));
                                        } catch (ChipConfigurationException e) {
                                            Toast.makeText(
                                                            MainActivity.this,
                                                            "Error: " + e.getMessage(),
                                                            Toast.LENGTH_LONG)
                                                    .show();
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
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "PICProgramming");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, 1, getString(R.string.modelo_programador));
        menu.add(Menu.NONE, 2, 2, getString(R.string.protocolo));
        menu.add(Menu.NONE, 3, 3, "📚 " + getString(R.string.gputils_termux_asm));
        menu.add(Menu.NONE, 4, 4, getString(R.string.politica_de_privacidad));
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
                publicidad.disposeBanner();
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
}
