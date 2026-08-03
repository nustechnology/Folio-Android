package com.nus.folio.di

import android.content.Context
import com.nus.folio.data.auth.AuthCapabilities
import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.data.datasource.GreetingDataSource
import com.nus.folio.data.datasource.NoteDataSource
import com.nus.folio.data.datasource.SourceDataSource
import com.nus.folio.data.datasource.SourceOriginalFileDataSource
import com.nus.folio.data.datasource.SpaceDataSource
import com.nus.folio.data.repository.AskRepositoryImpl
import com.nus.folio.data.repository.AuthRepositoryImpl
import com.nus.folio.data.repository.GreetingRepositoryImpl
import com.nus.folio.data.repository.NoteRepositoryImpl
import com.nus.folio.data.repository.SourceRepositoryImpl
import com.nus.folio.data.repository.SpaceRepositoryImpl
import com.nus.folio.domain.repository.AskRepository
import com.nus.folio.domain.repository.AuthRepository
import com.nus.folio.domain.repository.GreetingRepository
import com.nus.folio.domain.repository.NoteRepository
import com.nus.folio.domain.repository.SourceRepository
import com.nus.folio.domain.repository.SpaceRepository
import com.nus.folio.domain.usecase.ClearAuthSessionUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetGreetingUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourceOriginalFileUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.domain.usecase.RequestPasswordResetUseCase
import com.nus.folio.domain.usecase.SignInUseCase
import com.nus.folio.domain.usecase.SignInWithAppleUseCase
import com.nus.folio.domain.usecase.SignUpUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase

class AppContainer(
    appContext: Context,
) {

    private val applicationContext = appContext.applicationContext

    /** False in release until AuthDataSource is wired to a real backend. */
    val isAuthAvailable: Boolean = AuthCapabilities.isBackendAvailable

    /** Prefills login fields in debug; empty in release. */
    val defaultLoginEmail: String = AuthCapabilities.defaultLoginEmail
    val defaultLoginPassword: String = AuthCapabilities.defaultLoginPassword

    private val greetingDataSource: GreetingDataSource by lazy { GreetingDataSource() }

    private val greetingRepository: GreetingRepository by lazy {
        GreetingRepositoryImpl(greetingDataSource)
    }

    val getGreetingUseCase: GetGreetingUseCase by lazy {
        GetGreetingUseCase(greetingRepository)
    }

    private val sourceDataSource: SourceDataSource by lazy { SourceDataSource() }

    private val sourceOriginalFileDataSource: SourceOriginalFileDataSource by lazy {
        SourceOriginalFileDataSource(applicationContext, sourceDataSource)
    }

    private val sourceRepository: SourceRepository by lazy {
        SourceRepositoryImpl(sourceDataSource, sourceOriginalFileDataSource)
    }

    val getSourcesUseCase: GetSourcesUseCase by lazy {
        GetSourcesUseCase(sourceRepository)
    }

    val updateSourceUseCase: UpdateSourceUseCase by lazy {
        UpdateSourceUseCase(sourceRepository)
    }

    val deleteSourceUseCase: DeleteSourceUseCase by lazy {
        DeleteSourceUseCase(sourceRepository)
    }

    val getSourceDetailUseCase: GetSourceDetailUseCase by lazy {
        GetSourceDetailUseCase(sourceRepository)
    }

    val getSourceOriginalFileUseCase: GetSourceOriginalFileUseCase by lazy {
        GetSourceOriginalFileUseCase(sourceRepository)
    }

    private val askDataSource: AskDataSource by lazy { AskDataSource() }

    private val askRepository: AskRepository by lazy {
        AskRepositoryImpl(askDataSource)
    }

    val getAskTopicsUseCase: GetAskTopicsUseCase by lazy {
        GetAskTopicsUseCase(askRepository)
    }

    private val noteDataSource: NoteDataSource by lazy { NoteDataSource() }

    private val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(noteDataSource)
    }

    val getNotesUseCase: GetNotesUseCase by lazy {
        GetNotesUseCase(noteRepository)
    }

    val updateNoteUseCase: UpdateNoteUseCase by lazy {
        UpdateNoteUseCase(noteRepository)
    }

    val deleteNoteUseCase: DeleteNoteUseCase by lazy {
        DeleteNoteUseCase(noteRepository)
    }

    private val spaceDataSource: SpaceDataSource by lazy { SpaceDataSource() }

    private val spaceRepository: SpaceRepository by lazy {
        SpaceRepositoryImpl(spaceDataSource)
    }

    val getSpacesUseCase: GetSpacesUseCase by lazy {
        GetSpacesUseCase(spaceRepository)
    }

    private val authDataSource: AuthDataSource by lazy { AuthDataSource() }

    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authDataSource)
    }

    val signUpUseCase: SignUpUseCase by lazy {
        SignUpUseCase(authRepository)
    }

    val signInUseCase: SignInUseCase by lazy {
        SignInUseCase(authRepository)
    }

    val signInWithAppleUseCase: SignInWithAppleUseCase by lazy {
        SignInWithAppleUseCase(authRepository)
    }

    val requestPasswordResetUseCase: RequestPasswordResetUseCase by lazy {
        RequestPasswordResetUseCase(authRepository)
    }

    val getCurrentSessionUseCase: GetCurrentSessionUseCase by lazy {
        GetCurrentSessionUseCase(authRepository)
    }

    val clearAuthSessionUseCase: ClearAuthSessionUseCase by lazy {
        ClearAuthSessionUseCase(authRepository)
    }
}
