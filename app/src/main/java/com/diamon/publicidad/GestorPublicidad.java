package com.diamon.publicidad;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.diamon.nucleo.Publicidad;
import com.diamon.pic.PicApplication;
import com.diamon.pic.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor CENTRALIZADO de publicidad de Google Mobile Ads.
 * Gestiona Banner, Interstitial, Native Ads y Rewarded Video en un solo lugar.
 *
 * Política de carga:
 * - El banner se pide una sola vez y se reutiliza.
 * - El rewarded se carga al abrir el diálogo Pro o al pulsar el botón, no al arrancar.
 * - Los nativos se cargan cuando el diálogo que los muestra se va a abrir.
 * - Todo anuncio cargado caduca a los 60 minutos.
 */
public class GestorPublicidad implements Publicidad {

    private static final String TAG = "GestorPublicidad";

    // IDs de Unidades de Anuncio (producción)
    private static final String BANNER_ID = "ca-app-pub-5141499161332805/5248084133";
    public static final String KEY_NATIVE_MEMORY = "memory_native";
    public static final String KEY_NATIVE_PROGRAMMING = "programming_native";

    /** ID Real AdMob de producción: "Pase Pro 12 Horas K150" */
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-5141499161332805/7291519778";
    public static final String REWARDED_ID = REWARDED_AD_UNIT_ID; // Alias retrocompatible

    private static final Map<String, String> NATIVE_IDS = new HashMap<String, String>() {
        {
            put(KEY_NATIVE_MEMORY, "ca-app-pub-5141499161332805/1625082944");
            put(KEY_NATIVE_PROGRAMMING, "ca-app-pub-5141499161332805/2642812533");
        }
    };

    // Los anuncios cargados se descartan a la hora para evitar mostrarlos caducados.
    private static final long AD_TTL_MS = 60 * 60 * 1000;
    // Espera máxima cuando el usuario pulsa "Ver video" y el rewarded aún no está listo.
    private static final long REWARDED_PENDING_TIMEOUT_MS = 20 * 1000;
    // Reintentos máximos esperando la inicialización de MobileAds (2 s cada uno).
    private static final int MAX_INIT_RETRIES = 10;
    // Throttling entre peticiones de la misma unidad nativa.
    private static final long MIN_REQUEST_INTERVAL = 10 * 1000;

    private final AppCompatActivity actividad;
    private final Handler mainHandler;
    private final AdRequest adRequest;

    public interface AdsDisabledChecker {
        boolean areAdsDisabled();
    }

    private AdsDisabledChecker adsDisabledChecker;

    public void setAdsDisabledChecker(AdsDisabledChecker checker) {
        this.adsDisabledChecker = checker;
        if (isAdDisabled()) {
            ocultarBanner();
        }
    }

    private boolean isAdDisabled() {
        // Si el usuario rechazó el consentimiento, no se puede pedir publicidad.
        // Se consulta ConsentManager en cada llamada para respetar también los
        // cambios hechos desde "Opciones de privacidad".
        if (consentResolved && !ConsentManager.canRequestAds()) {
            return true;
        }
        return adsDisabledChecker != null && adsDisabledChecker.areAdsDisabled();
    }

