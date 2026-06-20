package com.nunchuk.android.main.groupwallet

import com.nunchuk.android.core.util.UiText
import com.nunchuk.android.compose.miniscript.ReplaceKeyData
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
    val errorMessage: UiText? = null,
    val supportedTypes: List<SupportedSigner> = emptyList(),
    val occupiedSlotsIndex : Set<Int> = emptySet(),
    val requestCacheTapSignerXpubEvent: Boolean = false,
    val isCreatedReplaceGroup: Boolean = false,
    val finalizeGroup: GroupSandbox? = null,
    val finalizedWalletId: String? = null,
    val isInReplaceMode: Boolean = false,
    val isReplaceKeyWithTimelock: Boolean = false,
    // Per-key replacement info for miniscript wallets, derived by diffing the current
    // sandbox keys against the original wallet's keys. Drives the green "Replacing X"
    // annotation and the Replace/Remove buttons in the ScriptNodeTree.
    val replaceKeyData: Map<String, ReplaceKeyData> = emptyMap(),
    val miniscriptTemplate: String = "",
    val scriptNode: ScriptNode? = null,
    val keyPath: List<String> = emptyList(),
    val scriptNodeMuSig: ScriptNode? = null,
    val namedSigners: Map<String, SignerModel?> = emptyMap(),
    val namedOccupied: Set<String> = emptySet(),
    // Miniscript key names another device added but whose signer data hasn't synced here yet.
    val waitingToJoinKeys: Set<String> = emptySet(),
    val currentKeyToAssign: String = "",
    val event: FreeGroupWalletEvent? = null,
    val isTestNet: Boolean = false,
    val pendingAddSignerState: PendingAddSignerState? = null,
    val miniscriptReuseSigner: Boolean = false,
)

sealed class FreeGroupWalletEvent {
    data class ShowDuplicateSignerWarning(val signer: SignerModel, val keyName: String) : FreeGroupWalletEvent()
}