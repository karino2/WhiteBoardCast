//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.io.IOException;

public class Stream extends EnclosedContent {

    boolean mIsBinary = false;
	public Stream() {
		super();
		setBeginKeyword("stream",false,true);
		setEndKeyword("endstream",false,true);
	}

    byte[] mBinaryContent;
    public void setBinaryContent(byte[] content)
    {
        mIsBinary = true;
        mBinaryContent = content;
    }

    @Override
    public boolean hasContent() {
        if(mIsBinary)
            return mBinaryContent.length > 0;
        return super.hasContent();
    }


    @Override
    public void writePDFString(PositionedOutputStream os) throws IOException {
        writeBegin(os);
        if(mIsBinary)
            os.write(mBinaryContent);
        else
            os.write(mContent.toString());
        writeEnd(os);
    }


}
