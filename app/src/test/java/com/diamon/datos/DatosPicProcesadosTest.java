package com.diamon.datos;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.diamon.chip.ChipPic;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DatosPicProcesadosTest {

    private ChipPic crearChip16F628A() throws Exception {
        Map<String, Object> fuses = new HashMap<>();
        return new ChipPic(
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

    @Test
    public void configOnlyHex_debeInterpretarFuseConEndianCorrectoEnBit14() throws Exception {
        String hexConfigOnly = ":02400E00743FFD\n:00000001FF\n";
        Context context = mock(Context.class);
        when(context.getString(anyInt())).thenReturn("msg");
        when(context.getString(anyInt(), any())).thenReturn("msg");

        DatosPicProcesados procesado = new DatosPicProcesados(
                context,
                hexConfigOnly,
                crearChip16F628A());

        procesado.iniciarProcesamientoDeDatos();

        int[] fuses = procesado.obtenerValoresIntHexFusesPocesado();
        assertEquals(1, fuses.length);
        assertEquals(0x3F74, fuses[0]);
    }
}
