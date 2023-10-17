package org.hildan.bots.riseoflords.client.parsers

import org.hildan.ocr.reference.*
import javax.imageio.*
import kotlin.io.path.*
import kotlin.test.*

/**
 * To regenerate new images.
 */
fun main() {
    val inputDir = Path("src/test/resources/gold-imgs")
    val outputDir = Path("src/main/resources/img2")
    GoldImageOCR.textDetector.splitAndSaveSubImages(inputDir, outputDir)
}

class GoldImageOCRTest {

    @Test
    fun tests() {
        assertGoldAmountEquals(50_690_425, "50.690.425.png")
        assertGoldAmountEquals(103_810_000, "103.810.000.png")
        assertGoldAmountEquals(174_900, "174.900.png")
        assertGoldAmountEquals(280_000, "280.000.png")
        assertGoldAmountEquals(350_000, "350.000.png")
    }

    private fun assertGoldAmountEquals(expected: Long, imgName: String) {
        val img = ImageIO.read(javaClass.getResourceAsStream("/gold-img-samples/$imgName"))
        val amount = GoldImageOCR.readAmount(img)
        assertEquals(expected, amount)
    }
}