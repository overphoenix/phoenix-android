package tech.nagual.phoenix.tools.organizer.workflows

import android.os.Parcel
import android.os.Parcelable
import me.zhanghai.android.files.compat.writeParcelableListCompat
import me.zhanghai.android.files.util.LinkedMapSet
import me.zhanghai.android.files.util.readParcelableListCompat
import tech.nagual.phoenix.tools.organizer.data.model.Workflow

class WorkflowItemSet() : LinkedMapSet<Long, Workflow>(Workflow::id), Parcelable {
    constructor(parcel: Parcel) : this() {
        addAll(parcel.readParcelableListCompat())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelableListCompat(toList(), flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WorkflowItemSet> {
        override fun createFromParcel(parcel: Parcel): WorkflowItemSet = WorkflowItemSet(parcel)

        override fun newArray(size: Int): Array<WorkflowItemSet?> = arrayOfNulls(size)
    }
}

fun workflowItemSetOf(vararg workflows: Workflow) = WorkflowItemSet().apply { addAll(workflows) }
