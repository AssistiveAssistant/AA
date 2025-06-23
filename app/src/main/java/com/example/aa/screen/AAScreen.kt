package com.example.aa.screen

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.*
import kotlin.concurrent.schedule
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aa.R
import com.example.aa.viewmodel.IdentifyViewModel

enum class AAScreen(@StringRes val title: Int) {
    Start(title = R.string.start),
    Process(title = R.string.processing),
    Result(title = R.string.result),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AAAppBar(
    currentScreen: AAScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}

//region Text-To-Speech
fun getInstanceTextToSpeech(context: Context) = TextToSpeech(context) { status ->
    when (status) {
        TextToSpeech.ERROR, // internal::TextToSpeech.LANG_MISSING_DATA,
        TextToSpeech.LANG_NOT_SUPPORTED,
        TextToSpeech.ERROR_SYNTHESIS,
        TextToSpeech.ERROR_SERVICE,
        TextToSpeech.ERROR_NETWORK,
        TextToSpeech.ERROR_NETWORK_TIMEOUT,
        TextToSpeech.ERROR_INVALID_REQUEST,
        TextToSpeech.ERROR_NOT_INSTALLED_YET,
        TextToSpeech.ERROR_OUTPUT -> {
            Toast.makeText(
                context,
                context.getString(R.string.internal_tts_error),
                Toast.LENGTH_SHORT
            ).show()
        }

        TextToSpeech.LANG_AVAILABLE,
        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE,
        TextToSpeech.LANG_COUNTRY_AVAILABLE -> {
            Toast.makeText(
                context,
                context.getString(R.string.tts_ready), Toast.LENGTH_SHORT
            ).show()
        }
    }
}
//endregion

@Composable
fun AAApp(
    viewModel: IdentifyViewModel, // = viewModel(), Dispatcher!-> über MainActivity!
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    viewModel.initSegment(context)
    viewModel.initClassification(context)

    val tts: TextToSpeech by (remember { mutableStateOf(getInstanceTextToSpeech(context)) })

    val backStackEntry by navController.currentBackStackEntryAsState()

    val currentScreen = AAScreen.valueOf(
        backStackEntry?.destination?.route ?: AAScreen.Start.name
    )

    val haptic = LocalHapticFeedback.current

    val toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 100)

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    val successEffect = VibrationEffect.createWaveform(
        longArrayOf(100, 100, 300, 300),
        intArrayOf(20, 100, 0, 240),
        -1
    )

    val failureEffect = VibrationEffect.createOneShot(
        500L,
        VibrationEffect.DEFAULT_AMPLITUDE
    )

    Scaffold(
        topBar = {
            AAAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    cancelAndNavigateToStart(viewModel, navController, toneGenerator)
//                    navController.navigateUp()
                }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        NavHost(
            navController = navController,
            startDestination = AAScreen.Start.name,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            composable(route = AAScreen.Start.name) {
                val context = LocalContext.current
                StartScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    startProcessing = {
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 50)
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        navController.navigate(AAScreen.Process.name)

                        Timer().schedule(5000)
                        {
                            viewModel.startProcess()
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = AAScreen.Process.name) {
                val context = LocalContext.current
                ProcessScreen(
                    uiState = uiState,
                    onCancelButtonClicked = {
                        cancelAndNavigateToStart(viewModel, navController, toneGenerator)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onFinished = { ->
                        navController.navigate(AAScreen.Result.name)

                        if (uiState.description.isEmpty() || uiState.description == "FAILURE") {
                            vibrator.vibrate(failureEffect)
                            toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 100)
                        } else {
                            vibrator.vibrate(successEffect)
                            toneGenerator.startTone(ToneGenerator.TONE_SUP_CONGESTION, 50)
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = AAScreen.Result.name) {
                val context = LocalContext.current
                ResultScreen(
                    uiState = uiState,
                    onSendButtonClicked = { subject: String, summary: String ->
                        shareResult(context, subject = subject, summary = summary)
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    },
                    onCancelButtonClicked = {
                        cancelAndNavigateToStart(viewModel, navController, toneGenerator)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onPlayAudioButtonClicked = {
                        var s = ":This is a: ${uiState.description}"
                        if (uiState.description.isEmpty() || uiState.description == "FAILURE")
                            s = ":Could not identify object"
                        tts.speak(
                            s,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

private fun cancelAndNavigateToStart(
    viewModel: IdentifyViewModel,
    navController: NavHostController,
    toneGenerator: ToneGenerator
) {
    toneGenerator.startTone(ToneGenerator.TONE_CDMA_MED_S_X4, 75)
    viewModel.reset()
    navController.popBackStack(AAScreen.Start.name, inclusive = false)
}

private fun shareResult(
    context: Context,
    subject: String,
    summary: String,
    filename: String = ""
) {
    val intent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, summary)
        type = "text/plain"

//        if (!filename.isEmpty()) {
//            val imageUri: Uri? = ("file://$filename").toUri()
//            setAction(Intent.ACTION_SEND)
//            putExtra(Intent.EXTRA_STREAM, imageUri)
//            //   Intent.setType = "image/jpg"
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.send)
        )
    )
}
