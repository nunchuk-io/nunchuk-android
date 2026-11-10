package com.nunchuk.android.core.repository

import com.nunchuk.android.core.manager.UserWalletApiManager
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicyType

/**
 * Whether [groupId]'s (or the personal) draft is a configurable on-chain draft (NUN-10320). Throws if
 * the draft can't be read, so a caller never treats an unknown draft as legacy.
 */
internal suspend fun UserWalletApiManager.isConfigurableDraft(groupId: String): Boolean {
    val response = if (groupId.isEmpty()) walletApi.getDraftWallet() else groupWalletApi.getDraftWallet(groupId)
    if (response.isSuccess.not()) throw response.error
    return response.data.draftWallet?.onChainInheritancePolicy?.type == OnChainInheritancePolicyType.CONFIGURABLE.name
}

/**
 * Whether [walletId] (in [groupId], or personal) is a configurable on-chain wallet (NUN-10320). Throws
 * if the wallet can't be read, so a caller never treats an unknown wallet as legacy.
 */
internal suspend fun UserWalletApiManager.isConfigurableWallet(groupId: String, walletId: String): Boolean {
    val response = if (groupId.isEmpty()) walletApi.getWallet(walletId) else groupWalletApi.getGroupWallet(groupId)
    if (response.isSuccess.not()) throw response.error
    return response.data.wallet?.onChainInheritancePolicy?.type == OnChainInheritancePolicyType.CONFIGURABLE.name
}
