package com.example.ui.navigation

import kotlinx.serialization.Serializable

// Rotas type-safe (Navigation Compose 2.8+). Substituem as strings ("book_detail/$id")
// por tipos @Serializable: o compilador cobriga a passar os args certos e a Navigation
// cuida do encode/decode (não precisa mais URLEncoder no título da discussão).
// Objetos = sem argumento; data classes = com argumento.

@Serializable object Welcome
@Serializable object Login
@Serializable object Signup
@Serializable object ForgotPassword
@Serializable object ResetPassword
@Serializable object CreateClub
@Serializable object JoinClub
@Serializable object MainTabs
@Serializable object ManageClub
@Serializable object ManageChapters
@Serializable object ModerationLog
@Serializable object ModerationQueue
@Serializable object Frases
@Serializable object Notifications
@Serializable object SuggestBook
@Serializable object AddBookManual
@Serializable object About

@Serializable data class MeetingDetail(val meetingId: String)
@Serializable data class BookDetail(val bookId: String)
@Serializable data class Discussion(val chapterId: String, val title: String)
