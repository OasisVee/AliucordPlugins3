package com.scruzism.plugins

import android.content.Context
import android.webkit.MimeTypeMap

import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.entities.Plugin
import com.aliucord.annotations.AliucordPlugin

import com.aliucord.patcher.before
import com.aliucord.utils.GsonUtils
import com.aliucord.api.CommandsAPI.CommandResult
import com.discord.api.commands.ApplicationCommandType
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.input.ChatInputViewModel
import com.lytefast.flexinput.model.Attachment

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.IOException
import java.lang.IndexOutOfBoundsException
import java.util.regex.Pattern


private fun uploadToCatbox(file: File, log: Logger): String {
    val lock = Object()
    val result = StringBuilder()

    synchronized(lock) {
        Utils.threadPool.execute {
            try {
                val params = mutableMapOf<String, Any>()
                val resp = Http.Request("https://catbox.moe/user/api.php", "POST")

                params["reqtype"] = "fileupload"
                params["fileToUpload"] = file
                
                result.append(resp.executeWithMultipartForm(params).text())
            }
            catch (ex: Throwable) {
                if (ex is IOException) {
                    log.debug("${ex.message} | ${ex.cause} | $ex | ${ex.printStackTrace()}")
                }
                log.error(ex)
            }
            finally {
                synchronized(lock) {
                    lock.notifyAll()
                }
            }
        }
        lock.wait(9_000)
    }
    try {
        log.debug("API RESPONSE: ${result.toString()}")
    } catch (e: JSONException) {
        log.debug("API RESPONSE: ${result.toString()}")
    }
    return result.toString()
}

@AliucordPlugin
class UITH : Plugin() {

    init {
        settingsTab = SettingsTab(PluginSettings::class.java).withArgs(settings)
    }

    private val LOG = Logger("UITH")

    // For modifying the message content
    private val textContentField = MessageContent::class.java.getDeclaredField("textContent").apply { isAccessible = true }
    private fun MessageContent.set(text: String) = textContentField.set(this, text)

    override fun start(ctx: Context) {

        val args = listOf(
                Utils.createCommandOption(
                        ApplicationCommandType.SUBCOMMAND, "current", "View current UITH settings"
                ),
                Utils.createCommandOption(
                        ApplicationCommandType.SUBCOMMAND, "disable", "Disable plugin",
                        subCommandOptions = listOf(
                                Utils.createCommandOption(
                                        ApplicationCommandType.BOOLEAN,
                                        "disable",
                                        required = true
                                )
                        )
                )
        )
        
        commands.registerCommand("uith", "Upload Image To Catbox", args) {
            if (it.containsArg("current")) {
                val settingsUploadAllAttachments = settings.getBool("uploadAllAttachments", false)
                val settingsPluginOff = settings.getBool("pluginOff", false)
                val sb = StringBuilder()
                sb.append("Host: catbox.moe\n\n")
                sb.append("uploadAllAttachments: `$settingsUploadAllAttachments`\n")
                sb.append("pluginOff: `$settingsPluginOff`")
                return@registerCommand CommandResult(sb.toString(), null, false)
            }

            if (it.containsArg("disable")) {
                val set = it.getSubCommandArgs("disable")?.get("disable").toString()
                if (set.lowercase() == "true") settings.setBool("pluginOff", true)
                if (set.lowercase() == "false") settings.setBool("pluginOff", false)
                return@registerCommand CommandResult(
                        "Plugin Disabled: ${settings.getBool("pluginOff", false)}", null, false
                )
            }

            CommandResult("", null, false)
        }

        patcher.before<ChatInputViewModel>(
                "sendMessage",
                Context::class.java,
                MessageManager::class.java,
                MessageContent::class.java,
                List::class.java,
                Boolean::class.javaPrimitiveType!!,
                Function1::class.java
        ) {
            val context = it.args[0] as Context
            val content = it.args[2] as MessageContent
            val plainText = content.textContent
            val attachments = (it.args[3] as List<Attachment<*>>).toMutableList()
            val firstAttachment = try { attachments[0] } catch (t: IndexOutOfBoundsException) { return@before }

            // Check if plugin is OFF
            if (settings.getBool("pluginOff", false)) { return@before }

            // Check if multiple attachments provided
            if (attachments.size > 1) {
                Utils.showToast("UITH: Multiple attachment uploads are not supported!", true)
                return@before
            }

            // Check file type and don't upload if `uploadAllAttachments` is false
            val mime = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.getContentResolver().getType(firstAttachment.uri)) as String
            if (mime !in arrayOf("png", "jpg", "jpeg", "webp", "gif")) {
                if (settings.getBool("uploadAllAttachments", false) == false) {
                    return@before
                }
            }

            Utils.showToast("UITH: Uploading to catbox.moe...", false)
            val url = uploadToCatbox(File(firstAttachment.data.toString()), LOG)

            // If upload failed, show error
            if (!url.startsWith("https://")) {
                Utils.showToast("UITH: Upload failed, check debug logs", true)
                LOG.error("Upload failed, response: $url")
                return@before
            }

            // Send message with the URL received from catbox
            content.set("$plainText\n$url")
            it.args[2] = content
            it.args[3] = emptyList<Attachment<*>>()
            return@before
        }
    }

    override fun stop(ctx: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
    }
}
