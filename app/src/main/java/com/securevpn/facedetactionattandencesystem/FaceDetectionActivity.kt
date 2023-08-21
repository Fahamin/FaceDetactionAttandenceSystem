package com.securevpn.facedetactionattandencesystem

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class FaceDetectionActivity : AppCompatActivity() {
    lateinit var mPreviewView: PreviewView
    lateinit var captureImage: ImageView
    lateinit var cameraLayout: RelativeLayout
    lateinit var imageurl: String
    lateinit var uri: Uri
    lateinit var imageView: ImageView
    lateinit var btnScan: Button

    lateinit var myBitmap: Bitmap
    lateinit var cropBitmap: Bitmap

    //for camera
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        "android.permission.CAMERA",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    )


    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detection)

        FirebaseApp.initializeApp(this)


        imageView = findViewById(R.id.imageID)
        cameraLayout = findViewById(R.id.camera_LayoutID)
        captureImage = findViewById(R.id.captureImg)
        mPreviewView = findViewById(R.id.camera)
        btnScan = findViewById(R.id.btn_Scan)


        if (methodRequiresTwoPermission()) {
            cameraLayout.visibility = View.VISIBLE
            startCamera()

            //start camera if permission has been granted by user
        }

        btnScan.setOnClickListener(View.OnClickListener {
            detectFaces()
        })

    }

    // function to let's the user to choose image from camera or gallery
    private fun chooseImage(context: Context) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_choice)
        dialog.setCancelable(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        val btnCancel = dialog.findViewById<View>(R.id.btnCancle)
        val cameraIv: LinearLayout = dialog.findViewById(R.id.ivCamera)
        val galleryIv: LinearLayout = dialog.findViewById(R.id.ivGallary)



        cameraIv.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                startCamera()
            }
            if (methodRequiresTwoPermission()) {
                cameraLayout.visibility = View.VISIBLE
                startCamera()
                dialog.dismiss()

                //start camera if permission has been granted by user
            }
        }

        /*galleryIv.setOnClickListener {
            if (methodRequiresTwoPermission()) {
                val pickPhoto =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                imagePickerLuncher.launch(pickPhoto)

                dialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()*/

    }

    private fun methodRequiresTwoPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                REQUIRED_PERMISSIONS[0]
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            launcherPermission.launch(REQUIRED_PERMISSIONS)
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
            false
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    var launcherPermission = registerForActivityResult<Array<String>, Map<String, Boolean>>(
        ActivityResultContracts.RequestMultiplePermissions(),
        ActivityResultCallback<Map<String, Boolean>> { granted: Map<String, Boolean> ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    REQUIRED_PERMISSIONS[0]
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            }
        })

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            }
        }
    }

    // Luncher  imagepick from gallary
    private var imagePickerLuncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            val data = result.data
            uri = data?.data!!
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            if (selectedImage != null) {
                val cursor = contentResolver.query(selectedImage, filePathColumn, null, null, null)
                if (cursor != null) {
                    cursor.moveToFirst()
                    val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                    val picturePath = cursor.getString(columnIndex)
                    imageView.visibility = View.VISIBLE
                    btnScan.visibility = View.VISIBLE
                    imageView.setImageURI(uri)
                    imageurl = picturePath
                    myBitmap = BitmapFactory.decodeFile(picturePath)

                    cursor.close()
                }
            }
        }
    }


    //take image from camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {

        //  val preview = Preview.Builder().build()
        val preview = Preview.Builder().apply {
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setTargetRotation(Surface.ROTATION_90)
            setTargetRotation(mPreviewView.display.rotation)
        }.build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .build()

        val builder = ImageCapture.Builder()


        val imageCapture = builder
            .setTargetRotation(this.windowManager.defaultDisplay.rotation)
            .build()
        preview.setSurfaceProvider(mPreviewView.surfaceProvider)

        if (preview != null) {
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis,
                imageCapture
            )
        } else {
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageAnalysis,
                imageCapture
            )
        }

        captureImage.setOnClickListener {
            mPreviewView.visibility = View.VISIBLE
            val mDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            val file: File = File(batchDirectoryName, mDateFormat.format(Date()) + ".jpg")
            uri = Uri.fromFile(file)
            Log.e("uri", uri.toString())
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(
                outputFileOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val h = Handler(Looper.getMainLooper())
                        h.post {
                            //here show dialog
                            myBitmap = BitmapFactory.decodeFile(file.absolutePath)
                            cameraLayout.visibility = View.GONE

                            imageView.visibility = View.VISIBLE
                            btnScan.visibility = View.VISIBLE
                            imageView.setImageBitmap(myBitmap)

                            MediaScannerConnection.scanFile(
                                applicationContext, arrayOf(file.absolutePath),
                                null
                            ) { path, uri ->


                            }
                        }
                    }

                    override fun onError(error: ImageCaptureException) {
                        error.printStackTrace()
                        Log.e("imageTake", error.message.toString())

                    }
                })
        }
    }

    private fun detectFaces() {
        // [START set_detector_options]
        val image = InputImage.fromBitmap(myBitmap, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        // [END set_detector_options]

        // [START get_detector]
        val detector = FaceDetection.getClient(options)
        // Or, to use the default option:
        // val detector = FaceDetection.getClient();
        // [END get_detector]

        // [START run_detector]
        val result = detector.process(image)
            .addOnSuccessListener { faces ->
                // Task completed successfully
                // [START_EXCLUDE]
                // [START get_face_info]
                for (face in faces) {
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees
                    Log.e("detectImage", bounds.toString());
                    Log.e("detectImage", rotY.toString());
                    Log.e("detectImage", rotZ.toString());

                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                    // nose available):
                    val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)

                    leftEar?.let {
                        val leftEarPos = leftEar.position
                        Log.e("detectImage","leftEar"+ leftEar.toString());

                    }

                    // If classification was enabled:
                    if (face.smilingProbability != null) {
                        val smileProb = face.smilingProbability
                    }
                    if (face.rightEyeOpenProbability != null) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                        Log.e("detectImage","rightEyeOpenProb"+ leftEar.toString());

                    }

                    // If face tracking was enabled:
                    if (face.trackingId != null) {
                        val id = face.trackingId
                    }
                }
                // [END get_face_info]
                // [END_EXCLUDE]
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }
        // [END run_detector]
    }

    private fun faceOptionsExamples() {
        // [START mlkit_face_options_examples]
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        // Real-time contour detection
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        // [END mlkit_face_options_examples]
    }

    private fun processFaceList(faces: List<Face>) {
        // [START mlkit_face_list]
        for (face in faces) {
            val bounds = face.boundingBox
            val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
            val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            val leftEar = face.getLandmark(FaceLandmark.LEFT_EAR)
            leftEar?.let {
                val leftEarPos = leftEar.position
            }

            // If contour detection was enabled:
            val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
            val upperLipBottomContour = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points

            // If classification was enabled:
            if (face.smilingProbability != null) {
                val smileProb = face.smilingProbability
            }
            if (face.rightEyeOpenProbability != null) {
                val rightEyeOpenProb = face.rightEyeOpenProbability
            }

            // If face tracking was enabled:
            if (face.trackingId != null) {
                val id = face.trackingId
            }
        }
        // [END mlkit_face_list]
    }

    private val batchDirectoryName: String
        get() {
            var app_folder_path = ""
            app_folder_path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).toString() + "/images"
            val dir = File(app_folder_path)
            if (!dir.exists() && !dir.mkdirs()) {
            }
            return app_folder_path
        }

    companion object {
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 0)
            ORIENTATIONS.append(Surface.ROTATION_90, 90)
            ORIENTATIONS.append(Surface.ROTATION_180, 180)
            ORIENTATIONS.append(Surface.ROTATION_270, 270)
        }

        private const val REQUEST_ID_MULTIPLE_PERMISSIONS = 292
    }
}