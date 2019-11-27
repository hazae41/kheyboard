package fr.rhaz.kheyboard.utils

import android.content.Context
import android.widget.Toast

fun Context.toast(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
fun Context.longToast(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()