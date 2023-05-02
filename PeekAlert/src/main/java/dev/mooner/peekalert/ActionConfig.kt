package dev.mooner.peekalert

data class ActionConfig(
    var text            : String? = null,
    var backgroundColor : Int? = null,
    var textColor       : Int? = null,
    var onActionListener: PeekActionListener? = null,
)
