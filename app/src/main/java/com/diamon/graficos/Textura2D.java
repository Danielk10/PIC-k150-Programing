package com.diamon.graficos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.diamon.nucleo.Graficos;
import com.diamon.nucleo.Graficos.FormatoTextura;
import com.diamon.nucleo.Textura;

public class Textura2D implements Textura {

    private Bitmap bitmap;

    private Graficos.FormatoTextura formatoTextura;

    @SuppressWarnings("deprecation")
    public Textura2D(float ancho, float alto, Graficos.FormatoTextura formatoTextura) {

        Bitmap.Config config;

        this.formatoTextura = formatoTextura;

        if (formatoTextura == Graficos.FormatoTextura.RGB565) {

            config = Bitmap.Config.RGB_565;

            this.bitmap = Bitmap.createBitmap((int) ancho, (int) alto, config);

        } else if (formatoTextura == Graficos.FormatoTextura.ARGB4444) {

            config = Bitmap.Config.ARGB_4444;

            this.bitmap = Bitmap.createBitmap((int) ancho, (int) alto, config);

        } else {

            config = Bitmap.Config.ARGB_8888;

            this.bitmap = Bitmap.createBitmap((int) ancho, (int) alto, config);
        }

        new BitmapFactory.Options().inPreferredConfig = config;
    }

    @SuppressWarnings("deprecation")
    public Textura2D(Bitmap bitmap) {

        if (bitmap.getConfig() == Bitmap.Config.RGB_565) {

            formatoTextura = Graficos.FormatoTextura.RGB565;

            this.bitmap = Bitmap.createBitmap(bitmap);

        } else if (bitmap.getConfig() == Bitmap.Config.ARGB_4444) {

            formatoTextura = Graficos.FormatoTextura.ARGB4444;

            this.bitmap = Bitmap.createBitmap(bitmap);

        } else {

            formatoTextura = Graficos.FormatoTextura.ARGB8888;

            this.bitmap = Bitmap.createBitmap(bitmap);
        }

        new BitmapFactory.Options().inPreferredConfig = bitmap.getConfig();
    }

    @SuppressWarnings("deprecation")
    public Textura2D(Bitmap bitmap, float ancho, float alto) {

        float w = bitmap.getWidth();

        float h = bitmap.getHeight();

        float sw = ancho / w;

        float sh = alto / h;

        Matrix max = new Matrix();

        max.postScale(sw, sh);

        // Obtén la configuración del bitmap
        Bitmap.Config config = bitmap.getConfig();

        // Asigna el formato de textura basado en la configuración
        if (config == Bitmap.Config.RGB_565) {
            formatoTextura = Graficos.FormatoTextura.RGB565;
        } else if (config == Bitmap.Config.ARGB_4444) {
            formatoTextura = Graficos.FormatoTextura.ARGB4444;
        } else {
            formatoTextura = Graficos.FormatoTextura.ARGB8888;
        }

        this.bitmap = Bitmap.createBitmap(bitmap, 0, 0, (int) w, (int) h, max, false);

        new BitmapFactory.Options().inPreferredConfig = this.bitmap.getConfig();
    }

    @SuppressWarnings("deprecation")
    public Textura2D(Textura textura, float ancho, float alto) {

        float w = textura.getAncho();

        float h = textura.getAlto();

        float sw = ancho / w;

        float sh = alto / h;

        Matrix max = new Matrix();

        max.postScale(sw, sh);

        // Obtén la configuración del bitmap
        Bitmap.Config config = textura.getBipmap().getConfig();

        // Asigna el formato de textura basado en la configuración
        if (config == Bitmap.Config.RGB_565) {
            formatoTextura = Graficos.FormatoTextura.RGB565;
        } else if (config == Bitmap.Config.ARGB_4444) {
            formatoTextura = Graficos.FormatoTextura.ARGB4444;
        } else {
            formatoTextura = Graficos.FormatoTextura.ARGB8888;
        }

        this.bitmap = Bitmap.createBitmap(textura.getBipmap(), 0, 0, (int) w, (int) h, max, false);

        new BitmapFactory.Options().inPreferredConfig = this.bitmap.getConfig();
    }

    @Override
    public FormatoTextura getFormatoTextura() {

        return this.formatoTextura;
    }

    @Override
    public Bitmap getBipmap() {

        return bitmap;
    }

    @Override
    public float getAncho() {

        return (float) bitmap.getWidth();
    }

    @Override
    public float getAlto() {

        return (float) bitmap.getHeight();
    }

    @Override
    public void dispose() {

        bitmap.recycle();
    }
}
