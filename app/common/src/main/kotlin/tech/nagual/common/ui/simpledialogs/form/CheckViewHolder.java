package tech.nagual.common.ui.simpledialogs.form;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import tech.nagual.common.R;

/**
 * The ViewHolder class for {@link Check}
 * 
 * This class is used to create a CheckBox and to maintain it's functionality
 * 
 * Created by eltos on 23.02.17.
 */

class CheckViewHolder extends FormElementViewHolder<Check> {

    protected static final String SAVED_CHECK_STATE = "checked";
    private CheckBox checkBox;

    public CheckViewHolder(Check field) {
        super(field);
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.simpledialogfragment_form_item_check;
    }

    @Override
    protected void setUpView(View view, Context context, Bundle savedInstanceState,
                             final SimpleFormDialog.DialogActions actions) {

        checkBox = (CheckBox) view.findViewById(R.id.checkBox);

        // Label
        checkBox.setText(field.getText(context));

        // Check preset
        if (savedInstanceState != null) {
            checkBox.setChecked(savedInstanceState.getBoolean(SAVED_CHECK_STATE));
        } else {
            checkBox.setChecked(field.getInitialState(context));
        }

        // Positive button state for single element forms
        if (actions.isOnlyFocusableElement()) {
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    actions.updatePosButtonState();
                }
            });
        }

    }


    @Override
    protected void saveState(Bundle outState) {
        outState.putBoolean(SAVED_CHECK_STATE, checkBox.isChecked());
    }


    @Override
    protected void putResults(Bundle results, String key) {
        results.putBoolean(key, checkBox.isChecked());
    }


    @Override
    protected boolean focus(final SimpleFormDialog.FocusActions actions) {
        actions.hideKeyboard();

        // Temporary make this view focusable, so that we can scroll to it
        // and show a short flash of the box getting focused
        checkBox.setFocusableInTouchMode(true);

        checkBox.postDelayed(new Runnable() {
            @Override
            public void run() {
                // finally remove focus
                checkBox.setFocusable(false);
                actions.clearCurrentFocus();
            }
        }, 100);

        return checkBox.requestFocus();
    }


    @Override
    protected boolean posButtonEnabled(Context context) {
        return !field.required || checkBox.isChecked();
    }


    @Override
    protected boolean validate(Context context) {
        boolean valid = posButtonEnabled(context);
        if (valid) {
            TypedValue value = new TypedValue();
            checkBox.getContext().getTheme().resolveAttribute(android.R.attr.checkboxStyle, value, true);
            int[] attr = new int[] {android.R.attr.textColor};
            TypedArray a = context.obtainStyledAttributes(value.data, attr);
            checkBox.setTextColor(a.getColor(0, Color.BLACK));
            a.recycle();

        } else {
            //noinspection deprecation
            checkBox.setTextColor(context.getResources().getColor(R.color.simpledialogfragment_error_color));
        }
//        checkBox.setError(valid ? null : context.getString(R.string.required));
        return valid;
    }
}
