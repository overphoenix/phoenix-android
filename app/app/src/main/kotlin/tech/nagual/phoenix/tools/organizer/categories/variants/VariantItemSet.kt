package tech.nagual.phoenix.tools.organizer.categories.variants

import android.os.Parcel
import android.os.Parcelable
import me.zhanghai.android.files.compat.writeParcelableListCompat
import tech.nagual.phoenix.tools.organizer.data.model.Variant
import me.zhanghai.android.files.util.LinkedMapSet
import me.zhanghai.android.files.util.readParcelableListCompat

class VariantItemSet() : LinkedMapSet<String, Variant>(Variant::value), Parcelable {
    constructor(parcel: Parcel) : this() {
        addAll(parcel.readParcelableListCompat())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelableListCompat(toList(), flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VariantItemSet> {
        override fun createFromParcel(parcel: Parcel): VariantItemSet = VariantItemSet(parcel)

        override fun newArray(size: Int): Array<VariantItemSet?> = arrayOfNulls(size)
    }
}

fun variantItemSetOf(vararg variants: Variant) = VariantItemSet().apply { addAll(variants) }
