package com.nunchuk.android.messages.util

import android.view.View
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.nunchuk.android.messages.R
import org.matrix.android.sdk.api.session.events.model.toContent

internal val gson = GsonBuilder()
    .apply {
        registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, JsonMapDeserializer())
    }.create()


fun String.toMatrixContent() = gson.fromJson<Map<String, Any>>(
    this,
    object : TypeToken<Map<String, Any>>() {}.type
).toContent()

fun View.bindNotificationBackground(highlight: Boolean) {
    background = if (highlight) {
        ContextCompat.getDrawable(context, R.drawable.nc_slime_tint_background)
    } else {
        ContextCompat.getDrawable(context, R.drawable.nc_rounded_whisper_disable_background)
    }
}