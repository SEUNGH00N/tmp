package com.example.excelimport.storage;

import com.example.excelimport.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final StorageProperties storageProperties;

    public FileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public StoredFile store(MultipartFile multipartFile, UUID tenantId) {
        String extension = extensionOf(multipartFile.getOriginalFilename());
        if (!"csv".equals(extension) && !"xlsx".equals(extension)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "https://example.com/problems/unsupported-format",
                    "Unsupported Media Type",
                    "only csv and xlsx are supported");
        }

        try {
            Path root = Path.of(storageProperties.getRoot());
            Files.createDirectories(root);
            Path tempFile = Files.createTempFile(root, "upload-", ".tmp");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(multipartFile.getInputStream(), digest)) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String sha = HexFormat.of().formatHex(digest.digest());
            String prefix = sha.substring(0, 8);
            Path targetDir = root.resolve(tenantId.toString()).resolve(prefix);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(sha + "." + extension);

            if (!Files.exists(target)) {
                try {
                    Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                Files.deleteIfExists(tempFile);
            }

            return new StoredFile(target.toAbsolutePath().toString(), extension);
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "https://example.com/problems/storage-failed",
                    "Storage Failed",
                    ex.getMessage());
        }
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String fileUri, String extension) {
    }
}
