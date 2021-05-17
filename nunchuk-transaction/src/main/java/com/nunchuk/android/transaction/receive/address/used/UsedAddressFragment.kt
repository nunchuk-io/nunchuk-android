package com.nunchuk.android.transaction.receive.address.used

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.receive.address.AddressFragmentArgs
import dagger.android.support.DaggerFragment
import javax.inject.Inject

internal class UsedAddressFragment : DaggerFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: UsedAddressViewModel by lazy {
        ViewModelProviders.of(requireActivity(), factory).get(UsedAddressViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_used_address, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
    }

    private fun initViews() {

    }

    companion object {

        fun newInstance(walletId: String) = UsedAddressFragment().apply {
            arguments = AddressFragmentArgs(walletId = walletId).buildBundle()
        }

    }

}