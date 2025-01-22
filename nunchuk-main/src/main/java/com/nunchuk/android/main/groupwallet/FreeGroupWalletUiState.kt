package com.nunchuk.android.main.groupwallet

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.GroupSandbox

data class FreeGroupWalletUiState(
    val group: GroupSandbox? = null,
    val signers: List<SignerModel?> = emptyList(),
    val allSigners: List<SignerModel> = emptyList(),
    val isLoading: Boolean = false,
    val isFinishScreen: Boolean = false,
    val numberOfOnlineUsers: Int = 1,
    val groupWalletUnavailable: Boolean = false,
    val errorMessage: String = ""
)