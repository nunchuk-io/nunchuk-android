package com.nunchuk.android.main.groupwallet

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.signer.SupportedSigner

data class FreeGroupWalletUiState(
    val group: GroupSandbox? = null,
    val signers: List<SignerModel?> = emptyList(),
    val replaceSigners : List<SignerModel?> = emptyList(),
    val allSigners: List<SignerModel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFinishScreen: Boolean = false,
    val numberOfOnlineUsers: Int = 1,
    val groupWalletUnavailable: Boolean = false,
    val errorMessage: String = "",
    val supportedTypes: List<SupportedSigner> = emptyList(),
    val occupiedSlotsIndex : Set<Int> = emptySet(),
    val requestCacheTapSignerXpubEvent: Boolean = false,
    val isCreatedReplaceGroup: Boolean = false,
    val finalizeGroup: GroupSandbox? = null,
    val finalizedWalletId: String? = null,
    val isInReplaceMode: Boolean = false,
    val miniscriptTemplate: String = "",
    val scriptNode: ScriptNode? = null,
    val keyPath: List<String> = emptyList(),
    val scriptNodeMuSig: ScriptNode? = null,
    val namedSigners: Map<String, SignerModel?> = emptyMap(),
    val namedOccupied: Set<String> = emptySet(),
    val currentKeyToAssign: String = "",
    val event: FreeGroupWalletEvent? = null,
    val isTestNet: Boolean = false,
    val pendingAddSignerState: PendingAddSignerState? = null,
)

sealed class FreeGroupWalletEvent {
    data class ShowDuplicateSignerWarning(val signer: SignerModel, val keyName: String) : FreeGroupWalletEvent()
}