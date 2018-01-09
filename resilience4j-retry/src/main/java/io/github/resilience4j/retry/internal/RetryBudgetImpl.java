package io.github.resilience4j.retry.internal;

import io.github.resilience4j.bitset.RingBitSet;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryBudgetConfig;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.vavr.CheckedConsumer;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryBudgetImpl implements Retry {

    private final Metrics metrics;
    private final RetryEventProcessor eventProcessor;

    private String name;
    private RetryBudgetConfig config;
    private int maxAttempts;
    private Function<Integer, Long> intervalFunction;
    private Predicate<Throwable> exceptionPredicate;
    private int bufferSize;
    // to avoid integer division
    private double _bufferSize;
    private double retryThreshold;
    private LongAdder succeededAfterRetryCounter;
    private LongAdder failedAfterRetryCounter;
    private LongAdder succeededWithoutRetryCounter;
    private LongAdder failedWithoutRetryCounter;
    /*package*/ static CheckedConsumer<Long> sleepFunction = Thread::sleep;
    private RingBitSet ringBitSet;

    public RetryBudgetImpl(String name, RetryBudgetConfig config) {
        this.name = name;
        this.config = config;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();
        this.bufferSize = config.getBufferSize();
        this._bufferSize = config.getBufferSize();
        this.retryThreshold = config.getRetryThreshold();
        this.metrics = this.new RetryMetrics();
        this.eventProcessor = new RetryEventProcessor();
        this.succeededAfterRetryCounter = new LongAdder();
        this.failedAfterRetryCounter = new LongAdder();
        this.succeededWithoutRetryCounter = new LongAdder();
        this.failedWithoutRetryCounter = new LongAdder();
        this.ringBitSet = new RingBitSet(bufferSize);
    }

    public final class ContextImpl implements Retry.Context {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private final AtomicReference<Exception> lastException = new AtomicReference<>();
        private final AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

        private ContextImpl() {
        }

        public void onSuccess() {
            int currentNumOfAttempts = numOfAttempts.get();
            ringBitSet.setNextBit(false);
            if (currentNumOfAttempts > 0) {
                succeededAfterRetryCounter.increment();
                Throwable throwable = Option.of(lastException.get()).getOrElse(lastRuntimeException.get());
                publishRetryEvent(() -> new RetryOnSuccessEvent(getName(), currentNumOfAttempts, throwable));
            } else {
                succeededWithoutRetryCounter.increment();
            }
        }

        public void onError(Exception exception) throws Throwable {
            if (exceptionPredicate.test(exception)) {
                lastException.set(exception);
                throwOrSleepAfterException();
            } else {
                failedWithoutRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), exception));
                throw exception;
            }
        }

        public void onRuntimeError(RuntimeException runtimeException) {
            if (exceptionPredicate.test(runtimeException)) {
                lastRuntimeException.set(runtimeException);
                throwOrSleepAfterRuntimeException();
            } else {
                failedWithoutRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), runtimeException));
                throw runtimeException;
            }
        }

        private void throwOrSleepAfterException() throws Exception {
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            if (canRetry(currentNumOfAttempts)) {
                waitIntervalAfterFailure();
            } else {
                failedAfterRetryCounter.increment();
                Exception throwable = lastException.get();
                publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            }
        }

        private void throwOrSleepAfterRuntimeException() {
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            if (canRetry(currentNumOfAttempts)) {
                waitIntervalAfterFailure();
            } else {
                failedAfterRetryCounter.increment();
                RuntimeException throwable = lastRuntimeException.get();
                publishRetryEvent(() -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            }
        }

        private void waitIntervalAfterFailure() {
            // wait interval until the next attempt should start
            long interval = intervalFunction.apply(numOfAttempts.get());
            Try.run(() -> sleepFunction.accept(interval))
                    .getOrElseThrow(ex -> lastRuntimeException.get());
        }

        private boolean canRetry(final int currentNumOfAttempts) {
            if (currentNumOfAttempts >= maxAttempts) {
                return false;
            }
            synchronized (this) {
                double retriesPct = ringBitSet.cardinality() / _bufferSize;
                if (retriesPct >= retryThreshold) {
                    return false;
                }
                ringBitSet.setNextBit(true);
                return true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Context context() {
        return new RetryBudgetImpl.ContextImpl();
    }

    @Override
    public RetryBudgetConfig getConfig() {
        return config;
    }


    private void publishRetryEvent(Supplier<RetryEvent> event) {
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(event.get());
        }
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return this.metrics;
    }

    public final class RetryMetrics implements Metrics {
        private RetryMetrics() {
        }

        @Override
        public long getNumberOfSuccessfulCallsWithoutRetryAttempt() {
            return succeededWithoutRetryCounter.longValue();
        }

        @Override
        public long getNumberOfFailedCallsWithoutRetryAttempt() {
            return failedWithoutRetryCounter.longValue();
        }

        @Override
        public long getNumberOfSuccessfulCallsWithRetryAttempt() {
            return succeededAfterRetryCounter.longValue();
        }

        @Override
        public long getNumberOfFailedCallsWithRetryAttempt() {
            return failedAfterRetryCounter.longValue();
        }
    }
}
