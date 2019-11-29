package fr.rhaz.kheyboard

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.MediaPlayer
import android.view.KeyEvent
import android.view.View.GONE
import android.view.View.VISIBLE
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
import org.json.JSONObject

fun Kheyboard.Risibank() = layoutInflater.inflate(R.layout.keyboard_risibank) {

    var data = JSONObject()
    val stickers = emptyDataSourceTyped<String>()
    lateinit var selected: Button

    fun request() = deferred<JSONObject> { resolve, reject ->
        val req = JsonObjectRequest("https://risibank.fr/api/v0/load", null, resolve, reject)
        Volley.newRequestQueue(this@Risibank).add(req)
    }

    fun reload() = GlobalScope.launch {
        try {
            val res = request().await()
            error_tip.visibility = GONE
            data = res.getJSONObject("stickers")
        } catch (ex: Exception) {
            ex.printStackTrace()
            stickers.clear()
            error_tip.visibility = VISIBLE
        }
    }

    val buttons = listOf(nouveauxbtn, populairesbtn, favorisbtn, aleatoiresbtn)

    fun display(button: Button) {
        val array = when (button) {
            nouveauxbtn -> data.array("tms")
            populairesbtn -> data.array("views")
            favorisbtn -> Config.config.array("favoris")
            aleatoiresbtn -> data.array("random")
            else -> return
        }
        stickers.clear()
        stickers.set(array.getStickers())
        recycler.smoothScrollToPosition(0)
    }

    fun update(button: Button) {
        if (button != selected) {
            button.setBackgroundColor(resources.getColor(android.R.color.transparent))
            button.setTextColor(resources.getColor(android.R.color.white))
        } else {
            button.background = getDrawable(R.drawable.shape_button)
        }
    }

    fun updateTip() {
        favoris_tip.visibility = when (selected == favorisbtn && stickers.isEmpty()) {
            true -> VISIBLE
            false -> GONE
        }
    }

    fun select(button: Button) {
        selected = button
        display(button)
        buttons.forEach { update(it) }
    }

    buttons.forEach { button ->
        button.setOnClickListener {
            select(button)
            updateTip()
            vibrate()
        }
    }

    donatebtn.setOnClickListener {
        val intent = Intent(this@Risibank, Billing::class.java)
        intent.flags += FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        vibrate()
    }

    logo.setOnClickListener {
        inputMethodManager.showInputMethodPicker()
        vibrate()
    }

    logo.setOnLongClickListener {
        MediaPlayer.create(this@Risibank, R.raw.risitas).start()
        vibrate()
        true
    }

    backspacebtn.setOnClickListener {
        val selectedText = currentInputConnection.getSelectedText(0)
        if (selectedText == null || selectedText.isEmpty())
            currentInputConnection.deleteSurroundingText(1, 0)
        else currentInputConnection.commitText("", 1)
        vibrate()
    }

    spacebtn.setOnClickListener {
        currentInputConnection.commitText(" ", 1)
        vibrate()
    }

    searchbtn.setOnClickListener {
        setInputView(azerty)
        azerty.input.requestFocus()
        vibrate()
    }

    returnbtn.setOnClickListener {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        vibrate()
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
                if (selected == favorisbtn) {
                    unfavorite(item)
                    display(selected)
                    updateTip()
                } else favorite(item)
                vibrate()
            }
        }
    }

    select(populairesbtn)

    GlobalScope.launch(Dispatchers.Main) {
        reload().join()
        display(selected)
    }
}