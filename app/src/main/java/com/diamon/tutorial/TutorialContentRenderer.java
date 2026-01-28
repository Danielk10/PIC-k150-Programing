package com.diamon.tutorial;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase que renderiza el contenido del tutorial con formato profesional
 * Incluye: comandos en cajas negras, código ensamblador numerado, enlaces
 * clickeables
 */
public class TutorialContentRenderer {

    private Context context;
    private LinearLayout container;
    private String currentLanguage = "es";

    // Colores del tema
    private static final int COLOR_COMMAND_BG = Color.parseColor("#1E1E1E");
    private static final int COLOR_COMMAND_TEXT = Color.parseColor("#00FF00");
    private static final int COLOR_CODE_BG = Color.parseColor("#282C34");
    private static final int COLOR_CODE_TEXT = Color.parseColor("#ABB2BF");
    private static final int COLOR_LINE_NUMBER = Color.parseColor("#5C6370");
    private static final int COLOR_TITLE = Color.parseColor("#2196F3");
    private static final int COLOR_LINK = Color.parseColor("#2196F3");
    private static final int COLOR_COMMENT = Color.parseColor("#5C6370");
    private static final int COLOR_KEYWORD = Color.parseColor("#C678DD");

    public TutorialContentRenderer(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
    }

    public void renderTutorial(String tutorialText) {
        container.removeAllViews();

        String[] lines = tutorialText.split("\\n");
        int i = 0;

        while (i < lines.length) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                addSpacer(8);
                i++;
                continue;
            }

            // Detectar bloques de código Markdown (```)
            if (trimmedLine.startsWith("```")) {
                String language = trimmedLine.substring(3).trim().toLowerCase();
                StringBuilder codeBlock = new StringBuilder();
                i++; // Saltar la línea del marcador inicial

                while (i < lines.length && !lines[i].trim().equals("```")) {
                    codeBlock.append(lines[i]).append("\n");
                    i++;
                }

                String code = codeBlock.toString().trim();
                if (language.equals("asm") || language.equals("assembly")) {
                    addAssemblyCodeBlock(code);
                } else if (language.equals("c") || language.equals("cpp")) {
                    addCCodeBlock(code);
                } else if (language.equals("bash") || language.equals("shell") || language.isEmpty()) {
                    addCommandBlock(code);
                } else {
                    addCommandBlock(code); // Fallback
                }

                if (i < lines.length)
                    i++; // Saltar la línea del marcador final
                continue;
            }

            // Detectar encabezados Markdown
            if (trimmedLine.startsWith("# ")) {
                addTitle(trimmedLine.substring(2).trim(), 20, true);
                i++;
                continue;
            } else if (trimmedLine.startsWith("## ")) {
                addSectionTitle(trimmedLine.substring(3).trim());
                i++;
                continue;
            } else if (trimmedLine.startsWith("### ")) {
                addSubtitle(trimmedLine.substring(4).trim());
                i++;
                continue;
            } else if (trimmedLine.startsWith("#### ")) {
                addSubtitle(trimmedLine.substring(5).trim());
                i++;
                continue;
            }

            // Detectar líneas horizontales
            if (trimmedLine.equals("---") || trimmedLine.equals("***")) {
                addSeparator();
                i++;
                continue;
            }

