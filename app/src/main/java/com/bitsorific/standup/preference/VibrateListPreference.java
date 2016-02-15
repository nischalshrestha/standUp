package com.bitsorific.standup.preference;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.bitsorific.standup.activity.SettingsActivity;

/**
 * Custom ListPreference class for adding an OK button to confirm
 * user selection and perview selection while on the AlertDialog.
 *
 * Created by nischal on 2/15/16.
 */
public class VibrateListPreference extends ListPreference {

    // Constants for vibrate duration
    private final int SLOW_SPEED = 800;
    private final int NORMAL_SPEED = 500;
    private final int FAST_SPEED = 100;
    private final int VERY_FAST_SPEED = 50;

    // For grabbing standing/sitting number of pulses
    private String key;
    private SharedPreferences prefs;

    // Runnable vars for vibrating
    private Handler mhandler = new Handler();
    private Boolean isRunning = false;
    private int numPulses;
    private int speedPulse;
    private Vibrator v;

    private int mClickedDialogEntryIndex;
    private int prevDialogEntryIndex;

    /**
     * Vibrate x number of times based on given duration of pulse (speed)
     */
    private Runnable vibrateAlert = new Runnable() {
        int count = 0;
        @Override
        public void run() {
            isRunning = true;
            if (++count <= numPulses) {
                v.vibrate(speedPulse);
                // 100ms needs to be added for proper distance btw pulses
                mhandler.postDelayed(this, speedPulse+100);
            } else{
                count = 0;
                isRunning = false;
            }
        }
    };

    /**
     * Constructor which in addition to setting up the ListPreference, initializes the Vibrator and
     * SharedPreferences for previewing the speed of pulse
     * @param context
     * @param attrs
     */
    public VibrateListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        key = getKey();
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        getNumPulses();

    }

    /**
     * Grab the settings for the respective (sitting/standing) number of pulses for accurate preview
     * when calling {@link #previewVibe(int)}
     */
    private void getNumPulses(){
        if(key.equals(SettingsActivity.KEY_PREF_PULSE_SPEED)){
            numPulses = (prefs.getInt(SettingsActivity.KEY_PREF_PULSE_NUM,
                    SettingsActivity.PULSE_NUM_DEFAULT_VALUE) * 1) + 1;
        } else{
            numPulses = (prefs.getInt(SettingsActivity.KEY_PREF_PULSE_NUM_SIT,
                    SettingsActivity.PULSE_NUM_DEFAULT_VALUE_SIT) * 1) + 1;
        }
    }


    private int getValueIndex() {
        return findIndexOfValue(this.getValue() + "");
    }

    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        prevDialogEntryIndex = getValueIndex();
        mClickedDialogEntryIndex = getValueIndex();
        builder.setSingleChoiceItems(this.getEntries(), mClickedDialogEntryIndex, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                previewVibe(which);
                mClickedDialogEntryIndex = which;
            }
        });

        builder.setPositiveButton("OK", this);
    }

    /**
     * Preview the user selected value while on the dialog given the index of selection
     * @param which Index of the selection
     */
    private void previewVibe(int which){
        // Get num of pulses again in case settings have changed while on the Preference activity
        getNumPulses();
        switch(which){
            case 0:
                speedPulse = SLOW_SPEED;
                break;
            case 1:
                speedPulse = NORMAL_SPEED;
                break;
            case 2:
                speedPulse = FAST_SPEED;
                break;
            case 3:
                speedPulse = VERY_FAST_SPEED;
                break;
            default:
                break;
        }
        // Vibrate if it's not already running
        if(!isRunning){
            vibrateAlert.run();
        }
    }

    public  void onClick (DialogInterface dialog, int which) {
        // Cancel: which = -2;
        // OK: which = -1;

        // Set value only if the user clicked OK
        if(which == -2){
            this.setValue(this.getEntryValues()[prevDialogEntryIndex]+"");
        }
        else {
            this.setValue(this.getEntryValues()[mClickedDialogEntryIndex]+"");
        }
    }


}
