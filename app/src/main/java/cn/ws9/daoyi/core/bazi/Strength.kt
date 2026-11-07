package cn.ws9.daoyi.core.bazi

import kotlin.math.abs
import kotlin.math.roundToInt

/** 日主旺衰：得令、通根、透干三项逐项可追溯打分 */
object Strength {

    private val WX = listOf("木", "火", "土", "金", "水")

    /** 0 同我 / 1 我生 / 2 我克 / 3 克我 / 4 生我 */
    private fun rel(a: String, b: String): Int {
        val i = WX.indexOf(a).coerceAtLeast(0)
        val j = WX.indexOf(b).coerceAtLeast(0)
        return Ganzhi.mod(j - i, 5)
    }

    fun analyze(pillars: List<Pillar>): StrengthResult {
        val dm = pillars[2].gan
        val dmWx = Ganzhi.wx(dm)
        val items = mutableListOf<StrengthItem>()

        // 一、得令（月令）
        val monthWx = Ganzhi.wx(pillars[1].zhi)
        val ling = when (rel(dmWx, monthWx)) {
            0 -> 32.0
            4 -> 25.0
            3 -> -22.0
            2 -> -12.0
            else -> -14.0
        }
        items += StrengthItem("得令 · ${pillars[1].zhi}月", ling, "月令$monthWx 对日主$dmWx")

        // 二、通根（地支藏干，本气/中气/余气加权）
        var rootScore = 0.0
        var support = 0.0
        pillars.forEach { p ->
            Ganzhi.hidden(p.zhi).forEach { h ->
                val r = rel(dmWx, Ganzhi.wx(h.gan))
                val v = h.weight * when (r) {
                    0 -> 11.0
                    4 -> 8.0
                    3 -> -5.0
                    2 -> -4.0
                    else -> -4.0
                }
                rootScore += v
                if (r == 0 || r == 4) support += h.weight
            }
        }
        items += StrengthItem("通根 · 四支藏干", rootScore, "帮扶藏干合计 ${round1(support)}")

        // 三、透干（年月时天干）
        var stemScore = 0.0
        listOf(0, 1, 3).forEach { i ->
            val g = pillars[i].gan
            val r = rel(dmWx, Ganzhi.wx(g))
            stemScore += when (r) {
                0 -> 9.0
                4 -> 8.0
                3 -> -9.0
                2 -> -7.0
                else -> -7.0
            }
        }
        items += StrengthItem("透干 · 年月时", stemScore, "印比加分、克泄耗减分")

        val sum = items.sumOf { it.delta }
        val score = (50.0 + sum).coerceIn(0.0, 100.0)
        val level = when {
            score < 18.0 && support < 1.0 -> "疑似从格（需人工复核）"
            score >= 68.0 -> "身强 · 偏旺"
            score >= 56.0 -> "身强 · 稍旺"
            score <= 32.0 -> "身弱 · 偏弱"
            score <= 44.0 -> "身弱 · 稍弱"
            else -> "中和 · 平和"
        }

        // 五行力量分布
        val raw = WX.associateWith { 0.0 }.toMutableMap()
        pillars.forEach { p ->
            raw[Ganzhi.wx(p.gan)] = raw.getValue(Ganzhi.wx(p.gan)) + 1.0
            Ganzhi.hidden(p.zhi).forEach { h ->
                val k = Ganzhi.wx(h.gan)
                raw[k] = raw.getValue(k) + h.weight * 0.6
            }
        }
        val total = raw.values.sum().coerceAtLeast(0.0001)
        val power = WX.associateWith { raw.getValue(it) / total * 100.0 }
        val bias = WX.associateWith { ((power.getValue(it) - 20.0) / 20.0).coerceIn(-1.0, 1.0) }

        val max = bias.maxByOrNull { it.value }!!
        val min = bias.minByOrNull { it.value }!!
        val advice = "损有余：${max.key}偏盛（${round1(power.getValue(max.key))}%）；补不足：${min.key}偏弱（${round1(power.getValue(min.key))}%）。" +
                "日主$level，养生以扶${min.key}、缓${max.key}为纲，用药寒热须由四诊合参。"

        return StrengthResult(round1(score), level, items, power.mapValues { round1(it.value) }, bias, advice)
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
