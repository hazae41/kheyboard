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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

fun Kheyboard.Risibank() = layoutInflater.inflate(R.layout.keyboard_risibank) {

    var data = JSONObject()
    var error = false
    var loading = true
    val stickers = emptyDataSourceTyped<String>()
    lateinit var selected: Button

    fun request() = deferred<JSONObject> { resolve, reject ->
        val req = JsonObjectRequest("https://risibank.fr/api/v0/load", null, resolve, reject)
        Volley.newRequestQueue(this@Risibank).add(req)
    }

    val buttons = listOf(nouveauxbtn, populairesbtn, favorisbtn, aleatoiresbtn)

    fun updateTips() {
        when (selected) {
            favorisbtn -> {
                error_msg.visibility = GONE
                loading_bar.visibility = GONE
                favoris_tip.visibility = when (stickers.isEmpty()) {
                    true -> VISIBLE
                    false -> GONE
                }
            }
            else -> {
                favoris_tip.visibility = GONE
                error_msg.visibility = when (error) {
                    true -> VISIBLE
                    false -> GONE
                }
                loading_bar.visibility = when (loading) {
                    true -> VISIBLE
                    false -> GONE
                }
            }
        }
    }

    fun refresh() {
        val array = when (selected) {
            nouveauxbtn -> data.array("tms")
            populairesbtn -> data.array("views")
            favorisbtn -> Config.config.array("favoris")
            aleatoiresbtn -> data.array("random")
            else -> return
        }
        stickers.clear()
        stickers.set(array.getStickers())
        recycler.smoothScrollToPosition(0)
        updateTips()
    }

    fun update(button: Button) {
        if (button != selected) {
            button.setBackgroundColor(resources.getColor(android.R.color.transparent))
            button.setTextColor(resources.getColor(android.R.color.white))
        } else {
            button.background = getDrawable(R.drawable.shape_button)
        }
    }

    fun select(button: Button) {
        selected = button
        refresh()
        buttons.forEach { update(it) }
    }

    buttons.forEach { button ->
        button.setOnClickListener {
            select(button)
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
                    refresh()
                } else favorite(item)
                vibrate()
            }
        }
    }

    suspend fun reload() {
        try {
            val res = request().await()
            data = res.getJSONObject("stickers")
            loading = false
            error = false
            refresh()
        } catch (ex: Exception) {
            ex.printStackTrace()
            loading = false
            error = true
            updateTips()
            delay(1000)
            reload()
        }
    }

    select(populairesbtn)

    GlobalScope.launch(Dispatchers.Main) {
        reload()
    }
}