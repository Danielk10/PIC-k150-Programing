package com.diamon.pic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;

import com.diamon.managers.ChipSelectionManager;
import com.diamon.managers.FileManager;
import com.diamon.managers.MemoryDisplayManager;
import com.diamon.managers.PicProgrammingManager;
import com.diamon.managers.ProgrammingDialogManager;
import com.diamon.managers.UsbConnectionManager;
import com.diamon.politicas.Politicas;
import com.diamon.publicidad.MostrarPublicidad;
import com.diamon.utilidades.Recurso;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

/**
 * MainActivity COMPLETAMENTE CORREGIDA - Banner ABAJO funcional - Filtro de archivos .hex/.bin
 * funcional - Botones con estados visuales (activo/desactivado) - Datos ROM/EEPROM en UN SOLO popup
 * - Colores: Verde (datos) / Blanco (vacio) - ScrollView en ambas secciones - Datos NO se salen de
 * la pantalla
 */
public class MainActivity extends AppCompatActivity {

    private TextView connectionStatusTextView;
    private TextView processStatusTextView;
    private TextView chipInfoTextView;
    private LinearLayout romDataContainer;
    private LinearLayout eepromDataContainer;
    private Spinner chipSpinner;
    private android.widget.Button btnSelectHex;
    private android.widget.Button btnProgramarPic;
    private android.widget.Button btnLeerMemoriaDeLPic;
    private android.widget.Button btnVerificarMemoriaDelPic;
    private android.widget.Button btnBorrarMemoriaDeLPic;
    private android.widget.Button btnDetectarPic;

    private UsbConnectionManager usbManager;
    private PicProgrammingManager programmingManager;
    private FileManager fileManager;
    private ChipSelectionManager chipSelectionManager;
    private MemoryDisplayManager memoryDisplayManager;
    private ProgrammingDialogManager dialogManager;
    private MostrarPublicidad publicidad;
    private Recurso recurso;
    private PowerManager.WakeLock wakeLock;

    private String firmware = "";
    private ChipPic currentChip;

