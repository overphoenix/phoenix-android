package tech.nagual.phoenix.tools.organizer.categories

import android.os.Parcel
import android.os.Parcelable
import me.zhanghai.android.files.compat.writeParcelableListCompat
import tech.nagual.phoenix.tools.organizer.data.model.Category
import me.zhanghai.android.files.util.LinkedMapSet
import me.zhanghai.android.files.util.readParcelableListCompat

class CategoryItemSet() : LinkedMapSet<Long, Category>(Category::id), Parcelable {
    constructor(parcel: Parcel) : this() {
        addAll(parcel.readParcelableListCompat())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelableListCompat(toList(), flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CategoryItemSet> {
        override fun createFromParcel(parcel: Parcel): CategoryItemSet = CategoryItemSet(parcel)

        override fun newArray(size: Int): Array<CategoryItemSet?> = arrayOfNulls(size)
    }
}

fun categoryItemSetOf(vararg categories: Category) = CategoryItemSet().apply { addAll(categories) }
