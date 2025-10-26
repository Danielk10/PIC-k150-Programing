package com.diamon.managers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
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
 * Gestor de dialogos de programacion con publicidad integrada. Muestra el progreso de programacion
 * y anuncios nativos de Google Ads.
 */
public class ProgrammingDialogManager {

    private final Context context;
    private PopupWindow popupWindow;
    private NativeAd nativeAd;

    // Referencias a elementos del dialogo
    private TextView titleTextView;
    private TextView descriptionTextView;
    private ProgressBar statusProgressBar;
    private ImageView statusResultIcon;
    private Button actionButton;

    // Callbacks
    private Runnable onProgrammingStartCallback;
    private Runnable onDismissCallback;

    /**
     * Constructor del gestor de dialogos
     *
     * @param context Contexto de la aplicacion
     */
    public ProgrammingDialogManager(Context context) {
        this.context = context;
    }

    /**
     * Muestra el dialogo de programacion con anuncio
     *
     * @param onStart Callback al iniciar programacion
     * @param onDismiss Callback al cerrar dialogo
     */
    public void showProgrammingDialog(Runnable onStart, Runnable onDismiss) {
        this.onProgrammingStartCallback = onStart;
        this.onDismissCallback = onDismiss;

        createPopupWindow();

        // Iniciar programacion en callback
        if (onProgrammingStartCallback != null) {
            onProgrammingStartCallback.run();
        }
    }

    /** Crea y muestra el PopupWindow con el dialogo */
    private void createPopupWindow() {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        int popupHeight = (int) (screenHeight * 0.85);

        LinearLayout popupContainer = createPopupContainer(popupHeight);

        popupWindow = new PopupWindow(popupContainer, (int) (screenWidth * 0.9), popupHeight, true);

        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(0);

        View rootView = ((android.app.Activity) context).findViewById(android.R.id.content);
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);

