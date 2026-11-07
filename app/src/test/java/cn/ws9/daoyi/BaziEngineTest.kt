package cn.ws9.daoyi

import cn.ws9.daoyi.core.bazi.BaziEngine
import cn.ws9.daoyi.core.bazi.BirthInput
import cn.ws9.daoyi.core.bazi.Ganzhi
import cn.ws9.daoyi.core.bazi.Pillar
import cn.ws9.daoyi.core.bazi.SolarTerm
import cn.ws9.daoyi.core.bazi.Strength
import cn.ws9.daoyi.core.bazi.Yunqi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BaziEngineTest {

    @Test
    fun julianDayAnchor() {
        assertEquals(2451545L, SolarTerm.julianDayNumber(LocalDate.of(2000, 1, 1)))
    }

    @Test
    fun dayPillarAnchor2000() {
        val idx = Ganzhi.mod(SolarTerm.julianDayNumber(LocalDate.of(2000, 1, 1)) + 49L, 60L).toInt()
        assertEquals("戊午", Ganzhi.jiaziName(idx))
    }

    @Test
    fun liChunFallsInEarlyFebruary() {
        val lc = SolarTerm.at(2024, 2)
        assertEquals(2, lc.monthValue)
        assertTrue("立春应在 2/3-2/5，实际 ${lc.dayOfMonth}日", lc.dayOfMonth in 3..5)
    }

    @Test
    fun springEquinoxIsAroundMarch20() {
        val eq = SolarTerm.at(2024, 5)
        assertEquals(3, eq.monthValue)
        assertTrue("春分应在 3/19-3/21", eq.dayOfMonth in 19..21)
    }

    @Test
    fun yearPillarUsesLiChunBoundary() {
        // 1984-02-02 在立春（约 2/4）之前，仍属癸亥年
        val chart = BaziEngine.compute(
            BirthInput(LocalDate.of(1984, 2, 2), LocalTime.of(12, 0), 120.0, "测试", true,
                applyEquationOfTime = false, referenceYear = 2026)
        )
        assertEquals("癸", chart.pillars[0].gan)
        assertEquals("亥", chart.pillars[0].zhi)
        assertEquals("丑", chart.pillars[1].zhi)
    }

    @Test
    fun trueSolarTimeByLongitude() {
        val bj = LocalDate.of(1990, 5, 15).atTime(12, 0)
        val same = SolarTerm.trueSolarTime(bj, 120.0, false)
        assertEquals(bj, same)
        val west = SolarTerm.trueSolarTime(bj, 90.0, false)
        assertEquals(bj.minusMinutes(120), west)
    }

    @Test
    fun ziHourShiftsDayPillar() {
        val a = BaziEngine.compute(
            BirthInput(LocalDate.of(1990, 5, 15), LocalTime.of(23, 30), 120.0, "测试", true,
                applyEquationOfTime = false, ziHourShiftsDay = true)
        )
        val b = BaziEngine.compute(
            BirthInput(LocalDate.of(1990, 5, 15), LocalTime.of(23, 30), 120.0, "测试", true,
                applyEquationOfTime = false, ziHourShiftsDay = false)
        )
        assertTrue("子时换日开关应改变日柱", a.pillars[2].name != b.pillars[2].name)
        assertEquals("子", a.pillars[3].zhi)
    }

    @Test
    fun strengthRespondsToMonthCommand() {
        val strong = Strength.analyze(
            listOf(Pillar("甲", "寅"), Pillar("甲", "寅"), Pillar("甲", "寅"), Pillar("甲", "寅"))
        )
        val weak = Strength.analyze(
            listOf(Pillar("庚", "申"), Pillar("庚", "申"), Pillar("甲", "申"), Pillar("庚", "申"))
        )
        assertTrue("得令且多比劫应旺于受克", strong.score > weak.score)
        assertEquals(100.0, strong.power.values.sum(), 0.2)
    }

    @Test
    fun yunqi2026BingWu() {
        val y = Yunqi.compute(2026, LocalDate.of(2026, 5, 15).atTime(10, 0))
        assertEquals("丙午", y.ganZhi)
        assertEquals("水运 · 太过", y.zhongYun)
        assertEquals("少阴君火", y.siTian)
        assertEquals("阳明燥金", y.zaiQuan)
    }

    @Test
    fun dayunProducesEightSteps() {
        val chart = BaziEngine.compute(
            BirthInput(LocalDate.of(1990, 5, 15), LocalTime.of(10, 30), 116.59, "济宁", true,
                referenceYear = 2026)
        )
        assertEquals(8, chart.dayun.steps.size)
        assertTrue(chart.dayun.startAge >= 0.0)
        assertEquals("丙午", chart.dayun.liuNian.name)
    }
}
