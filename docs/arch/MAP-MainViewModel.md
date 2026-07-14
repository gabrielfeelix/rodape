# Mapa completo — MainViewModel.kt (referência p/ o split de VM)

> Leitura integral (2026-07-14). Arquivo REAL tem 1781 linhas. Números driftam com o
> refactor — use como mapa estrutural.

Classe: linha 21 `MainViewModel(application) : AndroidViewModel`. Colaboradores (campos):
`repository = RemoteRepository(appContext)`(23), `dataStoreManager = DataStoreManager`(24),
`authRepository = AuthRepository()`(25).

## 1. Estado (56 flows) por domínio

**Sessão/Auth/User (a espinha):** sessionStatus(37), supabaseUserId(39),
supabaseDisplayName(45), supabaseEmail(48), **currentUserId(52, GLOBAL)**, currentUser(54),
userName(59), userEmail(60), **activeClubId(65, GLOBAL, backing _activeClubId@64)**.

**Prefs/Ratings/Onboarding:** ratedApp(68), engagementCount(71), shouldShowRatePrompt(75),
fontScale(93), themeMode(100), introSeen(117), pendingInviteCode(131), onboardedUsers(138),
needsOnboarding(142).

**Clubs:** allClubs(160), activeClub(164), currentBooksMap(168), currentUserPapel(292),
isCurrentUserAdmin(307), isCurrentUserSuperAdmin(311), activeClubMembersRaw(316),
clubMembers(228), archivedClubsForUser(408).

**Books/Chapters:** currentBook(190), currentChapters(199), clubBooks(215),
suggestedBooks(219), finishedBooks(223), nextBooks(284), favoriteBooks(257),
finishedBooksMeetingDates(413).

**Progress:** userProgress(204), allProgressForClub(232).

**Meetings/RSVP:** latestMeeting(237), latestMeetingRsvps(241), activeMeetingPattern(321),
meetingsForCurrentBook(1673), scheduledMeetingsInActiveClub(1684).

**Notifications:** notifications(246). **Quotes:** savedQuotes(251).

**Voting:** activeVotingRound(263), votesForActiveRound(272), bookSuggestionsByBookId(277).

**Moderation:** removedCommentsInActiveClub(326), blockedIds(332), pendingReports(382),
pendingReportsLoading(384).

**Search:** searchResults(425), searchResultsUnified(428), searchLoading(431).

**Tab-nav:** requestedTab(435). **Sync:** pendingMutationsCount(1169).

**Flows por-chamada (não stateIn):** getCommentsForChapter(687), getSavedQuotesForBook(692),
getReactionsForChapter(721), getBookSummaryFlow(821), getBookRatingsFlow(826),
getBookRatingOfCurrentUserFlow(831), getBookSuggestionFlow(838), getCommentsForBookFlow(843),
isBookFavoriteFlow(872), getRsvpOfUser(910), getMeetingByIdFlow(1688),
getMeetingMinutesFlow(1690), getMyMeetingNoteFlow(1693).

## 2. Funções (ações) — resumo por domínio

**Prefs/Onboarding:** markAppRated(79), bumpEngagement(84,CROSS), dismissRatePromptForever
(89), setFontScale(96), setThemeMode(103), syncPushToken(108), markIntroSeen(121),
setPendingInviteCode(132), consumePendingInviteCode(135), completeOnboarding(148).

**Moderation:** reportContent(337), blockUser(353), unblockUser(359), moderateRemove(365),
refreshPendingReports(386), dismissReport(396), resolveReportByRemoving(401).

**Tab:** requestTab(437), consumeRequestedTab(438).

**Auth:** login(519,no-op), logout(531), signOutSupabase(548), deleteAccount(574),
updateUserProfile(594).

