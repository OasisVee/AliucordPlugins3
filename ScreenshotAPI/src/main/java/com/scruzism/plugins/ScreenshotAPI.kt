package com.scruzism.plugins

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.TextView
import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.Logger
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.fragments.SettingsPage
import com.discord.api.commands.ApplicationCommandType
import com.lytefast.flexinput.R
import com.aliucord.views.TextInput
import java.net.URLEncoder
import java.io.File
import java.io.FileOutputStream

@AliucordPlugin
class ScreenshotAPI : Plugin() {

    private val log = Logger("ScreenshotAPI")
    
    init {
        settingsTab = SettingsTab(ScreenshotAPISettings::class.java, SettingsTab.Type.PAGE).also {
            it.withArgs(this)
        }
    }

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

        commands.registerCommand("screenshot", "Screenshot a website", args) { cmdContext ->
            val url = URLEncoder.encode(cmdContext.getRequiredString("url"))
            val shouldSend = cmdContext.getBoolOrDefault("send", false)
            try {
                val apiKey = settings.getString("api_key", null)
                if (apiKey.isNullOrEmpty()) {
                    return@registerCommand CommandsAPI.CommandResult("API key is not set. Please configure the API key in settings.", null, false)
                }

                // Direct URL to the screenshot image
                val imageUrl = "https://api.screenshotmachine.com?key=$apiKey&url=$url&dimension=1024x768&format=png"
                log.debug("Original URL: ${cmdContext.getRequiredString("url")}")
                log.debug("Encoded URL: $url")
                log.debug("Should send as attachment: $shouldSend")
                
                if (shouldSend) {
                    // Download the image and send as attachment
                    try {
                        log.debug("Downloading screenshot from URL: [URL REDACTED FOR SECURITY]")
                        val res = Http.Request(imageUrl).execute()
                        log.debug("Download response received, status: ${res.statusCode}")
                        
                        val file = File.createTempFile("screenshot", ".png", ctx.cacheDir)
                        log.debug("Temp file created at: ${file.absolutePath}")
                        
                        FileOutputStream(file).use { fos -> 
                            res.pipe(fos)
                            log.debug("Image data written to file, size: ${file.length()} bytes")
                        }
                        file.deleteOnExit()
                        
                        // Check if file exists and has content
                        if (!file.exists() || file.length() == 0L) {
                            log.error("File doesn't exist or is empty after download")
                            return@registerCommand CommandsAPI.CommandResult("Failed to save screenshot. File is empty or missing.", null, false)
                        }
                        
                        val fileUri = Uri.fromFile(file).toString()
                        log.debug("File URI created: $fileUri")
                        
                        // Add the file as an attachment
                        cmdContext.addAttachment(fileUri, "screenshot.png")
                        log.debug("Attachment added to context")
                        
                        return@registerCommand CommandsAPI.CommandResult("Screenshot attached", null, true, "ScreenshotAPI")
                    } catch (e: Exception) {
                        log.error("Error in screenshot attachment process", e)
                        return@registerCommand CommandsAPI.CommandResult("Failed to process screenshot: ${e.message ?: "Unknown error"}", null, false)
                    }
                } else {
                    val embed = MessageEmbedBuilder().setRandomColor().setImage(imageUrl, null, 876, 1680).build()
                    return@registerCommand CommandsAPI.CommandResult(null, mutableListOf(embed), false, "ScreenshotAPI")
                }
            } catch (t: Throwable) {
                log.error("Top-level exception in command", t)
                return@registerCommand CommandsAPI.CommandResult("An error occurred: ${t.message ?: "Unknown error"}. Check Debug Logs", null, false)
            }
        }
    }

    override fun stop(ctx: Context) = commands.unregisterAll()
}

class ScreenshotAPISettings(private val plugin: ScreenshotAPI) : SettingsPage() {

    override fun onViewBound(view: View) {
        super.onViewBound(view)
        setActionBarTitle("ScreenshotAPI Settings")

        val ctx = view.context

        // Use TextInput from Aliucord views like in CheckLinks
        TextInput(ctx, "ScreenshotMachine API Key").run {
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
