package com.diamon.publicidad;

import androidx.appcompat.app.AppCompatActivity;

import com.diamon.nucleo.Publicidad;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class MostrarPublicidad implements Publicidad {

    private AppCompatActivity actividad;

    private static final String AD_UNIT_ID = "ca-app-pub-5141499161332805/9306546396";

    private AdView adView;

    private AdRequest adRequest;

    public MostrarPublicidad(AppCompatActivity actividad) {

        this.actividad = actividad;

        MobileAds.initialize(
                actividad,
                new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(
                            InitializationStatus initializationStatus) {}
                });

        adView = new AdView(actividad);
        adView.setAdUnitId(AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);
        adRequest = new AdRequest.Builder().build();
    }

    @Override
    public void mostrarInterstitial() {}

    @Override
    public void cargarInterstitial() {}

    @Override
    public void cargarBanner() {

        adView.loadAd(this.adRequest);
    }

    public AdView getBanner() {

        return adView;
    }

    @Override
    public void mostrarBanner() {}

    @Override
    public void ocultarBanner() {}

    public AdRequest getAdReques() {

        return this.adRequest;
    }

    public void resumenBanner() {

        if (adView != null) {

            adView.resume();
        }
    }

    public void pausarBanner() {

        if (adView != null) {

            adView.pause();
        }
    }

    public void disposeBanner() {

        if (adView != null) {

            adView.destroy();
        }
    }
}
