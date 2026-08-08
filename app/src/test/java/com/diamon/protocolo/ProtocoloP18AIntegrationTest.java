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
        
        boolean programOk = protocolo.programarMemoriaROMDelPic(chip16f628a, datosPic);
        assertTrue("Fallo al programar la memoria ROM", programOk);

        // Leer ROM programada
        String romLeida = protocolo.leerMemoriaROMDelPic(chip16f628a);
        assertNotNull("La ROM leída no puede ser null", romLeida);
        assertFalse("La ROM leída no debe contener error", romLeida.startsWith("Error"));
        assertTrue("La ROM leída no debe estar vacía", romLeida.length() > 0);
    }

    @Test
    public void testEEPROMProgramacionYLectura() throws Exception {
        // Iniciar variables de programación del chip
        assertTrue(protocolo.iniciarVariablesDeProgramacion(chip16f628a));

        // Leer archivo HEX de prueba
        String hexContent = new String(Files.readAllBytes(Paths.get(HEX_FILE_PATH)), StandardCharsets.UTF_8);

        // Programar EEPROM
        com.diamon.datos.DatosPicProcesados datosPic = new com.diamon.datos.DatosPicProcesados(mockContext, hexContent, chip16f628a);
        datosPic.iniciarProcesamientoDeDatos();
        boolean programOk = protocolo.programarMemoriaEEPROMDelPic(chip16f628a, datosPic);
        assertTrue("Fallo al programar la memoria EEPROM", programOk);

        // Leer EEPROM programada
        String eepromLeida = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        assertNotNull("La EEPROM leída no puede ser null", eepromLeida);
        assertFalse("La EEPROM leída no debe contener error", eepromLeida.startsWith("Error"));
        assertTrue("La EEPROM leída no debe estar vacía", eepromLeida.length() > 0);
    }

    @Test
    public void testFusesYConfiguracion() throws Exception {
        // Iniciar variables de programación del chip
        assertTrue(protocolo.iniciarVariablesDeProgramacion(chip16f628a));

        // Leer archivo HEX de prueba
        String hexContent = new String(Files.readAllBytes(Paths.get(HEX_FILE_PATH)), StandardCharsets.UTF_8);

        // Programar Fuses e ID (VID, PID o config del PIC)
        com.diamon.datos.DatosPicProcesados datosPic = new com.diamon.datos.DatosPicProcesados(mockContext, hexContent, chip16f628a);
        datosPic.iniciarProcesamientoDeDatos();
        byte[] idPic = new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44 };
        java.util.List<Integer> fuses = java.util.Arrays.asList(0x3F74);
        boolean fusesOk = protocolo.programarFusesIDDelPic(chip16f628a, datosPic, idPic, fuses);
        assertTrue("Fallo al programar Fuses e ID", fusesOk);

        // Leer configuración
        String config = protocolo.leerDatosDeConfiguracionDelPic();
        assertNotNull("La configuración leída no puede ser null", config);
        assertFalse("La configuración leída no debe contener error", config.startsWith("Error"));
        assertTrue("La configuration leída debe contener el ID grabado", config.contains("11223344"));
    }

    @Test
    public void testImportacionYProgramacionIntercaladaParcial() throws Exception {
        // Paths a probar
        String[] hexPaths = {
            "/home/danielpdiamon/pwmc_main107_628A.HEX",
            "/home/danielpdiamon/PIC-k150-Programing/main.hex",
            "/home/danielpdiamon/PIC-k150-Programing/nuevoled.hex"
        };

        com.diamon.datos.DatosPicProcesados[] parsedHexes = new com.diamon.datos.DatosPicProcesados[3];

        // 1. Validar la importación y procesamiento de todos los archivos HEX
        for (int i = 0; i < hexPaths.length; i++) {
            File f = new File(hexPaths[i]);
            assertTrue("El archivo HEX " + hexPaths[i] + " no existe", f.exists());
            String hexContent = new String(Files.readAllBytes(Paths.get(hexPaths[i])), StandardCharsets.UTF_8);
            
            // Probamos pasando null como Context para validar que funciona sin dependencias de Android
            parsedHexes[i] = new com.diamon.datos.DatosPicProcesados(null, hexContent, chip16f628a);
            parsedHexes[i].iniciarProcesamientoDeDatos();
            
            byte[] romBytes = parsedHexes[i].obtenerBytesHexROMPocesado();
            assertNotNull("Los bytes ROM parseados no deben ser null", romBytes);
            assertTrue("Debe contener bytes ROM", romBytes.length > 0);
        }

        // 2. Iniciar variables de programación del chip
        assertTrue(protocolo.iniciarVariablesDeProgramacion(chip16f628a));

        // 3. Borrar el chip completo inicialmente (Paso base)
        assertTrue("Fallo al borrar el chip", protocolo.borrarMemoriasDelPic());

        // 4. Programar ROM con el segundo HEX (main.hex)
        assertTrue("Fallo al programar ROM", protocolo.programarMemoriaROMDelPic(chip16f628a, parsedHexes[1]));

        // 5. Leer ROM y verificar que esté programada y coincida, pero la EEPROM siga limpia (todos 0xFF en el emulador)
        String romOriginal = protocolo.leerMemoriaROMDelPic(chip16f628a);
        assertNotNull("ROM leída no debe ser null", romOriginal);
        assertFalse("ROM leída no debe contener error", romOriginal.startsWith("Error"));

        String eepromLimpia = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        assertNotNull("EEPROM leída no debe ser null", eepromLimpia);
        assertTrue("EEPROM debería estar limpia (FF)", eepromLimpia.startsWith("FF") || eepromLimpia.contains("FFFF"));

        // 6. Programar EEPROM usando el primer HEX (pwmc_main107_628A.HEX) sin borrar la ROM
        assertTrue("Fallo al programar EEPROM", protocolo.programarMemoriaEEPROMDelPic(chip16f628a, parsedHexes[0]));

        // 7. Verificar persistencia: La EEPROM debe estar programada y la ROM debe mantener su contenido original sin alterarse
        String eepromLeida = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        assertNotNull("EEPROM leída no debe ser null", eepromLeida);
        assertFalse("EEPROM leída no debe ser limpia", eepromLeida.startsWith("FFFFFFFFFFFFFFFF"));

        String romPersistida = protocolo.leerMemoriaROMDelPic(chip16f628a);
        assertEquals("La ROM no debe haber sido borrada o modificada al escribir la EEPROM", romOriginal, romPersistida);

        // 8. Programar Fuses y ID con el tercer HEX (nuevoled.hex) sin borrar ROM ni EEPROM
        byte[] idPic = new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD };
        java.util.List<Integer> fuses = java.util.Arrays.asList(0x3F30);
        assertTrue("Fallo al programar Fuses e ID", protocolo.programarFusesIDDelPic(chip16f628a, parsedHexes[2], idPic, fuses));

        // 9. Verificar persistencia final: Configuración debe tener los nuevos fusibles/ID, pero la ROM y EEPROM siguen intactas
        String config = protocolo.leerDatosDeConfiguracionDelPic();
        assertNotNull(config);
        assertTrue("Debe contener el ID grabado", config.contains("AABBCCDD"));

        String romPersistidaFinal = protocolo.leerMemoriaROMDelPic(chip16f628a);
        assertEquals("La ROM debe seguir intacta", romOriginal, romPersistidaFinal);

        String eepromPersistidaFinal = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        assertEquals("La EEPROM debe seguir intacta", eepromLeida, eepromPersistidaFinal);
    }

    @Test
    public void testExportacionYFormatosHexBin() throws Exception {
        // 1. Leer y parsear el HEX original
        String hexContentOriginal = new String(Files.readAllBytes(Paths.get("/home/danielpdiamon/pwmc_main107_628A.HEX")), StandardCharsets.UTF_8);
        com.diamon.datos.DatosPicProcesados datosPicOriginal = new com.diamon.datos.DatosPicProcesados(null, hexContentOriginal, chip16f628a);
        datosPicOriginal.iniciarProcesamientoDeDatos();

        byte[] romOriginal = datosPicOriginal.obtenerBytesHexROMPocesado();
        byte[] eepromOriginal = datosPicOriginal.obtenerBytesHexEEPROMPocesado();

        // 2. Formatear para exportación (Little Endian swabbing / padding según arquitectura PIC16 de 14-bits)
        byte[] romExportReady = com.diamon.managers.HexExportManager.formatForHexExport(romOriginal, 14, false);
        byte[] eepromExportReady = com.diamon.managers.HexExportManager.formatForHexExport(eepromOriginal, 14, true);

        // 3. Generar la representación Intel HEX de cada segmento
        String romGeneratedHex = com.diamon.managers.HexExportManager.convertToIntelHexWithAddress(romExportReady, 0);
        
        // La dirección base de EEPROM para el PIC16F628A es 0x2100 (dirección de palabras, que es 0x4200 byte-address)
        String eepromGeneratedHex = com.diamon.managers.HexExportManager.convertToIntelHexWithAddress(eepromExportReady, 0x4200);

        // 4. Importar y parsear los HEX generados para asegurar que el formato es 100% válido y mantiene integridad
        com.diamon.datos.DatosPicProcesados datosPicROMGenerado = new com.diamon.datos.DatosPicProcesados(null, romGeneratedHex, chip16f628a);
        datosPicROMGenerado.iniciarProcesamientoDeDatos();
        byte[] romParseadoGenerado = datosPicROMGenerado.obtenerBytesHexROMPocesado();

        assertArrayEquals("La ROM exportada e importada de vuelta no coincide con la original", romOriginal, romParseadoGenerado);

        com.diamon.datos.DatosPicProcesados datosPicEEPROMGenerado = new com.diamon.datos.DatosPicProcesados(null, eepromGeneratedHex, chip16f628a);
        datosPicEEPROMGenerado.iniciarProcesamientoDeDatos();
        byte[] eepromParseadoGenerado = datosPicEEPROMGenerado.obtenerBytesHexEEPROMPocesado();

        assertArrayEquals("La EEPROM exportada e importada de vuelta no coincide con la original", eepromOriginal, eepromParseadoGenerado);
    }
}
