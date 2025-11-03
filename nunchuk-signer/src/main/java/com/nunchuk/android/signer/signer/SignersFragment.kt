package com.nunchuk.android.signer.signer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.nav.NunchukNavigator
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
        navigator.openSignerIntroScreen(activityContext = requireActivity())
    }
}