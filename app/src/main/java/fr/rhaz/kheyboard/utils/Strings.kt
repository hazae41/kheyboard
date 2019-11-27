package fr.rhaz.kheyboard.utils

fun String.dropLastWord(): String {
    return split(" ").dropLast(1).joinToString(" ")
}