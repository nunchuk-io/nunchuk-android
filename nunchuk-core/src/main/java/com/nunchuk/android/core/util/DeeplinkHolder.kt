package com.nunchuk.android.core.util

import com.google.gson.Gson
import com.nunchuk.android.core.data.model.DeeplinkInfo
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeeplinkHolder @Inject constructor(
    private val gson: Gson,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val applicationScope: CoroutineScope
) {
    private val _groupLinkInfo = MutableStateFlow<DeeplinkInfo?>(null)
    val groupLinkInfo = _groupLinkInfo.asStateFlow()

    private val _btcUri = MutableStateFlow<BtcUri?>(null)
    val btcUri = _btcUri.asStateFlow()

    fun setDeeplinkInfo(json: String) {
        if (json.isBlank()) return
        applicationScope.launch {
            runCatching {
                gson.fromJson(json, DeeplinkInfo::class.java)
            }.onSuccess {
                _groupLinkInfo.value = it
                Timber.tag("BranchSDK_Tester").e("setDeeplinkInfo info %s", it)
            }
        }
    }

    fun setBtcUri(uri: String) {
        if (uri.isBlank()) return
        applicationScope.launch {
            runCatching {
                parseBtcUriUseCase(uri).getOrThrow()
            }.onSuccess {
                _btcUri.value = it
            }
        }
    }

    fun clearGroupInfo() {
        _groupLinkInfo.value = null
    }

    fun clearBtcUri() {
        _btcUri.value = null
    }
}