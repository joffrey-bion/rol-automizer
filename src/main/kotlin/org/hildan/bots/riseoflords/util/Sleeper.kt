package org.hildan.bots.riseoflords.util

import org.slf4j.LoggerFactory
import kotlin.random.Random

class Sleeper(private val speed: Speed) {

    enum class Speed(private val factor: Int) {
        INHUMAN(400),
        FAST(700),
        NORMAL(1000),
        SLOW(1500),
        REALLY_SLOW(2000);

        fun scale(millis: Int): Int = millis * factor / 1000
    }

    private fun sleep(minMillis: Int, maxMillis: Int, scaleDuration: Boolean = true) {
        try {
            val millis = Random.nextInt(minMillis, maxMillis)
            val scaledMillis = if (scaleDuration) speed.scale(millis) else millis
            logger.debug("    ...  faking human delay {} ms  ...", scaledMillis)
            Thread.sleep(scaledMillis.toLong())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    fun actionInPage() = sleep(600, 1000)

    private fun changePage() = sleep(900, 1500)

    fun changePageLong() = sleep(1000, 2000)

    fun readPlayerListPage() = sleep(1200, 2500)

    fun beforeRepair() = changePage()

    fun beforeGoldStorage() = changePage()

    fun afterGoldStorage() = sleep(2000, 3000)

    fun betweenAttacksWhenNoStorage() = changePage()

    fun waitAfterLogin() = sleep(6000, 7000, false)

    companion object {
        private val logger = LoggerFactory.getLogger(Sleeper::class.java)
    }
}