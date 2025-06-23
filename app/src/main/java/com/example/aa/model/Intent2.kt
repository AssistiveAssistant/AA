package com.example.aa.model

import android.content.Context
import android.net.Uri

sealed class Intent2 {
    data object OnPermissionDenied : Intent2()

    data class OnPermissionGrantedWith(val compositionContext: Context) : Intent2()

    data class OnImageSavedWith(val compositionContext: Context) : Intent2()

    data object OnImageSavingCanceled : Intent2()

    data class OnFinishPickingImagesWith(
        val compositionContext: Context,
        val imageUrls: List<Uri>
    ) : Intent2()

}