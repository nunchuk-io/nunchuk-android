package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.claimnote

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceViewModel
import com.nunchuk.android.model.InheritanceAdditional
import kotlinx.serialization.Serializable

@Serializable
data object ClaimNoteRoute

fun NavGraphBuilder.claimNote(
    onDoneClick: () -> Unit = {},
    onWithdrawClick: (
        balance: Double,
        signers: List<SignerModel>,
        magic: String,
        derivationPaths: List<String>
    ) -> Unit = { _, _, _, _ -> },
) {
    composable<ClaimNoteRoute> {
        val activity = LocalActivity.current as ComponentActivity
        val activityViewModel: ClaimInheritanceViewModel = hiltViewModel(viewModelStoreOwner = activity)
        val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()
        
        claimData?.let { data ->
            data.inheritanceAdditional?.let { inheritanceAdditional ->
                ClaimNoteScreen(
                    signers = data.signers,
                    magic = data.magic,
                    inheritanceAdditional = inheritanceAdditional,
                    derivationPaths = data.derivationPaths,
                    onDoneClick = {
                        onDoneClick()
                    },
                    onWithdrawClick = onWithdrawClick,
                )
            }
        }
    }
}

fun NavController.navigateToClaimNote(
    signers: List<SignerModel>,
    magic: String,
    inheritanceAdditional: InheritanceAdditional,
    derivationPaths: List<String>,
    activityViewModel: ClaimInheritanceViewModel
) {
    activityViewModel.setClaimNoteData(signers, magic, inheritanceAdditional, derivationPaths)
    navigate(ClaimNoteRoute)
}

