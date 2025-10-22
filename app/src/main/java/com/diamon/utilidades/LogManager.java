package com.diamon.utilidades;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Gestor centralizado de logging para la aplicación PIC K150 Programming.
 * Proporciona logging estructurado con niveles apropiados y contexto específico del dominio.
 * 
 * <p>Características principales:</p>
 * <ul>
 *   <li>Logging categorizado por módulos (USB, HEX, CHIP, UI, DATA)</li>
 *   <li>Niveles estándar de Android (VERBOSE, DEBUG, INFO, WARN, ERROR)</li>
 *   <li>Formateo consistente con timestamps y contexto</li>
 *   <li>Integración con Android Log y Microsoft AppCenter</li>
 *   <li>Control de configuración por categorías</li>
 * </ul>
 * 
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public class LogManager {

    // ========== CONSTANTES DE CONFIGURACIÓN ==========
    
    /** Prefijo para todos los tags de logging de la aplicación */
    private static final String APP_TAG_PREFIX = "PIC_K150";
    
    /** Separador para formatear mensajes estructurados */
    private static final String SEPARATOR = " | ";
    
    /** Formato para timestamps en los logs */
    private static final SimpleDateFormat TIMESTAMP_FORMAT = 
        new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    // ========== CATEGORÍAS DE LOGGING ==========
    
    /**
     * Categorías de logging específicas del dominio PIC K150.
     * Cada categoría representa un módulo o área funcional de la aplicación.
     */
    public enum Categoria {
        /** Comunicación USB con el programador K150 */
        USB("USB", true),
        
        /** Procesamiento de archivos HEX */
        HEX("HEX", true),
        
        /** Operaciones de programación de chips PIC */
        CHIP("CHIP", true),
        
        /** Interfaz de usuario y eventos */
        UI("UI", true),
        
        /** Procesamiento general de datos */
        DATA("DATA", true),
        
        /** Operaciones generales del sistema */
        SISTEMA("SYS", true);

        private final String tag;
        private boolean habilitada;

        Categoria(String tag, boolean habilitada) {
            this.tag = tag;
            this.habilitada = habilitada;
        }

        /**
         * Obtiene el tag completo para logging de Android.
         * @return Tag formateado como "PIC_K150_USB", "PIC_K150_HEX", etc.
         */
        public String getTag() {
            return APP_TAG_PREFIX + "_" + tag;
        }

        /**
         * Verifica si esta categoría está habilitada para logging.
         * @return true si está habilitada, false en caso contrario
         */
        public boolean estaHabilitada() {
            return habilitada;
        }

        /**
         * Habilita o deshabilita esta categoría de logging.
         * @param habilitada true para habilitar, false para deshabilitar
         */
        public void setHabilitada(boolean habilitada) {
            this.habilitada = habilitada;
        }
    }

    // ========== NIVELES DE LOGGING ==========
    
    /**
     * Niveles de logging con descripción de uso recomendado.
     */
    public enum Nivel {
        /** Información muy detallada, solo para desarrollo */
        VERBOSE(Log.VERBOSE, "VERB"),
        
        /** Información de debug, útil para solución de problemas */
        DEBUG(Log.DEBUG, "DEBG"),
        
        /** Información general de operaciones importantes */
        INFO(Log.INFO, "INFO"),
        
        /** Advertencias de situaciones que requieren atención */
        WARN(Log.WARN, "WARN"),
        
        /** Errores que requieren acción inmediata */
        ERROR(Log.ERROR, "ERRO");

        private final int nivelAndroid;
        private final String prefijo;

        Nivel(int nivelAndroid, String prefijo) {
            this.nivelAndroid = nivelAndroid;
            this.prefijo = prefijo;
        }

        public int getNivelAndroid() {
            return nivelAndroid;
        }

        public String getPrefijo() {
            return prefijo;
        }
    }

    // ========== CONFIGURACIÓN DE INSTANCIA ==========
    
    /** Instancia singleton del LogManager */
    private static LogManager instancia;
    
    /** Configuración de niveles mínimos por categoría */
    private final Map<Categoria, Nivel> nivelesMinimos;
    
    /** Constructor privado para patrón singleton */
    private LogManager() {
        nivelesMinimos = new HashMap<>();
        inicializarConfiguracionPorDefecto();
    }

    /**
     * Obtiene la instancia singleton del LogManager.
     * @return La instancia única del LogManager
     */
    public static LogManager getInstance() {
        if (instancia == null) {
            synchronized (LogManager.class) {
                if (instancia == null) {
                    instancia = new LogManager();
                }
            }
        }
        return instancia;
    }

    /**
     * Inicializa la configuración por defecto de logging.
     * En modo DEBUG todas las categorías están en VERBOSE.
     * En modo RELEASE solo INFO y superiores.
     */
    private void inicializarConfiguracionPorDefecto() {
        // En desarrollo: logs muy detallados
        boolean esDesarrollo = android.util.Log.isLoggable(APP_TAG_PREFIX, Log.DEBUG);
        
        Nivel nivelPorDefecto = esDesarrollo ? Nivel.VERBOSE : Nivel.INFO;
        
        for (Categoria categoria : Categoria.values()) {
            nivelesMinimos.put(categoria, nivelPorDefecto);
        }
        
        // USB y CHIP siempre con nivel DEBUG mínimo para troubleshooting
        nivelesMinimos.put(Categoria.USB, Nivel.DEBUG);
        nivelesMinimos.put(Categoria.CHIP, Nivel.DEBUG);
    }

    // ========== MÉTODOS PRINCIPALES DE LOGGING ==========

    /**
     * Registra un mensaje VERBOSE para debugging detallado.
     * 
     * @param categoria Categoría del mensaje
     * @param mensaje Mensaje a registrar
     */
    public static void v(Categoria categoria, String mensaje) {
        getInstance().log(categoria, Nivel.VERBOSE, mensaje, null);
    }

    /**
     * Registra un mensaje VERBOSE con contexto adicional.
     * 
     * @param categoria Categoría del mensaje
     * @param contexto Contexto adicional (ej: nombre del método)
     * @param mensaje Mensaje a registrar
     */
    public static void v(Categoria categoria, String contexto, String mensaje) {
        getInstance().log(categoria, Nivel.VERBOSE, contexto + ": " + mensaje, null);
    }

    /**
     * Registra un mensaje DEBUG para información de desarrollo.
     * 
     * @param categoria Categoría del mensaje
     * @param mensaje Mensaje a registrar
     */
    public static void d(Categoria categoria, String mensaje) {
        getInstance().log(categoria, Nivel.DEBUG, mensaje, null);
    }

    /**
     * Registra un mensaje DEBUG con contexto.
     * 
     * @param categoria Categoría del mensaje
     * @param contexto Contexto adicional
     * @param mensaje Mensaje a registrar
     */
    public static void d(Categoria categoria, String contexto, String mensaje) {
        getInstance().log(categoria, Nivel.DEBUG, contexto + ": " + mensaje, null);
    }

    /**
     * Registra un mensaje INFO para operaciones normales importantes.
     * 
     * @param categoria Categoría del mensaje
     * @param mensaje Mensaje a registrar
     */
    public static void i(Categoria categoria, String mensaje) {
        getInstance().log(categoria, Nivel.INFO, mensaje, null);
    }

    /**
     * Registra un mensaje INFO con contexto.
     * 
     * @param categoria Categoría del mensaje
     * @param contexto Contexto adicional
     * @param mensaje Mensaje a registrar
     */
    public static void i(Categoria categoria, String contexto, String mensaje) {
        getInstance().log(categoria, Nivel.INFO, contexto + ": " + mensaje, null);
    }

    /**
     * Registra un mensaje WARN para situaciones que requieren atención.
     * 
     * @param categoria Categoría del mensaje
     * @param mensaje Mensaje a registrar
     */
    public static void w(Categoria categoria, String mensaje) {
        getInstance().log(categoria, Nivel.WARN, mensaje, null);
    }

    /**
     * Registra un mensaje WARN con contexto.
     * 
     * @param categoria Categoría del mensaje
     * @param contexto Contexto adicional
     * @param mensaje Mensaje a registrar
     */
    public static void w(Categoria categoria, String contexto, String mensaje) {
        getInstance().log(categoria, Nivel.WARN, contexto + ": " + mensaje, null);
    }

    /**
     * Registra un mensaje ERROR para errores críticos.
     * 
     * @param categoria Categoría del mensaje
     * @param mensaje Mensaje de error
     */
    public static void e(Categoria categoria, String mensaje) {
        getInstance().log(categoria, Nivel.ERROR, mensaje, null);
    }

    /**
     * Registra un mensaje ERROR con contexto.
     * 
     * @param categoria Categoría del mensaje
     * @param contexto Contexto adicional
     * @param mensaje Mensaje de error
     */
    public static void e(Categoria categoria, String contexto, String mensaje) {
        getInstance().log(categoria, Nivel.ERROR, contexto + ": " + mensaje, null);
    }

    /**
     * Registra un mensaje ERROR con excepción asociada.
     * 
     * @param categoria Categoría del mensaje
     * @param mensaje Mensaje de error
     * @param excepcion Excepción que causó el error
     */
    public static void e(Categoria categoria, String mensaje, Throwable excepcion) {
        getInstance().log(categoria, Nivel.ERROR, mensaje, excepcion);
    }

    /**
     * Registra un mensaje ERROR con contexto y excepción.
     * 
     * @param categoria Categoría del mensaje
     * @param contexto Contexto adicional
     * @param mensaje Mensaje de error
     * @param excepcion Excepción que causó el error
     */
    public static void e(Categoria categoria, String contexto, String mensaje, Throwable excepcion) {
        getInstance().log(categoria, Nivel.ERROR, contexto + ": " + mensaje, excepcion);
    }

    // ========== MÉTODOS ESPECIALIZADOS ==========

    /**
     * Registra datos de comunicación USB en formato hexadecimal.
     * 
     * @param direccion Dirección de comunicación (ENVÍO/RECEPCIÓN)
     * @param datos Array de bytes a registrar
     * @param longitud Longitud válida de datos
     */

    public static void logDatosUSB(String direccion, byte[] datos, int longitud) {
        if (!getInstance().debeLoguear(Categoria.USB, Nivel.DEBUG)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(direccion).append(" [").append(longitud).append(" bytes]: ");
        
        for (int i = 0; i < longitud && i < datos.length; i++) {
            sb.append(String.format("%02X ", datos[i]));
            // Limitar a 16 bytes por línea para legibilidad
            if ((i + 1) % 16 == 0 && i + 1 < longitud) {
                sb.append("\n                    ");
            }
        }
        
        d(Categoria.USB, "DatosUSB", sb.toString());
    }

    /**
     * Registra el inicio de una operación importante con duración.
     * 
     * @param categoria Categoría de la operación
     * @param operacion Nombre de la operación
     * @return Timestamp de inicio para calcular duración
     */
    public static long logInicioOperacion(Categoria categoria, String operacion) {
        long inicio = System.currentTimeMillis();
        i(categoria, "InicioOperacion", operacion + " iniciada");
        return inicio;
    }

    /**
     * Registra el fin de una operación con duración calculada.
     * 
     * @param categoria Categoría de la operación
     * @param operacion Nombre de la operación
     * @param inicioTimestamp Timestamp del inicio
     * @param exitosa true si la operación fue exitosa
     */
    public static void logFinOperacion(Categoria categoria, String operacion, 
                                     long inicioTimestamp, boolean exitosa) {
        long duracion = System.currentTimeMillis() - inicioTimestamp;
        String resultado = exitosa ? "ÉXITO" : "FALLO";
        i(categoria, "FinOperacion", 
          operacion + " completada - " + resultado + " (" + duracion + "ms)");
    }

    /**
     * Registra información de configuración de chip PIC.
     * 
     * @param nombreChip Nombre del chip
     * @param configuracion Mapa de configuración
     */
    public static void logConfiguracionChip(String nombreChip, Map<String, String> configuracion) {
        if (!getInstance().debeLoguear(Categoria.CHIP, Nivel.INFO)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Configuración para ").append(nombreChip).append(":");
        
        for (Map.Entry<String, String> entry : configuracion.entrySet()) {
            sb.append("\n  ").append(entry.getKey()).append(" = ").append(entry.getValue());
        }
        
        i(Categoria.CHIP, "ConfigChip", sb.toString());
    }

    // ========== MÉTODO INTERNO DE LOGGING ==========

    /**
     * Método interno que realiza el logging efectivo.
     * 
     * @param categoria Categoría del mensaje
     * @param nivel Nivel de logging
     * @param mensaje Mensaje a registrar
     * @param excepcion Excepción opcional
     */
    private void log(Categoria categoria, Nivel nivel, String mensaje, Throwable excepcion) {
        // Verificar si se debe loguear este mensaje
        if (!debeLoguear(categoria, nivel)) {
            return;
        }

        // Formatear mensaje con timestamp y contexto
        String mensajeFormateado = formatearMensaje(nivel, mensaje);
        
        // Escribir al log de Android
        if (excepcion != null) {
            Log.println(nivel.getNivelAndroid(), categoria.getTag(), 
                       mensajeFormateado + "\n" + Log.getStackTraceString(excepcion));
        } else {
            Log.println(nivel.getNivelAndroid(), categoria.getTag(), mensajeFormateado);
        }

        // TODO: Integrar con Microsoft AppCenter para logs remotos en producción
        // if (nivel == Nivel.ERROR && excepcion != null) {
        //     Crashes.trackError(excepcion, buildPropertiesMap(categoria, mensaje), null);
        // }
    }

    /**
     * Verifica si se debe registrar un mensaje según la configuración.
     * 
     * @param categoria Categoría del mensaje
     * @param nivel Nivel del mensaje
     * @return true si se debe registrar, false en caso contrario
     */
    private boolean debeLoguear(Categoria categoria, Nivel nivel) {
        // Verificar si la categoría está habilitada
        if (!categoria.estaHabilitada()) {
            return false;
        }

        // Verificar si el nivel es suficiente
        Nivel nivelMinimo = nivelesMinimos.get(categoria);
        if (nivelMinimo == null) {
            nivelMinimo = Nivel.INFO;
        }

        return nivel.getNivelAndroid() >= nivelMinimo.getNivelAndroid();
    }

    /**
     * Formatea un mensaje con timestamp y prefijos consistentes.
     * 
     * @param nivel Nivel del mensaje
     * @param mensaje Mensaje original
     * @return Mensaje formateado
     */
    private String formatearMensaje(Nivel nivel, String mensaje) {
        return TIMESTAMP_FORMAT.format(new Date()) + SEPARATOR + 
               nivel.getPrefijo() + SEPARATOR + mensaje;
    }

    // ========== MÉTODOS DE CONFIGURACIÓN ==========

    /**
     * Configura el nivel mínimo de logging para una categoría.
     * 
     * @param categoria Categoría a configurar
     * @param nivelMinimo Nivel mínimo para esta categoría
     */
    public static void configurarNivelMinimo(Categoria categoria, Nivel nivelMinimo) {
        getInstance().nivelesMinimos.put(categoria, nivelMinimo);
        i(Categoria.SISTEMA, "ConfigLog", 
          "Nivel mínimo para " + categoria.name() + " configurado a " + nivelMinimo.name());
    }

    /**
     * Habilita o deshabilita una categoría específica.
     * 
     * @param categoria Categoría a modificar
     * @param habilitada true para habilitar, false para deshabilitar
     */
    public static void configurarCategoria(Categoria categoria, boolean habilitada) {
        categoria.setHabilitada(habilitada);
        i(Categoria.SISTEMA, "ConfigLog", 
          "Categoría " + categoria.name() + " " + (habilitada ? "habilitada" : "deshabilitada"));
    }

    /**
     * Configura logging modo desarrollo (todos los niveles activos).
     */
    public static void configurarModoDesarrollo() {
        for (Categoria categoria : Categoria.values()) {
            getInstance().nivelesMinimos.put(categoria, Nivel.VERBOSE);
            categoria.setHabilitada(true);
        }
        i(Categoria.SISTEMA, "ConfigLog", "Modo desarrollo activado - logging completo habilitado");
    }

    /**
     * Configura logging modo producción (solo INFO y superiores).
     */
    public static void configurarModoProduccion() {
        for (Categoria categoria : Categoria.values()) {
            getInstance().nivelesMinimos.put(categoria, Nivel.INFO);
        }
        // USB y CHIP mantienen DEBUG para troubleshooting
        getInstance().nivelesMinimos.put(Categoria.USB, Nivel.DEBUG);
        getInstance().nivelesMinimos.put(Categoria.CHIP, Nivel.DEBUG);
        
        i(Categoria.SISTEMA, "ConfigLog", "Modo producción activado - logging optimizado");
    }
}