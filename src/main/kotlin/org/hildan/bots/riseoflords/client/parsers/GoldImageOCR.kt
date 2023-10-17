package org.hildan.bots.riseoflords.client.parsers

import org.hildan.ocr.*
import org.hildan.ocr.reference.*
import java.awt.image.*

private fun referenceImage(imgName: String, text: String) = ReferenceImage(
    image = GoldImageOCR::class.java.getResourceImage("/img/$imgName"),
    text = text,
)

private val refImgDigits = (0..9).map { referenceImage(imgName = "$it.png", text = "$it") }
private val refImgDot = referenceImage(imgName = "dot.png", text = ".")

internal object GoldImageOCR {

    internal val textDetector = TextDetector(
        textColorFilter = ColorSimilarityFilter(
            referenceColor = Color.BLACK,
            rgbTolerance = 90,
            alphaTolerance = 0,
        ),
    )

    private val ocr = SimpleOcr(
        referenceImages = refImgDigits + refImgDot,
        textDetector = textDetector,
        minRecognitionScore = 0.9,
        spaceWidthThreshold = 50, // big enough so nothing is detected as a space
    )

    fun readAmount(img: BufferedImage): Long {
        require(img.width == 70) { "image width is not 70" }
        require(img.height == 8) { "image height is not 8" }
        val amountAsText = ocr.recognizeText(img)
        val amount = amountAsText.replace(".", "").toLongOrNull()
        return amount ?: error("Bad OCR result, cannot convert '$amountAsText' to a gold amount")
    }
}
