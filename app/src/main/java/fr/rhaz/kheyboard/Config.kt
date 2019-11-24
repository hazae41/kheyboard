package fr.rhaz.kheyboard

import android.content.Context
import org.jetbrains.anko.alert
import org.jetbrains.anko.checkBox
import org.jetbrains.anko.customView
import org.jetbrains.anko.dip
import org.jetbrains.anko.padding
import org.jetbrains.anko.verticalLayout
import org.json.JSONObject
import java.io.File

val Context.Config get() = Config(ctx)

class Config(val ctx: Context) {
    val settings get() = File(ctx.getExternalFilesDir(null)!!, "settings.json")
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

    fun show() = ctx.alert {
        title = "Configuration"
        customView {
            verticalLayout {
                padding = dip(16)
                checkBox("Toujours utiliser les URL") {
                    isChecked = useUrls
                    setOnClickListener {
                        json { put("use-urls", isChecked) }
                    }
                }
                checkBox("Désactiver les vibrations") {
                    isChecked = !vibrations
                    setOnClickListener {
                        json { put("vibrations", !isChecked) }
                    }
                }
            }
        }
        positiveButton("Fermer") {}
        show()
    }
}