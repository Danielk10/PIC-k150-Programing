package com.diamon.pic;

import android.Manifest;
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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
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

import com.diamon.chip.ChipPic;
import com.diamon.datos.ChipinfoReader;
import com.diamon.politicas.Politicas;
import com.diamon.protocolo.ProtocoloP018;
import com.diamon.publicidad.MostrarPublicidad;
import com.diamon.utilidad.Recurso;
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

    private ProgressBar progressBar;

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

        AppCenter.start(
                getApplication(),
                "c9a1ef1a-bbfb-443a-863e-1c1d77e49c18",
                Analytics.class,
                Crashes.class);

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

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);

        romData = new LinearLayout(this);

        romData.setOrientation(LinearLayout.VERTICAL);

        romData.setPadding(24, 24, 24, 24);

        // Crear background con bordes redondeados programáticamente
        GradientDrawable backgroundDrawable1 = new GradientDrawable();
        backgroundDrawable1.setColor(Color.parseColor("#1E1E2C")); // Color de fondo de la tarjeta
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

        eepromData.setPadding(24, 24, 24, 24);

        // Crear background con bordes redondeados programáticamente
        GradientDrawable backgroundDrawable2 = new GradientDrawable();
        backgroundDrawable2.setColor(Color.parseColor("#1E1E2C")); // Color de fondo de la tarjeta
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

        btnProgramarPic = createIconButton(getString(R.string.program_pic), R.drawable.ic_chip);

        btnProgramarPic.setEnabled(false);

        btnVerificarMemoriaDelPic =
                createIconButton(getString(R.string.verify_memory_erased), R.drawable.ic_chip);

        btnVerificarMemoriaDelPic.setEnabled(false);

        btnBorrarMemoriaDeLPic =
                createIconButton(getString(R.string.erase_memory), R.drawable.ic_chip);

        btnBorrarMemoriaDeLPic.setEnabled(false);

        btnLeerMemoriaDeLPic =
                createIconButton(getString(R.string.read_memory), R.drawable.ic_chip);

        btnLeerMemoriaDeLPic.setEnabled(false);

        btnDetectarPic = createIconButton(getString(R.string.detect_pic), R.drawable.ic_chip);

        btnDetectarPic.setEnabled(false);

        btnSelectHex = createIconButton(getString(R.string.load_hex_file), R.drawable.ic_flash);

        Spinner chipSpinner = new Spinner(this);

        // Spinner chipSpinner =  createChipSpinner();

        btnProgramarPic.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

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
                });

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
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
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
        // layout.setBackgroundColor(Color.parseColor("#B5651D")); // Fondo oscuro profesional

        layout.addView(toolbar, toolbarParams);

        mensaje.setText("Desconectado");
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

        // LinearLayout para barra de progreso y etiqueta
        LinearLayout progressLayout = new LinearLayout(this);
        progressLayout.setOrientation(LinearLayout.HORIZONTAL);
        progressLayout.setId(View.generateViewId());
        progressLayout.setGravity(Gravity.CENTER_VERTICAL);
        layout.addView(progressLayout);

        // Etiqueta para la barra de progreso
        TextView progressLabel = new TextView(this);
        progressLabel.setText("");
        progressLabel.setTextColor(Color.WHITE);
        progressLabel.setId(View.generateViewId());
        progressLayout.addView(progressLabel);

        // Barra de progreso
        progressBar.setId(View.generateViewId());
        progressBar.setProgress(0); // Valor inicial para ser visible
        progressBar.setMax(0); // Máximo para pruebas
        progressBar.setLayoutParams(
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        progressLayout.addView(progressBar);

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

        // romData.setBackgroundColor(Color.WHITE); // Fondo blanco para destacar
        scrollContent.addView(romData);

        // Etiquetas y datos para EEPROM
        TextView eepromLabel = new TextView(this);
        eepromLabel.setText(getString(R.string.eeprom_memory_data));
        eepromLabel.setTextColor(Color.GREEN);
        eepromLabel.setTextSize(16);

        eepromLabel.setTypeface(null, Typeface.BOLD);
        eepromLabel.setPadding(0, 16, 0, 8); // Espaciado superior

        scrollContent.addView(eepromLabel);

        // eepromData.setBackgroundColor(Color.WHITE); // Fondo blanco para destacar
        scrollContent.addView(eepromData);

        TextView ban = new TextView(this);
        ban.setId(View.generateViewId());
        ban.setText("");
        ban.setTextColor(Color.GREEN);
        ban.setTextSize(16);
        layout.addView(ban);

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

        // Layout para barra de progreso y etiqueta
        constraints.connect(
                progressLayout.getId(),
                ConstraintSet.TOP,
                chipLayout.getId(),
                ConstraintSet.BOTTOM,
                20);
        constraints.connect(
                progressLayout.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                progressLayout.getId(),
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                20);

        // ScrollView
        constraints.connect(
                scrollView.getId(),
                ConstraintSet.TOP,
                progressLayout.getId(),
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

    private void mostrarInformacionPic(String modelo) {

        chipPIC = chip.getChipEntry(modelo);

        String resuesta = chipPIC.getUbicacionPin1DelPic();

        if (!resuesta.equals("null")) {

            chipPIC.setActivarICSP(false);

            proceso.setText(getString(R.string.place_pic) + " " + resuesta);

        } else {

            chipPIC.setActivarICSP(true);

            proceso.setText(getString(R.string.icsp_mode_only));
        }

        if (protocolo != null) {

            protocolo.iniciarVariablesDeProgramacion(chipPIC);
        }

        Toast.makeText(getApplicationContext(), modelo, Toast.LENGTH_LONG).show();
    }

    ///////

    // Crear layout principal programáticamente
    private void createMainLayout() {
        // Layout principal
        layout = new ConstraintLayout(this);
        layout.setBackgroundColor(Color.parseColor("#1A1A2E"));
        layout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Toolbar
        Toolbar toolbar = createToolbar();
        layout.addView(toolbar);

        // Contenedor de botones
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setId(View.generateViewId());
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setGravity(Gravity.CENTER);
        layout.addView(buttonContainer);

        // Configurar constraints
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);

        // Posicionar toolbar
        set.connect(toolbar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        set.connect(
                toolbar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        set.connect(toolbar.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

        // Posicionar contenedor de botones
        set.connect(
                buttonContainer.getId(),
                ConstraintSet.TOP,
                toolbar.getId(),
                ConstraintSet.BOTTOM,
                dpToPx(24));
        set.connect(
                buttonContainer.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START);
        set.connect(
                buttonContainer.getId(),
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END);

        // Añadir botones al contenedor
        btnSelectHex = createIconButton("Cargar HEX", R.drawable.ic_flash);
        buttonContainer.addView(btnSelectHex);

        btnDetectarPic = createIconButton("Detectar PIC", R.drawable.ic_chip);
        buttonContainer.addView(btnDetectarPic);

        // Añadir más botones...

        // Aplicar constraints
        set.applyTo(layout);

        setContentView(layout);
    }

    private Toolbar createToolbar() {
        Toolbar toolbar = new Toolbar(this);
        toolbar.setId(View.generateViewId());
        toolbar.setTitle("PIC K150 Programmer");
        toolbar.setSubtitle("Conectado vía USB OTG");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.LTGRAY);
        toolbar.setBackgroundColor(Color.parseColor("#003366"));
        toolbar.setLogo(R.drawable.ic_pic_logo);
        toolbar.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        toolbar.setElevation(dpToPx(8));

        // Configurar logo y título
        toolbar.setContentInsetsRelative(dpToPx(72), dpToPx(16));

        return toolbar;
    }

    private Button createIconButton(String text, @DrawableRes int iconRes) {
        Button button = new Button(this);
        button.setText(text);
        button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        button.setCompoundDrawablePadding(dpToPx(8));

        // Estilo del botón
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(24));
        shape.setColor(Color.parseColor("#FF6600"));
        shape.setStroke(dpToPx(2), Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            button.setBackground(shape);
        } else {
            button.setBackgroundDrawable(shape);
        }

        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        button.setElevation(dpToPx(4));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(16));
        button.setLayoutParams(params);

        return button;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private LinearLayout createDataCardView(String title, String data) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        // Fondo de la tarjeta
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2A2A3E"));
        bg.setCornerRadius(dpToPx(12));
        card.setBackground(bg);
        card.setElevation(dpToPx(4));
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Título
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.parseColor("#FF6600"));
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        card.addView(titleView);

        // Divisor
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#FF6600"));
        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        dividerParams.setMargins(0, dpToPx(8), 0, dpToPx(12));
        divider.setLayoutParams(dividerParams);
        card.addView(divider);

        // Datos (con scroll)
        ScrollView scrollView = new ScrollView(this);
        TextView dataView = new TextView(this);
        dataView.setText(data);
        dataView.setTextColor(Color.WHITE);
        dataView.setTypeface(Typeface.MONOSPACE);
        dataView.setTextSize(14);
        scrollView.addView(dataView);
        card.addView(scrollView);

        return card;
    }

    private Spinner createChipSpinner() {
        Spinner spinner = new Spinner(this);

        // Fondo del spinner
        GradientDrawable spinnerBg = new GradientDrawable();
        spinnerBg.setColor(Color.TRANSPARENT);
        spinnerBg.setCornerRadius(dpToPx(16));
        spinnerBg.setStroke(dpToPx(2), Color.parseColor("#FF6600"));
        spinner.setBackground(spinnerBg);

        // Adapter personalizado
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        chip.getModelosPic().toArray(new String[0])) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        view.setTextColor(Color.WHITE);
                        view.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        TextView view =
                                (TextView) super.getDropDownView(position, convertView, parent);
                        view.setTextColor(Color.BLACK);
                        view.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
                        return view;
                    }
                };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        return spinner;
    }

    ////////

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

    private void connectToDevice(UsbSerialDriver driver) {

        try {
            usbSerialPort = driver.getPorts().get(0); // Seleccionar el primer puerto

            usbSerialPort.open(this.usbManager.openDevice(driver.getDevice()));

            // Configurar el puerto
            usbSerialPort.setParameters(
                    19200, // Velocidad (baudios)
                    8, // Bits de datos
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE);

            protocolo = new ProtocoloP018(this, usbSerialPort);

            protocolo.iniciarProtocolo();

            mensaje.setTextColor(Color.GREEN);

            mensaje.setText("Conectado");

        } catch (IOException e) {

            // Toast.makeText(this, getString(R.string.connection_error),
            // Toast.LENGTH_SHORT).show();
        }
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

    // Lee el archivo .hex seleccionado
    private String readHexFile(Uri uri) {

        String hexFileContent = "";

        String fileName = getFileName(uri); // Implementa esta función

        if (fileName.endsWith(".hex") || fileName.endsWith(".HEX")) {

            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder fileContent = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {

                    if (("" + line.charAt(0)).equals(";")) {

                        break;

                    } else {

                        fileContent.append(line).append("\n");
                    }
                }

                reader.close();

                hexFileContent = fileContent.toString();

                btnProgramarPic.setEnabled(true);

                btnVerificarMemoriaDelPic.setEnabled(true);

                btnBorrarMemoriaDeLPic.setEnabled(true);

                btnLeerMemoriaDeLPic.setEnabled(true);

                btnDetectarPic.setEnabled(true);

            } catch (Exception e) {
            }

        } else {
            Toast.makeText(this, "Selecciona un archivo binario válido", Toast.LENGTH_SHORT).show();
        }

        return hexFileContent;
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

    @Override
    protected void onDestroy() {

        publicidad.disposeBanner();

        super.onDestroy();

        if (ioManager != null) {
            ioManager.stop();
        }

        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
            } catch (IOException e) {
            }
        }

        executorService.shutdownNow(); // Detener inmediatamente

        unregisterReceiver(usbReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem menuInicio = menu.add(Menu.NONE, 1, 1, "Obtener Modelo del Programador");

        MenuItem menuProductos = menu.add(Menu.NONE, 2, 2, "Obtener Protocolo");

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
                    builder.setTitle("Modelo del Programador"); // set Title
                    builder.setMessage(
                            "" + protocolo.obtenerVersionOModeloDelProgramador()); // set message
                    builder.setCancelable(true); //  Sets whether the dialog is cancelable or not
                    builder.setIcon(R.mipmap.ic_launcher);

                    builder.setPositiveButton(
                            "Aceptar",
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
                    builder1.setTitle("Protocolo del Programador"); // set Title
                    builder1.setMessage(
                            "" + protocolo.obtenerProtocoloDelProgramador()); // set message
                    builder1.setCancelable(true); //  Sets whether the dialog is cancelable or not
                    builder1.setIcon(R.mipmap.ic_launcher);

                    builder1.setPositiveButton(
                            "Aceptar",
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
