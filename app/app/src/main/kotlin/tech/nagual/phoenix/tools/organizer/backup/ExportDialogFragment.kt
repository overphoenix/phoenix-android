package tech.nagual.phoenix.tools.organizer.backup

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import me.zhanghai.android.files.ui.UnfilteredArrayAdapter
import tech.nagual.common.ui.simpledialogs.BaseMaterialDialogFragment
import me.zhanghai.android.files.util.*
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerExportDialogBinding
import tech.nagual.phoenix.tools.organizer.utils.ExportOrganizerContract
import tech.nagual.phoenix.tools.organizer.utils.launch
import java.time.Instant

@AndroidEntryPoint
class ExportDialogFragment : BaseMaterialDialogFragment() {

    override val positiveName = R.string.organizer_export_title

    private lateinit var binding: OrganizerExportDialogBinding

    private var exportType: BackupService.ExportType
        get() {
            val adapter = binding.exportType.adapter
            val items = List(adapter.count) { adapter.getItem(it) as CharSequence }
            val selectedItem = binding.exportType.text
            val selectedIndex = items.indexOfFirst { TextUtils.equals(it, selectedItem) }
            return BackupService.ExportType.values()[selectedIndex]
        }
        set(value) {
            val adapter = binding.exportType.adapter
            val item = adapter.getItem(value.ordinal) as CharSequence
            binding.exportType.setText(item, false)
        }

    override fun getTitle(): String = getString(R.string.organizer_export_dialog_title)

    private val exportOrganizerLauncher =
        registerForActivityResult(ExportOrganizerContract) { uri ->
            if (uri == null) return@registerForActivityResult
            BackupService.export(
                this@ExportDialogFragment.requireContext(),
                null,
                this@ExportDialogFragment.exportType,
                this@ExportDialogFragment.binding.withoutAttachments.isChecked,
                Uri.parse(uri.toString() + "/" + this@ExportDialogFragment.binding.nameEdit.text.toString())
            )
            this@ExportDialogFragment.dismiss()
        }

    override fun createView(savedInstanceState: Bundle?): View {
        binding =
            OrganizerExportDialogBinding.inflate(requireContext().layoutInflater)

        val backupName = "backup_${Instant.now().epochSecond}.zip"
        binding.nameLayout.placeholderText = backupName
        binding.nameEdit.setTextWithSelection(backupName)

        binding.exportType.setAdapter(
            UnfilteredArrayAdapter(
                binding.exportType.context, R.layout.dropdown_item,
                objects = getTextArray(R.array.organizer_export_types)
            )
        )
        exportType = BackupService.ExportType.EXPORT_ALL
        binding.exportType.doAfterTextChanged {
            binding.withoutAttachments.isEnabled =
                exportType != BackupService.ExportType.EXPORT_ONLY_CATEGORIES

        }
        return binding.root
    }

    override fun submit() {
        exportOrganizerLauncher.launch()
    }

    companion object {
        fun show(fragment: Fragment) {
            ExportDialogFragment().show(fragment)
        }
    }
}