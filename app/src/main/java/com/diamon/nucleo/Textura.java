package com.diamon.nucleo;

import android.graphics.Bitmap;

public interface Textura {

    public abstract Graficos.FormatoTextura getFormatoTextura();

    public abstract Bitmap getBipmap();

    public abstract float getAncho();

    public abstract float getAlto();

    public abstract void dispose();
}
