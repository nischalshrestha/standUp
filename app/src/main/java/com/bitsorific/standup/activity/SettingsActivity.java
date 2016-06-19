package com.bitsorific.standup.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.bitsorific.standup.R;
import com.bitsorific.standup.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    /** Sound **/
    public static final String KEY_PREF_ALARM_TONE_STAND = "pref_key_alarm_tone_stand";
    public static final String KEY_PREF_ALARM_TONE_SIT = "pref_key_alarm_tone_sit";
    public static final String KEY_PREF_SOUND = "pref_key_sound";
    // Default values
    public static final Boolean PREF_SOUND_DEFAULT = false;

    /** Vibrate **/
    // Amount
    public static final String KEY_PREF_PULSE_NUM = "pref_pulse_number_stand";
    public static final String KEY_PREF_PULSE_NUM_SIT = "pref_pulse_number_sit";
    // Speed
    public static final String KEY_PREF_PULSE_SPEED = "pref_pulse_speed_stand";
    public static final String KEY_PREF_PULSE_SPEED_SIT = "pref_pulse_speed_sit";
    // Default values
    public static final int PULSE_NUM_DEFAULT_VALUE = 0;
    public static final int PULSE_NUM_DEFAULT_VALUE_SIT = 0;
    public static final String PULSE_SPEED_DEFAULT_VALUE = "800";

    /** Timer **/
    public static final String KEY_PREF_SITTING_PERIOD = "sitting_period_preference";
    public static final String KEY_PREF_STANDING_PERIOD = "standing_period_preference";
    // The way NumberPicker works forces us to pick 0 as the default instead of simply any other min
    // since it refers to the position in the scrolling view; > 0 causes an error when scrolling
    public static final int SITTING_DEFAULT_VALUE = 0;
    public static final String STANDING_DEFAULT_VALUE = "5";

    // Multiply index selection of numberpicker for sitting period so you get multiples of 5 min
    public static final int SITTING_MULTIPLE = 5;
    // Minimum period for sitting
    public static final int SITTING_MIN = 15;

    /** Notifications **/
    public static final String KEY_NOTIFICATIONS = "notification_preference";
    public static final boolean PREF_NOTIFICATION_DEFAULT = true;

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

    /**
     * Handles actions on the ActionBar
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }


}
