package com.diamon.excepciones;

/**
 * Excepción base para todos los errores relacionados con la programación de chips PIC.
 * 
 * <p>Esta es la clase padre de todas las excepciones específicas del dominio PIC K150.
 * Proporciona funcionalidad común y estructura jerárquica para el manejo de errores.</p>
 * 
 * <p>Jerarquía de excepciones:</p>
 * <pre>
 * PicProgrammingException
 * ├── UsbCommunicationException     (Errores de comunicación USB)
 * ├── HexProcessingException        (Errores de procesamiento HEX)
 * ├── ChipConfigurationException    (Errores de configuración de chip)
 * ├── ProtocolException             (Errores de protocolo P018)
 * └── DataValidationException       (Errores de validación de datos)
 * </pre>
 * 
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public class PicProgrammingException extends Exception {

    /** Código de error específico de la aplicación */
    private final String codigoError;
    
    /** Contexto adicional del error (operación, chip, archivo, etc.) */
    private final String contexto;
    
    /** Indica si el error es recuperable o crítico */
    private final boolean esRecuperable;

    /**
     * Constructor básico con mensaje.
     * 
     * @param mensaje Descripción del error
     */
    public PicProgrammingException(String mensaje) {
        super(mensaje);
        this.codigoError = "PIC_ERROR_GENERAL";
        this.contexto = "";
        this.esRecuperable = false;
    }

    /**
     * Constructor con mensaje y causa.
     * 
     * @param mensaje Descripción del error
     * @param causa Excepción que causó este error
     */
    public PicProgrammingException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoError = "PIC_ERROR_GENERAL";
        this.contexto = "";
        this.esRecuperable = false;
    }

    /**
     * Constructor completo con toda la información de contexto.
     * 
     * @param mensaje Descripción del error
     * @param causa Excepción que causó este error
     * @param codigoError Código específico del error
     * @param contexto Contexto adicional (operación, archivo, etc.)
     * @param esRecuperable true si el error es recuperable
     */
    public PicProgrammingException(String mensaje, Throwable causa, String codigoError, 
                                 String contexto, boolean esRecuperable) {
        super(mensaje, causa);
        this.codigoError = codigoError != null ? codigoError : "PIC_ERROR_GENERAL";
        this.contexto = contexto != null ? contexto : "";
        this.esRecuperable = esRecuperable;
    }

    /**
     * Constructor con código de error y contexto.
     * 
     * @param mensaje Descripción del error
     * @param codigoError Código específico del error
     * @param contexto Contexto adicional
     * @param esRecuperable true si el error es recuperable
     */
    public PicProgrammingException(String mensaje, String codigoError, 
                                 String contexto, boolean esRecuperable) {
        super(mensaje);
        this.codigoError = codigoError != null ? codigoError : "PIC_ERROR_GENERAL";
        this.contexto = contexto != null ? contexto : "";
        this.esRecuperable = esRecuperable;
    }

    /**
     * Obtiene el código de error específico de la aplicación.
     * 
     * @return Código de error
     */
    public String getCodigoError() {
        return codigoError;
    }

    /**
     * Obtiene el contexto adicional del error.
     * 
     * @return Contexto del error
     */
    public String getContexto() {
        return contexto;
    }

    /**
     * Indica si el error es recuperable.
     * 
     * @return true si el error es recuperable, false si es crítico
     */
    public boolean esRecuperable() {
        return esRecuperable;
    }

    /**
     * Obtiene un mensaje detallado que incluye código de error y contexto.
     * 
     * @return Mensaje formateado completo
     */
    public String getMensajeDetallado() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(codigoError).append("] ");
        sb.append(getMessage());
        
        if (!contexto.isEmpty()) {
            sb.append(" (Contexto: ").append(contexto).append(")");
        }
        
        if (esRecuperable) {
            sb.append(" [RECUPERABLE]");
        } else {
            sb.append(" [CRÍTICO]");
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return getMensajeDetallado();
    }
}