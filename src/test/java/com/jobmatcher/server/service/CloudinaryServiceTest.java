package com.jobmatcher.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import com.jobmatcher.server.domain.PortfolioItem;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.exception.UploadFileException;
import com.jobmatcher.server.model.UserResponseDTO;
import com.jobmatcher.server.repository.PortfolioItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock private Cloudinary cloudinary;
    @Mock private IUserService userService;
    @Mock private ImageOptimizer imageOptimizer;
    @Mock private IPortfolioItemService portfolioItemService;
    @Mock private PortfolioItemRepository portfolioItemRepository;
    @InjectMocks private CloudinaryService service;
    @Mock private MultipartFile multipartFile;
    @Mock private Uploader uploader;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    // ---------- uploadImage tests ----------

    @Test
    void uploadImage_successfulUpload_shouldCallDependencies() throws Exception {
        // Stub content type for validation
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(1024L); // small file

        File optimizedFile = mock(File.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(imageOptimizer.compressAndConvertToWebP(multipartFile)).thenReturn(optimizedFile);
        when(optimizedFile.exists()).thenReturn(true);
        when(optimizedFile.delete()).thenReturn(true);

        UserResponseDTO userResponse = UserResponseDTO.builder()
                .pictureUrl(null)
                .build();
        when(userService.getUserById(userId)).thenReturn(userResponse);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/test.webp");
        when(uploader.upload(eq(optimizedFile), anyMap())).thenReturn(uploadResult);

        service.uploadImage(userId, multipartFile);

        verify(imageOptimizer).compressAndConvertToWebP(multipartFile);
        verify(uploader).upload(eq(optimizedFile), anyMap());
        verify(userService).updateUserById(eq(userId), any());
        verify(optimizedFile).delete();
    }


    @Test
    void uploadImage_invalidFileType_shouldThrow() {
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        assertThrows(UnsupportedMediaTypeStatusException.class,
                () -> service.uploadImage(userId, multipartFile));
    }

    @Test
    void uploadImage_tooLarge_shouldThrow() {
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(20L * 1024 * 1024); // 20MB
        assertThrows(UploadFileException.class,
                () -> service.uploadImage(userId, multipartFile));
    }

    // ---------- uploadMultipleImages tests ----------

    @Test
    void uploadMultipleImages_successfulUpload_shouldCallPortfolioService() throws Exception {
        MultipartFile file1 = mock(MultipartFile.class);
        when(file1.getContentType()).thenReturn("image/png");
        when(file1.getSize()).thenReturn(1024L);

        File optimizedFile = mock(File.class);
        when(imageOptimizer.compressAndConvertToWebP(file1)).thenReturn(optimizedFile);
        when(optimizedFile.exists()).thenReturn(true);
        when(optimizedFile.delete()).thenReturn(true);

        when(cloudinary.uploader()).thenReturn(uploader);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/test.webp");
        when(uploader.upload(eq(optimizedFile), anyMap())).thenReturn(uploadResult);

        service.uploadMultipleImages(UUID.randomUUID(), userId, List.of(file1));

        verify(portfolioItemService).uploadPortfolioItemImages(any(UUID.class), anyList());
        verify(optimizedFile).delete();
    }

    @Test
    void uploadMultipleImages_noFiles_shouldThrow() {
        assertThrows(UploadFileException.class,
                () -> service.uploadMultipleImages(UUID.randomUUID(), userId, Collections.emptyList()));
    }

    @Test
    void uploadMultipleImages_allInvalidUrls_shouldThrow() throws Exception {
        MultipartFile file1 = mock(MultipartFile.class);
        when(file1.getContentType()).thenReturn("image/png");
        when(file1.getSize()).thenReturn(1024L);

        File optimizedFile = mock(File.class);
        when(imageOptimizer.compressAndConvertToWebP(file1)).thenReturn(optimizedFile);
        when(optimizedFile.exists()).thenReturn(true);
        when(optimizedFile.delete()).thenReturn(true);

        when(cloudinary.uploader()).thenReturn(uploader);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "  "); // blank URL
        when(uploader.upload(eq(optimizedFile), anyMap())).thenReturn(uploadResult);

        assertThrows(UploadFileException.class,
                () -> service.uploadMultipleImages(UUID.randomUUID(), userId, List.of(file1)));

        verify(optimizedFile).delete();
    }

    // ---------- deletePortfolioItemImage tests ----------

    @Test
    void deletePortfolioItemImage_existingImage_shouldCallCloudinaryDestroy() throws Exception {
        UUID itemId = UUID.randomUUID();
        PortfolioItem item = new PortfolioItem();
        item.setImageUrls(new HashSet<>(List.of("https://cloudinary.com/upload/v123/test.webp")));

        when(portfolioItemRepository.findByIdWithImages(itemId)).thenReturn(Optional.of(item));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(null);

        service.deletePortfolioItemImage(itemId, "https://cloudinary.com/upload/v123/test.webp");

        verify(portfolioItemRepository).save(item);
        verify(uploader).destroy(anyString(), anyMap());
    }

    @Test
    void deletePortfolioItemImage_nonExistingItem_shouldThrow() {
        UUID itemId = UUID.randomUUID();
        when(portfolioItemRepository.findByIdWithImages(itemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deletePortfolioItemImage(itemId, "https://cloudinary.com/upload/test.webp"));
    }

    @Test
    void deletePortfolioItemImage_urlNotInItem_shouldNotSaveOrDestroy() {
        UUID itemId = UUID.randomUUID();
        PortfolioItem item = new PortfolioItem();
        item.setImageUrls(new HashSet<>(List.of("other_url")));

        when(portfolioItemRepository.findByIdWithImages(itemId)).thenReturn(Optional.of(item));

        service.deletePortfolioItemImage(itemId, "missing_url");

        verify(portfolioItemRepository, never()).save(item);
        verifyNoInteractions(uploader);
    }

    // ------------------ uploadImage: old picture exists ------------------
    @Test
    void uploadImage_oldPictureExists_callsDestroy() throws Exception {
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(1024L);

        File optimizedFile = mock(File.class);
        when(imageOptimizer.compressAndConvertToWebP(multipartFile)).thenReturn(optimizedFile);
        when(optimizedFile.exists()).thenReturn(true);
        when(optimizedFile.delete()).thenReturn(true);

        when(cloudinary.uploader()).thenReturn(uploader);

        UserResponseDTO userResponse = UserResponseDTO.builder()
                .pictureUrl("https://res.cloudinary.com/demo/upload/v123/old_image.webp")
                .build();
        when(userService.getUserById(userId)).thenReturn(userResponse);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/upload/v123/new_image.webp");
        when(uploader.upload(any(File.class), anyMap())).thenReturn(uploadResult);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(null);

        service.uploadImage(userId, multipartFile);

        verify(uploader).destroy("old_image", ObjectUtils.emptyMap());
    }

    // ------------------ uploadImage: IOException ------------------
    @Test
    void uploadImage_ioException_throwsUploadFileException() throws Exception {
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(imageOptimizer.compressAndConvertToWebP(multipartFile)).thenThrow(new IOException("fail"));

        UploadFileException ex = assertThrows(UploadFileException.class,
                () -> service.uploadImage(userId, multipartFile));
        assertTrue(ex.getMessage().contains("Upload to storage failed"));
    }

    // ------------------ uploadImage: InterruptedException ------------------
    @Test
    void uploadImage_interruptedException_throwsRuntimeException() throws Exception {
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(imageOptimizer.compressAndConvertToWebP(multipartFile))
                .thenThrow(new InterruptedException("interrupted"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadImage(userId, multipartFile));
        assertEquals("interrupted", ex.getCause().getMessage());
    }

    // ------------------ uploadMultipleImages: IOException ------------------
    @Test
    void uploadMultipleImages_ioException_throwsUploadFileException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(1024L);
        when(imageOptimizer.compressAndConvertToWebP(file)).thenThrow(new IOException("fail"));

        UploadFileException ex = assertThrows(UploadFileException.class,
                () -> service.uploadMultipleImages(UUID.randomUUID(), userId, List.of(file)));
        assertTrue(ex.getMessage().contains("Upload to storage failed"));
    }

    // ------------------ uploadMultipleImages: InterruptedException ------------------
    @Test
    void uploadMultipleImages_interruptedException_throwsRuntimeException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(1024L);
        when(imageOptimizer.compressAndConvertToWebP(file)).thenThrow(new InterruptedException("interrupted"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadMultipleImages(UUID.randomUUID(), userId, List.of(file)));
        assertEquals("interrupted", ex.getCause().getMessage());
    }

    // ------------------ deletePortfolioItemImage: Cloudinary destroy fails ------------------
    @Test
    void deletePortfolioItemImage_cloudinaryDestroyException_logsError() throws IOException {
        UUID itemId = UUID.randomUUID();
        String url = "https://res.cloudinary.com/demo/upload/v123/image.webp";

        PortfolioItem item = new PortfolioItem();
        item.setImageUrls(new HashSet<>(List.of(url)));
        when(portfolioItemRepository.findByIdWithImages(itemId)).thenReturn(Optional.of(item));
        when(cloudinary.uploader()).thenReturn(uploader);
        doThrow(new RuntimeException("fail")).when(uploader).destroy(anyString(), anyMap());

        // just ensure no exception propagates
        assertDoesNotThrow(() -> service.deletePortfolioItemImage(itemId, url));
        verify(portfolioItemRepository).save(item);
    }

    // ------------------ extractPublicId: exception ------------------
    @Test
    void uploadImage_oldPictureWithBadUrl_shouldSkipDestroy() throws Exception {
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(1024L);

        File optimizedFile = mock(File.class);
        when(imageOptimizer.compressAndConvertToWebP(multipartFile)).thenReturn(optimizedFile);
        when(optimizedFile.exists()).thenReturn(true);
        when(optimizedFile.delete()).thenReturn(true);

        when(cloudinary.uploader()).thenReturn(uploader);

        // Provide a malformed URL that will make extractPublicId fail
        UserResponseDTO userResponse = UserResponseDTO.builder()
                .pictureUrl("malformed_url_without_upload_segment")
                .build();
        when(userService.getUserById(userId)).thenReturn(userResponse);

        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/test.webp");
        when(uploader.upload(eq(optimizedFile), anyMap())).thenReturn(uploadResult);

        // Should succeed without throwing, extractPublicId returns null internally
        assertDoesNotThrow(() -> service.uploadImage(userId, multipartFile));

        // Verify uploader.destroy is never called
        verify(uploader, never()).destroy(anyString(), anyMap());
    }


    // ------------------ cleanupTempFile: file exists but delete fails ------------------
    @Test
    void cleanupTempFile_deleteFails_logsWarning() {
        File file = mock(File.class);
        when(file.exists()).thenReturn(true);
        when(file.delete()).thenReturn(false);

        // invoke private method via reflection
        assertDoesNotThrow(() -> {
            var method = CloudinaryService.class.getDeclaredMethod("cleanupTempFile", File.class);
            method.setAccessible(true);
            method.invoke(service, file);
        });
    }

}
