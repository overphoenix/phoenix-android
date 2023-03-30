package tech.nagual.common.ui.simpledialogs.form;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Base-class for form elements to be used with {@link SimpleFormDialog}
 * <p>
 * Each form element holds a {@link FormElement#resultKey} that is used to receive element
 * specific results in {@link SimpleFormDialog#onResult}
 * <p>
 * Created by eltos on 20.02.17.
 */

@SuppressWarnings("WeakerAccess")
public abstract class FormElement<T extends FormElement, V extends FormElementViewHolder> implements Parcelable {

    protected static final int NO_ID = -1;

    protected String resultKey;
    protected boolean required = false;
    private String text = null;
    private int textResourceId = NO_ID;


    protected FormElement(String resultKey) {
        this.resultKey = resultKey;
    }


    /**
     * Return your custom implementation of {@link FormElementViewHolder} here
     *
     * @return The view holder that can represent this form element
     */
    public abstract V buildViewHolder();


    /**
     * Mark this Field as required.
     * See {@link FormElement#required(boolean)}
     *
     * @return this instance
     */
    public T required() {
        return required(true);
    }

    /**
     * Set the required flag for this field.
     * This has different meanings depending on the element type
     * <p>
     * Input fields will display an error message if their input is empty.
     * Check fields will be required to be checked
     *
     * @param required whether this field is required
     * @return this instance
     */
    @SuppressWarnings("unchecked cast")
    public T required(boolean required) {
        this.required = required;
        return (T) this;
    }

    /**
     * Sets the label
     *
     * @param text label text as string
     * @return this instance
     */
    @SuppressWarnings("unchecked cast")
    public T label(String text) {
        this.text = text;
        return (T) this;
    }

    /**
     * Sets the label
     *
     * @param textResourceId label text as android string resource
     * @return this instance
     */
    @SuppressWarnings("unchecked cast")
    public T label(@StringRes int textResourceId) {
        this.textResourceId = textResourceId;
        return (T) this;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    protected String getText(Context context) {
        if (text != null) {
            return text;
        } else if (textResourceId != NO_ID) {
            return context.getString(textResourceId);
        }
        return null;
    }


    // Parcel implementation

    protected FormElement(Parcel in) {
        resultKey = in.readString();
        required = in.readByte() != 0;
        text = in.readString();
        textResourceId = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(resultKey);
        dest.writeByte((byte) (required ? 1 : 0));
        dest.writeString(text);
        dest.writeInt(textResourceId);
    }
}
