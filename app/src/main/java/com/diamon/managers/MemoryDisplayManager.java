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
    private Button reprogramButton;

    // Colores
    private static final int COLOR_BACKGROUND = Color.parseColor("#505060");
    private static final int COLOR_ADDRESS = Color.parseColor("#FFD700");
    private static final int COLOR_DATA_LOADED = Color.parseColor("#4CAF50");
    private static final int COLOR_DATA_EMPTY = Color.parseColor("#F44336");
    private static final int COLOR_ASCII = Color.parseColor("#00BCD4");
    private static final int COLOR_BUTTON = Color.parseColor("#2196F3");
    private static final int COLOR_BUTTON_REPROGRAM = Color.parseColor("#FF6600");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#9E9E9E");

    public interface OnMemoryRowClickListener {
        void onRowClick(boolean isRom, int address, TextView rowTextView);
    }

    public interface OnReprogramClickListener {
        void onReprogramClick();
    }

    private OnMemoryRowClickListener rowClickListener;
    private OnReprogramClickListener reprogramClickListener;

    public void setOnMemoryRowClickListener(OnMemoryRowClickListener listener) {
        this.rowClickListener = listener;
    }

    public void setOnReprogramClickListener(OnReprogramClickListener listener) {
        this.reprogramClickListener = listener;
    }

    public MemoryDisplayManager(Context context) {
        this.context = context;
    }

    public void preloadAd() {
        // La precarga ahora se maneja centralizadamente en GestorPublicidad
    }

    /**
     * Muestra el popup con estado de carga (progreso + anuncio).
     * Llamar ANTES de iniciar la lectura de memoria.
     */
    public void showLoadingState() {
        if (!isActivityValid()) {
            android.util.Log.w("MemoryDisplayManager", "showLoadingState: Activity no valida, ignorando");
            return;
        }

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        LinearLayout mainContainer = createMainContainer(screenWidth, screenHeight);

        if (memoryPopup != null && memoryPopup.isShowing()) {
            try {
                memoryPopup.dismiss();
            } catch (Exception ignored) {
            }
        }

        memoryPopup = new PopupWindow(
                mainContainer,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                true);
        memoryPopup.setOutsideTouchable(false);
        memoryPopup.setAnimationStyle(R.style.PopupAnimation);

        android.app.Activity activity = (android.app.Activity) context;
        View rootView = activity.getWindow().getDecorView();

        try {
            memoryPopup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
            applyShowAnimation(mainContainer);
        } catch (android.view.WindowManager.BadTokenException e) {
            android.util.Log.e("MemoryDisplayManager", "BadTokenException al mostrar popup: " + e.getMessage());
            return;
        }

        // Cargar o mostrar anuncio nativo usando el gestor centralizado
        if (context instanceof com.diamon.pic.MainActivity) {
            com.diamon.pic.MainActivity mainActivity = (com.diamon.pic.MainActivity) context;
            mainActivity.getPublicidad().mostrarNativeAd(com.diamon.publicidad.GestorPublicidad.KEY_NATIVE_MEMORY,
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

        // Ajustar el contenedor del anuncio para mantener visibilidad sin invadir el área de datos.
        if (adContainer != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) adContainer.getLayoutParams();
            lp.height = dpToPx(280); // Mantener minimo 280dp para cumplir el requisito de 120x120dp del MediaView
            adContainer.setLayoutParams(lp);
        }

        if (statusTextView != null) {
            statusTextView.setText(context.getString(com.diamon.pic.R.string.lectura_completa));
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

        if (reprogramButton != null) {
            reprogramButton.setVisibility(View.VISIBLE);
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
        container.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(8));
        container.setClipChildren(true);
        container.setClipToPadding(true);

        TextView title = new TextView(context);
        title.setText(context.getString(com.diamon.pic.R.string.datos_de_memoria));
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(4));
        container.addView(title);

        LinearLayout statusContainer = new LinearLayout(context);
        statusContainer.setOrientation(LinearLayout.VERTICAL);
        statusContainer.setGravity(Gravity.CENTER);
        statusContainer.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
        progressParams.setMargins(0, 0, 0, dpToPx(8));
        progressBar.setLayoutParams(progressParams);
        statusContainer.addView(progressBar);

        statusTextView = new TextView(context);
        statusTextView.setText(context.getString(com.diamon.pic.R.string.leyendo_memoria));
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
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(300));
        adParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
        adContainer.setLayoutParams(adParams);
        // Sin padding para que los assets del anuncio no se salgan del NativeAdView
        adContainer.setClipChildren(true);
        adContainer.setClipToPadding(true);
        container.addView(adContainer);

        LinearLayout contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        contentContainer.setLayoutParams(contentParams);

        romLabel = new TextView(context);
        romLabel.setText("▶ " + context.getString(com.diamon.pic.R.string.memoria_rom));
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
        eepromLabel.setText("▶ " + context.getString(com.diamon.pic.R.string.memoria_eeprom));
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

        LinearLayout buttonBar = new LinearLayout(context);
        buttonBar.setOrientation(LinearLayout.HORIZONTAL);
        buttonBar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        barParams.setMargins(0, dpToPx(6), 0, 0);
        buttonBar.setLayoutParams(barParams);

        reprogramButton = new Button(context);
        reprogramButton.setText(context.getString(R.string.reprogramar));
        reprogramButton.setTextColor(Color.WHITE);
        reprogramButton.setTextSize(14);
        reprogramButton.setAllCaps(false);

        GradientDrawable reprogramBg = new GradientDrawable();
        reprogramBg.setShape(GradientDrawable.RECTANGLE);
        reprogramBg.setColor(COLOR_BUTTON_REPROGRAM);
        reprogramBg.setCornerRadius(dpToPx(8));
        reprogramButton.setBackground(reprogramBg);
        reprogramButton.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        reprogramButton.setVisibility(View.GONE);

        LinearLayout.LayoutParams reprogramParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        reprogramParams.setMargins(0, 0, dpToPx(12), 0);
        reprogramButton.setLayoutParams(reprogramParams);
        reprogramButton.setOnClickListener(v -> {
            dismiss();
            if (reprogramClickListener != null) {
                reprogramClickListener.onReprogramClick();
            }
        });

        Button closeButton = new Button(context);
        closeButton.setText(context.getString(R.string.cerrar));
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(14);
        closeButton.setAllCaps(false);

        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setShape(GradientDrawable.RECTANGLE);
        buttonBg.setColor(COLOR_BUTTON);
        buttonBg.setCornerRadius(dpToPx(8));
        closeButton.setBackground(buttonBg);
        closeButton.setPadding(dpToPx(28), dpToPx(12), dpToPx(28), dpToPx(12));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        closeButton.setLayoutParams(buttonParams);
        closeButton.setOnClickListener(v -> dismissWithAnimation());

        buttonBar.addView(reprogramButton);
        buttonBar.addView(closeButton);
        container.addView(buttonBar);

        return container;
    }

    private void displayDataWithColors(LinearLayout container, String data, int groupSize, int columns, boolean isROM,
            int memorySize) {
        int address = 0;
        int bytesPerRow = groupSize * columns;

        for (int i = 0; i < data.length(); i += bytesPerRow) {
            String fullText = buildRowText(data, i, address, groupSize, columns, isROM);
            String addressHex = String.format("%04X", address);
            TextView rowTextView = createColoredRowWithAscii(fullText, addressHex, groupSize, isROM, 0);
            attachRowClickListener(rowTextView, isROM, address, groupSize, columns, memorySize);
            container.addView(rowTextView);

            address += columns;
        }

        int totalDataRows = (int) Math.ceil(data.length() / (double) bytesPerRow);
        int totalMemoryRows = (int) Math.ceil(memorySize / (double) columns);

        for (int i = totalDataRows; i < totalMemoryRows && i < totalDataRows + 50; i++) {
            String fullText = buildRowText(data, address * groupSize, address, groupSize, columns, isROM);
            String addressHex = String.format("%04X", address);
            TextView rowTextView = createColoredRowWithAscii(fullText, addressHex, groupSize, isROM, 0);
            attachRowClickListener(rowTextView, isROM, address, groupSize, columns, memorySize);
            container.addView(rowTextView);

            address += columns;
        }
    }

    private String buildRowText(String data, int rowStartOffset, int rowBaseAddress, int groupSize, int columns, boolean isROM) {
        String addressHex = String.format("%04X", rowBaseAddress);
        StringBuilder hexPart = new StringBuilder();
        StringBuilder asciiPart = new StringBuilder();
        hexPart.append(addressHex).append(": ");

        String emptyValue = isROM ? (groupSize == 4 ? "3FFF" : "FFFF") : "FF";
        int safeLen = (data != null) ? data.length() : 0;

        for (int j = 0; j < columns; j++) {
            int start = rowStartOffset + j * groupSize;
            int end = start + groupSize;
            String hexGroup;
            if (data != null && start >= 0 && end <= safeLen) {
                hexGroup = data.substring(start, end);
            } else if (data != null && start >= 0 && start < safeLen) {
                hexGroup = data.substring(start, safeLen);
            } else {
                hexGroup = emptyValue;
            }
            hexPart.append(hexGroup).append(" ");

            if (isROM) {
                if (hexGroup.length() >= 2) {
                    String lowByteHex = hexGroup.substring(hexGroup.length() - 2);
                    appendAscii(asciiPart, lowByteHex);
                } else {
                    asciiPart.append('.');
                }
            } else {
                for (int k = 0; k < hexGroup.length(); k += 2) {
                    if (k + 2 <= hexGroup.length()) {
                        appendAscii(asciiPart, hexGroup.substring(k, k + 2));
                    }
                }
            }
        }

        return hexPart.toString() + "│" + asciiPart.toString();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void attachRowClickListener(TextView rowTextView, boolean isROM, int rowBaseAddr,
            int groupSize, int columns, int memorySize) {
        final float[] lastTouchX = new float[] { -1f };
        rowTextView.setOnTouchListener((v, event) -> {
            if (event != null && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                lastTouchX[0] = event.getX();
            }
            return false;
        });
        rowTextView.setOnClickListener(v -> {
            if (rowClickListener == null) {
                return;
            }
            int col = 0;
            if (lastTouchX[0] >= 0f && rowTextView.getLayout() != null) {
                float x = lastTouchX[0] - rowTextView.getTotalPaddingLeft() + rowTextView.getScrollX();
                int charOffset = rowTextView.getLayout().getOffsetForHorizontal(0, Math.max(0f, x));
                int dataStart = 6; // "XXXX: "
                int cellStride = groupSize + 1; // "XXXX " o "XX "
                int dataEnd = dataStart + columns * cellStride;
                CharSequence text = rowTextView.getText();
                int sepPos = text != null ? text.toString().indexOf("│") : -1;
                if (charOffset < dataStart) {
                    col = 0;
                } else if (charOffset < dataEnd) {
                    col = (charOffset - dataStart) / cellStride;
                } else if (sepPos >= 0 && charOffset > sepPos) {
                    col = charOffset - (sepPos + 1);
                }
            }
            if (col < 0) {
                col = 0;
            } else if (col >= columns) {
                col = columns - 1;
            }
            int targetAddr = rowBaseAddr + col;
            if (memorySize > 0 && targetAddr >= memorySize) {
                targetAddr = Math.max(rowBaseAddr, memorySize - 1);
            }
            rowClickListener.onRowClick(isROM, targetAddr, rowTextView);
        });
    }

    /**
     * Refresca visualmente una fila de memoria tras editar una celda en el Editor Hexadecimal,
     * conservando todos los colores de sintaxis y actualizando la columna ASCII.
     */
    public void refreshRow(boolean isROM, int address, String fullHexData, int memorySize) {
        LinearLayout container = isROM ? romContainer : eepromContainer;
        if (container == null) {
            return;
        }
        int columns = 8;
        int groupSize = isROM ? 4 : 2;
        int rowIndex = address / columns;
        int rowBaseAddr = rowIndex * columns;
        if (rowIndex < 0 || rowIndex >= container.getChildCount()) {
            return;
        }
        View child = container.getChildAt(rowIndex);
        if (!(child instanceof TextView)) {
            return;
        }
        TextView rowTextView = (TextView) child;
        String addressHex = String.format("%04X", rowBaseAddr);
        String fullText = buildRowText(fullHexData, rowBaseAddr * groupSize, rowBaseAddr, groupSize, columns, isROM);
        rowTextView.setText(buildColoredSpannable(fullText, addressHex, isROM));
    }

    private TextView createColoredRowWithAscii(String fullText, String address, int groupSize, boolean isROM,
            int hexPartLength) {
        TextView textView = new TextView(context);
        textView.setText(buildColoredSpannable(fullText, address, isROM));
        textView.setTextSize(12);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setPadding(dpToPx(4), dpToPx(1), dpToPx(4), dpToPx(1));

        return textView;
    }

    private SpannableString buildColoredSpannable(String fullText, String address, boolean isROM) {
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

        return spannableString;
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
            try {
                memoryPopup.dismiss();
            } catch (IllegalArgumentException e) {
                android.util.Log.w("MemoryDisplayManager", "dismiss: View no attached: " + e.getMessage());
            } catch (Exception e) {
                android.util.Log.w("MemoryDisplayManager", "dismiss: Error inesperado: " + e.getMessage());
            }
        }
    }

    /** Verifica si la Activity del contexto esta activa y no fue destruida. */
    private boolean isActivityValid() {
        if (!(context instanceof android.app.Activity)) {
            return false;
        }
        android.app.Activity activity = (android.app.Activity) context;
        return !activity.isFinishing() && !activity.isDestroyed();
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
