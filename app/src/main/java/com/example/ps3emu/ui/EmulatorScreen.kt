package com.example.ps3emu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ps3emu.core.ConsoleType
import com.example.ps3emu.viewmodel.EmulatorViewModel

@Composable
fun EmulatorScreen(viewModel: EmulatorViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp).background(Color(0xFF202020)),
                contentAlignment = Alignment.Center
            ) {
                val info = viewModel.frameInfo
                Text("${viewModel.selectedConsole.name} Frame: ${info.width}x${info.height}", color = Color.White)
            }

            Text("Core status: ${viewModel.statusText}", color = Color.White)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.setConsole(ConsoleType.PS1) }) { Text("PS1") }
                Button(onClick = { viewModel.setConsole(ConsoleType.PS2) }) { Text("PS2") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.start() }) { Text("Start") }
                Button(onClick = viewModel::pause) { Text("Pause") }
                Button(onClick = viewModel::resume) { Text("Resume") }
                Button(onClick = viewModel::stop) { Text("Stop") }
            }
        }

        VirtualGamepad(onButton = viewModel::onButton, onStick = viewModel::onStick)
    }
}
