package com.nunchuk.android.signer.components.details.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignMessageFragment : Fragment() {
    private val viewModel: SignMessageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                SignMessageContent()
            }
        }
    }
}

@Composable
private fun SignMessageContent() {
//    NunchukTheme {
//        Scaffold(topBar = NcTopAppBar(title =)) {
//
//        }
//    }
}