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
import com.aliucord.fragments.SettingsPage
import com.discord.api.commands.ApplicationCommandType
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

    override fun getSettingsLayout(ctx: Context): SettingsPage {
        return object : SettingsPage() {
            @SuppressLint("SetTextI18n")
            override fun onViewBound(view: View) {
                super.onViewBound(view)
                setActionBarTitle("ScreenshotAPI Settings")

                val ctx = view.context
                val plugin = PluginManager.plugins["ScreenshotAPI"] as ScreenshotAPI

                com.aliucord.views.TextInput(ctx, "API Key").run {
                    editText.run {
                        maxLines = 1
                        setText(plugin.settings.getString("api_key", ""))
                        hint = "Enter your ScreenshotMachine API key here"

                        addTextChangedListener(object : com.discord.utilities.view.text.TextWatcher() {
                            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable) {
                                plugin.settings.setString("api_key", s.toString())
                            }
                        })
                    }

                    linearLayout.addView(this)
                }

                TextView(ctx, null, 0, R.i.UiKit_Settings_Item_SubText).apply {
                    text = "You can get a free API key from screenshotmachine.com. The plugin will not work without a valid API key."
                    linearLayout.addView(this)
                }
            }
        }
    }
}
