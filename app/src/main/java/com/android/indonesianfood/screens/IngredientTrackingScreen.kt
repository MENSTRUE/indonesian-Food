package com.android.indonesianfood.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientTrackingScreen(onBackClick: () -> Unit, paddingValues: PaddingValues, onBackPressExit: () -> Unit) { // Tambahkan callback exit
    BackHandler(enabled = true) {
        // Pop back jika ada riwayat, jika tidak ada, panggil onBackPressExit
        if (onBackClick != null) { // Pastikan onBackClick bisa dipanggil
            onBackClick()
        } else {
            onBackPressExit()
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var livePredictionResult by remember { mutableStateOf("Menunggu deteksi...") }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val imageProcessor = remember {
        ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .add(org.tensorflow.lite.support.common.ops.NormalizeOp(0.0f, 255.0f))
            .build()
    }

    val tfliteModel = remember {
        try {
            Model.createModel(context.applicationContext, "model_klasifikasi_bahan_masak.tflite")
        } catch (e: IOException) {
            Log.e("IngredientTracking", "Gagal memuat model: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("IngredientTracking", "Error in Model creation: ${e.message}", e)
            null
        }
    }
    val labels = remember {
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { it.toList() }
        } catch (e: IOException) {
            Log.e("IngredientTracking", "Gagal memuat label: ${e.message}")
            emptyList()
        }
    }

    val imageAnalyzer = remember(tfliteModel, labels, imageProcessor) {
        ImageAnalysis.Analyzer { imageProxy ->
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null && tfliteModel != null && labels.isNotEmpty()) {
                predictImageLive(bitmap, imageProcessor, tfliteModel, labels) { result ->
                    livePredictionResult = result
                }
            }
            imageProxy.close()
        }
    }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showPermissionDeniedDialog = true
        }
    }

    DisposableEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onDispose {
            cameraExecutor.shutdown()
            tfliteModel?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracking Bahan (Live)", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Deteksi Bahan Secara Langsung",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Arahkan kamera ke bahan masakan Anda untuk deteksi real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.medium),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                            setBackgroundColor(android.graphics.Color.BLACK)
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, imageAnalyzer)
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                Log.e("IngredientTracking", "Use case binding failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(context))
                        previewView
                    }
                )
            } else {
                Text(
                    text = "Izin kamera dibutuhkan untuk deteksi real-time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hasil Deteksi Live:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = livePredictionResult,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Izin Dibutuhkan") },
            text = { Text("Aplikasi membutuhkan izin Kamera untuk fitur ini.") },
            confirmButton = {
                Button(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Oke")
                }
            }
        )
    }
}

private fun predictImageLive(
    bitmap: Bitmap,
    imageProcessor: ImageProcessor,
    tfliteModel: Model,
    labels: List<String>,
    onResult: (String) -> Unit
) {
    try {
        val rotatedBitmap = rotateBitmap(bitmap, 90f)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(rotatedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val inputByteBuffer = processedImage.buffer

        val outputTensorShape = intArrayOf(1, labels.size)
        val outputDataType = DataType.FLOAT32

        val outputBuffer = TensorBuffer.createFixedSize(outputTensorShape, outputDataType)
        val outputsMap = mapOf(0 to outputBuffer.buffer)

        tfliteModel.run(arrayOf(inputByteBuffer), outputsMap)

        val labeledProbabilities = TensorLabel(labels, outputBuffer).getMapWithFloatValue()
        val sortedResults = labeledProbabilities.entries.sortedByDescending { it.value }

        val topResult = sortedResults.firstOrNull()

        val resultString = if (topResult != null && topResult.value > 0.5) {
            "${topResult.key}: ${(topResult.value * 100).toInt()}%"
        } else {
            "Tidak dapat mendeteksi bahan."
        }

        onResult(resultString)
    } catch (e: Exception) {
        Log.e("IngredientTracking", "Error prediksi live: ${e.message}", e)
        onResult("Error: ${e.message}")
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val vuBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}