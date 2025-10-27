package com.diamon.tutorial.utilidadestutorial;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Clase para manejar la interacción y comportamiento del tutorial
 * Proporciona métodos para seleccionar, copiar y navegar en el tutorial
 */
public class TutorialInteractionHandler {

    private TextView tutorialTextView;
    private ScrollView scrollView;
    private Context context;
    private TutorialManager tutorialManager;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFF00; // Amarillo para destacar

    public TutorialInteractionHandler(TextView textView, ScrollView scroll, Context context) {
        this.tutorialTextView = textView;
        this.scrollView = scroll;
        this.context = context;
        this.tutorialManager = new TutorialManager(context);

        // Configurar interacciones
        setupInteractions();
    }

    /**
     * Configura los gestos y eventos de interacción
     */
    private void setupInteractions() {
        // Hacer el texto seleccionable
        tutorialTextView.setTextIsSelectable(true);

        // Permitir scroll con el dedo
        tutorialTextView.setOnTouchListener((v, event) -> {
            scrollView.onTouchEvent(event);
            return false;
        });

        // Configurar doble toque para copiar
        tutorialTextView.setOnLongClickListener(v -> {
            copySelectedText();
            return true;
        });
    }

    /**
     * Copia el texto seleccionado al portapapeles
     */
    private void copySelectedText() {
        int selStart = tutorialTextView.getSelectionStart();
        int selEnd = tutorialTextView.getSelectionEnd();
        if (selStart != selEnd && selStart != -1 && selEnd != -1) {
            String selectedText = tutorialTextView.getText()
                    .subSequence(selStart, selEnd).toString();
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) context.getSystemService(
                            Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("tutorial_text", selectedText);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
        }

        android.widget.Toast.makeText(context, "Texto copiado al portapapeles",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    /**
     * Desplaza hacia un paso específico del tutorial
     * @param stepNumber Número del paso a que desplazarse
     */
    public void scrollToStep(int stepNumber) {
        String searchText = "PASO " + stepNumber;
        CharSequence tutorialContent = tutorialTextView.getText();
        String content = tutorialContent.toString();
        int startPos = content.indexOf(searchText);

        if (startPos != -1 && tutorialTextView.getLayout() != null) {
            // Calcular la posición en píxeles
            int line = tutorialTextView.getLayout().getLineForOffset(startPos);
            int y = tutorialTextView.getLayout().getLineTop(line);
            scrollView.smoothScrollTo(0, y);
        }
    }

    /**
     * Busca un término en el tutorial y lo destaca
     * @param searchTerm Término a buscar
     * @return true si se encontró, false en caso contrario
     */
    public boolean search(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return false;
        }

        CharSequence tutorialContent = tutorialTextView.getText();
        String content = tutorialContent.toString();
        int startPos = content.indexOf(searchTerm);

        if (startPos != -1) {
            // Crear un SpannableString para destacar el término encontrado
            SpannableStringBuilder builder = new SpannableStringBuilder(tutorialContent);

            // Aplicar color de fondo al término encontrado
            builder.setSpan(
                    new BackgroundColorSpan(HIGHLIGHT_COLOR),
                    startPos,
                    startPos + searchTerm.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // Actualizar el TextView con el texto destacado
            tutorialTextView.setText(builder);

            // Desplazar para que sea visible
            if (tutorialTextView.getLayout() != null) {
                int line = tutorialTextView.getLayout().getLineForOffset(startPos);
                int y = tutorialTextView.getLayout().getLineTop(line);
                scrollView.smoothScrollTo(0, y);
            }

            return true;
        }

        android.widget.Toast.makeText(context, "Término no encontrado: " + searchTerm,
                android.widget.Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Busca todos los términos en el tutorial y los destaca con diferentes colores
     * @param searchTerm Término a buscar
     * @return Número de coincidencias encontradas
     */
    public int searchAll(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return 0;
        }

        CharSequence tutorialContent = tutorialTextView.getText();
        String content = tutorialContent.toString();
        SpannableStringBuilder builder = new SpannableStringBuilder(tutorialContent);

        int count = 0;
        int startPos = 0;

        // Encontrar y destacar todas las coincidencias
        while ((startPos = content.indexOf(searchTerm, startPos)) != -1) {
            builder.setSpan(
                    new BackgroundColorSpan(HIGHLIGHT_COLOR),
                    startPos,
                    startPos + searchTerm.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            count++;
            startPos += searchTerm.length();
        }

        if (count > 0) {
            tutorialTextView.setText(builder);
            android.widget.Toast.makeText(context,
                    "Se encontraron " + count + " coincidencias",
                    android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(context,
                    "Término no encontrado: " + searchTerm,
                    android.widget.Toast.LENGTH_SHORT).show();
        }

        return count;
    }

    /**
     * Limpia todos los highlights del tutorial
     */
    public void clearHighlights() {
        CharSequence tutorialContent = tutorialTextView.getText();
        if (tutorialContent instanceof Spannable) {
            Spannable spannable = (Spannable) tutorialContent;
            BackgroundColorSpan[] spans = spannable.getSpans(
                    0,
                    tutorialContent.length(),
                    BackgroundColorSpan.class
            );
            for (BackgroundColorSpan span : spans) {
                spannable.removeSpan(span);
            }
        }
    }

    /**
     * Obtiene el texto completo del tutorial
     * @return String con todo el contenido del tutorial
     */
    public String getFullTutorialText() {
        return tutorialTextView.getText().toString();
    }

    /**
     * Establece el idioma del tutorial
     * @param language "es" para español o "en" para inglés
     */
    public void setLanguage(String language) {
        tutorialManager.setLanguage(language);
    }

    /**
     * Obtiene una traducción
     * @param key Clave de traducción
     * @return Texto traducido
     */
    public String getTranslation(String key) {
        return tutorialManager.getTranslation(key);
    }
}
