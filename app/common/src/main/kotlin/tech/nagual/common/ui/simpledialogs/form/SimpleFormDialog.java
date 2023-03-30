package tech.nagual.common.ui.simpledialogs.form;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Collections;

import tech.nagual.common.R;
import tech.nagual.common.ui.simpledialogs.CustomViewDialog;
import tech.nagual.common.ui.simpledialogs.SimpleDialog;

/**
 * A form dialog to display a number of input fields to the user, such as
 * - Input fields ({@link Input})
 * - Check-boxes ({@link Check})
 * - Dropdown-menus ({@link Spinner})
 *
 * Created by eltos on 20.02.17.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class SimpleFormDialog extends CustomViewDialog<SimpleFormDialog> implements SimpleDialog.OnDialogResultListener {

    public static final String TAG = "SimpleFormDialog.";


    public static SimpleFormDialog build(){
        return new SimpleFormDialog();
    }


    /**
     * Convenient method to build a form dialog with a single email input
     *
     * @param emailFieldKey the key that can be used to receive the entered text from the bundle in
     *                      {@link SimpleDialog.OnDialogResultListener#onResult}
     * @return this instance
     */
    public static SimpleFormDialog buildEmailInput(String emailFieldKey){
        return SimpleFormDialog.build()
                .fields(
                        Input.email(emailFieldKey).required()
                );
    }

    /**
     * Convenient method to build a form dialog with a single password input
     *
     * @param passwordFieldKey the key that can be used to receive the entered text from the bundle
     *                         in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @return this instance
     */
    public static SimpleFormDialog buildPasswordInput(String passwordFieldKey){
        return SimpleFormDialog.build()
                .fields(
                        Input.password(passwordFieldKey).required()
                );
    }

    /**
     * Convenient method to build a form dialog with a single pin code input
     *
     * @param pinFieldKey the key that can be used to receive the entered text from the bundle
     *                         in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @return this instance
     */
    public static SimpleFormDialog buildPinCodeInput(String pinFieldKey){
        return SimpleFormDialog.build()
                .fields(
                        Input.pin(pinFieldKey).required()
                );
    }

    /**
     * Convenient method to build a form dialog with a single pin code input
     *
     * @param pinFieldKey the key that can be used to receive the entered text from the bundle
     *                         in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @param digits the length of the pin code
     * @return this instance
     */
    public static SimpleFormDialog buildPinCodeInput(String pinFieldKey, int digits){
        return SimpleFormDialog.build()
                .fields(
                        Input.pin(pinFieldKey).required().min(digits).max(digits)
                );
    }

    /**
     * Convenient method to build a form dialog with a single number input
     *
     * @param numberFieldKey the key that can be used to receive the entered text from the bundle
     *                       in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @return this instance
     */
    public static SimpleFormDialog buildNumberInput(String numberFieldKey){
        return SimpleFormDialog.build()
                .fields(
                        Input.phone(numberFieldKey).required()
                );
    }

    /**
     * Convenient method to build a form dialog with an email input alongside
     * a password input for login with email address and password
     *
     * @param emailFieldKey the key that can be used to receive the entered email from the bundle
     *                      in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @param passwordFieldKey the key that can be used to receive the entered password from the
     *                         bundle in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @return this instance
     */
    public static SimpleFormDialog buildLoginEmail(String emailFieldKey, String passwordFieldKey){
        return SimpleFormDialog.build()
                .title(R.string.dialogs_login)
                .pos(R.string.dialogs_login)
                .fields(
                        Input.email(emailFieldKey).required(),
                        Input.password(passwordFieldKey).required()
                );
    }

    /**
     * Convenient method to build a form dialog with a plain input alongside
     * a password input for login with username and password
     *
     * @param userFieldKey the key that can be used to receive the entered username from the bundle
     *                     in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @param passwordFieldKey the key that can be used to receive the entered password from the
     *                         bundle in {@link SimpleDialog.OnDialogResultListener#onResult}
     * @return this instance
     */
    public static SimpleFormDialog buildLogin(String userFieldKey, String passwordFieldKey){
        return SimpleFormDialog.build()
                .title(R.string.dialogs_login)
                .pos(R.string.dialogs_login)
                .fields(
                        Input.plain(userFieldKey).hint(R.string.dialogs_user).required(),
                        Input.password(passwordFieldKey).required()
                );
    }



    /**
     * Convenient method to populate the form with form elements
     *
     * @param elements the {@link FormElement}s that form should contain
     * @return this instance
     */
    public SimpleFormDialog fields(FormElement... elements){
        ArrayList<FormElement> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        getArgs().putParcelableArrayList(INPUT_FIELDS, list);
        return this;
    }
    
    /**
     * En- or disables the automatic focussing of the first field in the form when the dialog opens.
     * This is enabled by default.
     *
     * @param enabled whether or not to autofocus the first field
     * @return this instance
     */
    public SimpleFormDialog autofocus(boolean enabled){
        return setArg(AUTO_FOCUS, enabled);
    }



    public interface InputValidator {
        /**
         * Let the hosting fragment or activity implement this interface to make
         * custom validations for {@link Input} fields.
         * You may also use {@link Input#validatePattern} with a custom or predefined
         * pattern.
         * The method is called every time the user hits the positive button or next key.
         *
         * @param dialogTag the tag of this fragment
         * @param fieldKey the key of the field as supplied when the corresponding
         *                 {@link Input} was created (see {@link Input#plain(String)} etc)
         * @param input the text entered by the user
         * @param extras the extras passed with {@link SimpleFormDialog#extra(Bundle)}
         *
         * @return the error message to display or null if the input is valid
         */
        String validate(String dialogTag, String fieldKey, @Nullable String input, @NonNull Bundle extras);
    }








    ///////////////////////////////////////////////////////////////////////////////////////////

    protected static final String INPUT_FIELDS = TAG + "inputFields";
    protected static final String AUTO_FOCUS = TAG + "autofocus";
    protected static final String SAVE_TAG = "form.";

    private FocusActions mFocusActions = new FocusActions();
    ArrayList<FormElementViewHolder<?>> mViews = new ArrayList<>(0);
    ViewGroup mFormContainer;



    protected String onValidateInput(String fieldKey, @Nullable String input){
        Bundle extras = getExtras();
        if (getTargetFragment() instanceof InputValidator) {
            return ((InputValidator) getTargetFragment())
                    .validate(getTag(), fieldKey, input, extras);
        }
        if (getActivity() instanceof InputValidator) {
            return ((InputValidator) getActivity())
                    .validate(getTag(), fieldKey, input, extras);
        }
        return null;
    }




    @Override
    protected void onDialogShown() {
        // resize dialog when keyboard is shown to prevent fields from hiding behind the keyboard
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setPositiveButtonEnabled(posButtonEnabled());
        
        if (getArgs().getBoolean(AUTO_FOCUS, true)){
            requestFocus(0);
        }
    }


    @Override
    protected boolean acceptsPositiveButtonPress() {
        boolean okay = true;
        for (FormElementViewHolder holder : mViews){
            if (!holder.validate(getContext())){
                if (okay) holder.focus(mFocusActions); // focus first element that is not valid
                okay = false;
            } else if (holder instanceof InputViewHolder){
                // custom validation
                String error = onValidateInput(holder.field.resultKey, ((InputViewHolder) holder).getText());
                if (error != null){
                    ((InputViewHolder) holder).setError(true, error);
                    if (okay) holder.focus(mFocusActions); // focus first element that is not valid
                    okay = false;
                }
            }
        }
        return okay;
    }


    protected boolean posButtonEnabled() {
        int first = getFirstFocusableIndex();
        if (0 <= first && isLastFocusableIndex(first) && first < mViews.size()){
            // first==last --> only one
            return mViews.get(first).posButtonEnabled(getContext());
        }
        return true;
    }


    protected void requestFocus(int viewIndex){
        if (0 <= viewIndex && viewIndex < mViews.size()) {
            mViews.get(viewIndex).focus(mFocusActions);
        }
    }




    /**
     * A Callback Class with useful methods used by {@link FormElementViewHolder#focus}
     */
    public class FocusActions {
        /**
         * Helper to hide the soft keyboard
         */
        public void hideKeyboard(){
            View view = getDialog().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }

        /**
         * Helper for opening the soft keyboard on a specified view
         */
        public void showKeyboard(final View view){
            SimpleFormDialog.this.showKeyboard(view);
        }

        /**
         * Helper to clear the current focus
         */
        public void clearCurrentFocus(){
            mFormContainer.requestFocus();
        }


    }


    /**
     * A Callback Class with useful methods used by {@link FormElementViewHolder#setUpView}
     */
    public class DialogActions extends FocusActions {

        private int index;
        private int lastIndex;

        private DialogActions(int index, int lastIndex){
            this.index = index;
            this.lastIndex = lastIndex;
        }

        /**
         * Helper to request an update of the positive button state
         */
        public void updatePosButtonState(){
            setPositiveButtonEnabled(posButtonEnabled());
        }

        /**
         * Check if this is the only (focusable) element
         *
         * @return true if this is the only (focusable) element
         */
        public boolean isOnlyFocusableElement(){
            return isOnlyFocusableIndex(index);
        }

        /**
         * Check if this is the last (focusable) element
         *
         * @return true if this is the last (focusable) element
         */
        public boolean isLastFocusableElement(){
            return isLastFocusableIndex(index);
        }

        /**
         * Helper to move the focus to the next element or to simulate a positive button
         * press if this is the last element
         *
         * @param mayPressPositiveButtonIfLast whether the positive button can be pressed
         *                                     if this was the last element
         */
        public void continueWithNextElement(boolean mayPressPositiveButtonIfLast){
            if (mayPressPositiveButtonIfLast && isLastFocusableElement()){
                pressPositiveButton();
            } else {
                requestFocus(getNextFocusableIndex(index));
            }
        }

        public void showDialog(SimpleDialog dialog, String tag){
            dialog.show(SimpleFormDialog.this, tag);
        }

    }


    private boolean isFocusableIndex(int i){
        ArrayList<FormElement> fields = getArgs().getParcelableArrayList(INPUT_FIELDS);
        return 0 <= i && fields != null && i < fields.size() && !(fields.get(i) instanceof Hint);
    }

    private int getNextFocusableIndex(int i){
        ArrayList<FormElement> fields = getArgs().getParcelableArrayList(INPUT_FIELDS);
        do {
            i++;
            if (fields == null || i >= fields.size()) return Integer.MAX_VALUE;
        } while (!isFocusableIndex(i));
        return i;
    }

    private int getFirstFocusableIndex(){
        return getNextFocusableIndex(-1);
    }

    private boolean isOnlyFocusableIndex(int i){
        return i == getFirstFocusableIndex() && isLastFocusableIndex(i);
    }

    private boolean isLastFocusableIndex(int i){
        return isFocusableIndex(i) && getNextFocusableIndex(i) == Integer.MAX_VALUE;
    }




    /**
     * Method for view creation. Inflates the layout and calls
     * {@link SimpleFormDialog#populateContainer(ViewGroup, Bundle)}
     * to populate the container with the fields specified
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     * or null if this fragment is created for the first time.
     *
     * @return inflated view
     */
    @Override
    public View onCreateContentView(Bundle savedInstanceState) {

        // inflate custom view
        View view = inflate(R.layout.simpledialogfragment_form);
        ViewGroup container = (ViewGroup) view.findViewById(R.id.container);

        populateContainer(container, savedInstanceState);

        setPositiveButtonEnabled(posButtonEnabled());

        return view;
    }

    /**
     * Creates FormElements and adds them to the container
     *
     * @param container the container to hold the FormElements
     * @param savedInstanceState saved state
     */
    protected void populateContainer(@NonNull ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        mFormContainer = container;

        ArrayList<FormElement> fields = getArgs().getParcelableArrayList(INPUT_FIELDS);

        if (fields != null) {

            mViews = new ArrayList<>(fields.size());

            int lastI = fields.size() - 1;
            for (int i = 0; i < fields.size(); i++) {

                FormElementViewHolder<?> viewHolder = fields.get(i).buildViewHolder();

                View child = inflate(viewHolder.getContentViewLayout(), mFormContainer, false);

                Bundle savedState = savedInstanceState == null ? null :
                        savedInstanceState.getBundle(SAVE_TAG + i);

                viewHolder.setUpView(child, getContext(), savedState, new DialogActions(i, lastI));

                mFormContainer.addView(child);
                mViews.add(viewHolder);

            }

        }
    }


    @Override
    public Bundle onResult(int which) {
        Bundle result = new Bundle();
        for (FormElementViewHolder holder : mViews) {
            holder.putResults(result, holder.field.resultKey);
        }
        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        for (int i = 0; i < mViews.size(); i++) {
            Bundle viewState = new Bundle();
            mViews.get(i).saveState(viewState);
            outState.putBundle(SAVE_TAG + i, viewState);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        ArrayList<FormElement> fields = getArgs().getParcelableArrayList(INPUT_FIELDS);
        if (fields != null) {
            for (FormElementViewHolder<?> view : mViews) {
                if (view instanceof OnDialogResultListener){
                    if (((OnDialogResultListener) view).onResult(dialogTag, which, extras)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
