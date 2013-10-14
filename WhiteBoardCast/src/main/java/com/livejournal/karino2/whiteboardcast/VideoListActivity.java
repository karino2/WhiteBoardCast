package com.livejournal.karino2.whiteboardcast;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.io.IOException;


/**
 * An activity representing a list of Videos. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link VideoDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link VideoListFragment} and the item details
 * (if present) is a {@link VideoDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link VideoListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class VideoListActivity extends FragmentActivity
        implements VideoListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        if (findViewById(R.id.video_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((VideoListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.video_list))
                    .setActivateOnItemClick(true);
        }

        String detailUriString = getIntent().getStringExtra("DetailUri");
        if(detailUriString != null) {
            long id = urlStringToId(detailUriString);
            if(mTwoPane) {
                selectDetailItem(id);
            } else {
                startDetailActivity(id);
                finish();
            }

        }
    }

    private long urlStringToId(String detailUriString) {
        MediaDatabase database = new MediaDatabase(getContentResolver());
        try {
            return database.uriToId(detailUriString);
        } catch (IOException e) {
            Log.d("WBCast", "never reached here: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Callback method from {@link VideoListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(long id) {
        // TODO: modify id to uri.
        if (mTwoPane) {
            selectDetailItem(id);
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            startDetailActivity(id);
        }
    }

    private void selectDetailItem(long id) {
        // In two-pane mode, show the detail view in this activity by
        // adding or replacing the detail fragment using a
        // fragment transaction.
        Bundle arguments = new Bundle();
        arguments.putLong(VideoDetailFragment.ARG_ITEM_ID, id);
        VideoDetailFragment fragment = new VideoDetailFragment();
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.video_detail_container, fragment)
                .commit();
    }

    void startDetailActivity(long id) {
        Intent detailIntent = new Intent(this, VideoDetailActivity.class);
        detailIntent.putExtra(VideoDetailFragment.ARG_ITEM_ID, id);
        startActivity(detailIntent);
    }
}
