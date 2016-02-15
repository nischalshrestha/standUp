package com.bitsorific.standup.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import com.bitsorific.standup.R;
import com.bitsorific.standup.activity.SettingsActivity;

/**
 * Created by nischal on 2/10/16.
 */
public class NumberPickerPreference extends DialogPreference implements DialogInterface.OnClickListener{

    private static final int MIN_SITTING_PERIOD = 0;
    private static final int MAX_SITTING_PERIOD = 8;

    private static final int MIN_VIBRATE_NUM = 0;
    private static final int MAX_VIBRATE_NUM = 4;

    private String[] sittingValues = new String[MAX_SITTING_PERIOD - MIN_SITTING_PERIOD + 1];
    private String[] vibrateValues = new String[MAX_VIBRATE_NUM - MIN_VIBRATE_NUM + 1];

    private int mCurrentValue;
    private int mNewValue;

    private String key;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        key = getKey();

        // The number picker dialog for sitting period has a 'min'
        // while the vibrate does not
        if(key.equals(SettingsActivity.KEY_PREF_SITTING_PERIOD)) {
            setDialogLayoutResource(R.layout.numberpicker_dialog);
        } else {
            setDialogLayoutResource(R.layout.numberpicker_dialog_vibrate);
        }

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);

    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            if(key.equals(SettingsActivity.KEY_PREF_SITTING_PERIOD)) {
                mCurrentValue = this.getPersistedInt(SettingsActivity.SITTING_DEFAULT_VALUE);
            } else {
                mCurrentValue = this.getPersistedInt(SettingsActivity.PULSE_NUM_DEFAULT_VALUE);
            }
        } else {
            // Set default state from the XML attribute
            mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }
    }

    @Override
    protected View onCreateDialogView() {
        View dialogView =  super.onCreateDialogView();
        NumberPicker np = (NumberPicker) dialogView.findViewById(R.id.numberPicker);
        // Sitting period picker
        if(key.equals(SettingsActivity.KEY_PREF_SITTING_PERIOD)) {
            np.setMaxValue(MAX_SITTING_PERIOD);
            np.setMinValue(MIN_SITTING_PERIOD);
            np.setValue(this.getPersistedInt(SettingsActivity.SITTING_DEFAULT_VALUE));
            // To display 20-60 in multiples of 5
            for (int i = MIN_SITTING_PERIOD, j = 0; j < sittingValues.length; i++, j++) {
                sittingValues[j] = Integer.toString(i * 5 + 20);
                Log.d("Num", sittingValues[j]);
            }
            np.setDisplayedValues(sittingValues);
        } else{
            np.setMaxValue(MAX_VIBRATE_NUM);
            np.setMinValue(MIN_VIBRATE_NUM);
            np.setValue(this.getPersistedInt(SettingsActivity.PULSE_NUM_DEFAULT_VALUE));
            // To display 20-60 in multiples of 5
            for (int i = MIN_VIBRATE_NUM, j = 0; j < vibrateValues.length; i++, j++) {
                vibrateValues[j] = Integer.toString(i * 1 + 1);
                Log.d("Num", vibrateValues[j]);
            }
            np.setDisplayedValues(vibrateValues);
        }
        return dialogView;
    }

    @Override
    public void onClick(DialogInterface dialog, int which){
        if(which == DialogInterface.BUTTON_POSITIVE) {
            // User has
            NumberPicker picker = (NumberPicker) getDialog().findViewById(R.id.numberPicker);
            if(mCurrentValue != picker.getValue()) {
                mNewValue = picker.getValue();
                persistInt(mNewValue);
                mCurrentValue = mNewValue; // update current value
            }
        } else if(which == DialogInterface.BUTTON_NEGATIVE){
            // do your stuff to handle negative button
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        if(key.equals(SettingsActivity.KEY_PREF_PULSE_NUM)){
            return a.getInteger(index, SettingsActivity.PULSE_NUM_DEFAULT_VALUE);
        }
        return a.getInteger(index, SettingsActivity.SITTING_DEFAULT_VALUE);
    }
}
