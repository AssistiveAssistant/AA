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
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.metadata.MetadataExtractor
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * Code was modified and stratified
 */
class ImageClassificationHelper(
    private val context: Context,
    private var options: Options = Options(),
) {
    class Options(
        var delegate: Delegate = DEFAULT_DELEGATE,
        var resultCount: Int = DEFAULT_RESULT_COUNT,
        var probabilityThreshold: Float = DEFAULT_THRESHOLD,
        var threadCount: Int = DEFAULT_THREAD_COUNT
    )

    companion object {
        private const val TAG = "ImageClassification"

        val DEFAULT_DELEGATE = Delegate.CPU
        const val DEFAULT_RESULT_COUNT = 3
        const val DEFAULT_THRESHOLD = 0.3f
        const val DEFAULT_THREAD_COUNT = 2
    }

    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String>

    suspend fun initClassifier() {
        interpreter = try {
            val litertBuffer = FileUtil.loadMappedFile(context, "efficientnet_lite0.tflite")
            Log.i(TAG, "Done creating TFLite buffer from efficientnet_lite0")
            val options = Interpreter.Options().apply {
                numThreads = options.threadCount
                useNNAPI = options.delegate == Delegate.NNAPI
            }
            labels = getModelMetadata(litertBuffer)
            Interpreter(litertBuffer, options)
        } catch (e: Exception) {
            Log.i(TAG, "Create TFLite from efficientnet_lite0 is failed ${e.message}")
            null
        }
    }

    suspend fun classify(bitmap: Bitmap, rotationDegrees: Int): Classification? {
        try {
            if (interpreter == null) return null
            val startTime = SystemClock.uptimeMillis()

            val rotation = -rotationDegrees / 90
            val (_, h, w, _) = interpreter?.getInputTensor(0)?.shape() ?: return null
            val imageProcessor =
                ImageProcessor.Builder().add(ResizeOp(h, w, ResizeOp.ResizeMethod.BILINEAR))
                    .add(Rot90Op(rotation)).add(NormalizeOp(127.5f, 127.5f)).build()

            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
            val output = classifyWithTFLite(tensorImage)

            val outputList = output.map {
                if (it < options.probabilityThreshold) 0f else it
            }

            val categories = labels.zip(outputList).map {
                Category(label = it.first, score = it.second)
            }.sortedByDescending { it.score }.take(options.resultCount)

            return Classification(categories)
        } catch (e: Exception) {
            Log.i(TAG, "Image classification error occurred: ${e.message}")
            return null
        }
    }

    private fun classifyWithTFLite(tensorImage: TensorImage): FloatArray {
        val outputShape = interpreter!!.getOutputTensor(0).shape()
        val outputBuffer = FloatBuffer.allocate(outputShape[1])

        outputBuffer.rewind()
        interpreter?.run(tensorImage.tensorBuffer.buffer, outputBuffer)
        outputBuffer.rewind()
        val output = FloatArray(outputBuffer.capacity())
        outputBuffer.get(output)
        return output
    }

    private fun getModelMetadata(litertBuffer: ByteBuffer): List<String> {
        val metadataExtractor = MetadataExtractor(litertBuffer)
        val labels = mutableListOf<String>()
        if (metadataExtractor.hasMetadata()) {
            val inputStream = metadataExtractor.getAssociatedFile("labels_without_background.txt")
            labels.addAll(readFileInputStream(inputStream))
            Log.i(
                TAG, "Successfully loaded model metadata ${metadataExtractor.associatedFileNames}"
            )
        }
        return labels
    }

    private fun readFileInputStream(inputStream: InputStream): List<String> {
        val reader = BufferedReader(InputStreamReader(inputStream))

        val list = mutableListOf<String>()
        var index = 0
        var line = ""
        while (reader.readLine().also { if (it != null) line = it } != null) {
            list.add(line)
            index++
        }

        reader.close()
        return list
    }

    enum class Delegate {
        CPU, NNAPI
    }

    data class Classification(
        val categories: List<Category>
    )

    data class Category(val label: String, val score: Float)
}