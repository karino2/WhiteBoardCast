package com.livejournal.karino2.whiteboardcast

import android.graphics.Bitmap


/**
 * Created by karino on 11/29/13.
 */
class CacheEngine {
    var keys = ArrayList<String>()
    var entries = HashMap<String, Bitmap>()
    var cacheSize = 100 // default size

    internal constructor()
    constructor(size: Int) {
        cacheSize = size
    }

    fun lookup(path: String): Bitmap? {
        return if (entries.containsKey(path)) entries[path] else null
    }


    fun put(path: String, thumbnail: Bitmap) {
        if (keys.contains(path)) {
            keys.remove(path)
            keys.add(path)
            return
        }
        keys.add(path)
        entries[path] = thumbnail
        if (keys.size > cacheSize) {
            val first = keys[0]
            entries.remove(first)
            keys.removeAt(0)
        }
    }
}
