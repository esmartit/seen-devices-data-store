package com.esmartit.seendevicesdatastore.domain

enum class Position(val value: Int) {
    IN(3), LIMIT(2), OUT(1), NO_POSITION(-1);

    companion object {
        fun byValue(value: Int): Position {
            return when (value) {
                3 -> IN
                2 -> LIMIT
                1 -> OUT
                else -> NO_POSITION
            }
        }
    }
}
