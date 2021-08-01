package com.nunchuk.android.wallet.components.confirm

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.type.WalletType.ESCROW
import com.nunchuk.android.usecase.CreateWalletUseCase
import com.nunchuk.android.usecase.DraftWalletUseCase
import com.nunchuk.android.usecase.GetUnusedSignerFromMasterSignerUseCase
import com.nunchuk.android.wallet.components.confirm.WalletConfirmEvent.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

internal class WalletConfirmViewModel @Inject constructor(
    private val getUnusedSignerUseCase: GetUnusedSignerFromMasterSignerUseCase,
    private val draftWalletUseCase: DraftWalletUseCase,
    private val createWalletUseCase: CreateWalletUseCase
) : NunchukViewModel<Unit, WalletConfirmEvent>() {

    override val initialState = Unit
    private var descriptor = ""

    fun handleContinueEvent(
        walletName: String,
        walletType: WalletType,
        addressType: AddressType,
        totalRequireSigns: Int,
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    ) {
        event(SetLoadingEvent(true))
        viewModelScope.launch {
            val unusedSignerSigners = ArrayList<SingleSigner>()
            masterSigners.forEach {
                getUnusedSignerUseCase
                    .execute(it.id, walletType, addressType)
                    .collect { signer -> unusedSignerSigners.add(signer) }
            }
            draftWallet(walletName, totalRequireSigns, addressType, walletType, unusedSignerSigners + remoteSigners)
        }
    }

    private suspend fun draftWallet(
        walletName: String,
        totalRequireSigns: Int,
        addressType: AddressType,
        walletType: WalletType,
        signers: List<SingleSigner>
    ) {
        draftWalletUseCase.execute(
            name = walletName,
            totalRequireSigns = totalRequireSigns,
            signers = signers,
            addressType = addressType,
            isEscrow = walletType == ESCROW
        ).catch {
            event(CreateWalletErrorEvent(it.message.orUnknownError()))
            event(SetLoadingEvent(false))
        }.collect {
            descriptor = it
            createWallet(walletName, totalRequireSigns, signers, addressType, walletType)
        }
    }

    private fun createWallet(
        walletName: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        walletType: WalletType
    ) {
        viewModelScope.launch {
            createWalletUseCase.execute(
                name = walletName,
                totalRequireSigns = totalRequireSigns,
                signers = signers,
                addressType = addressType,
                isEscrow = walletType == ESCROW
            ).catch {
                event(CreateWalletErrorEvent(it.message.orUnknownError()))
                event(SetLoadingEvent(false))
            }.collect {
                event(CreateWalletSuccessEvent(it.id, descriptor))
            }
        }
    }

}

internal fun String.isWalletExisted() = this.toLowerCase(Locale.getDefault()).startsWith(WALLET_EXISTED)

internal const val WALLET_EXISTED = "wallet existed"