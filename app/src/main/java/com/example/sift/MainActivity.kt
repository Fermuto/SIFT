package com.example.sift

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sift.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@OptIn(androidx.camera.core.ExperimentalGetImage::class)

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.processingPrompt.visibility = View.GONE

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    fun getTargetResolution(): Size {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> Size(1200, 1600)
            Configuration.ORIENTATION_LANDSCAPE -> Size(1600, 1200)
            else -> Size(1600, 1200)
        }
    }
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case

        val imageCapture = imageCapture ?: return

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    val msg = "Photo capture failed"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(capture: ImageProxy){
                    val msg = "Photo capture succeeded:"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    viewBinding.processingPrompt.visibility = View.VISIBLE
                    viewBinding.processingPrompt.setOnClickListener {
                        val i = Intent(this@MainActivity, Processing::class.java)
                        i.putExtra("Planes", capture.image!!.planes[0].buffer.toString())
                        var j = 0
                        while (j < 500) {
                            Log.e(
                                "BUFFERCONTENTS: ",
                                capture.image!!.planes[0].buffer[301].toString()
                            )
                            j++
                        }
                        Log.e("BUFFERTYPE: ", capture.image!!.planes[0].buffer[0].javaClass.kotlin.qualifiedName.toString())
                        Log.e("PIXELSTRIDE: ", capture.image!!.planes[0].pixelStride.toString())
                        i.putExtra("Height", capture.height)
                        i.putExtra("Width", capture.width)

                        var buffer: ByteBuffer = capture.planes.get(0).buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
                        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                        startActivity(i)
                    }
                }
            }
        )
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            val resolution = getTargetResolution()
            imageCapture = ImageCapture.Builder()
                .setTargetResolution((resolution))
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)


            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).toTypedArray()
    }
}