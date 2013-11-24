package com.livejournal.karino2.multigallery;

import java.util.ArrayList;

/**
* Created by karino on 11/24/13.
*/
class AlbumSlidingWindow {
    final int CACHE_SIZE =  96;
    ImageItem[] entries;
    int contentStart;
    int contentEnd;
    AlbumLoader loader;
    AlbumSlidingWindow(AlbumLoader loader) {
        this.loader = loader;
        entries = new ImageItem[CACHE_SIZE];
        contentStart = contentEnd = 0;
    }

    private ImageItem get(int slotIndex) {
        if(!isContentSlot(slotIndex)) {
            throw new RuntimeException("invalid slot:" + slotIndex + ", (" + contentStart + ", " + contentEnd + ")");
        }
        return entries[slotIndex % entries.length];
    }

    boolean isContentSlot(int slotIndex) {
        return slotIndex >= contentStart && slotIndex < contentEnd;
    }

    public /* static */ int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public ImageItem requestSlot(int slotIndex) {
        if(isContentSlot(slotIndex))
            return get(slotIndex);


        ImageItem[] data = entries;
        int contentStart = clamp(slotIndex - data.length / 2,
                0, Math.max(0, size() - data.length));
        int contentEnd = Math.min(contentStart + data.length, size());
        setContentWindow(contentStart, contentEnd);

        return get(slotIndex);
    }


    void setContentWindow(int argContentStart, int argContentEnd) {
        if(contentStart == argContentStart && contentEnd == argContentEnd) return;

        if(argContentStart >= contentEnd || contentStart >= argContentEnd) {
            for(int i = contentStart, n=contentEnd; i<n; ++i ) {
               freeSlotContent(i);
            }

            // block here. I think this is fast enough.
            ArrayList<ImageItem> items = loader.getMediaItem(argContentStart, argContentEnd);

            for(int i = argContentStart; i < argContentEnd; ++i) {
                putSlotContent(i, items.get(i - argContentStart));
            }
        } else if(argContentStart > contentStart){
            for (int i = contentStart; i < argContentStart; ++i) {
                freeSlotContent(i);
            }

            // block here. I think this is fast enough.
            ArrayList<ImageItem> items = loader.getMediaItem(contentEnd, argContentEnd);

            for (int i = contentEnd; i < argContentEnd; ++i) {
                putSlotContent(i, items.get(i - contentEnd));
            }
        } else /* argContentStart < contentStart */ {
            for (int i = argContentEnd, n = contentEnd; i < n; ++i) {
                freeSlotContent(i);
            }

            // block here. I think this is fast enough.
            ArrayList<ImageItem> items = loader.getMediaItem(argContentStart, contentStart);

            for (int i = argContentStart, n = contentStart; i < n; ++i) {
                putSlotContent(i, items.get(i - argContentStart));
            }
        }
        contentStart = argContentStart;
        contentEnd = argContentEnd;

    }

    private void putSlotContent(int slotIndex, ImageItem imageItem) {
        entries[slotIndex%entries.length] = imageItem;

    }

    private void freeSlotContent(int slotIndex) {
        entries[slotIndex%entries.length] = null;
    }


    public int size() {
        return loader.getItemCount();
    }
}
