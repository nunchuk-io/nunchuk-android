package com.nunchuk.android.core.provider

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.util.*
import javax.inject.Inject

class LocaleProvider @Inject constructor(private val context: Context) {

    fun current(): Locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]

}
