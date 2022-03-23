package com.nunchuk.android.settings.devices

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.profile.UserDeviceResponse
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.ItemUserDeviceBinding
import com.nunchuk.android.widget.util.inflate

internal class UserDevicesAdapter(
    private val listener: (UserDeviceResponse) -> Unit
) : RecyclerView.Adapter<UserDeviceViewHolder>() {

    internal var items: List<UserDeviceResponse> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserDeviceViewHolder(
        parent.inflate(R.layout.item_user_device),
        listener
    )

    override fun onBindViewHolder(holder: UserDeviceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class UserDeviceViewHolder(
    itemView: View,
    val onItemSelectedListener: (UserDeviceResponse) -> Unit
) : BaseViewHolder<UserDeviceResponse>(itemView) {

    private val binding = ItemUserDeviceBinding.bind(itemView)

    override fun bind(data: UserDeviceResponse) {
        binding.deviceName.text = data.name
        binding.deviceInfo.isVisible = adapterPosition == 0
        binding.btnMore.isVisible = adapterPosition != 0
        binding.btnMore.setOnClickListener { onItemSelectedListener.invoke(data) }
    }

}
