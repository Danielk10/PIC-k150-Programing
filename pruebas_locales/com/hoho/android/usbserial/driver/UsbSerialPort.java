package com.hoho.android.usbserial.driver;

import java.io.IOException;

public interface UsbSerialPort {
    int read(byte[] dest, int timeout) throws IOException;
    void write(byte[] src, int timeout) throws IOException;
    void open(Object connection) throws IOException;
    void close() throws IOException;
    void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException;
}
