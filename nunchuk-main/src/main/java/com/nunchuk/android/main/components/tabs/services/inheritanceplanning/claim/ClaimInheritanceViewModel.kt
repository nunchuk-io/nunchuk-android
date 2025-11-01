package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.InheritanceAdditional
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ClaimInheritanceViewModel @Inject constructor() : ViewModel() {

    private val _claimData = MutableStateFlow<ClaimData?>(null)
    val claimData = _claimData.asStateFlow()

    fun setClaimNoteData(
        signers: List<SignerModel>,
        magic: String,
        inheritanceAdditional: InheritanceAdditional,
        derivationPaths: List<String>
    ) {
        _claimData.update {
            ClaimData(
                signers = signers,
                magic = magic,
                derivationPaths = derivationPaths,
                inheritanceAdditional = inheritanceAdditional,
                walletBalance = null
            )
        }
    }

    fun setWithdrawData(
        walletBalance: Double,
        signers: List<SignerModel>,
        magic: String,
        derivationPaths: List<String>
    ) {
        _claimData.update {
            ClaimData(
                signers = signers,
                magic = magic,
                derivationPaths = derivationPaths,
                inheritanceAdditional = null,
                walletBalance = walletBalance
            )
        }
    }
}

data class ClaimData(
    val signers: List<SignerModel>,
    val magic: String,
    val derivationPaths: List<String>,
    val inheritanceAdditional: InheritanceAdditional? = null,
    val walletBalance: Double? = null
)

