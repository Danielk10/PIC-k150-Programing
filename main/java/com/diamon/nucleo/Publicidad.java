package com.diamon.nucleo;

public interface Publicidad {

    public void cargarInterstitial();

    public void mostrarInterstitial();

    public void cargarBanner(android.view.ViewGroup container);

    public void mostrarBanner();

    public void ocultarBanner();

    public void precargarNativeAd(String key);

    public void mostrarNativeAd(String key, android.view.ViewGroup container);

    public void pausarBanner();

    public void resumenBanner();

    public void destruirPublicidad();
}
