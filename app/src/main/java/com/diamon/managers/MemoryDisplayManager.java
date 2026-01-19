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

import androidx.core.graphics.drawable.DrawableCompat;

import com.diamon.pic.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

/**
 * Gestor de visualizacion de memoria - VERSION MEJORADA
 * Caracteristicas:
 * - Diseno responsive (porcentajes de pantalla)
 * - Columna ASCII junto a datos hex
 * - Barra de progreso durante carga
 * - Anuncio nativo integrado
 * - Colores premium
 */
public class MemoryDisplayManager {

    private final Context context;
    private PopupWindow memoryPopup;
    private NativeAd nativeAd;

    // Referencias a elementos del popup
    private LinearLayout contentContainer;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private FrameLayout adContainer;
    private ScrollView romScrollView;
    private ScrollView eepromScrollView;
    private LinearLayout romContainer;
    private LinearLayout eepromContainer;
    private TextView romLabel;
    private TextView eepromLabel;
    private Button closeButton;

    // Colores
    private static final int COLOR_BACKGROUND = Color.parseColor("#505060"); // Tono mas claro
    private static final int COLOR_CARD = Color.parseColor("#2A2A3E");
    private static final int COLOR_ADDRESS = Color.parseColor("#FFD700");
    private static final int COLOR_DATA_LOADED = Color.parseColor("#4CAF50");
    private static final int COLOR_DATA_EMPTY = Color.parseColor("#F44336");
    private static final int COLOR_ASCII = Color.parseColor("#00BCD4");
    private static final int COLOR_BUTTON = Color.parseColor("#2196F3");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#E0E0E0");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#9E9E9E");

    public MemoryDisplayManager(Context context) {
        this.context = context;
    }

    /**
     * Pre-carga un anuncio nativo para que este listo cuando se muestre el popup.
     */
    public void preloadAd() {
        if (nativeAd != null) {
            return; // Ya hay uno cargado
        }

        AdLoader.Builder builder = new AdLoader.Builder(context, "ca-app-pub-5141499161332805/1625082944");
        builder.forNativeAd(ad -> {
            nativeAd = ad; // Guardar referencia para uso posterior
        });

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .setVideoOptions(videoOptions)
                .build();

        builder.withNativeAdOptions(adOptions);
        builder.build().loadAd(new AdRequest.Builder().build());
    }

    /**
     * Muestra el popup con estado de carga (progreso + anuncio).
     * Llamar ANTES de iniciar la lectura de memoria.
     */
    public void showLoadingState() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        int popupWidth = (int) (screenWidth * 0.92);
        int popupHeight = (int) (screenHeight * 0.85);

        LinearLayout mainContainer = createMainContainer(popupWidth, popupHeight);

        if (memoryPopup != null && memoryPopup.isShowing()) {
            memoryPopup.dismiss();
        }

        memoryPopup = new PopupWindow(mainContainer, popupWidth, popupHeight, true);
        memoryPopup.setOutsideTouchable(false);
        memoryPopup.setAnimationStyle(0);

