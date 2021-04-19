package org.hildan.bots.riseoflords.client.parsers

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

internal object GoldImageOCR {

    private val DIGITS = arrayOf(
        loadImage("0.png"),
        loadImage("1.png"),
        loadImage("2.png"),
        loadImage("3.png"),
        loadImage("4.png"),
        loadImage("5.png"),
        loadImage("6.png"),
        loadImage("7.png"),
        loadImage("8.png"),
        loadImage("9.png"),
    )

    private val DOT = loadImage("dot.png")

    private fun loadImage(filename: String): BufferedImage =
        ImageIO.read(GoldImageOCR::class.java.getResourceAsStream("/img/$filename"))

    fun readAmount(img: BufferedImage): Int {
        require(img.width == 70) { "image width is not 70" }
        require(img.height == 8) { "image height is not 8" }
        val amountAsText = img.splitAroundEmptyColumns().joinToString("") { it.toDigitOrDot() }
        val amount = amountAsText.replace(".", "").toIntOrNull()
        return amount ?: error("Bad OCR result, cannot convert '$amountAsText' to a gold amount")
    }

    private fun BufferedImage.toDigitOrDot(): String {
        DIGITS.forEachIndexed { digit, digitImg ->
            if (hasSameAlphaAs(digitImg)) {
                return digit.toString()
            }
        }
        if (hasSameAlphaAs(DOT)) {
            return "."
        }
        error("Unrecognized digit in gold image")
    }
}

private fun BufferedImage.hasSameAlphaAs(reference: BufferedImage): Boolean {
    // check dimensions
    if (height != reference.height) {
        return false
    }
    if (width != reference.width) {
        return false
    }
    // check pixels
    for (i in 0 until width) {
        for (j in 0 until height) {
            val recoPixel = getRGB(i, j)
            val refPixel = reference.getRGB(i, j)
            if (recoPixel.alpha != refPixel.alpha) {
                return false
            }
        }
    }
    return true
}

private fun BufferedImage.splitAroundEmptyColumns(): List<BufferedImage> =
    getNonEmptyColumnRanges().map { colRange -> subImage(colRange) }

@OptIn(ExperimentalStdlibApi::class)
private fun BufferedImage.getNonEmptyColumnRanges(): List<IntRange> {
    val rangeBounds = buildList {
        add(-1)
        addAll(getEmptyColumnsIndexes())
        add(width)
    }
    return rangeBounds.zipWithNext { s, e -> (s+1) until e }.filter { !it.isEmpty() }
}

private fun BufferedImage.getEmptyColumnsIndexes(): List<Int> = (0 until width).filter { col -> isEmptyColumn(col) }

private fun BufferedImage.isEmptyColumn(col: Int) = (0 until height).all { row -> getRGB(col, row).alpha == 0 }

val Int.alpha get() = this ushr 24

private fun BufferedImage.subImage(colRange: IntRange): BufferedImage {
    val digitImg = BufferedImage(colRange.last - colRange.first + 1, height, BufferedImage.TYPE_INT_ARGB)
    for (i in 0 until digitImg.width) {
        for (j in 0 until digitImg.height) {
            digitImg.setRGB(i, j, getRGB(i + colRange.first, j))
        }
    }
    return digitImg
}