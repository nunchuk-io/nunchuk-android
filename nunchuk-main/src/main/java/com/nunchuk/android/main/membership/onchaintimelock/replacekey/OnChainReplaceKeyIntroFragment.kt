package com.nunchuk.android.main.membership.onchaintimelock.replacekey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.main.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnChainReplaceKeyIntroFragment : Fragment() {
    private val args by navArgs<OnChainReplaceKeyIntroFragmentArgs>()

    private val viewModel: OnChainReplaceKeysViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var shouldShowIntro by remember { mutableStateOf(false) }

                LaunchedEffect(uiState.isDataLoaded) {
                    if (uiState.isDataLoaded) {
                        if (uiState.pendingReplaceXfps.isNotEmpty()) {
                            openReplaceKeysScreen()
                        } else {
                            shouldShowIntro = true
                        }
                    }
                }

                if (shouldShowIntro) {
                    OnChainReplaceKeyIntroScreen(
                        isReplaceKeyWithTimelock = args.isReplaceKeyWithTimelock,
                        onContinueClicked = ::openReplaceKeysScreen,
                    )
                }
            }
        }
    }

    private fun openReplaceKeysScreen() {
        findNavController().navigate(
            OnChainReplaceKeyIntroFragmentDirections.actionOnChainReplaceKeyIntroFragmentToOnChainReplaceKeysFragment(
                walletId = args.walletId,
                groupId = args.groupId,
                isReplaceKeyWithTimelock = args.isReplaceKeyWithTimelock
            )
        )
    }
}

@Composable
fun OnChainReplaceKeyIntroScreen(
    isLoading: Boolean = false,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onContinueClicked: () -> Unit = {},
    isReplaceKeyWithTimelock: Boolean = false,
) = NunchukTheme {
    if (isLoading) {
        NcLoadingDialog()
    }

    val titleRes = if (isReplaceKeyWithTimelock) {
        R.string.nc_replace_key_change_timelock
    } else {
        R.string.nc_replace_key_title
    }

    val descriptionRes = if (isReplaceKeyWithTimelock) {
        R.string.nc_replace_key_change_timelock_desc
    } else {
        R.string.nc_replace_key_intro_desc_simple
    }

    val noteRes = if (isReplaceKeyWithTimelock) {
        R.string.nc_replace_key_change_timelock_note
    } else {
        R.string.nc_replace_key_intro_note_simple
    }

    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_roll_over_illustrations,
                backIconRes = R.drawable.ic_close
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(id = titleRes),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(descriptionRes),
                style = NunchukTheme.typography.body
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                text = stringResource(noteRes),
                style = NunchukTheme.typography.body
            )
        }
    }
}

@Preview
@Composable
fun OnChainReplaceKeyIntroScreenPreview() {
    OnChainReplaceKeyIntroScreen()
}
