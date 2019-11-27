package fr.rhaz.kheyboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import fr.rhaz.kheyboard.utils.inputMethodManager
import kotlinx.android.synthetic.main.app.*
import kotlinx.android.synthetic.main.app_selection.*

class Selection(val main: Main) : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            state: Bundle?
    ) = inflater.inflate(R.layout.app_selection, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        
        select.setOnClickListener {
            activity!!.inputMethodManager.showInputMethodPicker()
        }
        nextbtn.setOnClickListener {
            main.pager.currentItem++
        }
    }
}
