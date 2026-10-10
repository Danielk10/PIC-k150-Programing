package com.diamon.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.excepciones.ChipConfigurationException;

import org.junit.Test;

/**
 * Pruebas unitarias para HexEditorManager.
 * Valida el parcheo de tramas Intel HEX con recálculo exacto de checksum,
 * actualización de búferes en memoria (ROM Little-Endian y EEPROM)
 * y mapeo según arquitectura del núcleo (14-bit vs 16-bit).
 */
public class HexEditorManagerTest {

    /**
     * Valida la integridad del checksum de un registro Intel HEX (:LLAAAATT[DD...]CC).
     * La suma de todos los bytes incluyendo el checksum debe dar 0 (módulo 256).
     */
    private boolean validarChecksumRegistroHex(String linea) {
        if (!linea.startsWith(":") || linea.length() < 11) {
            return false;
        }
        int sum = 0;
        for (int i = 1; i < linea.length(); i += 2) {
            String byteStr = linea.substring(i, i + 2);
            sum += Integer.parseInt(byteStr, 16);
        }
        return (sum & 0xFF) == 0;
    }

    @Test
    public void testParcharIntelHex_modificacionRegistroDatosExistente_recalculaChecksum() {
        // Registro de datos válido: 16 bytes en dirección 0x0000
        String hexOriginal = ":1000000000308A118A120313831203138316051515\n:00000001FF\n";
        assertTrue("Checksum original debe ser válido", validarChecksumRegistroHex(":1000000000308A118A120313831203138316051515"));

        // Modificamos los dos primeros bytes (palabra en dirección 0x0000): 0x55, 0x3A
        byte[] nuevosBytes = new byte[]{(byte) 0x55, (byte) 0x3A};
        String hexParchado = HexEditorManager.parcharIntelHex(hexOriginal, 0, nuevosBytes);

        assertNotNull(hexParchado);
        String[] lineas = hexParchado.split("\\r?\\n");
        assertEquals(2, lineas.length);

        String lineaModificada = lineas[0];
        // Comprobar que los nuevos bytes están presentes
        assertTrue(lineaModificada.startsWith(":10000000553A"));
        // Comprobar que el checksum recalculado es matemáticamente válido
        assertTrue("Checksum recalculado debe ser válido", validarChecksumRegistroHex(lineaModificada));
    }

    @Test
    public void testParcharIntelHex_registroNoExistente_insertaRegistroConChecksumValido() {
        String hexOriginal = ":1000000000308A118A120313831203138316051515\n:00000001FF\n";

        // Parcheamos dirección 0x0050 (80), que no está presente en el registro inicial
        byte[] nuevosBytes = new byte[]{(byte) 0xAA, (byte) 0xBB};
        String hexParchado = HexEditorManager.parcharIntelHex(hexOriginal, 0x0050, nuevosBytes);

        assertNotNull(hexParchado);
        assertTrue(hexParchado.contains(":02005000AABB"));

        String[] lineas = hexParchado.split("\\r?\\n");
        for (String linea : lineas) {
            if (linea.startsWith(":02005000")) {
                assertTrue("Nuevo registro debe tener checksum válido", validarChecksumRegistroHex(linea));
            }
        }
        assertTrue(lineas[lineas.length - 1].contains(":00000001FF"));
    }

    @Test
    public void testParcharIntelHex_conExtendedLinearAddress_calculaDireccionAbsoluta() {
        // Registro Extended Linear Address (Tipo 04) apuntando a bloque 0x0001 (65536)
        String hexOriginal = ":020000040001F9\n"
                + ":100000000102030405060708090A0B0C0D0E0F1068\n"
                + ":00000001FF\n";

        assertTrue(validarChecksumRegistroHex(":020000040001F9"));
        assertTrue(validarChecksumRegistroHex(":100000000102030405060708090A0B0C0D0E0F1068"));

        // Modificamos el byte en dirección absoluta 0x10003 (offset 3 dentro del bloque de datos)
        int targetAddress = 0x10003;
        byte[] nuevosBytes = new byte[]{(byte) 0x99};

        String hexParchado = HexEditorManager.parcharIntelHex(hexOriginal, targetAddress, nuevosBytes);
        String[] lineas = hexParchado.split("\\r?\\n");

        String lineaDatos = lineas[1];
        // En la posición de datos (después de :10000000 + 3 bytes = 010203), debe aparecer 99
        assertTrue(lineaDatos.startsWith(":1000000001020399"));
        assertTrue("Checksum recalculado en segmento lineal debe ser válido", validarChecksumRegistroHex(lineaDatos));
    }

    @Test
    public void testAplicarCambioEnDatosProcesados_romLittleEndian() {
        DatosPicProcesados mockDatos = mock(DatosPicProcesados.class);
        byte[] romBytes = new byte[32];
        when(mockDatos.obtenerBytesHexROMProcesado()).thenReturn(romBytes);

        // Modificamos dirección de ROM 3 con valor 0x1234
        // Offset = 3 * 2 = 6 (Byte bajo = 0x34, Byte alto = 0x12)
        HexEditorManager.aplicarCambioEnDatosProcesados(mockDatos, true, 3, 0x1234);

        assertEquals((byte) 0x34, romBytes[6]);
        assertEquals((byte) 0x12, romBytes[7]);
    }

    @Test
    public void testAplicarCambioEnDatosProcesados_eepromByte() {
        DatosPicProcesados mockDatos = mock(DatosPicProcesados.class);
        byte[] eepromBytes = new byte[16];
        when(mockDatos.obtenerBytesHexEEPROMProcesado()).thenReturn(eepromBytes);

        // Modificamos dirección de EEPROM 7 con valor 0xFE
        HexEditorManager.aplicarCambioEnDatosProcesados(mockDatos, false, 7, 0xFE);

        assertEquals((byte) 0xFE, eepromBytes[7]);
    }

    @Test
    public void testParcharFirmwareIntelHex_romMapeoWordABytes() {
        String hexOriginal = ":1000000000308A118A120313831203138316051515\n:00000001FF\n";

        // ROM address 2 -> byteAddress = 4. Word 0x3FFF -> Low 0xFF, High 0x3F
        String hexParchado = HexEditorManager.parcharFirmwareIntelHex(hexOriginal, true, 2, 0x3FFF, null);

        assertNotNull(hexParchado);
        String linea = hexParchado.split("\\r?\\n")[0];
        // :10000000 0030 8A11 [FF3F]
        assertTrue(linea.startsWith(":1000000000308A11FF3F"));
        assertTrue(validarChecksumRegistroHex(linea));
    }

    @Test
    public void testParcharFirmwareIntelHex_eeprom14Bit_baseAddress2100() throws ChipConfigurationException {
        ChipPic mockChip = mock(ChipPic.class);
        when(mockChip.getTipoDeNucleoBit()).thenReturn(14);

        String hexOriginal = ":1021000000000000000000000000000000000000CF\n:00000001FF\n";
        assertTrue(validarChecksumRegistroHex(":1021000000000000000000000000000000000000CF"));

        // EEPROM address 0 en 14-bit PIC -> Dirección Intel HEX 0x2100. Valor 0x5A
        String hexParchado = HexEditorManager.parcharFirmwareIntelHex(hexOriginal, false, 0, 0x5A, mockChip);

        assertNotNull(hexParchado);
        String linea = hexParchado.split("\\r?\\n")[0];
        assertTrue(linea.startsWith(":102100005A"));
        assertTrue(validarChecksumRegistroHex(linea));
    }
}
