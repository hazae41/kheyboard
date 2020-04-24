package fr.rhaz.kheyboard

import android.content.ClipData
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy.DATA
import kotlinx.android.synthetic.main.sticker_infos.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class Sticker(val keyboard: Keyboard, val info: StickerInfo) : FakeFragment {
    override val view by lazy { onCreate() }

    private fun onCreate() = keyboard.layoutInflater.inflate(R.layout.sticker_infos, null).apply {
        btn_back.setOnClickListener {
            keyboard.setInputView(keyboard.current.view)
            keyboard.current.onResume()
        }

        btn_send.setOnClickListener {
            keyboard.send(info)
        }

        btn_copy.setOnClickListener {
            val clip = ClipData.newPlainText("Sticker", info.url)
            keyboard.clipboard.setPrimaryClip(clip)
            keyboard.push(info)
            keyboard.toast("Lien copié")
        }

        btn_favorite.setOnClickListener {
            keyboard.favorite(info, btn_favorite.isSelected)
            btn_favorite.isSelected = !btn_favorite.isSelected
        }

        btn_expand.setOnClickListener {
            keyboard.expanded = !keyboard.expanded
            onExpand(keyboard.expanded)
        }

        logo.setOnClickListener {
            keyboard.inputs.showInputMethodPicker();
        }

        logo.setOnLongClickListener {
            MediaPlayer.create(keyboard, R.raw.risitas).start()
            true
        }

        btn_download.setOnClickListener {
            keyboard.push(info)
            GlobalScope.launch(IO) {
                val file = Glide.with(keyboard).asFile().load(info.url)
                    .diskCacheStrategy(DATA).submit().get()

                val downloads =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                val target = File(downloads, info.name)

                file.copyTo(target, true)

                target.setLastModified(System.currentTimeMillis())
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(target)
                keyboard.sendBroadcast(intent)

                withContext(Main) {
                    keyboard.toast("Sticker téléchargé")
                }
            }
        }
        
        text_tags.text = info.tags
        swiper.isEnabled = false
        onExpand(keyboard.expanded)
        btn_favorite.isSelected = keyboard.isFavorite(info)
        Glide.with(keyboard).load(info.url).diskCacheStrategy(DATA).into(image)
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

    override fun onResume() {}
}