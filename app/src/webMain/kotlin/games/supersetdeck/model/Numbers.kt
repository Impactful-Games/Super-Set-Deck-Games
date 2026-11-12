package games.supersetdeck.model

/**
 * The compact form of a list of integers: sorted, every run of at least three consecutive integers
 * becomes `first-last`, the other values stay alone. `[2, 3, 4, 7, 8, 9, 10]` → `["2-4", "7-10"]`.
 */
fun Iterable<Int>.abbreviated(): List<String> {
    val sorted = toSet().sorted()
    val out = ArrayList<String>()
    var i = 0
    while (i < sorted.size) {
        var j = i
        while (j + 1 < sorted.size && sorted[j + 1] == sorted[j] + 1) ++j
        if (j - i >= 2) out += "${sorted[i]}-${sorted[j]}"
        else (i..j).forEach { out += sorted[it].toString() }
        i = j + 1
    }
    return out
}

/** `[3, 4, 6]` → `3, 4, 6`; `[2, 3, 4, 5, 6]` → `2-6`. */
fun Iterable<Int>.abbreviatedText(): String = abbreviated().joinToString(", ")
