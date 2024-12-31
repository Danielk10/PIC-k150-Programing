package com.diamon.nucleo;

import android.graphics.Canvas;
import android.graphics.Paint;

public interface Graficos {

    public enum FormatoTextura {
        ARGB8888,
        ARGB4444,
        RGB565
    }

    public abstract void limpiar(int color);

    public abstract void dibujarPixel(float x, float y, int color);

    public abstract void dibujarLinea(float x, float y, float x1, float y1, int color);

    public abstract void dibujarRectangulo(float x, float y, float ancho, float alto, int color);

    public abstract void dibujarTexto(String texto, float x, float y, int color);

    public abstract void dibujarTextura(Textura textura, float x, float y);

    public abstract void dibujarTextura(
            Textura textura, float x, float y, float x1, float y1, float ancho, float alto);

    public abstract Paint getLapiz();

    public abstract float getAncho();

    public abstract float getAlto();

    public abstract Textura crearTextura(float ancho, float alto, FormatoTextura formatoTextura);

    public abstract Canvas getCanvas();
}
