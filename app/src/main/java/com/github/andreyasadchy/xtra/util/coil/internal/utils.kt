/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.andreyasadchy.xtra.util.coil.internal

internal fun String?.toNonNegativeInt(defaultValue: Int): Int {
    try {
        val value = this?.toLong() ?: return defaultValue
        return when {
            value > Int.MAX_VALUE -> Int.MAX_VALUE
            value < 0 -> 0
            else -> value.toInt()
        }
    } catch (_: NumberFormatException) {
        return defaultValue
    }
}

/**
 * Returns the next index in this at or after [startIndex] that is a character from
 * [characters]. Returns the input length if none of the requested characters can be found.
 */
internal fun String.indexOfElement(
    characters: String,
    startIndex: Int = 0,
): Int {
    for (i in startIndex until length) {
        if (this[i] in characters) {
            return i
        }
    }
    return length
}

/**
 * Returns the index of the next non-whitespace character in this. Result is undefined if input
 * contains newline characters.
 */
internal fun String.indexOfNonWhitespace(startIndex: Int = 0): Int {
    for (i in startIndex until length) {
        val c = this[i]
        if (c != ' ' && c != '\t') {
            return i
        }
    }
    return length
}
