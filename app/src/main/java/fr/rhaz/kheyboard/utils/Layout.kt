package fr.rhaz.kheyboard.utils

import android.view.LayoutInflater
import android.view.View

fun LayoutInflater.inflate(id: Int, builder: View.() -> Unit = {}) =
        inflate(id, null).apply(builder)