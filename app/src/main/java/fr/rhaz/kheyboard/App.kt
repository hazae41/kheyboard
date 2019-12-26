package fr.rhaz.kheyboard

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.fragment.app.FragmentStatePagerAdapter
import com.gelitenight.waveview.library.WaveView
import kotlinx.android.synthetic.main.app.*

class Main : AppCompatActivity() {

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.app)
        pager.adapter = Pages()

        waveView.apply {
            isShowWave = true
            setShapeType(WaveView.ShapeType.SQUARE)
            this.setWaveColor(resources.getColor(R.color.light_dark), resources.getColor(R.color.darker))
        }

        val waveShiftAnim = ObjectAnimator.ofFloat(waveView, "waveShiftRatio", 0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            duration = 1000
            interpolator = LinearInterpolator()
        }

        val amplitudeAnim = ObjectAnimator.ofFloat(waveView, "amplitudeRatio", 0.001f, 0.020f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1000
            interpolator = LinearInterpolator()
        }

        AnimatorSet().apply {
            playTogether(waveShiftAnim, amplitudeAnim)
            start()
        }
    }

    inner class Pages : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        val pages = arrayOf(Activation(this@Main), Selection(this@Main), Testing(this@Main))
        override fun getCount() = pages.size
        override fun getItem(id: Int) = pages[id]
    }

    override fun onBackPressed() {
        if (pager.currentItem == 0)
            super.onBackPressed()
        pager.currentItem--
    }

    override fun onCreateOptionsMenu(menu: Menu) = true.also {
        menuInflater.inflate(R.menu.menu, menu)
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_rate -> {
                val uri = Uri.parse("market://details?id=$packageName")
                val intent = Intent(ACTION_VIEW, uri)
                intent.addFlags(FLAG_ACTIVITY_NO_HISTORY or FLAG_ACTIVITY_MULTIPLE_TASK)
                startActivity(intent)
            }
            R.id.action_donate -> {
                startActivity(Intent(this, Billing::class.java))
            }
            R.id.action_options -> {
                Config(this).openSettings()
            }
            else -> return false
        }
        return true
    }
}