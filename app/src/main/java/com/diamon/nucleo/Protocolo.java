package com.diamon.nucleo;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.excepciones.UsbCommunicationException;
import com.diamon.utilidades.ByteUtils;
import com.diamon.utilidades.LogManager;
import com.diamon.utilidades.LogManager.Categoria;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Clase abstracta base para protocolos de comunicación con programadores PIC.
 *
 * <p>Esta clase proporciona funcionalidad común para todos los protocolos de comunicación con
 * programadores PIC, incluyendo manejo de conexión USB, operaciones básicas de comunicación y
 * logging estructurado de todas las operaciones.
 *
 * <p>Características principales:
 *
 * <ul>
 *   <li>Logging integrado de todas las operaciones USB
 *   <li>Manejo robusto de excepciones específicas del dominio
 *   <li>Operaciones seguras de bytes con validación automática
 *   <li>Timeouts configurables y manejo de errores
 *   <li>Trazabilidad completa de comandos y respuestas
 * </ul>
 *
 * @author Danielk10
 * @version 2.0 - Integrado con sistema de logging y excepciones mejoradas
 * @since 2025
 */
public abstract class Protocolo {

    /** Contexto de la aplicación Android */
    protected final Context contexto;

    /** Puerto serie USB para comunicación con el programador */
    protected final UsbSerialPort usbSerialPort;

    /** Nombre del protocolo para logging */
    protected final String nombreProtocolo;

    /**
     * Constructor de la clase base Protocolo.
     *
     * @param contexto Contexto de la aplicación Android
     * @param usbSerialPort Puerto serie USB configurado
     */
    public Protocolo(Context contexto, UsbSerialPort usbSerialPort) {
        this.contexto = contexto;
        this.usbSerialPort = usbSerialPort;
        this.nombreProtocolo = this.getClass().getSimpleName();

        LogManager.i(
                Categoria.USB,
                "ProtocoloInit",
                String.format("Protocolo %s inicializado", nombreProtocolo));
    }

    /**
     * Limpia el buffer de recepción USB consumiendo todos los datos pendientes.
     *
     * @throws UsbCommunicationException Si ocurre un error durante la limpieza
     */
    private void clearBuffer() throws UsbCommunicationException {
        if (usbSerialPort == null) {
            throw new UsbCommunicationException("Puerto USB no inicializado");
        }

        try {
            byte[] tmpBuffer = new byte[1024];
            int totalLimpiado = 0;
            int bytesLeidos;

            long inicioOperacion = LogManager.logInicioOperacion(Categoria.USB, "clearBuffer");

            while ((bytesLeidos = usbSerialPort.read(tmpBuffer, 100)) > 0) {
                totalLimpiado += bytesLeidos;
                LogManager.v(
                        Categoria.USB,
                        "clearBuffer",
                        String.format("Limpiados %d bytes del buffer", bytesLeidos));
            }

            LogManager.logFinOperacion(Categoria.USB, "clearBuffer", inicioOperacion, true);

            if (totalLimpiado > 0) {
                LogManager.d(
                        Categoria.USB,
                        "clearBuffer",
                        String.format("Buffer USB limpiado: %d bytes total", totalLimpiado));
            }
        } catch (IOException e) {
            LogManager.e(Categoria.USB, "clearBuffer", "Error limpiando buffer USB", e);
            throw new UsbCommunicationException("Error limpiando buffer USB", e);
        }
    }

