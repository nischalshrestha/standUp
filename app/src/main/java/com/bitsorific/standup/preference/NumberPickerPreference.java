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

    private static final int MIN_SITTING_PERIOD = 1;
    private static final int MAX_SITTING_PERIOD = 60;

    private int mCurrentValue;
    private int mNewValue;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.numberpicker_dialog);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedInt(SettingsActivity.SITTING_DEFAULT_VALUE);
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
        np.setMaxValue(MAX_SITTING_PERIOD);
        np.setMinValue(MIN_SITTING_PERIOD);
        np.setValue(this.getPersistedInt(SettingsActivity.SITTING_DEFAULT_VALUE));
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
        return a.getInteger(index, SettingsActivity.SITTING_DEFAULT_VALUE);
    }
}
