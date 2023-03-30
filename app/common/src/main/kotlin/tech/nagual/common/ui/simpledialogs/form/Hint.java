package tech.nagual.common.ui.simpledialogs.form;

import android.os.Parcel;

import androidx.annotation.StringRes;


/**
 * A hint element to be used with {@link SimpleFormDialog}
 * 
 * This is simple hint text
 * 
 * Created by philipp on 06.07.2018
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class Hint extends FormElement<Hint, HintViewHolder> {

    public Hint() {
        super((String) null);
    }




    /**
     * Factory method for a hint.
     *
     * @param hint the hint text
     * @return this instance
     */
    public static Hint plain(String hint){
        return new Hint().label(hint);
    }
    public static Hint plain(@StringRes int hint){
        return new Hint().label(hint);
    }







    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public HintViewHolder buildViewHolder() {
        return new HintViewHolder(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////


    protected Hint(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Hint> CREATOR = new Creator<Hint>() {
        @Override
        public Hint createFromParcel(Parcel in) {
            return new Hint(in);
        }

        @Override
        public Hint[] newArray(int size) {
            return new Hint[size];
        }
    };

}
