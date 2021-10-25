package com.nunchuk.android.settings

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.*
import com.nunchuk.android.settings.AccountEvent.SignOutEvent
import com.nunchuk.android.settings.databinding.FragmentAccountBinding
import java.io.ByteArrayOutputStream
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible

internal class AccountFragment : BaseFragment<FragmentAccountBinding>() {

    private val viewModel: AccountViewModel by viewModels { factory }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAccountBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupData()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: AccountState) {
        binding.appVersion.text = state.appVersion
        binding.avatar.loadImage(
            imageUrl = state.account.avatarUrl.orEmpty().fromMxcUriToMatrixDownloadUrl(),
            circleCrop = true,
            cornerRadius = null,
            placeHolder = ContextCompat.getDrawable(requireContext(), R.drawable.ic_account)
        )
        binding.avatar.isInvisible = state.account.avatarUrl?.isEmpty().orFalse()

        binding.avatarHolder.text = state.account.name.shorten()
        binding.avatarHolder.isInvisible = state.account.avatarUrl?.isNotEmpty().orFalse()

        binding.name.text = state.account.name
        binding.email.text = state.account.email
        binding.edit.setOnClickListener { editName() }
        binding.takePicture.setOnClickListener { changeAvatar() }
        binding.unit.setOnClickListener { changeUnitSetting() }
        binding.network.setOnClickListener { changeNetworkSetting() }
        binding.password.setOnClickListener { openChangePasswordScreen() }
        binding.devices.setOnClickListener { openLoggedInDevicesScreen() }
        binding.about.setOnClickListener { openAboutScreen() }
    }

    private fun openAboutScreen() {
        showComingSoonText()
    }

    private fun openLoggedInDevicesScreen() {
        showComingSoonText()
    }

    private fun openChangePasswordScreen() {
        navigator.openChangePasswordScreen(requireActivity())
    }

    private fun changeNetworkSetting() {
        showComingSoonText()
    }

    private fun changeUnitSetting() {
        showComingSoonText()
    }

    private fun changeAvatar() {
        val bottomSheet = EditPhotoUserBottomSheet.show(
            fragmentManager = childFragmentManager
        )
        bottomSheet.listener = {
            when(it) {
                EditPhotoOption.SelectAlbum -> {
                    if (isPermissionPhotoGranted()) {
                        openAlbum()
                    } else {
                        requestPermissions(REQUEST_PERMISSION_SELECT_PHOTO_CODE)
                    }
                }
                EditPhotoOption.TakePhoto -> {
                    if (isPermissionPhotoGranted()) {
                        takePhoto()
                    } else {
                        requestPermissions(REQUEST_PERMISSION_TAKE_PHOTO_CODE)
                    }
                }
                EditPhotoOption.RemovePhoto -> {
                    removePhoto()
                }
            }
        }
    }

    private fun removePhoto() {
        viewModel.updateUserProfile(
            name = viewModel.getCurrentAccountInfo().name,
            avatarUrl = null
        )
    }

    private fun editName() {
        val bottomSheet = EditNameUserBottomSheet.show(
            name = viewModel.getCurrentAccountInfo().name,
            fragmentManager = childFragmentManager
        )

        bottomSheet.listener = {
            if (it is EditNameUserOption.Save) {
                viewModel.updateUserProfile(
                    name = it.name,
                    avatarUrl = viewModel.getCurrentAccountInfo().avatarUrl
                )
            }
        }
    }

    private fun handleEvent(event: AccountEvent) {
        when (event) {
            SignOutEvent -> {
                val activity = requireActivity()
                navigator.openSignInScreen(activity)
                activity.finish()
            }
            is AccountEvent.GetUserProfileSuccessEvent -> {
            }
            is AccountEvent.UploadPhotoSuccessEvent -> {
                viewModel.updateUserProfile(
                    name = viewModel.getCurrentAccountInfo().name,
                    avatarUrl = event.matrixUri
                )
            }
        }
    }

    private fun openAlbum() {
        pickPhotoWithResult(REQUEST_SELECT_PHOTO_CODE)
    }

    private fun takePhoto() {
        takePhotoWithResult(REQUEST_TAKE_PHOTO_CODE)
    }

    // TODO: refactor with registerForActivityResult later
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }

        when(requestCode) {
            REQUEST_TAKE_PHOTO_CODE -> {
                val imageBitmap = data?.extras?.get("data") as Bitmap
                uploadPhotoData(imageBitmap)
            }

            REQUEST_SELECT_PHOTO_CODE -> {
                data?.data?.let {
                    val bitmap = BitmapFactory.decodeStream(requireActivity().contentResolver.openInputStream(it))
                    uploadPhotoData(bitmap)
                }
            }
            else -> {}
        }
    }

    private fun uploadPhotoData(bitmap: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        val byteArray = byteArrayOutputStream.toByteArray()
        viewModel.uploadPhotoToMaTrix(byteArray)
    }

    private fun requestPermissions(code: Int) {
        if (isPermissionPhotoGranted()) {
            return
        }

        when(code) {
            REQUEST_PERMISSION_SELECT_PHOTO_CODE -> requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE), code)
            REQUEST_PERMISSION_TAKE_PHOTO_CODE -> requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE, CAMERA), code)
            else -> {}
        }
    }

    // TODO: refactor with registerForActivityResult later
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_TAKE_PHOTO_CODE || requestCode == REQUEST_PERMISSION_SELECT_PHOTO_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePermissionGranted(requestCode)
            } else if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE) || shouldShowRequestPermissionRationale(CAMERA)) {
                showAlertPermissionNotGranted(requestCode)
            } else {
                showAlertPermissionDeniedPermanently()
            }
        }
    }

    private fun handlePermissionGranted(permissionCode: Int) {
        if (permissionCode == REQUEST_PERMISSION_TAKE_PHOTO_CODE) {
            takePhoto()
        } else {
            openAlbum()
        }
    }

    private fun showAlertPermissionNotGranted(permissionCode: Int) {
        requireActivity().showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied),
            message = getString(R.string.nc_text_des_permission_denied),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                requestPermissions(permissionCode)
            },
            negativeClick = {
            }
        )
    }

    private fun showAlertPermissionDeniedPermanently() {
        requireActivity().showAlertDialog(
            title = getString(R.string.nc_text_title_permission_denied_permanently),
            message = getString(R.string.nc_text_des_permission_denied_permanently),
            positiveButtonText = getString(android.R.string.ok),
            negativeButtonText = getString(android.R.string.cancel),
            positiveClick = {
                requireActivity().startActivityAppSetting()
            },
            negativeClick = {
            }
        )
    }

    private fun isPermissionPhotoGranted() =
        requireActivity().isPermissionGranted(WRITE_EXTERNAL_STORAGE) && requireActivity().isPermissionGranted(
            CAMERA)

    private fun setupViews() {
        binding.btnSignOut.setOnClickListener { viewModel.handleSignOutEvent() }
    }

    private fun setupData() {
        viewModel.getCurrentUser()
    }

    private fun showComingSoonText() {
        requireActivity().showToast("Coming soon")
    }

    companion object {
        private const val REQUEST_PERMISSION_TAKE_PHOTO_CODE = 1247
        private const val REQUEST_PERMISSION_SELECT_PHOTO_CODE = 1248
        private const val REQUEST_TAKE_PHOTO_CODE = 1249
        private const val REQUEST_SELECT_PHOTO_CODE = 1250
    }

}