package com.nunchuk.android.main.components.tabs.chat.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.main.databinding.FragmentContactsBinding
import javax.inject.Inject

internal class ContactsFragment : BaseFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: ContactsViewModel by activityViewModels { factory }

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

        viewModel.retrieveContacts()
        observeEvent()
    }

    private fun setupViews() {
        adapter = ContactsAdapter {}
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleState(state: ContactsState) {
        adapter.items = state.contacts
    }

    private fun handleEvent(event: ContactsEvent) {
    }

    companion object {
        fun newInstance() = ContactsFragment()
    }

}