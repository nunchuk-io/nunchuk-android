package com.nunchuk.android.signer.mk4

import androidx.lifecycle.ViewModel
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class Mk4ViewModel @Inject constructor() : ViewModel() {

    lateinit var coldCardBackUpParam: ColdCardBackUpParam
        private set

    fun setOrUpdate(param: ColdCardBackUpParam) {
        coldCardBackUpParam = param
    }
}

data class ColdCardBackUpParam(
    val isHasPassphrase: Boolean = false,
    val xfp: String,
    val keyType: SignerType,
    val filePath: String,
    val keyName: String,
    val backUpFileName: String,
    val keyId: String,
    val isRequestAddOrReplaceKey: Boolean
)