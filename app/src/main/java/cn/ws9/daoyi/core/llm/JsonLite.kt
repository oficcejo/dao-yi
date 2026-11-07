package cn.ws9.daoyi.core.llm

/**
 * 极简 JSON 解析/序列化，纯 Kotlin、零依赖。
 * Android 端与 JVM 单元测试共用，避免依赖系统 org.json 在不同实现下的行为差异，
 * 也让大模型响应解析可以在本地完整回归测试。
 *
 * 解析结果为原生 Kotlin 类型：Map<String, Any?> / List<Any?> / String / Double / Boolean / null。
 */
object JsonLite {

    fun parse(text: String): Any? {
        val p = Parser(text)
        val value = p.value()
        p.ws()
        if (!p.end()) throw IllegalArgumentException("JSON 结尾存在多余字符（位置 ${p.pos()}）")
        return value
    }

    fun stringify(value: Any?): String = when (value) {
        null -> "null"
        is String -> quote(value)
        is Boolean -> value.toString()
        is Number -> value.toString()
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (k, v) ->
            quote(k.toString()) + ":" + stringify(v)
        }
        is Iterable<*> -> value.joinToString(",", "[", "]") { stringify(it) }
        else -> quote(value.toString())
    }

    @Suppress("UNCHECKED_CAST")
    fun asMap(v: Any?): Map<String, Any?> =
        (v as? Map<String, Any?>) ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    fun asList(v: Any?): List<Any?> =
        when (v) {
            is List<*> -> v as List<Any?>
            null -> emptyList()
            else -> listOf(v)
        }

    /** 把任意值转成可读文本：对象只取常见文本键，绝不吐出整段 JSON 字符串。 */
    fun text(v: Any?): String = when (v) {
        null -> ""
        is String -> v.trim()
        is Number, is Boolean -> v.toString()
        is Map<*, *> -> {
            val m = asMap(v)
            listOf("name", "title", "text", "value", "content", "名称", "药名", "说明")
                .firstNotNullOfOrNull { text(m[it]).takeIf { s -> s.isNotBlank() } }.orEmpty()
        }
        is Iterable<*> -> v.mapNotNull { text(it).takeIf { s -> s.isNotBlank() } }
            .joinToString("；").take(500)
        else -> ""
    }

    fun number(v: Any?): Double? = when (v) {
        is Number -> v.toDouble()
        is String -> Regex("-?\\d+(\\.\\d+)?").find(v)?.value?.toDoubleOrNull()
        else -> null
    }

    /** 从对象里按多个别名（含中文键）取值，兼容大小写差异。 */
    fun pick(obj: Map<String, Any?>, vararg aliases: String): Any? {
        for (a in aliases) {
            obj[a]?.let { return it }
        }
        val lower = obj.entries.associateBy { it.key.lowercase() }
        for (a in aliases) {
            lower[a.lowercase()]?.value?.let { return it }
        }
        return null
    }

    /** 从任意内容里截出最外层 JSON 对象，容忍 Markdown 代码块与前后解释文字。 */
    fun extractObject(content: String): Map<String, Any?> {
        var s = content.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
                .removeSuffix("```").trim()
        }
        val a = s.indexOf('{')
        val b = s.lastIndexOf('}')
        if (a < 0 || b <= a) {
            throw IllegalArgumentException("模型未返回 JSON 对象（返回内容前 60 字：${s.take(60)}）")
        }
        return asMap(parse(s.substring(a, b + 1)))
    }

    private fun quote(s: String): String {
        val sb = StringBuilder("\"")
        s.forEach { c ->
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.append('"').toString()
    }

    private class Parser(private val s: String) {
        private var i = 0
        fun pos(): Int = i
        fun end(): Boolean = i >= s.length
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun value(): Any? {
            ws()
            if (end()) throw err("内容为空")
            return when (s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> { word("true"); true }
                'f' -> { word("false"); false }
                'n' -> { word("null"); null }
                else -> num()
            }
        }

        private fun obj(): Map<String, Any?> {
            i++ // {
            val m = LinkedHashMap<String, Any?>()
            ws()
            if (!end() && s[i] == '}') { i++; return m }
            while (true) {
                ws()
                if (end() || s[i] != '"') throw err("对象键应为字符串")
                val k = str()
                ws()
                if (end() || s[i] != ':') throw err("对象缺少冒号")
                i++
                m[k] = value()
                ws()
                if (end()) throw err("对象未闭合")
                when (s[i]) {
                    ',' -> i++
                    '}' -> { i++; return m }
                    else -> throw err("对象内应为逗号或右括号")
                }
            }
        }

        private fun arr(): List<Any?> {
            i++ // [
            val out = mutableListOf<Any?>()
            ws()
            if (!end() && s[i] == ']') { i++; return out }
            while (true) {
                out += value()
                ws()
                if (end()) throw err("数组未闭合")
                when (s[i]) {
                    ',' -> i++
                    ']' -> { i++; return out }
                    else -> throw err("数组内应为逗号或右方括号")
                }
            }
        }

        private fun str(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (i >= s.length) throw err("转义不完整")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw err("Unicode 转义不完整")
                                val code = s.substring(i, i + 4).toIntOrNull(16) ?: throw err("非法 Unicode 转义")
                                sb.append(code.toChar())
                                i += 4
                            }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw err("字符串未闭合")
        }

        private fun num(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(start, i)
            return t.toDoubleOrNull() ?: throw err("非法数字 $t")
        }

        private fun word(w: String) {
            if (!s.startsWith(w, i)) throw err("期望 $w")
            i += w.length
        }

        private fun err(msg: String): IllegalArgumentException =
            IllegalArgumentException("$msg（位置 $i）")
    }
}
