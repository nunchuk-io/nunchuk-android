package com.nunchuk.android.main.groupwallet

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.fragment.app.FragmentManager
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragment
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragmentArgs
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable

fun NavGraphBuilder.customKeyNavigation(
    viewModel: FreeGroupWalletViewModel,
    onCustomIndexDone: (SingleSigner) -> Unit = {},
) {
    composable<SignerModel> { backStackEntry ->
        val signer: SignerModel = backStackEntry.toRoute()
        val fragmentState = rememberFragmentState()
        val args = CustomKeyAccountFragmentArgs(signer = signer, groupId = viewModel.groupId, isFreeWallet = true, isMultisigWallet = true)
        val view = LocalView.current
        val fragmentManager = remember(view) {
            FragmentManager.findFragmentManager(view)
        }
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(Unit) {
            fragmentManager.setFragmentResultListener(
                CustomKeyAccountFragment.REQUEST_KEY,
                lifecycleOwner
            ) { _, bundle ->
                bundle.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)
                    ?.let(onCustomIndexDone)
                fragmentManager.clearFragmentResult(CustomKeyAccountFragment.REQUEST_KEY)
            }
        }

        AndroidFragment<CustomKeyAccountFragment>(
            modifier = Modifier.systemBarsPadding(),
            fragmentState = fragmentState,
            arguments = args.toBundle()
        )
    }
}

fun NavController.navigateCustomKey(signer: SignerModel) {
    navigate(signer)
}