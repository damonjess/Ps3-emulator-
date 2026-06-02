package com.example.ps3emu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ps3emu.core.AnalogStick
import com.example.ps3emu.core.GamepadButton

@Composable
fun VirtualGamepad(
    onButton: (GamepadButton, Boolean) -> Unit,
    onStick: (AnalogStick, Float, Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopButtons(onButton)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DPad(onButton)
            FaceButtons(onButton)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AnalogPad(AnalogStick.LEFT, onStick)
            CenterButtons(onButton)
            AnalogPad(AnalogStick.RIGHT, onStick)
        }
    }
}

@Composable
private fun TopButtons(onButton: (GamepadButton, Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GamepadButton("L1", GamepadButton.L1, onButton)
            GamepadButton("L2", GamepadButton.L2, onButton)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GamepadButton("R1", GamepadButton.R1, onButton)
            GamepadButton("R2", GamepadButton.R2, onButton)
        }
    }
}

@Composable
private fun CenterButtons(onButton: (GamepadButton, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GamepadButton("SEL", GamepadButton.SELECT, onButton)
            GamepadButton("PS", GamepadButton.PS, onButton)
            GamepadButton("STA", GamepadButton.START, onButton)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GamepadButton("L3", GamepadButton.L3, onButton)
            Spacer(modifier = Modifier.size(48.dp))
            GamepadButton("R3", GamepadButton.R3, onButton)
        }
    }
}

@Composable
private fun DPad(onButton: (GamepadButton, Boolean) -> Unit) { /* unchanged layout */
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GamepadButton("↑", GamepadButton.UP, onButton)
        Row {
            GamepadButton("←", GamepadButton.LEFT, onButton)
            Box(modifier = Modifier.size(48.dp))
            GamepadButton("→", GamepadButton.RIGHT, onButton)
        }
        GamepadButton("↓", GamepadButton.DOWN, onButton)
    }
}

@Composable
private fun FaceButtons(onButton: (GamepadButton, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GamepadButton("△", GamepadButton.TRIANGLE, onButton)
        Row {
            GamepadButton("□", GamepadButton.SQUARE, onButton)
            Box(modifier = Modifier.size(48.dp))
            GamepadButton("○", GamepadButton.CIRCLE, onButton)
        }
        GamepadButton("✕", GamepadButton.CROSS, onButton)
    }
}

@Composable
private fun AnalogPad(stick: AnalogStick, onStick: (AnalogStick, Float, Float) -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(Color(0xFF303030), CircleShape)
            .pointerInput(stick) {
                detectDragGestures(
                    onDragStart = { offset -> emitStick(stick, offset, onStick) },
                    onDrag = { change, _ -> emitStick(stick, change.position, onStick) },
                    onDragEnd = { onStick(stick, 0f, 0f) },
                    onDragCancel = { onStick(stick, 0f, 0f) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(34.dp).background(Color.Gray, CircleShape))
    }
}

private fun emitStick(stick: AnalogStick, offset: Offset, onStick: (AnalogStick, Float, Float) -> Unit) {
    val x = ((offset.x - 48f) / 48f).coerceIn(-1f, 1f)
    val y = ((offset.y - 48f) / 48f).coerceIn(-1f, 1f)
    onStick(stick, x, y)
}

@Composable
private fun GamepadButton(label: String, button: GamepadButton, onButton: (GamepadButton, Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(Color.DarkGray, CircleShape)
            .pointerInput(button) {
                detectTapGestures(onPress = {
                    onButton(button, true)
                    try { awaitRelease() } finally { onButton(button, false) }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White)
    }
}
