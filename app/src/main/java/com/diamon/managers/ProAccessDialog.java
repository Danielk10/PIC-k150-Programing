package com.diamon.managers;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.diamon.billing.BillingManager;
import com.diamon.publicidad.GestorPublicidad;

/**
 * Diálogo modal para gestionar el Acceso K150 Pro.
 * Ofrece dos vías para desbloquear las herramientas avanzadas:
 * 1. 🎬 Ver un video publicitario para activar el Pase Libre de 12 Horas.
 * 2. 👑 Adquirir la Versión Pro Permanente mediante Google Play Billing.
 */
public class ProAccessDialog {

    private static final int COLOR_BG = 0xFF181824;
    private static final int COLOR_CARD = 0xFF222234;
    private static final int COLOR_ACCENT_CYAN = 0xFF00B0FF;
    private static final int COLOR_ACCENT_GOLD = 0xFFFFB300;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFB0B0C4;
    private static final int COLOR_CHECK = 0xFF00E676;

    public interface OnProUnlockedListener {
        void onProUnlocked();
    }

    /**
     * Muestra el diálogo modal de Acceso K150 Pro.
     *
     * @param activity         Actividad host.
     * @param gestorPublicidad Gestor de anuncios para video recompensado.
     * @param billingManager   Gestor de facturación Google Play Billing.
     * @param proPassManager   Gestor de pase temporal de 12 horas.
     * @param onProUnlocked    Acción a ejecutar si el usuario desbloquea el acceso Pro (opcional).
     */
    public static void show(
            @NonNull Activity activity,
            @Nullable GestorPublicidad gestorPublicidad,
            @Nullable BillingManager billingManager,
            @NonNull ProPassManager proPassManager,
            @Nullable Runnable onProUnlocked) {

        if (activity.isFinishing() || activity.isDestroyed()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        float density = activity.getResources().getDisplayMetrics().density;

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout rootLayout = new LinearLayout(activity);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dp(20, density), dp(20, density), dp(20, density), dp(20, density));
        rootLayout.setBackgroundColor(COLOR_BG);

        // 1. TÍTULO CON ICONO
        TextView titleView = new TextView(activity);
        titleView.setText("⭐ Acceso K150 Pro");
        titleView.setTextSize(22);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(COLOR_ACCENT_GOLD);
        titleView.setGravity(Gravity.CENTER);
        rootLayout.addView(titleView);

        // Subtítulo
        TextView subtitleView = new TextView(activity);
        subtitleView.setText("Desbloquea las herramientas técnicas avanzadas de MicroPro y silicio");
        subtitleView.setTextSize(13);
        subtitleView.setTextColor(COLOR_TEXT_SECONDARY);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setPadding(0, dp(6, density), 0, dp(16, density));
        rootLayout.addView(subtitleView);

        // 2. TARJETA DE BENEFICIOS PRO
        LinearLayout cardLayout = new LinearLayout(activity);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(dp(16, density), dp(16, density), dp(16, density), dp(16, density));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(COLOR_CARD);
        cardBg.setCornerRadius(dp(12, density));
        cardBg.setStroke(dp(1, density), 0xFF35354D);
        cardLayout.setBackground(cardBg);

        String[] features = new String[]{
                "Editor Hexadecimal en Vivo (Live Patching)",
                "Diagnóstico de Hardware & Prueba de Voltajes (VPP 13V / VDD 5V)",
                "Vector de Depuración ICD (24 bits)",
                "Rescate y Edición Manual de OSCCAL",
                "Cero Anuncios en toda la aplicación durante 12 horas"
        };

        for (String feat : features) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4, density), 0, dp(4, density));

            TextView checkView = new TextView(activity);
            checkView.setText("✓ ");
            checkView.setTextSize(15);
            checkView.setTypeface(null, Typeface.BOLD);
            checkView.setTextColor(COLOR_CHECK);

            TextView featView = new TextView(activity);
            featView.setText(feat);
            featView.setTextSize(13);
            featView.setTextColor(COLOR_TEXT_PRIMARY);

            row.addView(checkView);
            row.addView(featView);
            cardLayout.addView(row);
        }
        rootLayout.addView(cardLayout);

        // Espaciador
        View spacer = new View(activity);
        spacer.setMinimumHeight(dp(18, density));
        rootLayout.addView(spacer);

        AlertDialog dialog = builder.setView(scrollView).create();

        // 3. BOTÓN 1: VER VIDEO RECOMPENSADO (PASE 12 HORAS)
        Button btnRewarded = new Button(activity);
        btnRewarded.setText("🎬 Ver Video (Pase Libre de 12 Horas)");
        btnRewarded.setTextSize(14);
        btnRewarded.setTypeface(null, Typeface.BOLD);
        btnRewarded.setTextColor(Color.WHITE);
        GradientDrawable btnRewardBg = new GradientDrawable();
        btnRewardBg.setColor(COLOR_ACCENT_CYAN);
        btnRewardBg.setCornerRadius(dp(8, density));
        btnRewarded.setBackground(btnRewardBg);
        btnRewarded.setPadding(dp(12, density), dp(12, density), dp(12, density), dp(12, density));

        btnRewarded.setOnClickListener(v -> {
            if (gestorPublicidad != null) {
                gestorPublicidad.mostrarRewardedAd(activity, rewardItem -> {
                    proPassManager.activarPase12Horas();
                    Toast.makeText(activity, "⭐ ¡Pase K150 Pro activado por 12 horas!", Toast.LENGTH_LONG).show();
                    if (onProUnlocked != null) {
                        onProUnlocked.run();
                    }
                    dialog.dismiss();
                }, () -> {
                    // Si se cierra sin recompensa
                });
            } else {
                Toast.makeText(activity, "Anuncios no disponibles", Toast.LENGTH_SHORT).show();
            }
        });
        rootLayout.addView(btnRewarded);

        // Espaciador entre botones
        View btnSpacer = new View(activity);
        btnSpacer.setMinimumHeight(dp(10, density));
        rootLayout.addView(btnSpacer);

        // 4. BOTÓN 2: COMPRA PERMANENTE GOOGLE PLAY
        Button btnBuyPro = new Button(activity);
        String priceText = (billingManager != null && billingManager.getFormattedPrice() != null)
                ? " (" + billingManager.getFormattedPrice() + ")"
                : " ($4.99)";
        btnBuyPro.setText("👑 Comprar Versión Pro Permanente" + priceText);
        btnBuyPro.setTextSize(14);
        btnBuyPro.setTypeface(null, Typeface.BOLD);
        btnBuyPro.setTextColor(0xFF1E1E2E);
        GradientDrawable btnBuyBg = new GradientDrawable();
        btnBuyBg.setColor(COLOR_ACCENT_GOLD);
        btnBuyBg.setCornerRadius(dp(8, density));
        btnBuyPro.setBackground(btnBuyBg);
        btnBuyPro.setPadding(dp(12, density), dp(12, density), dp(12, density), dp(12, density));

        btnBuyPro.setOnClickListener(v -> {
            if (billingManager != null) {
                billingManager.launchPurchaseFlow(activity);
                dialog.dismiss();
            } else {
                Toast.makeText(activity, "Facturación de Google Play no disponible", Toast.LENGTH_SHORT).show();
            }
        });
        rootLayout.addView(btnBuyPro);

        // 5. BOTÓN 3: CANCELAR
        Button btnCancel = new Button(activity);
        btnCancel.setText("Cancelar");
        btnCancel.setTextSize(13);
        btnCancel.setTextColor(COLOR_TEXT_SECONDARY);
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setPadding(0, dp(10, density), 0, dp(4, density));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        rootLayout.addView(btnCancel);

        scrollView.addView(rootLayout);
        dialog.show();
    }

    private static int dp(int dp, float density) {
        return Math.round(dp * density);
    }
}
