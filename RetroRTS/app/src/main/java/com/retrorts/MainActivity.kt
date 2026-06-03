package com.retrorts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.os.PerformanceHintManager
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.retrorts.ui.ConsoleType
import com.retrorts.ui.DosboxBridge
import com.retrorts.ui.GameProfile
import com.retrorts.ui.GameProfileStore
import com.retrorts.ui.GamePathValidator
import com.retrorts.ui.NativeEmulatorBridge
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.roundToInt

data class GameEntry(
    val name: String,
    val filePath: String,
    val gameId: String = name.lowercase().replace(" ", "_"),
    val consoleType: ConsoleType = ConsoleType.detect(filePath)
)
data class SettingsState(var displayScale: Float = 1f, var sensitivity: Float = 1f, var volume: Float = 0.8f)

enum class AppScreen { SPLASH, HOME, SETTINGS, ABOUT, GAME, NEEDS_PERMISSION }

class MainActivity : ComponentActivity() {
    private var audioFocusRequest: AudioFocusRequest? = null
    private var thermalRegistered = false

    private val storagePermissionLauncher =
        registerForActivityResult(StartActivityForResult()) { _: ActivityResult ->
            // Re-check logic is in RootApp LaunchedEffect
        }

    private val legacyPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Re-check logic is in RootApp LaunchedEffect
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestStoragePermissions()
        setContent {
            RetroRtsTheme {
                RootApp(
                    ::requestAudioFocus,
                    ::abandonAudioFocus,
                    ::startThermalMonitor,
                    ::hasStoragePermission,
                    ::requestStoragePermissions
                )
            }
        }
    }

    private fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    storagePermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    // Fallback to the general settings list if the package-specific intent fails
                    storagePermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
    private fun requestAudioFocus() { val am=getSystemService(AUDIO_SERVICE) as AudioManager; val req=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener { if (it<=0) DosboxBridge.stopDosbox() }.build(); audioFocusRequest=req; am.requestAudioFocus(req)}
    private fun abandonAudioFocus() { val am=getSystemService(AUDIO_SERVICE) as AudioManager; audioFocusRequest?.let { am.abandonAudioFocusRequest(it) } }

    override fun onStop() {
        super.onStop()
        DosboxBridge.stopDosbox()
        abandonAudioFocus()
    }
    private fun startThermalMonitor() {
        if (thermalRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        
        // Android Dynamic Performance Framework (ADPF) for MagicOS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val phm = getSystemService(PerformanceHintManager::class.java)
                // Hint that we are targeting 60fps (16.6ms per frame)
                val session = phm?.createHintSession(intArrayOf(android.os.Process.myTid()), 16666666L)
                // MagicOS will now prioritize our threads to prevent frame drops
            }
        }

        runCatching {
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
}


data class LaunchResult(val started: Boolean, val message: String)

private suspend fun launchGameWithNativeBackend(
    context: Context,
    game: GameEntry,
    settings: SettingsState
): LaunchResult = withContext(Dispatchers.IO) {
    if (!DosboxBridge.isAvailable) {
        return@withContext LaunchResult(false, "Native library not loaded. Check NDK build.")
    }
    val result = NativeEmulatorBridge.launchGame(
        console  = game.consoleType.name,   // "PS1", "DOSBOX", etc.
        romPath  = game.filePath,
        settings = settings
    )
    LaunchResult(result.started, result.message)
}

private fun writeDosboxConfig(context: Context, profile: GameProfile, game: GameEntry): String {
    val configDir = context.getExternalFilesDir("configs") ?: File(context.filesDir, "configs")
    if (!configDir.exists()) {
        configDir.mkdirs()
    }

    return runCatching {
        File(configDir, "${profile.gameId}.conf").apply {
            writeText(profile.toDosboxConfig(game.filePath))
        }.absolutePath
    }.getOrDefault("")
}

