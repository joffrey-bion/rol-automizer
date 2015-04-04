package org.hildan.bots.riseoflords.network.parsers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.hildan.bots.riseoflords.util.Log;

public class GoldImageOCR {

    private static final String DIGITS_DIR = "/img";

    private static final BufferedImage[] DIGITS = { loadImage(DIGITS_DIR + "/0.png"), loadImage(DIGITS_DIR + "/1.png"),
        loadImage(DIGITS_DIR + "/2.png"), loadImage(DIGITS_DIR + "/3.png"), loadImage(DIGITS_DIR + "/4.png"),
        loadImage(DIGITS_DIR + "/5.png"), loadImage(DIGITS_DIR + "/6.png"), loadImage(DIGITS_DIR + "/7.png"),
        loadImage(DIGITS_DIR + "/8.png"), loadImage(DIGITS_DIR + "/9.png") };

    private static final BufferedImage DOT = loadImage(DIGITS_DIR + "/dot.png");

    public static int readAmount(BufferedImage img) {
        assert img.getWidth() == 70 : "image width is not 70";
        assert img.getHeight() == 8 : "image height is not 8";
        final List<BufferedImage> digits = getDigitsImages(img);
        final StringBuilder sb = new StringBuilder();
        for (final BufferedImage digit : digits) {
            sb.append(getDigit(digit));
        }
        try {
            final String str = sb.toString();
            if (str.equals("")) {
                System.err.println("OCR failed to recognize anything");
                return -1;
            }
            return Integer.valueOf(str.replace(".", ""));
        } catch (final NumberFormatException e) {
            System.err.println("bad OCR result");
            return -1;
        }
    }

    private static BufferedImage loadImage(String filename) {
        try {
            final BufferedImage img = ImageIO.read(GoldImageOCR.class.getResourceAsStream(filename));
            return img;
        } catch (final IOException e) {
            throw new RuntimeException("image couldn't be loaded");
        }
    }

    private static List<Integer> getEmptyColumns(BufferedImage img) {
        final List<Integer> emptyCols = new ArrayList<>();
        col_loop: for (int i = 0; i < img.getWidth(); i++) {
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
                if (start != null && end != null) {
                    digitsBounds.add(new Integer[] { start, end });
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

    private static List<BufferedImage> getDigitsImages(BufferedImage img) {
        final List<Integer[]> digitsBounds = getDigitsBounds(img);
        final List<BufferedImage> digitsImages = new ArrayList<>();
        for (final Integer[] bounds : digitsBounds) {
            final BufferedImage digitImg = new BufferedImage(bounds[1] - bounds[0] + 1, img.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
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
        if (recoARGB[0] == refARGB[0]) {
            return true;
        }
        return false;
    }

    private static boolean areSimilar(BufferedImage recoDigit, BufferedImage refDigit) {
        // check dimensions
        if (recoDigit.getHeight() != refDigit.getHeight()) {
            return false;
        }
        if (recoDigit.getWidth() != refDigit.getWidth()) {
            return false;
        }
        // check pixels
        for (int i = 0; i < recoDigit.getWidth(); i++) {
            for (int j = 0; j < recoDigit.getHeight(); j++) {
                final int recoPixel = recoDigit.getRGB(i, j);
                final int refPixel = refDigit.getRGB(i, j);
                if (!areSimilar(recoPixel, refPixel)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String getDigit(BufferedImage digitImg) {
        for (int i = 0; i < DIGITS.length; i++) {
            if (areSimilar(digitImg, DIGITS[i])) {
                return String.valueOf(i);
            }
        }
        if (areSimilar(digitImg, DOT)) {
            return ".";
        }
        Log.get().e("OCR", "unrecognized digit");
        return " ";
    }

}
