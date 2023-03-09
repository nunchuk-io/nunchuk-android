package com.nunchuk.android.utils

object Utils {
    fun maskValue(originalValue: String, isMask: Boolean, numberOfBullet: Int = 6): String {
        return if (isMask) '\u2022'.toString().repeat(numberOfBullet) else originalValue
    }
}