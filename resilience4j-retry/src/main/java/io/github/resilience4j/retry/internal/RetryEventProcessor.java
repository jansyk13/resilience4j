package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;

class RetryEventProcessor extends EventProcessor<RetryEvent> implements EventConsumer<RetryEvent>, Retry.EventPublisher {

    @Override
    public void consumeEvent(RetryEvent event) {
        super.processEvent(event);
    }

    @Override
    public Retry.EventPublisher onSuccess(EventConsumer<RetryOnSuccessEvent> onSuccessEventConsumer) {
        registerConsumer(RetryOnSuccessEvent.class, onSuccessEventConsumer);
        return this;
    }

    @Override
    public Retry.EventPublisher onError(EventConsumer<RetryOnErrorEvent> onErrorEventConsumer) {
        registerConsumer(RetryOnErrorEvent.class, onErrorEventConsumer);
        return this;
    }

    @Override
    public Retry.EventPublisher onIgnoredError(EventConsumer<RetryOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
        registerConsumer(RetryOnIgnoredErrorEvent.class, onIgnoredErrorEventConsumer);
        return this;
    }
}
