package com.diamon.managers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.diamon.chip.ChipPic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vista de animación de alta fidelidad para simular la grabación de un microcontrolador PIC
 * en un zócalo ZIF de 40 pines (Verde Textool K150).
 *
 * Características principales:
 * - Paleta oficial del programador K150 (Verde Textool #0F7A4D con relieves y sombras 3D).
 * - Palanca de bloqueo cromada (#CBD5E1) con perilla esférica (#1E293B) en posición cerrada/bloqueada.
 * - Indicador dinámico de Pin 1 (flecha naranja #FF6600 y número de fila en el lateral izquierdo).
 * - Dimensiones proporcionales dinámicas del encapsulado DIP según número de pines del chip real.
 * - Grabado láser dinámico centrado con el nombre del modelo PIC.
 * - Llenado lumínico de memoria de arriba hacia abajo (gradiente #2000E676 a #5000B0FF) y haz láser de escaneo.
 * - Halo verde resplandeciente (#6600E676) al completar exitosamente la grabación.
 */
public class PicAnimationView extends View {

    // --- Paleta Oficial Zócalo K150 & Chips (Constantes ARGB directas de alto rendimiento) ---
    private static final int COLOR_SOCKET_BASE = 0xFF0F7A4D;       // Verde Textool K150
    private static final int COLOR_SOCKET_SHADOW = 0xFF084D30;     // Relieve y sombra 3D
    private static final int COLOR_SOCKET_HIGHLIGHT = 0xFF34D399;  // Brillo superior 3D (Verde menta brillante)
    private static final int COLOR_SOCKET_GROOVE = 0xFF063823;     // Canal central
    private static final int COLOR_SOCKET_SLOT = 0xFF0D2319;       // Ranuras de contactos
    private static final int COLOR_CONTACT_METAL = 0xFF94A3B8;     // Terminales metálicos niquelados

    private static final int COLOR_LEVER_ARM = 0xFFCBD5E1;         // Brazo cromado
    private static final int COLOR_LEVER_SHADOW = 0xFF94A3B8;      // Sombra palanca
    private static final int COLOR_LEVER_KNOB = 0xFF1E293B;        // Perilla plástica esférica
    private static final int COLOR_LEVER_KNOB_HL = 0xFF475569;     // Brillo perilla
    private static final int COLOR_LEVER_PIVOT = 0xFF64748B;       // Pivote mecánico

    private static final int COLOR_PIN1_INDICATOR = 0xFFFF6600;    // Flecha y número Pin 1 (Naranja)

    private static final int COLOR_CHIP_BORDER = 0xFF373737;       // Borde chip
    private static final int COLOR_NOTCH = 0xFF0F7A4D;             // Muesca chip notch
    private static final int COLOR_NOTCH_SHADOW = 0xFF084D30;      // Sombra notch
    private static final int COLOR_CHIP_TEXT = 0xFFB0BEC5;         // Grabado láser chip
    private static final int COLOR_DOT = 0xFF0F0F0F;               // Punto referencia Pin 1
    private static final int COLOR_DOT_HL = 0xFF546E7A;            // Brillo punto Pin 1
    private static final int COLOR_PIN = 0xFFECEFF1;               // Pines plateados
    private static final int COLOR_PIN_HL = 0xFFFFFFFF;            // Brillo pines

    private static final int COLOR_MEM_FILL_START = 0x2000E676;    // Gradiente llenado luminiscente
    private static final int COLOR_MEM_FILL_END = 0x5000B0FF;      // Gradiente llenado luminiscente
    private static final int COLOR_LASER_SCAN = 0xFF00E676;        // Haz láser horizontal brillante
    private static final int COLOR_LASER_GLOW = 0x6600E676;        // Brillo haz láser

    private static final int COLOR_HALO_OUTER = 0x2200E676;        // Halo resplandeciente exterior
    private static final int COLOR_HALO_INNER = 0x6600E676;        // Halo resplandeciente medio
    private static final int COLOR_HALO_CORE = 0xAA00E676;         // Halo resplandeciente centro

    private static final int COLOR_PARTICLE_GREEN = 0xFF00E676;
    private static final int COLOR_PARTICLE_CYAN = 0xFF00B0FF;
    private static final int COLOR_PARTICLE_RED = 0xFFFF1744;

    // --- Pinturas Zócalo ZIF Textool K150 ---
    private Paint socketPaint;
    private Paint socketBorderPaint;
    private Paint socketHighlightPaint;
    private Paint socketGroovePaint;
    private Paint socketSlotPaint;
    private Paint slotContactPaint;

    // --- Pinturas Palanca ZIF ---
    private Paint leverArmPaint;
    private Paint leverShadowPaint;
    private Paint leverKnobPaint;
    private Paint leverKnobHighlightPaint;
    private Paint leverPivotPaint;

    // --- Pinturas Indicador Pin 1 ---
    private Paint pinIndicatorArrowPaint;
    private Paint pinIndicatorTextPaint;

    // --- Pinturas Microcontrolador PIC ---
    private Paint chipPaint;
    private Paint chipBorderPaint;
    private Paint notchPaint;
    private Paint notchShadowPaint;
    private Paint chipTextPaint;
    private Paint dotPaint;
    private Paint dotHighlightPaint;

    // --- Pinturas Pines Metálicos ---
    private Paint pinPaint;
    private Paint pinHighlightPaint;

    // --- Pinturas Llenado de Memoria y Láser ---
    private Paint memoryFillPaint;
    private Paint laserScanPaint;
    private Paint laserGlowPaint;

    // --- Pinturas Halo de Finalización ---
    private Paint haloOuterPaint;
    private Paint haloInnerPaint;
    private Paint haloCorePaint;

    // --- Pintura Partículas ---
    private Paint particlePaint;

    // --- Geometría ---
    private float socketWidth;
    private float socketHeight;
    private float socketX;
    private float socketY;

    private float chipWidth;
    private float chipHeight;
    private float chipX;
    private float chipY;

    private float pulseScale = 1.0f;

    // --- Estado Dinámico ---
    private ChipPic chip;
    private int progress = 0;
    private boolean isProgramming = true;
    private boolean isCompleted = false;

    // --- Objetos reutilizables (Cero asignaciones en onDraw) ---
    private final Path arrowPath = new Path();
    private final Path chipClipPath = new Path();
    private final RectF socketRect = new RectF();
    private final RectF innerRect = new RectF();
    private final RectF grooveRect = new RectF();
    private final RectF chipRect = new RectF();
    private final RectF fillRect = new RectF();
    private final RectF haloRect = new RectF();
    private final RectF particleRect = new RectF();
    private final RectF leftSlotRect = new RectF();
    private final RectF rightSlotRect = new RectF();
    private final RectF leftPinRect = new RectF();
    private final RectF rightPinRect = new RectF();

    // --- Sistema de Partículas ---
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private int maxParticles = 20;

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

    public PicAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 1. Zócalo ZIF Textool Oficial K150 (Verde Textool)
        socketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketPaint.setColor(COLOR_SOCKET_BASE);
        socketPaint.setStyle(Paint.Style.FILL);

        // Relieve y sombra 3D del zócalo
        socketBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketBorderPaint.setColor(COLOR_SOCKET_SHADOW);
        socketBorderPaint.setStyle(Paint.Style.STROKE);
        socketBorderPaint.setStrokeWidth(6f);

        // Brillo superior 3D (Verde menta brillante)
        socketHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketHighlightPaint.setColor(COLOR_SOCKET_HIGHLIGHT);
        socketHighlightPaint.setStyle(Paint.Style.STROKE);
        socketHighlightPaint.setStrokeWidth(3f);

        // Canal central longitudinal del zócalo ZIF
        socketGroovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketGroovePaint.setColor(COLOR_SOCKET_GROOVE);
        socketGroovePaint.setStyle(Paint.Style.FILL);

        // Ranuras de contactos de pines
        socketSlotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketSlotPaint.setColor(COLOR_SOCKET_SLOT);
        socketSlotPaint.setStyle(Paint.Style.FILL);

        // Contactos metálicos niquelados dentro de las ranuras
        slotContactPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotContactPaint.setColor(COLOR_CONTACT_METAL);
        slotContactPaint.setStyle(Paint.Style.FILL);

        // 2. Palanca Metálica de Bloqueo (The ZIF Lever)
        leverArmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverArmPaint.setColor(COLOR_LEVER_ARM);
        leverArmPaint.setStyle(Paint.Style.STROKE);
        leverArmPaint.setStrokeWidth(7f);
        leverArmPaint.setStrokeCap(Paint.Cap.ROUND);

        leverShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverShadowPaint.setColor(COLOR_LEVER_SHADOW);
        leverShadowPaint.setStyle(Paint.Style.STROKE);
        leverShadowPaint.setStrokeWidth(7f);
        leverShadowPaint.setStrokeCap(Paint.Cap.ROUND);

        leverKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverKnobPaint.setColor(COLOR_LEVER_KNOB);
        leverKnobPaint.setStyle(Paint.Style.FILL);

        leverKnobHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverKnobHighlightPaint.setColor(COLOR_LEVER_KNOB_HL);
        leverKnobHighlightPaint.setStyle(Paint.Style.FILL);

        leverPivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverPivotPaint.setColor(COLOR_LEVER_PIVOT);
        leverPivotPaint.setStyle(Paint.Style.FILL);

        // 3. Indicador de Pin 1
        pinIndicatorArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinIndicatorArrowPaint.setColor(COLOR_PIN1_INDICATOR);
        pinIndicatorArrowPaint.setStyle(Paint.Style.FILL);

        pinIndicatorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinIndicatorTextPaint.setColor(COLOR_PIN1_INDICATOR);
        pinIndicatorTextPaint.setTextAlign(Paint.Align.RIGHT);
        pinIndicatorTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // 4. Chip PIC Encapsulado
        chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipPaint.setStyle(Paint.Style.FILL);

        chipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipBorderPaint.setColor(COLOR_CHIP_BORDER);
        chipBorderPaint.setStyle(Paint.Style.STROKE);
        chipBorderPaint.setStrokeWidth(3f);

        notchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notchPaint.setColor(COLOR_NOTCH);
        notchPaint.setStyle(Paint.Style.FILL);

        notchShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notchShadowPaint.setColor(COLOR_NOTCH_SHADOW);
        notchShadowPaint.setStyle(Paint.Style.STROKE);
        notchShadowPaint.setStrokeWidth(3f);

        chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipTextPaint.setColor(COLOR_CHIP_TEXT);
        chipTextPaint.setTextAlign(Paint.Align.CENTER);
        chipTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(COLOR_DOT);
        dotPaint.setStyle(Paint.Style.FILL);

        dotHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotHighlightPaint.setColor(COLOR_DOT_HL);
        dotHighlightPaint.setStyle(Paint.Style.STROKE);
        dotHighlightPaint.setStrokeWidth(1.5f);

        // 5. Pines Plateados
        pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(COLOR_PIN);
        pinPaint.setStyle(Paint.Style.FILL);

        pinHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinHighlightPaint.setColor(COLOR_PIN_HL);
        pinHighlightPaint.setStyle(Paint.Style.FILL);

        // 6. Llenado de Memoria y Haz Láser
        memoryFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        memoryFillPaint.setStyle(Paint.Style.FILL);

        laserScanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserScanPaint.setColor(COLOR_LASER_SCAN);
        laserScanPaint.setStyle(Paint.Style.STROKE);
        laserScanPaint.setStrokeWidth(3f);

        laserGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserGlowPaint.setColor(COLOR_LASER_GLOW);
        laserGlowPaint.setStyle(Paint.Style.STROKE);
        laserGlowPaint.setStrokeWidth(6f);

        // 7. Halo de Finalización Verde
        haloOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloOuterPaint.setColor(COLOR_HALO_OUTER);
        haloOuterPaint.setStyle(Paint.Style.STROKE);
        haloOuterPaint.setStrokeWidth(10f);

        haloInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloInnerPaint.setColor(COLOR_HALO_INNER);
        haloInnerPaint.setStyle(Paint.Style.STROKE);
        haloInnerPaint.setStrokeWidth(5f);

        haloCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloCorePaint.setColor(COLOR_HALO_CORE);
        haloCorePaint.setStyle(Paint.Style.STROKE);
        haloCorePaint.setStrokeWidth(2f);

        // 8. Partículas
        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDimensions(w, h);
    }

    /**
     * Calcula las dimensiones proporcionales del Zócalo ZIF (40 pines, 20 filas)
     * y del chip PIC dinámico según la cantidad de pines y la fila del Pin 1.
     */
    private void updateDimensions(int w, int h) {
        if (w <= 0 || h <= 0) return;

        socketHeight = h * 0.94f;
        socketWidth = Math.min(w * 0.46f, socketHeight * 0.46f);
        socketX = (w - socketWidth) / 2f;
        socketY = (h - socketHeight) / 2f;

        socketRect.set(socketX, socketY, socketX + socketWidth, socketY + socketHeight);
        innerRect.set(socketX + 3f, socketY + 3f, socketX + socketWidth - 3f, socketY + socketHeight - 3f);

        float centerX = socketX + socketWidth / 2f;
        float grooveW = socketWidth * 0.055f;
        grooveRect.set(centerX - grooveW / 2f, socketY + 14f, centerX + grooveW / 2f, socketY + socketHeight - 14f);

        // Zócalo K150 tiene 20 filas (40 pines en total)
        int totalZifRows = 20;
        float slotSpacing = socketHeight / (totalZifRows + 1);

        int numPins = (chip != null && chip.getNumeroDePines() > 0) ? chip.getNumeroDePines() : 18;
        int pinCount = Math.max(2, numPins / 2);

        int targetRow = parseTargetRow(numPins);
        if (targetRow < 1) targetRow = 1;
        if (targetRow + pinCount - 1 > totalZifRows) {
            targetRow = Math.max(1, totalZifRows - pinCount + 1);
        }
        int lastRow = targetRow + pinCount - 1;

        chipWidth = socketWidth * 0.65f;
        chipX = (w - chipWidth) / 2f;

        float chipTop = (socketY + targetRow * slotSpacing) - slotSpacing * 0.65f;
        float chipBottom = (socketY + lastRow * slotSpacing) + slotSpacing * 0.65f;
        chipHeight = chipBottom - chipTop;
        chipY = chipTop;

        chipRect.set(chipX, chipY, chipX + chipWidth, chipY + chipHeight);

        // Gradiente realista del cuerpo del chip
        LinearGradient chipGrad = new LinearGradient(
                chipX, chipY, chipX + chipWidth, chipY + chipHeight,
                new int[]{0xFF2C2C2C, 0xFF151515, 0xFF111111},
                new float[]{0.0f, 0.5f, 1.0f},
                Shader.TileMode.CLAMP
        );
        chipPaint.setShader(chipGrad);
    }

    /**
     * Determina la fila del ZIF donde encaja el Pin 1 del chip según chip.getUbicacionPin1DelPic().
     */
    private int parseTargetRow(int numPins) {
        String ubicacion = (chip != null) ? chip.getUbicacionPin1DelPic() : null;
        if (ubicacion != null && !ubicacion.isEmpty()) {
            if (ubicacion.contains("13")) {
                return 13;
            } else if (ubicacion.contains("2")) {
                return 2;
            } else if (ubicacion.contains("1")) {
                return 1;
            } else {
                String digits = ubicacion.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    try {
                        return Integer.parseInt(digits);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        // Valores por defecto según catálogo K150 oficial
        if (numPins == 8 || numPins == 14) {
            return 13;
        } else if (numPins == 18) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * Retorna el texto del Pin 1 para el indicador lateral ("1", "2" o "13").
     */
    private String getPin1Label(int targetRow) {
        String ubicacion = (chip != null) ? chip.getUbicacionPin1DelPic() : null;
        if (ubicacion != null && !ubicacion.isEmpty()) {
            if (ubicacion.contains("13")) return "13";
            if (ubicacion.contains("2")) return "2";
            if (ubicacion.contains("1")) return "1";
            String digits = ubicacion.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) return digits;
        }
        return String.valueOf(targetRow);
    }

    // --- Métodos de Control y Sincronización ---

    public void setChip(ChipPic chip) {
        this.chip = chip;
        updateDimensions(getWidth(), getHeight());
        postInvalidateOnAnimation();
    }

    public ChipPic getChip() {
        return chip;
    }

    public void setProgramming(boolean programming) {
        this.isProgramming = programming;
        if (!programming) {
            maxParticles = 0;
            particles.clear();
        }
        postInvalidateOnAnimation();
    }

    public boolean isProgramming() {
        return isProgramming;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        postInvalidateOnAnimation();
    }

    public int getProgress() {
        return progress;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        if (completed) {
            this.progress = 100;
            this.isProgramming = false;
            this.particles.clear();
        }
        postInvalidateOnAnimation();
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Actualizar geometría si las dimensiones aún no se calcularon o cambiaron
        updateDimensions(w, h);

        int numPins = (chip != null && chip.getNumeroDePines() > 0) ? chip.getNumeroDePines() : 18;
        int pinCount = Math.max(2, numPins / 2);
        int totalZifRows = 20;
        float slotSpacing = socketHeight / (totalZifRows + 1);

        int targetRow = parseTargetRow(numPins);
        if (targetRow < 1) targetRow = 1;
        if (targetRow + pinCount - 1 > totalZifRows) {
            targetRow = Math.max(1, totalZifRows - pinCount + 1);
        }

        // =========================================================================
        // 1. GENERACIÓN Y ACTUALIZACIÓN DE PARTÍCULAS DE DATOS (Zero Allocation Loop)
        // =========================================================================
        if (isProgramming && particles.size() < maxParticles && random.nextFloat() < 0.25f) {
            Particle p = new Particle();
            p.x = chipX + random.nextFloat() * chipWidth;
            p.y = 0;
            p.speedY = 8f + random.nextFloat() * 12f;
            p.size = 12f + random.nextFloat() * 16f;

            int colorSel = random.nextInt(3);
            if (colorSel == 0) {
                p.color = COLOR_PARTICLE_GREEN;
            } else if (colorSel == 1) {
                p.color = COLOR_PARTICLE_CYAN;
            } else {
                p.color = COLOR_PARTICLE_RED;
            }
            particles.add(p);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.y += p.speedY;

            // Colisión con el microcontrolador
            if (p.y >= chipY && p.x >= chipX && p.x <= (chipX + chipWidth)) {
                particles.remove(i);
                pulseScale = 1.15f;
            } else if (p.y > h) {
                particles.remove(i);
            } else {
                particlePaint.setColor(p.color);
                particleRect.set(p.x - p.size / 2f, p.y - p.size / 2f, p.x + p.size / 2f, p.y + p.size / 2f);
                canvas.drawRoundRect(particleRect, p.size / 3f, p.size / 3f, particlePaint);
            }
        }

        // Desvanecimiento suave del pulso
        if (pulseScale > 1.0f) {
            pulseScale -= 0.025f;
            if (pulseScale < 1.0f) {
                pulseScale = 1.0f;
            }
        }

        // =========================================================================
        // 2. DIBUJAR ZÓCALO ZIF TEXTOOL (Base Verde Oficial K150 #0F7A4D)
        // =========================================================================
        // Base del cuerpo
        canvas.drawRoundRect(socketRect, 18f, 18f, socketPaint);

        // Relieve y sombra 3D (#084D30)
        canvas.drawRoundRect(socketRect, 18f, 18f, socketBorderPaint);

        // Brillo superior 3D (#34D399)
        canvas.drawRoundRect(innerRect, 15f, 15f, socketHighlightPaint);

        // Canal central (#063823)
        canvas.drawRoundRect(grooveRect, 2f, 2f, socketGroovePaint);

        // 20 Ranuras de contactos ZIF (#0D2319 con terminales #94A3B8)
        float slotWidth = socketWidth * 0.085f;
        float slotHeight = slotSpacing * 0.58f;
        float leftSlotX = socketX + socketWidth * 0.16f;
        float rightSlotX = socketX + socketWidth * 0.84f - slotWidth;

        for (int r = 1; r <= totalZifRows; r++) {
            float sy = socketY + r * slotSpacing - slotHeight / 2f;

            // Ranura izquierda
            leftSlotRect.set(leftSlotX, sy, leftSlotX + slotWidth, sy + slotHeight);
            canvas.drawRoundRect(leftSlotRect, 2f, 2f, socketSlotPaint);
            canvas.drawRect(leftSlotX + slotWidth * 0.25f, sy + slotHeight * 0.20f,
                    leftSlotX + slotWidth * 0.75f, sy + slotHeight * 0.80f, slotContactPaint);

            // Ranura derecha
            rightSlotRect.set(rightSlotX, sy, rightSlotX + slotWidth, sy + slotHeight);
            canvas.drawRoundRect(rightSlotRect, 2f, 2f, socketSlotPaint);
            canvas.drawRect(rightSlotX + slotWidth * 0.25f, sy + slotHeight * 0.20f,
                    rightSlotX + slotWidth * 0.75f, sy + slotHeight * 0.80f, slotContactPaint);
        }

        // =========================================================================
        // 3. PALANCA METÁLICA DE BLOQUEO (The ZIF Lever)
        // =========================================================================
        // Brazo cromado (#CBD5E1, stroke 7f con sombra #94A3B8) en vertical trabada/abajo
        float leverHingeX = socketX + socketWidth;
        float leverHingeY = socketY + socketHeight * 0.07f;
        float leverArmX = socketX + socketWidth + 14f;
        float leverEndY = socketY + socketHeight * 0.75f;

        // Sombra de la palanca (desplazada +2.5f, +2.5f)
        canvas.drawLine(leverHingeX - 4f + 2.5f, leverHingeY + 2.5f, leverArmX + 2.5f, leverHingeY + 2.5f, leverShadowPaint);
        canvas.drawLine(leverArmX + 2.5f, leverHingeY + 2.5f, leverArmX + 2.5f, leverEndY + 2.5f, leverShadowPaint);

        // Brazo cromado
        canvas.drawLine(leverHingeX - 4f, leverHingeY, leverArmX, leverHingeY, leverArmPaint);
        canvas.drawLine(leverArmX, leverHingeY, leverArmX, leverEndY, leverArmPaint);

        // Soporte del pivote mecánico
        canvas.drawRect(leverHingeX - 6f, leverHingeY - 6f, leverHingeX + 2f, leverHingeY + 6f, leverPivotPaint);

        // Perilla plástica esférica (#1E293B) en el extremo inferior
        float knobRadius = 11f;
        canvas.drawCircle(leverArmX + 2.5f, leverEndY + 2.5f, knobRadius, leverShadowPaint);
        canvas.drawCircle(leverArmX, leverEndY, knobRadius, leverKnobPaint);
        canvas.drawCircle(leverArmX - 3f, leverEndY - 3f, 3.5f, leverKnobHighlightPaint);

        // =========================================================================
        // 4. INDICADOR DE POSICIÓN DEL PIN 1 (Flecha Naranja y Número)
        // =========================================================================
        float pin1Y = socketY + targetRow * slotSpacing;

        // Flecha indicadora naranja (#FF6600) apuntando hacia la ranura del zócalo
        arrowPath.reset();
        arrowPath.moveTo(socketX - 8f, pin1Y);
        arrowPath.lineTo(socketX - 22f, pin1Y - 6.5f);
        arrowPath.lineTo(socketX - 22f, pin1Y + 6.5f);
        arrowPath.close();
        canvas.drawPath(arrowPath, pinIndicatorArrowPaint);

        // Número de pin ("1", "2" o "13")
        String pin1Label = getPin1Label(targetRow);
        pinIndicatorTextPaint.setTextSize(Math.max(26f, slotSpacing * 1.15f));
        float textX = socketX - 26f;
        float textY = pin1Y - (pinIndicatorTextPaint.descent() + pinIndicatorTextPaint.ascent()) / 2f;
        canvas.drawText(pin1Label, textX, textY, pinIndicatorTextPaint);

        // =========================================================================
        // 5. MICROCONTROLADOR PIC DINÁMICO (Con escala interactiva)
        // =========================================================================
        canvas.save();
        float centerX = chipX + chipWidth / 2f;
        float centerY = chipY + chipHeight / 2f;
        canvas.scale(pulseScale, pulseScale, centerX, centerY);

        // Patillas metálicas individuales insertándose en las ranuras
        float pinW = (chipX - leftSlotX) + slotWidth * 0.6f;
        float pinH = Math.min(10f, slotHeight * 0.85f);

        for (int i = 0; i < pinCount; i++) {
            int row = targetRow + i;
            float py = socketY + row * slotSpacing;

            // Pin izquierdo
            leftPinRect.set(chipX - pinW, py - pinH / 2f, chipX, py + pinH / 2f);
            canvas.drawRoundRect(leftPinRect, 2f, 2f, pinPaint);
            canvas.drawRect(chipX - pinW, py - pinH / 2f, chipX, py - pinH * 0.15f, pinHighlightPaint);

            // Pin derecho
            rightPinRect.set(chipX + chipWidth, py - pinH / 2f, chipX + chipWidth + pinW, py + pinH / 2f);
            canvas.drawRoundRect(rightPinRect, 2f, 2f, pinPaint);
            canvas.drawRect(chipX + chipWidth, py - pinH / 2f, chipX + chipWidth + pinW, py - pinH * 0.15f, pinHighlightPaint);
        }

        // Halo de finalización resplandeciente (#6600E676)
        if (isCompleted) {
            haloRect.set(chipX - 8f, chipY - 8f, chipX + chipWidth + 8f, chipY + chipHeight + 8f);
            canvas.drawRoundRect(haloRect, 18f, 18f, haloOuterPaint);

            haloRect.set(chipX - 4f, chipY - 4f, chipX + chipWidth + 4f, chipY + chipHeight + 4f);
            canvas.drawRoundRect(haloRect, 14f, 14f, haloInnerPaint);

            haloRect.set(chipX - 1f, chipY - 1f, chipX + chipWidth + 1f, chipY + chipHeight + 1f);
            canvas.drawRoundRect(haloRect, 11f, 11f, haloCorePaint);
        }

        // Encapsulado principal del PIC
        canvas.drawRoundRect(chipRect, 10f, 10f, chipPaint);

        // Capa interior de llenado de memoria y haz láser móvil
        if (progress > 0) {
            canvas.save();
            chipClipPath.reset();
            chipClipPath.addRoundRect(chipRect, 10f, 10f, Path.Direction.CW);
            canvas.clipPath(chipClipPath);

            float fillFraction = Math.max(0, Math.min(100, progress)) / 100f;
            float fillHeight = chipHeight * fillFraction;
            fillRect.set(chipX, chipY, chipX + chipWidth, chipY + fillHeight);

            LinearGradient fillGrad = new LinearGradient(
                    chipX, chipY,
                    chipX, chipY + Math.max(1f, fillHeight),
                    COLOR_MEM_FILL_START,
                    COLOR_MEM_FILL_END,
                    Shader.TileMode.CLAMP
            );
            memoryFillPaint.setShader(fillGrad);
            canvas.drawRect(fillRect, memoryFillPaint);

            // Haz láser horizontal brillante (#00E676, stroke 3f)
            if (isProgramming && progress > 0 && progress < 100) {
                float laserY = chipY + fillHeight;
                canvas.drawLine(chipX, laserY, chipX + chipWidth, laserY, laserGlowPaint);
                canvas.drawLine(chipX, laserY, chipX + chipWidth, laserY, laserScanPaint);
            }

            canvas.restore();
        }

        // Borde y relieve 3D del encapsulado
        canvas.drawRoundRect(chipRect, 10f, 10f, chipBorderPaint);

        // Muesca de orientación notch (#0F7A4D con sombra #084D30)
        float notchRadius = chipWidth * 0.12f;
        canvas.drawCircle(centerX, chipY, notchRadius, notchPaint);
        canvas.drawArc(
                centerX - notchRadius,
                chipY - notchRadius,
                centerX + notchRadius,
                chipY + notchRadius,
                0, 180, false, notchShadowPaint);

        // Grabado láser dinámico: nombre real del chip centrado
        String displayName = (chip != null && chip.getNombreDelPic() != null && !chip.getNombreDelPic().isEmpty())
                ? chip.getNombreDelPic()
                : "PIC16F628A";
        if (!displayName.toUpperCase().startsWith("PIC")) {
            displayName = "PIC" + displayName;
        }

        float desiredTextSize = Math.min(chipWidth * 0.17f, chipHeight * (pinCount <= 4 ? 0.22f : 0.12f));
        chipTextPaint.setTextSize(desiredTextSize);
        float textW = chipTextPaint.measureText(displayName);
        if (textW > chipWidth * 0.82f) {
            chipTextPaint.setTextSize(desiredTextSize * (chipWidth * 0.82f / textW));
        }
        float textDrawY = centerY - (chipTextPaint.descent() + chipTextPaint.ascent()) / 2f;
        canvas.drawText(displayName, centerX, textDrawY, chipTextPaint);

        // Punto de referencia del Pin 1
        float dotX = chipX + chipWidth * 0.18f;
        float dotY = chipY + Math.min(chipHeight * 0.12f, 16f);
        canvas.drawCircle(dotX, dotY, 6f, dotPaint);
        canvas.drawCircle(dotX, dotY, 6f, dotHighlightPaint);

        canvas.restore();

        // Continuar ciclo de renderizado para animación fluida
        if (isProgramming || pulseScale > 1.0f || !particles.isEmpty() || isCompleted) {
            postInvalidateOnAnimation();
        }
    }
}
