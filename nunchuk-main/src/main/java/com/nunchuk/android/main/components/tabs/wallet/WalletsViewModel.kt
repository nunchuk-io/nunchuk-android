package com.nunchuk.android.main.components.tabs.wallet

import android.util.Log
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.process
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.messages.usecase.contact.GetContactsUseCase
import com.nunchuk.android.messages.usecase.contact.GetReceivedContactsUseCase
import com.nunchuk.android.messages.usecase.contact.GetSentContactsUseCase
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import javax.inject.Inject

internal class WalletsViewModel @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val getSentContactsUseCase: GetSentContactsUseCase,
    private val getReceivedContactsUseCase: GetReceivedContactsUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    override val initialState = WalletsState()

    fun retrieveData() {
        getSigners()
        getWallets()
        getContacts()
    }

    private fun getContacts() {
        process(getContactsUseCase::execute)
        process(getSentContactsUseCase::execute)
        process(getReceivedContactsUseCase::execute)
    }

    private fun getSigners() {
        getMasterSigners()
        getRemoteSigners()
    }

    private fun getRemoteSigners() {
        process(getRemoteSignersUseCase::execute, {
            updateState { copy(signers = it) }
        }, {
            updateState { copy(signers = emptyList()) }
            Log.e(TAG, "get signers error: ${it.message}")
        })
    }

    private fun getMasterSigners() {
        process(getMasterSignersUseCase::execute, {
            updateState { copy(masterSigners = it) }
        }, {
            updateState { copy(signers = emptyList()) }
            Log.e(TAG, "get signers error: ${it.message}")
        })
    }

    private fun getWallets() {
        process(getWalletsUseCase::execute, {
            updateState { copy(wallets = it) }
        }, {
            updateState { copy(wallets = emptyList()) }
            Log.e(TAG, "get wallets error: ${it.message}")
        })
    }

    fun handleAddSignerOrWallet() {
        if (hasSigner()) {
            handleAddWallet()
        } else {
            handleAddSigner()
        }
    }

    fun handleAddSigner() {
        event(ShowSignerIntroEvent)
    }

    fun handleAddWallet() {
        if (hasSigner()) {
            event(AddWalletEvent)
        } else {
            event(WalletEmptySignerEvent)
        }
    }

    private fun hasSigner() = getState().signers.isNotEmpty() || getState().masterSigners.isNotEmpty()

    companion object {
        private const val TAG = "WalletsViewModel"
    }
}