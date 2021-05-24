package com.nunchuk.android.signer.ss.confirm

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ItemConfirmSeedBinding
import com.nunchuk.android.widget.util.inflate

internal class ConfirmSeedAdapter(
    private val onItemUpdatedListener: (PhraseWordGroup) -> Unit
) : RecyclerView.Adapter<ConfirmSeedViewHolder>() {

    internal var items: List<PhraseWordGroup> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ConfirmSeedViewHolder(
        parent.inflate(R.layout.item_confirm_seed),
        onItemUpdatedListener
    )

    override fun onBindViewHolder(holder: ConfirmSeedViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

internal class ConfirmSeedViewHolder(
    itemView: View,
    val onItemUpdatedListener: (PhraseWordGroup) -> Unit
) : BaseViewHolder<PhraseWordGroup>(itemView) {

    private val binding = ItemConfirmSeedBinding.bind(itemView)

    override fun bind(data: PhraseWordGroup) {
        val wordNum = "Word #${data.index + 1}"
        binding.wordNum.text = wordNum
        binding.firstWord.text = data.firstWord.word
        binding.secondWord.text = data.secondWord.word
        binding.thirdWord.text = data.thirdWord.word

        bindState(binding.firstWord, data.firstWord.selected)
        bindState(binding.secondWord, data.secondWord.selected)
        bindState(binding.thirdWord, data.thirdWord.selected)

        binding.firstWord.setOnClickListener {
            onItemUpdatedListener(
                data.copy(
                    firstWord = data.firstWord.copy(selected = true),
                    secondWord = data.secondWord.copy(selected = false),
                    thirdWord = data.thirdWord.copy(selected = false)
                )
            )
        }
        binding.secondWord.setOnClickListener {
            onItemUpdatedListener(
                data.copy(
                    firstWord = data.firstWord.copy(selected = false),
                    secondWord = data.secondWord.copy(selected = true),
                    thirdWord = data.thirdWord.copy(selected = false)
                )
            )
        }
        binding.thirdWord.setOnClickListener {
            onItemUpdatedListener(
                data.copy(
                    firstWord = data.firstWord.copy(selected = false),
                    secondWord = data.secondWord.copy(selected = false),
                    thirdWord = data.thirdWord.copy(selected = true)
                )
            )
        }
    }

    private fun bindState(textView: TextView, selected: Boolean) {
        if (selected) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.nc_white_color))
            textView.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_dark_thin_background)
        } else {
            textView.setTextColor(ContextCompat.getColor(context, R.color.nc_black_color))
            textView.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_light_thin_background)
        }
    }

}
