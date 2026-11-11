package dev.mooner.peekalert

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import dev.mooner.peekalert.databinding.LayoutAlertBinding
import dev.mooner.peekalert.utils.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * One alert as it is on screen: a row as wide as its host, with the card in it. Only the card takes touches, so what
 * is beside it stays usable.
 */
class PeekAlertView(
    context: Context,
    width: Int
): ConstraintLayout(context) {

    constructor(context: Context):
            this(context, WRAP_CONTENT)

    private val binding: LayoutAlertBinding = LayoutAlertBinding.inflate(LayoutInflater.from(context), this)

    private var draggable = false
    private var position: PeekAlert.Position = PeekAlert.Position.Bottom

    /** The alert that shows this view, for the host to ask it to leave when too many are up. */
    internal var owner: PeekAlert? = null

    /** The space this alert keeps from the screen's edge while it is the one in front, in px. */
    internal var edgeMargin: Int = resources.getDimensionPixelSize(R.dimen.peeker_margin_edge)

    /** The user swiped the alert away. */
    internal var onDismissRequest: (() -> Unit)? = null

    /** The user tapped the alert, or an accessibility service clicked it. */
    internal var onTap: (() -> Unit)? = null

    /** A drag ended without sending the alert off: it goes back to its place. */
    internal var onSettle: (() -> Unit)? = null

    /** A finger went down on the alert, or left it. */
    internal var onTouchingChanged: ((touching: Boolean) -> Unit)? = null

    // Kept for the host, which places the alert in its deck.
    /** Where the host last sent this alert, and whether it has sent it anywhere yet. */
    internal var targetY = 0f
    internal var targetScaleX = 1f
    internal var targetScaleY = 1f
    internal var placed = false
    /** Whether a swipe may move it: only the one in front, or any one while the deck is spread out. */
    internal var dragAllowed = true
    /** Under a finger that has moved it: the host leaves it where the finger put it. */
    internal var isDragged = false
        private set
    private var shownDepth = 0

    // How the card is set apart from what is behind it, and the size of its icon.
    private var strokeColor: Int? = null
    private var strokeWidth = 0
    private var elevation = 0f
    private var iconSize: Int? = null
    private var iconPadding = resources.getDimensionPixelSize(R.dimen.peeker_padding_default)

    init {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val horizontal = resources.getDimensionPixelSize(R.dimen.peeker_margin_horizontal)
        setPadding(horizontal, 0, horizontal, 0)
        clipChildren = false
        clipToPadding = false
        if (width != WRAP_CONTENT) {
            binding.cardBackground.updateLayoutParams {
                this.width = width
            }
        }
        // Read out when it appears, without taking the focus from what the user is doing.
        ViewCompat.setAccessibilityLiveRegion(binding.cardBackground, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        updateTouch()
    }

    /** How wide the card is, which is less than this row unless the alert was made as wide as its parent. */
    internal val cardWidth: Int
        get() = binding.cardBackground.width

    /**
     * How far behind the front of its deck the alert is: 0 for the front one. One behind shows as a blank card, only
     * its edge in sight: what it says would be cut off there. It is darker than the one in front, each more than the
     * one before, as things further back are, and outlined, so that its edge tells apart from the one in front and
     * from what is behind it whatever their colors. Only the front one casts a shadow: three would be a smudge.
     */
    internal fun setDepth(depth: Int) {
        if (depth == shownDepth)
            return
        shownDepth = depth
        updateSurface()
        val content = binding.cardBackground.getChildAt(0) ?: return
        val alpha = if (depth == 0) 1f else 0f
        if (content.alpha != alpha)
            content.animate().alpha(alpha).setDuration(CONTENT_FADE_MILLIS).start()
    }

    /**
     * The card's outline, shadow and shade are one thing to draw, set here from the stroke, the elevation and how far
     * back the alert is. The shade goes over the outline, so one behind is darker as a whole.
     */
    private fun updateSurface() {
        val card = binding.cardBackground
        val depth = shownDepth
        val outline: Drawable? = strokeColor?.takeIf { strokeWidth > 0 }?.let { color ->
            GradientDrawable().apply {
                cornerRadius = card.radius
                setColor(Color.TRANSPARENT)
                setStroke(strokeWidth, color)
            }
        }
        val shade: Drawable? = if (depth == 0) null else {
            GradientDrawable().apply {
                cornerRadius = card.radius
                setColor(Color.argb(depth.coerceAtMost(MAX_SHADED_DEPTH) * SHADE_STEP, 0, 0, 0))
                setStroke(resources.displayMetrics.density.toInt().coerceAtLeast(1), Color.argb(OUTLINE_ALPHA, 0, 0, 0))
            }
        }
        card.foreground = when {
            outline != null && shade != null -> LayerDrawable(arrayOf(outline, shade))
            else                             -> outline ?: shade
        }
        card.cardElevation = if (depth == 0) elevation else 0f
    }

    /** A line around the card, [width] px wide, or none with a null [color]. */
    fun setStroke(@ColorInt color: Int?, @Px width: Int) {
        strokeColor = color
        strokeWidth = width
        updateSurface()
    }

    /** How high above what is behind it the card is, which is how large a shadow it casts, in px. */
    fun setCardElevation(@Px elevation: Float) {
        this.elevation = elevation
        updateSurface()
    }

    /** How much room past the card its shadow takes, for the host to leave it. */
    internal val shadowRoom: Int
        get() = (elevation * SHADOW_REACH).toInt()

    /** The icon's width and height in px, or its own size with null. */
    fun setIconSize(@Px size: Int?) {
        iconSize = size
        updateIcon()
    }

    /** The icon's view is the icon and the card's padding around it, so the card is as tall as both. */
    private fun updateIcon() {
        val icon = binding.icon
        val visible = icon.visibility == VISIBLE
        icon.updateLayoutParams<LinearLayout.LayoutParams> {
            val side = iconSize?.let { it + iconPadding * 2 }
            width = if (!visible) 16.dp else side ?: WRAP_CONTENT
            height = side ?: WRAP_CONTENT
        }
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        icon.setPadding(iconPadding)
    }

    fun setTitle(config: TextConfig) {
        binding.title.apply {
            visibility = View.VISIBLE
            text = config.content
            config.size?.let(::setTextSize)
            config.color?.let(::setTextColor)
            config.typeface?.let(::setTypeface)
            config.maxLines?.let(::setMaxLines)
        }
    }

    fun setText(config: TextConfig) {
        binding.content.apply {
            text = config.content
            config.size?.let(::setTextSize)
            config.color?.let(::setTextColor)
            config.typeface?.let(::setTypeface)
            config.maxLines?.let(::setMaxLines)
        }
    }

    fun setIcon(@DrawableRes res: Int?, @ColorInt tint: Int?) {
        binding.icon.apply {
            if (res != null) {
                visibility = VISIBLE
                setImageResource(res)
                if (tint != null) {
                    imageTintList = ColorStateList.valueOf(tint)
                }
            } else {
                visibility = INVISIBLE
            }
        }
        updateIcon()
    }

    override fun setBackgroundColor(@ColorInt color: Int) {
        binding.cardBackground.setCardBackgroundColor(color)
    }

    fun setPadding(@Px px: Int) {
        iconPadding = px
        updateIcon()
        binding.cardAction.updateLayoutParams<LinearLayout.LayoutParams> {
            marginEnd = px
        }
        binding.content.updateLayoutParams<LinearLayout.LayoutParams> {
            marginEnd = px
        }
    }

    fun setMargin(@Px horizontal: Int) {
        updatePadding(left = horizontal, right = horizontal)
    }

    fun setRadius(@Px radius: Float) {
        binding.cardBackground.radius = radius
        updateSurface()
    }

    fun setDraggable(draggable: Boolean, position: PeekAlert.Position) {
        this.draggable = draggable
        this.position = position
    }

    /**
     * The button at the alert's end. With no color or typeface of its own it takes those of the alert's title, or of
     * its text where it has no title, and is on a faint wash of that color: it goes with whatever the card looks like.
     */
    fun setAction(config: ActionConfig) {
        val model = if (binding.title.visibility == View.VISIBLE) binding.title else binding.content
        val textColor = config.textColor ?: model.currentTextColor
        binding.cardAction.apply {
            visibility = View.VISIBLE
            setCardBackgroundColor(config.backgroundColor ?: washOf(textColor))
            // As round as it can be: a pill.
            radius = resources.displayMetrics.density * ACTION_RADIUS_DP
            setOnClickListener(config.onActionListener)
        }
        binding.actionText.apply {
            text = config.text
            setTextColor(textColor)
            config.textSize?.let { setTextSize(TypedValue.COMPLEX_UNIT_SP, it) }
            typeface = config.typeface ?: model.typeface
        }
    }

    private fun washOf(@ColorInt color: Int): Int =
        Color.argb(ACTION_WASH_ALPHA, Color.red(color), Color.green(color), Color.blue(color))

    // A fade would otherwise go through a layer the size of this row, which cuts the card's shadow off until the
    // fade ends. Nothing in a row is drawn over something else that shows through it, so it needs no such layer.
    override fun hasOverlappingRendering(): Boolean = false

    /** Which way the alert leaves the screen: up for one at the top, down for one at the bottom. */
    internal val exitDirection: Int
        get() = if (position == PeekAlert.Position.Top) -1 else 1

    @SuppressLint("ClickableViewAccessibility")
    private fun updateTouch() {
        val card = binding.cardBackground
        // A click is what an accessibility service does, and what a touch that didn't move ends in.
        card.setOnClickListener { onTap?.invoke() }
        card.setOnTouchListener(CardTouch())
    }

    /**
     * Drags the row toward the edge it came from and lets it go there past half its height; a touch that didn't move
     * is a tap. It works on the row's translation, which is also how the host places the row in its deck.
     */
    private inner class CardTouch : OnTouchListener {

        private val slop = ViewConfiguration.get(context).scaledTouchSlop
        private var downY = 0f
        private var startTranslation = 0f
        private var dragging = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                    startTranslation = translationY
                    dragging = false
                    onTouchingChanged?.invoke(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!draggable || !dragAllowed)
                        return true
                    val moved = event.rawY - downY
                    if (!dragging && abs(moved) > slop) {
                        dragging = true
                        isDragged = true
                    }
                    if (dragging) {
                        val target = startTranslation + moved
                        // Only toward its edge: pulled the other way, it stays where it is.
                        translationY = if (exitDirection < 0) min(target, startTranslation) else max(target, startTranslation)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val wasDragging = dragging
                    dragging = false
                    isDragged = false
                    onTouchingChanged?.invoke(false)
                    when {
                        wasDragging && abs(translationY - startTranslation) > view.height / 2f -> onDismissRequest?.invoke()
                        wasDragging                                                           -> onSettle?.invoke()
                        else                                                                  -> view.performClick()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    isDragged = false
                    onTouchingChanged?.invoke(false)
                    onSettle?.invoke()
                }
            }
            return true
        }
    }

    private companion object {
        /** The black laid over an alert for each step it is behind the front one, of 255. */
        const val SHADE_STEP = 0x26
        const val OUTLINE_ALPHA = 0x30
        const val MAX_SHADED_DEPTH = 3
        const val CONTENT_FADE_MILLIS = 150L
        /** How far a shadow reaches past its card, as a multiple of the card's elevation. */
        const val SHADOW_REACH = 2.5f
        const val ACTION_RADIUS_DP = 100f
        /** How strong the wash behind an action's text is, of 255. */
        const val ACTION_WASH_ALPHA = 0x1F
    }
}
