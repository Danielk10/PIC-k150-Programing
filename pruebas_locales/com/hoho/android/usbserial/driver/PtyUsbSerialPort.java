package com.hoho.android.usbserial.driver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class PtyUsbSerialPort implements UsbSerialPort {
    private final FileInputStream in;
    private final FileOutputStream out;

    public PtyUsbSerialPort(File vttyFile) throws IOException {
        this.in = new FileInputStream(vttyFile);
        this.out = new FileOutputStream(vttyFile);
    }

    @Override
    public void write(byte[] src, int timeout) throws IOException {
        out.write(src);
        out.flush();
    }

    @Override
    public int read(byte[] dest, int timeout) throws IOException {
        long start = System.currentTimeMillis();
        int totalRead = 0;
        try {
            while (totalRead < dest.length && (System.currentTimeMillis() - start) < timeout) {
                int avail = in.available();
                if (avail > 0) {
                    int toRead = Math.min(avail, dest.length - totalRead);
                    int readNow = in.read(dest, totalRead, toRead);
                    if (readNow > 0) {
                        totalRead += readNow;
                    }
                } else {
                    Thread.sleep(2);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Read interrupted", e);
        }
        return totalRead;
    }

    @Override
    public void open(Object connection) {}
    
    @Override
    public void close() throws IOException {
        in.close();
        out.close();
    }
    
    @Override
    public void setParameters(int baudRate, int dataBits, int stopBits, int parity) {}
}
