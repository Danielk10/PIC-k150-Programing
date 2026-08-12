package com.diamon.managers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vista de animación realista para simular la grabación de un PIC en un zócalo ZIF.
 * Muestra el zócalo ZIF de color verde con sus ranuras para los pines, la palanca metálica de cierre
 * y el chip PIC16F628A con sus pines plateados encajados. Los paquetes de datos
 * caen desde arriba y hacen parpadear/pulsar el chip al grabarse.
 */
public class PicAnimationView extends View {

    private Paint socketPaint;
    private Paint socketSlotPaint;
    private Paint leverPaint;
    private Paint chipPaint;
    private Paint pinPaint;
    private Paint particlePaint;
    private Paint textPaint;
    private Paint chipTextPaint;

    private float socketWidth;
    private float socketHeight;
    private float socketX;
    private float socketY;

    private float chipWidth;
    private float chipHeight;
    private float chipX;
    private float chipY;

    private float pulseScale = 1.0f;

    private List<Particle> particles = new ArrayList<>();
    private Random random = new Random();
    private int maxParticles = 20;
    private boolean isProgramming = true;

    private static class Particle {
        float x;
        float y;
        float speedY;
        float size;
        int color;
    }

    public PicAnimationView(Context context) {
        super(context);
        init();
    }

    public PicAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Pintura para el cuerpo del zócalo ZIF (Azul Textool profesional)
        socketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketPaint.setColor(Color.parseColor("#0F5B9E"));
        socketPaint.setStyle(Paint.Style.FILL);

        // Pintura para las ranuras del zócalo (Negro/Gris muy oscuro)
        socketSlotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketSlotPaint.setColor(Color.parseColor("#1A1A1A"));
        socketSlotPaint.setStyle(Paint.Style.FILL);

        // Pintura para la palanca metálica del zócalo (Cromado/Plateado)
        leverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverPaint.setColor(Color.parseColor("#90A4AE"));
        leverPaint.setStrokeWidth(6f);
        leverPaint.setStyle(Paint.Style.STROKE);
        leverPaint.setStrokeCap(Paint.Cap.ROUND);

        // Pintura para el cuerpo del PIC (Negro mate integrado)
        chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipPaint.setColor(Color.parseColor("#1F1F1F"));
        chipPaint.setStyle(Paint.Style.FILL);

        // Pintura para los pines del PIC (Plata metálico brillante)
        pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(Color.parseColor("#ECEFF1"));
        pinPaint.setStyle(Paint.Style.FILL);

        // Pintura para las partículas de datos
        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Pintura para texto genérico
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        
        // Pintura para el grabado del chip
        chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipTextPaint.setColor(Color.parseColor("#B0BEC5")); // Color laser etching (gris claro)
        chipTextPaint.setTextAlign(Paint.Align.CENTER);
        chipTextPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Configuración de dimensiones del Zócalo ZIF (ocupa gran parte del área vertical)
        socketWidth = w * 0.46f;
        socketHeight = h * 0.96f;
        socketX = (w - socketWidth) / 2f;
        socketY = h * 0.02f;

