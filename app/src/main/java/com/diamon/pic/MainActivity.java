package com.diamon.pic;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.diamon.chip.ChipPic;
import com.diamon.datos.ChipinfoReader;
import com.diamon.excepciones.UsbCommunicationException;
import com.diamon.excepciones.HexProcessingException;
import com.diamon.politicas.Politicas;
import com.diamon.protocolo.ProtocoloP018;
import com.diamon.publicidad.MostrarPublicidad;
import com.diamon.utilidades.LogManager;
import com.diamon.utilidades.LogManager.Categoria;
import com.diamon.utilidades.Recurso;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.AdChoicesView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.diamon.pic.USB_PERMISSION";

    private static final int REQUEST_CODE_OPEN_FILE = 1;

    private static final int REQUEST_CODE_PERMISSION = 2;

    // Colores principales (agregar en los recursos o como constantes)
    private static final int COLOR_PRIMARY = Color.parseColor("#003366"); // Azul oscuro profesional

    private static final int COLOR_SECONDARY = Color.parseColor("#FF6600"); // Naranja Microchip

    private static final int COLOR_BACKGROUND = Color.parseColor("#1A1A2E"); // Fondo oscuro

    private static final int COLOR_CARD = Color.parseColor("#2A2A3E"); // Tarjetas

    private static final int COLOR_TEXT = Color.parseColor("#E0E0E0"); // Texto claro

    private static final int COLOR_ACCENT = Color.parseColor("#4CAF50"); // Verde para acciones

    private UsbSerialPort usbSerialPort;

    private List<UsbSerialDriver> drivers;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager ioManager;

    private UsbManager usbManager;

    private ConstraintLayout layout;

    private Button btnProgramarPic;

    private Button btnVerificarMemoriaDelPic;

    private Button btnBorrarMemoriaDeLPic;

    private Button btnLeerMemoriaDeLPic;

    private Button btnDetectarPic;

    private Button btnSelectHex;

    private LinearLayout romData;

    private LinearLayout eepromData;

    private volatile TextView mensaje;

    private TextView proceso;

    private String firware;

    private ChipPic chipPIC;

    private ChipinfoReader chip;

    private ProtocoloP018 protocolo;

    private WakeLock wakeLock;

    private MostrarPublicidad publicidad;

    private RelativeLayout disenoBanner;

    private FrameLayout disenoUI;

    private LinearLayout disenoBannerDimencion;

    private AlertDialog alertDialog;

    private AlertDialog.Builder builder;

    private AlertDialog alertDialog1;

    private AlertDialog.Builder builder1;

    private Recurso recurso;

    // Publicidad
    private PopupWindow popupWindow;

    private NativeAd nativeAd;

    // --- Variables para controlar el estado y la UI dinámica ---
    private boolean procesoGrabado = false; // Bandera de control

    private Thread hiloGrabado;

    private volatile boolean procesoCancelado = false;

    // Referencias a los componentes de la UI que cambiarán
    private ProgressBar statusProgressBar;

    private ImageView statusResultIcon;

    private Button actionButton;

    private TextView titleTextView;

    private TextView descriptionTextView;
    // -----------------------------------------------------------

    private final BroadcastReceiver usbReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    if (ACTION_USB_PERMISSION.equals(intent.getAction())) {

                        if (!drivers.isEmpty()) {

                            UsbSerialDriver driver = drivers.get(0);

                            connectToDevice(driver);

                            return;
                        }
                    }
                }
            };

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "InvalidWakeLockTag"})
    @SuppressWarnings({"deprecation", "unused"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar sistema de logging
        LogManager.configurarModoDesarrollo(); // Cambiar a configurarModoProduccion() para release
        LogManager.i(Categoria.UI, "MainActivity", "Iniciando aplicación PIC K150 Programming");

        AppCenter.start(
                getApplication(),
                "c9a1ef1a-bbfb-443a-863e-1c1d77e49c18",
                Analytics.class,
                Crashes.class);
        
        LogManager.i(Categoria.SISTEMA, "onCreate", "AppCenter inicializado para analytics y crash reports");

        recurso = new Recurso(this);

        publicidad = new MostrarPublicidad(this);

        publicidad.cargarBanner();

        firware = new String();

        disenoBannerDimencion = new LinearLayout(this);

        disenoBannerDimencion.setOrientation(LinearLayout.VERTICAL);

        disenoBannerDimencion.setId(View.generateViewId());

        LayoutParams parametros =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        disenoBanner = new RelativeLayout(this);

        disenoBanner.setId(View.generateViewId());

        disenoUI = new FrameLayout(this);

        disenoUI.setId(View.generateViewId());

        RelativeLayout.LayoutParams parametrosBanner =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        parametrosBanner.addRule(RelativeLayout.CENTER_HORIZONTAL);
        parametrosBanner.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        layout = new ConstraintLayout(this);

        layout.setBackgroundColor(Color.parseColor("#1A1A2E"));

        layout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        romData = new LinearLayout(this);

        romData.setOrientation(LinearLayout.VERTICAL);

        romData.setPadding(18, 18, 18, 18);

        // Crear background con bordes redondeados programáticamente
        GradientDrawable backgroundDrawable1 = new GradientDrawable();
        backgroundDrawable1.setColor(Color.parseColor("#000000")); // Color de fondo de la tarjeta
        backgroundDrawable1.setCornerRadius(30f); // Bordes redondeados
        romData.setBackground(backgroundDrawable1);

        // Agregar sombras en API 21 o superior (minSdkVersion >= 21)
        romData.setElevation(8);

        // LayoutParams con márgenes
        LinearLayout.LayoutParams layoutParams1 =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams1.setMargins(16, 16, 16, 16);
        romData.setLayoutParams(layoutParams1);

        eepromData = new LinearLayout(this);

        eepromData.setOrientation(LinearLayout.VERTICAL);

        eepromData.setPadding(18, 18, 18, 18);

        // Crear background con bordes redondeados programáticamente
        GradientDrawable backgroundDrawable2 = new GradientDrawable();
        backgroundDrawable2.setColor(Color.parseColor("#000000")); // Color de fondo de la tarjeta
        backgroundDrawable2.setCornerRadius(30f); // Bordes redondeados
        eepromData.setBackground(backgroundDrawable2);

        // Agregar sombras en API 21 o superior (minSdkVersion >= 21)
        eepromData.setElevation(8);

        // LayoutParams con márgenes
        LinearLayout.LayoutParams layoutParams2 =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams2.setMargins(16, 16, 16, 16);
        eepromData.setLayoutParams(layoutParams2);

        // Crear el Toolbar de manera programática
        Toolbar toolbar = new Toolbar(this);
        toolbar.setId(View.generateViewId());
        toolbar.setTitle("PIC k150 Programing");
        toolbar.setBackgroundColor(Color.parseColor("#003366")); // Fondo gris claro
        toolbar.setElevation(8);
        toolbar.setTitleTextColor(Color.WHITE);

        Drawable logo = getResources().getDrawable(R.mipmap.logo); // Reemplaza con tu logo

        // recurso
        toolbar.setLogo(logo);

        // Configurar el Toolbar como ActionBar
        setSupportActionBar(toolbar);

        ConstraintLayout.LayoutParams toolbarParams =
                new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);

        proceso = new TextView(this);

        mensaje = new TextView(this);

        btnProgramarPic =
                createIconButton(getString(R.string.program_pic), R.drawable.ic_program_chip);

        btnProgramarPic.setEnabled(false);

        btnVerificarMemoriaDelPic =
                createIconButton(
                        getString(R.string.verify_memory_erased), R.drawable.ic_verify_chip);

        btnVerificarMemoriaDelPic.setEnabled(false);

        btnBorrarMemoriaDeLPic =
                createIconButton(getString(R.string.erase_memory), R.drawable.ic_verify_chip);

        btnBorrarMemoriaDeLPic.setEnabled(false);

        btnLeerMemoriaDeLPic =
                createIconButton(getString(R.string.read_memory), R.drawable.ic_verify_chip);

        btnLeerMemoriaDeLPic.setEnabled(false);

        btnDetectarPic =
                createIconButton(getString(R.string.detect_pic), R.drawable.ic_verify_chip);

        btnDetectarPic.setEnabled(false);

        btnSelectHex =
                createIconButton(getString(R.string.load_hex_file), R.drawable.ic_verify_chip);

        Spinner chipSpinner = new Spinner(this);

        btnProgramarPic.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        showAdPopup();

                        publicidad.ocultarBanner();
                    }
                });

        /* btnProgramarPic.setOnClickListener(
        new OnClickListener() {

            @Override
            public void onClick(View v) {

                showAdPopup();

                publicidad.ocultarBanner();



                if (protocolo != null) {


                    boolean respuesta = protocolo.borrarMemoriasDelPic();

                    if (respuesta) {

                        proceso.setText(getString(R.string.memory_erased_successfully));

                    } else {

                        proceso.setText(getString(R.string.memory_erase_error));

                        return;
                    }

                    respuesta = protocolo.programarMemoriaROMDelPic(chipPIC, firware);

                    if (respuesta) {

                        proceso.setText(getString(R.string.rom_programmed_successfully));

                    } else {

                        proceso.setText(getString(R.string.rom_program_error));

                        return;
                    }

                    if (chipPIC.isTamanoValidoDeEEPROM()) {

                        respuesta =
                                protocolo.programarMemoriaEEPROMDelPic(chipPIC, firware);

                        if (respuesta) {

                            proceso.setText(
                                    getString(R.string.eeprom_programmed_successfully));

                        } else {

                            proceso.setText(getString(R.string.eeprom_program_error));

                            return;
                        }
                    }

                    respuesta = protocolo.programarFusesIDDelPic(chipPIC, firware);

                    if (respuesta) {

                        proceso.setText(getString(R.string.fuses_programmed_successfully));

                    } else {

                        proceso.setText(getString(R.string.fuses_program_error));

                        return;
                    }

                    if (chipPIC.getTipoDeNucleoBit() == 16) {

                        respuesta = protocolo.programarFusesDePics18F();

                        if (respuesta) {

                            proceso.setText(
                                    getString(R.string.fuses_18f_programmed_successfully));

                        } else {

                            proceso.setText(getString(R.string.fuses_18f_program_error));

                            return;
                        }
                    }

                    if (respuesta) {

                        proceso.setText(getString(R.string.pic_programmed_successfully));

                    } else {

                        proceso.setText(getString(R.string.pic_program_error));

                        return;
                    }
                }
            }
        });*/

        btnLeerMemoriaDeLPic.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (protocolo != null) {

                            StringBuffer datos1 = new StringBuffer();

                            StringBuffer datos2 = new StringBuffer();

                            datos1.append(protocolo.leerMemoriaROMDelPic(chipPIC));

                            if (chipPIC.isTamanoValidoDeEEPROM()) {

                                datos2.append(protocolo.leerMemoriaEEPROMDelPic(chipPIC));
                            }

                            if (datos1.length() > 0) {

                                if (romData.getChildCount() > 0) {

                                    romData.removeAllViewsInLayout();

                                    romData.requestLayout();

                                    romData.invalidate();
                                }

                                displayData(
                                        romData,
                                        datos1.toString(),
                                        4,
                                        8); // 4 dígitos por grupo, 8 columnas

                                if (datos2.length() > 0) {

                                    if (eepromData.getChildCount() > 0) {

                                        eepromData.removeAllViewsInLayout();

                                        eepromData.requestLayout();

                                        eepromData.invalidate();
                                    }

                                    displayData(
                                            eepromData,
                                            datos2.toString(),
                                            2,
                                            8); // 2 dígitos por grupo, 8 columnas
                                }

                                proceso.setText(getString(R.string.memory_read_successfully));

                            } else {

                                proceso.setText(getString(R.string.memory_read_error));
                            }
                        }
                    }
                });

        btnBorrarMemoriaDeLPic.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (protocolo != null) {

                            boolean respuesta = protocolo.borrarMemoriasDelPic();

                            if (respuesta) {

                                proceso.setText(getString(R.string.memory_erased_successfully));

                            } else {

                                proceso.setText(getString(R.string.memory_erase_error));
                            }
                        }
                    }
                });

        btnVerificarMemoriaDelPic.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (protocolo != null) {

                            boolean respuesta =
                                    protocolo.verificarSiEstaBarradaLaMemoriaEEPROMDelDelPic();

                            if (respuesta) {

                                proceso.setText(getString(R.string.memory_empty));

                            } else {

                                proceso.setText(getString(R.string.memory_has_data));
                            }
                        }
                    }
                });

        btnDetectarPic.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        if (protocolo != null) {

                            boolean respuesta = protocolo.detectarPicEnElSocket();

                            if (respuesta) {

                                proceso.setText(getString(R.string.pic_in_socket));

                            } else {

                                proceso.setText(getString(R.string.pic_not_in_socket));
                            }
                        }
                    }
                });

        btnSelectHex.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        checkPermissionsAndOpenFile();
                    }
                });

        usbManager = (UsbManager) getSystemService(USB_SERVICE);

        // Detectar dispositivos USB
        drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        // Registrar el BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU es API 33
            // Para Android 13 (API 33) y superior, DEBES especificar el flag.
            // Usamos RECEIVER_NOT_EXPORTED porque es lo más común y seguro para este caso.
            ContextCompat.registerReceiver(
                    this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        } else {
            // Para versiones anteriores a API 33, el registro antiguo es suficiente
            // y no devuelve un Intent a menos que sea para un sticky broadcast.
            registerReceiver(usbReceiver, filter);
        }

        if (!drivers.isEmpty()) {

            if (usbManager.hasPermission(drivers.get(0).getDevice())) {

                PendingIntent permissionIntent =
                        PendingIntent.getBroadcast(
                                this,
                                0,
                                new Intent(ACTION_USB_PERMISSION),
                                PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(drivers.get(0).getDevice(), permissionIntent);
            }
        }

        chip = new ChipinfoReader(this);

        String[] pic = new String[chip.getModelosPic().size()];

        int numerosPic = 0;

        for (String modelo : chip.getModelosPic()) {
            pic[numerosPic] = modelo;

            numerosPic++;
        }

        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, pic);

        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        chipSpinner.setAdapter(arrayAdapter);

        chipSpinner.setPopupBackgroundDrawable(
                new ColorDrawable(Color.LTGRAY)); // Fondo del dropdown

        chipSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {

                        if (view != null) {
                            ((TextView) view)
                                    .setTextColor(
                                            Color.GREEN); // Color verde del texto seleccionado
                        } else {

                        }

                        switch (position) {
                            case 0:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 1:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 2:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 3:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 4:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 5:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 6:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 7:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 8:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 9:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 10:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 11:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 12:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 13:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 14:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 15:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 16:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 17:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 18:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 19:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 20:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 21:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 22:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 23:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 24:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 25:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 26:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 27:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 28:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 29:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 30:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 31:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 32:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 33:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 34:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 35:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 36:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 37:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 38:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 39:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 40:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 41:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 42:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 43:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 44:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 45:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 46:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 47:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 48:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 49:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 50:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 51:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 52:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 53:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 54:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 55:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 56:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 57:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 58:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 59:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 60:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 61:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 62:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 63:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 64:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 65:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 66:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 67:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 68:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 69:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 70:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 71:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 72:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 73:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 74:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 75:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 76:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 77:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 78:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 79:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 80:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 81:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 82:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 83:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 84:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 85:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 86:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 87:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 88:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 89:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 90:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 91:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 92:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 93:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 94:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 95:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 96:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 97:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 98:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 99:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 100:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 101:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 102:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 103:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 104:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 105:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 106:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 107:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 108:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 109:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 110:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 111:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 112:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 113:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 114:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 115:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 116:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 117:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 118:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 119:
                                mostrarInformacionPic(pic[position]);
                                break;
                            case 120:
                                mostrarInformacionPic(pic[position]);
                                break;

                            case 121:
                                mostrarInformacionPic(pic[position]);
                                break;

                            default:
                                break;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

        layout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        layout.addView(toolbar, toolbarParams);

        mensaje.setText(getString(R.string.desconectado));
        mensaje.setTextSize(18);
        mensaje.setTextColor(Color.WHITE);
        mensaje.setGravity(Gravity.CENTER);
        mensaje.setId(View.generateViewId());
        layout.addView(mensaje);

        // LinearLayout para agrupar botones
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setId(View.generateViewId());
        layout.addView(buttonLayout);

        buttonLayout.addView(btnProgramarPic);

        buttonLayout.addView(btnVerificarMemoriaDelPic);

        buttonLayout.addView(btnBorrarMemoriaDeLPic);

        buttonLayout.addView(btnLeerMemoriaDeLPic);

        buttonLayout.addView(btnDetectarPic);

        buttonLayout.addView(btnSelectHex);

        proceso.setText(getString(R.string.waiting_for_pic));
        proceso.setTextColor(Color.WHITE);
        proceso.setGravity(Gravity.CENTER);
        proceso.setId(View.generateViewId());
        layout.addView(proceso);

        // LinearLayout para agrupar chipLabel y chipSpinner
        LinearLayout chipLayout = new LinearLayout(this);
        chipLayout.setOrientation(LinearLayout.HORIZONTAL);
        chipLayout.setId(View.generateViewId());
        layout.addView(chipLayout);

        // Etiqueta para seleccionar chip
        TextView chipLabel = new TextView(this);
        chipLabel.setText(getString(R.string.select_pic));
        chipLabel.setTextColor(Color.WHITE);
        chipLabel.setId(View.generateViewId());
        chipLayout.addView(chipLabel);

        // Spinner para selección de chip
        chipSpinner.setId(View.generateViewId());
        chipLayout.addView(chipSpinner);

        // ScrollView para datos leídos del chip
        ScrollView scrollView = new ScrollView(this);

        scrollView.setId(View.generateViewId());

        layout.addView(scrollView);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);

        // Crear background con bordes redondeados programáticamente
        GradientDrawable backgroundDrawable3 = new GradientDrawable();
        backgroundDrawable3.setCornerRadius(30f); // Bordes redondeados
        scrollContent.setBackground(backgroundDrawable3);

        // Agregar sombras en API 21 o superior (minSdkVersion >= 21)
        scrollContent.setElevation(8);

        // LayoutParams con márgenes
        LinearLayout.LayoutParams layoutParams3 =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams3.setMargins(16, 16, 16, 16);
        scrollContent.setLayoutParams(layoutParams3);

        scrollView.addView(scrollContent);

        // Etiquetas y datos para ROM
        TextView romLabel = new TextView(this);
        romLabel.setText(getString(R.string.rom_memory_data));
        romLabel.setTextColor(Color.GREEN);

        romLabel.setTextSize(16);

        romLabel.setTypeface(null, Typeface.BOLD);
        romLabel.setPadding(0, 16, 0, 8); // Espaciado superior

        scrollContent.addView(romLabel);

        scrollContent.addView(romData);

        // Etiquetas y datos para EEPROM
        TextView eepromLabel = new TextView(this);
        eepromLabel.setText(getString(R.string.eeprom_memory_data));
        eepromLabel.setTextColor(Color.GREEN);
        eepromLabel.setTextSize(16);

        eepromLabel.setTypeface(null, Typeface.BOLD);
        eepromLabel.setPadding(0, 16, 0, 8); // Espaciado superior

        scrollContent.addView(eepromLabel);

        scrollContent.addView(eepromData);

        //

        TextView ban = new TextView(this);
        ban.setId(View.generateViewId());
        ban.setText("");
        ban.setTextColor(Color.GREEN);
        ban.setTextSize(16);
        layout.addView(ban);

        //

        // Configurar el ConstraintLayout
        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(layout);

        // Título
        constraints.connect(
                toolbar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        constraints.connect(
                toolbar.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                0);
        constraints.connect(
                toolbar.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);

        // Mensaje
        constraints.connect(
                mensaje.getId(), ConstraintSet.TOP, toolbar.getId(), ConstraintSet.BOTTOM, 20);
        constraints.connect(
                mensaje.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                mensaje.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 20);

        // Botones
        constraints.connect(
                buttonLayout.getId(), ConstraintSet.TOP, mensaje.getId(), ConstraintSet.BOTTOM, 20);
        constraints.connect(
                buttonLayout.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                buttonLayout.getId(),
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                20);

        constraints.connect(
                proceso.getId(), ConstraintSet.TOP, buttonLayout.getId(), ConstraintSet.BOTTOM, 20);
        constraints.connect(
                proceso.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                proceso.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 20);

        // Layout para chipLabel y chipSpinner
        constraints.connect(
                chipLayout.getId(), ConstraintSet.TOP, proceso.getId(), ConstraintSet.BOTTOM, 20);
        constraints.connect(
                chipLayout.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                chipLayout.getId(),
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                20);

        // ScrollView
        constraints.connect(
                scrollView.getId(),
                ConstraintSet.TOP,
                chipLayout.getId(),
                ConstraintSet.BOTTOM,
                20);
        constraints.connect(
                scrollView.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                scrollView.getId(),
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                20);

        constraints.connect(
                scrollView.getId(), ConstraintSet.BOTTOM, ban.getId(), ConstraintSet.TOP, 20);
        // Ajustar la altura del ScrollView para que ocupe el espacio disponible
        constraints.constrainHeight(scrollView.getId(), 0); // MATCH_CONSTRAINT

        constraints.connect(
                ban.getId(),
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                20);
        constraints.connect(
                ban.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 20);
        constraints.connect(
                ban.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 20);

        constraints.applyTo(layout);

        disenoBannerDimencion.addView(publicidad.getBanner(), parametros);

        disenoBanner.addView(disenoBannerDimencion, parametrosBanner);

        disenoUI.addView(
                layout,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        disenoUI.addView(disenoBanner);

        setContentView(disenoUI);

        PowerManager powerManejador = (PowerManager) getSystemService(Context.POWER_SERVICE);

        wakeLock = powerManejador.newWakeLock(PowerManager.FULL_WAKE_LOCK, "GLGame");

        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    /**
     * Muestra información del chip PIC seleccionado y configura el protocolo.
     *
     * @param modelo Modelo del chip PIC seleccionado
     */
    private void mostrarInformacionPic(String modelo) {
        if (modelo == null || modelo.trim().isEmpty()) {
            LogManager.e(Categoria.UI, "mostrarInfoPic", "Modelo de chip es null o vacío");
            return;
        }
        
        try {
            LogManager.i(Categoria.UI, "mostrarInfoPic",
                        String.format("Mostrando información para chip: %s", modelo));

            chipPIC = chip.getChipEntry(modelo);
            if (chipPIC == null) {
                LogManager.e(Categoria.UI, "mostrarInfoPic",
                           String.format("No se encontró información para chip: %s", modelo));
                proceso.setText("Error: Chip no encontrado en base de datos");
                return;
            }

            String ubicacionPin = chipPIC.getUbicacionPin1DelPic();
            boolean esICSPSolamente = ubicacionPin.equals("null");

            if (!esICSPSolamente) {
                chipPIC.setActivarICSP(false);
                proceso.setText(getString(R.string.place_pic) + " " + ubicacionPin);
                LogManager.d(Categoria.UI, "mostrarInfoPic",
                           String.format("Chip %s configurado para socket: %s", modelo, ubicacionPin));
            } else {
                chipPIC.setActivarICSP(true);
                proceso.setText(getString(R.string.icsp_mode_only));
                LogManager.d(Categoria.UI, "mostrarInfoPic",
                           String.format("Chip %s configurado para modo ICSP únicamente", modelo));
            }

            // Inicializar variables de programación si hay protocolo disponible
            if (protocolo != null) {
                try {
                    boolean inicializado = protocolo.iniciarVariablesDeProgramacion(chipPIC);
                    if (inicializado) {
                        LogManager.d(Categoria.CHIP, "mostrarInfoPic",
                                   "Variables de programación inicializadas para " + modelo);
                    } else {
                        LogManager.w(Categoria.CHIP, "mostrarInfoPic",
                                   "Error inicializando variables de programación para " + modelo);
                    }
                } catch (Exception e) {
                    LogManager.e(Categoria.CHIP, "mostrarInfoPic",
                               "Error inicializando variables de programación", e);
                }
            } else {
                LogManager.w(Categoria.UI, "mostrarInfoPic",
                           "Protocolo no disponible, no se pueden inicializar variables de programación");
            }

            // Registrar configuración del chip para debugging
            LogManager.logConfiguracionChip(modelo, chipPIC.getVariablesDeProgramacion());

            Toast.makeText(getApplicationContext(),
                          String.format("Chip seleccionado: %s", modelo), Toast.LENGTH_LONG).show();
            
            LogManager.i(Categoria.UI, "mostrarInfoPic",
                        String.format("Información de chip %s mostrada exitosamente", modelo));

        } catch (Exception e) {
            LogManager.e(Categoria.UI, "mostrarInfoPic",
                        String.format("Error inesperado mostrando información de chip %s", modelo), e);
            proceso.setText("Error procesando información del chip");
            Toast.makeText(this, "Error procesando chip seleccionado", Toast.LENGTH_SHORT).show();
        }
    }

    private Button createIconButton(String text, @DrawableRes int iconRes) {
        Button button = new Button(this, null, android.R.attr.buttonStyle);
        button.setText(text);

        // Aplicar el nuevo fondo con animaciones y estados
        button.setBackgroundResource(R.drawable.button_background);

        // Asignar el nuevo icono profesional a la izquierda del texto
        button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        button.setCompoundDrawablePadding(dpToPx(12)); // Espacio entre icono y texto

        // Estilo del texto y padding
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false); // Letras en formato normal (ej: "Grabar" no "GRABAR")
        button.setPadding(dpToPx(18), dpToPx(10), dpToPx(18), dpToPx(10));

        // Configuración de Layout
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(10), 0, dpToPx(10), dpToPx(10)); // Márgenes laterales
        button.setLayoutParams(params);

        // La elevación es manejada por el estilo por defecto, pero puedes añadirla si quieres más
        // sombra
        // button.setElevation(dpToPx(4));

        return button;
    }

    private int dpToPx(int dp) {
        return (int)
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void displayData(LinearLayout container, String data, int groupSize, int columns) {
        int address = 0;
        StringBuilder formattedRow = new StringBuilder();

        for (int i = 0; i < data.length(); i += groupSize * columns) {
            // Obtener dirección en formato hexadecimal
            String addressHex = String.format("%04X", address);
            formattedRow.append(addressHex).append(": ");

            // Dividir los datos en grupos y columnas
            for (int j = 0; j < columns; j++) {
                int start = i + j * groupSize;
                int end = Math.min(start + groupSize, data.length());
                if (start < data.length()) {
                    formattedRow.append(data.substring(start, end)).append(" ");
                }
            }

            // Crear TextView para la fila y agregarla al contenedor
            TextView rowTextView = new TextView(this);
            rowTextView.setText(formattedRow.toString().trim());
            rowTextView.setTextSize(14);
            rowTextView.setTextColor(Color.WHITE);
            rowTextView.setPadding(16, 4, 16, 4);
            container.addView(rowTextView);

            // Incrementar la dirección y limpiar el formato de la fila
            address += 8;
            formattedRow.setLength(0);
        }
    }

    /**
     * Conecta al dispositivo USB programador con logging detallado y manejo robusto de errores.
     *
     * @param driver Driver USB del dispositivo programador
     */
    private void connectToDevice(UsbSerialDriver driver) {
        if (driver == null) {
            LogManager.e(Categoria.USB, "connectToDevice", "Driver USB es null");
            return;
        }
        
        long inicioOperacion = LogManager.logInicioOperacion(Categoria.USB, "conectarDispositivo");
        
        try {
            LogManager.i(Categoria.USB, "connectToDevice",
                        String.format("Iniciando conexión a dispositivo: %s",
                                    driver.getDevice().getProductName()));
            
            // Verificar que el dispositivo tiene puertos
            if (driver.getPorts().isEmpty()) {
                LogManager.e(Categoria.USB, "connectToDevice", "El dispositivo no tiene puertos disponibles");
                mostrarErrorConexion("Dispositivo sin puertos USB disponibles");
                LogManager.logFinOperacion(Categoria.USB, "conectarDispositivo", inicioOperacion, false);
                return;
            }
            
            // Seleccionar el primer puerto
            usbSerialPort = driver.getPorts().get(0);
            LogManager.d(Categoria.USB, "connectToDevice", "Puerto USB seleccionado");

            // Abrir conexión
            usbSerialPort.open(this.usbManager.openDevice(driver.getDevice()));
            LogManager.d(Categoria.USB, "connectToDevice", "Puerto USB abierto exitosamente");

            // Configurar parámetros del puerto serie
            usbSerialPort.setParameters(
                    19200, // Velocidad (baudios)
                    8, // Bits de datos
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE);
            
            LogManager.d(Categoria.USB, "connectToDevice",
                        "Puerto configurado: 19200 baud, 8N1");

            // Crear e inicializar protocolo
            protocolo = new ProtocoloP018(this, usbSerialPort);
            
            boolean protocoloIniciado = protocolo.iniciarProtocolo();
            if (!protocoloIniciado) {
                LogManager.w(Categoria.USB, "connectToDevice", "Protocolo no se pudo inicializar correctamente");
                mostrarErrorConexion("Error inicializando protocolo de comunicación");
                LogManager.logFinOperacion(Categoria.USB, "conectarDispositivo", inicioOperacion, false);
                return;
            }

            // Actualizar UI de conexión exitosa
            runOnUiThread(() -> {
                mensaje.setTextColor(Color.GREEN);
                mensaje.setText(getString(R.string.conectado));
            });

            LogManager.i(Categoria.USB, "connectToDevice", "Dispositivo conectado y protocolo iniciado exitosamente");
            LogManager.logFinOperacion(Categoria.USB, "conectarDispositivo", inicioOperacion, true);

        } catch (IOException e) {
            LogManager.e(Categoria.USB, "connectToDevice", "Error de I/O conectando dispositivo", e);
            
            // Mostrar error específico al usuario
            String mensajeError = "Error de conexión USB: " + e.getMessage();
            mostrarErrorConexion(mensajeError);
            
            // Limpiar estado de conexión
            limpiarEstadoConexion();
            
            LogManager.logFinOperacion(Categoria.USB, "conectarDispositivo", inicioOperacion, false);
            
        } catch (Exception e) {
            LogManager.e(Categoria.USB, "connectToDevice", "Error inesperado conectando dispositivo", e);
            
            mostrarErrorConexion("Error inesperado durante la conexión");
            limpiarEstadoConexion();
            
            LogManager.logFinOperacion(Categoria.USB, "conectarDispositivo", inicioOperacion, false);
        }
    }

    /**
     * Muestra un error de conexión al usuario de forma consistente.
     *
     * @param mensajeError Mensaje de error a mostrar
     */
    private void mostrarErrorConexion(String mensajeError) {
        runOnUiThread(() -> {
            mensaje.setTextColor(Color.RED);
            mensaje.setText(getString(R.string.desconectado));
            Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show();
        });
        
        LogManager.i(Categoria.UI, "mostrarError", "Error mostrado al usuario: " + mensajeError);
    }

    /**
     * Limpia el estado de conexión cuando ocurre un error.
     */
    private void limpiarEstadoConexion() {
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
                LogManager.d(Categoria.USB, "limpiarEstado", "Puerto USB cerrado");
            } catch (IOException e) {
                LogManager.w(Categoria.USB, "limpiarEstado", "Error cerrando puerto USB", e);
            }
            usbSerialPort = null;
        }
        
        protocolo = null;
        LogManager.d(Categoria.USB, "limpiarEstado", "Estado de conexión limpiado");
    }

    private void processSelectedFile(Uri uri) {
        String fileName = getFileName(uri); // Implementa esta función
        if (fileName.endsWith(".bin") || fileName.endsWith(".hex")) {
            // Procesa el archivo
            firware = readHexFile(uri);
        } else {
            Toast.makeText(this, getString(R.string.select_valid_binary_file), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Lee y procesa un archivo .hex seleccionado con logging detallado y validación.
     *
     * @param uri URI del archivo seleccionado
     * @return Contenido del archivo HEX como string, o string vacío si hay error
     */
    private String readHexFile(Uri uri) {
        if (uri == null) {
            LogManager.e(Categoria.UI, "readHexFile", "URI del archivo es null");
            Toast.makeText(this, getString(R.string.select_valid_binary_file), Toast.LENGTH_SHORT).show();
            return "";
        }

        long inicioOperacion = LogManager.logInicioOperacion(Categoria.UI, "leerArchivoHEX");
        String hexFileContent = "";
        String fileName = getFileName(uri);

        try {
            LogManager.i(Categoria.UI, "readHexFile",
                        String.format("Iniciando lectura de archivo: %s", fileName));

            // Validar extensión del archivo
            if (fileName == null || (!fileName.toLowerCase().endsWith(".hex") && !fileName.toLowerCase().endsWith(".bin"))) {
                String mensaje = String.format("Archivo con extensión inválida: %s", fileName);
                LogManager.w(Categoria.UI, "readHexFile", mensaje);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.select_valid_binary_file), Toast.LENGTH_SHORT).show();
                });
                
                LogManager.logFinOperacion(Categoria.UI, "leerArchivoHEX", inicioOperacion, false);
                return "";
            }

            // Leer contenido del archivo
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                LogManager.e(Categoria.UI, "readHexFile", "No se pudo abrir el InputStream del archivo");
                mostrarErrorLecturaArchivo("Error abriendo el archivo seleccionado");
                LogManager.logFinOperacion(Categoria.UI, "leerArchivoHEX", inicioOperacion, false);
                return "";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder fileContent = new StringBuilder();
            String line;
            int lineasLeidas = 0;

            while ((line = reader.readLine()) != null) {
                lineasLeidas++;
                
                // Parar en comentarios (líneas que empiezan con ';')
                if (line.length() > 0 && line.charAt(0) == ';') {
                    LogManager.v(Categoria.UI, "readHexFile",
                               String.format("Encontrado comentario en línea %d, finalizando lectura", lineasLeidas));
                    break;
                } else {
                    fileContent.append(line).append("\n");
                }
            }

            reader.close();
            inputStream.close();

            hexFileContent = fileContent.toString();
            
            // Validar que el archivo tiene contenido válido
            if (hexFileContent.trim().isEmpty()) {
                LogManager.w(Categoria.UI, "readHexFile", "Archivo HEX está vacío");
                mostrarErrorLecturaArchivo("El archivo seleccionado está vacío");
                LogManager.logFinOperacion(Categoria.UI, "leerArchivoHEX", inicioOperacion, false);
                return "";
            }

            // Habilitar botones después de cargar archivo exitosamente
            habilitarBotonesOperacion();

            LogManager.i(Categoria.UI, "readHexFile",
                        String.format("Archivo HEX cargado exitosamente: %s (%d líneas, %d caracteres)",
                                    fileName, lineasLeidas, hexFileContent.length()));
            LogManager.logFinOperacion(Categoria.UI, "leerArchivoHEX", inicioOperacion, true);

        } catch (IOException e) {
            LogManager.e(Categoria.UI, "readHexFile",
                        String.format("Error I/O leyendo archivo %s", fileName), e);
            mostrarErrorLecturaArchivo("Error leyendo el archivo: " + e.getMessage());
            LogManager.logFinOperacion(Categoria.UI, "leerArchivoHEX", inicioOperacion, false);
            
        } catch (SecurityException e) {
            LogManager.e(Categoria.UI, "readHexFile",
                        String.format("Error de permisos accediendo archivo %s", fileName), e);
            mostrarErrorLecturaArchivo("Permisos insuficientes para leer el archivo");
            LogManager.logFinOperacion(Categoria.UI, "leerArchivoHEX", inicioOperacion, false);
            
        } catch (Exception e) {
            LogManager.e(Categoria.UI, "readHexFile",
                        String.format("Error inesperado leyendo archivo %s", fileName), e);
            mostrarErrorLecturaArchivo("Error inesperado leyendo el archivo");
            LogManager.logFinOperacion(Categoria.UI, "leerArchivoHEX", inicioOperacion, false);
        }

        return hexFileContent;
    }

    /**
     * Muestra un error específico de lectura de archivo al usuario.
     *
     * @param mensajeError Mensaje de error detallado
     */
    private void mostrarErrorLecturaArchivo(String mensajeError) {
        runOnUiThread(() -> {
            Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show();
            proceso.setText("Error cargando archivo HEX");
        });
    }

    /**
     * Habilita todos los botones de operación después de cargar un archivo HEX.
     */
    private void habilitarBotonesOperacion() {
        runOnUiThread(() -> {
            btnProgramarPic.setEnabled(true);
            btnVerificarMemoriaDelPic.setEnabled(true);
            btnBorrarMemoriaDeLPic.setEnabled(true);
            btnLeerMemoriaDeLPic.setEnabled(true);
            btnDetectarPic.setEnabled(true);
            
            LogManager.d(Categoria.UI, "habilitarBotones", "Botones de operación habilitados");
        });
    }

    // Verifica permisos y abre el selector de archivos
    private void checkPermissionsAndOpenFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // En Android 13 no necesitas permisos adicionales para abrir archivos con
            // ACTION_OPEN_DOCUMENT
            openFilePicker();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6 a 12: Verifica y solicita permiso para leer almacenamiento
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION);
            } else {
                openFilePicker();
            }
        } else {
            // Para Android 5 o inferior
            openFilePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                // Manejar el caso donde el permiso es denegado (opcional)
            }
        }
    }

    // Abre el selector de archivos
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Permitir múltiples extensiones específicas
        String[] mimeTypes = {"application/octet-stream", "application/x-binary"};
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        filePickerLauncher.launch(intent);
    }

    // Manejo del resultado del selector de archivos
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                firware = readHexFile(uri);
                            }
                        }
                    });

    private String getFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            String name = cursor.getString(nameIndex);
            cursor.close();
            return name;
        }
        return null;
    }

    @Override
    protected void onPause() {

        publicidad.pausarBanner();

        super.onPause();

        wakeLock.release();
    }

    @Override
    protected void onResume() {

        super.onResume();

        publicidad.resumenBanner();

        wakeLock.acquire();
    }

    private void showAdPopup() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // Aumentar la altura del popup para dar más espacio al contenido y al anuncio
        int popupHeight = (int) (screenHeight * 0.85);

        LinearLayout popupContainer = new LinearLayout(this);
        popupContainer.setOrientation(LinearLayout.VERTICAL);
        popupContainer.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, popupHeight));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(40f);
        shape.setColor(Color.WHITE);
        popupContainer.setBackground(shape);

        // --- Contenido Superior Dinámico ---
        LinearLayout topContent = new LinearLayout(this);
        topContent.setOrientation(LinearLayout.VERTICAL);
        topContent.setGravity(Gravity.CENTER);
        // Usar WRAP_CONTENT para que ocupe solo el espacio necesario
        LinearLayout.LayoutParams topParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topContent.setLayoutParams(topParams);
        topContent.setPadding(32, 32, 32, 32);

        titleTextView = new TextView(this);
        titleTextView.setText(getString(R.string.grabando_pic));
        titleTextView.setTextSize(22);
        titleTextView.setTextColor(Color.BLACK);
        titleTextView.setGravity(Gravity.CENTER);
        topContent.addView(titleTextView);

        FrameLayout statusIndicatorContainer = new FrameLayout(this);
        statusIndicatorContainer.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams indicatorParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        indicatorParams.setMargins(0, 24, 0, 24);
        topContent.addView(statusIndicatorContainer, indicatorParams);

        statusProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
        statusProgressBar.setVisibility(View.VISIBLE);
        statusIndicatorContainer.addView(statusProgressBar);

        statusResultIcon = new ImageView(this);
        statusResultIcon.setVisibility(View.GONE);
        statusIndicatorContainer.addView(statusResultIcon);

        descriptionTextView = new TextView(this);
        descriptionTextView.setText(getString(R.string.espere_grabacion_pic));
        descriptionTextView.setTextSize(16);
        descriptionTextView.setTextColor(Color.DKGRAY);
        descriptionTextView.setGravity(Gravity.CENTER);
        topContent.addView(descriptionTextView);

        popupContainer.addView(topContent);

        View divider = new View(this);
        divider.setBackgroundColor(Color.LTGRAY);
        popupContainer.addView(
                divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));

        // --- Contenedor del Anuncio ---
        FrameLayout adContainer = new FrameLayout(this);
        // Asignar peso para que ocupe el espacio restante y flexible
        LinearLayout.LayoutParams adParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        adContainer.setLayoutParams(adParams);
        adContainer.setPadding(8, 8, 8, 8);
        // Asegurar una altura mínima para el contenedor del anuncio en dp
        adContainer.setMinimumHeight(dpToPx(250));
        adContainer.setId(View.generateViewId());
        popupContainer.addView(adContainer);

        // --- Contenedor para el Botón ---
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        buttonContainer.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        buttonContainer.setPadding(16, 16, 16, 16);

        actionButton = new Button(this);
        LinearLayout.LayoutParams buttonLayoutParam =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        actionButton.setLayoutParams(buttonLayoutParam);
        actionButton.setPadding(100, 40, 100, 40);
        actionButton.setTextColor(Color.WHITE);

        actionButton.setText(getString(R.string.cancelar));
        actionButton.setBackgroundResource(R.drawable.button_background_red);
        actionButton.setOnClickListener(
                v -> {
                    if (hiloGrabado != null && hiloGrabado.isAlive()) {
                        procesoCancelado = true;
                    }
                    publicidad.mostrarBanner();
                    dismissPopupWithSlideDown();
                });

        buttonContainer.addView(actionButton);
        popupContainer.addView(buttonContainer);

        popupWindow = new PopupWindow(popupContainer, (int) (screenWidth * 0.9), popupHeight, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(0);

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        applyCustomAnimation(popupContainer);
        loadNativeAd(adContainer);

        simularProcesoDeGrabado();
    }


    private void simularProcesoDeGrabado() {

        procesoCancelado = false;
        // Ejecutar en un hilo separado
        hiloGrabado =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (protocolo != null && !procesoCancelado) {

                                    try {

                                        // Paso 1: Borrar memorias (10% del progreso)
                                        procesoGrabado = protocolo.borrarMemoriasDelPic();

                                        runOnUiThread(
                                                () -> {
                                                    if (procesoGrabado) {
                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .memory_erased_successfully));
                                                    } else {
                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .memory_erase_error));
                                                    }
                                                });

                                        if (!procesoGrabado || procesoCancelado) {
                                            runOnUiThread(
                                                    () ->
                                                            actualizarUIProcesoFinalizado(
                                                                    procesoGrabado));

                                            return;
                                        }

                                    } catch (Exception e) {

                                        procesoGrabado = false;

                                        runOnUiThread(
                                                () -> {
                                                    if (!procesoGrabado) {

                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .memory_erase_error));
                                                    }
                                                });

                                        if (!procesoGrabado || procesoCancelado) {
                                            runOnUiThread(
                                                    () ->
                                                            actualizarUIProcesoFinalizado(
                                                                    procesoGrabado));

                                            return;
                                        }
                                    }

                                    try {

                                        // Paso 2: Programar ROM (30% del progreso)
                                        procesoGrabado =
                                                protocolo.programarMemoriaROMDelPic(
                                                        chipPIC, firware);

                                        runOnUiThread(
                                                () -> {
                                                    if (procesoGrabado) {
                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .rom_programmed_successfully));
                                                    } else {
                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .rom_program_error));
                                                    }
                                                });

                                        if (!procesoGrabado || procesoCancelado) {
                                            runOnUiThread(
                                                    () ->
                                                            actualizarUIProcesoFinalizado(
                                                                    procesoGrabado));

                                            return;
                                        }

                                    } catch (Exception e) {

                                        procesoGrabado = false;

                                        runOnUiThread(
                                                () -> {
                                                    if (!procesoGrabado) {

                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .rom_program_error));
                                                    }
                                                });

                                        if (!procesoGrabado || procesoCancelado) {
                                            runOnUiThread(
                                                    () ->
                                                            actualizarUIProcesoFinalizado(
                                                                    procesoGrabado));

                                            return;
                                        }
                                    }

                                    // Paso 3: Programar EEPROM si es necesario (20% del progreso)
                                    if (chipPIC.isTamanoValidoDeEEPROM()) {

                                        try {

                                            procesoGrabado =
                                                    protocolo.programarMemoriaEEPROMDelPic(
                                                            chipPIC, firware);

                                            runOnUiThread(
                                                    () -> {
                                                        if (procesoGrabado) {
                                                            proceso.setText(
                                                                    getString(
                                                                            R.string
                                                                                    .eeprom_programmed_successfully));
                                                        } else {
                                                            proceso.setText(
                                                                    getString(
                                                                            R.string
                                                                                    .eeprom_program_error));
                                                        }
                                                    });

                                            if (!procesoGrabado || procesoCancelado) {
                                                runOnUiThread(
                                                        () ->
                                                                actualizarUIProcesoFinalizado(
                                                                        procesoGrabado));

                                                return;
                                            }

                                        } catch (Exception e) {

                                            procesoGrabado = false;

                                            runOnUiThread(
                                                    () -> {
                                                        if (!procesoGrabado) {
                                                            proceso.setText(
                                                                    getString(
                                                                            R.string
                                                                                    .eeprom_program_error));
                                                        }
                                                    });

                                            if (!procesoGrabado || procesoCancelado) {
                                                runOnUiThread(
                                                        () ->
                                                                actualizarUIProcesoFinalizado(
                                                                        procesoGrabado));

                                                return;
                                            }
                                        }
                                    }

                                    try {

                                        // Paso 4: Programar Fuses (20% del progreso)
                                        procesoGrabado =
                                                protocolo.programarFusesIDDelPic(chipPIC, firware);
                                        runOnUiThread(
                                                () -> {
                                                    if (procesoGrabado) {
                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .fuses_programmed_successfully));
                                                    } else {
                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .fuses_program_error));
                                                    }
                                                });

                                        if (!procesoGrabado || procesoCancelado) {
                                            runOnUiThread(
                                                    () ->
                                                            actualizarUIProcesoFinalizado(
                                                                    procesoGrabado));

                                            return;
                                        }

                                    } catch (Exception e) {

                                        procesoGrabado = false;

                                        runOnUiThread(
                                                () -> {
                                                    if (!procesoGrabado) {
                                                        proceso.setText(
                                                                getString(
                                                                        R.string
                                                                                .fuses_program_error));
                                                    }
                                                });

                                        if (!procesoGrabado || procesoCancelado) {
                                            runOnUiThread(
                                                    () ->
                                                            actualizarUIProcesoFinalizado(
                                                                    procesoGrabado));

                                            return;
                                        }
                                    }

                                    // Paso 5: Programar Fuses adicionales para PIC18F (10% del
                                    // progreso)
                                    if (chipPIC.getTipoDeNucleoBit() == 16) {

                                        try {

                                            procesoGrabado = protocolo.programarFusesDePics18F();

                                            runOnUiThread(
                                                    () -> {
                                                        if (procesoGrabado) {
                                                            proceso.setText(
                                                                    getString(
                                                                            R.string
                                                                                    .fuses_18f_programmed_successfully));
                                                        } else {
                                                            proceso.setText(
                                                                    getString(
                                                                            R.string
                                                                                    .fuses_18f_program_error));
                                                        }
                                                    });

                                            if (!procesoGrabado || procesoCancelado) {
                                                runOnUiThread(
                                                        () ->
                                                                actualizarUIProcesoFinalizado(
                                                                        procesoGrabado));

                                                return;
                                            }

                                        } catch (Exception e) {

                                            procesoGrabado = false;

                                            runOnUiThread(
                                                    () -> {
                                                        if (!procesoGrabado) {
                                                            proceso.setText(
                                                                    getString(
                                                                            R.string
                                                                                    .fuses_18f_program_error));
                                                        }
                                                    });

                                            if (!procesoGrabado || procesoCancelado) {
                                                runOnUiThread(
                                                        () ->
                                                                actualizarUIProcesoFinalizado(
                                                                        procesoGrabado));

                                                return;
                                            }
                                        }
                                    }

                                    // Completado (100%)
                                    runOnUiThread(
                                            () -> {
                                                if (procesoGrabado) {
                                                    proceso.setText(
                                                            getString(
                                                                    R.string
                                                                            .pic_programmed_successfully));
                                                } else {
                                                    proceso.setText(
                                                            getString(R.string.pic_program_error));
                                                }
                                            });

                                    if (!procesoGrabado || procesoCancelado) {
                                        runOnUiThread(
                                                () ->
                                                        actualizarUIProcesoFinalizado(
                                                                procesoGrabado));

                                        return;
                                    }

                                    if (procesoGrabado || !procesoCancelado) {
                                        runOnUiThread(
                                                () ->
                                                        actualizarUIProcesoFinalizado(
                                                                procesoGrabado));

                                        return;
                                    }
                                }
                            }
                        });

        hiloGrabado.start();
    }

    private void actualizarUIProcesoFinalizado(boolean exito) {
        // Ocultar barra de progreso y mostrar icono de resultado
        statusProgressBar.setVisibility(View.GONE);
        statusResultIcon.setVisibility(View.VISIBLE);

        // Permitir que el popup se cierre tocando fuera
        popupWindow.setOutsideTouchable(true);

        if (exito) {
            // Estado de ÉXITO
            titleTextView.setText(getString(R.string.grabacion_completada_pic));
            descriptionTextView.setText(getString(R.string.grabacion_correcta_pic));
            statusResultIcon.setImageResource(R.drawable.ic_status_success);
            Drawable successDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
            DrawableCompat.setTint(successDrawable, Color.parseColor("#4CAF50")); // Verde
        } else {
            // Estado de FALLO
            titleTextView.setText(getString(R.string.fallo_grabacion_pic));
            descriptionTextView.setText(getString(R.string.proceso_no_completado));
            statusResultIcon.setImageResource(R.drawable.ic_status_failure);
            Drawable failureDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
            DrawableCompat.setTint(failureDrawable, Color.parseColor("#D32F2F")); // Rojo
        }

        // Actualizar el botón al estado "Aceptar"
        actionButton.setText(getString(R.string.aceptar));
        actionButton.setBackgroundResource(R.drawable.button_background_blue);
        // El nuevo OnClickListener simplemente cierra el popup
        actionButton.setOnClickListener(
                v -> {
                    publicidad.mostrarBanner();
                    dismissPopupWithSlideDown();
                });
    }

    private void dismissPopupWithSlideDown() {
        if (popupWindow != null && popupWindow.isShowing()) {
            final View popupView = popupWindow.getContentView();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int currentScreenHeight = displayMetrics.heightPixels;
            float startTranslationY = popupView.getTranslationY();
            float targetTranslationY = currentScreenHeight - popupView.getTop();
            ValueAnimator animator = ValueAnimator.ofFloat(startTranslationY, targetTranslationY);
            animator.setDuration(600);
            animator.setInterpolator(new AccelerateInterpolator(1.5f));
            animator.addUpdateListener(
                    animation -> {
                        float value = (float) animation.getAnimatedValue();
                        popupView.setTranslationY(value);
                    });
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            popupWindow.dismiss();
                            if (nativeAd != null) {
                                nativeAd.destroy();
                                nativeAd = null;
                            }
                        }
                    });
            animator.start();
        }
    }

    private void applyCustomAnimation(View popupView) {
        popupView.setScaleY(0);
        popupView.setPivotY(0);
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0f, 1f);
        scaleAnimator.setDuration(1000);
        scaleAnimator.setInterpolator(new BounceInterpolator());
        scaleAnimator.addUpdateListener(
                animation -> {
                    float value = (float) animation.getAnimatedValue();
                    popupView.setScaleY(value);
                });
        scaleAnimator.start();

        popupWindow.setTouchInterceptor(
                (v, event) -> {
                    // Este interceptor ahora solo funciona si outsideTouchable es true
                    if (popupWindow.isOutsideTouchable()
                            && !isPointInsideView(
                                    event.getRawX(),
                                    event.getRawY(),
                                    popupWindow.getContentView())) {
                        dismissPopupWithSlideDown();
                        return true;
                    }
                    return false;
                });
    }

    private boolean isPointInsideView(float x, float y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        return (x >= viewX && x <= (viewX + view.getWidth()))
                && (y >= viewY && y <= (viewY + view.getHeight()));
    }

    private void loadNativeAd(FrameLayout adContainer) {
        AdLoader.Builder builder =
                new AdLoader.Builder(this, "ca-app-pub-5141499161332805/4642845838");

        builder.forNativeAd(
                loadedNativeAd -> {
                    adContainer.removeAllViews();
                    if (this.nativeAd != null) {
                        this.nativeAd.destroy();
                    }
                    this.nativeAd = loadedNativeAd;

                    // --- 1. CREAR TODOS LOS ELEMENTOS VISUALES ---

                    // El contenedor raíz del anuncio. Todo debe ir dentro de este.
                    NativeAdView adView = new NativeAdView(this);
                    adView.setLayoutParams(
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT));

                    // Layout interno para organizar los elementos verticalmente
                    LinearLayout adInternalLayout = new LinearLayout(this);
                    adInternalLayout.setOrientation(LinearLayout.VERTICAL);
                    adInternalLayout.setLayoutParams(
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT));
                    adInternalLayout.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
                    adInternalLayout.setGravity(Gravity.CENTER_HORIZONTAL);

                    // Etiqueta "Ad"
                    TextView adBadge = new TextView(this);
                    adBadge.setText("Ad");
                    adBadge.setTextSize(10);
                    adBadge.setTextColor(Color.WHITE);
                    GradientDrawable adBadgeBackground = new GradientDrawable();
                    adBadgeBackground.setShape(GradientDrawable.RECTANGLE);
                    adBadgeBackground.setColor(Color.parseColor("#FFCC66"));
                    adBadgeBackground.setCornerRadius(8f);
                    adBadge.setBackground(adBadgeBackground);
                    adBadge.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

                    // Fila para el ícono y el título (horizontal)
                    LinearLayout headerRow = new LinearLayout(this);
                    headerRow.setOrientation(LinearLayout.HORIZONTAL);
                    headerRow.setGravity(Gravity.CENTER_VERTICAL);
                    headerRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                    ImageView iconView = new ImageView(this);
                    if (nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                        iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
                        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
                        iconParams.setMarginEnd(dpToPx(8));
                        iconView.setLayoutParams(iconParams);
                    } else {
                        iconView.setVisibility(View.GONE);
                    }

                    TextView headlineView = new TextView(this);
                    headlineView.setText(nativeAd.getHeadline());
                    headlineView.setTextSize(16);
                    headlineView.setTypeface(null, Typeface.BOLD);
                    headlineView.setTextColor(Color.BLACK);

                    TextView bodyView = new TextView(this);
                    bodyView.setText(nativeAd.getBody());
                    bodyView.setTextSize(14);
                    bodyView.setTextColor(Color.DKGRAY);
                    LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    bodyParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
                    bodyView.setLayoutParams(bodyParams);

                    MediaView mediaView = new MediaView(this);
                    LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
                    mediaParams.gravity = Gravity.CENTER_HORIZONTAL;
                    mediaView.setLayoutParams(mediaParams);

                    Button callToAction = new Button(this);
                    callToAction.setText(nativeAd.getCallToAction());
                    callToAction.setAllCaps(false);
                    callToAction.setTextColor(Color.WHITE);
                    callToAction.setBackgroundColor(Color.parseColor("#4CAF50"));
                    LinearLayout.LayoutParams ctaParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    ctaParams.setMargins(0, dpToPx(8), 0, 0);
                    callToAction.setLayoutParams(ctaParams);

                    AdChoicesView adChoicesView = new AdChoicesView(this);
                    FrameLayout.LayoutParams adChoicesParams = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT);
                    adChoicesParams.gravity = Gravity.TOP | Gravity.END;

                    // --- 2. CONSTRUIR LA JERARQUÍA DE VISTAS (AÑADIR VISTAS A SUS PADRES) ---
                    headerRow.addView(iconView);
                    headerRow.addView(headlineView);

                    LinearLayout.LayoutParams adBadgeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    adBadgeParams.setMargins(0, 0, 0, dpToPx(8));
                    adBadgeParams.gravity = Gravity.START;
                    adInternalLayout.addView(adBadge, adBadgeParams);
                    adInternalLayout.addView(headerRow);
                    adInternalLayout.addView(bodyView);
                    adInternalLayout.addView(mediaView);
                    adInternalLayout.addView(callToAction);

                    adView.addView(adInternalLayout);
                    adView.addView(adChoicesView, adChoicesParams);

                    // --- 3. REGISTRAR LAS VISTAS (SOLO DESPUÉS DE QUE ESTÉN EN LA JERARQUÍA) ---
                    adView.setHeadlineView(headlineView);
                    adView.setBodyView(bodyView);
                    adView.setCallToActionView(callToAction);
                    adView.setIconView(iconView);
                    adView.setMediaView(mediaView);
                    adView.setAdChoicesView(adChoicesView);

                    // --- 4. ASIGNAR EL ANUNCIO Y AÑADIR AL CONTENEDOR PRINCIPAL ---
                    adView.setNativeAd(nativeAd);
                    adContainer.addView(adView);
                });


        AdListener adListener =
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        adContainer.setVisibility(View.GONE);
                    }
                };
        builder.withAdListener(adListener);

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions =
                new NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setVideoOptions(videoOptions)
                        .build();
        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    @Override
    protected void onDestroy() {
        LogManager.i(Categoria.UI, "onDestroy", "Iniciando limpieza de recursos de la aplicación");
        
        try {
            // Limpiar publicidad
            if (publicidad != null) {
                publicidad.disposeBanner();
                LogManager.d(Categoria.UI, "onDestroy", "Banner de publicidad limpiado");
            }

            // Limpiar anuncios nativos
            if (nativeAd != null) {
                nativeAd.destroy();
                nativeAd = null;
                LogManager.d(Categoria.UI, "onDestroy", "Anuncio nativo destruido");
            }
            
            // Cerrar popup si está abierto
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
                LogManager.d(Categoria.UI, "onDestroy", "Popup cerrado");
            }

            super.onDestroy();

            // Detener manager de I/O
            if (ioManager != null) {
                ioManager.stop();
                LogManager.d(Categoria.USB, "onDestroy", "I/O Manager detenido");
            }

            // Cerrar puerto USB de forma segura
            if (usbSerialPort != null) {
                try {
                    usbSerialPort.close();
                    LogManager.d(Categoria.USB, "onDestroy", "Puerto USB cerrado exitosamente");
                } catch (IOException e) {
                    LogManager.w(Categoria.USB, "onDestroy", "Error cerrando puerto USB", e);
                }
                usbSerialPort = null;
            }

            // Detener executor service
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
                LogManager.d(Categoria.SISTEMA, "onDestroy", "ExecutorService detenido");
            }

            // Cancelar hilo de grabado si está activo
            if (hiloGrabado != null && hiloGrabado.isAlive()) {
                procesoCancelado = true;
                try {
                    hiloGrabado.interrupt();
                    LogManager.d(Categoria.SISTEMA, "onDestroy", "Hilo de grabado cancelado");
                } catch (Exception e) {
                    LogManager.w(Categoria.SISTEMA, "onDestroy", "Error cancelando hilo de grabado", e);
                }
            }

            // Desregistrar receiver USB
            try {
                unregisterReceiver(usbReceiver);
                LogManager.d(Categoria.USB, "onDestroy", "USB receiver desregistrado");
            } catch (IllegalArgumentException e) {
                LogManager.w(Categoria.USB, "onDestroy", "USB receiver ya estaba desregistrado", e);
            }

            LogManager.i(Categoria.UI, "onDestroy", "Limpieza de recursos completada exitosamente");
            
        } catch (Exception e) {
            LogManager.e(Categoria.UI, "onDestroy", "Error durante limpieza de recursos", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem menuInicio = menu.add(Menu.NONE, 1, 1, getString(R.string.obtener_modelo));

        MenuItem menuProductos = menu.add(Menu.NONE, 2, 2, getString(R.string.obtener_protocolo));

        MenuItem menuServicios = menu.add(Menu.NONE, 3, 3, getString(R.string.privacy_policy));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Manejar eventos del menú
        switch (item.getItemId()) {
            case 1:
                if (protocolo != null) {

                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(getString(R.string.modelo_programador)); // set Title
                    builder.setMessage(
                            "" + protocolo.obtenerVersionOModeloDelProgramador()); // set message
                    builder.setCancelable(true); //  Sets whether the dialog is cancelable or not
                    builder.setIcon(R.mipmap.ic_launcher);

                    builder.setPositiveButton(
                            getString(R.string.aceptar),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Replace your Own Action

                                    // Cancel the AlertDialog
                                    alertDialog.cancel();
                                }
                            });

                    alertDialog = builder.create();
                    alertDialog.show();
                }
                return true;
            case 2:
                if (protocolo != null) {

                    builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setTitle(getString(R.string.protocolo_programador)); // set Title
                    builder1.setMessage(
                            "" + protocolo.obtenerProtocoloDelProgramador()); // set message
                    builder1.setCancelable(true); //  Sets whether the dialog is cancelable or not
                    builder1.setIcon(R.mipmap.ic_launcher);

                    builder1.setPositiveButton(
                            getString(R.string.aceptar),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Replace your Own Action

                                    // Cancel the AlertDialog
                                    alertDialog1.cancel();
                                }
                            });

                    alertDialog1 = builder1.create();
                    alertDialog1.show();
                }

                return true;
            case 3:
                Intent nuevaActividad = new Intent(MainActivity.this, Politicas.class);

                startActivity(nuevaActividad);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {

        // add event for back button pressed
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);
        return true;
    }
}
