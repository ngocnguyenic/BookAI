package service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.apache.pdfbox.cos.COSName;

public class PDFImageExtractor {

    public static void extractImages(String pdfPath, String outputDir) throws IOException {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDPageTree pages = document.getPages();
            int imageCount = 1;

            for (PDPage page : pages) {
                PDResources resources = page.getResources();
                for (COSName xObjectName : resources.getXObjectNames()) {
                    if (resources.isImageXObject(xObjectName)) {
                        PDImageXObject image = (PDImageXObject) resources.getXObject(xObjectName);
                        BufferedImage bImage = image.getImage();

                        String fileName = outputDir + File.separator + "image_" + (imageCount++) + ".png";
                        ImageIO.write(bImage, "png", new File(fileName));
                        System.out.println("Saved: " + fileName);
                    }
                }
            }
        }
    }
}
