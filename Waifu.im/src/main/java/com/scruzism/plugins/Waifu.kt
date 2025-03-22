package com.scruzism.plugins

import android.content.Context

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.Logger
import com.aliucord.entities.Plugin
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.Utils.createCommandChoice
import com.aliucord.api.CommandsAPI.CommandResult

import com.discord.api.commands.ApplicationCommandType
import com.discord.api.message.embed.MessageEmbed

class Result(
        val images: List<Images>
) {
    data class Images(
            val width: String,
            val height: String,
            val url: String
    )
}

private fun request(tag: String, isNsfw: Boolean, log: Logger): Result {
    // Construct the URL with properly formatted tags
    val baseUrl = "https://api.waifu.im/search/?gif=false"
    
    // Create separate URLs for NSFW and SFW requests
    val url = if (isNsfw) {
        "$baseUrl&is_nsfw=true&included_tags=$tag"
    } else {
        "$baseUrl&is_nsfw=false&included_tags=$tag"
    }
    
    log.debug("Making request to: $url")
    
    try {
        val result = Http.simpleJsonGet(url, Result::class.java)
        log.debug("Received ${result.images.size} images")
        return result
    } catch (e: Exception) {
        log.error("Error fetching images", e)
        throw e
    }
}

private fun createEmbed(url: String, height: String, width: String): MessageEmbed {
    return MessageEmbedBuilder()
            .setRandomColor()
            .setImage(url, null, height.toInt(), width.toInt())
            .setAuthor("waifu.im")
            .build()
}


@AliucordPlugin
class Waifu : Plugin() {

    private val log = Logger("Waifu.im")

    override fun start(ctx: Context) {
        // sfwChoices
        val sfwChoices = listOf(
                createCommandChoice("maid", "maid"),
                createCommandChoice("mori-calliope", "mori-calliope"),
                createCommandChoice("oppai", "oppai"),
                createCommandChoice("uniform", "uniform"),
                createCommandChoice("raiden-shogun","raiden-shogun"),
                createCommandChoice("selfies", "selfies"),
                createCommandChoice("waifu", "waifu"),
        )
        
        // Add some NSFW choices as well
        val nsfwChoices = listOf(
                createCommandChoice("hentai", "hentai"),
                createCommandChoice("ero", "ero"),
                createCommandChoice("ecchi", "ecchi")
        )
        
        // Combine all choices based on whether it's NSFW or not
        val allChoices = sfwChoices + nsfwChoices

        val args = listOf(
                Utils.createCommandOption(
                        ApplicationCommandType.STRING,
                        "tags",
                        "Choose a tag - Default: waifu",
                        choices = allChoices
                ),
                Utils.createCommandOption(
                        ApplicationCommandType.BOOLEAN,
                        "is_nsfw",
                        "Whether the image should be NSFW - Default: false",
                ),
                Utils.createCommandOption(
                        ApplicationCommandType.BOOLEAN,
                        "send",
                        "Send image to chat - Default: false"
                )
        )

        commands.registerCommand("waifu", "Get images from waifu.im API", args) {
            val sfwTag = it.getStringOrDefault("tags", "waifu")
            val isNsfw = it.getBoolOrDefault("is_nsfw", false)
            val send = it.getBoolOrDefault("send", false)

            try {
                val result = request(sfwTag, isNsfw, log)
                
                // Check if we got any images back
                if (result.images.isEmpty()) {
                    return@registerCommand CommandResult("No images found for tag: $sfwTag", null, false)
                }
                
                val image = result.images[0]

                if (!send) {
                    val embed = createEmbed(image.url, image.height, image.width)
                    return@registerCommand CommandResult(null, mutableListOf(embed), false)
                }

                CommandResult(image.url, null, send)
            } catch (e: Exception) {
                CommandResult("Error: ${e.message}", null, false)
            }
        }
    }

    override fun stop(ctx: Context) = commands.unregisterAll()
}
