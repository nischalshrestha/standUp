package com.bitsorific.standup.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.bitsorific.standup.R;
import com.bitsorific.standup.activity.SettingsActivity;

/**
 * Created by nischal on 2/9/16.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Handle input
        if (key.equals(SettingsActivity.KEY_PREF_SITTING_PERIOD)) {
//            Log.d("Pref", "val: "+sharedPreferences.getInt(SettingsActivity.KEY_PREF_SITTING_PERIOD, 20));

        }
    }
}
