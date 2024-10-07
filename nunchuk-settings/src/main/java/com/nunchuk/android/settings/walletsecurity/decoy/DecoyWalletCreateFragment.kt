package com.nunchuk.android.settings.walletsecurity.decoy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.wallet.WalletBottomSheetResult
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.R
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DecoyWalletCreateFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<DecoyWalletCreateViewModel>()
    private val args: DecoyWalletCreateFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val state by viewModel.state.collectAsStateWithLifecycle()
        DecoyWalletCreateScreen(onCreateDecoyWallet = {
            navigator.openAddWalletScreen(activityContext = requireContext(), decoyPin = args.decoyPin)
        }, onUseExistingWallet = {
            WalletComposeBottomSheet.show(
                fragmentManager = childFragmentManager,
                exclusiveAssistedWalletIds = state.assistedWalletIds,
                configArgs = WalletComposeBottomSheet.ConfigArgs()
            )
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.setFragmentResultListener(
            WalletComposeBottomSheet.TAG,
            this
        ) { _, bundle ->
            val result = bundle.parcelable<WalletBottomSheetResult>(WalletComposeBottomSheet.RESULT)
                ?: return@setFragmentResultListener
            if (result.walletId != null) {
                viewModel.createDecoyWallet(result.walletId.orEmpty(), args.decoyPin)
            }
            childFragmentManager.clearFragmentResult(WalletComposeBottomSheet.TAG)
        }

        flowObserver(viewModel.event) { event ->
            when (event) {
                is DecoyWalletCreateEvent.Error -> showError(event.message)
                DecoyWalletCreateEvent.WalletCreated -> {
                    findNavController().navigate(DecoyWalletCreateFragmentDirections.actionDecoyWalletCreateFragmentToDecoyWalletSuccessFragment())
                }

                is DecoyWalletCreateEvent.Loading -> showOrHideLoading(event.loading)
            }
        }
    }
}


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
                        Text(text = "Create new decoy wallet",
                            style = NunchukTheme.typography.title.copy(color = Color.White))
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

@Preview
@Composable
private fun DecoyWalletCreateScreenPreview() {
    DecoyWalletCreateScreen()
}