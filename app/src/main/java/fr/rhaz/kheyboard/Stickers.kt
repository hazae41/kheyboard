package fr.rhaz.kheyboard

import android.content.Intent
import android.media.MediaPlayer
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.URLUtil
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import com.afollestad.recyclical.datasource.emptyDataSourceTyped
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy.DATA
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.result.Result
import fr.rhaz.kheyboard.Stickers.Category.*
import kotlinx.android.synthetic.main.stickers.view.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class Stickers(val keyboard: Keyboard) : FakeFragment {
    enum class Category { History, New, Favorite, Random, Trending, Popular }

    override val view by lazy { onCreate() }

    var all: JSONObject? = null

    var category = Trending
    val displayed = emptyDataSourceTyped<JSONObject>()

    private fun onCreate() = keyboard.layoutInflater.inflate(R.layout.stickers, null).apply {
        recycler.setup {
            withDataSource(displayed)
            withItem<JSONObject, StickerViewHolder>(R.layout.sticker_vertical) {
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

        btn_link.setOnClickListener {
            val clipboard = keyboard.clipboard.primaryClip?.getItemAt(0)
            val url = clipboard?.text?.toString()
            if (url == null || !URLUtil.isValidUrl(url)) {
                keyboard.toast("Copiez un lien pour l'afficher")
            } else {
                val json = JSONObject()
                json.put("risibank_link", url)
                json.put("tags", "lien")
                val info = StickerInfo(json)
                val view = Sticker(keyboard, info).view
                keyboard.setInputView(view)
            }
        }

        btn_favorite.setOnClickListener {
            category = Favorite
            update(true)
        }

        btn_history.setOnClickListener {
            if (!keyboard.premium) premium()
            else {
                category = History
                update(true)
            }
        }

        btn_trending.setOnClickListener {
            category = Trending
            update(true)
        }

        btn_popular.setOnClickListener {
            category = Popular
            update(true)
        }

        btn_new.setOnClickListener {
            category = New;
            update(true)
        }

        btn_random.setOnClickListener {
            category = Random
            update(true)
        }

        btn_premium.setOnClickListener {
            val intent = Intent(keyboard, Billing::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            keyboard.startActivity(intent)
        }

        btn_search.setOnClickListener {
            keyboard.current = keyboard.search
            keyboard.setInputView(keyboard.current.view)
        }

        logo.setOnClickListener {
            keyboard.inputs.showInputMethodPicker();
        }

        logo.setOnLongClickListener {
            MediaPlayer.create(keyboard, R.raw.risitas).start()
            true
        }

        btn_expand.setOnClickListener {
            keyboard.expanded = !keyboard.expanded
            onExpand(keyboard.expanded)
        }

        onExpand(keyboard.expanded)
        swiper.setOnRefreshListener { refresh() }
        refresh()
    }

    override fun onResume() {
        view.onExpand(keyboard.expanded)
        if (category in listOf(History, Favorite)) view.update(false)
    }

    fun View.onExpand(more: Boolean) {
        val minimized = keyboard.resources.getDimensionPixelSize(R.dimen.height)
        val height = if (more) ConstraintSet.WRAP_CONTENT else minimized
        val constraints = ConstraintSet().apply { clone(layout) }
        constraints.constrainHeight(swiper.id, height)
        constraints.applyTo(layout)

        val icon = if (more) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        btn_expand.setImageDrawable(keyboard.getDrawable(icon))
    }

    fun View.refresh() = GlobalScope.launch(Main) {
        withSwiper(swiper) {
            try {
                download()
                update(true)
            } catch (e: Exception) {
                Toast.makeText(keyboard, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun download() {
        when (val res = Fuel.get("https://risibank.fr/api/v0/load").awaitStringResult()) {
            is Result.Success -> all = JSONObject(res.value).getJSONObject("stickers")
            is Result.Failure -> throw res.error
        }
    }

    private fun View.update(scroll: Boolean) {
        displayed.clear()

        val stickers = when (category) {
            Popular -> all?.getJSONArray("views")?.toList()
            New -> all?.getJSONArray("tms")?.toList()
            Trending -> all?.getJSONArray("trending")?.toList()
            Random -> all?.getJSONArray("random")?.toList()
            History -> keyboard.history.toList().reversed()
            Favorite -> keyboard.favorites.toList().reversed()
        }

        if (stickers != null) displayed.addAll(stickers);

        recycler.visibility = VISIBLE
        layout_premium.visibility = GONE
        btn_history.isSelected = category === History
        btn_favorite.isSelected = category === Favorite
        btn_trending.isSelected = category === Trending
        btn_popular.isSelected = category === Popular
        btn_new.isSelected = category === New
        btn_random.isSelected = category === Random

        if (scroll && displayed.isNotEmpty()) recycler.scrollToPosition(0)
    }

    fun View.premium() {
        layout_premium.visibility = VISIBLE
        recycler.visibility = GONE
    }

}