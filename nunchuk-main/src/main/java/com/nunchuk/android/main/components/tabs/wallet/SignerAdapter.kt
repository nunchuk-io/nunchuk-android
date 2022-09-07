package com.nunchuk.android.main.components.tabs.wallet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.core.util.toReadableSignerTypeDrawable
import com.nunchuk.android.signer.databinding.ItemSignerBinding

class SignerAdapter(private val callback: (signer: SignerModel) -> Unit) : ListAdapter<SignerModel, SingerViewHolder>(ITEM_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingerViewHolder {
        return SingerViewHolder(callback, ItemSignerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SingerViewHolder, position: Int) {
        holder.bindItem(getItem(position))
    }

    companion object {
        private val ITEM_CALLBACK = object : DiffUtil.ItemCallback<SignerModel>() {
            override fun areItemsTheSame(oldItem: SignerModel, newItem: SignerModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SignerModel, newItem: SignerModel): Boolean {
                return oldItem.equals(newItem)
            }
        }
    }
}

class SingerViewHolder(
    private val callback: (signer: SignerModel) -> Unit,
    private val binding: ItemSignerBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindItem(model: SignerModel) {
        val xfpValue = "XFP: ${model.fingerPrint}"

        binding.signerName.text = model.name
        binding.xpf.text = xfpValue
        binding.signerType.text = model.toReadableSignerType(binding.root.context)
        binding.icPin.setImageDrawable(model.toReadableSignerTypeDrawable(binding.root.context))
        binding.root.setOnClickListener { callback(model) }
    }
}