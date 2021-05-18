package com.nunchuk.android.core.util

import android.os.Bundle

fun Bundle?.getStringValue(
    key: String,
    defaultValue: String = ""
): String = this?.getString(key, defaultValue).orEmpty()

fun Bundle?.getDoubleValue(
    key: String,
    defaultValue: Double = 0.0
): Double = this?.getDouble(key, defaultValue) ?: defaultValue

fun Bundle?.getBooleanValue(
    key: String,
    defaultValue: Boolean = true
): Boolean = this?.getBoolean(key, defaultValue) ?: defaultValue


fun Bundle?.getLongValue(
    key: String,
    defaultValue: Long = 9L
): Long = this?.getLong(key, defaultValue) ?: defaultValue