package com.nunchuk.android.signer.satscard.unseal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ItemUnsealedSlotBinding

class SatsCardUnsealSlotAdapter(private val slots: List<SatsCardSlot>) : RecyclerView.Adapter<SatsCardUnsealSlotHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatsCardUnsealSlotHolder {
        return SatsCardUnsealSlotHolder(ItemUnsealedSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SatsCardUnsealSlotHolder, position: Int) {
        holder.bind(slots[position])
    }

    override fun getItemCount(): Int = slots.size
}

class SatsCardUnsealSlotHolder(private val binding: ItemUnsealedSlotBinding) : RecyclerView.ViewHolder(binding.root) {
    private val qrCodeSize = binding.root.context.resources.getDimensionPixelSize(R.dimen.nc_padding_36)

    fun bind(slot: SatsCardSlot) {
        binding.qrCode.setImageBitmap(slot.address.orEmpty().convertToQRCode(qrCodeSize, qrCodeSize))
        binding.tvAddress.text = slot.address
        binding.tvBalanceBtc.text = slot.balance.getBTCAmount()
    }
}