package com.cartethyia.easyorange.payment.domain.saga;

import com.cartethyia.easyorange.payment.domain.exception.SagaCompensationFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("SagaOrchestrator Saga 编排器测试")
class SagaOrchestratorTest {

    // -- helper factories --------------------------------------------------

    private static Supplier<SagaStepResult<?>> successStep() {
        return () -> SagaStepResult.success("ok");
    }

    private static Supplier<SagaStepResult<?>> failingStep(String errorMsg) {
        return () -> SagaStepResult.failure(errorMsg);
    }

    // -- tests -------------------------------------------------------------

    @Nested
    @DisplayName("正常执行")
    class ExecutionSuccessTests {

        @Test
        @DisplayName("单步骤成功执行，补偿被注册但不会被调用")
        void singleStep_success() {
            var executed = new AtomicBoolean(false);
            var compensated = new AtomicBoolean(false);
            var orchestrator = new SagaOrchestrator();

            orchestrator.addStep("step1",
                () -> {
                    executed.set(true);
                    return SagaStepResult.success("ok");
                },
                () -> compensated.set(true));

            orchestrator.execute();

            assertThat(executed).isTrue();
            assertThat(compensated).isFalse();
        }

        @Test
        @DisplayName("多个步骤全部成功，执行顺序正确且无异常")
        void multipleSteps_allSuccess() {
            var executionOrder = new ArrayList<String>();
            var compensationCalls = new ArrayList<String>();
            var orchestrator = new SagaOrchestrator();

            orchestrator
                .addStep("step1",
                    () -> {
                        executionOrder.add("step1");
                        return SagaStepResult.success("ok");
                    },
                    () -> compensationCalls.add("step1"))
                .addStep("step2",
                    () -> {
                        executionOrder.add("step2");
                        return SagaStepResult.success("ok");
                    },
                    () -> compensationCalls.add("step2"))
                .addStep("step3",
                    () -> {
                        executionOrder.add("step3");
                        return SagaStepResult.success("ok");
                    },
                    () -> compensationCalls.add("step3"));

            orchestrator.execute();

            assertThat(executionOrder).containsExactly("step1", "step2", "step3");
            // 全部成功时补偿不会被调用
            assertThat(compensationCalls).isEmpty();
        }

        @Test
        @DisplayName("使用 Runnable 重载的步骤成功执行")
        void addStep_withRunnableOverload() {
            var executed = new AtomicBoolean(false);
            var compensated = new AtomicBoolean(false);
            var orchestrator = new SagaOrchestrator();

            orchestrator.addStep("step1",
                (Runnable) () -> executed.set(true),
                () -> compensated.set(true));

            orchestrator.execute();

            assertThat(executed).isTrue();
            assertThat(compensated).isFalse();
        }