        applyShowAnimation(popupContainer);
    }

    /**
     * Crea el contenedor principal del popup
     *
     * @param height Altura del popup
     * @return LinearLayout configurado
     */
    private LinearLayout createPopupContainer(int height) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        container.setBackgroundColor(Color.WHITE);
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Contenido superior
        LinearLayout topContent = createTopContent();
        container.addView(topContent);

        // Divisor
        View divider = new View(context);
        divider.setBackgroundColor(Color.LTGRAY);
        container.addView(
                divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));

        // Contenedor de anuncio
        FrameLayout adContainer = createAdContainer();
        container.addView(adContainer);
        loadNativeAd(adContainer);

        // Boton de accion
        LinearLayout buttonContainer = createButtonContainer();
        container.addView(buttonContainer);

        return container;
    }

    /**
     * Crea el contenido superior con titulo, icono y descripcion
     *
     * @return LinearLayout con contenido superior
     */
    private LinearLayout createTopContent() {
        LinearLayout topContent = new LinearLayout(context);
        topContent.setOrientation(LinearLayout.VERTICAL);
        topContent.setGravity(Gravity.CENTER);
        topContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Titulo
        titleTextView = new TextView(context);
        titleTextView.setText(R.string.grabando_pic);
        titleTextView.setTextSize(22);
        titleTextView.setTextColor(Color.BLACK);
        titleTextView.setGravity(Gravity.CENTER);
        topContent.addView(titleTextView);

        // Contenedor de indicador de estado
        FrameLayout statusContainer = new FrameLayout(context);
        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dpToPx(24), 0, dpToPx(24));

        statusProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyle);
        statusProgressBar.setVisibility(View.VISIBLE);
        statusContainer.addView(statusProgressBar);

        statusResultIcon = new ImageView(context);
        statusResultIcon.setVisibility(View.GONE);
        statusContainer.addView(statusResultIcon);

        topContent.addView(statusContainer, statusParams);

        // Descripcion
        descriptionTextView = new TextView(context);
        descriptionTextView.setText(R.string.espere_grabacion_pic);
        descriptionTextView.setTextSize(16);
        descriptionTextView.setTextColor(Color.DKGRAY);
        descriptionTextView.setGravity(Gravity.CENTER);
        topContent.addView(descriptionTextView);

        return topContent;
    }

    /**
     * Crea el contenedor para anuncios
     *
     * @return FrameLayout para anuncios
     */
    private FrameLayout createAdContainer() {
        FrameLayout adContainer = new FrameLayout(context);
        LinearLayout.LayoutParams adParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        adContainer.setLayoutParams(adParams);
        adContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        adContainer.setMinimumHeight(dpToPx(250));
        return adContainer;
    }

    /**
     * Crea el contenedor del boton de accion
     *
     * @return LinearLayout con boton
     */
    private LinearLayout createButtonContainer() {
        LinearLayout buttonContainer = new LinearLayout(context);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        buttonContainer.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        actionButton = new Button(context);
        actionButton.setText(R.string.cancelar);
        actionButton.setTextColor(Color.WHITE);
        actionButton.setBackgroundResource(R.drawable.button_background_red);
        actionButton.setPadding(dpToPx(50), dpToPx(20), dpToPx(50), dpToPx(20));

        actionButton.setOnClickListener(v -> dismissWithAnimation());

        buttonContainer.addView(actionButton);
        return buttonContainer;
    }

    /**
     * Actualiza el dialogo con el resultado de la programacion
     *
     * @param success true si fue exitosa, false en caso contrario
     */
    public void updateProgrammingResult(boolean success) {
        if (statusProgressBar != null) {
            statusProgressBar.setVisibility(View.GONE);
        }

        if (statusResultIcon != null) {
            statusResultIcon.setVisibility(View.VISIBLE);
        }

        if (popupWindow != null) {
            popupWindow.setOutsideTouchable(true);
        }

        if (success) {
            showSuccessState();
        } else {
            showFailureState();
        }

        updateActionButton();
    }

    /** Muestra estado de exito */
    private void showSuccessState() {
        titleTextView.setText(R.string.grabacion_completada_pic);
        descriptionTextView.setText(R.string.grabacion_correcta_pic);
        statusResultIcon.setImageResource(R.drawable.ic_status_success);

        Drawable successDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
        DrawableCompat.setTint(successDrawable, Color.parseColor("#4CAF50"));
    }

    /** Muestra estado de fallo */
    private void showFailureState() {
        titleTextView.setText(R.string.fallo_grabacion_pic);
        descriptionTextView.setText(R.string.proceso_no_completado);
        statusResultIcon.setImageResource(R.drawable.ic_status_failure);

        Drawable failureDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
        DrawableCompat.setTint(failureDrawable, Color.parseColor("#D32F2F"));
    }

    /** Actualiza el boton de accion al estado final */
    private void updateActionButton() {
        actionButton.setText(R.string.aceptar);
        actionButton.setBackgroundResource(R.drawable.button_background_blue);
        actionButton.setOnClickListener(v -> dismissWithAnimation());
    }

    /**
     * Carga un anuncio nativo en el contenedor
     *
     * @param adContainer Contenedor para el anuncio
     */
    private void loadNativeAd(FrameLayout adContainer) {
        AdLoader.Builder builder =
                new AdLoader.Builder(context, "ca-app-pub-5141499161332805/4642845838");

        builder.forNativeAd(
                ad -> {
                    if (nativeAd != null) {
                        nativeAd.destroy();
                    }
                    nativeAd = ad;
                    populateNativeAdView(adContainer, ad);
                });

        builder.withAdListener(
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        adContainer.setVisibility(View.GONE);
                    }
                });

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions =
                new NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setVideoOptions(videoOptions)
                        .build();

        builder.withNativeAdOptions(adOptions);
        builder.build().loadAd(new AdRequest.Builder().build());
    }

    /**
     * Puebla la vista del anuncio nativo con datos
     *
     * @param container Contenedor del anuncio
     * @param ad Anuncio nativo cargado
     */
    private void populateNativeAdView(FrameLayout container, NativeAd ad) {
        container.removeAllViews();

        NativeAdView adView = new NativeAdView(context);
        adView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        // Crear vista del anuncio (simplificado)
        LinearLayout adLayout = new LinearLayout(context);
        adLayout.setOrientation(LinearLayout.VERTICAL);
        adLayout.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
        adLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Titulo
        TextView headline = new TextView(context);
        headline.setText(ad.getHeadline());
        headline.setTextSize(16);
        headline.setTextColor(Color.BLACK);
        adLayout.addView(headline);

        // Cuerpo
        TextView body = new TextView(context);
        body.setText(ad.getBody());
        body.setTextSize(14);
        body.setTextColor(Color.DKGRAY);
        adLayout.addView(body);

        // Media view
        MediaView mediaView = new MediaView(context);
        LinearLayout.LayoutParams mediaParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        mediaView.setLayoutParams(mediaParams);
        adLayout.addView(mediaView);

        // Boton de accion
        Button cta = new Button(context);
        cta.setText(ad.getCallToAction());
        cta.setTextColor(Color.WHITE);
        cta.setBackgroundColor(Color.parseColor("#4CAF50"));
        adLayout.addView(cta);

        adView.addView(adLayout);
        adView.setHeadlineView(headline);
        adView.setBodyView(body);
        adView.setMediaView(mediaView);
        adView.setCallToActionView(cta);
        adView.setNativeAd(ad);

        container.addView(adView);
    }

    /**
     * Aplica animacion de entrada al popup
     *
     * @param view Vista a animar
     */
    private void applyShowAnimation(View view) {
        view.setScaleY(0);
        view.setPivotY(0);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.setInterpolator(new BounceInterpolator());
        animator.addUpdateListener(
                animation -> {
                    float value = (float) animation.getAnimatedValue();
                    view.setScaleY(value);
                });
        animator.start();
    }

    /** Cierra el dialogo con animacion */
    private void dismissWithAnimation() {
        if (popupWindow == null || !popupWindow.isShowing()) {
            return;
        }

        View popupView = popupWindow.getContentView();
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        float targetY = metrics.heightPixels - popupView.getTop();

        ValueAnimator animator = ValueAnimator.ofFloat(0, targetY);
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
                        dismiss();
                    }
                });

        animator.start();
    }

    /** Cierra el dialogo y libera recursos */
    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }

        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }

    /**
     * Convierte dp a pixeles
     *
     * @param dp Valor en dp
     * @return Valor en pixeles
     */
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
