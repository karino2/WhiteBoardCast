//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class Pages {

	private PDFDocument mDocument;
	private ArrayList<Page> mPageList;
	private IndirectObject mIndirectObject;
	private Array mMediaBox;
	private Array mKids;
	
	public Pages(PDFDocument document, int pageWidth, int pageHeight) {
		mDocument = document;
		mIndirectObject = mDocument.newIndirectObject();
		mPageList = new ArrayList<Page>();
		mMediaBox = new Array();
		String content[] = {"0", "0", Integer.toString(pageWidth), Integer.toString(pageHeight)};
		mMediaBox.addItemsFromStringArray(content);
		mKids = new Array();
	}
	
	public IndirectObject getIndirectObject() {
		return mIndirectObject;
	}
	
	public Page newPage() {
		Page lPage = new Page(mDocument);
		mPageList.add(lPage);
		mKids.addItem(lPage.getIndirectObject().getIndirectReference());
		return lPage;
	}
	
	public Page getPageAt(int position) {
		return mPageList.get(position);
	}
	
	public int getCount() {
		return mPageList.size();
	}
	
	public void render() {
		mIndirectObject.setDictionaryContent(
				"  /Type /Pages\n" +
				"  /MediaBox " + mMediaBox.toPDFString() + "\n" +
				"  /Count " + Integer.toString(mPageList.size()) + "\n" +
				"  /Kids " + mKids.toPDFString() + "\n"
		);
		for (Page lPage: mPageList) {
			lPage.render(mIndirectObject.getIndirectReference());
		}
	}

    public void writePagesHeader(PositionedOutputStream os, int pageNum) throws IOException {
        setupDummyKidIDs(pageNum);
        mIndirectObject.writeDictionaryContent(os,
                "  /Type /Pages\n" +
                        "  /MediaBox " + mMediaBox.toPDFString() + "\n" +
                        "  /Count " + Integer.toString(pageNum) + "\n" +
                        "  /Kids " + mKids.toPDFString() + "\n"
        );
    }

    // very dirty!
    private void setupDummyKidIDs(int pageNum) {
        int startID = 3;
        // page 0 is already added.
        for(int i = 1; i < pageNum; i++) {
            int id = startID+i*4; // for every page,  they have font, image, streamcontent. so next page = 3+1
            mKids.addItem(id+" 0 R");
        }
    }
}