    /**
     * Evita cargar anuncios (y crear WebViews internos de AdMob) cuando la
     * actividad ya no está visible. Crashlytics 169756c0...: el ANR ocurría con
     * la app en segundo plano mientras AdMob inicializaba un WebView.
     */
    private boolean isActivityVisible() {
        return actividad != null
                && !actividad.isFinishing()
                && !actividad.isDestroyed()
                && actividad.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    private AdView adView;
    private ViewGroup bannerContainer;
    private boolean bannerLoading = false;
    private boolean bannerLoaded = false;
    private long bannerLoadedAt = 0;
    private int bannerInitRetries = 0;

    private RewardedAd rewardedAd;
    private boolean isRewardedLoading = false;
    private long rewardedLoadedAt = 0;
    private int rewardedInitRetries = 0;
    private boolean rewardedShowPending = false;
    private long rewardedPendingSince = 0;
    private Activity pendingRewardedActivity;
    private OnUserEarnedRewardListener pendingRewardListener;
    private Runnable pendingRewardedDismissListener;

    private final Map<String, NativeAd> nativeAdsMap = new ConcurrentHashMap<>();
    private final Map<String, Long> nativeLoadedAtMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loadingAdsMap = new ConcurrentHashMap<>();
    private final Map<String, ViewGroup> pendingNativeContainers = new ConcurrentHashMap<>();
    private final Map<String, Integer> nativeInitRetriesMap = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastRequestTimeMap = new ConcurrentHashMap<>();

    private boolean destroyed = false;
    private boolean consentResolved = false;
    private boolean adsAllowed = false;
    private boolean adsReady = false;

    public GestorPublicidad(AppCompatActivity actividad) {
        this.actividad = actividad;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.adRequest = new AdRequest.Builder().build();
        // El rewarded NO se precarga aquí: se hace al abrir el diálogo Pro.
        requestConsent();
    }

    private void requestConsent() {
        ConsentManager.requestConsent(
                actividad,
                canRequestAds -> {
                    if (!destroyed) {
                        onConsentState(canRequestAds);
                    }
                });
    }

    /**
     * Reaplica el estado después de que el usuario cambie su decisión en
     * "Opciones de privacidad". Si concede el consentimiento, se inicializa
     * MobileAds y se cargan los anuncios pendientes.
     */
    public void onConsentChanged() {
        if (!destroyed) {
            onConsentState(ConsentManager.canRequestAds());
        }
    }

    private void onConsentState(boolean canRequestAds) {
        consentResolved = true;
        adsAllowed = canRequestAds;
        // El flujo de UMP ya terminó. Si el usuario rechazó, será
        // isAdDisabled() quien bloquee las peticiones.
        adsReady = true;

        if (!actividad.isFinishing() && !actividad.isDestroyed()) {
            actividad.invalidateOptionsMenu();
        }

        if (canRequestAds) {
            PicApplication.initializeMobileAds(actividad);
            if (bannerContainer != null) {
                cargarBanner(bannerContainer);
            }
            if (rewardedShowPending) {
                cargarRewardedAd();
                schedulePendingRewardedTimeout();
            }
        } else {
            ocultarBanner();
            Runnable dismissed = pendingRewardedDismissListener;
            clearPendingRewarded();
            if (dismissed != null) {
                dismissed.run();
            }
        }
    }

    // --- REWARDED ---

    /**
     * Carga el anuncio de video bonificado (Rewarded Ad) en segundo plano.
     */
    public void cargarRewardedAd() {
        mainHandler.post(() -> {
            try {
                if (destroyed || !adsReady || isAdDisabled()) {
                    return;
                }
                if (!isActivityVisible()) {
                    Log.d(TAG, "RewardedAd: actividad no visible; se reanudara al volver.");
                    return;
                }
                if (!PicApplication.isMobileAdsInitialized()) {
                    if (rewardedInitRetries >= MAX_INIT_RETRIES) {
                        Log.w(TAG, "RewardedAd: MobileAds no se inicializó; se cancela la carga.");
                        return;
                    }
                    rewardedInitRetries++;
                    Log.d(TAG, "Diferido: Cargando RewardedAd tras inicialización...");
                    mainHandler.postDelayed(this::cargarRewardedAd, 2000);
                    return;
                }
                rewardedInitRetries = 0;

                if (isRewardedAdLoaded() || isRewardedLoading) {
                    return;
                }
                if (rewardedAd != null) {
                    rewardedAd = null;
                    rewardedLoadedAt = 0;
                }

                isRewardedLoading = true;
                Log.d(TAG, "Iniciando carga de RewardedAd...");
                RewardedAd.load(
                        actividad,
                        REWARDED_AD_UNIT_ID,
                        adRequest,
                        new RewardedAdLoadCallback() {
                            @Override
                            public void onAdLoaded(@NonNull RewardedAd ad) {
                                rewardedAd = ad;
                                rewardedLoadedAt = System.currentTimeMillis();
                                isRewardedLoading = false;
                                Log.d(TAG, "RewardedAd cargado con éxito.");
                                showPendingRewardedIfReady();
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                rewardedAd = null;
                                rewardedLoadedAt = 0;
                                isRewardedLoading = false;
                                Log.w(TAG, "Error cargando RewardedAd: código "
                                        + loadAdError.getCode() + " - " + loadAdError.getMessage());
                                if (rewardedShowPending) {
                                    Runnable dismissed = pendingRewardedDismissListener;
                                    clearPendingRewarded();
                                    notifyRewardedLoadFailed(dismissed);
                                }
                            }
                        });
            } catch (Exception e) {
                isRewardedLoading = false;
                Log.e(TAG, "Excepción cargando RewardedAd: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Alias retrocompatible para precarga de Rewarded Ad.
     */
    public void precargarRewardedAd() {
        cargarRewardedAd();
    }

    /**
     * Retorna true si el anuncio de video bonificado está cargado y listo para mostrarse.
     */
    public boolean isRewardedAdLoaded() {
        return rewardedAd != null
                && rewardedLoadedAt > 0
                && (System.currentTimeMillis() - rewardedLoadedAt < AD_TTL_MS);
    }

    @VisibleForTesting
    public void setRewardedAd(@Nullable RewardedAd rewardedAd) {
        this.rewardedAd = rewardedAd;
        this.rewardedLoadedAt = rewardedAd != null ? System.currentTimeMillis() : 0;
    }

    /**
     * Muestra el video de recompensa para desbloquear el Pase Pro de 12 horas.
     * Si aún no está cargado, espera a que termine la carga y lo muestra.
     *
     * @param activity             Actividad que presenta el anuncio.
     * @param rewardListener       Listener cuando el usuario completa la visualización y gana la recompensa.
     * @param onDismissedListener  Callback ejecutado cuando el anuncio se descarta o falla al mostrarse.
     */
    public void mostrarRewardedAd(
            Activity activity,
            OnUserEarnedRewardListener rewardListener,
            Runnable onDismissedListener) {

        Activity targetActivity =
                (activity != null && !activity.isFinishing() && !activity.isDestroyed())
                        ? activity
                        : this.actividad;
        if (destroyed || isAdDisabled() || targetActivity == null
                || targetActivity.isFinishing() || targetActivity.isDestroyed()) {
            if (onDismissedListener != null) {
                onDismissedListener.run();
            }
            return;
        }

        // Si el consentimiento aún no está resuelto, guardamos la intención
        // sin arrancar el temporizador; el formulario puede tardar más de 20 s.
        if (!adsReady) {
            pendingRewardedActivity = targetActivity;
            pendingRewardListener = rewardListener;
            pendingRewardedDismissListener = onDismissedListener;
            rewardedShowPending = true;
            rewardedPendingSince = System.currentTimeMillis();
            return;
        }

        mainHandler.post(() -> {
            if (isRewardedAdLoaded()) {
                showLoadedRewarded(targetActivity, rewardListener, onDismissedListener);
                return;
            }

            pendingRewardedActivity = targetActivity;
            pendingRewardListener = rewardListener;
            pendingRewardedDismissListener = onDismissedListener;
            rewardedShowPending = true;
            rewardedPendingSince = System.currentTimeMillis();
            rewardedInitRetries = 0;
            cargarRewardedAd();
            schedulePendingRewardedTimeout();
        });
    }

    private void showPendingRewardedIfReady() {
        if (!rewardedShowPending || !isRewardedAdLoaded()) {
            return;
        }

        Activity activity =
                pendingRewardedActivity != null ? pendingRewardedActivity : actividad;
        OnUserEarnedRewardListener rewardListener = pendingRewardListener;
        Runnable dismissed = pendingRewardedDismissListener;
        clearPendingRewarded();

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (dismissed != null) {
                dismissed.run();
            }
            return;
        }

        showLoadedRewarded(activity, rewardListener, dismissed);
    }

    private void showLoadedRewarded(
            Activity activity,
            OnUserEarnedRewardListener rewardListener,
            Runnable onDismissedListener) {

        RewardedAd adParaMostrar = rewardedAd;
        rewardedAd = null;
        rewardedLoadedAt = 0;

        if (adParaMostrar == null) {
            if (onDismissedListener != null) {
                onDismissedListener.run();
            }
            return;
        }

        adParaMostrar.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "RewardedAd cerrado por el usuario.");
                if (!destroyed && !isAdDisabled()) {
                    cargarRewardedAd();
                }
                if (onDismissedListener != null) {
                    onDismissedListener.run();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(
                    @NonNull com.google.android.gms.ads.AdError adError) {
                Log.w(TAG, "Fallo al mostrar RewardedAd: " + adError.getMessage());
                if (!destroyed && !isAdDisabled()) {
                    cargarRewardedAd();
                }
                if (onDismissedListener != null) {
                    onDismissedListener.run();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "RewardedAd mostrado en pantalla completa.");
            }
        });

        adParaMostrar.show(
                activity,
                rewardListener != null
                        ? rewardListener
                        : reward -> Log.d(TAG, "Recompensa otorgada sin listener."));
    }

    private void schedulePendingRewardedTimeout() {
        if (rewardedShowPending) {
            mainHandler.postDelayed(this::expirePendingRewarded, REWARDED_PENDING_TIMEOUT_MS);
        }
    }

    private void expirePendingRewarded() {
        if (!rewardedShowPending) {
            return;
        }
        if (System.currentTimeMillis() - rewardedPendingSince < REWARDED_PENDING_TIMEOUT_MS) {
            return;
        }

        Runnable dismissed = pendingRewardedDismissListener;
        clearPendingRewarded();
        notifyRewardedLoadFailed(dismissed);
    }

    private void notifyRewardedLoadFailed(@Nullable Runnable onDismissedListener) {
        if (actividad != null && !actividad.isFinishing() && !actividad.isDestroyed()) {
            Toast.makeText(
                            actividad,
                            actividad.getString(R.string.ad_loading_rewarded_video_toast),
                            Toast.LENGTH_SHORT)
                    .show();
        }
        if (onDismissedListener != null) {
            onDismissedListener.run();
        }
    }

    private void clearPendingRewarded() {
        rewardedShowPending = false;
        pendingRewardedActivity = null;
        pendingRewardListener = null;
        pendingRewardedDismissListener = null;
        rewardedPendingSince = 0;
    }

    // --- INTERSTITIAL (sin unidad asignada todavía) ---

    @Override
    public void cargarInterstitial() {
        // Sin bloque de interstitial para PIC.
    }

    @Override
    public void mostrarInterstitial() {
        // Sin bloque de interstitial para PIC.
    }

    // --- BANNER ---

    @Override
    public void pausarBanner() {
        mainHandler.post(() -> {
            if (adView != null) {
                adView.pause();
            }
        });
    }

    @Override
    public void resumenBanner() {
        mainHandler.post(() -> {
            if (isAdDisabled()) {
                if (adView != null) {
                    adView.setVisibility(View.GONE);
                }
                return;
            }
            if (adView != null) {
                adView.resume();
            }
            if (!bannerLoaded && !bannerLoading && bannerContainer != null) {
                cargarBanner(bannerContainer);
            }
        });
    }

    /**
     * Calcula el tamaño óptimo de Anchored Adaptive Banner según el ancho de pantalla del dispositivo.
     */
    private AdSize getAdaptiveBannerAdSize(Activity activity) {
        if (activity == null) {
            return AdSize.BANNER;
        }
        try {
            int adWidthPixels;
            DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
                adWidthPixels = windowMetrics.getBounds().width();
            } else {
                adWidthPixels = displayMetrics.widthPixels;
            }
            float density = displayMetrics.density;
            int adWidth = (int) (adWidthPixels / density);
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
        } catch (Exception e) {
            Log.e(TAG, "Error calculando adaptive banner size, fallback a estándar", e);
            return AdSize.BANNER;
        }
    }

    @Override
    public void cargarBanner(ViewGroup container) {
        if (container == null) {
            return;
        }
        this.bannerContainer = container;

        if (!adsReady) {
            // El consentimiento se está gestionando; se carga al terminar.
            return;
        }

        mainHandler.post(() -> {
            try {
                if (destroyed || isAdDisabled()) {
                    if (adView != null) {
                        adView.setVisibility(View.GONE);
                    }
                    return;
                }
                if (!PicApplication.isMobileAdsInitialized()) {
                    if (bannerInitRetries >= MAX_INIT_RETRIES) {
                        Log.w(TAG, "Banner: MobileAds no se inicializó; se cancela la carga.");
                        return;
                    }
                    bannerInitRetries++;
                    Log.w(TAG, "Diferido: Cargando banner tras inicialización...");
                    mainHandler.postDelayed(() -> cargarBanner(container), 2000);
                    return;
                }
                bannerInitRetries = 0;

                // Si el banner ya está en este contenedor, solo pedirlo si falta.
                if (adView != null && adView.getParent() == container) {
                    if (bannerLoaded && bannerLoadedAt > 0
                            && System.currentTimeMillis() - bannerLoadedAt >= AD_TTL_MS) {
                        bannerLoaded = false;
                    }
                    if (!bannerLoaded && !bannerLoading) {
                        bannerLoading = true;
                        adView.loadAd(adRequest);
                    }
                    return;
                }

                if (adView != null) {
                    if (adView.getParent() != null) {
                        ((ViewGroup) adView.getParent()).removeView(adView);
                    }
                    adView.destroy();
                    adView = null;
                }

                adView = new AdView(actividad);
                adView.setAdUnitId(BANNER_ID);
                adView.setAdSize(getAdaptiveBannerAdSize(actividad));
                bannerLoaded = false;

                container.removeAllViews();
                container.addView(adView);

                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        bannerLoaded = true;
                        bannerLoadedAt = System.currentTimeMillis();
                        bannerLoading = false;
                        Log.d(TAG, "Banner adaptable inferior cargado exitosamente");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        bannerLoaded = false;
                        bannerLoading = false;
                        Log.w(TAG, "Fallo al cargar banner adaptable: código "
                                + loadAdError.getCode() + " - " + loadAdError.getMessage());
                    }

                    @Override
                    public void onAdImpression() {
                        Log.d(TAG, "Impresión de banner registrada.");
                    }
                });

                bannerLoading = true;
                adView.loadAd(adRequest);
                Log.d(TAG, "Solicitud de Banner adaptable inferior enviada");

            } catch (Exception e) {
                bannerLoading = false;
                Log.e(TAG, "Error cargando banner: " + e.getMessage());
            }
        });
    }

    @Override
    public void mostrarBanner() {
        if (!adsReady) {
            return;
        }
        mainHandler.post(() -> {
            if (isAdDisabled()) {
                if (adView != null) {
                    adView.setVisibility(View.GONE);
                }
                return;
            }
            if (adView == null) {
                if (bannerContainer != null) {
                    cargarBanner(bannerContainer);
                }
                return;
            }
            adView.setVisibility(View.VISIBLE);
            if (bannerLoaded && bannerLoadedAt > 0
                    && System.currentTimeMillis() - bannerLoadedAt >= AD_TTL_MS) {
                bannerLoaded = false;
            }
            if (!bannerLoaded && !bannerLoading) {
                bannerLoading = true;
                adView.loadAd(adRequest);
            }
        });
    }

    @Override
    public void ocultarBanner() {
        mainHandler.post(() -> {
            if (adView != null) {
                adView.setVisibility(View.GONE);
            }
        });
    }

    // --- NATIVOS ---

    private boolean isNativeAdFresh(String key) {
        NativeAd ad = nativeAdsMap.get(key);
        Long loadedAt = nativeLoadedAtMap.get(key);
        if (ad == null || loadedAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - loadedAt >= AD_TTL_MS) {
            nativeAdsMap.remove(key);
            nativeLoadedAtMap.remove(key);
            ad.destroy();
            return false;
        }
        return true;
    }

    @Override
    public void precargarNativeAd(String key) {
        String adUnitId = NATIVE_IDS.get(key);
        if (adUnitId == null) {
            return;
        }

        mainHandler.post(() -> {
            try {
                if (destroyed || !adsReady || isAdDisabled()) {
                    return;
                }
                if (!isActivityVisible()) {
                    Log.d(TAG, "NativeAd (" + key + "): actividad no visible; se reanudara al volver.");
                    return;
                }
                if (!PicApplication.isMobileAdsInitialized()) {
                    Integer intentosPrevios = nativeInitRetriesMap.get(key);
                    int intentos = intentosPrevios != null ? intentosPrevios : 0;
                    if (intentos >= MAX_INIT_RETRIES) {
                        Log.w(TAG, "NativeAd (" + key + "): MobileAds no se inicializó; se cancela.");
                        return;
                    }
                    nativeInitRetriesMap.put(key, intentos + 1);
                    Log.d(TAG, "Diferido: Precargando NativeAd (" + key + ") tras inicialización...");
                    mainHandler.postDelayed(() -> precargarNativeAd(key), 2000);
                    return;
                }
                nativeInitRetriesMap.put(key, 0);

                // Evitar peticiones si ya hay una en curso o si el anuncio sigue fresco.
                if (Boolean.TRUE.equals(loadingAdsMap.get(key))) {
                    Log.d(TAG, "NativeAd (" + key + ") ya se está cargando. Ignorando...");
                    return;
                }
                if (isNativeAdFresh(key)) {
                    Log.d(TAG, "NativeAd (" + key + ") ya disponible en caché.");
                    return;
                }

                // Throttling: evitar peticiones consecutivas muy rápidas (por errores en bucle)
                long now = System.currentTimeMillis();
                Long ultimoRegistro = lastRequestTimeMap.get(key);
                long lastRequest = ultimoRegistro != null ? ultimoRegistro : 0L;
                if (now - lastRequest < MIN_REQUEST_INTERVAL) {
                    Log.w(TAG, "Solicitud de NativeAd (" + key + ") muy frecuente. Esperando cooldown.");
                    return;
                }

                Log.d(TAG, "Iniciando precarga de NativeAd: " + key);
                loadingAdsMap.put(key, true);
                lastRequestTimeMap.put(key, now);

                AdLoader adLoader = new AdLoader.Builder(actividad, adUnitId)
                        .forNativeAd(nativeAd -> {
                            loadingAdsMap.put(key, false);
                            NativeAd oldAd = nativeAdsMap.put(key, nativeAd);
                            nativeLoadedAtMap.put(key, System.currentTimeMillis());
                            if (oldAd != null) {
                                oldAd.destroy();
                            }
                            Log.d(TAG, "NativeAd precargado con éxito: " + key);

                            // Si un diálogo está esperando este anuncio, mostrarlo ya.
                            ViewGroup pending = pendingNativeContainers.remove(key);
                            if (pending != null) {
                                nativeAdsMap.remove(key);
                                nativeLoadedAtMap.remove(key);
                                renderNativeAd(nativeAd, pending);
                            }
                        })
                        .withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(LoadAdError adError) {
                                loadingAdsMap.put(key, false);
                                Log.e(TAG, "Fallo precarga NativeAd (" + key + "): "
                                        + adError.getMessage() + " Code: " + adError.getCode());
                                ViewGroup pending = pendingNativeContainers.remove(key);
                                if (pending != null) {
                                    showNativePlaceholder(pending);
                                }
                            }
                        })
                        .withNativeAdOptions(new NativeAdOptions.Builder()
                                .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                                .build())
                        .build();

                adLoader.loadAd(adRequest);
            } catch (Exception e) {
                loadingAdsMap.put(key, false);
                Log.e(TAG, "Error en precarga NativeAd: " + e.getMessage());
            }
        });
    }

    @Override
    public void mostrarNativeAd(String key, ViewGroup container) {
        if (container == null) {
            return;
        }

        mainHandler.post(() -> {
            if (destroyed || !adsReady || isAdDisabled()) {
                showNativePlaceholder(container);
                return;
            }

            container.removeAllViews();

            if (isNativeAdFresh(key)) {
                NativeAd ad = nativeAdsMap.remove(key);
                nativeLoadedAtMap.remove(key);
                Log.d(TAG, "Mostrando NativeAd desde caché: " + key);
                renderNativeAd(ad, container);
                return;
            }

            // Si ya hay una carga en curso, esperar a que termine y mostrarla aquí.
            if (Boolean.TRUE.equals(loadingAdsMap.get(key))) {
                Log.d(TAG, "NativeAd (" + key + ") en carga; se mostrará en este contenedor.");
                showNativePlaceholder(container);
                pendingNativeContainers.put(key, container);
                return;
            }

            Log.w(TAG, "NativeAd no listo para: " + key + ". Cargando dinámicamente...");
            loadAndShowNativeAd(key, container);
        });
    }

    private void loadAndShowNativeAd(String key, ViewGroup container) {
        String adUnitId = NATIVE_IDS.get(key);
        if (adUnitId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long ultimoRegistro = lastRequestTimeMap.get(key);
        long lastRequest = ultimoRegistro != null ? ultimoRegistro : 0L;
        if (now - lastRequest < MIN_REQUEST_INTERVAL) {
            Log.w(TAG, "Carga dinámica de NativeAd (" + key + ") en cooldown; se muestra placeholder.");
            showNativePlaceholder(container);
            return;
        }

        loadingAdsMap.put(key, true);
        lastRequestTimeMap.put(key, now);
        showNativePlaceholder(container);

        AdLoader adLoader = new AdLoader.Builder(actividad, adUnitId)
                .forNativeAd(nativeAd -> {
                    loadingAdsMap.put(key, false);
                    renderNativeAd(nativeAd, container);
                    Log.d(TAG, "NativeAd cargado y mostrado dinámicamente: " + key);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        loadingAdsMap.put(key, false);
                        Log.e(TAG, "Fallo carga dinámica NativeAd (" + key + "): "
                                + adError.getMessage());
                        showNativePlaceholder(container);
                    }
                })
                .build();
        adLoader.loadAd(adRequest);
    }

    private void renderNativeAd(NativeAd nativeAd, ViewGroup container) {
        try {
            // Limpiar vistas previas para evitar duplicados
            container.removeAllViews();

            // Inflar CON parent para que los LayoutParams se apliquen correctamente
            NativeAdView adView = (NativeAdView) actividad.getLayoutInflater()
                    .inflate(R.layout.layout_native_ad, container, false);

            // Registrar vistas de assets con el NativeAdView
            TextView headlineView = adView.findViewById(R.id.ad_headline);
            TextView bodyView = adView.findViewById(R.id.ad_body);
            TextView ctaView = adView.findViewById(R.id.ad_call_to_action);
            ImageView iconView = adView.findViewById(R.id.ad_app_icon);
            com.google.android.gms.ads.nativead.MediaView mediaView = adView.findViewById(R.id.ad_media);
            TextView advertiserView = adView.findViewById(R.id.ad_advertiser);

            adView.setHeadlineView(headlineView);
            adView.setBodyView(bodyView);
            adView.setCallToActionView(ctaView);
            adView.setIconView(iconView);
            adView.setMediaView(mediaView);
            adView.setAdvertiserView(advertiserView);

            // Rellenar titulo (obligatorio)
            if (headlineView != null && nativeAd.getHeadline() != null) {
                headlineView.setText(nativeAd.getHeadline());
            }

            // Rellenar cuerpo (recomendado)
            if (bodyView != null) {
                if (nativeAd.getBody() != null) {
                    bodyView.setText(nativeAd.getBody());
                    bodyView.setVisibility(View.VISIBLE);
                } else {
                    bodyView.setVisibility(View.GONE);
                }
            }

            // Rellenar boton de accion (obligatorio)
            if (ctaView != null) {
                if (nativeAd.getCallToAction() != null) {
                    ctaView.setText(nativeAd.getCallToAction());
                    ctaView.setVisibility(View.VISIBLE);
                } else {
                    ctaView.setVisibility(View.GONE);
                }
            }

            // Rellenar icono (obligatorio si se proporciona)
            if (iconView != null) {
                if (nativeAd.getIcon() != null) {
                    iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
                    iconView.setVisibility(View.VISIBLE);
                } else {
                    iconView.setVisibility(View.GONE);
                }
            }

            // Rellenar nombre del anunciante (recomendado)
            if (advertiserView != null) {
                if (nativeAd.getAdvertiser() != null) {
                    advertiserView.setText(nativeAd.getAdvertiser());
                    advertiserView.setVisibility(View.VISIBLE);
                } else {
                    advertiserView.setVisibility(View.GONE);
                }
            }

            // Escalar imagenes para llenar el espacio del MediaView
            if (mediaView != null) {
                mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
            }

            // Forzar que el NativeAdView llene su contenedor completamente
            adView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            // IMPORTANTE: añadir la vista al contenedor ANTES de registrar el
            // anuncio. La impresión se cuenta cuando el NativeAdView está
            // attached y visible.
            container.addView(adView);

            if (container.isAttachedToWindow()) {
                if (!destroyed) {
                    adView.setNativeAd(nativeAd);
                }
            } else {
                container.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(@NonNull View v) {
                        container.removeOnAttachStateChangeListener(this);
                        if (!destroyed) {
                            adView.setNativeAd(nativeAd);
                        }
                    }

                    @Override
                    public void onViewDetachedFromWindow(@NonNull View v) {
                        // Nada: el anuncio se destruye desde destruirPublicidad().
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al renderizar NativeAd: " + e.getMessage());
            showNativePlaceholder(container);
        }
    }

    private void showNativePlaceholder(ViewGroup container) {
        container.removeAllViews();
        TextView placeholder = new TextView(actividad);
        placeholder.setText(actividad.getString(R.string.publicidad_recomendada)); // Un texto más amigable
        placeholder.setPadding(30, 30, 30, 30);
        placeholder.setTextColor(Color.GRAY);
        container.addView(placeholder);
    }

    @Override
    public void destruirPublicidad() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        for (NativeAd ad : nativeAdsMap.values()) {
            ad.destroy();
        }
        nativeAdsMap.clear();
        nativeLoadedAtMap.clear();
        loadingAdsMap.clear();
        pendingNativeContainers.clear();
        rewardedAd = null;
        rewardedLoadedAt = 0;
        isRewardedLoading = false;
        clearPendingRewarded();
    }
}
