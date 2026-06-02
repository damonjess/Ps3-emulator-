package com.retrorts

import android.app.AlertDialog
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.retrorts.ui.DosboxBridge
import com.retrorts.ui.GameProfileStore
import com.retrorts.ui.GamePathValidator
import com.retrorts.ui.NativeEmulatorBridge
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

data class GameEntry(val name: String, val filePath: String) { val gameId get() = name.lowercase().replace(" ", "_") }
data class SettingsState(var displayScale: Float = 1f, var sensitivity: Float = 1f, var volume: Float = 0.8f)

enum class AppScreen { SPLASH, HOME, SETTINGS, ABOUT, GAME }

class MainActivity : ComponentActivity() {
    private var audioFocusRequest: AudioFocusRequest? = null
    private var thermalRegistered = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RetroRtsTheme { RootApp(::requestAudioFocus, ::abandonAudioFocus, ::startThermalMonitor) } }
    }
    private fun requestAudioFocus() { val am=getSystemService(AUDIO_SERVICE) as AudioManager; val req=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener { if (it<=0) DosboxBridge.stopDosbox() }.build(); audioFocusRequest=req; am.requestAudioFocus(req)}
    private fun abandonAudioFocus() { val am=getSystemService(AUDIO_SERVICE) as AudioManager; audioFocusRequest?.let { am.abandonAudioFocusRequest(it) } }

    override fun onStop() {
        super.onStop()
        DosboxBridge.stopDosbox()
        abandonAudioFocus()
    }
    private fun startThermalMonitor() {
        if (thermalRegistered) return
        getSystemService(PowerManager::class.java)?.addThermalStatusListener(mainExecutor) {
            DosboxBridge.notifyThermalLevel(
                if (it >= PowerManager.THERMAL_STATUS_SEVERE) 3
                else if (it >= PowerManager.THERMAL_STATUS_MODERATE) 2
                else if (it >= PowerManager.THERMAL_STATUS_LIGHT) 1
                else 0
            )
        }
        thermalRegistered = true
    }
}


private fun launchGameWithNativeBackend(game: GameEntry, settings: SettingsState): Boolean {
    val profile = GameProfileStore.loadByGameName(game.name)
    val configDir = File("/sdcard/RetroRTS/configs").apply { mkdirs() }
    val configPath = File(configDir, "${profile.gameId}.conf").apply {
        writeText(profile.toDosboxConfig())
    }.absolutePath

    val started = when (profile.platform) {
        "amiga" -> NativeEmulatorBridge.launchGame("AMIGA", game.filePath).isNotBlank()
        "dsi" -> NativeEmulatorBridge.launchGame("NINTENDO_DSI", game.filePath).let { it.isNotBlank() && !it.startsWith("ERROR:") }
        else -> DosboxBridge.startDosbox(game.filePath, configPath)
    }

    if (started) {
        if (profile.platform == "dosbox") {
            DosboxBridge.setCpuCycles(profile.cycles)
        }
        DosboxBridge.setFrameCap(profile.frameCap)
        DosboxBridge.setVolume(settings.volume)
    }
    return started
}

@Composable
private fun RootApp(onRequestAudioFocus: () -> Unit, onAbandonAudioFocus: () -> Unit, onThermalMonitor: () -> Unit) {
    var screen by remember { mutableStateOf(AppScreen.SPLASH) }
    var settings by remember { mutableStateOf(SettingsState()) }
    var activeGame by remember { mutableStateOf<GameEntry?>(null) }

    LaunchedEffect(Unit) { delay(1500); screen = AppScreen.HOME }

    BackHandler(enabled = screen != AppScreen.SPLASH) {
        when (screen) {
            AppScreen.HOME -> {}
            AppScreen.SETTINGS, AppScreen.ABOUT -> screen = AppScreen.HOME
            AppScreen.GAME -> { DosboxBridge.stopDosbox(); onAbandonAudioFocus(); activeGame = null; screen = AppScreen.HOME }
            else -> {}
        }
    }

    when (screen) {
        AppScreen.SPLASH -> SplashScreen()
        AppScreen.SETTINGS -> SettingsScreen(settings) { settings = it; screen = AppScreen.HOME }
        AppScreen.ABOUT -> AboutScreen { screen = AppScreen.HOME }
        AppScreen.GAME -> activeGame?.let { DosboxPlayScreen(it, settings) { DosboxBridge.stopDosbox(); onAbandonAudioFocus(); activeGame = null; screen = AppScreen.HOME } }
        AppScreen.HOME -> LauncherScreen(settings, onSettings = { screen = AppScreen.SETTINGS }, onAbout = { screen = AppScreen.ABOUT }, onLaunch = { game ->
            if (launchGameWithNativeBackend(game, settings)) {
                onRequestAudioFocus()
                onThermalMonitor()
                activeGame = game
                screen = AppScreen.GAME
            }
        })
    }
}

@Composable private fun SplashScreen() { Box(Modifier.fillMaxSize().background(Color(0xFF1B1A16)), contentAlignment = Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally){ Text("RetroRTS", color=Color(0xFFD8C77A), style=MaterialTheme.typography.headlineLarge, fontWeight=FontWeight.Bold); Text("Command Center Booting...", color=Color(0xFF93A17B)) } } }

