package com.nunchuk.android.wallet.components.coin.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.LabelNumberAndDesc
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.coin.CollectionFlow
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionIntroFragment : Fragment() {
    private val args by navArgs<CollectionIntroFragmentArgs>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            CollectionIntroScreen(
                onContinueClick = {
                    findNavController().navigate(
                        directions = CoinNavigationDirections.actionGlobalCoinCollectionInfoFragment(
                            walletId = args.walletId,
                            coinCollection = null,
                            flow = CollectionFlow.ADD,
                        ),
                        navOptions = NavOptions.Builder().apply {
                            setPopUpTo(R.id.collectionIntroFragment, true)
                        }.build()
                    )
                }
            )
        }
    }
}

@Composable
private fun CollectionIntroScreen(
    onContinueClick: () -> Unit = {}
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(backgroundRes = R.drawable.bg_coin_collection)
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_coin_collection),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    text = stringResource(R.string.nc_coin_collection_intro),
                    style = NunchukTheme.typography.body
                )

                LabelNumberAndDesc(
                    index = 1,
                    title = stringResource(R.string.nc_create_a_filter),
                    titleStyle = NunchukTheme.typography.title
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.nc_create_filter_desc),
                        style = NunchukTheme.typography.body
                    )
                }

                LabelNumberAndDesc(
                    index = 2,
                    title = stringResource(R.string.nc_set_action),
                    titleStyle = NunchukTheme.typography.title
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.nc_set_action_desc),
                        style = NunchukTheme.typography.body
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CollectionIntroScreenPreview() {
    CollectionIntroScreen()
}