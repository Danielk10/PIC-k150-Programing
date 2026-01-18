package com.diamon.managers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
 * Gestor de dialogos de programacion con publicidad integrada. Muestra el
 * progreso de programacion
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
     * @param onStart   Callback al iniciar programacion
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
        // CORREGIDO: Evitar usar windowManager.getDefaultDisplay() (Deprecado)
        // Usamos los recursos del contexto que ya tienen las metricas ajustadas
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        int popupHeight = (int) (screenHeight * 0.85);

        LinearLayout popupContainer = createPopupContainer(popupHeight);

        // CORREGIDO: Ancho 92% para consistencia
        popupWindow = new PopupWindow(popupContainer, (int) (screenWidth * 0.92), popupHeight, true);

        // IMPORTANTE: Fondo transparente para ver bordes redondeados y sombra
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        // Elevacion para sombra
        popupWindow.setElevation(24);

        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(0);

        // CORREGIDO: Usar getWindow().getDecorView() para compatibilidad con
        // edge-to-edge
        // En Android 15 con edge-to-edge, android.R.id.content puede tener dimensiones
        // incorrectas
        View rootView;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            rootView = activity.getWindow().getDecorView();
        } else {
            rootView = ((android.app.Activity) context).findViewById(android.R.id.content);
        }

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
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(16));
        shape.setColor(Color.parseColor("#505060")); // Nuevo color de fondo
        shape.setStroke(dpToPx(2), Color.parseColor("#3A3A4E")); // Borde
        container.setBackground(shape);
        // Elevacion del contenedor
        container.setElevation(16f);
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Contenido superior
        LinearLayout topContent = createTopContent();
        container.addView(topContent);

        // Divisor
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#3A3A4E")); // Color borde
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
        titleTextView.setText(R.string.grabando_pic);
        titleTextView.setTextSize(22);
        titleTextView.setTextColor(Color.WHITE); // Texto blanco
        titleTextView.setGravity(Gravity.CENTER);
        topContent.addView(titleTextView);

        // Contenedor de indicador de estado
        FrameLayout statusContainer = new FrameLayout(context);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dpToPx(24), 0, dpToPx(24));

        statusProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyle);
        // CORREGIDO: Usar PorterDuffColorFilter para evitar API deprecada
        // (setColorFilter(int, Mode))
        statusProgressBar.getIndeterminateDrawable().setColorFilter(
                new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
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
        descriptionTextView.setTextColor(Color.LTGRAY); // Texto gris claro
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
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0,
                1.0f);
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
        AdLoader.Builder builder = new AdLoader.Builder(context, "ca-app-pub-5141499161332805/2642812533");

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
                        // Mostrar placeholder cuando el anuncio no carga
                        // Esto es especialmente util en emuladores donde Google Play Services
                        // puede no estar completamente funcional
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
     * Puebla la vista del anuncio nativo con datos
     *
     * @param container Contenedor del anuncio
     * @param ad        Anuncio nativo cargado
     */
    private void populateNativeAdView(FrameLayout container, NativeAd ad) {
        container.removeAllViews();

        // Inflar el diseno XML personalizado
        NativeAdView adView = (NativeAdView) android.view.LayoutInflater.from(context)
                .inflate(R.layout.layout_native_ad, null);

        // Ajustar parametros de layout
        adView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // Vincular vistas
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        // Poblar vistas
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

        // CORREGIDO: Usar recursos del contexto para metricas
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

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
     * Muestra un placeholder cuando el anuncio no se puede cargar.
     * Esto mantiene el dialogo visible y con buen aspecto incluso cuando
     * el anuncio no esta disponible (comun en emuladores).
     *
     * @param container Contenedor del anuncio
     */
    private void showAdPlaceholder(FrameLayout container) {
        container.removeAllViews();
        container.setVisibility(View.VISIBLE);

        LinearLayout placeholderLayout = new LinearLayout(context);
        placeholderLayout.setOrientation(LinearLayout.VERTICAL);
        placeholderLayout.setGravity(Gravity.CENTER);
        placeholderLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        placeholderLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Icono de informacion
        ImageView icon = new ImageView(context);
        icon.setImageResource(android.R.drawable.ic_dialog_info);
        icon.setColorFilter(Color.parseColor("#9E9E9E"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        iconParams.setMargins(0, 0, 0, dpToPx(8));
        placeholderLayout.addView(icon, iconParams);

        // Texto informativo
        TextView placeholderText = new TextView(context);
        placeholderText.setText("Anuncio no disponible");
        placeholderText.setTextSize(14);
        placeholderText.setTextColor(Color.parseColor("#757575"));
        placeholderText.setGravity(Gravity.CENTER);
        placeholderLayout.addView(placeholderText);

        container.addView(
                placeholderLayout,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
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
