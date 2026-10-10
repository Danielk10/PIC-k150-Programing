package com.diamon.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.widget.ImageView;

import com.diamon.chip.ChipPic;

/**
 * Gestor de dibujo visual de alta fidelidad 3D para el zócalo ZIF de 40 pines
 * y conector ICSP del programador PIC K150.
 *
 * <p>Mejoras visuales inspiradas en {@link PicAnimationView}:
 * <ul>
 *   <li>Fondo 100% transparente: se integra limpiamente con el fondo de la UI (#2A2A3E de CardView) sin recuadro negro.</li>
 *   <li>Cuerpo del zócalo azul Textool (#0F5B9E) con biseles e iluminación 3D superior e inferior.</li>
 *   <li>Palanca metálica ZIF cromada (#CBD5E1) con sombra, pivote mecánico y perilla esférica ergonómica.</li>
 *   <li>Ranuras con contactos metálicos plateados/niquelados dual-tone con brillo especular.</li>
 *   <li>Microcontrolador PIC dinámico en 3D con cuerpo de epoxi texturizado, notch de orientación, patas metálicas,
 *       punto de referencia del Pin 1 y grabado láser nítido del modelo.</li>
 *   <li>Flecha naranja y número en relieve indicando la orientación exacta del Pin 1.</li>
 * </ul>
 */
public class SocketDrawingManager {

    private final Context context;
    private final ImageView chipSocketImageView;
    private Bitmap texturaChipSocket;

    public SocketDrawingManager(Context context, ImageView chipSocketImageView) {
        this.context = context;
        this.chipSocketImageView = chipSocketImageView;
    }

    public void updateChipImage(ChipPic chip, boolean isICSPMode) {
        if (isICSPMode) {
            dibujarICSP();
        } else {
            dibujarSocketZIF(chip);
        }
    }