    @SuppressLint({"InvalidWakeLockTag", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeAppCenter();
        initializeBasicComponents();
        findViews();
        setupBanner(); // IMPORTANTE: Configurar banner ABAJO
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
        chipSpinner = findViewById(R.id.chipSpinner);

        btnSelectHex = findViewById(R.id.btnSelectHex);
        btnProgramarPic = findViewById(R.id.btnProgramarPic);
        btnLeerMemoriaDeLPic = findViewById(R.id.btnLeerMemoriaDeLPic);
        btnVerificarMemoriaDelPic = findViewById(R.id.btnVerificarMemoriaDelPic);
        btnBorrarMemoriaDeLPic = findViewById(R.id.btnBorrarMemoriaDeLPic);
        btnDetectarPic = findViewById(R.id.btnDetectarPic);

        romDataContainer = findViewById(R.id.romDataContainer);
        eepromDataContainer = findViewById(R.id.eepromDataContainer);
    }

    /** CONFIGURACION DEL BANNER ABAJO */
    private void setupBanner() {
        FrameLayout bannerContainer = findViewById(R.id.bannerContainer);
        if (bannerContainer != null && publicidad != null) {
            com.google.android.gms.ads.AdView banner = publicidad.getBanner();
            if (banner != null) {
                // Limpiar cualquier padre previo
                if (banner.getParent() != null) {
                    ((android.view.ViewGroup) banner.getParent()).removeView(banner);
                }

                // Agregar al contenedor con parametros correctos
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
                                    connectionStatusTextView.setText("Conectado");
                                    programmingManager.setProtocolo(usbManager.getProtocolo());
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    "Conectado al programador",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                });
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(
                                () -> {
                                    connectionStatusTextView.setTextColor(Color.RED);
                                    connectionStatusTextView.setText("Desconectado");
                                });
                    }

                    @Override
                    public void onConnectionError(String errorMessage) {
                        runOnUiThread(
                                () -> {
                                    connectionStatusTextView.setTextColor(Color.RED);
                                    connectionStatusTextView.setText("Desconectado");
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

                        if (usbManager.isConnected()) {
                            try {
                                usbManager.getProtocolo().iniciarVariablesDeProgramacion(chip);
                            } catch (Exception e) {
                                Toast.makeText(
                                                MainActivity.this,
                                                "Error inicializando chip",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }

                    @Override
                    public void onChipSelectionError(String errorMessage) {
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });

        chipSelectionManager.setupSpinner(chipSpinner);
    }

    private void setupFileManagerListeners() {
        fileManager.setFileLoadListener(
                new FileManager.FileLoadListener() {
                    @Override
                    public void onFileLoaded(String content, String fileName) {
                        firmware = content;
                        processStatusTextView.setText("Archivo cargado: " + fileName);
                        enableOperationButtons(true);
                        Toast.makeText(
                                        MainActivity.this,
                                        "Archivo HEX cargado exitosamente",
                                        Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onFileLoadError(String errorMessage) {
                        processStatusTextView.setText("Error cargando archivo");
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
                                () -> processStatusTextView.setText("Iniciando programacion..."));
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
                                                "PIC programado exitosamente");
                                    } else {
                                        processStatusTextView.setText("Error programando PIC");
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
        btnProgramarPic.setOnClickListener(v -> executeProgram());
        btnLeerMemoriaDeLPic.setOnClickListener(v -> executeReadMemory());
        btnBorrarMemoriaDeLPic.setOnClickListener(v -> executeEraseMemory());
        btnVerificarMemoriaDelPic.setOnClickListener(v -> executeVerifyMemory());
        btnDetectarPic.setOnClickListener(v -> executeDetectChip());
    }

    /** HABILITAR/DESHABILITAR BOTONES CON ESTADOS VISUALES CLAROS */
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
                // ACTIVO: Color brillante + texto blanco
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
                // DESACTIVADO: Gris oscuro + texto gris claro
                btn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#555555")));
                btn.setTextColor(Color.parseColor("#AAAAAA"));
            }
        }
    }

    private void executeProgram() {
        if (currentChip == null || firmware.isEmpty()) {
            Toast.makeText(this, "Seleccione un chip y cargue un archivo HEX", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        publicidad.ocultarBanner();

        dialogManager.showProgrammingDialog(
                () -> {
                    new Thread(
                                    () -> {
                                        boolean success =
                                                programmingManager.programChip(
                                                        currentChip, firmware);
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

    /** LEER MEMORIA: Muestra ROM y EEPROM en UN SOLO popup */
    private void executeReadMemory() {
        if (currentChip == null) {
            Toast.makeText(this, "Seleccione un chip", Toast.LENGTH_SHORT).show();
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
                                                        ? currentChip
                                                                .getTamanoEEPROM()
                                                        : 0;
                                        boolean hasEeprom =
                                                currentChip.isTamanoValidoDeEEPROM()
                                                        && !eepromData.isEmpty();

                                        // UN SOLO POPUP con ROM y EEPROM
                                        memoryDisplayManager.showMemoryDataPopup(
                                                romData != null ? romData : "",
                                                romSize,
                                                eepromData != null ? eepromData : "",
                                                eepromSize,
                                                hasEeprom);

                                        processStatusTextView.setText("Memoria leida exitosamente");
                                        }catch(ChipConfigurationException e)
                                        {
                                            
                                            
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
                                                    "Memoria borrada exitosamente");
                                        } else {
                                            processStatusTextView.setText("Error borrando memoria");
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
                                            processStatusTextView.setText("Memoria vacia");
                                        } else {
                                            processStatusTextView.setText("Memoria contiene datos");
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
                                                    "PIC detectado en socket");
                                        } else {
                                            processStatusTextView.setText(
                                                    "No se detecto PIC en socket");
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
        menu.add(Menu.NONE, 1, 1, "Modelo Programador");
        menu.add(Menu.NONE, 2, 2, "Protocolo");
        menu.add(Menu.NONE, 3, 3, "Politica de Privacidad");
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
                startActivity(new Intent(this, Politicas.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgrammerModelDialog() {
        if (usbManager.isConnected()) {
            String model = usbManager.getProtocolo().obtenerVersionOModeloDelProgramador();
            new AlertDialog.Builder(this)
                    .setTitle("Modelo del Programador")
                    .setMessage(model)
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }

    private void showProtocolDialog() {
        if (usbManager.isConnected()) {
            String protocol = usbManager.getProtocolo().obtenerProtocoloDelProgramador();
            new AlertDialog.Builder(this)
                    .setTitle("Protocolo del Programador")
                    .setMessage(protocol)
                    .setPositiveButton("Aceptar", null)
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

            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }

        } catch (Exception e) {
            // Manejo de excepciones
        }

        super.onDestroy();
    }
}
