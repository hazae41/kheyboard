package fr.rhaz.kheyboard

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import com.bumptech.glide.Glide
import fr.rhaz.kheyboard.R.layout.*
import kotlinx.android.synthetic.main.activation.*
import kotlinx.android.synthetic.main.activity.*
import kotlinx.android.synthetic.main.selection.*
import kotlinx.android.synthetic.main.test.*
import org.jetbrains.anko.toast

val Context.ctx get() = this

class Main : AppCompatActivity() {

    val adapter get() = object: FragmentStatePagerAdapter(supportFragmentManager) {
        override fun getCount() = 3
        override fun getItem(id: Int) = when(id){
            0 -> Activation()
            1 -> Selection()
            2 -> Testing()
            else -> null!!
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(activity)
        pager.adapter = adapter
    }

    override fun onBackPressed() {
        if (pager.currentItem != 0)
            pager.currentItem--
        else super.onBackPressed()
    }
}

class Activation: Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ) = inflater.inflate(activation, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        activate.setOnClickListener {
            startActivity(Intent(ACTION_INPUT_METHOD_SETTINGS))
        }
    }
}

class Selection: Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ) = inflater.inflate(selection, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        val ctx = activity!!.ctx
        select.setOnClickListener {
            val man = ctx.applicationContext.getSystemService(INPUT_METHOD_SERVICE)
            (man as InputMethodManager).showInputMethodPicker()
        }
    }
}

class Testing: Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ) = inflater.inflate(test, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        val ctx = activity!!.ctx
        input.requestFocus()
        testbtn.setOnClickListener {
            val url = input.text.toString()
            input.text.clear()
            if(!URLUtil.isValidUrl(url))
                 ctx.toast("URL invalide")
            else Glide.with(ctx).load(url).into(sticker)
        }
    }
}