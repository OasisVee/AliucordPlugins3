package com.scruzism.plugins

import android.content.Context

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.Logger
import com.aliucord.entities.Plugin
import com.aliucord.Constants.ALIUCORD_GUILD_ID
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.Utils.createCommandChoice
import com.aliucord.api.CommandsAPI.CommandResult

import com.discord.api.commands.ApplicationCommandType
import com.discord.api.message.embed.MessageEmbed

class Result(val url: String)

private fun buildEmbeds(urls: MutableList<String>, text: String? = null): List<MessageEmbed> {
    val embedList = mutableListOf<MessageEmbed>()
    for (url in urls) {
        val embed = MessageEmbedBuilder().setImage(url).setRandomColor()
        if (text != null) { embed.apply { embed.setFooter(text) } }
        embedList.add(embed.build())
    }
    return embedList
}

private fun makeReq(chosen: String, count: Long): List<String> {
    val urls = mutableListOf<String>()
    for (i in 0 until count) {
        Thread.sleep(2_000)
        val result = Http.simpleJsonGet("https://nekos.life/api/v2/img/$chosen", Result::class.java)
        urls.add(result.url)
    }
    return urls
}

@AliucordPlugin
class NekosLife : Plugin() {

    private val LOG = Logger("NekosLife")

    override fun start(ctx: Context) {
        val choices = listOf(
                createCommandChoice("avatar", "avatar"),
                createCommandChoice("cuddle", "cuddle"),
                createCommandChoice("feed", "feed"),
                createCommandChoice("fox girl", "fox_girl"),
                createCommandChoice("gasm", "gasm"),
                createCommandChoice("gecg", "gecg"),
                createCommandChoice("kiss", "kiss"),
                createCommandChoice("lewd", "lewd"),
                createCommandChoice("meow", "meow"),
                createCommandChoice("neko", "neko"),
                createCommandChoice("neko gif", "ngif"),
                createCommandChoice("smug", "smug"),
                createCommandChoice("slap", "slap"),
                createCommandChoice("spank", "spank"),
                createCommandChoice("waifu", "waifu"),
                createCommandChoice("wallpaper", "wallpaper"),
                createCommandChoice("woof", "woof"),
        )
        val limitChoices = listOf(
                createCommandChoice("2", "2"),
                createCommandChoice("5", "5"),
                createCommandChoice("8", "8"),
                createCommandChoice("10", "10")
        )

        val args = listOf(
                Utils.createCommandOption(
                        ApplicationCommandType.STRING,
                        "category",
                        "Category of image/gif to get",
                        required = true,
                        choices = choices
                ),
                Utils.createCommandOption(
                        ApplicationCommandType.BOOLEAN,
                        "send",
                        "Send to chat (WARNING: Use NSFW channel)"
                ),
                Utils.createCommandOption(
                        ApplicationCommandType.STRING,
                        "limit",
                        "The limit of results to get. Default (1)",
                        choices = limitChoices
                )
        )
        commands.registerCommand(
                "nekoslife",
                "Get images/gifs from nekos.life",
                args
        )
        { ctx ->
            val chosen = ctx.getRequiredString("category")
            val send   = ctx.getBoolOrDefault("send", false)
            val limit  = ctx.getLongOrDefault("limit", 1)
            val urls   = makeReq(chosen, limit)
            try {
                if (ctx.currentChannel.guildId == ALIUCORD_GUILD_ID && send) {
                    val embeds = buildEmbeds(urls as MutableList<String>, "Won't send this in Aliucord server.")
                    return@registerCommand CommandResult(null, embeds, false, "NekosLife")
                }

                else {
                    if (send) {
                        CommandResult(urls.joinToString("\n"), null, true)
                    }
                    else {
                        val embeds = buildEmbeds(urls as MutableList<String>)
                        CommandResult(null, embeds, false, "NekosLife")
                    }
                }
            }
            catch (t: Throwable) {
                LOG.error(t)
                CommandResult("Oops, an error occured. Check Debug Logs.",
                        null, false, "NekosLife")
            }
        }
    }

    override fun stop(ctx: Context) = commands.unregisterAll()

}
