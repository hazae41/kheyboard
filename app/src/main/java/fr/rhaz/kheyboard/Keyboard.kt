package fr.rhaz.kheyboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.provider.MediaStore
import android.support.v13.view.inputmethod.EditorInfoCompat
import android.support.v13.view.inputmethod.InputConnectionCompat
import android.support.v13.view.inputmethod.InputContentInfoCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import org.jetbrains.anko.dip
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.jetbrains.anko.vibrator
import org.json.JSONArray
import org.json.JSONObject

val Context.clipboardManager: ClipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

class Kheyboard: InputMethodService() {

    fun inflate(id: Int, builder: View.() -> Unit = {})
    = layoutInflater.inflate(id, null).apply(builder)

    fun risibank() { setInputView(Risibank()) }
    fun azerty() { setInputView(Azerty()) }
    override fun onCreateInputView() = Risibank()

    fun JSONObject.array(name: String) =
        if(name in keys().asSequence())
            getJSONArray(name)
        else JSONArray()

    fun favorite(url: String) {
        if (!URLUtil.isValidUrl(url)) toast("URL invalide")
        else Config.json {
            put("favoris", array("favoris").put(
                JSONObject().put("risibank_link", url)
            ))
        }
        longToast("Ajouté $url")
    }

    fun unfavorite(url: String?) {
        if (url == null) return
        if (!URLUtil.isValidUrl(url)) return
        Config.json {
            val favs = array("favoris")
            for (i in 0..favs.length()) {
                val it = favs.getJSONObject(i)
                if (url == it.getString("risibank_link")) {
                    favs.remove(i)
                    break
                }
            }
            put("favoris", favs)
        }
        longToast("Supprimé $url")
    }

    val selected = mutableListOf<String>()

    fun select(stickers: JSONArray) {
        selected.clear()
        for (i in 0 until stickers.length())
            selected += stickers.getJSONObject(i).getString("risibank_link")
    }

    fun RecyclerView.display(stickers: JSONArray) {
        select(stickers)
        smoothScrollToPosition(0)
        adapter?.notifyDataSetChanged()
    }

    class Sticker(val data: ImageButton) : RecyclerView.ViewHolder(data)

    inner class StickerAdapter: RecyclerView.Adapter<Sticker>() {
        var longClick: (Int) -> Unit = { position -> }

        override fun getItemCount() = selected.size
        override fun onCreateViewHolder(group: ViewGroup, position: Int): Sticker {
            return ImageButton(ctx).run {
                setBackgroundColor(resources.getColor(android.R.color.transparent))
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = ViewGroup.LayoutParams(dip(130), dip(130))
                Sticker(this)
            }
        }

        fun commitImage(url: String) = Glide.with(ctx).asBitmap().load(url).into(bitmapReceiver { commitImage(it) })

        fun bitmapReceiver(then: (Bitmap) -> Unit) = object: SimpleTarget<Bitmap>(){
            override fun onResourceReady(img: Bitmap, transition: Transition<in Bitmap>?) = then(img)
        }

        fun commitImage(img: Bitmap){
            val str = MediaStore.Images.Media.insertImage(contentResolver, img, "Sticker", "Sticker")
            val uri = Uri.parse(str)
            val clip = ClipDescription("Sticker", arrayOf("image/gif"))
            val inputContentInfo = InputContentInfoCompat(uri, clip, null)
            val inputConnection = currentInputConnection
            val editorInfo = currentInputEditorInfo
            var flags = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                flags = flags or InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
            }
            InputConnectionCompat.commitContent(inputConnection, editorInfo, inputContentInfo, flags, null)
        }

        override fun onBindViewHolder(holder: Sticker, position: Int) {
            Glide.with(ctx).load(selected[position]).into(holder.data)
            holder.data.setOnClickListener {
                val types = EditorInfoCompat.getContentMimeTypes(currentInputEditorInfo)
                val gifSupport = types.any {
                    ClipDescription.compareMimeTypes(it, "image/gif")
                }
                if(gifSupport && !Config.useUrls) commitImage(selected[position])
                else currentInputConnection.commitText(selected[position], 1)
                if(Config.vibrations) vibrator.vibrate(100)
            }
            holder.data.setOnLongClickListener {
                longClick(position)
                true
            }
        }
    }
}