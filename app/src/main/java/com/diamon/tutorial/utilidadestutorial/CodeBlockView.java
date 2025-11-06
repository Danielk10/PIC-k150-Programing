package com.diamon.tutorial.utilidadestutorial;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Vista de bloques de código optimizada
 * Compatible con API Level 23-36
 */
public class CodeBlockView {

    private LinearLayout codeContainer;
    private Context context;

    // Colores para sintaxis
    private static final int COLOR_KEYWORD = 0xFF569CD6;
    private static final int COLOR_COMMENT = 0xFF6A9955;
    private static final int COLOR_NUMBER = 0xFFB5CEA8;
    private static final int COLOR_REGISTER = 0xFF4EC9B0;
    private static final int COLOR_DIRECTIVE = 0xFFC586C0;
    private static final int COLOR_LABEL = 0xFFDCDCAA;

    public CodeBlockView(Context context, LinearLayout container) {
        this.context = context;
        this.codeContainer = container;
    }

    /**
     * Añade bloque de comando
     */
    public void addCommandBlock(String command, String language) {
        MaterialCardView cardView = new MaterialCardView(context);
        cardView.setCardElevation(4);
        cardView.setRadius(12);
        cardView.setCardBackgroundColor(0xFF1E1E1E);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 16, 0, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setPadding(16, 16, 16, 16);

        // Header
        LinearLayout headerLayout = createHeader(
                language.equals("es") ? "💻 Comando" : "💻 Command",
                command
        );
        blockLayout.addView(headerLayout);

        // Separador
        blockLayout.addView(createSeparator());

        // Código
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollView.setLayoutParams(scrollParams);
        scrollView.setHorizontalScrollBarEnabled(true);

        TextView codeView = new TextView(context);
        codeView.setText(command);
        codeView.setTextColor(0xFFD4D4D4);
        codeView.setBackgroundColor(Color.TRANSPARENT);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextSize(13);
        codeView.setPadding(12, 12, 12, 12);
        codeView.setTextIsSelectable(true);

        scrollView.addView(codeView);
        blockLayout.addView(scrollView);

        cardView.addView(blockLayout);
        codeContainer.addView(cardView);
    }

    /**
     * Añade bloque de código ensamblador
     */
    public void addAssemblyBlock(String asmCode, String language) {
        MaterialCardView cardView = new MaterialCardView(context);
        cardView.setCardElevation(4);
        cardView.setRadius(12);
        cardView.setCardBackgroundColor(0xFF1E1E1E);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 16, 0, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setPadding(16, 16, 16, 16);

        // Header
        LinearLayout headerLayout = createHeader(
                language.equals("es") ? "📝 Código Ensamblador" : "📝 Assembly Code",
                asmCode
        );
        blockLayout.addView(headerLayout);

        // Separador
        blockLayout.addView(createSeparator());

        // Código con numeración
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollView.setLayoutParams(scrollParams);
        scrollView.setHorizontalScrollBarEnabled(true);

        LinearLayout codeWithLineNumbers = new LinearLayout(context);
        codeWithLineNumbers.setOrientation(LinearLayout.HORIZONTAL);

        // Números de línea
        codeWithLineNumbers.addView(createLineNumbers(asmCode));

        // Separador vertical
        View verticalSep = new View(context);
        verticalSep.setBackgroundColor(0xFF3E3E42);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(2,
                LinearLayout.LayoutParams.MATCH_PARENT);
        sepParams.setMargins(8, 0, 8, 0);
        verticalSep.setLayoutParams(sepParams);
        codeWithLineNumbers.addView(verticalSep);

        // Código con resaltado
        TextView codeView = new TextView(context);
        codeView.setText(highlightAssemblyCode(asmCode));
        codeView.setBackgroundColor(Color.TRANSPARENT);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextSize(13);
        codeView.setPadding(12, 12, 12, 12);
        codeView.setTextIsSelectable(true);

        codeWithLineNumbers.addView(codeView);
        scrollView.addView(codeWithLineNumbers);
        blockLayout.addView(scrollView);

        cardView.addView(blockLayout);
        codeContainer.addView(cardView);
    }

    /**
     * Añade bloque de output esperado
     */
    public void addExpectedOutputBlock(String output, String language) {
        MaterialCardView cardView = new MaterialCardView(context);
        cardView.setCardElevation(4);
        cardView.setRadius(12);
        cardView.setCardBackgroundColor(0xFF263238); // Color diferente para output

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 16, 0, 16);
        cardView.setLayoutParams(cardParams);

        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setPadding(16, 16, 16, 16);

        // Header
        LinearLayout headerLayout = createHeader(
                language.equals("es") ? "✨ Salida Esperada" : "✨ Expected Output",
                output
        );
        blockLayout.addView(headerLayout);

        // Separador
        blockLayout.addView(createSeparator());

        // Output
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        scrollView.setLayoutParams(scrollParams);

        TextView outputView = new TextView(context);
        outputView.setText(output);
        outputView.setTextColor(0xFF80CBC4); // Verde agua para output
        outputView.setBackgroundColor(Color.TRANSPARENT);
        outputView.setTypeface(Typeface.MONOSPACE);
        outputView.setTextSize(13);
        outputView.setPadding(12, 12, 12, 12);
        outputView.setTextIsSelectable(true);

