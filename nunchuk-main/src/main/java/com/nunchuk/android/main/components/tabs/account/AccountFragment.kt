package com.nunchuk.android.main.components.tabs.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseFragment
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.main.databinding.FragmentAccountBinding
import javax.inject.Inject

internal class AccountFragment : BaseFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    private val accountViewModel: AccountViewModel by lazy {
        ViewModelProviders.of(this, factory).get(AccountViewModel::class.java)
    }

    private var _binding: FragmentAccountBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val root: View = binding.root

        accountViewModel.text.observe(viewLifecycleOwner, {
            binding.textAccount.text = it
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}