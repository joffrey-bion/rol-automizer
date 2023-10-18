package org.hildan.bots.riseoflords.client.parsers

import org.hildan.ocr.reference.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
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

    @ParameterizedTest
    @ValueSource(strings = ["50.690.425.png", "103.810.000.png", "174.900.png", "280.000.png", "350.000.png"])
    fun readAmountImage(imageName: String) {
        val image = ImageIO.read(javaClass.getResourceAsStream("/gold-img-samples/$imageName"))
        val actualAmount = GoldImageOCR.readAmount(image)
        val expectedAmount = imageName.removeSuffix(".png").replace(".", "").toLong()
        assertEquals(expectedAmount, actualAmount)
    }
}
