package com.cartethyia.easyorange.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DDD/CQRS architecture rules")
class ArchitectureRulesTest {

    private static final List<String> DOMAIN_FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import org.springframework.",
            "import com.baomidou.mybatisplus.",
            "import jakarta.servlet.",
            "import com.cartethyia.easyorange.*.controller.",
            "import com.cartethyia.easyorange.*.dto.request.",
            "import com.cartethyia.easyorange.*.dto.vo.",
            "import com.cartethyia.easyorange.*.mapper.",
            "import com.cartethyia.easyorange.*.service.impl."
    );

    private static final Set<String> DOMAIN_IMPORT_ALLOWLIST = Set.of(
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/saga/CreateOrderSaga.java|import org.springframework.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/ProductRepository.java|import com.cartethyia.easyorange.product.infrastructure.persistence.dataobject.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepository.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import org.springframework.data.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import org.springframework.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/valueobject/ImageSet.java|import com.cartethyia.easyorange.product.infrastructure.persistence.dataobject.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/service/ProductReportDomainService.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageRepository.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisOfflineMessageRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisOfflineMessageRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisOfflineMessageRepository.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageSubscriptionRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageSubscriptionRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageSubscriptionRepository.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageTemplateRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageTemplateRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageTemplateRepository.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MessageQueryRepository.java|import com.cartethyia.easyorange.*.dto.request.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MessageQueryRepository.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MybatisMessageQueryRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MybatisMessageQueryRepository.java|import com.cartethyia.easyorange.*.dto.request.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MybatisMessageQueryRepository.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MybatisMessageQueryRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MybatisMessageQueryRepository.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/service/MessageRoutingService.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/service/OfflineMessageStoreService.java|import org.springframework.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/domain/repository/PaymentRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/domain/repository/PaymentQueryRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/aggregate/UserAggregate.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/event/PasswordChangedEventHandler.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/service/AccountTypeDetector.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/service/LoginSecurityService.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/service/LoginSecurityDomainService.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/service/PasswordDomainService.java|import org.springframework."
    );

    private static final Set<String> COMMAND_QUERY_COUPLING_ALLOWLIST = Set.of(
    );

    @Test
    @DisplayName("domain packages do not introduce new framework/persistence/web imports")
    void domainPackages_doNotIntroduceNewForbiddenImports() throws IOException {
        Path backendRoot = backendRoot();
        List<String> violations = new ArrayList<>();

        for (Path javaFile : javaFiles(backendRoot)) {
            String normalized = normalize(backendRoot, javaFile);
            if (!normalized.contains("/domain/")) {
                continue;
            }
            List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                for (String forbiddenPrefix : DOMAIN_FORBIDDEN_IMPORT_PREFIXES) {
                    if (matchesImport(line, forbiddenPrefix) && !isAllowed(normalized, forbiddenPrefix, DOMAIN_IMPORT_ALLOWLIST)) {
                        violations.add(normalized + " -> " + line.trim());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "New forbidden domain imports:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("command handlers do not introduce new query-handler coupling")
    void commandHandlers_doNotIntroduceNewQueryHandlerCoupling() throws IOException {
        Path backendRoot = backendRoot();
        List<String> violations = new ArrayList<>();

        for (Path javaFile : javaFiles(backendRoot)) {
            String normalized = normalize(backendRoot, javaFile);
            if (!normalized.contains("/application/command/") || !normalized.endsWith("CommandHandler.java")) {
                continue;
            }
            List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.contains("QueryHandler") && !isAllowed(normalized, "ProductQueryHandler", COMMAND_QUERY_COUPLING_ALLOWLIST)) {
                    violations.add(normalized + " -> " + line.trim());
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "New command-to-query-handler coupling:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("query handlers do not import command handlers")
    void queryHandlers_doNotImportCommandHandlers() throws IOException {
        Path backendRoot = backendRoot();
        List<String> violations = new ArrayList<>();

        for (Path javaFile : javaFiles(backendRoot)) {
            String normalized = normalize(backendRoot, javaFile);
            if (!(normalized.contains("/application/query/") || normalized.contains("/application/handler/"))) {
                continue;
            }
            List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith("import ") && line.contains("CommandHandler")) {
                    violations.add(normalized + " -> " + line.trim());
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Query handlers importing command handlers:\n" + String.join("\n", violations));
    }

    private static Path backendRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("easyorange-common"))
                    && Files.isDirectory(current.resolve("easyorange-application"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate easyorange-backend module root from " + Path.of("").toAbsolutePath());
    }

    private static List<Path> javaFiles(Path backendRoot) throws IOException {
        try (Stream<Path> stream = Files.walk(backendRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> path.toString().contains("/src/main/java/"))
                    .toList();
        }
    }

    private static boolean matchesImport(String line, String forbiddenPrefix) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("import ")) {
            return false;
        }
        if (!forbiddenPrefix.contains("*")) {
            return trimmed.startsWith(forbiddenPrefix);
        }
        String regex = forbiddenPrefix
                .replace(".", "\\.")
                .replace("*", "[^.]+")
                + ".*";
        return trimmed.matches(regex);
    }

    private static boolean isAllowed(String normalizedPath, String marker, Set<String> allowlist) {
        return allowlist.contains(normalizedPath + "|" + marker);
    }

    private static String normalize(Path backendRoot, Path javaFile) {
        return backendRoot.relativize(javaFile).toString().replace('\\', '/');
    }
}
