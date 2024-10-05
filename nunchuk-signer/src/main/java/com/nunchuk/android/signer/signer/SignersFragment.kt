package com.nunchuk.android.signer.signer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignersFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by activityViewModels<SignersViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        SignersContent(
            uiState,
            onSignerClick = ::openSignerInfoScreen,
            onAddSignerClick = ::openSignerIntroScreen
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAllSigners()
    }

    private fun openSignerInfoScreen(
        signer: SignerModel,
    ) {
        navigator.openSignerInfoScreen(
            activityContext = requireActivity(),
            isMasterSigner = signer.isMasterSigner,
            id = signer.id,
            masterFingerprint = signer.fingerPrint,
            name = signer.name,
            type = signer.type,
            derivationPath = signer.derivationPath,
        )
    }

    private fun openSignerIntroScreen() {
        navigator.openSignerIntroScreen(requireActivity())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignersContent(
    uiState: SignerUiState = SignerUiState(),
    onAddSignerClick: () -> Unit = {},
    onSignerClick: (SignerModel) -> Unit = {},
) {
    NunchukTheme(isSetStatusBar = false) {
        NcScaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                    title = {
                        Text(
                            text = stringResource(R.string.nc_title_signers),
                            style = NunchukTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    actions = {
                        IconButton(onClick = onAddSignerClick) {
                            Icon(
                                painterResource(id = R.drawable.ic_add_dark),
                                contentDescription = "Add Signer",
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(vertical = 4.dp),
            ) {
                items(uiState.signers.orEmpty()) { signer ->
                    SignerCard(item = signer, onSignerSelected = onSignerClick)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignersContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    SignersContent(SignerUiState(signers = signers))
}
