package com.example.quick;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/files")
public class UploadController {
    private final Path root;
    public UploadController(@Value("${app.upload-dir}") String dir) { root = Path.of(dir).toAbsolutePath(); }
    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("空文件");
        Files.createDirectories(root);
        String id = UUID.randomUUID().toString();
        try (var input = file.getInputStream()) { Files.copy(input, root.resolve(id)); }
        return ResponseEntity.status(201).body(Map.of("fileId", id));
    }
}
