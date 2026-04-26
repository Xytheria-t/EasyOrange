package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.exception.FileException;
import com.cartethyia.easyorange.common.exception.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.exception.InvalidExtensionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FileUtils} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("FileUtils Tests")
class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Extension Tests")
    class ExtensionTests {

        @Test
        @DisplayName("getExtension with valid filename should return extension")
        void getExtension_withValidFilename_returnsExtension() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

            // Act
            String extension = FileUtils.getExtension(file);

            // Assert
            assertThat(extension).isEqualToIgnoringCase("jpg");
        }

        @Test
        @DisplayName("getExtension with no extension should return empty string")
        void getExtension_withNoExtension_returnsEmptyString() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", "testfile", "text/plain", new byte[]{1, 2, 3});

            // Act
            String extension = FileUtils.getExtension(file);

            // Assert
            assertThat(extension).isEmpty();
        }

        @Test
        @DisplayName("getExtension with null filename should extract from content type")
        void getExtension_withNullFilename_extractsFromContentType() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", null, "image/png", new byte[]{1, 2, 3});

            // Act
            String extension = FileUtils.getExtension(file);

            // Assert
            assertThat(extension).isEqualToIgnoringCase("png");
        }

        @Test
        @DisplayName("getExtension with uppercase should return lowercase")
        void getExtension_withUppercase_returnsLowercase() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", "test.JPG", "image/jpeg", new byte[]{1, 2, 3});

            // Act
            String extension = FileUtils.getExtension(file);

            // Assert
            assertThat(extension).isEqualTo("jpg");
        }
    }

    @Nested
    @DisplayName("Base Name Tests")
    class BaseNameTests {

        @Test
        @DisplayName("getBaseName with valid filename should return name without extension")
        void getBaseName_withValidFilename_returnsNameWithoutExtension() {
            // Arrange
            String filename = "test.jpg";

            // Act
            String baseName = FileUtils.getBaseName(filename);

            // Assert
            assertThat(baseName).isEqualTo("test");
        }

        @Test
        @DisplayName("getBaseName with multiple dots should return name before last dot")
        void getBaseName_withMultipleDots_returnsNameBeforeLastDot() {
            // Arrange
            String filename = "archive.tar.gz";

            // Act
            String baseName = FileUtils.getBaseName(filename);

            // Assert
            assertThat(baseName).isEqualTo("archive.tar");
        }

        @Test
        @DisplayName("getBaseName with null should return empty string")
        void getBaseName_withNull_returnsEmptyString() {
            // Act
            String baseName = FileUtils.getBaseName(null);

            // Assert
            assertThat(baseName).isEmpty();
        }

        @Test
        @DisplayName("getBaseName with no extension should return original name")
        void getBaseName_withNoExtension_returnsOriginalName() {
            // Arrange
            String filename = "testfile";

            // Act
            String baseName = FileUtils.getBaseName(filename);

            // Assert
            assertThat(baseName).isEqualTo("testfile");
        }
    }

    @Nested
    @DisplayName("File Size Format Tests")
    class FileSizeFormatTests {

        @Test
        @DisplayName("formatFileSize with bytes should format correctly")
        void formatFileSize_withBytes_formatsCorrectly() {
            // Act
            String formatted = FileUtils.formatFileSize(500);

            // Assert
            assertThat(formatted).isEqualTo("500 B");
        }

        @Test
        @DisplayName("formatFileSize with KB should format correctly")
        void formatFileSize_withKB_formatsCorrectly() {
            // Act
            String formatted = FileUtils.formatFileSize(2048);

            // Assert
            assertThat(formatted).isEqualTo("2.00 KB");
        }

        @Test
        @DisplayName("formatFileSize with MB should format correctly")
        void formatFileSize_withMB_formatsCorrectly() {
            // Act
            String formatted = FileUtils.formatFileSize(5242880);

            // Assert
            assertThat(formatted).isEqualTo("5.00 MB");
        }

        @Test
        @DisplayName("formatFileSize with GB should format correctly")
        void formatFileSize_withGB_formatsCorrectly() {
            // Act
            String formatted = FileUtils.formatFileSize(5368709120L);

            // Assert
            assertThat(formatted).isEqualTo("5.00 GB");
        }
    }

    @Nested
    @DisplayName("Allowed Extension Tests")
    class AllowedExtensionTests {

        @Test
        @DisplayName("isAllowedExtension with matching extension should return true")
        void isAllowedExtension_withMatchingExtension_returnsTrue() {
            // Arrange
            Collection<String> allowed = Set.of("jpg", "png", "gif");

            // Act
            boolean result = FileUtils.isAllowedExtension("jpg", allowed);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAllowedExtension with case insensitive should return true")
        void isAllowedExtension_withCaseInsensitive_returnsTrue() {
            // Arrange
            Collection<String> allowed = Set.of("jpg", "png");

            // Act
            boolean result = FileUtils.isAllowedExtension("JPG", allowed);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAllowedExtension with non-matching should return false")
        void isAllowedExtension_withNonMatching_returnsFalse() {
            // Arrange
            Collection<String> allowed = Set.of("jpg", "png");

            // Act
            boolean result = FileUtils.isAllowedExtension("exe", allowed);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isAllowedExtension with null collection should return true")
        void isAllowedExtension_withNullCollection_returnsTrue() {
            // Act
            boolean result = FileUtils.isAllowedExtension("jpg", null);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAllowedExtension with empty collection should return true")
        void isAllowedExtension_withEmptyCollection_returnsTrue() {
            // Arrange
            Collection<String> allowed = Set.of();

            // Act
            boolean result = FileUtils.isAllowedExtension("jpg", allowed);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Assert Allowed Tests")
    class AssertAllowedTests {

        @Test
        @DisplayName("assertAllowed with valid file should not throw")
        void assertAllowed_withValidFile_shouldNotThrow() {
            // Arrange
            byte[] jpgHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpgHeader);
            Collection<String> allowed = Set.of("jpg", "png");

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> FileUtils.assertAllowed(file, allowed));
        }

        @Test
        @DisplayName("assertAllowed with file size exceeded should throw")
        void assertAllowed_withFileSizeExceeded_shouldThrow() {
            // Arrange
            long oversizedSize = FileUtils.DEFAULT_MAX_SIZE + 1;
            byte[] content = new byte[(int) oversizedSize];
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
            Collection<String> allowed = Set.of("jpg");

            // Act & Assert
            assertThatThrownBy(() -> FileUtils.assertAllowed(file, allowed))
                    .isInstanceOf(FileSizeLimitExceededException.class);
        }

        @Test
        @DisplayName("assertAllowed with invalid extension should throw")
        void assertAllowed_withInvalidExtension_shouldThrow() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", "test.exe", "application/x-executable", new byte[]{1, 2, 3});
            Collection<String> allowed = Set.of("jpg", "png");

            // Act & Assert
            assertThatThrownBy(() -> FileUtils.assertAllowed(file, allowed))
                    .isInstanceOf(InvalidExtensionException.class);
        }

        @Test
        @DisplayName("assertAllowed with mismatched magic number should throw")
        void assertAllowed_withMismatchedMagicNumber_shouldThrow() {
            // Arrange
            byte[] wrongHeader = new byte[]{0x00, 0x00, 0x00, 0x00};
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", wrongHeader);
            Collection<String> allowed = Set.of("jpg");

            // Act & Assert
            assertThatThrownBy(() -> FileUtils.assertAllowed(file, allowed))
                    .isInstanceOf(InvalidExtensionException.class);
        }
    }

    @Nested
    @DisplayName("Generate Filename Tests")
    class GenerateFilenameTests {

        @Test
        @DisplayName("generateUuidFilename should return UUID format")
        void generateUuidFilename_shouldReturnUuidFormat() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

            // Act
            String filename = FileUtils.generateUuidFilename(file);

            // Assert
            assertThat(filename).containsPattern("^\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.jpg$");
        }

        @Test
        @DisplayName("generateTimestampFilename should contain timestamp")
        void generateTimestampFilename_shouldContainTimestamp() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

            // Act
            String filename = FileUtils.generateTimestampFilename(file);

            // Assert
            assertThat(filename).containsPattern("^\\d{4}/\\d{2}/\\d{2}/test_\\d+\\.jpg$");
        }

        @Test
        @DisplayName("generateTimestampFilename with null filename should handle gracefully")
        void generateTimestampFilename_withNullFilename_shouldHandleGracefully() {
            // Arrange
            MultipartFile file = new MockMultipartFile("file", null, "image/jpeg", new byte[]{1, 2, 3});

            // Act & Assert - 当文件名为 null 时，getBaseName 返回空字符串，不会抛异常
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                String result = FileUtils.generateTimestampFilename(file);
                // 结果格式：yyyy/MM/dd/_timestamp.jpg
                assertThat(result).containsPattern("^\\d{4}/\\d{2}/\\d{2}/_\\d+\\.jpg$");
            });
        }
    }

    @Nested
    @DisplayName("Get Absolute File Tests")
    class GetAbsoluteFileTests {

        @Test
        @DisplayName("getAbsoluteFile with valid path should return file")
        void getAbsoluteFile_withValidPath_shouldReturnFile() throws IOException {
            // Arrange
            String baseDir = tempDir.toString();
            String fileName = "test.txt";

            // Act
            File result = FileUtils.getAbsoluteFile(baseDir, fileName);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getParentFile()).exists();
            assertThat(result.getParentFile()).isDirectory();
        }

        @Test
        @DisplayName("getAbsoluteFile with path traversal should throw")
        void getAbsoluteFile_withPathTraversal_shouldThrow() {
            // Arrange
            String baseDir = tempDir.toString();
            String fileName = "../../../etc/passwd";

            // Act & Assert
            assertThatThrownBy(() -> FileUtils.getAbsoluteFile(baseDir, fileName))
                    .isInstanceOf(FileException.class)
                    .hasMessageContaining("非法文件路径");
        }

        @Test
        @DisplayName("getAbsoluteFile with nested path should create directories")
        void getAbsoluteFile_withNestedPath_shouldCreateDirectories() throws IOException {
            // Arrange
            String baseDir = tempDir.toString();
            String fileName = "2024/01/15/test.txt";

            // Act
            File result = FileUtils.getAbsoluteFile(baseDir, fileName);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getParentFile()).exists();
            assertThat(result.getParentFile()).isDirectory();
            assertThat(result.getParentFile().getParentFile()).exists();
            assertThat(result.getParentFile().getParentFile()).isDirectory();
        }
    }

    @Nested
    @DisplayName("Delete File Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("deleteFile with existing file should return true")
        void deleteFile_withExistingFile_shouldReturnTrue() throws IOException {
            // Arrange
            Path file = tempDir.resolve("test.txt");
            Files.writeString(file, "content");

            // Act
            boolean result = FileUtils.deleteFile(file.toString());

            // Assert
            assertThat(result).isTrue();
            assertThat(file).doesNotExist();
        }

        @Test
        @DisplayName("deleteFile with non-existing file should return false")
        void deleteFile_withNonExistingFile_shouldReturnFalse() {
            // Arrange
            String filePath = tempDir.resolve("nonexistent.txt").toString();

            // Act
            boolean result = FileUtils.deleteFile(filePath);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("deleteFile with null should return false")
        void deleteFile_withNull_shouldReturnFalse() {
            // Act
            boolean result = FileUtils.deleteFile(null);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Exists Tests")
    class ExistsTests {

        @Test
        @DisplayName("exists with existing file should return true")
        void exists_withExistingFile_shouldReturnTrue() throws IOException {
            // Arrange
            Path file = tempDir.resolve("test.txt");
            Files.writeString(file, "content");

            // Act
            boolean result = FileUtils.exists(file.toString());

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("exists with non-existing file should return false")
        void exists_withNonExistingFile_shouldReturnFalse() {
            // Arrange
            String filePath = tempDir.resolve("nonexistent.txt").toString();

            // Act
            boolean result = FileUtils.exists(filePath);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("exists with null should return false")
        void exists_withNull_shouldReturnFalse() {
            // Act
            boolean result = FileUtils.exists(null);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Get File Name Tests")
    class GetFileNameTests {

        @Test
        @DisplayName("getFileName with valid path should return filename")
        void getFileName_withValidPath_shouldReturnFilename() {
            // Arrange
            String filePath = "/home/user/documents/test.txt";

            // Act
            String fileName = FileUtils.getFileName(filePath);

            // Assert
            assertThat(fileName).isEqualTo("test.txt");
        }

        @Test
        @DisplayName("getFileName with Windows path should return filename")
        void getFileName_withWindowsPath_shouldReturnFilename() {
            // Arrange
            String filePath = "C:\\Users\\test\\documents\\file.pdf";

            // Act
            String fileName = FileUtils.getFileName(filePath);

            // Assert
            assertThat(fileName).isEqualTo("file.pdf");
        }

        @Test
        @DisplayName("getFileName with null should return empty string")
        void getFileName_withNull_shouldReturnEmptyString() {
            // Act
            String fileName = FileUtils.getFileName(null);

            // Assert
            assertThat(fileName).isEmpty();
        }

        @Test
        @DisplayName("getFileName with empty string should return empty string")
        void getFileName_withEmptyString_shouldReturnEmptyString() {
            // Act
            String fileName = FileUtils.getFileName("");

            // Assert
            assertThat(fileName).isEmpty();
        }

        @Test
        @DisplayName("getFileName with path without separator should return original")
        void getFileName_withPathWithoutSeparator_shouldReturnOriginal() {
            // Arrange
            String filePath = "test.txt";

            // Act
            String fileName = FileUtils.getFileName(filePath);

            // Assert
            assertThat(fileName).isEqualTo("test.txt");
        }
    }

    @Nested
    @DisplayName("Calculate MD5 Tests")
    class CalculateMd5Tests {

        @Test
        @DisplayName("calculateMd5 should return consistent hash")
        void calculateMd5_shouldReturnConsistentHash() throws IOException {
            // Arrange
            byte[] content = "test content".getBytes();
            MultipartFile file1 = new MockMultipartFile("file", "test.txt", "text/plain", content);
            MultipartFile file2 = new MockMultipartFile("file", "test2.txt", "text/plain", content);

            // Act
            String md51 = FileUtils.calculateMd5(file1);
            String md52 = FileUtils.calculateMd5(file2);

            // Assert
            assertThat(md51).isEqualTo(md52);
            assertThat(md51).hasSize(32);
        }
    }
}
