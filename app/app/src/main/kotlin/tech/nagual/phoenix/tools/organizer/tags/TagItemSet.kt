package tech.nagual.phoenix.tools.organizer.tags

import android.os.Parcel
import android.os.Parcelable
import me.zhanghai.android.files.compat.writeParcelableListCompat
import tech.nagual.phoenix.tools.organizer.data.model.Tag
import me.zhanghai.android.files.util.LinkedMapSet
import me.zhanghai.android.files.util.readParcelableListCompat

fun <A, B, C> ((A) -> B).nested(getter : (B) -> C) : (A) -> C = { getter(this(it)) }

class TagItemSet() : LinkedMapSet<String, TagData>(TagData::tag.nested(Tag::name)), Parcelable {
    constructor(parcel: Parcel) : this() {
        addAll(parcel.readParcelableListCompat())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelableListCompat(toList(), flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TagItemSet> {
        override fun createFromParcel(parcel: Parcel): TagItemSet = TagItemSet(parcel)

        override fun newArray(size: Int): Array<TagItemSet?> = arrayOfNulls(size)
    }
}

fun tagItemSetOf(vararg tags: TagData) = TagItemSet().apply { addAll(tags) }
