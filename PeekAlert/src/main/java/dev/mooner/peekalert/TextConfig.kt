package dev.mooner.peekalert

import android.graphics.Typeface

data class TextConfig(
    var content : String? = null,
    var size    : Float? = null,
    var color   : Int? = null,
    var typeface: Typeface? = null,
)
