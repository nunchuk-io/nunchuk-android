package com.nunchuk.android.main.rollover.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.CustomSlider
import com.nunchuk.android.compose.CustomSliderDefaults
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.progress
import com.nunchuk.android.compose.track
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.rollover.RollOverWalletViewModel
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverBroadcastTransactionFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: RollOverBroadcastTransactionViewModel by viewModels()
    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                RollOverBroadcastTransactionView(viewModel) { randomizeBroadcast, days ->
                    rollOverWalletViewModel.createRollOverTransactions(randomizeBroadcast, days)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flowObserver(rollOverWalletViewModel.uiState) {
            viewModel.updateIsFreeWallet(it.isFreeWallet)
        }
    }
}

@Composable
private fun RollOverBroadcastTransactionView(
    viewModel: RollOverBroadcastTransactionViewModel = hiltViewModel(),
    onContinueClicked: (Boolean, Int) -> Unit = { _, _ -> },
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RollOverBroadcastTransactionContent(
        uiState = uiState,
        onContinueClicked = {
            onContinueClicked(uiState.randomizeBroadcast, uiState.days)
        },
        onSlideChanged = { days ->
            viewModel.updateDays(days)
        },
        onRandomizeBroadcastChanged = { randomizeBroadcast ->
            viewModel.updateRandomizeBroadcast(randomizeBroadcast)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RollOverBroadcastTransactionContent(
    uiState: RollOverBroadcastTransactionUiState,
    onContinueClicked: () -> Unit = {},
    onSlideChanged: (days: Int) -> Unit = {},
    onRandomizeBroadcastChanged: (randomizeBroadcast: Boolean) -> Unit = {},
) {
    var sliderValue by remember { mutableFloatStateOf(1f) }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = "Before you continue",
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
                    NcHintMessage(
                        messages = listOf(
                            ClickAbleText(
                                content = "Broadcasting the rollover transactions at the same time might allow someone to link those transactions and damage your privacy."
                            )
                        ),
                        type = HighlightMessageType.WARNING,
                    )

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = {
                            onContinueClicked()
                        }) {
                        Text(text = stringResource(R.string.nc_text_continue))
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
                        painter = painterResource(id = R.drawable.ic_circle_notification),
                        contentDescription = ""
                    )
                }

                NcSpannedText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_roll_over_broadcast_notification),
                    baseStyle = NunchukTheme.typography.body,
                    styles = mapOf(
                        SpanIndicator('A') to SpanStyle(fontWeight = FontWeight.Bold),
                    )
                )

                if (uiState.isFreeWallet.not()) {
                    Column(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.greyLight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(top = 10.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Automatically randomize broadcast",
                                style = NunchukTheme.typography.title
                            )

                            NcSwitch(
                                checked = uiState.randomizeBroadcast,
                                onCheckedChange = {
                                    onRandomizeBroadcastChanged(it)
                                },
                            )
                        }

                        if (uiState.randomizeBroadcast) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                text = "Broadcast transactions within:",
                                style = NunchukTheme.typography.body
                            )

                            CustomSlider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 10.dp, bottom = 16.dp),
                                value = sliderValue,
                                showLabel = true,
                                onValueChange = {
                                    sliderValue = it
                                },
                                onValueChangeFinished = {
                                    onSlideChanged(it.toInt())
                                },
                                valueRange = 1f..7f,
                                gap = 1,
                                thumb = {
                                    CustomSliderDefaults.Thumb(
                                        thumbValue = "",
                                        color = Color.White,
                                        size = 25.dp,
                                    )
                                },
                                label = { value ->
                                    Text(
                                        text = when (value) {
                                            1 -> "1 day"
                                            else -> "$value days"
                                        },
                                        style = NunchukTheme.typography.title
                                    )
                                },
                                track = { sliderState ->
                                    Box(
                                        modifier = Modifier
                                            .track()
                                            .background(Color(0xFFE0E0E0)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .progress(sliderState = sliderState)
                                                .background(color = colorResource(id = R.color.nc_text_primary))
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun RollOverBroadcastTransactionScreenContentPreview() {
    RollOverBroadcastTransactionContent(
        uiState = RollOverBroadcastTransactionUiState(
            isFreeWallet = true,
            randomizeBroadcast = true
        ),
    )
}

@Composable
@Preview
private fun RollOverBroadcastTransactionScreenContentAssistedPreview() {
    RollOverBroadcastTransactionContent(
        uiState = RollOverBroadcastTransactionUiState(
            isFreeWallet = false,
            randomizeBroadcast = true
        ),
    )
}