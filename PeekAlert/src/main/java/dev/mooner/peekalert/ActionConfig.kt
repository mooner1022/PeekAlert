package dev.mooner.peekalert

import android.graphics.Typeface

data class ActionConfig(
    var text            : String? = null,
    /** The button's color; null takes a faint wash of its text's color. */
    var backgroundColor : Int? = null,
    /** The color of the button's text; null takes the color of the alert's title, or of its text with no title. */
    var textColor       : Int? = null,
    var onActionListener: PeekActionListener? = null,
    /** The size of the button's text in sp; null keeps the layout's. */
    var textSize        : Float? = null,
    /** The typeface of the button's text; null takes the one of the alert's title, or of its text with no title. */
    var typeface        : Typeface? = null,
) {
    /** As it was before [textSize] and [typeface], for code built against that version. */
    constructor(text: String?, backgroundColor: Int?, textColor: Int?, onActionListener: PeekActionListener?) :
            this(text, backgroundColor, textColor, onActionListener, null, null)
}
