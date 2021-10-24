/*
 * Wyvtils, a utility mod for 1.8.9.
 * Copyright (C) 2021 Wyvtils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.qalcyo.rysm.utils

import gg.essential.api.EssentialAPI
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.util.Util
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion
import xyz.qalcyo.rysm.Rysm
import xyz.qalcyo.rysm.Rysm.mc
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import xyz.qalcyo.requisite.Requisite
import xyz.qalcyo.rysm.gui.DownloadGui
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


object Updater {
    var latestTag = ""
    var shouldUpdate = false
    var updateUrl = ""

    /**
     * Stolen from SimpleTimeChanger under AGPLv3
     * https://github.com/My-Name-Is-Jeff/SimpleTimeChanger/blob/master/LICENSE
     */
    fun update() {
        CoroutineScope(Dispatchers.IO + CoroutineName("${Rysm.MOD_NAME}-UpdateChecker")).launch {
            val latestRelease =
                APIUtil.getJSONResponse("https://api.github.com/repos/Qalcyo/${Rysm.MODID}/releases/latest")
            latestTag = latestRelease.get("tag_name").asString

            val currentVersion = ModVersion(Rysm.VERSION)
            latestTag = latestRelease.get("tag_name").asString.substringAfter("v")
            val latestVersion = ModVersion(latestTag)
            if (currentVersion < latestVersion) {
                updateUrl = latestRelease["assets"].asJsonArray[0].asJsonObject["browser_download_url"].asString
                Requisite.getInstance().notifications
                    .push(
                        "Mod Update",
                        "${Rysm.MOD_NAME} $latestTag is available!\nClick here to download it!"
                    ) {
                        EssentialAPI.getGuiUtil().openScreen(DownloadGui(mc.currentScreen))
                    }
                shouldUpdate = true
            }
        }
    }

    /**
     * Adapted from RequisiteLaunchwrapper under LGPLv3
     * https://github.com/Qalcyo/RequisiteLaunchwrapper/blob/main/LICENSE
     */
    fun download(url: String, file: File): Boolean {
        if (file.exists()) return true
        var newUrl = url
        newUrl = newUrl.replace(" ", "%20")
        val downloadClient: HttpClient =
            HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(10000).build())
                .build()
        try {
            FileOutputStream(file).use { fileOut ->
                val downloadResponse: HttpResponse = downloadClient.execute(HttpGet(newUrl))
                val buffer = ByteArray(1024)
                var read: Int
                while (downloadResponse.entity.content.read(buffer).also { read = it } > 0) {
                    fileOut.write(buffer, 0, read)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /**
     * Adapted from Skytils under AGPLv3
     * https://github.com/Skytils/SkytilsMod/blob/1.x/LICENSE.md
     */
    fun addShutdownHook() {
        EssentialAPI.getShutdownHookUtil().register {
            println("Deleting old ${Rysm.MOD_NAME} jar file...")
            try {
                val runtime = getJavaRuntime()
                if (Util.getOSType() == Util.EnumOS.OSX) {
                    println("On Mac, trying to open mods folder")
                    Desktop.getDesktop().open(Rysm.jarFile.parentFile)
                }
                println("Using runtime $runtime")
                val file = File("config/Qalcyo/Deleter-1.2.jar")
                println("\"$runtime\" -jar \"${file.absolutePath}\" \"${Rysm.jarFile.absolutePath}\"")
                if (Util.getOSType() == Util.EnumOS.LINUX) {
                    println("On Linux, giving Deleter jar execute permissions")
                    Runtime.getRuntime()
                        .exec("chmod +x \"${file.absolutePath}\"")
                }
                Runtime.getRuntime()
                    .exec("\"$runtime\" -jar \"${file.absolutePath}\" \"${Rysm.jarFile.absolutePath}\"")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Gets the current Java runtime being used.
     * @link https://stackoverflow.com/a/47925649
     */
    @Throws(IOException::class)
    fun getJavaRuntime(): String {
        val os = System.getProperty("os.name")
        val java = "${System.getProperty("java.home")}${File.separator}bin${File.separator}${
            if (os != null && os.lowercase().startsWith("windows")) "java.exe" else "java"
        }"
        if (!File(java).isFile) {
            throw IOException("Unable to find suitable java runtime at $java")
        }
        return java
    }

    /**
     * Stolen from SimpleTimeChanger under AGPLv3
     * https://github.com/My-Name-Is-Jeff/SimpleTimeChanger/blob/master/LICENSE
     */
    class ModVersion(private val versionString: String) : Comparable<ModVersion> {

        companion object {
            val regex = Regex("^(?<version>[\\d.]+)-?(?<type>\\D+)?(?<typever>\\d+\\.?\\d*)?\$")
        }

        private val matched by lazy {
            regex.find(versionString)
        }
        private val isSafe = matched != null

        val version = matched!!.groups["version"]!!.value
        private val versionArtifact = DefaultArtifactVersion(version)
        private val specialVersionType by lazy {
            val typeString = matched!!.groups["type"]?.value ?: return@lazy UpdateType.RELEASE

            return@lazy UpdateType.values().find { typeString == it.prefix } ?: UpdateType.UNKNOWN
        }
        private val specialVersion by lazy {
            if (specialVersionType == UpdateType.RELEASE) return@lazy null
            return@lazy matched!!.groups["typever"]?.value?.toDoubleOrNull()
        }

        override fun compareTo(other: ModVersion): Int {
            if (!isSafe || !other.isSafe) return -1
            return if (versionArtifact.compareTo(other.versionArtifact) == 0) {
                if (specialVersionType.ordinal == other.specialVersionType.ordinal) {
                    specialVersion!!.compareTo(other.specialVersion!!)
                } else other.specialVersionType.ordinal - specialVersionType.ordinal
            } else versionArtifact.compareTo(other.versionArtifact)
        }
    }

    /**
     * Stolen from SimpleTimeChanger under AGPLv3
     * https://github.com/My-Name-Is-Jeff/SimpleTimeChanger/blob/master/LICENSE
     */
    enum class UpdateType(val prefix: String) {
        UNKNOWN("unknown"),
        RELEASE(""),
        RELEASECANDIDATE("rc"),
        PRERELEASE("beta"),
    }

}