package com.retrorts.download

data class DownloadableSuite(
    val id: String,
    val name: String,
    val description: String,
    val platform: String,       // PS1, AMIGA, DOSBOX, NINTENDO_DSI
    val downloadUrl: String,
    val fileName: String,       // e.g. "240p_suite_ps1.zip"
    val installSubPath: String  // e.g. "PS1", "Amiga", "DOSBox", "NintendoDSi"
)
