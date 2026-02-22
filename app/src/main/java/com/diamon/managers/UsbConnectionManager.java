package com.diamon.managers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.diamon.protocolo.ProtocoloP018;
import com.diamon.pic.R;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * Gestor de conexiones USB para comunicacion con el programador PIC K150.
 * 
 * Maneja la deteccion de dispositivos USB, permisos y configuracion del puerto
 * serial.
 * Usa SafeBroadcastManager para registro seguro de BroadcastReceivers.
 */
public class UsbConnectionManager {

    private static final String TAG = "UsbConnectionManager";
    private static final String ACTION_USB_PERMISSION = "com.diamon.pic.USB_PERMISSION";

    // Parametros de configuracion del puerto serial
    private static final int BAUD_RATE = 19200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = UsbSerialPort.STOPBITS_1;
    private static final int PARITY = UsbSerialPort.PARITY_NONE;

    private final Context context;
    private final SafeBroadcastManager broadcastManager;
    private final Handler mainHandler;
    private UsbManager usbManager;
    private UsbSerialPort usbSerialPort;
    private List<UsbSerialDriver> drivers;
    private ProtocoloP018 protocolo;

    // Interfaz para notificar eventos de conexion
    private UsbConnectionListener connectionListener;

    /** Interfaz para manejar eventos de conexion USB */
    public interface UsbConnectionListener {
        void onConnected();

        void onDisconnected();

        void onConnectionError(String errorMessage);
    }

    /** BroadcastReceiver para manejar permisos USB */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                // Verificar que drivers no sea null antes de acceder
                if (drivers != null && !drivers.isEmpty()) {
                    UsbSerialDriver driver = drivers.get(0);
                    connectToDevice(driver);
                }
            }
        }
    };

    /**
     * Constructor del gestor de conexiones USB
     *
     * @param context Contexto de la aplicacion
     */
    public UsbConnectionManager(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.broadcastManager = new SafeBroadcastManager(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Establece el listener para eventos de conexion
     *
     * @param listener Listener que sera notificado de eventos
     */
    public void setConnectionListener(UsbConnectionListener listener) {
        this.connectionListener = listener;
    }

    /**
     * Inicializa el gestor USB.
     * NOTA: El registro del BroadcastReceiver DEBE hacerse en el main thread,
     * pero el escaneo de drivers puede estar en background.
     * Esta funcion puede llamarse desde cualquier hilo.
     */
    public void initialize() {
        // Registrar el BroadcastReceiver en el hilo principal para evitar bloqueos del binder
        mainHandler.post(() -> {
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            boolean registered = broadcastManager.registerReceiver(usbReceiver, filter, false);
            if (!registered) {
                Log.w(TAG, "Failed to register USB receiver");
            }
        });

        // Escanear drivers USB en segundo plano
        drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        // Solicitar permisos si hay dispositivos detectados (desde el hilo actual,
        // ya que requestPermission solo lanza un Intent)
        requestPermissionsIfNeeded();
    }

    /** Solicita permisos USB si hay dispositivos disponibles */
    private void requestPermissionsIfNeeded() {
        if (!drivers.isEmpty()) {
            UsbSerialDriver driver = drivers.get(0);

            if (usbManager.hasPermission(driver.getDevice())) {
                // Ya tiene permisos, conectar directamente
                connectToDevice(driver);
            } else {
                // Solicitar permisos
                PendingIntent permissionIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        new Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(driver.getDevice(), permissionIntent);
            }
        }
    }

    /**
     * Conecta al dispositivo USB programador
     *
     * @param driver Driver USB del dispositivo programador
     */
    private void connectToDevice(UsbSerialDriver driver) {
        if (driver == null) {
            notifyError(context.getString(R.string.driver_usb_no_disponible));
            return;
        }

        try {
            // Verificar que el dispositivo tiene puertos
            if (driver.getPorts().isEmpty()) {
                notifyError(context.getString(R.string.dispositivo_sin_puertos_usb_di));
                return;
            }

            // Seleccionar el primer puerto
            usbSerialPort = driver.getPorts().get(0);

            // Abrir conexion
            usbSerialPort.open(usbManager.openDevice(driver.getDevice()));

            // Configurar parametros del puerto serial
            usbSerialPort.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);

            // Crear e inicializar protocolo
            protocolo = new ProtocoloP018(context, usbSerialPort);
            boolean protocoloIniciado = protocolo.iniciarProtocolo();

            if (!protocoloIniciado) {
                cleanupConnection();
                notifyError(context.getString(R.string.error_inicializando_protocolo_));
                return;
            }

            // Notificar conexion exitosa
            if (connectionListener != null) {
                connectionListener.onConnected();
            }

        } catch (IOException e) {
            String mensajeError = context.getString(R.string.error_de_conexion_usb) + ": " + e.getMessage();
            cleanupConnection();
            notifyError(mensajeError);
        } catch (Exception e) {
            cleanupConnection();
            notifyError(context.getString(R.string.error_inesperado_durante_la_co));
        }
    }

    /** Limpia el estado de conexion cuando ocurre un error */
    private void cleanupConnection() {
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
            } catch (IOException e) {
                // Ignorar errores al cerrar
            }
            usbSerialPort = null;
        }
        protocolo = null;
    }

    /**
     * Notifica un error al listener
     *
     * @param errorMessage Mensaje de error
     */
    private void notifyError(String errorMessage) {
        if (connectionListener != null) {
            connectionListener.onConnectionError(errorMessage);
        }
    }

    /**
     * Obtiene el protocolo de comunicacion P018
     *
     * @return Instancia del protocolo o null si no esta conectado
     */
    public ProtocoloP018 getProtocolo() {
        return protocolo;
    }

    /**
     * Verifica si hay una conexion activa
     *
     * @return true si esta conectado, false en caso contrario
     */
    public boolean isConnected() {
        return usbSerialPort != null && protocolo != null;
    }

    /** Cierra la conexion USB y libera recursos */
    public void disconnect() {
        cleanupConnection();

        if (connectionListener != null) {
            connectionListener.onDisconnected();
        }
    }

    /** Libera todos los recursos y desregistra el BroadcastReceiver */
    public void release() {
        disconnect();

        // Usar SafeBroadcastManager para desregistro seguro
        broadcastManager.unregisterReceiver(usbReceiver);
    }
}
