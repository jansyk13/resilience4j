package io.github.resilience4j.retry;

import java.util.function.Function;
import java.util.function.Predicate;

public interface Config {

    int getMaxAttempts();

    Function<Integer, Long> getIntervalFunction();

    Predicate<Throwable> getExceptionPredicate();
}
