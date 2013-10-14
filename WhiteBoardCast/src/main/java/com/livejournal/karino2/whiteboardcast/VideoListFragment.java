package com.livejournal.karino2.whiteboardcast;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.livejournal.karino2.whiteboardcast.dummy.DummyContent;

import java.io.File;
import java.io.IOException;

/**
 * A list fragment representing a list of Videos. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link VideoDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class VideoListFragment extends ListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public VideoListFragment() {
    }

    void showError(String msg) {
        Log.d("WBCast", msg);
        showMessage(msg);
    }

    private void showMessage(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        ContentResolver resolver = getActivity().getContentResolver();

        try {

            MediaDatabase mediaDatabase = new MediaDatabase(resolver);
            Cursor cursor = mediaDatabase.getAllVideos();

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(), R.layout.list_video_item, cursor,
                    new String[]{"_id"}, new int[]{R.id.imageView}, 0);

            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                    if(columnIndex == 0) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 1;
                        Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(getActivity().getContentResolver(), cursor.getLong(cursor.getColumnIndex("_id")), MediaStore.Video.Thumbnails.MINI_KIND, options);
                        ((ImageView) view).setImageBitmap(bitmap);
                        return true;
                    }
                    return false;
                }
            });

            /*
            TODO: implement DetailView.
            - Place view, share button
            - impelement rename
            -- rename
            -- update MediaStore DB
            - Improve layout
             */
            /*
            CursorAdapter adapter = new CursorAdapter(getActivity(), cursor, true) {
                @Override
                public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                    // TODO: this is detail code...
                    LayoutInflater inflater =getActivity().getLayoutInflater();
                    View view = inflater.inflate(R.layout.detail_video_item, null);
                    return view;
                }

                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    // TODO: this is detail code...
                    final int DATA_INDEX = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

                    String uriString = cursor.getString(DATA_INDEX);
                    ((EditText)view.findViewById(R.id.editPath)).setText(uriString);
                    Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(uriString, MediaStore.Video.Thumbnails.MINI_KIND);
                    ((ImageView)view.findViewById(R.id.imageView)).setImageBitmap(thumbnail);

                    // TODO: add button handler.
                }
            };
            */

            /*
                        CursorAdapter adapter = new CursorAdapter(getActivity(),cursor, R.layout.detail_video_item,
                    null, new String[] {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATA, MediaStore.Video.VideoColumns.DATE_ADDED}, new int[] {R.id.imageView, R.id.editPath, R.id.btnSave});

            */
            setListAdapter(adapter);

        } catch (IOException e) {
            showError(e.getMessage());
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
