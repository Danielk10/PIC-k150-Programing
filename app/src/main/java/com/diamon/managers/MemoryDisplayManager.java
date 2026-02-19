package com.diamon.managers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.diamon.pic.R;

/**
 * Gestor de visualizacion de memoria - VERSION MEJORADA
 * Caracteristicas:
 * - Diseno responsive (porcentajes de pantalla)
 * - Columna ASCII junto a datos hex
 * - Barra de progreso durante carga
 * - Anuncio nativo centralizado
 * - Colores premium
 */
public class MemoryDisplayManager {

    private final Context context;
    private PopupWindow memoryPopup;

    // Referencias a elementos del popup
    private ProgressBar progressBar;
    private TextView statusTextView;
    private FrameLayout adContainer;
    private ScrollView romScrollView;
    private ScrollView eepromScrollView;
    private LinearLayout romContainer;
    private LinearLayout eepromContainer;
    private TextView romLabel;
    private TextView eepromLabel;

    // Colores
    private static final int COLOR_BACKGROUND = Color.parseColor("#505060");
    private static final int COLOR_CARD = Color.parseColor("#2A2A3E");
    private static final int COLOR_ADDRESS = Color.parseColor("#FFD700");
    private static final int COLOR_DATA_LOADED = Color.parseColor("#4CAF50");
    private static final int COLOR_DATA_EMPTY = Color.parseColor("#F44336");
    private static final int COLOR_ASCII = Color.parseColor("#00BCD4");
    private static final int COLOR_BUTTON = Color.parseColor("#2196F3");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#9E9E9E");

    public MemoryDisplayManager(Context context) {
        this.context = context;
    }

    public void preloadAd() {
        // La precarga ahora se maneja centralizadamente en MostrarPublicidad
    }

    /**
     * Muestra el popup con estado de carga (progreso + anuncio).
     * Llamar ANTES de iniciar la lectura de memoria.
     */
    public void showLoadingState() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        LinearLayout mainContainer = createMainContainer(screenWidth, screenHeight);

        if (memoryPopup != null && memoryPopup.isShowing()) {
            memoryPopup.dismiss();
        }

        memoryPopup = new PopupWindow(
                mainContainer,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                true);
        memoryPopup.setOutsideTouchable(false);
        memoryPopup.setAnimationStyle(R.style.PopupAnimation);

