package com.livejournal.karino2.whiteboardcast;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by karino on 10/23/13.
 */
public class SlideList {
    WorkFileStore fileStore;
    File slideFolder;
    public SlideList(WorkFileStore fileStore) {
        this.fileStore = fileStore;
        this.slideFolder = fileStore.getSlideListDirectory();
    }

    public List<File> getFiles() throws IOException {
        return fileStore.listActualSlideFiles(slideFolder);
    }

    public static void deleteAllFiles(File folder) {
        WorkFileStore.Companion.deleteAllFiles(folder);
    }


    public void deleteAll() {
        deleteAllFiles(slideFolder);
        deleteAllFiles(fileStore.getThumbnailDirectory());
    }
}
