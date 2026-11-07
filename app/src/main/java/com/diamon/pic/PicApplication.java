package com.diamon.pic;

import android.app.Application;
import android.content.Context;
import android.os.Build;
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

    static {
        try {
            System.loadLibrary("fdsan_bypass");
        } catch (Throwable t) {
            Log.w(TAG, "No se pudo cargar libfdsan_bypass: " + t.getMessage());
        }
    }

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

        // MobileAds se inicializa desde ConsentManager, después de que el
        // usuario acepte el consentimiento de privacidad.
    }

    /**
     * Configura un UncaughtExceptionHandler personalizado que captura
     * CannotDeliverBroadcastException y otras excepciones del sistema
     * únicamente en hilos de fondo. En el hilo principal se delega siempre al
     * handler por defecto para no dejar el looper muerto (ANR).
     * 
     * Esto evita que la app crashee por errores internos del sistema Android
     * o del SDK de Google Ads que están fuera de nuestro control.
     */
    private void setupUncaughtExceptionHandler() {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Solo absorber errores del sistema en hilos de fondo. Si se traga
            // una excepción del hilo principal, el looper muere y el proceso
            // termina en DestroyJavaVM esperando hilos no-daemon (ANR).
            boolean isMainThread = thread == android.os.Looper.getMainLooper().getThread();
            if (!isMainThread && isSystemException(throwable)) {
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
     * Inicializa Google Mobile Ads SDK una sola vez, después de que UMP haya
     * resuelto el consentimiento. Se puede llamar desde cualquier Activity.
     */
    public static void initializeMobileAds(Context context) {
        if (mobileAdsInitialized) {
            return;
        }
        mobileAdsInitialized = true;
        try {
            MobileAds.initialize(
                    context.getApplicationContext(),
                    initializationStatus -> Log.d(TAG, "MobileAds initialized successfully"));
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MobileAds: " + e.getMessage());
        }
    }

    /**
     * Verifica si MobileAds ya fue inicializado
     */
    public static boolean isMobileAdsInitialized() {
        return mobileAdsInitialized;
    }
}