        View rootView;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            rootView = activity.getWindow().getDecorView();
        } else {
            rootView = ((android.app.Activity) context).findViewById(android.R.id.content);
        }

        memoryPopup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
        applyShowAnimation(mainContainer);

        // Cargar o mostrar anuncio nativo usando el gestor centralizado
        if (context instanceof com.diamon.pic.MainActivity) {
            com.diamon.pic.MainActivity activity = (com.diamon.pic.MainActivity) context;
            activity.getPublicidad().mostrarNativeAd(com.diamon.publicidad.MostrarPublicidad.KEY_NATIVE_MEMORY,
                    adContainer);
        }
    }

    /**
     * Actualiza el popup con los datos de memoria.
     * Llamar DESPUES de que la lectura termine.
     */
    public void updateWithData(String romData, int romSize, String eepromData, int eepromSize, boolean hasEeprom) {
        if (memoryPopup == null || !memoryPopup.isShowing()) {
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // No ocultamos el adContainer inmediatamente, solo bajamos su peso visual si es
        // necesario
        // o lo mantenemos para que la impresión se cuente.
        if (adContainer != null) {
            // adContainer.setVisibility(View.GONE); // COMENTADO: Mantener visible
            // Podríamos reducir su altura para dar más espacio a los datos si fuera
            // necesario
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) adContainer.getLayoutParams();
            lp.height = dpToPx(220); // Mantener anuncio visible y liberar espacio para el visor HEX
            adContainer.setLayoutParams(lp);
        }

        if (statusTextView != null) {
            statusTextView.setText("Lectura Completa");
            statusTextView.setTextColor(COLOR_DATA_LOADED);
        }

        if (romScrollView != null) {
            romScrollView.setVisibility(View.VISIBLE);
        }
        if (romLabel != null) {
            romLabel.setVisibility(View.VISIBLE);
        }

        if (romContainer != null) {
            romContainer.removeAllViews();
            displayDataWithColors(romContainer, romData != null ? romData : "", 4, 8, true, romSize);
        }

        if (hasEeprom) {
            if (eepromLabel != null) {
                eepromLabel.setVisibility(View.VISIBLE);
            }
            if (eepromScrollView != null) {
                eepromScrollView.setVisibility(View.VISIBLE);
            }
            if (eepromContainer != null) {
                eepromContainer.removeAllViews();
                displayDataWithColors(eepromContainer, eepromData != null ? eepromData : "", 2, 8, false, eepromSize);
            }
        }

        if (memoryPopup != null) {
            memoryPopup.setOutsideTouchable(true);
        }
    }

    /**
     * Metodo legacy para compatibilidad.
     */
    public void showMemoryDataPopup(String romData, int romSize, String eepromData, int eepromSize, boolean hasEeprom) {
        showLoadingState();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            updateWithData(romData, romSize, eepromData, eepromSize, hasEeprom);
        }, 100);
    }

    private LinearLayout createMainContainer(int width, int height) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(width, height));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(16));
        shape.setColor(COLOR_BACKGROUND);
        shape.setStroke(dpToPx(2), Color.parseColor("#3A3A4E"));
        container.setBackground(shape);
        container.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(16));

        TextView title = new TextView(context);
        title.setText("Datos de Memoria");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(12));
        container.addView(title);

        LinearLayout statusContainer = new LinearLayout(context);
        statusContainer.setOrientation(LinearLayout.VERTICAL);
        statusContainer.setGravity(Gravity.CENTER);
        statusContainer.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
        progressParams.setMargins(0, 0, 0, dpToPx(8));
        progressBar.setLayoutParams(progressParams);
        statusContainer.addView(progressBar);

        statusTextView = new TextView(context);
        statusTextView.setText("Leyendo memoria...");
        statusTextView.setTextColor(COLOR_TEXT_SECONDARY);
        statusTextView.setTextSize(14);
        statusTextView.setGravity(Gravity.CENTER);
        statusContainer.addView(statusTextView);

        container.addView(statusContainer);

        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#3A3A4E"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        dividerParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        container.addView(divider, dividerParams);

        adContainer = new FrameLayout(context);
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(240));
        adParams.setMargins(0, 0, 0, dpToPx(8));
        adContainer.setLayoutParams(adParams);
        adContainer.setMinimumHeight(dpToPx(200));
        container.addView(adContainer);

        LinearLayout contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        contentContainer.setLayoutParams(contentParams);

        romLabel = new TextView(context);
        romLabel.setText("▶ Memoria ROM");
        romLabel.setTextColor(COLOR_DATA_LOADED);
        romLabel.setTextSize(14);
        romLabel.setTypeface(null, Typeface.BOLD);
        romLabel.setPadding(0, 0, 0, dpToPx(4));
        romLabel.setVisibility(View.GONE);
        contentContainer.addView(romLabel);

        romScrollView = new ScrollView(context);
        LinearLayout.LayoutParams romScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        romScrollView.setLayoutParams(romScrollParams);
        romScrollView.setVisibility(View.GONE);
        romScrollView.setFillViewport(true);

        GradientDrawable dataBg = new GradientDrawable();
        dataBg.setColor(Color.BLACK);
        dataBg.setCornerRadius(dpToPx(8));
        romScrollView.setBackground(dataBg);

        android.widget.HorizontalScrollView romHorizontalScroll = new android.widget.HorizontalScrollView(context);
        romContainer = new LinearLayout(context);
        romContainer.setOrientation(LinearLayout.VERTICAL);
        romContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        romHorizontalScroll.addView(romContainer);

        romScrollView.addView(romHorizontalScroll);
        contentContainer.addView(romScrollView);

        eepromLabel = new TextView(context);
        eepromLabel.setText("▶ Memoria EEPROM");
        eepromLabel.setTextColor(COLOR_DATA_LOADED);
        eepromLabel.setTextSize(14);
        eepromLabel.setTypeface(null, Typeface.BOLD);
        eepromLabel.setPadding(0, 0, 0, dpToPx(4));
        eepromLabel.setVisibility(View.GONE);
        contentContainer.addView(eepromLabel);

        eepromScrollView = new ScrollView(context);
        LinearLayout.LayoutParams eepromScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        eepromScrollView.setLayoutParams(eepromScrollParams);
        eepromScrollView.setVisibility(View.GONE);
        eepromScrollView.setFillViewport(true);
        eepromScrollView.setBackground(dataBg);

        android.widget.HorizontalScrollView eepromHorizontalScroll = new android.widget.HorizontalScrollView(context);
        eepromContainer = new LinearLayout(context);
        eepromContainer.setOrientation(LinearLayout.VERTICAL);
        eepromContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        eepromHorizontalScroll.addView(eepromContainer);

        eepromScrollView.addView(eepromHorizontalScroll);
        contentContainer.addView(eepromScrollView);

        container.addView(contentContainer);

        Button closeButton = new Button(context);
        closeButton.setText("Cerrar");
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(14);

        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setShape(GradientDrawable.RECTANGLE);
        buttonBg.setColor(COLOR_BUTTON);
        buttonBg.setCornerRadius(dpToPx(8));
        closeButton.setBackground(buttonBg);
        closeButton.setPadding(dpToPx(40), dpToPx(12), dpToPx(40), dpToPx(12));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.setMargins(0, dpToPx(12), 0, 0);
        closeButton.setLayoutParams(buttonParams);
        closeButton.setOnClickListener(v -> dismissWithAnimation());

        container.addView(closeButton);

        return container;
    }

    private void displayDataWithColors(LinearLayout container, String data, int groupSize, int columns, boolean isROM,
            int memorySize) {
        int address = 0;
        int bytesPerRow = groupSize * columns;

        for (int i = 0; i < data.length(); i += bytesPerRow) {
            StringBuilder hexPart = new StringBuilder();
            StringBuilder asciiPart = new StringBuilder();

            String addressHex = String.format("%04X", address);
            hexPart.append(addressHex).append(": ");

            for (int j = 0; j < columns; j++) {
                int start = i + j * groupSize;
                int end = Math.min(start + groupSize, data.length());

                if (start < data.length()) {
                    String hexGroup = data.substring(start, end);
                    hexPart.append(hexGroup).append(" ");

                    if (isROM) {
                        if (hexGroup.length() >= 2) {
                            String lowByteHex = hexGroup.substring(hexGroup.length() - 2);
                            appendAscii(asciiPart, lowByteHex);
                        }
                    } else {
                        for (int k = 0; k < hexGroup.length(); k += 2) {
                            if (k + 2 <= hexGroup.length()) {
                                appendAscii(asciiPart, hexGroup.substring(k, k + 2));
                            }
                        }
                    }
                }
            }

            String fullText = hexPart.toString() + "│" + asciiPart.toString();
            TextView rowTextView = createColoredRowWithAscii(fullText, addressHex, groupSize, isROM, hexPart.length());
            container.addView(rowTextView);

            address += columns;
        }

        int totalDataRows = (int) Math.ceil(data.length() / (double) bytesPerRow);
        int totalMemoryRows = (int) Math.ceil(memorySize / (double) columns);

        String emptyValue = isROM ? (groupSize == 4 ? "3FFF" : "FFFF") : "FF";

        for (int i = totalDataRows; i < totalMemoryRows && i < totalDataRows + 50; i++) {
            String addressHex = String.format("%04X", address);
            StringBuilder hexPart = new StringBuilder();
            StringBuilder asciiPart = new StringBuilder();

            hexPart.append(addressHex).append(": ");

            for (int j = 0; j < columns; j++) {
                hexPart.append(emptyValue).append(" ");
                for (int k = 0; k < groupSize / 2; k++) {
                    asciiPart.append('.');
                }
            }

            String fullText = hexPart.toString() + "│" + asciiPart.toString();
            TextView rowTextView = createColoredRowWithAscii(fullText, addressHex, groupSize, isROM, hexPart.length());
            container.addView(rowTextView);

            address += columns;
        }
    }

    private TextView createColoredRowWithAscii(String fullText, String address, int groupSize, boolean isROM,
            int hexPartLength) {
        TextView textView = new TextView(context);
        SpannableString spannableString = new SpannableString(fullText);

        int dirEnd = address.length() + 2;
        spannableString.setSpan(
                new ForegroundColorSpan(COLOR_ADDRESS),
                0, dirEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int dataStart = dirEnd;
        int separatorPos = fullText.indexOf("│");
        if (separatorPos > 0) {
            String dataSection = fullText.substring(dataStart, separatorPos);
            String[] groups = dataSection.trim().split("\\s+");

            int currentPos = dataStart;
            for (String group : groups) {
                if (group.isEmpty())
                    continue;

                boolean isEmpty = isEmptyData(group, isROM);
                int color = isEmpty ? COLOR_DATA_EMPTY : COLOR_DATA_LOADED;

                int groupEnd = currentPos + group.length();
                if (groupEnd <= fullText.length()) {
                    spannableString.setSpan(
                            new ForegroundColorSpan(color),
                            currentPos, groupEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                currentPos = groupEnd + 1;
            }

            if (separatorPos + 1 < fullText.length()) {
                spannableString.setSpan(
                        new ForegroundColorSpan(COLOR_ASCII),
                        separatorPos, fullText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        textView.setText(spannableString);
        textView.setTextSize(12);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setPadding(dpToPx(4), dpToPx(1), dpToPx(4), dpToPx(1));

        return textView;
    }

    private boolean isEmptyData(String data, boolean isROM) {
        if (data == null || data.isEmpty())
            return false;
        String upper = data.toUpperCase();

        if (isROM) {
            return upper.equals("3FFF") || upper.equals("FFFF");
        } else {
            return upper.equals("FF");
        }
    }

    private void applyShowAnimation(View view) {
        view.setScaleY(0);
        view.setPivotY(0);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new BounceInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            view.setScaleY(value);
        });
        animator.start();
    }

    private void dismissWithAnimation() {
        if (memoryPopup == null || !memoryPopup.isShowing()) {
            return;
        }

        View popupView = memoryPopup.getContentView();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float targetY = metrics.heightPixels - popupView.getTop();

        ValueAnimator animator = ValueAnimator.ofFloat(0, targetY);
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateInterpolator(1.5f));
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            popupView.setTranslationY(value);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dismiss();
            }
        });

        animator.start();
    }

    public void dismiss() {
        if (memoryPopup != null && memoryPopup.isShowing()) {
            memoryPopup.dismiss();
        }
    }

    public void dismissAllPopups() {
        dismiss();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private void appendAscii(StringBuilder builder, String hexByte) {
        try {
            int value = Integer.parseInt(hexByte, 16);
            char c = (value >= 32 && value <= 126) ? (char) value : '.';
            builder.append(c);
        } catch (NumberFormatException e) {
            builder.append('.');
        }
    }
}
