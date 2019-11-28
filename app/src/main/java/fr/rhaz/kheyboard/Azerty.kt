package fr.rhaz.kheyboard

import android.view.View
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.android.volley.Request.Method.POST
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import fr.rhaz.kheyboard.utils.array
import fr.rhaz.kheyboard.utils.deferred
import fr.rhaz.kheyboard.utils.inflate
import fr.rhaz.kheyboard.utils.inputMethodManager
import kotlinx.android.synthetic.main.keyboard_azerty.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

fun Kheyboard.Azerty() = layoutInflater.inflate(R.layout.keyboard_azerty) {

    val stickers = emptyDataSourceTyped<String>()

    recycler.setup {
        withDataSource(stickers)
        withStickers {
            onLongClick {
                favorite(item)
                vibrate()
            }
        }
    }

    fun request(search: CharSequence) = deferred<JSONObject> { resolve, reject ->
        val args = JSONObject().put("search", search)
        val req = JsonObjectRequest(POST, "https://risibank.fr/api/v0/search", args, resolve, reject)
        Volley.newRequestQueue(this@Azerty).add(req)
    }

    fun search() {
        if (input.length() < 3) {
            stickers.clear()
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            val res = request(input.text).await()
            val array = res.array("stickers")
            stickers.set(array.getStickers())
            recycler.smoothScrollToPosition(0)
        }
    }

    backspacebtn.setOnClickListener {
        if (input.length() <= 0) return@setOnClickListener

        if (input.hasSelection()) {
            input.text.delete(input.selectionStart, input.selectionEnd)
        } else {
            input.text.delete(input.selectionEnd - 1, input.selectionEnd)
        }

        vibrate()
    }

    deletebtn.setOnClickListener {
        input.text.clear()
        vibrate()
    }

    searchbtn.setOnClickListener {
        search()
        vibrate()
    }

    risibankbtn.setOnClickListener {
        setInputView(risibank)
        vibrate()
    }

    spacebar.setOnLongClickListener {
        inputMethodManager.showInputMethodPicker()
        vibrate()
        true
    }

    fun Map.Entry<View, String>.assign() {
        key.setOnClickListener {
            input.text.insert(input.selectionEnd, value)
            vibrate()
        }
    }

    mapOf<View, String>(
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

    input.requestFocus()
}