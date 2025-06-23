/*
 * Copyright 2024 The Google AI Edge Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.aa

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ColorSpaceType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ImageProperties
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * Code was modified and stratified
 */
class ImageSegmentationHelper(private val context: Context) {
    companion object {
        private const val TAG = "ImageSegmentation"
    }

    private var interpreter: Interpreter? = null

    suspend fun initClassifier(delegate: Delegate = Delegate.CPU) {
        interpreter = try {
            val litertBuffer = FileUtil.loadMappedFile(context, "deeplab_v3.tflite")
            Log.i(TAG, "Done creating LiteRT buffer from deeplab_v3")
            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = delegate == Delegate.NNAPI
            }
            Interpreter(litertBuffer, options)
        } catch (e: Exception) {
            Log.i(TAG, "Create LiteRT from deeplab_v3 is failed ${e.message}")
            null
        }
    }

    suspend fun segment(bitmap: Bitmap, rotationDegrees: Int): Segmentation? {
        try {
            if (interpreter == null) return null

            val rotation = -rotationDegrees / 90
            val (_, h, w, _) = interpreter?.getOutputTensor(0)?.shape()
                ?: return null
            val imageProcessor =
                ImageProcessor
                    .Builder()
                    .add(ResizeOp(h, w, ResizeOp.ResizeMethod.BILINEAR))
                    .add(Rot90Op(rotation))
                    .add(NormalizeOp(127.5f, 127.5f))
                    .build()

            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
            val segmentResult: Segmentation = segment(tensorImage)

            return segmentResult
        } catch (e: Exception) {
            Log.i(TAG, "Image segment error occurred: ${e.message}")
            return null
        }
    }

    private fun segment(tensorImage: TensorImage): Segmentation {
        val (_, h, w, c) = interpreter!!.getOutputTensor(0).shape()
        val outputBuffer = FloatBuffer.allocate(h * w * c)

        outputBuffer.rewind()
        interpreter?.run(tensorImage.tensorBuffer.buffer, outputBuffer)

        outputBuffer.rewind()
        val inferenceData =
            InferenceData(width = w, height = h, channels = c, buffer = outputBuffer)
        val mask = processImage(inferenceData)

        val imageProperties =
            ImageProperties
                .builder()
                .setWidth(inferenceData.width)
                .setHeight(inferenceData.height)
                .setColorSpaceType(ColorSpaceType.GRAYSCALE)
                .build()
        val maskImage = TensorImage()
        maskImage.load(mask, imageProperties)
        return Segmentation(listOf(maskImage))
    }

    private fun processImage(inferenceData: InferenceData): ByteBuffer {
        val mask = ByteBuffer.allocateDirect(inferenceData.width * inferenceData.height)
        for (i in 0 until inferenceData.height) {
            for (j in 0 until inferenceData.width) {
                val offset = inferenceData.channels * (i * inferenceData.width + j)

                var maxIndex = 0
                var maxValue = inferenceData.buffer.get(offset)

                for (index in 1 until inferenceData.channels) {
                    if (inferenceData.buffer.get(offset + index) > maxValue) {
                        maxValue = inferenceData.buffer.get(offset + index)
                        maxIndex = index
                    }
                }

                mask.put(i * inferenceData.width + j, maxIndex.toByte())
            }
        }

        return mask
    }

    data class Segmentation(
        val masks: List<TensorImage>
    )

    enum class Delegate {
        CPU, NNAPI
    }

    data class InferenceData(
        val width: Int,
        val height: Int,
        val channels: Int,
        val buffer: FloatBuffer,
    )
}
