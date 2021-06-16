package com.nunchuk.android.main.components.tabs.chat.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.main.databinding.FragmentContactsBinding
import com.nunchuk.android.messages.model.ContactsProvider

internal class ContactsFragment : Fragment() {

    private lateinit var adapter: ContactsAdapter

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        adapter = ContactsAdapter {}
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), VERTICAL, false)
        binding.recyclerView.adapter = adapter

        //FIXME remove this mock data
        adapter.items = ContactsProvider.contacts()
    }

    companion object {
        fun newInstance() = ContactsFragment()
    }

}