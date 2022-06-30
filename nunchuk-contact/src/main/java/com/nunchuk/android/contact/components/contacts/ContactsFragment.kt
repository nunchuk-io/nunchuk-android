package com.nunchuk.android.contact.components.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.databinding.FragmentContactsBinding
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.model.Contact
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactsFragment : BaseFragment<FragmentContactsBinding>() {

    private val viewModel: ContactsViewModel by activityViewModels()

    private lateinit var adapter: ContactsAdapter

    private var emptyStateView: View? = null

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentContactsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        observeEvent()
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveContacts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emptyStateView = null
    }

    private fun setupViews() {
        adapter = ContactsAdapter {}
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), VERTICAL, false)
        binding.recyclerView.adapter = adapter
        binding.fab.setOnClickListener {
            navigator.openAddContactsScreen(childFragmentManager, viewModel::retrieveContacts)
        }
        binding.viewAll.setOnClickListener {
            navigator.openPendingContactsScreen(childFragmentManager, viewModel::retrieveContacts)
        }

        setEmptyState()

        emptyStateView?.findViewById<View>(R.id.btnAddContacts)?.setOnClickListener {
            navigator.openAddContactsScreen(childFragmentManager, viewModel::retrieveContacts)
        }
    }

    private fun setEmptyState() {
        emptyStateView = binding.viewStubEmptyState.inflate()
        emptyStateView?.findViewById<TextView>(R.id.tvEmptyStateDes)?.text = getString(R.string.nc_contact_no_contact)
        emptyStateView?.findViewById<ImageView>(R.id.ivContactAdd)?.setImageResource(R.drawable.ic_contact_add)
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: ContactsState) {
        updateContacts(state)
        updatePendingContacts(state.pendingContacts)
    }

    private fun updateContacts(state: ContactsState) {
        adapter.items = state.contacts
        emptyStateView?.isVisible = state.pendingContacts.isEmpty() && state.contacts.isEmpty()
    }

    private fun bindPendingContact(contact: Contact) {
        binding.email.text = contact.email
        binding.avatar.text = "${contact.name.first()}"
    }

    private fun updatePendingContacts(pendingContacts: List<Contact>) {
        val hasPendingContacts = pendingContacts.isNotEmpty()
        binding.pendingContacts.isVisible = hasPendingContacts
        binding.viewAll.isVisible = hasPendingContacts
        if (hasPendingContacts) {
            bindPendingContact(pendingContacts.first())
        }
    }

    companion object {
        fun newInstance() = ContactsFragment()
    }

}