/**
 * Minimal, deterministic JSON writer for the data files generated at build time.
 *
 * Handles maps (keys in iteration order), iterables, strings, numbers, booleans and null.
 */
internal fun toJson(value: Any?, indent: String = ""): String = buildString { appendJson(value, indent) }

private fun StringBuilder.appendJson(value: Any?, indent: String) {
    val next = "$indent  "
    when (value) {
        null -> append("null")
        is String -> appendJsonString(value)
        is Number, is Boolean -> append(value.toString())
        is Map<*, *> -> {
            if (value.isEmpty()) {
                append("{}")
                return
            }
            append("{\n")
            value.entries.forEachIndexed { index, (k, v) ->
                append(next)
                appendJsonString(k.toString())
                append(": ")
                appendJson(v, next)
                if (index < value.size - 1) append(",")
                append("\n")
            }
            append(indent).append("}")
        }
        is Iterable<*> -> {
            val list = value.toList()
            if (list.isEmpty()) {
                append("[]")
                return
            }
            append("[\n")
            list.forEachIndexed { index, v ->
                append(next)
                appendJson(v, next)
                if (index < list.size - 1) append(",")
                append("\n")
            }
            append(indent).append("]")
        }
        else -> error("Cannot serialize ${value::class} to JSON")
    }
}

private fun StringBuilder.appendJsonString(value: String) {
    append('"')
    value.forEach { c ->
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }
    append('"')
}
