package com.diamon.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.ImageView;

import com.diamon.chip.ChipPic;

/**
 * Gestor de dibujo visual del zócalo ZIF y conector ICSP.
 * Encapsula la lógica de renderizado gráfico de la interfaz de usuario.
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

        if (width <= 0 || height <= 0 || chip == null)
            return;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fondo oscuro / Slate para coincidir con la UI oscura
        canvas.drawColor(Color.parseColor("#121212"));

        float scaleX = width / 300f;
        float scaleY = height / 360f;

        // 1. Base del Zócalo ZIF (Azul)
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#0F5B9E")); // Azul ZIF Textool profesional
        paint.setStyle(Paint.Style.FILL);
        
        // Ajuste: Cubrir todo el borde vertical (0 a 360)
        float socketTop = 0;
        float socketBottom = 360 * scaleY;
        RectF socketRect = new RectF(40 * scaleX, socketTop, 260 * scaleX, socketBottom);
        canvas.drawRoundRect(socketRect, 10 * scaleX, 10 * scaleY, paint);

        // Borde relieve 3D del zócalo (Sombra)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4 * scaleX);
        paint.setColor(Color.parseColor("#0A3C69"));
        canvas.drawRoundRect(socketRect, 10 * scaleX, 10 * scaleY, paint);

        // Brillo superior e izquierdo para efecto 3D (Corrección de escalado)
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.parseColor("#42A5F5")); // Celeste claro
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(2f * scaleX);
        RectF innerRect = new RectF(42 * scaleX, 2 * scaleY, 258 * scaleX, (360 - 2) * scaleY);
        canvas.drawRoundRect(innerRect, 8 * scaleX, 8 * scaleY, highlightPaint);

        // Canal central longitudinal del zócalo ZIF (realismo y extensión hasta abajo)
        Paint groovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groovePaint.setColor(Color.parseColor("#083054")); // Azul muy oscuro
        groovePaint.setStyle(Paint.Style.FILL);
        RectF grooveRect = new RectF(144 * scaleX, 8 * scaleY, 156 * scaleX, (360 - 8) * scaleY);
        canvas.drawRoundRect(grooveRect, 2 * scaleX, 2 * scaleY, groovePaint);

        // 3. Ranuras del zócalo ZIF (Grid de 20x2)
        Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotPaint.setColor(Color.parseColor("#121212")); // Ranura negra
        slotPaint.setStyle(Paint.Style.FILL);

        Paint slotContactPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotContactPaint.setColor(Color.parseColor("#B0BEC5")); // Plata metálico base
        slotContactPaint.setStyle(Paint.Style.FILL);

        Paint slotHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotHighlightPaint.setColor(Color.WHITE); // Brillo metálico superior
        slotHighlightPaint.setStyle(Paint.Style.FILL);
        
        Paint slotShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slotShadowPaint.setColor(Color.parseColor("#546E7A")); // Sombra metálica inferior
        slotShadowPaint.setStyle(Paint.Style.FILL);

        float slotW = 18 * scaleX;
        float slotH = 8 * scaleY;
        float totalAvailableHeight = 360 * scaleY;
        float startPadding = 12 * scaleY;
        float usableHeight = totalAvailableHeight - (2 * startPadding);
        float verticalStep = usableHeight / 19f; // Para 20 filas

        for (int i = 0; i < 20; i++) {
            float rowY = startPadding + (i * verticalStep) - (slotH / 2f);

            // Ranura Izquierda
            RectF leftSlot = new RectF(54 * scaleX, rowY, (54 + 18) * scaleX, rowY + slotH);
            canvas.drawRoundRect(leftSlot, 2f * scaleX, 2f * scaleY, slotPaint);
            
            // PIN PLATEADO IZQUIERDO (Efecto 3D mejorado)
            float pinLeft = 57 * scaleX;
            float pinRight = 69 * scaleX;
            // Capa base
            canvas.drawRect(pinLeft, rowY + 2 * scaleY, pinRight, rowY + 6 * scaleY, slotContactPaint);
            // Sombra inferior
            canvas.drawRect(pinLeft, rowY + 5 * scaleY, pinRight, rowY + 6 * scaleY, slotShadowPaint);
            // Brillo superior
            canvas.drawRect(pinLeft, rowY + 2 * scaleY, pinRight, rowY + 3.5f * scaleY, slotHighlightPaint);

            // Ranura Derecha
            RectF rightSlot = new RectF(228 * scaleX, rowY, (228 + 18) * scaleX, rowY + slotH);
            canvas.drawRoundRect(rightSlot, 2f * scaleX, 2f * scaleY, slotPaint);
            
            // PIN PLATEADO DERECHO
            float pinLeftR = 231 * scaleX;
            float pinRightR = 243 * scaleX;
            // Capa base
            canvas.drawRect(pinLeftR, rowY + 2 * scaleY, pinRightR, rowY + 6 * scaleY, slotContactPaint);
            // Sombra inferior
            canvas.drawRect(pinLeftR, rowY + 5 * scaleY, pinRightR, rowY + 6 * scaleY, slotShadowPaint);
            // Brillo superior
            canvas.drawRect(pinLeftR, rowY + 2 * scaleY, pinRightR, rowY + 3.5f * scaleY, slotHighlightPaint);
        }

        // 4. Indicadores (Número y Flecha para el Pin 1 del chip)
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

        float indicatorY = startPadding + (pinStartRow * verticalStep) - (slotH / 2f);
        float rowCenterY = indicatorY + (slotH / 2f);

        // Flecha indicadora naranja llamativa
        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.parseColor("#FF6600")); // Naranja
        arrowPaint.setStyle(Paint.Style.FILL);

        Path path = new Path();
        float arrowWidth = 10 * scaleX;
        float arrowHeight = 8 * scaleY;
        path.moveTo(22 * scaleX, rowCenterY - arrowHeight);
        path.lineTo(38 * scaleX, rowCenterY);
        path.lineTo(22 * scaleX, rowCenterY + arrowHeight);
        path.close();
        canvas.drawPath(path, arrowPaint);

        // Texto indicador "1", "2" o "13"
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18 * scaleY);
        textPaint.setFakeBoldText(true);
        canvas.drawText(indicatorText, 4 * scaleX, rowCenterY + 6 * scaleY, textPaint);

        // 5. Cuerpo del Chip PIC (Negro)
        int numPines = chip.getNumeroDePines();
        if (numPines > 0) {
            float left = 90 * scaleX;
            float right = 210 * scaleX;

            float top = startPadding + (pinStartRow * verticalStep) - (slotH / 2f) - (2 * scaleY);
            int numFilas = numPines / 2;
            float chipHeightVal = ((numFilas - 1) * verticalStep + slotH + 4 * scaleY);
            float bottom = top + chipHeightVal;

            // A. Patas plateadas del chip (salen del chip y se meten en las ranuras)
            Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            legPaint.setColor(Color.parseColor("#B0BEC5")); // Plateado base
            legPaint.setStyle(Paint.Style.FILL);

            Paint legHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
            legHighlight.setColor(Color.parseColor("#FFFFFF")); // Brillo
            legHighlight.setStyle(Paint.Style.FILL);

            for (int i = 0; i < numFilas; i++) {
                float legY = startPadding + ((pinStartRow + i) * verticalStep) - (slotH / 2f) + (2 * scaleY);
                // Pata Izquierda
                canvas.drawRect(72 * scaleX, legY, 92 * scaleX, legY + 4 * scaleY, legPaint);
                canvas.drawRect(72 * scaleX, legY, 92 * scaleX, legY + 1.5f * scaleY, legHighlight);
                // Pata Derecha
                canvas.drawRect(208 * scaleX, legY, 228 * scaleX, legY + 4 * scaleY, legPaint);
                canvas.drawRect(208 * scaleX, legY, 228 * scaleX, legY + 1.5f * scaleY, legHighlight);
            }

            // B. Cuerpo del chip (Con gradiente para realismo 3D)
            Paint chipBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipBodyPaint.setStyle(Paint.Style.FILL);
            android.graphics.LinearGradient chipGrad = new android.graphics.LinearGradient(
                left, top, right, bottom,
                new int[]{Color.parseColor("#2C2C2C"), Color.parseColor("#151515"), Color.parseColor("#111111")},
                new float[]{0.0f, 0.5f, 1.0f},
                android.graphics.Shader.TileMode.CLAMP
            );
            chipBodyPaint.setShader(chipGrad);
            RectF chipRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(chipRect, 8f * scaleX, 8f * scaleY, chipBodyPaint);

            // Borde relieve del chip
            Paint chipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            chipBorderPaint.setColor(Color.parseColor("#3A3A3A"));
            chipBorderPaint.setStyle(Paint.Style.STROKE);
            chipBorderPaint.setStrokeWidth(1.2f * scaleX);
            canvas.drawRoundRect(chipRect, 8f * scaleX, 8f * scaleY, chipBorderPaint);

            // C. Muesca semicircular superior (Notch)
            Paint notchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            notchPaint.setColor(Color.parseColor("#0F5B9E")); // Mismo azul del fondo del zócalo
            notchPaint.setStyle(Paint.Style.FILL);
            float notchWidth = 36 * scaleX;
            float notchHeight = 16 * scaleY;
            canvas.drawArc(
                    (300 / 2f - notchWidth / 2f) * scaleX,
                    top - notchHeight / 2f,
                    (300 / 2f + notchWidth / 2f) * scaleX,
                    top + notchHeight / 2f,
                    0, 180, true, notchPaint);

            // Sombra en el arco del notch
            Paint notchShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            notchShadow.setColor(Color.parseColor("#083054"));
            notchShadow.setStyle(Paint.Style.STROKE);
            notchShadow.setStrokeWidth(1.5f * scaleX);
            canvas.drawArc(
                    (300 / 2f - notchWidth / 2f) * scaleX,
                    top - notchHeight / 2f,
                    (300 / 2f + notchWidth / 2f) * scaleX,
                    top + notchHeight / 2f,
                    0, 180, false, notchShadow);

            // D. Grabado modelo del chip
            String chipName = chip.getNombreDelPic();
            Paint modelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            modelTextPaint.setColor(Color.parseColor("#B0BEC5")); // Gris claro láser
            modelTextPaint.setTextSize(18 * scaleY);
            modelTextPaint.setTextAlign(Paint.Align.CENTER);
            modelTextPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD));

            float chipCenterX = (left + right) / 2f;
            float chipCenterY = (top + bottom) / 2f;

            // Texto del modelo
            canvas.drawText(chipName, chipCenterX, chipCenterY + (6 * scaleY), modelTextPaint);

            // Indentación del Pin 1
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(Color.parseColor("#0F0F0F"));
            dotPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(left + 15 * scaleX, top + 15 * scaleY, 4 * scaleX, dotPaint);

            Paint dotHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotHighlight.setColor(Color.parseColor("#546E7A"));
            dotHighlight.setStyle(Paint.Style.STROKE);
            dotHighlight.setStrokeWidth(0.8f * scaleX);
            canvas.drawCircle(left + 15 * scaleX, top + 15 * scaleY, 4 * scaleX, dotHighlight);
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

        // 1. Fondo PURPURA
        canvas.drawColor(Color.parseColor("#800080"));

        float scaleX = width / 200f;
        float scaleY = height / 240f;

        // 2. Conector Gris
        // Añadimos un margen vertical interno (padding) para evitar que el texto
        // superior (VPP1) se corte
        float vPadding = height * 0.05f; // 5% de margen
        float effectiveHeight = height - (2 * vPadding);

        float rectWidth = 35 * scaleX;
        float rectX = 10 * scaleX;
        float rectY = vPadding;
        float rectHeight = effectiveHeight;

        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#808080"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rectX, rectY, rectX + rectWidth, rectY + rectHeight, paint);

        // Borde del conector
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * scaleX);
        paint.setColor(Color.BLACK); // O el color original del borde
        canvas.drawRect(rectX, rectY, rectX + rectWidth, rectY + rectHeight, paint);

        // 3. Cables y Etiquetas
        String[] labels = { "VPP1", "LOW", "DAT", "CLK", "VCC", "GND" };
        int[] colors = {
                Color.WHITE,
                Color.BLUE,
                Color.parseColor("#008000"), // Verde oscuro
                Color.RED,
                Color.BLACK,
                Color.YELLOW
        };

        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);

        float lineStartX = rectX + rectWidth;
        float lineEndX = width - (2 * scaleX);

        // Distribucion sobre el alto EFECTIVO (con padding)
        float lineSpacing = effectiveHeight / labels.length;

        for (int i = 0; i < labels.length; i++) {
            float currentY = rectY + (i * lineSpacing) + (lineSpacing / 2f);

            // Dibujar Cable
            paint.setColor(colors[i]);
            float strokeWidth = effectiveHeight / (labels.length * 4f); // Un poco mas fino para dar aire
            paint.setStrokeWidth(strokeWidth);
            canvas.drawLine(lineStartX, currentY, lineEndX, currentY, paint);

            // Dibujar Etiqueta CLARAMENTE arriba del cable (evita solapamiento y clipping)
            paint.setColor(Color.WHITE);
            paint.setTextSize(strokeWidth * 1.8f);
            float textX = lineStartX + (6 * scaleX);
            float textY = currentY - (strokeWidth / 1.2f); // Mas separacion
            canvas.drawText(labels[i], textX, textY, paint);
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
