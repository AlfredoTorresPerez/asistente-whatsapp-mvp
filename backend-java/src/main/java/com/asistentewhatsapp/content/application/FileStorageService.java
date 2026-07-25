package com.asistentewhatsapp.content.application;

import java.io.InputStream;
import java.util.Optional;

public interface FileStorageService {

    /**
     * Guarda un archivo y devuelve la ruta relativa donde se almacenó.
     * El nombre del archivo debe ser único (generado por el servicio).
     */
    String store(String businessId, String originalFilename, InputStream inputStream, long contentLength, String contentType);

    /**
     * Guarda un MultipartFile y devuelve la ruta relativa.
     */
    default String store(org.springframework.web.multipart.MultipartFile file, String businessId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacio");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return store(businessId, file.getOriginalFilename(), inputStream, file.getSize(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo", e);
        }
    }

    /**
     * Elimina un archivo por su ruta relativa.
     * No lanza excepción si el archivo no existe.
     */
    void delete(String relativePath);

    /**
     * Verifica si un archivo existe.
     */
    boolean exists(String relativePath);

    /**
     * Devuelve un InputStream para leer el archivo.
     */
    Optional<InputStream> read(String relativePath);

    /**
     * Devuelve la URL pública para acceder al archivo.
     * Para almacenamiento local, puede ser una ruta servida por un controlador.
     */
    String getPublicUrl(String relativePath);

    /**
     * Valida que el archivo sea una imagen permitida (PNG, JPG, JPEG, WebP).
     * Lanza excepción si no es válido.
     */
    void validateImage(String originalFilename, String contentType, InputStream inputStream, long contentLength);

    /**
     * Tamaño máximo permitido en bytes.
     */
    long getMaxFileSize();

    /**
     * Extensiones permitidas.
     */
    String[] getAllowedExtensions();

    /**
     * Tipos MIME permitidos.
     */
    String[] getAllowedMimeTypes();
}