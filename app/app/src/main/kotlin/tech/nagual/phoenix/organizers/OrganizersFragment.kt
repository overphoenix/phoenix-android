package tech.nagual.phoenix.organizers

import android.content.Context
import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe
import me.zhanghai.android.files.util.getDrawable
import tech.nagual.common.R
import tech.nagual.phoenix.databinding.OrganizersFragmentBinding
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Organizer
import tech.nagual.phoenix.tools.organizer.data.repo.OrganizerRepository
import javax.inject.Inject

@AndroidEntryPoint
class OrganizersFragment : Fragment(), OrganizersAdapter.Listener,
    OrganizersManager.OrganizersListener {
    private lateinit var binding: OrganizersFragmentBinding

    private lateinit var adapter: OrganizersAdapter
    private lateinit var dragDropManager: RecyclerViewDragDropManager
    private lateinit var wrappedAdapter: RecyclerView.Adapter<*>

    @Inject
    lateinit var organizerRepository: OrganizerRepository

    override val currentContext: Context
        get() = requireContext()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        OrganizersFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(
            activity, RecyclerView.VERTICAL, false
        )
        adapter = OrganizersAdapter(this)
        dragDropManager = RecyclerViewDragDropManager().apply {
            setDraggingItemShadowDrawable(
                getDrawable(R.drawable.ms9_composite_shadow_z2) as NinePatchDrawable
            )
        }

        wrappedAdapter = dragDropManager.createWrappedAdapter(adapter)
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = DraggableItemAnimator()
        dragDropManager.attachRecyclerView(binding.recyclerView)
        binding.fab.setOnClickListener {
            EditOrganizerDialogActivity.showCreateOrganizerDialog(
                requireContext()
            )
        }
    }

    override fun onStart() {
        super.onStart()
        OrganizersManager.getInstance().addOrganizersListener(this)
    }

    override fun onStop() {
        super.onStop()
        OrganizersManager.getInstance().removeOrganizersListener(this)
    }

    override fun updateOrganizers(organizers: List<Organizer>) {
        binding.emptyView.fadeToVisibilityUnsafe(organizers.isEmpty())
        adapter.replace(organizers)
    }

    override fun onPause() {
        super.onPause()

        dragDropManager.cancelDrag()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        dragDropManager.release()
        WrapperAdapterUtils.releaseAll(wrappedAdapter)
    }

    override fun moveOrganizer(fromPosition: Int, toPosition: Int) {
        val organizers = OrganizersManager.organizers.toMutableList()
        val inc: Int
        val min: Int
        val max: Int
        val toPos: Int
        val toOrdinal: Int
        if (fromPosition < toPosition) {
            inc = -1
            min = fromPosition + 1
            max = toPosition
            toPos = min + inc
            toOrdinal = max
        } else {
            inc = 1
            min = toPosition
            max = fromPosition - 1
            toPos = max + inc
            toOrdinal = min
        }

        for (i in min..max) {
            val organizer = organizers[i]
            organizers[i] = organizer.copy(ordinal = organizer.ordinal + inc)
        }
        val organizer = organizers[toPos]
        organizers[toPos] = organizer.copy(ordinal = toOrdinal)

        lifecycleScope.launch {
            organizerRepository.update(*organizers.toTypedArray())
        }
    }
}
