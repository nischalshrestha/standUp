package com.bitsorific.standup.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

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
        if (key.equals(SettingsActivity.KEY_PREF_SITTING_PERIOD)) {
//             = sharedPreferences.getString(key, "");
            EditTextPreference editTextPref = (EditTextPreference) findPreference(key);
            String sittingPref = editTextPref.getText();
            Log.d("Pref", "Sitting period is: " + TextUtils.isDigitsOnly(sittingPref)+" : "+getActivity().getApplicationContext());
            // Set summary to be the user-description for the selected value
            if(sittingPref.equals("")) {
                Toast.makeText(getActivity().getApplicationContext(), "Please enter an integer!", Toast.LENGTH_LONG);
            }
            try{
                Integer.parseInt(sittingPref);
            } catch (NumberFormatException e){
                Toast.makeText(getActivity(), "Please enter an integer!", Toast.LENGTH_LONG);
            }
        }
    }
}
