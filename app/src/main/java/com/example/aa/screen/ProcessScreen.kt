package com.example.aa.screen

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aa.R
import com.example.aa.model.IdentifyUiState
import com.example.aa.ui.theme.AATheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessScreen(
    uiState: IdentifyUiState,
    onCancelButtonClicked: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {

    var hasNavigated: Boolean by (remember { mutableStateOf(false) })

    BackHandler(enabled = true) {
        onCancelButtonClicked()
        Log.i("ProcessScreen", "BACK BUTTON")
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            } else
                p = ColorPainter(color = MaterialTheme.colorScheme.background)

            Image(
                painter = p,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .alpha(0.7f)
                    .fillMaxSize()
                    .border(
                        border = BorderStroke(
                            width = dimensionResource(R.dimen.thickness_divider),
                            color = Color.Green
                        )
                    )
            )

            if (uiState.finished == 0) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(94.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.inverseOnSurface,
                    strokeWidth = 10.dp
                )
            } else {
                if (uiState.finished > 1)
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier.size(32.dp)
                    )
                if (uiState.finished < 1)
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        modifier.size(32.dp)
                    )

                Log.i("Process->Result", "NAVIGATION ATTEMPT!")
                if (!hasNavigated)
                    onFinished()
                hasNavigated = true
            }
        }

        Row(
            modifier = Modifier
        ) {
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .height(200.dp)
                    .padding(dimensionResource(R.dimen.padding_medium))
            ) {
//                Button(
//                    modifier = Modifier.fillMaxWidth().alpha(1f),
//                    onClick = {}
//                ) {
//                    Text(stringResource(R.string.send))
//                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCancelButtonClicked
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Preview
@Composable
fun ProcessScreenPreview() {
    AATheme {
        ProcessScreen(
            uiState = IdentifyUiState(),
            onCancelButtonClicked = {},
            onFinished = {},
        )
    }
}
