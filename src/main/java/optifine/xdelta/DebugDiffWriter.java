package optifine.xdelta;

import java.io.IOException;

public class DebugDiffWriter implements DiffWriter {

    public byte[] buf = new byte[256];
    public int buflen = 0;


    public void addCopy(int offset, int length) throws IOException {
        if (this.buflen > 0) {
            this.writeBuf();
        }

        System.err.println("COPY off: " + offset + ", len: " + length);
    }

    public void addData(byte b) throws IOException {
        if (this.buflen < 256) {
            this.buf[this.buflen++] = b;
        } else {
            this.writeBuf();
        }

    }

    public void writeBuf() {
        System.err.print("DATA: ");

        for (int ix = 0; ix < this.buflen; ++ix) {
            if (this.buf[ix] == 10) {
                System.err.print("\\n");
            } else {
                System.err.print((char) this.buf[ix]);
            }
        }

        System.err.println();
        this.buflen = 0;
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
    }
}
