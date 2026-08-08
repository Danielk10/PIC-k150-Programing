package com.diamon.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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

        // Colores originales del vector
        int colorTeal = Color.parseColor("#005F5F");
        int colorBlueFrame = Color.parseColor("#1565C0");
        int colorInnerRecess = Color.parseColor("#0D47A1");
        int colorPinGreen = Color.parseColor("#4CAF50");
        int colorPinGold = Color.parseColor("#FFD700");

        // Fondo Teal
        canvas.drawColor(colorTeal);

        float scaleX = width / 300f;
        float scaleY = height / 360f;

        // 1. Marco del Socket (BLUE)
        Paint paint = new Paint();
        paint.setColor(colorBlueFrame);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(40 * scaleX, 10 * scaleY, 260 * scaleX, 350 * scaleY, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * scaleX);
        paint.setColor(Color.WHITE);
        canvas.drawRect(40 * scaleX, 10 * scaleY, 260 * scaleX, 350 * scaleY, paint);

        // 2. Hueco Central (Inner Recess)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colorInnerRecess);
        canvas.drawRect(90 * scaleX, 20 * scaleY, 210 * scaleX, 340 * scaleY, paint);

        // 3. Pines (Grid de 20x2)
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 20; i++) {
            float rowY = (30 + i * 16) * scaleY;

            // Columna Izquierda
            paint.setColor(colorPinGreen);
            canvas.drawRect(50 * scaleX, rowY, 80 * scaleX, rowY + 10 * scaleY, paint);
            paint.setColor(colorPinGold);
            canvas.drawRect(70 * scaleX, rowY + 2 * scaleY, 76 * scaleX, rowY + 8 * scaleY, paint);

            // Columna Derecha
            paint.setColor(colorPinGreen);
            canvas.drawRect(220 * scaleX, rowY, 250 * scaleX, rowY + 10 * scaleY, paint);
            paint.setColor(colorPinGold);
            canvas.drawRect(224 * scaleX, rowY + 2 * scaleY, 230 * scaleX, rowY + 8 * scaleY, paint);
        }

        // 4. Indicadores (Numero y Flecha)
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

        float indicatorY = (30 + pinStartRow * 16) * scaleY; // Y coord of the first pin row
        float rowCenterY = indicatorY + (5 * scaleY); // Center of the 10-unit high pin

        // Flecha Blanca - Tamaño moderado para evitar solape
        paint.setColor(Color.WHITE);
        Path path = new Path();
        float arrowWidth = 12 * scaleX;
        float arrowHeight = 10 * scaleY;
        path.moveTo(22 * scaleX, rowCenterY - arrowHeight);
        path.lineTo(40 * scaleX, rowCenterY);
        path.lineTo(22 * scaleX, rowCenterY + arrowHeight);
        path.close();
        canvas.drawPath(path, paint);

        // Texto del indicador - Resuelto solapamiento
        paint.setTextSize(24 * scaleY);
        paint.setFakeBoldText(true);
        // Desplazado para evitar la flecha si es de dos dígitos
        paint.setColor(Color.WHITE);
        canvas.drawText(indicatorText, 2 * scaleX, rowCenterY + 10 * scaleY, paint);

        // Brillo sutil en el texto
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.5f * scaleX);
        paint.setColor(Color.LTGRAY);
        canvas.drawText(indicatorText, 2 * scaleX, rowCenterY + 10 * scaleY, paint);
        paint.setStyle(Paint.Style.FILL);

        // 5. Cuerpo del Chip (Negro)
        int numPines = chip.getNumeroDePines();
        if (numPines > 0) {
            float left = 90 * scaleX;
            float right = 210 * scaleX;

            // Calculo exacto para que el chip coincida con los pines (2 unidades de margen arriba/abajo)
            float top = (30 + pinStartRow * 16 - 2) * scaleY;
            int numFilas = numPines / 2;
            float chipHeight = ((numFilas - 1) * 16 + 10 + 4) * scaleY;
            float bottom = top + chipHeight;

            int colorChipBody = Color.parseColor("#151515");
            int colorChipBorder = Color.parseColor("#404040");
            int colorNotch = Color.parseColor("#8B4513"); // Marrón
            int colorLegs = Color.parseColor("#BDBDBD"); // Plateado metálico

            // A. PATAS del Chip (debajo del cuerpo)
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < numFilas; i++) {
                float legY = (30 + (pinStartRow + i) * 16 + 3) * scaleY;
                // Pata Izquierda
                paint.setColor(colorLegs);
                canvas.drawRect(80 * scaleX, legY, 92 * scaleX, legY + 4 * scaleY, paint);
                // Pata Derecha
                canvas.drawRect(208 * scaleX, legY, 220 * scaleX, legY + 4 * scaleY, paint);
            }

            // B. Cuerpo
            paint.setColor(colorChipBody);
            canvas.drawRect(left, top, right, bottom, paint);

            // C. Modelo del Chip (Texto grabado) - Horizontal y más grande
            String chipName = chip.getNombreDelPic();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#D0D0D0")); // Gris claro láser
            paint.setTextSize(24 * scaleY); // Aumentado
            paint.setFakeBoldText(true);

            float chipCenterX = (left + right) / 2f;
            float chipCenterY = (top + bottom) / 2f;
            float textWidth = paint.measureText(chipName);

            // Dibujar centrado horizontalmente y verticalmente
            canvas.drawText(chipName, chipCenterX - textWidth / 2f, chipCenterY + (8 * scaleY), paint);

            // Borde del chip
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f * scaleX);
            paint.setColor(colorChipBorder);
            canvas.drawRect(left, top, right, bottom, paint);

            // D. Muesca (Notch)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colorNotch);
            float notchWidth = 40 * scaleX;
            float notchHeight = 18 * scaleY;
            canvas.drawArc(
                    (300 / 2f - notchWidth / 2f) * scaleX,
                    top - notchHeight / 2f,
                    (300 / 2f + notchWidth / 2f) * scaleX,
                    top + notchHeight / 2f,
                    0, 180, true, paint);

            // Sombra interna notch
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(0.8f * scaleX);
            paint.setColor(Color.BLACK);
            canvas.drawArc(
                    (300 / 2f - notchWidth / 2f) * scaleX,
                    top - notchHeight / 2f,
                    (300 / 2f + notchWidth / 2f) * scaleX,
                    top + notchHeight / 2f,
                    0, 180, false, paint);
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
