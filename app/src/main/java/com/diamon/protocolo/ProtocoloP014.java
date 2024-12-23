package com.diamon.protocolo;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.nucleo.Protocolo;
import com.hoho.android.usbserial.driver.UsbSerialPort;

public class ProtocoloP014 extends Protocolo {

    public ProtocoloP014(Context contexto, UsbSerialPort usbSerialPort) {
        super(contexto, usbSerialPort);
    }

    @Override
    public String hacerUnEco() {

        return null;
    }

    @Override
    public boolean iniciarVariablesDeProgramacion(ChipPic chipPIC) {

        return false;
    }

    @Override
    public boolean activarVoltajesDeProgramacion() {
        return false;
    }

    @Override
    public boolean desactivarVoltajesDeProgramacion() {

        return false;
    }

    @Override
    public boolean reiniciarVoltajesDeProgramacion() {

        return false;
    }

    @Override
    public boolean programarMemoriaROMDelPic(ChipPic chipPIC, String firware) {

        return false;
    }

    @Override
    public boolean programarMemoriaEEPROMDelPic(ChipPic chipPIC, String firware) {

        return false;
    }

    @Override
    public boolean programarFusesIDDelPic(ChipPic chipPIC, String firware) {

        return false;
    }

    @Override
    public boolean programarCalibracionDelPic(ChipPic chipPIC, String firware) {

        return false;
    }

    @Override
    public String leerMemoriaROMDelPic(ChipPic chipPIC) {

        return null;
    }

    @Override
    public String leerMemoriaEEPROMDelPic(ChipPic chipPIC) {

        return null;
    }

    @Override
    public String leerDatosDeConfiguracionDelPic() {

        return null;
    }

    @Override
    public String leerDatosDeCalibracionDelPic() {

        return null;
    }

    @Override
    public boolean borrarMemoriasDelPic() {

        return false;
    }

    @Override
    public boolean verificarSiEstaBarradaLaMemoriaROMDelDelPic(ChipPic chipPIC) {

        return false;
    }

    @Override
    public boolean verificarSiEstaBarradaLaMemoriaEEPROMDelDelPic() {

        return false;
    }

    @Override
    public boolean programarFusesDePics18F() {

        return false;
    }

    @Override
    public boolean detectarPicEnElSocket() {

        return false;
    }

    @Override
    public boolean detectarSiEstaFueraElPicDelSocket() {

        return false;
    }

    @Override
    public String obtenerVersionOModeloDelProgramador() {

        return null;
    }

    @Override
    public String obtenerProtocoloDelProgramador() {

        return null;
    }

    @Override
    public boolean programarVectorDeDepuracionDelPic(ChipPic chipPIC) {

        return false;
    }

    @Override
    public String leerVectorDeDepuracionDelPic() {

        return null;
    }

    @Override
    public boolean programarDatosDeCalibracionDePics10F() {

        return false;
    }
}
