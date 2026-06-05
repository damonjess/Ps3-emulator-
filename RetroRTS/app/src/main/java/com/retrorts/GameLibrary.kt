package com.retrorts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GameLibrary {

    private fun libraryFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Config")
            .also { it.mkdirs() }
        return File(dir, "library.json")
    }

    fun save(context: Context, games: List<GameEntry>) {
        runCatching {
            val arr = JSONArray()
            games.forEach { g ->
                arr.put(JSONObject().apply {
                    put("name",     g.name)
                    put("filePath", g.filePath)
                    put("gameId",   g.gameId)
                })
            }
            libraryFile(context).writeText(arr.toString())
        }
    }

    fun load(context: Context): List<GameEntry> {
        return runCatching {
            val text = libraryFile(context).readText()
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
        }.getOrElse {
             // Migration check: try reading from legacy location
             runCatching {
                 val legacy = File(android.os.Environment.getExternalStorageDirectory(), "RetroRTS/library.json")
                 if (legacy.exists()) {
                     val list = JSONArray(legacy.readText()).let { a ->
                         (0 until a.length()).map { i ->
                             val o = a.getJSONObject(i)
                             GameEntry(o.getString("name"), o.getString("filePath"))
                         }
                     }
                     save(context, list)
                     list
                 } else emptyList()
             }.getOrDefault(emptyList())
        }
    }

    fun scanGamesFolder(context: Context): List<GameEntry> {
        val roots = mutableListOf(
            File(context.getExternalFilesDir(null), "Imported"),
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
