package com.nunchuk.android.messages.components.detail.viewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.messages.components.detail.RoomDetailViewModel
import com.nunchuk.android.messages.databinding.FragmentRoomMediaViewerBinding
import com.nunchuk.android.messages.util.toMediaSources
import com.nunchuk.android.utils.formatMessageDate
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import java.util.*

class RoomMediaViewerFragment : BaseFragment<FragmentRoomMediaViewerBinding>() {
    private val viewModel: RoomDetailViewModel by activityViewModels()
    private val args by navArgs<RoomMediaViewerFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        requireActivity().setLightStatusBar(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, true)
        requireActivity().setLightStatusBar()
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRoomMediaViewerBinding {
        return FragmentRoomMediaViewerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnDebounceClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        val timelineEvents = viewModel.getMediaMessages()
        val items = timelineEvents.toMediaSources()
        binding.pager.adapter = RoomMediaAdapter(items, viewLifecycleOwner)
        val initialIndex = items.indexOfFirst { it.eventId == args.initEventId }.coerceAtLeast(0)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                binding.tvImageCount.text = "${position.inc()} of ${items.size}"
                val eventId = items[position].eventId
                binding.tvFileInfo.text = timelineEvents.find { it.eventId == eventId }?.let {
                    "${it.senderInfo.disambiguatedDisplayName} ${it.root.originServerTs?.let { time -> Date(time).formatMessageDate() } ?: "-"}"
                }.orEmpty()
            }
        })
        binding.pager.setCurrentItem(initialIndex, false)
    }
}