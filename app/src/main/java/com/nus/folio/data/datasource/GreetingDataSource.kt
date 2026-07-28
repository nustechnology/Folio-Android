package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Greeting
import kotlinx.coroutines.delay

class GreetingDataSource {

    suspend fun fetchGreeting(): Greeting {
        delay(300)
        return Greeting(message = "Hello Folio!")
    }
}
