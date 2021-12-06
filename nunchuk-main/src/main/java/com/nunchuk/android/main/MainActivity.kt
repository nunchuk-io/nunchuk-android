package com.nunchuk.android.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.saveToFile
import com.nunchuk.android.main.databinding.ActivityMainBinding
import com.nunchuk.android.main.di.MainAppEvent
import org.matrix.android.sdk.api.session.initsync.InitSyncStep
import org.matrix.android.sdk.api.session.initsync.InitialSyncProgressService
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

        setupData()
        setupNavigationView()
        subscribeEvents()
        initObserver()
    }

    private fun setupData() {
        viewModel.checkSyncRoom()
        viewModel.scheduleGetBTCConvertPrice()
        viewModel.addBlockChainConnectionListener()
    }

    private fun initObserver() {
        viewModel.initialSyncProgressStatus?.observe(this) { status ->
            if (status is InitialSyncProgressService.Status.Progressing
                && status.initSyncStep == InitSyncStep.ImportingAccount
                && status.percentProgress == 100
            ) {
                viewModel.checkSyncRoom()
                viewModel.addBlockChainConnectionListener()
            }
        }
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: MainAppEvent) {
        when (event) {
            is MainAppEvent.DownloadFileSyncSucceed -> {
                handleDownloadedSyncFile(event)
            }
            is MainAppEvent.UploadFileSyncSucceed -> {
                viewModel.backupFile(event.fileJsonInfo, event.fileUri)
            }
        }
    }

    private fun handleDownloadedSyncFile(event: MainAppEvent.DownloadFileSyncSucceed) {
        event.responseBody.byteStream()
            .saveToFile(externalCacheDir.toString() + File.separator + "FileBackup")
        val saveFile = File(externalCacheDir.toString() + File.separator + "FileBackup")
        viewModel.consumeSyncFile(event.jsonInfo, saveFile.readBytes())
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