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
    private boolean isMarkdownEnabled = false;

    // Colores del tema Legacy (GPUTILS)
    private static final int COLOR_TITLE_LEGACY = Color.parseColor("#2196F3");
    private static final int COLOR_SUBTITLE_LEGACY = Color.parseColor("#424242");

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

    public void setMarkdownEnabled(boolean enabled) {
        this.isMarkdownEnabled = enabled;
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
            if (isMarkdownEnabled) {
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
                    addSmallHeader(trimmedLine.substring(5).trim());
                    i++;
                    continue;
                }

                if (trimmedLine.equals("---") || trimmedLine.equals("***")) {
                    addSeparator();
                    i++;
                    continue;
                }

                // Detectar Tablas Markdown (| Celda | Celda |)
                if (trimmedLine.startsWith("|") && i + 1 < lines.length
                        && (lines[i + 1].trim().contains("|-") || lines[i + 1].trim().contains("|"))) {
                    java.util.List<String> tableLines = new java.util.ArrayList<>();
                    while (i < lines.length && lines[i].trim().startsWith("|")) {
                        tableLines.add(lines[i].trim());
                        i++;
                    }
                    if (tableLines.size() > 1) {
                        addTable(tableLines);
                        continue;
                    }
                }
            }

            // Lógica diferenciada para GPUTILS (Legacy)
            if (!isMarkdownEnabled) {
                // Agrupar bloques de comandos o ensamblador en modo Legacy
                if (isCommandLine(line) || isAssemblyCode(line)) {
                    StringBuilder block = new StringBuilder();
                    boolean isAsm = isAssemblyCode(line);
                    while (i < lines.length && (isCommandLine(lines[i]) || isAssemblyCode(lines[i])
                            || (lines[i].trim().startsWith(";") || lines[i].startsWith("    ")))) {
                        block.append(lines[i]).append("\n");
                        i++;
                    }
                    if (isAsm) {
                        addAssemblyCodeBlock(block.toString().trim());
                    } else {
                        addCommandBlock(block.toString().trim());
                    }
                    continue;
                }

                if (line.contains("PASO") || line.contains("STEP") ||
                        line.contains("📋") || line.contains("ℹ️") || line.contains("⚠️") ||
                        line.contains("✨") || line.contains("📝") || line.contains("📂") ||
                        line.contains("⏱️") || line.contains("💡") || line.contains("💾") ||
                        line.contains("🔨")) {
                    if (line.length() < 60) {
                        addTitle(line.trim(), 18, true);
                    } else {
                        addNormalText(line.trim());
                    }
                    i++;
                    continue;
                }
            }

            // Texto normal (SDCC o GPUTILS)
            addNormalText(line);
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
        // Mantenemos esto por compatibilidad
        String trimmed = line.trim();
        if (trimmed.isEmpty())
            return false;
        return trimmed.startsWith(";") || trimmed.startsWith("LIST ") ||
                trimmed.startsWith("#include") || trimmed.startsWith("__CONFIG");
    }

    private void addTitle(String text, int textSize, boolean bold) {
        if (isMarkdownEnabled) {
            TextView titleView = new TextView(context);
            titleView.setText(processMarkdownSpans(text)); // [FIX]: Soporte para negritas en títulos
            titleView.setTextSize(textSize + 4);
            titleView.setTextColor(Color.BLACK);
            titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            titleView.setPadding(0, dpToPx(24), 0, dpToPx(8));
            container.addView(titleView);

            // Línea divisoria debajo del título estilo GitHub
            View divider = new View(context);
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(2));
            params.setMargins(0, 0, 0, dpToPx(16));
            divider.setLayoutParams(params);
            container.addView(divider);
        } else {
            // Estilo Original GPUTILS
            TextView titleView = new TextView(context);
            titleView.setText(text);
            titleView.setTextSize(textSize);
            titleView.setTextColor(COLOR_TITLE_LEGACY);
            if (bold) {
                titleView.setTypeface(null, Typeface.BOLD);
            }
            titleView.setPadding(0, dpToPx(16), 0, dpToPx(8));
            titleView.setGravity(Gravity.CENTER);
            container.addView(titleView);
        }
    }

    private void addSectionTitle(String text) {
        if (isMarkdownEnabled) {
            TextView sectionView = new TextView(context);
            sectionView.setText(processMarkdownSpans(text)); // [FIX]: Soporte para negritas en secciones
            sectionView.setTextSize(20);
            sectionView.setTextColor(Color.parseColor("#24292E"));
            sectionView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            sectionView.setPadding(0, dpToPx(20), 0, dpToPx(8));
            container.addView(sectionView);

            // Línea fina estilo GitHub
            View divider = new View(context);
            divider.setBackgroundColor(Color.parseColor("#E1E4E8"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
            params.setMargins(0, 0, 0, dpToPx(12));
            divider.setLayoutParams(params);
            container.addView(divider);
        } else {
            // Estilo Original GPUTILS
            TextView sectionView = new TextView(context);
            sectionView.setText(text);
            sectionView.setTextSize(18);
            sectionView.setTextColor(COLOR_TITLE_LEGACY);
            sectionView.setTypeface(null, Typeface.BOLD);
            sectionView.setPadding(0, dpToPx(16), 0, dpToPx(8));
            container.addView(sectionView);
        }
    }

    private void addSubtitle(String text) {
        if (isMarkdownEnabled) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(processMarkdownSpans(text)); // [FIX]: Soporte para negritas en subtítulos
            subtitleView.setTextSize(16);
            subtitleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            subtitleView.setTextColor(Color.parseColor("#24292E"));
            subtitleView.setPadding(0, dpToPx(12), 0, dpToPx(4));
            container.addView(subtitleView);
        } else {
            // Estilo Original GPUTILS
            TextView subtitleView = new TextView(context);
            subtitleView.setText(text);
            subtitleView.setTextSize(14);
            subtitleView.setTypeface(null, Typeface.BOLD);
            subtitleView.setTextColor(COLOR_SUBTITLE_LEGACY);
            subtitleView.setPadding(0, dpToPx(8), 0, dpToPx(4));
            container.addView(subtitleView);
        }
    }

    private void addSmallHeader(String text) {
        TextView headerView = new TextView(context);
        headerView.setText(processMarkdownSpans(text));
        headerView.setTextSize(14);
        headerView.setTypeface(Typeface.DEFAULT_BOLD);
        headerView.setTextColor(Color.parseColor("#444444"));
        headerView.setPadding(0, dpToPx(10), 0, dpToPx(2));
        container.addView(headerView);
    }

    private void addCommandBlock(String command) {
        // En modo Legacy (GPUTILS), NUNCA ocultar el botón de copiar.
        // Solo en Markdown (SDCC) ocultamos si parece un log extenso o listado de
        // archivos.
        String firstLine = command.split("\n")[0].trim().toLowerCase();
        boolean looksLikeFileListing = firstLine.startsWith("-rw") || firstLine.startsWith("drwx")
                || firstLine.startsWith("total ");

        boolean isLogContent = isMarkdownEnabled
                && ((command.contains("\n") && command.split("\n").length > 5 && !isCommandLine(command))
                        || looksLikeFileListing);

        // [FIX]: Si el comando contiene palabras clave de comandos, FORZAR que NO sea
        // log
        if (isCommandLine(command.split("\n")[0])) {
            isLogContent = false;
        }

        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blockParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        blockLayout.setLayoutParams(blockParams);

        // Header (Botón copiar)
        if (!isLogContent) {
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(0, 0, 0, dpToPx(8));

            TextView titleView = new TextView(context);
            titleView.setText("💻 " + (currentLanguage.equals("es") ? "Comando" : "Command"));
            titleView.setTextSize(11);
            titleView.setTextColor(Color.parseColor("#00FF00"));
            titleView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);
            titleView.setLayoutParams(titleParams);
            header.addView(titleView);

            Button copyBtn = new Button(context);
            copyBtn.setText(currentLanguage.equals("es") ? "Copiar" : "Copy");
            copyBtn.setTextSize(10);
            copyBtn.setTextColor(Color.WHITE);
            copyBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
            copyBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            copyBtn.setMinimumHeight(0);
            copyBtn.setMinimumWidth(0);
            copyBtn.setOnClickListener(v -> copyToClipboard(command));
            header.addView(copyBtn);

            blockLayout.addView(header);
        }

        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        TextView commandView = new TextView(context);
        commandView.setText(command);
        commandView.setTextColor(isLogContent ? Color.parseColor("#ABB2BF") : Color.parseColor("#00FF00"));
        commandView.setTypeface(Typeface.MONOSPACE);
        commandView.setTextSize(12);
        commandView.setTextIsSelectable(true);

        scrollView.addView(commandView);
        blockLayout.addView(scrollView);

        container.addView(blockLayout);
    }

    private void addAssemblyCodeBlock(String code) {
        renderCodeBlock(code, "ASM", Color.WHITE, Color.parseColor("#1E1E1E"));
    }

    private void addCCodeBlock(String code) {
        renderCodeBlock(code, "C", Color.WHITE, Color.parseColor("#1E1E1E"));
    }

    private void renderCodeBlock(String code, String languageLabel, int textColor, int bgColor) {
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(bgColor);
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blockParams.setMargins(0, dpToPx(12), 0, dpToPx(12));
        blockLayout.setLayoutParams(blockParams);

        // Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, 0, 0, dpToPx(8));

        TextView langView = new TextView(context);
        langView.setText(languageLabel);
        langView.setTextSize(10);
        langView.setTextColor(Color.parseColor("#C678DD"));
        langView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        LinearLayout.LayoutParams langParams = new LinearLayout.LayoutParams(0, -2, 1f);
        langView.setLayoutParams(langParams);
        header.addView(langView);

        Button copyBtn = new Button(context);
        copyBtn.setText(currentLanguage.equals("es") ? "Copiar" : "Copy");
        copyBtn.setTextSize(10);
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        copyBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        copyBtn.setMinimumHeight(0);
        copyBtn.setMinimumWidth(0);
        copyBtn.setOnClickListener(v -> copyToClipboard(code));
        header.addView(copyBtn);

        blockLayout.addView(header);

        HorizontalScrollView scrollView = new HorizontalScrollView(context);
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

        scrollView.addView(codeView);
        blockLayout.addView(scrollView);

        container.addView(blockLayout);
    }

    private void addTable(java.util.List<String> tableLines) {
        if (tableLines.size() < 2)
            return;

        android.widget.TableLayout tableLayout = new android.widget.TableLayout(context);
        tableLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        tableLayout.setPadding(0, dpToPx(8), 0, dpToPx(16));

        for (int rowIdx = 0; rowIdx < tableLines.size(); rowIdx++) {
            String line = tableLines.get(rowIdx);
            if (line.contains("|-"))
                continue;

            android.widget.TableRow tableRow = new android.widget.TableRow(context);
            String[] cells = line.split("\\|");

            for (String cell : cells) {
                String trimmed = cell.trim();
                if (trimmed.isEmpty() && cell.equals(cells[0]))
                    continue;
                if (trimmed.isEmpty() && cell.equals(cells[cells.length - 1]))
                    continue;

                TextView cellView = new TextView(context);
                cellView.setText(processMarkdownSpans(trimmed)); // [FIX]: Procesar negritas y código dentro de celdas
                cellView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
                cellView.setTextSize(12);
                cellView.setBackgroundResource(android.R.drawable.edit_text); // Borde simple
                cellView.setMovementMethod(LinkMovementMethod.getInstance()); // Permitir enlaces si los hay

                if (rowIdx == 0) {
                    cellView.setTypeface(null, Typeface.BOLD);
                    cellView.setTextColor(Color.WHITE);
                    cellView.setBackgroundColor(Color.parseColor("#24292E"));
                } else {
                    cellView.setTextColor(Color.BLACK);
                }
                tableRow.addView(cellView);
            }
            tableLayout.addView(tableRow);
        }

        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.addView(tableLayout);
        container.addView(hsv);
    }

    private SpannableString highlightAssemblySyntax(String code) {
        SpannableString spannable = new SpannableString(code);

        // Colores One Dark Pro
        int keywordColor = Color.parseColor("#C678DD");
        int commentColor = Color.parseColor("#5C6370");

        // Comentarios (;)
        Pattern commentPattern = Pattern.compile(";.*");
        Matcher cm = commentPattern.matcher(code);
        while (cm.find()) {
            spannable.setSpan(new ForegroundColorSpan(commentColor),
                    cm.start(), cm.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Instrucciones
        String[] commonInstr = { "movlw", "movwf", "goto", "call", "return", "bsf", "bcf", "decfsz", "banksel",
                "clrf" };
        for (String instr : commonInstr) {
            Pattern p = Pattern.compile("\\b" + instr + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(code);
            while (m.find()) {
                spannable.setSpan(new ForegroundColorSpan(keywordColor),
                        m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannable;
    }

    private SpannableString highlightCSyntax(String code) {
        SpannableString spannable = new SpannableString(code);

        int keywordColor = Color.parseColor("#C678DD"); // Púrpura
        int stringColor = Color.parseColor("#98C379"); // Verde
        int commentColor = Color.parseColor("#5C6370"); // Gris
        int directiveColor = Color.parseColor("#D19A66"); // Naranja

        // Comentarios bloque
        Pattern commentBlock = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        Matcher mBlock = commentBlock.matcher(code);
        while (mBlock.find()) {
            spannable.setSpan(new ForegroundColorSpan(commentColor),
                    mBlock.start(), mBlock.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Comentarios línea
        Pattern commentLine = Pattern.compile("//.*");
        Matcher mLine = commentLine.matcher(code);
        while (mLine.find()) {
            spannable.setSpan(new ForegroundColorSpan(commentColor),
                    mLine.start(), mLine.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Directivas
        Pattern directive = Pattern.compile("^\\s*#\\w+", Pattern.MULTILINE);
        Matcher mDir = directive.matcher(code);
        while (mDir.find()) {
            spannable.setSpan(new ForegroundColorSpan(directiveColor),
                    mDir.start(), mDir.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Keywords
        String[] keywords = { "void", "main", "while", "for", "if", "return", "int", "uint16_t", "uint8_t", "volatile",
                "__code", "__at" };
        for (String kw : keywords) {
            Pattern p = Pattern.compile("\\b" + kw + "\\b");
            Matcher m = p.matcher(code);
            while (m.find()) {
                spannable.setSpan(new ForegroundColorSpan(keywordColor),
                        m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // Cadenas
        Pattern strings = Pattern.compile("\".*?\"");
        Matcher mStr = strings.matcher(code);
        while (mStr.find()) {
            spannable.setSpan(new ForegroundColorSpan(stringColor),
                    mStr.start(), mStr.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    private void addNormalText(String text) {
        TextView textView = new TextView(context);
        textView.setTextSize(14);
        textView.setTextColor(isMarkdownEnabled ? Color.parseColor("#24292E") : COLOR_SUBTITLE_LEGACY);
        textView.setLineSpacing(0, 1.3f);
        textView.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        String processedText = text;
        if (isMarkdownEnabled) {
            String temp = text.trim();
            if (temp.startsWith("- ") || temp.startsWith("* ")) {
                processedText = "  • " + temp.substring(2);
            } else if (temp.matches("^\\d+\\.\\s.*")) {
                processedText = "  " + temp;
            }
        } else {
            // [FIX]: Mejorar distición de parámetros en modo Legacy (GPUTILS)
            if (text.trim().startsWith("•")) {
                textView.setTextColor(Color.parseColor("#1A73E8")); // Azul suave para parámetros
                textView.setTypeface(null, Typeface.BOLD);
            }
        }

        textView.setText(processMarkdownSpans(processedText));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setTextIsSelectable(true);
        container.addView(textView);
    }

    /**
     * Motor unificado para procesar Markdown (Negritas, Código en línea, Enlaces)
     * Funciona tanto para texto normal como para celdas de tabla.
     */
    private android.text.SpannableStringBuilder processMarkdownSpans(String text) {
        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();

        if (!isMarkdownEnabled) {
            ssb.append(text);
            return ssb;
        }

        // 1. Procesar Negritas **texto** o ***texto***
        Pattern boldPattern = Pattern.compile("\\*{2,3}(.*?)\\*{2,3}");
        Matcher mBold = boldPattern.matcher(text);
        int lastPos = 0;
        while (mBold.find()) {
            ssb.append(text.substring(lastPos, mBold.start()));
            int start = ssb.length();
            ssb.append(mBold.group(1));
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            lastPos = mBold.end();
        }
        ssb.append(text.substring(lastPos));

        // 2. Procesar Código en línea (Inline Code) con fondo gris
        String tempText = ssb.toString();
        ssb.clear();
        Pattern codePattern = Pattern.compile("(`|')(.*?)(\\1)");
        Matcher mCode = codePattern.matcher(tempText);
        lastPos = 0;
        while (mCode.find()) {
            ssb.append(tempText.substring(lastPos, mCode.start()));
            int start = ssb.length();
            String content = mCode.group(2);
            ssb.append(content);

            ssb.setSpan(new TypefaceSpan("monospace"), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#E4405F")), start, ssb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new android.text.style.BackgroundColorSpan(Color.parseColor("#F3F3F3")), start, ssb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            lastPos = mCode.end();
        }
        ssb.append(tempText.substring(lastPos));

        // 3. Enlaces clickeables
        String currentText = ssb.toString();
        Pattern urlPattern = Pattern.compile("(https?://[^\\s\\)\\]\\*\\,]+)");
        Matcher matcher = urlPattern.matcher(currentText);

        while (matcher.find()) {
            final String url = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#2196F3"));
                    ds.setUnderlineText(true);
                }
            };
            ssb.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ssb;
    }

    private void addClickableLink(String text) {
        // Redirigir a addNormalText para que use el mismo motor de parseo unificado
        addNormalText(text);
    }

    // Clase auxiliar para Monoespacio en API antiguas
    private static class TypefaceSpan extends android.text.style.MetricAffectingSpan {
        private final Typeface typeface;

        public TypefaceSpan(String family) {
            this.typeface = Typeface.create(family, Typeface.NORMAL);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setTypeface(typeface);
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            paint.setTypeface(typeface);
        }
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