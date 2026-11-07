package cn.ws9.daoyi.core.safety

import cn.ws9.daoyi.core.llm.HerbDose
import cn.ws9.daoyi.core.llm.LlmPrescription
import cn.ws9.daoyi.core.tcm.Extraction
import cn.ws9.daoyi.core.tcm.KnowledgeBase
import cn.ws9.daoyi.core.tcm.PersonProfile
import kotlin.math.roundToInt

data class SafetyReport(
    val blocked: Boolean,
    val adjusted: List<HerbDose>,
    val warnings: List<String>,
    val refusal: String?
)

/** 硬规则闸门：模型输出之后、展示之前，任何越界都拦截或校正。 */
object SafetyGate {

    const val DISCLAIMER =
        "自用研究工具 · 典籍整理，未经医师审校 · 非诊断、非处方、不构成医疗建议 · 请勿自行抓药，身体不适请线下就医"

    fun checkBefore(extraction: Extraction, profile: PersonProfile): String? {
        if (extraction.redFlags.isNotEmpty()) {
            return "检出红旗征象（${extraction.redFlags.joinToString("、")}）。本工具不提供任何方剂建议，请立即前往线下医疗机构就诊。"
        }
        if (profile.age < 3) return "婴幼儿用药风险极高，本工具不提供方剂建议，请就医。"
        return null
    }

    fun verify(prescription: LlmPrescription, profile: PersonProfile): SafetyReport {
        val warnings = mutableListOf<String>()
        var blocked = false
        var refusal: String? = null

        val adjusted = prescription.herbs.map { dose ->
            val herb = KnowledgeBase.herb(dose.name)
            if (herb == null) {
                warnings += "「${dose.name}」未收录于药典剂量表，无法校验克数，需人工复核。"
                return@map dose
            }
            var g = dose.grams
            if (g <= 0.0) {
                g = herb.minGram.toDouble()
                warnings += "「${dose.name}」剂量缺失，已按药典下限 ${herb.minGram}g 填充。"
            }
            if (g > herb.maxGram) {
                warnings += "「${dose.name}」原 ${round1(g)}g 超过药典常用上限 ${herb.maxGram}g，已校正为 ${herb.maxGram}g。"
                g = herb.maxGram.toDouble()
            }
            if (g < herb.minGram) {
                warnings += "「${dose.name}」${round1(g)}g 低于常用下限 ${herb.minGram}g，效力可能不足，需复核。"
            }
            if (herb.toxic) {
                warnings += "「${dose.name}」属有毒/有小毒药材：${herb.decoctNote.ifBlank { "须炮制并严格控量" }}。"
            }
            if (herb.decoctNote.isNotBlank() && !herb.toxic) {
                warnings += "「${dose.name}」：${herb.decoctNote}。"
            }
            dose.copy(grams = g)
        }

        // 十八反 / 十九畏
        for (i in adjusted.indices) {
            for (j in i + 1 until adjusted.size) {
                val a = adjusted[i].name
                val b = adjusted[j].name
                val hit = KnowledgeBase.incompatiblePairs.any { (x, y) ->
                    (a.contains(x) && b.contains(y)) || (a.contains(y) && b.contains(x))
                }
                if (hit) {
                    blocked = true
                    warnings += "配伍禁忌：「$a」与「$b」属十八反/十九畏，禁止同用。"
                }
            }
        }

        // 特殊人群
        if (profile.pregnant) {
            val hard = adjusted.map { it.name }.filter { n ->
                KnowledgeBase.pregnancyForbidden.any { n.contains(it) }
            }
            if (hard.isNotEmpty()) {
                blocked = true
                warnings += "妊娠禁用：「${hard.joinToString("、")}」。"
            }
            val soft = adjusted.map { it.name }.filter { n ->
                KnowledgeBase.herb(n)?.pregnancyAvoid == true && n !in hard
            }
            if (soft.isNotEmpty()) warnings += "妊娠慎用：「${soft.joinToString("、")}」，须医师评估。"
        }
        if (profile.age < 12) {
            val c = adjusted.map { it.name }.filter { n -> KnowledgeBase.childCaution.any { n.contains(it) } }
            if (c.isNotEmpty()) warnings += "儿童慎用：「${c.joinToString("、")}」，剂量须按体重另算。"
        }
        if (profile.age > 70) warnings += "高龄人群肝肾功能下降，建议从药典下限起，由医师调整。"
        if (profile.liverKidneyIssue) warnings += "已有肝肾疾患，含附子、细辛、麻黄等方须由医师评估，不可自用。"
        if (profile.allergy.isNotBlank()) warnings += "过敏史：${profile.allergy}，须逐味核对药材。"

        warnings += "本方剂克数为《中国药典》常用日用量区间的现代参考量，非仲景原方折算量；古今剂量折算存在 15.625g/两等争议。"
        warnings += DISCLAIMER

        if (blocked) {
            refusal = "安全闸门拦截：本次推演结果存在配伍禁忌或妊娠禁用药，已停止展示方剂。请线下就医，勿按此抓药。"
        }
        return SafetyReport(blocked, adjusted, warnings.distinct(), refusal)
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
