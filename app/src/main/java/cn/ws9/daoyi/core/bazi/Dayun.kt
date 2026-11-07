package cn.ws9.daoyi.core.bazi

import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.abs

/** 大运起运与流年：三日为一岁、一日为四个月 */
object Dayun {

    fun compute(
        birthTrueSolar: java.time.LocalDateTime,
        yearGan: String,
        male: Boolean,
        monthPillar: Pillar,
        referenceYear: Int
    ): DayunInfo {
        val yang = Ganzhi.isYang(yearGan)
        val forward = (yang && male) || (!yang && !male)
        val zdt: ZonedDateTime = birthTrueSolar.atZone(SolarTerm.BJ_ZONE)

        val jie = mutableListOf<ZonedDateTime>()
        for (y in birthTrueSolar.year - 1..birthTrueSolar.year + 1) {
            SolarTerm.JIE_INDICES.forEach { idx -> jie += SolarTerm.at(y, idx) }
        }
        jie.sortBy { it.toEpochSecond() }

        val boundary = if (forward) {
            jie.firstOrNull { it.isAfter(zdt) } ?: zdt.plusDays(90)
        } else {
            jie.lastOrNull { it.isBefore(zdt) } ?: zdt.minusDays(90)
        }
        val days = abs(Duration.between(zdt, boundary).toMinutes()) / 1440.0
        val startAge = days / 3.0

        val monthIdx = Ganzhi.jiaziIndexOf(monthPillar.gan, monthPillar.zhi)
        val startYear = birthTrueSolar.year + startAge.toInt()
        val steps = (1..8).map { i ->
            val offset = if (forward) i else -i
            DayunStep(
                order = i,
                startAge = startAge.toInt() + (i - 1) * 10,
                startYear = startYear + (i - 1) * 10,
                ganZhi = Ganzhi.jiaziName(monthIdx + offset)
            )
        }

        val lnIdx = Ganzhi.yearJiazi(referenceYear)
        val liuNian = Pillar(Ganzhi.ganOfJiazi(lnIdx), Ganzhi.zhiOfJiazi(lnIdx))
        val age = referenceYear - birthTrueSolar.year
        val current = steps.firstOrNull { age >= it.startAge && age < it.startAge + 10 }?.ganZhi ?: "未起运（月柱代运）"

        return DayunInfo(
            forward = forward,
            startAge = (startAge * 10.0).toInt() / 10.0,
            steps = steps,
            liuNianYear = referenceYear,
            liuNian = liuNian,
            current = current,
            note = "${if (forward) "顺排" else "逆排"}，${(days * 10.0).toInt() / 10.0}日折${(startAge * 10.0).toInt() / 10.0}岁起运；" +
                    "流年 $referenceYear ${liuNian.name}，大运 $current。运与流年仅作时空参考，权重低于四诊。"
        )
    }
}
