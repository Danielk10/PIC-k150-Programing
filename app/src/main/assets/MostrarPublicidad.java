package com.diamon.publicidad;

import android.view.View;

import androidx.annotation.NonNull;

import com.diamon.helicoptero.Publicidad;
import com.diamon.helicoptero.android.AndroidLauncher;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MostrarPublicidad implements Publicidad {

    private static final String AD_UNIT_ID = "ca-app-pub-5141499161332805/3936571775";
    private AndroidLauncher actividad;
    private AdView adView;
    private InterstitialAd interstitialAd;

    public MostrarPublicidad(AndroidLauncher actividad) {
        this.actividad = actividad;

        // Configurar el banner
        actividad.runOnUiThread(
                () -> {
                    adView = new AdView(actividad);
                    adView.setAdSize(AdSize.BANNER);
                    adView.setAdUnitId(AD_UNIT_ID); // Banner de prueba

                    AdRequest adRequest = new AdRequest.Builder().build();
                    adView.loadAd(adRequest);

                    
                    adView.setVisibility(View.GONE); // Oculto inicialmente
                });

        // Configurar el interstitial
        actividad.runOnUiThread(
                () -> {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    InterstitialAd.load(
                            actividad,
                            "ca-app-pub-3940256099942544/1033173712", 
                            adRequest,
                            new InterstitialAdLoadCallback() {
                                @Override
                                public void onAdLoaded(@NonNull InterstitialAd ad) {
                                    interstitialAd = ad;
                                }

                                @Override
                                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                    interstitialAd = null;
                                }
                            });
                });
    }

    @Override
    public void mostrarBanner() {
        actividad.runOnUiThread(
                () -> {
                    if (adView != null) {
                        adView.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void ocultarBanner() {
        actividad.runOnUiThread(
                () -> {
                    if (adView != null) {
                        adView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void mostrarInterstitial() {
        actividad.runOnUiThread(
                () -> {
                    if (interstitialAd != null) {
                        interstitialAd.show(actividad);
                    } else {
                        // Opcional: recargar si aún no está disponible
                        AdRequest adRequest = new AdRequest.Builder().build();
                        InterstitialAd.load(
                                actividad,
                                "ca-app-pub-3940256099942544/1033173712",
                                adRequest,
                                new InterstitialAdLoadCallback() {
                                    @Override
                                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                                        interstitialAd = ad;
                                    }

                                    @Override
                                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                        interstitialAd = null;
                                    }
                                });
                    }
                });
    }

    
    public AdView getBanner()
    {
        
        return adView;
        
    }
    
    @Override
    public void botonAtrasInterstitial() {}

    @Override
    public void cargarBanner() {}

    @Override
    public void iniciarActividad() {}
}
