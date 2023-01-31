package com.nunchuk.android.messages.components.detail.viewer

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.nunchuk.android.core.base.BasePermissionFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.messages.components.detail.RoomDetailViewModel
import com.nunchuk.android.messages.databinding.FragmentRoomMediaViewerBinding
import com.nunchuk.android.messages.util.toMediaSources
import com.nunchuk.android.utils.formatMessageDate
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class RoomMediaViewerFragment : BasePermissionFragment<FragmentRoomMediaViewerBinding>() {
    private val sharedViewModel: RoomDetailViewModel by activityViewModels()
    private val viewModel: RoomMediaViewerViewModel by viewModels()
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
        val timelineEvents = sharedViewModel.getMediaMessages()
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
        binding.ivDownload.setOnDebounceClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                handleDownload()
            } else {
                requestPermissionOrExecuteAction()
            }
        }
        observer()
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            when(it) {
                RoomMediaViewerEvent.DownloadFileSuccess -> showSuccess("Downloaded")
                is RoomMediaViewerEvent.Loading -> showOrHideLoading(it.isLoading)
                is RoomMediaViewerEvent.ShowError -> showError(it.message)
            }
        }
    }

    override fun onPermissionGranted(fromUser: Boolean) {
        handleDownload()
    }

    private fun handleDownload() {
        (binding.pager.adapter as RoomMediaAdapter).getItem(binding.pager.currentItem)?.let {
            viewModel.download(it)
        }
    }

    override val permissions: List<String> = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}