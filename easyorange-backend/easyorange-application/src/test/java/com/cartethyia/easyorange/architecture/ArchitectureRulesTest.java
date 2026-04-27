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

/**
 * Transitional architecture compliance checks for the DDD/CQRS migration.
 *
 * <p>The allowlist documents current technical debt so new violations fail fast
 * without blocking the incremental refactor on pre-existing package debt.</p>
 */
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
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderReadRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderReadRepository.java|import com.cartethyia.easyorange.*.dto.request.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderReadRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/OrderReadRepository.java|import com.cartethyia.easyorange.*.dto.request.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/MybatisProductRepository.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderReadRepository.java|import com.cartethyia.easyorange.order.dto.request.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderReadRepository.java|import com.cartethyia.easyorange.order.mapper.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderReadRepository.java|import org.springframework.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderRepository.java|import com.cartethyia.easyorange.order.mapper.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/MybatisOrderRepository.java|import org.springframework.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/repository/OrderReadRepository.java|import com.cartethyia.easyorange.order.dto.request.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/domain/repository/PaymentRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/MybatisProductRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/MybatisProductRepository.java|import com.cartethyia.easyorange.product.mapper.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/MybatisProductRepository.java|import org.springframework.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/outbound/persistence/ProductPersistenceMapper.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/outbound/persistence/ProductPersistenceMapper.java|import com.cartethyia.easyorange.product.entity.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/outbound/persistence/ProductPersistenceMapper.java|import com.cartethyia.easyorange.product.mapper.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/adapter/outbound/persistence/ProductPersistenceMapper.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/aggregate/UserAggregate.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/event/PasswordChangedEventHandler.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/service/AccountTypeDetector.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/service/LoginSecurityService.java|import org.springframework.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/outbound/persistence/OrderPersistenceMapper.java|import com.baomidou.mybatisplus.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/outbound/persistence/OrderPersistenceMapper.java|import com.cartethyia.easyorange.order.mapper.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/adapter/outbound/persistence/OrderPersistenceMapper.java|import org.springframework.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/saga/CreateOrderSaga.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/saga/CreateOrderSaga.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/saga/CreateOrderSaga.java|import org.springframework.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/domain/strategy/MockPaymentStrategy.java|import org.springframework.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/ProductAttachmentRepositoryImpl.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/ProductAttachmentRepositoryImpl.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/ProductAttachmentRepositoryImpl.java|import org.springframework.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepository.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import com.baomidou.mybatisplus.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import com.cartethyia.easyorange.*.mapper.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import org.springframework.data.",
            "easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/repository/query/ProductQueryRepositoryImpl.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/MybatisMessageRepository.java|import com.cartethyia.easyorange.*.mapper."
    );

    private static final Set<String> COMMAND_QUERY_COUPLING_ALLOWLIST = Set.of(
            "easyorange-order/src/main/java/com/cartethyia/easyorange/order/application/command/OrderCommandHandler.java|ProductSnapshotPort"
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
