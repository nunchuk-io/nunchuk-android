package com.nunchuk.android.settings.walletsecurity.decoy

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.wallet.WalletBottomSheetResult
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.AddWalletArgs
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.walletsecurity.DecoyWalletCreateRoute
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DecoyWalletCreateScreen(
    onCreateDecoyWallet: () -> Unit = {},
    onUseExistingWallet: () -> Unit = {}
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Create decoy wallet",
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            }, bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = onCreateDecoyWallet
                    ) {
                        Text(
                            text = "Create new decoy wallet",
                            style = NunchukTheme.typography.title.copy(
                                color = MaterialTheme.colorScheme.controlTextPrimary
                            )
                        )
                    }

                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        onClick = onUseExistingWallet
                    ) {
                        Text(
                            "Use existing wallet as decoy wallet",
                            style = NunchukTheme.typography.title
                        )
                    }
                }

            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Image(
                    painter = painterResource(id = R.drawable.ic_create_decoy_wallet),
                    contentDescription = "Decoy wallet intro",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )

                Text(
                    text = "You can either create a new decoy wallet or use an existing wallet as your decoy wallet.",
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DecoyWalletCreateScreenPreview() {
    DecoyWalletCreateScreen()
}

fun NavController.navigateToDecoyWalletCreate(decoyPin: String) {
    navigate(DecoyWalletCreateRoute(decoyPin = decoyPin))
}

fun NavGraphBuilder.decoyWalletCreateScreen(
    activity: FragmentActivity,
    fragmentManager: FragmentManager,
    navigator: NunchukNavigator,
    quickWalletParam: QuickWalletParam?,
    onNavigateToSuccess: () -> Unit,
) {
    composable<DecoyWalletCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<DecoyWalletCreateRoute>()
        val decoyPin = route.decoyPin
        val viewModel = hiltViewModel<DecoyWalletCreateViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(fragmentManager, lifecycleOwner, decoyPin) {
            val listener = androidx.fragment.app.FragmentResultListener { _, bundle ->
                val result =
                    bundle.parcelable<WalletBottomSheetResult>(WalletComposeBottomSheet.RESULT)
                        ?: return@FragmentResultListener
                result.walletId?.let { walletId ->
                    viewModel.createDecoyWallet(walletId = walletId, decoyPin = decoyPin)
                }
                fragmentManager.clearFragmentResult(WalletComposeBottomSheet.TAG)
            }

            fragmentManager.setFragmentResultListener(
                WalletComposeBottomSheet.TAG,
                lifecycleOwner,
                listener,
            )
            onDispose {
                fragmentManager.clearFragmentResultListener(WalletComposeBottomSheet.TAG)
            }
        }

        LaunchedEffect(viewModel) {
            viewModel.event.collectLatest { event ->
                when (event) {
                    is DecoyWalletCreateEvent.Error -> {
                        NCToastMessage(activity).showError(event.message)
                    }

                    DecoyWalletCreateEvent.WalletCreated -> {
                        onNavigateToSuccess()
                    }

                    is DecoyWalletCreateEvent.Loading -> {
                        activity.showOrHideLoading(event.loading)
                    }
                }
            }
        }

        DecoyWalletCreateScreen(onCreateDecoyWallet = {
            navigator.openAddWalletScreen(
                activityContext = activity,
                args = AddWalletArgs(
                    decoyPin = decoyPin,
                    quickWalletParam = quickWalletParam
                ),
            )
        }, onUseExistingWallet = {
            WalletComposeBottomSheet.show(
                fragmentManager = fragmentManager,
                exclusiveAssistedWalletIds = state.assistedWalletIds,
                configArgs = WalletComposeBottomSheet.ConfigArgs()
            )
        })
    }
}
