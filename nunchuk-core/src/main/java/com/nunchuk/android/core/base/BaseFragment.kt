package com.nunchuk.android.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import dagger.android.support.DaggerFragment
import javax.inject.Inject

abstract class BaseFragment<out Binding : ViewBinding> : DaggerFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    protected lateinit var navigator: NunchukNavigator

    private var _binding: Binding? = null

    protected val binding get() = _binding!!

    abstract fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): Binding

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = initializeBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}