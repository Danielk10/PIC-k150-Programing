package com.diamon.publicidad;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.diamon.nucleo.Publicidad;
import com.diamon.pic.PicApplication;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

/**
 * Gestor de publicidad de Google Mobile Ads.
 * 
 * Optimizado para evitar bloqueo del hilo principal:
 * - Inicialización diferida de MobileAds si no fue inicializado en Application
 * - Carga de banner diferida después de que la UI esté renderizada
 * - Manejo seguro de errores
 */
public class MostrarPublicidad implements Publicidad {

    private static final String TAG = "MostrarPublicidad";
    private static final String AD_UNIT_ID = "ca-app-pub-5141499161332805/5248084133";

    // Delay antes de crear AdView para permitir que la UI se renderice
    private static final long BANNER_INIT_DELAY_MS = 500;

    private final AppCompatActivity actividad;
    private AdView adView;
    private AdRequest adRequest;
    private boolean isInitialized = false;

    public MostrarPublicidad(AppCompatActivity actividad) {
        this.actividad = actividad;

        // Diferir la inicialización del AdView para evitar bloqueo del main thread
        initializeAdsDeferred();
    }

    /**
     * Inicializa MobileAds y crea el AdView de forma diferida.
     * Esto evita bloquear el hilo principal durante el onCreate de la Activity.
     */
    private void initializeAdsDeferred() {
        // Usar View.post() para esperar a que la UI esté lista
        actividad.getWindow().getDecorView().post(() -> {
            // Segundo delay para asegurar que la UI esté completamente renderizada
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // Verificar si MobileAds ya fue inicializado en Application
                    if (!PicApplication.isMobileAdsInitialized()) {
                        // Inicializar si no fue hecho en Application
                        MobileAds.initialize(actividad, initializationStatus -> {
                            Log.d(TAG, "MobileAds initialized from MostrarPublicidad");
                            createAdView();
                        });
                    } else {
                        // Ya inicializado, crear AdView directamente
                        createAdView();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing ads: " + e.getMessage());
                }
            }, BANNER_INIT_DELAY_MS);
        });
    }

    public interface BannerListener {
        void onBannerLoaded(AdView banner);
    }

    private BannerListener bannerListener;

    public void setBannerListener(BannerListener listener) {
        this.bannerListener = listener;
        // Si ya esta listo, notificar de inmediato
        if (isInitialized && adView != null) {
            listener.onBannerLoaded(adView);
        }
    }

    /**
     * Crea el AdView y prepara el AdRequest.
     * Debe ser llamado después de que MobileAds esté inicializado.
     */
    private void createAdView() {
        try {
            actividad.runOnUiThread(() -> {
                adView = new AdView(actividad);
                adView.setAdUnitId(AD_UNIT_ID);
                adView.setAdSize(AdSize.BANNER);
                adView.setId(View.generateViewId());
                adRequest = new AdRequest.Builder().build();
                isInitialized = true;
                Log.d(TAG, "AdView created successfully");

                // Notificar listener si existe
                if (bannerListener != null) {
                    bannerListener.onBannerLoaded(adView);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating AdView: " + e.getMessage());
        }
    }

    @Override
    public void mostrarInterstitial() {
    }

    @Override
    public void cargarInterstitial() {
    }

    @Override
    public void cargarBanner() {
        if (!isInitialized || adView == null) {
            // Si aún no está inicializado, reintentar después de un delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isInitialized && adView != null && adRequest != null) {
                    adView.loadAd(adRequest);
                }
            }, 1000);
            return;
        }

        actividad.runOnUiThread(() -> {
            try {
                if (adView != null && adRequest != null) {
                    adView.loadAd(adRequest);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading banner: " + e.getMessage());
            }
        });
    }

    public AdView getBanner() {
        return adView;
    }

    @Override
    public void mostrarBanner() {
        actividad.runOnUiThread(() -> {
            if (adView != null) {
                adView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void ocultarBanner() {
        actividad.runOnUiThread(() -> {
            if (adView != null) {
                adView.setVisibility(View.GONE);
            }
        });
    }

    public AdRequest getAdReques() {
        return this.adRequest;
    }

    public void resumenBanner() {
        actividad.runOnUiThread(() -> {
            if (adView != null) {
                adView.resume();
            }
        });
    }

    public void pausarBanner() {
        actividad.runOnUiThread(() -> {
            if (adView != null) {
                adView.pause();
            }
        });
    }

    public void disposeBanner() {
        actividad.runOnUiThread(() -> {
            if (adView != null) {
                adView.destroy();
                adView = null;
                isInitialized = false;
            }
        });
    }

    /**
     * Verifica si el banner está inicializado y listo para usar.
     * 
     * @return true si el AdView está creado y listo
     */
    public boolean isReady() {
        return isInitialized && adView != null;
    }
}
