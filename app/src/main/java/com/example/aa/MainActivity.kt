package com.example.aa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aa.screen.AAApp
import com.example.aa.viewmodel.IdentifyViewModel
import com.example.aa.ui.theme.AATheme
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: IdentifyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = IdentifyViewModel(coroutineContext = Dispatchers.Default)

        enableEdgeToEdge()
        setContent {
            AATheme {
                AAApp(viewModel = viewModel)
            }
        }
    }
}
