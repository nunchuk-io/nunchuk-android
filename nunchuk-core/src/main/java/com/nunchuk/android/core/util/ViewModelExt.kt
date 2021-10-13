package com.nunchuk.android.core.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.matrix.android.sdk.api.session.events.model.toContent

val gson: Gson = GsonBuilder()
    .apply {
        registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, JsonMapDeserializer())
    }.create()


fun String.toMatrixContent() = gson.fromJson<Map<String, Any>>(
    this,
    object : TypeToken<Map<String, Any>>() {}.type
).toContent()
