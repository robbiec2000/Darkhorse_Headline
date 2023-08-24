package com.heima.tess4j;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

public class Application {
    public static void main(String[] args) throws TesseractException {
        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath("C:\\Users\\Robbie\\leadnews\\tessdata");
        tesseract.setLanguage("chi_sim");
        File file = new File("C:\\Users\\Robbie\\leadnews\\test_img.jpeg");

        String result = tesseract.doOCR(file);
        System.out.println(result);
    }
}
