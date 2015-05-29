//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.io.IOException;
import java.io.OutputStream;

public class Header extends Base {

	private String mVersion;
	private String mRenderedHeader;

	public Header() {
		clear();
	}
	public void setVersion(int Major, int Minor) {
		mVersion = Integer.toString(Major) + "." + Integer.toString(Minor);
		render();
	}
	
	public int getPDFStringSize() {
		return mRenderedHeader.length();
	}

    String mFirstLine;

	private void render() {
        mFirstLine = "%PDF-" + mVersion + "\n";
        mRenderedHeader = "%PDF-" + mVersion + "\n%����\n";
	}
	
	@Override
	public String toPDFString() {
		return mRenderedHeader;
	}

    public void writeToStream(PositionedOutputStream os) throws IOException {
        os.write(mFirstLine);
        // what ever.
        os.write("%");
        os.write(0xa9);
        os.write(0xbb);
        os.write(0xaa);
        os.write(0xb5);
        os.write("\n");
    }

	@Override
	public void clear() {
		setVersion(1, 4);
	}

}
