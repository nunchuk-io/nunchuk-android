package com.nunchuk.android.main.membership.byzantine.groupdashboard.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.main.membership.byzantine.payment.qr.QrDetailScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QrDetailFragment : Fragment() {
    private val agrs: QrDetailFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
               QrDetailScreen(address = agrs.address)
            }
        }
    }
}