@Composable
private fun LauncherScreen(settings: SettingsState, onSettings: () -> Unit, onAbout: () -> Unit, onLaunch: (GameEntry) -> Unit) {
    val games = remember { mutableStateListOf(GameEntry("Dune 2000", "/sdcard/RetroRTS/Games/Dune2000"), GameEntry("C&C: Red Alert", "/sdcard/RetroRTS/Games/RedAlert"), GameEntry("Amiga A500 Demo", "/sdcard/RetroRTS/Games/AmigaA500"), GameEntry("Nintendo DSi Demo", "/sdcard/RetroRTS/Games/NintendoDSi/game.nds")) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? -> uri?.let { if (GamePathValidator.isValid(it.toString())) games.add(GameEntry("Imported ${games.size+1}", it.toString())) } }
    Scaffold(containerColor = Color(0xFF1B1A16)) { p -> Column(Modifier.fillMaxSize().padding(p).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick=onSettings){Text("Settings")}; Button(onClick=onAbout){Text("About")}; Button(onClick={ picker.launch(null) }){Text("Add Game")}}
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(games) { g -> Card(colors= CardDefaults.cardColors(containerColor = Color(0xFF2B2920)), modifier=Modifier.fillMaxWidth()){ Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically){ Column(Modifier.weight(1f)){Text(g.name,color=Color(0xFFE6DCA3)); Text(g.filePath,color=Color(0xFFB9B38A))}; Button(onClick={ onLaunch(g) }){Text("Launch")}} } } }
    } }
}

@Composable
private fun SettingsScreen(state: SettingsState, onDone: (SettingsState) -> Unit) {
    var s by remember { mutableStateOf(state) }
    Scaffold(containerColor = Color(0xFF1B1A16)) { p -> Column(Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", color = Color(0xFFD8C77A), style = MaterialTheme.typography.headlineSmall)
        Text("Display Scaling ${"%.2f".format(s.displayScale)}", color = Color.White); Slider(value=s.displayScale,onValueChange={s=s.copy(displayScale=it)},valueRange=0.5f..1.5f)
        Text("Control Sensitivity ${"%.2f".format(s.sensitivity)}", color = Color.White); Slider(value=s.sensitivity,onValueChange={s=s.copy(sensitivity=it)},valueRange=0.5f..2f)
        Text("Audio Volume ${"%.2f".format(s.volume)}", color = Color.White); Slider(value=s.volume,onValueChange={s=s.copy(volume=it)},valueRange=0f..1f)
        Button(onClick = { onDone(s) }) { Text("Save & Back") }
    } }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Scaffold(containerColor = Color(0xFF1B1A16)) { p -> Column(Modifier.fillMaxSize().padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("About RetroRTS", color=Color(0xFFD8C77A), style=MaterialTheme.typography.headlineSmall)
        Text("Powered by DOSBox, Amiga, and Nintendo DSi emulator backends (open source).", color=Color.White)
        Text("Uses AndroidX Jetpack Compose, Kotlin, and Android NDK/CMake.", color=Color.White)
        Text("Respect licenses for DOSBox and third-party libraries.", color=Color(0xFFB9B38A))
        Button(onClick=onBack){ Text("Back") }
    } }
}

@Composable
private fun DosboxPlayScreen(game: GameEntry, settings: SettingsState, onExit: () -> Unit) {
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }
    if (showExitDialog) AlertDialog(onDismissRequest={showExitDialog=false}, confirmButton={Button({onExit()}){Text("Exit")}}, dismissButton={Button({showExitDialog=false}){Text("Cancel")}}, text={Text("Exit game session?")})

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF111111)), contentAlignment = Alignment.Center) { Text("Emulator Surface: ${game.name}", color = Color(0xFF93A17B)) }
        RtsOverlay(settings, Modifier.fillMaxSize(), onExit)
    }
}

@Composable
private fun RtsOverlay(settings: SettingsState, modifier: Modifier, onExit: () -> Unit) {
    var cursor by remember { mutableStateOf(Offset(300f, 300f)) }
    var keyboard by remember { mutableStateOf(false) }
    Box(modifier.pointerInput(settings.sensitivity) { detectDragGestures { change, drag -> change.consume(); cursor = Offset(cursor.x + drag.x * settings.sensitivity, cursor.y + drag.y * settings.sensitivity) } }) {
        Box(Modifier.offset { IntOffset(cursor.x.roundToInt(), cursor.y.roundToInt()) }.size((20 * settings.displayScale).dp).background(Color(0xFFCCDD88), CircleShape).border(2.dp, Color.Black, CircleShape))
        Row(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {}) { Text("Left") }; Button(onClick = {}) { Text("Right") }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Keyboard", color = Color(0xFFD8C77A)); Switch(checked = keyboard, onCheckedChange = { keyboard = it }) }
            Button(onClick = onExit) { Text("Exit") }
        }
    }
}

@Composable private fun RetroRtsTheme(content: @Composable () -> Unit) { MaterialTheme(content = content) }
