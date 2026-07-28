package com.nus.folio.data.repository

import com.nus.folio.data.datasource.GreetingDataSource
import com.nus.folio.domain.model.Greeting
import com.nus.folio.domain.repository.GreetingRepository

class GreetingRepositoryImpl(
    private val dataSource: GreetingDataSource,
) : GreetingRepository {

    override suspend fun getGreeting(): Result<Greeting> = runCatching {
        dataSource.fetchGreeting()
    }
}
