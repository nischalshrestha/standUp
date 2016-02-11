package com.bitsorific.standup.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bitsorific.standup.R;
import com.bitsorific.standup.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    // Sound key constants
    public static final String KEY_PREF_ALARM_TONE_STAND = "pref_key_alarm_tone_stand";
    public static final String KEY_PREF_ALARM_TONE_SIT = "pref_key_alarm_tone_sit";
    public static final String KEY_PREF_SOUND = "pref_key_sound";
    public static final Boolean PREF_SOUND_DEFAULT = false;

    // Timer key constants
    public static final String KEY_PREF_SITTING_PERIOD = "sitting_period_preference";
    public static final String KEY_PREF_STANDING_PERIOD = "standing_period_preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Give the Activity a distinct color
        ActionBar ab = getSupportActionBar();
        ColorDrawable colorDrawable = new ColorDrawable(getResources().getColor(R.color.colorPrimaryDark));
        ab.setBackgroundDrawable(colorDrawable);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }
}
