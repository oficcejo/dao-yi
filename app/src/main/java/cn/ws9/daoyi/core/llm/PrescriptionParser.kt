package cn.ws9.daoyi.core.llm

/**
 * 大模型响应归一化解析。
 *
 * 现实中模型常返回：中文字段名、```json 代码块、把整个处方包在 result/data 里、
 * 药物写成字符串数组「桂枝 9g」、中成药写成对象数组。旧实现只认固定英文键与对象数组，
 * 会把对象转成一段 JSON 文本或直接丢空字段，界面上就表现为「输出很乱 / 空白卡片」。
 * 这里统一归一化到 LlmPrescription，并对明显无效的结果直接报错，交给上层降级。
 */
object PrescriptionParser {

    private val ALIASES = mapOf(
        "syndrome" to arrayOf("syndrome", "证型", "辨证", "证候"),
        "therapy" to arrayOf("therapy", "治法", "治则"),
        "formulaName" to arrayOf("formulaName", "formula", "方名", "方剂", "方剂名", "处方名", "方"),
        "source" to arrayOf("source", "出处", "出处条文", "条文", "来源"),
        "herbs" to arrayOf("herbs", "药物", "方药", "组成", "药味", "composition"),
        "decoction" to arrayOf("decoction", "煎服法", "煎服", "服法", "古今剂量"),
        "patentMedicines" to arrayOf("patentMedicines", "中成药", "成药"),
        "diet" to arrayOf("diet", "饮食", "饮食建议"),
        "lifestyle" to arrayOf("lifestyle", "作息", "起居"),
        "exercise" to arrayOf("exercise", "导引", "运动"),
        "emotional" to arrayOf("emotional", "情志"),
        "seasonal" to arrayOf("seasonal", "节气"),
        "references" to arrayOf("references", "条文依据", "依据", "典籍依据"),
        "cautions" to arrayOf("cautions", "禁忌", "禁忌与就医提示", "注意事项"),
        "confidence" to arrayOf("confidence", "置信度")
    )

    private val HERB_ALIASES = mapOf(
        "name" to arrayOf("name", "药名", "药材", "药"),
        "grams" to arrayOf("grams", "克数", "剂量", "用量", "dose", "g"),
        "processing" to arrayOf("processing", "炮制", "制法"),
        "note" to arrayOf("note", "剂量依据", "备注", "说明")
    )

    private val WRAPPERS = arrayOf("result", "data", "prescription", "处方", "结果")

    fun parse(content: String): LlmPrescription {
        var root = JsonLite.extractObject(content)
        // 有些服务把处方包一层；没有关键字段时下钻，最多 3 层
        var depth = 0
        while (depth < 3) {
            val hasKey = ALIASES.values.any { aliases -> JsonLite.pick(root, *aliases) != null }
            if (hasKey) break
            val nested = WRAPPERS.firstNotNullOfOrNull { w ->
                JsonLite.asMap(root[w]).takeIf { it.isNotEmpty() }
            } ?: break
            root = nested
            depth++
        }

        val herbs = parseHerbs(JsonLite.pick(root, *ALIASES.getValue("herbs")))
        val formulaName = str(root, "formulaName")

        if (formulaName.isBlank() && herbs.isEmpty()) {
            throw IllegalArgumentException(
                "模型返回缺少可识别的方名与药味（字段可能因语言或结构差异未被识别）"
            )
        }

        return LlmPrescription(
            syndrome = str(root, "syndrome"),
            therapy = str(root, "therapy"),
            formulaName = formulaName.ifBlank { "参考方" }.take(40),
            source = str(root, "source"),
            herbs = herbs,
            decoction = str(root, "decoction"),
            patentMedicines = list(root, "patentMedicines"),
            diet = list(root, "diet"),
            lifestyle = list(root, "lifestyle"),
            exercise = str(root, "exercise"),
            emotional = str(root, "emotional"),
            seasonal = str(root, "seasonal"),
            references = list(root, "references"),
            cautions = list(root, "cautions"),
            confidence = str(root, "confidence", limit = 20)
        )
    }

    private fun str(root: Map<String, Any?>, field: String, limit: Int = 600): String =
        JsonLite.text(JsonLite.pick(root, *ALIASES.getValue(field)))
            .replace(Regex("\\s+"), " ").trim().take(limit)

    private fun list(root: Map<String, Any?>, field: String): List<String> {
        val v = JsonLite.pick(root, *ALIASES.getValue(field)) ?: return emptyList()
        val parts: List<Any?> = when (v) {
            is String -> if (v.contains(Regex("[；;\\n、]"))) v.split(Regex("[；;\\n、]")) else listOf(v)
            else -> JsonLite.asList(v)
        }
        return parts.map { JsonLite.text(it) }
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() && !it.startsWith("{") && it != "null" }
            .map { it.take(300) }
            .distinct()
            .take(12)
    }

    private fun parseHerbs(raw: Any?): List<HerbDose> {
        if (raw == null) return emptyList()
        val parts: List<Any?> = when (raw) {
            is String -> raw.split(Regex("[、；;\\n]"))
            else -> JsonLite.asList(raw)
        }
        return parts.flatMap { item ->
            when (item) {
                is Map<*, *> -> {
                    val m = JsonLite.asMap(item)
                    val name = JsonLite.text(JsonLite.pick(m, *HERB_ALIASES.getValue("name")))
                        .replace(Regex("\\s+"), " ").trim().take(20)
                    if (name.isBlank()) emptyList()
                    else listOf(
                        HerbDose(
                            name = name,
                            grams = JsonLite.number(JsonLite.pick(m, *HERB_ALIASES.getValue("grams"))) ?: 6.0,
                            processing = JsonLite.text(
                                JsonLite.pick(m, *HERB_ALIASES.getValue("processing"))
                            ).take(120),
                            note = JsonLite.text(JsonLite.pick(m, *HERB_ALIASES.getValue("note"))).take(160)
                        )
                    )
                }
                else -> parseHerbString(JsonLite.text(item))?.let { listOf(it) } ?: emptyList()
            }
        }.take(24)
    }

    /** 「桂枝 9g」「白芍：9克」「炙甘草」等自由文本 → 结构化药味。 */
    private fun parseHerbString(raw: String): HerbDose? {
        val s = raw.trim().removePrefix("·").removePrefix("-").trim()
        if (s.isBlank() || s.startsWith("{")) return null
        val m = Regex("\\d+(?:\\.\\d+)?\\s*(?:g|G|克|grams?)?").find(s)
        val grams = m?.let { JsonLite.number(it.value) } ?: 6.0
        val name = (if (m != null) s.substring(0, m.range.first) else s)
            .trim().trimEnd('：', ':', ' ', '、', ',', '，').trim().take(20)
        if (name.isBlank()) return null
        return HerbDose(name = name, grams = grams, processing = "", note = "由文本自动解析，需复核")
    }
}
