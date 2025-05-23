package com.nunchuk.android.core.scanner

import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GoogleCameraScanController(
    private val context: Context,
    private val previewView: PreviewView,
) : CameraScanController {

    private var camera: Camera? = null
    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()
    private val barcodeScanner = BarcodeScanning.getClient()
    private var listener: ((String) -> Unit)? = null

    private val cameraAspectRatio by lazy {
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        metrics.getAspectRatio()
    }

    override fun setOnBarcodeResultListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    fun DisplayMetrics.getAspectRatio(): Int {
        val ratio43Value = 4.0 / 3.0
        val ratio169Value = 16.0 / 9.0

        val previewRatio = max(this.widthPixels, this.heightPixels).toDouble() / min(
            this.widthPixels,
            this.heightPixels
        )

        if (abs(previewRatio - ratio43Value) <= abs(previewRatio - ratio169Value)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun startScanning(intent: Intent) {
        createCameraProvider()
    }

    private fun createCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            bindToLifecycleUseCaseGroup(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindToLifecycleUseCaseGroup(cameraProvider: ProcessCameraProvider) {
        val previewUseCase = Preview.Builder()
            .setTargetRotation(previewView.display.rotation)
            .setTargetAspectRatio(cameraAspectRatio)
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val analysisUseCase = ImageAnalysis.Builder()
            .setTargetRotation(previewView.display.rotation)
            .setTargetAspectRatio(cameraAspectRatio)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy)
                }
            }

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(previewUseCase)
            .addUseCase(analysisUseCase)
            .build()

        camera = cameraProvider.bindToLifecycle(
            context as LifecycleOwner,
            cameraSelector,
            useCaseGroup
        )
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            processBarcode(barcodeScanner, inputImage)
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    private fun processBarcode(
        barcodeScanner: BarcodeScanner,
        inputImage: InputImage
    ) = barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            Timber.tag("camera-scanner").d("barcodes: ${barcodes.size}")
            if (barcodes.isNotEmpty()) {
                listener?.invoke(barcodes.first().rawValue ?: "")
            }
        }
        .addOnFailureListener {
            Timber.tag("camera-scanner").e(it)
        }

    override fun stopScanning() {
//        camera?.close()
        camera = null
    }

    override fun resumeScanning() {

    }

    override fun onDestroy() {
        // Cleanup if needed
    }

    override fun torchState(isOn: Boolean) {
        camera?.cameraControl?.enableTorch(isOn)
    }
}