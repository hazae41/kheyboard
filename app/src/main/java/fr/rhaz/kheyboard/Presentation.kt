package fr.rhaz.kheyboard

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.synthetic.main.activate.*
import kotlinx.android.synthetic.main.presentation.*
import kotlinx.android.synthetic.main.select.*

class Presentation : AppCompatActivity() {

    val methods get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabled get() = methods.enabledInputMethodList.any { it.packageName == packageName }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presentation)
        ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 1);
        pager.adapter = Pages()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (results.first() != PERMISSION_GRANTED) finish()
    }

    inner class Pages : FragmentStateAdapter(this) {
        val pages = listOf(Activate(), Select(this@Presentation))
        override fun getItemCount() = pages.size
        override fun createFragment(position: Int) = pages[position]
    }

    override fun onResume() {
        super.onResume()

        if (pager.currentItem == 0) {
            if (enabled) pager.setCurrentItem(1, true)
        }
    }
}

class Activate : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ) = inflater.inflate(R.layout.activate, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        activateButton.setOnClickListener { activate() }
    }

    fun activate() = startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
}

class Select(val app: Presentation) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.select, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        selectButton.setOnClickListener { select() }
    }

    fun select() = app.methods.showInputMethodPicker()
}