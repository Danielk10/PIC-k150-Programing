package com.diamon.publicidad;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.diamon.nucleo.Publicidad;
import com.diamon.pic.PicApplication;
import com.diamon.pic.R;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor CENTRALIZADO de publicidad de Google Mobile Ads.
 * Gestiona Banner, Interstitial y Native Ads en un solo lugar.
 */
public class MostrarPublicidad implements Publicidad {

    private static final String TAG = "MostrarPublicidad";

    // IDs de Unidades de Anuncio
    private static final String BANNER_ID = "ca-app-pub-5141499161332805/5248084133";
    public static final String KEY_NATIVE_MEMORY = "memory_native";
    public static final String KEY_NATIVE_PROGRAMMING = "programming_native";

    private static final Map<String, String> NATIVE_IDS = new HashMap<String, String>() {
        {
            put(KEY_NATIVE_MEMORY, "ca-app-pub-5141499161332805/1625082944");
            put(KEY_NATIVE_PROGRAMMING, "ca-app-pub-5141499161332805/2642812533");
        }
    };

    private final AppCompatActivity actividad;
    private final Handler mainHandler;

    private AdView adView;
    private final Map<String, NativeAd> nativeAdsMap = new HashMap<>();
    private final Map<String, Boolean> loadingAdsMap = new HashMap<>();
    private static final Map<String, Long> lastRequestTimeMap = new HashMap<>();
    private static final long MIN_REQUEST_INTERVAL = 10000; // 10 segundos entre peticiones para la misma unidad

