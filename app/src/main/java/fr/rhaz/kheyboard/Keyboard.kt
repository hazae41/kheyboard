package fr.rhaz.kheyboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import org.jetbrains.anko.dip
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

val Context.clipboardManager: ClipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

class Kheyboard: InputMethodService() {

    fun inflate(id: Int, builder: View.() -> Unit = {})
    = layoutInflater.inflate(id, null).apply(builder)

    fun risibank() { setInputView(Risibank()) }
    fun azerty() { setInputView(Azerty()) }
    override fun onCreateInputView() = Risibank()

    val settings get() = File(ctx.getExternalFilesDir(null)!!, "settings.json")
    val config get() = if(settings.exists()) JSONObject(settings.readText()) else JSONObject()
    fun config(action: JSONObject.() -> JSONObject){
        val settings = settings
        val result = config.run(action)
        settings.createNewFile()
        settings.writeText(result.toString())
    }

    fun JSONObject.array(name: String) =
        if(name in keys().asSequence())
            getJSONArray(name)
        else JSONArray()

    fun favorite(url: String) {
        if (!URLUtil.isValidUrl(url)) toast("URL invalide")
        else config {
            put("favoris", array("favoris").put(
                JSONObject().put("risibank_link", url)
            ))
        }
        longToast("Ajouté $url")
    }

    fun unfavorite(url: String?) {
        if (url == null) return
        if (!URLUtil.isValidUrl(url)) return
        config {
            val favs = array("favoris")
            for (i in 0..favs.length()) {
                val it = favs.getJSONObject(i)
                if (url == it.getString("risibank_link")) {
                    favs.remove(i)
                    break
                }
            }
            put("favoris", favs)
        }
        longToast("Supprimé $url")
    }

    val selected = mutableListOf<String>()

    fun select(stickers: JSONArray) {
        selected.clear()
        for (i in 0 until stickers.length())
            selected += stickers.getJSONObject(i).getString("risibank_link")
    }

    fun RecyclerView.display(stickers: JSONArray) {
        select(stickers)
        smoothScrollToPosition(0)
        adapter?.notifyDataSetChanged()
    }

    class Sticker(val data: ImageButton) : RecyclerView.ViewHolder(data)

    inner class StickerAdapter: RecyclerView.Adapter<Sticker>() {
        var longClick: (Int) -> Unit = { position -> }

        override fun getItemCount() = selected.size
        override fun onCreateViewHolder(group: ViewGroup, position: Int): Sticker {
            return ImageButton(ctx).run {
                setBackgroundColor(resources.getColor(android.R.color.transparent))
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = ViewGroup.LayoutParams(dip(130), dip(130))
                Sticker(this)
            }
        }

        override fun onBindViewHolder(holder: Sticker, position: Int) {
            Glide.with(ctx).load(selected[position]).into(holder.data)
            holder.data.setOnClickListener {
                currentInputConnection.commitText(selected[position], 1)
            }
            holder.data.setOnLongClickListener {
                longClick(position)
                true
            }
        }
    }
}