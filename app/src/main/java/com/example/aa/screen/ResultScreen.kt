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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aa.R
import com.example.aa.model.IdentifyUiState
import com.example.aa.ui.theme.AATheme
import com.example.aa.OverlayPainter

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalStdlibApi::class)
@Composable
fun ResultScreen(
    uiState: IdentifyUiState,
    onSendButtonClicked: (String, String) -> Unit,
    onCancelButtonClicked: () -> Unit,
    onPlayAudioButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {

    BackHandler(enabled = true) {
        onCancelButtonClicked()
        Log.i("ResultScreen", "BACK BUTTON")
    }

    val resources = LocalContext.current.resources

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
            var p: Painter = ColorPainter(MaterialTheme.colorScheme.onBackground)
            if (!uiState.selectedPictures.isEmpty()) {
                if (uiState.mask != null) {
                    val img = uiState.selectedPictures.first()
                    p = OverlayPainter(
                        img,
                        uiState.mask.asImageBitmap()
                    )
                } else {// uiState.mask == null)
                    val img = uiState.selectedPictures.first()
                    p = BitmapPainter(img)
                }
            }

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
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxSize()
                    .padding(
                        dimensionResource(R.dimen.padding_medium)
                    )
            )
            {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxHeight()
                ) {

                    Text(
                        text = uiState.description,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        modifier = Modifier
                    )

                    Text(
                        text = "    ${uiState.colour.toHexString().uppercase()}    ",
                        modifier = Modifier
                            .drawBehind {
                                drawRoundRect(
                                    Color(uiState.colour),
                                    cornerRadius = CornerRadius(10.dp.toPx())
                                )
                            }
                            .padding(4.dp)


                    )
                }

                MediumFloatingActionButton(
                    onClick = {
                        onPlayAudioButtonClicked()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                }
            }
        }
    }
}


@Preview
@Composable
fun ResultScreenPreview() {
    AATheme {
        ResultScreen(
            uiState = IdentifyUiState(description = "TEST RESULT"),
            onSendButtonClicked = { subject: String, summary: String -> },
            onCancelButtonClicked = {},
            onPlayAudioButtonClicked = { },
            modifier = Modifier.fillMaxHeight()
        )
    }
}
