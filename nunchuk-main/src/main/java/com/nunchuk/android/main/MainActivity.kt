package com.nunchuk.android.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.saveToFile
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsViewModel
import com.nunchuk.android.main.databinding.ActivityMainBinding
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.*
import javax.inject.Inject

class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private lateinit var navController: NavController

    private val viewModel: MainActivityViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.scheduleGetBTCConvertPrice()
        viewModel.restoreAndBackUp()
        setupNavigationView()
        //subscribeEvents()

    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: WalletsEvent) {
        if (event is WalletsEvent.Test) {
            event.responseBody.byteStream().saveToFile(externalCacheDir.toString() + File.separator + "FileBackup")

            val saveFile = File(externalCacheDir.toString() + File.separator + "FileBackup")

            saveFile.inputStream().readBytes().iterator().forEach { byte ->
                Timber.d("[App] byteStream: ${byte.toString()}")
            }
            viewModel.consumeSyncFile(event.jsonInfo,saveFile.readBytes())
        }
    }

    private val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
        binding.toolbarTitle.text = destination.label
    }

    private fun setupNavigationView() {
        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        navController.addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        navController.removeOnDestinationChangedListener(listener)
        super.onPause()
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }
    }

}