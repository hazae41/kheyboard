package fr.rhaz.kheyboard

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.risibank.view.*
import org.jetbrains.anko.inputMethodManager
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import org.json.JSONObject

fun Kheyboard.Risibank() = inflate(R.layout.risibank) {

    val list = list

    var data = JSONObject()
    fun reload(then: () -> Unit) {
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

    fun Button.display(){
        when (this) {
            nouveauxbtn -> list.display(data.getJSONArray("tms"))
            populairesbtn -> list.display(data.getJSONArray("views"))
            favorisbtn -> list.display(Config.config.array("favoris"))
            aleatoiresbtn -> list.display(data.getJSONArray("random"))
            else -> {}
        }
    }

    lateinit var selectedbtn: Button
    fun Button.unselected() = setBackgroundColor(resources.getColor(android.R.color.transparent))
    fun Button.selected() {
        selectedbtn = this
        setBackgroundColor(resources.getColor(R.color.light_gray))
    }

    fun Button.clicked() {
        buttons.forEach { it.unselected() }
        this.selected()
        this.display()
    }

    buttons.forEach { button ->
        button.setOnClickListener { button.clicked() }
    }

    fun refresh() = reload { selectedbtn.clicked() }

    refreshbtn.setOnClickListener { refresh() }

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

    spacebtn.setOnClickListener {
        currentInputConnection.commitText(" ", 1)
    }

    searchbtn.setOnClickListener { azerty() }

    returnbtn.setOnClickListener {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
    }

    addbtn.setOnClickListener {
        val clipboard = clipboardManager.primaryClip?.getItemAt(0)
        val url = clipboard?.text?.toString()
        if(url == null)
            toast("Copiez un lien pour l'ajouter aux favoris")
        else{
            favorite(url)
            favorisbtn.clicked()
        }
    }

    list.adapter = StickerAdapter().apply {
        longClick = { position ->
            if (selectedbtn == favorisbtn){
                unfavorite(selected[position])
                favorisbtn.display()
            }
            else favorite(selected[position])
        }
    }

    reload { populairesbtn.clicked() }
}