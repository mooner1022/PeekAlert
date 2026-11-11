package dev.mooner.peekalert

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import dev.mooner.peekalert.interpolator.CubicBezierInterpolator
import dev.mooner.peekalert.utils.mainHandler
import kotlin.math.max

/**
 * Holds the alerts shown at one edge of a container as a deck, so that several can be up at once without covering
 * the screen: the newest is in front at its full size, and each older one is tucked behind it, a little smaller, with
 * only its far edge showing. The deck is about as tall as one alert however many it holds.
 *
 * A tap on a deck of more than one spreads it out for a while, each alert in full under the one before, so that the
 * ones behind can be read and swiped away; it folds back by itself.
 *
 * There is one host per container and position, added the first time an alert is shown there and kept: alerts come
 * and go inside it. It keeps clear of the status bar, the navigation bar, a display cutout and the keyboard by
 * itself, by how far the container reaches under them, so it doesn't matter whether the window draws edge to edge or
 * whether another view consumed the insets.
 */
@SuppressLint("ViewConstructor")
internal class PeekAlertHost(
    context: Context,
    val position: PeekAlert.Position,
) : FrameLayout(context) {

    private val isTop = position == PeekAlert.Position.Top
    /** The way from the edge into the screen: down for a deck at the top, up for one at the bottom. */
    private val inward = if (isTop) 1 else -1

    private val peek = resources.getDimensionPixelSize(R.dimen.peeker_stack_peek)
    private val gap = resources.getDimensionPixelSize(R.dimen.peeker_stack_gap)
    private val slide = resources.getDimensionPixelSize(R.dimen.peeker_stack_slide)

    private val location = IntArray(2)
    private val onGlobalLayout = ViewTreeObserver.OnGlobalLayoutListener(::fitWindow)

    /** Added and not yet brought in: it comes in from the edge the next time the deck is arranged. */
    private val entering = HashSet<PeekAlertView>()
    /** On their way out: they no longer count as part of the deck. */
    private val leaving = HashSet<PeekAlertView>()

    /** Whether the deck is spread out. */
    var isExpanded = false
        private set
    private val fold = Runnable { setExpanded(false) }

    init {
        // An alert comes in from the screen's edge, across this host's padding, and the ones behind reach past it.
        clipChildren = false
        clipToPadding = false
        visibility = GONE
    }

    /** The alerts in the deck, the newest (the one in front) first. */
    val deck: List<PeekAlertView>
        get() = children.filterIsInstance<PeekAlertView>().filter { it !in leaving }.toList().asReversed()

    /**
     * Puts [row] in front of the deck. Returns the alerts that should leave for the deck to hold no more than [max],
     * the oldest ones.
     */
    fun add(row: PeekAlertView, max: Int): List<PeekAlert> {
        visibility = VISIBLE
        entering += row
        row.alpha = 0f
        addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT, if (isTop) Gravity.TOP else Gravity.BOTTOM))
        fitWindow()
        if (isExpanded)
            touched()
        return deck.mapNotNull(PeekAlertView::owner).filter(PeekAlert::isShowing).drop(max.coerceAtLeast(1))
    }

    /**
     * Sends [row] off toward the edge, from wherever a swipe left it, and calls [onGone] when it is out of the view
     * tree. The rest of the deck closes up meanwhile.
     */
    fun dismiss(row: PeekAlertView, onGone: () -> Unit) {
        leaving += row
        entering -= row
        row.animate().cancel()
        // Not on screen, so there is nothing to animate, and an animation that never runs would never end.
        if (!row.isAttachedToWindow || !row.isLaidOut) {
            remove(row)
            onGone()
            return
        }
        row.animate()
            .translationY(row.translationY - inward * (row.height + row.edgeMargin).toFloat())
            .alpha(0f)
            .setDuration(HIDE_MILLIS)
            .setInterpolator(CubicBezierInterpolator(0.05, 1.0, 1.0, 1.0))
            // Run by the animator between frames. A view must not be taken out while the views are being drawn, as
            // the end of a view animation is: its parent is going through its children just then.
            .withEndAction {
                remove(row)
                onGone()
            }
            .start()
        arrange()
    }

    /** Takes [row] out at once. */
    fun remove(row: PeekAlertView) {
        row.animate().cancel()
        leaving -= row
        entering -= row
        removeView(row)
        if (childCount == 0) {
            mainHandler.removeCallbacks(fold)
            isExpanded = false
            visibility = GONE
        } else {
            arrange()
        }
    }

    /** Spreads the deck out, or folds it back. Alerts don't leave by themselves while it is spread out. */
    fun setExpanded(expanded: Boolean) {
        mainHandler.removeCallbacks(fold)
        val expand = expanded && deck.size > 1
        if (expand)
            mainHandler.postDelayed(fold, EXPANDED_MILLIS)
        if (isExpanded == expand)
            return
        isExpanded = expand
        for (row in deck)
            row.owner?.setPaused(expand)
        requestLayout()
        arrange()
    }

    fun toggleExpanded() = setExpanded(!isExpanded)

    /** The user is doing something with the deck: it stays spread out a while longer. */
    fun touched() {
        if (!isExpanded)
            return
        mainHandler.removeCallbacks(fold)
        mainHandler.postDelayed(fold, EXPANDED_MILLIS)
    }

    /** Puts [row] back where it belongs after a drag that didn't send it off. */
    fun settle(row: PeekAlertView) {
        row.placed = false
        arrange()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        arrange()
    }

    /**
     * Moves every alert to its place in the deck. All of them are laid out at the edge; where each one shows is its
     * translation and scale, so that a change of the deck is one smooth move for each.
     */
    private fun arrange() {
        val rows = deck
        val front = rows.firstOrNull() ?: return
        if (isExpanded && rows.size < 2) {
            mainHandler.removeCallbacks(fold)
            isExpanded = false
            front.owner?.setPaused(false)
        }
        // Not measured yet: the layout pass that follows comes back here.
        if (rows.any { it.height == 0 })
            return

        val margin = front.edgeMargin
        var spread = margin
        rows.forEachIndexed { depth, row ->
            val height = row.height
            val offset: Int
            var scaleX = 1f
            var scaleY = 1f
            if (isExpanded) {
                offset = spread
                spread += height + gap
                row.clipBounds = null
                row.setDepth(0)
            } else {
                // Its far edge shows a little past the front one's, whatever their heights are.
                offset = margin + front.height + depth * peek - height
                scaleY = 1f - SCALE_STEP * depth
                // As wide as the front one, less a step: a wider one would show at its sides.
                scaleX = scaleY * if (depth > 0 && row.cardWidth > 0) front.cardWidth / row.cardWidth.toFloat() else 1f
                // One taller than the front one would stick out at the near edge too.
                row.clipBounds = if (depth > 0 && height > front.height) {
                    if (isTop) Rect(0, height - front.height, row.width, height) else Rect(0, 0, row.width, front.height)
                } else null
                row.setDepth(depth)
            }
            row.dragAllowed = isExpanded || depth == 0
            // Scaled from its far edge, which is the part that shows.
            row.pivotX = row.width / 2f
            row.pivotY = if (isTop) height.toFloat() else 0f
            place(row, inward * offset.toFloat(), scaleX, scaleY, first = rows.size == 1)
        }
    }

    /**
     * As tall as what the deck shows, not as its tallest alert: the alerts are laid out on top of one another and
     * reach past that by their translation, where the parent would cut them off and touches wouldn't reach them.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val rows = deck
        val front = rows.firstOrNull() ?: return
        // Past the deck there is room for the shadow of its last card, which this host's parent would cut off too.
        val extent = front.edgeMargin + rows.maxOf { it.shadowRoom } + if (isExpanded && rows.size > 1)
            rows.sumOf { it.measuredHeight } + (rows.size - 1) * gap
        else
            front.measuredHeight + (rows.size - 1) * peek
        val wanted = extent + paddingTop + paddingBottom
        if (wanted > measuredHeight)
            setMeasuredDimension(measuredWidth, resolveSize(wanted, heightMeasureSpec))
    }

    private fun place(row: PeekAlertView, y: Float, scaleX: Float, scaleY: Float, first: Boolean) {
        val enters = entering.remove(row)
        if (enters) {
            // The first one comes in from the screen's edge; one that joins others, from a short way off.
            val distance = if (first) row.height + row.edgeMargin else slide
            row.translationY = y - inward * distance
            row.scaleX = scaleX
            row.scaleY = scaleY
        } else if (row.placed && row.targetY == y && row.targetScaleX == scaleX && row.targetScaleY == scaleY || row.isDragged) {
            return
        }
        row.placed = true
        row.targetY = y
        row.targetScaleX = scaleX
        row.targetScaleY = scaleY
        row.animate()
            .translationY(y)
            .scaleX(scaleX)
            .scaleY(scaleY)
            .alpha(1f)
            .setDuration(if (enters) SHOW_MILLIS else MOVE_MILLIS)
            .setInterpolator(CubicBezierInterpolator(0.0, 0.0, 0.05, 1.0))
            .withEndAction(null)
            .start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayout)
        fitWindow()
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayout)
        mainHandler.removeCallbacks(fold)
        super.onDetachedFromWindow()
    }

    /**
     * Pads this host by as much as its container lies under the system bars, a cutout or the keyboard. The container's
     * place in the window is compared with the window's insets, which are whole whoever consumed them on the way down.
     */
    private fun fitWindow() {
        val container = parent as? View ?: return
        if (visibility == GONE || !isAttachedToWindow)
            return
        val insets = ViewCompat.getRootWindowInsets(this)
            ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            ?: return
        val root = rootView
        container.getLocationInWindow(location)
        val left   = max(0, insets.left - location[0])
        val top    = max(0, insets.top - location[1])
        val right  = max(0, location[0] + container.width - (root.width - insets.right))
        val bottom = max(0, location[1] + container.height - (root.height - insets.bottom))
        val newTop = if (isTop) top else 0
        val newBottom = if (isTop) 0 else bottom
        if (paddingLeft != left || paddingTop != newTop || paddingRight != right || paddingBottom != newBottom)
            setPadding(left, newTop, right, newBottom)
    }

    companion object {

        private const val SHOW_MILLIS = 300L
        private const val HIDE_MILLIS = 200L
        private const val MOVE_MILLIS = 250L
        /** How long the deck stays spread out after the last touch. */
        private const val EXPANDED_MILLIS = 4000L
        /** How much smaller each alert behind the front one is than the one before it. */
        private const val SCALE_STEP = 0.05f

        /** The host for [position] in [container], added to it if this is the first alert shown there. */
        fun obtain(container: ViewGroup, position: PeekAlert.Position): PeekAlertHost {
            container.children
                .filterIsInstance<PeekAlertHost>()
                .firstOrNull { it.position == position }
                ?.let { return it }
            return PeekAlertHost(container.context, position).also { host ->
                container.addView(host, paramsFor(container, position == PeekAlert.Position.Top))
            }
        }

        /** Across the container's width at its top or bottom, for the kinds of container that can place a view there. */
        private fun paramsFor(container: ViewGroup, top: Boolean): ViewGroup.LayoutParams {
            val gravity = if (top) Gravity.TOP else Gravity.BOTTOM
            return when (container) {
                is FrameLayout ->
                    LayoutParams(MATCH_PARENT, WRAP_CONTENT, gravity)
                is CoordinatorLayout ->
                    CoordinatorLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also { it.gravity = gravity }
                is ConstraintLayout ->
                    ConstraintLayout.LayoutParams(0, WRAP_CONTENT).also {
                        it.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        it.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        if (top)
                            it.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        else
                            it.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                is RelativeLayout ->
                    RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                        it.addRule(if (top) RelativeLayout.ALIGN_PARENT_TOP else RelativeLayout.ALIGN_PARENT_BOTTOM)
                    }
                else ->
                    ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }
        }
    }
}
