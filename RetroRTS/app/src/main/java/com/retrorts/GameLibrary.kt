package com.retrorts

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GameLibrary {

    private fun libraryFile(): File {
        val dir = File(
            android.os.Environment.getExternalStorageDirectory(),
            "RetroRTS"
        ).also { it.mkdirs() }
        return File(dir, "library.json")
    }

    fun save(games: List<GameEntry>) {
        runCatching {
            val arr = JSONArray()
            games.forEach { g ->
                arr.put(JSONObject().apply {
                    put("name",     g.name)
                    put("filePath", g.filePath)
                    put("gameId",   g.gameId)
                })
            }
            libraryFile().writeText(arr.toString())
        }
    }

    fun load(): List<GameEntry> {
        return runCatching {
            val text = libraryFile().readText()
            val arr  = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                GameEntry(
                    name     = o.getString("name"),
                    filePath = o.getString("filePath"),
                    gameId   = o.optString("gameId", o.getString("name")
                        .lowercase().replace(" ", "_"))
                )
            }
        }.getOrDefault(emptyList())
    }

    fun scanGamesFolder(): List<GameEntry> {
        val roots = listOf(
            File(android.os.Environment.getExternalStorageDirectory(), "RetroRTS/Games"),
            File(android.os.Environment.getExternalStorageDirectory(), "Download")
        )
        
        val validExts = setOf("bin","cue","img","iso","exe","com","bat","adf","hdf","nds","dsi")
        val found = mutableListOf<GameEntry>()

        roots.forEach { root ->
            if (!root.exists()) return@forEach
            root.walkTopDown().maxDepth(3).forEach { file ->
                val ext = file.extension.lowercase()
                if (file.isFile && ext in validExts) {
                    // Skip small files that likely aren't games (except for tiny DOS/Amiga files)
                    if (file.length() < 1024 && ext !in setOf("bat", "com", "cue")) return@forEach

                    found.add(GameEntry(
                        name     = file.nameWithoutExtension,
                        filePath = file.absolutePath
                    ))
                } else if (file.isDirectory && file != root) {
                    val hasGame = file.listFiles()?.any {
                        it.extension.lowercase() in validExts
                    } == true
                    if (hasGame) {
                        found.add(GameEntry(
                            name     = file.name,
                            filePath = file.absolutePath
                        ))
                    }
                }
            }
        }

        // Remove folder entries that have a matching .bin already found
        val binNames = found
            .filter { it.filePath.endsWith(".bin", ignoreCase = true) }
            .map { it.name.substringBeforeLast('.').lowercase() }
            .toSet()

        return found.filter { entry ->
            val isFolder = !entry.filePath.contains('.')
            if (isFolder) {
                entry.name.lowercase() !in binNames
            } else true
        }
    }
}
