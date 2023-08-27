package com.livejournal.karino2.whiteboardcast

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.Arrays

class WorkFileStore(val fileStoreDirectory: File) {
    companion object {
        @Throws(IOException::class)
        fun ensureDirExist(dir: File) {
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    throw IOException()
                }
            }
        }

        fun getFileStoreDirectory(context: Context): File {
            return try {
                val dir = File(context.filesDir, "WorkDir")
                ensureDirExist(dir)
                dir
            } catch (e: IOException) {
                throw RuntimeException("Fail to create dir inside app specific directory. Should never happen.")
            }
        }

        fun deleteAllFiles(folder: File) {
            folder.listFiles { pathname -> !pathname.isDirectory }
                ?.forEach { it.delete() }
        }


    }
    constructor(context : Context) : this(getFileStoreDirectory(context))

    val workVideoPath by lazy { fileStoreDirectory.absolutePath +  "/temp.mp4" }

    val slideListDirectory by lazy {
        File(fileStoreDirectory, "slides").apply { ensureDirExist(this) }
    }

    val thumbnailDirectory by lazy {
        File(fileStoreDirectory, "thumbnail").apply { ensureDirExist(this) }
    }

    val temporaryPdfFolder by lazy {
        File(fileStoreDirectory, "temporaryPdf").apply { ensureDirExist(this) }
    }

    // delete created mp4 files except for workVideoFiles.
    fun deletePreviousCreatedMovieFiles() {
        fileStoreDirectory.listFiles { fi-> fi.isFile && fi.path.endsWith(".mp4") && fi.name != "temp.mp4"}
            .forEach{ file-> file.delete() }
    }

    fun getThumbnailFile(parent: File) = File(thumbnailDirectory, parent.name)

    @Throws(IOException::class)
    fun listActualSlideFiles(folder: File): List<File> {
        return folder.listFiles { _, filename ->
            filename.endsWith(".png") || filename.endsWith(".PNG") ||
                    filename.endsWith(".jpg") || filename.endsWith(".JPG")
        }?.toList()
        ?.sortedWith { f1, f2->
                f2.lastModified().compareTo(f1.lastModified())  } ?: emptyList()
    }
}