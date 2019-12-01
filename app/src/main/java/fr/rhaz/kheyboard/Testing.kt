package fr.rhaz.kheyboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import fr.rhaz.kheyboard.utils.toast
import kotlinx.android.synthetic.main.app_test.*

class Testing(val main: Main) : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            state: Bundle?
    ) = inflater.inflate(R.layout.app_test, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)

        testbtn.setOnClickListener {
            val url = input.text.toString()
            input.text.clear()
            if (!URLUtil.isValidUrl(url))
                activity!!.toast("URL invalide")
            else Glide.with(activity!!).load(url).dontTransform().into(sticker)
        }

        input.requestFocus()
    }
}