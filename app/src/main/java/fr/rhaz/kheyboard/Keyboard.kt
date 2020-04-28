package fr.rhaz.kheyboard

import android.content.ClipDescription
import android.content.ClipDescription.compareMimeTypes
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat.getContentMimeTypes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.recyclical.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy.DATA
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

interface FakeFragment {
    val view: View;
    fun onResume();
}

class StickerViewHolder(itemView: View) : ViewHolder(itemView) {
    val image = itemView.findViewById<ImageView>(R.id.image)
}

suspend fun withSwiper(swiper: SwipeRefreshLayout, action: suspend () -> Unit) {
    swiper.isRefreshing = true
    action()
    swiper.isRefreshing = false
}

class StickerInfo(val json: JSONObject) {
    val name get() = Uri.parse(url).pathSegments.last()
    val url get() = json.getString("risibank_link")
    val tags get() = tryOr("") { json.getString("tags") }
}

class Keyboard : InputMethodService() {
    val stickers = Stickers(this)
    val search = Search(this)

    var current: FakeFragment = stickers;
    var expanded = false

    val settings get() = File(getExternalFilesDir(null), "settings.json")

    override fun onCreateInputView() = current.view

    val inputs get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val vibrator get() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val clipboard get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun supports(mime: String) =
        getContentMimeTypes(currentInputEditorInfo).any { compareMimeTypes(it, mime) }

    val history get() = tryOr(JSONArray()) { JSONFile(settings).getJSONArray("history") }
    val favorites get() = tryOr(JSONArray()) { JSONFile(settings).getJSONArray("favoris") }
    val premium get() = tryOr(false) { JSONFile(settings).getBoolean("premium") }

    fun isFavorite(info: StickerInfo) = favorites.toList().any { StickerInfo(it).url == info.url }

    fun favorite(info: StickerInfo, remove: Boolean) = json(settings) {
        val favorites = tryOr(mutableListOf()) { getJSONArray("favoris").toList() }
        if (!remove) favorites.add(info.json)
        else favorites.removeIf { StickerInfo(it).url == info.url }
        put("favoris", JSONArray(favorites))
    }

    fun push(info: StickerInfo) = json(settings) {
        val history = tryOr(mutableListOf()) { getJSONArray("history").toList() }
        history.removeIf { StickerInfo(it).url == info.url }
        history.add(info.json)
        put("history", JSONArray(history.takeLast(30)))
    }

    fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    fun send(info: StickerInfo) = GlobalScope.launch(Main) {
        push(info)

        val extension = info.url.split(".").last()
        val mime = when (extension) {
            "gif" -> "image/gif"
            "png" -> "image/png"
            "jpg" -> "image/jpeg"
            "jpeg" -> "image/jpeg"
            else -> return@launch
        }

        if (!supports(mime)) {
            currentInputConnection.commitText(" ${info.url}", 1)
            return@launch
        }

        val tmp = File(getExternalFilesDir(null), "sticker.$extension")

        withContext(IO) {
            tmp.delete()

            val file = Glide
                .with(this@Keyboard).asFile().load(info.url)
                .diskCacheStrategy(DATA).submit().get()

            file.copyTo(tmp, true);
        }

        if (!tmp.exists()) return@launch
        val uri = FileProvider.getUriForFile(this@Keyboard, "$packageName.fileprovider", tmp)
        val description = ClipDescription("Sticker", arrayOf(mime))
        val content = InputContentInfo(uri, description)
        val flag = InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        currentInputConnection.commitContent(content, flag, null)
    }
}