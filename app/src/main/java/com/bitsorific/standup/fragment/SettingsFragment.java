package com.bitsorific.standup.fragment;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;

import com.bitsorific.standup.R;
import com.bitsorific.standup.activity.SettingsActivity;

/**
 * Created by nischal on 2/9/16.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    private Preference.OnPreferenceChangeListener soundChangeListener;
    private Preference standSoundPref;
    private Preference sitSoundPref;

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

        final String WARNING = getResources().getString(R.string.warning);

        // Since RingtonePreference opens a new Activity, this is another way to listen to the
        // changes so we can handle the case of the tone not being the right format, and warn
        // the user!
        soundChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // Try to create a MediaPlayer, and if it's null it failed so warn user with dialog
                MediaPlayer mpAlarm = MediaPlayer.create(getActivity(), Uri.parse(newValue.toString()));
                if (mpAlarm == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(WARNING)
                            .setNegativeButton("Got it!", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // dismiss the dialog and return to activity
                                    dialog.dismiss();
                                }
                            });
                    // Create the AlertDialog object and return it
                    builder.create().show();
                } else{
                    mpAlarm.release();
                }
                return true;
            }
        };

        // Set the Stand up alarm's listener
        standSoundPref = findPreference(SettingsActivity.KEY_PREF_ALARM_TONE_STAND);
        standSoundPref.setOnPreferenceChangeListener(soundChangeListener);

        // Set the Sit down alarm's listener
        sitSoundPref = findPreference(SettingsActivity.KEY_PREF_ALARM_TONE_SIT);
        sitSoundPref.setOnPreferenceChangeListener(soundChangeListener);
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
        if(key.equals(SettingsActivity.KEY_PREF_STANDING_PERIOD)){
            String val = sharedPreferences.getString(key, SettingsActivity.STANDING_DEFAULT_VALUE);
            if(val.equals("")) {
                sharedPreferences.edit().putString(key, SettingsActivity.STANDING_DEFAULT_VALUE).commit();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Prevent leaks
        standSoundPref.setOnPreferenceChangeListener(null);
        sitSoundPref.setOnPreferenceChangeListener(null);
        soundChangeListener = null;
    }
}
