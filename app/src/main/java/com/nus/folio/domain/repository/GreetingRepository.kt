package com.nus.folio.domain.repository

import com.nus.folio.domain.model.Greeting

interface GreetingRepository {
    suspend fun getGreeting(): Result<Greeting>
}
