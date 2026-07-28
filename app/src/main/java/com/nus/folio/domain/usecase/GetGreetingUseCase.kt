package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Greeting
import com.nus.folio.domain.repository.GreetingRepository

class GetGreetingUseCase(
    private val repository: GreetingRepository,
) {
    suspend operator fun invoke(): Result<Greeting> = repository.getGreeting()
}
