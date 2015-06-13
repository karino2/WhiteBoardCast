package com.livejournal.karino2.whiteboardcast;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * Created by karino on 6/13/15.
 */
public class ExportPDFDialog extends ProgressDialog {
    public ExportPDFDialog(Context context) {
        super(context);
    }

    BoardList boardList;
    File pdfFile;
    public void preparePDFExport(File file, BoardList boards) {
        this.boardList = boards;
        this.pdfFile = file;

        (new AsyncTask<Void, Integer, Boolean>(){

            @Override
            protected Boolean doInBackground(Void... params) {

                ImagePDFWriter writer = null;
                try {
                    writer = new ImagePDFWriter(pdfFile, boardList.getWidth(), boardList.getHeight(), boardList.size());
                    for(int i = 0; i < boardList.size(); i++) {
                        writer.writePage(boardList.getBoard(i).createSynthesizedTempBmp());
                        publishProgress(i);
                    }
                    writer.done();
                    return true;
                } catch (IOException e) {
                    errorMessage = "Fail to write pdf: " + e.getMessage();
                    return false;
                }
            }

            String errorMessage;

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                ExportPDFDialog.this.setMessage("Export page: " + values[0]);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                ExportPDFDialog.this.dismiss();
                if(success) {
                    showMessage("Finish export pdf: " + pdfFile.getAbsolutePath());
                } else {
                    showMessage(errorMessage);
                }
            }
        }).execute();

    }

    public void showMessage(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

    }

}
