package fr.rhaz.kheyboard

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.MediaPlayer
import android.view.KeyEvent
import android.widget.Button
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import fr.rhaz.kheyboard.utils.*
import kotlinx.android.synthetic.main.keyboard_azerty.view.*
import kotlinx.android.synthetic.main.keyboard_risibank.view.*
import kotlinx.android.synthetic.main.keyboard_risibank.view.backspacebtn
import kotlinx.android.synthetic.main.keyboard_risibank.view.recycler
import kotlinx.android.synthetic.main.keyboard_risibank.view.searchbtn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

fun Kheyboard.Risibank() = layoutInflater.inflate(R.layout.keyboard_risibank) {

    var data = JSONObject()
    val stickers = emptyDataSourceTyped<String>()
    lateinit var selectedbtn: Button

    fun request() = deferred<JSONObject> { resolve, reject ->
        val req = JsonObjectRequest("https://risibank.fr/api/v0/load", null, resolve, reject)
        Volley.newRequestQueue(this@Risibank).add(req)
    }

    fun reload() = GlobalScope.launch {
        try {
            val res = request().await()
            data = res.getJSONObject("stickers")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    val buttons = listOf(nouveauxbtn, populairesbtn, favorisbtn, aleatoiresbtn)

    fun display(array: JSONArray) {
        stickers.clear()
        stickers.set(array.getStickers())
        recycler.smoothScrollToPosition(0)
    }

    fun Button.choose() {
        println(data.keys().asSequence().toList().toTypedArray())
        when (this) {
            nouveauxbtn -> display(data.getJSONArray("tms"))
            populairesbtn -> display(data.getJSONArray("views"))
            favorisbtn -> display(Config.config.array("favoris"))
            aleatoiresbtn -> display(data.getJSONArray("random"))
        }
    }

    fun Button.unselect() {
        setBackgroundColor(resources.getColor(android.R.color.transparent))
    }

    fun Button.select() {
        selectedbtn = this
        setBackgroundColor(resources.getColor(R.color.light_gray))
        choose()
    }

    fun Button.click() {
        buttons.forEach { it.unselect() }
        select()
    }

    buttons.forEach { button ->
        button.setOnClickListener { button.click() }
    }

    donatebtn.setOnClickListener {
        val intent = Intent(this@Risibank, Billing::class.java)
        intent.flags += FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    logo.setOnClickListener {
        inputMethodManager.showInputMethodPicker()
    }

    logo.setOnLongClickListener {
        MediaPlayer.create(this@Risibank, R.raw.risitas).start()
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

    searchbtn.setOnClickListener {
        setInputView(azerty)
        azerty.input.requestFocus()
    }

    returnbtn.setOnClickListener {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
    }

    addbtn.setOnClickListener {
        val clipboard = clipboardManager.primaryClip?.getItemAt(0)
        val url = clipboard?.text?.toString()

        if (url == null) {
            toast("Copiez un lien pour l'ajouter aux favoris")
        } else {
            favorite(url)
        }
    }

    recycler.setup {
        withDataSource(stickers)
        withStickers {
            onLongClick {
                if (selectedbtn == favorisbtn) {
                    unfavorite(item)
                    favorisbtn.choose()
                } else favorite(item)
            }
        }
    }

    GlobalScope.launch(Dispatchers.Main) {
        reload().join()
        populairesbtn.click()
    }
}