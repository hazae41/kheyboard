package fr.rhaz.kheyboard.utils

import android.content.ClipboardManager
import android.content.Context
import android.os.Vibrator
import android.view.inputmethod.InputMethodManager

val Context.inputMethodManager get() = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
val Context.vibrator get() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
val Context.clipboardManager get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager