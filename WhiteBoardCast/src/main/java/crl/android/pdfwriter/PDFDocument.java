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

public class PDFDocument extends Base {

	private Header mHeader;
	private Body mBody;
	private CrossReferenceTable mCRT;
	private Trailer mTrailer;
    private PositionedOutputStream mOutputStream;
	
	public PDFDocument() {
		mHeader = new Header();
		mBody = new Body();
		mBody.setByteOffsetStart(mHeader.getPDFStringSize());
		mBody.setObjectNumberStart(0);
		mCRT = new CrossReferenceTable();
		mTrailer = new Trailer();
	}

    public PDFDocument(PositionedOutputStream outputStream) {
        mOutputStream = outputStream;
        mHeader = new Header();
        mBody = new Body(mOutputStream);
        mBody.setByteOffsetStart(mHeader.getPDFStringSize());
        mBody.setObjectNumberStart(0);
        mCRT = new CrossReferenceTable();
        mTrailer = new Trailer();
    }

	
	public IndirectObject newIndirectObject() {
		return mBody.getNewIndirectObject();
	}
	
	public IndirectObject newRawObject(String content) {
		IndirectObject iobj = mBody.getNewIndirectObject();
		iobj.setContent(content);
		return iobj;
	}
	
	public IndirectObject newDictionaryObject(String dictionaryContent) {
		IndirectObject iobj = mBody.getNewIndirectObject();
		iobj.setDictionaryContent(dictionaryContent);
		return iobj;
	}
	
	public IndirectObject newStreamObject(String streamContent) {
		IndirectObject iobj = mBody.getNewIndirectObject();
		iobj.setDictionaryContent("  /Length " + Integer.toString(streamContent.length()) + "\n");
		iobj.setStreamContent(streamContent);
		return iobj;
	}
	
	public void includeIndirectObject(IndirectObject iobj) {
		mBody.includeIndirectObject(iobj);
	}

    public void writeHeader() throws IOException {
        if(mOutputStream == null)
            throw new NullPointerException();

        mHeader.writeToStream(mOutputStream);
    }
	
	@Override
	public String toPDFString() {
		StringBuilder sb = new StringBuilder();
		sb.append(mHeader.toPDFString());
		sb.append(mBody.toPDFString());
		mCRT.setObjectNumberStart(mBody.getObjectNumberStart());
		int x = 0;
		while (x < mBody.getObjectsCount()) {
			IndirectObject iobj = mBody.getObjectByNumberID(++x);
			if (iobj != null) {
				mCRT.addObjectXRefInfo(iobj.getByteOffset(), iobj.getGeneration(), iobj.getInUse());
			}
		}
		mTrailer.setObjectsCount(mBody.getObjectsCount());
		mTrailer.setCrossReferenceTableByteOffset(sb.length());
		mTrailer.setId(Indentifiers.generateId());
		return sb.toString() + mCRT.toPDFString() + mTrailer.toPDFString();
	}

    public void writeFooter() throws IOException {
        mCRT.setObjectNumberStart(mBody.getObjectNumberStart());
        int x = 0;
        while (x < mBody.getObjectsCount()) {
            IndirectObject iobj = mBody.getObjectByNumberID(++x);
            if (iobj != null) {
                mCRT.addObjectXRefInfo(iobj.getByteOffset(), iobj.getGeneration(), iobj.getInUse());
            }
        }
        int xrefPos = mOutputStream.getPos();
        mOutputStream.write(mCRT.toPDFString());


        mTrailer.setObjectsCount(mBody.getObjectsCount());
        mTrailer.setCrossReferenceTableByteOffset(xrefPos);
        mTrailer.setId(Indentifiers.generateId());
        mOutputStream.write(mTrailer.toPDFString());
    }
	
	@Override
	public void clear() {
		mHeader.clear();
		mBody.clear();
		mCRT.clear();
		mTrailer.clear();
	}
}
