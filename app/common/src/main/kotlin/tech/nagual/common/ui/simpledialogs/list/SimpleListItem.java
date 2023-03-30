package tech.nagual.common.ui.simpledialogs.list;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ListView;

public class SimpleListItem implements Parcelable {

    private String string;
    private long id;

    @Override
    public String toString() {
        return getString();
    }

    protected SimpleListItem(String string) {
        this(string, ListView.INVALID_ROW_ID);
    }

    protected SimpleListItem(String string, long id) {
        this.string = string;
        this.id = id;
    }

    private SimpleListItem(Parcel in) {
        string = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(string);
    }

    public static final Creator<SimpleListItem> CREATOR = new Creator<>() {
        @Override
        public SimpleListItem createFromParcel(Parcel in) {
            return new SimpleListItem(in);
        }

        @Override
        public SimpleListItem[] newArray(int size) {
            return new SimpleListItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
