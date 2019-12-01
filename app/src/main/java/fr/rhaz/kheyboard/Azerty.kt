package fr.rhaz.kheyboard

import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.android.volley.Request.Method.POST
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import fr.rhaz.kheyboard.utils.*
import kotlinx.android.synthetic.main.keyboard_azerty.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class Azerty(val kheyboard: Kheyboard) {

    val stickers = emptyDataSourceTyped<String>()
    val view = kheyboard.layoutInflater.inflate(R.layout.keyboard_azerty, null)

    fun request(search: CharSequence) = deferred<JSONObject> { resolve, reject ->
        val args = JSONObject().put("search", search)
        val req = JsonObjectRequest(POST, "https://risibank.fr/api/v0/search", args, resolve, reject)
        Volley.newRequestQueue(kheyboard).add(req)
    }

    fun search(): Unit = view.run {
        if (input.length() < 3) {
            kheyboard.toast("Entrez au moins 3 caractères")
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            try {
                tipText.visibility = GONE
                progressBar.visibility = VISIBLE
                val res = request(input.text).await()
                progressBar.visibility = GONE
                val array = res.array("stickers")
                stickers.set(kheyboard.getStickers(array))
                recycler.smoothScrollToPosition(0)
            } catch (ex: Exception) {
                ex.printStackTrace()
                tipText.visibility = VISIBLE
                progressBar.visibility = GONE
                kheyboard.longToast("Impossible de se connecter, essayez de désactiver l'économiseur de batterie")
            }
        }
    }

    fun initSearchButton() = view.run {
        searchbtn.setOnClickListener {
            search()
        }
    }


    fun initStickers() = view.run {
        recycler.setup {
            withDataSource(stickers)
            kheyboard.run {
                withStickers(R.layout.keyboard_sticker_horizontal) {
                    onLongClick {
                        when (kheyboard.isFavorite(item)) {
                            false -> favorite(item)
                            true -> unfavorite(item)
                        }
                        vibrate()
                    }
                }
            }
        }
    }

    fun initLetters() = view.run {
        fun Map.Entry<TextView, String>.assign() {
            key.setOnClickListener {
                input.text.insert(input.selectionEnd, value)
            }
        }

        mapOf<TextView, String>(
                spacebar to " ",
                lettreA to "a",
                lettreB to "b",
                lettreC to "c",
                lettreE to "e",
                lettreF to "f",
                lettreG to "g",
                lettreH to "h",
                lettreI to "i",
                lettreJ to "j",
                lettreK to "k",
                lettreL to "l",
                lettreM to "m",
                lettreN to "n",
                lettreO to "o",
                lettreP to "p",
                lettreQ to "q",
                lettreR to "r",
                lettreS to "s",
                lettreT to "t",
                lettreU to "u",
                lettreV to "v",
                lettreW to "w",
                lettreX to "x",
                lettreY to "y",
                lettreZ to "z"
        ).forEach { it.assign() }
    }

    fun initSpacebar() = view.run {
        spacebar.setOnLongClickListener {
            val input = kheyboard.inputMethodManager
            input.showInputMethodPicker()
            true
        }
    }

    fun initBackspaceButton() = view.run {
        backspacebtn.setOnClickListener {
            if (input.length() == 0) return@setOnClickListener

            if (input.hasSelection()) {
                input.text.delete(input.selectionStart, input.selectionEnd)
            } else {
                input.text.delete(input.selectionEnd - 1, input.selectionEnd)
            }
        }
    }

    fun initDeleteButton() = view.run {
        deletebtn.setOnClickListener {
            input.text.clear()
        }
    }

    fun initRisibankButton() = view.run {
        risibankbtn.setOnClickListener {
            kheyboard.risibank.switch()
        }
    }

    init {
        initStickers()
        initLetters()
        initSpacebar()
        initBackspaceButton()
        initDeleteButton()
        initSearchButton()
        initRisibankButton()
    }

    fun switch() {
        kheyboard.setInputView(view)
        view.input.requestFocus()
    }
}