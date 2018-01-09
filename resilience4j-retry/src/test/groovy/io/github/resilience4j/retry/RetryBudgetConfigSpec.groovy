package io.github.resilience4j.retry

import spock.lang.Specification


class RetryBudgetConfigSpec extends Specification {


    def 'builds config with default values'() {
        when:
        RetryBudgetConfig config = new RetryBudgetConfig.Builder().build()

        then:
        config.bufferSize == 100
        config.maxAttempts == 3
        config.retryThreshold == 0.2
    }

    def 'fails config build'() {
        when:
        supplier.call(new RetryBudgetConfig.Builder())

        then:
        thrown(expect)

        where:
        supplier << [
                { RetryBudgetConfig.Builder b -> b.bufferSize(-1).build() },
                { RetryBudgetConfig.Builder b -> b.retryThreshold(-1).build() },
                { RetryBudgetConfig.Builder b -> b.maxAttempts(-1).build() },
                { RetryBudgetConfig.Builder b -> b.exceptionPredicate(null).build() },
                { RetryBudgetConfig.Builder b -> b.intervalFunction(null).build() }
        ]
        expect << [
                IllegalArgumentException,
                IllegalArgumentException,
                IllegalArgumentException,
                NullPointerException,
                NullPointerException
        ]
    }
}
