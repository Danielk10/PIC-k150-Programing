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
import android.view.ViewGroup;
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

/**
 * Gestor de dialogos de programacion con publicidad integrada. Muestra el
 * progreso de programacion
 * y anuncios nativos de Google Ads centralizados.
 */
public class ProgrammingDialogManager {

    private final Context context;
    private PopupWindow popupWindow;

    private TextView titleTextView;
    private TextView descriptionTextView;
    private ProgressBar statusProgressBar;
    private ImageView statusResultIcon;
    private Button actionButton;

    private Runnable onProgrammingStartCallback;
    private Runnable onDismissCallback;

    public ProgrammingDialogManager(Context context) {
        this.context = context;
    }

    public void showProgrammingDialog(Runnable onStart, Runnable onDismiss) {
        this.onProgrammingStartCallback = onStart;
        this.onDismissCallback = onDismiss;

        createPopupWindow();

        if (onProgrammingStartCallback != null) {
            onProgrammingStartCallback.run();
        }
    }

    public void preloadAd() {
        // La precarga ahora se maneja centralizadamente en MostrarPublicidad
    }

    private void createPopupWindow() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        int screenHeight = displayMetrics.heightPixels;

        LinearLayout popupContainer = createPopupContainer(screenHeight);

        popupWindow = new PopupWindow(
                popupContainer,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(24);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(R.style.PopupAnimation);

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

    private LinearLayout createPopupContainer(int height) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(16));
        shape.setColor(Color.parseColor("#505060"));
        shape.setStroke(dpToPx(2), Color.parseColor("#3A3A4E"));
        container.setBackground(shape);
        container.setElevation(16f);
        container.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(8));

        container.addView(createTopContent());

        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#3A3A4E"));
        container.addView(
                divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));

        FrameLayout adContainer = createAdContainer();
        container.addView(adContainer);

        if (context instanceof com.diamon.pic.MainActivity) {
            com.diamon.pic.MainActivity activity = (com.diamon.pic.MainActivity) context;
            activity.getPublicidad().mostrarNativeAd(com.diamon.publicidad.MostrarPublicidad.KEY_NATIVE_PROGRAMMING,
                    adContainer);
        }

        container.addView(createButtonContainer());

        return container;
    }

    private LinearLayout createTopContent() {
        LinearLayout topContent = new LinearLayout(context);
        topContent.setOrientation(LinearLayout.VERTICAL);
        topContent.setGravity(Gravity.CENTER);
        topContent.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(2));

        titleTextView = new TextView(context);
        titleTextView.setText(R.string.grabando_pic);
        titleTextView.setTextSize(16);
        titleTextView.setTextColor(Color.WHITE);
        titleTextView.setGravity(Gravity.CENTER);
        topContent.addView(titleTextView);

        FrameLayout statusContainer = new FrameLayout(context);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dpToPx(4), 0, dpToPx(4));

        statusProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyle);
        statusProgressBar.getIndeterminateDrawable().setColorFilter(
                new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        LinearLayout.LayoutParams progressSizeParams = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        statusProgressBar.setLayoutParams(progressSizeParams);
        statusProgressBar.setVisibility(View.VISIBLE);
        statusContainer.addView(statusProgressBar);

        statusResultIcon = new ImageView(context);
        statusResultIcon.setVisibility(View.GONE);
        statusContainer.addView(statusResultIcon);

        topContent.addView(statusContainer, statusParams);

        descriptionTextView = new TextView(context);
        descriptionTextView.setText(R.string.espere_grabacion_pic);
        descriptionTextView.setTextSize(14);
        descriptionTextView.setTextColor(Color.LTGRAY);
        descriptionTextView.setGravity(Gravity.CENTER);
        topContent.addView(descriptionTextView);

        return topContent;
    }

    private FrameLayout createAdContainer() {
        FrameLayout adContainer = new FrameLayout(context);
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0,
                1.0f);
        adContainer.setLayoutParams(adParams);
        // Sin padding para que los assets del anuncio no se salgan del NativeAdView
        adContainer.setClipChildren(true);
        adContainer.setClipToPadding(true);
        adContainer.setMinimumHeight(dpToPx(250));
        return adContainer;
    }

    private LinearLayout createButtonContainer() {
        LinearLayout buttonContainer = new LinearLayout(context);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        buttonContainer.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(0));

        actionButton = new Button(context);
        actionButton.setText(R.string.cancelar);
        actionButton.setTextColor(Color.WHITE);
        actionButton.setBackgroundResource(R.drawable.button_background_red);
        actionButton.setPadding(dpToPx(50), dpToPx(10), dpToPx(50), dpToPx(10));
        actionButton.setOnClickListener(v -> dismissWithAnimation());

        buttonContainer.addView(actionButton);
        return buttonContainer;
    }

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

    private void showSuccessState() {
        titleTextView.setText(R.string.grabacion_completada_pic);
        descriptionTextView.setText(R.string.grabacion_correcta_pic);
        statusResultIcon.setImageResource(R.drawable.ic_status_success);
        Drawable successDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
        DrawableCompat.setTint(successDrawable, Color.parseColor("#4CAF50"));
    }

    private void showFailureState() {
        titleTextView.setText(R.string.fallo_grabacion_pic);
        descriptionTextView.setText(R.string.proceso_no_completado);
        statusResultIcon.setImageResource(R.drawable.ic_status_failure);
        Drawable failureDrawable = DrawableCompat.wrap(statusResultIcon.getDrawable());
        DrawableCompat.setTint(failureDrawable, Color.parseColor("#D32F2F"));
    }

    private void updateActionButton() {
        actionButton.setText(R.string.aceptar);
        actionButton.setBackgroundResource(R.drawable.button_background_blue);
        actionButton.setOnClickListener(v -> dismissWithAnimation());
    }

    private void applyShowAnimation(View view) {
        view.setScaleY(0);
        view.setPivotY(0);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.setInterpolator(new BounceInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            view.setScaleY(value);
        });
        animator.start();
    }

    private void dismissWithAnimation() {
        if (popupWindow == null || !popupWindow.isShowing()) {
            return;
        }

        View popupView = popupWindow.getContentView();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float targetY = metrics.heightPixels - popupView.getTop();

        ValueAnimator animator = ValueAnimator.ofFloat(0, targetY);
        animator.setDuration(600);
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
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
