package com.nunchuk.android.main.components.tabs.services.keyrecovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.*
import com.nunchuk.android.widget.NCInputDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KeyRecoveryFragment : Fragment() {

    private val viewModel: KeyRecoveryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                KeyRecoveryScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            when (it) {
                is KeyRecoveryEvent.ItemClick -> {
                    enterPasswordDialog(it.item)
                }
                is KeyRecoveryEvent.Loading -> showOrHideLoading(loading = it.isLoading)
                is KeyRecoveryEvent.CheckPasswordSuccess -> {
                    when(it.item) {
                        is KeyRecoveryActionItem.StartKeyRecovery -> {
                            findNavController().navigate(KeyRecoveryFragmentDirections.actionKeyRecoveryFragmentToKeyRecoveryIntroFragment(verifyToken = it.verifyToken))
                        }
                        is KeyRecoveryActionItem.UpdateRecoveryQuestion -> {
                            findNavController().navigate(
                                KeyRecoveryFragmentDirections.actionKeyRecoveryFragmentToRecoveryQuestionFragment(
                                    isRecoveryFlow = true,
                                    verifyToken = it.verifyToken
                                )
                            )
                        }
                    }
                }
                is KeyRecoveryEvent.ProcessFailure -> {
                    showError(it.message)
                }
            }
        }
    }

    private fun enterPasswordDialog(item: KeyRecoveryActionItem) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                viewModel.confirmPassword(it, item)
            }
        )
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun KeyRecoveryScreen(
    viewModel: KeyRecoveryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KeyRecoveryScreenContent(state, onItemClick = {
        viewModel.onItemClick(it)
    })
}

@Composable
fun KeyRecoveryScreenContent(
    state: KeyRecoveryState = KeyRecoveryState(), onItemClick: (KeyRecoveryActionItem) -> Unit = {}
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
                    title = stringResource(R.string.nc_key_recovery),
                    textStyle = NunchukTheme.typography.titleLarge
                )

                state.actionItems.forEach {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)
                            .clickable {
                                onItemClick(it)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = it.title),
                            modifier = Modifier.weight(weight = 1f),
                            style = NunchukTheme.typography.body
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun KeyRecoveryScreenContentPreview() {
    KeyRecoveryScreenContent()
}