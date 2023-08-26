package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by karino on 6/13/15.
 */
public class ImagePDFWriter {
    PdfDocument document;
    File output;
    FileOutputStream outputStream;


    int width;
    int height;

    public ImagePDFWriter(File outFile, int width, int height, int pageNum) throws IOException {
        output = outFile;
        output.createNewFile();
        outputStream = new FileOutputStream(output);
        document = new PdfDocument();
        this.width = width;
        this.height = height;


    }

    public void done() throws IOException {
        document.writeTo(outputStream);
        document.close();
        outputStream.close();
        outputStream = null;
    }

    int currentPage = 1;

    public void writePage(Bitmap image) throws IOException {
        PdfDocument.Page curPage = document.startPage( new PdfDocument.PageInfo.Builder(width, height, currentPage).create() );
        curPage.getCanvas().drawBitmap(image, 0.0f, 0.0f, null);
        document.finishPage(curPage);
        currentPage++;
    }

}
