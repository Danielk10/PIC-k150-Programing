package com.diamon.excepciones;

/**
 * Excepción específica para errores de procesamiento de archivos HEX.
 * 
 * <p>Esta excepción se lanza cuando ocurren problemas al procesar archivos HEX,
 * incluyendo errores de formato, checksums inválidos, registros malformados,
 * o datos fuera de rango para el chip PIC seleccionado.</p>
 * 
 * <p>Tipos de errores HEX manejados:</p>
 * <ul>
 *   <li>Formato de archivo HEX inválido</li>
 *   <li>Checksums incorrectos en registros</li>
 *   <li>Registros malformados o incompletos</li>
 *   <li>Direcciones de memoria fuera de rango</li>
 *   <li>Datos incompatibles con el chip seleccionado</li>
 *   <li>Archivos HEX corruptos o truncados</li>
 * </ul>
 * 
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public class HexProcessingException extends PicProgrammingException {

    /** Códigos de error específicos para procesamiento HEX */
    public static class CodigosError {
        public static final String FORMATO_INVALIDO = "HEX_INVALID_FORMAT";
        public static final String CHECKSUM_INCORRECTO = "HEX_INVALID_CHECKSUM";
        public static final String REGISTRO_MALFORMADO = "HEX_MALFORMED_RECORD";
        public static final String DIRECCION_FUERA_RANGO = "HEX_ADDRESS_OUT_OF_RANGE";
        public static final String TIPO_REGISTRO_DESCONOCIDO = "HEX_UNKNOWN_RECORD_TYPE";
        public static final String ARCHIVO_TRUNCADO = "HEX_FILE_TRUNCATED";
        public static final String DATOS_INCOMPATIBLES = "HEX_DATA_INCOMPATIBLE";
        public static final String LONGITUD_INCORRECTA = "HEX_INVALID_LENGTH";
        public static final String CARACTERES_INVALIDOS = "HEX_INVALID_CHARACTERS";
        public static final String EOF_AUSENTE = "HEX_MISSING_EOF";
    }

    /** Información adicional del registro problemático */
    private final int numeroLinea;
    private final String registroProblematico;
    private final int direccionMemoria;

    /**
     * Constructor para errores de checksum con detalles del registro.
     * 
     * @param numeroLinea Número de línea donde ocurrió el error
     * @param registro Contenido del registro problemático
     * @param checksumCalculado Checksum calculado
     * @param checksumEsperado Checksum esperado
     */
    public static HexProcessingException crearErrorChecksum(int numeroLinea, String registro,
                                                           int checksumCalculado, int checksumEsperado) {
        String mensaje = String.format(
            "Checksum incorrecto en línea %d. Calculado: 0x%02X, Esperado: 0x%02X",
            numeroLinea, checksumCalculado, checksumEsperado);
        String contexto = String.format("LINEA=%d, CALC=0x%02X, ESP=0x%02X", 
                                        numeroLinea, checksumCalculado, checksumEsperado);
        return new HexProcessingException(mensaje, CodigosError.CHECKSUM_INCORRECTO, 
                                        contexto, numeroLinea, registro, -1, false);
    }

    /**
     * Constructor para errores de formato de registro.
     * 
     * @param numeroLinea Número de línea donde ocurrió el error
     * @param registro Contenido del registro problemático
     * @param razonError Descripción específica del error de formato
     */
    public static HexProcessingException crearErrorFormato(int numeroLinea, String registro, 
                                                          String razonError) {
        String mensaje = String.format("Formato inválido en línea %d: %s", numeroLinea, razonError);
        String contexto = String.format("LINEA=%d, RAZON=%s", numeroLinea, razonError);
        return new HexProcessingException(mensaje, CodigosError.FORMATO_INVALIDO,
                                        contexto, numeroLinea, registro, -1, false);
    }

    /**
     * Constructor para errores de dirección fuera de rango.
     * 
     * @param numeroLinea Número de línea donde ocurrió el error
     * @param direccion Dirección problemática
     * @param rangoMinimo Dirección mínima válida
     * @param rangoMaximo Dirección máxima válida
     * @param tipoMemoria Tipo de memoria (ROM, EEPROM, CONFIG)
     */
    public static HexProcessingException crearErrorDireccion(int numeroLinea, int direccion,
                                                           int rangoMinimo, int rangoMaximo, 
                                                           String tipoMemoria) {
        String mensaje = String.format(
            "Dirección 0x%04X fuera de rango para %s en línea %d. Rango válido: 0x%04X-0x%04X",
            direccion, tipoMemoria, numeroLinea, rangoMinimo, rangoMaximo);
        String contexto = String.format("TIPO=%s, DIR=0x%04X, MIN=0x%04X, MAX=0x%04X", 
                                        tipoMemoria, direccion, rangoMinimo, rangoMaximo);
        return new HexProcessingException(mensaje, CodigosError.DIRECCION_FUERA_RANGO,
                                        contexto, numeroLinea, "", direccion, false);
    }

    /**
     * Constructor para tipos de registro desconocidos.
     * 
     * @param numeroLinea Número de línea donde ocurrió el error
     * @param registro Contenido del registro problemático
     * @param tipoRegistro Tipo de registro desconocido
     */
    public static HexProcessingException crearTipoDesconocido(int numeroLinea, String registro,
                                                             int tipoRegistro) {
        String mensaje = String.format("Tipo de registro desconocido 0x%02X en línea %d", 
                                       tipoRegistro, numeroLinea);
        String contexto = String.format("TIPO=0x%02X, LINEA=%d", tipoRegistro, numeroLinea);
        return new HexProcessingException(mensaje, CodigosError.TIPO_REGISTRO_DESCONOCIDO,
                                        contexto, numeroLinea, registro, -1, false);
    }

    /**
     * Constructor para datos incompatibles con el chip.
     * 
     * @param chipModelo Modelo del chip PIC
     * @param razonIncompatibilidad Razón de la incompatibilidad
     */
    public static HexProcessingException crearDatosIncompatibles(String chipModelo, 
                                                                String razonIncompatibilidad) {
        String mensaje = String.format("Datos HEX incompatibles con %s: %s", 
                                       chipModelo, razonIncompatibilidad);
        String contexto = String.format("CHIP=%s, RAZON=%s", chipModelo, razonIncompatibilidad);
        return new HexProcessingException(mensaje, CodigosError.DATOS_INCOMPATIBLES,
                                        contexto, -1, "", -1, false);
    }

    /**
     * Constructor básico con mensaje.
     * 
     * @param mensaje Descripción del error
     */
    public HexProcessingException(String mensaje) {
        super(mensaje, "HEX_ERROR_GENERAL", "Procesamiento HEX", false);
        this.numeroLinea = -1;
        this.registroProblematico = "";
        this.direccionMemoria = -1;
    }

    /**
     * Constructor con mensaje y causa.
     * 
     * @param mensaje Descripción del error
     * @param causa Excepción que causó este error
     */
    public HexProcessingException(String mensaje, Throwable causa) {
        super(mensaje, causa, "HEX_ERROR_GENERAL", "Procesamiento HEX", false);
        this.numeroLinea = -1;
        this.registroProblematico = "";
        this.direccionMemoria = -1;
    }

    /**
     * Constructor completo con detalles del registro.
     * 
     * @param mensaje Descripción del error
     * @param codigoError Código específico del error
     * @param contexto Contexto adicional del error
     * @param numeroLinea Número de línea problemática (-1 si no aplica)
     * @param registroProblematico Contenido del registro problemático
     * @param direccionMemoria Dirección de memoria problemática (-1 si no aplica)
     * @param esRecuperable true si el error es recuperable
     */
    public HexProcessingException(String mensaje, String codigoError, String contexto,
                                 int numeroLinea, String registroProblematico, 
                                 int direccionMemoria, boolean esRecuperable) {
        super(mensaje, codigoError, contexto, esRecuperable);
        this.numeroLinea = numeroLinea;
        this.registroProblematico = registroProblematico != null ? registroProblematico : "";
        this.direccionMemoria = direccionMemoria;
    }

    /**
     * Obtiene el número de línea donde ocurrió el error.
     * 
     * @return Número de línea, o -1 si no aplica
     */
    public int getNumeroLinea() {
        return numeroLinea;
    }

    /**
     * Obtiene el contenido del registro problemático.
     * 
     * @return Contenido del registro, o cadena vacía si no aplica
     */
    public String getRegistroProblematico() {
        return registroProblematico;
    }

    /**
     * Obtiene la dirección de memoria problemática.
     * 
     * @return Dirección de memoria, o -1 si no aplica
     */
    public int getDireccionMemoria() {
        return direccionMemoria;
    }

    /**
     * Verifica si el error está relacionado con la integridad de datos.
     * 
     * @return true si es un error de integridad
     */
    public boolean esErrorDeIntegridad() {
        String codigo = getCodigoError();
        return CodigosError.CHECKSUM_INCORRECTO.equals(codigo) ||
               CodigosError.ARCHIVO_TRUNCADO.equals(codigo) ||
               CodigosError.DATOS_INCOMPATIBLES.equals(codigo);
    }

    /**
     * Verifica si el error está relacionado con formato de archivo.
     * 
     * @return true si es un error de formato
     */
    public boolean esErrorDeFormato() {
        String codigo = getCodigoError();
        return CodigosError.FORMATO_INVALIDO.equals(codigo) ||
               CodigosError.REGISTRO_MALFORMADO.equals(codigo) ||
               CodigosError.CARACTERES_INVALIDOS.equals(codigo) ||
               CodigosError.LONGITUD_INCORRECTA.equals(codigo);
    }

    /**
     * Obtiene sugerencias de resolución basadas en el tipo de error.
     * 
     * @return Sugerencia para resolver el error
     */
    public String getSugerenciaResolucion() {
        String codigo = getCodigoError();
        
        switch (codigo) {
            case CodigosError.CHECKSUM_INCORRECTO:
                return "Verificar la integridad del archivo HEX y regenerarlo desde el compilador";
                
            case CodigosError.FORMATO_INVALIDO:
            case CodigosError.REGISTRO_MALFORMADO:
                return "Verificar que el archivo tenga formato Intel HEX válido";
                
            case CodigosError.DIRECCION_FUERA_RANGO:
                return "Verificar que el código esté compilado para el chip PIC seleccionado";
                
            case CodigosError.DATOS_INCOMPATIBLES:
                return "Verificar la compatibilidad entre el archivo HEX y el modelo de chip";
                
            case CodigosError.ARCHIVO_TRUNCADO:
                return "Verificar que el archivo HEX esté completo y no esté dañado";
                
            case CodigosError.EOF_AUSENTE:
                return "El archivo debe terminar con un registro EOF válido (:00000001FF)";
                
            default:
                return "Verificar la integridad del archivo HEX y su compatibilidad con el chip";
        }
    }

    /**
     * Obtiene un mensaje detallado incluyendo información del registro.
     * 
     * @return Mensaje formateado completo con detalles
     */
    @Override
    public String getMensajeDetallado() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMensajeDetallado());
        
        if (numeroLinea > 0) {
            sb.append(" [Línea: ").append(numeroLinea).append("]");
        }
        
        if (direccionMemoria >= 0) {
            sb.append(" [Dirección: 0x").append(String.format("%04X", direccionMemoria)).append("]");
        }
        
        if (!registroProblematico.isEmpty() && registroProblematico.length() <= 50) {
            sb.append(" [Registro: ").append(registroProblematico).append("]");
        }
        
        return sb.toString();
    }
}