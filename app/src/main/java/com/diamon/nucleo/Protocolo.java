package com.diamon.nucleo;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.excepciones.UsbCommunicationException;
import com.diamon.utilidades.ByteUtils;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
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
     * Tamaño mínimo de buffer para lecturas USB.
     *
     * <p>El driver FTDI ({@code FtdiSerialDriver}) antepone dos bytes de estado
     * a cada paquete y lanza {@link IllegalArgumentException} si el buffer de
     * destino tiene dos bytes o menos. Con 3 bytes se garantiza al menos un
     * byte útil y se evita el crash "Read buffer too small".
     */
    private static final int MIN_READ_BUFFER_SIZE = 3;

    /**
     * Bytes ya recibidos del puerto USB que aún no se han entregado al llamador.
     *
     * <p>El driver FTDI exige búferes de al menos 3 bytes, por lo que una
     * lectura de 1 byte puede arrastrar el byte siguiente (por ejemplo la 'P'
     * final tras la 'Y' de un bloque). Esos bytes se guardan aquí para la
     * próxima lectura en lugar de descartarlos.
     */
    private final Deque<Byte> bytesPendientes = new ArrayDeque<>();

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
     * <p>Cambiado de private a protected para permitir que las subclases lo invoquen
     * antes de inicializar variables de programación tras un cambio de chip,
     * evitando que bytes residuales corrompan el siguiente comando.
     *
     * @throws UsbCommunicationException Si ocurre un error durante la limpieza
     */
    protected void clearBuffer() throws UsbCommunicationException {
        if (usbSerialPort == null) {
            throw new UsbCommunicationException("Puerto USB no inicializado");
        }

        synchronized (bytesPendientes) {
            bytesPendientes.clear();
        }

        try {
            byte[] tmpBuffer = new byte[1024];
            int totalLimpiado = 0;
            int bytesLeidos;

            while ((bytesLeidos = usbSerialPort.read(tmpBuffer, 10)) > 0) {
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
            // Buffer final para los datos leídos
            byte[] resultado = new byte[count];
            int totalBytesRead = extraerPendientes(resultado, 0, count);
            long startTime = System.currentTimeMillis();

            // Buffer temporal reutilizable fuera del bucle para eliminar la presión sobre el Garbage Collector
            int chunkCapacity = Math.max(Math.min(count, 1024), MIN_READ_BUFFER_SIZE);
            byte[] tmpBuffer = new byte[chunkCapacity];

            // Mientras no se hayan leído todos los bytes y el tiempo de espera no haya expirado
            while (totalBytesRead < count
                    && (System.currentTimeMillis() - startTime) < timeoutMillis) {

                int remaining = count - totalBytesRead;
                // Nunca pasar al driver un buffer de 1 o 2 bytes: FTDI necesita
                // espacio para sus 2 bytes de estado (READ_HEADER_LENGTH = 2).
                int readCapacity =
                        Math.max(Math.min(remaining, tmpBuffer.length), MIN_READ_BUFFER_SIZE);
                byte[] targetBuf = (readCapacity == tmpBuffer.length)
                        ? tmpBuffer
                        : new byte[readCapacity];

                int bytesRead;
                try {
                    bytesRead = usbSerialPort.read(targetBuf, Math.min(timeoutMillis, 100));
                } catch (RuntimeException e) {
                    // Los drivers USB de terceros pueden lanzar excepciones no
                    // comprobadas (p. ej. "Read buffer too small"). Convertirlas
                    // evita que un fallo de lectura tumbe el hilo de la app.
                    throw new UsbCommunicationException(
                            "Error del driver USB durante lectura de "
                                    + count
                                    + " bytes (búfer de "
                                    + targetBuf.length
                                    + " bytes)",
                            e);
                }

                if (bytesRead > 0) {
                    int bytesToCopy = Math.min(bytesRead, Math.min(targetBuf.length, remaining));
                    System.arraycopy(targetBuf, 0, resultado, totalBytesRead, bytesToCopy);
                    totalBytesRead += bytesToCopy;
                    guardarPendientes(targetBuf, bytesToCopy, bytesRead - bytesToCopy);

                } else if (bytesRead == 0) {
                    // Si no se reciben datos, esperar brevemente antes de reintentar
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new UsbCommunicationException("Hilo interrumpido durante lectura", e);
                    }
                } else {
                    // bytesRead < 0: desconexión física de hardware o EOF USB
                    throw UsbCommunicationException.crearConexionPerdida();
                }
            }

            // Verificar si se leyeron todos los bytes
            if (totalBytesRead < count) {
                long tiempoTranscurrido = System.currentTimeMillis() - startTime;
                String mensaje =
                        String.format(
                                "Timeout leyendo bytes: esperados=%d, leídos=%d, tiempo=%dms",
                                count, totalBytesRead, tiempoTranscurrido);
                throw UsbCommunicationException.crearTimeoutError("lectura", timeoutMillis);
            }

            return resultado;

        } catch (IOException e) {
            throw new UsbCommunicationException("Error I/O durante lectura USB", e);
        }
    }

    /**
     * Lee hasta completar el buffer indicado o agotar el timeout, usando
     * buffers internos de al menos {@link #MIN_READ_BUFFER_SIZE} bytes.
     *
     * <p>Se usa para lecturas puntuales de 1 o 2 bytes (por ejemplo respuestas
     * {@code 'Y'} o {@code 'N'}) que de otro modo hacen fallar al driver FTDI
     * con {@code IllegalArgumentException: Read buffer too small}. El bucle
     * también permite reconstruir respuestas de varios bytes cuando FTDI
     * entrega un byte útil por transferencia.
     *
     * @param destino Buffer donde se copiarán los bytes leídos
     * @param timeoutMillis Tiempo máximo de espera en milisegundos
     * @return Número de bytes copiados en {@code destino}; 0 si no hubo datos
     * @throws IOException Si ocurre un error de comunicación USB
     */
    protected int readDisponible(byte[] destino, int timeoutMillis) throws IOException {
        if (destino == null || destino.length == 0) {
            return 0;
        }
        if (usbSerialPort == null) {
            throw new IOException("Puerto USB no inicializado");
        }

        int totalLeidos = extraerPendientes(destino, 0, destino.length);
        long startTime = System.currentTimeMillis();

        while (totalLeidos < destino.length
                && (System.currentTimeMillis() - startTime) < timeoutMillis) {

            int restante = destino.length - totalLeidos;
            int capacidad = Math.max(restante, MIN_READ_BUFFER_SIZE);
            byte[] buffer = new byte[capacidad];

            int leidos;
            try {
                leidos = usbSerialPort.read(buffer, Math.min(timeoutMillis, 100));
            } catch (RuntimeException e) {
                throw new IOException("Error del driver USB durante lectura", e);
            }

            if (leidos > 0) {
                int copiados = Math.min(leidos, restante);
                System.arraycopy(buffer, 0, destino, totalLeidos, copiados);
                totalLeidos += copiados;
                guardarPendientes(buffer, copiados, leidos - copiados);
            } else if (leidos < 0) {
                // Desconexión física o EOF del puerto USB.
                synchronized (bytesPendientes) {
                    bytesPendientes.clear();
                }
                break;
            }
            // leidos == 0: reintentar hasta que expire el timeout.
        }

        return totalLeidos;
    }

    /**
     * Extrae bytes del búfer de sobrantes hacia {@code destino}.
     *
     * @param destino Buffer de salida
     * @param offset Posición inicial en {@code destino}
     * @param maximo Número máximo de bytes a extraer
     * @return Bytes extraídos
     */
    private int extraerPendientes(byte[] destino, int offset, int maximo) {
        int copiados = 0;
        synchronized (bytesPendientes) {
            while (copiados < maximo && !bytesPendientes.isEmpty()) {
                destino[offset + copiados] = bytesPendientes.removeFirst();
                copiados++;
            }
        }
        return copiados;
    }

    /**
     * Guarda en el búfer de sobrantes los bytes leídos que no se entregaron al
     * llamador, para no perderlos.
     *
     * @param origen Buffer con los datos leídos
     * @param offset Posición del primer byte sobrante
     * @param cantidad Número de bytes sobrantes
     */
    private void guardarPendientes(byte[] origen, int offset, int cantidad) {
        if (cantidad <= 0) {
            return;
        }
        synchronized (bytesPendientes) {
            for (int i = 0; i < cantidad; i++) {
                bytesPendientes.addLast(origen[offset + i]);
            }
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
        ByteUtils.validarArray(contexto, expected, -1, "expected");

        byte[] response = readBytes(expected.length, timeoutMillis);

        if (!Arrays.equals(response, expected)) {
            String esperadoHex = ByteUtils.bytesToHex(expected);
            String recibidoHex = ByteUtils.bytesToHex(response);
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
        return leerRespuesta(response, esperado, mensajeError, 500);
    }

    protected boolean leerRespuesta(
            byte[] response, char expected, String errorMessage, int timeoutMs)
            throws UsbCommunicationException {
        ByteUtils.validarArray(contexto, response, -1, "response");

        if (response.length == 0) {
            throw new IllegalArgumentException("Array de respuesta debe tener al menos 1 byte");
        }

        try {
            byte[] respuestaBytes = readBytes(1, timeoutMs);
            response[0] = respuestaBytes[0];
            return (respuestaBytes[0] == expected);
        } catch (Exception e) {
            throw new UsbCommunicationException("Error leyendo respuesta USB: " + errorMessage, e);
        }
    }

    /**
     * Envía un comando numérico (opcode) al dispositivo USB siguiendo el protocolo Kitsrus/P18A:
     * 1. 0x01 -> espera 'Q'
     * 2. 'P'  -> espera 'P' (entra a la tabla de salto)
     * 3. opcode (1 byte)
     *
     * @param opcode Código de operación a ejecutar
     * @throws UsbCommunicationException Si ocurre error durante el envío
     */
    public void enviarComandoOpcode(int opcode) throws UsbCommunicationException {
        if (usbSerialPort == null) {
            throw new UsbCommunicationException("Puerto USB no inicializado");
        }

        try {
            // Paso 0: Limpiar residuos previos en el búfer
            try {
                clearBuffer();
            } catch (Exception ignored) {
            }

            // Paso 1: Enviar 0x01 para inicializar
            byte[] inicializacion = ByteUtils.prepararDatosUSB(contexto, new byte[] {0x01}, "inicializacion");
            usbSerialPort.write(inicializacion, 100);
            // Paso 2: Esperar respuesta 'Q'
            expectResponse(new byte[] {'Q'}, 500);

            // Paso 3: Enviar 'P' para ir a la tabla de salto
            byte[] salto = ByteUtils.prepararDatosUSB(contexto, new byte[] {'P'}, "salto_tabla");
            usbSerialPort.write(salto, 100);

            // Paso 4: Leer acknowledgment 'P'
            byte[] ack = readBytes(1, 500);
            if (ack[0] != 'P') {
                String mensaje =
                        String.format(
                                "Acknowledgment inválido: esperado 'P', recibido 0x%02X", ack[0]);
                throw UsbCommunicationException.crearRespuestaInesperada(
                        "P", String.format("0x%02X", ack[0]), "enviarComandoOpcode");
            }

            // Paso 5: Enviar el número del comando si es distinto de 0 (comando 0 solo resetea a wait)
            if (opcode != 0) {
                byte[] comandoFinal = ByteUtils.prepararDatosUSB(contexto, new byte[] {(byte) opcode}, "comando_final");
                usbSerialPort.write(comandoFinal, 100);
            }

        } catch (IOException e) {
            throw new UsbCommunicationException("Error I/O enviando comando", e);
        }
    }

    /**
     * Envía un comando al dispositivo USB siguiendo el protocolo establecido.
     *
     * @param comando String con el número de comando a enviar
     * @throws UsbCommunicationException Si ocurre error durante el envío
     */
    protected void enviarComando(String comando) throws UsbCommunicationException {
        if (comando == null || comando.trim().isEmpty()) {
            throw new IllegalArgumentException("Comando no puede ser null o vacío");
        }

        int opcode;
        try {
            opcode = Integer.parseInt(comando.trim());
        } catch (NumberFormatException e) {
            String mensaje = String.format("Comando inválido: '%s' - no es un número válido", comando);
            throw new IllegalArgumentException(mensaje, e);
        }

        enviarComandoOpcode(opcode);
    }

    /**
     * Inicializa el protocolo de comunicación con el dispositivo.
     * Realiza una sincronización determinista tolerante a residuos en el bus serie.
     *
     * @return true si la inicialización fue exitosa, false en caso contrario
     */
    public boolean iniciarProtocolo() {
        if (usbSerialPort == null) {
            return false;
        }

        // Realizar hasta 5 intentos de sincronización universal determinista
        for (int intento = 1; intento <= 5; intento++) {
            try {
                // 1. Limpiar cualquier residuo previo en los búferes del puerto
                try {
                    clearBuffer();
                } catch (Exception ignored) {
                }

                // 2. Enviar 0x01 (abortar estado previo)
                byte[] abortCmd = new byte[] { 0x01 };
                usbSerialPort.write(abortCmd, 100);

                // Leer búfer buscando 'Q' (consumiendo cualquier residuo previo como 'A' o basuras)
                long startWait = System.currentTimeMillis();
                byte[] readBuf = new byte[64];
                boolean foundQ = false;
                while (System.currentTimeMillis() - startWait < 400) {
                    int len = usbSerialPort.read(readBuf, 100);
                    if (len > 0) {
                        for (int i = 0; i < len; i++) {
                            if (readBuf[i] == 'Q') {
                                foundQ = true;
                                break;
                            } else if (readBuf[i] == 'P') {
                                // Ya respondió 'P', está directamente en el Jump Table
                                return true;
                            }
                        }
                        if (foundQ) break;
                    }
                }

                if (foundQ) {
                    // Respondió 'Q', ahora solicitar entrada a Command Jump Table con 'P'
                    try { clearBuffer(); } catch (Exception ignored) {}
                    byte[] pCmd = new byte[] { 'P' };
                    usbSerialPort.write(pCmd, 100);

                    startWait = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startWait < 400) {
                        int len = usbSerialPort.read(readBuf, 100);
                        if (len > 0) {
                            for (int i = 0; i < len; i++) {
                                if (readBuf[i] == 'P') {
                                    return true;
                                }
                            }
                        }
                    }
                }

                // 3. Fallback directo: enviar 'P' y esperar 'P' (Power-up directo)
                try {
                    clearBuffer();
                } catch (Exception ignored) {
                }
                byte[] pCmd = new byte[] { 'P' };
                usbSerialPort.write(pCmd, 100);
                startWait = System.currentTimeMillis();
                while (System.currentTimeMillis() - startWait < 400) {
                    int len = usbSerialPort.read(readBuf, 100);
                    if (len > 0) {
                        for (int i = 0; i < len; i++) {
                            if (readBuf[i] == 'P') {
                                return true;
                            }
                        }
                    }
                }

                Thread.sleep(80);

            } catch (Exception e) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
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

            byte[] data = ByteUtils.prepararDatosUSB(contexto, new byte[] {0}, "sincronizacion");
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
    public boolean resetearComandos() {
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

        ByteUtils.validarArray(contexto, datos, -1, "datos");

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

    public abstract boolean programarMemoriaROMDelPic(ChipPic chipPIC, DatosPicProcesados datosPic)
            throws ChipConfigurationException;

    public abstract boolean programarMemoriaEEPROMDelPic(ChipPic chipPIC, DatosPicProcesados datosPic)
            throws ChipConfigurationException;

    public abstract boolean programarFusesIDDelPic(
            ChipPic chipPIC, DatosPicProcesados datosPic, byte[] IDPic, List<Integer> fusesUsuario);

    public abstract boolean programarCalibracionDelPic(ChipPic chipPIC, DatosPicProcesados datosPic);

    public abstract String leerMemoriaROMDelPic(ChipPic chipPIC);

    public abstract String leerMemoriaEEPROMDelPic(ChipPic chipPIC);

    public abstract String leerDatosDeConfiguracionDelPic();

    public abstract String leerDatosDeCalibracionDelPic();

    public abstract boolean borrarMemoriasDelPic();

    public abstract boolean verificarSiEstaBorradaLaMemoriaROMDelPic(ChipPic chipPIC);

    public abstract boolean verificarSiEstaBorradaLaMemoriaEEPROMDelPic();

    public abstract boolean programarFusesDePics18F();

    public abstract boolean detectarPicEnElSocket();

    /**
     * Realiza una sonda segura y no bloqueante para determinar si hay un microcontrolador
     * presente en el zócalo ZIF. Si el zócalo está vacío, no se cuelga ni bloquea el firmware.
     */
    public boolean sondaPicPresenteEnSocket() {
        return detectarPicEnElSocket();
    }

    public abstract boolean detectarSiEstaFueraElPicDelSocket();

    public abstract boolean esperarExtraccionDelSocket(long timeoutMs, java.util.concurrent.atomic.AtomicBoolean cancelar);

    public abstract boolean esperarInsercionEnElSocket(long timeoutMs, java.util.concurrent.atomic.AtomicBoolean cancelar);

    public abstract String obtenerVersionOModeloDelProgramador();

    public abstract String obtenerProtocoloDelProgramador();

    public abstract boolean programarVectorDeDepuracionDelPic(ChipPic chipPIC);

    public abstract String leerVectorDeDepuracionDelPic();

    public abstract boolean programarDatosDeCalibracionDePics10F();

    public boolean programarCalibracionDelPic(ChipPic chipPIC, int calibrate) {
        return false;
    }

    public boolean programarCalibracionDelPic(ChipPic chipPIC, int calibrate, int fuse) {
        return false;
    }

    public boolean programarVectorDeDepuracionDelPic(int address) {
        return false;
    }

    public abstract int leerDeviceIDDelSocket();
}
