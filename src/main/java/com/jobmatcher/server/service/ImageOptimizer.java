package com.jobmatcher.server.service;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

@Service
public class ImageOptimizer {

    /**
     * Resizes and compresses image to 300x300, saves temp PNG,
     * then converts that PNG to WebP using the cwebp CLI tool.
     * Returns the WebP temp file.
     */
    public File compressAndConvertToWebP(MultipartFile file) throws IOException, InterruptedException {
        // Step 1: Resize input image to 300x300 PNG
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        File tempPngFile = File.createTempFile("temp-", ".png");

        Thumbnails.of(originalImage)
                .size(300, 300)
                .outputFormat("png")
                .outputQuality(0.85)
                .toFile(tempPngFile);

        // Step 2: Convert PNG to WebP using cwebp command-line
        File tempWebPFile = File.createTempFile("optimized-", ".webp");

        // Adjust path to cwebp if it's not on your system PATH
        String cwebpCmd = "cwebp";

        ProcessBuilder pb = new ProcessBuilder(
                cwebpCmd,
                "-q", "85",  // quality 85%
                tempPngFile.getAbsolutePath(),
                "-o",
                tempWebPFile.getAbsolutePath()
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        // Delete the temp PNG since we don't need it anymore
        tempPngFile.delete();

        if (exitCode != 0) {
            throw new IOException("cwebp conversion failed with exit code " + exitCode);
        }

        return tempWebPFile;
    }
}
