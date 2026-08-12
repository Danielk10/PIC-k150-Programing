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
    private TextView progressPercentTextView;
    private PicAnimationView picAnimView;
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

        if (!isActivityValid()) {
            android.util.Log.w("ProgrammingDialogManager", "showProgrammingDialog: Activity no valida, ignorando");
            return;
        }

        createPopupWindow();

        if (onProgrammingStartCallback != null) {
            onProgrammingStartCallback.run();
        }
    }

    public void preloadAd() {
        // La precarga ahora se maneja centralizadamente en GestorPublicidad
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

        if (!isActivityValid()) {
            android.util.Log.w("ProgrammingDialogManager", "createPopupWindow: Activity no valida, abortando show");
            return;
        }

        View rootView;
        android.app.Activity activity = (android.app.Activity) context;
        rootView = activity.getWindow().getDecorView();

        try {
            popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);
            applyShowAnimation(popupContainer);
        } catch (android.view.WindowManager.BadTokenException e) {
            android.util.Log.e("ProgrammingDialogManager", "BadTokenException al mostrar popup: " + e.getMessage());
        }
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
            activity.getPublicidad().mostrarNativeAd(com.diamon.publicidad.GestorPublicidad.KEY_NATIVE_PROGRAMMING,
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
        titleTextView.setTextSize(18);
        titleTextView.setTextColor(Color.WHITE);
        titleTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTextView.setGravity(Gravity.CENTER);
        topContent.addView(titleTextView);

        progressPercentTextView = new TextView(context);
        progressPercentTextView.setText("0%");
        progressPercentTextView.setTextSize(24);
        progressPercentTextView.setTextColor(Color.parseColor("#00E676"));
        progressPercentTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        progressPercentTextView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        percentParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
        topContent.addView(progressPercentTextView, percentParams);

        picAnimView = new PicAnimationView(context);
        LinearLayout.LayoutParams animParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(240));
        animParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        topContent.addView(picAnimView, animParams);

        FrameLayout statusContainer = new FrameLayout(context);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dpToPx(6), 0, dpToPx(6));

        statusProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        statusProgressBar.setProgressDrawable(context.getDrawable(R.drawable.progress_bar_custom));
        statusProgressBar.setIndeterminate(false);
        statusProgressBar.setMax(100);
        statusProgressBar.setProgress(0);
        FrameLayout.LayoutParams progressSizeParams = new FrameLayout.LayoutParams(dpToPx(260), dpToPx(12));
        progressSizeParams.gravity = Gravity.CENTER;
        statusProgressBar.setLayoutParams(progressSizeParams);
        statusProgressBar.setVisibility(View.VISIBLE);
        statusContainer.addView(statusProgressBar);

        statusResultIcon = new ImageView(context);
        statusResultIcon.setVisibility(View.GONE);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dpToPx(45), dpToPx(45));
        iconParams.gravity = Gravity.CENTER;
        statusResultIcon.setLayoutParams(iconParams);
        statusContainer.addView(statusResultIcon);

        topContent.addView(statusContainer, statusParams);

        descriptionTextView = new TextView(context);
        descriptionTextView.setText(R.string.espere_grabacion_pic);
        descriptionTextView.setTextSize(14);
        descriptionTextView.setTextColor(Color.parseColor("#CCCCCC"));
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

    public void updateProgress(final int progress, final String message) {
        if (!isActivityValid()) return;

        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (statusProgressBar != null) {
                    statusProgressBar.setProgress(progress);
                }
                if (progressPercentTextView != null) {
                    progressPercentTextView.setVisibility(View.VISIBLE);
                    progressPercentTextView.setText(progress + "%");
                    
                    // Transition color from green (#00E676) to cyan (#00B0FF)
                    float fraction = (float) progress / 100f;
                    int color = interpolateColor(Color.parseColor("#00E676"), Color.parseColor("#00B0FF"), fraction);
                    progressPercentTextView.setTextColor(color);
                    
                    // Micro-animation: pulse scale slightly on update
                    progressPercentTextView.setScaleX(1.15f);
                    progressPercentTextView.setScaleY(1.15f);
                    progressPercentTextView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                }
                if (descriptionTextView != null) {
                    descriptionTextView.setText(message);
                }
            });
        }
    }

    private int interpolateColor(int colorStart, int colorEnd, float fraction) {
        float[] startHsv = new float[3];
        float[] endHsv = new float[3];
        Color.colorToHSV(colorStart, startHsv);
        Color.colorToHSV(colorEnd, endHsv);
        float[] outHsv = new float[3];
        outHsv[0] = startHsv[0] + (endHsv[0] - startHsv[0]) * fraction;
        outHsv[1] = startHsv[1] + (endHsv[1] - startHsv[1]) * fraction;
        outHsv[2] = startHsv[2] + (endHsv[2] - startHsv[2]) * fraction;
        return Color.HSVToColor(outHsv);
    }

    public void updateProgrammingResult(boolean success) {
        if (picAnimView != null) {
            picAnimView.setProgramming(false);
            picAnimView.setVisibility(View.GONE);
        }

        if (progressPercentTextView != null) {
            progressPercentTextView.setVisibility(View.GONE);
        }

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
            try {
                popupWindow.dismiss();
            } catch (IllegalArgumentException e) {
                android.util.Log.w("ProgrammingDialogManager", "dismiss: View no attached: " + e.getMessage());
            } catch (Exception e) {
                android.util.Log.w("ProgrammingDialogManager", "dismiss: Error inesperado: " + e.getMessage());
            }
        }

        if (onDismissCallback != null) {
            onDismissCallback.run();
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

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
