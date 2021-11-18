package org.hildan.bots.riseoflords.client.parsers

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private class RefImage(
    imgName: String,
    val value: String,
) {
    val img: BufferedImage = ImageIO.read(RefImage::class.java.getResourceAsStream("/img/$imgName"))
}

internal object GoldImageOCR {

    private val REF_IMGS = (0..9).map { RefImage("$it.png", "$it") } + RefImage("dot.png", ".")

    fun readAmount(img: BufferedImage): Long {
        require(img.width == 70) { "image width is not 70" }
        require(img.height == 8) { "image height is not 8" }
        val amountAsText = img.splitOnTransparentColumns().joinToString("") { it.toDigitOrDot() }
        val amount = amountAsText.replace(".", "").toLongOrNull()
        return amount ?: error("Bad OCR result, cannot convert '$amountAsText' to a gold amount")
    }

    private fun BufferedImage.toDigitOrDot(): String =
        REF_IMGS.firstOrNull { hasSameAlphaAs(it.img) }?.value ?: error("Unrecognized digit in gold image")
}

private fun BufferedImage.hasSameAlphaAs(reference: BufferedImage): Boolean {
    if (height != reference.height || width != reference.width) {
        return false
    }
    for (i in 0 until width) {
        for (j in 0 until height) {
            if (getAlphaAt(i, j) != reference.getAlphaAt(i, j)) {
                return false
            }
        }
    }
    return true
}

private fun BufferedImage.splitOnTransparentColumns(): List<BufferedImage> =
    getNonTransparentColumnRanges().map { colRange -> subImage(colRange) }

@OptIn(ExperimentalStdlibApi::class)
private fun BufferedImage.getNonTransparentColumnRanges(): List<IntRange> {
    val rangeBounds = buildList {
        add(-1)
        addAll(getTransparentColumnsIndices())
        add(width)
    }
    return rangeBounds.zipWithNext { s, e -> (s + 1) until e }.filter { !it.isEmpty() }
}

private fun BufferedImage.getTransparentColumnsIndices(): List<Int> = (0 until width).filter { isTransparentColumn(it) }

private fun BufferedImage.isTransparentColumn(col: Int) = (0 until height).all { row -> getAlphaAt(col, row) == 0 }

private fun BufferedImage.getAlphaAt(i: Int, j: Int) = getRGB(i, j) ushr 24

private fun BufferedImage.subImage(colRange: IntRange): BufferedImage =
    getSubimage(colRange.first, 0, colRange.last - colRange.first + 1, height)
