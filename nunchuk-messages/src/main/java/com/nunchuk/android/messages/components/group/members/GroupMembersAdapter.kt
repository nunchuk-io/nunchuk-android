package com.nunchuk.android.messages.components.group.members

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemSuggestContactBinding
import com.nunchuk.android.widget.util.inflate
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary

internal class GroupMembersAdapter : RecyclerView.Adapter<ContactViewHolder>() {

    internal var items: List<RoomMemberSummary> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactViewHolder(
        parent.inflate(R.layout.item_suggest_contact)
    )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class ContactViewHolder(
    itemView: View
) : BaseViewHolder<RoomMemberSummary>(itemView) {

    private val binding = ItemSuggestContactBinding.bind(itemView)

    override fun bind(data: RoomMemberSummary) {
        binding.avatar.text = data.displayName?.shorten().orEmpty()
        binding.name.text = data.displayName.orEmpty()
        binding.email.text = data.userId
        binding.email.isVisible = data.userId.isNotEmpty()
    }

}
