package com.diamon.tutorial;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
import android.widget.RelativeLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderizador para tutoriales en formato texto (.txt).
 * Optimizado para GPUTILS con resaltado de sintaxis y soporte multi-idioma.
 */
public class LegacyTutorialRenderer {
    private final Context context;
    private final LinearLayout container;
    private String lang = "es";

    public LegacyTutorialRenderer(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
    }

    public void setLanguage(String lang) {
        this.lang = lang;
    }

    public void renderTutorial(String content) {
        container.removeAllViews();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty())
                continue;

            // Detección de Bloques de Código (Comandos o Ensamblador)
            if (isCommandLine(line) || isAssemblyCode(line)) {
                StringBuilder blockBuilder = new StringBuilder();
                boolean isAsmBlock = false;

                // Determinar si el bloque empieza como ensamblador
                if (isAssemblyCode(line))
                    isAsmBlock = true;

                // Agrupar líneas consecutivas o líneas que pertenecen al contexto del bloque
                while (i < lines.length) {
                    String currentLine = lines[i];
                    String trimmedCurrent = currentLine.trim();

                    if (isAsmBlock) {
                        // Lógica de agrupación de Ensamblador
                        if (trimmedCurrent.isEmpty() && i + 1 < lines.length && isAssemblyCode(lines[i + 1])) {
                            blockBuilder.append("\n");
                            i++;
                            continue;
                        }
                        if (isAssemblyCode(currentLine) || currentLine.startsWith("    ")
                                || currentLine.startsWith("\t") || trimmedCurrent.startsWith(";")) {
                            blockBuilder.append(currentLine).append("\n");
                            i++;
                            continue;
                        }
                        break;
                    } else {
                        // Lógica de agrupación de Comandos (Shell)
                        if (isCommandLine(currentLine)) {
                            blockBuilder.append(currentLine).append("\n");
                            i++;
                            continue;
                        }
                        break;
                    }
                }

                String finalBlock = blockBuilder.toString().trim();
                if (!finalBlock.isEmpty()) {
                    if (isAsmBlock) {
                        addAssemblyCodeBlock(finalBlock);
                    } else {
                        addCommandBlock(finalBlock);
                    }
                }
                i--; // Ajustar índice
                continue;
            }

            // Detección de Títulos (Emojis o PASO X:)
            if (trimmedLine.contains("PASO") || trimmedLine.contains("STEP") ||
                    trimmedLine.contains("📋") || trimmedLine.contains("ℹ️") || trimmedLine.contains("⚠️") ||
                    trimmedLine.contains("✨") || trimmedLine.contains("📝") || trimmedLine.contains("📂") ||
                    trimmedLine.contains("⏱️") || trimmedLine.contains("💡") || trimmedLine.contains("💾") ||
                    trimmedLine.contains("🔨") || trimmedLine.contains("📦") || trimmedLine.contains("🔧") ||
                    trimmedLine.contains("🔍") || trimmedLine.contains("✅") || trimmedLine.contains("🚀")) {

                if (trimmedLine.length() < 100) {
                    addTitle(trimmedLine);
                } else {
                    addNormalText(trimmedLine);
                }
                continue;
            }

