package com.nunchuk.android.signer.satscard.qr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.signer.databinding.FragmentSatscardSlotQrBinding


class SatsCardSlotQrFragment : BaseFragment<FragmentSatscardSlotQrBinding>() {
    private val args: SatsCardSlotQrFragmentArgs by navArgs()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSatscardSlotQrBinding {
        return FragmentSatscardSlotQrBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        val width = resources.displayMetrics.widthPixels
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        binding.qrCode.setImageBitmap(args.slot.address.orEmpty().convertToQRCode(width, width))
        binding.tvAddress.text = args.slot.address
        binding.tvBalanceBtc.text = args.slot.balance.getBTCAmount()
    }
}