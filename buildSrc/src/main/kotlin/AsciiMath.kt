/**
 * Converts the AsciiMath subset used by the rules (`stem:[...]`) to MathML Core, which every current
 * browser renders natively, offline, without any script or font download.
 *
 * Supported: numbers, identifiers, quoted text, `+ - * xx x / = ( ) [ ] _ ^ ...` and any other
 * single character as an operator. `/` builds a fraction and `_` / `^` build sub- and superscripts,
 * dropping the brackets around their operands as AsciiMath does.
 */
internal object AsciiMath {

    private sealed interface Node {
        fun toMathML(): String
    }

    private data class Leaf(val tag: String, val text: String) : Node {
        override fun toMathML() = "<$tag>${text.escapeXml()}</$tag>"
    }

    private data class Row(val children: List<Node>, val open: String? = null, val close: String? = null) : Node {
        override fun toMathML() = buildString {
            append("<mrow>")
            open?.let { append("<mo>${it.escapeXml()}</mo>") }
            children.forEach { append(it.toMathML()) }
            close?.let { append("<mo>${it.escapeXml()}</mo>") }
            append("</mrow>")
        }

        /** The operand of a fraction or script loses its brackets. */
        fun unbracketed() = copy(open = null, close = null)
    }

    private data class Script(val tag: String, val base: Node, val script: Node) : Node {
        override fun toMathML() = "<$tag>${base.toMathML()}${script.toMathML()}</$tag>"
    }

    private data class Fraction(val numerator: Node, val denominator: Node) : Node {
        override fun toMathML() = "<mfrac>${numerator.toMathML()}${denominator.toMathML()}</mfrac>"
    }

    private val operators = mapOf(
        "..." to "…",
        "xx" to "×",
        "**" to "∗",
        "*" to "⋅",
        "-:" to "÷",
        "+-" to "±",
        "!=" to "≠",
        "<=" to "≤",
        ">=" to "≥",
        "->" to "→",
        "-" to "−",
    )

    private class Parser(private val input: String) {
        private var pos = 0

        private fun skipSpaces() {
            while (pos < input.length && input[pos].isWhitespace()) ++pos
        }

        private fun peek(): Char? {
            skipSpaces()
            return input.getOrNull(pos)
        }

        fun parseExpression(closing: Set<Char> = emptySet()): List<Node> {
            val nodes = ArrayList<Node>()
            while (true) {
                val c = peek() ?: break
                if (c in closing) break
                nodes += parseIntermediate()
            }
            return nodes
        }

        private fun parseIntermediate(): Node {
            var node = parseScripted()
            if (peek() == '/') {
                ++pos
                val denominator = parseScripted()
                node = Fraction(node.unbracket(), denominator.unbracket())
            }
            return node
        }

        private fun parseScripted(): Node {
            val base = parseSimple()
            return when (peek()) {
                '_' -> {
                    ++pos
                    val sub = parseSimple().unbracket()
                    if (peek() == '^') {
                        ++pos
                        val sup = parseSimple().unbracket()
                        SubSup(base, sub, sup)
                    } else Script("msub", base, sub)
                }
                '^' -> {
                    ++pos
                    Script("msup", base, parseSimple().unbracket())
                }
                else -> base
            }
        }

        private fun parseSimple(): Node {
            skipSpaces()
            val c = input[pos]
            return when {
                c == '"' -> {
                    val end = input.indexOf('"', pos + 1).takeIf { it >= 0 } ?: input.length
                    val text = input.substring(pos + 1, end)
                    pos = minOf(end + 1, input.length)
                    Leaf("mtext", text)
                }
                c == '(' || c == '[' || c == '{' -> {
                    val close = when (c) { '(' -> ')'; '[' -> ']'; else -> '}' }
                    ++pos
                    val children = parseExpression(setOf(close))
                    if (peek() == close) ++pos
                    Row(children, c.toString(), close.toString())
                }
                c.isDigit() || (c == '.' && input.getOrNull(pos + 1)?.isDigit() == true) -> {
                    val start = pos
                    while (pos < input.length && (input[pos].isDigit() || (input[pos] == '.' && input.getOrNull(pos + 1)?.isDigit() == true))) ++pos
                    Leaf("mn", input.substring(start, pos))
                }
                else -> {
                    operators.entries.firstOrNull { input.startsWith(it.key, pos) }?.let { (key, symbol) ->
                        pos += key.length
                        return Leaf("mo", symbol)
                    }
                    if (c.isLetter()) {
                        val start = pos
                        while (pos < input.length && input[pos].isLetter()) ++pos
                        val word = input.substring(start, pos)
                        // A lone "x" between quantities is used as the multiplication sign in the rules.
                        if (word == "x") Leaf("mo", "×") else Leaf("mi", word)
                    } else {
                        ++pos
                        Leaf("mo", c.toString())
                    }
                }
            }
        }

        private fun Node.unbracket(): Node = if (this is Row) unbracketed() else this
    }

    private data class SubSup(val base: Node, val sub: Node, val sup: Node) : Node {
        override fun toMathML() = "<msubsup>${base.toMathML()}${sub.toMathML()}${sup.toMathML()}</msubsup>"
    }

    fun toMathML(asciiMath: String): String {
        val nodes = Parser(asciiMath).parseExpression()
        return "<math display=\"inline\"><mrow>${nodes.joinToString("") { it.toMathML() }}</mrow></math>"
    }

    private fun String.escapeXml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
