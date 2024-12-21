package com.diamon.pic;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.diamon.chip.InformacionPic;
import com.diamon.chip.ProtocoloP018;
import com.diamon.datos.ChipinfoReader;
import com.diamon.datos.HexFileListo;
import com.diamon.utilidades.HexFileUtils;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

    private LinearLayout diseno;

    private LinearLayout fusesPic;

    private TextView texto;

    private TextView textoPic;

    private Thread hilo;

    private boolean conectado;

    private volatile boolean iniciar;

    private Button btnSelectHex;

    private Button btnProgramPic;

    private StringBuffer datos;

    private String comandoEviado = "";

    private String respuesta = "";

    private String firware;

    private InformacionPic chipPIC;

    private ChipinfoReader chip;

    private TextView informacionPic;

    private ProtocoloP018 protocolo;

    private TextView test;

    // Pruebas

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
                                    protocolo =
                                            new ProtocoloP018(
                                                    getApplicationContext(), usbSerialPort);
                                    texto.setText("" + protocolo.iniciarProtocolo());

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

        diseno = new LinearLayout(this);

        driversP = new ArrayList<UsbSerialDriver>();

        texto = new TextView(this);

        textoPic = new TextView(this);

        informacionPic = new TextView(this);

        test = new TextView(this);

        btnSelectHex = new Button(this);

        Spinner spinner = new Spinner(this);

        ScrollView scrollView = new ScrollView(this);

        Button btnInicio = new Button(this);
        btnInicio.setText("Boton inicio");
        btnInicio.setOnClickListener(v -> protocolo.esperarInicioDeNuevoComando());

        Button btnResearComandos = new Button(this);
        btnResearComandos.setText("Resetear Comandos");
        btnResearComandos.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        texto.setText("" + protocolo.verificarBorradoMemoriaEEPROMPic());

                        // texto.setText("" + protocolo.leerVectorDeDepuracion());

                        // texto.setText("" + protocolo.obtenerProtocoloDelProgramador());

                        // texto.setText("" + protocolo.obtenerVersionDelProgramador());

                        // texto.setText("" + protocolo.programarFusesIDPic(chipPIC, firware));

                        // texto.setText("" + protocolo.programarEEPROMPic(chipPIC,firware));

                        // texto.setText("" + protocolo.leerCalibracionPic());

                        // texto.setText("" + protocolo.programarROMPic(chipPIC, firware));

                        // texto.setText("Hola " + protocolo.leerConfiguracionPic());

                        // texto.setText("" + protocolo.leerMemoriaEEPROMPic(chipPIC));

                        // texto.setText("Hola "+protocolo.borrarMemoriaPic());

                    }
                });

        Button btnHacerEco = new Button(this);
        btnHacerEco.setText("Hacer Eco");
        btnHacerEco.setOnClickListener(v -> texto.setText("" + protocolo.hacerEco()));

        Button btnInciarVaribles = new Button(this);
        btnInciarVaribles.setText("Iniciar Variables de Programación");
        btnInciarVaribles.setOnClickListener(
                v -> texto.setText("" + protocolo.iniciarVariablesDeProgramacion(chipPIC)));

        Button btnActivarVoltaje = new Button(this);
        btnActivarVoltaje.setText("Activar voltajes de Programación");
        btnActivarVoltaje.setOnClickListener(
                v -> texto.setText("" + protocolo.activarVoltajesDeProgramacion()));

        Button btnDesactivarVoltaje = new Button(this);
        btnDesactivarVoltaje.setText("Desactivar voltajes de Programación");
        btnDesactivarVoltaje.setOnClickListener(
                v -> texto.setText("" + protocolo.desactivarVoltajesDeProgramacion()));

        Button btnReiniciaVoltaje = new Button(this);
        btnReiniciaVoltaje.setText("Reinicia voltajes de Programación");
        btnReiniciaVoltaje.setOnClickListener(
                v -> texto.setText("" + protocolo.reiniciarVoltajesDeProgramacion()));

        btnProgramPic = new Button(this);
        btnProgramPic.setText("Programar ROM");
        btnProgramPic.setEnabled(false);
        btnProgramPic.setOnClickListener(
                v -> texto.setText("" + protocolo.programarROMPic(chipPIC, firware)));

        Button btnProgramEEPRON = new Button(this);
        btnProgramEEPRON.setText("Programar EEPROM");
        btnProgramEEPRON.setOnClickListener(
                v -> texto.setText("" + protocolo.programarEEPROMPic(chipPIC, firware)));

        Button btnProgramarIDFuses = new Button(this);
        btnProgramarIDFuses.setText("Programar ID y Fuses");
        btnProgramarIDFuses.setOnClickListener(
                v -> texto.setText("" + protocolo.programarFusesIDPic(chipPIC, firware)));

        Button btnCalibracion = new Button(this);
        btnCalibracion.setText("Calibracion");
        btnCalibracion.setOnClickListener(v -> texto.setText("No implementado"));

        Button btnLeerMemoria = new Button(this);
        btnLeerMemoria.setText("Leer Memoria Rom");

        btnLeerMemoria.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        texto.setText("" + protocolo.leerMemoriaROMPic(chipPIC));
                    }
                });

        Button btnLeerMemoriaEEPROM = new Button(this);
        btnLeerMemoriaEEPROM.setText("Leer Memoria EEPROM");

        btnLeerMemoriaEEPROM.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        texto.setText("" + protocolo.leerMemoriaEEPROMPic(chipPIC));
                    }
                });

        Button btnLeerConfiguracion = new Button(this);
        btnLeerConfiguracion.setText("Leer Configuración");
        btnLeerConfiguracion.setOnClickListener(
                v -> texto.setText("" + protocolo.leerConfiguracionPic()));

        Button btnLeerCalibracion = new Button(this);
        btnLeerCalibracion.setText("Leer Calibración");
        btnLeerCalibracion.setOnClickListener(
                v -> texto.setText("" + protocolo.leerCalibracionPic()));

        Button btnBarraPic = new Button(this);
        btnBarraPic.setText("Barrar Momoria del PIC");
        btnBarraPic.setOnClickListener(v -> texto.setText("" + protocolo.borrarMemoriaPic()));

        Button btnVerificarBorrado = new Button(this);
        btnVerificarBorrado.setText("Verificar si se Borro Memoria del PIC");
        btnVerificarBorrado.setOnClickListener(v -> texto.setText("No implementado"));

        Button btnChekearBorrarEEPROM = new Button(this);
        btnChekearBorrarEEPROM.setText("Chekear Borrado de EEPROM");
        btnChekearBorrarEEPROM.setOnClickListener(
                v -> texto.setText("" + protocolo.verificarBorradoMemoriaEEPROMPic()));

        Button btnProgramarFuses18F = new Button(this);
        btnProgramarFuses18F.setText("Programar Fuses18F");
        btnProgramarFuses18F.setOnClickListener(
                v -> texto.setText("" + protocolo.programarFuses18F()));

        Button btnDetectarChip = new Button(this);
        btnDetectarChip.setText("Detectar PIC en el Soket");
        btnDetectarChip.setOnClickListener(v -> texto.setText("" + protocolo.detectarPicEnSoket()));

        Button btnDetectarChipFuera = new Button(this);
        btnDetectarChipFuera.setText("Detectar PIC fuera del Soket");
        btnDetectarChipFuera.setOnClickListener(
                v -> texto.setText("" + protocolo.detectarPicFueraDelSoket()));

        Button btnVersionProgramdor = new Button(this);
        btnVersionProgramdor.setText("Obtener Version del Programador");
        btnVersionProgramdor.setOnClickListener(
                v -> texto.setText("" + protocolo.obtenerVersionDelProgramador()));

        Button btnObtenerProtocolo = new Button(this);
        btnObtenerProtocolo.setText("Obtener Protocolo del K150");
        btnObtenerProtocolo.setOnClickListener(
                v -> texto.setText("" + protocolo.obtenerProtocoloDelProgramador()));

        Button btnDepuracionPrograma = new Button(this);
        btnDepuracionPrograma.setText("Programar Vector de Depuración");
        btnDepuracionPrograma.setOnClickListener(v -> texto.setText("No implementado"));

        Button btnLeetDebug = new Button(this);
        btnLeetDebug.setText("Leer Vector de Depuración");
        btnLeetDebug.setOnClickListener(
                v -> texto.setText("" + protocolo.leerVectorDeDepuracion()));

        Button btnDatosCalibracion10F = new Button(this);
        btnDatosCalibracion10F.setText("Datos de Calibracion 10F");
        btnDatosCalibracion10F.setOnClickListener(v -> texto.setText("No implementado"));

        btnSelectHex.setText("Seleccionar Archivo HEX");

        btnSelectHex.setOnClickListener(v -> checkPermissionsAndOpenFile());

        diseno.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(diseno);

        diseno.addView(btnInicio);

        diseno.addView(btnResearComandos);

        diseno.addView(btnHacerEco);

        diseno.addView(btnInciarVaribles);

        diseno.addView(btnActivarVoltaje);

        diseno.addView(btnDesactivarVoltaje);

        diseno.addView(btnReiniciaVoltaje);

        diseno.addView(btnProgramPic);

        diseno.addView(btnProgramEEPRON);

        diseno.addView(btnProgramarIDFuses);

        diseno.addView(btnCalibracion);

        diseno.addView(btnLeerMemoria);

        diseno.addView(btnLeerMemoriaEEPROM);

        diseno.addView(btnLeerConfiguracion);

        diseno.addView(btnLeerCalibracion);

        diseno.addView(btnBarraPic);

        diseno.addView(btnVerificarBorrado);

        diseno.addView(btnChekearBorrarEEPROM);

        diseno.addView(btnProgramarFuses18F);

        diseno.addView(btnDetectarChip);

        diseno.addView(btnDetectarChipFuera);

        diseno.addView(btnVersionProgramdor);

        diseno.addView(btnObtenerProtocolo);

        diseno.addView(btnDepuracionPrograma);

        diseno.addView(btnLeetDebug);

        diseno.addView(btnDatosCalibracion10F);

        diseno.addView(btnSelectHex);

        diseno.addView(texto);

        diseno.addView(textoPic);

        diseno.addView(informacionPic);

        diseno.addView(test);

        setContentView(scrollView);

        hilo = new Thread(this);

        datos = new StringBuffer();

        conectado = false;

        iniciar = true;

        usbManager = (UsbManager) getSystemService(USB_SERVICE);

        // Detectar dispositivos USB
        drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        hilo.start();

        // Registrar el BroadcastReceiver
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        registerReceiver(usbReceiver, filter);

        // Pruebas

        chip = new ChipinfoReader(this);

        String[] pic = new String[chip.getModelosPic().size()];

        int nu = 0;

        for (String modelo : chip.getModelosPic()) {
            pic[nu] = modelo;

            nu++;
        }

        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, pic);

        spinner.setAdapter(arrayAdapter);

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {

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

        diseno.addView(spinner);
    }

    // Crear etiquetas para la memoria (ROM o EEPROM)
    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(18);
        label.setTextColor(Color.DKGRAY);
        return label;
    }

    // Vista de memoria dinámica con direcciones y datos
    private LinearLayout createMemoryView(
            int memorySize, int byteGroupSize, byte[] blankData, String receivedData) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Convertir los datos en blanco y recibidos a formato hexadecimal
        String[] blankHex = convertBytesToHexArray(blankData, byteGroupSize);
        String[] receivedHex = receivedData.split(" ");

        int address = 0; // Dirección inicial
        int itemsPerLine = 8; // Columnas por fila

        // Generar las líneas dinámicamente
        for (int i = 0; i < blankHex.length; i += itemsPerLine) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);

            // Mostrar la dirección
            TextView addressView = new TextView(this);
            addressView.setText(String.format("%04X: ", address));
            addressView.setTextColor(Color.DKGRAY);
            line.addView(addressView);

            // Mostrar los datos
            for (int j = 0; j < itemsPerLine && (i + j) < blankHex.length; j++) {
                TextView dataView = new TextView(this);
                String value = (i + j < receivedHex.length) ? receivedHex[i + j] : blankHex[i + j];
                dataView.setText(value + " ");
                dataView.setTextColor((i + j < receivedHex.length) ? Color.GREEN : Color.BLACK);
                line.addView(dataView);
            }

            container.addView(line);
            address += itemsPerLine * byteGroupSize;
        }

        return container;
    }

    // Convertir bytes en formato hexadecimal agrupados por tamaño
    private String[] convertBytesToHexArray(byte[] data, int groupSize) {
        String[] hexArray = new String[data.length / groupSize];
        for (int i = 0; i < data.length; i += groupSize) {
            if (groupSize == 2) { // Agrupar en palabras (2 bytes)
                int value = ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);
                hexArray[i / groupSize] = String.format("%04X", value);
            } else { // Agrupar 1 byte
                hexArray[i / groupSize] = String.format("%02X", data[i]);
            }
        }
        return hexArray;
    }

    private void prueba() {

        // Generar y mostrar la memoria ROM
        diseno.addView(createLabel("Memoria ROM:"));
        LinearLayout romView =
                createMemoryView(
                        chipPIC.getTamanoROM() * 2,
                        2,
                        HexFileUtils.generateRomBlank(
                                chipPIC.getTipoNucleoBit(), chipPIC.getTamanoROM()),
                        protocolo.leerMemoriaROMPic(chipPIC));
        diseno.addView(romView);

        // Generar y mostrar la memoria EEPROM
        diseno.addView(createLabel("Memoria EEPROM:"));
        LinearLayout eepromView =
                createMemoryView(
                        chipPIC.getTamanoEEPROM(),
                        1,
                        HexFileUtils.generateEepromBlank(chipPIC.getTamanoEEPROM()),
                        protocolo.leerMemoriaEEPROMPic(chipPIC));
        diseno.addView(eepromView);
    }

    private void mostrarInformacionPic(String modelo) {

        chipPIC = chip.getChipEntry(modelo);

        fusesPic = new LinearLayout(this);

        fusesPic.setOrientation(LinearLayout.VERTICAL);

        /* for (int i = 0; i < chipPIC.getValoresFuses().size(); i++) {

            DatosFuses fusesD = chipPIC.getValoresFuses().get(i);

            TextView prueba = new TextView(this);

            prueba.setText(fusesD.getTitulo());

            fusesPic.addView(prueba);

            for (int j = 0; j < fusesD.getDescription().size(); j++) {
                TextView prueba1 = new TextView(this);

                prueba1.setText(fusesD.getDescription().get(j) + " " + fusesD.getValor().get(j));

                fusesPic.addView(prueba1);
            }
        }*/

        /*for (int i = 0; i < chipPIC.getFuseBlack().length; i++) {

            int[] fusesD = chipPIC.getFuseBlack();

            TextView prueba = new TextView(this);

            prueba.setText(" " + fusesD[i]);

            fusesPic.addView(prueba);
        }

        diseno.addView(fusesPic);*/

        if (protocolo != null) {

            protocolo.iniciarVariablesDeProgramacion(chipPIC);
        }

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

            texto.setText("" + protocolo.iniciarProtocolo());

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

            btnProgramPic.setEnabled(true);

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
