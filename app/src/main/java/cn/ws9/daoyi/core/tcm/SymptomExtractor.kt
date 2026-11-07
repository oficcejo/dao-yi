package cn.ws9.daoyi.core.tcm

/** 症状词表 → 结构化证据。组内证据在贝叶斯层合并，避免重复计数。 */
object SymptomExtractor {

    private val DICT: List<Triple<String, String, Double>> = listOf(
        Triple("恶寒", "主症", 1.0), Triple("畏寒", "主症", 1.0), Triple("发热", "主症", 1.0),
        Triple("无汗", "主症", 1.0), Triple("汗出", "主症", 1.0), Triple("自汗", "主症", 1.0),
        Triple("盗汗", "主症", 1.0), Triple("头痛", "主症", 0.9), Triple("身痛", "主症", 0.9),
        Triple("项强", "主症", 0.9), Triple("口渴", "主症", 0.9), Triple("口苦", "主症", 0.9),
        Triple("咽干", "主症", 0.9), Triple("目眩", "主症", 0.9), Triple("胸胁苦满", "主症", 1.0),
        Triple("往来寒热", "主症", 1.0), Triple("心烦", "主症", 0.8), Triple("呕吐", "主症", 0.9),
        Triple("咳嗽", "主症", 0.9), Triple("气喘", "主症", 0.9), Triple("痰多", "主症", 0.8),
        Triple("腹胀", "主症", 0.9), Triple("腹痛", "主症", 0.9), Triple("腹泻", "主症", 0.9),
        Triple("便溏", "主症", 0.9), Triple("便秘", "主症", 0.9), Triple("食欲不振", "主症", 0.8),
        Triple("小便不利", "主症", 0.9), Triple("水肿", "主症", 0.9), Triple("四肢厥冷", "主症", 1.0),
        Triple("乏力", "主症", 0.8), Triple("嗜睡", "主症", 0.8), Triple("失眠", "主症", 0.8),
        Triple("心悸", "主症", 0.8), Triple("眩晕", "主症", 0.8), Triple("月经不调", "主症", 0.8),
        Triple("舌淡", "舌脉", 0.8), Triple("舌红", "舌脉", 0.8), Triple("舌暗", "舌脉", 0.8),
        Triple("苔薄白", "舌脉", 0.8), Triple("苔黄", "舌脉", 0.8), Triple("苔腻", "舌脉", 0.8),
        Triple("脉浮", "舌脉", 0.8), Triple("脉紧", "舌脉", 0.8), Triple("脉缓", "舌脉", 0.8),
        Triple("脉弦", "舌脉", 0.8), Triple("脉沉", "舌脉", 0.8), Triple("脉细", "舌脉", 0.8),
        Triple("脉数", "舌脉", 0.8), Triple("脉迟", "舌脉", 0.8), Triple("脉滑", "舌脉", 0.8),
        Triple("喜温", "兼症", 0.6), Triple("喜冷", "兼症", 0.6), Triple("面色苍白", "兼症", 0.6),
        Triple("面红", "兼症", 0.6), Triple("口干不欲饮", "兼症", 0.6), Triple("神疲", "兼症", 0.6),
        Triple("烦躁", "兼症", 0.6), Triple("情志抑郁", "兼症", 0.6), Triple("胁痛", "兼症", 0.6),
        Triple("嗳气", "兼症", 0.6), Triple("反酸", "兼症", 0.6), Triple("尿黄", "兼症", 0.6),
        Triple("尿清长", "兼症", 0.6), Triple("五心烦热", "兼症", 0.6), Triple("腰膝酸软", "兼症", 0.6),
        Triple("怕风", "兼症", 0.6), Triple("鼻塞", "兼症", 0.6), Triple("咽痛", "兼症", 0.6)
    )

    /** 命中即拒答，不进入推理与开方 */
    private val RED_FLAGS = listOf(
        "胸痛", "心前区痛", "呼吸困难", "气促", "窒息", "昏迷", "意识不清", "抽搐",
        "偏瘫", "言语不清", "口角歪斜", "高热不退", "持续高热", "剧烈腹痛", "呕血",
        "便血", "黑便", "大出血", "咯血", "喉头水肿", "过敏性休克", "严重外伤", "服毒",
        "自伤", "轻生", "急症", "急性发作", "血压骤升", "心律失常", "胎动不安", "阴道出血"
    )

    fun extract(text: String, selected: List<String> = emptyList()): Extraction {
        val source = (text + " " + selected.joinToString(" ")).trim()
        val hits = mutableListOf<SymptomHit>()
        val seen = mutableSetOf<String>()
        for ((word, group, weight) in DICT) {
            if (source.contains(word) && seen.add(word)) {
                hits += SymptomHit(word, group, weight)
            }
        }
        // 自由文本中未被词表覆盖的片段，作为低权重补充证据
        val extras = text.split(Regex("[，,。;；、\\s]+"))
            .map { it.trim() }
            .filter { it.length in 2..8 && it.none { c -> c.isDigit() } && hits.none { h -> h.text == it } }
            .take(6)
        extras.forEach { hits += SymptomHit(it, "自由描述", 0.35) }

        val flags = RED_FLAGS.filter { source.contains(it) }
        return Extraction(hits, flags, text)
    }

    fun dictionary(): List<String> = DICT.map { it.first }.distinct()
}
