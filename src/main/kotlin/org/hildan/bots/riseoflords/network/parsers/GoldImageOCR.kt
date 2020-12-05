package org.hildan.bots.riseoflords.network.parsers

import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

internal object GoldImageOCR {

    private val logger = LoggerFactory.getLogger(GoldImageOCR::class.java)

    private const val DIGITS_DIR = "/img"

    private val DIGITS = arrayOf(
        loadInternalImageResource("$DIGITS_DIR/0.png"),
        loadInternalImageResource("$DIGITS_DIR/1.png"),
        loadInternalImageResource("$DIGITS_DIR/2.png"),
        loadInternalImageResource("$DIGITS_DIR/3.png"),
        loadInternalImageResource("$DIGITS_DIR/4.png"),
        loadInternalImageResource("$DIGITS_DIR/5.png"),
        loadInternalImageResource("$DIGITS_DIR/6.png"),
        loadInternalImageResource("$DIGITS_DIR/7.png"),
        loadInternalImageResource("$DIGITS_DIR/8.png"),
        loadInternalImageResource("$DIGITS_DIR/9.png")
    )

    private val DOT = loadInternalImageResource("$DIGITS_DIR/dot.png")

    fun readAmount(img: BufferedImage): Int {
        assert(img.width == 70) { "image width is not 70" }
        assert(img.height == 8) { "image height is not 8" }
        val digits = splitIntoDigits(img)
        val sb = StringBuilder()
        for (digit in digits) {
            sb.append(recognizeDigitOrDot(digit))
        }
        val amountAsText = sb.toString()
        return try {
            if (amountAsText == "") {
                logger.error("OCR failed to recognize anything")
                return -1
            }
            Integer.valueOf(amountAsText.replace(".", ""))
        } catch (e: NumberFormatException) {
            logger.error("Bad OCR result, cannot convert '{}' to a gold amount", amountAsText)
            -1
        }
    }

    private fun loadInternalImageResource(filename: String): BufferedImage = try {
        ImageIO.read(GoldImageOCR::class.java.getResourceAsStream(filename))
    } catch (e: IOException) {
        throw RuntimeException("Internal image $filename couldn't be loaded")
    }

    private fun getEmptyColumns(img: BufferedImage): List<Int> {
        val emptyCols: MutableList<Int> = ArrayList()
        col_loop@ for (i in 0 until img.width) {
            for (j in 0 until img.height) {
                val rgb = img.getRGB(i, j)
                if (rgb ushr 24 != 0) {
                    continue@col_loop
                }
            }
            emptyCols.add(i)
        }
        return emptyCols
    }

    private fun getDigitsBounds(img: BufferedImage): List<Array<Int?>> {
        val emptyCols = getEmptyColumns(img)
        val digitsBounds: MutableList<Array<Int?>> = ArrayList()
        var start: Int? = null
        var end: Int? = null
        for (i in 0 until img.width) {
            if (emptyCols.contains(i)) {
                if (start != null) {
                    digitsBounds.add(arrayOf(start, end))
                }
                start = null
                end = null
                continue
            }
            if (start == null) {
                start = i
            }
            end = i
        }
        return digitsBounds
    }

    private fun splitIntoDigits(img: BufferedImage): List<BufferedImage> {
        val digitsBounds = getDigitsBounds(img)
        val digitsImages: MutableList<BufferedImage> = ArrayList()
        for (bounds in digitsBounds) {
            val digitImg = BufferedImage(bounds[1]!! - bounds[0]!! + 1, img.height, BufferedImage.TYPE_INT_ARGB)
            for (i in 0 until digitImg.width) {
                for (j in 0 until digitImg.height) {
                    digitImg.setRGB(i, j, img.getRGB(i + bounds[0]!!, j))
                }
            }
            digitsImages.add(digitImg)
        }
        return digitsImages
    }

    private fun getARGB(argb: Int): IntArray {
        val res = IntArray(4)
        res[0] = argb ushr 24 and 0xFF
        res[1] = argb ushr 16 and 0xFF
        res[2] = argb ushr 8 and 0xFF
        res[3] = argb and 0xFF
        return res
    }

    private fun areSimilar(recoPixel: Int, refPixel: Int): Boolean {
        if (recoPixel == refPixel) {
            return true
        }
        val recoARGB = getARGB(recoPixel)
        val refARGB = getARGB(refPixel)
        return recoARGB[0] == refARGB[0]
    }

    private fun areSimilar(candidate: BufferedImage, reference: BufferedImage): Boolean {
        // check dimensions
        if (candidate.height != reference.height) {
            return false
        }
        if (candidate.width != reference.width) {
            return false
        }
        // check pixels
        for (i in 0 until candidate.width) {
            for (j in 0 until candidate.height) {
                val recoPixel = candidate.getRGB(i, j)
                val refPixel = reference.getRGB(i, j)
                if (!areSimilar(recoPixel, refPixel)) {
                    return false
                }
            }
        }
        return true
    }

    private fun recognizeDigitOrDot(digitImg: BufferedImage): String {
        for (i in DIGITS.indices) {
            if (areSimilar(digitImg, DIGITS[i])) {
                return i.toString()
            }
        }
        if (areSimilar(digitImg, DOT)) {
            return "."
        }
        logger.error("Unrecognized digit in gold image")
        return " "
    }
}