package com.diamon.tutorial;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
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

public class LegacyTutorialRenderer {
    private final Context context;
    private final LinearLayout container;

    public LegacyTutorialRenderer(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
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
                StringBuilder block = new StringBuilder();
                boolean isAsm = isAssemblyCode(line);

                // Agrupar líneas consecutivas o líneas que pertenecen al contexto del bloque
                while (i < lines.length) {
                    String currentLine = lines[i];
                    String trimmedCurrent = currentLine.trim();

                    // Si estamos en un bloque de ASM, permitimos etiquetas, instrucciones,
                    // comentarios y sangrías
                    if (isAsm) {
                        if (trimmedCurrent.isEmpty() && i + 1 < lines.length && isAssemblyCode(lines[i + 1])) {
                            block.append("\n"); // Permitir una línea en blanco si sigue habiendo ensamblador
                            i++;
                            continue;
                        }
                        if (isAssemblyCode(currentLine) || currentLine.startsWith("    ")
                                || currentLine.startsWith("\t") || trimmedCurrent.startsWith(";")) {
                            block.append(currentLine).append("\n");
                            i++;
                            continue;
                        }
                        break; // Fin del bloque ASM
                    } else {
                        // Bloque de Comandos (Shell)
                        if (isCommandLine(currentLine)) {
                            block.append(currentLine).append("\n");
                            i++;
                            continue;
                        }
                        break; // Fin del bloque de Comandos
                    }
                }

                String finalBlock = block.toString().trim();
                if (!finalBlock.isEmpty()) {
                    if (isAsm) {
                        addAssemblyCodeBlock(finalBlock);
                    } else {
                        addCommandBlock(finalBlock);
                    }
                }
                i--; // Ajustar índice por el bucle interno
                continue;
            }

            // Detección de Títulos (Emojis o PASO X:)
            if (trimmedLine.contains("PASO") || trimmedLine.contains("STEP") ||
                    trimmedLine.contains("📋") || trimmedLine.contains("ℹ️") || trimmedLine.contains("⚠️") ||
                    trimmedLine.contains("✨") || trimmedLine.contains("📝") || trimmedLine.contains("📂") ||
                    trimmedLine.contains("⏱️") || trimmedLine.contains("💡") || trimmedLine.contains("💾") ||
                    trimmedLine.contains("🔨") || trimmedLine.contains("📦") || trimmedLine.contains("🔧") ||
                    trimmedLine.contains("🔍") || trimmedLine.contains("✅") || trimmedLine.contains("🚀")) {

                if (trimmedLine.length() < 70) {
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
        if (trimmed.startsWith(";") && (line.startsWith(" ") || line.startsWith("\t") || line.length() < 50))
            return true;

        // Etiquetas (Etiqueta:)
        if (trimmed.endsWith(":") && !trimmed.contains(" ") && trimmed.length() > 1)
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
        titleView.setTextSize(20);
        titleView.setTextColor(Color.parseColor("#1A73E8")); // Azul Google
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.START);
        titleView.setPadding(0, dpToPx(24), 0, dpToPx(8));
        container.addView(titleView);
    }

    private void addNormalText(String text) {
        TextView textView = new TextView(context);
        String trimmed = text.trim();

        // Estilo especial para parámetros (•) - EXCLUIR emojis informativos
        boolean isInfoNote = trimmed.startsWith("ℹ️") || trimmed.startsWith("📋") ||
                trimmed.startsWith("📝") || trimmed.startsWith("⚠️") ||
                trimmed.startsWith("✨") || trimmed.startsWith("💡");

        if (!isInfoNote && (trimmed.startsWith("•") || (trimmed.contains(":") && trimmed.length() < 100
                && (trimmed.startsWith("-") || trimmed.startsWith("*"))))) {
            SpannableString ss = new SpannableString(text);
            int colonIndex = text.indexOf(":");
            if (colonIndex != -1) {
                // Resaltar hasta los dos puntos
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
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.START);
        textView.setPadding(0, dpToPx(4), 0, dpToPx(4));
        container.addView(textView);
    }

    private void addCommandBlock(String command) {
        addGenericCodeBlock(command, "#1E1E1E", Color.GREEN, "COMPARTIR/COPIAR");
    }

    private void addAssemblyCodeBlock(String code) {
        addGenericCodeBlock(code, "#121212", Color.parseColor("#BB86FC"), "COPIAR CÓDIGO");
    }

    private void addGenericCodeBlock(final String code, String bgColor, int textColor, String btnText) {
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(Color.parseColor(bgColor));
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
        blockLayout.setLayoutParams(params);

        // Header con botón
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(context);
        label.setText("💻 Bloque Técnico");
        label.setTextColor(Color.GRAY);
        label.setTextSize(12);

        View spacer = new View(context);
        header.addView(label);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        Button copyBtn = new Button(context);
        copyBtn.setText("COPIAR");
        copyBtn.setTextSize(10);
        copyBtn.setBackgroundColor(Color.parseColor("#2E7D32"));
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setOnClickListener(v -> copyToClipboard(code));
        header.addView(copyBtn, new LinearLayout.LayoutParams(dpToPx(80), dpToPx(36)));

        blockLayout.addView(header);

        // Contenido de código
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        TextView codeView = new TextView(context);
        codeView.setText(code);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextColor(textColor);
        codeView.setTextSize(14);
        scroll.addView(codeView);

        blockLayout.addView(scroll);
        container.addView(blockLayout);
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("tutorial_code", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
