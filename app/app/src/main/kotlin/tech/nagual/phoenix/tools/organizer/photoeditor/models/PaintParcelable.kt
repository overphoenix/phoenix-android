package tech.nagual.phoenix.tools.organizer.photoeditor.models

import android.os.Parcel
import android.os.Parcelable
import android.view.View

internal class PaintParcelable : View.BaseSavedState {
    var paths = LinkedHashMap<PaintPath, PaintOptions>()

    constructor(superState: Parcelable) : super(superState)

    constructor(parcel: Parcel) : super(parcel) {
        val size = parcel.readInt()
        for (i in 0 until size) {
            val key = parcel.readSerializable() as PaintPath
            val paintOptions =
                PaintOptions(parcel.readInt(), parcel.readFloat())
            paths[key] = paintOptions
        }
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeInt(paths.size)
        for ((path, paintOptions) in paths) {
            out.writeSerializable(path)
            out.writeInt(paintOptions.color)
            out.writeFloat(paintOptions.strokeWidth)
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PaintParcelable> =
            object : Parcelable.Creator<PaintParcelable> {
                override fun createFromParcel(source: Parcel) = PaintParcelable(source)

                override fun newArray(size: Int) = arrayOf<PaintParcelable>()
            }
    }
}
