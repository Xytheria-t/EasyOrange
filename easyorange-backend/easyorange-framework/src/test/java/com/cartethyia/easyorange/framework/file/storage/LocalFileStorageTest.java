package com.cartethyia.easyorange.framework.file.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageTest {

    private LocalFileStorage storage;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage();
        ReflectionTestUtils.setField(storage, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(storage, "urlPrefix", "/api/file/");
    }

    @Test
    void store_shouldSaveFileAndReturnIdentifier() throws Exception {
        byte[] content = "test image content".getBytes();

        String identifier = storage.store(content, "test.jpg", "image/jpeg");

        assertNotNull(identifier);
        assertTrue(identifier.endsWith(".jpg"));
        assertTrue(identifier.contains("/"));

        byte[] loaded = storage.load(identifier);
        assertArrayEquals(content, loaded);
    }

    @Test
    void store_withoutExtension_shouldDeriveFromContentType() throws Exception {
        byte[] content = "test".getBytes();

        String identifier = storage.store(content, "noext", "image/png");

        assertTrue(identifier.endsWith(".png"));
    }

    @Test
    void store_withUnknownContentType_shouldUseBinExtension() throws Exception {
        byte[] content = "binary data".getBytes();

        String identifier = storage.store(content, "data", "application/octet-stream");

        assertTrue(identifier.endsWith(".bin"));
    }

    @Test
    void load_withStoredFile_shouldReturnContent() throws Exception {
        byte[] content = "hello world".getBytes();
        String identifier = storage.store(content, "hello.txt", "text/plain");

        byte[] loaded = storage.load(identifier);

        assertArrayEquals(content, loaded);
    }

    @Test
    void load_withNonExistentFile_shouldThrow() {
        assertThrows(Exception.class, () -> storage.load("nonexistent/file.txt"));
    }

    @Test
    void delete_shouldRemoveFile() throws Exception {
        byte[] content = "delete me".getBytes();
        String identifier = storage.store(content, "delete.jpg", "image/jpeg");

        assertNotNull(storage.load(identifier));

        storage.delete(identifier);

        assertThrows(Exception.class, () -> storage.load(identifier));
    }

    @Test
    void delete_withNonExistentFile_shouldNotThrow() throws Exception {
        storage.delete("nonexistent/file.txt");
    }

    @Test
    void getUrl_shouldReturnPrefixedPath() {
        String url = storage.getUrl("2026/05/17/uuid.jpg");
        assertEquals("/api/file/2026/05/17/uuid.jpg", url);
    }

    @Test
    void getUrl_shouldNormalizeBackslashes() {
        String url = storage.getUrl("2026\\05\\17\\uuid.jpg");
        assertEquals("/api/file/2026/05/17/uuid.jpg", url);
    }

    @Test
    void supportsDirectUrl_shouldReturnFalse() {
        assertFalse(storage.supportsDirectUrl());
    }

    @Test
    void store_generatesDateBasedDirectoryStructure() throws Exception {
        byte[] content = "test".getBytes();

        String identifier = storage.store(content, "test.jpg", "image/jpeg");

        assertTrue(identifier.contains("/"), "Identifier should contain date-based directory: " + identifier);
    }

    @Test
    void store_multipleCalls_generateDifferentIdentifiers() throws Exception {
        byte[] content = "test".getBytes();

        String id1 = storage.store(content, "test.jpg", "image/jpeg");
        String id2 = storage.store(content, "test.jpg", "image/jpeg");

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2, "Each store should generate a unique identifier");
    }
}
