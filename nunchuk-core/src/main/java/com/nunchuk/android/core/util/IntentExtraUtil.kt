package com.nunchuk.android.core.util

import android.os.Bundle

fun Bundle?.getStringValue(
    key: String,
    defaultValue: String = ""
) = this?.getString(key, defaultValue).orEmpty()

fun Bundle?.getDoubleValue(
    key: String,
    defaultValue: Double = 0.0
) = this?.getDouble(key, defaultValue) ?: defaultValue

fun Bundle?.getBooleanValue(
    key: String,
    defaultValue: Boolean = true
) = this?.getBoolean(key, defaultValue) ?: defaultValue

fun Bundle?.getLongValue(
    key: String,
    defaultValue: Long = 0L
) = this?.getLong(key, defaultValue) ?: defaultValue

fun Bundle?.getIntValue(
    key: String,
    defaultValue: Int = 0
) = this?.getInt(key, defaultValue) ?: defaultValue