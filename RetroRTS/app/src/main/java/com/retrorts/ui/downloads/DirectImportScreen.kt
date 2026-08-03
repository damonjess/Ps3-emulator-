package com.retrorts.ui.downloads

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.retrorts.download.DirectUrlImporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectImportScreen() {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from URL") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Paste a direct download link to a game file you own or have permission to download. " +
                       "The app will automatically place it in the correct RetroRTS/Games folder.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Direct download URL") },
                placeholder = { Text("https://example.com/game.zip") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (urlText.isBlank()) {
                        Toast.makeText(context, "Enter a URL first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                        Toast.makeText(context, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val id = DirectUrlImporter.enqueueDownload(context, urlText)
                    if (id != -1L) {
                        isDownloading = true
                        Toast.makeText(context, "Download started…", Toast.LENGTH_SHORT).show()
                        urlText = ""
                    } else {
                        Toast.makeText(context, "Failed to start download", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download & Auto-Install")
            }

            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.myabandonware.com/"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find Games (External Browser)")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Supported formats:",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "• PS1: .bin, .cue, .img, .iso, .pbp, .chd\n" +
                       "• Amiga: .adf, .hdf, .dms, .ipf\n" +
                       "• DOSBox: .exe, .com, .bat, .zip\n" +
                       "• DSi: .nds, .dsi, .srl",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.weight(1f))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Only download games you legally own or that are freeware/shareware. " +
                           "RetroRTS does not host or endorse copyrighted material.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
