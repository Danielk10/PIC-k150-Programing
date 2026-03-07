package com.diamon.pic;

import android.app.Application;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import com.google.android.gms.ads.MobileAds;

/**
 * Clase Application personalizada para PIC k150 Programming.
 * 
 * Funcionalidades:
 * - Inicialización diferida de Google Mobile Ads SDK para evitar bloqueo del
 * hilo principal
 * - UncaughtExceptionHandler para capturar errores del sistema no controlables
 * - Pre-carga de WebView en background para reducir tiempo de primera
 * inicialización
 */
public class PicApplication extends Application {

    private static final String TAG = "PicApplication";

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    /**
     * Bandera para indicar si MobileAds ya fue inicializado
     */
    private static volatile boolean mobileAdsInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Definir el sufijo del directorio de datos de WebView
        // antes de inicializar SDKs que dependan de WebView (por ejemplo, AdMob).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                String processName = getProcessName();
                if (!getPackageName().equals(processName)) {
                    WebView.setDataDirectorySuffix(processName);
                } else {
                    WebView.setDataDirectorySuffix("webview");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error setting WebView data directory suffix: " + e.getMessage());
            }
        }

        // Configurar handler para excepciones del sistema que no podemos controlar
        setupUncaughtExceptionHandler();

        // Diferir inicialización de MobileAds
        initializeMobileAdsDeferred();
    }

    /**
     * Configura un UncaughtExceptionHandler personalizado que captura
     * CannotDeliverBroadcastException y otras excepciones del sistema.
     * 
     * Esto evita que la app crashee por errores internos del sistema Android
     * o del SDK de Google Ads que están fuera de nuestro control.
     */
    private void setupUncaughtExceptionHandler() {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Verificar si es una excepción del sistema que podemos absorber
            if (isSystemException(throwable)) {
                Log.w(TAG, "Absorbed system exception: " + throwable.getClass().getSimpleName(), throwable);
                // No re-lanzar, simplemente logear
                return;
            }

            // Para otras excepciones, delegar al handler por defecto
            if (defaultExceptionHandler != null) {
                defaultExceptionHandler.uncaughtException(thread, throwable);
            }
        });
    }

    /**
     * Determina si una excepción es del sistema y puede ser absorbida
     * sin afectar la experiencia del usuario.
     */
    private boolean isSystemException(Throwable throwable) {
        if (throwable == null)
            return false;

        String className = throwable.getClass().getName();
        String message = throwable.getMessage();

        // CannotDeliverBroadcastException - Error del sistema al entregar broadcasts
        if (className.contains("CannotDeliverBroadcastException")) {
            return true;
        }

        // RemoteServiceException - Errores de servicios del sistema
        if (className.contains("RemoteServiceException") && message != null) {
            if (message.contains("can't deliver broadcast")) {
                return true;
            }
        }

        // DeadSystemException - El sistema está muriendo
        if (className.contains("DeadSystemException")) {
            return true;
        }

        // BadTokenException - Intento de mostrar ventana cuando la Activity ya no es
        // valida
        // Ocurre al desplegar el menu de desbordamiento (overflow) en una Activity que
        // ya fue destruida
        if (throwable instanceof android.view.WindowManager.BadTokenException) {
            String msg = throwable.getMessage();
            if (msg != null && msg.contains("is not valid")) {
                return true;
            }
        }

        // Verificar causa recursivamente
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isSystemException(cause);
        }

        return false;
    }

    /**
     * Inicializa Google Mobile Ads SDK de forma diferida.
     * Espera 1 segundo después del inicio para no bloquear el main thread.
     */
    private void initializeMobileAdsDeferred() {
        if (mobileAdsInitialized)
            return;

        // Corrección para evitar bloqueos asociados a ANR (Unsafe.park).
        // Iniciar un HILO DE FONDO real para la inicialización pesada de
        // AdMob/StartApp.
        // Esto evita que el main thread se bloquee esperando mediadores sincronizados.
        new Thread(() -> {
            try {
                // Esperar a que la app termine de cargar la UI básica
                Thread.sleep(2000);

                MobileAds.initialize(PicApplication.this, initializationStatus -> {
                    mobileAdsInitialized = true;
                    Log.d(TAG, "MobileAds initialized successfully in background thread");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error initializing MobileAds in background: " + e.getMessage());
            }
        }, "AdMobInitializer").start();
    }

    /**
     * Verifica si MobileAds ya fue inicializado
     */
    public static boolean isMobileAdsInitialized() {
        return mobileAdsInitialized;
    }
}
