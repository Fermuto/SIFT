package com.example.sift

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@OptIn(androidx.camera.core.ExperimentalGetImage::class)
val scaleHeight = 480
val scaleWidth = 640

class MainActivity : AppCompatActivity() {

    private val MODE_PRIVATE: Int = 0
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.processingPrompt.visibility = View.VISIBLE

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.imageImportButton.setOnClickListener { directImport() }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun createImageFromBitmap(bitmap: Bitmap): String? {
        var fileName: String? = "myImage" //no .png or .jpg needed
        try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val fo: FileOutputStream = openFileOutput(fileName, this@MainActivity.MODE_PRIVATE)
            fo.write(bytes.toByteArray())
            // remember close file output
            fo.close()
        } catch (e: Exception) {
            e.printStackTrace()
            fileName = null
        }
        Log.e("FILENAME ", fileName.toString())
        return fileName
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
                    val msg = "Photo capture failed!"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(capture: ImageProxy){
                    val msg = "Photo capture succeeded!"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    viewBinding.processingPrompt.visibility = View.VISIBLE
                    viewBinding.processingPrompt.setOnClickListener {
                        val i = Intent(this@MainActivity, Processing::class.java)

//                        var j = 0
//                        while (j < 500) {
//                            Log.e(
//                                "BUFFERCONTENTS: ",
//                                capture.image!!.planes[0].buffer[j].toString()
//                            )
//                            j++
//                        }
//                        Log.e("BUFFERNUM: ", capture.image!!.planes[0].buffer[0].javaClass.kotlin.qualifiedName.toString())
//                        Log.e("PLANES: ", capture.image!!.planes.size.toString())
//                        Log.e("FORMAT: ", capture.image!!.format.toString())


                        var buffer: ByteBuffer = capture.planes.get(0).buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)
//                        val bitmapMid = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                        val bitmapMid = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                        val bitmapImage = Bitmap.createScaledBitmap(bitmapMid, scaleWidth, scaleHeight, true)

//                        val stream = ByteArrayOutputStream()
//                        bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
//                        val byteArray = stream.toByteArray()

                        val bitmapname = createImageFromBitmap(bitmapImage)

                        i.putExtra("Mode", 0)
                        i.putExtra("Capture", bitmapname);
                        i.putExtra("Height", scaleHeight)
                        i.putExtra("Width", scaleWidth)

//                        var j = 0
//                        var k = 0
//                        while (j < 10) {
//                            while (k < 10){
//                                Log.e(
//                                    "CONTENTS: ",
//                                    bitmapImage.getPixel(j, k).toString()
//                                )
//                                k++
//                            }
//                            j++
//                        }
//                        var colour = bitmapImage.getPixel(392, 404)
//
//                        Log.e("DATATYPE: ", colour::class.java.typeName.toString())
//
//                        Log.e("RED: ", Color.red(colour).toString())
//                        Log.e("GRN: ", Color.green(colour).toString())
//                        Log.e("BLU: ", Color.blue(colour).toString())
//                        Log.e("ALPHA: ", Color.alpha(colour).toString())
//
//                        Log.e("FORMAT: ", bitmapImage.getPixel(0,0).toString())
//                        Log.e("New Height: ", bitmapImage.height.toString())
//                        Log.e("New Width: ", bitmapImage.width.toString())
//                        Log.e("New Width: ", bitmapImage.width.toString())

                        capture.close()

                        startActivity(i)
                    }
                }
            }
        )
    }
    private fun directImport(){
        val i = Intent(this@MainActivity, Processing::class.java)

        i.putExtra("Mode", 1)
        i.putExtra("Height", scaleHeight)
        i.putExtra("Width", scaleWidth)

        Toast.makeText(baseContext, "Importing...", Toast.LENGTH_SHORT).show()

        startActivity(i)
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
//            val resolution = getTargetResolution()
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1600, 1200))
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