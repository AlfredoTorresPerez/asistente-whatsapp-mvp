package com.asistentewhatsapp.content.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path storageRoot;
    private final long maxFileSize;
    private final String[] allowedExtensions = { "png", "jpg", "jpeg", "webp" };
    private final String[] allowedMimeTypes = { "image/png", "image/jpeg", "image/webp" };

    public LocalFileStorageService(
            @Value("${app.content.storage.root:./storage/content}") String storageRoot,
            @Value("${app.content.storage.max-file-size:5242880}") long maxFileSize) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento: " + this.storageRoot, e);
        }
    }

    @Override
    public String store(String businessId, String originalFilename, InputStream inputStream, long contentLength, String contentType) {
        validateImage(originalFilename, contentType, inputStream, contentLength);

        String extension = getExtension(originalFilename);
        String filename = UUID.randomUUID() + "." + extension;
        Path businessDir = storageRoot.resolve(businessId);
        try {
            Files.createDirectories(businessDir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio del negocio: " + businessDir, e);
        }

        Path targetPath = businessDir.resolve(filename);
        try {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo: " + targetPath, e);
        }

        return businessId + "/" + filename;
    }

    @Override
    public String store(org.springframework.web.multipart.MultipartFile file, String businessId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacio");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return store(businessId, file.getOriginalFilename(), inputStream, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo", e);
        }
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path targetPath = storageRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new SecurityException("Intento de path traversal detectado: " + relativePath);
        }
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar el archivo: " + targetPath, e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        Path targetPath = storageRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            return false;
        }
        return Files.exists(targetPath);
    }

    @Override
    public Optional<InputStream> read(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }
        Path targetPath = storageRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.newInputStream(targetPath));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public String getPublicUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        return "/api/v1/content-items/media/" + relativePath;
    }

    @Override
    public void validateImage(String originalFilename, String contentType, InputStream inputStream, long contentLength) {
        if (contentLength > maxFileSize) {
            throw new IllegalArgumentException("El archivo supera el tamaño maximo permitido: " + (maxFileSize / 1024 / 1024) + " MB");
        }
        if (contentLength == 0) {
            throw new IllegalArgumentException("El archivo esta vacio");
        }

        String extension = getExtension(originalFilename).toLowerCase();
        if (!Arrays.asList(allowedExtensions).contains(extension)) {
            throw new IllegalArgumentException("Extension no permitida: " + extension + ". Permitidas: " + Arrays.toString(allowedExtensions));
        }

        if (contentType != null && !Arrays.asList(allowedMimeTypes).contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Tipo MIME no permitido: " + contentType + ". Permitidos: " + Arrays.toString(allowedMimeTypes));
        }

        try {
            byte[] header = inputStream.readNBytes(12);
            InputStream newStream = new ByteArrayInputStream(header);
            BufferedImage image = ImageIO.read(newStream);
            if (image == null) {
                throw new IllegalArgumentException("El archivo no es una imagen valida o esta corrupto");
            }
            inputStream.reset();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo validar el contenido de la imagen", e);
        }
    }

    @Override
    public long getMaxFileSize() {
        return maxFileSize;
    }

    @Override
    public String[] getAllowedExtensions() {
        return allowedExtensions.clone();
    }

    @Override
    public String[] getAllowedMimeTypes() {
        return allowedMimeTypes.clone();
    }

    private String getExtension(String filename) {
        String cleanName = StringUtils.getFilename(filename);
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < cleanName.length() - 1) {
            return cleanName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}