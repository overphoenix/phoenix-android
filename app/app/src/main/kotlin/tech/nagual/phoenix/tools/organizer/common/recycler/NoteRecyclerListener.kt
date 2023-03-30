package tech.nagual.phoenix.tools.organizer.common.recycler

import tech.nagual.phoenix.databinding.OrganizerNoteItemBinding

interface NoteRecyclerListener {
    fun onItemClick(position: Int, viewBinding: OrganizerNoteItemBinding)
    fun onLongClick(position: Int, viewBinding: OrganizerNoteItemBinding): Boolean
}
