package com.jobmatcher.server.controller;

import com.jobmatcher.server.model.SuccessResponse;
import com.jobmatcher.server.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.jobmatcher.server.model.ApiConstants.API_VERSION;

@RestController
@RequestMapping(value = API_VERSION + "/users")
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    public FileUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PatchMapping("/{id}/profile_picture")
    public ResponseEntity<SuccessResponse> upload(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        cloudinaryService.uploadImage(UUID.fromString(id), file);
        return ResponseEntity.ok().body(new SuccessResponse(true));
    }
}
