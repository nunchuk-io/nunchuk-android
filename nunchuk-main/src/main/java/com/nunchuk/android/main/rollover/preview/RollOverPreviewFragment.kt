package com.nunchuk.android.main.rollover.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import com.nunchuk.android.compose.CoinCollectionGroupView
import com.nunchuk.android.compose.CoinTagGroupView
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.rollover.RollOverWalletViewModel
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverPreviewFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: RollOverPreviewViewModel by viewModels()
    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RollOverPreviewView(
                    viewModel = viewModel,
                    onContinueClicked = {
                        findNavController().navigate(
                            RollOverPreviewFragmentDirections.actionRollOverPreviewFragmentToRollOverBroadcastTransactionFragment()
                        )
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowObserver(rollOverWalletViewModel.uiState) { state ->
            viewModel.updateTagsAndCollections(
                tags = rollOverWalletViewModel.getCoinTags(),
                collections = rollOverWalletViewModel.getCoinCollections()
            )
            if (rollOverWalletViewModel.getSelectedTags() != null
                && rollOverWalletViewModel.getSelectedCollections() != null
            ) {
                viewModel.init(
                    oldWalletId = rollOverWalletViewModel.getOldWalletId(),
                    newWalletId = rollOverWalletViewModel.getNewWalletId(),
                    selectedTags = rollOverWalletViewModel.getSelectedTags().orEmpty(),
                    selectedCollections = rollOverWalletViewModel.getSelectedCollections().orEmpty(),
                    feeRate = rollOverWalletViewModel.getFeeRate(),
                    signingPath = rollOverWalletViewModel.getSigningPath()
                )
            }
        }

        flowObserver(viewModel.event) { event ->
            when (event) {
                is RollOverPreviewEvent.Error -> showError(event.message)
                is RollOverPreviewEvent.Loading -> showOrHideLoading(event.isLoading)
            }
        }
    }
}

@Composable
private fun RollOverPreviewView(
    viewModel: RollOverPreviewViewModel = hiltViewModel(),
    onContinueClicked: () -> Unit = { },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RollOverPreviewContent(
        uiState = uiState,
        onContinueClicked = onContinueClicked
    )
}

@Composable
private fun RollOverPreviewContent(
    uiState: RollOverPreviewUiState = RollOverPreviewUiState(),
    onContinueClicked: () -> Unit = { },
) {
    NunchukTheme {
        NcScaffold(modifier = Modifier.systemBarsPadding(), topBar = {
            NcTopAppBar(title = "Rollover preview",
                textStyle = NunchukTheme.typography.title,
                actions = {
                    Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                })
        }, bottomBar = {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Total fee: ${uiState.totalFee.pureBTC().getBTCAmount()}",
                    style = NunchukTheme.typography.titleSmall
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(), onClick = onContinueClicked
                ) {
                    Text(text = stringResource(R.string.nc_confirm_create_transactions))
                }
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {

                LazyColumn {
                    item {
                        Text(
                            text = "The following transactions will be created.",
                            style = NunchukTheme.typography.body,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    items(uiState.uis) { ui ->
                        PreviewTransactionView(uiState = ui)

                        HorizontalDivider(modifier = Modifier.padding(top = 24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTransactionView(
    uiState: PreviewTransactionUi
) {
    Row(
        modifier = Modifier.padding(
            top = 24.dp,
        )
    ) {
        Text(
            text = uiState.transaction.outputs.firstOrNull()?.first.orEmpty(),
            style = NunchukTheme.typography.title,
            modifier = Modifier
                .weight(1f, fill = true)
                .padding(end = 16.dp)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = uiState.transaction.totalAmount.pureBTC().getBTCAmount(),
                style = NunchukTheme.typography.title,
                modifier = Modifier
            )
            Text(
                text = uiState.transaction.totalAmount.pureBTC().getCurrencyAmount(),
                style = NunchukTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (uiState.tags.isNotEmpty()) {
        CoinTagGroupView(
            modifier = Modifier.padding(top = 4.dp),
            tagIds = uiState.tags.keys,
            tags = uiState.tags,
            onViewTagDetail = {}
        )
    }

    if (uiState.collections.isNotEmpty()) {
        CoinCollectionGroupView(
            modifier = Modifier.padding(top = 4.dp),
            collectionIds = uiState.collections.keys,
            collections = uiState.collections,
            onViewCollectionDetail = {}
        )
    }

    Row(
        modifier = Modifier.padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Estimated fee",
            style = NunchukTheme.typography.body,
            modifier = Modifier
                .weight(1f, fill = true)
                .padding(end = 16.dp)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = uiState.transaction.feeRate.pureBTC().getBTCAmount(),
                style = NunchukTheme.typography.title,
                modifier = Modifier
            )
            Text(
                text = uiState.transaction.feeRate.pureBTC().getCurrencyAmount(),
                style = NunchukTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
@Preview
private fun RollOverPreviewScreenContentPreview() {
    RollOverPreviewContent(
        uiState = RollOverPreviewUiState()
    )
}