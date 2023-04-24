package com.nunchuk.android.wallet.components.coin.tag

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TagItem
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.hexToColor
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.wallet.components.coin.tag.CoinTagColorUtil.hexColors
import com.nunchuk.android.wallet.components.coin.util.MaxLengthTransformation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinTagListFragment : Fragment() {

    private val viewModel: CoinTagListViewModel by viewModels()
    private val coinListViewModel: CoinListViewModel by activityViewModels()
    private val args: CoinTagListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CoinTagListScreen(viewModel, tagFlow = args.tagFlow, onSelectColorClick = {
                    findNavController().navigate(
                        CoinTagListFragmentDirections.actionCoinTagListFragmentToCoinTagSelectColorBottomSheetFragment(
                            selectedColor = it
                        )
                    )
                }, onTagClick = {
                    findNavController().navigate(
                        CoinTagListFragmentDirections.actionCoinTagListFragmentToCoinTagDetailFragment(
                            walletId = args.walletId,
                            coinTag = it.coinTag
                        )
                    )
                })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is CoinTagListEvent.Error -> showError(message = event.message)
                is CoinTagListEvent.Loading -> showOrHideLoading(loading = event.show)
                is CoinTagListEvent.AddCoinToTagSuccess -> {
                    handleTagInfoChange()
                    showSuccess(
                        message = if (event.numsCoin > 1) getString(R.string.nc_coins_updated) else getString(
                            R.string.nc_coin_updated
                        )
                    )
                    findNavController().popBackStack()
                }

                CoinTagListEvent.CreateTagSuccess -> coinListViewModel.refresh()
                CoinTagListEvent.ExistedTagError -> showError(message = getString(R.string.nc_tag_name_already_exists))
            }
        }

        flowObserver(coinListViewModel.state) { coinListState ->
            val numberOfCoinByTagId = mutableMapOf<Int, Int>()
            coinListState.coins.forEach { output ->
                output.tags.forEach { tagId ->
                    numberOfCoinByTagId[tagId] = numberOfCoinByTagId.getOrPut(tagId) { 0 } + 1
                }
            }
            viewModel.updateCoins(coinListState.tags.values.toList(), numberOfCoinByTagId)
        }

        setFragmentResultListener(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            bundle.getString(CoinTagSelectColorBottomSheetFragment.EXTRA_SELECT_COLOR)
                ?.let {
                    viewModel.changeColor(it)
                } ?: run {
                clearFragmentResult(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY)
            }
        }
    }

    private fun handleTagInfoChange() {
        coinListViewModel.refresh()
        requireActivity().setResult(Activity.RESULT_OK)
    }

    companion object {
        const val LIMIT_TAG_NAME = 40
        const val REQUEST_KEY = "CoinTagListFragment"
    }
}

@Composable
fun CoinTagListScreen(
    viewModel: CoinTagListViewModel = viewModel(),
    @TagFlow.TagFlowInfo tagFlow: Int = TagFlow.NONE,
    onSelectColorClick: (String) -> Unit = {},
    onTagClick: (CoinTagAddition) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoinTagListScreenContent(tags = state.tags.sortedBy { it.coinTag.name },
        tagFlow = tagFlow,
        coinTagInputHolder = state.coinTagInputHolder,
        selectedCoinTags = state.selectedCoinTags,
        enableSaveButton = viewModel.enableButtonSave(),
        onSaveClick = {
            viewModel.addCoinTag()
        },
        onValueChange = {
            viewModel.onInputValueChange(it)
        }, onDoneClick = {
            viewModel.onDoneInputClick()
        },
        onCheckedChange = { id, checked ->
            viewModel.onCheckedChange(id, checked)
        },
        onCreateNewTagClick = {
            viewModel.onCreateNewCoinTagClick()
        }, onSelectColorClick = {
            onSelectColorClick(viewModel.getCoinTagInputHolder()?.color.orEmpty())
        }, onTagClick = onTagClick
    )
}

