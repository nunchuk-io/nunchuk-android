package com.nunchuk.android.wallet.components.coin.tag

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
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
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.tag.CoinTagColorUtil.hexColors
import com.nunchuk.android.wallet.util.hexToColor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinTagListFragment : Fragment() {

    private val viewModel: CoinTagListViewModel by viewModels()
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
                }, onSaveClick = {

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
                CoinTagListEvent.AddCoinToTagSuccess -> {
                    showSuccess(message = getString(R.string.nc_coin_updated))
                    findNavController().popBackStack()
                }
            }
        }

        setFragmentResultListener(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            bundle.getString(CoinTagSelectColorBottomSheetFragment.EXTRA_SELECT_COLOR)
                ?.let {
                    viewModel.changeColor(it)
                } ?: run {
                clearFragmentResult(CoinTagSelectColorBottomSheetFragment.REQUEST_KEY)
            }
        }

//        setFragmentResultListener(CoinTagDetailFragment.REQUEST_KEY) { _, bundle ->
//            bundle.parcelable<CoinTagAddition>(CoinTagDetailFragment.EXTRA_DELETE_TAG)
//                ?.let {
//                    viewModel.getCoinTags()
//                } ?: run {
//                clearFragmentResult(CoinTagDetailFragment.REQUEST_KEY)
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getCoinTags(loadSilent = true)
    }

    companion object {
        const val LIMIT_TAG_NAME = 40
        const val REQUEST_KEY = "CoinTagListFragment"
        const val EXTRA_SELECTED_TAG_LIST = "EXTRA_SELECTED_TAG_LIST"
    }
}

@Composable
fun CoinTagListScreen(
    viewModel: CoinTagListViewModel = viewModel(),
    @TagFlow.TagFlowInfo tagFlow: Int = TagFlow.NONE,
    onSelectColorClick: (String) -> Unit = {},
    onTagClick: (CoinTagAddition) -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoinTagListScreenContent(tags = state.tags,
        tagFlow = tagFlow,
        coinTagInputHolder = state.coinTagInputHolder,
        selectedCoinTags = state.selectedCoinTags,
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
    selectedCoinTags: List<Int> = emptyList(),
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
                    isDisableElevation = true
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
                        enabled = selectedCoinTags.isNotEmpty(),
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
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text
            ),
            title = "",
            value = input,
            visualTransformation = MaxLengthTransformation(CoinTagListFragment.LIMIT_TAG_NAME),
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
fun TagItem(
    id: Int = -1,
    name: String = "",
    color: String = "",
    numCoins: Int = 0,
    checked: Boolean = false,
    tagFlow: Int = TagFlow.NONE,
    onTagClick: () -> Unit = {},
    onCheckedChange: ((Boolean) -> Unit) = {}
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable {
                if (tagFlow == TagFlow.VIEW) {
                    onTagClick()
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 48.dp)
                .clip(CircleShape)
                .background(color = color.hexToColor())
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = name,
                style = NunchukTheme.typography.body
            )
            Text(
                text = stringResource(id = R.string.nc_num_coins_data, numCoins.toString()),
                modifier = Modifier.padding(top = 4.dp),
                style = NunchukTheme.typography.bodySmall
            )
        }
        if (tagFlow == TagFlow.VIEW) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = ""
            )
        } else {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private class MaxLengthTransformation(private val maxLength: Int) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val truncatedText = if (text.text.length > maxLength) {
            text.text.substring(0, maxLength)
        } else {
            text.text
        }
        return TransformedText(AnnotatedString(truncatedText), OffsetMapping.Identity)
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
