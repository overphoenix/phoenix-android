package tech.nagual.common.ui.simpledialogs.form;

import android.content.Context;
import android.os.Parcel;
import androidx.annotation.ArrayRes;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.text.InputType;

import java.util.ArrayList;
import java.util.regex.Pattern;

import tech.nagual.common.R;
import tech.nagual.common.ui.simpledialogs.SimpleDialog.OnDialogResultListener;

/**
 * An input form element to be used with {@link SimpleFormDialog}
 * 
 * This is an EditText that can be used to enter text, email-addresses, numbers, passwords etc.
 * Optionally supports auto-complete behaviour using suggestions.
 * 
 * This will add a String to resource bundle containing the entered text.
 * 
 * Created by eltos on 20.02.17.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class Input extends FormElement<Input, InputViewHolder> {

    private String hint = null;
    private int hintResourceId = NO_ID;
    private String text = null;
    private int textResourceId = NO_ID;
    int inputType = InputType.TYPE_CLASS_TEXT;
    int maxLength = -1;
    int minLength = -1;
    private int suggestionArrayRes = NO_ID;
    private int[] suggestionStringResArray = null;
    private String[] suggestions = null;
    boolean passwordToggleVisible;
    boolean forceSuggestion = false;
    boolean isSpinner = false;
    String pattern = null;
    private String patternError = null;
    private int patternErrorId = NO_ID;
    private Pattern compiledPattern = null;


    private Input(String key){
        super(key);
    }


    /**
     * Factory method for a plain input field.
     *
     * @param key the key that can be used to receive the entered text from the bundle in
     *            {@link OnDialogResultListener#onResult}
     * @return this instance
     */
    public static Input plain(String key){
        return new Input(key);
    }

    /**
     * Factory method for a name input field.
     * InputType and hint are preset.
     *
     * @param key the key that can be used to receive the entered text from the bundle in
     *            {@link OnDialogResultListener#onResult}
     * @return this instance
     */
    public static Input name(String key){
        return new Input(key)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .hint(R.string.dialogs_name);
    }

    /**
     * Factory method for a password input field.
     * InputType and hint are preset.
     * Shows a button to toggle the passwords visibility
     *
     * @param key the key that can be used to receive the entered text from the bundle in
     *            {@link OnDialogResultListener#onResult}
     * @return this instance
     */
    public static Input password(String key){
        return new Input(key)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .showPasswordToggle()
                .hint(R.string.dialogs_password);
    }

    /**
     * Factory method for a pin input field.
     * InputType and hint are preset.
     *
     * @param key the key that can be used to receive the entered text from the bundle in
     *            {@link OnDialogResultListener#onResult}
     * @return this instance
     */
    public static Input pin(String key){
        return new Input(key)
                .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                .hint(R.string.dialogs_pin);
    }

    /**
     * Factory method for an email input field.
     * InputType and hint are preset.
     * This field also validates, that an email matching the default pattern was entered.
     *
     * @param key the key that can be used to receive the entered text from the bundle in
     *            {@link OnDialogResultListener#onResult}
     * @return this instance
     */
    public static Input email(String key){
        return new Input(key)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .validatePatternEmail()
                .hint(R.string.dialogs_email_address);
    }

    /**
     * Factory method for a phone input field.
     * InputType and hint are preset.
     * This field also automatically format the phone number while the user is typing.
     *
     * @param key the key that can be used to receive the entered text from the bundle in
     *            {@link OnDialogResultListener#onResult}
     * @return this instance
     */
    public static Input phone(String key){
        return new Input(key)
                .inputType(InputType.TYPE_CLASS_PHONE)
                .hint(R.string.dialogs_phone_number);
    }

    /**
     * Factory method for a spinner input field.
     * This disables editing and only uses suggestions.
     *
     * Make sure to supply the items via {@link Input#suggest}
     *
     * @param key the key that can be used to receive the entered text from the bundle in
     *            {@link OnDialogResultListener#onResult}
     * @return this instance
     */
    public static Input spinner(String key){
        return new Input(key)
                .asSpinner(true)
                .forceSuggestion()
                .inputType(InputType.TYPE_NULL);
    }
    public static Input spinner(String key, @ArrayRes int suggestionArrayRes){
        return Input.spinner(key).suggest(suggestionArrayRes);
    }
    public static Input spinner(String key, @StringRes int... suggestionStringResArray){
        return Input.spinner(key).suggest(suggestionStringResArray);
    }
    public static Input spinner(String key, String... strings){
        return Input.spinner(key).suggest(strings);
    }
    public static Input spinner(String key, ArrayList<String> strings){
        return Input.spinner(key).suggest(strings);
    }








    /**
     * Sets a hint
     *
     * @param hint the hint as string
     * @return this instance
     */
    public Input hint(String hint){
        this.hint = hint;
        return this;
    }

    /**
     * Sets a hint
     *
     * @param hintResourceId the hint as android string resource
     * @return this instance
     */
    public Input hint(@StringRes int hintResourceId){
        this.hintResourceId = hintResourceId;
        return this;
    }

    /**
     * Sets the initial text
     *
     * @param text initial text as string
     * @return this instance
     */
    public Input text(String text){
        this.text = text;
        return this;
    }

    /**
     * Sets the initial text
     *
     * @param textResourceId initial text as android string resource
     * @return this instance
     */
    public Input text(@StringRes int textResourceId){
        this.textResourceId = textResourceId;
        return this;
    }

    /**
     * Sets the input type
     * The default is {@link InputType#TYPE_CLASS_TEXT}.
     *
     * @param inputType the input type. See {@link InputType}
     * @return this instance
     */
    public Input inputType(int inputType){
        this.inputType = inputType;
        return this;
    }

    /**
     * Displays a button to toggle the password visibility.
     * Note that this will only work if the input type is a password.
     * See {@link Input#showPasswordToggle(boolean)}
     *
     * @return this instance
     */
    public Input showPasswordToggle(){
        return showPasswordToggle(true);
    }

    /**
     * Hide or show a button to toggle the password visibility.
     * Note that this will only work if the input type is a password.
	 *
	 * @param show whether to show the password toggle button
     * @return this instance
     */
    public Input showPasswordToggle(boolean show){
        this.passwordToggleVisible = show;
        return this;
    }

    /**
     * Sets an upper limit to the input's text length.
     *
     * @param maxLength the maximum text length
     * @return this instance
     */
    public Input max(@IntRange(from=1) int maxLength){
        this.maxLength = maxLength;
        return this;
    }

    /**
     * Sets a lower limit to the input's text length.
     *
     * @param minLength the minimum text length
     * @return this instance
     */
    public Input min(@IntRange(from=1) int minLength){
        this.minLength = minLength;
        return this;
    }

    /**
     * Provide an array resource with suggestions to be shown while the user is typing.
     * This enables the auto-complete behaviour.
     *
     * @param suggestionArrayRes the string array resource to suggest
     * @return this instance
     */
    public Input suggest(@ArrayRes int suggestionArrayRes){
        this.suggestionArrayRes = suggestionArrayRes;
        return this;
    }

    /**
     * Provide an array of suggestions to be shown while the user is typing
     * This enables the auto-complete behaviour.
     *
     * @param suggestionStringResArray array of string resources to suggest
     * @return this instance
     */
    public Input suggest(@StringRes int... suggestionStringResArray){
        if (suggestionStringResArray != null && suggestionStringResArray.length > 0) {
            this.suggestionStringResArray = suggestionStringResArray;
        }
        return this;
    }

    /**
     * Provide an array of suggestions to be shown while the user is typing
     * This enables the auto-complete behaviour.
     *
     * @param strings array of strings to suggest
     * @return this instance
     */
    public Input suggest(String... strings){
        if (strings != null && strings.length > 0) {
            this.suggestions = strings;
        }
        return this;
    }

    /**
     * Provide an array of suggestions to be shown while the user is typing
     * This enables the auto-complete behaviour.
     *
     * @param strings An ArrayList of strings to suggest
     * @return this instance
     */
    public Input suggest(ArrayList<String> strings){
        if (strings != null && strings.size() > 0) {
            return suggest(strings.toArray(new String[strings.size()]));
        }
        return this;
    }

    /**
     * Shortcut for {@link Input#forceSuggestion(boolean)}
     *
     * @return this instance
     */
    public Input forceSuggestion(){
        return forceSuggestion(true);
    }

    /**
     * Specify whether this input may contain only one of the suggestions provided.
     * If enabled, the EditText will show an error message if something else was entered. This
     * will only take affect if suggestions were set by any of the {@link Input#suggest} methods
     * 
     * If the suggestion array is small, consider using a spinner instead.
     *
     * @param force whether to force the input to be one of the suggestions or not
     * @return this instance
     */
    public Input forceSuggestion(boolean force){
        this.forceSuggestion = force;
        return this;
    }

    /**
     * Make this Input field a spinner (not editable, drop down button)
     *
     * @param spinner whether to make this a spinner
     * @return this instance
     */
    public Input asSpinner(boolean spinner){
        this.isSpinner = spinner;
        return this;
    }

    /**
     * Validate input using the supplied regular expression pattern and display an error
     * message if the pattern does not match.
     *
     * @param pattern a regular expression used to validate input
     * @param errorMsg the error message to display, if the pattern does not match the input
     * @return this instance
     */
    public Input validatePattern(String pattern, @Nullable String errorMsg){
        this.pattern = pattern;
        this.patternError = errorMsg;
        return this;
    }

    /**
     * Validate input using the supplied regular expression pattern and display an error
     * message if the pattern does not match. See {@link Input#validatePattern(String, String)}
     *
     * @param pattern a regular expression used to validate input
     * @param errorMsgId the error message to display as string resource
     * @return this instance
     */
    public Input validatePattern(String pattern, @StringRes int errorMsgId){
        this.pattern = pattern;
        this.patternErrorId = errorMsgId;
        return this;
    }

    /**
     * Validate input as email address. Shortcut for {@link Input#validatePattern(String, int)}.
     *
     * @return this instance
     */
    public Input validatePatternEmail(){
        return validatePattern(EMAIL_PATTERN, R.string.dialogs_invalid_email_address);
    }

    /**
     * Validate input as password. The password must consists of at least 8 chars and contains
     * at least one number, one special character, one upper and one lower case letter
     * Shortcut for {@link Input#validatePattern(String, int)}.
     *
     * @return this instance
     */
    public Input validatePatternStrongPassword(){
        if (minLength < 8) min(8);
        return validatePattern(STRONG_PW_PATTERN, R.string.dialogs_strong_pw_requirements);
    }

    /**
     * Validate input so that only upper- and lowercase letters are contained.
     * Shortcut for {@link Input#validatePattern(String, int)}.
     *
     * @return this instance
     */
    public Input validatePatternLetters(){
        return validatePattern(LETTERS_PATTERN, R.string.dialogs_letters_only_error);
    }

    /**
     * Validate input to consist of alpha-numeric characters only.
     * Shortcut for {@link Input#validatePattern(String, int)}.
     *
     * @return this instance
     */
    public Input validatePatternAlphanumeric(){
        return validatePattern(ALPHANUMERIC_PATTERN, R.string.dialogs_alphanumeric_only_error);
    }



    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public InputViewHolder buildViewHolder() {
        return new InputViewHolder(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static final String STRONG_PW_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])" +
                    "(?=.*[@%_/'!&#:;,<>=~\"\\|\\*\\+\\-\\$\\^\\?\\.\\(\\)\\{\\}\\[\\]\\\\])" +
                    "(?=\\S+$).*$";

    private static final String LETTERS_PATTERN =
            "^[a-zA-Z]*$";

    private static final String ALPHANUMERIC_PATTERN =
            "^[a-zA-Z0-9]*$";

    protected String validatePattern(Context context, @Nullable String input){
        if (pattern != null && input != null){
            if (compiledPattern == null){
                compiledPattern = Pattern.compile(pattern);
            }
            if (!compiledPattern.matcher(input).matches()){
                return getPatternError(context);
            }
        }
        return null;
    }

    @Nullable
    protected String getPatternError(Context context){
        if (patternError != null) {
            return patternError;
        } else if (patternErrorId != NO_ID){
            return context.getString(patternErrorId);
        }
        return null;
    }

    @Nullable
    protected String getHint(Context context){
        if (hint != null) {
            return hint;
        } else if (hintResourceId != NO_ID){
            return context.getString(hintResourceId);
        }
        return null;
    }

    @Nullable
    protected String getText(Context context){
        if (text != null) {
            return text;
        } else if (textResourceId != NO_ID){
            return context.getString(textResourceId);
        }
        return null;
    }

    @Nullable
    protected String[] getSuggestions(Context context){
        if (suggestions != null){
            return suggestions;
        } else if (suggestionStringResArray != null){
            String[] s = new String[suggestionStringResArray.length];
            for (int i = 0; i < suggestionStringResArray.length; i++) {
                s[i] = context.getString(suggestionStringResArray[i]);
            }
            return s;
        } else if (suggestionArrayRes != NO_ID){
            return context.getResources().getStringArray(suggestionArrayRes);
        }
        return null;
    }


    private Input(Parcel in) {
        super(in);
        hint = in.readString();
        hintResourceId = in.readInt();
        text = in.readString();
        textResourceId = in.readInt();
        inputType = in.readInt();
        maxLength = in.readInt();
        minLength = in.readInt();
        suggestionArrayRes = in.readInt();
        suggestionStringResArray = in.createIntArray();
        suggestions = in.createStringArray();
        passwordToggleVisible = in.readByte() != 0;
        forceSuggestion = in.readByte() != 0;
        pattern = in.readString();
        patternError = in.readString();
        patternErrorId = in.readInt();
        isSpinner = in.readByte() != 0;
    }

    public static final Creator<Input> CREATOR = new Creator<Input>() {
        @Override
        public Input createFromParcel(Parcel in) {
            return new Input(in);
        }

        @Override
        public Input[] newArray(int size) {
            return new Input[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(hint);
        dest.writeInt(hintResourceId);
        dest.writeString(text);
        dest.writeInt(textResourceId);
        dest.writeInt(inputType);
        dest.writeInt(maxLength);
        dest.writeInt(minLength);
        dest.writeInt(suggestionArrayRes);
        dest.writeIntArray(suggestionStringResArray);
        dest.writeStringArray(suggestions);
        dest.writeByte((byte) (passwordToggleVisible ? 1 : 0));
        dest.writeByte((byte) (forceSuggestion ? 1 : 0));
        dest.writeString(pattern);
        dest.writeString(patternError);
        dest.writeInt(patternErrorId);
        dest.writeByte((byte) (isSpinner ? 1 : 0));
    }




}


