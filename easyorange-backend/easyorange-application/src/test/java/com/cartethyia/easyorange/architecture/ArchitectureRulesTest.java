package com.cartethyia.easyorange.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 架构守卫测试 — 使用 ArchUnit 1.4.x 真实 API（@AnalyzeClasses + @ArchTest）。
 * <p>
 * 6 项规则守护 DDD/CQRS 分层：
 * <ol>
 *   <li>domain 层零框架依赖（Spring/MyBatis/servlet + controller/dto/mapper/service.impl）</li>
 *   <li>命令 handler 禁止依赖查询 handler（CQRS 写读分离）</li>
 *   <li>查询 handler 禁止依赖命令 handler（CQRS 读写分离）</li>
 *   <li>业务模块间仅通过 domain.port / domain.valueobject 通信</li>
 *   <li>端口接口必须有适配器实现</li>
 *   <li>禁止 infrastructure/ 包（已废弃，用 adapter/outbound/）</li>
 * </ol>
 * 无白名单 — 所有规则必须严格合规。
 */
@AnalyzeClasses(
        packages = "com.cartethyia.easyorange",
        importOptions = ImportOption.DoNotIncludeTests.class
)
@DisplayName("DDD/CQRS architecture rules (ArchUnit)")
class ArchitectureRulesTest {

    // ==================== Rule 1: Domain 层纯度 ====================

    @ArchTest
    static final ArchRule domain_should_not_depend_on_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "com.baomidou..",
                            "jakarta.servlet..",
                            "com.cartethyia.easyorange.framework..")
                    .because("domain 层必须零框架依赖 — 禁止 Spring/MyBatis/servlet/project-framework");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_web_layers =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..controller..",
                            "..mapper..",
                            "..service.impl..")
                    .because("domain 层禁止依赖 adapter/application 层");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_dto_layers =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..dto.request..",
                            "..dto.vo..")
                    .because("domain 层禁止依赖 DTO");

    // ==================== Rule 2: CQRS — 命令 handler ≠ 查询 handler ====================

    @ArchTest
    static final ArchRule command_handlers_should_not_depend_on_query_handlers =
            noClasses().that().resideInAPackage("..application.command..")
                    .and().haveSimpleNameEndingWith("CommandHandler")
                    .should().dependOnClassesThat().haveSimpleNameContaining("QueryHandler")
                    .because("CQRS: 命令 handler 禁止依赖查询 handler");

    // ==================== Rule 3: 查询 handler 禁止依赖命令 handler ====================

    @ArchTest
    static final ArchRule query_handlers_should_not_depend_on_command_handlers =
            noClasses().that().resideInAPackage("..application.query..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("CommandHandler")
                    .because("CQRS: 查询 handler 禁止依赖命令 handler");

    // ==================== Rule 4: 业务模块间仅通过端口通信 ====================

    private static final Set<String> BUSINESS_MODULES = Set.of("order", "product", "message", "favorite");

    @ArchTest
    static final ArchRule business_modules_communicate_only_through_ports =
            classes().that().resideInAnyPackage(
                            "com.cartethyia.easyorange.order..",
                            "com.cartethyia.easyorange.product..",
                            "com.cartethyia.easyorange.message..",
                            "com.cartethyia.easyorange.favorite..")
                    .and().resideOutsideOfPackage("..adapter.outbound.messaging..")
                    .should(notDirectlyDependOnOtherBusinessModuleInternals())
                    .because("业务模块间仅通过 domain.port / domain.valueobject 通信，禁止直接导入其他模块内部类");

    private static ArchCondition<JavaClass> notDirectlyDependOnOtherBusinessModuleInternals() {
        return new ArchCondition<JavaClass>("not directly depend on other business modules' internals") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                var dependentModule = extractModule(javaClass.getPackageName());
                if (dependentModule == null) {
                    return;
                }
                for (Dependency dep : javaClass.getDirectDependenciesFromSelf()) {
                    var target = dep.getTargetClass();
                    if (target == null) {
                        continue;
                    }
                    var targetModule = extractModule(target.getPackageName());
                    if (targetModule == null || targetModule.equals(dependentModule)) {
                        continue;
                    }
                    var targetPackage = target.getPackageName();
                    if (!targetPackage.contains(".domain.port.")
                            && !targetPackage.contains(".domain.valueobject.")) {
                        events.add(SimpleConditionEvent.violated(javaClass,
                                javaClass.getSimpleName() + " -> " + target.getSimpleName()
                                        + " (" + targetPackage + ")"));
                    }
                }
            }

            private String extractModule(String packageName) {
                for (String module : BUSINESS_MODULES) {
                    if (packageName.startsWith("com.cartethyia.easyorange." + module)) {
                        return module;
                    }
                }
                return null;
            }
        };
    }

    // ==================== Rule 5: 端口接口必须有适配器实现 ====================

    private static final Set<String> PORT_ALLOWLIST = Set.of();
    private static final Set<String> PORT_ADAPTER_SUFFIXES = Set.of(
            "Adapter", "Repository", "Store", "Verifier", "Publisher", "Storage", "Impl"
    );

    @ArchTest
    static void port_interfaces_must_have_adapter_implementations(JavaClasses classes) {
        var ports = new ArrayList<JavaClass>();
        var adapters = new ArrayList<JavaClass>();

        for (JavaClass cls : classes) {
            if (cls.getPackageName().contains(".domain.port.") && cls.getSimpleName().endsWith("Port")) {
                ports.add(cls);
            }
            if (cls.getPackageName().contains(".adapter.outbound.")) {
                var name = cls.getSimpleName();
                if (PORT_ADAPTER_SUFFIXES.stream().anyMatch(name::endsWith)) {
                    adapters.add(cls);
                }
            }
        }

        var missing = new ArrayList<String>();
        for (var port : ports) {
            if (PORT_ALLOWLIST.contains(port.getSimpleName())) {
                continue;
            }
            var coreName = port.getSimpleName().replace("Port", "");
            var hasAdapter = adapters.stream()
                    .anyMatch(a -> a.getSimpleName().contains(coreName));
            if (!hasAdapter) {
                missing.add(port.getSimpleName());
            }
        }

        assertThat(missing).withFailMessage(() ->
                "Port interfaces without adapter implementations:\n" + String.join("\n", missing))
                .isEmpty();
    }

    // ==================== Rule 6: 禁止 infrastructure/ 包 ====================

    @ArchTest
    static final ArchRule no_infrastructure_packages =
            noClasses().should().resideInAPackage("..infrastructure..")
                    .because("infrastructure/ 包已废弃，所有实现应放在 adapter/outbound/ 下");
}
