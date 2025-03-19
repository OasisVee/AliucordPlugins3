package com.scruzism.plugins

import android.view.View
import android.widget.TextView
import android.annotation.SuppressLint
import android.text.util.Linkify

import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Divider

import com.lytefast.flexinput.R
import com.discord.utilities.color.ColorCompat
import com.discord.views.CheckedSetting


class PluginSettings(private val settings: SettingsAPI) : SettingsPage() {

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View?) {
        super.onViewBound(view)
        setActionBarTitle("UITH - Catbox.moe")
        val ctx = requireContext()
        val p = DimenUtils.defaultPadding

        // HEADER
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Catbox.moe Image Uploader"
            addView(this)
        }

        // TEXT
        TextView(ctx).apply {
            text = "This plugin uploads images to catbox.moe instead of Discord's servers."
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary))
            setPadding(p, p, p, p)
            addView(this)
        }

        // DIV
        val divider = Divider(ctx).apply { setPadding(p, p, p, p) }

        // ADVANCED SETTINGS HEADER
        val advHeader = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply { text = "Settings" }

        // CHECKED SETTINGS
        val uploadAllAttachments = Utils.createCheckedSetting(
                ctx, CheckedSetting.ViewType.CHECK,
                "Upload all attachment types", "Try to upload all attachment types instead of just images.\n(Warning: Might error)"
        ).apply {
            isChecked = settings.getBool("uploadAllAttachments", false)
            setOnCheckedListener {
                settings.setBool("uploadAllAttachments", it)
            }
        }
        val switchOffPlugin = Utils.createCheckedSetting(
                ctx, CheckedSetting.ViewType.CHECK,
                "Disable UITH", "Disable this plugin to send attachments normally.\nSlash command available: \"/uith disable\""
        ).apply {
            isChecked = settings.getBool("pluginOff", false)
            setOnCheckedListener {
                settings.setBool("pluginOff", it)
            }
        }

        // 2nd DIV
        val secondDivider = Divider(ctx).apply { setPadding(p, p, p, p) }

        // ABOUT HEADER
        val aboutHeader = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply { text = "About" }

        // ABOUT TEXT 
        val aboutText = TextView(ctx).apply {
            text = "This plugin uploads images to catbox.moe instead of Discord's servers. Catbox.moe is a free image hosting service with no account required.\n\n" +
                   "When you send an image, it will be uploaded to catbox.moe and the link will be posted in the chat automatically."
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary))
            setPadding(p, p, p, p)
        }

        addView(divider)
        
        addView(advHeader)
        addView(uploadAllAttachments)
        addView(switchOffPlugin)
        
        addView(secondDivider)
        addView(aboutHeader)
        addView(aboutText)
    }
}
