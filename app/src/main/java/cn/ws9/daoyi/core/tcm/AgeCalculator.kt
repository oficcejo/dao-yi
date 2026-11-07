package cn.ws9.daoyi.core.tcm

import java.time.LocalDate
import java.time.Period

/** 按公历出生日期自动推算周岁，用于安全闸门的人群判断，不再由用户手填。 */
object AgeCalculator {

    fun years(birth: LocalDate, today: LocalDate = LocalDate.now()): Int =
        if (birth.isAfter(today)) 0 else Period.between(birth, today).years.coerceIn(0, 120)

    fun label(birth: LocalDate, today: LocalDate = LocalDate.now()): String {
        val y = years(birth, today)
        return if (y == 0) "不满 1 周岁" else "$y 周岁"
    }
}
