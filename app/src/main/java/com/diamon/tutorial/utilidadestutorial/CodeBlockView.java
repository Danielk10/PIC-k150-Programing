package com.diamon.tutorial.utilidadestutorial;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Clase para manejar bloques de código con capacidad de copiar
 * Proporciona un contenedor visual para código con emoticones y botón de copiar
 */
public class CodeBlockView {

    private LinearLayout codeContainer;
    private String codeContent;
    private Context context;

    public CodeBlockView(Context context, LinearLayout container) {
        this.context = context;
        this.codeContainer = container;
    }

    /**
     * Añade un bloque de código al contenedor
     * @param emoji Emoticón para el título
     * @param title Título del bloque
     * @param code Código a mostrar
     */
    public void addCodeBlock(String emoji, String title, String code) {
        this.codeContent = code;

        // Crear LinearLayout para el bloque
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setPadding(12, 12, 12, 12);
        blockLayout.setBackgroundColor(0xFFFAFAFA);

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blockParams.setMargins(8, 8, 8, 8);
        blockLayout.setLayoutParams(blockParams);

        // Crear header con título y botón
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        headerLayout.setLayoutParams(headerParams);

        // Título con emoticón
        TextView titleView = new TextView(context);
        titleView.setText(emoji + " " + title);
        titleView.setTextSize(14);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(0xFF424242);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        titleView.setLayoutParams(titleParams);
        headerLayout.addView(titleView);

        // Botón de copiar
        ImageButton copyBtn = new ImageButton(context);
        copyBtn.setImageResource(android.R.drawable.ic_menu_edit);
        copyBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                48,
                48);
        copyBtn.setLayoutParams(btnParams);
        copyBtn.setOnClickListener(v -> copyToClipboard(code));
        headerLayout.addView(copyBtn);

        blockLayout.addView(headerLayout);

        // Separador
        View separator = new View(context);
        separator.setBackgroundColor(0xFFE0E0E0);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2);
        sepParams.setMargins(0, 8, 0, 8);
        separator.setLayoutParams(sepParams);
        blockLayout.addView(separator);

        // Código con fondo negro
        TextView codeView = new TextView(context);
        codeView.setText(code);
        codeView.setTextColor(0xFFFAFAFA);
        codeView.setBackgroundColor(0xFF1A1A1A);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextSize(12);
        codeView.setPadding(12, 12, 12, 12);
        codeView.setTextIsSelectable(true);
        LinearLayout.LayoutParams codeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        codeView.setLayoutParams(codeParams);
        blockLayout.addView(codeView);

        // Añadir al contenedor principal
        codeContainer.addView(blockLayout);
    }

    /**
     * Copia el código al portapapeles
     * @param code Código a copiar
     */
    private void copyToClipboard(String code) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(
                        Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("code", code);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    /**
     * Añade una nota explicativa con emoticón
     * @param emoji Emoticón para la nota
     * @param noteText Texto de la nota
     */
    public void addNote(String emoji, String noteText) {
        LinearLayout noteLayout = new LinearLayout(context);
        noteLayout.setOrientation(LinearLayout.VERTICAL);
        noteLayout.setBackgroundColor(0xFFE8F5E9);
        noteLayout.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        noteParams.setMargins(8, 8, 8, 8);
        noteLayout.setLayoutParams(noteParams);

        TextView noteTitle = new TextView(context);
        noteTitle.setText(emoji + " Nota:");
        noteTitle.setTextSize(13);
        noteTitle.setTypeface(null, Typeface.BOLD);
        noteTitle.setTextColor(0xFF2E7D32);
        noteLayout.addView(noteTitle);

        TextView noteContent = new TextView(context);
        noteContent.setText(noteText);
        noteContent.setTextSize(12);
        noteContent.setTextColor(0xFF424242);
        noteContent.setLineSpacing(4, 1.0f);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        contentParams.topMargin = 6;
        noteContent.setLayoutParams(contentParams);
        noteLayout.addView(noteContent);

        codeContainer.addView(noteLayout);
    }

    /**
     * Añade un separador visual
     */
    public void addSeparator() {
        View separator = new View(context);
        separator.setBackgroundColor(0xFFBDBDBD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                3);
        params.setMargins(8, 16, 8, 16);
        separator.setLayoutParams(params);
        codeContainer.addView(separator);
    }
}