**Club:** selectActiveClub(562,writer), createClub(604), joinClubWithCode(639),
editClubInfo(1184), regenerateInviteCode(1192), promoteMemberToAdmin(1205),
demoteAdminToMember(1218), transferSuperAdmin(1231), removeMember(1244), leaveActiveClub
(1257), archiveClub(1500), unarchiveClub(1513).

**Progress:** updateBookProgress(678).

**Discussion:** sendComment(698), toggleReaction(725), editComment(1485), deleteOwnComment
(1494), removeComment(1371), restoreRemovedComment(1381).

**Voting:** voteForBook(745), openVotingRound(767), closeActiveVotingRound(793),
maybeAutoCloseExpiredRound(801), closeRoundInternal(811,priv), removeSuggestion(1388).

**Book:** saveBookSummary(848), saveBookRating(859), toggleBookFavorite(877),
openFavoriteBook(887), setBookMeetingDate(894), createBookSuggestion(976), createManualBook
(1034), existingClubBookId(963,priv CROSS), changeCurrentBookManually(1400),
setSearchedBookAsCurrent(1419), promoteNextQueuedBook(1451,priv CROSS), markCurrentBookFinished
(1458), upsertChapters(1474), fetchChaptersOnline(1573), shareChapterTemplate(1618),
fetchChaptersFromEpub(1634,priv), downloadBytes(1651,priv), verifyAuthorWithGoogleBooks(1529),
searchOpenLibrary(921).

**Meetings:** rsvpMeeting(902), upsertMeetingPattern(1278), deactivateMeetingPattern(1310),
upsertMeeting(1320), cancelMeeting(1361), concludeMeeting(1733), saveMeetingMinutes(1697),
saveMyMeetingNote(1715), suggestNextChapterRange(1767).

**Notifications:** markAllNotificationsAsRead(1120), markNotificationAsRead(1127).

**Quotes:** saveQuote(1134), deleteQuote(1153), restoreQuote(1160).

**Sync:** forceRefresh(1173).

## 3. Grafo de sessão compartilhado (O NÓ — vai pro SessionManager)

**Tier 1 (lido em quase tudo):**
- **currentUserId(52)** — lido por ~30 ações + a maioria dos flows user-scoped.
- **activeClubId(65)** — lido por ~todos flows club-scoped; ESCRITO por init(473-487),
  selectActiveClub(563), createClub(623), joinClubWithCode(653), leaveActiveClub(1267),
  archiveClub(1508), unarchiveClub(1519), deleteAccount(589), logout(541), openFavoriteBook.

**Tier 2 (derivados, multi-domínio):** currentBook(190), currentChapters(199),
activeClub(164), currentUserPapel(292)/isAdmin(307)/isSuperAdmin(311), clubBooks(215),
activeVotingRound(263).

**Tier 3 (utilitários cross-domain):** bumpEngagement(84), toastErro(29,priv),
existingClubBookId(963,priv), promoteNextQueuedBook(1451,priv), selectActiveClub(562).

## 4. init{} (440-514) — 4 observers (vão pro SessionManager)

1. (452-468) guard troca-de-conta: clearLocalCacheNoDrain se currentUserId mudou.
2. (473-487) auto-seleção de clube: seta activeClubId de allClubs/lastActiveClubId. **É
   onde activeClubId ganha valor de sessão.**
3. (490-498) auto-close de votação expirada.
4. (503-513) avatar-default gendered.
onCleared(524): repository.close(). Eager stateIn: user/name/email/fontScale/themeMode/
introSeen/onboardedUsers/needsOnboarding. Resto WhileSubscribed(60s).

## 5. Ordem de extração (folha→hub)

tab → sync → notificações → busca → prefs(sem bumpEngagement) → quotes → moderação →
progresso → ratings → votação → meetings → discussão → **book → club → auth/sessão (por
último — viram/alimentam o SessionManager)**.

**Obstáculo estrutural:** o init (4 launches) + os writers de currentUserId/activeClubId
são a cola. Nada de VM-por-tela até esse grafo virar SessionManager.
