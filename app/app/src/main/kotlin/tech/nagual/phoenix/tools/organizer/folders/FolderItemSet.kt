package tech.nagual.phoenix.tools.organizer.folders

import android.os.Parcel
import android.os.Parcelable
import me.zhanghai.android.files.compat.writeParcelableListCompat
import me.zhanghai.android.files.util.LinkedMapSet
import me.zhanghai.android.files.util.readParcelableListCompat
import tech.nagual.phoenix.tools.organizer.data.model.Folder

class FolderItemSet() : LinkedMapSet<Long, Folder>(Folder::id), Parcelable {
    constructor(parcel: Parcel) : this() {
        addAll(parcel.readParcelableListCompat())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelableListCompat(toList(), flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FolderItemSet> {
        override fun createFromParcel(parcel: Parcel): FolderItemSet = FolderItemSet(parcel)

        override fun newArray(size: Int): Array<FolderItemSet?> = arrayOfNulls(size)
    }
}

fun folderItemSetOf(vararg folders: Folder) = FolderItemSet().apply { addAll(folders) }
