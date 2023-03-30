package tech.nagual.common.ui.simplemenu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tech.nagual.common.R
import java.util.*

/**
 * Extension of [PopupWindow] that implements Simple Menus in Material Design 1.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class SimpleMenuPopupWindow @SuppressLint("InflateParams") constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : PopupWindow(context, attrs, defStyleAttr, defStyleRes) {
    interface OnItemClickListener {
        fun onItemClick(i: Int)
    }

    protected val elevation = IntArray(2)
    protected val margin = Array(2) { IntArray(2) }
    val listPadding = Array<IntArray?>(2) { IntArray(2) }
    protected val itemHeight: Int
    protected val dialogMaxWidth: Int
    protected val unit: Int
    protected val maxUnits: Int
    var mode = POPUP_MENU
        private set
    private var mRequestMeasure = true
    private val mList: RecyclerView
    private val mAdapter: SimpleMenuListAdapter
    var onItemClickListener: OnItemClickListener? = null
    private lateinit var mEntries: Array<CharSequence>
    var selectedIndex = 0
    private var mMeasuredWidth = 0

    val entries: Array<CharSequence>?
        get() = mEntries

    fun setEntries(entries: Array<CharSequence>) {
        mEntries = entries
    }

    override fun getContentView(): RecyclerView {
        return super.getContentView() as RecyclerView
    }

    override fun getBackground(): CustomBoundsDrawable {
        val background = super.getBackground()
        if (background != null
            && background !is CustomBoundsDrawable
        ) {
            setBackgroundDrawable(background)
        }
        return super.getBackground() as CustomBoundsDrawable
    }

    override fun setBackgroundDrawable(background: Drawable) {
        var background = background
            ?: throw IllegalStateException("SimpleMenuPopupWindow must have a background")
        if (background !is CustomBoundsDrawable) {
            background = CustomBoundsDrawable(background)
        }
        super.setBackgroundDrawable(background)
    }

    /**
     * Show the PopupWindow
     *
     * @param anchor      View that will be used to calc the position of windows
     * @param container   View that will be used to calc the position of windows
     * @param extraMargin extra margin start
     */
    fun show(anchor: View, container: View, extraMargin: Int) {
        val maxMaxWidth = container.width - margin[POPUP_MENU][HORIZONTAL] * 2
        val measuredWidth = measureWidth(maxMaxWidth, mEntries)
        if (measuredWidth == -1) {
            mode = DIALOG
        } else if (measuredWidth != 0) {
            mode = POPUP_MENU
            mMeasuredWidth = measuredWidth
        }
        mAdapter.notifyDataSetChanged()

        // clear last bounds
        val zeroRect = Rect()
        background.setCustomBounds(zeroRect)
        contentView.invalidateOutline()
        if (mode == POPUP_MENU) {
            showPopupMenu(anchor, container, mMeasuredWidth, extraMargin)
        } else {
            showDialog(anchor, container)
        }
    }

    /**
     * Show popup window in dialog mode
     *
     * @param parent    a parent view to get the [View.getWindowToken] token from
     * @param container Container view that holds preference list, also used to calc width
     */
    private fun showDialog(parent: View, container: View) {
        val index = Math.max(0, selectedIndex)
        val count = mEntries.size
        contentView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        contentView.scrollToPosition(index)
        width = Math.min(
            dialogMaxWidth,
            container.width - margin[DIALOG][HORIZONTAL] * 2
        )
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        animationStyle = R.style.Animation_Preference_SimpleMenuCenter
        setElevation(elevation[DIALOG].toFloat())
        super.showAtLocation(parent, Gravity.CENTER_VERTICAL, 0, 0)
        contentView.post {

            // disable over scroll when no scroll
            val lm = contentView.layoutManager as LinearLayoutManager?
            if (lm!!.findFirstCompletelyVisibleItemPosition() == 0
                && lm.findLastCompletelyVisibleItemPosition() == count - 1
            ) {
                contentView.overScrollMode = View.OVER_SCROLL_NEVER
            }
            val width = contentView.width
            val height = contentView.height
            val start = Rect(width / 2, height / 2, width / 2, height / 2)
            SimpleMenuAnimation.startEnterAnimation(
                background,
                contentView,
                width,
                height,
                width / 2,
                height / 2,
                start,
                itemHeight,
                elevation[DIALOG] / 4,
                index
            )
        }
    }

    /**
     * Show popup window in popup mode
     *
     * @param anchor    View that will be used to calc the position of the window
     * @param container Container view that holds preference list, also used to calc width
     * @param width     Measured width of this window
     */
    private fun showPopupMenu(anchor: View, container: View, width: Int, extraMargin: Int) {
        val rtl = container.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val index = Math.max(0, selectedIndex)
        val count = mEntries.size
        val anchorTop = anchor.top - container.paddingTop
        val anchorHeight = anchor.height
        val measuredHeight = itemHeight * count + listPadding[POPUP_MENU]!![VERTICAL] * 2
        val location = IntArray(2)
        container.getLocationInWindow(location)
        val containerTopInWindow = location[1] + container.paddingTop
        val containerHeight = container.height - container.paddingTop - container.paddingBottom
        var y: Int
        val height: Int
        val elevation = elevation[POPUP_MENU]
        val centerX =
            if (rtl) location[0] + extraMargin - width + listPadding[POPUP_MENU]!![HORIZONTAL] else location[0] + extraMargin + listPadding[POPUP_MENU]!![HORIZONTAL]
        val centerY: Int
        val animItemHeight = itemHeight + listPadding[POPUP_MENU]!![VERTICAL] * 2
        val animStartRect: Rect
        if (measuredHeight > containerHeight) {
            // too high, use scroll
            y = containerTopInWindow + margin[POPUP_MENU][VERTICAL]

            // scroll to select item
            val scroll = (itemHeight * index
                    - anchorTop) + listPadding[POPUP_MENU]!![VERTICAL] + margin[POPUP_MENU][VERTICAL]
            -anchorHeight / 2 + itemHeight / 2
            contentView.post {
                contentView.scrollBy(0, -measuredHeight) // to top
                contentView.scrollBy(0, scroll)
            }
            contentView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            height = containerHeight - margin[POPUP_MENU][VERTICAL] * 2
            centerY = itemHeight * index
        } else {
            // calc align to selected
            y =
                containerTopInWindow + anchorTop + anchorHeight / 2 - itemHeight / 2 - listPadding[POPUP_MENU]!![VERTICAL] - index * itemHeight

            // make sure window is in parent view
            val maxY =
                containerTopInWindow + containerHeight - measuredHeight - margin[POPUP_MENU][VERTICAL]
            y = Math.min(y, maxY)
            val minY = containerTopInWindow + margin[POPUP_MENU][VERTICAL]
            y = Math.max(y, minY)
            contentView.overScrollMode = View.OVER_SCROLL_NEVER
            height = measuredHeight

            // center of selected item
            centerY =
                (listPadding[POPUP_MENU]!![VERTICAL] + index * itemHeight + itemHeight * 0.5).toInt()
        }
        setWidth(width)
        setHeight(height)
        setElevation(elevation.toFloat())
        animationStyle = R.style.Animation_Preference_SimpleMenuCenter
        enterTransition = null
        exitTransition = null
        super.showAtLocation(anchor, Gravity.NO_GRAVITY, centerX, y)
        val startTop = centerY - (itemHeight * 0.2).toInt()
        val startBottom = centerY + (itemHeight * 0.2).toInt()
        val startLeft: Int
        val startRight: Int
        if (!rtl) {
            startLeft = centerX
            startRight = centerX + unit
        } else {
            startLeft = centerX + width - unit
            startRight = centerX + width
        }
        animStartRect = Rect(startLeft, startTop, startRight, startBottom)
        val animElevation = Math.round(elevation * 0.25).toInt()
        contentView.post {
            SimpleMenuAnimation.startEnterAnimation(
                background, contentView,
                width, height, centerX, centerY, animStartRect, animItemHeight, animElevation, index
            )
        }
    }

    /**
     * Request a measurement before next show, call this when entries changed.
     */
    fun requestMeasure() {
        mRequestMeasure = true
    }

    /**
     * Measure window width
     *
     * @param maxWidth max width for popup
     * @param entries  Entries of preference hold this window
     * @return 0: skip
     * -1: use dialog
     * other: measuredWidth
     */
    private fun measureWidth(maxWidth: Int, entries: Array<CharSequence>): Int {
        // skip if should not measure
        var maxWidth = maxWidth
        var entries = entries
        if (!mRequestMeasure) {
            return 0
        }
        mRequestMeasure = false
        entries = Arrays.copyOf(entries, entries.size)
        Arrays.sort(entries) { o1: CharSequence?, o2: CharSequence? -> o2!!.length - o1!!.length }
        val context = contentView.context
        var width = 0
        maxWidth = Math.min(unit * maxUnits, maxWidth)
        val bounds = Rect()
        val view = LayoutInflater.from(context).inflate(R.layout.simple_menu_item, null, false)
            .findViewById<TextView>(android.R.id.text1)
        val textPaint: Paint = view.paint
        for (chs in entries) {
            textPaint.getTextBounds(chs.toString(), 0, chs.toString().length, bounds)
            width = Math.max(
                width,
                bounds.right + 1 + Math.round((listPadding[POPUP_MENU]!![HORIZONTAL] * 2 + 1).toFloat())
            )

            // more than one line should use dialog
            if (width > maxWidth
                || chs.toString().contains("\n")
            ) {
                return -1
            }
        }

        // width is a multiple of a unit
        var w = 0
        while (width > w) {
            w += unit
        }
        return w
    }

    override fun showAtLocation(parent: View, gravity: Int, x: Int, y: Int) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }

    override fun showAsDropDown(anchor: View) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }

    override fun showAsDropDown(anchor: View, xoff: Int, yoff: Int) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }

    override fun showAsDropDown(anchor: View, xoff: Int, yoff: Int, gravity: Int) {
        throw UnsupportedOperationException("use show(anchor) to show the window")
    }

    companion object {
        const val POPUP_MENU = 0
        const val DIALOG = 1
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }

    init {
        isFocusable = true
        isOutsideTouchable = false
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.SimpleMenuPopup, defStyleAttr, defStyleRes
        )
        elevation[POPUP_MENU] =
            a.getDimension(R.styleable.SimpleMenuPopup_listElevation, 4f).toInt()
        elevation[DIALOG] = a.getDimension(R.styleable.SimpleMenuPopup_dialogElevation, 48f).toInt()
        margin[POPUP_MENU][HORIZONTAL] =
            a.getDimension(R.styleable.SimpleMenuPopup_listMarginHorizontal, 0f).toInt()
        margin[POPUP_MENU][VERTICAL] =
            a.getDimension(R.styleable.SimpleMenuPopup_listMarginVertical, 0f).toInt()
        margin[DIALOG][HORIZONTAL] =
            a.getDimension(R.styleable.SimpleMenuPopup_dialogMarginHorizontal, 0f).toInt()
        margin[DIALOG][VERTICAL] =
            a.getDimension(R.styleable.SimpleMenuPopup_dialogMarginVertical, 0f).toInt()
        listPadding[POPUP_MENU]!![HORIZONTAL] =
            a.getDimension(R.styleable.SimpleMenuPopup_listItemPadding, 0f).toInt()
        listPadding[DIALOG]!![HORIZONTAL] =
            a.getDimension(R.styleable.SimpleMenuPopup_dialogItemPadding, 0f).toInt()
        dialogMaxWidth = a.getDimension(R.styleable.SimpleMenuPopup_dialogMaxWidth, 0f).toInt()
        unit = a.getDimension(R.styleable.SimpleMenuPopup_unit, 0f).toInt()
        maxUnits = a.getInteger(R.styleable.SimpleMenuPopup_maxUnits, 0)
        mList =
            LayoutInflater.from(context).inflate(R.layout.simple_menu_list, null) as RecyclerView
        mList.isFocusable = true
        mList.layoutManager = LinearLayoutManager(context)
        mList.itemAnimator = null
        mList.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                background.getOutline(outline)
            }
        }
        mList.clipToOutline = true
        contentView = mList
        mAdapter = SimpleMenuListAdapter(this)
        mList.adapter = mAdapter
        a.recycle()

        // TODO do not hardcode
        itemHeight = Math.round(context.resources.displayMetrics.density * 48)
        listPadding[DIALOG]!![VERTICAL] = Math.round(context.resources.displayMetrics.density * 8)
        listPadding[POPUP_MENU]!![VERTICAL] = listPadding[DIALOG]!![VERTICAL]
    }
}