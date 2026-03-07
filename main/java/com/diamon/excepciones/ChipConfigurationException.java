package com.diamon.excepciones;

/**
 * Excepción específica para errores de configuración de chips PIC.
 * 
 * <p>Esta excepción se lanza cuando ocurren problemas con la configuración
 * del chip PIC seleccionado, incluyendo parámetros inválidos, configuraciones
 * incompatibles, o problemas con las definiciones de chip.</p>
 * 
 * <p>Tipos de errores de configuración manejados:</p>
 * <ul>
 *   <li>Modelo de chip no soportado o no encontrado</li>
 *   <li>Parámetros de programación inválidos</li>
 *   <li>Configuración de fuses incorrecta</li>
 *   <li>Configuración de voltajes fuera de rango</li>
 *   <li>Configuración de secuencias de encendido inválida</li>
 *   <li>Incompatibilidad entre chip y programador</li>
 * </ul>
 * 
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public class ChipConfigurationException extends PicProgrammingException {

    /** Códigos de error específicos para configuración de chips */
    public static class CodigosError {
        public static final String CHIP_NO_SOPORTADO = "CHIP_NOT_SUPPORTED";
        public static final String CHIP_NO_ENCONTRADO = "CHIP_NOT_FOUND";
        public static final String PARAMETROS_INVALIDOS = "CHIP_INVALID_PARAMETERS";
        public static final String FUSES_INVALIDOS = "CHIP_INVALID_FUSES";
        public static final String VOLTAJES_FUERA_RANGO = "CHIP_VOLTAGE_OUT_OF_RANGE";
        public static final String SECUENCIA_INVALIDA = "CHIP_INVALID_POWER_SEQUENCE";
        public static final String CORE_TYPE_INVALIDO = "CHIP_INVALID_CORE_TYPE";
        public static final String MEMORIA_INSUFICIENTE = "CHIP_INSUFFICIENT_MEMORY";
        public static final String CONFIGURACION_ICSP = "CHIP_ICSP_CONFIG_ERROR";
        public static final String SOCKET_INCOMPATIBLE = "CHIP_SOCKET_INCOMPATIBLE";
    }

    /** Información adicional del chip problemático */
    private final String modeloChip;
    private final String parametroProblematico;
    private final Object valorProblematico;

    /**
     * Constructor para chip no encontrado.
     * 
     * @param modeloChip Modelo del chip que no se encontró
     */
    public static ChipConfigurationException crearChipNoEncontrado(String modeloChip) {
        String mensaje = String.format("Chip '%s' no encontrado en la base de datos", modeloChip);
        return new ChipConfigurationException(mensaje, CodigosError.CHIP_NO_ENCONTRADO,
                                            "Verificar modelo del chip", modeloChip, "", null, false);
    }

    /**
     * Constructor para chip no soportado por el programador.
     * 
     * @param modeloChip Modelo del chip no soportado
     * @param programadorModelo Modelo del programador
     */
    public static ChipConfigurationException crearChipNoSoportado(String modeloChip, 
                                                                String programadorModelo) {
        String mensaje = String.format("Chip '%s' no soportado por programador '%s'", 
                                       modeloChip, programadorModelo);
        String contexto = String.format("CHIP=%s, PROG=%s", modeloChip, programadorModelo);
        return new ChipConfigurationException(mensaje, CodigosError.CHIP_NO_SOPORTADO,
                                            contexto, modeloChip, "programador", programadorModelo, false);
    }

    /**
     * Constructor para parámetros de configuración inválidos.
     * 
     * @param modeloChip Modelo del chip
     * @param parametro Nombre del parámetro inválido
     * @param valor Valor inválido
     * @param valoresPermitidos Descripción de valores permitidos
     */
    public static ChipConfigurationException crearParametroInvalido(String modeloChip, 
                                                                  String parametro, Object valor, 
                                                                  String valoresPermitidos) {
        String mensaje = String.format("Parámetro '%s' inválido para chip '%s'. Valor: %s. %s",
                                       parametro, modeloChip, valor, valoresPermitidos);
        String contexto = String.format("PARAM=%s, VALOR=%s", parametro, valor);
        return new ChipConfigurationException(mensaje, CodigosError.PARAMETROS_INVALIDOS,
                                            contexto, modeloChip, parametro, valor, true);
    }

    /**
     * Constructor para configuración de fuses inválida.
     * 
     * @param modeloChip Modelo del chip
     * @param fuseIndex Índice del fuse problemático
     * @param valorFuse Valor del fuse inválido
     * @param valorMaximo Valor máximo permitido para este fuse
     */
    public static ChipConfigurationException crearFuseInvalido(String modeloChip, int fuseIndex,
                                                             int valorFuse, int valorMaximo) {
        String mensaje = String.format("Fuse %d inválido para chip '%s'. Valor: 0x%04X, Máximo: 0x%04X",
                                       fuseIndex, modeloChip, valorFuse, valorMaximo);
        String contexto = String.format("FUSE=%d, VALOR=0x%04X, MAX=0x%04X", 
                                        fuseIndex, valorFuse, valorMaximo);
        return new ChipConfigurationException(mensaje, CodigosError.FUSES_INVALIDOS,
                                            contexto, modeloChip, "fuse_" + fuseIndex, valorFuse, true);
    }

    /**
     * Constructor para memoria insuficiente.
     * 
     * @param modeloChip Modelo del chip
     * @param tipoMemoria Tipo de memoria (ROM/EEPROM)
     * @param tamanoRequerido Tamaño requerido
     * @param tamanoDisponible Tamaño disponible
     */
    public static ChipConfigurationException crearMemoriaInsuficiente(String modeloChip,
                                                                     String tipoMemoria,
                                                                     int tamanoRequerido,
                                                                     int tamanoDisponible) {
        String mensaje = String.format("Memoria %s insuficiente en chip '%s'. Requerido: %d, Disponible: %d",
                                       tipoMemoria, modeloChip, tamanoRequerido, tamanoDisponible);
        String contexto = String.format("TIPO=%s, REQ=%d, DISP=%d", 
                                        tipoMemoria, tamanoRequerido, tamanoDisponible);
        return new ChipConfigurationException(mensaje, CodigosError.MEMORIA_INSUFICIENTE,
                                            contexto, modeloChip, tipoMemoria.toLowerCase(), 
                                            tamanoRequerido, false);
    }

    /**
     * Constructor para configuración ICSP inválida.
     * 
     * @param modeloChip Modelo del chip
     * @param razonError Razón específica del error ICSP
     */
    public static ChipConfigurationException crearErrorICSP(String modeloChip, String razonError) {
        String mensaje = String.format("Error de configuración ICSP para chip '%s': %s", 
                                       modeloChip, razonError);
        return new ChipConfigurationException(mensaje, CodigosError.CONFIGURACION_ICSP,
                                            razonError, modeloChip, "ICSP", null, true);
    }

    /**
     * Constructor para socket incompatible.
     * 
     * @param modeloChip Modelo del chip
     * @param socketRequerido Socket requerido por el chip
     * @param socketDisponible Socket disponible en el programador
     */
    public static ChipConfigurationException crearSocketIncompatible(String modeloChip,
                                                                    String socketRequerido,
                                                                    String socketDisponible) {
        String mensaje = String.format("Socket incompatible para chip '%s'. Requerido: %s, Disponible: %s",
                                       modeloChip, socketRequerido, socketDisponible);
        String contexto = String.format("REQ=%s, DISP=%s", socketRequerido, socketDisponible);
        return new ChipConfigurationException(mensaje, CodigosError.SOCKET_INCOMPATIBLE,
                                            contexto, modeloChip, "socket", socketRequerido, false);
    }

    /**
     * Constructor básico con mensaje.
     * 
     * @param mensaje Descripción del error
     */
    public ChipConfigurationException(String mensaje) {
        super(mensaje, "CHIP_ERROR_GENERAL", "Configuración de chip", false);
        this.modeloChip = "";
        this.parametroProblematico = "";
        this.valorProblematico = null;
    }

    /**
     * Constructor con mensaje y causa.
     * 
     * @param mensaje Descripción del error
     * @param causa Excepción que causó este error
     */
    public ChipConfigurationException(String mensaje, Throwable causa) {
        super(mensaje, causa, "CHIP_ERROR_GENERAL", "Configuración de chip", false);
        this.modeloChip = "";
        this.parametroProblematico = "";
        this.valorProblematico = null;
    }

    /**
     * Constructor completo con detalles del chip.
     * 
     * @param mensaje Descripción del error
     * @param codigoError Código específico del error
     * @param contexto Contexto adicional del error
     * @param modeloChip Modelo del chip problemático
     * @param parametroProblematico Nombre del parámetro problemático
     * @param valorProblematico Valor problemático
     * @param esRecuperable true si el error es recuperable
     */
    public ChipConfigurationException(String mensaje, String codigoError, String contexto,
                                    String modeloChip, String parametroProblematico,
                                    Object valorProblematico, boolean esRecuperable) {
        super(mensaje, codigoError, contexto, esRecuperable);
        this.modeloChip = modeloChip != null ? modeloChip : "";
        this.parametroProblematico = parametroProblematico != null ? parametroProblematico : "";
        this.valorProblematico = valorProblematico;
    }

    /**
     * Obtiene el modelo del chip problemático.
     * 
     * @return Modelo del chip, o cadena vacía si no aplica
     */
    public String getModeloChip() {
        return modeloChip;
    }

    /**
     * Obtiene el nombre del parámetro problemático.
     * 
     * @return Nombre del parámetro, o cadena vacía si no aplica
     */
    public String getParametroProblematico() {
        return parametroProblematico;
    }

    /**
     * Obtiene el valor problemático.
     * 
     * @return Valor problemático, o null si no aplica
     */
    public Object getValorProblematico() {
        return valorProblematico;
    }

    /**
     * Verifica si el error está relacionado con limitaciones de hardware.
     * 
     * @return true si es una limitación de hardware
     */
    public boolean esLimitacionDeHardware() {
        String codigo = getCodigoError();
        return CodigosError.CHIP_NO_SOPORTADO.equals(codigo) ||
               CodigosError.MEMORIA_INSUFICIENTE.equals(codigo) ||
               CodigosError.SOCKET_INCOMPATIBLE.equals(codigo) ||
               CodigosError.VOLTAJES_FUERA_RANGO.equals(codigo);
    }

    /**
     * Verifica si el error puede resolverse cambiando la configuración.
     * 
     * @return true si se puede resolver con cambios de configuración
     */
    public boolean puedeResolverseConConfiguracion() {
        String codigo = getCodigoError();
        return CodigosError.PARAMETROS_INVALIDOS.equals(codigo) ||
               CodigosError.FUSES_INVALIDOS.equals(codigo) ||
               CodigosError.SECUENCIA_INVALIDA.equals(codigo) ||
               CodigosError.CONFIGURACION_ICSP.equals(codigo);
    }

    /**
     * Obtiene sugerencias de resolución basadas en el tipo de error.
     * 
     * @return Sugerencia para resolver el error
     */
    public String getSugerenciaResolucion() {
        String codigo = getCodigoError();
        
        switch (codigo) {
            case CodigosError.CHIP_NO_ENCONTRADO:
                return "Verificar el modelo del chip y actualizar la base de datos si es necesario";
                
            case CodigosError.CHIP_NO_SOPORTADO:
                return "Verificar compatibilidad del chip con el programador K150";
                
            case CodigosError.PARAMETROS_INVALIDOS:
                return "Revisar los parámetros de configuración en la documentación del chip";
                
            case CodigosError.FUSES_INVALIDOS:
                return "Verificar los valores de configuración (fuses) en el datasheet del chip";
                
            case CodigosError.MEMORIA_INSUFICIENTE:
                return "Reducir el tamaño del programa o seleccionar un chip con más memoria";
                
            case CodigosError.SOCKET_INCOMPATIBLE:
                return "Verificar que el chip sea compatible con el socket del programador";
                
            case CodigosError.CONFIGURACION_ICSP:
                return "Configurar correctamente el modo ICSP o usar socket apropiado";
                
            case CodigosError.VOLTAJES_FUERA_RANGO:
                return "Verificar los voltajes de programación en el datasheet del chip";
                
            default:
                return "Verificar la configuración del chip en la documentación oficial";
        }
    }

    /**
     * Obtiene información detallada del error incluyendo chip y parámetros.
     * 
     * @return Mensaje formateado completo con detalles
     */
    @Override
    public String getMensajeDetallado() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMensajeDetallado());
        
        if (!modeloChip.isEmpty()) {
            sb.append(" [Chip: ").append(modeloChip).append("]");
        }
        
        if (!parametroProblematico.isEmpty()) {
            sb.append(" [Parámetro: ").append(parametroProblematico).append("]");
        }
        
        if (valorProblematico != null) {
            sb.append(" [Valor: ").append(valorProblematico).append("]");
        }
        
        return sb.toString();
    }
}