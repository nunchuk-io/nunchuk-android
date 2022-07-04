package com.nunchuk.android.core.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.TapProtocolException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor(

) : ViewModel() {
    private val _nfcScanInfo = MutableStateFlow<NfcScanInfo?>(null)
    private val _event = MutableStateFlow<NfcState?>(null)

    var inputCvc: String? = null
        private set

    var masterSigner: MasterSigner? = null
        private set

    val nfcScanInfo = _nfcScanInfo.filterIsInstance<NfcScanInfo>()
    val event = _event.filterIsInstance<NfcState>()

    fun updateInputCvc(cvc: String) {
        inputCvc = cvc
    }

    fun updateMasterSigner(masterSigner: MasterSigner) {
        this.masterSigner = masterSigner
    }

    fun updateNfcScanInfo(requestCode: Int, tag: Tag) {
        _nfcScanInfo.value = NfcScanInfo(requestCode, tag)
    }

    fun clearScanInfo() {
        _nfcScanInfo.value = null
    }

    fun clearEvent() {
        _event.value = null
    }

    fun handleNfcError(e: Throwable?) : Boolean {
        if (e is NCNativeException) {
            if (e.message.contains(TapProtocolException.BAD_AUTH.toString())) {
                _event.value = NfcState.WrongCvc
                return true
            } else if (e.message.contains(TapProtocolException.RATE_LIMIT.toString())) {
                _event.value = NfcState.LimitCvcInput
                return true
            }
        }
        return false
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

sealed class NfcState {
    object WrongCvc : NfcState()
    object LimitCvcInput : NfcState()
}