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

        // Configurar handler para excepciones del sistema que no podemos controlar
        setupUncaughtExceptionHandler();

        // Pre-cargar WebView en background para reducir bloqueo en primera instancia
        preloadWebViewAsync();

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

        // Verificar causa recursivamente
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isSystemException(cause);
        }

        return false;
    }

    /**
     * Pre-carga WebView en un thread background para reducir el tiempo
     * de inicialización cuando se muestre el primer anuncio.
     */
    private void preloadWebViewAsync() {
        new Thread(() -> {
            try {
                // Pequeño delay para no interferir con el inicio de la app
                Thread.sleep(2000);

                // Handler para ejecutar en el main thread (requerido para WebView)
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        // Pre-cargar el provider de WebView
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            WebView.setDataDirectorySuffix("webview");
                        }
                        // Crear y descartar inmediatamente para pre-cargar
                        new WebView(getApplicationContext()).destroy();
                        Log.d(TAG, "WebView preloaded successfully");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to preload WebView: " + e.getMessage());
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "WebViewPreloader").start();
    }

    /**
     * Inicializa Google Mobile Ads SDK de forma diferida.
     * Espera 1 segundo después del inicio para no bloquear el main thread.
     */
    private void initializeMobileAdsDeferred() {
        if (mobileAdsInitialized)
            return;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                MobileAds.initialize(this, initializationStatus -> {
                    mobileAdsInitialized = true;
                    Log.d(TAG, "MobileAds initialized successfully");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error initializing MobileAds: " + e.getMessage());
            }
        }, 1500); // Delay de 1.5 segundos para permitir que la UI se renderice primero
    }

    /**
     * Verifica si MobileAds ya fue inicializado
     */
    public static boolean isMobileAdsInitialized() {
        return mobileAdsInitialized;
    }
}
