package tech.nagual.phoenix.tools.organizer.workflows

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import tech.nagual.common.ui.simpledialogs.BaseMaterialDialogFragment
import me.zhanghai.android.files.util.*
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizerWorkflowEditDialogBinding
import tech.nagual.phoenix.tools.organizer.data.model.Workflow
import tech.nagual.phoenix.tools.organizer.data.repo.WorkflowRepository
import tech.nagual.phoenix.tools.organizer.preferences.DateFormat
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class WorkflowEditDialogFragment : BaseMaterialDialogFragment() {
    private lateinit var workflow: Workflow
    private val args by args<Args>()

    private val listener: Listener
        get() = requireParentFragment() as Listener

    private lateinit var binding: OrganizerWorkflowEditDialogBinding

    @Inject
    lateinit var workflowRepository: WorkflowRepository

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    override fun getTitle(): String {
        return getString(if (args.workflow == null) R.string.organizer_workflow_create_title else R.string.organizer_workflow_rename_title)
    }

    override fun createView(savedInstanceState: Bundle?): View {
        binding = OrganizerWorkflowEditDialogBinding.inflate(requireContext().layoutInflater)

        lifecycleScope.launch {
            val offset = ZoneId.systemDefault().rules.getOffset(Instant.now())
            val today = LocalDateTime.ofEpochSecond(Instant.now().epochSecond, 0, offset)
            val dateFormat = preferenceRepository.get<DateFormat>().first()
            val formatter = DateTimeFormatter.ofPattern(getString(dateFormat.patternResource))
            binding.nameLayout.placeholderText = today.format(formatter)
        }

        if (savedInstanceState == null && args.workflow != null) {
            workflow = args.workflow!!
            binding.nameEdit.setTextWithSelection(
                workflow.name
            )
        }

        return binding.root
    }

    override fun submit() {
        val name =
            if (binding.nameEdit.text.isNullOrEmpty()) binding.nameLayout.placeholderText.toString()
            else binding.nameEdit.text.toString()

        when {
            this::workflow.isInitialized -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = workflowExistsByName(name, ignoreId = workflow.id)
                    if (!exists) {
                        val workflow = workflow.copy(name = name)
                        renameWorkflow(workflow)
                        return@launch dismiss()
                    }

                    showExistsToast(name)
                }
            }
            else -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val exists = workflowExistsByName(name)
                    if (!exists) {
                        withContext(Dispatchers.Main) {
                            listener.createWorkflow(name)
                            dismiss()
                        }
                        return@launch
                    }

                    showExistsToast(name)
                }
            }
        }
    }

    private suspend fun showExistsToast(name: String) {
        withContext(Dispatchers.Main) {
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.organizer_workflow_already_exists, name),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private fun renameWorkflow(workflow: Workflow) {
        lifecycleScope.launch(Dispatchers.IO) {
            workflowRepository.update(workflow)
        }
    }

    private suspend fun workflowExistsByName(name: String, ignoreId: Long? = null): Boolean {
        val workflow = workflowRepository.getByName(name).first()
        return workflow != null && (if (ignoreId != null) workflow.id != ignoreId else true)
    }

    companion object {
        fun show(workflow: Workflow?, fragment: Fragment) {
            WorkflowEditDialogFragment().putArgs(Args(workflow)).show(fragment)
        }
    }

    interface Listener {
        fun createWorkflow(workflowName: String)
    }

    @Parcelize
    class Args(val workflow: Workflow?) : ParcelableArgs
}