        scrollView.addView(outputView);
        blockLayout.addView(scrollView);

        cardView.addView(blockLayout);
        codeContainer.addView(cardView);
    }

    /**
     * Crea header con botón de copiar (ICONO CORRECTO)
     */
    private LinearLayout createHeader(String title, String code) {
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerLayout.setLayoutParams(headerParams);

        // Título
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(14);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(0xFFD4D4D4);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        titleView.setLayoutParams(titleParams);
        headerLayout.addView(titleView);

        // Botón copiar con MaterialButton (SIN ICONO, SOLO TEXTO)
        MaterialButton copyBtn = new MaterialButton(context);
        copyBtn.setText("Copiar");
        copyBtn.setTextSize(12);
        copyBtn.setTextColor(0xFFFFFFFF);
        copyBtn.setBackgroundColor(0xFF4CAF50);
        copyBtn.setCornerRadius(8);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        copyBtn.setLayoutParams(btnParams);
        copyBtn.setOnClickListener(v -> copyToClipboard(code));

        headerLayout.addView(copyBtn);

        return headerLayout;
    }

    /**
     * Crea separador
     */
    private View createSeparator() {
        View separator = new View(context);
        separator.setBackgroundColor(0xFF3E3E42);

        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
        );
        sepParams.setMargins(0, 12, 0, 12);
        separator.setLayoutParams(sepParams);

        return separator;
    }

    /**
     * Crea números de línea
     */
    private LinearLayout createLineNumbers(String code) {
        LinearLayout lineNumbersLayout = new LinearLayout(context);
        lineNumbersLayout.setOrientation(LinearLayout.VERTICAL);
        lineNumbersLayout.setBackgroundColor(0xFF252526);
        lineNumbersLayout.setPadding(8, 12, 8, 12);

        String[] lines = code.split("\n");
        for (int i = 1; i <= lines.length; i++) {
            TextView lineNumber = new TextView(context);
            lineNumber.setText(String.valueOf(i));
            lineNumber.setTextColor(0xFF858585);
            lineNumber.setTextSize(13);
            lineNumber.setTypeface(Typeface.MONOSPACE);
            lineNumber.setGravity(Gravity.END);
            lineNumber.setPadding(4, 0, 4, 0);

            lineNumbersLayout.addView(lineNumber);
        }

        return lineNumbersLayout;
    }

    /**
     * Resaltado de sintaxis optimizado
     */
    private SpannableStringBuilder highlightAssemblyCode(String code) {
        SpannableStringBuilder builder = new SpannableStringBuilder(code);

        String[] lines = code.split("\n");
        int currentPos = 0;

        String[] keywords = {"LIST", "INCLUDE", "ORG", "END", "CBLOCK", "ENDC",
                "goto", "call", "return", "banksel", "movlw", "movwf",
                "bsf", "bcf", "btfss", "btfsc", "decfsz", "incfsz",
                "addwf", "subwf", "andwf", "iorwf", "xorwf", "comf",
                "rlf", "rrf", "swapf", "clrf", "clrw", "nop"};

        String[] directives = {"__CONFIG", "#include", "#define", "equ", "EQU"};

        for (String line : lines) {
            int lineStart = currentPos;
            int lineEnd = currentPos + line.length();

            if (line.trim().startsWith(";")) {
                builder.setSpan(
                        new ForegroundColorSpan(COLOR_COMMENT),
                        lineStart, lineEnd,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                for (String keyword : keywords) {
                    highlightWord(builder, line, keyword, COLOR_KEYWORD, lineStart);
                }

                for (String directive : directives) {
                    highlightWord(builder, line, directive, COLOR_DIRECTIVE, lineStart);
                }

                highlightPattern(builder, line, "\\b[A-Z][A-Z0-9]+\\b", COLOR_REGISTER, lineStart);
                highlightPattern(builder, line, "\\b0x[0-9A-Fa-f]+\\b|\\b\\d+\\b|\\bb'[01]+'\\b",
                        COLOR_NUMBER, lineStart);
                highlightPattern(builder, line, "^\\w+:", COLOR_LABEL, lineStart);
            }

            currentPos += line.length() + 1;
        }

        return builder;
    }

    private void highlightWord(SpannableStringBuilder builder, String line,
                               String word, int color, int lineStart) {
        int index = 0;
        while ((index = line.indexOf(word, index)) != -1) {
            boolean isWord = (index == 0 || !Character.isLetterOrDigit(line.charAt(index - 1))) &&
                    (index + word.length() >= line.length() ||
                            !Character.isLetterOrDigit(line.charAt(index + word.length())));

            if (isWord) {
                builder.setSpan(
                        new ForegroundColorSpan(color),
                        lineStart + index,
                        lineStart + index + word.length(),
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            index += word.length();
        }
    }

    private void highlightPattern(SpannableStringBuilder builder, String line,
                                  String pattern, int color, int lineStart) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(line);

        while (m.find()) {
            builder.setSpan(
                    new ForegroundColorSpan(color),
                    lineStart + m.start(),
                    lineStart + m.end(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    /**
     * Copia al portapapeles
     */
    private void copyToClipboard(String code) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                Context.CLIPBOARD_SERVICE
        );
        ClipData clip = ClipData.newPlainText("code", code);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "✅ Código copiado", Toast.LENGTH_SHORT).show();
        }
    }
}