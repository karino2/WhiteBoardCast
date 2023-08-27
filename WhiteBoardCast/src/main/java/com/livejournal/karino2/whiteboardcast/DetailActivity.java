package com.livejournal.karino2.whiteboardcast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class DetailActivity extends Activity {
    final int DIALOG_ID_FILE_RENAME = 1;
    final int REQUEST_CREATE_FILE_ID = 2;

    // only support ".mp4" for a while.
    private static String baseName(String name) {
        if(!name.endsWith(".mp4"))
            throw new IllegalArgumentException("Not .mp4 extension: " + name);
        return name.substring(0, name.length() - 4);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        getActionBar().setDisplayHomeAsUpEnabled(true);


        Button btn = (Button)findViewById(R.id.buttonVideoName);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_ID_FILE_RENAME);
            }
        });

        FrameLayout frame = (FrameLayout)findViewById(R.id.videoThumbnailFame);
        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewVideoIntent();
            }
        });


        Intent intent = getIntent();
        if(intent != null)
        {
            videoFile = new File(intent.getStringExtra("VIDEO_PATH"));
            displayName = videoFile.getName();
            videoUri = Uri.parse(intent.getStringExtra("VIDEO_URI"));
            pdfFile = new File(intent.getStringExtra("PDF_PATH"));

            setLabelToNameButton(baseName(displayName));

            Bitmap bmp = ThumbnailUtils.createVideoThumbnail(videoFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);

            ImageView iv = (ImageView)findViewById(R.id.videoThumbnailView);
            iv.setImageBitmap(bmp);

            // VideoView vv;

            // iv.setImageURI(videoUri);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("VIDEO_PATH", videoFile.getAbsolutePath());
        outState.putString("DISP_NAME", displayName);
        outState.putString("VIDEO_URI", videoUri.toString());
        outState.putString("PDF_PATH", pdfFile.getAbsolutePath());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        videoFile = new File(savedInstanceState.getString("VIDEO_PATH"));
        displayName = savedInstanceState.getString("DISP_NAME");
        videoUri = Uri.parse(savedInstanceState.getString("VIDEO_URI"));
        pdfFile = new File(savedInstanceState.getString("PDF_PATH"));
        setLabelToNameButton(baseName(displayName));
    }

    private void setLabelToNameButton(String newName) {
        Button btn = (Button)findViewById(R.id.buttonVideoName);

        btn.setText(newName);
    }

    private WorkFileStore fileStore = null;
    public WorkFileStore getFileStore() {
        if (fileStore == null)
            fileStore = new WorkFileStore(this);
        return fileStore;
    }

    File videoFile;
    String displayName;
    Uri videoUri;
    File pdfFile;

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void viewVideoIntent() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(videoUri, "video/*");
        i.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    private void requestExportFile()
    {
        Button btn = (Button)findViewById(R.id.buttonVideoName);
        String pdfFileName =  btn.getText().toString() + ".pdf";


        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, pdfFileName);

        startActivityForResult(intent, REQUEST_CREATE_FILE_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CREATE_FILE_ID && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                try {
                    copyPdfTo(data.getData());
                } catch(IOException ie) {
                    showMessage("Fail to write pdf file.");
                }
            }
            else {
                showMessage("Can't get uri of selected pdf file.");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_send) {
            shareVideoIntent();
            return true;
        } else if (id == R.id.action_export) {
            requestExportFile();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void copyPdfTo(Uri targetUri) throws IOException {
        WhiteBoardCastActivity.copyFileTo(getContentResolver(), pdfFile, targetUri);
        showMessage("Export pdf done");
    }



    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ID_FILE_RENAME:
                return createFileNameDialog();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
            case DIALOG_ID_FILE_RENAME:
                EditText et = (EditText)dialog.findViewById(R.id.edit_filename);
                // back from another app but this app is already killed. just dismiss.
                et.setText(baseName(displayName));

                setupFileNameViewListener(dialog);

                break;
        }
        super.onPrepareDialog(id, dialog);
    }

    private boolean renameFileNameTo(String newName) {
        if(newName.equals(""))
        {
            showMessage("Empty file name!");
            return false;
        }

        displayName = newName;
        updateDisplayNameToContentDB(newName);
        return true;
    }

    private void updateDisplayNameToContentDB(String newDisplayName) {
        ContentValues content = new ContentValues(2);

        content.put(MediaStore.Video.Media.DISPLAY_NAME, newDisplayName);
        ContentResolver resolver = getBaseContext().getContentResolver();
        resolver.update(videoUri, content, null, null);
    }

    private void shareVideoIntent() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("video/mp4");

        i.putExtra(Intent.EXTRA_STREAM, videoUri);
        startActivity(Intent.createChooser(i, "Share video"));

    }

    Dialog fileRenameDialog;
    private Dialog createFileNameDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.rename, null);
        fileRenameDialog = new AlertDialog.Builder(this).setTitle(R.string.label_file_name)
                .setView(view)
                .create();
        return fileRenameDialog;
    }


    private void setupFileNameViewListener(Dialog dialog) {
        fileRenameDialog = dialog;
        setOnClickListener(fileRenameDialog, R.id.button_cancel, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileRenameDialog.dismiss();
            }
        });
        setOnClickListener(fileRenameDialog, R.id.button_save, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText)fileRenameDialog.findViewById(R.id.edit_filename);
                boolean success = renameFileNameTo(et.getText().toString() + ".mp4");
                if(success) {
                    setLabelToNameButton(baseName(displayName));
                    fileRenameDialog.dismiss();
                }
            }
        });
    }

    private void setOnClickListener(Dialog dialog, int id, View.OnClickListener onclick) {
        Button button = (Button)dialog.findViewById(id);
        button.setOnClickListener(onclick);
    }

}
