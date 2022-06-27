package com.nunchuk.android.core.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor() : ViewModel() {
    private val _nfcScanInfo = MutableStateFlow<NfcScanInfo?>(null)
    var inputCvc: String? = null
        private set

    val nfcScanInfo = _nfcScanInfo.filterIsInstance<NfcScanInfo>()

    fun updateInputCvc(cvc: String) {
        inputCvc = cvc
    }

    fun updateNfcScanInfo(requestCode: Int, tag: Tag) {
        _nfcScanInfo.value = NfcScanInfo(requestCode, tag)
    }

    fun clearScanInfo() {
        _nfcScanInfo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        closeNfc()
    }

    private fun closeNfc() {
        runCatching {
            _nfcScanInfo.value?.let {
                IsoDep.get(it.tag)?.close()
            }
        }
    }
}