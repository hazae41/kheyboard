package fr.rhaz.kheyboard

import android.media.MediaPlayer
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy.DATA
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.search.view.*
import kotlinx.android.synthetic.main.sticker_infos.view.btn_back
import kotlinx.android.synthetic.main.sticker_infos.view.logo
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class Search(val keyboard: Keyboard) : FakeFragment {
    override val view by lazy { onCreate() }

    val displayed = emptyDataSourceTyped<JSONObject>()

    fun onCreate() = keyboard.layoutInflater.inflate(R.layout.search, null).apply {
        btn_back.setOnClickListener {
            keyboard.current = keyboard.stickers
            keyboard.setInputView(keyboard.current.view)
        }

        logo.setOnClickListener {
            keyboard.inputs.showInputMethodPicker();
        }

        logo.setOnLongClickListener {
            MediaPlayer.create(keyboard, R.raw.risitas).start()
            true
        }

        key_a.setOnClickListener {
            input.text.append("a")
            update()
        }

        key_b.setOnClickListener {
            input.text.append("b")
            update()
        }

        key_c.setOnClickListener {
            input.text.append("c")
            update()
        }

        key_d.setOnClickListener {
            input.text.append("d")
            update()
        }

        key_e.setOnClickListener {
            input.text.append("e")
            update()
        }

        key_f.setOnClickListener {
            input.text.append("f")
            update()
        }

        key_g.setOnClickListener {
            input.text.append("g")
            update()
        }

        key_h.setOnClickListener {
            input.text.append("h")
            update()
        }

        key_i.setOnClickListener {
            input.text.append("i")
            update()
        }

        key_j.setOnClickListener {
            input.text.append("j")
            update()
        }

        key_k.setOnClickListener {
            input.text.append("k")
            update()
        }

        key_l.setOnClickListener {
            input.text.append("l")
            update()
        }

        key_m.setOnClickListener {
            input.text.append("m")
            update()
        }

        key_n.setOnClickListener {
            input.text.append("n")
            update()
        }

        key_o.setOnClickListener {
            input.text.append("o")
            update()
        }

        key_p.setOnClickListener {
            input.text.append("p")
            update()
        }

        key_q.setOnClickListener {
            input.text.append("q")
            update()
        }

        key_r.setOnClickListener {
            input.text.append("r")
            update()
        }

        key_s.setOnClickListener {
            input.text.append("s")
            update()
        }

        key_t.setOnClickListener {
            input.text.append("t")
            update()
        }

        key_u.setOnClickListener {
            input.text.append("u")
            update()
        }

        key_v.setOnClickListener {
            input.text.append("v")
            update()
        }

        key_w.setOnClickListener {
            input.text.append("w")
            update()
        }

        key_x.setOnClickListener {
            input.text.append("x")
            update()
        }

        key_y.setOnClickListener {
            input.text.append("y")
            update()
        }

        key_z.setOnClickListener {
            input.text.append("z")
            update()
        }

        btn_space.setOnClickListener {
            input.text.append(" ")
            update()
        }

        btn_backspace.setOnClickListener {
            if (input.text.isNotEmpty()) {
                input.text.delete(input.text.length - 1, input.text.length)
                update()
            }
        }

        btn_backspace.setOnLongClickListener {
            input.text.clear()
            update()
            true
        }
        
        btn_expand.setOnClickListener {
            keyboard.expanded = !keyboard.expanded
            onExpand(keyboard.expanded)
        }

        onExpand(keyboard.expanded)
    }

    fun View.update() = GlobalScope.launch(Main) {
        try {
            displayed.clear()
            if (input.text.length <= 1) return@launch
            val stickers = withContext(IO) { search(input.text.toString()) }
            displayed.addAll(stickers.toList())
        } catch (e: Exception) {
            Toast.makeText(keyboard, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun search(text: String): JSONArray {
        val url = "https://risibank.fr/api/v0/search"
        val args = listOf("search" to text)
        return when (val res = Fuel.post(url, args).awaitStringResult()) {
            is Result.Success -> JSONObject(res.value).getJSONArray("stickers")
            is Result.Failure -> throw res.error
        }
    }

    override fun onResume() {}

    fun View.onExpand(more: Boolean) {
        val minimized = keyboard.resources.getDimensionPixelSize(R.dimen.height)
        val height = if (more) ConstraintSet.WRAP_CONTENT else minimized
        val constraints = ConstraintSet().apply { clone(layout) }
        constraints.constrainHeight(swiper.id, height)
        constraints.applyTo(layout)

        val icon = if (more) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        btn_expand.setImageDrawable(keyboard.getDrawable(icon))

        val layoutManager: RecyclerView.LayoutManager =
            if (more) StaggeredGridLayoutManager(2, VERTICAL)
            else LinearLayoutManager(keyboard, HORIZONTAL, false)

        val stickerLayout =
            if (more) R.layout.sticker_vertical
            else R.layout.sticker_horizontal

        recycler.setup {
            withLayoutManager(layoutManager)
            withDataSource(displayed)
            withItem<JSONObject, StickerViewHolder>(stickerLayout) {
                onClick {
                    val info = StickerInfo(item)
                    val view = Sticker(keyboard, info).view
                    keyboard.setInputView(view)
                }
                onLongClick {
                    keyboard.vibrator.vibrate(1)
                    keyboard.send(StickerInfo(item))
                }
                onBind(::StickerViewHolder) { _, item ->
                    setIsRecyclable(false)
                    val url = StickerInfo(item).url
                    Glide.with(keyboard).load(url).diskCacheStrategy(DATA).into(image)
                }
            }
        }
    }
}