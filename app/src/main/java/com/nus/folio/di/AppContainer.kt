package com.nus.folio.di

import android.content.Context
import com.nus.folio.data.auth.AuthCapabilities
import com.nus.folio.data.auth.AuthSessionStore
import com.nus.folio.data.auth.EncryptedAuthSessionStore
import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.data.datasource.NoteDataSource
import com.nus.folio.data.datasource.SourceDataSource
import com.nus.folio.data.datasource.SourceOriginalFileDataSource
import com.nus.folio.data.datasource.SpaceDataSource
import com.nus.folio.data.repository.AskRepositoryImpl
import com.nus.folio.data.repository.AuthRepositoryImpl
import com.nus.folio.data.repository.NoteRepositoryImpl
import com.nus.folio.data.repository.SourceRepositoryImpl
import com.nus.folio.data.repository.SpaceRepositoryImpl
import com.nus.folio.data.util.ContentResolverSourceFileBytesReader
import com.nus.folio.domain.repository.AskRepository
import com.nus.folio.domain.repository.AuthRepository
import com.nus.folio.domain.repository.NoteRepository
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.repository.SourceRepository
import com.nus.folio.domain.repository.SpaceRepository
import com.nus.folio.domain.usecase.ClearAuthSessionUseCase
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.CreateSourceUseCase
import com.nus.folio.domain.usecase.CreateSpaceUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSpaceUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskSuggestionsUseCase
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetNoteDetailUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourceOriginalFileUseCase
import com.nus.folio.domain.usecase.GetSourcePreviewUrlUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.RequestPasswordResetUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.SignInUseCase
import com.nus.folio.domain.usecase.SignInWithAppleUseCase
import com.nus.folio.domain.usecase.SignUpUseCase
import com.nus.folio.domain.usecase.StreamAskAnswerUseCase
import com.nus.folio.domain.usecase.SyncCurrentUserUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.domain.usecase.UpdateSpaceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppContainer(
    appContext: Context,
    applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val applicationContext = appContext.applicationContext

    /** False in release until AuthDataSource is wired to a real backend. */
    val isAuthAvailable: Boolean = AuthCapabilities.isBackendAvailable

    private val _isSessionRestored = MutableStateFlow(false)

    /** Becomes true after the persisted auth session has been restored off the main thread. */
    val isSessionRestored: StateFlow<Boolean> = _isSessionRestored.asStateFlow()

    private val sourceDataSource: SourceDataSource by lazy {
        SourceDataSource(
            accessTokenProvider = { authRepository.getCurrentSession()?.accessToken },
            refreshAccessToken = {
                authRepository.refreshSession().getOrNull()?.accessToken
            },
        )
    }

    private val sourceOriginalFileDataSource: SourceOriginalFileDataSource by lazy {
        SourceOriginalFileDataSource(applicationContext, sourceDataSource)
    }

    private val sourceRepository: SourceRepository by lazy {
        SourceRepositoryImpl(sourceDataSource, sourceOriginalFileDataSource)
    }

    val sourceFileBytesReader: SourceFileBytesReader by lazy {
        ContentResolverSourceFileBytesReader(applicationContext)
    }

    val getSourcesUseCase: GetSourcesUseCase by lazy {
        GetSourcesUseCase(sourceRepository)
    }

    val createSourceUseCase: CreateSourceUseCase by lazy {
        CreateSourceUseCase(sourceRepository)
    }

    val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase by lazy {
        ObserveSourceProcessingUseCase(sourceRepository)
    }

    val updateSourceUseCase: UpdateSourceUseCase by lazy {
        UpdateSourceUseCase(sourceRepository)
    }

    val deleteSourceUseCase: DeleteSourceUseCase by lazy {
        DeleteSourceUseCase(sourceRepository)
    }

    val retrySourceUseCase: RetrySourceUseCase by lazy {
        RetrySourceUseCase(sourceRepository)
    }

    val getSourceDetailUseCase: GetSourceDetailUseCase by lazy {
        GetSourceDetailUseCase(sourceRepository)
    }

    val getSourceOriginalFileUseCase: GetSourceOriginalFileUseCase by lazy {
        GetSourceOriginalFileUseCase(sourceRepository)
    }

    val getSourcePreviewUrlUseCase: GetSourcePreviewUrlUseCase by lazy {
        GetSourcePreviewUrlUseCase(sourceRepository)
    }

    private val askDataSource: AskDataSource by lazy { AskDataSource() }

    private val askRepository: AskRepository by lazy {
        AskRepositoryImpl(askDataSource)
    }

    val getAskTopicsUseCase: GetAskTopicsUseCase by lazy {
        GetAskTopicsUseCase(askRepository)
    }

    val getAskSuggestionsUseCase: GetAskSuggestionsUseCase by lazy {
        GetAskSuggestionsUseCase(askRepository)
    }

    val streamAskAnswerUseCase: StreamAskAnswerUseCase by lazy {
        StreamAskAnswerUseCase(askRepository)
    }

    private val noteDataSource: NoteDataSource by lazy {
        NoteDataSource(
            accessTokenProvider = { authRepository.getCurrentSession()?.accessToken },
            refreshAccessToken = {
                authRepository.refreshSession().getOrNull()?.accessToken
            },
        )
    }

    private val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(noteDataSource)
    }

    val getNotesUseCase: GetNotesUseCase by lazy {
        GetNotesUseCase(noteRepository)
    }

    val getNoteDetailUseCase: GetNoteDetailUseCase by lazy {
        GetNoteDetailUseCase(noteRepository)
    }

    val createNoteUseCase: CreateNoteUseCase by lazy {
        CreateNoteUseCase(noteRepository)
    }

    val updateNoteUseCase: UpdateNoteUseCase by lazy {
        UpdateNoteUseCase(noteRepository)
    }

    val deleteNoteUseCase: DeleteNoteUseCase by lazy {
        DeleteNoteUseCase(noteRepository)
    }

    private val authDataSource: AuthDataSource by lazy { AuthDataSource() }

    private val authSessionStore: AuthSessionStore by lazy {
        EncryptedAuthSessionStore(applicationContext)
    }

    private val authRepositoryImpl: AuthRepositoryImpl by lazy {
        AuthRepositoryImpl(authDataSource, authSessionStore)
    }

    private val authRepository: AuthRepository
        get() = authRepositoryImpl

    init {
        applicationScope.launch {
            try {
                authRepositoryImpl.restoreSession()
                // Access tokens expire while the app is killed; refresh before any screen
                // loads. AuthApiException clears the session inside refreshSession().
                if (authRepositoryImpl.getCurrentSession() != null) {
                    authRepositoryImpl.refreshSession()
                }
            } finally {
                _isSessionRestored.value = true
            }
        }
    }

    private val spaceDataSource: SpaceDataSource by lazy {
        SpaceDataSource(
            accessTokenProvider = { authRepository.getCurrentSession()?.accessToken },
            refreshAccessToken = {
                authRepository.refreshSession().getOrNull()?.accessToken
            },
        )
    }

    private val spaceRepository: SpaceRepository by lazy {
        SpaceRepositoryImpl(spaceDataSource)
    }

    val getSpacesUseCase: GetSpacesUseCase by lazy {
        GetSpacesUseCase(spaceRepository)
    }

    val createSpaceUseCase: CreateSpaceUseCase by lazy {
        CreateSpaceUseCase(spaceRepository)
    }

    val updateSpaceUseCase: UpdateSpaceUseCase by lazy {
        UpdateSpaceUseCase(spaceRepository)
    }

    val deleteSpaceUseCase: DeleteSpaceUseCase by lazy {
        DeleteSpaceUseCase(spaceRepository)
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

    val refreshAuthSessionUseCase: RefreshAuthSessionUseCase by lazy {
        RefreshAuthSessionUseCase(authRepository)
    }

    val requestPasswordResetUseCase: RequestPasswordResetUseCase by lazy {
        RequestPasswordResetUseCase(authRepository)
    }

    val getCurrentSessionUseCase: GetCurrentSessionUseCase by lazy {
        GetCurrentSessionUseCase(authRepository)
    }

    val syncCurrentUserUseCase: SyncCurrentUserUseCase by lazy {
        SyncCurrentUserUseCase(authRepository)
    }

    val clearAuthSessionUseCase: ClearAuthSessionUseCase by lazy {
        ClearAuthSessionUseCase(authRepository)
    }
}
