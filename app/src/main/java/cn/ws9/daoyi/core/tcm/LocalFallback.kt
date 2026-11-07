package cn.ws9.daoyi.core.tcm

/** 未配置 API Key 或模型失败时，退化为纯本地典籍参考卡（不生成新方） */
object LocalFallback {

    private val MAP = mapOf(
        "S01" to "桂枝汤", "S02" to "麻黄汤", "S03" to "五苓散", "S04" to "小柴胡汤",
        "S05" to "白虎汤", "S06" to "大承气汤", "S07" to "理中丸", "S08" to "四逆汤",
        "S09" to "黄连阿胶汤", "S10" to "乌梅丸", "S11" to "苓桂术甘汤", "S12" to "真武汤",
        "S13" to "小青龙汤", "S14" to "麻杏石甘汤", "S15" to "半夏泻心汤", "S16" to "小建中汤",
        "S17" to "当归芍药散", "S18" to "炙甘草汤", "S19" to "百合地黄汤", "S20" to "茵陈蒿汤",
        "S21" to "桂枝茯苓丸", "S22" to "半夏厚朴汤"
    )

    fun pick(s: Syndrome): Formula {
        val name = MAP[s.id] ?: "桂枝汤"
        return KnowledgeBase.formulas.firstOrNull { it.name == name } ?: KnowledgeBase.formulas.first()
    }

    fun patentFor(s: Syndrome): PatentMedicine =
        KnowledgeBase.patents.firstOrNull { it.syndrome.contains(s.liuJing) || s.name.contains(it.syndrome.take(2)) }
            ?: KnowledgeBase.patents.first()
}
