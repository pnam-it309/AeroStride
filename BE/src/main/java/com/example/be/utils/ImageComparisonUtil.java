package com.example.be.utils;

import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageComparisonUtil {

    private static final int BASE_SIZE = 100;

    /**
     * So sánh 2 bức ảnh, trả về % giống nhau
     */
    public static double compareImage(MultipartFile uploadedFile, String base64DbImage) throws IOException {
        if (base64DbImage == null || base64DbImage.isEmpty()) {
            return 0.0;
        }

        // Chuyển MultipartFile thành BufferedImage
        BufferedImage img1 = ImageIO.read(uploadedFile.getInputStream());

        // Chuyển Base64 thành BufferedImage
        String base64Data = base64DbImage;
        if (base64Data.contains(",")) {
            base64Data = base64Data.split(",")[1];
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        BufferedImage img2 = ImageIO.read(new ByteArrayInputStream(imageBytes));

        if (img1 == null || img2 == null) {
            return 0.0;
        }

        // Resize và Grayscale
        BufferedImage processedImg1 = processImage(img1);
        BufferedImage processedImg2 = processImage(img2);

        return calculateSimilarity(processedImg1, processedImg2);
    }

    private static BufferedImage processImage(BufferedImage img) {
        BufferedImage resizedImage = new BufferedImage(BASE_SIZE, BASE_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(img, 0, 0, BASE_SIZE, BASE_SIZE, null);
        g.dispose();
        return resizedImage;
    }

    private static double calculateSimilarity(BufferedImage img1, BufferedImage img2) {
        long diff = 0;
        for (int y = 0; y < BASE_SIZE; y++) {
            for (int x = 0; x < BASE_SIZE; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                
                int r1 = (rgb1 >> 16) & 0xff;
                int r2 = (rgb2 >> 16) & 0xff;
                
                diff += Math.abs(r1 - r2);
            }
        }
        double maxDiff = 255.0 * BASE_SIZE * BASE_SIZE;
        double percentage = 100.0 - (100.0 * diff / maxDiff);
        
        return percentage;
    }
}
