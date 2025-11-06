package com.diamon.tutorial.utilidadestutorial;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser optimizado para el tutorial
 * Compatible con API Level 23-36
 */
public class TutorialContentParser {

    private Context context;
    private LinearLayout containerLayout;
    private CodeBlockView codeBlockView;
    private String currentLanguage;

    // Patrones optimizados
    private static final Pattern STEP_PATTERN = Pattern.compile(
            "^([📦🔧📥📂⚙️🔨💾✅💻📋🚀])\\s*(PASO|STEP)\\s*(\\d+):\\s*(.+)$"
    );

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "^(pkg|wget|tar|cd|\\./configure|make|gpasm|gplink|gplib|cp|ls|nano|chmod|export|termux-setup-storage|cat|echo)\\s+"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)"
    );

    public TutorialContentParser(Context context, LinearLayout containerLayout) {
        this.context = context;
        this.containerLayout = containerLayout;
        this.codeBlockView = new CodeBlockView(context, containerLayout);
    }

    /**
     * Parsea el tutorial de forma optimizada
     */
    public void parseTutorial(String tutorialContent, String language) {
        this.currentLanguage = language;

        // Limpiar contenedor de forma eficiente
        if (containerLayout.getChildCount() > 0) {
            containerLayout.removeAllViews();
        }

        String[] lines = tutorialContent.split("\n");

        List<String> currentBlock = new ArrayList<>();
        BlockType currentBlockType = BlockType.TEXT;
        boolean inAsmBlock = false;
        int lineCount = lines.length;

        for (int i = 0; i < lineCount; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // Detectar inicio de bloque ensamblador
            if (trimmedLine.startsWith("; Programa") ||
                    trimmedLine.startsWith("; Program")) {

                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                }

                inAsmBlock = true;
                currentBlockType = BlockType.ASSEMBLY;
                currentBlock.add(line);
                continue;
            }

            // Detectar fin de bloque ensamblador
            if (inAsmBlock && trimmedLine.equals("END")) {
                currentBlock.add(line);
                processBlock(currentBlock, currentBlockType);
                currentBlock.clear();
                inAsmBlock = false;
                currentBlockType = BlockType.TEXT;
                continue;
            }

            // Acumular líneas del bloque ensamblador
            if (inAsmBlock) {
                currentBlock.add(line);
                continue;
            }

            // Detectar títulos de pasos
            Matcher stepMatcher = STEP_PATTERN.matcher(trimmedLine);
            if (stepMatcher.find()) {
                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                }

                addStepTitle(
                        stepMatcher.group(1),
                        stepMatcher.group(2),
                        stepMatcher.group(3),
                        stepMatcher.group(4)
                );
                currentBlockType = BlockType.TEXT;
                continue;
            }

            // Detectar "Deberías ver algo como:" o "You should see something like:"
            if (trimmedLine.contains("Resultado Esperado:") ||
                    trimmedLine.contains("Expected Result:") ||
                    trimmedLine.contains("ver algo como") ||
                    trimmedLine.contains("see something like")) {

                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                }

                addSectionTitle(trimmedLine);
                currentBlockType = BlockType.EXPECTED_OUTPUT;
                continue;
            }

            // Si estamos esperando output y la línea no está vacía
            if (currentBlockType == BlockType.EXPECTED_OUTPUT && !trimmedLine.isEmpty()) {
                // Acumular hasta encontrar línea vacía
                currentBlock.add(line);

                // Verificar si la siguiente línea está vacía o es una sección nueva
                if (i + 1 < lineCount && (lines[i + 1].trim().isEmpty() ||
                        STEP_PATTERN.matcher(lines[i + 1].trim()).find())) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                    currentBlockType = BlockType.TEXT;
                }
                continue;
            }

            // Detectar comandos de terminal
            Matcher commandMatcher = COMMAND_PATTERN.matcher(trimmedLine);
            if (commandMatcher.find() &&
                    !trimmedLine.startsWith("•") &&
                    !trimmedLine.startsWith("-")) {

                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                }

                currentBlock.add(trimmedLine);
                processBlock(currentBlock, BlockType.COMMAND);
                currentBlock.clear();
                currentBlockType = BlockType.TEXT;
                continue;
            }

            // Detectar notas especiales
            if (trimmedLine.startsWith("ℹ️") ||
                    trimmedLine.startsWith("⚠️") ||
                    trimmedLine.startsWith("📋") ||
                    trimmedLine.startsWith("✨") ||
                    trimmedLine.startsWith("⏱️") ||
                    trimmedLine.startsWith("📁") ||
                    trimmedLine.startsWith("✅")) {

                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                }

                addNote(trimmedLine);
                currentBlockType = BlockType.TEXT;
                continue;
            }

            // Detectar secciones secundarias
            if (trimmedLine.matches("^[📋📁✨📝💡🔍⚙️🔨💻📥📂⏱️]\\s+.+:$")) {
                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                }

                addSectionTitle(trimmedLine);
                currentBlockType = BlockType.TEXT;
                continue;
            }

            // Líneas vacías separan bloques
            if (trimmedLine.isEmpty()) {
                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, currentBlockType);
                    currentBlock.clear();
                    currentBlockType = BlockType.TEXT;
                }
                continue;
            }

            // Acumular línea normal
            currentBlock.add(line);
        }

        // Procesar último bloque
        if (!currentBlock.isEmpty()) {
            processBlock(currentBlock, currentBlockType);
        }
    }

    /**
     * Procesa un bloque según su tipo
     */
    private void processBlock(List<String> block, BlockType type) {
        if (block.isEmpty()) return;

        switch (type) {
            case COMMAND:
                String command = String.join("\n", block).trim();
                codeBlockView.addCommandBlock(command, currentLanguage);
                break;

            case ASSEMBLY:
                String asmCode = String.join("\n", block);
                codeBlockView.addAssemblyBlock(asmCode, currentLanguage);
                break;

            case EXPECTED_OUTPUT:
                String output = String.join("\n", block);
                codeBlockView.addExpectedOutputBlock(output, currentLanguage);
                break;

            case TEXT:
            default:
                addFormattedText(block);
                break;
        }
    }

    /**
     * Añade título de paso
     */
    private void addStepTitle(String emoji, String stepWord, String stepNumber, String title) {
        MaterialCardView cardView = new MaterialCardView(context);
        cardView.setCardElevation(2);
        cardView.setRadius(12);
        cardView.setCardBackgroundColor(0xFFE3F2FD);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 24, 0, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout stepLayout = new LinearLayout(context);
        stepLayout.setOrientation(LinearLayout.VERTICAL);
        stepLayout.setPadding(16, 12, 16, 12);

        TextView titleView = new TextView(context);
        String fullTitle = emoji + " " + stepWord + " " + stepNumber + ": " + title;

        SpannableString spannable = new SpannableString(fullTitle);
        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                fullTitle.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        titleView.setText(spannable);
        titleView.setTextSize(18);
        titleView.setTextColor(0xFF1565C0);

        stepLayout.addView(titleView);
        cardView.addView(stepLayout);
        containerLayout.addView(cardView);
    }

    /**
     * Añade título de sección
     */
    private void addSectionTitle(String sectionText) {
        TextView sectionView = new TextView(context);

        SpannableString spannable = new SpannableString(sectionText);
        spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                sectionText.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        sectionView.setText(spannable);
        sectionView.setTextSize(15);
        sectionView.setTextColor(0xFF424242);
        sectionView.setPadding(0, 16, 0, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        sectionView.setLayoutParams(params);

        containerLayout.addView(sectionView);
    }

    /**
     * Añade texto formateado con enlaces clickeables
     */
    private void addFormattedText(List<String> textLines) {
        String text = String.join("\n", textLines);

        if (text.trim().isEmpty()) {
            return;
        }

        TextView textView = new TextView(context);
        textView.setText(formatTextWithLinks(text));
        textView.setTextSize(14);
        textView.setTextColor(0xFF424242);
        textView.setLineSpacing(6, 1.0f);
        textView.setPadding(4, 8, 4, 8);
        textView.setTextIsSelectable(true);

        // Hacer los enlaces clickeables
        textView.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);
        textView.setLinksClickable(true);
        textView.setLinkTextColor(0xFF1976D2);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        textView.setLayoutParams(params);

        containerLayout.addView(textView);
    }

    /**
     * Formatea el texto con negritas y enlaces
     */
    private CharSequence formatTextWithLinks(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        // Palabras clave en negrita
        String[] keywords = {
                "Importante", "Important",
                "Nota", "Note",
                "Advertencia", "Warning",
                "Explicación", "Explanation",
                "Instalación", "Installation",
                "Configuración", "Configuration",
                "Compilación", "Compilation",
                "Paquetes Instalados", "Installed Packages",
                "Utilidades", "Utilities",
                "Opciones de Configuración", "Configuration Options",
                "Ubicaciones de Instalación", "Installation Locations",
                "Resultado Esperado", "Expected Result",
                "Instrucciones Finales", "Final Instructions",
                "Archivos Generados", "Generated Files"
        };

        for (String keyword : keywords) {
            int start = 0;
            String lowerText = text.toLowerCase();
            String lowerKeyword = keyword.toLowerCase();

            while ((start = lowerText.indexOf(lowerKeyword, start)) != -1) {
                boolean isStart = (start == 0 || !Character.isLetterOrDigit(text.charAt(start - 1)));
                boolean isEnd = (start + keyword.length() >= text.length() ||
                        !Character.isLetterOrDigit(text.charAt(start + keyword.length())));

                if (isStart && isEnd) {
                    builder.setSpan(
                            new StyleSpan(Typeface.BOLD),
                            start,
                            start + keyword.length(),
                            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
                start += keyword.length();
            }
        }

        return builder;
    }

    /**
     * Añade nota con color según tipo
     */
    private void addNote(String noteLine) {
        MaterialCardView cardView = new MaterialCardView(context);
        cardView.setCardElevation(2);
        cardView.setRadius(8);

        int backgroundColor;
        int textColor;

        if (noteLine.contains("⚠️")) {
            backgroundColor = 0xFFFFF3E0;
            textColor = 0xFFE65100;
        } else if (noteLine.contains("✅") || noteLine.contains("✨")) {
            backgroundColor = 0xFFE8F5E9;
            textColor = 0xFF2E7D32;
        } else {
            backgroundColor = 0xFFE3F2FD;
            textColor = 0xFF1565C0;
        }

        cardView.setCardBackgroundColor(backgroundColor);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 12, 0, 12);
        cardView.setLayoutParams(cardParams);

        LinearLayout noteLayout = new LinearLayout(context);
        noteLayout.setOrientation(LinearLayout.VERTICAL);
        noteLayout.setPadding(16, 12, 16, 12);

        TextView noteText = new TextView(context);
        noteText.setText(noteLine);
        noteText.setTextSize(13);
        noteText.setTextColor(textColor);
        noteText.setLineSpacing(4, 1.0f);
        noteText.setTextIsSelectable(true);

        noteLayout.addView(noteText);
        cardView.addView(noteLayout);
        containerLayout.addView(cardView);
    }

    /**
     * Tipos de bloques
     */
    private enum BlockType {
        TEXT,
        COMMAND,
        ASSEMBLY,
        NOTE,
        EXPECTED_OUTPUT
    }
}