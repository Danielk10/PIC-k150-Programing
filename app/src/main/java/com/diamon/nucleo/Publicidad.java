package com.diamon.nucleo;

public interface Publicidad {

    public void cargarInterstitial();

    public void mostrarInterstitial();

    public void cargarBanner(android.view.ViewGroup container);

    public void mostrarBanner();

    public void ocultarBanner();

    /**
     * Oculta y destruye de inmediato el banner y todos los anuncios nativos
     * (tarjeta compacta y tarjetas de diálogos). Se usa al activar el pase
     * Pro o al comprar "Quitar anuncios", para que ninguna carga en curso
     * pueda volver a mostrar un nativo.
     */
    public void ocultarTodosLosAnuncios();

    public void precargarNativeAd(String key);

    public void mostrarNativeAd(String key, android.view.ViewGroup container);

    public void mostrarNativeAdCompacto(String key, android.view.ViewGroup container);

    public void pausarBanner();

    public void resumenBanner();

    public void destruirPublicidad();
}