@Composable
fun CoinTagListScreenContent(
    tags: List<CoinTagAddition> = emptyList(),
    coinTagInputHolder: CoinTag? = null,
    tagFlow: Int = TagFlow.NONE,
    selectedCoinTags: Set<Int> = hashSetOf(),
    enableSaveButton: Boolean = false,
    onSaveClick: () -> Unit = {},
    onValueChange: (value: String) -> Unit = {},
    onDoneClick: () -> Unit = {},
    onCreateNewTagClick: () -> Unit = {},
    onSelectColorClick: () -> Unit = {},
    onTagClick: (CoinTagAddition) -> Unit = {},
    onCheckedChange: ((Int, Boolean) -> Unit) = { _, _ -> }
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(
                    stringResource(id = R.string.nc_coin_tags),
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = false,
                    elevation = 0.dp,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    }
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                        .clickable {
                            if (coinTagInputHolder == null) {
                                onCreateNewTagClick()
                            }
                        },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_circle),
                        contentDescription = ""
                    )
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = stringResource(id = R.string.nc_create_a_new_tag),
                        style = NunchukTheme.typography.title
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (coinTagInputHolder != null) {
                        item {
                            InputTagItem(
                                input = coinTagInputHolder.name,
                                color = coinTagInputHolder.color,
                                onValueChange = onValueChange,
                                onDoneClick = {
                                    val input = coinTagInputHolder.name
                                    if (input.isBlank() ||
                                        input.isBlank().not() && input.contains(" ").not()
                                    ) {
                                        onDoneClick()
                                    }
                                }, onSelectColorClick = onSelectColorClick
                            )
                        }
                    }
                    items(tags) { tag ->
                        TagItem(
                            id = tag.coinTag.id,
                            name = tag.coinTag.name,
                            color = tag.coinTag.color,
                            numCoins = tag.numCoins,
                            checked = selectedCoinTags.contains(tag.coinTag.id),
                            onTagClick = { onTagClick(tag) },
                            tagFlow = tagFlow,
                            onCheckedChange = {
                                onCheckedChange(tag.coinTag.id, it)
                            }
                        )
                    }
                }
                if (tagFlow == TagFlow.ADD) {
                    NcPrimaryDarkButton(
                        enabled = enableSaveButton,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onSaveClick,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_save))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun InputTagItem(
    input: String = "",
    color: String = "",
    onValueChange: (value: String) -> Unit = {},
    onDoneClick: () -> Unit = {},
    onSelectColorClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 48.dp)
                .clip(CircleShape)
                .background(color = color.hexToColor())
                .clickable {
                    onSelectColorClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_select_color),
                contentDescription = ""
            )
        }

        NcTextField(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Text
            ),
            title = "",
            value = input,
            visualTransformation = MaxLengthTransformation(CoinTagListFragment.LIMIT_TAG_NAME, "#"),
            onValueChange = { newValue ->
                if (newValue.length <= CoinTagListFragment.LIMIT_TAG_NAME) {
                    onValueChange(newValue)
                }
            },
        )

        Text(
            text = stringResource(id = R.string.nc_text_done),
            modifier = Modifier
                .padding(start = 12.dp)
                .clickable {
                    onDoneClick()
                },
            style = NunchukTheme.typography.title,
            textDecoration = TextDecoration.Underline
        )
    }
}

@Preview
@Composable
private fun CoinTagListScreenContentPreview() {
    val tags = arrayListOf<CoinTagAddition>()
    tags.apply {
        add(CoinTagAddition(CoinTag(name = "#111", color = hexColors[0])))
        add(CoinTagAddition(CoinTag(name = "#222", color = hexColors[1])))
        add(CoinTagAddition(CoinTag(name = "#333", color = hexColors[2])))
        add(CoinTagAddition(CoinTag(name = "#444", color = hexColors[3])))
    }
    CoinTagListScreenContent(tags = tags)
}
