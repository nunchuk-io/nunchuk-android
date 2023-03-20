package com.nunchuk.android.wallet.components.coin.tagdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.tag.CoinTagListFragment
import com.nunchuk.android.wallet.components.coin.util.MaxLengthTransformation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditTagNameBottomSheetFragment : BaseComposeBottomSheet() {

    private val viewModel by viewModels<EditTagNameBottomSheetViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NunchukTheme {
                    EditTagNameBottomSheetScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                EditTagNameBottomSheetEvent.UpdateTagNameSuccess -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(EXTRA_TAG_NAME to viewModel.getCoinTagName())
                    )
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "EditTagNameBottomSheetFragment"
        const val EXTRA_TAG_NAME = "tag_name"
    }
}


@Composable
private fun EditTagNameBottomSheetScreen(
    viewModel: EditTagNameBottomSheetViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    EditTagNameBottomSheetScreenContent(
        coinTag = state.coinTag,
        errorMsg = state.errorMsg,
        onSaveClick = {
            viewModel.onSaveClick()
        },
        onValueChange = {
            viewModel.updateTagName(it)
        })
}

@Composable
private fun EditTagNameBottomSheetScreenContent(
    coinTag: CoinTag = CoinTag(),
    errorMsg: String = "",
    onSaveClick: () -> Unit = {},
    onValueChange: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = stringResource(id = R.string.nc_text_save),
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable {
                        if (coinTag.name.isBlank() ||
                            coinTag.name.isBlank().not() && coinTag.name.contains(" ").not()
                        ) {
                            onSaveClick()
                        }
                    },
                style = NunchukTheme.typography.title,
                textDecoration = TextDecoration.Underline,
            )

        }

        Text(
            text = stringResource(id = R.string.nc_tag_name),
            style = NunchukTheme.typography.title,
            modifier = Modifier.padding(top = 14.dp)
        )
        Column() {
            BasicTextField(
                value = coinTag.name,
                onValueChange = onValueChange,
                visualTransformation = MaxLengthTransformation(
                    CoinTagListFragment.LIMIT_TAG_NAME,
                    "#"
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(top = 8.dp)
            )
        }
        if (errorMsg.isBlank().not()) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp),
                    painter = painterResource(id = R.drawable.ic_error_outline),
                    contentDescription = "Error icon",
                    tint = colorResource(id = R.color.nc_orange_color)
                )
                Text(
                    text = errorMsg, style = NunchukTheme.typography.bodySmall.copy(
                        color = colorResource(
                            id = R.color.nc_orange_color
                        )
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun EditTagNameBottomSheetScreenContentPreview() {
    NunchukTheme {
        EditTagNameBottomSheetScreenContent(coinTag = CoinTag(name = "#aaa"))
    }
}
