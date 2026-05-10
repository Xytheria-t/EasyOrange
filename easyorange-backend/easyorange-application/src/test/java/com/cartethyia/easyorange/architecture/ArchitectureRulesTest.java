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
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MessageQueryRepository.java|import com.cartethyia.easyorange.*.dto.request.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/repository/query/MessageQueryRepository.java|import com.cartethyia.easyorange.*.dto.vo.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/service/MessageRoutingService.java|import org.springframework.",
            "easyorange-message/src/main/java/com/cartethyia/easyorange/message/domain/service/OfflineMessageStoreService.java|import org.springframework.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/infrastructure/security/CallbackSignatureVerifier.java|import org.springframework.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/adapter/outbound/persistence/MybatisPaymentRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/adapter/outbound/persistence/MybatisPaymentRepository.java|import org.springframework.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/adapter/outbound/persistence/JdbcDomainEventStore.java|import com.baomidou.mybatisplus.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/adapter/outbound/persistence/JdbcDomainEventStore.java|import org.springframework.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/adapter/outbound/persistence/MybatisIdempotencyKeyRepository.java|import com.baomidou.mybatisplus.",
            "easyorange-payment/src/main/java/com/cartethyia/easyorange/payment/adapter/outbound/persistence/MybatisIdempotencyKeyRepository.java|import org.springframework.",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/UserStatus.java|import com.baomidou.mybatisplus.annotation.EnumValue",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/UserStatus.java|import com.baomidou.mybatisplus.annotation.IEnum",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/LoginMethod.java|import com.baomidou.mybatisplus.annotation.EnumValue",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/LoginMethod.java|import com.baomidou.mybatisplus.annotation.IEnum",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/UserType.java|import com.baomidou.mybatisplus.annotation.EnumValue",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/UserType.java|import com.baomidou.mybatisplus.annotation.IEnum",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/Sex.java|import com.baomidou.mybatisplus.annotation.EnumValue",
            "easyorange-user/src/main/java/com/cartethyia/easyorange/user/domain/enums/Sex.java|import com.baomidou.mybatisplus.annotation.IEnum"
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

    @Test
    @DisplayName("business modules do not directly import other business modules domain classes")
    void businessModules_doNotDirectlyImportOtherDomainClasses() throws IOException {
        Path backendRoot = backendRoot();
        List<String> violations = new ArrayList<>();

        Set<String> businessModules = Set.of("easyorange-order", "easyorange-product", "easyorange-message", "easyorange-favorite");

        for (Path javaFile : javaFiles(backendRoot)) {
            String normalized = normalize(backendRoot, javaFile);

            String currentModule = businessModules.stream()
                    .filter(normalized::startsWith)
                    .findFirst()
                    .orElse(null);

            if (currentModule == null) {
                continue;
            }

            if (normalized.contains("/adapter/outbound/messaging/") || normalized.contains("/infrastructure/acl/")) {
                continue;
            }

            List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                for (String otherModule : businessModules) {
                    if (otherModule.equals(currentModule)) {
                        continue;
                    }
                    String forbiddenImport = "import com.cartethyia.easyorange." + otherModule.replace("easyorange-", "") + ".";
                    if (line.startsWith(forbiddenImport)) {
                        if (!line.contains(".domain.port.output.") && !line.contains(".domain.valueobject.")) {
                            violations.add(normalized + " -> " + line.trim());
                        }
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Business modules directly importing other domain classes:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("port interfaces have adapter implementations in application module")
    void portInterfaces_haveAdapterImplementations() throws IOException {
        Path backendRoot = backendRoot();
        List<String> portInterfaces = new ArrayList<>();
        List<String> adapterImplementations = new ArrayList<>();

        for (Path javaFile : javaFiles(backendRoot)) {
            String normalized = normalize(backendRoot, javaFile);

            if (normalized.contains("/domain/port/output/") && normalized.endsWith("Port.java")) {
                String portName = javaFile.getFileName().toString().replace(".java", "");
                portInterfaces.add(portName);
            }

            if (normalized.contains("/adapter/outbound/") && normalized.endsWith("Adapter.java")) {
                String adapterName = javaFile.getFileName().toString().replace(".java", "");
                adapterImplementations.add(adapterName);
            }
        }

        List<String> missingAdapters = new ArrayList<>();
        for (String port : portInterfaces) {
            String expectedAdapter = port.replace("Port", "Adapter");
            boolean hasAdapter = adapterImplementations.stream()
                    .anyMatch(adapter -> adapter.contains(expectedAdapter.replace("Port", "")));
            if (!hasAdapter) {
                missingAdapters.add(port);
            }
        }

        assertTrue(missingAdapters.isEmpty(), () -> "Port interfaces without adapter implementations:\n" + String.join("\n", missingAdapters));
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
