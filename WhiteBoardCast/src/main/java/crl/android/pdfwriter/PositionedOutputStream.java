package crl.android.pdfwriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by karino on 12/18/14.
 */
public class PositionedOutputStream  {

    OutputStream stream;
    public PositionedOutputStream(OutputStream original){
        stream = original;
        pos = 0;
    }

    int pos;

    public int getPos() { return pos; }
    public void write(byte[] bytes) throws IOException {
        stream.write(bytes);
        pos += bytes.length;
    }

    public void write(String val) throws IOException {
        write(val.getBytes("ISO-8859-1"));
    }

    public void write(int oneByte) throws IOException {
        stream.write(oneByte);
        pos+= 1;
    }

}
