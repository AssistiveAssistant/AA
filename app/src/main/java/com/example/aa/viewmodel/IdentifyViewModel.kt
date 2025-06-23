package com.example.aa.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aa.ImageClassificationHelper
import com.example.aa.ImageClassificationHelper.Classification
import com.example.aa.ImageSegmentationHelper
import com.example.aa.ImageSegmentationHelper.Segmentation
import com.example.aa.R
import com.example.aa.model.IdentifyUiState
import com.example.aa.model.Intent2
import com.example.aa.model.SegParsed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext

enum class IdentifyState(@StringRes val title: Int) {
    None(title = R.string.none),
    ImageSelected(title = R.string.ImageLoaded),
    Successful(title = R.string.successful),
    Failed(title = R.string.failed),
}

class IdentifyViewModel(private val coroutineContext: CoroutineContext) : ViewModel() {
    companion object {
        private const val TAG = "IdentifyViewModel"
    }

    private val _uiState = MutableStateFlow(IdentifyUiState())
    val uiState: StateFlow<IdentifyUiState> = _uiState.asStateFlow()

    var state: IdentifyState = IdentifyState.None

    fun setFinished(value: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                finished = value,
            )
        }
    }

    fun reset() {
        state = IdentifyState.None
        stopSegment()
        stopClassify()
        segmentJob = null
        classificationJob = null
        _uiState.value = IdentifyUiState()

        Timer().schedule(200)
        {
            state = IdentifyState.None
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun startProcess() {
        var s: Boolean = false
        if (!_uiState.value.selectedPictures.isEmpty())
            s = startSegment(_uiState.value.selectedPictures.first().asAndroidBitmap())

        var c: Boolean = false
        if (!_uiState.value.selectedPictures.isEmpty())
            c = startClassify(_uiState.value.selectedPictures.first().asAndroidBitmap())

        if (s && c) {
            state = IdentifyState.Successful
            setFinished(1)
        } else {
            state = IdentifyState.Failed
            _uiState.update { currentState ->
                currentState.copy(
                    finished = -1,
                    description = "FAILURE"
                )
            }
        }
    }

    //region CLASSIFICATION
    var imageClassificationHelper: ImageClassificationHelper? = null

    fun initClassification(context: Context) {
        if (imageClassificationHelper != null)
            return

        imageClassificationHelper = ImageClassificationHelper(context)

        viewModelScope.launch {
            imageClassificationHelper!!.initClassifier()
        }
    }

    private var classificationJob: Job? = null

    fun stopClassify() {
        if (imageClassificationHelper == null)
            return

        classificationJob?.cancel()
    }

    fun startClassify(bitmap: Bitmap, rotationDegrees: Int = 0): Boolean {
        Log.i(TAG, "start CLASSIFICATION")
        try {
            var cls: Classification?;
            var job: Deferred<Classification?> = viewModelScope.async {
                val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                imageClassificationHelper!!.classify(argbBitmap, rotationDegrees)
            }

            runBlocking {
                cls = job.await()
            }

            if (cls != null && !cls.categories.isEmpty()) {
                _uiState.update { currentState ->
                    currentState.copy(
                        description = cls.categories.first().label
                    )
                }
                return true
            } else {
                Log.i(TAG, "Image classification: no labels found:")
                _uiState.update { currentState ->
                    currentState.copy(
                        description = "Object could not be identified!"
                    )
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "Image classification error occurred: ${e.message}")
        }

        return false
    }
    // endregion

    //region SEGMENTATION
    var imageSegmentationHelper: ImageSegmentationHelper? = null

    fun initSegment(context: Context) {
        if (imageSegmentationHelper != null)
            return

        imageSegmentationHelper = ImageSegmentationHelper(context)

        viewModelScope.launch {
            imageSegmentationHelper!!.initClassifier()
        }
    }

    private var segmentJob: Job? = null

    fun stopSegment() {
        if (imageSegmentationHelper == null)
            return

        viewModelScope.launch {
            segmentJob?.cancel()
        }
    }

    private fun parseSegmentResult(seg: Segmentation): SegParsed {
        val maskTensor = seg.masks[0]
        val maskArray = maskTensor.buffer.array()
        val pixels = IntArray(maskArray.size)

        for (i in maskArray.indices) {
            if (maskArray[i].toInt() != 0) //0 is background
                pixels[i] = android.graphics.Color.CYAN
        }

        val width = maskTensor.width
        val height = maskTensor.height

        return SegParsed(width, height, pixels)
    }

    fun estimateColour(maskedBitmap: Bitmap, pixelSpace: Int = 1): Int {
        var R = 0
        var G = 0
        var B = 0
        var n = 0
        var i = 0

        val height = maskedBitmap.getHeight()
        val width = maskedBitmap.getWidth()

        val pixels = IntArray(width * height)
        maskedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        while (i < pixels.size) {
            val color = pixels[i]
            if (color != 0) {
                R += Color.red(color)
                G += Color.green(color)
                B += Color.blue(color)
                n++
            }
            i += pixelSpace
        }

        return Color.rgb(R / n, G / n, B / n)
    }

    fun maskOut(bitmap: Bitmap, mask: Bitmap): Bitmap {
        val height = bitmap.getHeight()
        val width = bitmap.getWidth()

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val maskPixels = IntArray(width * height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        var i = 0
        while (i < pixels.size) {
            if (maskPixels[i] == 0)
                pixels[i] = 0
            i++
        }

        return Bitmap.createBitmap(
            pixels, width, height, Bitmap.Config.ARGB_8888
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun startSegment(bitmap: Bitmap, rotationDegrees: Int = 0): Boolean {
        Log.i(TAG, "start SEGMENTATION")
        try {
            var seg: Segmentation?;
            var job: Deferred<Segmentation?> = viewModelScope.async {
                val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                imageSegmentationHelper!!.segment(argbBitmap, rotationDegrees)
            }

            runBlocking {
                seg = job.await()
            }

            if (seg != null) {
                var sp = parseSegmentResult(seg)
                val b = Bitmap.createBitmap(
                    sp.pixels, sp.width, sp.height, Bitmap.Config.ARGB_8888
                )

                val scaleBitmap = b.scale(bitmap.width, bitmap.height)

                var scrIn = maskOut(bitmap, scaleBitmap)

                var c = estimateColour(scrIn)
                Log.i(TAG, "inferred colour: ${c.toHexString()}")

                var hsv = floatArrayOf(0f, 0f, 0f)
                Color.colorToHSV(c, hsv)

                var value = hsv[2] + 0.15f
                if (value > 1)
                    value = 1f
                hsv[2] = value

                c = Color.HSVToColor(hsv)
                Log.i(TAG, "improved colour: ${c.toHexString()}")

                _uiState.update { currentState ->
                    currentState.copy(
                        mask = scaleBitmap,//b,
                        segParsed = sp,
                        colour = c,
                        maskedOut = scrIn,
                    )
                }
                return true
            }
        } catch (e: Exception) {
            Log.i(TAG, "Image segment error occurred: ${e.message}")
        }

        return false
    }

    //region CAMERA & GALLERY
    fun doJob(intent: Intent2) = viewModelScope.launch(coroutineContext) {
        when (intent) {
            is Intent2.OnPermissionGrantedWith -> {
                try {

                    val tempFile = File.createTempFile(
                        "temp_image_file_",
                        ".jpg",
                        intent.compositionContext.cacheDir
                    )

                    val uri = FileProvider.getUriForFile(
                        intent.compositionContext,
                        "com.example.aa.provider",
                        tempFile
                    )
                    _uiState.value = _uiState.value.copy(tempFileUrl = uri)
                } catch (ex: Exception) {
                    Log.e("ViewModel", ex.toString())
                }
            }

            is Intent2.OnFinishPickingImagesWith -> {
                if (intent.imageUrls.isNotEmpty()) {
                    val newImages = mutableListOf<ImageBitmap>()
                    for (eachImageUrl in intent.imageUrls) {
                        val inputStream =
                            intent.compositionContext.contentResolver.openInputStream(eachImageUrl)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes != null) {
                            val bitmapOptions = BitmapFactory.Options()
                            bitmapOptions.inMutable = true
                            val bitmap: Bitmap =
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bitmapOptions)

                            val resizedBitmap = bitmap.scale(bitmap.width / 2, bitmap.height / 2)
                            newImages.add(resizedBitmap.asImageBitmap())
                        } else {
                            Log.e("ViewModel", "Unable to read image from: $eachImageUrl")
                        }
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedPictures = newImages,
                            tempFileUrl = null,
                        )
                    }

//                    val currentViewState = _uiState.value
//                    val newCopy = currentViewState.copy(
//                        selectedPictures = newImages,
//                        tempFileUrl = null,
//                    )
//                    _uiState.value = newCopy
                } else
                    Log.i("ViewModel", "User did not select an image!")
            }

            is Intent2.OnImageSavedWith -> {
                val tempImageUrl = _uiState.value.tempFileUrl
                if (tempImageUrl != null) {
                    val source = ImageDecoder.createSource(
                        intent.compositionContext.contentResolver,
                        tempImageUrl
                    )

                    val currentPictures = _uiState.value.selectedPictures.toMutableList()
                    currentPictures.add(ImageDecoder.decodeBitmap(source).asImageBitmap())

                    val bi = ImageDecoder.decodeBitmap(source).asShared()

                    _uiState.value = _uiState.value.copy(
                        tempFileUrl = null,
                        selectedPictures = currentPictures,
                    )
                }
            }

            is Intent2.OnImageSavingCanceled -> {
                _uiState.value = _uiState.value.copy(tempFileUrl = null)
            }

            is Intent2.OnPermissionDenied -> {
                Log.e("ViewModel", "Permission for camera not granted/was revoked!")
            }
        }
    }
    //endregion
}