            // Texto normal o parámetros
            addNormalText(line);
        }
    }

    private boolean isCommandLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("pkg ") || trimmed.startsWith("wget ") ||
                trimmed.startsWith("tar ") || trimmed.startsWith("cd ") ||
                trimmed.startsWith("./") || trimmed.startsWith("make") ||
                trimmed.startsWith("nano ") || trimmed.startsWith("gpasm ") ||
                trimmed.startsWith("gplink ") || trimmed.startsWith("gplib ") ||
                trimmed.startsWith("ls ") || trimmed.startsWith("cp ") ||
                trimmed.startsWith("chmod ") || trimmed.startsWith("export ") ||
                trimmed.startsWith("echo ") || trimmed.startsWith("cat ") ||
                trimmed.startsWith("adb ") || trimmed.startsWith("termux-setup-storage");
    }

    private boolean isAssemblyCode(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty())
            return false;

        // Comentarios internos del código (con sangría o cortos después de instrucción)
        if (trimmed.startsWith(";") && (line.startsWith(" ") || line.startsWith("\t") || line.length() < 80))
            return true;

        // Etiquetas (Etiqueta:) - No deben tener espacios y deben terminar en :
        if (trimmed.endsWith(":") && !trimmed.contains(" ") && trimmed.length() > 1 && !trimmed.contains("PASO")
                && !trimmed.contains("STEP"))
            return true;

        String u = line.toUpperCase();
        return u.contains(" LIST ") || u.contains("ORG ") || u.contains("GOTO ") ||
                u.contains("BANKSEL ") || u.contains("MOVLW ") || u.contains("MOVWF ") ||
                u.contains("BSF ") || u.contains("BCF ") || u.contains("CALL ") ||
                u.contains("DECFSZ ") || u.contains("RETURN") || u.contains(" END") || u.equals("END") ||
                u.contains("__CONFIG") || u.contains("CBLOCK") || u.contains("ENDC") ||
                u.contains("#INCLUDE") || u.contains(" ENDC") || u.contains(" END ");
    }

    private void addTitle(String text) {
        TextView titleView = new TextView(context);
        titleView.setText(text);
        titleView.setTextIsSelectable(true);
        titleView.setTextSize(20);
        titleView.setTextColor(Color.parseColor("#1A73E8")); // Azul Google
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.START);
        titleView.setPadding(0, dpToPx(32), 0, dpToPx(8)); // Más espacio arriba
        container.addView(titleView);
    }

    private void addNormalText(String text) {
        TextView textView = new TextView(context);
        String trimmed = text.trim();

        // Estilo especial para parámetros (•) - EXCLUIR emojis informativos
        boolean isInfoNote = trimmed.startsWith("ℹ️") || trimmed.startsWith("📋") ||
                trimmed.startsWith("📝") || trimmed.startsWith("⚠️") ||
                trimmed.startsWith("✨") || trimmed.startsWith("💡") ||
                trimmed.contains("Notas Importantes") || trimmed.contains("Important Notes");

        if (!isInfoNote && (trimmed.startsWith("•") || (trimmed.contains(":") && trimmed.length() < 100
                && (trimmed.startsWith("-") || trimmed.startsWith("*"))))) {
            SpannableString ss = new SpannableString(text);
            int colonIndex = text.indexOf(":");
            if (colonIndex != -1) {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#1A73E8")), 0, colonIndex + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, colonIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#1A73E8")), 0, text.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(ss);
        } else {
            textView.setText(text);
        }

        textView.setTextSize(16);
        textView.setTextColor(isInfoNote ? Color.parseColor("#444444") : Color.BLACK);
        textView.setTextIsSelectable(true);
        textView.setAutoLinkMask(Linkify.WEB_URLS);
        textView.setLinksClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setLinkTextColor(Color.parseColor("#1A73E8"));

        textView.setGravity(Gravity.START);
        textView.setPadding(0, dpToPx(8), 0, dpToPx(8)); // Más padding para evitar mezcla
        container.addView(textView);
    }

    private void addCommandBlock(String command) {
        String label = lang.equals("es") ? "💻 Comando" : "💻 Command";
        addGenericCodeBlock(command, "#1E1E1E", Color.GREEN, label, null);
    }

    private void addAssemblyCodeBlock(String code) {
        String label = lang.equals("es") ? "📄 Código ASM" : "📄 ASM Code";
        addGenericCodeBlock(code, "#121212", Color.WHITE, label, highlightAssemblySyntax(code));
    }

    private void addGenericCodeBlock(final String code, String bgColor, int textColor, String labelText,
            CharSequence coloredContent) {
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(Color.parseColor(bgColor));
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
        blockLayout.setLayoutParams(params);

        // Header con botón - Mejorado con RelativeLayout para evitar solapamiento
        RelativeLayout header = new RelativeLayout(context);
        header.setPadding(0, 0, 0, dpToPx(8));

        TextView label = new TextView(context);
        label.setText(labelText);
        label.setTextColor(Color.GRAY);
        label.setTextSize(11);
        label.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        RelativeLayout.LayoutParams labelParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        labelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        labelParams.addRule(RelativeLayout.CENTER_VERTICAL);
        header.addView(label, labelParams);

        Button copyBtn = new Button(context);
        copyBtn.setText(lang.equals("es") ? "COPIAR" : "COPY");
        copyBtn.setTextSize(10);
        copyBtn.setBackgroundColor(Color.parseColor("#2E7D32"));
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setOnClickListener(v -> copyToClipboard(code));

        RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(dpToPx(80), dpToPx(36));
        btnParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        header.addView(copyBtn, btnParams);

        blockLayout.addView(header);

        // Contenido de código
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        TextView codeView = new TextView(context);
        if (coloredContent != null) {
            codeView.setText(coloredContent);
        } else {
            codeView.setText(code);
            codeView.setTextColor(textColor);
        }
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextSize(13);
        codeView.setTextIsSelectable(true);
        scroll.addView(codeView);

        blockLayout.addView(scroll);
        container.addView(blockLayout);
    }

    private CharSequence highlightAssemblySyntax(String code) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(code);

        // Colores de sintaxis
        int colorComment = Color.parseColor("#808080"); // Gris
        int colorLabel = Color.parseColor("#DCDCAA"); // Amarillo claro
        int colorKeyword = Color.parseColor("#C586C0"); // Púrpura
        int colorNumber = Color.parseColor("#B5CEA8"); // Verde claro
        int colorDirective = Color.parseColor("#569CD6"); // Azul

        // 1. Comentarios (;)
        Matcher mComments = Pattern.compile(";.*").matcher(code);
        while (mComments.find()) {
            ssb.setSpan(new ForegroundColorSpan(colorComment), mComments.start(), mComments.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 2. Etiquetas (Etiqueta:) al inicio de línea
        Matcher mLabels = Pattern.compile("(?m)^[a-zA-Z_][a-zA-Z0-9_]*:").matcher(code);
        while (mLabels.find()) {
            ssb.setSpan(new ForegroundColorSpan(colorLabel), mLabels.start(), mLabels.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), mLabels.start(), mLabels.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 3. Instrucciones comunes
        String[] keywords = { "movlw", "movwf", "goto", "call", "return", "bsf", "bcf", "decfsz", "banksel", "clrf",
                "andlw", "iorlw", "sublw", "xorlw", "addlw" };
        for (String kw : keywords) {
            Matcher m = Pattern.compile("(?i)\\b" + kw + "\\b").matcher(code);
            while (m.find()) {
                ssb.setSpan(new ForegroundColorSpan(colorKeyword), m.start(), m.end(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // 4. Directivas
        String[] directives = { "LIST", "ORG", "END", "CBLOCK", "ENDC", "#include", "__CONFIG" };
        for (String dir : directives) {
            Matcher m = Pattern.compile("(?i)\\b" + dir.replace("#", "#") + "\\b").matcher(code);
            while (m.find()) {
                ssb.setSpan(new ForegroundColorSpan(colorDirective), m.start(), m.end(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // 5. Números (h'FF', b'0101', 0xXX)
        Matcher mNumbers = Pattern.compile("(?i)(0x[0-9A-F]+|[hb]'[01A-F]+'|[0-9]+)").matcher(code);
        while (mNumbers.find()) {
            ssb.setSpan(new ForegroundColorSpan(colorNumber), mNumbers.start(), mNumbers.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ssb;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("tutorial_code", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, lang.equals("es") ? "Copiado al portapapeles" : "Copied to clipboard",
                Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
