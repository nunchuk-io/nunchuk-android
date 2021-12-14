package com.nunchuk.android.utils

fun CharSequence?.safeInt(): Int = if (isNullOrEmpty()) 0 else toString().toInt()

fun CharSequence?.isNoneEmpty(): Boolean = this?.toString().orEmpty().isNotEmpty()