        View rootView;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            rootView = activity.getWindow().getDecorView();
        } else {
            rootView = ((android.app.Activity) context).findViewById(android.R.id.content);
        }

        memoryPopup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
        applyShowAnimation(mainContainer);

        // Cargar o mostrar anuncio nativo
        if (nativeAd != null) {
            populateNativeAdView(adContainer, nativeAd);
        } else {
            loadNativeAd(adContainer);
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

        // Ocultar progreso y anuncio para dar espacio a los datos
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (adContainer != null) {
            adContainer.setVisibility(View.GONE);
            adContainer.removeAllViews(); // Limpiar recursos
        }

        // Actualizar texto de estado - String: lectura_completada
        if (statusTextView != null) {
            statusTextView.setText("Lectura Completa"); // R.string.lectura_completada
            statusTextView.setTextColor(COLOR_DATA_LOADED);
        }

        // Mostrar contenedor de datos
        if (romScrollView != null) {
            romScrollView.setVisibility(View.VISIBLE);
        }
        if (romLabel != null) {
            romLabel.setVisibility(View.VISIBLE);
        }

        // Poblar datos ROM
        if (romContainer != null) {
            romContainer.removeAllViews();
            displayDataWithColors(romContainer, romData != null ? romData : "", 4, 8, true, romSize);
        }

        // EEPROM si existe
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

        // Habilitar cierre
        if (memoryPopup != null) {
            memoryPopup.setOutsideTouchable(true);
        }
    }

    /**
     * Metodo legacy para compatibilidad.
     */
    public void showMemoryDataPopup(String romData, int romSize, String eepromData, int eepromSize, boolean hasEeprom) {
        showLoadingState();
        // Pequeno delay para mostrar el popup primero
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            updateWithData(romData, romSize, eepromData, eepromSize, hasEeprom);
        }, 100);
    }

    /**
     * Crea el contenedor principal del popup
     */
    private LinearLayout createMainContainer(int width, int height) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(width, height));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(16));
        shape.setColor(COLOR_BACKGROUND);
        shape.setStroke(dpToPx(2), Color.parseColor("#3A3A4E")); // Borde para diferenciar del fondo
        container.setBackground(shape);
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Titulo - String: datos_de_memoria
        TextView title = new TextView(context);
        title.setText("Datos de Memoria"); // R.string.datos_de_memoria
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(12));
        container.addView(title);

        // Contenedor de estado (progreso + texto)
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

        // Texto de estado - String: leyendo_memoria
        statusTextView = new TextView(context);
        statusTextView.setText("Leyendo memoria..."); // R.string.leyendo_memoria
        statusTextView.setTextColor(COLOR_TEXT_SECONDARY);
        statusTextView.setTextSize(14);
        statusTextView.setGravity(Gravity.CENTER);
        statusContainer.addView(statusTextView);

        container.addView(statusContainer);

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#3A3A4E"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        dividerParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        container.addView(divider, dividerParams);

        // Contenedor de anuncio
        adContainer = new FrameLayout(context);
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(380));
        adParams.setMargins(0, 0, 0, dpToPx(8));
        adContainer.setLayoutParams(adParams);
        adContainer.setMinimumHeight(dpToPx(350));
        container.addView(adContainer);

        // Contenedor de datos (ScrollView con peso para llenar espacio restante)
        contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        contentContainer.setLayoutParams(contentParams);

        // Label ROM - String: memoria_rom
        romLabel = new TextView(context);
        romLabel.setText("▶ Memoria ROM"); // R.string.memoria_rom
        romLabel.setTextColor(COLOR_DATA_LOADED);
        romLabel.setTextSize(14);
        romLabel.setTypeface(null, Typeface.BOLD);
        romLabel.setPadding(0, 0, 0, dpToPx(4));
        romLabel.setVisibility(View.GONE);
        contentContainer.addView(romLabel);

        // ScrollView ROM
        romScrollView = new ScrollView(context);
        LinearLayout.LayoutParams romScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        romScrollView.setLayoutParams(romScrollParams);
        romScrollView.setVisibility(View.GONE);
        romScrollView.setFillViewport(true); // Estirar contenido para simetria

        GradientDrawable dataBg = new GradientDrawable();
        dataBg.setColor(Color.BLACK);
        dataBg.setCornerRadius(dpToPx(8));
        romScrollView.setBackground(dataBg);
        romScrollView.setClipToOutline(true);

        // Horizontal Scroll para ROM
        android.widget.HorizontalScrollView romHorizontalScroll = new android.widget.HorizontalScrollView(context);
        romHorizontalScroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)); // Llenar scrollview

        romContainer = new LinearLayout(context);
        romContainer.setOrientation(LinearLayout.VERTICAL);
        romContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        romHorizontalScroll.addView(romContainer);
        romScrollView.addView(romHorizontalScroll);
        contentContainer.addView(romScrollView);

        // Label EEPROM - String: memoria_eeprom
        eepromLabel = new TextView(context);
        eepromLabel.setText("▶ Memoria EEPROM"); // R.string.memoria_eeprom
        eepromLabel.setTextColor(COLOR_DATA_LOADED);
        eepromLabel.setTextSize(14);
        eepromLabel.setTypeface(null, Typeface.BOLD);
        eepromLabel.setPadding(0, 0, 0, dpToPx(4)); // Padding identico a ROM
        eepromLabel.setVisibility(View.GONE);
        contentContainer.addView(eepromLabel);

        // ScrollView EEPROM
        eepromScrollView = new ScrollView(context);
        LinearLayout.LayoutParams eepromScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f); // Simetrico con ROM
        eepromScrollView.setLayoutParams(eepromScrollParams);
        eepromScrollView.setVisibility(View.GONE);
        eepromScrollView.setFillViewport(true); // Estirar contenido

        eepromScrollView.setBackground(dataBg); // Reusar background
        eepromScrollView.setClipToOutline(true);

        // Horizontal Scroll para EEPROM
        android.widget.HorizontalScrollView eepromHorizontalScroll = new android.widget.HorizontalScrollView(context);
        eepromHorizontalScroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        eepromContainer = new LinearLayout(context);
        eepromContainer.setOrientation(LinearLayout.VERTICAL);
        eepromContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        eepromHorizontalScroll.addView(eepromContainer);
        eepromScrollView.addView(eepromHorizontalScroll);
        contentContainer.addView(eepromScrollView);

        container.addView(contentContainer);

        // Boton cerrar - String: cerrar
        closeButton = new Button(context);
        closeButton.setText("Cerrar"); // R.string.cerrar
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

    /**
     * Muestra datos con colores y columna ASCII
     */
    private void displayDataWithColors(LinearLayout container, String data, int groupSize, int columns, boolean isROM,
            int memorySize) {
        int address = 0;
        int bytesPerRow = groupSize * columns;

        for (int i = 0; i < data.length(); i += bytesPerRow) {
            StringBuilder hexPart = new StringBuilder();
            StringBuilder asciiPart = new StringBuilder();

            // Direccion
            String addressHex = String.format("%04X", address);
            hexPart.append(addressHex).append(": ");

            // Datos hex y ASCII
            for (int j = 0; j < columns; j++) {
                int start = i + j * groupSize;
                int end = Math.min(start + groupSize, data.length());

                if (start < data.length()) {
                    String hexGroup = data.substring(start, end);
                    hexPart.append(hexGroup).append(" ");

                    // Convertir a ASCII
                    for (int k = 0; k < hexGroup.length(); k += 2) {
                        if (k + 2 <= hexGroup.length()) {
                            try {
                                int value = Integer.parseInt(hexGroup.substring(k, k + 2), 16);
                                char c = (value >= 32 && value <= 126) ? (char) value : '.';
                                asciiPart.append(c);
                            } catch (NumberFormatException e) {
                                asciiPart.append('.');
                            }
                        }
                    }
                }
            }

            // Crear fila con colores
            String fullText = hexPart.toString() + "│" + asciiPart.toString();
            TextView rowTextView = createColoredRowWithAscii(fullText, addressHex, groupSize, isROM, hexPart.length());
            container.addView(rowTextView);

            address += columns;
        }

        // Agregar filas vacias hasta el tamano real de memoria
        int totalDataRows = (int) Math.ceil(data.length() / (double) bytesPerRow);
        int totalMemoryRows = (int) Math.ceil(memorySize / (double) columns);

        String emptyValue = isROM ? (groupSize == 4 ? "3FFF" : "FFFF") : "FF";
        int charsPerGroup = groupSize;

        for (int i = totalDataRows; i < totalMemoryRows && i < totalDataRows + 50; i++) {
            String addressHex = String.format("%04X", address);
            StringBuilder hexPart = new StringBuilder();
            StringBuilder asciiPart = new StringBuilder();

            hexPart.append(addressHex).append(": ");

            for (int j = 0; j < columns; j++) {
                hexPart.append(emptyValue).append(" ");
                for (int k = 0; k < charsPerGroup / 2; k++) {
                    asciiPart.append('.');
                }
            }

            String fullText = hexPart.toString() + "│" + asciiPart.toString();
            TextView rowTextView = createColoredRowWithAscii(fullText, addressHex, groupSize, isROM, hexPart.length());
            container.addView(rowTextView);

            address += columns;
        }
    }

    /**
     * Crea fila con colores: DORADO (dir), VERDE/ROJO (datos), CYAN (ASCII)
     */
    private TextView createColoredRowWithAscii(String fullText, String address, int groupSize, boolean isROM,
            int hexPartLength) {
        TextView textView = new TextView(context);
        SpannableString spannableString = new SpannableString(fullText);

        // Color DORADO para direccion (0000: )
        int dirEnd = address.length() + 2;
        spannableString.setSpan(
                new ForegroundColorSpan(COLOR_ADDRESS),
                0, dirEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Colores para datos hex
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

            // Color CYAN para ASCII
            if (separatorPos + 1 < fullText.length()) {
                spannableString.setSpan(
                        new ForegroundColorSpan(COLOR_ASCII),
                        separatorPos, fullText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        textView.setText(spannableString);
        textView.setTextSize(12); // Fuente mas grande para mejor legibilidad
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setPadding(dpToPx(4), dpToPx(1), dpToPx(4), dpToPx(1));

        return textView;
    }

    /**
     * Detecta si un dato es vacio (FF, 3FFF, FFFF).
     */
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

    /**
     * Carga un anuncio nativo
     */
    private void loadNativeAd(FrameLayout adContainer) {
        // ID de anuncio para lectura de memoria
        AdLoader.Builder builder = new AdLoader.Builder(context, "ca-app-pub-5141499161332805/1625082944");

        builder.forNativeAd(ad -> {
            if (nativeAd != null) {
                nativeAd.destroy();
            }
            nativeAd = ad;
            populateNativeAdView(adContainer, ad);
        });

        builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                showAdPlaceholder(adContainer);
            }
        });

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .setVideoOptions(videoOptions)
                .build();

        builder.withNativeAdOptions(adOptions);
        builder.build().loadAd(new AdRequest.Builder().build());
    }

    /**
     * Puebla la vista del anuncio nativo
     */
    private void populateNativeAdView(FrameLayout container, NativeAd ad) {
        container.removeAllViews();

        NativeAdView adView = (NativeAdView) android.view.LayoutInflater.from(context)
                .inflate(R.layout.layout_native_ad, null);

        adView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        ((TextView) adView.getHeadlineView()).setText(ad.getHeadline());
        ((TextView) adView.getBodyView()).setText(ad.getBody());
        ((TextView) adView.getCallToActionView()).setText(ad.getCallToAction());

        if (ad.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(ad.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        if (ad.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(ad.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        adView.setNativeAd(ad);
        container.addView(adView);
    }

    /**
     * Muestra placeholder cuando el anuncio no carga
     */
    private void showAdPlaceholder(FrameLayout container) {
        container.removeAllViews();

        LinearLayout placeholderLayout = new LinearLayout(context);
        placeholderLayout.setOrientation(LinearLayout.VERTICAL);
        placeholderLayout.setGravity(Gravity.CENTER);
        placeholderLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_CARD);
        bg.setCornerRadius(dpToPx(8));
        placeholderLayout.setBackground(bg);

        ImageView icon = new ImageView(context);
        icon.setImageResource(android.R.drawable.ic_dialog_info);
        icon.setColorFilter(COLOR_TEXT_SECONDARY);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        iconParams.setMargins(0, 0, 0, dpToPx(8));
        placeholderLayout.addView(icon, iconParams);

        TextView placeholderText = new TextView(context);
        placeholderText.setText("Anuncio no disponible");
        placeholderText.setTextSize(14);
        placeholderText.setTextColor(COLOR_TEXT_SECONDARY);
        placeholderText.setGravity(Gravity.CENTER);
        placeholderLayout.addView(placeholderText);

        container.addView(placeholderLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /**
     * Aplica animacion de entrada.
     */
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

    /**
     * Cierra con animacion.
     */
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

    /**
     * Cierra el popup y libera recursos, y precarga el siguiente anuncio.
     */
    public void dismiss() {
        if (memoryPopup != null && memoryPopup.isShowing()) {
            memoryPopup.dismiss();
        }

        // NO destruir el anuncio aqui por si queremos precargarlo, o destruirlo y
        // cargar uno nuevo
        // Mejor estrategia: Pre-cargar uno nuevo para la proxima vez
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }

        // Precargar para la siguiente vez (usando handler para asegurar que se ejecute
        // despues de liberar)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::preloadAd, 500);
    }

    public void dismissAllPopups() {
        dismiss();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
