package com.nunchuk.android.widget.currency

/**
 * Helper method to truncate extra decimal digits from numbers.
 * Was created because the previously used approach, [java.math.RoundingMode.DOWN] approach
 * didn't work correctly for some devices.
 *
 * @param number the original number to format
 * @param maxDecimalDigits the maximum number of decimal digits permitted
 * @param decimalSeparator the decimal separator of the currently selected locale
 * @return a version of number that has a maximum of [maxDecimalDigits] decimal digits.
 * e.g.
 * - 14.333 with 2 max decimal digits return 14.33
 * - 19.2 with 2 max decimal digits return 19.2
 */
fun truncateNumberToMaxDecimalDigits(
    number: String,
    maxDecimalDigits: Int,
    decimalSeparator: Char
): String {
    // Split number into whole and decimal part
    val arr = number
        .split(decimalSeparator)
        .toMutableList()

    // We should have exactly 2 elements in our string;
    // the whole part and the decimal part
    if (arr.size != 2) {
        return number
    }

    // Take the first n (or shorter) from the decimal digits.
    arr[1] = arr[1].take(maxDecimalDigits)

    return arr.joinToString(separator = decimalSeparator.toString())
}
