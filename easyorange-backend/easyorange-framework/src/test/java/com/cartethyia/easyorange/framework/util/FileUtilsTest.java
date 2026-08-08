package com.cartethyia.easyorange.framework.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.file.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.exception.file.InvalidExtensionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("FileUtils Tests")
class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("assertAllowed")
    class AssertAllowedTests {

        @Test
        @DisplayName("should pass for allowed extension with valid magic number")
        void assertAllowed_withAllowedExtension_shouldPass() {
            byte[] pngContent = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngContent);

            FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION);
        }

        @Test
        @DisplayName("should throw for disallowed extension")
        void assertAllowed_withDisallowedExtension_shouldThrow() {
            MockMultipartFile file =
                    new MockMultipartFile("file", "test.exe", "application/x-msdownload", "fake content".getBytes());

            assertThatThrownBy(() -> FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION))
                    .isInstanceOf(InvalidExtensionException.class);
        }

        @Test
        @DisplayName("should throw for file exceeding max size")
        void assertAllowed_withOversizedFile_shouldThrow() {
            byte[] largeContent = new byte[52 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", largeContent);

            assertThatThrownBy(() -> FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION))
                    .isInstanceOf(FileSizeLimitExceededException.class);
        }

        @Test
        @DisplayName("should skip extension check when allowedExtension is empty")
        void assertAllowed_withEmptyAllowedExtension_shouldPass() {
            byte[] content = "some content".getBytes();
            MockMultipartFile file = new MockMultipartFile("file", "test.exe", "application/x-msdownload", content);

            FileUtils.assertAllowed(file, Set.of());
        }

        @Test
        @DisplayName("should fail magic number check for mismatched content")
        void assertAllowed_withWrongMagicNumber_shouldThrow() {
            byte[] fakePng = "This is not a PNG file".getBytes();
            MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", fakePng);

            assertThatThrownBy(() -> FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION))
                    .isInstanceOf(InvalidExtensionException.class);
        }
    }

    @Nested
    @DisplayName("getExtension")
    class GetExtensionTests {

        @Test
        @DisplayName("should extract extension from filename")
        void getExtension_withValidFilename_shouldReturnExtension() {
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

            assertThat(FileUtils.getExtension(file)).isEqualTo("jpg");
        }

        @Test
        @DisplayName("should fallback to MIME type when no dot in filename")
        void getExtension_withoutDotInFilename_shouldUseMimeType() {
            MockMultipartFile file = new MockMultipartFile("file", "photo", "image/png", new byte[0]);

            assertThat(FileUtils.getExtension(file)).isEqualTo("png");
        }

        @Test
        @DisplayName("should return empty extension for unknown MIME type")
        void getExtension_withUnknownMimeType_shouldReturnEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "photo", "application/octet-stream", new byte[0]);

            assertThat(FileUtils.getExtension(file)).isEqualTo("");
        }

        @Test
        @DisplayName("should return lowercase extension")
        void getExtension_withUpperCaseExtension_shouldReturnLowercase() {
            MockMultipartFile file = new MockMultipartFile("file", "photo.JPG", "image/jpeg", new byte[0]);

            assertThat(FileUtils.getExtension(file)).isEqualTo("jpg");
        }
    }

    @Nested
    @DisplayName("deleteFile")
    class DeleteFileTests {

        @Test
        @DisplayName("should return true when file is deleted")
        void deleteFile_withExistingFile_shouldReturnTrue() throws IOException {
            Path testFile = tempDir.resolve("test.txt");
            Files.writeString(testFile, "content");

            assertThat(FileUtils.deleteFile(testFile.toString())).isTrue();
            assertThat(Files.exists(testFile)).isFalse();
        }

        @Test
        @DisplayName("should return false when file does not exist")
        void deleteFile_withNonExistentFile_shouldReturnFalse() {
            assertThat(FileUtils.deleteFile("/nonexistent/path/file.txt")).isFalse();
        }

        @Test
        @DisplayName("should return false for null path")
        void deleteFile_withNullPath_shouldReturnFalse() {
            assertThat(FileUtils.deleteFile(null)).isFalse();
        }
    }
}
