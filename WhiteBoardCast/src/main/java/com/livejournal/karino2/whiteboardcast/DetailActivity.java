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

    // only support ".mp4" for a while.
    String baseName(File file) {
        String name = file.getName();
        if(!name.endsWith(".mp4"))
            throw new IllegalArgumentException("Not .mp4 extension: " + name);
        return name.substring(0, name.length()-4);
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
            videoUri = Uri.parse(intent.getStringExtra("VIDEO_URI"));
            pdfFile = new File(intent.getStringExtra("PDF_PATH"));

            setLabelToNameButton(baseName(videoFile));

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
        outState.putString("VIDEO_URI", videoUri.toString());
        outState.putString("PDF_PATH", pdfFile.getAbsolutePath());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        videoFile = new File(savedInstanceState.getString("VIDEO_PATH"));
        videoUri = Uri.parse(savedInstanceState.getString("VIDEO_URI"));
        pdfFile = new File(savedInstanceState.getString("PDF_PATH"));
        setLabelToNameButton(baseName(videoFile));
    }

    private void setLabelToNameButton(String newName) {
        Button btn = (Button)findViewById(R.id.buttonVideoName);

        btn.setText(newName);
    }

    File videoFile;
    Uri videoUri;
    File pdfFile;

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void viewVideoIntent() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(videoUri, "video/*");
        // i.setDataAndType(Uri.fromFile(videoFile), "video/*");
        i.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_send:
                shareVideoIntent();
                return true;
            case R.id.action_export:
                try {
                    copyPdf();
                } catch (IOException e) {
                    showMessage("Fail to copy pdf file: " + e.getMessage());
                }
                return true;
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void copyFile(File src, File dest) throws IOException {
        dest.createNewFile();
        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(src).getChannel();
            destination = new FileOutputStream(dest).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }


    private void copyPdf() throws IOException {
        Button btn = (Button)findViewById(R.id.buttonVideoName);
        File targetPdf = new File(WhiteBoardCastActivity.getFileStoreDirectory(), btn.getText().toString() + ".pdf");
        copyFile(pdfFile, targetPdf);
        showMessage("Export to: " + targetPdf.getAbsolutePath());
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
                et.setText(baseName(videoFile));

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

        File newNameFile = new File(videoFile.getParentFile(), newName);
        if(newNameFile.exists()) {
            showMessage("File \"" + newNameFile.getAbsolutePath()+ "\" is already exists");
            return false;
        }

        videoFile.renameTo(newNameFile);
        videoFile = newNameFile;
        updateNewFileNameToContentDB(videoFile);
        return true;
    }

    private void updateNewFileNameToContentDB(File newFile) {
        ContentValues content = new ContentValues(2);

        long id = ContentUris.parseId(videoUri);


        content.put(MediaStore.Video.Media.DATA, newFile.getAbsolutePath());
        content.put(MediaStore.Video.Media.DISPLAY_NAME, newFile.getName());
        ContentResolver resolver = getBaseContext().getContentResolver();
        resolver.update(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, content, "_id = ?", new String[] {String.valueOf(id)});
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
                    setLabelToNameButton(baseName(videoFile));
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
