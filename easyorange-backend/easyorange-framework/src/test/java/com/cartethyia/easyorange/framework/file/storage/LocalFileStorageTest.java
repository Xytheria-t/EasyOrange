package com.cartethyia.easyorange.framework.file.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.cartethyia.easyorange.framework.config.properties.FileUploadProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    private LocalFileStorage storage;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        var properties = new FileUploadProperties();
        properties.setPath(tempDir.toString());
        properties.setUrlPrefix("/api/file/");
        storage = new LocalFileStorage(properties);
        storage.init();
    }

    @Test
    void store_shouldSaveFileAndReturnIdentifier() throws Exception {
        var content = "test image content".getBytes();
        var identifier = storage.store(content, "test.jpg", "image/jpeg");

        assertNotNull(identifier);
        assertTrue(identifier.endsWith(".jpg"));
        assertTrue(identifier.contains("/"));

        var saved = Files.readAllBytes(storage.getPath(identifier));
        assertArrayEquals(content, saved);
    }

    @Test
    void store_withoutExtension_shouldDeriveFromContentType() throws Exception {
        var identifier = storage.store("test".getBytes(), "noext", "image/png");
        assertTrue(identifier.endsWith(".png"));
    }

    @Test
    void store_withUnknownContentType_shouldUseBinExtension() throws Exception {
        var identifier = storage.store("binary data".getBytes(), "data", "application/octet-stream");
        assertTrue(identifier.endsWith(".bin"));
    }

    @Test
    void getPath_withStoredFile_shouldReturnPath() throws Exception {
        var content = "hello world".getBytes();
        var identifier = storage.store(content, "hello.txt", "text/plain");

        var path = storage.getPath(identifier);
        assertTrue(Files.exists(path));
        assertArrayEquals(content, Files.readAllBytes(path));
    }

    @Test
    void getPath_withNonExistentFile_shouldThrow() {
        assertThrows(Exception.class, () -> storage.getPath("../etc/passwd"));
    }

    @Test
    void delete_shouldRemoveFile() throws Exception {
        var content = "delete me".getBytes();
        var identifier = storage.store(content, "delete.jpg", "image/jpeg");

        var path = storage.getPath(identifier);
        assertTrue(Files.exists(path));

        storage.delete(identifier);
        assertFalse(Files.exists(path));
    }

    @Test
    void delete_withNonExistentFile_shouldNotThrow() throws Exception {
        storage.delete("nonexistent/file.txt");
    }

    @Test
    void getUrl_shouldReturnPrefixedPath() {
        assertEquals("/api/file/2026/05/17/uuid.jpg", storage.getUrl("2026/05/17/uuid.jpg"));
    }

    @Test
    void getUrl_shouldNormalizeBackslashes() {
        assertEquals("/api/file/2026/05/17/uuid.jpg", storage.getUrl("2026\\05\\17\\uuid.jpg"));
    }

    @Test
    void store_generatesDateBasedDirectoryStructure() throws Exception {
        var identifier = storage.store("test".getBytes(), "test.jpg", "image/jpeg");
        assertTrue(identifier.contains("/"), "Identifier should contain date-based directory: " + identifier);
    }

    @Test
    void store_multipleCalls_generateDifferentIdentifiers() throws Exception {
        var content = "test".getBytes();
        var id1 = storage.store(content, "test.jpg", "image/jpeg");
        var id2 = storage.store(content, "test.jpg", "image/jpeg");
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2, "Each store should generate a unique identifier");
    }
}
