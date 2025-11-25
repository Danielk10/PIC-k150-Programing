package com.diamon.utilidades;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class PantallaCompleta {

    private final AppCompatActivity actividad;

    public PantallaCompleta(AppCompatActivity actividad) {
        this.actividad = actividad;
    }

    /**
     * Habilita edge-to-edge para Android 15+
     * Configura la ventana para que el contenido se dibuje detrás de las barras del
     * sistema
     */
    public void habilitarEdgeToEdge() {
        Window window = actividad.getWindow();

        // Habilitar edge-to-edge usando WindowCompat
        WindowCompat.setDecorFitsSystemWindows(window, false);
    }

    /**
     * Aplica window insets a una vista específica
     * Esto asegura que el contenido no quede oculto por las barras del sistema
     * 
     * @param view La vista raíz a la que aplicar los insets
     */
    public void aplicarWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Aplicar padding para que el contenido no quede debajo de las barras del
            // sistema
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    // Colocar en Pantalla completa (deprecado - usar habilitarEdgeToEdge)
    @Deprecated
    public void pantallaCompleta() {
        actividad
                .getWindow()
                .setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * Ocultar los botones virtuales de navegación
     * Usa WindowInsetsController para API 30+ y flags legacy para versiones
     * anteriores
     */
    public void ocultarBotonesVirtuales() {
        Window window = actividad.getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Usar WindowInsetsController para API 30+ (Android 11+)
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // Ocultar barras del sistema
                controller.hide(android.view.WindowInsets.Type.systemBars());
                // Configurar comportamiento inmersivo (las barras reaparecen con swipe y se
                // ocultan automáticamente)
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Usar flags legacy para API 21-29
            window.getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    /**
     * Configuración completa de edge-to-edge con ocultación de barras
     * Combina edge-to-edge con modo inmersivo
     */
    public void configurarEdgeToEdgeCompleto() {
        habilitarEdgeToEdge();
        ocultarBotonesVirtuales();
    }
}