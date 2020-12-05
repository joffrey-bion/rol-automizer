package org.hildan.bots.riseoflords.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

object Format {
    private val symbols = DecimalFormatSymbols.getInstance().apply { groupingSeparator = '.' }
    private val fmt: DecimalFormat = DecimalFormat("###,###.##", symbols)

    fun gold(amount: Int): String = fmt.format(amount.toLong())
}