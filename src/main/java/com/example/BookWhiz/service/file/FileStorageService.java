package com.example.BookWhiz.service.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads/posters}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    // ── Store a poster file ───────────────────────────────────
    public String storePoster(MultipartFile file) throws IOException {

        // Validate
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct.toLowerCase()))
            throw new IllegalArgumentException("Only JPEG, PNG, WebP, GIF images are allowed");

        if (file.getSize() > MAX_BYTES)
            throw new IllegalArgumentException("File exceeds 5 MB limit");

        // Build safe unique filename
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "img";
        String ext      = original.contains(".")
            ? original.substring(original.lastIndexOf(".")).toLowerCase() : ".jpg";
        if (!Set.of(".jpg",".jpeg",".png",".webp",".gif").contains(ext)) ext = ".jpg";

        String fileName  = UUID.randomUUID().toString().replace("-", "") + ext;

        // Create directory & save
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        // Return public URL the browser can load
        return baseUrl + "/uploads/posters/" + fileName;
    }

    // ── Delete old poster (best-effort) ───────────────────────
    public void deletePoster(String posterUrl) {
        if (posterUrl == null || posterUrl.isBlank()) return;
        try {
            String name = posterUrl.substring(posterUrl.lastIndexOf("/") + 1);
            Files.deleteIfExists(Paths.get(uploadDir).toAbsolutePath().normalize().resolve(name));
        } catch (Exception ignored) {}
    }
}