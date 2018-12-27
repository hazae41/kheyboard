package fr.rhaz.kheyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.media.MediaPlayer
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.risibank.view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.text.TextUtils
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.KeyEvent.*


val Context.clipboardManager: ClipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

class Kheyboard : InputMethodService() {

    fun inflate(id: Int, builder: View.() -> Unit = {})
    = layoutInflater.inflate(id, null).apply(builder)

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
}

fun Kheyboard.Risibank() = inflate(R.layout.risibank) {

    class Sticker(val data: ImageButton): RecyclerView.ViewHolder(data)

    val selected = mutableListOf<String>()
    fun select(stickers: JSONArray){
        selected.clear()
        for(i in 0 until stickers.length())
            selected += stickers.getJSONObject(i).getString("risibank_link")
    }

    fun display(stickers: JSONArray) {
        select(stickers)
        list.smoothScrollToPosition(0)
        list.adapter?.notifyDataSetChanged()
    }

    var data = JSONObject()
    fun reload(then: () -> Unit){
        val req = JsonObjectRequest("https://risibank.fr/api/v0/load", null,
            Response.Listener {
                data = it.getJSONObject("stickers")
                then()
            },
            Response.ErrorListener {
                it.printStackTrace()
            }
        )
        Volley.newRequestQueue(ctx).add(req)
    }

    val buttons = listOf(nouveauxbtn, populairesbtn, favorisbtn, aleatoiresbtn)

    fun Button.display() = when (this) {
        nouveauxbtn -> display(data.getJSONArray("tms"))
        populairesbtn -> display(data.getJSONArray("views"))
        favorisbtn -> display(config.array("favoris"))
        aleatoiresbtn -> display(data.getJSONArray("random"))
        else -> {}
    }

    lateinit var selectedbtn: Button
    fun Button.unselected() = setBackgroundColor(resources.getColor(android.R.color.transparent))
    fun Button.selected(){
        selectedbtn = this
        setBackgroundColor(resources.getColor(R.color.light_gray))
    }

    fun Button.clicked() {
        buttons.forEach { it.unselected() }
        selected()
        display()
    }

    buttons.forEach { button ->
        button.setOnClickListener { button.clicked() }
    }

    fun refresh() = reload { selectedbtn.clicked() }

    refreshbtn.setOnClickListener { refresh() }

    fun favorite(url: String?) {
        if (url == null) toast("Copiez un lien pour l'ajouter aux favoris")
        else if (!URLUtil.isValidUrl(url)) toast("URL invalide")
        else config {
            put(
                "favoris", array("favoris").put(
                    JSONObject().put("risibank_link", url)
                )
            )
        }
        longToast("Ajouté $url")
        favorisbtn.clicked()
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
        favorisbtn.clicked()
    }

    logo.setOnClickListener {
        val man = ctx.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE)
        (man as InputMethodManager).showInputMethodPicker()
    }

    logo.setOnLongClickListener {
        MediaPlayer.create(ctx, R.raw.risitas).start()
        true
    }

    backspacebtn.setOnClickListener {
        val selectedText = currentInputConnection.getSelectedText(0)
        if (selectedText == null || selectedText.isEmpty())
            currentInputConnection.deleteSurroundingText(1, 0)
        else currentInputConnection.commitText("", 1)
    }

    returnbtn.setOnClickListener {
        sendDownUpKeyEvents(KEYCODE_ENTER)
    }

    addbtn.setOnClickListener {
        val clipboard = clipboardManager.primaryClip?.getItemAt(0)
        favorite(clipboard?.text.toString())
    }

    list.adapter = object : RecyclerView.Adapter<Sticker>() {
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
                if (selectedbtn == favorisbtn)
                    unfavorite(selected[position])
                else
                    favorite(selected[position])
                true
            }
        }
    }

    reload { populairesbtn.clicked() }
}