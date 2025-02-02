package com.nunchuk.android.core.util

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.DeeplinkInfo
import javax.inject.Inject

class DeeplinkHolder @Inject constructor(val gson: Gson) {
    var info: DeeplinkInfo? = null
        private set

    fun setDeeplinkInfo(json: String) {
        info = kotlin.runCatching {
            gson.fromJson(json, DeeplinkInfo::class.java)
        }.getOrNull()
    }
}