package tech.nagual.phoenix.tools.organizer.workflows

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.files.util.valueCompat
import tech.nagual.phoenix.tools.organizer.data.model.Workflow
import tech.nagual.phoenix.tools.organizer.data.repo.WorkflowRepository
import tech.nagual.phoenix.tools.organizer.preferences.PreferenceRepository
import javax.inject.Inject

@HiltViewModel
class WorkflowsViewModel @Inject constructor(
    private val workflowRepository: WorkflowRepository,
    private val preferenceRepository: PreferenceRepository
) :
    ViewModel() {

    private val _selectedWorkflowsLiveData = MutableLiveData(workflowItemSetOf())
    val selectedWorkflowsLiveData: LiveData<WorkflowItemSet>
        get() = _selectedWorkflowsLiveData
    val selectedWorkflows: WorkflowItemSet
        get() = _selectedWorkflowsLiveData.valueCompat

    fun selectWorkflow(workflow: Workflow, selected: Boolean) {
        selectWorkflows(workflowItemSetOf(workflow), selected)
    }

    fun selectWorkflows(workflows: WorkflowItemSet, selected: Boolean) {
        val selectedWorkflows = _selectedWorkflowsLiveData.valueCompat
        if (selectedWorkflows === workflows) {
            if (!selected && selectedWorkflows.isNotEmpty()) {
                selectedWorkflows.clear()
                _selectedWorkflowsLiveData.value = selectedWorkflows
            }
            return
        }
        var changed = false
        for (workflow in workflows) {
            changed = changed or if (selected) {
                selectedWorkflows.add(workflow)
            } else {
                selectedWorkflows.remove(workflow)
            }
        }
        if (changed) {
            _selectedWorkflowsLiveData.value = selectedWorkflows
        }
    }

    fun updateWorkflowState(workflow: Workflow) {
        viewModelScope.launch(Dispatchers.IO) {
            workflowRepository.update(workflow.copy(
                active = !workflow.active
            ))
        }
    }

    fun clearSelectedWorkflows() {
        val selectedWorkflows = _selectedWorkflowsLiveData.valueCompat
        if (selectedWorkflows.isEmpty()) {
            return
        }
        selectedWorkflows.clear()
        _selectedWorkflowsLiveData.value = selectedWorkflows
    }

    fun deleteWorkflows(workflowItemSet: WorkflowItemSet) {
        val workflows = workflowItemSet.toTypedArray()
        deleteWorkflows(*workflows)
        selectWorkflows(workflowItemSet, false)
    }

    fun deleteWorkflows(vararg workflows: Workflow) {
        viewModelScope.launch(Dispatchers.IO) {
            workflowRepository.delete(*workflows)
        }
    }
}
