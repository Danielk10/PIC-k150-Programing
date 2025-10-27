package com.diamon.tutorial.utilidadestutorial;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.widget.TextView;

/**
 * Clase para aplicar formatos especiales al texto del tutorial
 * Proporciona métodos para colorear, estilos y formatos personalizados
 */
public class TextFormatter {

    private Context context;
    private static final int COLOR_KEYWORD = 0xFF0066CC;      // Azul para palabras clave
    private static final int COLOR_STRING = 0xFF66BB6A;        // Verde para strings
    private static final int COLOR_COMMENT = 0xFF888888;       // Gris para comentarios
    private static final int COLOR_ERROR = 0xFFD32F2F;         // Rojo para errores
    private static final int COLOR_SUCCESS = 0xFF388E3C;       // Verde oscuro para éxito
    private static final int COLOR_WARNING = 0xFFFFA500;       // Naranja para advertencias
    private static final int COLOR_CODE_BG = 0xFF1A1A1A;       // Negro para fondo de código
    private static final int COLOR_CODE_TEXT = 0xFFEEEEEE;     // Blanco roto para texto de código

    public TextFormatter(Context context) {
        this.context = context;
    }

    /**
     * Formatea código en un TextView con colores de sintaxis
     * @param textView TextView donde mostrar el código
     * @param code Código a formatear
     */
    public void formatCode(TextView textView, String code) {
        SpannableStringBuilder builder = new SpannableStringBuilder(code);

        // Aplicar fondo negro
        textView.setBackgroundColor(COLOR_CODE_BG);
        textView.setTextColor(COLOR_CODE_TEXT);
        textView.setTypeface(Typeface.MONOSPACE);

        // Formatear líneas comentadas
        String[] lines = code.split("\n");
        int currentPos = 0;

        for (String line : lines) {
            if (line.trim().startsWith("#") || line.trim().startsWith(";")) {
                // Es un comentario
                int startPos = currentPos;
                int endPos = currentPos + line.length();
                builder.setSpan(
                        new ForegroundColorSpan(COLOR_COMMENT),
                        startPos,
                        endPos,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else if (line.trim().startsWith("\"") || line.trim().startsWith("'")) {
                // Es una cadena
                int startPos = currentPos;
                int endPos = currentPos + line.length();
                builder.setSpan(
                        new ForegroundColorSpan(COLOR_STRING),
                        startPos,
                        endPos,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            currentPos += line.length() + 1; // +1 para el salto de línea
        }

        textView.setText(builder);
    }

    /**
     * Colorea palabras clave en un texto
     * @param text Texto donde buscar palabras clave
     * @param keywords Array de palabras clave a colorear
     * @param color Color a aplicar
     * @return SpannableString formateado
     */
    public SpannableString highlightKeywords(String text, String[] keywords, int color) {
        SpannableString spannable = new SpannableString(text);

        for (String keyword : keywords) {
            int start = 0;
            while ((start = text.indexOf(keyword, start)) != -1) {
                int end = start + keyword.length();
                spannable.setSpan(
                        new ForegroundColorSpan(color),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                start = end;
            }
        }

        return spannable;
    }

    /**
     * Crea un texto con estilo de comando del terminal
     * @param command Comando a formatear
     * @return SpannableString formateado
     */
    public SpannableString formatTerminalCommand(String command) {
        SpannableString spannable = new SpannableString("$ " + command);

        // Aplicar monospace al comando
        spannable.setSpan(
                new TypefaceSpan("monospace"),
                0,
                spannable.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Colorear símbolo de prompt
        spannable.setSpan(
                new ForegroundColorSpan(0xFF00AA00),  // Verde
                0,
                2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannable;
    }

    /**
     * Formatea un bloque de nota con estilo destacado
     * @param emoji Emoticón de la nota
     * @param title Título de la nota
     * @param content Contenido de la nota
     * @return SpannableString formateado
     */
    public SpannableString formatNote(String emoji, String title, String content) {
        String fullText = emoji + " " + title + ": " + content;
        SpannableStringBuilder builder = new SpannableStringBuilder(fullText);

        // Bold para el título
        int titleStart = emoji.length() + 1;
        int titleEnd = titleStart + title.length();
        builder.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                titleStart,
                titleEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Color para el emoji
        builder.setSpan(
                new ForegroundColorSpan(0xFFFFA500),  // Naranja
                0,
                emoji.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return new SpannableString(builder);
    }

    /**
     * Formatea un paso del tutorial
     * @param stepNumber Número del paso
     * @param stepTitle Título del paso
     * @param emoji Emoticón del paso
     * @return SpannableString formateado
     */
    public SpannableString formatStepTitle(int stepNumber, String stepTitle, String emoji) {
        String fullText = emoji + " PASO " + stepNumber + ": " + stepTitle;
        SpannableStringBuilder builder = new SpannableStringBuilder(fullText);

        // Colorear número del paso
        String stepNum = String.valueOf(stepNumber);
        int stepNumStart = fullText.indexOf(stepNum);
        int stepNumEnd = stepNumStart + stepNum.length();

        builder.setSpan(
                new ForegroundColorSpan(COLOR_KEYWORD),
                stepNumStart,
                stepNumEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Bold para el título del paso
        int titleStart = fullText.lastIndexOf(": ") + 2;
        builder.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                titleStart,
                fullText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return new SpannableString(builder);
    }

    /**
     * Crea un texto de error formateado
     * @param errorMessage Mensaje de error
     * @return SpannableString formateado
     */
    public SpannableString formatError(String errorMessage) {
        SpannableString spannable = new SpannableString("❌ ERROR: " + errorMessage);

        spannable.setSpan(
                new ForegroundColorSpan(COLOR_ERROR),
                0,
                spannable.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                0,
                9,  // "❌ ERROR:"
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannable;
    }

    /**
     * Crea un texto de éxito formateado
     * @param successMessage Mensaje de éxito
     * @return SpannableString formateado
     */
    public SpannableString formatSuccess(String successMessage) {
        SpannableString spannable = new SpannableString("✅ ÉXITO: " + successMessage);

        spannable.setSpan(
                new ForegroundColorSpan(COLOR_SUCCESS),
                0,
                spannable.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                0,
                9,  // "✅ ÉXITO:"
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannable;
    }

    /**
     * Crea un texto de advertencia formateado
     * @param warningMessage Mensaje de advertencia
     * @return SpannableString formateado
     */
    public SpannableString formatWarning(String warningMessage) {
        SpannableString spannable = new SpannableString("⚠️ ADVERTENCIA: " + warningMessage);

        spannable.setSpan(
                new ForegroundColorSpan(COLOR_WARNING),
                0,
                spannable.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                0,
                15,  // "⚠️ ADVERTENCIA:"
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return spannable;
    }
}
