//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class IndirectObject extends Base {

	private EnclosedContent mContent;
	private Dictionary mDictionaryContent;
	private Stream mStreamContent;
	private IndirectIdentifier mID;
	private int mByteOffset;
	private boolean mInUse;

	public IndirectObject() {
		clear();
	}
	
	public void setNumberID(int Value) {
		mID.setNumber(Value);
	}

	public int getNumberID() {
		return mID.getNumber();
	}

	public void setGeneration(int Value) {
		mID.setGeneration(Value);
	}

	public int getGeneration() {
		return mID.getGeneration();
	}
	
	public String getIndirectReference() {
		return mID.toPDFString() + " R";
	}

	public void setByteOffset(int Value) {
		mByteOffset = Value;
	}
	
	public int getByteOffset() {
		return mByteOffset;
	}

	public void setInUse(boolean Value) {
		mInUse = Value;
	}
	
	public boolean getInUse() {
		return mInUse;
	}
	
	public void addContent(String Value) {
		mContent.addContent(Value);		
	}

	public void setContent(String Value) {
		mContent.setContent(Value);		
	}

	public String getContent() {
		return mContent.getContent();
	}
	
	public void addDictionaryContent(String Value) {
		mDictionaryContent.addContent(Value);		
	}

	public void setDictionaryContent(String Value) {
		mDictionaryContent.setContent(Value);		
	}

	public String getDictionaryContent() {
		return mDictionaryContent.getContent();		
	}
	
	public void addStreamContent(String Value) {
		mStreamContent.addContent(Value);		
	}

    public void setStreamContentBinary(byte[] content) {
        mStreamContent.setBinaryContent(content);
    }

	public void setStreamContent(String Value) {
		mStreamContent.setContent(Value);		
	}

	public String getStreamContent() {
		return mStreamContent.getContent();
	}
	
	protected String render() {
		StringBuilder sb = new StringBuilder();
		sb.append(mID.toPDFString());
		sb.append(" ");
		// j-a-s-d: this can be performed in inherited classes DictionaryObject and StreamObject
		if (mDictionaryContent.hasContent()) {
			mContent.setContent(mDictionaryContent.toPDFString());
			if (mStreamContent.hasContent())
				mContent.addContent(mStreamContent.toPDFString());
		}
		sb.append(mContent.toPDFString());
		return sb.toString();
	}

    public void writeToStreamAndPurge(PositionedOutputStream os) throws IOException {
        writeToStream(os);

        mDictionaryContent = null;
        mStreamContent = null;
        mContent = null;
    }

    public void writeToStream(PositionedOutputStream os) throws IOException {
        mByteOffset = os.getPos();

        os.write(mID.toPDFString());
        os.write(" ");
        mContent.writeBegin(os);
        if (mDictionaryContent.hasContent()) {
            mDictionaryContent.writePDFString(os);
            if (mStreamContent.hasContent()) {
                mStreamContent.writePDFString(os);
            }
        }
        mContent.writeEnd(os);
    }

    @Override
	public void clear() {
		mID = new IndirectIdentifier();
		mByteOffset = 0;
		mInUse = false;
		mContent = new EnclosedContent();
		mContent.setBeginKeyword("obj", false, true);
		mContent.setEndKeyword("endobj", false, true);
		mDictionaryContent = new Dictionary();
		mStreamContent = new Stream();
	}

	@Override
	public String toPDFString() {
		return render();
	}

    public void writeDictionaryStreamContent(PositionedOutputStream outputStream, String streamContent, String dictionaryContent) throws IOException {
        mByteOffset = outputStream.getPos();

        outputStream.write(mID.toPDFString());
        outputStream.write(" ");
        mContent.writeBegin(outputStream);
        mDictionaryContent.writeBegin(outputStream);
        outputStream.write(dictionaryContent);
        mDictionaryContent.writeEnd(outputStream);
        if(streamContent != null) {
            mStreamContent.writeBegin(outputStream);
            outputStream.write(streamContent);
            mStreamContent.writeEnd(outputStream);
        }

        mContent.writeEnd(outputStream);

    }

    public void writeDictionaryContent(PositionedOutputStream outputStream, String content) throws IOException {
        writeDictionaryStreamContent(outputStream, null, content);
    }
}
