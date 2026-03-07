package com.diamon.protocolo;

/**
 * Tipos de protocolo soportados por los programadores K150/K128/K149.
 *
 * <p>
 * Cada variante define los números de comando específicos para las
 * operaciones de conexión (detección de chip, versión del programador,
 * lectura de protocolo). Los comandos de programación (3-17) son
 * idénticos en todos los protocolos.
 *
 * <p>
 * Basado en la implementación de referencia picpro (Python 3).
 *
 * @author Danielk10
 * @since 2025
 */
public enum TipoProtocolo {

    /**
     * Protocolo P18A - Usado por programadores K150 modernos.
     * Comandos de conexión: 18-21
     */
    P18A("P18A", 18, 19, 20, 21, 14, 15, 16),

    /**
     * Protocolo P018 - Variante intermedia.
     * Comandos de conexión: 19-22 (idénticos a P014/P016)
     */
    P018("P018", 19, 20, 21, 22, 15, 16, 17),

    /**
     * Protocolo P016 - Versión anterior.
     * Comandos de conexión: 19-22
     */
    P016("P016", 19, 20, 21, 22, 15, 16, 17),

    /**
     * Protocolo P014 - Versión más antigua.
     * Comandos de conexión: 19-22
     */
    P014("P014", 19, 20, 21, 22, 15, 16, 17);

    /** Nombre del protocolo en formato texto (e.g. "P18A") */
    private final String nombre;

    /** Número de comando para detectar chip en socket */
    private final int cmdDetectarEnSocket;

    /** Número de comando para detectar chip fuera del socket */
    private final int cmdDetectarFueraSocket;

    /** Número de comando para obtener versión del programador */
    private final int cmdVersion;

    /** Número de comando para obtener protocolo del programador */
    private final int cmdProtocolo;

    /** Número de comando para borrar chip */
    private final int cmdBorrarChip;

    /** Número de comando para Blank Check ROM */
    private final int cmdBlankCheckROM;

    /** Número de comando para Blank Check EEPROM */
    private final int cmdBlankCheckEEPROM;

    TipoProtocolo(String nombre, int cmdDetectarEnSocket,
            int cmdDetectarFueraSocket, int cmdVersion, int cmdProtocolo,
            int cmdBorrarChip, int cmdBlankCheckROM, int cmdBlankCheckEEPROM) {
        this.nombre = nombre;
        this.cmdDetectarEnSocket = cmdDetectarEnSocket;
        this.cmdDetectarFueraSocket = cmdDetectarFueraSocket;
        this.cmdVersion = cmdVersion;
        this.cmdProtocolo = cmdProtocolo;
        this.cmdBorrarChip = cmdBorrarChip;
        this.cmdBlankCheckROM = cmdBlankCheckROM;
        this.cmdBlankCheckEEPROM = cmdBlankCheckEEPROM;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCmdDetectarEnSocket() {
        return cmdDetectarEnSocket;
    }

    public int getCmdDetectarFueraSocket() {
        return cmdDetectarFueraSocket;
    }

    public int getCmdVersion() {
        return cmdVersion;
    }

    public int getCmdProtocolo() {
        return cmdProtocolo;
    }

    public int getCmdBorrarChip() {
        return cmdBorrarChip;
    }

    public int getCmdBlankCheckROM() {
        return cmdBlankCheckROM;
    }

    public int getCmdBlankCheckEEPROM() {
        return cmdBlankCheckEEPROM;
    }

    /**
     * Obtiene el tipo de protocolo a partir de un string devuelto por el
     * programador.
     *
     * @param protocoloStr String del protocolo (e.g. "P18A", "P016")
     * @return TipoProtocolo correspondiente, o P18A como fallback
     */
    public static TipoProtocolo fromString(String protocoloStr) {
        if (protocoloStr == null)
            return P18A;
        String upper = protocoloStr.trim().toUpperCase();
        for (TipoProtocolo tp : values()) {
            if (upper.contains(tp.nombre)) {
                return tp;
            }
        }
        return P18A; // Fallback seguro
    }

    /**
     * Retorna todos los nombres de protocolos como array.
     * Útil para mostrar en diálogos de selección.
     */
    public static String[] getNombres() {
        TipoProtocolo[] values = values();
        String[] nombres = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            nombres[i] = values[i].nombre;
        }
        return nombres;
    }
}
