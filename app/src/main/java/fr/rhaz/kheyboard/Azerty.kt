package fr.rhaz.kheyboard

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.azerty.view.*
import org.jetbrains.anko.toast
import org.jetbrains.anko.vibrator
import org.json.JSONObject

fun Kheyboard.Azerty() = inflate(R.layout.azerty){

    input.requestFocus()

    fun refresh(){
        if(input.length() < 3){
            selected.clear()
            list.adapter?.notifyDataSetChanged()
        }
        else{
            val args = JSONObject().put("search", input.text)
            val req = JsonObjectRequest(POST, "https://risibank.fr/api/v0/search", args,
                Response.Listener { json ->
                    println(json)
                    val stickers = json.getJSONArray("stickers")
                    list.display(stickers)
                },
                Response.ErrorListener {  }
            )
            Volley.newRequestQueue(ctx).add(req)
        }
    }

    fun write(char: String): () -> Unit = {
        input.text.insert(input.selectionEnd, char)
        if(Config.vibrations) vibrator.vibrate(100)
    }

    val backspace: () -> Unit = {
        if (input.length() > 0) {
            if (input.hasSelection()) {
                input.text.delete(input.selectionStart, input.selectionEnd)
            } else{
                input.text.delete(input.selectionEnd - 1, input.selectionEnd)
            }
            if(Config.vibrations) vibrator.vibrate(100)
        }
    }

    val delete: () -> Unit = {
        input.text.clear()
    }

    val actions = mapOf<View, () -> Any>(
        searchbtn to { refresh() },
        risibankbtn to { risibank() },
        spacebar to write(" "),
        backspacebtn to backspace,
        deletebtn to delete,
        lettreA to write("a"), lettreB to write("b"), lettreC to write("c"), lettreD to write("d"), lettreE to write("e"),
        lettreF to write("f"), lettreG to write("g"), lettreH to write("h"), lettreI to write("i"), lettreJ to write("j"),
        lettreK to write("k"), lettreL to write("l"), lettreM to write("m"), lettreN to write("n"), lettreO to write("o"),
        lettreP to write("p"), lettreQ to write("q"), lettreR to write("r"), lettreS to write("s"), lettreT to write("t"),
        lettreU to write("u"), lettreV to write("v"), lettreW to write("w"), lettreX to write("x"), lettreY to write("y"),
        lettreZ to write("z")
    )

    actions.keys.forEach { it.setOnClickListener { actions[it]?.invoke() } }

    spacebar.setOnLongClickListener {
        val man = ctx.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE)
        (man as InputMethodManager).showInputMethodPicker()
        true
    }

    selected.clear()
    list.adapter = StickerAdapter().apply {
        longClick = { position -> favorite(selected[position]) }
    }

}