@Composable
private fun RootApp(
    onRequestAudioFocus: () -> Unit,
    onAbandonAudioFocus: () -> Unit,
    onThermalMonitor: () -> Unit,
    hasStoragePermission: () -> Boolean,
    onRequestStorage: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(AppScreen.SPLASH) }
    var settings by remember { mutableStateOf(SettingsState()) }
    var activeGame by remember { mutableStateOf<GameEntry?>(null) }
    var launchError by remember { mutableStateOf<String?>(null) }
    var isLaunching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        screen = if (hasStoragePermission()) AppScreen.HOME else AppScreen.NEEDS_PERMISSION
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (screen == AppScreen.NEEDS_PERMISSION && hasStoragePermission()) {
                    screen = AppScreen.HOME
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (isLaunching) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Launching Engine") },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { CircularProgressIndicator(); Text("Loading native components...", modifier = Modifier.padding(top = 8.dp)) } }
        )
    }

    launchError?.let { message ->
        AlertDialog(
            onDismissRequest = { launchError = null },
            confirmButton = { Button(onClick = { launchError = null }) { Text("OK") } },
            title = { Text("Launch failed") },
            text = { Text(message) },
        )
    }

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
        AppScreen.NEEDS_PERMISSION -> PermissionScreen(onRequestStorage)
        AppScreen.SETTINGS -> SettingsScreen(settings) { settings = it; screen = AppScreen.HOME }
        AppScreen.ABOUT -> AboutScreen { screen = AppScreen.HOME }
        AppScreen.GAME -> activeGame?.let { DosboxPlayScreen(it, settings) { DosboxBridge.stopDosbox(); onAbandonAudioFocus(); activeGame = null; screen = AppScreen.HOME } }
        AppScreen.HOME -> LauncherScreen(settings, onSettings = { screen = AppScreen.SETTINGS }, onAbout = { screen = AppScreen.ABOUT }, onLaunch = { game ->
            isLaunching = true
            scope.launch {
                val result = launchGameWithNativeBackend(context, game, settings)
                isLaunching = false
                if (result.started) {
                    onRequestAudioFocus()
                    onThermalMonitor()
                    activeGame = game
                    screen = AppScreen.GAME
                } else {
                    launchError = result.message
                }
            }
        })
    }
}

@Composable private fun SplashScreen() { Box(Modifier.fillMaxSize().background(Color(0xFF1B1A16)), contentAlignment = Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally){ Text("RetroRTS", color=Color(0xFFD8C77A), style=MaterialTheme.typography.headlineLarge, fontWeight=FontWeight.Bold); Text("Command Center Booting...", color=Color(0xFF93A17B)) } } }

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF1B1A16)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "Storage Access Required",
                color = Color(0xFFD8C77A),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "RetroRTS needs 'All Files Access' to read your game files " +
                        "from /sdcard/RetroRTS/Games. Tap the button below, then " +
                        "enable the permission and return to the app.",
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onRequest) { Text("Grant Storage Access") }
        }
    }
}

