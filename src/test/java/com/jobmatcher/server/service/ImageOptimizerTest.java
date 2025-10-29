package com.jobmatcher.server.service;

import net.coobird.thumbnailator.Thumbnails;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageOptimizerTest {

    private final ImageOptimizer imageOptimizer = new ImageOptimizer();

    @Test
    void compressAndConvertToWebP_invalidImage_throwsIOException() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "corrupt.png", "image/png", new byte[]{0, 1, 2}
        );

        // Mock ImageIO.read to throw IOException
        try (MockedStatic<ImageIO> imageIOMock = Mockito.mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.read(any(InputStream.class))).thenThrow(new IOException("bad image"));

            IOException ex = assertThrows(IOException.class, () ->
                    imageOptimizer.compressAndConvertToWebP(multipartFile));

            assertEquals("bad image", ex.getMessage());
        }
    }

    @Test
    void compressAndConvertToWebP_cwebpFails_throwsIOException() throws Exception {
        BufferedImage dummyImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "fail.png", "image/png", new byte[]{1,2,3}
        );

        // Mock ImageIO.read
        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.read(any(InputStream.class))).thenReturn(dummyImage);

            // Mock Thumbnails builder chain
            Thumbnails.Builder<BufferedImage> builderMock = mock(Thumbnails.Builder.class, RETURNS_SELF);
            try (MockedStatic<Thumbnails> thumbnailsMock = mockStatic(Thumbnails.class)) {
                thumbnailsMock.when(() -> Thumbnails.of(dummyImage)).thenReturn(builderMock);
                when(builderMock.size(anyInt(), anyInt())).thenReturn(builderMock);
                when(builderMock.outputFormat(anyString())).thenReturn(builderMock);
                when(builderMock.outputQuality(anyDouble())).thenReturn(builderMock);
                doAnswer(invocation -> {
                    File arg = invocation.getArgument(0);
                    arg.createNewFile();
                    return null;
                }).when(builderMock).toFile(any(File.class));

                // Mock ProcessBuilder construction
                try (var pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                    Process processMock = mock(Process.class);
                    when(processMock.waitFor()).thenReturn(255); // simulate failure
                    when(mock.start()).thenReturn(processMock);
                })) {

                    ImageOptimizer optimizer = new ImageOptimizer();
                    IOException ex = assertThrows(IOException.class,
                            () -> optimizer.compressAndConvertToWebP(multipartFile));
                    assertTrue(ex.getMessage().contains("cwebp conversion failed"));
                }
            }
        }
    }

    @Test
    void compressAndConvertToWebP_success_returnsWebPFile() throws Exception {
        BufferedImage dummyImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{1,2,3}
        );

        ImageOptimizer optimizer = new ImageOptimizer();

        // 1️⃣ Mock ImageIO.read
        try (MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)) {
            imageIOMock.when(() -> ImageIO.read(any(InputStream.class))).thenReturn(dummyImage);

            // 2️⃣ Mock Thumbnails builder chain
            Thumbnails.Builder<BufferedImage> builderMock = mock(Thumbnails.Builder.class, RETURNS_SELF);
            try (MockedStatic<Thumbnails> thumbnailsMock = mockStatic(Thumbnails.class)) {
                thumbnailsMock.when(() -> Thumbnails.of(dummyImage)).thenReturn(builderMock);
                when(builderMock.size(anyInt(), anyInt())).thenReturn(builderMock);
                when(builderMock.outputFormat(anyString())).thenReturn(builderMock);
                when(builderMock.outputQuality(anyDouble())).thenReturn(builderMock);

                doAnswer(invocation -> {
                    File arg = invocation.getArgument(0);
                    arg.createNewFile(); // simulate writing file
                    return null;
                }).when(builderMock).toFile(any(File.class));

                // 3️⃣ Mock ProcessBuilder construction
                try (var pbMock = mockConstruction(ProcessBuilder.class, (mock, context) -> {
                    Process processMock = mock(Process.class);
                    when(processMock.waitFor()).thenReturn(0); // success exit code
                    when(mock.start()).thenReturn(processMock);
                })) {
                    // Run method
                    File result = optimizer.compressAndConvertToWebP(multipartFile);
                    assertNotNull(result);
                    assertTrue(result.getName().endsWith(".webp"));

                    // Clean up
                    result.delete();
                }
            }
        }
    }

}
