package com.diamon.graficos;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.diamon.nucleo.Graficos;
import com.diamon.nucleo.Textura;

public class Graficos2D implements Graficos {

    private Canvas canvas;

    private Paint paint;

    private Textura textura;

    private Rect dstRect;

    private Rect srcRect;

    public Graficos2D(Textura textura) {

        this.textura = textura;

        this.canvas = new Canvas(textura.getBipmap());

        this.paint = new Paint();

        this.dstRect = new Rect();

        this.srcRect = new Rect();
    }

    @Override
    public void limpiar(int color) {

        this.canvas.drawRGB((color & 0xff0000) >> 16, (color & 0xff00) >> 8, (color & 0xff));
    }

    @Override
    public void dibujarPixel(float x, float y, int color) {

        this.paint.setColor(color);

        this.canvas.drawPoint(x, y, this.paint);
    }

    @Override
    public void dibujarLinea(float x, float y, float x1, float y1, int color) {

        this.paint.setColor(color);

        this.canvas.drawLine(x, y, x1, y1, this.paint);
    }

    @Override
    public void dibujarRectangulo(float x, float y, float ancho, float alto, int color) {

        this.paint.setColor(color);

        this.paint.setStyle(Paint.Style.FILL);

        this.canvas.drawRect(x, y, ((x + ancho) - 1), ((y + alto) - 1), this.paint);
    }

    @Override
    public void dibujarTexto(String texto, float x, float y, int color) {

        this.paint.setColor(color);

        this.canvas.drawText(texto, x, y, paint);
    }

    @Override
    public void dibujarTextura(Textura textura, float x, float y) {

        this.canvas.drawBitmap(textura.getBipmap(), x, y, null);
    }

    @Override
    public void dibujarTextura(
            Textura textura, float x, float y, float x1, float y1, float ancho, float alto) {

        this.srcRect.left = (int) x1;

        this.srcRect.top = (int) y1;

        this.srcRect.right = (int) ((x1 + ancho) - 1);

        this.srcRect.bottom = (int) ((y1 + alto) - 1);

        this.dstRect.left = (int) x;

        this.dstRect.top = (int) y;

        this.dstRect.right = (int) ((x + ancho) - 1);

        this.dstRect.bottom = (int) ((y + alto) - 1);

        this.canvas.drawBitmap(textura.getBipmap(), this.srcRect, this.dstRect, null);
    }

    @Override
    public Paint getLapiz() {

        return this.paint;
    }

    @Override
    public float getAncho() {

        return textura.getAncho();
    }

    @Override
    public float getAlto() {

        return textura.getAlto();
    }

    @Override
    public Textura crearTextura(float ancho, float alto, FormatoTextura formatoTextura) {

        textura = new Textura2D(alto, alto, formatoTextura);

        return textura;
    }

    public Canvas getCanvas() {
        return this.canvas;
    }

}
