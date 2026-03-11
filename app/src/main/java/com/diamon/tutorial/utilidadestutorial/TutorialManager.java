package com.diamon.tutorial.utilidadestutorial;

import android.content.Context;
import com.diamon.pic.R;

/**
 * Clase para gestionar la carga y visualización de tutoriales en múltiples idiomas
 * Proporciona métodos para cargar contenido formateado desde archivos
 */
public class TutorialManager {

    private Context context;
    private String currentLanguage = "es";

    public TutorialManager(Context context) {
        this.context = context;
    }

    /**
     * Establece el idioma actual para el tutorial
     * @param language "es" para español o "en" para inglés
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
    }

    /**
     * Obtiene el idioma actual
     * @return Código del idioma ("es" o "en")
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Obtiene el nombre del archivo de tutorial basado en el idioma
     * @return Nombre del archivo
     */
    public String getTutorialFileName() {
        return currentLanguage.equals("es") ?
                "tutorial_gputils_es.txt" :
                "tutorial_gputils_en.txt";
    }

    /**
     * Obtiene el texto de traducción para un código específico
     * @param key Clave del texto a traducir
     * @return Texto traducido
     */
    public String getTranslation(String key) {
        int resId = -1;
        switch (key) {
            case "copy_button": resId = R.string.copiar_btn; break;
            case "copy_success": resId = R.string.codigo_copiado_portapapeles; break;
            case "language_label": 
                String label = context.getString(R.string.idioma_label);
                return label.replace(": %s", ":");
            case "spanish": resId = R.string.idioma_espanol; break;
            case "english": resId = R.string.idioma_ingles; break;
            case "loading": resId = R.string.cargando_tutorial; break;
            case "error_loading": resId = R.string.error_cargar_tutorial; break;
            case "step": resId = R.string.paso_label; break;
            case "note": resId = R.string.nota_label; break;
            case "important": resId = R.string.importante_label; break;
            case "warning": resId = R.string.advertencia_label; break;
            case "example": resId = R.string.ejemplo_label; break;
            case "explanation": resId = R.string.explicacion_label; break;
        }

        if (resId != -1) {
            String s = context.getString(resId);
            if (key.equals("error_loading")) return s + ":";
            return s;
        }
        return key;
    }

    /**
     * Formatea un bloque de código con colores y estilos
     * @param code Código a formatear
     * @param title Título del bloque (opcional)
     * @return String formateado
     */
    public String formatCodeBlock(String code, String title) {
        StringBuilder formatted = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            formatted.append("═══════════════════════════════════\n");
            formatted.append(" ").append(title).append("\n");
            formatted.append("═══════════════════════════════════\n\n");
        }

        formatted.append(code).append("\n\n");
        return formatted.toString();
    }

    /**
     * Crea un contenedor de nota formateada
     * @param emoji Emoticón de la nota
     * @param title Título de la nota
     * @param content Contenido de la nota
     * @return String formateado
     */
    public String formatNote(String emoji, String title, String content) {
        StringBuilder note = new StringBuilder();
        note.append("\n").append(emoji).append(" ").append(title).append(":\n");
        note.append("┌─────────────────────────────────────┐\n");
        note.append("│ ").append(content).append("\n");
        note.append("└─────────────────────────────────────┘\n\n");
        return note.toString();
    }

    /**
     * Obtiene un emoji apropiado para el tipo de sección
     * @param type Tipo de sección ("step", "note", "warning", etc.)
     * @return Emoji correspondiente
     */
    public String getEmoji(String type) {
        switch (type) {
            case "step":
                return "📝";
            case "note":
                return "ℹ️";
            case "warning":
                return "⚠️";
            case "code":
                return "💻";
            case "file":
                return "📂";
            case "download":
                return "📥";
            case "success":
                return "✅";
            case "error":
                return "❌";
            case "info":
                return "ℹ️";
            case "configure":
                return "⚙️";
            case "compile":
                return "🔨";
            case "install":
                return "💾";
            case "verify":
                return "✔️";
            case "command":
                return "💬";
            default:
                return "•";
        }
    }
}
