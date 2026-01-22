package com.diamon.publicidad;

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
                Log.d(TAG, "Banner cargado correctamente");

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
            if (!PicApplication.isMobileAdsInitialized()) {
                Log.d(TAG, "NativeAd precarga diferida para: " + key);
                return;
            }

            AdLoader adLoader = new AdLoader.Builder(actividad, adUnitId)
                    .forNativeAd(nativeAd -> {
                        NativeAd oldAd = nativeAdsMap.put(key, nativeAd);
                        if (oldAd != null)
                            oldAd.destroy();
                        Log.d(TAG, "NativeAd precargado: " + key);
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(LoadAdError adError) {
                            Log.e(TAG, "Fallo precarga NativeAd (" + key + "): " + adError.getMessage());
                        }
                    })
                    .withNativeAdOptions(new NativeAdOptions.Builder()
                            .setVideoOptions(new VideoOptions.Builder().setStartMuted(true).build())
                            .build())
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());
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
                renderNativeAd(ad, container);
                // Precargar el siguiente una vez usado
                precargarNativeAd(key);
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

        AdLoader adLoader = new AdLoader.Builder(actividad, adUnitId)
                .forNativeAd(nativeAd -> {
                    renderNativeAd(nativeAd, container);
                    Log.d(TAG, "NativeAd cargado y mostrado dinámicamente: " + key);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        showNativePlaceholder(container);
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void renderNativeAd(NativeAd nativeAd, ViewGroup container) {
        NativeAdView adView = (NativeAdView) actividad.getLayoutInflater()
                .inflate(R.layout.layout_native_ad, null);

        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

        if (nativeAd.getIcon() != null) {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        } else {
            adView.getIconView().setVisibility(View.GONE);
        }

        adView.setNativeAd(nativeAd);
        container.addView(adView);
    }

    private void showNativePlaceholder(ViewGroup container) {
        container.removeAllViews();
        TextView placeholder = new TextView(actividad);
        placeholder.setText("Anuncio no disponible");
        placeholder.setPadding(32, 32, 32, 32);
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
        });
    }
}
