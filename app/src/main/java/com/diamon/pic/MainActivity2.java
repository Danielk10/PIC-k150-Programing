package com.diamon.pic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoOptions; // Importar VideoOptions
import com.google.android.gms.ads.nativead.AdChoicesView;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

public class MainActivity2 extends AppCompatActivity {

    private PopupWindow popupWindow;
    private NativeAd nativeAd;

    // screenHeight se obtendrá dinámicamente donde se necesite o se puede mantener si se actualiza
    // en config changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, initializationStatus -> {});

        RelativeLayout mainLayout = new RelativeLayout(this);
        mainLayout.setLayoutParams(
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));
        mainLayout.setBackgroundColor(Color.parseColor("#EEEEEE"));

        Button showPopupButton = new Button(this);
        showPopupButton.setText("Mostrar Anuncio");
        showPopupButton.setId(View.generateViewId());

        RelativeLayout.LayoutParams buttonParams =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
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
        int currentScreenHeight = displayMetrics.heightPixels; // Usar un valor actual

        int popupHeight = (int) (currentScreenHeight * 0.7);

        LinearLayout popupContainer = new LinearLayout(this);
        popupContainer.setOrientation(LinearLayout.VERTICAL);
        popupContainer.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, popupHeight));

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(40f);
        shape.setColor(Color.WHITE);
        popupContainer.setBackground(shape);

        LinearLayout topContent = new LinearLayout(this);
        topContent.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams topParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.8f);
        topContent.setLayoutParams(topParams);
        topContent.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Contenido Premium");
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        topContent.addView(title);

        TextView description = new TextView(this);
        description.setText(
                "Desbloquea todas las funciones con nuestra versión premium o continúa con la versión gratuita con anuncios.");
        description.setTextSize(16);
        description.setTextColor(Color.DKGRAY);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 16, 0, 16);
        topContent.addView(description);

        popupContainer.addView(topContent);

        View divider = new View(this);
        divider.setBackgroundColor(Color.LTGRAY);
        popupContainer.addView(
                divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));

        FrameLayout adContainer = new FrameLayout(this);
        LinearLayout.LayoutParams adParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.2f);
        adContainer.setLayoutParams(adParams);
        adContainer.setPadding(8, 8, 8, 8);
        adContainer.setId(View.generateViewId());
        popupContainer.addView(adContainer);

        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        buttonContainer.setPadding(16, 16, 16, 16);

        Button acceptButton = new Button(this);
        acceptButton.setText("Aceptar");
        LinearLayout.LayoutParams buttonLayoutParam =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        buttonLayoutParam.setMarginEnd(8);
        acceptButton.setLayoutParams(buttonLayoutParam);
        acceptButton.setOnClickListener(
                v -> {
                    Toast.makeText(this, "Anuncio aceptado", Toast.LENGTH_SHORT).show();
                    dismissPopupWithSlideDown();
                });

        Button closeButton = new Button(this);
        closeButton.setText("Cerrar");
        LinearLayout.LayoutParams buttonLayoutParam2 =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        buttonLayoutParam2.setMarginStart(8);
        closeButton.setLayoutParams(buttonLayoutParam2);
        closeButton.setOnClickListener(v -> dismissPopupWithSlideDown());

        buttonContainer.addView(acceptButton);
        buttonContainer.addView(closeButton);
        popupContainer.addView(buttonContainer);

        popupWindow = new PopupWindow(popupContainer, (int) (screenWidth * 0.9), popupHeight, true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(0);

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        applyCustomAnimation(popupContainer);
        loadNativeAd(adContainer);
    }

    private void dismissPopupWithSlideDown() {
        if (popupWindow != null && popupWindow.isShowing()) {
            final View popupView = popupWindow.getContentView();

            // Obtener la altura actual de la pantalla para el cálculo
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int currentScreenHeight = displayMetrics.heightPixels;

            // La propiedad translationY es relativa a la posición original de la vista.
            // Queremos que la parte superior de la vista (popupView.getTop() +
            // popupView.getTranslationY())
            // alcance la parte inferior de la pantalla (currentScreenHeight).
            // Entonces, el valor final de translationY debe ser: currentScreenHeight -
            // popupView.getTop().
            // Esto moverá la parte superior del popup hasta el borde inferior de la pantalla,
            // haciéndolo desaparecer completamente hacia abajo.
            float startTranslationY =
                    popupView
                            .getTranslationY(); // Normalmente 0 si no hay otras traslaciones
                                                // activas
            float targetTranslationY = currentScreenHeight - popupView.getTop();

            ValueAnimator animator = ValueAnimator.ofFloat(startTranslationY, targetTranslationY);
            animator.setDuration(600);
            animator.setInterpolator(new AccelerateInterpolator(1.5f));

            animator.addUpdateListener(
                    animation -> {
                        float value = (float) animation.getAnimatedValue();
                        popupView.setTranslationY(value);
                    });

            animator.addListener(
                    new AnimatorListenerAdapter() {
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
        scaleAnimator.addUpdateListener(
                animation -> {
                    float value = (float) animation.getAnimatedValue();
                    popupView.setScaleY(value);
                });
        scaleAnimator.start();

        popupWindow.setTouchInterceptor(
                (v, event) -> {
                    if (!isPointInsideView(
                            event.getRawX(), event.getRawY(), popupWindow.getContentView())) {
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
        AdLoader.Builder builder =
                new AdLoader.Builder(
                        this, "ca-app-pub-5141499161332805/4642845838"); // ID de prueba

        builder.forNativeAd(
                loadedNativeAd -> {
                    adContainer.removeAllViews();
                    if (this.nativeAd != null) {
                        this.nativeAd.destroy();
                    }
                    this.nativeAd = loadedNativeAd;

                    NativeAdView adView = new NativeAdView(this);
                    adView.setLayoutParams(
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT));

                    LinearLayout adInternalLayout = new LinearLayout(this);
                    adInternalLayout.setOrientation(LinearLayout.VERTICAL);
                    adInternalLayout.setLayoutParams(
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT));
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
                    LinearLayout.LayoutParams adBadgeParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    adBadgeParams.setMargins(0, 0, 0, 8);
                    adInternalLayout.addView(adBadge, adBadgeParams);

                    MediaView mediaView = new MediaView(this);
                    LinearLayout.LayoutParams mediaParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
                    mediaParams.gravity = Gravity.CENTER_HORIZONTAL;
                    // Ajustar la altura mínima para videos si es necesario, por ejemplo:
                    mediaParams.height =
                            (int)
                                    (200
                                            * getResources()
                                                    .getDisplayMetrics()
                                                    .density); // Altura mínima de 200dp
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
                        LinearLayout.LayoutParams iconParams =
                                new LinearLayout.LayoutParams(
                                        (int) (40 * getResources().getDisplayMetrics().density),
                                        (int) (40 * getResources().getDisplayMetrics().density));
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
                    // Hacer que el headlineView ocupe el espacio restante en la fila
                    LinearLayout.LayoutParams headlineInRowParams =
                            new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    headlineView.setLayoutParams(headlineInRowParams);
                    headerRow.addView(headlineView);

                    Button callToAction = new Button(this);
                    callToAction.setText(nativeAd.getCallToAction());
                    callToAction.setAllCaps(false);
                    callToAction.setTextColor(Color.WHITE);
                    callToAction.setBackgroundColor(Color.parseColor("#4CAF50"));
                    LinearLayout.LayoutParams ctaParams =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                    ctaParams.setMargins(0, 8, 0, 0);
                    callToAction.setLayoutParams(ctaParams);
                    adView.setCallToActionView(callToAction);

                    adInternalLayout.addView(headerRow);
                    adInternalLayout.addView(bodyView);
                    adInternalLayout.addView(mediaView);
                    adInternalLayout.addView(callToAction);

                    adView.addView(adInternalLayout);

                    AdChoicesView adChoicesView = new AdChoicesView(this);
                    FrameLayout.LayoutParams adChoicesParams =
                            new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT);
                    adChoicesParams.gravity = Gravity.TOP | Gravity.END;
                    adChoicesParams.setMargins(0, 4, 4, 0);
                    adView.addView(adChoicesView, adChoicesParams);
                    adView.setAdChoicesView(adChoicesView);

                    adView.setNativeAd(nativeAd);
                    adContainer.addView(adView);
                });

        AdListener adListener =
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        adContainer.removeAllViews();
                        TextView errorText = new TextView(MainActivity2.this);
                        errorText.setText("Error al cargar anuncio: " + adError.getMessage());
                        errorText.setTextColor(Color.RED);
                        errorText.setGravity(Gravity.CENTER);
                        adContainer.addView(errorText);
                        Toast.makeText(
                                        MainActivity2.this,
                                        "Fallo al cargar anuncio: " + adError.getMessage(),
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                };
        builder.withAdListener(adListener);

        // --- Configuración de Video para Anuncios Nativos ---
        VideoOptions videoOptions =
                new VideoOptions.Builder()
                        .setStartMuted(true) // Recomendado: iniciar videos silenciados
                        .build();

        NativeAdOptions adOptions =
                new NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setVideoOptions(videoOptions) // Aplicar las opciones de video
                        .build();
        builder.withNativeAdOptions(adOptions);
        // --- Fin Configuración de Video ---

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
        super.onDestroy();
    }
}
