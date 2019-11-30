package fr.rhaz.kheyboard

import android.content.ClipDescription
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.webkit.URLUtil
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.afollestad.recyclical.ItemDefinition
import com.afollestad.recyclical.RecyclicalSetup
import com.afollestad.recyclical.ViewHolder
import com.afollestad.recyclical.withItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import fr.rhaz.kheyboard.utils.array
import fr.rhaz.kheyboard.utils.deferred
import fr.rhaz.kheyboard.utils.toast
import fr.rhaz.kheyboard.utils.vibrator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException


class StickerViewHolder(itemView: View) : ViewHolder(itemView) {
    val image = itemView.findViewById<ImageView>(R.id.image)
}

class Kheyboard : InputMethodService() {

    lateinit var azerty: Azerty
    lateinit var risibank: Risibank
    val Config = Config(this)

    override fun onCreateInputView(): View {
        azerty = Azerty(this)
        risibank = Risibank(this)

        return risibank.view
    }

    fun getStickers(array: JSONArray): List<String> {
        return (0 until array.length()).map {
            array.getJSONObject(it).getString("risibank_link")
        }
    }

    fun vibrate() {
        if (Config.vibrations) vibrator.vibrate(100)
    }

    fun favorite(url: String) {
        if (!URLUtil.isValidUrl(url)) {
            toast("URL invalide")
            return
        }

        Config.json {
            val favoris = array("favoris")
            val sticker = JSONObject().put("risibank_link", url)
            favoris.put(sticker)
            put("favoris", favoris)
        }

        toast("Ajouté aux favoris")
    }

    fun unfavorite(url: String) {
        if (!URLUtil.isValidUrl(url)) return

        Config.json {
            val favoris = array("favoris")

            getStickers(favoris).forEachIndexed { i, it ->
                if (url == it) favoris.remove(i)
            }

            put("favoris", favoris)
        }

        toast("Supprimé des favoris")
    }

    fun RecyclicalSetup.withStickers(layoutRes: Int, block: ItemDefinition<String, StickerViewHolder>.() -> Unit) {
        withItem<String, StickerViewHolder>(layoutRes) {
            onBind(::StickerViewHolder) { _, item ->
                Glide.with(this@Kheyboard).load(item).into(image)
            }
            onClick {
                val types = EditorInfoCompat.getContentMimeTypes(currentInputEditorInfo)
                val gifSupport = types.any {
                    ClipDescription.compareMimeTypes(it, "image/gif")
                }
                if (gifSupport && !Config.useUrls) commitImage(item)
                else currentInputConnection.commitText(item, 1)
                if (Config.vibrations) vibrator.vibrate(100)
            }
            block()
        }
    }

    fun commitImage(url: String) = GlobalScope.launch {
        try {
            val resource = download(url).await()
            val file = File(getExternalFilesDir(null), "stickers/sticker.gif")
            resource.copyTo(file, true)
            commitImage(file)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun commitImage(file: File) {
        val uri = FileProvider.getUriForFile(this, "fr.rhaz.kheyboard.fileprovider", file)
        val clip = ClipDescription("Sticker", arrayOf("image/gif"))

        val inputContentInfo = InputContentInfoCompat(uri, clip, null)
        val inputConnection = currentInputConnection
        val editorInfo = currentInputEditorInfo

        val flags = run {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) 0
            else InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        }

        InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                inputContentInfo,
                flags,
                null
        )
    }
}

fun Context.download(url: String) = deferred<File> { resolve, reject ->
    val req = object : RequestListener<File> {
        override fun onLoadFailed(
                e: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean
        ): Boolean {
            if (e != null) reject(e)
            else reject(IOException("Error"))
            return false
        }

        override fun onResourceReady(
                resource: File,
                model: Any?,
                target: Target<File>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
        ): Boolean {
            resolve(resource)
            return false
        }
    }
    Glide.with(this).download(url).listener(req).submit()
}