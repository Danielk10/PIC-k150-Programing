package com.diamon.excepciones;

/**
 * Excepción específica para errores de comunicación USB con el programador PIC K150.
 * 
 * <p>Esta excepción se lanza cuando ocurren problemas en la comunicación serial USB,
 * incluyendo timeouts, errores de conexión, problemas de permisos, o fallos en
 * la transmisión/recepción de datos.</p>
 * 
 * <p>Tipos de errores USB manejados:</p>
 * <ul>
 *   <li>Timeout en operaciones de lectura/escritura</li>
 *   <li>Errores de conexión o desconexión inesperada</li>
 *   <li>Problemas de permisos USB</li>
 *   <li>Datos corruptos o respuestas inesperadas</li>
 *   <li>Buffer overflow o underflow</li>
 * </ul>
 * 
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public class UsbCommunicationException extends PicProgrammingException {

    /** Códigos de error específicos para comunicación USB */
    public static class CodigosError {
        public static final String TIMEOUT_LECTURA = "USB_TIMEOUT_READ";
        public static final String TIMEOUT_ESCRITURA = "USB_TIMEOUT_WRITE";
        public static final String CONEXION_PERDIDA = "USB_CONNECTION_LOST";
        public static final String DISPOSITIVO_NO_ENCONTRADO = "USB_DEVICE_NOT_FOUND";
        public static final String PERMISOS_INSUFICIENTES = "USB_PERMISSION_DENIED";
        public static final String RESPUESTA_INESPERADA = "USB_UNEXPECTED_RESPONSE";
        public static final String BUFFER_OVERFLOW = "USB_BUFFER_OVERFLOW";
        public static final String DATOS_CORRUPTOS = "USB_CORRUPTED_DATA";
        public static final String PUERTO_OCUPADO = "USB_PORT_BUSY";
        public static final String CONFIGURACION_PUERTO = "USB_PORT_CONFIG_ERROR";
    }

    /**
     * Constructor para timeout en operaciones USB.
     * 
     * @param operacion Tipo de operación (lectura/escritura)
     * @param timeoutMs Tiempo de timeout en milisegundos
     */
    public static UsbCommunicationException crearTimeoutError(String operacion, int timeoutMs) {
        String mensaje = String.format("Timeout en %s USB después de %d ms", operacion, timeoutMs);
        String codigo = operacion.toLowerCase().contains("lectura") ? 
                       CodigosError.TIMEOUT_LECTURA : CodigosError.TIMEOUT_ESCRITURA;
        return new UsbCommunicationException(mensaje, codigo, operacion, true);
    }

    /**
     * Constructor para errores de respuesta inesperada.
     * 
     * @param esperado Respuesta esperada
     * @param recibido Respuesta recibida
     * @param comando Comando que generó la respuesta
     */
    public static UsbCommunicationException crearRespuestaInesperada(
            String esperado, String recibido, String comando) {
        String mensaje = String.format("Respuesta inesperada para comando '%s'. Esperado: %s, Recibido: %s", 
                                       comando, esperado, recibido);
        String contexto = String.format("CMD=%s, ESP=%s, REC=%s", comando, esperado, recibido);
        return new UsbCommunicationException(mensaje, CodigosError.RESPUESTA_INESPERADA, contexto, true);
    }

    /**
     * Constructor para errores de conexión perdida.
     */
    public static UsbCommunicationException crearConexionPerdida() {
        return new UsbCommunicationException(
            "Conexión USB perdida con el programador",
            CodigosError.CONEXION_PERDIDA,
            "Verificar conexión física del dispositivo",
            false
        );
    }

    /**
     * Constructor para errores de permisos USB.
     */
    public static UsbCommunicationException crearErrorPermisos() {
        return new UsbCommunicationException(
            "Permisos insuficientes para acceder al dispositivo USB",
            CodigosError.PERMISOS_INSUFICIENTES,
            "Verificar permisos USB en AndroidManifest.xml",
            true
        );
    }

    /**
     * Constructor para datos corruptos con detalles de verificación.
     * 
     * @param bytesEsperados Número de bytes esperados
     * @param bytesRecibidos Número de bytes recibidos
     * @param checksumValido true si el checksum es válido
     */
    public static UsbCommunicationException crearDatosCorruptos(
            int bytesEsperados, int bytesRecibidos, boolean checksumValido) {
        String mensaje = String.format(
            "Datos USB corruptos. Esperados: %d bytes, Recibidos: %d bytes, Checksum: %s",
            bytesEsperados, bytesRecibidos, checksumValido ? "VÁLIDO" : "INVÁLIDO");
        String contexto = String.format("ESP=%d, REC=%d, CHK=%s", 
                                        bytesEsperados, bytesRecibidos, checksumValido);
        return new UsbCommunicationException(mensaje, CodigosError.DATOS_CORRUPTOS, contexto, true);
    }

    /**
     * Constructor básico con mensaje.
     * 
     * @param mensaje Descripción del error
     */
    public UsbCommunicationException(String mensaje) {
        super(mensaje, "USB_ERROR_GENERAL", "Comunicación USB", false);
    }

    /**
     * Constructor con mensaje y causa.
     * 
     * @param mensaje Descripción del error
     * @param causa Excepción que causó este error
     */
    public UsbCommunicationException(String mensaje, Throwable causa) {
        super(mensaje, causa, "USB_ERROR_GENERAL", "Comunicación USB", false);
    }

    /**
     * Constructor completo con contexto específico.
     * 
     * @param mensaje Descripción del error
     * @param codigoError Código específico del error USB
     * @param contexto Contexto adicional del error
     * @param esRecuperable true si el error es recuperable
     */
    public UsbCommunicationException(String mensaje, String codigoError, 
                                   String contexto, boolean esRecuperable) {
        super(mensaje, codigoError, contexto, esRecuperable);
    }

    /**
     * Constructor completo con causa.
     * 
     * @param mensaje Descripción del error
     * @param causa Excepción que causó este error
     * @param codigoError Código específico del error USB
     * @param contexto Contexto adicional del error
     * @param esRecuperable true si el error es recuperable
     */
    public UsbCommunicationException(String mensaje, Throwable causa, String codigoError, 
                                   String contexto, boolean esRecuperable) {
        super(mensaje, causa, codigoError, contexto, esRecuperable);
    }

    /**
     * Verifica si este error indica un problema de hardware.
     * 
     * @return true si es un problema de hardware
     */
    public boolean esProblemaDeHardware() {
        String codigo = getCodigoError();
        return CodigosError.CONEXION_PERDIDA.equals(codigo) ||
               CodigosError.DISPOSITIVO_NO_ENCONTRADO.equals(codigo) ||
               CodigosError.PUERTO_OCUPADO.equals(codigo);
    }

    /**
     * Verifica si este error puede resolverse reintentando la operación.
     * 
     * @return true si se puede reintentar
     */
    public boolean puedeReintentarse() {
        String codigo = getCodigoError();
        return CodigosError.TIMEOUT_LECTURA.equals(codigo) ||
               CodigosError.TIMEOUT_ESCRITURA.equals(codigo) ||
               CodigosError.DATOS_CORRUPTOS.equals(codigo) ||
               CodigosError.RESPUESTA_INESPERADA.equals(codigo);
    }

    /**
     * Obtiene sugerencias de resolución basadas en el tipo de error.
     * 
     * @return Sugerencia para resolver el error
     */
    public String getSugerenciaResolucion() {
        String codigo = getCodigoError();
        
        switch (codigo) {
            case CodigosError.TIMEOUT_LECTURA:
            case CodigosError.TIMEOUT_ESCRITURA:
                return "Verificar la conexión USB y reintentar la operación";
                
            case CodigosError.CONEXION_PERDIDA:
                return "Reconectar el dispositivo USB y reiniciar la aplicación";
                
            case CodigosError.DISPOSITIVO_NO_ENCONTRADO:
                return "Verificar que el programador K150 esté conectado correctamente";
                
            case CodigosError.PERMISOS_INSUFICIENTES:
                return "Otorgar permisos USB a la aplicación en la configuración del sistema";
                
            case CodigosError.DATOS_CORRUPTOS:
                return "Verificar la integridad del cable USB y reintentar";
                
            case CodigosError.PUERTO_OCUPADO:
                return "Cerrar otras aplicaciones que puedan estar usando el puerto USB";
                
            default:
                return "Verificar la conexión del programador y reintentar la operación";
        }
    }
}