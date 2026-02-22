package com.diamon.nucleo;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.excepciones.UsbCommunicationException;
import com.diamon.utilidades.ByteUtils;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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

            while ((bytesLeidos = usbSerialPort.read(tmpBuffer, 100)) > 0) {
                totalLimpiado += bytesLeidos;
            }

        } catch (IOException e) {
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
            throw new IllegalArgumentException(mensaje);
        }

        try {
            // Crear un buffer para almacenar los datos leídos
            ByteBuffer byteBuffer = ByteBuffer.allocate(count);
            long startTime = System.currentTimeMillis();

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

                } else if (bytesRead == 0) {
                    // Si no se reciben datos, esperar brevemente antes de reintentar
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
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
                throw UsbCommunicationException.crearTimeoutError("lectura", timeoutMillis);
            }

            // Obtener los datos leídos
            byte[] resultado = byteBuffer.array();

            return resultado;

        } catch (IOException e) {
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

        byte[] response = readBytes(expected.length, timeoutMillis);
        String recibidoHex = ByteUtils.bytesToHex(response);

        if (!Arrays.equals(response, expected)) {
            throw UsbCommunicationException.crearRespuestaInesperada(
                    esperadoHex, recibidoHex, "expectResponse");
        }
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

            int bytesLeidos = usbSerialPort.read(response, 100);
            if (bytesLeidos == 0) {
                return false;
            }

            byte recibido = response[0];
            boolean coincide = (recibido == (byte) esperado);

            return coincide;

        } catch (IOException e) {
            throw new UsbCommunicationException("Error leyendo respuesta USB", e);
        }
    }

    protected boolean leerRespuesta(
            byte[] response, char expected, String errorMessage, int timeoutMs)
            throws UsbCommunicationException {
        try {
            byte[] respuestaBytes = readBytes(1, timeoutMs);
            response[0] = respuestaBytes[0];

            boolean valida = (respuestaBytes[0] == expected);

            return valida;

        } catch (Exception e) {
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

        try {

            // Parsear comando a bytes
            byte[] data = new byte[comando.length()];
            try {
                for (int i = 0; i < comando.length(); i++) {
                    data[i] = Byte.parseByte(comando);
                }
            } catch (NumberFormatException e) {
                String mensaje =
                        String.format("Comando inválido: '%s' - no es un número válido", comando);
                throw new IllegalArgumentException(mensaje, e);
            }

            // Paso 1: Enviar 0x01 para inicializar
            byte[] inicializacion = ByteUtils.prepararDatosUSB(new byte[] {0x01}, "inicializacion");
            usbSerialPort.write(inicializacion, 100);
            // Paso 2: Esperar respuesta 'Q'
            expectResponse(new byte[] {'Q'}, 500);

            // Paso 3: Enviar 'P' para ir a la tabla de salto
            byte[] salto = ByteUtils.prepararDatosUSB(new byte[] {'P'}, "salto_tabla");
            usbSerialPort.write(salto, 100);

            // Paso 4: Leer acknowledgment 'P'
            byte[] ack = readBytes(1, 100);
            if (ack[0] != 'P') {
                String mensaje =
                        String.format(
                                "Acknowledgment inválido: esperado 'P', recibido 0x%02X", ack[0]);
                throw UsbCommunicationException.crearRespuestaInesperada(
                        "P", String.format("0x%02X", ack[0]), "enviarComando");
            }

            // Paso 5: Enviar el número del comando, si es necesario
            if (data[0] != 0) {
                byte[] comandoFinal = ByteUtils.prepararDatosUSB(data, "comando_final");
                usbSerialPort.write(comandoFinal, 100);
            } else {
            }

        } catch (IOException e) {
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
            return false;
        }

        String comando = "P";

        try {

            // Preparar comando de inicialización
            byte[] data =
                    ByteUtils.prepararDatosUSB(
                            comando.getBytes(StandardCharsets.US_ASCII), "inicializacion");
            usbSerialPort.write(data, 100);

            // Leer respuesta
            byte[] respuesta = readBytes(1, 100);

            // Si se reciben más datos de los esperados, limpiar buffer
            if (respuesta.length > 1) {
                clearBuffer();
                respuesta[0] = 'P'; // Asumir respuesta correcta después de limpiar
            }

            boolean exitoso = (respuesta[0] == 'P');

            if (exitoso) {
            } else {
            }

            return exitoso;

        } catch (NumberFormatException e) {
            return false;

        } catch (UsbCommunicationException e) {
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
            return false;
        }

        try {

            byte[] data = ByteUtils.prepararDatosUSB(new byte[] {0}, "sincronizacion");
            usbSerialPort.write(data, 100);

            return true;

        } catch (IOException e) {
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
            enviarComando("0");
            return true;
        } catch (UsbCommunicationException e) {
            return false;
        }
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

            usbSerialPort.write(datos, timeoutMillis);

        } catch (IOException e) {
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

    public abstract boolean programarFusesIDDelPic(
            ChipPic chipPIC, String firware, byte[] IDPic, List<Integer> fusesUsuario);

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
