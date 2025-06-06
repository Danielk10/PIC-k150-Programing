package com.diamon.pic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.AdChoicesView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.Random;

public class MainActivity2 extends AppCompatActivity {

    private PopupWindow popupWindow;
    private NativeAd nativeAd;

    // --- Variables para controlar el estado y la UI dinámica ---
    private boolean procesoGrabado = false; // Bandera de control
    private Handler sim_handler = new Handler(Looper.getMainLooper()); // Para simular el proceso

    // Referencias a los componentes de la UI que cambiarán
    private ProgressBar statusProgressBar;
    private ImageView statusResultIcon;
    private Button actionButton;
    private TextView titleTextView;
    private TextView descriptionTextView;
    // -----------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, initializationStatus -> {});

        RelativeLayout mainLayout = new RelativeLayout(this);
        mainLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        mainLayout.setBackgroundColor(Color.parseColor("#EEEEEE"));

        Button showPopupButton = new Button(this);
        showPopupButton.setText("Iniciar Grabación de PIC");
        showPopupButton.setId(View.generateViewId());

        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        showPopupButton.setLayoutParams(buttonParams);

        showPopupButton.setOnClickListener(v -> showAdPopup());

        mainLayout.addView(showPopupButton);
        setContentView(mainLayout);
    }

    private void showAdPopup() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int currentScreenHeight = displayMetrics.heightPixels;

        int popupHeight = (int) (currentScreenHeight * 0.7);

        LinearLayout popupContainer = new LinearLayout(this);
        popupContainer.setOrientation(LinearLayout.VERTICAL);
        popupContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, popupHeight));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(40f);
        shape.setColor(Color.WHITE);
        popupContainer.setBackground(shape);

        // --- Contenido Superior Dinámico ---
        LinearLayout topContent = new LinearLayout(this);
        topContent.setOrientation(LinearLayout.VERTICAL);
        topContent.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.8f);
        topContent.setLayoutParams(topParams);
        topContent.setPadding(32, 32, 32, 32);

        titleTextView = new TextView(this);
        titleTextView.setText("Grabando PIC...");
        titleTextView.setTextSize(22);
        titleTextView.setTextColor(Color.BLACK);
        titleTextView.setGravity(Gravity.CENTER);
        topContent.addView(titleTextView);

        // Contenedor para el indicador (ProgressBar / Icono de resultado)
        FrameLayout statusIndicatorContainer = new FrameLayout(this);
        statusIndicatorContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        indicatorParams.setMargins(0, 24, 0, 24);
        topContent.addView(statusIndicatorContainer, indicatorParams);

        // Barra de progreso (visible al inicio)
        statusProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
        statusProgressBar.setVisibility(View.VISIBLE);
        statusIndicatorContainer.addView(statusProgressBar);

        // Icono de resultado (oculto al inicio)
        statusResultIcon = new ImageView(this);
        statusResultIcon.setVisibility(View.GONE);
        statusIndicatorContainer.addView(statusResultIcon);

        descriptionTextView = new TextView(this);
        descriptionTextView.setText("Por favor, espere. No desconecte el dispositivo.");
        descriptionTextView.setTextSize(16);
        descriptionTextView.setTextColor(Color.DKGRAY);
        descriptionTextView.setGravity(Gravity.CENTER);
        topContent.addView(descriptionTextView);

        popupContainer.addView(topContent);
        // --- Fin Contenido Superior Dinámico ---


        View divider = new View(this);
        divider.setBackgroundColor(Color.LTGRAY);
        popupContainer.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));

        FrameLayout adContainer = new FrameLayout(this);
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.2f);
        adContainer.setLayoutParams(adParams);
        adContainer.setPadding(8, 8, 8, 8);
        adContainer.setId(View.generateViewId());
        popupContainer.addView(adContainer);

        // --- Contenedor para el Botón Único ---
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setGravity(Gravity.CENTER_HORIZONTAL); // Centrar el botón
        buttonContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        buttonContainer.setPadding(16, 16, 16, 16);

        actionButton = new Button(this);
        LinearLayout.LayoutParams buttonLayoutParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        actionButton.setLayoutParams(buttonLayoutParam);
        actionButton.setPadding(100, 40, 100, 40);
        actionButton.setTextColor(Color.WHITE);

        // Estado inicial del botón
        actionButton.setText("Cancelar");
        actionButton.setBackgroundResource(R.drawable.button_background_red);
        actionButton.setOnClickListener(v -> {
            // Si se cancela, se detiene el "proceso" y se cierra
            sim_handler.removeCallbacksAndMessages(null); // Detener el handler
            dismissPopupWithSlideDown();
        });

        buttonContainer.addView(actionButton);
        popupContainer.addView(buttonContainer);
        // --- Fin Contenedor para el Botón Único ---

        popupWindow = new PopupWindow(popupContainer, (int) (screenWidth * 0.9), popupHeight, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(false); // No permitir cerrar tocando fuera durante el proceso
        popupWindow.setAnimationStyle(0);

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        applyCustomAnimation(popupContainer);
        loadNativeAd(adContainer);

        // --- Iniciar simulación del proceso de grabado ---
        simularProcesoDeGrabado();
    }

    private void simularProcesoDeGrabado() {
        // Simula un proceso que tarda 5 segundos y puede fallar o tener éxito
        sim_handler.postDelayed(() -> {
            procesoGrabado = new Random().nextBoolean(); // Resultado aleatorio
            // Se debe ejecutar en el Hilo de UI
            runOnUiThread(() -> actualizarUIProcesoFinalizado(procesoGrabado));
        }, 5000); // 5 segundos de simulación
    }

    private void actualizarUIProcesoFinalizado(boolean exito) {
        // Ocultar barra de progreso y mostrar icono de resultado
        statusProgressBar.setVisibility(View.GONE);
        statusResultIcon.setVisibility(View.VISIBLE);

        // Permitir que el popup se cierre tocando fuera
        popupWindow.setOutsideTouchable(true);

        if (exito) {
            // Estado de ÉXITO
            titleTextView.setText("Grabación Completa");
            descriptionTextView.setText("El firmware se ha grabado correctamente en el PIC.");
            statusResultIcon.setImageResource(R.drawable.ic_status_success);
            Drawable successDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
            DrawableCompat.setTint(successDrawable, Color.parseColor("#4CAF50")); // Verde
        } else {
            // Estado de FALLO
            titleTextView.setText("Fallo en la Grabación");
            descriptionTextView.setText("No se pudo completar el proceso. Verifique la conexión.");
            statusResultIcon.setImageResource(R.drawable.ic_status_failure);
            Drawable failureDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
            DrawableCompat.setTint(failureDrawable, Color.parseColor("#D32F2F")); // Rojo
        }

        // Actualizar el botón al estado "Aceptar"
        actionButton.setText("Aceptar");
        actionButton.setBackgroundResource(R.drawable.button_background_blue);
        // El nuevo OnClickListener simplemente cierra el popup
        actionButton.setOnClickListener(v -> dismissPopupWithSlideDown());
    }

    // El resto de los métodos (dismiss, animations, loadAd, etc.) permanecen igual...
    // ... (pegando el resto de tus métodos sin cambios para que el código esté completo)

    private void dismissPopupWithSlideDown() {
        if (popupWindow != null && popupWindow.isShowing()) {
            final View popupView = popupWindow.getContentView();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int currentScreenHeight = displayMetrics.heightPixels;
            float startTranslationY = popupView.getTranslationY();
            float targetTranslationY = currentScreenHeight - popupView.getTop();
            ValueAnimator animator = ValueAnimator.ofFloat(startTranslationY, targetTranslationY);
            animator.setDuration(600);
            animator.setInterpolator(new AccelerateInterpolator(1.5f));
            animator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                popupView.setTranslationY(value);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    popupWindow.dismiss();
                    if (nativeAd != null) {
                        nativeAd.destroy();
                        nativeAd = null;
                    }
                }
            });
            animator.start();
        }
    }

    private void applyCustomAnimation(View popupView) {
        popupView.setScaleY(0);
        popupView.setPivotY(0);
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0f, 1f);
        scaleAnimator.setDuration(1000);
        scaleAnimator.setInterpolator(new BounceInterpolator());
        scaleAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            popupView.setScaleY(value);
        });
        scaleAnimator.start();

        popupWindow.setTouchInterceptor((v, event) -> {
            // Este interceptor ahora solo funciona si outsideTouchable es true
            if (popupWindow.isOutsideTouchable() && !isPointInsideView(event.getRawX(), event.getRawY(), popupWindow.getContentView())) {
                dismissPopupWithSlideDown();
                return true;
            }
            return false;
        });
    }

    private boolean isPointInsideView(float x, float y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        return (x >= viewX && x <= (viewX + view.getWidth()))
                && (y >= viewY && y <= (viewY + view.getHeight()));
    }

    private void loadNativeAd(FrameLayout adContainer) {
        // El ID de prueba es el más seguro de usar para desarrollo
        AdLoader.Builder builder = new AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110");

        builder.forNativeAd(loadedNativeAd -> {
            // ... (el código interno de forNativeAd no necesita cambios)
            adContainer.removeAllViews();
            if (this.nativeAd != null) { this.nativeAd.destroy(); }
            this.nativeAd = loadedNativeAd;
            NativeAdView adView = new NativeAdView(this);
            adView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            LinearLayout adInternalLayout = new LinearLayout(this);
            adInternalLayout.setOrientation(LinearLayout.VERTICAL);
            adInternalLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            adInternalLayout.setPadding(16, 16, 16, 16);
            TextView adBadge = new TextView(this);
            adBadge.setText("Ad");
            adBadge.setTextSize(10);
            adBadge.setTextColor(Color.WHITE);
            GradientDrawable adBadgeBackground = new GradientDrawable();
            adBadgeBackground.setShape(GradientDrawable.RECTANGLE);
            adBadgeBackground.setColor(Color.parseColor("#FFCC66"));
            adBadgeBackground.setCornerRadius(8f);
            adBadge.setBackground(adBadgeBackground);
            adBadge.setPadding(8, 4, 8, 4);
            LinearLayout.LayoutParams adBadgeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            adBadgeParams.setMargins(0, 0, 0, 8);
            adInternalLayout.addView(adBadge, adBadgeParams);
            MediaView mediaView = new MediaView(this);
            LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            mediaParams.gravity = Gravity.CENTER_HORIZONTAL;
            mediaParams.height = (int) (200 * getResources().getDisplayMetrics().density);
            mediaView.setLayoutParams(mediaParams);
            adView.setMediaView(mediaView);
            TextView headlineView = new TextView(this);
            headlineView.setText(nativeAd.getHeadline());
            headlineView.setTextSize(18);
            headlineView.setTextColor(Color.BLACK);
            headlineView.setGravity(Gravity.START);
            headlineView.setPadding(0, 8, 0, 4);
            adView.setHeadlineView(headlineView);
            TextView bodyView = new TextView(this);
            bodyView.setText(nativeAd.getBody());
            bodyView.setTextSize(14);
            bodyView.setTextColor(Color.DKGRAY);
            bodyView.setPadding(0, 4, 0, 8);
            adView.setBodyView(bodyView);
            ImageView iconView = new ImageView(this);
            if (nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                iconView.setImageDrawable(nativeAd.getIcon().getDrawable());
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams((int) (40 * getResources().getDisplayMetrics().density), (int) (40 * getResources().getDisplayMetrics().density));
                iconParams.setMarginEnd(8);
                iconView.setLayoutParams(iconParams);
            } else {
                iconView.setVisibility(View.GONE);
            }
            adView.setIconView(iconView);
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            if (iconView.getVisibility() == View.VISIBLE) {
                headerRow.addView(iconView);
            }
            LinearLayout.LayoutParams headlineInRowParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            headlineView.setLayoutParams(headlineInRowParams);
            headerRow.addView(headlineView);
            Button callToAction = new Button(this);
            callToAction.setText(nativeAd.getCallToAction());
            callToAction.setAllCaps(false);
            callToAction.setTextColor(Color.WHITE);
            callToAction.setBackgroundColor(Color.parseColor("#4CAF50"));
            LinearLayout.LayoutParams ctaParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ctaParams.setMargins(0, 8, 0, 0);
            callToAction.setLayoutParams(ctaParams);
            adView.setCallToActionView(callToAction);
            adInternalLayout.addView(headerRow);
            adInternalLayout.addView(bodyView);
            adInternalLayout.addView(mediaView);
            adInternalLayout.addView(callToAction);
            adView.addView(adInternalLayout);
            AdChoicesView adChoicesView = new AdChoicesView(this);
            FrameLayout.LayoutParams adChoicesParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            adChoicesParams.gravity = Gravity.TOP | Gravity.END;
            adChoicesParams.setMargins(0, 4, 4, 0);
            adView.addView(adChoicesView, adChoicesParams);
            adView.setAdChoicesView(adChoicesView);
            adView.setNativeAd(nativeAd);
            adContainer.addView(adView);
        });

        AdListener adListener = new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                // ...
            }
        };
        builder.withAdListener(adListener);

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).setVideoOptions(videoOptions).build();
        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    @Override
    protected void onDestroy() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        // Asegurarse de detener el handler si la actividad se destruye
        sim_handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
