/*
 *
 *  Copyright 2017: Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.circuitbreaker;

import java.time.Duration;
import java.util.function.Supplier;

import io.github.resilience4j.core.StopWatch;
import io.vertx.core.Future;

/**
 * CircuitBreaker decorators for Vert.x
 */
public interface VertxCircuitBreaker {

    /**
     * Decorates and executes the decorated Future.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier the Future Supplier
     * @param <T> the type of results returned by this Future
     * @return a future which is decorated by a CircuitBreaker.
     */
    static <T> Future<T> executeFuture(CircuitBreaker circuitBreaker, Supplier<Future<T>> supplier){
        return decorateFuture(circuitBreaker, supplier).get();
    }

    /**
     * Returns a Future which is decorated by a CircuitBreaker.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param supplier the Future supplier
     * @param <T> the type of the returned Future's result
     * @return a future which is decorated by a CircuitBreaker.
     */
    static <T> Supplier<Future<T>> decorateFuture(CircuitBreaker circuitBreaker, Supplier<Future<T>> supplier){
        return () -> {
            final Future<T> future = Future.future();

            if (!circuitBreaker.isCallPermitted()) {
                future.fail(
                        new CircuitBreakerOpenException(
                                String.format("CircuitBreaker '%s' is open", circuitBreaker.getName())));

            } else {
                long start = System.nanoTime();
                try {
                    supplier.get().setHandler(result -> {
                        long durationInNanos = System.nanoTime() - start;
                        if (result.failed()) {
                            circuitBreaker.onError(durationInNanos, result.cause());
                            future.fail(result.cause());
                        } else {
                            circuitBreaker.onSuccess(durationInNanos);
                            future.complete(result.result());
                        }
                    });
                } catch (Throwable throwable) {
                    long durationInNanos = System.nanoTime() - start;
                    circuitBreaker.onError(durationInNanos, throwable);
                    future.fail(throwable);
                }
            }
            return future;
        };
    }
}
