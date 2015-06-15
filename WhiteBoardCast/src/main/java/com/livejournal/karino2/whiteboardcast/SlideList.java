package com.livejournal.karino2.whiteboardcast;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by karino on 10/23/13.
 */
public class SlideList {
    File slideFolder;
    public SlideList(File slideFolder) throws IOException {
        this.slideFolder = slideFolder;
    }

    public static SlideList createSlideListWithDefaultFolder() throws IOException {
        File folder = getSlideListDirectory();
        return new SlideList(folder);
    }

    public static File[] getActualSlideFiles(File folder) throws IOException {
        File[] slideFiles = folder.listFiles(new FilenameFilter() {
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

    public static File getSlideListDirectory() throws IOException {
        File parent = WhiteBoardCastActivity.getFileStoreDirectory();
        File dir = new File(parent, "slides");
        WhiteBoardCastActivity.ensureDirExist(dir);
        return dir;
    }

    public List<File> getFiles() throws IOException {
        return Arrays.asList(getActualSlideFiles(slideFolder));
    }



    public static void deleteAllFiles(File folder) {
        for(File file : folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if(pathname.isDirectory())
                    return false;
                return true;
            }
        })) {
            file.delete();
        }

    }

    public static File getThumbnailDirectory() throws IOException {
        return ImportDialog.getThumbnailDirectory();
    }

    public void deleteAll() {
        deleteAllFiles(slideFolder);
        try {
            deleteAllFiles(getThumbnailDirectory());
        } catch (IOException e) {
            // fail to get thumbnail directory, so can't delete itself is not a problem.
        }
    }
}
