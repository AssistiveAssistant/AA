package com.example.aa.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aa.R
import com.example.aa.model.IdentifyUiState
import com.example.aa.model.Intent2
import com.example.aa.ui.theme.AATheme
import com.example.aa.viewmodel.IdentifyState
import com.example.aa.viewmodel.IdentifyViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartScreen(
    uiState: IdentifyUiState,
    viewModel: IdentifyViewModel,
    startProcessing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        )
    } else {
        rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        )
    }

    when {
        ps.allPermissionsGranted -> StartScreenPage(
            uiState,
            viewModel,
            startProcessing,
            modifier
        )

        else -> PermissionHelper(ps)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview
@Composable
fun PermissionHelperPreview() {
    AATheme {
        val ps = rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        )
        PermissionHelper(ps)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHelper(ps: MultiplePermissionsState) {

    LaunchedEffect(Unit) {
        ps.launchMultiplePermissionRequest()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current

        Text(
            text = stringResource(R.string.camera_and_media_permission_required),
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(dimensionResource(R.dimen.padding_small))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
        Text(stringResource(R.string.required_for_the_app_to_take_pictures))

        Spacer(modifier = Modifier.height(300.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        )
        {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                modifier = Modifier
            ) {
                Button(
                    onClick = {
                        ps.permissions.forEach { s -> s.launchPermissionRequest() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.request_permissions))
                }

                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }
        }
    }
}


@Composable
fun StartScreenPage(
    uiState: IdentifyUiState,
    viewModel: IdentifyViewModel,
    startProcessing: () -> Unit,
    modifier: Modifier = Modifier
) {

    val currentContext = LocalContext.current

    val launcherImageFromGallery =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(20)
        ) { urls ->
            viewModel.doJob(Intent2.OnFinishPickingImagesWith(currentContext, urls))
        }

    val launcherPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { permissionGranted ->
            if (permissionGranted) {
                viewModel.doJob(Intent2.OnPermissionGrantedWith(currentContext))
            } else {
                viewModel.doJob(Intent2.OnPermissionDenied)

                Toast.makeText(
                    currentContext,
                    "To take photographs, you must allow this app access to the camera",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    val launcherCamera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isImageSaved ->
            if (isImageSaved) {
                viewModel.doJob(Intent2.OnImageSavedWith(currentContext))
            } else {
                viewModel.doJob(Intent2.OnImageSavingCanceled)
            }
        }

    LaunchedEffect(key1 = uiState.tempFileUrl) {
        uiState.tempFileUrl?.let {
            launcherCamera.launch(it)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, true)
                .padding(dimensionResource(R.dimen.padding_medium)),
            contentAlignment = Alignment.Center
        ) {
            var p: Painter
            if (!uiState.selectedPictures.isEmpty()) {
                p = BitmapPainter(uiState.selectedPictures.first())
                if (viewModel.state == IdentifyState.None) {
                    viewModel.state = IdentifyState.ImageSelected
                    startProcessing()
                }
            } else
                p = ColorPainter(color = MaterialTheme.colorScheme.background)

            Image(
                painter = p,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .alpha(1f)
                    .fillMaxSize()
                    .border(
                        border = BorderStroke(
                            width = dimensionResource(R.dimen.thickness_divider),
                            color = Color.Green
                        )
                    )
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.padding_medium))
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    onClick =
                        {
                            launcherPermission.launch(Manifest.permission.CAMERA)
                        }
                ) {
                    Text(stringResource(R.string.TakePhoto))
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    onClick =
                        {
                            launcherImageFromGallery.launch(
                                PickVisualMediaRequest
                                    (ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Text(stringResource(R.string.LoadImage))
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun StartScreenPreview() {
    AATheme {
        var vm = IdentifyViewModel(coroutineContext = Dispatchers.Default)
        val uis by vm.uiState.collectAsState()
        StartScreen(
            uiState = uis,
            viewModel = vm,
            startProcessing = {},
            modifier = Modifier
        )
    }
}






















