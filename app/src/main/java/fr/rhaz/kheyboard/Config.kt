package fr.rhaz.kheyboard

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import kotlinx.android.synthetic.main.settings.view.*
import org.json.JSONObject
import java.io.File

class Config(val ctx: Context) {
    val settings get() = File(ctx.getExternalFilesDir(null), "settings.json")
    val config get() = if (settings.exists()) JSONObject(settings.readText()) else JSONObject()

    fun json(action: JSONObject.() -> JSONObject) {
        val settings = settings
        val result = config.run(action)
        settings.createNewFile()
        settings.writeText(result.toString())
    }

    val useUrls
        get() = config.run {
            if (!has("use-urls")) false
            else getBoolean("use-urls")
        }

    val vibrations
        get() = config.run {
            if (!has("vibrations")) true
            else getBoolean("vibrations")
        }

    fun openSettings() {
        MaterialDialog(ctx).show {
            title(text = "Options")
            customView(R.layout.settings)
            getCustomView().apply {
                useUrlsSwitch.apply {
                    isChecked = useUrls
                    setOnClickListener {
                        json { put("use-urls", isChecked) }
                    }
                }
                vibrateSwitch.apply {
                    isChecked = vibrations
                    setOnClickListener {
                        json { put("vibrations", isChecked) }
                    }
                }
            }
        }
    }
}