package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import crl.android.pdfwriter.PDFWriter;

/**
 * Created by karino on 6/13/15.
 */
public class ImagePDFWriter {
    PDFWriter writer;
    File output;
    FileOutputStream outputStream;

    public ImagePDFWriter(File outFile, int width, int height, int pageNum) throws IOException {
        output = outFile;
        output.createNewFile();
        outputStream = new FileOutputStream(output);
        writer = new PDFWriter(width, height, outputStream);

        writer.writeHeader();
        writer.writeCatalogStream();
        writer.writePagesHeader(pageNum);

    }

    public void done() throws IOException {
        writer.writeFooter();
        outputStream.close();
        outputStream = null;
    }

    int currentPage = 0;

    public void writePage(byte[] deflatedImage, int width, int height) throws IOException {
        if(currentPage != 0)
            writer.newOrphanPage();

        currentPage++;
        writer.writeDeflatedImagePage(deflatedImage, width, height);
    }
}
