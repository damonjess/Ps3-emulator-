package com.example.ps3emu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.ps3emu.core.MockPs3Core
import com.example.ps3emu.ui.EmulatorScreen
import com.example.ps3emu.viewmodel.EmulatorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { EmulatorViewModel(MockPs3Core()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    EmulatorScreen(viewModel = viewModel)
                }
            }
        }
    }
}
