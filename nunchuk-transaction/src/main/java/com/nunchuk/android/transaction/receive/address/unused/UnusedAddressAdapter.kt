package com.nunchuk.android.transaction.receive.address.unused

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.ItemUnusedAddressBinding

class UnusedAddressAdapter(private val context: Context, private val listener: (String?) -> Unit) : PagerAdapter() {

    internal var items = emptyList<String>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount(): Int {
        return items.size + 1
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = if (position == items.size) createEmptyItem(layoutInflater) else createNormalItem(layoutInflater, position)
        val vp = container as ViewPager
        vp.addView(view, 0)
        return view
    }

    private fun createNormalItem(layoutInflater: LayoutInflater, position: Int): View {
        val binding = ItemUnusedAddressBinding.inflate(layoutInflater, null, false)
        val address = items[position]
        binding.qrCode.setImageBitmap(address.convertToQRCode())
        binding.address.text = address
        binding.root.setOnClickListener { listener(address) }
        return binding.root
    }

    private fun createEmptyItem(layoutInflater: LayoutInflater): View {
        val view = layoutInflater.inflate(R.layout.item_generate_address, null)
        view.setOnClickListener { listener(null) }
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val vp = container as ViewPager
        val view = `object` as View
        vp.removeView(view)
    }

}