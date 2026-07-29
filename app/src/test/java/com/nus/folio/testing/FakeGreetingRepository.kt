package com.nus.folio.testing

import com.nus.folio.domain.model.Greeting
import com.nus.folio.domain.repository.GreetingRepository

class FakeGreetingRepository : GreetingRepository {

    var getGreetingResult: Result<Greeting> = Result.success(Greeting("Hello Folio!"))
    var getGreetingCallCount = 0

    override suspend fun getGreeting(): Result<Greeting> {
        getGreetingCallCount++
        return getGreetingResult
    }
}
