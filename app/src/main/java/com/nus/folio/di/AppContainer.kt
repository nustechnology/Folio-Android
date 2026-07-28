package com.nus.folio.di

import com.nus.folio.data.datasource.GreetingDataSource
import com.nus.folio.data.repository.GreetingRepositoryImpl
import com.nus.folio.domain.repository.GreetingRepository
import com.nus.folio.domain.usecase.GetGreetingUseCase

class AppContainer {

    private val greetingDataSource: GreetingDataSource by lazy { GreetingDataSource() }

    private val greetingRepository: GreetingRepository by lazy {
        GreetingRepositoryImpl(greetingDataSource)
    }

    val getGreetingUseCase: GetGreetingUseCase by lazy {
        GetGreetingUseCase(greetingRepository)
    }
}
