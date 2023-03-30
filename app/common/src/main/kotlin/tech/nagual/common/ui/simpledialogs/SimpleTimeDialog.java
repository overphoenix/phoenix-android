package tech.nagual.common.ui.simpledialogs;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

/**
 * A time-picker dialog
 * <p>
 * Created by eltos on 02.02.2017.
 */
public class SimpleTimeDialog extends CustomViewDialog<SimpleTimeDialog>
        implements TimePicker.OnTimeChangedListener {

    public static final String TAG = "SimpleTimeDialog.";

    public static final String
            HOUR = TAG + "HOUR",
            MINUTE = TAG + "MINUTE";


    public static SimpleTimeDialog build() {
        return new SimpleTimeDialog();
    }


    /**
     * Specify the initially set hour
     *
     * @param hour initial hour (0-23)
     * @return this instance
     */
    public SimpleTimeDialog hour(int hour) {
        return setArg(HOUR, hour);
    }

    /**
     * Specify the initially set minute
     *
     * @param minute initial minute (0-59)
     * @return this instance
     */
    public SimpleTimeDialog minute(int minute) {
        return setArg(MINUTE, minute);
    }

    /**
     * Changes the hour display mode between 24 and AM/PM
     *
     * @param view24Hour true to use 24 hour mode, false to use AM/PM
     * @return this instance
     */
    public SimpleTimeDialog set24HourView(boolean view24Hour) {
        return setArg(VIEW_24_HOUR, view24Hour);
    }


    protected static final String
            VIEW_24_HOUR = TAG + "VIEW_24_HOUR";

    private TimePicker picker;


    @SuppressWarnings("deprecation")
    @Override
    protected View onCreateContentView(Bundle savedInstanceState) {

        picker = new TimePicker(getContext());

        if (savedInstanceState != null) {
            picker.setCurrentHour(savedInstanceState.getInt(HOUR));
            picker.setCurrentMinute(savedInstanceState.getInt(MINUTE));

        } else {
            if (getArgs().containsKey(HOUR)) {
                picker.setCurrentHour(getArgs().getInt(HOUR));
            }
            if (getArgs().containsKey(MINUTE)) {
                picker.setCurrentMinute(getArgs().getInt(MINUTE));
            }
        }

        if (getArgs().containsKey(VIEW_24_HOUR)) {
            picker.setIs24HourView(getArgs().getBoolean(VIEW_24_HOUR));
        } else {
            picker.setIs24HourView(DateFormat.is24HourFormat(getContext())); // system default
        }
        picker.setOnTimeChangedListener(this);

        return picker;
    }


    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        // currently not used
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(HOUR, picker.getCurrentHour());
        outState.putInt(MINUTE, picker.getCurrentMinute());
        super.onSaveInstanceState(outState);
    }


    @SuppressWarnings("deprecation")
    @Override
    protected Bundle onResult(int which) {
        Bundle results = new Bundle();
        results.putInt(HOUR, picker.getCurrentHour());
        results.putInt(MINUTE, picker.getCurrentMinute());
        return results;
    }
}
