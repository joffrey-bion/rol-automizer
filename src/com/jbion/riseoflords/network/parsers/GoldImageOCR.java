package com.jbion.riseoflords.network.parsers;

import java.awt.image.BufferedImage;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class GoldImageOCR {

    public static int readAmount(BufferedImage img) {
        assert img.getWidth() == 70 : "image width is not 70";
        assert img.getHeight() == 8 : "image height is not 8";
        Tesseract ocr = Tesseract.getInstance();
        try {
            String str = ocr.doOCR(img);
            return Integer.valueOf(str.replace(".", ""));
        } catch (TesseractException e) {
            System.out.println("OCR failed");
            return -1;
        }
    }

}
