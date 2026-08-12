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
    private ChipPic chip18f2550;
    private ChipPic chip12f675;

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

        // Crear configuración de ChipPic para PIC18F2550
        Map<String, Object> fuses18f = new HashMap<>();
        chip18f2550 = new ChipPic(
                "18F2550",
                "Y",
                "28pin",
                "4",
                "Y",
                "VccVpp1",
                "10",
                "1",
                "05",
                "bit16_B",
                "004000",
                "00000100",
                new String[] { "CF3F", "1F3F", "8700", "00E5", "C00F", "E00F", "400F" },
                "N",
                "N",
                "N",
                "N",
                "1240",
                fuses18f);

        // Crear configuración de ChipPic para PIC12F675
        Map<String, Object> fuses12f = new HashMap<>();
        chip12f675 = new ChipPic(
                "12F675",
                "Y",
                "8pin",
                "2",
                "Y",
                "Vpp2Vcc",
                "80",
                "1",
                "0",
                "bit14_B",
                "000400",
                "00000080",
                new String[] { "31FF" },
                "N",
                "Y",
                "Y",
                "N",
                "0FC0",
                fuses12f);
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
            
            byte[] romBytes = parsedHexes[i].obtenerBytesHexROMProcesado();
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

        byte[] romOriginal = datosPicOriginal.obtenerBytesHexROMProcesado();
        byte[] eepromOriginal = datosPicOriginal.obtenerBytesHexEEPROMProcesado();

        // 2. Formatear para exportación (Little Endian swabbing / padding según arquitectura PIC16 de 14-bits)
        byte[] romExportReady = com.diamon.managers.HexExportManager.formatForHexExport(romOriginal, 14, false);
        byte[] eepromExportReady = com.diamon.managers.HexExportManager.formatForHexExport(eepromOriginal, 14, true);

        // 3. Generar la representación Intel HEX de cada segmento
        String romGeneratedHex = com.diamon.managers.HexExportManager.convertToIntelHexWithAddress(romExportReady, 0);
        
        // La dirección base de EEPROM para el PIC16F628A es 0x2100 (dirección de palabras, que es 0x4200 byte-address)
        String eepromGeneratedHex = com.diamon.managers.HexExportManager.convertToIntelHexWithAddress(eepromExportReady, 0x4200);

        com.diamon.datos.DatosPicProcesados datosPicROMGenerado = new com.diamon.datos.DatosPicProcesados(null, romGeneratedHex, chip16f628a);
        datosPicROMGenerado.iniciarProcesamientoDeDatos();
        byte[] romParseadoGenerado = datosPicROMGenerado.obtenerBytesHexROMProcesado();

        assertArrayEquals("La ROM exportada e importada de vuelta no coincide con la original", romOriginal, romParseadoGenerado);

        com.diamon.datos.DatosPicProcesados datosPicEEPROMGenerado = new com.diamon.datos.DatosPicProcesados(null, eepromGeneratedHex, chip16f628a);
        datosPicEEPROMGenerado.iniciarProcesamientoDeDatos();
        byte[] eepromParseadoGenerado = datosPicEEPROMGenerado.obtenerBytesHexEEPROMProcesado();

        assertArrayEquals("La EEPROM exportada e importada de vuelta no coincide con la original", eepromOriginal, eepromParseadoGenerado);
    }

    @Test
    public void testManagerProgramacionCompleta() throws Exception {
        // 1. Instanciar PicProgrammingManager
        com.diamon.managers.PicProgrammingManager manager = new com.diamon.managers.PicProgrammingManager(mockContext);
        manager.setProtocolo(protocolo);

        // Configurar un listener para capturar eventos y verificar que se disparen
        final boolean[] startedCalled = {false};
        final boolean[] progressCalled = {false};
        final boolean[] completedCalled = {false};
        
        manager.setProgrammingListener(new com.diamon.managers.PicProgrammingManager.ProgrammingListener() {
            @Override
            public void onProgrammingStarted() {
                startedCalled[0] = true;
            }

            @Override
            public void onProgrammingProgress(String message, int progress) {
                progressCalled[0] = true;
            }

            @Override
            public void onProgrammingCompleted(boolean success) {
                completedCalled[0] = success;
            }

            @Override
            public void onProgrammingError(String errorMessage) {
                fail("No debería ocurrir error de programación: " + errorMessage);
            }
        });

        // 2. Leer archivo HEX completo
        String hexContent = new String(Files.readAllBytes(Paths.get("/home/danielpdiamon/pwmc_main107_628A.HEX")), StandardCharsets.UTF_8);

        // 3. Ejecutar programación completa (ROM + EEPROM + Config)
        byte[] idPic = new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44 };
        java.util.List<Integer> fuses = java.util.Arrays.asList(0x3F74);
        
        boolean success = manager.programChip(chip16f628a, hexContent, idPic, fuses);
        assertTrue("La programación completa del manager debería ser exitosa", success);
        
        // Verificar disparadores del listener
        assertTrue("onProgrammingStarted debería ser llamado", startedCalled[0]);
        assertTrue("onProgrammingProgress debería ser llamado", progressCalled[0]);
        assertTrue("onProgrammingCompleted debería ser llamado con éxito", completedCalled[0]);

        // 4. Leer de vuelta de la memoria simulada del emulador para verificar el grabado completo
        String rom = protocolo.leerMemoriaROMDelPic(chip16f628a);
        assertNotNull(rom);
        assertFalse(rom.startsWith("Error"));

        String eeprom = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        assertNotNull(eeprom);
        assertFalse(eeprom.startsWith("Error"));

        String config = protocolo.leerDatosDeConfiguracionDelPic();
        assertNotNull(config);
        assertTrue("La configuración debe contener el ID grabado mediante el manager", config.contains("11223344"));
    }

    @Test
    public void testPIC18F2550ProgramacionYLectura() throws Exception {
        // 1. Crear configuración de ChipPic para PIC18F2550
        Map<String, Object> fusesMap = new HashMap<>();
        ChipPic chip18f2550 = new ChipPic(
                "18F2550",
                "Y",
                "28Npin",
                "4",
                "Y",
                "VccVpp1",
                "10",
                "1",
                "05",
                "bit16_B",
                "004000",
                "00000100",
                new String[] { "CF3F", "1F3F", "8700", "00E5", "C00F", "E00F", "400F" },
                "N",
                "N",
                "N",
                "N",
                "1240",
                fusesMap);

        // 2. Iniciar variables de programación del chip
        assertTrue(protocolo.iniciarVariablesDeProgramacion(chip18f2550));

        // 3. Borrar chip
        assertTrue(protocolo.borrarMemoriasDelPic());

        // 4. Leer y programar archivo HEX real de PIC18F2550
        String hexPath = "/home/danielpdiamon/PIC-k150-Programing/waw_pic18f2550.hex";
        File hexFile = new File(hexPath);
        assertTrue("El archivo HEX de PIC18F2550 no existe", hexFile.exists());
        String hexContent = new String(Files.readAllBytes(Paths.get(hexPath)), StandardCharsets.UTF_8);

        com.diamon.datos.DatosPicProcesados datosPic = new com.diamon.datos.DatosPicProcesados(mockContext, hexContent, chip18f2550);
        datosPic.iniciarProcesamientoDeDatos();

        // 5. Programar ROM
        boolean romOk = protocolo.programarMemoriaROMDelPic(chip18f2550, datosPic);
        assertTrue("Fallo al programar ROM para PIC18F2550", romOk);

        // 6. Leer ROM y verificar que coincida o no esté vacía
        String romLeida = protocolo.leerMemoriaROMDelPic(chip18f2550);
        assertNotNull(romLeida);
        assertFalse(romLeida.startsWith("Error"));
        assertTrue(romLeida.length() > 0);

        // 7. Programar EEPROM
        boolean eepromOk = protocolo.programarMemoriaEEPROMDelPic(chip18f2550, datosPic);
        assertTrue("Fallo al programar EEPROM para PIC18F2550", eepromOk);

        // 8. Leer EEPROM y verificar
        String eepromLeida = protocolo.leerMemoriaEEPROMDelPic(chip18f2550);
        assertNotNull(eepromLeida);
        assertFalse(eepromLeida.startsWith("Error"));
        assertTrue(eepromLeida.length() > 0);

        // 9. Programar ID y Fuses
        byte[] idPic = new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, (byte) 0x77, (byte) 0x88 };
        java.util.List<Integer> fusesList = java.util.Arrays.asList(0xCF3F, 0x1F3F, 0x8700, 0x00E5, 0xC00F, 0xE00F, 0x400F);
        boolean fusesOk = protocolo.programarFusesIDDelPic(chip18f2550, datosPic, idPic, fusesList);
        assertTrue("Fallo al programar Fuses e ID para PIC18F2550", fusesOk);

        // 10. Leer Configuración
        String config = protocolo.leerDatosDeConfiguracionDelPic();
        assertNotNull(config);
        assertFalse(config.startsWith("Error"));
        // El emulador reporta ChipID = 4640 (0x1220 en LE = 2012)
        assertTrue("Config debe reportar ChipID", config.toLowerCase().startsWith("2012"));
        assertTrue("Config debe contener el ID grabado", config.contains("1122334455667788"));
    }

    @Test
    public void testPIC12F675ProgramacionYLectura() throws Exception {
        // 1. Crear configuración de ChipPic para PIC12F675
        Map<String, Object> fusesMap = new HashMap<>();
        ChipPic chip12f675 = new ChipPic(
                "12F675",
                "Y",
                "8pin",
                "2",
                "Y",
                "Vpp2Vcc",
                "80",
                "1",
                "0",
                "bit14_B",
                "000400",
                "00000080",
                new String[] { "31FF" },
                "N",
                "N",
                "N",
                "N",
                "0FC0",
                fusesMap);

        // 2. Iniciar variables de programación del chip
        assertTrue(protocolo.iniciarVariablesDeProgramacion(chip12f675));

        // 3. Borrar chip
        assertTrue(protocolo.borrarMemoriasDelPic());

        // 4. Leer y programar archivo HEX real de PIC12F675
        String hexPath = "/home/danielpdiamon/PIC-k150-Programing/32x-autohz_12f675.hex";
        File hexFile = new File(hexPath);
        assertTrue("El archivo HEX de PIC12F675 no existe", hexFile.exists());
        String hexContent = new String(Files.readAllBytes(Paths.get(hexPath)), StandardCharsets.UTF_8);

        com.diamon.datos.DatosPicProcesados datosPic = new com.diamon.datos.DatosPicProcesados(mockContext, hexContent, chip12f675);
        datosPic.iniciarProcesamientoDeDatos();

        // 5. Programar ROM
        boolean romOk = protocolo.programarMemoriaROMDelPic(chip12f675, datosPic);
        assertTrue("Fallo al programar ROM para PIC12F675", romOk);

        // 6. Leer ROM y verificar
        String romLeida = protocolo.leerMemoriaROMDelPic(chip12f675);
        assertNotNull(romLeida);
        assertFalse(romLeida.startsWith("Error"));
        assertTrue(romLeida.length() > 0);

        // 7. Programar EEPROM
        boolean eepromOk = protocolo.programarMemoriaEEPROMDelPic(chip12f675, datosPic);
        assertTrue("Fallo al programar EEPROM para PIC12F675", eepromOk);

        // 8. Leer EEPROM y verificar
        String eepromLeida = protocolo.leerMemoriaEEPROMDelPic(chip12f675);
        assertNotNull(eepromLeida);
        assertFalse(eepromLeida.startsWith("Error"));
        assertTrue(eepromLeida.length() > 0);

        // 9. Programar ID y Fuses
        byte[] idPic = new byte[] { (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44 };
        java.util.List<Integer> fusesList = java.util.Arrays.asList(0x31FF);
        boolean fusesOk = protocolo.programarFusesIDDelPic(chip12f675, datosPic, idPic, fusesList);
        assertTrue("Fallo al programar Fuses e ID para PIC12F675", fusesOk);

        // 10. Leer Configuración
        String config = protocolo.leerDatosDeConfiguracionDelPic();
        assertNotNull(config);
        assertFalse(config.startsWith("Error"));
        // El emulador reporta ChipID = 4032 (0x0FC0 en LE = C00F)
        assertTrue("Config debe reportar ChipID", config.toLowerCase().startsWith("c00f"));
        assertTrue("Config debe contener el ID grabado", config.contains("11223344"));
    }

    @Test
    public void testImportacionYExportacionBinCompletoYParcial() throws Exception {
        // --- PARTE A: Programar con HEX real y capturar lecturas del emulador ---
        assertTrue("Fallo al iniciar variables de programación", protocolo.iniciarVariablesDeProgramacion(chip16f628a));
        assertTrue("Fallo al borrar el chip", protocolo.borrarMemoriasDelPic());

        String hexContentOriginal = new String(Files.readAllBytes(Paths.get("/home/danielpdiamon/pwmc_main107_628A.HEX")), StandardCharsets.UTF_8);
        com.diamon.datos.DatosPicProcesados datosPicOriginal = new com.diamon.datos.DatosPicProcesados(null, hexContentOriginal, chip16f628a);
        datosPicOriginal.iniciarProcesamientoDeDatos();

        assertTrue("Fallo al programar ROM original", protocolo.programarMemoriaROMDelPic(chip16f628a, datosPicOriginal));
        assertTrue("Fallo al programar EEPROM original", protocolo.programarMemoriaEEPROMDelPic(chip16f628a, datosPicOriginal));
        
        byte[] idPicOriginal = datosPicOriginal.obtenerValoresBytesHexIDProcesado();
        java.util.List<Integer> fusesListOriginal = new java.util.ArrayList<>();
        for (int f : datosPicOriginal.obtenerValoresIntHexFusesProcesado()) {
            fusesListOriginal.add(f);
        }
        assertTrue("Fallo al programar Fuses e ID originales",
                protocolo.programarFusesIDDelPic(chip16f628a, datosPicOriginal, idPicOriginal, fusesListOriginal));

        String romDeHex = protocolo.leerMemoriaROMDelPic(chip16f628a);
        String eepromDeHex = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        String configDeHex = protocolo.leerDatosDeConfiguracionDelPic();

        assertNotNull(romDeHex);
        assertNotNull(eepromDeHex);
        assertNotNull(configDeHex);

        // --- PARTE B: Generar Volcado .bin, Importar e Integridad Local ---
        byte[] romOriginalBytes = datosPicOriginal.obtenerBytesHexROMProcesado();
        byte[] eepromOriginalBytes = datosPicOriginal.obtenerBytesHexEEPROMProcesado();
        byte[] idOriginalBytes = datosPicOriginal.obtenerValoresBytesHexIDProcesado();
        int[] fusesOriginalInts = datosPicOriginal.obtenerValoresIntHexFusesProcesado();

        byte[] rawConfigOriginal = new byte[26];
        int chipId = chip16f628a.getIDPIC();
        rawConfigOriginal[0] = (byte) (chipId & 0xFF);
        rawConfigOriginal[1] = (byte) ((chipId >> 8) & 0xFF);
        System.arraycopy(idOriginalBytes, 0, rawConfigOriginal, 2, 4);
        rawConfigOriginal[10] = (byte) (fusesOriginalInts[0] & 0xFF);
        rawConfigOriginal[11] = (byte) ((fusesOriginalInts[0] >> 8) & 0xFF);

        int romLen = romOriginalBytes.length;
        int eepromLen = eepromOriginalBytes.length;
        byte[] fullBinData = new byte[romLen + 26 + eepromLen];
        System.arraycopy(romOriginalBytes, 0, fullBinData, 0, romLen);
        System.arraycopy(rawConfigOriginal, 0, fullBinData, romLen, 26);
        System.arraycopy(eepromOriginalBytes, 0, fullBinData, romLen + 26, eepromLen);

        com.diamon.managers.FileManager fileMgr = new com.diamon.managers.FileManager(null);
        String virtualHex = fileMgr.binaryToIntelHex(fullBinData, chip16f628a);
        assertNotNull(virtualHex);

        com.diamon.datos.DatosPicProcesados datosPicImportado = new com.diamon.datos.DatosPicProcesados(null, virtualHex, chip16f628a);
        datosPicImportado.iniciarProcesamientoDeDatos();

        assertArrayEquals("La ROM importada desde Full Dump .bin no coincide", romOriginalBytes, datosPicImportado.obtenerBytesHexROMProcesado());
        assertArrayEquals("La EEPROM importada desde Full Dump .bin no coincide", eepromOriginalBytes, datosPicImportado.obtenerBytesHexEEPROMProcesado());
        assertArrayEquals("El ID importado desde Full Dump .bin no coincide", idOriginalBytes, datosPicImportado.obtenerValoresBytesHexIDProcesado());
        assertArrayEquals("Los Fuses importados desde Full Dump .bin no coinciden", fusesOriginalInts, datosPicImportado.obtenerValoresIntHexFusesProcesado());

        // --- PARTE C: Borrar, Grabar del .bin Importado y Contrastar ---
        assertTrue("Fallo al borrar el chip antes de grabar desde bin", protocolo.borrarMemoriasDelPic());

        assertTrue("Fallo al programar ROM importada en el emulador",
                protocolo.programarMemoriaROMDelPic(chip16f628a, datosPicImportado));
        
        assertTrue("Fallo al programar EEPROM importada en el emulador",
                protocolo.programarMemoriaEEPROMDelPic(chip16f628a, datosPicImportado));
        
        java.util.List<Integer> fusesListImportado = new java.util.ArrayList<>();
        for (int fuse : datosPicImportado.obtenerValoresIntHexFusesProcesado()) {
            fusesListImportado.add(fuse);
        }
        byte[] idBytesImportado = datosPicImportado.obtenerValoresBytesHexIDProcesado();
        assertTrue("Fallo al programar Fuses e ID importados en el emulador",
                protocolo.programarFusesIDDelPic(chip16f628a, datosPicImportado, idBytesImportado, fusesListImportado));

        String romDeBin = protocolo.leerMemoriaROMDelPic(chip16f628a);
        String eepromDeBin = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        String configDeBin = protocolo.leerDatosDeConfiguracionDelPic();

        assertEquals("La ROM grabada y leída desde .bin no coincide con la del .hex real", romDeHex, romDeBin);
        assertEquals("La EEPROM grabada y leída desde .bin no coincide con la del .hex real", eepromDeHex, eepromDeBin);
        assertEquals("La configuración grabada y leída desde .bin no coincide con la del .hex real", configDeHex, configDeBin);

        // --- PARTE D: Validaciones de segmentación individual binaria ---
        String romOnlyHex = fileMgr.binaryToIntelHex(romOriginalBytes, chip16f628a);
        com.diamon.datos.DatosPicProcesados datosPicRomOnly = new com.diamon.datos.DatosPicProcesados(null, romOnlyHex, chip16f628a);
        datosPicRomOnly.iniciarProcesamientoDeDatos();
        assertArrayEquals("La ROM importada desde ROM-only .bin no coincide", romOriginalBytes, datosPicRomOnly.obtenerBytesHexROMProcesado());

        String eepromOnlyHex = fileMgr.binaryToIntelHex(eepromOriginalBytes, chip16f628a);
        com.diamon.datos.DatosPicProcesados datosPicEepromOnly = new com.diamon.datos.DatosPicProcesados(null, eepromOnlyHex, chip16f628a);
        datosPicEepromOnly.iniciarProcesamientoDeDatos();
        assertArrayEquals("La EEPROM importada desde EEPROM-only .bin no coincide", eepromOriginalBytes, datosPicEepromOnly.obtenerBytesHexEEPROMProcesado());

        String configOnlyHex = fileMgr.binaryToIntelHex(rawConfigOriginal, chip16f628a);
        com.diamon.datos.DatosPicProcesados datosPicConfigOnly = new com.diamon.datos.DatosPicProcesados(null, configOnlyHex, chip16f628a);
        datosPicConfigOnly.iniciarProcesamientoDeDatos();
        assertArrayEquals("El ID importado desde Config-only .bin no coincide", idOriginalBytes, datosPicConfigOnly.obtenerValoresBytesHexIDProcesado());
        assertArrayEquals("Los Fuses importados desde Config-only .bin no coinciden", fusesOriginalInts, datosPicConfigOnly.obtenerValoresIntHexFusesProcesado());
    }

    @Test
    public void testProgramacionYLecturaModoICSP() throws Exception {
        // Habilitamos ICSP en el chip de prueba
        chip16f628a.setActivarICSP(true);
        assertTrue("El chip debería tener activado el modo ICSP", chip16f628a.isISCPModo());

        // Ciclo completo de grabación y lectura con el emulador en modo ICSP
        assertTrue("Fallo al iniciar variables de programación en modo ICSP", protocolo.iniciarVariablesDeProgramacion(chip16f628a));
        assertTrue("Fallo al borrar el chip en modo ICSP", protocolo.borrarMemoriasDelPic());

        // Datos pic ficticios
        byte[] romDummy = new byte[4096];
        java.util.Arrays.fill(romDummy, (byte) 0x3F);
        byte[] eepromDummy = new byte[128];
        java.util.Arrays.fill(eepromDummy, (byte) 0xFF);
        byte[] idDummy = new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD };
        
        com.diamon.datos.DatosPicProcesados datosPic = mock(com.diamon.datos.DatosPicProcesados.class);
        when(datosPic.obtenerBytesHexROMProcesado()).thenReturn(romDummy);
        when(datosPic.obtenerBytesHexEEPROMProcesado()).thenReturn(eepromDummy);
        when(datosPic.obtenerValoresBytesHexIDProcesado()).thenReturn(idDummy);
        when(datosPic.obtenerValoresIntHexFusesProcesado()).thenReturn(new int[] { 0x3F74 });

        // Grabar ROM
        assertTrue("Fallo al grabar ROM en modo ICSP", protocolo.programarMemoriaROMDelPic(chip16f628a, datosPic));
        // Grabar EEPROM
        assertTrue("Fallo al grabar EEPROM en modo ICSP", protocolo.programarMemoriaEEPROMDelPic(chip16f628a, datosPic));
        // Grabar Fuses
        java.util.List<Integer> fusesList = java.util.Arrays.asList(0x3F74);
        assertTrue("Fallo al grabar Fuses en modo ICSP", protocolo.programarFusesIDDelPic(chip16f628a, datosPic, idDummy, fusesList));

        // Leer y comprobar integridad
        String romLeida = protocolo.leerMemoriaROMDelPic(chip16f628a);
        assertNotNull(romLeida);
        assertFalse(romLeida.startsWith("Error"));

        String eepromLeida = protocolo.leerMemoriaEEPROMDelPic(chip16f628a);
        assertNotNull(eepromLeida);
        assertFalse(eepromLeida.startsWith("Error"));

        String config = protocolo.leerDatosDeConfiguracionDelPic();
        assertNotNull(config);
        assertFalse(config.startsWith("Error"));
        assertTrue(config.toLowerCase().contains("aabbccdd"));
    }
}
