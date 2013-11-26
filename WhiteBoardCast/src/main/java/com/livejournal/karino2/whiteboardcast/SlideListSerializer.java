package com.livejournal.karino2.whiteboardcast;

import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by karino on 10/23/13.
 */
public class SlideListSerializer {
    File jsonFile;
    File slideFolder;
    public SlideListSerializer(File slideFolder) {
        this.slideFolder = slideFolder;
        jsonFile = new File(slideFolder, "files.json");
    }

    public File[] getActualSlideFiles() throws IOException {
        File[] slideFiles = slideFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.endsWith(".png") || filename.endsWith(".PNG") ||
                        filename.endsWith(".jpg") || filename.endsWith(".JPG"))
                    return true;
                return false;
            }
        });
        Arrays.sort(slideFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
            }

        });

        return slideFiles;
    }



    public List<File> parseFileList() throws IOException {
        ArrayList<File> files = new ArrayList<File>();
        try {
            FileInputStream is = new FileInputStream(jsonFile);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder builder = new StringBuilder();
                String line;
                while(( line = reader.readLine()) != null ) {
                    builder.append( line );
                    builder.append( '\n' );
                }
                JSONArray array = (JSONArray) new JSONTokener(builder.toString()).nextValue();
                for(int i = 0; i < array.length(); i++) {
                    String path = array.getString(i);
                    files.add(new File(path));
                }
                return files;
            }finally {
                is.close();
            }
        }catch(FileNotFoundException fe) {
            return files;
        } catch (JSONException e) {
            return files;
        }
    }

    public void save(List<File> files) throws IOException {
        JSONArray array = new JSONArray();
        for(File f :files) {
            array.put(f.getAbsolutePath());
        }
        FileWriter writer = new FileWriter(jsonFile);
        writer.write(array.toString());
        writer.close();
    }

    public static File getSlideListDirectory() throws IOException {
        File parent = WhiteBoardCastActivity.getFileStoreDirectory();
        File dir = new File(parent, "slides");
        WhiteBoardCastActivity.ensureDirExist(dir);
        return dir;
    }

    public static SlideListSerializer createSlideSelializer() throws IOException {
        return new SlideListSerializer(getSlideListDirectory());
    }

    public static File[] getActualFiles() throws IOException {
        SlideListSerializer parser = SlideListSerializer.createSlideSelializer();
        return parser.getActualSlideFiles();
    }

    public static SlideList createSlideListWithDefaultFolder() throws IOException {
        SlideListSerializer parser = SlideListSerializer.createSlideSelializer();
        SlideList slideList = new SlideList(parser.parseFileList(), parser.getActualSlideFiles());
        return slideList;
    }

    public static void updateActualFiles(SlideList slideList) throws IOException {
        slideList.invalidateActualAndSync(SlideListSerializer.getActualFiles());
    }

}
