package com.diamon.utilidades;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utilidad para configurar pantalla completa y Edge-to-Edge.
 * 
 * Optimizado para Android 15 (SDK 35):
 * - Usa EdgeToEdge.enable() de AndroidX Activity
 * - Maneja WindowInsets correctamente
 * - Proporciona retrocompatibilidad con versiones anteriores
 * 
 * Nota: En Android 15+, las apps se muestran edge-to-edge por defecto.
 * setStatusBarColor y setNavigationBarColor están deprecados y no tienen
 * efecto.
 */
public class PantallaCompleta {

    private final AppCompatActivity actividad;

    public PantallaCompleta(AppCompatActivity actividad) {
        this.actividad = actividad;
    }

    /**
     * Habilita Edge-to-Edge usando la API oficial de AndroidX.
     * 
     * Esta es la forma recomendada para Android 15+ y proporciona
     * retrocompatibilidad automática con versiones anteriores.
     * 
     * Debe llamarse ANTES de setContentView().
     */
    public void habilitarEdgeToEdge() {
        try {
            // Usar la API de EdgeToEdge de AndroidX Activity
            // Esto configura automáticamente:
            // - Barras del sistema transparentes
            // - setDecorFitsSystemWindows(false)
            // - Colores apropiados para el contenido
            EdgeToEdge.enable(actividad);
        } catch (Exception e) {
            // Fallback manual si EdgeToEdge falla
            Window window = actividad.getWindow();
            WindowCompat.setDecorFitsSystemWindows(window, false);
        }
    }

    /**
     * Aplica window insets a una vista específica.
     * Esto asegura que el contenido no quede oculto por las barras del sistema.
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

    /**
     * Aplica insets solo a la parte inferior de una vista.
     * Útil cuando el toolbar ya maneja el inset superior.
     * 
     * @param view La vista a la que aplicar los insets inferiores
     */
    public void aplicarWindowInsetsInferior(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Solo aplicar padding inferior
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    insets.bottom);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * @deprecated Use {@link #habilitarEdgeToEdge()} en su lugar.
     *             Este método usa FLAG_FULLSCREEN que está deprecado.
     */
    @Deprecated
    public void pantallaCompleta() {
        // Redirigir a la nueva implementación
        habilitarEdgeToEdge();
    }

    /**
     * Ocultar los botones virtuales de navegación en modo inmersivo.
     * Las barras reaparecen temporalmente con un swipe desde el borde.
     */
    public void ocultarBotonesVirtuales() {
        Window window = actividad.getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Usar WindowInsetsController para API 30+ (Android 11+)
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // Ocultar barras del sistema
                controller.hide(android.view.WindowInsets.Type.systemBars());
                // Configurar comportamiento inmersivo
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
     * Configuración completa de Edge-to-Edge con ocultación de barras.
     * Combina Edge-to-Edge con modo inmersivo.
     */
    public void configurarEdgeToEdgeCompleto() {
        habilitarEdgeToEdge();
        ocultarBotonesVirtuales();
    }

    /**
     * Configura los colores de las barras del sistema para modo claro.
     * 
     * Nota: En Android 15+ con SDK 35, setStatusBarColor y setNavigationBarColor
     * están deprecados y no tienen efecto. Las barras son transparentes por
     * defecto.
     * Este método solo tiene efecto en versiones anteriores.
     */
    public void configurarBarrasClaro() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = actividad.getWindow().getInsetsController();
            if (controller != null) {
                // Iconos oscuros para fondo claro
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
    }

    /**
     * Configura los colores de las barras del sistema para modo oscuro.
     */
    public void configurarBarrasOscuro() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = actividad.getWindow().getInsetsController();
            if (controller != null) {
                // Quitar apariencia light para iconos claros en fondo oscuro
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
    }
}