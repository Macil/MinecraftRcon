package tech.macil.minecraft.rcon.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

// Just like BufferedReader but only reads lines that ends in '\n'.
public class NLRequiringBufferedReader extends BufferedReader {
    public NLRequiringBufferedReader(Reader in) {
        super(in);
    }

    public NLRequiringBufferedReader(Reader in, int sz) {
        super(in, sz);
    }

    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int r = read();
            if (r == -1) {
                return null;
            }
            if (r == '\n') {
                return sb.toString();
            }
            sb.append((char) r);
        }
    }
}
