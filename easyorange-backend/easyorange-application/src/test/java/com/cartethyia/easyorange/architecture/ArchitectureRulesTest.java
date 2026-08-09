package com.cartethyia.easyorange.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;

/**
 * 架构守卫测试 — 使用 ArchUnit 真实 API（@AnalyzeClasses + @ArchTest），覆盖 DDD/CQRS 分层。
 * <p>
 * 10 条 @ArchTest 规则：
 * <ol>
 *   <li>domain 层白名单准入（onlyDependOnClassesThat，合并原「禁框架/web/DTO」3 项子检查为 1 条）</li>
 *   <li>command handler 禁止依赖 query handler（CQRS 写读分离）</li>
 *   <li>query handler 禁止依赖 command handler（CQRS 读写分离）</li>
 *   <li>业务模块间仅通过 domain.port / domain.valueobject 通信</li>
 *   <li>端口接口必须有适配器实现（以实现关系 isAssignableFrom 判定，替代名字猜测）</li>
 *   <li>禁止 infrastructure/ 包（已废弃，用 adapter/outbound/）</li>
 *   <li>domain/application 禁止反向依赖 adapter 实现（已知技术债由 FreezingArchRule 冻结在
 *       {@code src/test/resources/archunit_store/}，重构移除后自动解除，无需删豁免名单）</li>
 *   <li>controller 禁止直连 mapper（必须经由 application 服务）</li>
 *   <li>禁止 System.out / System.err（复用 ArchUnit GeneralCodingRules）</li>
 *   <li>禁止 e.printStackTrace()（统一 SLF4J）</li>
 * </ol>
 * 除 FreezingArchRule 冻结的已知技术债外无任何白名单 — 新违规直接失败。
 */
@AnalyzeClasses(packages = "com.cartethyia.easyorange", importOptions = ImportOption.DoNotIncludeTests.class)
@DisplayName("DDD/CQRS architecture rules (ArchUnit)")
// @ArchTest 的字段/方法由 ArchUnit JUnit5 扩展通过反射执行，静态分析会误报「未使用」；类级 Suppress 一次消噪。
@SuppressWarnings("unused")
class ArchitectureRulesTest {

    // ==================== Rule 1: Domain 层纯度（白名单准入） ====================

    @ArchTest
    static final ArchRule domain_only_depends_on_allowlist = classes()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "java..",
                    "jakarta.annotation..",
                    "lombok..",
                    "org.slf4j..",
                    "org.jetbrains.annotations..",
                    "com.fasterxml.jackson.annotation..",
                    "com.cartethyia.easyorange.common..",
                    "com.cartethyia.easyorange..domain..")
            .because("domain 层仅允许白名单依赖（JDK/jakarta.annotation/Lombok/Jackson-annotation/SLF4J/"
                    + "common/domain），新增任何框架或分层依赖直接失败");

    // ==================== Rule 2: CQRS — 命令 handler ≠ 查询 handler ====================

    @ArchTest
    static final ArchRule command_handlers_should_not_depend_on_query_handlers = noClasses()
            .that()
            .resideInAPackage("..application.command..")
            .and()
            .haveSimpleNameEndingWith("CommandHandler")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameContaining("QueryHandler")
            .because("CQRS: 命令 handler 禁止依赖查询 handler");

    // ==================== Rule 3: 查询 handler 禁止依赖命令 handler ====================

    @ArchTest
    static final ArchRule query_handlers_should_not_depend_on_command_handlers = noClasses()
            .that()
            .resideInAPackage("..application.query..")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("CommandHandler")
            .because("CQRS: 查询 handler 禁止依赖命令 handler");

    // ==================== Rule 4: 业务模块间仅通过端口通信 ====================

    private static final Set<String> BUSINESS_MODULES = Set.of("order", "product", "message", "favorite");

    @ArchTest
    static final ArchRule business_modules_communicate_only_through_ports = classes()
            .that()
            .resideInAnyPackage(
                    "com.cartethyia.easyorange.order..",
                    "com.cartethyia.easyorange.product..",
                    "com.cartethyia.easyorange.message..",
                    "com.cartethyia.easyorange.favorite..")
            .and()
            .resideOutsideOfPackage("..adapter.outbound.messaging..")
            .should(notDirectlyDependOnOtherBusinessModuleInternals())
            .because("业务模块间仅通过 domain.port / domain.valueobject 通信，禁止直接导入其他模块内部类");

    private static ArchCondition<JavaClass> notDirectlyDependOnOtherBusinessModuleInternals() {
        return new ArchCondition<>("not directly depend on other business modules' internals") {
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
                    if (!targetPackage.contains(".domain.port.") && !targetPackage.contains(".domain.valueobject.")) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getSimpleName() + " -> " + target.getSimpleName() + " (" + targetPackage
                                        + ")"));
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

    @ArchTest
    static void port_interfaces_must_have_adapter_implementations(JavaClasses classes) {
        var missing = new ArrayList<String>();
        for (JavaClass port : classes) {
            if (!port.getPackageName().contains(".domain.port.")
                    || !port.getSimpleName().endsWith("Port")) {
                continue;
            }
            if (PORT_ALLOWLIST.contains(port.getSimpleName())) {
                continue;
            }
            var hasAdapter = classes.stream()
                    .anyMatch(candidate -> !candidate.equals(port)
                            && candidate.getPackageName().contains(".adapter.outbound.")
                            && candidate.getAllClassesSelfIsAssignableTo().contains(port));
            if (!hasAdapter) {
                missing.add(port.getSimpleName());
            }
        }
        assertThat(missing)
                .withFailMessage(
                        () -> "Port interfaces without adapter implementations:\n" + String.join("\n", missing))
                .isEmpty();
    }

    // ==================== Rule 6: 禁止 infrastructure/ 包 ====================

    @ArchTest
    static final ArchRule no_infrastructure_packages = noClasses()
            .should()
            .resideInAPackage("..infrastructure..")
            .because("infrastructure/ 包已废弃，所有实现应放在 adapter/outbound/ 下");

    // ==================== Rule 7: 依赖方向 — 上层不依赖 adapter（技术债冻结） ====================

    // 已知技术债（被 FreezingArchRule 自动冻结在 src/test/resources/archunit_store/，重构后自动解除，
    // 无需删本文件的豁免名单；快照为 archunit 实际报送的违规行，与此处描述互为印证）：
    //   • SearchHistoryBufferAppService  — application 直构 SearchHistoryDO + 注入 SearchHistoryMapper
    //   • ViewCountBatchProcessor        — application 直注 ProductMapper（batchAddViewCounts）
    //   • ProductReportQueryHandler      — application 方法直接返回 adapter.inbound web DTO
    // 规则继续拦截 domain/application 对 adapter 的新增依赖。
    @ArchTest
    static final ArchRule domain_and_application_should_not_depend_on_adapter = FreezingArchRule.freeze(noClasses()
            .that()
            .resideInAnyPackage("..domain..", "..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .because("依赖方向: domain/application 只能依赖端口(domain.port)，禁止反向依赖 adapter 实现"));

    // ==================== Rule 8: Web 层不直连持久层 ====================

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_mappers = noClasses()
            .that()
            .resideInAPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..mapper..")
            .because("Web/controller 层禁止直接依赖 mapper，必须经由 application 服务");

    // ==================== Rule 9: 统一 SLF4J 日志 ====================

    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule no_print_stack_trace = noClasses()
            .should()
            .callMethod(Throwable.class, "printStackTrace")
            .because("禁止 e.printStackTrace() — 统一走 SLF4J(org.slf4j) 日志");
}
