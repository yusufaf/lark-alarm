package dev.yusufaf.lark.core

val AlarmListOrder: Comparator<Alarm> = compareBy({ it.hour }, { it.minute }, { it.id })
