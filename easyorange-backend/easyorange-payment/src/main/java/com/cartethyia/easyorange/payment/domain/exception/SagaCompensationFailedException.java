package com.cartethyia.easyorange.payment.domain.exception;

import java.util.List;

/**
 * Saga 补偿失败异常
 * 当 Saga 补偿操作失败时抛出，包含所有失败的补偿步骤信息
 */
public class SagaCompensationFailedException extends PaymentDomainException {

    private final List<CompensationFailure> failures;

    public SagaCompensationFailedException(List<CompensationFailure> failures) {
        super(buildMessage(failures));
        this.failures = List.copyOf(failures);
    }

    public SagaCompensationFailedException(List<CompensationFailure> failures, Throwable cause) {
        super(buildMessage(failures), cause);
        this.failures = List.copyOf(failures);
    }

    public List<CompensationFailure> getFailures() {
        return failures;
    }

    private static String buildMessage(List<CompensationFailure> failures) {
        if (failures.isEmpty()) {
            return "Saga 补偿失败";
        }
        var sb = new StringBuilder("Saga 补偿失败，以下补偿操作失败：");
        for (var failure : failures) {
            sb.append("\n  - ").append(failure.stepName())
              .append(": ").append(failure.errorMessage());
        }
        return sb.toString();
    }

    /**
     * 补偿失败记录
     */
    public record CompensationFailure(String stepName, String errorMessage, Throwable cause) {
        public CompensationFailure(String stepName, String errorMessage) {
            this(stepName, errorMessage, null);
        }
    }
}