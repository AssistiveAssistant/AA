package com.example.aa

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

class OverlayPainter(
    private val srcImage: ImageBitmap,
    private val overlayImage: ImageBitmap,
    private val srcOffset: IntOffset = IntOffset.Zero,
    private val srcSize: IntSize = IntSize(srcImage.width, srcImage.height),
    private val overlaySize: IntSize = IntSize(overlayImage.width, overlayImage.height)
) : Painter() {

    override val intrinsicSize: Size get() = size.toSize()

    private fun checkSize(srcOffset: IntOffset, srcSize: IntSize): IntSize {
        require(
            srcOffset.x >= 0 && srcOffset.y >= 0 && srcSize.width >= 0 && srcSize.height >= 0
                    && srcSize.width <= srcImage.width && srcSize.height <= srcImage.height
        )
        return srcSize
    }

    private val size: IntSize = checkSize(srcOffset, srcSize)
    override fun DrawScope.onDraw() {
        drawImage(
            srcImage,
            srcOffset,
            srcSize,
            dstSize = IntSize(
                this@onDraw.size.width.roundToInt(),
                this@onDraw.size.height.roundToInt()
            )
        )

        drawImage(
            overlayImage,
            srcOffset,
            overlaySize,
            dstSize = IntSize(
                this@onDraw.size.width.roundToInt(),
                this@onDraw.size.height.roundToInt()
            ),
            blendMode = BlendMode.Overlay
        )
    }
}