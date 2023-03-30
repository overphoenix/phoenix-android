package tech.nagual.common.ui.simpledialogs.input;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.textfield.TextInputLayout;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import tech.nagual.common.R;
import tech.nagual.common.ui.simpledialogs.CustomViewDialog;

/**
 * An simple dialog with an input field. Supports suggestions, input validations and
 * max length options.
 *
 * Results:
 *      TEXT    String      The entered text
 *
 * Created by eltos on 14.10.2015.
 */
public class SimpleInputDialog extends CustomViewDialog<SimpleInputDialog> {

    public static final String TAG = "SimpleInputDialog.";

    public static final String
            TEXT = TAG + "text";


    public static SimpleInputDialog build(){
        return new SimpleInputDialog();
    }


    /**
     * Sets the EditText's hint
     *
     * @param hint the hint as string
     * @return this instance
     */
    public SimpleInputDialog hint(String hint){ return setArg(HINT, hint); }

    /**
     * Sets the EditText's hint
     *
     * @param hintResourceId the hint as android string resource
     * @return this instance
     */
    public SimpleInputDialog hint(@StringRes int hintResourceId){ return setArg(HINT, hintResourceId); }

    /**
     * Sets the EditText's initial text
     *
     * @param text initial text as string
     * @return this instance
     */
    public SimpleInputDialog text(String text){ return setArg(TEXT, text); }

    /**
     * Sets the EditText's initial text
     *
     * @param textResourceId initial text as android string resource
     * @return this instance
     */
    public SimpleInputDialog text(@StringRes int textResourceId){ return setArg(TEXT, textResourceId); }

    /**
     * Sets the input type
     * The default is {@link InputType#TYPE_CLASS_TEXT}.
     *
     * @param inputType the InputType
     * @return this instance
     */
    public SimpleInputDialog inputType(int inputType){ return setArg(INPUT_TYPE, inputType); }

    /**
     * Allow empty input. Default is to disable the positive button until text is entered.
     *
     * @param allow whether to allow empty input
     * @return this instance
     */
    public SimpleInputDialog allowEmpty(boolean allow){ return setArg(ALLOW_EMPTY, allow); }

    /**
     * Sets a max limit to the EditText.
     *
     * @param maxLength the maximum text length
     * @return this instance
     */
    public SimpleInputDialog max(int maxLength){ return setArg(MAX_LENGTH, maxLength); }

    /**
     * Provide an array of suggestions to be shown while the user is typing
     *
     * @param context a context to resolve the resource ids
     * @param stringResourceIds suggestion array as android string resources
     * @return this instance
     */
    public SimpleInputDialog suggest(Context context, int[] stringResourceIds){
        String[] strings = new String[stringResourceIds.length];
        for (int i = 0; i < stringResourceIds.length; i++) {
            strings[i] = context.getString(stringResourceIds[i]);
        }
        return suggest(strings);
    }

    /**
     * Provide an array of suggestions to be shown while the user is typing
     *
     * @param strings suggestion string array
     * @return this instance
     */
    public SimpleInputDialog suggest(String[] strings){
        getArgs().putStringArray(SUGGESTIONS, strings);
        return this;
    }




    public interface InputValidator {
        /**
         * Let the hosting fragment or activity implement this interface to control
         * when a user can proceed or to display an error message on an invalid input.
         * The method is called every time the user hits the positive button
         *
         * @param dialogTag the tag of this fragment
         * @param input the text entered by the user
         * @param extras the extras passed with {@link SimpleInputDialog#extra(Bundle)}
         * @return an error message to display or null if the input is valid
         */
        @Nullable String validate(String dialogTag, @Nullable String input, @NonNull Bundle extras);
    }




    protected static final String
            HINT = TAG + "hint",
            INPUT_TYPE = TAG + "input_type",
            ALLOW_EMPTY = TAG + "allow_empty",
            MAX_LENGTH = TAG + "max_length",
            SUGGESTIONS = TAG + "suggestions";

    private AutoCompleteTextView mInput;
    private TextInputLayout mInputLayout;


    protected @Nullable String onValidateInput(@Nullable String input){
        Bundle extras = getExtras();
        if (getTargetFragment() instanceof InputValidator) {
            return ((InputValidator) getTargetFragment())
                    .validate(getTag(), input, extras);
        }
        if (getActivity() instanceof InputValidator) {
            return ((InputValidator) getActivity())
                    .validate(getTag(), input, extras);
        }
        return null;
    }



    /**
     * @return the current text or null
     */
    @Nullable
    public String getText(){
        return mInput.getText() != null ? mInput.getText().toString() : null;
    }

    public boolean isInputEmpty(){
        return getText() == null || getText().trim().isEmpty();
    }

    /**
     * Helper for opening the soft keyboard
     */
    public void openKeyboard(){
        showKeyboard(mInput);
    }

    @Override
    public View onCreateContentView(Bundle savedInstanceState) {
        // inflate and set your custom view here
        View view = inflate(R.layout.simpledialogfragment_input);
        mInput = (AutoCompleteTextView) view.findViewById(R.id.editText);
        mInputLayout = (TextInputLayout) view.findViewById(R.id.inputLayout);

        // Note: setting TYPE_CLASS_TEXT as default is very important!
        mInput.setInputType(getArgs().getInt(INPUT_TYPE, InputType.TYPE_CLASS_TEXT));
        if ((getArgs().getInt(INPUT_TYPE) & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_PHONE) {
            // format phone number automatically
            mInput.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        }
        //mInput.setHint(getArgString(HINT));
        mInputLayout.setHint(getArgString(HINT));
        if (getArgs().getInt(MAX_LENGTH) > 0) {
            mInputLayout.setCounterMaxLength(getArgs().getInt(MAX_LENGTH));
            mInputLayout.setCounterEnabled(true);
        }


        if (savedInstanceState != null) {
            mInput.setText(savedInstanceState.getString(TEXT));
        } else {
            mInput.setText(getArgString(TEXT));
            mInput.setSelection(0, mInput.length());
        }

        mInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    pressPositiveButton();
                    return true;
                }
                return false;
            }
        });

        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                setPositiveButtonEnabled(posEnabled());
            }
        });

        // Auto complete
        String[] suggestionList = getArgs().getStringArray(SUGGESTIONS);
        if (suggestionList != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    // android.R.layout.simple_dropdown_item_1line
                    android.R.layout.simple_list_item_1, suggestionList);
            mInput.setAdapter(adapter);
            mInput.setThreshold(1);
        }

        return view;
    }

    protected boolean posEnabled(){
        return (!isInputEmpty() || getArgs().getBoolean(ALLOW_EMPTY)) && (getText() == null
                || getText().length() <= getArgs().getInt(MAX_LENGTH, getText().length()));
    }



    @Override
    protected void onDialogShown() {
        setPositiveButtonEnabled(posEnabled());
        showKeyboard(mInput);
    }

    @Override
    protected boolean acceptsPositiveButtonPress() {
        String input = getText();
        String error = onValidateInput(input);
        if (error == null) {
            return true;
        } else {
            mInputLayout.setError(error);
            mInputLayout.setErrorEnabled(true);
            return false;
        }
    }


    @Override
    public Bundle onResult(int which) {
        Bundle result = new Bundle();
        result.putString(TEXT, getText());
        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TEXT, getText());
    }
}