    /**
     * Lee un número específico de bytes del puerto serie con timeout y logging detallado.
     *
     * @param count Número de bytes a leer (debe ser mayor que 0)
     * @param timeoutMillis Tiempo máximo de espera en milisegundos
     * @return Array con los bytes leídos
     * @throws UsbCommunicationException Si ocurre error de comunicación o timeout
     */
    protected byte[] readBytes(int count, int timeoutMillis) throws UsbCommunicationException {
        if (usbSerialPort == null) {
            throw new UsbCommunicationException("Puerto USB no inicializado");
        }

        if (count <= 0) {
            String mensaje = "El número de bytes a leer debe ser mayor que 0: " + count;
            LogManager.e(Categoria.USB, "readBytes", mensaje);
            throw new IllegalArgumentException(mensaje);
        }

        long inicioOperacion =
                LogManager.logInicioOperacion(Categoria.USB, String.format("readBytes(%d)", count));

        try {
            // Crear un buffer para almacenar los datos leídos
            ByteBuffer byteBuffer = ByteBuffer.allocate(count);
            long startTime = System.currentTimeMillis();

            LogManager.v(
                    Categoria.USB,
                    "readBytes",
                    String.format(
                            "Iniciando lectura de %d bytes con timeout %d ms",
                            count, timeoutMillis));

            // Mientras no se hayan leído todos los bytes y el tiempo de espera no haya expirado
            while (byteBuffer.position() < count
                    && (System.currentTimeMillis() - startTime) < timeoutMillis) {

                // Leer los bytes restantes
                int remaining = count - byteBuffer.position();
                byte[] tmpBuffer = new byte[remaining];
                int bytesRead = usbSerialPort.read(tmpBuffer, Math.min(timeoutMillis, 100));

                if (bytesRead > 0) {
                    // Añadir los bytes leídos al buffer
                    byteBuffer.put(tmpBuffer, 0, bytesRead);

                    LogManager.v(
                            Categoria.USB,
                            "readBytes",
                            String.format(
                                    "Leídos %d bytes, total: %d/%d",
                                    bytesRead, byteBuffer.position(), count));
                } else if (bytesRead == 0) {
                    // Si no se reciben datos, esperar brevemente antes de reintentar
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LogManager.e(
                                Categoria.USB, "readBytes", "Hilo interrumpido durante lectura", e);
                        throw new UsbCommunicationException("Hilo interrumpido durante lectura", e);
                    }
                }
            }

            // Verificar si se leyeron todos los bytes
            if (byteBuffer.position() < count) {
                long tiempoTranscurrido = System.currentTimeMillis() - startTime;
                String mensaje =
                        String.format(
                                "Timeout leyendo bytes: esperados=%d, leídos=%d, tiempo=%dms",
                                count, byteBuffer.position(), tiempoTranscurrido);
                LogManager.e(Categoria.USB, "readBytes", mensaje);
                throw UsbCommunicationException.crearTimeoutError("lectura", timeoutMillis);
            }

            // Obtener los datos leídos
            byte[] resultado = byteBuffer.array();

            // Logging de datos recibidos
            LogManager.logDatosUSB("RECEPCIÓN", resultado, resultado.length);
            LogManager.logFinOperacion(
                    Categoria.USB, String.format("readBytes(%d)", count), inicioOperacion, true);

            return resultado;

        } catch (IOException e) {
            LogManager.e(
                    Categoria.USB,
                    "readBytes",
                    String.format("Error I/O leyendo %d bytes", count),
                    e);
            LogManager.logFinOperacion(
                    Categoria.USB, String.format("readBytes(%d)", count), inicioOperacion, false);
            throw new UsbCommunicationException("Error I/O durante lectura USB", e);
        }
    }

    /**
     * Espera una respuesta específica del dispositivo USB con logging detallado.
     *
     * @param expected Array con la respuesta esperada
     * @param timeoutMillis Tiempo máximo de espera en milisegundos
     * @throws UsbCommunicationException Si la respuesta no coincide o hay timeout
     */
    private void expectResponse(byte[] expected, int timeoutMillis)
            throws UsbCommunicationException {
        ByteUtils.validarArray(expected, -1, "expected");

        String esperadoHex = ByteUtils.bytesToHex(expected);
        LogManager.v(
                Categoria.USB,
                "expectResponse",
                String.format("Esperando respuesta: %s", esperadoHex));

        byte[] response = readBytes(expected.length, timeoutMillis);
        String recibidoHex = ByteUtils.bytesToHex(response);

        if (!Arrays.equals(response, expected)) {
            LogManager.w(
                    Categoria.USB,
                    "expectResponse",
                    String.format(
                            "Respuesta no coincide - Esperado: %s, Recibido: %s",
                            esperadoHex, recibidoHex));
            throw UsbCommunicationException.crearRespuestaInesperada(
                    esperadoHex, recibidoHex, "expectResponse");
        }

        LogManager.v(
                Categoria.USB,
                "expectResponse",
                String.format("Respuesta correcta recibida: %s", recibidoHex));
    }

    /**
     * Lee una respuesta y valida contra un carácter esperado con logging detallado.
     *
     * @param response Array donde se almacenará la respuesta (debe tener al menos 1 byte)
     * @param esperado Carácter esperado en la respuesta
     * @param mensajeError Mensaje de error personalizado
     * @return true si la respuesta coincide, false en caso contrario
     * @throws UsbCommunicationException Si ocurre error de comunicación
     */
    protected boolean leerRespuesta(byte[] response, char esperado, String mensajeError)
            throws UsbCommunicationException {
        ByteUtils.validarArray(response, -1, "response");

        if (response.length == 0) {
            throw new IllegalArgumentException("Array de respuesta debe tener al menos 1 byte");
        }

        try {
            LogManager.v(
                    Categoria.USB,
                    "leerRespuesta",
                    String.format("Esperando carácter: '%c' (0x%02X)", esperado, (byte) esperado));

            int bytesLeidos = usbSerialPort.read(response, 100);
            if (bytesLeidos == 0) {
                LogManager.w(Categoria.USB, "leerRespuesta", "No se recibieron datos");
                return false;
            }

            byte recibido = response[0];
            boolean coincide = (recibido == (byte) esperado);

            if (coincide) {
                LogManager.v(
                        Categoria.USB,
                        "leerRespuesta",
                        String.format("Respuesta correcta: '%c' (0x%02X)", esperado, recibido));
            } else {
                LogManager.w(
                        Categoria.USB,
                        "leerRespuesta",
                        String.format(
                                "%s - Esperado: '%c' (0x%02X), Recibido: 0x%02X",
                                mensajeError, esperado, (byte) esperado, recibido));
            }

            return coincide;

        } catch (IOException e) {
            LogManager.e(Categoria.USB, "leerRespuesta", "Error I/O leyendo respuesta", e);
            throw new UsbCommunicationException("Error leyendo respuesta USB", e);
        }
    }

    protected boolean leerRespuesta(
            byte[] response, char expected, String errorMessage, int timeoutMs)
            throws UsbCommunicationException {
        try {
            byte[] respuestaBytes = readBytes(1, timeoutMs); // ✅ Timeout personalizado
            response[0] = respuestaBytes[0];

            boolean valida = (respuestaBytes[0] == expected);

            if (!valida) {
                LogManager.w(
                        Categoria.USB,
                        "leerRespuesta",
                        String.format(
                                "Respuesta inválida: esperado='%c' (0x%02X), recibido='%c' (0x%02X)",
                                expected,
                                (byte) expected,
                                (char) respuestaBytes[0],
                                respuestaBytes[0]));
            }

            return valida;

        } catch (Exception e) {
            LogManager.e(Categoria.USB, "leerRespuesta", errorMessage + " - " + e.getMessage(), e);
            throw new UsbCommunicationException("Error leyendo respuesta USB", e);
        }
    }

    /**
     * Envía un comando al dispositivo USB siguiendo el protocolo establecido.
     *
     * @param comando String con el comando a enviar
     * @throws UsbCommunicationException Si ocurre error durante el envío
     */
    protected void enviarComando(String comando) throws UsbCommunicationException {
        if (usbSerialPort == null) {
            throw new UsbCommunicationException("Puerto USB no inicializado");
        }

        if (comando == null || comando.isEmpty()) {
            throw new IllegalArgumentException("Comando no puede ser null o vacío");
        }

        long inicioOperacion =
                LogManager.logInicioOperacion(
                        Categoria.USB, String.format("enviarComando(%s)", comando));

        try {
            LogManager.i(
                    Categoria.USB,
                    "enviarComando",
                    String.format("Enviando comando: '%s'", comando));

            // Parsear comando a bytes
            byte[] data = new byte[comando.length()];
            try {
                for (int i = 0; i < comando.length(); i++) {
                    data[i] = Byte.parseByte(comando);
                }
            } catch (NumberFormatException e) {
                String mensaje =
                        String.format("Comando inválido: '%s' - no es un número válido", comando);
                LogManager.e(Categoria.USB, "enviarComando", mensaje, e);
                throw new IllegalArgumentException(mensaje, e);
            }

            // Paso 1: Enviar 0x01 para inicializar
            byte[] inicializacion = ByteUtils.prepararDatosUSB(new byte[] {0x01}, "inicializacion");
            usbSerialPort.write(inicializacion, 100);
            LogManager.v(Categoria.USB, "enviarComando", "Enviado byte de inicialización: 0x01");

            // Paso 2: Esperar respuesta 'Q'
            expectResponse(new byte[] {'Q'}, 500);

            // Paso 3: Enviar 'P' para ir a la tabla de salto
            byte[] salto = ByteUtils.prepararDatosUSB(new byte[] {'P'}, "salto_tabla");
            usbSerialPort.write(salto, 100);
            LogManager.v(Categoria.USB, "enviarComando", "Enviado comando de salto: 'P'");

            // Paso 4: Leer acknowledgment 'P'
            byte[] ack = readBytes(1, 100);
            if (ack[0] != 'P') {
                String mensaje =
                        String.format(
                                "Acknowledgment inválido: esperado 'P', recibido 0x%02X", ack[0]);
                LogManager.e(Categoria.USB, "enviarComando", mensaje);
                throw UsbCommunicationException.crearRespuestaInesperada(
                        "P", String.format("0x%02X", ack[0]), "enviarComando");
            }

            // Paso 5: Enviar el número del comando, si es necesario
            if (data[0] != 0) {
                byte[] comandoFinal = ByteUtils.prepararDatosUSB(data, "comando_final");
                usbSerialPort.write(comandoFinal, 100);
                LogManager.v(
                        Categoria.USB,
                        "enviarComando",
                        String.format(
                                "Enviado comando final: %s", ByteUtils.bytesToHex(comandoFinal)));
            } else {
                LogManager.v(
                        Categoria.USB, "enviarComando", "Comando 0 - no se envía data adicional");
            }

            LogManager.logFinOperacion(
                    Categoria.USB,
                    String.format("enviarComando(%s)", comando),
                    inicioOperacion,
                    true);
            LogManager.d(
                    Categoria.USB,
                    "enviarComando",
                    String.format("Comando '%s' enviado exitosamente", comando));

        } catch (IOException e) {
            LogManager.e(
                    Categoria.USB,
                    "enviarComando",
                    String.format("Error I/O enviando comando '%s'", comando),
                    e);
            LogManager.logFinOperacion(
                    Categoria.USB,
                    String.format("enviarComando(%s)", comando),
                    inicioOperacion,
                    false);
            throw new UsbCommunicationException("Error I/O enviando comando", e);
        }
    }

    /**
     * Inicializa el protocolo de comunicación con el dispositivo.
     *
     * @return true si la inicialización fue exitosa, false en caso contrario
     */
    public boolean iniciarProtocolo() {
        if (usbSerialPort == null) {
            LogManager.e(Categoria.USB, "iniciarProtocolo", "Puerto USB no inicializado");
            return false;
        }

        String comando = "P";
        long inicioOperacion = LogManager.logInicioOperacion(Categoria.USB, "iniciarProtocolo");

        try {
            LogManager.i(
                    Categoria.USB,
                    "iniciarProtocolo",
                    String.format("Iniciando protocolo %s", nombreProtocolo));

            // Preparar comando de inicialización
            byte[] data =
                    ByteUtils.prepararDatosUSB(
                            comando.getBytes(StandardCharsets.US_ASCII), "inicializacion");
            usbSerialPort.write(data, 100);

            // Leer respuesta
            byte[] respuesta = readBytes(1, 100);

            // Si se reciben más datos de los esperados, limpiar buffer
            if (respuesta.length > 1) {
                LogManager.w(
                        Categoria.USB,
                        "iniciarProtocolo",
                        "Datos adicionales en buffer, limpiando...");
                clearBuffer();
                respuesta[0] = 'P'; // Asumir respuesta correcta después de limpiar
            }

            boolean exitoso = (respuesta[0] == 'P');

            if (exitoso) {
                LogManager.i(
                        Categoria.USB,
                        "iniciarProtocolo",
                        String.format("Protocolo %s inicializado exitosamente", nombreProtocolo));
            } else {
                LogManager.w(
                        Categoria.USB,
                        "iniciarProtocolo",
                        String.format(
                                "Protocolo %s falló inicialización - respuesta: 0x%02X",
                                nombreProtocolo, respuesta[0]));
            }

            LogManager.logFinOperacion(Categoria.USB, "iniciarProtocolo", inicioOperacion, exitoso);
            return exitoso;

        } catch (NumberFormatException e) {
            LogManager.e(
                    Categoria.USB, "iniciarProtocolo", "Error de formato en inicialización", e);
            LogManager.logFinOperacion(Categoria.USB, "iniciarProtocolo", inicioOperacion, false);
            return false;

        } catch (UsbCommunicationException e) {
            LogManager.e(Categoria.USB, "iniciarProtocolo", "Error de comunicación USB", e);
            LogManager.logFinOperacion(Categoria.USB, "iniciarProtocolo", inicioOperacion, false);
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Espera el inicio de un nuevo comando enviando un byte de sincronización.
     *
     * @return true si el comando se envió exitosamente, false en caso contrario
     */
    public boolean esperarInicioDeNuevoComando() {
        if (usbSerialPort == null) {
            LogManager.e(Categoria.USB, "esperarInicio", "Puerto USB no inicializado");
            return false;
        }

        long inicioOperacion =
                LogManager.logInicioOperacion(Categoria.USB, "esperarInicioDeNuevoComando");

        try {
            LogManager.v(Categoria.USB, "esperarInicio", "Enviando byte de sincronización");

            byte[] data = ByteUtils.prepararDatosUSB(new byte[] {0}, "sincronizacion");
            usbSerialPort.write(data, 100);

            LogManager.d(
                    Categoria.USB, "esperarInicio", "Byte de sincronización enviado exitosamente");
            LogManager.logFinOperacion(
                    Categoria.USB, "esperarInicioDeNuevoComando", inicioOperacion, true);
            return true;

        } catch (IOException e) {
            LogManager.e(Categoria.USB, "esperarInicio", "Error I/O enviando sincronización", e);
            LogManager.logFinOperacion(
                    Categoria.USB, "esperarInicioDeNuevoComando", inicioOperacion, false);
            return false;
        }
    }

    /**
     * Resetea los comandos enviando comando "0" para limpiar el estado.
     *
     * @return true si el reset fue exitoso, false en caso contrario
     */
    protected boolean researComandos() {
        try {
            LogManager.d(Categoria.USB, "researComandos", "Reseteando comandos del protocolo");
            enviarComando("0");
            LogManager.d(Categoria.USB, "researComandos", "Comandos reseteados exitosamente");
            return true;
        } catch (UsbCommunicationException e) {
            LogManager.e(Categoria.USB, "researComandos", "Error reseteando comandos", e);
            return false;
        }
    }

    // ========== MÉTODOS AUXILIARES PARA LOGGING ==========

    /** Registra información del estado actual del protocolo. */
    protected void logEstadoProtocolo() {
        boolean puertoConectado = (usbSerialPort != null && usbSerialPort.isOpen());
        LogManager.i(
                Categoria.USB,
                "EstadoProtocolo",
                String.format(
                        "Protocolo: %s, Puerto USB: %s",
                        nombreProtocolo, puertoConectado ? "CONECTADO" : "DESCONECTADO"));
    }

    /**
     * Registra el inicio de una operación de programación con el chip.
     *
     * @param operacion Nombre de la operación
     * @param chipPIC Información del chip
     * @return Timestamp de inicio para medir duración
     */
    protected long logInicioOperacionChip(String operacion, ChipPic chipPIC)
            throws ChipConfigurationException {
        String infoChip =
                chipPIC != null
                        ? String.format(
                                "Chip: %s, ROM: %d words, EEPROM: %d bytes",
                                "Desconocido", chipPIC.getTamanoROM(), chipPIC.getTamanoEEPROM())
                        : "Chip: No especificado";

        LogManager.i(
                Categoria.CHIP, operacion, String.format("Iniciando %s - %s", operacion, infoChip));

        return System.currentTimeMillis();
    }

    /**
     * Registra el fin de una operación de programación con resultados.
     *
     * @param operacion Nombre de la operación
     * @param inicioTimestamp Timestamp de inicio
     * @param exitosa true si fue exitosa
     * @param bytesProcessados Número de bytes procesados (-1 si no aplica)
     */
    protected void logFinOperacionChip(
            String operacion, long inicioTimestamp, boolean exitosa, int bytesProcessados) {
        long duracion = System.currentTimeMillis() - inicioTimestamp;
        String resultado = exitosa ? "ÉXITO" : "FALLO";
        String info =
                bytesProcessados >= 0
                        ? String.format(" (%d bytes procesados)", bytesProcessados)
                        : "";

        LogManager.i(
                Categoria.CHIP,
                operacion,
                String.format(
                        "%s completada - %s en %dms%s", operacion, resultado, duracion, info));
    }

    /**
     * Escribe datos al puerto USB con validación y logging automático.
     *
     * @param datos Array de bytes a escribir
     * @param timeoutMillis Timeout para la operación de escritura
     * @param descripcion Descripción de los datos para logging
     * @throws UsbCommunicationException Si ocurre error de comunicación
     */
    protected void escribirDatosUSB(byte[] datos, int timeoutMillis, String descripcion)
            throws UsbCommunicationException {
        if (usbSerialPort == null) {
            throw new UsbCommunicationException("Puerto USB no inicializado");
        }

        ByteUtils.validarArray(datos, -1, "datos");

        try {
            LogManager.v(
                    Categoria.USB,
                    "escribirDatos",
                    String.format("Escribiendo %s: %d bytes", descripcion, datos.length));

            LogManager.logDatosUSB("ENVÍO", datos, datos.length);
            usbSerialPort.write(datos, timeoutMillis);

            LogManager.v(
                    Categoria.USB,
                    "escribirDatos",
                    String.format("%s enviado exitosamente", descripcion));

        } catch (IOException e) {
            LogManager.e(
                    Categoria.USB,
                    "escribirDatos",
                    String.format("Error escribiendo %s", descripcion),
                    e);
            throw new UsbCommunicationException("Error escribiendo datos USB: " + descripcion, e);
        }
    }

    // ========== MÉTODOS ABSTRACTOS DEL PROTOCOLO ==========

    public abstract String hacerUnEco();

    public abstract boolean iniciarVariablesDeProgramacion(ChipPic chipPIC)
            throws ChipConfigurationException;

    public abstract boolean activarVoltajesDeProgramacion();

    public abstract boolean desactivarVoltajesDeProgramacion();

    public abstract boolean reiniciarVoltajesDeProgramacion();

    public abstract boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firware)
            throws ChipConfigurationException;

    public abstract boolean programarMemoriaEEPROMDelPic(ChipPic chipPIC, String firware)
            throws ChipConfigurationException;

    public abstract boolean programarFusesIDDelPic(ChipPic chipPIC, String firware);

    public abstract boolean programarCalibracionDelPic(ChipPic chipPIC, String firware);

    public abstract String leerMemoriaROMDelPic(ChipPic chipPIC);

    public abstract String leerMemoriaEEPROMDelPic(ChipPic chipPIC);

    public abstract String leerDatosDeConfiguracionDelPic();

    public abstract String leerDatosDeCalibracionDelPic();

    public abstract boolean borrarMemoriasDelPic();

    public abstract boolean verificarSiEstaBarradaLaMemoriaROMDelDelPic(ChipPic chipPIC);

    public abstract boolean verificarSiEstaBarradaLaMemoriaEEPROMDelDelPic();

    public abstract boolean programarFusesDePics18F();

    public abstract boolean detectarPicEnElSocket();

    public abstract boolean detectarSiEstaFueraElPicDelSocket();

    public abstract String obtenerVersionOModeloDelProgramador();

    public abstract String obtenerProtocoloDelProgramador();

    public abstract boolean programarVectorDeDepuracionDelPic(ChipPic chipPIC);

    public abstract String leerVectorDeDepuracionDelPic();

    public abstract boolean programarDatosDeCalibracionDePics10F();
}
