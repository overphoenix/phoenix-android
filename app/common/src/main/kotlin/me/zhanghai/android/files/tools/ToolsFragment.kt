package me.zhanghai.android.files.tools

import android.content.Context
import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import me.zhanghai.android.files.util.getDrawable
import me.zhanghai.android.files.util.startActivitySafe
import tech.nagual.common.R
import tech.nagual.common.databinding.ToolsFragmentBinding
import tech.nagual.settings.Settings
import me.zhanghai.android.files.util.fadeToVisibilityUnsafe

class ToolsFragment : Fragment(), ToolsAdapter.Listener {
    private lateinit var binding: ToolsFragmentBinding

    private lateinit var adapter: ToolsAdapter
    private lateinit var dragDropManager: RecyclerViewDragDropManager
    private lateinit var wrappedAdapter: RecyclerView.Adapter<*>

    override val currentContext: Context
        get() = requireContext()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ToolsFragmentBinding.inflate(inflater, container, false)
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
        adapter = ToolsAdapter(this)
        dragDropManager = RecyclerViewDragDropManager().apply {
            setDraggingItemShadowDrawable(
                getDrawable(R.drawable.ms9_composite_shadow_z2) as NinePatchDrawable
            )
        }

        wrappedAdapter = dragDropManager.createWrappedAdapter(adapter)
        binding.recyclerView.adapter = wrappedAdapter
        binding.recyclerView.itemAnimator = DraggableItemAnimator()
        dragDropManager.attachRecyclerView(binding.recyclerView)

        Settings.TOOLS.observe(viewLifecycleOwner) { onToolListChanged(it) }
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

    private fun onToolListChanged(tools: List<Tool>) {
        binding.emptyView.fadeToVisibilityUnsafe(tools.isEmpty())
        adapter.replace(tools)
    }

    override fun editTool(tool: Tool) {
        startActivitySafe(tool.createEditIntent())
    }

    override fun moveTool(fromPosition: Int, toPosition: Int) {
        Tools.move(fromPosition, toPosition)
    }
}
