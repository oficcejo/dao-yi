package cn.ws9.daoyi.core.bazi

import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * 四柱排盘总装。
 * 规则：年以立春为界、月以「节」为界（非农历月）、日以儒略日连续纪日、时以真太阳时辰配五鼠遁。
 */
object BaziEngine {

    fun compute(input: BirthInput): BaziChart {
        val bj = LocalDateTime.of(input.date, input.time)
        val tst = SolarTerm.trueSolarTime(bj, input.longitude, input.applyEquationOfTime)
        val zdt: ZonedDateTime = tst.atZone(SolarTerm.BJ_ZONE)

        // 年柱：立春分界
        var ganYear = tst.year
        if (zdt.isBefore(SolarTerm.liChun(tst.year))) ganYear -= 1
        val yIdx = Ganzhi.yearJiazi(ganYear)
        val yearPillar = Pillar(Ganzhi.ganOfJiazi(yIdx), Ganzhi.zhiOfJiazi(yIdx))

        // 月柱：十二节分界，月序 0=寅
        val jie = mutableListOf<Pair<ZonedDateTime, Int>>()
        for (y in tst.year - 1..tst.year + 1) {
            SolarTerm.JIE_INDICES.forEachIndexed { order, termIdx ->
                jie += SolarTerm.at(y, termIdx) to order
            }
        }
        jie.sortBy { it.first.toEpochSecond() }
        var monthOrder = 11
        for ((time, order) in jie) {
            if (!time.isAfter(zdt)) monthOrder = order else break
        }
        val yinStartGan = (2 + 2 * (Ganzhi.ganIndex(yearPillar.gan) % 5)) % 10
        val monthPillar = Pillar(
            Ganzhi.GAN[(yinStartGan + monthOrder) % 10],
            Ganzhi.ZHI[(2 + monthOrder) % 12]
        )

        // 日柱：儒略日 + 49 取六十甲子；子时换日可配置
        var dayDate = tst.toLocalDate()
        if (input.ziHourShiftsDay && tst.hour >= 23) dayDate = dayDate.plusDays(1)
        val dIdx = Ganzhi.mod(SolarTerm.julianDayNumber(dayDate) + 49L, 60L).toInt()
        val dayPillar = Pillar(Ganzhi.ganOfJiazi(dIdx), Ganzhi.zhiOfJiazi(dIdx))

        // 时柱：五鼠遁
        val hourZhi = ((tst.hour + 1) / 2) % 12
        val ziGan = (2 * Ganzhi.ganIndex(dayPillar.gan)) % 10
        val hourPillar = Pillar(Ganzhi.GAN[(ziGan + hourZhi) % 10], Ganzhi.ZHI[hourZhi])

        val pillars = listOf(yearPillar, monthPillar, dayPillar, hourPillar)
        val dayMaster = dayPillar.gan

        val tenGods = pillars.mapIndexed { i, p ->
            if (i == 2) "日主" else Ganzhi.tenGod(dayMaster, p.gan)
        }
        val hiddenStems = pillars.map { Ganzhi.hidden(it.zhi) }
        val nayin = pillars.map { Ganzhi.nayin(Ganzhi.jiaziIndexOf(it.gan, it.zhi)) }

        val strength = Strength.analyze(pillars)
        val dayun = Dayun.compute(tst, yearPillar.gan, input.male, monthPillar, input.referenceYear)
        val yunqi = Yunqi.compute(input.referenceYear, tst)

        // 综合五行偏性：本命 70% + 大运流年 20% + 五运六气 10%
        val keys = listOf("木", "火", "土", "金", "水")
        val lnGanWx = Ganzhi.wx(dayun.liuNian.gan)
        val dyGan = dayun.current.takeIf { it.length == 2 }?.substring(0, 1)
        val combined = keys.associateWith { k ->
            var v = strength.bias.getValue(k) * 0.70
            v += yunqi.bias.getValue(k) * 0.10
            if (lnGanWx == k) v += 0.12
            if (dyGan != null && Ganzhi.wx(dyGan) == k) v += 0.08
            (v * 10.0).toInt() / 10.0
        }

        return BaziChart(
            pillars = pillars,
            tenGods = tenGods,
            hiddenStems = hiddenStems,
            nayin = nayin,
            dayMaster = dayMaster,
            solarTermName = currentTermName(zdt),
            trueSolarTime = tst,
            beijingTime = bj,
            strength = strength,
            dayun = dayun,
            yunqi = yunqi,
            wuxingBias = combined
        )
    }

    private fun currentTermName(zdt: ZonedDateTime): String {
        val all = mutableListOf<Pair<ZonedDateTime, String>>()
        for (y in zdt.year - 1..zdt.year + 1) {
            SolarTerm.NAMES.forEachIndexed { i, name -> all += SolarTerm.at(y, i) to name }
        }
        all.sortBy { it.first.toEpochSecond() }
        var name = "冬至"
        for ((time, n) in all) {
            if (!time.isAfter(zdt)) name = n else break
        }
        return name
    }
}
