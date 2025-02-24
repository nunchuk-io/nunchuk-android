package com.nunchuk.android.utils

import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.domain.SendMessageFreeGroupWalletUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import timber.log.Timber
import javax.inject.Inject

class GroupChatManager @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val sendMessageFreeGroupWalletUseCase: SendMessageFreeGroupWalletUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val hasSignerUseCase: HasSignerUseCase,
) {

    var selectedSigner: SingleSigner? = null
        private set

    suspend fun init(walletId: String) {
        getAllSignersUseCase(true).onSuccess { (masterSigners, singleSigners) ->
            val signers = mapSigners(singleSigners, masterSigners)
            getWalletDetail(walletId, signers)
        }
    }

    private suspend fun getWalletDetail(walletId: String, allSigner: List<SignerModel>) {
        if (walletId.isEmpty()) return
        getWalletDetail2UseCase(walletId).onSuccess { wallet ->
            val signers = wallet.signers
            selectedSigner = signers.find { signer ->
                allSigner.any { it.fingerPrint == signer.masterFingerprint && it.type != SignerType.UNKNOWN }
            }
            if (selectedSigner == null) {
                signers.forEach { signer ->
                    if (hasSignerUseCase(signer).isSuccess) {
                        selectedSigner = signer
                        return@forEach
                    }
                }
            }
        }.onFailure {
            // Handle failure
        }
    }

    suspend fun sendMessage(message: String, walletId: String, onError: (Throwable) -> Unit) {
        if (selectedSigner == null) return
        sendMessageFreeGroupWalletUseCase(
            SendMessageFreeGroupWalletUseCase.Param(
                walletId = walletId,
                message = message,
                singleSigner = selectedSigner!!,
            )
        ).onSuccess {
            Timber.e("group-wallet", "send message success")
        }.onFailure {
            onError(it)
            Timber.e("group-wallet", "send message failed: $it")
        }
    }

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>, masterSigners: List<MasterSigner>,
    ): List<SignerModel> {
        return masterSigners.map {
            masterSignerMapper(it)
        } + singleSigners.map(SingleSigner::toModel)
    }
}