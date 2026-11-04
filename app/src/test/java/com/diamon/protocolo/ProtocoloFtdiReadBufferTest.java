package com.diamon.protocolo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Regresión para Crashlytics 98da621c: el driver FTDI lanza
 * {@code IllegalArgumentException: Read buffer too small} cuando se le pasa un
 * buffer de 2 bytes o menos. El protocolo debe usar siempre un buffer de al
 * menos 3 bytes, incluso para respuestas de un solo byte.
 */
public class ProtocoloFtdiReadBufferTest {

    @Test
    public void resetearComandosYLeerRespuestaUnByteNoFallanConFtdi() throws Exception {
        UsbSerialPort usbPort = mock(UsbSerialPort.class);
        Context context = mock(Context.class);

        // Respuestas esperadas:
        //  - 'Q' y 'P' del reset inicial del test.
        //  - 'Q' y 'P' del reset interno de verificarSiEstaBorradaLaMemoriaEEPROMDelPic().
        //  - 'Y' del erase-check de EEPROM.
        //  - 'Q' y 'P' del reset final del método.
        Queue<Byte> respuestas =
                new ArrayDeque<>(
                        Arrays.asList(
                                (byte) 'Q', (byte) 'P',
                                (byte) 'Q', (byte) 'P',
                                (byte) 'Y',
                                (byte) 'Q', (byte) 'P'));
        AtomicInteger bufferMinimo = new AtomicInteger(Integer.MAX_VALUE);

        when(usbPort.read(any(byte[].class), anyInt()))
                .thenAnswer(
                        invocation -> {
                            byte[] destino = invocation.getArgument(0);
                            bufferMinimo.set(Math.min(bufferMinimo.get(), destino.length));

                            // Comportamiento real de FtdiSerialDriver: exige
                            // más de READ_HEADER_LENGTH (2) bytes.
                            if (destino.length <= 2) {
                                throw new IllegalArgumentException("Read buffer too small");
                            }

                            // clearBuffer() usa 1024 bytes y debe terminar sin datos.
                            if (destino.length > 3) {
                                return 0;
                            }

                            Byte respuesta = respuestas.poll();
                            if (respuesta == null) {
                                return 0;
                            }
                            destino[0] = respuesta;
                            return 1;
                        });

        ProtocoloP18A protocolo = new ProtocoloP18A(context, usbPort);

        assertTrue(protocolo.resetearComandos());
        assertTrue(protocolo.verificarSiEstaBorradaLaMemoriaEEPROMDelPic());
        assertTrue(
                "Se pasó al driver un buffer menor de 3 bytes", bufferMinimo.get() >= 3);
        assertEquals(0, respuestas.size());
    }
}
