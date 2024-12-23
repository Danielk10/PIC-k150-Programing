package com.diamon.nucleo;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class Protocolo {

    protected Context contexto;

    protected UsbSerialPort usbSerialPort;

    public Protocolo(Context contexto, UsbSerialPort usbSerialPort) {

        this.contexto = contexto;

        this.usbSerialPort = usbSerialPort;
    }

    private void clearBuffer() throws IOException {

        byte[] tmpBuffer = new byte[1024];

        while (usbSerialPort.read(tmpBuffer, 100) > 0) {
            // Consumir todos los datos pendientes

        }
    }

    // Método para leer bytes del puerto serie
    protected byte[] readBytes(int count, int timeoutMillis) throws IOException {

        if (usbSerialPort == null) {
            return null;
        }

        // Verificar que el número de bytes solicitados sea válido
        if (count <= 0) {
            throw new IllegalArgumentException("El número de bytes a leer debe ser mayor que 0.");
        }

        // Crear un buffer para almacenar los datos leídos
        ByteBuffer byteBuffer = ByteBuffer.allocate(count);
        long startTime = System.currentTimeMillis();

        // Mientras no se hayan leído todos los bytes y el tiempo de espera no haya expirado
        while (byteBuffer.position() < count
                && (System.currentTimeMillis() - startTime) < timeoutMillis) {
            // Leer los bytes restantes
            int remaining = count - byteBuffer.position();
            byte[] tmpBuffer = new byte[remaining];
            int bytesRead = usbSerialPort.read(tmpBuffer, timeoutMillis);

            if (bytesRead > 0) {
                // Añadir los bytes leídos al buffer
                byteBuffer.put(tmpBuffer, 0, bytesRead);
            } else if (bytesRead == 0) {
                // Si no se reciben datos, esperar brevemente antes de reintentar
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Hilo interrumpido durante la operación de lectura.", e);
                }
            }
        }

        // Si no se alcanzaron los bytes esperados dentro del tiempo de espera, lanzar una excepción
        if (byteBuffer.position() < count) {
            throw new IOException("Tiempo de espera agotado mientras se esperaban los datos.");
        }

        // Devolver los datos leídos como un arreglo de bytes
        return byteBuffer.array();
    }

    private void expectResponse(byte[] expected, int timeoutMillis) throws IOException {
        byte[] response = readBytes(expected.length, timeoutMillis);
        if (!Arrays.equals(response, expected)) {
            throw new IOException(
                    "Expected: "
                            + Arrays.toString(expected)
                            + ", but received: "
                            + Arrays.toString(response));
        }
    }

    // Método para leer respuesta y validar contra un carácter esperado
    protected boolean leerRespuesta(byte[] response, char esperado, String mensajeError)
            throws IOException {
        usbSerialPort.read(response, 100);
        if (response[0] != (byte) esperado) {
            System.err.println(mensajeError);
            return false;
        }
        return true;
    }

    protected void enviarComando(String comando) {

        if (usbSerialPort == null) {

            return;
        }
        try {
            byte[] data = new byte[comando.length()];

            for (int i = 0; i < comando.length(); i++) {

                data[i] = Byte.parseByte(comando);
            }

            usbSerialPort.write(new byte[] {0x01}, 100);

            expectResponse(new byte[] {'Q'}, 500);

            // Enviar 'P' para ir a la tabla de salto.
            usbSerialPort.write(new byte[] {'P'}, 100);

            byte[] ack = readBytes(1, 100);

            if (ack[0] != 'P') {

                throw new IOException("No se recibio P");
            }

            // Enviar el número del comando, si es necesario.
            if (data[0] != 0) {

                usbSerialPort.write(data, 100);
            }

        } catch (NumberFormatException e) {

        } catch (IOException e) {
        }
    }

    public boolean iniciarProtocolo() {

        if (usbSerialPort == null) {
            return false;
        }

        String comando = "P";

        try {

            byte[] data = comando.getBytes(StandardCharsets.US_ASCII);

            usbSerialPort.write(data, 100);

            byte[] respuesta = readBytes(1, 100);

            if (respuesta.length > 1) {

                clearBuffer();

                respuesta[0] = 'P';
            }

            if (respuesta[0] == 'P') {

                return true;
            } else {
                return false;
            }

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    public boolean esperarInicioDeNuevoComando() {

        if (usbSerialPort == null) {

            return false;
        }

        try {

            byte[] data = {0};

            usbSerialPort.write(data, 100);

            return true;

        } catch (NumberFormatException e) {

            return false;

        } catch (IOException e) {

            return false;
        }
    }

    protected boolean researComandos() {

        enviarComando("0");

        return true;
    }

    public abstract String hacerUnEco();

    public abstract boolean iniciarVariablesDeProgramacion(ChipPic chipPIC);

    public abstract boolean activarVoltajesDeProgramacion();

    public abstract boolean desactivarVoltajesDeProgramacion();

    public abstract boolean reiniciarVoltajesDeProgramacion();

    public abstract boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firware);

    public abstract boolean programarMemoriaEEPROMDelPic(ChipPic chipPIC, String firware);

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
