package com.scruzism.plugins

import android.view.View
import android.text.Editable
import android.widget.TextView
import android.annotation.SuppressLint
import android.text.util.Linkify

import com.aliucord.Utils
import com.aliucord.views.Button
import com.aliucord.views.TextInput
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Divider

import com.lytefast.flexinput.R
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.view.text.TextWatcher
import com.discord.views.CheckedSetting

class PluginSettings(private val settings: SettingsAPI) : SettingsPage() {

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View?) {
        super.onViewBound(view)
        setActionBarTitle("Cat-UITH")
        val ctx = requireContext()
        val p = DimenUtils.defaultPadding

        val errorField = TextView(ctx).apply {
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorError))
            setPadding(p, p, p, p)
        }

        // HEADER
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Regex Settings"
            addView(this)
        }

        // TEXT
        TextView(ctx).apply {
            text = "Enter the regex pattern of the URL to receive"
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary))
            setPadding(p, p, p, p)
            addView(this)
        }

        // INPUT BOX
        val input = TextInput(ctx, "Regex")
        input.apply {
            editText.setText(settings.getString("regex", "https:\\/\\/files\\.catbox\\.moe\\/[\\w.-]*"))
            editText.addTextChangedListener(object : TextWatcher() {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {}
            })
        }

        // SAVE BUTTON
        val button = Button(ctx).apply {
            text = "Save"
            setOnClickListener {
                settings.setString("regex", input.editText.text.toString().toRegex().toString())
                Utils.showToast("Saved")
            }
        }

        // RESET JSON BUTTON
        val resetButton = Button(ctx).apply {
            text = "Reset JSON to Default"
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnError))
            setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorError))
            setOnClickListener {
                settings.remove("jsonConfig")
                Utils.showToast("JSON configuration reset to default")
            }
        }

        // DIV
        val divider = Divider(ctx).apply { setPadding(p, p, p, p) }

        // CATBOX SETTINGS HEADER
        val catboxHeader = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply { text = "Catbox Settings" }

        // USERHASH INPUT BOX
        val userhashInput = TextInput(ctx, "Userhash")
        userhashInput.apply {
            editText.setText(settings.getString("catboxUserhash", ""))
            editText.addTextChangedListener(object : TextWatcher() {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {}
            })
        }

        // SAVE USERHASH BUTTON
        val userhashButton = Button(ctx).apply {
            text = "Save"
            setOnClickListener {
                settings.setString("catboxUserhash", userhashInput.editText.text.toString())
                Utils.showToast("Saved")
            }
        }

        // 2nd DIV
        val secondDivider = Divider(ctx).apply { setPadding(p, p, p, p) }

        // ADVANCED SETTINGS HEADER
        val advHeader = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply { text = "Advanced Settings" }

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

        // 3rd DIV
        val thirdDivider = Divider(ctx).apply { setPadding(p, p, p, p) }

        // LINKS HEADER
        val linksHeader = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply { text = "Links" }

        // HELP/INFO
        val helpInfo = TextView(ctx).apply {
            linksClickable = true
            text = "- Support Server: https://discord.gg/tdjBfsvhHT\n- UITH README: https://git.io/JSyri"
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary))
        }
        Linkify.addLinks(helpInfo, Linkify.WEB_URLS)

        addView(input)
        addView(button)
        addView(errorField)

        addView(divider)

        addView(catboxHeader)
        
        // CATBOX USERHASH - Moved here to appear directly under the header
        TextView(ctx).apply {
            text = "Enter your catbox.moe userhash (leave empty for anonymous uploads)"
            setTextColor(ColorCompat.getThemedColor(ctx, R.b.colorOnPrimary))
            setPadding(p, p, p, p)
            addView(this)
        }
        
        addView(userhashInput)
        addView(userhashButton)

        addView(secondDivider)

        addView(advHeader)
        addView(uploadAllAttachments)
        addView(switchOffPlugin)
        addView(resetButton)
        addView(thirdDivider)
        addView(linksHeader)
        addView(helpInfo)
    }
}
