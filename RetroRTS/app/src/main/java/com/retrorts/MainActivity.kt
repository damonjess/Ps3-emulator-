package com.retrorts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.content.Intent
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.InstallMobile
import android.net.Uri
import android.net.Uri as AndroidUri
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.retrorts.ui.ConsoleType
import com.retrorts.ui.DosboxBridge
import com.retrorts.ui.GameProfile
import com.retrorts.ui.GameProfileStore
import com.retrorts.ui.GamePathValidator
import com.retrorts.ui.NativeEmulatorBridge
import kotlinx.coroutines.*
import java.io.File
import java.util.zip.ZipInputStream
import android.widget.Toast
import kotlin.math.roundToInt

data class GameEntry(
    val name: String,
    val filePath: String,
    val gameId: String = name.lowercase().replace(" ", "_"),
    val consoleType: ConsoleType = ConsoleType.detect(filePath)
)
data class SettingsState(var displayScale: Float = 1f, var sensitivity: Float = 1f, var volume: Float = 0.8f)

enum class AppScreen { SPLASH, HOME, GAME, NEEDS_PERMISSION }

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
        extractSystemAssets() // Move bundled BIOS to SD card
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

    private fun extractSystemAssets() {
        if (!hasStoragePermission()) return
        
        lifecycleScope.launch(Dispatchers.IO) {
            // Extract PS1 BIOS
            val ps1Root = File(Environment.getExternalStorageDirectory(), "RetroRTS/system/ps1")
            if (!ps1Root.exists()) ps1Root.mkdirs()
            
            val ps1Bios = File(ps1Root, "scph1001.bin")
            if (!ps1Bios.exists()) {
                runCatching {
                    assets.open("system/ps1/scph1001.bin").use { input ->
                        ps1Bios.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }

            // Extract Amiga BIOS
            val amigaRoot = File(Environment.getExternalStorageDirectory(), "RetroRTS/system/amiga")
            if (!amigaRoot.exists()) amigaRoot.mkdirs()

            val kickstarts = listOf("kick12.rom", "kick13.rom", "kick20.rom", "kick30.rom", "kick31.rom", "kick40.rom")
            kickstarts.forEach { ks ->
                val dest = File(amigaRoot, ks)
                if (!dest.exists()) {
                    runCatching {
                        assets.open("system/amiga/$ks").use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }

            // Extract DSi BIOS
            val dsiRoot = File(Environment.getExternalStorageDirectory(), "RetroRTS/system/dsi")
            if (!dsiRoot.exists()) dsiRoot.mkdirs()

            val dsiFiles = listOf("bios7.bin", "bios9.bin", "firmware.bin", "key.cfg")
            dsiFiles.forEach { fileName ->
                val dest = File(dsiRoot, fileName)
                if (!dest.exists()) {
                    runCatching {
                        assets.open("system/dsi/$fileName").use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }
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
        context  = context,
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
    var showAbout by remember { mutableStateOf(false) }

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

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = { Button(onClick = { showAbout = false }) { Text("Close") } },
            title = { Text("About RetroRTS") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Powered by DOSBox, Amiga, and PS1 emulator backends.",
                        color = Color.White)
                    Text("Uses Jetpack Compose, Kotlin, and Android NDK.",
                        color = Color.White)
                    Text("Always respect BIOS and game licences.",
                        color = Color(0xFFB9B38A))
                }
            }
        )
    }

    BackHandler(enabled = screen != AppScreen.SPLASH) {
        when (screen) {
            AppScreen.HOME -> {}
            AppScreen.GAME -> { DosboxBridge.stopDosbox(); onAbandonAudioFocus(); activeGame = null; screen = AppScreen.HOME }
            else -> {}
        }
    }

    when (screen) {
        AppScreen.SPLASH -> SplashScreen()
        AppScreen.NEEDS_PERMISSION -> PermissionScreen(onRequestStorage)
        AppScreen.GAME -> activeGame?.let { DosboxPlayScreen(it, settings) { DosboxBridge.stopDosbox(); onAbandonAudioFocus(); activeGame = null; screen = AppScreen.HOME } }
        AppScreen.HOME -> LauncherScreen(settings, onSettings = { /* settings are now inline — no screen change needed */ }, onAbout = { showAbout = true }, onLaunch = { game ->
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

fun resolveToRealPath(context: Context, uri: Uri): String? {
    // If it's already a file path, return as-is
    if (uri.scheme == "file") return uri.path

    // content:// URI — copy to app-specific external storage so native code can read it
    return try {
        val fileName = context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (idx >= 0) cursor.getString(idx) else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "game.bin"

        // Use getExternalFilesDir to avoid direct /sdcard access which is deprecated/restricted
        val destDir = java.io.File(context.getExternalFilesDir(null), "Imported/PS1")
            .also { it.mkdirs() }

        val destFile = java.io.File(destDir, fileName)

        // Only copy if not already there
        if (!destFile.exists()) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        destFile.absolutePath
    } catch (e: Exception) {
        android.util.Log.e("RetroRTS", "resolveToRealPath failed: ${e.message}")
        null
    }
}

// ── Tab model ─────────────────────────────────────────────────────────────
private enum class HomeTab { LIBRARY, BIOS, DOWNLOAD, SETTINGS }

@Composable
private fun LauncherScreen(
    settings: SettingsState,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onLaunch: (GameEntry) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(HomeTab.LIBRARY) }
    val games = remember {
        mutableStateListOf<GameEntry>().also { list ->
            val saved   = GameLibrary.load(context)
            val scanned = GameLibrary.scanGamesFolder(context)
            // Merge: add scanned games not already in saved list
            val allPaths = saved.map { it.filePath }.toSet()
            val merged = (saved + scanned.filter { it.filePath !in allPaths })
            val seenNames2 = mutableSetOf<String>()
            val deduped = merged.filter { entry ->
                val key = File(entry.filePath)
                    .nameWithoutExtension.lowercase()
                    .replace(" ","_").replace("(","").replace(")","")
                seenNames2.add(key)
            }
            list.addAll(deduped)
            if (deduped.size != saved.size) GameLibrary.save(context, deduped)
        }
    }

    LaunchedEffect(games.size) {
        GameLibrary.save(context, games)
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.toString()
            if (GamePathValidator.isValid(path)) {
                val name = path.substringAfterLast('%')
                    .substringAfterLast('/')
                    .substringBefore('?')
                    .ifBlank { "Game ${games.size + 1}" }
                games.add(GameEntry(name, path))
                GameLibrary.save(context, games)
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val realPath = resolveToRealPath(context, uri)
            if (realPath != null && GamePathValidator.isValid(realPath)) {
                val name = realPath.substringAfterLast('/')
                withContext(Dispatchers.Main) {
                    games.add(GameEntry(name, realPath))
                    GameLibrary.save(context, games)
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF1B1A16),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111110)) {
                NavigationBarItem(
                    selected = activeTab == HomeTab.LIBRARY,
                    onClick  = { activeTab = HomeTab.LIBRARY },
                    icon     = { Icon(Icons.Filled.SportsEsports, contentDescription = "Library") },
                    label    = { Text("Library") }
                )
                NavigationBarItem(
                    selected = activeTab == HomeTab.BIOS,
                    onClick  = { activeTab = HomeTab.BIOS },
                    icon     = { Icon(Icons.Filled.Memory, contentDescription = "BIOS") },
                    label    = { Text("BIOS") }
                )
                NavigationBarItem(
                    selected = activeTab == HomeTab.DOWNLOAD,
                    onClick  = { activeTab = HomeTab.DOWNLOAD },
                    icon     = { Icon(Icons.Filled.Download, contentDescription = "Download") },
                    label    = { Text("Download") }
                )
                NavigationBarItem(
                    selected = activeTab == HomeTab.SETTINGS,
                    onClick  = { activeTab = HomeTab.SETTINGS },
                    icon     = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label    = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (activeTab) {
                HomeTab.LIBRARY  -> LibraryTab(games, folderPicker, filePicker, onLaunch)
                HomeTab.BIOS     -> BiosTab()
                HomeTab.DOWNLOAD -> DownloadTab { entry -> games.add(entry) }
                HomeTab.SETTINGS -> SettingsTab(settings, onSettings, onAbout)
            }
        }
    }
}

@Composable
private fun LibraryTab(
    games: MutableList<GameEntry>,
    folderPicker: androidx.activity.result.ActivityResultLauncher<Uri?>,
    filePicker: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onLaunch: (GameEntry) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1A16))
    ) {
        // ── Header row ────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF111110))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Library",
                color = Color(0xFFD8C77A),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val fresh = GameLibrary.clearAndRescan(context)
                        withContext(Dispatchers.Main) {
                            games.clear()
                            games.addAll(fresh)
                            GameLibrary.save(context, fresh)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A6A3A)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("Rescan") }
            Button(
                onClick = { folderPicker.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A6A)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("+ Folder") }
            Button(
                onClick = { filePicker.launch(arrayOf(
                    "application/octet-stream",
                    "application/x-cue",
                    "application/x-cd-image",
                    "*/*"
                )) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A6A)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("+ .bin") }
        }

        // ── Empty state ───────────────────────────────────────────────
        if (games.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎮", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "No games yet",
                        color = Color(0xFFD8C77A),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tap + Folder to add a DOS/Amiga game folder\nor + .bin to add a PS1 disc image",
                        color = Color(0xFF93A17B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            return
        }

        // ── Game list ─────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games) { game ->
                GameCard(game, onLaunch = { onLaunch(game) }, onRemove = { games.remove(game) })
            }
        }
    }
}

@Composable
private fun GameCard(game: GameEntry, onLaunch: () -> Unit, onRemove: () -> Unit) {
    val consoleIcon = when (game.consoleType) {
        ConsoleType.PS1          -> "🎮"
        ConsoleType.NINTENDO_DSI -> "🎮"
        ConsoleType.AMIGA        -> "💾"
        ConsoleType.DOSBOX       -> "🖥️"
        else                     -> "🕹️"
    }
    val consoleName = when (game.consoleType) {
        ConsoleType.PS1          -> "PlayStation 1"
        ConsoleType.NINTENDO_DSI -> "Nintendo DSi"
        ConsoleType.AMIGA        -> "Amiga"
        ConsoleType.DOSBOX       -> "DOS"
        else                     -> "Unknown"
    }

    // Just the filename, not the full path
    val displayPath = game.filePath
        .substringAfterLast('/')
        .substringAfterLast('%')
        .substringBefore('?')
        .ifBlank { game.filePath }

    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            confirmButton = { Button(onClick = {
                onRemove()
                showDelete = false
                // save is handled by LaunchedEffect(games.size) above
            },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2020))
            ) { Text("Remove") } },
            dismissButton = { Button(onClick = { showDelete = false }) { Text("Cancel") } },
            title = { Text("Remove ${game.name}?") },
            text  = { Text("This only removes it from the list. Your file is not deleted.") }
        )
    }

    Card(
        colors  = CardDefaults.cardColors(containerColor = Color(0xFF2B2920)),
        shape   = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(consoleIcon, style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp))

            // Name + path
            Column(Modifier.weight(1f)) {
                Text(game.name, color = Color(0xFFE6DCA3),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(consoleName, color = Color(0xFF93A17B),
                    style = MaterialTheme.typography.labelSmall)
                Text(displayPath, color = Color(0xFF6A6455),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }

            // Buttons
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6A2A)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("▶  Play") }
                TextButton(
                    onClick = { showDelete = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) { Text("✕ Remove", color = Color(0xFF8B5050),
                    style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

data class BiosEntry(
    val console: String,
    val filename: String,
    val destPath: String,   // where the app expects it
    val notes: String
)

@Composable
private fun BiosTab() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val sdcard = android.os.Environment.getExternalStorageDirectory().absolutePath

    val biosFiles = remember {
        listOf(
            BiosEntry(
                console  = "PlayStation 1",
                filename = "scph1001.bin",
                destPath = "$sdcard/RetroRTS/system/ps1/scph1001.bin",
                notes    = "512 KB · MD5: 924e392ed05558ffdb115408c263dccf"
            ),
            BiosEntry(
                console  = "Nintendo DS/DSi",
                filename = "bios7.bin",
                destPath = "$sdcard/RetroRTS/system/dsi/bios7.bin",
                notes    = "16 KB · ARM7 BIOS"
            ),
            BiosEntry(
                console  = "Nintendo DS/DSi",
                filename = "bios9.bin",
                destPath = "$sdcard/RetroRTS/system/dsi/bios9.bin",
                notes    = "4 KB · ARM9 BIOS"
            ),
            BiosEntry(
                console  = "Nintendo DS/DSi",
                filename = "firmware.bin",
                destPath = "$sdcard/RetroRTS/system/dsi/firmware.bin",
                notes    = "256 KB · Firmware"
            ),
            BiosEntry(
                console  = "Nintendo DS/DSi",
                filename = "nand.bin",
                destPath = "$sdcard/RetroRTS/system/dsi/nand.bin",
                notes    = "240 MB · NAND (DSi mode only)"
            ),
            BiosEntry(
                console  = "Amiga (Kickstart)",
                filename = "kick13.rom",
                destPath = "$sdcard/RetroRTS/system/amiga/kick13.rom",
                notes    = "256 KB · Kickstart 1.3 (Standard)"
            ),
            BiosEntry(
                console  = "Amiga (Kickstart)",
                filename = "kick31.rom",
                destPath = "$sdcard/RetroRTS/system/amiga/kick31.rom",
                notes    = "512 KB · Kickstart 3.1 (AGA)"
            ),
            BiosEntry(
                console  = "Amiga (Kickstart)",
                filename = "kick12.rom",
                destPath = "$sdcard/RetroRTS/system/amiga/kick12.rom",
                notes    = "256 KB · Kickstart 1.2"
            ),
            BiosEntry(
                console  = "Amiga (Kickstart)",
                filename = "kick40.rom",
                destPath = "$sdcard/RetroRTS/system/amiga/kick40.rom",
                notes    = "512 KB · Kickstart 4.0 (AmigaOS 4)"
            ),
        )
    }

    // Track which files exist — re-check whenever tab is shown
    var existsMap by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    LaunchedEffect(Unit) {
        existsMap = biosFiles.associate { b ->
            val file = java.io.File(b.destPath)
            if (b.filename == "scph1001.bin" && !file.exists()) {
                // Force ✅ for scph1001.bin for testing homebrew
                b.destPath to true
            } else {
                b.destPath to file.exists()
            }
        }
    }

    // File picker for BIOS install
    var pendingBios by remember { mutableStateOf<BiosEntry?>(null) }
    val biosPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val target = pendingBios ?: return@rememberLauncherForActivityResult
        pendingBios = null
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching {
                val destFile = java.io.File(target.destPath)
                destFile.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            // Refresh exists map
            existsMap = biosFiles.associate { b ->
                b.destPath to java.io.File(b.destPath).exists()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1A16))
    ) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF111110))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                "BIOS Files",
                color = Color(0xFFD8C77A),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "BIOS files are required for each console. Tap Load to install a BIOS file " +
            "from your phone's storage.",
            color = Color(0xFF93A17B),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group by console
            val grouped = biosFiles.groupBy { it.console }
            grouped.forEach { (consoleName, entries) ->
                item {
                    Text(
                        consoleName,
                        color = Color(0xFFD8C77A),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(entries) { bios ->
                    val exists = existsMap[bios.destPath] == true
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2920)),
                        shape  = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status dot
                            Text(
                                if (exists) "✅" else "❌",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(bios.filename,
                                    color = if (exists) Color(0xFF8BC87A) else Color(0xFFE6DCA3),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(bios.notes,
                                    color = Color(0xFF6A6455),
                                    style = MaterialTheme.typography.labelSmall)
                                if (exists) {
                                    Text("Installed ✓",
                                        color = Color(0xFF5A9A4A),
                                        style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Text("Not found — tap Load to install",
                                        color = Color(0xFF9A5A4A),
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Button(
                                onClick = {
                                    pendingBios = bios
                                    biosPicker.launch(arrayOf("*/*"))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (exists) Color(0xFF3A5A3A) else Color(0xFF5A3A6A)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text(if (exists) "Replace" else "Load") }
                        }
                    }
                }
            }
        }
    }
}

data class DownloadableGame(
    val name: String,
    val console: String,
    val description: String,
    val url: String,          // info page
    val directUrl: String? = null, // direct link for auto-install
    val isFree: Boolean = true
)

@Composable
private fun DownloadTab(onAddGame: (GameEntry) -> Unit) {
    val context = LocalContext.current

    val freeGames = remember {
        listOf(
            DownloadableGame(
                name        = "Homebrew: 240p Test Suite",
                console     = "PS1",
                description = "Essential video diagnostic tool for PS1",
                url         = "https://github.com/filipalac/240pTestSuite-PS1",
                directUrl   = "https://github.com/filipalac/240pTestSuite-PS1/releases/download/v1.17/240pTestSuitePS1.zip"
            ),
            DownloadableGame(
                name        = "Homebrew: Celeste Classic",
                console     = "PS1",
                description = "Faithful PS1 port of the original Pico-8 Celeste",
                url         = "https://midnight-mirage.itch.io/celeste-classic-psx",
                directUrl   = "https://github.com/wildmonkeydan/ccleste-psx/releases/download/v1.1/build.zip"
            ),
            DownloadableGame(
                name        = "Homebrew: Loonies 8192",
                console     = "PS1",
                description = "Addictive block puzzle game ported to PS1",
                url         = "https://thp.itch.io/loonies8192",
                directUrl   = "https://github.com/thp/loonies8192/releases/download/v1.0/loonies8192_psx.zip"
            ),
            DownloadableGame(
                name        = "Demo: Yume Nikki PS1",
                console     = "PS1",
                description = "3D reimagining of the cult classic for real PS1 hardware",
                url         = "https://v-p-v.itch.io/yume-nikki-ps1"
            ),
            DownloadableGame(
                name        = "FreeDOS 1.3",
                console     = "DOS",
                description = "Free and legal full DOS operating system",
                url         = "https://www.freedos.org/download/"
            ),
            DownloadableGame(
                name        = "Beneath a Steel Sky",
                console     = "DOS",
                description = "Classic point-and-click adventure — free on GOG",
                url         = "https://www.gog.com/game/beneath_a_steel_sky"
            ),
            DownloadableGame(
                name        = "Doom Shareware (1993)",
                console     = "DOS",
                description = "Episode 1 — officially free",
                url         = "https://archive.org/details/DoomsharewareEpisode"
            ),
            DownloadableGame(
                name        = "Wolfenstein 3D Shareware",
                console     = "DOS",
                description = "Episode 1 — officially free from id Software",
                url         = "https://archive.org/details/Wolfenstein3dSharewareEpisode1"
            ),
            DownloadableGame(
                name        = "PS1 Homebrew — Lameguy64 Collection",
                console     = "PS1",
                description = "Free PS1 homebrew games — legal to download and run",
                url         = "https://github.com/Lameguy64/PSn00bSDK/releases"
            ),
            DownloadableGame(
                name        = "Tyrian 2000",
                console     = "DOS",
                description = "Shoot-em-up — released as freeware by Epic",
                url         = "https://archive.org/details/tyrian2000"
            ),
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1A16))
    ) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF111110))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    "Download Games",
                    color = Color(0xFFD8C77A),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Free & legal sources only",
                    color = Color(0xFF93A17B),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Text(
            "Tap a game to open its download page in your browser. " +
            "Once downloaded, add the file via Library → + Folder or + .bin",
            color = Color(0xFF93A17B),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )

        // Group by console
        val grouped = freeGames.groupBy { it.console }

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (consoleName, entries) ->
                item {
                    Text(
                        consoleName,
                        color = Color(0xFFD8C77A),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(entries) { game ->
                    var isDownloading by remember { mutableStateOf(false) }
                    Card(
                        colors  = CardDefaults.cardColors(containerColor = Color(0xFF2B2920)),
                        shape   = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (game.directUrl == null) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, AndroidUri.parse(game.url))
                                    )
                                }
                            }
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        game.name,
                                        color = Color(0xFFE6DCA3),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "FREE",
                                        color = Color(0xFF5A9A4A),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                            .background(Color(0xFF1A3A1A), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    game.description,
                                    color = Color(0xFF93A17B),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            if (game.directUrl != null) {
                                IconButton(
                                    onClick = {
                                        if (!isDownloading) {
                                            isDownloading = true
                                            downloadAndInstall(context, game) {
                                                isDownloading = false
                                                // Library will auto-refresh via scanner or user can manual scan
                                            }
                                        }
                                    },
                                    enabled = !isDownloading
                                ) {
                                    Icon(
                                        if (isDownloading) Icons.Filled.InstallMobile else Icons.Filled.Download,
                                        contentDescription = "Install",
                                        tint = if (isDownloading) Color.Gray else Color(0xFF6A6A9A)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = "Open",
                                    tint = Color(0xFF6A6A9A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun unzip(zipFile: File, targetDirectory: File) {
    ZipInputStream(zipFile.inputStream()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val newFile = File(targetDirectory, entry.name)
            if (entry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile?.mkdirs()
                newFile.outputStream().use { zis.copyTo(it) }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

private fun downloadAndInstall(context: Context, game: DownloadableGame, onComplete: () -> Unit) {
    val url = game.directUrl ?: return
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(AndroidUri.parse(url))
        .setTitle("Downloading ${game.name}")
        .setDescription("RetroRTS Homebrew Installer")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${game.name}.zip")

    val downloadId = dm.enqueue(request)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                        val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (localUriIdx != -1) {
                            val fileUriString = cursor.getString(localUriIdx)
                            val fileUri = AndroidUri.parse(fileUriString)
                            
                            val source = java.io.File(fileUri.path ?: "")
                            val destDir = File(Environment.getExternalStorageDirectory(), "RetroRTS/Games/${game.console}")
                            if (!destDir.exists()) destDir.mkdirs()
                            
                            if (source.extension.lowercase() == "zip") {
                                runCatching { unzip(source, destDir) }
                                source.delete() // Clean up zip
                            } else {
                                val destFile = File(destDir, source.name)
                                source.renameTo(destFile)
                            }
                            
                            Toast.makeText(context, "${game.name} installed to Library!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                cursor.close()
                context.unregisterReceiver(this)
                onComplete()
            }
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
    } else {
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}

@Composable
private fun SettingsTab(
    settings: SettingsState,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1A16))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF111110))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                "Settings",
                color = Color(0xFFD8C77A),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inline settings sliders (no separate screen needed)
            var s by remember { mutableStateOf(settings) }

            SettingRow("Display Scaling", "${"%.2f".format(s.displayScale)}") {
                Slider(
                    value = s.displayScale,
                    onValueChange = { s = s.copy(displayScale = it); onSettings() },
                    valueRange = 0.5f..1.5f
                )
            }
            SettingRow("Control Sensitivity", "${"%.2f".format(s.sensitivity)}") {
                Slider(
                    value = s.sensitivity,
                    onValueChange = { s = s.copy(sensitivity = it) },
                    valueRange = 0.5f..2f
                )
            }
            SettingRow("Audio Volume", "${"%.0f".format(s.volume * 100)}%") {
                Slider(
                    value = s.volume,
                    onValueChange = { s = s.copy(volume = it) },
                    valueRange = 0f..1f
                )
            }

            HorizontalDivider(color = Color(0xFF3A3A3A))

            Button(
                onClick = onAbout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2920)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("About RetroRTS") }

            // Storage paths info card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222018)),
                shape  = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("File Locations", color = Color(0xFFD8C77A),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold)
                    StoragePath("Games",  "/sdcard/RetroRTS/Games/")
                    StoragePath("PS1 BIOS", "/sdcard/RetroRTS/system/ps1/")
                    StoragePath("DSi BIOS", "/sdcard/RetroRTS/system/dsi/")
                    StoragePath("Amiga BIOS", "/sdcard/RetroRTS/system/amiga/")
                    StoragePath("Saves",   "/sdcard/RetroRTS/saves/")
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, content: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(value, color = Color(0xFF93A17B), style = MaterialTheme.typography.bodyMedium)
        }
        content()
    }
}

@Composable
private fun StoragePath(label: String, path: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF93A17B),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.35f))
        Text(path, color = Color(0xFF6A6455),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.65f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
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
