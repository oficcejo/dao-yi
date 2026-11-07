package cn.ws9.daoyi.core.bazi

import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * 五运六气：中运（天干化运）、司天/在泉（地支）、主气六步、客气六步。
 * 输出五行偏性向量，供贝叶斯引擎作为低权重时空先验调制。
 */
object Yunqi {

    private val ZHONG_YUN = mapOf(
        "甲" to "土运", "己" to "土运",
        "乙" to "金运", "庚" to "金运",
        "丙" to "水运", "辛" to "水运",
        "丁" to "木运", "壬" to "木运",
        "戊" to "火运", "癸" to "火运"
    )

    private val SI_TIAN = mapOf(
        "子" to "少阴君火", "午" to "少阴君火",
        "丑" to "太阴湿土", "未" to "太阴湿土",
        "寅" to "少阳相火", "申" to "少阳相火",
        "卯" to "阳明燥金", "酉" to "阳明燥金",
        "辰" to "太阳寒水", "戌" to "太阳寒水",
        "巳" to "厥阴风木", "亥" to "厥阴风木"
    )

    private val ZAI_QUAN = mapOf(
        "子" to "阳明燥金", "午" to "阳明燥金",
        "丑" to "太阳寒水", "未" to "太阳寒水",
        "寅" to "厥阴风木", "申" to "厥阴风木",
        "卯" to "少阴君火", "酉" to "少阴君火",
        "辰" to "太阴湿土", "戌" to "太阴湿土",
        "巳" to "少阳相火", "亥" to "少阳相火"
    )

    /** 主气六步，固定不变 */
    private val ZHU_QI = listOf("厥阴风木", "少阴君火", "少阳相火", "太阴湿土", "阳明燥金", "太阳寒水")

    /** 客气按三阴三阳次序循环 */
    private val KE_ORDER = listOf("厥阴风木", "少阴君火", "太阴湿土", "少阳相火", "阳明燥金", "太阳寒水")

    private val QI_TO_WX = mapOf(
        "厥阴风木" to "木", "少阴君火" to "火", "少阳相火" to "火",
        "太阴湿土" to "土", "阳明燥金" to "金", "太阳寒水" to "水"
    )

    /** 六气分界：大寒、春分、小满、大暑、秋分、小雪 */
    private val BOUNDARY_TERMS = listOf(1, 5, 9, 13, 17, 21)

    fun compute(year: Int, at: LocalDateTime): YunqiInfo {
        val idx = Ganzhi.yearJiazi(year)
        val gan = Ganzhi.ganOfJiazi(idx)
        val zhi = Ganzhi.zhiOfJiazi(idx)
        val zhongYun = ZHONG_YUN[gan] ?: "土运"
        val taiGuo = if (Ganzhi.isYang(gan)) "太过" else "不及"
        val siTian = SI_TIAN[zhi] ?: "少阴君火"
        val zaiQuan = ZAI_QUAN[zhi] ?: "阳明燥金"

        val bounds = BOUNDARY_TERMS.map { SolarTerm.at(year, it) }
        val zdt: ZonedDateTime = at.atZone(SolarTerm.BJ_ZONE)
        var qiIndex = 5
        for (i in 0..4) {
            if (!zdt.isBefore(bounds[i]) && zdt.isBefore(bounds[i + 1])) {
                qiIndex = i
                break
            }
        }
        val zhuQi = ZHU_QI[qiIndex]

        val s = KE_ORDER.indexOf(siTian).coerceAtLeast(0)
        val keQi = KE_ORDER[Ganzhi.mod(s + (qiIndex - 2), 6)]

        val wxKeys = listOf("木", "火", "土", "金", "水")
        val acc = wxKeys.associateWith { 0.0 }.toMutableMap()
        fun add(qi: String, weight: Double) {
            val k = QI_TO_WX[qi] ?: return
            acc[k] = acc.getValue(k) + weight
        }
        add(zhongYun.replace("运", "").let { yun ->
            when (yun) {
                "木" -> "厥阴风木"
                "火" -> "少阴君火"
                "土" -> "太阴湿土"
                "金" -> "阳明燥金"
                else -> "太阳寒水"
            }
        }, 0.40)
        add(siTian, 0.35)
        add(zaiQuan, 0.25)
        add(zhuQi, 0.20)
        add(keQi, 0.15)
        val mean = acc.values.average()
        val bias = wxKeys.associateWith {
            (((acc.getValue(it) - mean) / 0.5).coerceIn(-1.0, 1.0) * 10.0).toInt() / 10.0
        }

        return YunqiInfo(
            year = year,
            ganZhi = Ganzhi.jiaziName(idx),
            zhongYun = "$zhongYun · $taiGuo",
            taiGuoBuJi = taiGuo,
            siTian = siTian,
            zaiQuan = zaiQuan,
            zhuQi = zhuQi,
            keQi = keQi,
            bias = bias,
            note = "$year ${Ganzhi.jiaziName(idx)}：中运$zhongYun$taiGuo，司天$siTian，在泉$zaiQuan；" +
                    "当前主气$zhuQi、客气$keQi。运气学说为传统时空参考，不作为诊断依据。"
        )
    }
}
