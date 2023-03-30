package tech.nagual.phoenix.tools.organizer.search

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import tech.nagual.phoenix.R
import tech.nagual.phoenix.databinding.OrganizerNoteItemBinding
import tech.nagual.phoenix.databinding.OrganizerSearchFragmentBinding
import tech.nagual.phoenix.tools.organizer.common.AbstractNotesFragment
import tech.nagual.phoenix.tools.organizer.data.model.Note
import tech.nagual.phoenix.tools.organizer.utils.navigateSafely
import tech.nagual.phoenix.tools.organizer.utils.requestFocusAndKeyboard
import tech.nagual.phoenix.tools.organizer.utils.viewBinding

class SearchFragment : AbstractNotesFragment(resId = R.layout.organizer_search_fragment) {
    private val binding by viewBinding(OrganizerSearchFragmentBinding::bind)
    private val args: SearchFragmentArgs by navArgs()

    override val currentDestinationId: Int = R.id.organizer_search_fragment
    override val model: SearchViewModel by viewModels()

    override val isSelectionEnabled = false
    override val hasMenu: Boolean = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerSearch
    override val swipeRefreshLayout: SwipeRefreshLayout
        get() = binding.layoutSwipeRefresh
    override val emptyIndicator: TextView
        get() = binding.emptyView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appBarLayout = binding.appBarLayout
        toolbar = binding.toolbar
        super.onViewCreated(view, savedInstanceState)

        recyclerAdapter.searchMode = true

        binding.editTextSearch.doAfterTextChanged { text ->
            model.setSearchQuery(text.toString())
        }

        when {
            !model.isFirstLoad -> return
            args.searchQuery.isNotEmpty() -> {
                binding.editTextSearch.setText(args.searchQuery)
                binding.editTextSearch.requestFocusAndMoveCaret()
            }
            binding.editTextSearch.text?.isEmpty() == true -> {
                binding.editTextSearch.requestFocusAndKeyboard()
            }
        }

        model.isFirstLoad = false
    }

    override fun onNoteClick(noteId: Long, position: Int, viewBinding: OrganizerNoteItemBinding) {
        applyNavToEditorAnimation(position)
        findNavController().navigateSafely(
            SearchFragmentDirections.actionSearchToEditor(
                "editor_$noteId"
            )
                .setNoteId(noteId),
            FragmentNavigatorExtras(viewBinding.root to "editor_$noteId")
        )
    }

    override fun onNoteLongClick(
        noteId: Long,
        position: Int,
        viewBinding: OrganizerNoteItemBinding
    ): Boolean {
        showMenuForNote(position, isSelectionEnabled = false)
        return true
    }

    override fun onNotesChanged(notes: List<Note>) {
        emptyIndicator.text =
            if (notes.isEmpty() && binding.editTextSearch.text?.isNotEmpty() == true) getString(R.string.indicator_no_results_found)
            else getString(R.string.indicator_search_empty)

        if (binding.editTextSearch.text?.isNotEmpty() == true)
            recyclerAdapter.submitList(notes)
        else
            recyclerAdapter.submitList(listOf())
    }
}
