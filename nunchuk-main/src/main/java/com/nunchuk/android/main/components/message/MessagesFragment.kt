package com.nunchuk.android.main.components.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseFragment
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.main.databinding.FragmentMessagesBinding
import javax.inject.Inject

internal class MessagesFragment : BaseFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    private val messageViewModel: MessageViewModel by lazy {
        ViewModelProviders.of(this, factory).get(MessageViewModel::class.java)
    }
    private var _binding: FragmentMessagesBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textMessage
        messageViewModel.text.observe(viewLifecycleOwner, {
            textView.text = it
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}