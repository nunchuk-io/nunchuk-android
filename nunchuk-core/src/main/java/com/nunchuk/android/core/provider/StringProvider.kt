package com.nunchuk.android.core.provider

import android.content.Context
import androidx.annotation.StringRes
import javax.inject.Inject

class StringProvider @Inject constructor(context: Context) {

    private val resources = context.resources

    fun getString(@StringRes resId: Int) = resources.getString(resId)

    fun getString(@StringRes resId: Int, vararg formatArgs: Any?) = resources.getString(resId, *formatArgs)
}

