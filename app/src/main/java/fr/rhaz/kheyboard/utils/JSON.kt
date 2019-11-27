package fr.rhaz.kheyboard.utils

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.array(name: String) = try {
    getJSONArray(name)
} catch (ex: Exception) {
    JSONArray()
}