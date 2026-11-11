package dev.mooner.peekalert

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment

private typealias BuilderBlock = PeekAlertBuilder.() -> Unit

@DslMarker
internal annotation class BuilderDsl
@DslMarker
internal annotation class InnerBuilderDsl

@BuilderDsl
class PeekAlertBuilder(
    private val parentView: ViewGroup,
    isMultiWindow: Boolean
) {
    private var alert = PeekAlert(parentView, isMultiWindow)

    var position    : PeekAlert.Position = PeekAlert.Position.Bottom
    var cornerRadius: Float? = null
    var width       : Int = ViewGroup.LayoutParams.WRAP_CONTENT
    var paddingPx   : Int? = null
    var paddingDp   : Int? = null
    var margin      : Pair<Int?, Int?> = Pair(null, null)
    @DrawableRes
    var iconRes     : Int? = null
    @ColorInt
    var iconTint    : Int? = null
    var draggable   : Boolean = false
    var hideOnTouch : Boolean = false
    var autoHideMillis : Long? = null
    /** How high the alert is above what is behind it, in dp, which is how large a shadow it casts; none by default. */
    var elevationDp : Float? = null
    /** The icon's width and height in dp; the size of its drawable by default. */
    var iconSizeDp  : Int? = null
    private var bgColor: Int? = null
    private var strokeColor: Int? = null
    private var strokeWidthDp: Float = 1f
    private var onDismiss: PeekDismissListener? = null

    /** Called each time the alert has left the screen. */
    fun onDismiss(listener: PeekDismissListener) {
        onDismiss = listener
    }

    fun backgroundColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
        bgColor = res?.let(parentView.context::getColor) ?: value
    }

    fun iconTint(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
        iconTint = res?.let(parentView.context::getColor) ?: value
    }

    /** A line around the alert, which sets it apart from what is behind it where their colors are close. */
    fun stroke(@ColorRes res: Int? = null, @ColorInt value: Int? = null, widthDp: Float = 1f) {
        strokeColor = res?.let(parentView.context::getColor) ?: value
        strokeWidthDp = widthDp
    }

    @InnerBuilderDsl
    fun title(content: String, block: TitleBuilder.() -> Unit = {}) {
        TitleBuilder(content, parentView.context).apply(block).build(alert)
    }

    @InnerBuilderDsl
    fun text(content: String, block: TextBuilder.() -> Unit = {}) {
        TextBuilder(content, parentView.context).apply(block).build(alert)
    }

    @InnerBuilderDsl
    fun action(text: String, block: ActionBuilder.() -> Unit) {
        ActionBuilder(text, parentView.context).apply(block).build(alert)
    }

    fun hide() {
        alert.hide()
    }

    class TitleBuilder(
        private val content: String,
        private val context: Context,
    ) {
        var textSize  : Float? = null
        var typeface  : Typeface? = null
        /** The most lines shown before the title is cut with an ellipsis; one by default. */
        var maxLines  : Int? = null
        private var textColor: Int? = null

        fun textColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
            textColor = res?.let(context::getColor) ?: value
        }

        internal fun build(peekAlert: PeekAlert) {
            peekAlert.setTitle(content)
            textSize?.let(peekAlert::setTitleSize)
            textColor?.let { peekAlert.setTitleColor(color = it) }
            typeface?.let(peekAlert::setTitleTypeface)
            maxLines?.let(peekAlert::setTitleMaxLines)
        }
    }

    class TextBuilder(
        private val content: String,
        private val context: Context,
    ) {
        var textSize  : Float? = null
        var typeface  : Typeface? = null
        /** The most lines shown before the text is cut with an ellipsis; four by default. */
        var maxLines  : Int? = null
        private var textColor: Int? = null

        fun textColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
            textColor = res?.let(context::getColor) ?: value
        }

        internal fun build(peekAlert: PeekAlert) {
            peekAlert.setText(content)
            textSize?.let(peekAlert::setTextSize)
            textColor?.let { peekAlert.setTextColor(color = it) }
            typeface?.let(peekAlert::setTextTypeface)
            maxLines?.let(peekAlert::setTextMaxLines)
        }
    }

    class ActionBuilder(
        private val text: String,
        private val context: Context,
    ) {
        /** The size of the button's text in sp. */
        var textSize  : Float? = null
        var typeface  : Typeface? = null
        private var backgroundColor : Int? = null
        private var textColor       : Int? = null
        private var onActionListener: PeekActionListener? = null

        fun backgroundColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
            backgroundColor = res?.let(context::getColor) ?: value
        }

        fun textColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
            textColor = res?.let(context::getColor) ?: value
        }

        fun setOnActionListener(listener: PeekActionListener) {
            onActionListener = listener
        }

        internal fun build(peekAlert: PeekAlert) {
            peekAlert.setAction(ActionConfig(text, backgroundColor, textColor, onActionListener, textSize, typeface))
        }
    }

    fun build(): PeekAlert {
        alert.setPosition(position)
        cornerRadius?.let(alert::setCornerRadius)
        alert.setWidth(width)
        paddingPx?.let(alert::setPaddingPx) ?: paddingDp?.let(alert::setPaddingDp)
        alert.setMargin(margin.first, margin.second)
        alert.setIcon(iconRes)
        iconTint?.let { alert.setIconTint(color = it) }
        alert.setDraggable(draggable)
        alert.setHideOnTouch(hideOnTouch)
        autoHideMillis?.let(alert::setAutoHide)
        bgColor?.let { alert.setBackgroundColor(color = it) }
        strokeColor?.let { alert.setStroke(color = it, widthDp = strokeWidthDp) }
        elevationDp?.let(alert::setElevationDp)
        iconSizeDp?.let(alert::setIconSizeDp)
        onDismiss?.let(alert::setOnDismissListener)
        return alert
    }
}

@JvmSynthetic
fun createPeekAlert(viewGroup: ViewGroup, block: BuilderBlock) =
    createBuilder(viewGroup, block).build()

@JvmSynthetic
fun createPeekAlert(activity: Activity, block: BuilderBlock) =
    createBuilder(activity, block).build()

@JvmSynthetic
fun createPeekAlert(fragment: Fragment, block: BuilderBlock) =
    createBuilder(fragment, block).build()

/** An alert shown in [window], such as a dialog's: one shown in the activity would be behind the dialog. */
@JvmSynthetic
fun createPeekAlert(window: Window, block: BuilderBlock) =
    createBuilder(window.decorView as ViewGroup, block).build()

/** An alert shown over [dialog], which must have been created or shown, so that it has a window. */
@JvmSynthetic
fun createPeekAlert(dialog: Dialog, block: BuilderBlock) =
    createPeekAlert(requireNotNull(dialog.window) { "The dialog has no window" }, block)

private fun createBuilder(
    parentView: Any,
    block: BuilderBlock
): PeekAlertBuilder {
    return when (parentView) {
        is ViewGroup ->
            PeekAlertBuilder(parentView, false).apply(block)
        is Activity ->
            PeekAlertBuilder(
                parentView.window.decorView as ViewGroup,
                parentView.isInMultiWindowMode
            ).apply(block)
        is Fragment ->
            createBuilder(parentView.requireActivity(), block)
        else -> error("Unintended parameter type")
    }
}