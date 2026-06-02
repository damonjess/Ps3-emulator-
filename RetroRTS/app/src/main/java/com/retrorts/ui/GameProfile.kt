package com.retrorts.ui

import org.json.JSONObject
import java.io.File

data class GameProfile(
    val gameId: String,
    val title: String,
    val os: String,
    val cycles: Int,
    val frameCap: Int,
    val memMb: Int,
    val mixerRate: Int,
    val machine: String,
    val platform: String = "dosbox",
) {
    fun toJson(): String = JSONObject()
        .put("gameId", gameId)
        .put("title", title)
        .put("os", os)
        .put("cycles", cycles)
        .put("frameCap", frameCap)
        .put("memMb", memMb)
        .put("mixerRate", mixerRate)
        .put("machine", machine)
        .put("platform", platform)
        .toString(2)

    fun toDosboxConfig(): String = """
        [dosbox]
        machine=$machine
        memsize=$memMb

        [cpu]
        core=dynamic
        cycles=$cycles

        [render]
        frameskip=0

        [mixer]
        rate=$mixerRate
        blocksize=1024
        prebuffer=20

        [sdl]
        priority=higher,normal
    """.trimIndent()

    companion object {
        fun fromJson(json: String): GameProfile {
            val j = JSONObject(json)
            return GameProfile(
                gameId = j.getString("gameId"),
                title = j.getString("title"),
                os = j.getString("os"),
                cycles = j.getInt("cycles"),
                frameCap = j.getInt("frameCap"),
                memMb = j.getInt("memMb"),
                mixerRate = j.getInt("mixerRate"),
                machine = j.getString("machine"),
                platform = j.optString("platform", "dosbox"),
            )
        }

        fun presetRedAlert95() = GameProfile(
            gameId = "cnc_red_alert_win95",
            title = "Command & Conquer: Red Alert",
            os = "Windows 95",
            cycles = 30000,
            frameCap = 60,
            memMb = 64,
            mixerRate = 44100,
            machine = "svga_s3",
        )

        fun presetDune2000Win98() = GameProfile(
            gameId = "dune_2000_win98",
            title = "Dune 2000",
            os = "Windows 98",
            cycles = 35000,
            frameCap = 60,
            memMb = 128,
            mixerRate = 48000,
            machine = "svga_s3",
        )

        fun presetAmigaA500() = GameProfile(
            gameId = "amiga_a500_demo",
            title = "Amiga A500 Demo",
            os = "Amiga Kickstart 1.3",
            cycles = 0,
            frameCap = 50,
            memMb = 1,
            mixerRate = 44100,
            machine = "amiga_a500",
            platform = "amiga",
        )

        fun presetNintendoDsi() = GameProfile(
            gameId = "nintendo_dsi_demo",
            title = "Nintendo DSi Demo",
            os = "Nintendo DSi firmware",
            cycles = 0,
            frameCap = 60,
            memMb = 16,
            mixerRate = 48000,
            machine = "nintendo_dsi",
            platform = "dsi",
        )
    }
}

object GameProfileStore {
    private const val ROOT = "/sdcard/RetroRTS/profiles"

    fun ensurePresetProfiles() {
        runCatching {
            val dir = File(ROOT)
            if (!dir.exists()) dir.mkdirs()
            writeIfMissing(GameProfile.presetRedAlert95())
            writeIfMissing(GameProfile.presetDune2000Win98())
            writeIfMissing(GameProfile.presetAmigaA500())
            writeIfMissing(GameProfile.presetNintendoDsi())
        }
    }

    private fun writeIfMissing(profile: GameProfile) {
        val file = File(ROOT, "${profile.gameId}.json")
        if (!file.exists()) file.writeText(profile.toJson())
    }

    fun loadByGameName(name: String): GameProfile {
        ensurePresetProfiles()
        val key = name.lowercase()
        val gameId = gameIdForName(key)
        val file = File(ROOT, "$gameId.json")
        return runCatching {
            if (file.exists()) GameProfile.fromJson(file.readText()) else presetForGameId(gameId)
        }.getOrElse { presetForGameId(gameId) }
    }

    private fun gameIdForName(key: String): String = when {
        "red alert" in key -> "cnc_red_alert_win95"
        "dune 2000" in key -> "dune_2000_win98"      // This already uses DOSBox - GOOD
        "amiga" in key || "a500" in key -> "amiga_a500_demo"
        "dsi" in key || "nintendo ds" in key -> "nintendo_dsi_demo"
        else -> key.replace(" ", "_")
    }

    private fun presetForGameId(gameId: String): GameProfile = when (gameId) {
        "cnc_red_alert_win95" -> GameProfile.presetRedAlert95()
        "amiga_a500_demo" -> GameProfile.presetAmigaA500()
        "nintendo_dsi_demo" -> GameProfile.presetNintendoDsi()
        else -> GameProfile.presetDune2000Win98()
    }
}
