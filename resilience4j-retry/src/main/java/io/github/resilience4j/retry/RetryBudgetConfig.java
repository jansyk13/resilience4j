package io.github.resilience4j.retry;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryBudgetConfig implements Config{
    private static final int DEFAULT_BUFFER_SIZE = 100;
    private static final double DEFAULT_RETRY_THRESHOLD = 0.2;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_WAIT_DURATION = 500;

    private final int bufferSize;
    private final double retryThreshold;
    private final int maxAttempts;
    private final IntervalFunction intervalFunction;
    private final Predicate<Throwable> exceptionPredicate;

    public RetryBudgetConfig(int bufferSize, double retryThreshold, int maxAttempts,
                             IntervalFunction intervalFunction, Predicate<Throwable> exceptionPredicate) {
        this.bufferSize = bufferSize;
        this.retryThreshold = retryThreshold;
        this.maxAttempts = maxAttempts;
        this.intervalFunction = intervalFunction;
        this.exceptionPredicate = exceptionPredicate;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public double getRetryThreshold() {
        return retryThreshold;
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public Function<Integer, Long> getIntervalFunction() {
        return intervalFunction;
    }

    @Override
    public Predicate<Throwable> getExceptionPredicate() {
        return exceptionPredicate;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static RetryBudgetConfig ofDefaults() {
        return new Builder().build();
    }

    public static class Builder {
        private int bufferSize = DEFAULT_BUFFER_SIZE;
        private double retryThreshold = DEFAULT_RETRY_THRESHOLD;
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private IntervalFunction intervalFunction = (numOfAttempts) -> DEFAULT_WAIT_DURATION;
        // The default exception predicate retries all exceptions.
        private Predicate<Throwable> exceptionPredicate = (exception) -> true;

        public Builder bufferSize(int bufferSize) {
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("Buffer size has to be bigger than 0");
            }
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder retryThreshold(double retryThreshold) {
            if (retryThreshold > 1 || retryThreshold <= 0) {
                throw new IllegalArgumentException("Retry threshold has to be between 0 and 1(inclusive)");
            }
            this.retryThreshold = retryThreshold;
            return this;
        }

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 0) {
                throw new IllegalArgumentException("Max retries has to be over 0");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder intervalFunction (IntervalFunction intervalFunction) {
            this.intervalFunction = Objects.requireNonNull(intervalFunction);
            return this;
        }

        public Builder exceptionPredicate (Predicate<Throwable> exceptionPredicate) {
            this.exceptionPredicate = Objects.requireNonNull(exceptionPredicate);
            return this;
        }

        public RetryBudgetConfig build() {
            return new RetryBudgetConfig(bufferSize, retryThreshold, maxAttempts, intervalFunction, exceptionPredicate);
        }
    }
}
