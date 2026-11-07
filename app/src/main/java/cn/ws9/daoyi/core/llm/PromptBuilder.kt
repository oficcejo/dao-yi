package cn.ws9.daoyi.core.llm

import cn.ws9.daoyi.core.bayes.BayesResult
import cn.ws9.daoyi.core.bazi.BaziChart
import cn.ws9.daoyi.core.tcm.Extraction
import cn.ws9.daoyi.core.tcm.KnowledgeBase
import cn.ws9.daoyi.core.tcm.PersonProfile

object PromptBuilder {

    const val SYSTEM = """你是「道医 · 参同契」自用研究工具的典籍推演助手，服务于中医经方学习者。
你必须遵守：
1. 只做文献整理与方证推演，不做诊断、不开具处方、不替代医师。所有输出视为「典籍学习参考」。
2. 优先采用张仲景《伤寒论》《金匮要略》经方；可自由选方（不限于内置方剂库），但须给出方名、出处与方证依据。
3. 每味中药剂量必须精确到克，且落在《中国药典》常用日用量区间内。另在 decoction 中说明汉代原方折算量与现代参考量的区别（汉代一两有 15.625g / 13.75g 等争议，原方多分三服、一煎，现代两煎）。
4. 严禁输出超过药典上限的剂量；含附子、细辛、麻黄、半夏等须注明炮制与先煎久煎要求及十八反十九畏。
5. 中成药须注明对应证型与不适用情形；养生建议按「补不足、损有余」给出，涵盖饮食、作息、导引、情志、节气。
6. 证据不足时须在 cautions 明确写「证据不足，建议线下四诊合参，不宜自行用药」。
7. 只输出 JSON，不要 Markdown 代码块，不要任何解释性前后缀。

输出 JSON 结构（字段名严格一致）：
{"syndrome":"证型","therapy":"治法","formulaName":"方名","source":"出处条文",
 "herbs":[{"name":"药名","grams":9,"processing":"炮制","note":"剂量依据"}],
 "decoction":"煎服法与古今剂量折算说明",
 "patentMedicines":["中成药（证型 / 不适用情形）"],
 "diet":["饮食建议"],"lifestyle":["作息建议"],"exercise":"导引建议",
 "emotional":"情志建议","seasonal":"节气建议",
 "references":["条文依据"],"cautions":["禁忌与就医提示"],"confidence":"高/中/低"}"""

    fun user(
        chart: BaziChart,
        bayes: BayesResult,
        extraction: Extraction,
        profile: PersonProfile
    ): String {
        val sb = StringBuilder()
        sb.appendLine("【一、四诊信息（结构化证据，权重最高）】")
        sb.appendLine("用户主述：${extraction.rawText}")
        sb.appendLine("结构化症候：${extraction.symptoms.joinToString("、") { "${it.text}(${it.group})" }}")
        sb.appendLine("年龄 ${profile.age} 岁，${if (profile.male) "男" else "女"}；" +
                "妊娠=${profile.pregnant}；肝肾疾患=${profile.liverKidneyIssue}；过敏=${profile.allergy.ifBlank { "无" }}")
        sb.appendLine()
        sb.appendLine("【二、贝叶斯证型后验（已由端侧确定性计算，不要重新判断）】")
        bayes.ranked.take(5).forEachIndexed { i, p ->
            sb.appendLine("${i + 1}. ${p.syndrome.name} 后验=${(p.probability * 100).toInt()}% 治法=${p.syndrome.therapy} 依据=${p.syndrome.classic}")
            sb.appendLine("   证据贡献：" + p.contributions.take(3).joinToString("；") {
                "${it.label} ${(it.delta * 100).toInt() / 100.0}"
            })
        }
        sb.appendLine(bayes.explanation)
        sb.appendLine()
        sb.appendLine("【三、本命与时空参考（传统文化维度，低权重，仅用于养生「补不足损有余」）】")
        sb.appendLine("四柱：" + chart.pillars.map { it.name }.joinToString(" "))
        sb.appendLine("日主 ${chart.dayMaster}，${chart.strength.level}（旺衰分 ${chart.strength.score}）；月令节气 ${chart.solarTermName}")
        sb.appendLine("五行力量：" + chart.strength.power.entries.joinToString("、") { "${it.key} ${it.value}%" })
        sb.appendLine("综合偏性：" + chart.wuxingBias.entries.joinToString("、") { "${it.key} ${it.value}" })
        sb.appendLine(chart.dayun.note)
        sb.appendLine(chart.yunqi.note)
        sb.appendLine("补不足损有余提示：${chart.strength.advice}")
        sb.appendLine("注意：八字与运气学说缺乏现代循证依据，只能作为养生倾向参考，不得据此改变基于四诊的方剂结构。")
        sb.appendLine()
        sb.appendLine("【四、可参考经方（非白名单，可另选）】")
        KnowledgeBase.formulas.take(16).forEach { f ->
            sb.appendLine("· ${f.name}（${f.source}）：" + f.herbs.joinToString("、") { "${it.first}${it.second}g" } + "；主证 ${f.syndrome}")
        }
        sb.appendLine()
        sb.appendLine("【五、任务】")
        sb.appendLine("请依据上述证型后验与四诊证据，优先取张仲景经方，给出方剂（药量精确到克，含炮制与煎服法）、对应中成药、养生建议；")
        sb.appendLine("在 references 中列出条文，在 cautions 中写明禁忌、红旗提示与「非处方、须线下就医」。")
        sb.appendLine("若证据不足，请把 confidence 置为「低」并降低方剂确定性表述。")
        return sb.toString().trim()
    }
}
