/**
 * PeekAlert created by Minki Moon(mooner1022)
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the MIT License.
 *
 * @author mooner1022
 */

package dev.mooner.peekalert

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.accessibility.AccessibilityManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.fragment.app.Fragment
import dev.mooner.peekalert.utils.dp
import dev.mooner.peekalert.utils.mainHandler
import dev.mooner.peekalert.utils.onMainThread

typealias PeekActionListener = (view: View) -> Unit

/** Called when an alert left the screen, however it did: by itself, by a swipe or a tap, by [PeekAlert.hide]. */
typealias PeekDismissListener = () -> Unit

/**
 * An alert that slides in at the top or the bottom of a window, or of a view, and leaves by itself, by a swipe or
 * when asked to.
 *
 * Several can be shown at once. At the same position they form a deck: the newest in front, the older ones tucked
 * behind it with their edges showing, up to [maxVisible]; a tap spreads the deck out for a while so each can be read.
 * Each alert is its own: showing or hiding one leaves the others as they are. An alert keeps clear of the status
 * bar, the navigation bar, a display cutout and the keyboard.
 *
 * [peek] and [hide] may be called from any thread.
 */
class PeekAlert internal constructor(
    private val parentView: ViewGroup,
    // The top inset used to be skipped in multi-window mode; where the alert goes is now measured, see PeekAlertHost.
    @Suppress("unused") private val isMultiWindow: Boolean
) {
    private enum class State { IDLE, SHOWING, HIDING }

    private var state: State = State.IDLE
    /** The view on screen while the alert is showing or leaving. */
    private var instance: PeekAlertView? = null
    /** Its deck is spread out for the user to read: the alert stays until the deck folds back. */
    private var paused = false
    private val autoHide = Runnable(::hide)
    private var onDismiss: PeekDismissListener? = null

    private var position   : Position = Position.Bottom
    private var radius     : Float? = null
    private var width      : Int = WRAP_CONTENT
    private var bgColor    : Int? = null
    private var paddingPx  : Int? = null
    private var margin     : Pair<Int?, Int?> = Pair(null, null)
    private val titleConfig: TextConfig by lazy { TextConfig() }
    private val textConfig : TextConfig by lazy { TextConfig() }
    private var iconRes    : Int? = R.drawable.round_info_24
    private var iconTint   : Int? = null
    private var draggable  : Boolean = false
    private var hideOnTouch: Boolean = false
    private var autoHideMillis: Long? = null
    private val actionConfig  : ActionConfig by lazy { ActionConfig() }
    private var strokeColor   : Int? = null
    private var strokeWidthDp : Float = 1f
    private var elevationDp   : Float? = null
    private var iconSizeDp    : Int? = null

    /**
     * Set the position of alert to be shown
     * @param position : Position of alert
     * @see Position
     */
    fun setPosition(position: Position): PeekAlert {
        this.position = position
        return this
    }

    /**
     * Set the background color of alert
     * @param res : Color resource
     * @param color : Color int
     */
    @JvmOverloads
    fun setBackgroundColor(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        this.bgColor = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set a line around the alert, which sets it apart from what is behind it where their colors are close.
     * With neither [res] nor [color], the alert has no line.
     * @param res : Color resource
     * @param color : Color int
     * @param widthDp : How wide the line is, in dp
     */
    @JvmOverloads
    fun setStroke(@ColorRes res: Int? = null, @ColorInt color: Int? = null, widthDp: Float = 1f): PeekAlert {
        this.strokeColor = res?.let(parentView.context::getColor) ?: color
        this.strokeWidthDp = widthDp
        return this
    }

    /**
     * Set how high the alert is above what is behind it, which is how large a shadow it casts; none by default.
     * Only the alert in front of a deck casts its shadow.
     * @param dp : The elevation in dp
     */
    fun setElevationDp(@Dimension(unit = Dimension.DP) dp: Float): PeekAlert {
        this.elevationDp = dp
        return this
    }

    /**
     * Set corner radius of alert
     * @param radius : radius of corner, by px
     */
    fun setCornerRadius(@Px radius: Float): PeekAlert {
        this.radius = radius
        return this
    }

    /**
     * Set width of alert view
     * Should be set as one of MATCH_PARENT or WRAP_CONTENT, unless you have a particular purpose
     * @param width : width of view
     */
    fun setWidth(width: Int): PeekAlert {
        this.width = width
        return this
    }

    /**
     * Set title of alert
     * @param title : The desired content of title text, or null for an alert with no title
     */
    fun setTitle(title: String?): PeekAlert {
        titleConfig.content = title
        return this
    }

    /**
     * Set size of title text
     * @param size : The scaled pixel size
     */
    fun setTitleSize(size: Float): PeekAlert {
        titleConfig.size = size
        return this
    }

    /**
     * Set color of title text
     * @param res : Resource value of color
     * @param color : Int value of color
     */
    @JvmOverloads
    fun setTitleColor(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        titleConfig.color = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set typeface of title text
     * @param typeface : The desired typeface of title
     */
    fun setTitleTypeface(typeface: Typeface): PeekAlert {
        titleConfig.typeface = typeface
        return this
    }

    /**
     * Set text of alert
     * @param text : The desired content of text
     */
    fun setText(text: String): PeekAlert {
        textConfig.content = text
        return this
    }

    /**
     * Set size of text
     * @param size : The scaled pixel size
     */
    fun setTextSize(size: Float): PeekAlert {
        textConfig.size = size
        return this
    }

    /**
     * Set color of text
     * @param res : Resource value of color
     * @param color : Int value of color
     */
    @JvmOverloads
    fun setTextColor(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        textConfig.color = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set typeface of text
     * @param typeface : The desired typeface of text
     */
    fun setTextTypeface(typeface: Typeface): PeekAlert {
        textConfig.typeface = typeface
        return this
    }

    /**
     * Set icon of alert
     * @param res : Drawable resource of desired icon content
     */
    fun setIcon(@DrawableRes res: Int?): PeekAlert {
        this.iconRes = res
        return this
    }

    /**
     * Set tint color of icon
     * @param res : Resource value of color
     * @param color : Int value of color
     */
    @JvmOverloads
    fun setIconTint(@ColorRes res: Int? = null, @ColorInt color: Int? = null): PeekAlert {
        this.iconTint = res?.let(parentView.context::getColor) ?: color
        return this
    }

    /**
     * Set the width and height of the icon
     * @param dp : The size in dp, or null for the size of the icon's drawable
     */
    fun setIconSizeDp(@Dimension(unit = Dimension.DP) dp: Int?): PeekAlert {
        this.iconSizeDp = dp
        return this
    }

    /**
     * Set padding of alert, in px
     * @param px : The desired padding in px
     */
    fun setPaddingPx(@Px px: Int): PeekAlert {
        this.paddingPx = px
        return this
    }

    /**
     * Set padding of alert, in dp
     * @param dp : The desired padding in dp
     */
    fun setPaddingDp(dp: Int): PeekAlert =
        setPaddingPx(dp.dp)

    /**
     * Set horizontal, vertical margin of alert
     * @param horizontal : The space kept at each side of the alert, in dp
     * @param vertical : The space kept from the edge of the screen the alert is shown at, in dp
     */
    fun setMargin(
        @Dimension(unit = Dimension.DP) horizontal: Int? = null,
        @Dimension(unit = Dimension.DP) vertical: Int? = null
    ): PeekAlert {
        margin = Pair(horizontal ?: margin.first, vertical ?: margin.second)
        return this
    }

    /**
     * Set the most lines of the title shown before it is cut with an ellipsis; one by default
     */
    fun setTitleMaxLines(maxLines: Int): PeekAlert {
        titleConfig.maxLines = maxLines
        return this
    }

    /**
     * Set the most lines of the text shown before it is cut with an ellipsis; four by default
     */
    fun setTextMaxLines(maxLines: Int): PeekAlert {
        textConfig.maxLines = maxLines
        return this
    }

    /**
     * Set a listener called each time the alert has left the screen, or null for none
     */
    fun setOnDismissListener(listener: PeekDismissListener?): PeekAlert {
        onDismiss = listener
        return this
    }

    /**
     * Set alert to be dragged and dismissed by user
     */
    fun setDraggable(draggable: Boolean): PeekAlert {
        this.draggable = draggable
        return this
    }

    /**
     * Set alert to be hidden on user touch
     */
    fun setHideOnTouch(hide: Boolean): PeekAlert {
        this.hideOnTouch = hide
        return this
    }

    /**
     * Set alert to be automatically hidden after 3000ms
     * @see setAutoHide(Long)
     */
    fun setAutoHide(hide: Boolean): PeekAlert {
        this.autoHideMillis =
            if (hide)
                3000L
            else
                null
        return this
    }

    /**
     * Set alert to be automatically hidden after the given delay
     * @param delay : The desired delay in milliseconds
     * @see setAutoHide(Boolean)
     */
    fun setAutoHide(delay: Long = 3000L): PeekAlert {
        this.autoHideMillis = delay
        return this
    }

    /**
     * Set a user-clickable button shown at right-end of alert
     * @param config : The desired action config
     * @see setAction
     */
    fun setAction(config: ActionConfig): PeekAlert {
        actionConfig.text             = config.text
        actionConfig.backgroundColor  = config.backgroundColor
        actionConfig.textColor        = config.textColor
        actionConfig.onActionListener = config.onActionListener
        actionConfig.textSize         = config.textSize
        actionConfig.typeface         = config.typeface
        return this
    }

    /**
     * Set a user-clickable button shown at right-end of alert
     * @param text : The desired text of action button
     * @param backgroundColorRes : The desired background color of action, as Resource value
     * @param backgroundColorInt : The desired background color of action, as Color Int value
     * @param textColorRes : The desired text color of action, as Resource value
     * @param textColorInt : The desired text color of action, as Color Int value
     * @param onClick : Callback listener called on user click action
     * @see setAction
     */
    @JvmOverloads
    fun setAction(
        text: String?,
        @ColorRes
        backgroundColorRes: Int? = null,
        @ColorInt
        backgroundColorInt: Int? = null,
        @ColorRes
        textColorRes: Int? = null,
        @ColorInt
        textColorInt: Int? = null,
        onClick: PeekActionListener
    ): PeekAlert {
        setAction(
            ActionConfig(
            text = text,
            backgroundColor = backgroundColorRes
                ?.let(parentView.context::getColor)
                ?: backgroundColorInt,
            textColor = textColorRes
                ?.let(parentView.context::getColor)
                ?: textColorInt,
            onActionListener = onClick
        ))
        return this
    }

    /** Whether the alert is on screen and not on its way out. */
    val isShowing: Boolean
        get() = state == State.SHOWING

    /**
     * Show the alert on screen, with settings set before. One that is showing already is shown anew, in place of
     * itself; other alerts stay.
     * @see hide
     */
    fun peek() = onMainThread(::show)

    /**
     * Hide the alert if it is being shown
     * @see peek
     */
    fun hide() = onMainThread(::leave)

    private fun show() {
        // Shown anew in its own place. If it was on its way out, that is where it has now got to.
        instance?.let { detach(it, notify = state == State.HIDING) }

        val context = parentView.context
        val view = PeekAlertView(context, width)
        configure(view)
        val host = PeekAlertHost.obtain(parentView, position)

        view.owner = this
        view.onDismissRequest = ::leave
        view.onSettle = { host.settle(view) }
        // A tap hides an alert set to hide on touch; otherwise it spreads out a deck of several, or folds it back.
        view.onTap = {
            if (hideOnTouch)
                leave()
            else
                host.toggleExpanded()
        }
        // The alert doesn't leave from under a finger: its time starts over when the finger lifts.
        view.onTouchingChanged = { touching ->
            host.touched()
            mainHandler.removeCallbacks(autoHide)
            if (!touching && state == State.SHOWING)
                scheduleAutoHide()
        }
        // The window went away with the alert in it: nothing is left to hide.
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                if (instance === view)
                    forget()
            }
        })

        instance = view
        state = State.SHOWING
        paused = host.isExpanded
        val overflow = host.add(view, maxVisible)
        scheduleAutoHide()

        for (alert in overflow)
            if (alert !== this)
                alert.leave()
    }

    private fun configure(view: PeekAlertView) {
        val context = parentView.context
        val (marginHoriz, marginVert) = margin

        marginVert?.let { view.edgeMargin = it.dp }

        if (titleConfig.content != null)
            view.setTitle(titleConfig)

        if (textConfig.content != null)
            view.setText(textConfig)

        paddingPx?.let(view::setPadding)

        view.setIconSize(iconSizeDp?.dp)
        view.setIcon(iconRes, iconTint)

        if (actionConfig.text != null)
            view.setAction(actionConfig)

        view.setDraggable(draggable, position)

        radius?.let(view::setRadius)

        bgColor?.let(view::setBackgroundColor)

        val density = context.resources.displayMetrics.density
        view.setStroke(strokeColor, (strokeWidthDp * density).toInt().coerceAtLeast(1))
        elevationDp?.let { view.setCardElevation(it * density) }

        marginHoriz?.let { view.setMargin(it.dp) }
    }

    /** Holds the alert's time to leave by itself, while its deck is spread out, or lets it start over. */
    internal fun setPaused(paused: Boolean) {
        this.paused = paused
        if (paused)
            mainHandler.removeCallbacks(autoHide)
        else if (state == State.SHOWING)
            scheduleAutoHide()
    }

    private fun scheduleAutoHide() {
        mainHandler.removeCallbacks(autoHide)
        val millis = autoHideMillis ?: return
        if (paused)
            return
        mainHandler.postDelayed(autoHide, readableTimeout(parentView.context, millis))
    }

    /** [millis], or longer where the user asked for more time to read and act, in the system's accessibility settings. */
    private fun readableTimeout(context: Context, millis: Long): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return millis
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return millis
        var flags = AccessibilityManager.FLAG_CONTENT_ICONS or AccessibilityManager.FLAG_CONTENT_TEXT
        if (actionConfig.text != null)
            flags = flags or AccessibilityManager.FLAG_CONTENT_CONTROLS
        return manager.getRecommendedTimeoutMillis(millis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), flags).toLong()
    }

    private fun leave() {
        val view = instance ?: return
        if (state != State.SHOWING)
            return
        state = State.HIDING
        mainHandler.removeCallbacks(autoHide)
        val host = view.parent as? PeekAlertHost
        if (host == null) {
            detach(view, notify = true)
            return
        }
        host.dismiss(view) { finish(view, notify = true) }
    }

    /** Takes [view] off the screen at once. */
    private fun detach(view: PeekAlertView, notify: Boolean) {
        (view.parent as? PeekAlertHost)?.remove(view)
            ?: (view.parent as? ViewGroup)?.removeView(view)
        finish(view, notify)
    }

    /** [view] is out of the view tree. */
    private fun finish(view: PeekAlertView, notify: Boolean) {
        view.onDismissRequest = null
        view.onSettle = null
        view.onTap = null
        view.onTouchingChanged = null
        view.owner = null
        if (instance === view)
            forget()
        if (notify)
            onDismiss?.invoke()
    }

    private fun forget() {
        mainHandler.removeCallbacks(autoHide)
        instance = null
        state = State.IDLE
        paused = false
    }

    sealed class Position {
        object Top   : Position()
        object Bottom: Position()
    }

    companion object {

        /**
         * The most alerts in the deck at one position of one window or view. One more makes the oldest leave. At
         * least 1.
         */
        @JvmStatic
        @Volatile
        var maxVisible: Int = 3

        /**
         * An alert shown in [view]. A `FrameLayout`, `CoordinatorLayout`, `ConstraintLayout` or `RelativeLayout` can
         * hold it at its top or bottom; in another kind of group it goes where that group puts a new child.
         */
        @JvmStatic
        fun create(view: ViewGroup): PeekAlert =
            PeekAlert(view, false)

        @JvmStatic
        fun create(activity: Activity): PeekAlert =
            PeekAlert(
                activity.window.decorView as ViewGroup,
                activity.isInMultiWindowMode
            )

        @JvmStatic
        fun create(fragment: Fragment): PeekAlert =
            create(fragment.requireActivity())

        /** An alert shown in [window], such as a dialog's: one shown in the activity would be behind the dialog. */
        @JvmStatic
        fun create(window: Window): PeekAlert =
            PeekAlert(window.decorView as ViewGroup, false)

        /** An alert shown over [dialog], which must have been created or shown, so that it has a window. */
        @JvmStatic
        fun create(dialog: Dialog): PeekAlert =
            create(requireNotNull(dialog.window) { "The dialog has no window" })
    }
}
