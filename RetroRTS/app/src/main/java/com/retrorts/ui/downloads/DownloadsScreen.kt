package com.retrorts.ui.downloads

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.retrorts.download.DownloadRepository
import com.retrorts.download.DownloadableSuite

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val suites = remember { DownloadRepository.getAvailableSuites() }
    val unavailablePlatforms = remember { DownloadRepository.getUnavailablePlatforms() }
    val activeDownloads = remember { mutableStateMapOf<String, Long>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (unavailablePlatforms.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Unavailable In This Build",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = unavailablePlatforms.joinToString(
                                separator = ", ",
                                prefix = "Hidden downloads: "
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bundle the matching native core and rebuild to enable them.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (suites.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "No downloadable suites are available for the currently bundled emulator cores.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(suites, key = { it.id }) { suite ->
            SuiteCard(
                suite = suite,
                isDownloading = activeDownloads.containsKey(suite.id),
                onDownload = {
                    val id = DownloadRepository.startDownload(context, suite)
                    if (id != -1L) {
                        activeDownloads[suite.id] = id
                        Toast.makeText(context, "Downloading ${suite.name}…", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Unable to start download.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun SuiteCard(
    suite: DownloadableSuite,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = suite.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = suite.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Platform: ${suite.platform}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Downloading…")
                } else {
                    Text("Download & Install")
                }
            }
        }
    }
}
