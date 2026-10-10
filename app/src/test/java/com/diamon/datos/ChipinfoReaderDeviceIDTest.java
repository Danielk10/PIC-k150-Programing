package com.diamon.datos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Pruebas unitarias para la resolución inversa de modelos PIC a partir de su Device ID
 * implementada en ChipinfoReader.
 */
public class ChipinfoReaderDeviceIDTest {

    private ChipinfoReader readerConDatosMinimos;

    @Before
    public void setUp() throws Exception {
        List<String> lineas = Arrays.asList(
                "CHIPname=16F628A",
                "INCLUDE=Y",
                "SocketImage=18pin",
                "EraseMode=2",
                "FlashChip=Y",
                "PowerSequence=Vpp2Vcc",
                "ProgramDelay=50",
                "ProgramTries=1",
                "OverProgram=0",
                "CoreType=bit14_B",
                "ROMsize=000800",
                "EEPROMsize=00000080",
                "FUSEblank=3FFF",
                "ChipID=1060",
                "",
                "CHIPname=18F1220",
                "INCLUDE=Y",
                "SocketImage=18pin",
                "EraseMode=4",
                "FlashChip=Y",
                "PowerSequence=VccVpp2",
                "ProgramDelay=10",
                "ProgramTries=1",
                "OverProgram=0",
                "CoreType=bit16_B",
                "ROMsize=000800",
                "EEPROMsize=00000100",
                "FUSEblank=CF00 1F0F",
                "ChipID=07E0",
                "",
                "CHIPname=PIC_SIN_ID",
                "CoreType=bit14_B",
                "ROMsize=000800",
                "ChipID=FFFF"
        );
        readerConDatosMinimos = new ChipinfoReader(lineas);
    }

    @Test
    public void buscarModeloPorDeviceID_coincidenciaDirecta() {
        // 0x1060 -> 16F628A
        assertEquals("16F628A", readerConDatosMinimos.buscarModeloPorDeviceID(0x1060));
        // 0x07E0 -> 18F1220
        assertEquals("18F1220", readerConDatosMinimos.buscarModeloPorDeviceID(0x07E0));
    }

    @Test
    public void buscarModeloPorDeviceID_mascaraRevision5Bits() {
        // En Microchip PIC, los 5 bits inferiores representan la revisión de silicio.
        // 0x1068: Device ID 0x1060 con revisión 0x08
        assertEquals("16F628A", readerConDatosMinimos.buscarModeloPorDeviceID(0x1068));
        // 0x107F: Device ID 0x1060 con revisión máxima 0x1F
        assertEquals("16F628A", readerConDatosMinimos.buscarModeloPorDeviceID(0x107F));
        // 0x07E5: 0x07E0 con revisión 0x05
        assertEquals("18F1220", readerConDatosMinimos.buscarModeloPorDeviceID(0x07E5));
    }

    @Test
    public void buscarModeloPorDeviceID_byteSwappedDirecto() {
        // Byte swap: Little-Endian a Big-Endian (0x1060 -> bytes 0x60, 0x10 -> palabra 0x6010)
        assertEquals("16F628A", readerConDatosMinimos.buscarModeloPorDeviceID(0x6010));
        // 0x07E0 -> bytes 0xE0, 0x07 -> 0xE007
        assertEquals("18F1220", readerConDatosMinimos.buscarModeloPorDeviceID(0xE007));
    }

    @Test
    public void buscarModeloPorDeviceID_byteSwappedConMascaraRevisionHardwareReal() {
        // CASO EXACTO VALIDADO EN SILICIO FÍSICO PIC16F628A:
        // El hardware real devuelve los bytes 0x68 y 0x10 (formando palabra 0x6810)
        // Al invertir bytes: 0x1068
        // Al aplicar máscara 0xFFE0: 0x1060 -> Identifica 16F628A
        assertEquals("16F628A", readerConDatosMinimos.buscarModeloPorDeviceID(0x6810));
        // 0x1068 directo con revisión
        assertEquals("16F628A", readerConDatosMinimos.buscarModeloPorDeviceID(0x1068));
        // 18F1220 con revisión 0x05 byte-swapped: 0x07E5 -> 0xE507
        assertEquals("18F1220", readerConDatosMinimos.buscarModeloPorDeviceID(0xE507));
    }

    @Test
    public void buscarModeloPorDeviceID_idsDesconocidosOInvalidosRetornanNull() {
        assertNull(readerConDatosMinimos.buscarModeloPorDeviceID(0x0000));
        assertNull(readerConDatosMinimos.buscarModeloPorDeviceID(0xFFFF));
        assertNull(readerConDatosMinimos.buscarModeloPorDeviceID(0x1234));
        assertNull(readerConDatosMinimos.buscarModeloPorDeviceID(-1));
    }

    @Test
    public void getDeviceIdMap_contieneMapeosYEsInmutable() {
        Map<Integer, String> map = readerConDatosMinimos.getDeviceIdMap();
        assertNotNull(map);
        assertTrue(map.containsKey(0x1060));
        assertEquals("16F628A", map.get(0x1060));
        assertTrue(map.containsKey(0x07E0));
        assertEquals("18F1220", map.get(0x07E0));

        // Debe ignorar FFFF
        assertTrue(!map.containsKey(0xFFFF));

        boolean excepcionLanzada = false;
        try {
            map.put(0x9999, "TEST");
        } catch (UnsupportedOperationException e) {
            excepcionLanzada = true;
        }
        assertTrue("El mapa devuelto por getDeviceIdMap() debe ser inmutable", excepcionLanzada);
    }

    @Test
    public void baseDeDatosCompleta_identifica16F628AEnHardwareReal() throws Exception {
        // Localizar el archivo chipinfo.cid del repositorio
        File file = new File("src/main/assets/chipinfo.cid");
        if (!file.exists()) {
            file = new File("app/src/main/assets/chipinfo.cid");
        }
        if (!file.exists()) {
            file = new File("/home/danielpdiamon/PIC-k150-Programing/app/src/main/assets/chipinfo.cid");
        }
        assertTrue("El archivo chipinfo.cid debe existir para la prueba", file.exists());

        List<String> lineas = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        ChipinfoReader readerCompleto = new ChipinfoReader(lineas);

        // Validar coincidencia directa
        assertEquals("16F628A", readerCompleto.buscarModeloPorDeviceID(0x1060));

        // Validar coincidencia con lectura en bruto de hardware físico (0x6810)
        assertEquals("16F628A", readerCompleto.buscarModeloPorDeviceID(0x6810));

        // Validar que 18F1220 también esté en la base de datos completa
        assertEquals("18F1220", readerCompleto.buscarModeloPorDeviceID(0x07E0));
    }
}
