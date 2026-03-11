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
        if (currentLanguage.equals("es")) {
            return getSpanishTranslation(key);
        } else {
            return getEnglishTranslation(key);
        }
    }

    private String getSpanishTranslation(String key) {
        switch (key) {
            case "copy_button":
                return context.getString(R.string.copiar_btn);
            case "copy_success":
                return context.getString(R.string.codigo_copiado_portapapeles);
            case "language_label":
                return context.getString(R.string.idioma_label).replace(": %s", ":");
            case "spanish":
                return context.getString(R.string.idioma_espanol);
            case "english":
                return context.getString(R.string.idioma_ingles);
            case "loading":
                return context.getString(R.string.cargando_tutorial);
            case "error_loading":
                return context.getString(R.string.error_cargar_tutorial) + ":";
            case "step":
                return "PASO";
            case "note":
                return "Nota";
            case "important":
                return "Importante";
            case "warning":
                return "Advertencia";
            case "example":
                return "Ejemplo";
            case "explanation":
                return "Explicación";
            default:
                return key;
        }
    }

    private String getEnglishTranslation(String key) {
        // Por ahora, como el archivo strings.xml solo tiene español, 
        // mantendremos estos hardcodeados para inglés o usaremos una estrategia de traducción si existe.
        // Dado que el usuario pidió "en español", priorizamos la lógica de recursos para español.
        switch (key) {
            case "copy_button":
                return "📋 Copy";
            case "copy_success":
                return "Code copied to clipboard";
            case "language_label":
                return "🌐 Language:";
            case "spanish":
                return "Spanish";
            case "english":
                return "English";
            case "loading":
                return "Loading tutorial...";
            case "error_loading":
                return context.getString(R.string.error_cargando_tutorial_prefijo);
            case "step":
                return "STEP";
            case "note":
                return "Note";
            case "important":
                return "Important";
            case "warning":
                return "Warning";
            case "example":
                return "Example";
            case "explanation":
                return "Explanation";
            default:
                return key;
        }
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
