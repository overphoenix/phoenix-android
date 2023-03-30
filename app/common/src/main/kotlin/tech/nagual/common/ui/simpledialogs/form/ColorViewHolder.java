package tech.nagual.common.ui.simpledialogs.form;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import tech.nagual.common.ui.simpledialogs.color.ColorView;
import tech.nagual.common.ui.simpledialogs.color.SimpleColorDialog;
import tech.nagual.common.R;
import tech.nagual.common.ui.simpledialogs.SimpleDialog;
import tech.nagual.common.ui.simpledialogs.color.ColorView;
import tech.nagual.common.ui.simpledialogs.color.SimpleColorDialog;

/**
 * The ViewHolder class for {@link ColorField}
 * 
 * This class is used to create a Color Box and to maintain it's functionality
 * 
 * Created by eltos on 06.07.2018.
 */

class ColorViewHolder extends FormElementViewHolder<ColorField> implements SimpleDialog.OnDialogResultListener {

    protected static final String SAVED_COLOR = "color";
    private static final String COLOR_DIALOG_TAG = "colorPickerDialogTag";
    private TextView label;
    private ColorView colorView;

    public ColorViewHolder(ColorField field) {
        super(field);
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.simpledialogfragment_form_item_color;
    }

    @Override
    protected void setUpView(View view, final Context context, Bundle savedInstanceState,
                             final SimpleFormDialog.DialogActions actions) {

        label = (TextView) view.findViewById(R.id.label);
        colorView = (ColorView) view.findViewById(R.id.color);

        // Label
        String text = field.getText(context);
        label.setText(text);
        label.setVisibility(text == null ? View.GONE : View.VISIBLE);
        label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorView.performClick();
            }
        });

        // Color preset
        if (savedInstanceState != null) {
            colorView.setColor(savedInstanceState.getInt(SAVED_COLOR));
        } else {
            colorView.setColor(field.getInitialColor(context));
        }
        colorView.setOutlineWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2 /*dp*/, context.getResources().getDisplayMetrics()));
        colorView.setOutlineColor(field.outline);
        colorView.setStyle(ColorView.Style.PALETTE);
        colorView.setChecked(true);


        colorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions.showDialog(SimpleColorDialog.build()
                        .title(field.getText(context))
                        .colors(field.colors)
                        .allowCustom(field.allowCustom)
                        .colorPreset(colorView.getColor())
                        .neut(),
                        COLOR_DIALOG_TAG+field.resultKey);
            }
        });

    }


    @Override
    protected void saveState(Bundle outState) {
        outState.putInt(SAVED_COLOR, colorView.getColor());
    }


    @Override
    protected void putResults(Bundle results, String key) {
        results.putInt(key, colorView.getColor());
    }


    @Override
    protected boolean focus(final SimpleFormDialog.FocusActions actions) {
        actions.hideKeyboard();
        //colorView.performClick();
        return colorView.requestFocus();

    }


    @Override
    protected boolean posButtonEnabled(Context context) {
        return !field.required || colorView.getColor() != ColorField.NONE;
    }


    @Override
    protected boolean validate(Context context) {
        boolean valid = posButtonEnabled(context);
        if (valid) {
            TypedValue value = new TypedValue();
            if (label.getContext().getTheme().resolveAttribute(android.R.attr.textColor, value, true)) {
                label.setTextColor(value.data);
            } else {
                label.setTextColor(0x8a000000);
            }
        } else {
            //noinspection deprecation
            label.setTextColor(context.getResources().getColor(R.color.simpledialogfragment_error_color));
        }
        return valid;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if ((COLOR_DIALOG_TAG+field.resultKey).equals(dialogTag)){
            if (which == BUTTON_POSITIVE && colorView != null){
                colorView.setColor(extras.getInt(SimpleColorDialog.COLOR, colorView.getColor()));
            }
            return true;
        }
        return false;
    }
}
