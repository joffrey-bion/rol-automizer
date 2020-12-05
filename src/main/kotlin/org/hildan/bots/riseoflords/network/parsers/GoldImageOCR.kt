package org.hildan.bots.riseoflords.network.parsers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GoldImageOCR {

    private static final Logger logger = LoggerFactory.getLogger(GoldImageOCR.class);

    private static final String DIGITS_DIR = "/img";

    private static final BufferedImage[] DIGITS =
            {loadInternalImageResource(DIGITS_DIR + "/0.png"), loadInternalImageResource(DIGITS_DIR + "/1.png"),
                    loadInternalImageResource(DIGITS_DIR + "/2.png"), loadInternalImageResource(DIGITS_DIR + "/3.png"),
                    loadInternalImageResource(DIGITS_DIR + "/4.png"), loadInternalImageResource(DIGITS_DIR + "/5.png"),
                    loadInternalImageResource(DIGITS_DIR + "/6.png"), loadInternalImageResource(DIGITS_DIR + "/7.png"),
                    loadInternalImageResource(DIGITS_DIR + "/8.png"), loadInternalImageResource(DIGITS_DIR + "/9.png")};

    private static final BufferedImage DOT = loadInternalImageResource(DIGITS_DIR + "/dot.png");

    static int readAmount(BufferedImage img) {
        assert img.getWidth() == 70 : "image width is not 70";
        assert img.getHeight() == 8 : "image height is not 8";
        final List<BufferedImage> digits = splitIntoDigits(img);
        final StringBuilder sb = new StringBuilder();
        for (final BufferedImage digit : digits) {
            sb.append(recognizeDigitOrDot(digit));
        }
        final String amountAsText = sb.toString();
        try {
            if (amountAsText.equals("")) {
                logger.error("OCR failed to recognize anything");
                return -1;
            }
            return Integer.valueOf(amountAsText.replace(".", ""));
        } catch (final NumberFormatException e) {
            logger.error("Bad OCR result, cannot convert '{}' to a gold amount", amountAsText);
            return -1;
        }
    }

    private static BufferedImage loadInternalImageResource(String filename) {
        try {
            return ImageIO.read(GoldImageOCR.class.getResourceAsStream(filename));
        } catch (final IOException e) {
            throw new RuntimeException(String.format("Internal image %s couldn't be loaded", filename));
        }
    }

    private static List<Integer> getEmptyColumns(BufferedImage img) {
        final List<Integer> emptyCols = new ArrayList<>();
        col_loop:
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                final int rgb = img.getRGB(i, j);
                if (!(rgb >>> 24 == 0)) {
                    continue col_loop;
                }
            }
            emptyCols.add(i);
        }
        return emptyCols;
    }

    private static List<Integer[]> getDigitsBounds(BufferedImage img) {
        final List<Integer> emptyCols = getEmptyColumns(img);
        final List<Integer[]> digitsBounds = new ArrayList<>();
        Integer start = null;
        Integer end = null;
        for (int i = 0; i < img.getWidth(); i++) {
            if (emptyCols.contains(i)) {
                if (start != null) {
                    digitsBounds.add(new Integer[]{start, end});
                }
                start = null;
                end = null;
                continue;
            }
            if (start == null) {
                start = i;
            }
            end = i;
        }
        return digitsBounds;
    }

    private static List<BufferedImage> splitIntoDigits(BufferedImage img) {
        final List<Integer[]> digitsBounds = getDigitsBounds(img);
        final List<BufferedImage> digitsImages = new ArrayList<>();
        for (final Integer[] bounds : digitsBounds) {
            final BufferedImage digitImg =
                    new BufferedImage(bounds[1] - bounds[0] + 1, img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int i = 0; i < digitImg.getWidth(); i++) {
                for (int j = 0; j < digitImg.getHeight(); j++) {
                    digitImg.setRGB(i, j, img.getRGB(i + bounds[0], j));
                }
            }
            digitsImages.add(digitImg);
        }
        return digitsImages;
    }

    private static int[] getARGB(int argb) {
        final int[] res = new int[4];
        res[0] = argb >>> 24 & 0xFF;
        res[1] = argb >>> 16 & 0xFF;
        res[2] = argb >>> 8 & 0xFF;
        res[3] = argb & 0xFF;
        return res;
    }

    private static boolean areSimilar(int recoPixel, int refPixel) {
        if (recoPixel == refPixel) {
            return true;
        }
        final int[] recoARGB = getARGB(recoPixel);
        final int[] refARGB = getARGB(refPixel);
        return recoARGB[0] == refARGB[0];
    }

    private static boolean areSimilar(BufferedImage candidate, BufferedImage reference) {
        // check dimensions
        if (candidate.getHeight() != reference.getHeight()) {
            return false;
        }
        if (candidate.getWidth() != reference.getWidth()) {
            return false;
        }
        // check pixels
        for (int i = 0; i < candidate.getWidth(); i++) {
            for (int j = 0; j < candidate.getHeight(); j++) {
                final int recoPixel = candidate.getRGB(i, j);
                final int refPixel = reference.getRGB(i, j);
                if (!areSimilar(recoPixel, refPixel)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String recognizeDigitOrDot(BufferedImage digitImg) {
        for (int i = 0; i < DIGITS.length; i++) {
            if (areSimilar(digitImg, DIGITS[i])) {
                return String.valueOf(i);
            }
        }
        if (areSimilar(digitImg, DOT)) {
            return ".";
        }
        logger.error("Unrecognized digit in gold image");
        return " ";
    }
}