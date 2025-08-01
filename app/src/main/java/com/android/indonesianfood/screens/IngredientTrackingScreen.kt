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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientTrackingScreen(
    onBackClick: () -> Unit,
    paddingValues: PaddingValues,
    onBackPressExit: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var livePredictionResult by remember { mutableStateOf("Menunggu deteksi...") }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) showPermissionDeniedDialog = true
    }

    val imageProcessor = remember {
        ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    val tfliteModel = remember {
        try {
            Model.createModel(context, "model_klasifikasi_bahan_masak.tflite")
        } catch (e: Exception) {
            Log.e("IngredientTracking", "Model error: ${e.message}", e)
            null
        }
    }

    val labels = remember {
        try {
            context.assets.open("labels.txt").bufferedReader().useLines { it.toList() }
        } catch (e: IOException) {
            Log.e("IngredientTracking", "Label error: ${e.message}")
            emptyList()
        }
    }

    val imageAnalyzer = remember(tfliteModel, labels) {
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

    DisposableEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
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
                title = { Text("Tracking Bahan (Live)") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Arahkan kamera ke bahan makanan", fontWeight = FontWeight.Bold)

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(MaterialTheme.shapes.medium),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .apply { setAnalyzer(cameraExecutor, imageAnalyzer) }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("CameraX", "Binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            } else {
                Text("Izin kamera dibutuhkan", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Hasil Deteksi: ", fontWeight = FontWeight.Bold)
            Text(livePredictionResult)
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Izin Dibutuhkan") },
            text = { Text("Aplikasi memerlukan izin kamera.") },
            confirmButton = {
                Button(onClick = { showPermissionDeniedDialog = false }) {
                    Text("OK")
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
        val tensorImage = TensorImage(DataType.FLOAT32).apply {
            load(rotatedBitmap)
        }
        val processedImage = imageProcessor.process(tensorImage)
        val inputBuffer = processedImage.buffer

        val outputTensor = tfliteModel.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
        tfliteModel.run(arrayOf(inputBuffer), mapOf(0 to outputBuffer.buffer))

        val labeled = TensorLabel(labels, outputBuffer).getMapWithFloatValue()
        val result = labeled.entries.sortedByDescending { it.value }.firstOrNull()

        Log.d("TFLite", "Full Result: $labeled")

        val resultText = if (result != null && result.value > 0.5f && result.key.isNotBlank()) {
            "${result.key}: ${"%.2f".format(result.value * 100)}%"
        } else {
            "Tidak dapat mendeteksi bahan."
        }
        onResult(resultText)
    } catch (e: Exception) {
        Log.e("TFLite", "Prediction error: ${e.message}", e)
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
    return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
}
