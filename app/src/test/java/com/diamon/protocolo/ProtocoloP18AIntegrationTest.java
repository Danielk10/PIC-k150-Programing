package com.diamon.protocolo;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Pruebas de integración para validar la lógica de ProtocoloP18A en local
 * comunicando con el emulador K150 (vtty).
 */
public class ProtocoloP18AIntegrationTest {

    private static final String VTTY_PATH = "/home/danielpdiamon/emulador_picpro/vtty";
    private static final String HEX_FILE_PATH = "/home/danielpdiamon/pwmc_main107_628A.HEX";

    private FileInputStream in;
    private FileOutputStream out;
    private UsbSerialPort mockUsbPort;
    private Context mockContext;
    private ProtocoloP18A protocolo;
    private ChipPic chip16f628a;

    @Before
    public void setUp() throws Exception {
        // Validar que el puerto virtual del emulador esté listo
        File vtty = new File(VTTY_PATH);
        assertTrue("El puerto virtual " + VTTY_PATH + " no existe. ¿Está corriendo el emulador?", vtty.exists());

        // Abrir flujos al PTY
        in = new FileInputStream(vtty);
        out = new FileOutputStream(vtty);

        // Limpiar cualquier byte basura previo
        while (in.available() > 0) {
            in.read();
        }

        // Resetear la máquina de estados del emulador al estado AWAITING_JUMP_TABLE
        out.write(new byte[]{0x01});
        out.flush();

        // Esperar y consumir la respuesta 'Q' del emulador
        long startWait = System.currentTimeMillis();
        while (in.available() == 0 && (System.currentTimeMillis() - startWait) < 500) {
            Thread.sleep(5);
        }
        while (in.available() > 0) {
            in.read();
        }

        // Mock de UsbSerialPort delegado a los flujos reales del PTY
        mockUsbPort = mock(UsbSerialPort.class);

        // Implementación de escritura
        doAnswer(invocation -> {
            byte[] src = invocation.getArgument(0);
            out.write(src);
            out.flush();
            return null;
        }).when(mockUsbPort).write(any(byte[].class), anyInt());

        // Implementación de lectura con control de timeout
        when(mockUsbPort.read(any(byte[].class), anyInt())).thenAnswer(invocation -> {
            byte[] dest = invocation.getArgument(0);
            int timeout = invocation.getArgument(1);
            long start = System.currentTimeMillis();
            int totalRead = 0;
            
            while (totalRead < dest.length && (System.currentTimeMillis() - start) < timeout) {
                int avail = in.available();
                if (avail > 0) {
                    int toRead = Math.min(avail, dest.length - totalRead);
                    int readNow = in.read(dest, totalRead, toRead);
                    if (readNow > 0) {
                        totalRead += readNow;
                    }
                } else {
                    Thread.sleep(5);
                }
            }
            return totalRead;
        });

        // Mock de Context
        mockContext = mock(Context.class);
        when(mockContext.getString(anyInt())).thenReturn("msg");
        when(mockContext.getString(anyInt(), any())).thenReturn("msg");

        // Instanciar protocolo P18A
        protocolo = new ProtocoloP18A(mockContext, mockUsbPort, TipoProtocolo.P18A);

        // Crear configuración de ChipPic para PIC16F628A
        Map<String, Object> fuses = new HashMap<>();
        chip16f628a = new ChipPic(
                "16F628A",
                "Y",
                "18pin",
                "2",
                "Y",
                "Vpp2Vcc",
                "50",
                "1",
                "0",
                "bit14_B",
                "000800",
                "00000080",
                new String[] { "3FFF" },
                "N",
                "N",
                "N",
                "N",
                "1060",
                fuses);
    }

    @After
    public void tearDown() throws Exception {
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
    }

    @Test
    public void testHandshakeYComandosBasicos() {
        // 1. Inicializar Protocolo ('P')
        boolean initOk = protocolo.iniciarProtocolo();
        assertTrue("Fallo iniciarProtocolo", initOk);

        // 2. Hacer Eco
        String ecoStr = protocolo.hacerUnEco();
        assertEquals("El eco no coincide", "\u0002", ecoStr);

        // 3. Obtener versión / modelo del programador
        String version = protocolo.obtenerVersionOModeloDelProgramador();
        assertEquals("Debe reportar K150 (modelo 3)", "K150", version);

        // 4. Obtener protocolo
        String protoStr = protocolo.obtenerProtocoloDelProgramador();
        assertTrue("El protocolo debe contener P18A", protoStr.contains("P18A"));
    }

    @Test
    public void testDeteccionSocketYBorrado() throws Exception {
        // 1. Detectar PIC en el Socket
        boolean picEnSocket = protocolo.detectarPicEnElSocket();
        assertTrue("El emulador debería indicar que el PIC está en el socket", picEnSocket);

        // 2. Iniciar variables del chip PIC16F628A
        boolean varsOk = protocolo.iniciarVariablesDeProgramacion(chip16f628a);
        assertTrue("Fallo al inicializar variables de programación del chip", varsOk);

        // 3. Borrar la memoria del chip
        boolean eraseOk = protocolo.borrarMemoriasDelPic();
        assertTrue("Fallo al borrar memorias del PIC", eraseOk);
    }

    @Test
    public void testProgramacionYLecturaCompleta() throws Exception {
        // Iniciar variables de programación del chip
        assertTrue(protocolo.iniciarVariablesDeProgramacion(chip16f628a));

        // Leer archivo HEX de prueba
        File hexFile = new File(HEX_FILE_PATH);
        assertTrue("El archivo HEX de prueba no existe", hexFile.exists());
        String hexContent = new String(Files.readAllBytes(Paths.get(HEX_FILE_PATH)), StandardCharsets.UTF_8);

        // Programar ROM
        com.diamon.datos.DatosPicProcesados datosPic = new com.diamon.datos.DatosPicProcesados(mockContext, hexContent, chip16f628a);
        datosPic.iniciarProcesamientoDeDatos(); // Esto lanzará la excepción real si falla el parseo
        
        boolean programOk = protocolo.programarMemoriaROMDelPic(chip16f628a, hexContent);
        assertTrue("Fallo al programar la memoria ROM", programOk);

        // Leer ROM programada
        String romLeida = protocolo.leerMemoriaROMDelPic(chip16f628a);
        assertNotNull("La ROM leída no puede ser null", romLeida);
        assertFalse("La ROM leída no debe contener error", romLeida.startsWith("Error"));
        assertTrue("La ROM leída no debe estar vacía", romLeida.length() > 0);
    }
}
