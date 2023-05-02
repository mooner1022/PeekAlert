package dev.mooner.peekalert

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
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
    private var bgColor: Int? = null

    fun backgroundColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
        bgColor = res?.let(parentView.context::getColor) ?: value
    }

    fun iconTint(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
        iconTint = res?.let(parentView.context::getColor) ?: value
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
        private var textColor: Int? = null

        fun textColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
            textColor = res?.let(context::getColor) ?: value
        }

        internal fun build(peekAlert: PeekAlert) {
            peekAlert.setTitle(content)
            textSize?.let(peekAlert::setTitleSize)
            textColor?.let { peekAlert.setTitleColor(color = it) }
            typeface?.let(peekAlert::setTitleTypeface)
        }
    }

    class TextBuilder(
        private val content: String,
        private val context: Context,
    ) {
        var textSize  : Float? = null
        var typeface  : Typeface? = null
        private var textColor: Int? = null

        fun textColor(@ColorRes res: Int? = null, @ColorInt value: Int? = null) {
            textColor = res?.let(context::getColor) ?: value
        }

        internal fun build(peekAlert: PeekAlert) {
            peekAlert.setText(content)
            textSize?.let(peekAlert::setTextSize)
            textColor?.let { peekAlert.setTextColor(color = it) }
            typeface?.let(peekAlert::setTextTypeface)
        }
    }

    class ActionBuilder(
        private val text: String,
        private val context: Context,
    ) {
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
            peekAlert.setAction(ActionConfig(text, backgroundColor, textColor, onActionListener))
        }
    }

    fun build(): PeekAlert {
        alert.setPosition(position)
        cornerRadius?.let(alert::setCornerRadius)
        alert.setWidth(width)
        paddingPx?.let(alert::setPaddingPx) ?: paddingDp?.let(alert::setPaddingDp)
        alert.setMargin(margin.first, margin.second)
        iconRes?.let(alert::setIcon)
        iconTint?.let { alert.setIconTint(color = it) }
        alert.setDraggable(draggable)
        alert.setHideOnTouch(hideOnTouch)
        autoHideMillis?.let(alert::setAutoHide)
        bgColor?.let { alert.setBackgroundColor(color = it) }
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