        @Test
        @DisplayName("空步骤列表执行无异常")
        void execute_withEmptySteps() {
            var orchestrator = new SagaOrchestrator();

            orchestrator.execute();

            // 无步骤列表不应抛出任何异常
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DisplayName("步骤失败与补偿")
    class ExecutionFailureTests {

        @Test
        @DisplayName("步骤失败触发已执行步骤的补偿，并抛出 SagaExecutionException")
        void stepFailure_triggersCompensation() {
            var compensated = new AtomicBoolean(false);
            var orchestrator = new SagaOrchestrator();

            orchestrator
                .addStep("step1",
                    successStep(),
                    () -> compensated.set(true))
                .addStep("step2",
                    failingStep("step2 failed"),
                    () -> { });

            var thrown = catchThrowable(() -> orchestrator.execute());
            assertThat(thrown).isInstanceOf(SagaExecutionException.class);
            var exception = (SagaExecutionException) thrown;
            assertThat(exception.getFailedStep()).isEqualTo("step2");
            assertThat(exception.getMessage()).contains("step2 failed");
            assertThat(compensated).isTrue();
        }

        @Test
        @DisplayName("步骤失败且补偿全部成功，不抛出 SagaCompensationFailedException")
        void stepFailure_allCompensationsSucceed() {
            var counter = new AtomicInteger(0);
            var orchestrator = new SagaOrchestrator();

            orchestrator
                .addStep("step1",
                    successStep(),
                    counter::incrementAndGet)
                .addStep("step2",
                    failingStep("step2 failed"),
                    () -> { });

            var thrown = catchThrowable(() -> orchestrator.execute());
            assertThat(thrown).isInstanceOf(SagaExecutionException.class);
            var exception = (SagaExecutionException) thrown;
            // 只抛出 SagaExecutionException，而非 SagaCompensationFailedException
            assertThat(exception.getFailedStep()).isEqualTo("step2");
            // 补偿已被调用
            assertThat(counter.get()).isOne();
        }

        @Test
        @DisplayName("Supplier 抛出非受检异常时补偿被调用，异常包装为 SagaExecutionException")
        void unexpectedException_triggersCompensation() {
            var compensated = new AtomicBoolean(false);
            var orchestrator = new SagaOrchestrator();

            orchestrator
                .addStep("step1",
                    successStep(),
                    () -> compensated.set(true))
                .addStep("step2",
                    () -> { throw new RuntimeException("unexpected boom"); },
                    () -> { });

            var thrown = catchThrowable(() -> orchestrator.execute());
            assertThat(thrown).isInstanceOf(SagaExecutionException.class);
            var exception = (SagaExecutionException) thrown;
            assertThat(exception.getFailedStep()).isEqualTo("unknown");
            assertThat(exception.getMessage()).contains("unexpected boom");
            assertThat(compensated).isTrue();
        }

        @Test
        @DisplayName("首个步骤即失败，补偿列表为空，不产生额外异常")
        void compensation_notCalled_whenNoStepsExecuted() {
            var orchestrator = new SagaOrchestrator();

            orchestrator.addStep("step1",
                failingStep("first step fail"),
                () -> { });

            var thrown = catchThrowable(() -> orchestrator.execute());
            assertThat(thrown).isInstanceOf(SagaExecutionException.class);
            var exception = (SagaExecutionException) thrown;
            assertThat(exception.getFailedStep()).isEqualTo("step1");
        }
    }

    @Nested
    @DisplayName("补偿失败")
    class CompensationFailureTests {

        @Test
        @DisplayName("单个补偿失败时抛出 SagaCompensationFailedException")
        void compensationFailure_collectsError() {
            var orchestrator = new SagaOrchestrator();

            orchestrator
                .addStep("step1",
                    successStep(),
                    () -> { throw new RuntimeException("comp failure"); })
                .addStep("step2",
                    failingStep("step2 failed"),
                    () -> { });

            var thrown = catchThrowable(() -> orchestrator.execute());
            assertThat(thrown).isInstanceOf(SagaCompensationFailedException.class);
            var exception = (SagaCompensationFailedException) thrown;

            assertThat(exception.getFailures()).hasSize(1);

            var failure = exception.getFailures().getFirst();
            assertThat(failure.stepName()).isEqualTo("compensation");
            assertThat(failure.errorMessage()).contains("comp failure");
        }

        @Test
        @DisplayName("多个补偿均失败时收集所有失败信息")
        void multipleCompensationFailures_collected() {
            var orchestrator = new SagaOrchestrator();

            orchestrator
                .addStep("step1",
                    successStep(),
                    () -> { throw new RuntimeException("comp1 fail"); })
                .addStep("step2",
                    successStep(),
                    () -> { throw new RuntimeException("comp2 fail"); })
                .addStep("step3",
                    failingStep("step3 failed"),
                    () -> { });

            var thrown = catchThrowable(() -> orchestrator.execute());
            assertThat(thrown).isInstanceOf(SagaCompensationFailedException.class);
            var exception = (SagaCompensationFailedException) thrown;

            assertThat(exception.getFailures()).hasSize(2);

            // 补偿按 LIFO 顺序执行，先注册的补偿后执行
            assertThat(exception.getFailures().get(0).errorMessage()).contains("comp2 fail");
            assertThat(exception.getFailures().get(1).errorMessage()).contains("comp1 fail");
        }
    }
}
