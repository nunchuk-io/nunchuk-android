package com.nunchuk.android.main.components.tabs.chat.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.main.databinding.FragmentMessagesBinding
import com.nunchuk.android.messages.model.MessageProvider

internal class MessagesFragment : Fragment() {

    private lateinit var adapter: MessagesAdapter

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        adapter = MessagesAdapter {}
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), VERTICAL, false)
        binding.recyclerView.adapter = adapter

        // FIXME remove this mock data
        adapter.items = MessageProvider.messages()
    }

    companion object {
        fun newInstance() = MessagesFragment()
    }

}