package com.nunchuk.android.wallet.components.coin.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getBtcFormatDate
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.component.CoinBadge
import com.nunchuk.android.wallet.components.coin.detail.component.CoinTransactionCard
import com.nunchuk.android.wallet.components.coin.detail.component.TagHorizontalList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CoinDetailFragment : Fragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator
    private val viewModel: CoinDetailViewModel by viewModels()
    private val args by navArgs<CoinDetailFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CoinDetailScreen(viewModel,
                    args = args,
                    onShowMore = {
                        BottomSheetOption.newInstance(
                            listOf(
                                SheetOption(
                                    type = SheetOptionType.TYPE_SHOW_OUTPOINT,
                                    label = context.getString(R.string.nc_show_out_point)
                                )
                            )
                        ).show(childFragmentManager, "BottomSheetOption")
                    },
                    onViewTransactionDetail = {
                        navigator.openTransactionDetailsScreen(
                            activityContext = requireActivity(),
                            walletId = args.walletId,
                            txId = args.output.txid,
                        )
                    }
                )
            }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_SHOW_OUTPOINT ->
                OutpointBottomSheet.newInstance("${args.output.txid}${args.output.vout}")
                    .show(
                        childFragmentManager, "OutpointBottomSheet"
                    )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->

            }
        }
    }
}

@Composable
private fun CoinDetailScreen(
    viewModel: CoinDetailViewModel = viewModel(),
    args: CoinDetailFragmentArgs,
    onShowMore: () -> Unit = {},
    onViewTransactionDetail: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoinDetailContent(
        output = args.output,
        transaction = state.transaction,
        onShowMore = onShowMore,
        onViewTransactionDetail = onViewTransactionDetail,
    )
}

@Composable
private fun CoinDetailContent(
    output: UnspentOutput = UnspentOutput(),
    coinTags: Map<Int, CoinTag> = emptyMap(),
    transaction: Transaction = Transaction(),
    onShowMore: () -> Unit = {},
    onViewTransactionDetail: () -> Unit = {},
    onUpdateTag: () -> Unit = {},
) {
    val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
    NunchukTheme {
        Scaffold(topBar = {
            Box(
                modifier = Modifier
                    .background(color = colorResource(id = R.color.nc_denim_tint_color))
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    elevation = 0.dp,
                    navigationIcon = {
                        IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Text(text = stringResource(R.string.nc_coin_detail))
                    },
                    backgroundColor = colorResource(id = R.color.nc_denim_tint_color),
                    actions = {
                        IconButton(onClick = onShowMore) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More"
                            )
                        }
                    },
                )
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .background(color = colorResource(id = R.color.nc_denim_tint_color))
                ) {
                    CoinBadgeRow(output)
                    Text(
                        text = output.amount.getBTCAmount(),
                        style = NunchukTheme.typography.heading,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = output.time.getBtcFormatDate(),
                        style = NunchukTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.nc_parent_transaction),
                            style = NunchukTheme.typography.title,
                        )

                        Text(
                            modifier = Modifier.clickable { onViewTransactionDetail() },
                            text = stringResource(R.string.nc_message_transaction_view_details),
                            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline),
                        )
                    }

                    CoinTransactionCard(transaction)
                }

                TagHorizontalList(
                    modifier = Modifier.padding(top = 8.dp),
                    output = output,
                    onUpdateTag = onUpdateTag,
                    coinTags = coinTags
                )
            }
        }
    }
}

@Composable
private fun CoinBadgeRow(output: UnspentOutput) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp, horizontal = 12.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        if (output.isChange) {
            CoinBadge(modifier = Modifier.padding(end = 4.dp)) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    text = stringResource(R.string.nc_change),
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 12.sp)
                )
            }
        }

        if (output.isLocked) {
            CoinBadge(
                modifier = Modifier.padding(end = 4.dp),
                border = 0.dp,
                backgroundColor = NcColor.whisper
            ) {
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Locked"
                )
                Text(
                    modifier = Modifier.padding(
                        start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp
                    ),
                    text = stringResource(R.string.nc_locked),
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 12.sp)
                )
            }
        }

        if (output.scheduleTime > 0L) {
            CoinBadge(
                border = 0.dp,
                backgroundColor = NcColor.whisper
            ) {
                Icon(
                    modifier = Modifier.padding(start = 10.dp),
                    painter = painterResource(id = R.drawable.ic_schedule),
                    contentDescription = "Locked"
                )
                Text(
                    modifier = Modifier.padding(
                        start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp
                    ),
                    text = stringResource(R.string.nc_scheduled_transaction),
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 12.sp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CoinDetailScreenPreview() {
    NunchukTheme {
        CoinDetailContent(

        )
    }
}