    public MostrarPublicidad(AppCompatActivity actividad) {
        this.actividad = actividad;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void cargarInterstitial() {
        // Implementar lógica de Interstitial si es necesario en el futuro
    }

    @Override
    public void mostrarInterstitial() {
        // Implementar lógica de Interstitial si es necesario en el futuro
    }

    @Override
    public void pausarBanner() {
        mainHandler.post(() -> {
            if (adView != null)
                adView.pause();
        });
    }

    @Override
    public void resumenBanner() {
        mainHandler.post(() -> {
            if (adView != null)
                adView.resume();
        });
    }

    @Override
    public void cargarBanner(ViewGroup container) {
        if (container == null)
            return;

        mainHandler.post(() -> {
            try {
                if (!PicApplication.isMobileAdsInitialized()) {
                    Log.w(TAG, "Diferido: Cargando banner tras inicialización...");
                    mainHandler.postDelayed(() -> cargarBanner(container), 2000);
                    return;
                }

                if (adView != null) {
                    if (adView.getParent() != null) {
                        ((ViewGroup) adView.getParent()).removeView(adView);
                    }
                    adView.destroy();
                }

                adView = new AdView(actividad);
                adView.setAdUnitId(BANNER_ID);
                adView.setAdSize(AdSize.BANNER);

                container.removeAllViews();
                container.addView(adView);

                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
                Log.d(TAG, "Solicitud de Banner enviada");

            } catch (Exception e) {
                Log.e(TAG, "Error cargando banner: " + e.getMessage());
            }
        });
    }

    @Override
    public void mostrarBanner() {
        mainHandler.post(() -> {
            if (adView != null)
                adView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void ocultarBanner() {
        mainHandler.post(() -> {
            if (adView != null)
                adView.setVisibility(View.GONE);
        });
    }

    @Override
    public void precargarNativeAd(String key) {
        String adUnitId = NATIVE_IDS.get(key);
        if (adUnitId == null)
            return;

        mainHandler.post(() -> {
            try {
                if (!PicApplication.isMobileAdsInitialized()) {
                    Log.d(TAG, "Diferido: Precargando NativeAd (" + key + ") tras inicialización...");
                    mainHandler.postDelayed(() -> precargarNativeAd(key), 2000);
                    return;
                }

                // Evitar peticiones si ya hay una en curso o si el anuncio ya existe
                if (Boolean.TRUE.equals(loadingAdsMap.get(key))) {
                    Log.d(TAG, "NativeAd (" + key + ") ya se está cargando. Ignorando...");
                    return;
                }

                if (nativeAdsMap.containsKey(key)) {
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
                            NativeAd oldAd = nativeAdsMap.put(key, nativeAd);
                            if (oldAd != null)
                                oldAd.destroy();
                            loadingAdsMap.put(key, false);
                            Log.d(TAG, "NativeAd precargado con éxito: " + key);
                        })
                        .withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(LoadAdError adError) {
                                loadingAdsMap.put(key, false);
                                Log.e(TAG, "Fallo precarga NativeAd (" + key + "): " + adError.getMessage() + " Code: "
                                        + adError.getCode());
                            }
                        })
                        .withNativeAdOptions(new NativeAdOptions.Builder()
                                .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                                .build())
                        .build();

                adLoader.loadAd(new AdRequest.Builder().build());
            } catch (Exception e) {
                loadingAdsMap.put(key, false);
                Log.e(TAG, "Error en precarga NativeAd: " + e.getMessage());
            }
        });
    }

    @Override
    public void mostrarNativeAd(String key, ViewGroup container) {
        if (container == null)
            return;

        mainHandler.post(() -> {
            container.removeAllViews();
            NativeAd ad = nativeAdsMap.get(key);

            if (ad != null) {
                Log.d(TAG, "Mostrando NativeAd desde caché: " + key);
                renderNativeAd(ad, container);
                // Remover de la caché para que se use una sola vez
                nativeAdsMap.remove(key);
                // Iniciar precarga del SIQUIENTE anuncio para la próxima vez
                // con un pequeño delay para no saturar si el usuario abre/cierra rápido
                mainHandler.postDelayed(() -> precargarNativeAd(key), 3000);
            } else {
                Log.w(TAG, "NativeAd no listo para: " + key + ". Cargando dinámicamente...");
                loadAndShowNativeAd(key, container);
            }
        });
    }

    private void loadAndShowNativeAd(String key, ViewGroup container) {
        String adUnitId = NATIVE_IDS.get(key);
        if (adUnitId == null)
            return;

        // Si ya se está cargando, esperar a que termine (el listener original se
        // encargará)
        // pero aquí necesitamos mostrarlo en ESTE contenedor.
        // Por sencillez, si no está en caché pedimos uno nuevo asegurando el
        // throttling.

        loadingAdsMap.put(key, true);
        lastRequestTimeMap.put(key, System.currentTimeMillis());

        AdLoader adLoader = new AdLoader.Builder(actividad, adUnitId)
                .forNativeAd(nativeAd -> {
                    loadingAdsMap.put(key, false);
                    renderNativeAd(nativeAd, container);
                    Log.d(TAG, "NativeAd cargado y mostrado dinámicamente: " + key);
                    // No lo guardamos en caché porque ya se está mostrando
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        loadingAdsMap.put(key, false);
                        Log.e(TAG, "Fallo carga dinámica NativeAd (" + key + "): " + adError.getMessage());
                        showNativePlaceholder(container);
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
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

            // Registrar el anuncio con la vista
            adView.setNativeAd(nativeAd);

            // Escalar imagenes para llenar el espacio del MediaView
            if (mediaView != null) {
                mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
            }

            // Forzar que el NativeAdView llene su contenedor completamente
            adView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            // Agregar al contenedor
            container.addView(adView);
        } catch (Exception e) {
            Log.e(TAG, "Error al renderizar NativeAd: " + e.getMessage());
            showNativePlaceholder(container);
        }
    }

    private void showNativePlaceholder(ViewGroup container) {
        container.removeAllViews();
        TextView placeholder = new TextView(actividad);
        placeholder.setText("Publicidad recomendada"); // Un texto más amigable
        placeholder.setPadding(30, 30, 30, 30);
        placeholder.setTextColor(Color.GRAY);
        container.addView(placeholder);
    }

    @Override
    public void destruirPublicidad() {
        mainHandler.post(() -> {
            if (adView != null) {
                adView.destroy();
                adView = null;
            }
            for (NativeAd ad : nativeAdsMap.values()) {
                ad.destroy();
            }
            nativeAdsMap.clear();
            loadingAdsMap.clear();
        });
    }
}
