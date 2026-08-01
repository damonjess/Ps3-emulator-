package com.retrorts

import com.retrorts.download.DownloadableSuite
import com.retrorts.ui.ConsoleType

object CoreAvailability {
    val hasPs1Core: Boolean = BuildConfig.HAS_PS1_CORE
    val hasDosboxCore: Boolean = BuildConfig.HAS_DOSBOX_CORE

    fun isLaunchSupported(consoleType: ConsoleType): Boolean = when (consoleType) {
        ConsoleType.PS1 -> hasPs1Core
        ConsoleType.DOSBOX -> hasDosboxCore
        ConsoleType.PS2 -> false
        else -> true
    }

    fun unsupportedLaunchMessage(consoleType: ConsoleType): String? = when (consoleType) {
        ConsoleType.PS1 ->
            if (hasPs1Core) null else {
                "PS1 launch is unavailable in this build. " +
                    "Add the PCSX-ReARMed source tree under RetroRTS/app/src/main/cpp/pcsx_rearmed and rebuild."
            }
        ConsoleType.DOSBOX ->
            if (hasDosboxCore) null else {
                "DOS launch is unavailable in this build. " +
                    "Add the DOSBox-Pure AAR/native library to RetroRTS/app/libs and rebuild."
            }
        ConsoleType.PS2 ->
            "PS2 launch is not implemented yet in RetroRTS."
        else -> null
    }

    fun isDownloadSupported(suite: DownloadableSuite): Boolean = when (suite.platform.uppercase()) {
        "PS1" -> hasPs1Core
        "DOSBOX", "DOS" -> hasDosboxCore
        else -> true
    }

    fun unavailableDownloadPlatforms(): List<String> = buildList {
        if (!hasPs1Core) add("PS1")
        if (!hasDosboxCore) add("DOSBox")
    }
}
