package com.nunchuk.android.messages.components.detail.viewer

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.nunchuk.android.core.base.BasePermissionFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.messages.components.detail.RoomDetailActivity
import com.nunchuk.android.messages.components.detail.RoomDetailViewModel
import com.nunchuk.android.messages.components.detail.viewer.gesture.SwipeDirection
import com.nunchuk.android.messages.components.detail.viewer.gesture.SwipeDirectionDetector
import com.nunchuk.android.messages.components.detail.viewer.gesture.SwipeToDismissHandler
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
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { RoomMediaAdapter(viewLifecycleOwner) }
    private var wasScaled: Boolean = false
    private var isOverlayWasClicked = false
    private var isImagePagerIdle = true
    private var swipeDirection: SwipeDirection? = null
    private var currentPosition = 0

    private val swipeDismissHandler: SwipeToDismissHandler by lazy(LazyThreadSafetyMode.NONE) {
        SwipeToDismissHandler(
            swipeView = binding.container,
            shouldAnimateDismiss = { true },
            onDismiss = { requireActivity().onBackPressedDispatcher.onBackPressed() },
            onSwipeViewMove = { _, _ -> }
        )
    }
    private val directionDetector: SwipeDirectionDetector by lazy(LazyThreadSafetyMode.NONE) {
        SwipeDirectionDetector(requireActivity()) { swipeDirection = it }
    }
    private val scaleDetector: ScaleGestureDetector by lazy(LazyThreadSafetyMode.NONE) {
        ScaleGestureDetector(requireActivity(), ScaleGestureDetector.SimpleOnScaleGestureListener())
    }
    private val gestureDetector: GestureDetectorCompat by lazy(LazyThreadSafetyMode.NONE) {
        GestureDetectorCompat(
            requireActivity(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (isImagePagerIdle) {
                        handleSingleTap(e, isOverlayWasClicked)
                    }
                    return false
                }
            })
    }

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
        adapter.items.apply {
            clear()
            addAll(items)
        }
        binding.pager.adapter = adapter
        val initialIndex = items.indexOfFirst { it.eventId == args.initEventId }.coerceAtLeast(0)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                currentPosition = position
                binding.tvImageCount.text = "${position.inc()} of ${items.size}"
                val eventId = items[position].eventId
                binding.tvFileInfo.text = timelineEvents.find { it.eventId == eventId }?.let {
                    "${it.senderInfo.disambiguatedDisplayName} ${
                        it.root.originServerTs?.let { time ->
                            Date(
                                time
                            ).formatMessageDate()
                        } ?: "-"
                    }"
                }.orEmpty()
            }

            override fun onPageScrollStateChanged(state: Int) {
                isImagePagerIdle = state == ViewPager2.SCROLL_STATE_IDLE
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
            when (it) {
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

    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // The zoomable view is configured to disallow interception when image is zoomed

        // Check if the overlay is visible, and wants to handle the click
        if (binding.overlay.isVisible && binding.overlay.dispatchTouchEvent(ev)) {
            return true
        }

        handleUpDownEvent(ev)

        if (swipeDirection == null && (scaleDetector.isInProgress || ev.pointerCount > 1 || wasScaled)) {
            wasScaled = true
            return binding.pager.dispatchTouchEvent(ev)
        }

        return (if (isScaled()) (requireActivity() as RoomDetailActivity).superDispatchTouchEvent(ev) else handleTouchIfNotScaled(
            ev
        ))
    }

    private fun handleUpDownEvent(event: MotionEvent) {
        // Log.v("ATTACHEMENTS", "handleUpDownEvent $event")
        if (event.action == MotionEvent.ACTION_UP) {
            handleEventActionUp(event)
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            handleEventActionDown(event)
        }

        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
    }

    private fun handleEventActionDown(event: MotionEvent) {
        swipeDirection = null
        wasScaled = false
        binding.pager.dispatchTouchEvent(event)

        swipeDismissHandler.onTouch(binding.container, event)
        isOverlayWasClicked = binding.overlay.dispatchTouchEvent(event)
    }

    private fun handleEventActionUp(event: MotionEvent) {
        swipeDismissHandler.onTouch(binding.container, event)
        binding.pager.dispatchTouchEvent(event)
        isOverlayWasClicked = binding.overlay.dispatchTouchEvent(event)
    }

    private fun handleTouchIfNotScaled(event: MotionEvent): Boolean {
        directionDetector.handleTouchEvent(event)

        return when (swipeDirection) {
            SwipeDirection.Up, SwipeDirection.Down -> {
                if (!wasScaled && isImagePagerIdle) {
                    swipeDismissHandler.onTouch(binding.container, event)
                } else true
            }
            SwipeDirection.Left, SwipeDirection.Right -> {
                binding.pager.dispatchTouchEvent(event)
            }
            else -> true
        }
    }

    private fun isScaled() = adapter.isScaled(currentPosition)

    private fun handleSingleTap(event: MotionEvent, isOverlayWasClicked: Boolean) {
        if (!isOverlayWasClicked) {
            binding.overlay.isVisible = binding.overlay.isVisible.not()
            (requireActivity() as RoomDetailActivity).superDispatchTouchEvent(event)
        }
    }
}