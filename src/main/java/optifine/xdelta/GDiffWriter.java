package optifine.xdelta;

import java.io.DataOutputStream;
import java.io.IOException;

public class GDiffWriter implements DiffWriter {

    public byte[] buf = new byte[256];
    public int buflen = 0;
    public boolean debug = false;
    public DataOutputStream output = null;


    public GDiffWriter(DataOutputStream os) throws IOException {
        this.output = os;
        this.output.writeByte(209);
        this.output.writeByte(255);
        this.output.writeByte(209);
        this.output.writeByte(255);
        this.output.writeByte(4);
    }

    public void setDebug(boolean flag) {
        this.debug = flag;
    }

    public void addCopy(int offset, int length) throws IOException {
        if (this.buflen > 0) {
            this.writeBuf();
        }

        if (this.debug) {
            System.err.println("COPY off: " + offset + ", len: " + length);
        }

        if (offset > Integer.MAX_VALUE) {
            this.output.writeByte(255);
        } else if (offset < 65536) {
            if (length < 256) {
                this.output.writeByte(249);
                this.output.writeShort(offset);
                this.output.writeByte(length);
            } else if (length > '\uffff') {
                this.output.writeByte(251);
                this.output.writeShort(offset);
                this.output.writeInt(length);
            } else {
                this.output.writeByte(250);
                this.output.writeShort(offset);
                this.output.writeShort(length);
            }
        } else if (length < 256) {
            this.output.writeByte(252);
            this.output.writeInt(offset);
            this.output.writeByte(length);
        } else if (length > '\uffff') {
            this.output.writeByte(254);
            this.output.writeInt(offset);
            this.output.writeInt(length);
        } else {
            this.output.writeByte(253);
            this.output.writeInt(offset);
            this.output.writeShort(length);
        }

    }

    public void addData(byte b) throws IOException {
        if (this.buflen >= 246) {
            this.writeBuf();
        }

        this.buf[this.buflen] = b;
        ++this.buflen;
    }

    public void writeBuf() throws IOException {
        if (this.debug) {

            for (int ix = 0; ix < this.buflen; ++ix) {
                if (this.buf[ix] == 10) {
                    System.err.print("\\n");
                } else {
                    System.err.print((char) this.buf[ix]);
                }
            }

        }

        if (this.buflen > 0) {
            this.output.writeByte(this.buflen);
            this.output.write(this.buf, 0, this.buflen);
        }

        this.buflen = 0;
    }

    public void flush() throws IOException {
        if (this.buflen > 0) {
            this.writeBuf();
        }

        this.buflen = 0;
        this.output.flush();
    }

    public void close() throws IOException {
        this.flush();
    }
}
