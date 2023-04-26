package com.nunchuk.android.transaction.components.send.confirmation.tag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcPrimaryButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TagItem
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.transaction.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssignTagFragment : BaseComposeBottomSheet() {
    private val viewModel: AssignTagViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AssignTagScreen(viewModel,
                    onDismiss = {
                        dismissAllowingStateLoss()
                    })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is AssignTagEvent.AssignTagFailed -> showError(event.message)
                        AssignTagEvent.AssignTagSuccess -> {
                            showSuccess(getString(R.string.nc_tags_assigned))
                            dismissAllowingStateLoss()
                        }
                    }
                }
        }
    }

    companion object {
        const val KEY_WALLET_ID = "wallet_id"
        const val KEY_COIN = "coin"
        const val KEY_TAGS = "tags"

        fun newInstance(
            walletId: String,
            coin: UnspentOutput,
            tags: List<CoinTag>
        ): AssignTagFragment {
            return AssignTagFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_WALLET_ID, walletId)
                    putParcelable(KEY_COIN, coin)
                    putParcelableArrayList(KEY_TAGS, ArrayList(tags))
                }
            }
        }
    }
}

@Composable
private fun AssignTagScreen(
    viewModel: AssignTagViewModel = viewModel(),
    onDismiss: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AssignTagContent(
        tags = state.tagAdditions,
        selectedCoinTags = state.selectedCoinTags,
        onCheckedChange = viewModel::onCheckedChange,
        onDismiss = onDismiss,
        onAssignTag = viewModel::onAssignTag,
        onSelectedAll = viewModel::toggleSelected
    )
}

@Composable
private fun AssignTagContent(
    tags: List<CoinTagAddition> = emptyList(),
    selectedCoinTags: Set<Int> = emptySet(),
    onCheckedChange: (id: Int, isChecked: Boolean) -> Unit = { _, _ -> },
    onAssignTag: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onSelectedAll: (isSelectAll: Boolean) -> Unit = {}
) {
    val isSelectAll = selectedCoinTags.size == tags.size
    NunchukTheme {
        Column(modifier = Modifier.fillMaxHeight(0.9f)) {
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                text = stringResource(R.string.nc_transaction_generate_change_coin_title),
                style = NunchukTheme.typography.body,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                    text = stringResource(R.string.nc_tags_from_parent_coins),
                    style = NunchukTheme.typography.title,
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp).clickable { onSelectedAll(isSelectAll) },
                    text = if (isSelectAll)
                        stringResource(id = R.string.nc_unselect_all)
                    else
                        stringResource(id = R.string.nc_select_all),
                    style = NunchukTheme.typography.title,
                    textDecoration = TextDecoration.Underline
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1.0f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tags) { tag ->
                    TagItem(
                        id = tag.coinTag.id,
                        name = tag.coinTag.name,
                        color = tag.coinTag.color,
                        numCoins = tag.numCoins,
                        checked = selectedCoinTags.contains(tag.coinTag.id),
                        tagFlow = TagFlow.NONE,
                        onCheckedChange = {
                            onCheckedChange(tag.coinTag.id, it)
                        }
                    )
                }
            }

            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                onClick = onAssignTag
            ) {
                Text(text = stringResource(R.string.nc_assign_tags))
            }
            NcPrimaryButton(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(), onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.nc_dont_assign_any_tags))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssignTagScreenPreview() {
    val tags = arrayListOf<CoinTagAddition>()
    tags.apply {
        add(CoinTagAddition(CoinTag(name = "#111", color = "#CF4018")))
        add(CoinTagAddition(CoinTag(name = "#222", color = "#CF4018")))
        add(CoinTagAddition(CoinTag(name = "#333", color = "#CF4018")))
        add(CoinTagAddition(CoinTag(name = "#444", color = "#CF4018")))
    }
    AssignTagContent(tags = tags)
}