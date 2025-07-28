package com.jobmatcher.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jobmatcher.server.exception.UploadFileException;
import com.jobmatcher.server.model.UserRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CloudinaryService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final IUserService userService;
    private final ImageOptimizer imageOptimizer;

    public CloudinaryService(Cloudinary cloudinary, IUserService userService, ImageOptimizer imageOptimizer) {
        this.cloudinary = cloudinary;
        this.userService = userService;
        this.imageOptimizer = imageOptimizer;
    }

    public void uploadFile(String id, MultipartFile file) {
        log.info("Uploading file for user {} to Cloudinary", id);

        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/tiff") ||
                        contentType.equals("image/bmp") ||
                        contentType.equals("image/avif") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/webp")
                )) {
            throw new UnsupportedMediaTypeStatusException("Unsupported media format. Allowed: tiff, bmp, avif, gif, jpeg, jpg, png, webp");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new UploadFileException("File is too large. Max size is 10MB.");
        }
        File optimizedFile = null;
        try {
            String oldPictureUrl = userService.getUserById(UUID.fromString(id)).getPictureUrl();
            if (oldPictureUrl != null && !oldPictureUrl.isBlank()) {
                String oldPublicId = extractPublicId(oldPictureUrl);
                if (oldPublicId != null) {
                    cloudinary.uploader().destroy(oldPublicId, ObjectUtils.emptyMap());
                }
            }

            if (id == null || id.length() < 8) {
                throw new UploadFileException("Invalid user ID");
            }
            String shortId = id.substring(0, 8);
            String publicId = "jobmatcher/users/" + id + "/profile_" + shortId;

            optimizedFile = imageOptimizer.compressAndConvertToWebP(file);

            @SuppressWarnings("unchecked")
            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image",
                    "folder", "jobmatcher/users/" + id
            );
            Map<?, ?> uploadResult = cloudinary.uploader().upload(optimizedFile, options);
            String pictureUrl = (String) uploadResult.get("secure_url");

            log.info("File uploaded successfully for user {}: {}", id, pictureUrl);

            UserRequestDTO request = UserRequestDTO.builder()
                    .pictureUrl(pictureUrl.trim())
                    .build();

            userService.updateUserById(id, request);
        } catch (IOException e) {
            log.error("IO Exception - Failed to upload image for user {}", id, e);
            throw new UploadFileException("Upload to Cloudinary failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unknown Exception - Failed to upload image for user {}", id, e);
            throw new UploadFileException("Upload to Cloudinary failed: " + e.getMessage());
        } finally {
            if (optimizedFile != null && optimizedFile.exists() && !optimizedFile.delete()) {
                log.warn("Failed to delete temporary file: {}", optimizedFile.getAbsolutePath());
            }
        }
    }

    private String extractPublicId(String url) {
        try {
            String[] parts = url.split("/upload/");
            String path = parts[1];
            path = path.split("\\?")[0];             // remove query params
            path = path.replaceFirst("\\.[^.]+$", ""); // remove file extension
            return path;
        } catch (Exception e) {
            log.warn("Failed to extract public_id from URL: {}", url, e);
            return null;
        }
    }
}