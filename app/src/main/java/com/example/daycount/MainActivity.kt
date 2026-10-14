package com.example.daycount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.daycount.ui.DayCountApp
import com.example.daycount.ui.DayCountViewModel
import com.example.daycount.ui.theme.DayCountTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DayCountViewModel by viewModels {
        DayCountViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            DayCountTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                DayCountApp(viewModel = viewModel)
            }
        }
    }
}
