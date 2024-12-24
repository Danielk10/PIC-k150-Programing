package com.diamon.pic;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.ProgressBar;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;

import com.diamon.chip.ChipPic;
import com.diamon.datos.ChipinfoReader;
import com.diamon.protocolo.ProtocoloP018;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements Runnable {

    private static final String ACTION_USB_PERMISSION = "com.example.USB_PERMISSION";

    private static final int REQUEST_CODE_OPEN_FILE = 1;

    private static final int REQUEST_CODE_PERMISSION = 2;

    private UsbSerialPort usbSerialPort;

    private List<UsbSerialDriver> drivers;

    private List<UsbSerialDriver> driversP;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager ioManager;

    private UsbManager usbManager;

    private Thread hilo;

    private boolean conectado;

    private volatile boolean iniciar;

    private ConstraintLayout layout;

    private Button btnProgramarPic;

    private Button btnVerificarMemoriaDelPic;

    private Button btnBorrarMemoriaDeLPic;

    private Button btnLeerMemoriaDeLPic;

    private Button btnDetectarPic;

    private Button btnSelectHex;

    private Button privacyPolicyButton;

    private ProgressBar progressBar;

    private TextView romData;

    private TextView eepromData;

    private TextView mensaje;

    private TextView proceso;

    private String firware;

    private ChipPic chipPIC;

    private ChipinfoReader chip;

    private ProtocoloP018 protocolo;


    private final BroadcastReceiver usbReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null
                                && intent.getBooleanExtra(
                                        UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            for (UsbSerialDriver driver : drivers) {
                                if (driver.getDevice().equals(device)) {
                                    connectToDevice(driver);

                                    break;
                                }
                            }
                        } else {

                        }
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firware = new String();

        driversP = new ArrayList<UsbSerialDriver>();

        layout = new ConstraintLayout(this);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);

        romData = new TextView(this);

        eepromData = new TextView(this);

        proceso = new TextView(this);

        mensaje = new TextView(this);

        btnProgramarPic = new Button(this);

        btnProgramarPic.setText("Programar PIC");

        btnProgramarPic.setPadding(40, 20, 40, 20);

        btnProgramarPic.setEnabled(false);

        btnVerificarMemoriaDelPic = new Button(this);

        btnVerificarMemoriaDelPic.setText("Verificar si la memoria del PIC está borrada");

        btnVerificarMemoriaDelPic.setPadding(40, 20, 40, 20);

        btnVerificarMemoriaDelPic.setEnabled(false);

        btnBorrarMemoriaDeLPic = new Button(this);

        btnBorrarMemoriaDeLPic.setText("Borrar Memoria");

        btnBorrarMemoriaDeLPic.setPadding(40, 20, 40, 20);

        btnBorrarMemoriaDeLPic.setEnabled(false);

        btnLeerMemoriaDeLPic = new Button(this);

        btnLeerMemoriaDeLPic.setText("Leer Memoria del PIC");

        btnLeerMemoriaDeLPic.setPadding(40, 20, 40, 20);

        btnLeerMemoriaDeLPic.setEnabled(false);

        btnDetectarPic = new Button(this);

        btnDetectarPic.setText("Detectar PIC en Socket");

        btnDetectarPic.setPadding(40, 20, 40, 20);

        btnDetectarPic.setEnabled(false);

        btnSelectHex = new Button(this);

        btnSelectHex.setText("Cargar Archivo Hex");

        btnSelectHex.setPadding(40, 20, 40, 20);

        privacyPolicyButton = new Button(this);

        Spinner chipSpinner = new Spinner(this);

        btnProgramarPic.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (protocolo != null) {

                            boolean respuesta =
                                    protocolo.programarMemoriaROMDelPic(chipPIC, firware);

                            if (respuesta) {

                                proceso.setText("Memoria ROM Programada Exitosamente");

                            } else {

                                proceso.setText("Error al Programar Memoria ROM del PIC");

                                return;
                            }

                            respuesta = protocolo.programarMemoriaEEPROMDelPic(chipPIC, firware);

                            if (respuesta) {

                                proceso.setText("Memoria EEPROM Programada Exitosamente");

                            } else {

                                proceso.setText("Error al Programar Memoria EEPROM del PIC");

                                return;
                            }

                            respuesta = protocolo.programarFusesIDDelPic(chipPIC, firware);

                            if (respuesta) {

                                proceso.setText("Fuses Programados Exitosamente");

                            } else {

                                proceso.setText("Error al Programar Fuses del PIC");

                                return;
                            }

                            if (respuesta) {

                                proceso.setText("PIC Programado Exitosamente");

                            } else {
                            
                                proceso.setText("Error al Programar PIC");

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

                            datos2.append(protocolo.leerMemoriaEEPROMDelPic(chipPIC));

                            if (datos1.length() > 0 && datos2.length() > 0) {

                                romData.setText(datos1.toString());

                                eepromData.setText(datos2.toString());
                            
                            
                            proceso.setText("Memoria del PIC Leida Exitosamente");

                            } else {

                                proceso.setText("Error al leer Memoria del PIC");
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

                                proceso.setText("Memoria del PIC Borrada Exitosamente");

                            } else {

                                proceso.setText("Error al Borrar Memoria del PIC");
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

                                proceso.setText("La Memoria No Contiene Datos");

                            } else {

                                proceso.setText("La Memoria Contiene Datos");
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

                                proceso.setText("El PIC se Encuentra en el Socket");

                            } else {

                                proceso.setText("El PIC No se Encuentra en el Socket");
                            }
                        }
                    }
                });

        btnSelectHex.setOnClickListener(v -> checkPermissionsAndOpenFile());

        privacyPolicyButton.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {}
                });

        hilo = new Thread(this);

        conectado = false;

        iniciar = true;

        usbManager = (UsbManager) getSystemService(USB_SERVICE);

        // Detectar dispositivos USB
        drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        hilo.start();

        
        // Registrar el BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        registerReceiver(usbReceiver, filter);

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

                        ((TextView) view)
                                .setTextColor(Color.GREEN); // Color verde del texto seleccionado

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
        layout.setBackgroundColor(Color.parseColor("#B5651D")); // Fondo oscuro profesional

        // Título centrado en la parte superior
        TextView title = new TextView(this);
        title.setText("PIC K150 Programing");
        title.setTextSize(24);
        title.setTextColor(Color.DKGRAY);
        title.setGravity(Gravity.CENTER);
        title.setId(View.generateViewId());
        layout.addView(title);

        mensaje.setText("Dispositivo Desconectado");
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

        proceso.setText("Esperando PIC");
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
        chipLabel.setText("Seleccionar PIC:");
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
        progressLabel.setText("Progreso ");
        progressLabel.setTextColor(Color.WHITE);
        progressLabel.setId(View.generateViewId());
        progressLayout.addView(progressLabel);

        // Barra de progreso
        progressBar.setId(View.generateViewId());
        progressBar.setProgress(50); // Valor inicial para ser visible
        progressBar.setMax(100); // Máximo para pruebas
        progressBar.setLayoutParams(
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        progressLayout.addView(progressBar);

        // ScrollView para datos leídos del chip
        ScrollView scrollView = new ScrollView(this);

        scrollView.setId(View.generateViewId());
        layout.addView(scrollView);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(scrollContent);

        // Etiquetas y datos para ROM
        TextView romLabel = new TextView(this);
        romLabel.setText("Datos de memoria ROM:");
        romLabel.setTextColor(Color.WHITE);
        scrollContent.addView(romLabel);

        romData.setTextColor(Color.BLACK);
        romData.setBackgroundColor(Color.WHITE); // Fondo blanco para destacar
        scrollContent.addView(romData);

        // Etiquetas y datos para EEPROM
        TextView eepromLabel = new TextView(this);
        eepromLabel.setText("Datos de memoria EEPROM:");
        eepromLabel.setTextColor(Color.WHITE);
        scrollContent.addView(eepromLabel);

        eepromData.setTextColor(Color.BLACK);
        eepromData.setBackgroundColor(Color.WHITE); // Fondo blanco para destacar
        scrollContent.addView(eepromData);

        // Menú de políticas y ayuda
        privacyPolicyButton.setText("Políticas de privacidad");
        privacyPolicyButton.setPadding(40, 20, 40, 20);
        privacyPolicyButton.setId(View.generateViewId());
        layout.addView(privacyPolicyButton);

        // Configurar el ConstraintLayout
        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(layout);

        // Título
        constraints.connect(
                title.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 20);
        constraints.connect(
                title.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                title.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 20);

        // Mensaje
        constraints.connect(
                mensaje.getId(), ConstraintSet.TOP, title.getId(), ConstraintSet.BOTTOM, 20);
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
                scrollView.getId(),
                ConstraintSet.BOTTOM,
                privacyPolicyButton.getId(),
                ConstraintSet.TOP,
                20);
        // Ajustar la altura del ScrollView para que ocupe el espacio disponible
        constraints.constrainHeight(scrollView.getId(), 0); // MATCH_CONSTRAINT

        // Botón de políticas de privacidad
        constraints.connect(
                privacyPolicyButton.getId(),
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                20);
        constraints.connect(
                privacyPolicyButton.getId(),
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                20);
        constraints.connect(
                privacyPolicyButton.getId(),
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                20);

        constraints.applyTo(layout);

        setContentView(layout);
    }

    private void prueba() {}

    private void mostrarInformacionPic(String modelo) {

        chipPIC = chip.getChipEntry(modelo);

        if (protocolo != null) {

            protocolo.iniciarVariablesDeProgramacion(chipPIC);
        }

        proceso.setText("El PIC se debe ubicar en el " + chipPIC.getUbicacionPin1DelPic());

        Toast.makeText(getApplicationContext(), modelo, Toast.LENGTH_LONG).show();
    }

    private void connectToDevice(UsbSerialDriver driver) {

        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);

        try {
            usbSerialPort = driver.getPorts().get(0); // Seleccionar el primer puerto

            usbSerialPort.open(usbManager.openDevice(driver.getDevice()));

            // Configurar el puerto
            usbSerialPort.setParameters(
                    19200, // Velocidad (baudios)
                    8, // Bits de datos
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE);

            protocolo = new ProtocoloP018(this, usbSerialPort);

            if (protocolo.iniciarProtocolo()) {

                //mensaje.setTextColor(Color.GREEN);

              // mensaje.setText("Dispositivo Conectado");
            }

        } catch (IOException e) {

            Toast.makeText(this, "Error al conectar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        iniciar = false;

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
    public void run() {

        while (iniciar) {

            if (!conectado) {

                drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

                if (!drivers.isEmpty()) {

                    UsbSerialDriver driver = drivers.get(0);

                    UsbDevice device = driver.getDevice();

                    if (driver != null) {

                        if (!usbManager.hasPermission(device)) {

                            if (driversP.isEmpty()) {

                                PendingIntent permissionIntent =
                                        PendingIntent.getBroadcast(
                                                this,
                                                0,
                                                new Intent(ACTION_USB_PERMISSION),
                                                PendingIntent.FLAG_IMMUTABLE);
                                usbManager.requestPermission(device, permissionIntent);
                            }

                            driversP.add(driver);

                        } else {
                            connectToDevice(driver);

                            conectado = true;
                        }
                    }
                }
            }
        }
    }

    // Verifica permisos y abre el selector de archivos
    private void checkPermissionsAndOpenFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            openFilePicker();
        }
    }

    // Abre el selector de archivos
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {

                firware = readHexFile(uri);
            }
        }
    }

    // Lee el archivo .hex seleccionado
    private String readHexFile(Uri uri) {

        String hexFileContent = "";

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

        return hexFileContent;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {

            }
        }
    }
}
