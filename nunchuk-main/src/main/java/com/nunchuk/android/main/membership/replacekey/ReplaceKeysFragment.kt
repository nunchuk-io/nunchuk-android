package com.nunchuk.android.main.membership.replacekey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R

class ReplaceKeysFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ReplaceKeysScreen()
            }
        }
    }
}

@Composable
private fun ReplaceKeysScreen(
    viewModel: ReplaceKeysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReplaceKeysContent(uiState)
}

@Composable
private fun ReplaceKeysContent(
    uiState: ReplaceKeysUiState = ReplaceKeysUiState(),
    onReplaceKeyClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onReplaceKeyClicked
                ) {
                    Text(text = stringResource(R.string.nc_continue_to_create_a_new_wallet))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.nc_which_key_would_you_like_to_replace),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_replace_one_or_multiple_keys),
                    style = NunchukTheme.typography.body
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.signers) { item ->
                        ReplaceKeyCard(
                            item = item,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReplaceKeyCard(
    item: SignerModel,
    modifier: Modifier = Modifier,
    onReplaceClicked: (data: SignerModel) -> Unit = {},
) {
    Box(
        modifier = modifier.background(
            color = colorResource(id = R.color.nc_beeswax_tint),
            shape = RoundedCornerShape(8.dp)
        ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            NcCircleImage(
                resId = item.toReadableDrawableResId(),
                color = colorResource(id = R.color.nc_white_color)
            )
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.name,
                    style = NunchukTheme.typography.body
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    NcTag(
                        label = item.toReadableSignerType(context = LocalContext.current),
                        backgroundColor = colorResource(
                            id = R.color.nc_whisper_color
                        ),
                    )
                    if (item.isShowAcctX()) {
                        NcTag(
                            modifier = Modifier.padding(start = 4.dp),
                            label = stringResource(R.string.nc_acct_x, item.index),
                            backgroundColor = colorResource(
                                id = R.color.nc_whisper_color
                            ),
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = item.getXfpOrCardIdLabel(),
                    style = NunchukTheme.typography.bodySmall
                )
            }
            NcOutlineButton(
                modifier = Modifier.height(36.dp),
                onClick = { onReplaceClicked(item) },
            ) {
                Text(text = stringResource(R.string.nc_replace))
            }
        }
    }
}

@Composable
@Preview
private fun ReplaceKeysContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    ReplaceKeysContent(
        uiState = ReplaceKeysUiState(signers = signers)
    )
}