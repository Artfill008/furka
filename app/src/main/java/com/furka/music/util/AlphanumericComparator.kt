package com.furka.music.util

import java.util.Comparator

/**
 * Natural Sort Comparator for Alphanumeric Sorting.
 * E.g. "Track 1", "Track 2", "Track 10" (Correct)
 * Instead of "Track 1", "Track 10", "Track 2" (Wrong)
 */
class AlphanumericComparator : Comparator<String> {
    override fun compare(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        var i1 = 0
        var i2 = 0

        while (i1 < len1 && i2 < len2) {
            val c1 = s1[i1]
            val c2 = s2[i2]

            if (c1.isDigit() && c2.isDigit()) {
                // Determine length of numeric chunk
                var end1 = i1 + 1
                while (end1 < len1 && s1[end1].isDigit()) end1++

                var end2 = i2 + 1
                while (end2 < len2 && s2[end2].isDigit()) end2++

                // Compare numeric values
                val num1 = s1.substring(i1, end1).toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
                val num2 = s2.substring(i2, end2).toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
                
                val diff = num1.compareTo(num2)
                if (diff != 0) return diff

                i1 = end1
                i2 = end2
            } else {
                // Compare characters case-insensitively
                val c1Upp = c1.uppercaseChar()
                val c2Upp = c2.uppercaseChar()
                
                if (c1Upp != c2Upp) {
                    return c1Upp.compareTo(c2Upp)
                }
                
                i1++
                i2++
            }
        }

        return len1 - len2
    }
}

private fun Char.isDigit(): Boolean = this in '0'..'9'
