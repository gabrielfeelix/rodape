package com.example.ui.prefs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataStoreManager
import com.example.data.ThemeMode
import com.example.data.model.User
import com.example.data.remote.AuthRepository
import com.example.data.remote.repo.UserRepository
import com.example.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// F5: preferências/onboarding/avaliação — corpos movidos verbatim do
// MainViewModel. MainActivity usa via by viewModels(); telas via hiltViewModel().
@HiltViewModel
class PrefsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val authRepository: AuthRepository,
    private val session: SessionManager,
    private val userRepo: UserRepository,
) : ViewModel() {

    private val supabaseEmail: StateFlow<String?> = authRepository.currentEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // App-level prefs (avaliação na Play Store + contador de engajamento)
    val ratedApp: StateFlow<Boolean> = dataStoreManager.ratedAppFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), false)

    val engagementCount: StateFlow<Int> = dataStoreManager.engagementCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), 0)

    /** Mostra o prompt de avaliação se: não avaliou ainda E já fez ≥3 ações de engajamento. */
    val shouldShowRatePrompt: StateFlow<Boolean> =
        combine(ratedApp, engagementCount) { rated, count -> !rated && count >= 3 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), false)

    fun markAppRated() {
        viewModelScope.launch { dataStoreManager.markAppRated() }
    }

    /** Marca como rated sem mostrar prompt de novo (pra "Agora não, talvez mais tarde" também silenciar). */
    fun dismissRatePromptForever() {
        viewModelScope.launch { dataStoreManager.markAppRated() }
    }

    val fontScale: StateFlow<Float> = dataStoreManager.fontScaleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    fun setFontScale(scale: Float) {
        viewModelScope.launch { dataStoreManager.setFontScale(scale) }
    }

    val themeMode: StateFlow<ThemeMode> = dataStoreManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { dataStoreManager.setThemeMode(mode) }
    }

    /** Registra o token FCM do device pro usuário logado (push F1). Idempotente. */
    fun syncPushToken() {
        com.example.push.PushTokens.sync(viewModelScope)
    }

    /**
     * Intro de primeiro uso ("como funciona"). `null` = ainda lendo do disco
     * (nao renderiza nada pra nao piscar), `false` = mostrar intro, `true` = ja
     * viu, segue pro welcome.
     */
    val introSeen: StateFlow<Boolean?> = dataStoreManager.introSeenFlow
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markIntroSeen() {
        viewModelScope.launch { dataStoreManager.markIntroSeen() }
    }

    /**
     * B1: código de convite capturado no Welcome (antes de criar conta). Fica
     * retido até a auth concluir; aí a UI faz o join automático. Em memória de
     * propósito — se o app fechar no meio, o usuário usa o join de dentro do app.
     */
    private val _pendingInviteCode = MutableStateFlow<String?>(null)
    val pendingInviteCode: StateFlow<String?> = _pendingInviteCode.asStateFlow()
    fun setPendingInviteCode(code: String) {
        _pendingInviteCode.value = code.trim().uppercase().ifBlank { null }
    }
    fun consumePendingInviteCode() { _pendingInviteCode.value = null }

    /** Conjunto de userIds que ja viram o onboarding pos-login neste device. */
    val onboardedUsers: StateFlow<Set<String>> = dataStoreManager.onboardedUsersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** True quando o user logado ainda precisa ver o onboarding. */
    val needsOnboarding: StateFlow<Boolean> =
        combine(session.currentUserId, onboardedUsers) { uid, set ->
            uid != null && uid !in set
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Salva preferencias do onboarding e marca como concluido. */
    fun completeOnboarding(apelido: String, avatarUrl: String, scale: Float) {
        viewModelScope.launch {
            val uid = session.currentUserId.value ?: return@launch
            val email = supabaseEmail.value.orEmpty()
            // Atualiza profile (nome + avatar). Email vem do JWT, nao mexer.
            runCatching { userRepo.insertUser(User(uid, apelido, email, avatarUrl)) }
            dataStoreManager.setFontScale(scale)
            dataStoreManager.markOnboardingDone(uid)
        }
    }
}
