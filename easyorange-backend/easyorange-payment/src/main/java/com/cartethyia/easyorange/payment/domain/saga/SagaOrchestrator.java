package com.cartethyia.easyorange.payment.domain.saga;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class SagaOrchestrator {

    private final List<SagaStep> steps = new ArrayList<>();
    private final List<Runnable> compensations = new ArrayList<>();

    public SagaOrchestrator addStep(String stepName, Supplier<SagaStepResult<?>> action, Runnable compensation) {
        steps.add(new SagaStep(stepName, action, compensation));
        return this;
    }

    public SagaOrchestrator addStep(String stepName, Runnable action, Runnable compensation) {
        steps.add(new SagaStep(stepName, () -> {
            action.run();
            return SagaStepResult.success(null);
        }, compensation));
        return this;
    }

    public void execute() {
        int executedSteps = 0;
        
        try {
            for (SagaStep step : steps) {
                log.info("Saga step starting: {}", step.name);
                SagaStepResult<?> result = step.action.get();
                
                if (!result.isSuccess()) {
                    log.error("Saga step failed: {}, error: {}", step.name, result.getErrorMessage());
                    throw new SagaExecutionException(step.name, result.getErrorMessage(), result.getCause());
                }
                
                if (step.compensation != null) {
                    compensations.add(0, step.compensation);
                }
                executedSteps++;
                log.info("Saga step completed: {}", step.name);
            }
        } catch (SagaExecutionException e) {
            log.error("Saga execution failed, starting compensation for {} steps", executedSteps);
            compensate();
            throw e;
        } catch (Exception e) {
            log.error("Saga execution failed with unexpected error, starting compensation for {} steps", executedSteps);
            compensate();
            throw new SagaExecutionException("unknown", e.getMessage(), e);
        }
    }

    private void compensate() {
        for (Runnable compensation : compensations) {
            try {
                compensation.run();
            } catch (Exception e) {
                log.error("Compensation failed", e);
            }
        }
    }

    private record SagaStep(String name, Supplier<SagaStepResult<?>> action, Runnable compensation) {}
}
