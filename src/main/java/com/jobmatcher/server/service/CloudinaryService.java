package com.jobmatcher.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jobmatcher.server.domain.PortfolioItem;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.exception.UploadFileException;
import com.jobmatcher.server.model.UserRequestDTO;
import com.jobmatcher.server.repository.PortfolioItemRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CloudinaryService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final IUserService userService;
    private final ImageOptimizer imageOptimizer;
    private final IPortfolioItemService portfolioItemService;
    private final PortfolioItemRepository portfolioItemRepository;

    public CloudinaryService(
            Cloudinary cloudinary,
            IUserService userService,
            ImageOptimizer imageOptimizer,
            IPortfolioItemService portfolioItemService, PortfolioItemRepository portfolioItemRepository
    ) {
        this.cloudinary = cloudinary;
        this.userService = userService;
        this.imageOptimizer = imageOptimizer;
        this.portfolioItemService = portfolioItemService;
        this.portfolioItemRepository = portfolioItemRepository;
    }

    @SuppressWarnings("unchecked")
    public void uploadImage(UUID id, MultipartFile file) {
        log.info("Uploading profile picture for user {}", id);

        File optimizedFile = null;
        try {
            // ✅ Validate and transform
            validateFile(file);
            optimizedFile = imageOptimizer.compressAndConvertToWebP(file);

            // Delete old picture if exists
            String oldPictureUrl = userService.getUserById(id).getPictureUrl();
            if (oldPictureUrl != null && !oldPictureUrl.isBlank()) {
                String oldPublicId = extractPublicId(oldPictureUrl);
                if (oldPublicId != null) {
                    cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
                }
            }

            // Use a stable public_id so it overwrites
            String shortId = id.toString().substring(0, 8);
            String publicId = "jobmatcher/users/" + id + "/profile_" + shortId;

            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image",
                    "folder", "jobmatcher/users/" + id
            );

            Map<?, ?> uploadResult = cloudinary.uploader().upload(optimizedFile, options);
            String pictureUrl = (String) uploadResult.get("secure_url");

            // Save to DB
            userService.updateUserById(id, UserRequestDTO.builder()
                    .pictureUrl(pictureUrl.trim())
                    .build());

            log.info("Profile picture uploaded successfully: {}", pictureUrl);

        } catch (IOException e) {
            throw new UploadFileException("Upload to storage failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            cleanupTempFile(optimizedFile);
        }
    }

    @SuppressWarnings("unchecked")
    public void uploadMultipleImages(UUID portfolioItemId, UUID userId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new UploadFileException("No files provided.");
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            File optimizedFile = null;
            try {
                // ✅ Validate and transform
                validateFile(file);
                optimizedFile = imageOptimizer.compressAndConvertToWebP(file);

                // Unique public_id for each file
                String uniqueId = UUID.randomUUID().toString().substring(0, 8);
                String publicId = "jobmatcher/users/" + userId + "/portfolio/image_" + uniqueId;

                Map<String, Object> options = ObjectUtils.asMap(
                        "public_id", publicId,
                        "overwrite", false,
                        "resource_type", "image",
                        "folder", "jobmatcher/users/" + userId + "/portfolio"
                );

                Map<?, ?> uploadResult = cloudinary.uploader().upload(optimizedFile, options);
                String url = (String) uploadResult.get("secure_url");

                uploadedUrls.add(url);
            } catch (IOException e) {
                throw new UploadFileException("Upload to storage failed: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                cleanupTempFile(optimizedFile);
            }
        }

        // Save URLs to portfolio item
        if (!uploadedUrls.isEmpty()) {
            Set<String> sanitizedUrls = uploadedUrls.stream()
                    .map(url -> url != null ? url.trim() : null)
                    .filter(url -> url != null && !url.isEmpty())
                    .collect(Collectors.toSet());
            if (sanitizedUrls.isEmpty()) {
                throw new UploadFileException("All provided image URLs are invalid.");
            }
            portfolioItemService.uploadPortfolioItemImages(portfolioItemId, new ArrayList<>(sanitizedUrls));
        }
        log.info("Uploaded {} images for portfolio item {}", uploadedUrls.size(), portfolioItemId);
    }

    @Transactional
    public void deletePortfolioItemImage(UUID portfolioItemId, String imageUrl) {
        PortfolioItem item = portfolioItemRepository.findByIdWithImages(portfolioItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found"));

        boolean removed = item.getImageUrls().remove(imageUrl);

        if (removed) {
            portfolioItemRepository.save(item);
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                try {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                } catch (Exception e) {
                    log.error("Cloudinary deletion failed for {}", publicId, e);
                }
            } else {
                log.warn("Could not extract Cloudinary public_id from URL: {}", imageUrl);
            }
        } else {
            log.warn("Image URL not found in portfolio item {}: {}", portfolioItemId, imageUrl);
        }
    }

    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;

        try {
            // Remove Cloudinary prefix
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String pathAfterUpload = imageUrl.substring(uploadIndex + "/upload/".length());
            // Remove version if exists (e.g., v1234567890/)
            if (pathAfterUpload.startsWith("v")) {
                int slashIndex = pathAfterUpload.indexOf('/');
                if (slashIndex != -1) {
                    pathAfterUpload = pathAfterUpload.substring(slashIndex + 1);
                }
            }

            // Remove file extension
            int dotIndex = pathAfterUpload.lastIndexOf('.');
            if (dotIndex != -1) {
                pathAfterUpload = pathAfterUpload.substring(0, dotIndex);
            }

            return pathAfterUpload;
        } catch (Exception e) {
            log.error("Failed to extract Cloudinary public_id from URL: {}", imageUrl, e);
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            throw new UnsupportedMediaTypeStatusException(
                    "Unsupported media format. Allowed: tiff, bmp, avif, gif, jpeg, jpg, png, webp");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new UploadFileException("File is too large. Max size is 10MB.");
        }
    }

    private boolean isAllowedType(String type) {
        return type.equals("image/tiff") ||
                type.equals("image/bmp") ||
                type.equals("image/avif") ||
                type.equals("image/gif") ||
                type.equals("image/jpeg") ||
                type.equals("image/jpg") ||
                type.equals("image/png") ||
                type.equals("image/webp");
    }

    private void cleanupTempFile(File file) {
        if (file != null && file.exists() && !file.delete()) {
            log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
        }
    }
}
