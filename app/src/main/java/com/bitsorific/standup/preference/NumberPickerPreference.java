package com.bitsorific.standup.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import com.bitsorific.standup.R;
import com.bitsorific.standup.activity.SettingsActivity;

/**
 * Created by nischal on 2/10/16.
 */
public class NumberPickerPreference extends DialogPreference implements DialogInterface.OnClickListener{

    public static final int SITTING_DEFAULT_VALUE = 20;
    public static final int STANDING_DEFAULT_VALUE = 5;

    private static final int MIN_SITTING_PERIOD = 20;
    private static final int MIN_STANDING_PERIOD = 5;

    private static final int MAX_SITTING_PERIOD = 60;
    private static final int MAX_STANDING_PERIOD = 60;

    private String key;
    private int mCurrentValue;
    private int mNewValue;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.numberpicker_dialog);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);

        key = getKey().toString();
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedInt(SITTING_DEFAULT_VALUE);
            if(key.equals(SettingsActivity.KEY_PREF_STANDING_PERIOD)){
                mCurrentValue = this.getPersistedInt(STANDING_DEFAULT_VALUE);
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
        if(key.equals(SettingsActivity.KEY_PREF_SITTING_PERIOD)){
            np.setMaxValue(MAX_SITTING_PERIOD);
            np.setMinValue(MIN_SITTING_PERIOD);
            np.setValue(this.getPersistedInt(SITTING_DEFAULT_VALUE));
        } else{
            np.setMaxValue(MAX_STANDING_PERIOD);
            np.setMinValue(MIN_STANDING_PERIOD);
            np.setValue(this.getPersistedInt(STANDING_DEFAULT_VALUE));
        }
        // set the default values to the view
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
        if(key.equals(SettingsActivity.KEY_PREF_SITTING_PERIOD)){
            return a.getInteger(index, SITTING_DEFAULT_VALUE);
        }
        return a.getInteger(index, STANDING_DEFAULT_VALUE);
    }
}
