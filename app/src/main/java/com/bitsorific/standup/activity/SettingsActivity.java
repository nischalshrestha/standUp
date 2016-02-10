package com.bitsorific.standup.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.bitsorific.standup.R;
import com.bitsorific.standup.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_SITTING_PERIOD = "sitting_time_preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }
}