    private void dibujarSocketZIF(ChipPic chip) {
        int width = chipSocketImageView.getWidth();
        int height = chipSocketImageView.getHeight();

        if (width <= 0 || height <= 0 || chip == null) {
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fondo 100% transparente: elimina el fondo negro #121212 y se integra al contenedor de la UI (#2A2A3E)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float scaleX = width / 300f;
        float scaleY = height / 360f;

        // Dimensiones base del zócalo ZIF
        float socketLeft = 38 * scaleX;
        float socketRight = 258 * scaleX;
        float socketTop = 4 * scaleY;
        float socketBottom = (360 - 4) * scaleY;
        float socketCornerRadius = 12 * scaleX;
        RectF socketRect = new RectF(socketLeft, socketTop, socketRight, socketBottom);

        // =========================================================================
        // 1. SOMBRA 3D ELEVADA SOBRE EL FONDO DE LA TARJETA UI
        // =========================================================================
        Paint dropShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dropShadowPaint.setColor(0x45000000); // Sombra suave volumétrica
        RectF dropShadowRect = new RectF(
                socketLeft + 2.5f * scaleX,
                socketTop + 3.5f * scaleY,
                socketRight + 2.5f * scaleX,
                socketBottom + 3.5f * scaleY
        );
        canvas.drawRoundRect(dropShadowRect, socketCornerRadius, socketCornerRadius, dropShadowPaint);

        // =========================================================================
        // 2. CUERPO DEL ZÓCALO ZIF (Azul Textool #0F5B9E con Gradiente 3D)
        // =========================================================================
        Paint socketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        socketPaint.setStyle(Paint.Style.FILL);
        LinearGradient socketGradient = new LinearGradient(
                socketLeft, socketTop, socketRight, socketBottom,
                new int[]{
                        Color.parseColor("#1976D2"), // Brillo azul cenital (Top)
                        Color.parseColor("#0F5B9E"), // Azul Textool oficial K150 (Medio)
                        Color.parseColor("#0A3C69")  // Azul profundo / sombra (Bottom)
                },
                new float[]{0.0f, 0.45f, 1.0f},
                Shader.TileMode.CLAMP
        );
        socketPaint.setShader(socketGradient);
        canvas.drawRoundRect(socketRect, socketCornerRadius, socketCornerRadius, socketPaint);

        // Relieve y bisel 3D del zócalo (Sombra exterior profunda)
        Paint shadowBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowBorderPaint.setStyle(Paint.Style.STROKE);
        shadowBorderPaint.setStrokeWidth(3f * scaleX);
        shadowBorderPaint.setColor(Color.parseColor("#06233D")); // Bisel sombra oscuro
        canvas.drawRoundRect(socketRect, socketCornerRadius, socketCornerRadius, shadowBorderPaint);

        // Brillo superior e izquierdo para efecto 3D pulido
        Paint innerHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerHighlightPaint.setStyle(Paint.Style.STROKE);
        innerHighlightPaint.setStrokeWidth(1.6f * scaleX);
        innerHighlightPaint.setColor(Color.parseColor("#42A5F5")); // Bisel celeste reflectante
        RectF innerHighlightRect = new RectF(
                socketLeft + 2.2f * scaleX,
                socketTop + 2.2f * scaleY,
                socketRight - 2.2f * scaleX,
                socketBottom - 2.2f * scaleY
        );
        canvas.drawRoundRect(innerHighlightRect, socketCornerRadius - 2f * scaleX, socketCornerRadius - 2f * scaleX, innerHighlightPaint);

        // Canal central longitudinal del zócalo ZIF (profundidad 3D)
        Paint groovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groovePaint.setColor(Color.parseColor("#06233D")); // Azul muy oscuro de cavidad
        groovePaint.setStyle(Paint.Style.FILL);
        RectF grooveRect = new RectF(144 * scaleX, socketTop + 8 * scaleY, 156 * scaleX, socketBottom - 8 * scaleY);
        canvas.drawRoundRect(grooveRect, 2.5f * scaleX, 2.5f * scaleY, groovePaint);

        Paint grooveEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        grooveEdgePaint.setStyle(Paint.Style.STROKE);
        grooveEdgePaint.setStrokeWidth(1f * scaleX);
        grooveEdgePaint.setColor(Color.parseColor("#1E88E5")); // Brillo en el borde del canal
        canvas.drawRoundRect(grooveRect, 2.5f * scaleX, 2.5f * scaleY, grooveEdgePaint);

        // =========================================================================
        // 3. RANURAS Y CONTACTOS METÁLICOS TEXTOOL (Grid de 20x2 con Realismo 3D)
        // =========================================================================
        float slotW = 18 * scaleX;
        float slotH = 8 * scaleY;
        float totalAvailableHeight = socketBottom - socketTop;
        float startPadding = socketTop + 13 * scaleY;
        float usableHeight = totalAvailableHeight - (26 * scaleY);
        float verticalStep = usableHeight / 19f; // 20 filas

        Paint slotCavityPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotCavityPaint.setColor(Color.parseColor("#051624")); // Cavidad interna profunda
        slotCavityPaint.setStyle(Paint.Style.FILL);

        Paint slotEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotEdgePaint.setStyle(Paint.Style.STROKE);
        slotEdgePaint.setStrokeWidth(0.8f * scaleX);
        slotEdgePaint.setColor(Color.parseColor("#0A3C69"));

        Paint contactBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contactBasePaint.setColor(Color.parseColor("#94A3B8")); // Metal níquel base
        contactBasePaint.setStyle(Paint.Style.FILL);

        Paint contactHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contactHighlightPaint.setColor(Color.WHITE); // Brillo metálico superior
        contactHighlightPaint.setStyle(Paint.Style.FILL);

        Paint contactShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contactShadowPaint.setColor(Color.parseColor("#475569")); // Sombra metálica inferior
        contactShadowPaint.setStyle(Paint.Style.FILL);

        float leftSlotX = 52 * scaleX;
        float rightSlotX = 226 * scaleX;

        for (int i = 0; i < 20; i++) {
            float rowY = startPadding + (i * verticalStep) - (slotH / 2f);

            // --- Ranura Izquierda ---
            RectF leftSlot = new RectF(leftSlotX, rowY, leftSlotX + slotW, rowY + slotH);
            canvas.drawRoundRect(leftSlot, 2f * scaleX, 2f * scaleY, slotCavityPaint);
            canvas.drawRoundRect(leftSlot, 2f * scaleX, 2f * scaleY, slotEdgePaint);

            float pinLeft = leftSlotX + 3f * scaleX;
            float pinRight = leftSlotX + slotW - 3f * scaleX;
            canvas.drawRect(pinLeft, rowY + 2 * scaleY, pinRight, rowY + slotH - 2 * scaleY, contactBasePaint);
            canvas.drawRect(pinLeft, rowY + 2 * scaleY, pinRight, rowY + 3.2f * scaleY, contactHighlightPaint);
            canvas.drawRect(pinLeft, rowY + slotH - 3f * scaleY, pinRight, rowY + slotH - 2 * scaleY, contactShadowPaint);

            // --- Ranura Derecha ---
            RectF rightSlot = new RectF(rightSlotX, rowY, rightSlotX + slotW, rowY + slotH);
            canvas.drawRoundRect(rightSlot, 2f * scaleX, 2f * scaleY, slotCavityPaint);
            canvas.drawRoundRect(rightSlot, 2f * scaleX, 2f * scaleY, slotEdgePaint);

            float pinLeftR = rightSlotX + 3f * scaleX;
            float pinRightR = rightSlotX + slotW - 3f * scaleX;
            canvas.drawRect(pinLeftR, rowY + 2 * scaleY, pinRightR, rowY + slotH - 2 * scaleY, contactBasePaint);
            canvas.drawRect(pinLeftR, rowY + 2 * scaleY, pinRightR, rowY + 3.2f * scaleY, contactHighlightPaint);
            canvas.drawRect(pinLeftR, rowY + slotH - 3f * scaleY, pinRightR, rowY + slotH - 2 * scaleY, contactShadowPaint);
        }

        // =========================================================================
        // 4. PALANCA METÁLICA DE BLOQUEO ZIF (The ZIF Lever)
        // =========================================================================
        float leverHingeX = socketRight;
        float leverHingeY = socketTop + 24 * scaleY;
        float leverArmX = socketRight + 14 * scaleX;
        float leverEndY = socketBottom - 48 * scaleY;

        // Sombra de la palanca mecánica
        Paint leverShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverShadowPaint.setColor(0x40000000);
        leverShadowPaint.setStyle(Paint.Style.STROKE);
        leverShadowPaint.setStrokeWidth(6.5f * scaleX);
        leverShadowPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(leverHingeX - 2 * scaleX + 2 * scaleX, leverHingeY + 2 * scaleY, leverArmX + 2 * scaleX, leverHingeY + 2 * scaleY, leverShadowPaint);
        canvas.drawLine(leverArmX + 2 * scaleX, leverHingeY + 2 * scaleY, leverArmX + 2 * scaleX, leverEndY + 2 * scaleY, leverShadowPaint);

        // Brazo cromado
        Paint leverArmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverArmPaint.setColor(Color.parseColor("#CBD5E1")); // Cromo plateado
        leverArmPaint.setStyle(Paint.Style.STROKE);
        leverArmPaint.setStrokeWidth(6.5f * scaleX);
        leverArmPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(leverHingeX - 2 * scaleX, leverHingeY, leverArmX, leverHingeY, leverArmPaint);
        canvas.drawLine(leverArmX, leverHingeY, leverArmX, leverEndY, leverArmPaint);

        // Brillo longitudinal especular del cromo
        Paint leverGleamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leverGleamPaint.setColor(Color.WHITE);
        leverGleamPaint.setStyle(Paint.Style.STROKE);
        leverGleamPaint.setStrokeWidth(1.8f * scaleX);
        leverGleamPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(leverArmX - 0.5f * scaleX, leverHingeY + 4 * scaleY, leverArmX - 0.5f * scaleX, leverEndY - 4 * scaleY, leverGleamPaint);

        // Soporte de pivote mecánico
        Paint pivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pivotPaint.setColor(Color.parseColor("#475569"));
        pivotPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(leverHingeX - 4 * scaleX, leverHingeY - 6 * scaleY, leverHingeX + 5 * scaleX, leverHingeY + 6 * scaleY), 2 * scaleX, 2 * scaleY, pivotPaint);

        // Perilla plástica esférica (#1E293B)
        float knobRadius = 10f * scaleX;
        Paint knobShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        knobShadowPaint.setColor(0x50000000);
        canvas.drawCircle(leverArmX + 2 * scaleX, leverEndY + 2 * scaleY, knobRadius, knobShadowPaint);

        Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        knobPaint.setStyle(Paint.Style.FILL);
        RadialGradient knobGrad = new RadialGradient(
                leverArmX - 3 * scaleX, leverEndY - 3 * scaleY, knobRadius,
                new int[]{Color.parseColor("#475569"), Color.parseColor("#1E293B"), Color.parseColor("#0F172A")},
                new float[]{0.0f, 0.6f, 1.0f},
                Shader.TileMode.CLAMP
        );
        knobPaint.setShader(knobGrad);
        canvas.drawCircle(leverArmX, leverEndY, knobRadius, knobPaint);

        // Specular highlight en la perilla
        Paint knobHlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        knobHlPaint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawCircle(leverArmX - 3.2f * scaleX, leverEndY - 3.2f * scaleY, 2.5f * scaleX, knobHlPaint);

        // =========================================================================
        // 5. INDICADOR DE ORIENTACIÓN DEL PIN 1 (Flecha Naranja y Número)
        // =========================================================================
        String pinLocation = chip.getUbicacionPin1DelPic();
        int pinStartRow = 0;
        String indicatorText = "1";

        if ("socket pin 2".equalsIgnoreCase(pinLocation)) {
            pinStartRow = 1;
            indicatorText = "2";
        } else if ("socket pin 13".equalsIgnoreCase(pinLocation)) {
            pinStartRow = 12;
            indicatorText = "13";
        }

        float indicatorY = startPadding + (pinStartRow * verticalStep);

        // Sombra de la flecha
        Path arrowShadowPath = new Path();
        arrowShadowPath.moveTo(socketLeft - 2 * scaleX + 1.5f * scaleX, indicatorY + 1.5f * scaleY);
        arrowShadowPath.lineTo(socketLeft - 16 * scaleX + 1.5f * scaleX, indicatorY - 7 * scaleY + 1.5f * scaleY);
        arrowShadowPath.lineTo(socketLeft - 16 * scaleX + 1.5f * scaleX, indicatorY + 7 * scaleY + 1.5f * scaleY);
        arrowShadowPath.close();
        Paint arrowShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowShadowPaint.setColor(0x35000000);
        canvas.drawPath(arrowShadowPath, arrowShadowPaint);

        // Flecha naranja brillante
        Path arrowPath = new Path();
        arrowPath.moveTo(socketLeft - 2 * scaleX, indicatorY);
        arrowPath.lineTo(socketLeft - 16 * scaleX, indicatorY - 7 * scaleY);
        arrowPath.lineTo(socketLeft - 16 * scaleX, indicatorY + 7 * scaleY);
        arrowPath.close();

        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.parseColor("#FF6600"));
        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, arrowPaint);

        Paint arrowBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowBorderPaint.setColor(Color.parseColor("#FFA726"));
        arrowBorderPaint.setStyle(Paint.Style.STROKE);
        arrowBorderPaint.setStrokeWidth(1.2f * scaleX);
        canvas.drawPath(arrowPath, arrowBorderPaint);

        // Número indicador ("1", "2" o "13")
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17 * scaleY);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setShadowLayer(3 * scaleX, 1 * scaleX, 1 * scaleY, 0x80000000);
        canvas.drawText(indicatorText, socketLeft - 18 * scaleX, indicatorY + 5.5f * scaleY, textPaint);

        // =========================================================================
        // 6. MICROCONTROLADOR PIC DINÁMICO (Encapsulado Epoxi 3D y Grabado Láser)
        // =========================================================================
        int numPines = chip.getNumeroDePines();
        if (numPines > 0) {
            float left = 88 * scaleX;
            float right = 208 * scaleX;
            int numFilas = numPines / 2;
            float top = startPadding + (pinStartRow * verticalStep) - (slotH / 2f) - (2 * scaleY);
            float chipHeightVal = ((numFilas - 1) * verticalStep + slotH + 4 * scaleY);
            float bottom = top + chipHeightVal;

            // A. Patas metálicas plateadas (conectadas desde el cuerpo del chip a las ranuras)
            Paint legShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            legShadowPaint.setColor(0x40000000);

            Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            legPaint.setColor(Color.parseColor("#B0BEC5")); // Plata base

            Paint legHlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            legHlPaint.setColor(Color.WHITE); // Brillo superior

            for (int i = 0; i < numFilas; i++) {
                float legY = startPadding + ((pinStartRow + i) * verticalStep) - (slotH / 2f) + (2 * scaleY);

                // Sombra de patas
                canvas.drawRect(68 * scaleX, legY + 1.5f * scaleY, left, legY + 5.5f * scaleY, legShadowPaint);
                canvas.drawRect(right, legY + 1.5f * scaleY, 228 * scaleX, legY + 5.5f * scaleY, legShadowPaint);

                // Pata izquierda
                canvas.drawRect(68 * scaleX, legY, left + 2 * scaleX, legY + 4.5f * scaleY, legPaint);
                canvas.drawRect(68 * scaleX, legY, left + 2 * scaleX, legY + 2 * scaleY, legHlPaint);

                // Pata derecha
                canvas.drawRect(right - 2 * scaleX, legY, 228 * scaleX, legY + 4.5f * scaleY, legPaint);
                canvas.drawRect(right - 2 * scaleX, legY, 228 * scaleX, legY + 2 * scaleY, legHlPaint);
            }

            // B. Sombra 3D proyectada por el chip sobre la superficie del zócalo azul
            Paint chipShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipShadowPaint.setColor(0x55000000);
            RectF chipShadowRect = new RectF(left + 2f * scaleX, top + 2.5f * scaleY, right + 2f * scaleX, bottom + 2.5f * scaleY);
            canvas.drawRoundRect(chipShadowRect, 6 * scaleX, 6 * scaleY, chipShadowPaint);

            // C. Cuerpo del chip con gradiente de epoxi negro/antracita 3D
            RectF chipRect = new RectF(left, top, right, bottom);
            Paint chipBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            LinearGradient chipGrad = new LinearGradient(
                    left, top, right, bottom,
                    new int[]{
                            Color.parseColor("#32323A"), // Cara superior moldeada iluminada
                            Color.parseColor("#1E1E24"), // Epoxi mate
                            Color.parseColor("#121216")  // Cara inferior en sombra
                    },
                    new float[]{0.0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            chipBodyPaint.setShader(chipGrad);
            canvas.drawRoundRect(chipRect, 6 * scaleX, 6 * scaleY, chipBodyPaint);

            // Bisel y relieve de borde del encapsulado
            Paint chipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipBorderPaint.setColor(Color.parseColor("#444450"));
            chipBorderPaint.setStyle(Paint.Style.STROKE);
            chipBorderPaint.setStrokeWidth(1.2f * scaleX);
            canvas.drawRoundRect(chipRect, 6 * scaleX, 6 * scaleY, chipBorderPaint);

            // D. Muesca notch semicircular (revela el zócalo azul en su interior con sombra)
            float notchWidth = 32 * scaleX;
            float notchHeight = 16 * scaleY;
            float notchLeft = (left + right) / 2f - notchWidth / 2f;
            float notchRight = (left + right) / 2f + notchWidth / 2f;

            Paint notchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            notchPaint.setColor(Color.parseColor("#0F5B9E")); // Mismo azul del zócalo
            notchPaint.setStyle(Paint.Style.FILL);
            canvas.drawArc(notchLeft, top - notchHeight / 2f, notchRight, top + notchHeight / 2f, 0, 180, true, notchPaint);

            Paint notchShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            notchShadowPaint.setColor(Color.parseColor("#06233D"));
            notchShadowPaint.setStyle(Paint.Style.STROKE);
            notchShadowPaint.setStrokeWidth(1.5f * scaleX);
            canvas.drawArc(notchLeft, top - notchHeight / 2f, notchRight, top + notchHeight / 2f, 0, 180, false, notchShadowPaint);

            // E. Indentación (punto) de referencia del Pin 1
            float dotX = left + 14 * scaleX;
            float dotY = top + 14 * scaleY;
            Paint dotShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotShadowPaint.setColor(Color.parseColor("#0A0A0E"));
            canvas.drawCircle(dotX, dotY, 4.2f * scaleX, dotShadowPaint);

            Paint dotHlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotHlPaint.setColor(Color.parseColor("#546E7A"));
            dotHlPaint.setStyle(Paint.Style.STROKE);
            dotHlPaint.setStrokeWidth(0.9f * scaleX);
            canvas.drawCircle(dotX, dotY, 4.2f * scaleX, dotHlPaint);

            // F. Grabado láser del microcontrolador
            String chipName = chip.getNombreDelPic();
            if (chipName != null && !chipName.isEmpty()) {
                Paint modelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                modelTextPaint.setColor(Color.parseColor("#E2E8F0")); // Blanco láser nítido
                modelTextPaint.setTextSize(Math.min(18 * scaleY, (bottom - top) * 0.28f));
                modelTextPaint.setTextAlign(Paint.Align.CENTER);
                modelTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                modelTextPaint.setShadowLayer(2f * scaleX, 0.8f * scaleX, 0.8f * scaleY, 0x90000000);

                float chipCenterX = (left + right) / 2f;
                float chipCenterY = (top + bottom) / 2f;
                canvas.drawText(chipName, chipCenterX, chipCenterY + (5 * scaleY), modelTextPaint);
            }
        }

        recycleTextura();

        texturaChipSocket = bitmap;
        chipSocketImageView.setImageBitmap(texturaChipSocket);
    }

    private void dibujarICSP() {
        int width = chipSocketImageView.getWidth();
        int height = chipSocketImageView.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fondo transparente para integrarse con la UI (#2A2A3E)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        float scaleX = width / 200f;
        float scaleY = height / 240f;

        // Conector Gris con gradiente y relieve 3D
        float vPadding = height * 0.05f;
        float effectiveHeight = height - (2 * vPadding);

        float rectWidth = 35 * scaleX;
        float rectX = 10 * scaleX;
        float rectY = vPadding;
        float rectHeight = effectiveHeight;

        // Sombra suave del conector
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x40000000);
        canvas.drawRoundRect(new RectF(rectX + 2 * scaleX, rectY + 2 * scaleY, rectX + rectWidth + 2 * scaleX, rectY + rectHeight + 2 * scaleY), 4 * scaleX, 4 * scaleY, shadowPaint);

        // Cuerpo conector
        Paint connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient connGrad = new LinearGradient(
                rectX, rectY, rectX + rectWidth, rectY + rectHeight,
                new int[]{Color.parseColor("#9E9E9E"), Color.parseColor("#616161")},
                null, Shader.TileMode.CLAMP
        );
        connectorPaint.setShader(connGrad);
        canvas.drawRoundRect(new RectF(rectX, rectY, rectX + rectWidth, rectY + rectHeight), 4 * scaleX, 4 * scaleY, connectorPaint);

        // Borde biselado
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.8f * scaleX);
        borderPaint.setColor(Color.parseColor("#37474F"));
        canvas.drawRoundRect(new RectF(rectX, rectY, rectX + rectWidth, rectY + rectHeight), 4 * scaleX, 4 * scaleY, borderPaint);

        // Cables y Etiquetas
        String[] labels = {"VPP1", "LOW", "DAT", "CLK", "VCC", "GND"};
        int[] colors = {
                Color.WHITE,
                Color.parseColor("#2196F3"), // Azul vivo
                Color.parseColor("#4CAF50"), // Verde
                Color.parseColor("#F44336"), // Rojo
                Color.parseColor("#212121"), // Negro
                Color.parseColor("#FFEB3B")  // Amarillo
        };

        float lineStartX = rectX + rectWidth;
        float lineEndX = width - (2 * scaleX);
        float lineSpacing = effectiveHeight / labels.length;

        Paint wirePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        labelPaint.setShadowLayer(2 * scaleX, 1 * scaleX, 1 * scaleY, 0xCC000000);

        for (int i = 0; i < labels.length; i++) {
            float currentY = rectY + (i * lineSpacing) + (lineSpacing / 2f);

            // Cable con sombra inferior
            float strokeWidth = effectiveHeight / (labels.length * 4.2f);
            wirePaint.setColor(0x40000000);
            wirePaint.setStrokeWidth(strokeWidth);
            canvas.drawLine(lineStartX, currentY + 1.5f * scaleY, lineEndX, currentY + 1.5f * scaleY, wirePaint);

            wirePaint.setColor(colors[i]);
            canvas.drawLine(lineStartX, currentY, lineEndX, currentY, wirePaint);

            // Etiqueta
            labelPaint.setTextSize(strokeWidth * 1.8f);
            float textX = lineStartX + (6 * scaleX);
            float textY = currentY - (strokeWidth / 1.2f);
            canvas.drawText(labels[i], textX, textY, labelPaint);
        }

        recycleTextura();

        texturaChipSocket = bitmap;
        chipSocketImageView.setImageBitmap(texturaChipSocket);
    }

    public void recycleTextura() {
        if (texturaChipSocket != null) {
            texturaChipSocket.recycle();
            texturaChipSocket = null;
        }
    }
}
