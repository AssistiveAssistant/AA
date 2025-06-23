package com.example.aa.model

import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.example.aa.ImageClassificationHelper.Category

data class IdentifyUiState(
    val tempFileUrl: Uri? = null,
    val selectedPictures: List<ImageBitmap> = emptyList(),
    val image: Image? = null,
    val categories: List<Category> = emptyList(),
    val segParsed: SegParsed? = null,
    val description: String = "",
    val colour: Int = Color.TRANSPARENT,
    val mask: Bitmap? = null,
    val finished: Int = 0, //<1:FAIL  >1:SUCCESS
    val maskedOut: Bitmap? = null,
)

@Immutable
class SegParsed(
    val width: Int,
    val height: Int,
    val pixels: IntArray
)
