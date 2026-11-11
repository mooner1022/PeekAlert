package dev.mooner.peekalert

import android.graphics.Typeface

data class TextConfig(
    var content : String? = null,
    var size    : Float? = null,
    var color   : Int? = null,
    var typeface: Typeface? = null,
    /** The most lines shown before the text is cut with an ellipsis; null keeps the layout's (1 for the title, 4 for the text). */
    var maxLines: Int? = null,
) {
    /** As it was before [maxLines], for code built against that version. */
    constructor(content: String?, size: Float?, color: Int?, typeface: Typeface?) :
            this(content, size, color, typeface, null)
}
