package service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PDFExtractor {

    // --- 1. Extract toàn bộ text ---
    public static String extractText(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // sắp xếp text theo vị trí
            return stripper.getText(document);
        }
    }

    // --- 2. Convert tất cả trang PDF thành ảnh PNG ---
    public static void convertToImages(String filePath, String outputDir) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300); // DPI cao hơn = ảnh sắc nét hơn
                String fileName = outputDir + File.separator + "page_" + (page + 1) + ".png";
                ImageIO.write(image, "png", new File(fileName));
                System.out.println("Saved: " + fileName);
            }
        }
    }
}
