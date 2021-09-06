package com.nunchuk.android.messages.util

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.matrix.android.sdk.api.session.events.model.toContent

internal val gson = GsonBuilder()
    .apply {
        registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, JsonMapDeserializer())
    }.create()


fun String.toMatrixContent() = gson.fromJson<Map<String, Any>>(
    this,
    object : TypeToken<Map<String, Any>>() {}.type
).toContent()