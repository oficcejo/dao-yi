package cn.ws9.daoyi

import cn.ws9.daoyi.core.bazi.Shichen
import cn.ws9.daoyi.core.tcm.AgeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class InputHelpersTest {

    @Test
    fun 周岁自动推算() {
        val birth = LocalDate.of(1990, 5, 15)
        assertEquals(36, AgeCalculator.years(birth, LocalDate.of(2026, 11, 7)))
        assertEquals(35, AgeCalculator.years(birth, LocalDate.of(2026, 1, 1)))
        assertEquals(0, AgeCalculator.years(birth, birth))
        assertEquals(0, AgeCalculator.years(LocalDate.of(2030, 1, 1), LocalDate.of(2026, 11, 7)))
        assertEquals("36 周岁", AgeCalculator.label(birth, LocalDate.of(2026, 11, 7)))
    }

    @Test
    fun 十二时辰边界() {
        assertEquals(0, Shichen.indexOf(LocalTime.of(23, 30)))
        assertEquals(0, Shichen.indexOf(LocalTime.of(0, 30)))
        assertEquals(1, Shichen.indexOf(LocalTime.of(1, 0)))
        assertEquals(1, Shichen.indexOf(LocalTime.of(2, 59)))
        assertEquals(2, Shichen.indexOf(LocalTime.of(3, 0)))
        assertEquals(5, Shichen.indexOf(LocalTime.of(10, 30)))
        assertEquals(11, Shichen.indexOf(LocalTime.of(22, 30)))
        assertEquals("巳时", Shichen.name(LocalTime.of(10, 30)))
    }

    @Test
    fun 每个时辰默认时刻都落在该时辰内() {
        Shichen.DEFAULTS.forEachIndexed { i, value ->
            assertEquals("时辰 $i 默认时刻 $value 越界", i, Shichen.indexOf(LocalTime.parse(value)))
        }
        assertTrue(Shichen.RANGES.size == 12 && Shichen.NAMES.size == 12)
    }
}
