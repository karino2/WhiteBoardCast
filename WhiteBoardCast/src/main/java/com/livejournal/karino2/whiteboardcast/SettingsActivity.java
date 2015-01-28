package com.livejournal.karino2.whiteboardcast;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by karino on 1/29/15.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }

}