        // Configuración de dimensiones del Chip PIC (dentro del zócalo)
        chipWidth = socketWidth * 0.65f;
        chipHeight = socketHeight * 0.70f;
        chipX = (w - chipWidth) / 2f;
        chipY = socketY + (socketHeight - chipHeight) / 2f;
    }

    public void setProgramming(boolean programming) {
        this.isProgramming = programming;
        if (!programming) {
            maxParticles = 0;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // 1. Spawneo y actualización de partículas
        if (isProgramming && particles.size() < maxParticles && random.nextFloat() < 0.25f) {
            Particle p = new Particle();
            // Spawnea partículas en el área sobre el chip
            p.x = chipX + random.nextFloat() * chipWidth;
            p.y = 0;
            p.speedY = 8f + random.nextFloat() * 12f;
            p.size = 12f + random.nextFloat() * 16f;

            // Alternar colores de transmisión (Verde neón, cian, magenta brillante)
            int colorSel = random.nextInt(3);
            if (colorSel == 0) {
                p.color = Color.parseColor("#00E676"); // Verde neón
            } else if (colorSel == 1) {
                p.color = Color.parseColor("#00B0FF"); // Cian
            } else {
                p.color = Color.parseColor("#FF1744"); // Rojo neón/Magenta
            }
            particles.add(p);
        }

        List<Particle> toRemove = new ArrayList<>();
        for (Particle p : particles) {
            p.y += p.speedY;

            // Detección de colisión con el chip
            if (p.y >= chipY && p.x >= chipX && p.x <= (chipX + chipWidth)) {
                toRemove.add(p);
                // Provocar pulso en la escala del chip
                pulseScale = 1.15f;
            } else if (p.y > h) {
                toRemove.add(p);
            } else {
                particlePaint.setColor(p.color);
                RectF pRect = new RectF(p.x - p.size/2, p.y - p.size/2, p.x + p.size/2, p.y + p.size/2);
                canvas.drawRoundRect(pRect, p.size/3, p.size/3, particlePaint);
            }
        }
        particles.removeAll(toRemove);

        // Desvanecimiento suave del pulso
        if (pulseScale > 1.0f) {
            pulseScale -= 0.025f;
            if (pulseScale < 1.0f) {
                pulseScale = 1.0f;
            }
        }

        // 2. DIBUJAR ZÓCALO ZIF (Base estática)
        // Cuerpo principal del ZIF
        RectF socketRect = new RectF(socketX, socketY, socketX + socketWidth, socketY + socketHeight);
        canvas.drawRoundRect(socketRect, 18f, 18f, socketPaint);

        // Borde relieve 3D del zócalo (Sombra)
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#0A3C69"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        canvas.drawRoundRect(socketRect, 18f, 18f, borderPaint);

        // Brillo superior e izquierdo para efecto 3D
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.parseColor("#42A5F5")); // Celeste claro
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(3f);
        RectF innerRect = new RectF(socketX + 3f, socketY + 3f, socketX + socketWidth - 3f, socketY + socketHeight - 3f);
        canvas.drawRoundRect(innerRect, 15f, 15f, highlightPaint);

        // Canal central longitudinal del zócalo ZIF (realismo)
        Paint groovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groovePaint.setColor(Color.parseColor("#083054")); // Azul muy oscuro
        groovePaint.setStyle(Paint.Style.FILL);
        float grooveW = socketWidth * 0.05f;
        RectF grooveRect = new RectF(
                (socketX + socketWidth/2f - grooveW/2f),
                socketY + 14f,
                (socketX + socketWidth/2f + grooveW/2f),
                socketY + socketHeight - 14f
        );
        canvas.drawRoundRect(grooveRect, 2f, 2f, groovePaint);

        // Dibujar las ranuras de conexión (Slots de pines)
        int slotsCount = 18; // Simula un zócalo de 36 pines
        float slotWidth = socketWidth * 0.08f;
        float slotHeight = socketHeight * 0.028f;
        float slotSpacing = socketHeight / (slotsCount + 1);

        float leftSlotX = socketX + socketWidth * 0.18f;
        float rightSlotX = socketX + socketWidth * 0.82f - slotWidth;

        Paint slotContactPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotContactPaint.setColor(Color.parseColor("#78909C")); // Metal
        slotContactPaint.setStyle(Paint.Style.FILL);

        for (int i = 1; i <= slotsCount; i++) {
            float sy = socketY + i * slotSpacing - slotHeight / 2f;
            // Ranuras lado izquierdo
            RectF leftSlot = new RectF(leftSlotX, sy, leftSlotX + slotWidth, sy + slotHeight);
            canvas.drawRoundRect(leftSlot, 2f, 2f, socketSlotPaint);
            // Contacto metálico izquierdo
            canvas.drawRect(leftSlotX + slotWidth * 0.33f, sy + slotHeight * 0.25f, leftSlotX + slotWidth * 0.67f, sy + slotHeight * 0.75f, slotContactPaint);

            // Ranuras lado derecho
            RectF rightSlot = new RectF(rightSlotX, sy, rightSlotX + slotWidth, sy + slotHeight);
            canvas.drawRoundRect(rightSlot, 2f, 2f, socketSlotPaint);
            // Contacto metálico derecho
            canvas.drawRect(rightSlotX + slotWidth * 0.33f, sy + slotHeight * 0.25f, rightSlotX + slotWidth * 0.67f, sy + slotHeight * 0.75f, slotContactPaint);
        }

        // 3. DIBUJAR CHIP PIC (Con escala interactiva)
        canvas.save();
        float centerX = chipX + chipWidth / 2f;
        float centerY = chipY + chipHeight / 2f;
        canvas.scale(pulseScale, pulseScale, centerX, centerY);

        // Dibujar patillas del integrado (Pines plateados insertándose en las ranuras)
        int pinCount = 14; // Un chip de 28 pines (14 a cada lado)
        float pinW = socketWidth * 0.12f; // Sobresale y entra en las ranuras del ZIF
        float pinH = chipHeight * 0.025f;
        float pinSpacing = chipHeight / (pinCount + 1);

        Paint legHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        legHighlight.setColor(Color.WHITE); // Brillo
        legHighlight.setStyle(Paint.Style.FILL);

        for (int i = 1; i <= pinCount; i++) {
            float py = chipY + i * pinSpacing - pinH / 2f;
            // Pines izquierdos (salen del cuerpo del chip e ingresan al zócalo)
            canvas.drawRoundRect(new RectF(chipX - pinW, py, chipX, py + pinH), 3f, 3f, pinPaint);
            canvas.drawRect(chipX - pinW, py, chipX, py + pinH * 0.35f, legHighlight);
            
            // Pines derechos
            canvas.drawRoundRect(new RectF(chipX + chipWidth, py, chipX + chipWidth + pinW, py + pinH), 3f, 3f, pinPaint);
            canvas.drawRect(chipX + chipWidth, py, chipX + chipWidth + pinW, py + pinH * 0.35f, legHighlight);
        }

        // Cuerpo del chip PIC (Encima de los pines - con gradiente)
        RectF chipRect = new RectF(chipX, chipY, chipX + chipWidth, chipY + chipHeight);
        Paint chipBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipBodyPaint.setStyle(Paint.Style.FILL);
        android.graphics.LinearGradient chipGrad = new android.graphics.LinearGradient(
                chipX, chipY, chipX + chipWidth, chipY + chipHeight,
                new int[]{Color.parseColor("#2C2C2C"), Color.parseColor("#151515"), Color.parseColor("#111111")},
                new float[]{0.0f, 0.5f, 1.0f},
                android.graphics.Shader.TileMode.CLAMP
        );
        chipBodyPaint.setShader(chipGrad);
        canvas.drawRoundRect(chipRect, 10f, 10f, chipBodyPaint);

        // Sombra / Relieve 3D en los bordes del chip
        Paint chipBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipBorder.setColor(Color.parseColor("#373737"));
        chipBorder.setStyle(Paint.Style.STROKE);
        chipBorder.setStrokeWidth(3f);
        canvas.drawRoundRect(chipRect, 10f, 10f, chipBorder);

        // Muesca de orientación (Notch semicircular en la parte superior)
        Paint notchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notchPaint.setColor(Color.parseColor("#0F5B9E")); // Mismo azul del zócalo
        notchPaint.setStyle(Paint.Style.FILL);
        float notchRadius = chipWidth * 0.12f;
        canvas.drawCircle(centerX, chipY, notchRadius, notchPaint);

        // Sombra en el notch
        Paint notchShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        notchShadow.setColor(Color.parseColor("#083054"));
        notchShadow.setStyle(Paint.Style.STROKE);
        notchShadow.setStrokeWidth(3f);
        canvas.drawArc(
                centerX - notchRadius,
                chipY - notchRadius,
                centerX + notchRadius,
                chipY + notchRadius,
                0, 180, false, notchShadow);

        // Grabado láser realista (Texto del integrado)
        float textYOffset = chipHeight * 0.18f;
        
        // Modelo del PIC (Ajustado para estar centrado)
        chipTextPaint.setTextSize(chipHeight * 0.10f);
        canvas.drawText("PIC16F628A", centerX, chipY + chipHeight / 2f, chipTextPaint);
        
        // Punto de referencia del Pin 1 (Círculo pequeño grabado abajo a la izquierda de la muesca)
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#0F0F0F"));
        dotPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(chipX + chipWidth * 0.18f, chipY + chipHeight * 0.12f, 7f, dotPaint);

        Paint dotHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotHighlight.setColor(Color.parseColor("#546E7A"));
        dotHighlight.setStyle(Paint.Style.STROKE);
        dotHighlight.setStrokeWidth(1.5f);
        canvas.drawCircle(chipX + chipWidth * 0.18f, chipY + chipHeight * 0.12f, 7f, dotHighlight);

        canvas.restore();

        // Continuar bucle de animación
        postInvalidateOnAnimation();
    }
}