@Composable
private fun LauncherScreen(settings: SettingsState, onSettings: () -> Unit, onAbout: () -> Unit, onLaunch: (GameEntry) -> Unit) {
    val gamesRoot = remember {
        "${android.os.Environment.getExternalStorageDirectory().absolutePath}/RetroRTS/Games"
    }
    val games = remember { mutableStateListOf(
        GameEntry("Dune 2000",        "$gamesRoot/Dune2000"),
        GameEntry("Dune II (Amiga)",  "$gamesRoot/Dune2.adf"),
        GameEntry("Amiga A500 Demo",  "$gamesRoot/AmigaA500"),
        GameEntry("Nintendo DSi Demo","$gamesRoot/NintendoDSi/game.nds"),
        GameEntry("PS1 Game (add via Add .bin/.cue)", "$gamesRoot/PS1/game.cue")
    )}
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? -> uri?.let { if (GamePathValidator.isValid(it.toString())) games.add(GameEntry("Imported ${games.size+1}", it.toString())) } }
    val discPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.toString()
            if (GamePathValidator.isValid(path)) {
                // Derive a display name from the URI
                val name = path.substringAfterLast('%').substringAfterLast('/')
                    .substringBefore('?')
                    .ifBlank { "PS1 Game ${games.size + 1}" }
                games.add(GameEntry(name, path))
            }
        }
    }
    Scaffold(containerColor = Color(0xFF1B1A16)) { p -> Column(Modifier.fillMaxSize().padding(p).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSettings) { Text("Settings") }
            Button(onClick = onAbout) { Text("About") }
            Button(onClick = { picker.launch(null) }) { Text("Add Folder") }
            Button(onClick = {
                discPicker.launch(
                    arrayOf(
                        "application/octet-stream",   // .bin / .img
                        "application/x-cue",          // .cue
                        "application/x-cd-image",     // .iso
                        "*/*"                          // fallback
                    )
                )
            }) { Text("Add .bin/.cue") }
        }
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
    var saveSlot by remember { mutableStateOf(1) }
    var statusMsg by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Live perf stats — poll every second
    var fps by remember { mutableStateOf(0f) }
    var cpuPct by remember { mutableStateOf(0f) }
    
    // Auto-detect high refresh rate
    LaunchedEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val maxRefresh = context.display?.supportedModes?.maxByOrNull { it.refreshRate }?.refreshRate ?: 60f
            if (maxRefresh > 60f) {
                DosboxBridge.setFrameCap(maxRefresh.toInt())
            }
        }

        while (true) {
            delay(1000L)
            val stats = DosboxBridge.getPerfStats()
            fps    = stats.getOrElse(0) { 0f }
            cpuPct = stats.getOrElse(1) { 0f }
        }
    }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            confirmButton    = { Button({ onExit() }) { Text("Exit") } },
            dismissButton    = { Button({ showExitDialog = false }) { Text("Cancel") } },
            text             = { Text("Exit game session? Unsaved progress will be lost.") }
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ── Native render surface ─────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) {
                            NativeEmulatorBridge.setSurface(h.surface)
                        }
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) {
                            NativeEmulatorBridge.setSurface(h.surface)
                        }
                        override fun surfaceDestroyed(h: SurfaceHolder) {
                            NativeEmulatorBridge.setSurface(null)
                        }
                    })
                }
            }
        )

        // ── HUD overlay ───────────────────────────────────────────────
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                "${"%.0f".format(fps)} fps  •  ${"%.0f".format(cpuPct)}% cpu",
                color = Color(0xAAD8C77A),
                style = MaterialTheme.typography.labelSmall
            )
            if (statusMsg.isNotBlank()) {
                Text(statusMsg, color = Color(0xFFD8C77A),
                    style = MaterialTheme.typography.labelSmall)
            }
        }

        // ── Bottom toolbar ────────────────────────────────────────────
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(8.dp)
        ) {
            // Save/load slot row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Slot:", color = Color.White)
                (1..3).forEach { slot ->
                    val selected = slot == saveSlot
                    Button(
                        onClick = { saveSlot = slot },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF6D7F3B) else Color(0xFF3A3A3A)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("$slot") }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val dir = "${android.os.Environment.getExternalStorageDirectory()}/RetroRTS/saves"
                            java.io.File(dir).mkdirs()
                            val path = "$dir/${game.gameId}_slot$saveSlot.sav"
                            val ok = DosboxBridge.saveState(game.gameId, saveSlot, path)
                            withContext(Dispatchers.Main) {
                                statusMsg = if (ok) "Saved slot $saveSlot ✓" else "Save failed"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A5A3A))
                ) { Text("Save") }
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val path = "${android.os.Environment.getExternalStorageDirectory()}" +
                                       "/RetroRTS/saves/${game.gameId}_slot$saveSlot.sav"
                            val ok = DosboxBridge.loadState(game.gameId, saveSlot, path)
                            withContext(Dispatchers.Main) {
                                statusMsg = if (ok) "Loaded slot $saveSlot ✓" else "No save in slot $saveSlot"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A5A))
                ) { Text("Load") }
            }

            // Gamepad row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RtsOverlay(settings, Modifier.weight(1f), onExit)
            }
        }
    }
}

// Keep RtsOverlay as before but remove the outer Box that filled the whole
// screen — it now lives inside the bottom toolbar row:
@Composable
private fun RtsOverlay(settings: SettingsState, modifier: Modifier, onExit: () -> Unit) {
    Row(
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = {}) { Text("L") }
        Button(onClick = {}) { Text("R") }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2020))
        ) { Text("Exit") }
    }
}

@Composable private fun RetroRtsTheme(content: @Composable () -> Unit) { MaterialTheme(content = content) }
