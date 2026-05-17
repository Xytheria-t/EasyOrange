package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.exception.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.exception.InvalidExtensionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileUtils Tests")
class FileUtilsTest {

    @Mock
    private MultipartFile mockFile;

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("assertAllowed")
    class AssertAllowedTests {

        @Test
        @DisplayName("should pass for allowed extension with valid magic number")
        void assertAllowed_withAllowedExtension_shouldPass() {
            // PNG file with valid magic number
            byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            MockMultipartFile file = new MockMultipartFile("file", "test.png",
                    "image/png", pngContent);

            // Should not throw
            FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION);
        }

        @Test
        @DisplayName("should throw for disallowed extension")
        void assertAllowed_withDisallowedExtension_shouldThrow() {
            MockMultipartFile file = new MockMultipartFile("file", "test.exe",
                    "application/x-msdownload", "fake content".getBytes());

            assertThatThrownBy(() -> FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION))
                    .isInstanceOf(InvalidExtensionException.class);
        }

        @Test
        @DisplayName("should throw for file exceeding max size")
        void assertAllowed_withOversizedFile_shouldThrow() {
            // Create a file larger than DEFAULT_MAX_SIZE (50MB)
            byte[] largeContent = new byte[52 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile("file", "test.png",
                    "image/png", largeContent);

            assertThatThrownBy(() -> FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION))
                    .isInstanceOf(FileSizeLimitExceededException.class);
        }

        @Test
        @DisplayName("should skip extension check when allowedExtension is empty")
        void assertAllowed_withEmptyAllowedExtension_shouldPass() {
            byte[] content = "some content".getBytes();
            MockMultipartFile file = new MockMultipartFile("file", "test.exe",
                    "application/x-msdownload", content);

            // Should not throw because allowedExtension is empty
            FileUtils.assertAllowed(file, Set.of());
        }

        @Test
        @DisplayName("should fail magic number check for mismatched content")
        void assertAllowed_withWrongMagicNumber_shouldThrow() {
            // Content that doesn't match PNG magic number but has .png extension
            byte[] fakePng = "This is not a PNG file".getBytes();
            MockMultipartFile file = new MockMultipartFile("file", "test.png",
                    "image/png", fakePng);

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
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
                    "image/jpeg", new byte[0]);

            String ext = FileUtils.getExtension(file);

            assertThat(ext).isEqualTo("jpg");
        }

        @Test
        @DisplayName("should fallback to MIME type when no dot in filename")
        void getExtension_withoutDotInFilename_shouldUseMimeType() {
            MockMultipartFile file = new MockMultipartFile("file", "photo",
                    "image/png", new byte[0]);

            String ext = FileUtils.getExtension(file);

            assertThat(ext).isEqualTo("png");
        }

        @Test
        @DisplayName("should return empty extension for unknown MIME type")
        void getExtension_withUnknownMimeType_shouldReturnEmpty() {
            MockMultipartFile file = new MockMultipartFile("file", "photo",
                    "application/octet-stream", new byte[0]);

            String ext = FileUtils.getExtension(file);

            assertThat(ext).isEqualTo("");
        }

        @Test
        @DisplayName("should return lowercase extension")
        void getExtension_withUpperCaseExtension_shouldReturnLowercase() {
            MockMultipartFile file = new MockMultipartFile("file", "photo.JPG",
                    "image/jpeg", new byte[0]);

            String ext = FileUtils.getExtension(file);

            assertThat(ext).isEqualTo("jpg");
        }
    }

    @Nested
    @DisplayName("formatFileSize")
    class FormatFileSizeTests {

        @Test
        @DisplayName("should format bytes")
        void formatFileSize_withBytes_shouldReturnB() {
            assertThat(FileUtils.formatFileSize(500)).isEqualTo("500 B");
        }

        @Test
        @DisplayName("should format zero bytes")
        void formatFileSize_withZero_shouldReturnB() {
            assertThat(FileUtils.formatFileSize(0)).isEqualTo("0 B");
        }

        @Test
        @DisplayName("should format KB")
        void formatFileSize_withKB_shouldReturnKB() {
            assertThat(FileUtils.formatFileSize(2048)).isEqualTo("2.00 KB");
        }

        @Test
        @DisplayName("should format MB")
        void formatFileSize_withMB_shouldReturnMB() {
            assertThat(FileUtils.formatFileSize(5 * 1024 * 1024)).isEqualTo("5.00 MB");
        }

        @Test
        @DisplayName("should format GB")
        void formatFileSize_withGB_shouldReturnGB() {
            assertThat(FileUtils.formatFileSize(2L * 1024 * 1024 * 1024)).isEqualTo("2.00 GB");
        }

        @Test
        @DisplayName("should format exactly 1 KB")
        void formatFileSize_withExactly1KB_shouldReturnKB() {
            assertThat(FileUtils.formatFileSize(1024)).isEqualTo("1.00 KB");
        }
    }

    @Nested
    @DisplayName("generateUuidFilename")
    class GenerateUuidFilenameTests {

        @Test
        @DisplayName("should generate UUID-based filename with extension")
        void generateUuidFilename_shouldReturnUuidFilename() {
            MockMultipartFile file = new MockMultipartFile("file", "photo.jpg",
                    "image/jpeg", new byte[0]);

            String filename = FileUtils.generateUuidFilename(file);

            assertThat(filename).endsWith(".jpg");
            // Should contain date path components
            assertThat(filename).contains("/");
            // UUID part should be 32 hex chars (no dashes)
            String uuidPart = filename.substring(filename.lastIndexOf("/") + 1, filename.lastIndexOf("."));
            assertThat(uuidPart).hasSize(32);
            assertThat(uuidPart).matches("[0-9a-f]+");
        }
    }

    @Nested
    @DisplayName("calculateMd5")
    class CalculateMd5Tests {

        @Test
        @DisplayName("should calculate MD5 hash of file content")
        void calculateMd5_shouldReturnHexHash() throws IOException {
            String content = "test content";
            MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                    "text/plain", content.getBytes(StandardCharsets.UTF_8));

            String md5 = FileUtils.calculateMd5(file);

            assertThat(md5).isNotBlank();
            assertThat(md5).hasSize(32);
            assertThat(md5).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("should produce same hash for same content")
        void calculateMd5_withSameContent_shouldProduceSameHash() throws IOException {
            String content = "test content";
            MockMultipartFile file1 = new MockMultipartFile("file", "test1.txt",
                    "text/plain", content.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file2 = new MockMultipartFile("file", "test2.txt",
                    "text/plain", content.getBytes(StandardCharsets.UTF_8));

            String md5_1 = FileUtils.calculateMd5(file1);
            String md5_2 = FileUtils.calculateMd5(file2);

            assertThat(md5_1).isEqualTo(md5_2);
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

            boolean result = FileUtils.deleteFile(testFile.toString());

            assertThat(result).isTrue();
            assertThat(Files.exists(testFile)).isFalse();
        }

        @Test
        @DisplayName("should return false when file does not exist")
        void deleteFile_withNonExistentFile_shouldReturnFalse() {
            boolean result = FileUtils.deleteFile("/nonexistent/path/file.txt");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null path")
        void deleteFile_withNullPath_shouldReturnFalse() {
            boolean result = FileUtils.deleteFile(null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isAllowedExtension")
    class IsAllowedExtensionTests {

        @Test
        @DisplayName("should return true for allowed extension")
        void isAllowedExtension_withAllowedExt_shouldReturnTrue() {
            boolean result = FileUtils.isAllowedExtension("jpg", FileUtils.DEFAULT_ALLOWED_EXTENSION);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for allowed extension with different case")
        void isAllowedExtension_withCaseInsensitive_shouldReturnTrue() {
            boolean result = FileUtils.isAllowedExtension("JPG", FileUtils.DEFAULT_ALLOWED_EXTENSION);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for disallowed extension")
        void isAllowedExtension_withDisallowedExt_shouldReturnFalse() {
            boolean result = FileUtils.isAllowedExtension("exe", FileUtils.DEFAULT_ALLOWED_EXTENSION);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true for any extension when collection is empty")
        void isAllowedExtension_withEmptyCollection_shouldReturnTrue() {
            boolean result = FileUtils.isAllowedExtension("exe", Set.of());

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getBaseName")
    class GetBaseNameTests {

        @Test
        @DisplayName("should extract base name from filename")
        void getBaseName_withDot_shouldReturnBaseName() {
            assertThat(FileUtils.getBaseName("photo.jpg")).isEqualTo("photo");
        }

        @Test
        @DisplayName("should return whole string when no dot")
        void getBaseName_withoutDot_shouldReturnFullName() {
            assertThat(FileUtils.getBaseName("photo")).isEqualTo("photo");
        }

        @Test
        @DisplayName("should return empty for null")
        void getBaseName_withNull_shouldReturnEmpty() {
            assertThat(FileUtils.getBaseName(null)).isEqualTo("");
        }

        @Test
        @DisplayName("should handle multiple dots")
        void getBaseName_withMultipleDots_shouldReturnFirstPart() {
            assertThat(FileUtils.getBaseName("archive.tar.gz")).isEqualTo("archive.tar");
        }
    }

    @Nested
    @DisplayName("exists")
    class ExistsTests {

        @Test
        @DisplayName("should return true for existing file")
        void exists_withExistingFile_shouldReturnTrue() throws IOException {
            Path testFile = tempDir.resolve("existing.txt");
            Files.writeString(testFile, "content");

            boolean result = FileUtils.exists(testFile.toString());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existing file")
        void exists_withNonExistingFile_shouldReturnFalse() {
            boolean result = FileUtils.exists("/nonexistent/path/file.txt");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null path")
        void exists_withNullPath_shouldReturnFalse() {
            boolean result = FileUtils.exists(null);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getFileName")
    class GetFileNameTests {

        @Test
        @DisplayName("should extract filename from path with forward slash")
        void getFileName_withForwardSlash_shouldReturnFilename() {
            assertThat(FileUtils.getFileName("/path/to/file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("should extract filename from path with backslash")
        void getFileName_withBackslash_shouldReturnFilename() {
            assertThat(FileUtils.getFileName("C:\\path\\to\\file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("should return input when no separator")
        void getFileName_withNoSeparator_shouldReturnSame() {
            assertThat(FileUtils.getFileName("file.txt")).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("should return empty for null")
        void getFileName_withNull_shouldReturnEmpty() {
            assertThat(FileUtils.getFileName(null)).isEqualTo("");
        }

        @Test
        @DisplayName("should return empty for empty string")
        void getFileName_withEmpty_shouldReturnEmpty() {
            assertThat(FileUtils.getFileName("")).isEqualTo("");
        }
    }
}
