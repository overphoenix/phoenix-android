package tech.nagual.phoenix.tools.gps.dialogs

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tech.nagual.phoenix.R

class RenameTrackDialog(private var renameTrackListener: RenameTrackListener) {

    /* Interface used to communicate back to activity */
    interface RenameTrackListener {
        fun onRenameTrackDialog(textInput: String) {
        }
    }

    /* Construct and show dialog */
    fun show(context: Context, trackName: String) {
        // prepare dialog builder
        val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context)

        // get input field
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.trackbook_dialog_rename_track, null)
        val inputField = view.findViewById<EditText>(R.id.dialog_rename_track_input_edit_text)

        // pre-fill with current track name
        inputField.setText(trackName, TextView.BufferType.EDITABLE)
        inputField.setSelection(trackName.length)
        inputField.inputType = InputType.TYPE_CLASS_TEXT

        // set dialog view
        builder.setView(view)

        // add "add" button
        builder.setPositiveButton(R.string.trackbook_dialog_rename_track_button) { _, _ ->
            // hand text over to initiating activity
            inputField.text?.let {
                var newStationName: String = it.toString()
                if (newStationName.isEmpty()) newStationName = trackName
                renameTrackListener.onRenameTrackDialog(newStationName)
            }
        }

        // add cancel button
        builder.setNegativeButton(R.string.trackbook_dialog_generic_button_cancel) { _, _ ->
            // listen for click on cancel button
            // do nothing
        }

        // display add dialog
        builder.show()
    }

}