            // Detectar listas
            if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.matches("^\\d+\\.\\s.*")) {
                addNormalText(line); // Mantener la indentación original si es posible
                i++;
                continue;
            }

            // Detectar referencias (contiene URLs)
            if (line.contains("http://") || line.contains("https://")) {
                addClickableLink(line.trim());
                i++;
                continue;
            }

            // Texto normal (fallback para patrones antiguos que no son estrictamente MD
            // pero el usuario los usa)
            if (line.contains("PASO") || line.contains("STEP") ||
                    line.contains("📋") || line.contains("ℹ️") || line.contains("⚠️") ||
                    line.contains("✨") || line.contains("📝") || line.contains("📂") ||
                    line.contains("⏱️") || line.contains("💡") || line.contains("💾") ||
                    line.contains("🔨")) {

                if (line.length() < 50) {
                    addSubtitle(line.trim());
                } else {
                    addNormalText(line.trim());
                }
                i++;
                continue;
            }

            // Texto normal
            addNormalText(line.trim());
            i++;
        }
    }

    private void addSeparator() {
        View separator = new View(context);
        separator.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        params.setMargins(0, dpToPx(16), 0, dpToPx(16));
        separator.setLayoutParams(params);
        container.addView(separator);
    }

    private boolean isCommandLine(String line) {
        // Mantenemos esto por compatibilidad si se llama desde otro lado,
        // pero renderTutorial ya no lo usa directamente para bloques.
        String trimmed = line.trim();
        return trimmed.startsWith("pkg ") || trimmed.startsWith("wget ") ||
                trimmed.startsWith("tar ") || trimmed.startsWith("cd ") ||
                trimmed.startsWith("./configure") || trimmed.startsWith("make") ||
                trimmed.startsWith("nano ") || trimmed.startsWith("gpasm ") ||
                trimmed.startsWith("gplink ") || trimmed.startsWith("gplib ") ||
                trimmed.startsWith("ls ") || trimmed.startsWith("cp ") ||
                trimmed.startsWith("chmod ") || trimmed.startsWith("export ") ||
                trimmed.startsWith("echo ") || trimmed.startsWith("cat ") ||
                trimmed.startsWith("termux-setup-storage");
    }

    private boolean isAssemblyCode(String line) {
        // Mantenemos esto por compatibilidad
        String trimmed = line.trim();
        if (trimmed.isEmpty())
            return false;
        return trimmed.startsWith(";") || trimmed.startsWith("LIST ") ||
                trimmed.startsWith("#include") || trimmed.startsWith("__CONFIG");
    }

    private void addTitle(String text, int textSize, boolean bold) {
        TextView titleView = new TextView(context);
        titleView.setText(text);
        titleView.setTextSize(textSize + 4);
        titleView.setTextColor(Color.BLACK);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleView.setPadding(0, dpToPx(24), 0, dpToPx(8));

        container.addView(titleView);

        // Línea divisoria debajo del título principal
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(2));
        params.setMargins(0, 0, 0, dpToPx(16));
        divider.setLayoutParams(params);
        container.addView(divider);
    }

    private void addSectionTitle(String text) {
        TextView sectionView = new TextView(context);
        sectionView.setText(text);
        sectionView.setTextSize(20);
        sectionView.setTextColor(Color.parseColor("#24292E"));
        sectionView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sectionView.setPadding(0, dpToPx(20), 0, dpToPx(8));

        container.addView(sectionView);

        // Línea fina debajo de secciones (estilo GitHub)
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#E1E4E8"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        params.setMargins(0, 0, 0, dpToPx(12));
        divider.setLayoutParams(params);
        container.addView(divider);
    }

    private void addSubtitle(String text) {
        TextView subtitleView = new TextView(context);
        subtitleView.setText(text);
        subtitleView.setTextSize(16);
        subtitleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        subtitleView.setTextColor(Color.parseColor("#24292E"));
        subtitleView.setPadding(0, dpToPx(12), 0, dpToPx(4));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(8), 0, dpToPx(4));
        subtitleView.setLayoutParams(params);

        container.addView(subtitleView);
    }

    private void addCommandBlock(String command) {
        // Contenedor principal con estilo de caja de terminal de GitHub
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(Color.parseColor("#F6F8FA")); // Gris muy claro
        blockLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Bordes redondeados simulados
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor("#F6F8FA"));
        gd.setCornerRadius(dpToPx(6));
        blockLayout.setBackground(gd);

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blockParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        blockLayout.setLayoutParams(blockParams);

        // Texto del comando con scroll horizontal
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);

        TextView commandView = new TextView(context);
        commandView.setText(command);
        commandView.setTextColor(Color.parseColor("#24292E"));
        commandView.setTypeface(Typeface.MONOSPACE);
        commandView.setTextSize(13);
        commandView.setTextIsSelectable(true);

        scrollView.addView(commandView);
        blockLayout.addView(scrollView);

        // Botón flotante de copia discreto a la derecha si es posible,
        // o simplemente añadirlo al final de la caja
        Button copyBtn = new Button(context);
        copyBtn.setText(currentLanguage.equals("es") ? "Copiar" : "Copy");
        copyBtn.setTextSize(10);
        copyBtn.setAllCaps(false);
        copyBtn.setTextColor(Color.parseColor("#0366D6"));
        copyBtn.setBackgroundColor(Color.TRANSPARENT);
        copyBtn.setPadding(0, dpToPx(4), 0, 0);
        copyBtn.setGravity(Gravity.END);

        copyBtn.setOnClickListener(v -> copyToClipboard(command));
        blockLayout.addView(copyBtn);

        container.addView(blockLayout);
    }

    private void addAssemblyCodeBlock(String code) {
        renderCodeBlock(code, "ASM", Color.parseColor("#24292E"), Color.parseColor("#F6F8FA"));
    }

    private void addCCodeBlock(String code) {
        renderCodeBlock(code, "C", Color.parseColor("#24292E"), Color.parseColor("#F6F8FA"));
    }

    private void renderCodeBlock(String code, String languageLabel, int textColor, int bgColor) {
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(dpToPx(6));
        gd.setStroke(dpToPx(1), Color.parseColor("#E1E4E8"));
        blockLayout.setBackground(gd);
        blockLayout.setPadding(0, 0, 0, 0);

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blockParams.setMargins(0, dpToPx(12), 0, dpToPx(12));
        blockLayout.setLayoutParams(blockParams);

        // Header de la caja de código
        LinearLayout header = new LinearLayout(context);
        header.setBackgroundColor(Color.parseColor("#F1F8FF"));
        header.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView langView = new TextView(context);
        langView.setText(languageLabel);
        langView.setTextSize(11);
        langView.setTextColor(Color.parseColor("#586069"));
        langView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        LinearLayout.LayoutParams langParams = new LinearLayout.LayoutParams(0, -2, 1f);
        langView.setLayoutParams(langParams);
        header.addView(langView);

        TextView copyLabel = new TextView(context);
        copyLabel.setText(currentLanguage.equals("es") ? "Copiar" : "Copy");
        copyLabel.setTextSize(11);
        copyLabel.setTextColor(Color.parseColor("#0366D6"));
        copyLabel.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        copyLabel.setOnClickListener(v -> copyToClipboard(code));
        header.addView(copyLabel);

        blockLayout.addView(header);

        // Contenido del código
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        TextView codeView = new TextView(context);
        if (languageLabel.equals("C")) {
            codeView.setText(highlightCSyntax(code));
        } else if (languageLabel.equals("ASM")) {
            codeView.setText(highlightAssemblySyntax(code));
        } else {
            codeView.setText(code);
        }
        codeView.setTextColor(textColor);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextSize(12);
        codeView.setLineSpacing(0, 1.2f);

        scrollView.addView(codeView);
        blockLayout.addView(scrollView);

        container.addView(blockLayout);
    }

    private SpannableString highlightAssemblySyntax(String code) {
        SpannableString spannable = new SpannableString(code);
        String[] lines = code.split("\n");
        int offset = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Comentarios (;)
            if (trimmedLine.contains(";")) {
                int commentStart = line.indexOf(";");
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#6A737D")),
                        offset + commentStart, offset + line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Instrucciones comunes
            String[] commonInstr = { "movlw", "movwf", "goto", "call", "return", "bsf", "bcf", "decfsz", "banksel",
                    "clrf" };
            for (String instr : commonInstr) {
                Pattern p = Pattern.compile("\\b" + instr + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(line);
                while (m.find()) {
                    spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#D73A49")),
                            offset + m.start(), offset + m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            offset += line.length() + 1;
        }
        return spannable;
    }

    private SpannableString highlightCSyntax(String code) {
        SpannableString spannable = new SpannableString(code);

        // Comentarios bloque
        Pattern commentBlock = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        Matcher mBlock = commentBlock.matcher(code);
        while (mBlock.find()) {
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#6A737D")),
                    mBlock.start(), mBlock.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Comentarios línea
        Pattern commentLine = Pattern.compile("//.*");
        Matcher mLine = commentLine.matcher(code);
        while (mLine.find()) {
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#6A737D")),
                    mLine.start(), mLine.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Directivas
        Pattern directive = Pattern.compile("^\\s*#\\w+", Pattern.MULTILINE);
        Matcher mDir = directive.matcher(code);
        while (mDir.find()) {
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#D73A49")),
                    mDir.start(), mDir.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Keywords
        String[] keywords = { "void", "main", "while", "for", "if", "return", "int", "uint16_t", "uint8_t", "volatile",
                "__code", "__at" };
        for (String kw : keywords) {
            Pattern p = Pattern.compile("\\b" + kw + "\\b");
            Matcher m = p.matcher(code);
            while (m.find()) {
                // Verificar que no sea parte de un comentario (aproximado)
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#D73A49")),
                        m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // Cadenas
        Pattern strings = Pattern.compile("\".*?\"");
        Matcher mStr = strings.matcher(code);
        while (mStr.find()) {
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#032F62")),
                    mStr.start(), mStr.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    private void addClickableLink(String text) {
        TextView linkView = new TextView(context);
        linkView.setTextSize(14);
        linkView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        linkView.setLineSpacing(0, 1.2f);

        // Extraer URL
        Pattern urlPattern = Pattern.compile("(https?://[^\\s\\)\\]]+)");
        Matcher matcher = urlPattern.matcher(text);

        SpannableString spannable = new SpannableString(text);

        while (matcher.find()) {
            final String url = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#0366D6"));
                    ds.setUnderlineText(true);
                }
            };

            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        linkView.setText(spannable);
        linkView.setMovementMethod(LinkMovementMethod.getInstance());

        container.addView(linkView);
    }

    private void addNormalText(String text) {
        TextView textView = new TextView(context);
        textView.setTextSize(14);
        textView.setTextColor(Color.parseColor("#24292E"));
        textView.setLineSpacing(0, 1.3f);
        textView.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        // Soporte básico para negrita y viñetas
        String processedText = text;
        if (text.startsWith("- ") || text.startsWith("* ")) {
            processedText = "  • " + text.substring(2);
        }

        SpannableString ss = new SpannableString(processedText);
        Pattern boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher mBold = boldPattern.matcher(processedText);
        while (mBold.find()) {
            ss.setSpan(new StyleSpan(Typeface.BOLD), mBold.start(), mBold.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Podríamos quitar las asteriscos aquí si quisiéramos ser más estrictos
        }

        textView.setText(ss);
        textView.setTextIsSelectable(true);

        container.addView(textView);
    }

    private void addSpacer(int heightDp) {
        View spacer = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(heightDp));
        spacer.setLayoutParams(params);
        container.addView(spacer);
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                .getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("code", text);
        clipboard.setPrimaryClip(clip);

        String message = currentLanguage.equals("es") ? "Copiado al portapapeles" : "Copied to clipboard";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}