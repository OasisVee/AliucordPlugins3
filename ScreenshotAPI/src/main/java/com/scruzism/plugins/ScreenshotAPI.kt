package com.scruzism.plugins

import android.content.Context

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.Logger
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.entities.PluginSettings
import com.discord.api.commands.ApplicationCommandType
import com.discord.api.message.embed.MessageEmbed
import com.discord.views.CheckedSetting
import com.lytefast.flexinput.R

import java.net.URLEncoder

data class APIResponse(
    val screenshot: String
)

@AliucordPlugin
class ScreenshotAPI : Plugin() {

    private val log = Logger("ScreenshotAPI")

    override fun start(ctx: Context) {
        val args = listOf(
                Utils.createCommandOption(
                        ApplicationCommandType.STRING,
                        "url",
                        "Enter website URL",
                        required = true
                ),
                Utils.createCommandOption(
                        ApplicationCommandType.BOOLEAN,
                        "send",
                        "Send to chat",
                )
        )

        commands.registerCommand("screenshot", "Screenshot a website", args) {
            val url = URLEncoder.encode(it.getRequiredString("url"))
            val shouldSend = it.getBoolOrDefault("send", false)
            try {
                val apiKey = settings.getString("api_key", null)
                if (apiKey.isNullOrEmpty()) {
                    return@registerCommand CommandsAPI.CommandResult("API key is not set. Please configure the API key in settings.", null, false)
                }

                val httpUrl = StringBuilder("https://api.screenshotmachine.com?key=$apiKey&url=$url&dimension=1024x768&format=png")
                    .toString()
                log.debug(url)
                val result = Http.Request(httpUrl, "GET").execute().json(APIResponse::class.java).screenshot

                if (!shouldSend) {
                    val embed = MessageEmbedBuilder().setRandomColor().setImage(result, null, 876, 1680).build()
                    return@registerCommand CommandsAPI.CommandResult(null, mutableListOf(embed), false, "ScreenshotAPI")
                }

                CommandsAPI.CommandResult(result, null, shouldSend, "ScreenshotAPI")
            } catch (t: Throwable) {
                log.error(t)
                CommandsAPI.CommandResult("An error occurred. Check Debug Logs", null, false)
            }
        }

    }

    override fun stop(ctx: Context) = commands.unregisterAll()

    override fun getSettingsLayout(ctx: Context): PluginSettings {
        val settings = PluginSettings()
        val apiKeySetting = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.EDIT, "API Key", "Enter your API key here")
        apiKeySetting.setOnCheckedListener { checked ->
            settings.setString("api_key", apiKeySetting.editText.text.toString())
        }
        settings.addSetting(apiKeySetting)
        return settings
    }
}
