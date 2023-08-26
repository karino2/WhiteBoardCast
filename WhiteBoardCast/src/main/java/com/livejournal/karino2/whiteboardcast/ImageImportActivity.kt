package com.livejournal.karino2.whiteboardcast

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ImageImportActivity : AppCompatActivity() {
    private fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private val imgFiles = mutableListOf<File>()

    class IIAdapter(private val imgFiles: MutableList<File>, private val requestDrag : (IIAdapter.ViewHolder)->Unit) : RecyclerView.Adapter<IIAdapter.ViewHolder>() {

        @SuppressLint("ClickableViewAccessibility")
        class ViewHolder(view: View, private val requestDrag : (IIAdapter.ViewHolder)->Unit) :RecyclerView.ViewHolder(view) {
            val draggableHandle : ImageView
            val contentView : ImageView

            init {
                draggableHandle = view.findViewById(R.id.imageViewDragHandle)
                contentView = view.findViewById(R.id.imageViewContent)
                draggableHandle.setOnTouchListener { view, motionEvent ->
                    if(motionEvent.actionMasked == MotionEvent.ACTION_DOWN)
                        requestDrag(ViewHolder@this)
                    false
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.image_import_item, parent, false)
            return ViewHolder(view) { requestDrag(it) }
        }

        override fun getItemCount() = imgFiles.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.contentView.setImageBitmap(BitmapFactory.decodeFile(imgFiles[position].absolutePath))
        }

        fun moveRow(fromPos: Int, toPos: Int) {
            val target = imgFiles[fromPos]
            imgFiles.removeAt(fromPos)
            imgFiles.add(toPos, target)
            notifyItemMoved(fromPos, toPos)
        }
    }


    private var pickMedia =
        registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(100)
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                startImportTask(uris.map { it.toString() })
            } else {
                showMessage("No image selected")
            }
        }

    private var canvasWidth = 0
    private var canvasHeight = 0

    private val fileStore by lazy { WorkFileStore(this) }
    private val slideList by lazy { SlideList(fileStore) }

    private val adapter : IIAdapter by lazy { IIAdapter(imgFiles) {
            touchHelper.startDrag(it)
        }
    }

    private val touchHelper by lazy {
        ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveRow(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun isItemViewSwipeEnabled() = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        } )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_import)

        intent?.let {
            canvasWidth = it.getIntExtra("CANVAS_WIDTH", 0)
            canvasHeight = it.getIntExtra("CANVAS_HEIGHT", 0)
            slideList.deleteAll()
            imgFiles.clear()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = adapter


        touchHelper.attachToRecyclerView(recyclerView)

        if(slideList.files.isEmpty())
            launchPickImage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("CANVAS_WIDTH", canvasWidth)
        outState.putInt("CANVAS_HEIGHT", canvasHeight)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        canvasWidth = savedInstanceState.getInt("CANVAS_WIDTH")
        canvasHeight = savedInstanceState.getInt("CANVAS_HEIGHT")
    }

    private fun launchPickImage() {
        pickMedia.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ImageOnly)
                .build()
        )
    }

    fun setResultToResultPaths() {
        val intent = Intent()
        intent.putStringArrayListExtra("all_path", ArrayList<String>( imgFiles.map { it.absolutePath } ))
        setResult(RESULT_OK, intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_done) {
            setResultToResultPaths()
            finish()
            return true
        }
        else if(id == R.id.action_pick_image)
        {
            launchPickImage()
            return true
        }
        /*
        if (id == android.R.id.home && isAlbum) {
            // finishAlbumAndStartAlbumSet()
            return true
        }
         */
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.multi_gallery, menu)
        return true
    }

    private fun startImportTask(imagePaths: List<String>) {
        val imp = ImportDialog(this)
        imp.prepareCopy(
            contentResolver, canvasWidth,
            canvasHeight, imagePaths
        ) {
            imgFiles.addAll(imp.imported)
            adapter.notifyDataSetChanged()
        }
    }

}