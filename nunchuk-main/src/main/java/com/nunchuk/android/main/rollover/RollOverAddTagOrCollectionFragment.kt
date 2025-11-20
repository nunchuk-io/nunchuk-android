package com.nunchuk.android.main.rollover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverAddTagOrCollectionFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RollOverAddTagOrCollectionView(
                    onAddTagOrCollectionClicked = {
                        navigator.openCoinList(
                            context = requireContext(),
                            walletId = rollOverWalletViewModel.getOldWalletId()
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RollOverAddTagOrCollectionView(
    onAddTagOrCollectionClicked: () -> Unit = { },
) {
    RollOverAddTagOrCollectionContent(
        onAddTagOrCollectionClicked = onAddTagOrCollectionClicked
    )
}

@Composable
private fun RollOverAddTagOrCollectionContent(
    onAddTagOrCollectionClicked: () -> Unit = { },
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_coin_control),
                    textStyle = NunchukTheme.typography.title,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = onAddTagOrCollectionClicked
                    ) {
                        Text(text = stringResource(R.string.nc_add_coin_tags_or_collections))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_circle_coin_control),
                        contentDescription = ""
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_no_coin_tags_collections),
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@Composable
@Preview
private fun RollOverAddTagOrCollectionScreenContentPreview() {
    RollOverAddTagOrCollectionContent()
}

