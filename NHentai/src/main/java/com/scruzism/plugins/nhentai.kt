package com.scruzism.plugins

import android.content.Context

import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.entities.Plugin
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.Utils.createCommandChoice
import com.aliucord.Utils.createCommandOption
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI.CommandResult

import com.discord.api.commands.ApplicationCommandType
import com.discord.api.message.embed.MessageEmbed

import java.util.Date
import java.text.SimpleDateFormat

val BASE_URL = "https://nhentai.net/api"
val AVATAR   = "https://media.discordapp.net/attachments/807840893054353430/907975245951029278/42k98qa0aam31.png"
val DOMAIN = "i2"

// convert to proper extension
private fun ext(t: String): String {
    return when (t) {
        "j" -> "jpg"
        "p" -> "png"
        "g" -> "gif"
        else -> "" // won't reach
    }
}

// convert to human readable time (bad example)
private fun Int.humanize(): String {
    val date = Date(this * 1000L)
    return SimpleDateFormat("dd MMM yyyy").format(date)
}

// (info)
// get the comic details and send as embed
private fun getComic(id: String): MessageEmbed {
    val result = Http.simpleJsonGet(BASE_URL + "/gallery/$id", Result::class.java)

    return MessageEmbedBuilder().setRandomColor()
            .setTitle(result.title.pretty)
            .setImage("https://$DOMAIN.nhentai.net/galleries/${result.media_id}/cover.${ext(result.images.cover.t)}",
                    null, result.images.cover.h, result.images.cover.w)
            .addField("ID", result.id.toString(), false)
            .addField("Number of Pages", result.num_pages.toString(), false)
            .addField("Number of Favorites", result.num_favorites.toString(), false)
            .addField("Uploaded at", result.upload_date.humanize(), false)
            .build()
}

// (pages)
// get the pages of the comic and send as embed
private fun getPages(id: String): MutableList<MessageEmbed> {
    // First get the comic info to get the media_id
    val result = Http.simpleJsonGet("$BASE_URL/gallery/$id", PageData::class.java)
    val pages = result.images.pages
    val embeds = mutableListOf<MessageEmbed>()

    // Make sure we have pages to display
    if (pages.isNotEmpty()) {
        // For each page, create an embed
        for (i in 0 until pages.size) {
            val pageNum = i + 1 // Page numbers start from 1
            val ext = ext(pages[i].t)

            // Ensure the extension is always appended
            val imageUrl = "https://$DOMAIN.nhentai.net/galleries/${result.media_id}/${pageNum}.$ext"

            val embed = MessageEmbedBuilder().setRandomColor()
                    .setImage(imageUrl, null, pages[i].h, pages[i].w)
                    .setFooter("Page $pageNum/${pages.size}")
                    .build()
            embeds.add(embed)
        }
    }
    return embeds
}

// (search)
// search and get the 25 results in 1st page and send as embed
private fun getSearch(q: String, sort: String): MutableList<MessageEmbed> {
    val params = "?query=$q&sort=$sort" // "&page=..." SOON:tm:
    val results = Http.simpleJsonGet("$BASE_URL/galleries/search$params", SearchResult::class.java).result
    val embeds = mutableListOf<MessageEmbed>()
    val embed = MessageEmbedBuilder().setColor(0xe00061)
            .setTitle("You searched for: $q..")
    for (i in 0 until results.size) {
        val res = results[i]
        embed.addField("${i+1}. ${res.title.pretty}",
                "ID: `${res.id}`\nPages: `${res.num_pages}`\nUploaded: `${res.upload_date.humanize()}`\nFavorites: `${res.num_favorites}`",
                false)
    }
    embeds.add(embed.build())
    return embeds
}

@AliucordPlugin
class NHentai : Plugin() {

    private val LOG = Logger("NHentai")

    override fun start(ctx: Context) {
        val args = listOf(
                createCommandOption(
                        ApplicationCommandType.SUBCOMMAND,
                        "info",
                        "Enter ID of the comic to get its info",
                        subCommandOptions = listOf(
                                createCommandOption(
                                        ApplicationCommandType.STRING,
                                        "id",
                                        "Enter the ID of the comic",
                                        required = true
                                )
                        )
                ),
                createCommandOption(
                        ApplicationCommandType.SUBCOMMAND,
                        "pages",
                        "Enter ID of the comic to get it's pages",
                        subCommandOptions = listOf(
                                createCommandOption(
                                        ApplicationCommandType.STRING,
                                        "id",
                                        "Enter the ID of the comic",
                                        required = true
                                )
                        )
                ),
                createCommandOption(
                        ApplicationCommandType.SUBCOMMAND,
                        "search",
                        "Search NHentai",
                        subCommandOptions = listOf(
                                createCommandOption(
                                        ApplicationCommandType.STRING,
                                        "query",
                                        "Enter a query to search",
                                        required = true
                                ),
                                createCommandOption(
                                        ApplicationCommandType.STRING,
                                        "sort",
                                        "Sort results by Date or Popularity | Default: Date",
                                        choices = listOf(
                                                createCommandChoice("date", "date"),
                                                createCommandChoice("popular", "popular")
                                        )
                                )
                        )
                )
        )

        commands.registerCommand("nhentai", ":smirk:", args) {
            if (it.containsArg("info")) {
                try {
                    val id = it.getSubCommandArgs("info")?.get("id").toString()
                    val embed = getComic(id)
                    return@registerCommand CommandResult(null, mutableListOf(embed), false, "NHentai", AVATAR)
                }
                catch (t: Throwable) {
                    LOG.error(t)
                    return@registerCommand CommandResult("An error occurred: ${t.message}", null, false, "NHentai", AVATAR)
                }
            }

            if (it.containsArg("pages")) {
                try {
                    val id = it.getSubCommandArgs("pages")?.get("id").toString()
                    val embeds = getPages(id)
                    if (embeds.isEmpty()) {
                        return@registerCommand CommandResult("No pages found for this ID.", null, false, "NHentai", AVATAR)
                    }
                    return@registerCommand CommandResult("Loading pages...", embeds, false, "NHentai", AVATAR)
                }
                catch (t: Throwable) {
                    LOG.error(t)
                    return@registerCommand CommandResult("An error occurred: ${t.message}", null, false, "NHentai", AVATAR)
                }
            }

            if (it.containsArg("search")) {
                try {
                    val query = it.getSubCommandArgs("search")?.get("query").toString()
                    val sort = it.getStringOrDefault("sort", "date")
                    val embeds = getSearch(query, sort)
                    return@registerCommand CommandResult(null, embeds, false, "NHentai", AVATAR)
                }
                catch (t: Throwable) {
                    LOG.error(t)
                    return@registerCommand CommandResult("An error occurred: ${t.message}", null, false, "NHentai", AVATAR)
                }
            }
            CommandResult("An unknown error occurred. Please check the Debug Logs for info and ask scruz if help needed.",
                    null, false, "NHentai", AVATAR)
        }
    }

    override fun stop(ctx: Context) = commands.unregisterAll()
}
