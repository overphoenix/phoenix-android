package tech.nagual.common.ui.simpledialogs.form;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import tech.nagual.common.R;

/**
 * The ViewHolder class for {@link Spinner}
 * 
 * This class is used to create a SpinnerView and to maintain it's functionality
 * 
 * Created by eltos on 23.02.17.
 */

@SuppressWarnings("WeakerAccess")
class SpinnerViewHolder extends FormElementViewHolder<Spinner> {

    public static final int NONE = -1;

    protected static final String SAVED_POSITION = "pos";
    private CustomSpinnerView spinner;
    private TextView label;
    private CustomSpinnerAdapter adapter;

    SpinnerViewHolder(Spinner field) {
        super(field);
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.simpledialogfragment_form_item_spinner;
    }

    @Override
    protected void setUpView(View view, Context context, Bundle savedInstanceState,
                             final SimpleFormDialog.DialogActions actions) {

        spinner = view.findViewById(R.id.spinner);
        label = (TextView) view.findViewById(R.id.label);

        // Label
        String text = field.getText(context);
        label.setText(text);
        label.setVisibility(text == null ? View.GONE : View.VISIBLE);

        // Items
        String[] items = field.getItems(context);
        String placeholder = field.getPlaceholderText(context);
        if (items != null) {
            adapter = new CustomSpinnerAdapter(context, items, !field.required,
                    placeholder != null ? placeholder : text != null ? text : "");
            spinner.setAdapter(adapter);

            int preset = field.position >= 0 && field.position < items.length ? field.position : NONE;
            setSelection(preset);
        }

        if (savedInstanceState != null) {
            setSelection(savedInstanceState.getInt(SAVED_POSITION));
        }

        // Select listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actions.continueWithNextElement(false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinner.setSpinnerEventsListener(new CustomSpinnerView.OnSpinnerOpenListener() {
            @Override
            public void onOpen() {
                if (getSelection() == NONE && field.required) {
                    setSelection(0);
                }
            }
        });


    }




    @Override
    protected void saveState(Bundle outState) {
        outState.putInt(SAVED_POSITION, getSelection());
    }

    @Override
    protected void putResults(Bundle results, String key) {
        results.putInt(key, getSelection());
    }

    @Override
    protected boolean focus(SimpleFormDialog.FocusActions actions) {
        actions.hideKeyboard();
        actions.clearCurrentFocus();
        spinner.performClick();
        return spinner.requestFocus();
    }

    @Override
    protected boolean posButtonEnabled(Context context) {
        return !field.required || getSelection() != NONE;
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




    private int getSelection(){
        return adapter.mapFromSelection(spinner.getSelectedItemPosition());
    }

    private void setSelection(int index){
        spinner.setSelection(adapter.mapToSelection(index), false);
    }

    private static class CustomSpinnerAdapter extends ArrayAdapter<String> {

        private int emptyIndex;
        private boolean allowEmpty;

        int mapToSelection(int index){
            return index == NONE ? emptyIndex : index < emptyIndex ? index : index + 1;
        }
        int mapFromSelection(int index){
            return index == emptyIndex ? NONE : index < emptyIndex ? index : index - 1;
        }

        CustomSpinnerAdapter(Context context, String[] items, boolean allowEmpty, String emptyString){
            super(context, android.R.layout.simple_spinner_dropdown_item);
            this.allowEmpty = allowEmpty;
            if (allowEmpty) {
                emptyIndex = 0;
                add(emptyString);
                addAll(items);
            } else {
                addAll(items);
                add(emptyString);
                emptyIndex = items.length;
            }
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (position == emptyIndex) {
                TextView label = (TextView) view.findViewById(android.R.id.text1);
                label.setText("");
                label.setHint(getItem(position));
            }
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            View view =  super.getDropDownView(position, convertView, parent);
            if (position == emptyIndex) {
                TextView label = (TextView) view.findViewById(android.R.id.text1);
                label.setText("");
                label.setHint(getItem(position));
            }
            return view;
        }

        @Override
        public int getCount() {
            return super.getCount() - (allowEmpty ? 0 : 1);
        }

    }


}
