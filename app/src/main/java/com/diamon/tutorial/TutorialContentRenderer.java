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
 * Incluye: comandos en cajas negras, código ensamblador numerado, enlaces clickeables
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
            
            // Detectar título principal
            if (line.trim().matches("^[A-Z][A-Z0-9 ]+$") && line.trim().length() > 10) {
                addTitle(line.trim(), 20, true);
                i++;
                continue;
            }
            
            // Detectar secciones con emoji y PASO/STEP
            if (line.contains("PASO") || line.contains("STEP")) {
                addSectionTitle(line.trim());
                i++;
                continue;
            }
            
            // Detectar comandos de terminal (líneas que empiezan con comandos comunes)
            if (isCommandLine(line)) {
                StringBuilder commandBlock = new StringBuilder();
                while (i < lines.length && isCommandLine(lines[i])) {
                    commandBlock.append(lines[i]).append("\n");
                    i++;
                }
                addCommandBlock(commandBlock.toString().trim());
                continue;
            }
            
            // Detectar código ensamblador - MEJORADO
            if (isAssemblyCode(line)) {
                StringBuilder codeBlock = new StringBuilder();
                int emptyLineCount = 0;
                
                while (i < lines.length) {
                    String currentLine = lines[i];
                    
                    // Si encontramos una nueva sección importante, detener
                    if (currentLine.contains("PASO") || currentLine.contains("STEP") ||
                        currentLine.contains("💾 Save") || currentLine.contains("💾 Guardar") ||
                        currentLine.contains("🔨 Compile") || currentLine.contains("🔨 Compilar") ||
                        currentLine.contains("📂 List") || currentLine.contains("📂 Listar")) {
                        break;
                    }
                    
                    // Si es código ensamblador o línea vacía dentro del bloque
                    if (isAssemblyCode(currentLine) || currentLine.trim().isEmpty()) {
                        if (currentLine.trim().isEmpty()) {
                            emptyLineCount++;
                            // Si hay más de 2 líneas vacías consecutivas, probablemente terminó el bloque
                            if (emptyLineCount > 2) {
                                break;
                            }
                        } else {
                            emptyLineCount = 0;
                            codeBlock.append(currentLine).append("\n");
                        }
                        i++;
                    } else {
                        // Si no es código ni línea vacía, terminó el bloque
                        break;
                    }
                }
                
                if (codeBlock.length() > 0) {
                    addAssemblyCodeBlock(codeBlock.toString().trim());
                }
                continue;
            }
            
            // Detectar subtítulos
            if (line.contains("📋") || line.contains("ℹ️") || line.contains("⚠️") || 
                line.contains("✨") || line.contains("📝") || line.contains("📂") ||
                line.contains("⏱️") || line.contains("💡") || line.contains("💾") ||
                line.contains("🔨")) {
                addSubtitle(line.trim());
                i++;
                continue;
            }
            
            // Detectar referencias (contiene URLs)
            if (line.contains("http://") || line.contains("https://")) {
                addClickableLink(line.trim());
                i++;
                continue;
            }
            
            // Texto normal
            if (!line.trim().isEmpty()) {
                addNormalText(line.trim());
            } else {
                addSpacer(8);
            }
            
            i++;
        }
    }
    
    private boolean isCommandLine(String line) {
        String trimmed = line.trim();
        // Comandos comunes de Termux/Linux
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
        String trimmed = line.trim();
        
        // Líneas vacías no son código (se manejan por separado)
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // Detectar código ensamblador - MEJORADO
        return trimmed.startsWith(";") ||                          // Comentarios
               trimmed.startsWith("LIST ") ||                      // Directiva LIST
               trimmed.startsWith("#include") ||                   // Include
               trimmed.startsWith("__CONFIG") ||                   // Configuración
               trimmed.startsWith("CBLOCK") ||                     // Bloque de constantes
               trimmed.startsWith("ENDC") ||                       // Fin de bloque
               trimmed.startsWith("ORG ") ||                       // Origen
               trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*:$") ||    // Labels (ej: Inicio:)
               trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") ||     // Variables (ej: d1, d2)
               trimmed.contains("goto ") ||                        // Instrucción goto
               trimmed.contains("call ") ||                        // Instrucción call
               trimmed.contains("movlw") ||                        // Instrucción movlw
               trimmed.contains("movwf") ||                        // Instrucción movwf
               trimmed.contains("banksel") ||                      // Instrucción banksel
               trimmed.contains("bsf ") ||                         // Instrucción bsf
               trimmed.contains("bcf ") ||                         // Instrucción bcf
               trimmed.contains("decfsz") ||                       // Instrucción decfsz
               trimmed.contains("return") ||                       // Instrucción return
               trimmed.equals("END") ||                            // Fin de programa
               (trimmed.startsWith("    ") &&                      // Instrucciones indentadas
                   (trimmed.contains("goto") || trimmed.contains("call") ||
                    trimmed.contains("mov") || trimmed.contains("bs") ||
                    trimmed.contains("decfsz") || trimmed.contains("return")));
    }
    
    private void addTitle(String text, int textSize, boolean bold) {
        TextView titleView = new TextView(context);
        titleView.setText(text);
        titleView.setTextSize(textSize);
        titleView.setTextColor(COLOR_TITLE);
        if (bold) {
            titleView.setTypeface(null, Typeface.BOLD);
        }
        titleView.setPadding(0, dpToPx(16), 0, dpToPx(8));
        titleView.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
        titleView.setLayoutParams(params);
        
        container.addView(titleView);
    }
    
    private void addSectionTitle(String text) {
        TextView sectionView = new TextView(context);
        sectionView.setText(text);
        sectionView.setTextSize(18);
        sectionView.setTextColor(COLOR_TITLE);
        sectionView.setTypeface(null, Typeface.BOLD);
        sectionView.setPadding(0, dpToPx(16), 0, dpToPx(8));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(8), 0, dpToPx(4));
        sectionView.setLayoutParams(params);
        
        container.addView(sectionView);
    }
    
    private void addSubtitle(String text) {
        TextView subtitleView = new TextView(context);
        subtitleView.setText(text);
        subtitleView.setTextSize(14);
        subtitleView.setTypeface(null, Typeface.BOLD);
        subtitleView.setTextColor(Color.parseColor("#424242"));
        subtitleView.setPadding(0, dpToPx(8), 0, dpToPx(4));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(4), 0, dpToPx(4));
        subtitleView.setLayoutParams(params);
        
        container.addView(subtitleView);
    }
    
    private void addCommandBlock(String command) {
        // Contenedor principal
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(COLOR_COMMAND_BG);
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blockParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        blockLayout.setLayoutParams(blockParams);
        
        // Header con título y botón
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView titleView = new TextView(context);
        titleView.setText("💻 " + (currentLanguage.equals("es") ? "Comando" : "Command"));
        titleView.setTextSize(12);
        titleView.setTextColor(Color.parseColor("#00FF00"));
        titleView.setTypeface(null, Typeface.BOLD);
        
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        
        headerLayout.addView(titleView);
        
        // Botón copiar
        Button copyBtn = new Button(context);
        copyBtn.setText(currentLanguage.equals("es") ? "📋 Copiar" : "📋 Copy");
        copyBtn.setTextSize(10);
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        copyBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        
        final String commandText = command;
        copyBtn.setOnClickListener(v -> copyToClipboard(commandText));
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        copyBtn.setLayoutParams(btnParams);
        
        headerLayout.addView(copyBtn);
        blockLayout.addView(headerLayout);
        
        // Separador
        View separator = new View(context);
        separator.setBackgroundColor(Color.parseColor("#555555"));
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        sepParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        separator.setLayoutParams(sepParams);
        blockLayout.addView(separator);
        
        // Texto del comando con scroll horizontal
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        
        TextView commandView = new TextView(context);
        commandView.setText(command);
        commandView.setTextColor(COLOR_COMMAND_TEXT);
        commandView.setTypeface(Typeface.MONOSPACE);
        commandView.setTextSize(12);
        commandView.setTextIsSelectable(true);
        commandView.setPadding(0, 0, dpToPx(16), 0);
        
        scrollView.addView(commandView);
        blockLayout.addView(scrollView);
        
        container.addView(blockLayout);
    }
    
    private void addAssemblyCodeBlock(String code) {
        // Contenedor principal
        LinearLayout blockLayout = new LinearLayout(context);
        blockLayout.setOrientation(LinearLayout.VERTICAL);
        blockLayout.setBackgroundColor(COLOR_CODE_BG);
        blockLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        
        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blockParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        blockLayout.setLayoutParams(blockParams);
        
        // Header con título y botón
        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView titleView = new TextView(context);
        titleView.setText("📝 " + (currentLanguage.equals("es") ? "Código Ensamblador" : "Assembly Code"));
        titleView.setTextSize(12);
        titleView.setTextColor(Color.parseColor("#61AFEF"));
        titleView.setTypeface(null, Typeface.BOLD);
        
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        
        headerLayout.addView(titleView);
        
        // Botón copiar
        Button copyBtn = new Button(context);
        copyBtn.setText(currentLanguage.equals("es") ? "📋 Copiar" : "📋 Copy");
        copyBtn.setTextSize(10);
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        copyBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        
        final String codeText = code;
        copyBtn.setOnClickListener(v -> copyToClipboard(codeText));
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        copyBtn.setLayoutParams(btnParams);
        
        headerLayout.addView(copyBtn);
        blockLayout.addView(headerLayout);
        
        // Separador
        View separator = new View(context);
        separator.setBackgroundColor(Color.parseColor("#3E4451"));
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        sepParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        separator.setLayoutParams(sepParams);
        blockLayout.addView(separator);
        
        // Código con numeración
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout codeContainer = new LinearLayout(context);
        codeContainer.setOrientation(LinearLayout.HORIZONTAL);
        
        // Columna de números de línea
        LinearLayout lineNumberLayout = new LinearLayout(context);
        lineNumberLayout.setOrientation(LinearLayout.VERTICAL);
        lineNumberLayout.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        
        // Columna de código
        LinearLayout codeLineLayout = new LinearLayout(context);
        codeLineLayout.setOrientation(LinearLayout.VERTICAL);
        
        String[] lines = code.split("\\n");
        int lineNumber = 1;
        
        for (String line : lines) {
            // Número de línea
            TextView lineNumView = new TextView(context);
            lineNumView.setText(String.format("%3d", lineNumber));
            lineNumView.setTextColor(COLOR_LINE_NUMBER);
            lineNumView.setTypeface(Typeface.MONOSPACE);
            lineNumView.setTextSize(11);
            lineNumView.setGravity(Gravity.END);
            lineNumberLayout.addView(lineNumView);
            
            // Línea de código con resaltado de sintaxis
            TextView codeLineView = new TextView(context);
            codeLineView.setText(highlightAssemblySyntax(line));
            codeLineView.setTextColor(COLOR_CODE_TEXT);
            codeLineView.setTypeface(Typeface.MONOSPACE);
            codeLineView.setTextSize(11);
            codeLineView.setTextIsSelectable(true);
            codeLineLayout.addView(codeLineView);
            
            lineNumber++;
        }
        
        codeContainer.addView(lineNumberLayout);
        codeContainer.addView(codeLineLayout);
        scrollView.addView(codeContainer);
        blockLayout.addView(scrollView);
        
        container.addView(blockLayout);
    }
    
    private SpannableString highlightAssemblySyntax(String line) {
        SpannableString spannable = new SpannableString(line);
        
        // Comentarios (empiezan con ;)
        if (line.trim().startsWith(";")) {
            spannable.setSpan(new ForegroundColorSpan(COLOR_COMMENT),
                    0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spannable;
        }
        
        // Palabras clave (instrucciones)
        String[] keywords = {"movlw", "movwf", "goto", "call", "return", "banksel",
                "bsf", "bcf", "decfsz", "LIST", "ORG", "CBLOCK", "ENDC", "__CONFIG"};
        
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                spannable.setSpan(new ForegroundColorSpan(COLOR_KEYWORD),
                        matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        
        return spannable;
    }
    
    private void addClickableLink(String text) {
        TextView linkView = new TextView(context);
        linkView.setTextSize(14);
        linkView.setPadding(0, dpToPx(4), 0, dpToPx(4));
        
        // Extraer URL
        Pattern urlPattern = Pattern.compile("(https?://[^\\s]+)");
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
                    ds.setColor(COLOR_LINK);
                    ds.setUnderlineText(true);
                }
            };
            
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        linkView.setText(spannable);
        linkView.setMovementMethod(LinkMovementMethod.getInstance());
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(4), 0, dpToPx(4));
        linkView.setLayoutParams(params);
        
        container.addView(linkView);
    }
    
    private void addNormalText(String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(14);
        textView.setTextColor(Color.parseColor("#424242"));
        textView.setTextIsSelectable(true);
        textView.setPadding(0, dpToPx(4), 0, dpToPx(4));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(2), 0, dpToPx(2));
        textView.setLayoutParams(params);
        
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
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("code", text);
        clipboard.setPrimaryClip(clip);
        
        String message = currentLanguage.equals("es") ? 
                "Código copiado al portapapeles" : 
                "Code copied to clipboard";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}