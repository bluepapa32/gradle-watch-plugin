package com.bluepapa32.gradle.plugins.watch;

import java.io.OutputStream;
import java.io.PrintStream;

public class DevNullPrintStream extends PrintStream {

    public DevNullPrintStream() {
        super(new OutputStream() {
            public void write(int b) { }
            public void write(byte[] b) { }
            public void write(byte[] b, int off, int len) { }
        });
    }
}
