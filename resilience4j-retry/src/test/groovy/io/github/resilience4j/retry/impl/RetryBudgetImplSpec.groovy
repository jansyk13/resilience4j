package io.github.resilience4j.retry.impl

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryBudgetConfig
import io.github.resilience4j.retry.internal.RetryBudgetImpl
import io.github.resilience4j.test.HelloWorldService
import spock.lang.Specification

class RetryBudgetImplSpec extends Specification {

    def 'should not retry'() {
        given:
        def sleptTime = 0L
        def config = new RetryBudgetConfig.Builder()
                .intervalFunction { sleep -> sleptTime += sleep }
                .build()
        def retryBudget = new RetryBudgetImpl('troll', config)
        def mockService = Mock(HelloWorldService)

        when:
        retry.call(retryBudget, mockService)

        then:
        sleptTime == 0
        interaction {
            1 * mockService.sayHelloWorld()
        }

        where:
        retry << [
                { r, s -> Retry.decorateRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCallable(r, { s.sayHelloWorld() }).call() },
                { r, s -> Retry.decorateFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateSupplier(r, { s.sayHelloWorld() }).get() },
                { r, s -> Retry.decorateCheckedFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateCheckedRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCheckedSupplier(r, { s.sayHelloWorld() }).apply() }
        ]
    }

    def 'should retry'() {
        given:
        def sleptTime = 0L
        def config = new RetryBudgetConfig.Builder()
                .intervalFunction { sleep -> sleptTime += sleep }
                .build()
        def retryBudget = new RetryBudgetImpl('troll', config)
        def mockService = Mock(HelloWorldService)

        when:
        retry.call(retryBudget, mockService)

        then:
        sleptTime == 1L
        interaction {
            2 * mockService.sayHelloWorld() >> { throw new RuntimeException() } >> {}
        }

        where:
        retry << [
                { r, s -> Retry.decorateRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCallable(r, { s.sayHelloWorld() }).call() },
                { r, s -> Retry.decorateFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateSupplier(r, { s.sayHelloWorld() }).get() },
                { r, s -> Retry.decorateCheckedFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateCheckedRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCheckedSupplier(r, { s.sayHelloWorld() }).apply() }
        ]
    }

    def 'should hit max number of attempts'() {
        given:
        def sleptTime = 0L
        def config = new RetryBudgetConfig.Builder()
                .maxAttempts(3)
                .intervalFunction { sleep -> sleptTime += sleep }
                .build()
        def retryBudget = new RetryBudgetImpl('troll', config)
        def mockService = Mock(HelloWorldService)

        when:
        retry.call(retryBudget, mockService)

        then:
        thrown(RuntimeException)
        sleptTime == 3L
        interaction {
            3 * mockService.sayHelloWorld() >> { throw new RuntimeException() }
        }

        where:
        retry << [
                { r, s -> Retry.decorateRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCallable(r, { s.sayHelloWorld() }).call() },
                { r, s -> Retry.decorateFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateSupplier(r, { s.sayHelloWorld() }).get() },
                { r, s -> Retry.decorateCheckedFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateCheckedRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCheckedSupplier(r, { s.sayHelloWorld() }).apply() }
        ]
    }

    def 'should hit retry threshold'() {
        given:
        def sleptTime = 0L
        def config = new RetryBudgetConfig.Builder()
                .bufferSize(2)
                .retryThreshold(0.3)
                .maxAttempts(5)
                .intervalFunction { sleep -> sleptTime += sleep }
                .build()
        def retryBudget = new RetryBudgetImpl('troll', config)
        def mockService = Mock(HelloWorldService)

        when:
        retry.call(retryBudget, mockService)

        then:
        thrown(RuntimeException)
        sleptTime == 1L
        interaction {
            2 * mockService.sayHelloWorld() >> { throw new RuntimeException() }
        }

        where:
        retry << [
                { r, s -> Retry.decorateRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCallable(r, { s.sayHelloWorld() }).call() },
                { r, s -> Retry.decorateFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateSupplier(r, { s.sayHelloWorld() }).get() },
                { r, s -> Retry.decorateCheckedFunction(r, { s.sayHelloWorld() }).apply(null) },
                { r, s -> Retry.decorateCheckedRunnable(r, { s.sayHelloWorld() }).run() },
                { r, s -> Retry.decorateCheckedSupplier(r, { s.sayHelloWorld() }).apply() }
        ]
    }

    def 'should recover from hitting retry threshold'() {
        given:
        def sleptTime = 0L
        def config = new RetryBudgetConfig.Builder()
                .bufferSize(5)
                .retryThreshold(0.5)
                .maxAttempts(3)
                .intervalFunction { sleep -> sleptTime += sleep }
                .build()
        def retryBudget = new RetryBudgetImpl('troll', config)
        def mockService = Mock(HelloWorldService)

        when:
        try {
            retry.call(retryBudget, mockService)
        } catch (RuntimeException e) {
            // expected
        }
        try {
            retry.call(retryBudget, mockService)
        } catch (RuntimeException e) {
            // expected
        }

        then:
        sleptTime == 4L
        interaction {
            5 * mockService.sayHelloWorld() >>
                    { throw new RuntimeException() } >>
                    { throw new RuntimeException() } >>
                    { throw new RuntimeException() } >>
                    { throw new RuntimeException() } >>
                    { throw new RuntimeException() }
        }

        where:
        retry << [
                { r, s -> Retry.decorateRunnable(r, { s.sayHelloWorld() }).run() },
//                { r, s -> Retry.decorateCallable(r, { s.sayHelloWorld() }).call() },
//                { r, s -> Retry.decorateFunction(r, { s.sayHelloWorld() }).apply(null) },
//                { r, s -> Retry.decorateSupplier(r, { s.sayHelloWorld() }).get() },
//                { r, s -> Retry.decorateCheckedFunction(r, { s.sayHelloWorld() }).apply(null) },
//                { r, s -> Retry.decorateCheckedRunnable(r, { s.sayHelloWorld() }).run() },
//                { r, s -> Retry.decorateCheckedSupplier(r, { s.sayHelloWorld() }).apply() }
        ]
    }
}
