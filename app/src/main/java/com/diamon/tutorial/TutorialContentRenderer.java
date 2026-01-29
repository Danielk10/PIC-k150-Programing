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
 * Clase que renderiza el contenido del tutorial en formato Markdown (.md)
 * Optimizada para el tutorial de SDCC.
 */
public class TutorialContentRenderer {

    private Context context;
    private LinearLayout container;
    private String currentLanguage = "es";

    public TutorialContentRenderer(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
    }

    // Método mantenido por compatibilidad de firma si se requiere, pero ignorado
    // internamente
    public void setMarkdownEnabled(boolean enabled) {
        // Siempre habilitado en esta clase
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
                } else {
                    addCommandBlock(code);
                }

                if (i < lines.length)
                    i++; // Saltar la línea del marcador final
                continue;
            }

            // Detectar encabezados Markdown
            if (trimmedLine.startsWith("# ")) {
                addTitle(trimmedLine.substring(2).trim(), 20);
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

            // Texto normal
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
                trimmed.startsWith("adb ") || trimmed.startsWith("sdcc ") ||
                trimmed.startsWith("termux-setup-storage");
    }

    private void addTitle(String text, int textSize) {
        TextView titleView = new TextView(context);
        titleView.setText(processMarkdownSpans(text));
        titleView.setTextSize(textSize + 4);
        titleView.setTextColor(Color.BLACK);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleView.setPadding(0, dpToPx(24), 0, dpToPx(8));
        container.addView(titleView);

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
        sectionView.setText(processMarkdownSpans(text));
        sectionView.setTextSize(20);
        sectionView.setTextColor(Color.parseColor("#24292E"));
        sectionView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sectionView.setPadding(0, dpToPx(20), 0, dpToPx(8));
        container.addView(sectionView);

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
        subtitleView.setText(processMarkdownSpans(text));
        subtitleView.setTextSize(16);
        subtitleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        subtitleView.setTextColor(Color.parseColor("#24292E"));
        subtitleView.setPadding(0, dpToPx(12), 0, dpToPx(4));
        container.addView(subtitleView);
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
        String[] lines = command.split("\n");
        String firstLine = lines[0].trim();

        boolean isLogContent = firstLine.startsWith("SDCC :") ||
                firstLine.startsWith("gpasm-") || firstLine.startsWith("gplink-") ||
                firstLine.startsWith("gplib-") ||
                firstLine.contains("published under GNU") ||
                command.contains("warning:") || command.contains("error:") ||
                command.contains("Message[") ||
                (firstLine.startsWith("-") && !isCommandLine(firstLine)) ||
                (firstLine.startsWith("#") && lines.length > 2) ||
                firstLine.startsWith("-rw") || firstLine.startsWith("drwx") || firstLine.startsWith("total ");

        if (!isLogContent && lines.length > 5 && !isCommandLine(firstLine)) {
            isLogContent = true;
        }

        if (isCommandLine(firstLine)) {
            isLogContent = false;
        }

        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
        blockLayout.setLayoutParams(params);

        if (!isLogContent) {
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(0, 0, 0, dpToPx(8));

            TextView titleView = new TextView(context);
            titleView.setText("💻 " + (currentLanguage.equals("es") ? "Comando" : "Command"));
            titleView.setTextSize(11);
            titleView.setTextColor(Color.GREEN);
            titleView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            header.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1f));

            Button copyBtn = new Button(context);
            copyBtn.setText(currentLanguage.equals("es") ? "Copiar" : "Copy");
            copyBtn.setTextSize(10);
            copyBtn.setTextColor(Color.WHITE);
            copyBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
            copyBtn.setOnClickListener(v -> copyToClipboard(command));
            header.addView(copyBtn, new LinearLayout.LayoutParams(dpToPx(80), dpToPx(36)));

            blockLayout.addView(header);
        }

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        TextView tv = new TextView(context);
        tv.setText(command);
        tv.setTextColor(isLogContent ? Color.parseColor("#ABB2BF") : Color.GREEN);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(12);
        scroll.addView(tv);
        blockLayout.addView(scroll);
        container.addView(blockLayout);
    }

    private void addAssemblyCodeBlock(String code) {
        renderCodeBlock(code, "ASM", highlightAssemblySyntax(code));
    }

    private void addCCodeBlock(String code) {
        renderCodeBlock(code, "C", highlightCSyntax(code));
    }

    private void renderCodeBlock(String code, String label, android.text.SpannableStringBuilder coloredCode) {
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dpToPx(12), 0, dpToPx(12));
        blockLayout.setLayoutParams(params);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, 0, 0, dpToPx(8));

        TextView langView = new TextView(context);
        langView.setText(label);
        langView.setTextSize(10);
        langView.setTextColor(Color.parseColor("#C678DD"));
        langView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        header.addView(langView, new LinearLayout.LayoutParams(0, -2, 1f));

        Button copyBtn = new Button(context);
        copyBtn.setText(currentLanguage.equals("es") ? "Copiar" : "Copy");
        copyBtn.setTextSize(10);
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        copyBtn.setOnClickListener(v -> copyToClipboard(code));
        header.addView(copyBtn, new LinearLayout.LayoutParams(dpToPx(80), dpToPx(36)));

        blockLayout.addView(header);

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        TextView tv = new TextView(context);
        tv.setText(coloredCode);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12);
        scroll.addView(tv);
        blockLayout.addView(scroll);
        container.addView(blockLayout);
    }

    private void addTable(java.util.List<String> tableLines) {
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
                if (trimmed.isEmpty() && (cell.equals(cells[0]) || cell.equals(cells[cells.length - 1])))
                    continue;

                TextView tv = new TextView(context);
                tv.setText(processMarkdownSpans(trimmed));
                tv.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
                tv.setTextSize(12);
                tv.setBackgroundResource(android.R.drawable.edit_text);
                tv.setMovementMethod(LinkMovementMethod.getInstance());

                if (rowIdx == 0) {
                    tv.setTypeface(null, Typeface.BOLD);
                    tv.setTextColor(Color.WHITE);
                    tv.setBackgroundColor(Color.parseColor("#24292E"));
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                tableRow.addView(tv);
            }
            tableLayout.addView(tableRow);
        }

        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.addView(tableLayout);
        container.addView(hsv);
    }

    private void addNormalText(String text) {
        TextView tv = new TextView(context);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#24292E"));
        tv.setLineSpacing(0, 1.3f);
        tv.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        String processedText = text;
        String temp = text.trim();
        if (temp.startsWith("- ") || temp.startsWith("* ")) {
            processedText = "  • " + temp.substring(2);
        } else if (temp.matches("^\\d+\\.\\s.*")) {
            processedText = "  " + temp;
        }

        tv.setText(processMarkdownSpans(processedText));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setTextIsSelectable(true);
        container.addView(tv);
    }

    private android.text.SpannableStringBuilder processMarkdownSpans(String text) {
        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
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

        String tempText = ssb.toString();
        ssb.clear();
        Pattern codePattern = Pattern.compile("(`|')(.*?)(\\1)");
        Matcher mCode = codePattern.matcher(tempText);
        lastPos = 0;
        while (mCode.find()) {
            ssb.append(tempText.substring(lastPos, mCode.start()));
            int start = ssb.length();
            ssb.append(mCode.group(2));
            ssb.setSpan(new TypefaceSpan("monospace"), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#E4405F")), start, ssb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new android.text.style.BackgroundColorSpan(Color.parseColor("#F3F3F3")), start, ssb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            lastPos = mCode.end();
        }
        ssb.append(tempText.substring(lastPos));

        String currentText = ssb.toString();
        Pattern urlPattern = Pattern.compile("(https?://[^\\s\\)\\]\\*\\,]+)");
        Matcher matcher = urlPattern.matcher(currentText);
        while (matcher.find()) {
            final String url = matcher.group(1);
            ssb.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View w) {
                    context.startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#2196F3"));
                    ds.setUnderlineText(true);
                }
            }, matcher.start(1), matcher.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }

    private android.text.SpannableStringBuilder highlightAssemblySyntax(String code) {
        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder(code);
        int commentColor = Color.parseColor("#5C6370");
        int keywordColor = Color.parseColor("#C678DD");

        Matcher cm = Pattern.compile(";.*").matcher(code);
        while (cm.find())
            ssb.setSpan(new ForegroundColorSpan(commentColor), cm.start(), cm.end(), 0);

        String[] keywords = { "movlw", "movwf", "goto", "call", "return", "bsf", "bcf", "decfsz", "banksel", "clrf" };
        for (String kw : keywords) {
            Matcher m = Pattern.compile("\\b" + kw + "\\b", Pattern.CASE_INSENSITIVE).matcher(code);
            while (m.find())
                ssb.setSpan(new ForegroundColorSpan(keywordColor), m.start(), m.end(), 0);
        }
        return ssb;
    }

    private android.text.SpannableStringBuilder highlightCSyntax(String code) {
        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder(code);
        int keywordColor = Color.parseColor("#C678DD");
        int commentColor = Color.parseColor("#5C6370");
        int stringColor = Color.parseColor("#98C379");

        Matcher mBlock = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(code);
        while (mBlock.find())
            ssb.setSpan(new ForegroundColorSpan(commentColor), mBlock.start(), mBlock.end(), 0);

        Matcher mLine = Pattern.compile("//.*").matcher(code);
        while (mLine.find())
            ssb.setSpan(new ForegroundColorSpan(commentColor), mLine.start(), mLine.end(), 0);

        String[] keywords = { "void", "main", "while", "for", "if", "return", "int", "uint16_t", "uint8_t", "volatile",
                "__code", "__at" };
        for (String kw : keywords) {
            Matcher m = Pattern.compile("\\b" + kw + "\\b").matcher(code);
            while (m.find())
                ssb.setSpan(new ForegroundColorSpan(keywordColor), m.start(), m.end(), 0);
        }

        Matcher mStr = Pattern.compile("\".*?\"").matcher(code);
        while (mStr.find())
            ssb.setSpan(new ForegroundColorSpan(stringColor), mStr.start(), mStr.end(), 0);

        return ssb;
    }

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
        container.addView(spacer, new LinearLayout.LayoutParams(-1, dpToPx(heightDp)));
    }

    private void copyToClipboard(String text) {
        ((android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(android.content.ClipData.newPlainText("code", text));
        Toast.makeText(context, currentLanguage.equals("es") ? "Copiado" : "Copied", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}