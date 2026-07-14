# Mapa completo — RemoteRepository.kt (referência p/ o refactor)

> Gerado por leitura integral (2026-07-14), ANTES do F3b. Números de linha são do
> estado pós-F3a (DTOs já movidos p/ RemoteDtos.kt). Vão driftar conforme o refactor
> avança — use como mapa estrutural (qual método / qual domínio / qual infra), não
> como offset fixo.

Arquivo: `app/src/main/java/com/example/data/remote/RemoteRepository.kt` (~2654 linhas)
DTOs: `app/src/main/java/com/example/data/remote/RemoteDtos.kt` (~697 linhas, internal)
Classe: linha 65 `class RemoteRepository(appContext, supabase = Supabase.client)`.

## 1. Infra compartilhada (o kernel → vira SyncEngine)

**Estado/campos:** `appContext`(66), `supabase`(67), `json`(70), `dao`(74, RodapeDao),
`pendingDao`(75), `scope`(79, SupervisorJob+IO).

**Lifecycle/cache:** `clearLocalCache`(87), `clearLocalCacheNoDrain`(94, dao.clearAll+
pendingDao.clear+lastSyncAt.clear), `close`(654, cancela scope+realtime).

**SWR/TTL (100-136):** `lastSyncAt`(112, ConcurrentHashMap), `recentlySynced`(115),
`markSynced`(121), `syncOnce(key,ttl,block)`(126), `Ttl`(132: FAST=5s/MED=30s/SLOW=300s).
NÃO existe `syncedOncePerProcess` — dedup é só via lastSyncAt+syncOnce.

**Fila offline (138-333):** `mutationHandlers`(152, kind→handler), `registerHandler`(160),
`enqueueMutation`(165), `immediateDrainInFlight`(186), `kickImmediateDrain`(190, backoff
250/750/1500/3000/5000ms), `tryRemoteOrEnqueue(kind,payload,notifyTable,block)`(211,
**envelope central de mutação**, 3 retries inline, isPermanentError→enfileira,
notifyLocalMutation no sucesso), `maxDrainAttempts`(247,=5), `isPermanentError`(252,
regex 408/429 transiente vs 4xx permanente), `drainMutex`(262), `tryDrainPendingQueue()`
(267, público, drena em ordem createdAt sob mutex, para no 1º transiente, dead-letter
permanente), `pendingMutationsCount`(306, público), `forceRefresh()`(313, público:
limpa lastSyncAt+drena+roda todos reloaders), `JsonObject.str`(330)/`strOrNull`(332),
`init{}`(335-552: registra 25 handlers + kick drain no 551).

**Realtime/reload (554-674):** `realtimeJobs`(577), `realtimeChannels`(581),
`tableReloaders`(589, table→(key→reload)), `registerReloader`(592),
`notifyLocalMutation(table)`(599, fast-path: roda reloaders da tabela),
`ensureRealtime(table,col,val,reload)`(615, registra reloader + 1 subscription
postgresChangeFlow por key via realtimeJobs.compute atômico), `stateOf`(674, legacy
polling — só getVotesForClubFlow usa).

**Superfície mínima que a engine expõe:** dao, pendingDao, supabase, json, scope;
syncOnce/markSynced/Ttl; tryRemoteOrEnqueue; notifyLocalMutation; ensureRealtime;
registerHandler; tryDrainPendingQueue/forceRefresh/pendingMutationsCount/clearLocalCache*/
close; str/strOrNull/escapeJson; stateOf.

## 2. Inventário por domínio (método@linha → infra/dao/dto)

TRO=tryRemoteOrEnqueue NLM=notifyLocalMutation ER=ensureRealtime SO=syncOnce MS=markSynced

**USERS/PROFILES (676-752):** getUserFlow(680,SO/ER profiles), syncUser(689,priv),
getUser(699), getAllUsersFlow(707,stub), insertUser(709,NLM+TRO upsert_profile,
ProfileUpdateDto nested@737), updateFontScale(746).

**CLUBS (754-958):** getClubFlow(758,SO/ER), syncClub(765), getClub(775),
getClubByCodigo(781), getClubsForUser(792,SO/ER club_members), syncClubsForUser(802,
JoinClubOnly@819), getClubsForUserList(821), insertClub(830,no-op),
createClubViaRpc(843,RPC create_club), joinClubWithCodeViaRpc(866,RPC),
extractIdFromRpcRow(877), extractFieldFromRpcRow(880), leaveClubViaRpc(896,RPC),
deleteOwnAccountViaRpc(906,RPC), regenerateInviteCodeViaRpc(912,RPC),
promoteMemberViaRpc(922,RPC), demoteAdminViaRpc(929,RPC), transferSuperAdminViaRpc(936,
RPC), removeMemberViaRpc(944,RPC), closeVotingRoundViaRpc(952,RPC).
> Nota: promote/demote/transfer/remove/leave/regenerate/closeVotingRound estão FISICAMENTE
> em CLUBS mas pertencem a Members/Admin/Voting → realocar no fatiamento.

**CLUB MEMBERS (960-1074):** insertClubMember(964,no-op), getClubMembersFlow(969,SO/ER),
syncClubMembers(977, JoinMemberProfile@995), getClubMember(1003),
getClubMembersListOrderedByJoin(1017), getClubMembersRawFlow(1024,ER),
updateMemberPapel(1031,NLM), deleteClubMember(1047,NLM), insertMemberRemoval(1059,no-op),
getMemberRemovalsForClubFlow(1063).

**CLUB ADMIN (1076-1118):** updateClubInfo(1080), updateClubCodigo(1091),
updateClubArquivado(1100), getArchivedClubsForUserFlow(1108).

**BOOKS/CLUB_BOOKS (1120-1307):** insertBook(1124,TRO insert_book), uploadBookCover(1154,
storage), insertClubBook(1168,TRO insert_club_book), getBookByStatusFlow(1177,SO/ER),
syncClubBooks(1185, JoinClubBookFull@1204), getClubBooksFlow(1213), getClubBookStatus(1221,
StatusOnlyDto@1231), getBook(1234), updateClubBookStatus(1245,NLM),
updateClubBookMeetingDate(1258,NLM), getClubBooksByStatusFlow(1272),
_oldClubBooksByStatusFlow(1281,dead), deleteClubBook(1296,NLM).

**CHAPTERS (1309-1400):** insertChapters(1313), getChaptersForBookFlow(1321),
deleteChaptersForBook(1334), saveChapters(1355,NLM, IdOnlyDto@1798),
getChapterTemplate(1380), shareChapterTemplate(1389).

**USER PROGRESS (1402-1473):** insertUserProgress(1406,TRO upsert_user_progress),
getUserProgressFlow(1427,ER), getUserProgress(1446), getAllProgressForClubFlow(1461,ER
sem filtro).

**COMMENTS/REACTIONS (1475-1795):** insertComment(1479,TRO insert_comment),
escapeJson(1498,priv helper), getCommentsForChapterFlow(1505,SO/ER),
syncCommentsForChapter(1513), getCommentsForBookFlow(1526, join chapters),
softRemoveComment(1543,TRO remove_comment), restoreComment(1559,TRO restore_comment),
editOwnComment(1573,TRO edit_comment), deleteOwnComment(1585,TRO delete_comment),
insertReaction(1742,TRO insert_reaction), deleteReaction(1750,TRO delete_reaction),
getReactionsForCommentFlow(1764,ER), getReactionsForChapterFlow(1778,ER sem filtro,
IdOnlyDto comments).

**MODERATION (1593-1738):** reportContent(1599,TRO insert_report), blockUser(1630,TRO
insert_user_block), unblockUser(1640,TRO delete_user_block), observeBlockedIds(1652),
isBlockedFlow(1657), syncMyBlocks(1659), tableForTarget(1666,priv), moderateRemoveContent
(1676,TRO moderate_remove+RPC, dao.markX Removed p/ 4 tabelas), fetchPendingReports(1710),
dismissReport(1718,RPC), getRemovedCommentsForClubFlow(1724).

**VOTES/VOTING (1800-1969):** insertVote(1804→setUserVoteInRound), setUserVoteInRound(1818,
TRO insert_vote), clearVotesForUserInClub(1829,NLM, IdOnlyDto voting_rounds),
getVotesForClubFlow(1845,stateOf), getVotesForRoundFlow(1862,ER), getVotesForRound(1876),
removeUserVoteForBookInRound(1888,TRO delete_vote), countUserVotesInRound(1907),
insertVotingRound(1918,NLM), getActiveVotingRoundFlow(1938,ER), getActiveVotingRound(1950),
closeVotingRound(1961,RPC+NLM votes/club_books/notifications).

**BOOK SUGGESTIONS (1971-2038):** insertBookSuggestion(1975,TRO), getBookSuggestionFlow
(1990), getBookSuggestionsForClubFlow(2006,ER), deleteBookSuggestion(2020,NLM),
deleteVotesForBook(2033,NLM votes — cross-domain).

**MEETINGS (2040-2381):** insertMeeting(2044,TRO insert_meeting +semeia FK books),
syncMeetingsForClub(2078), getLatestMeetingFlow(2088,ER), getAllMeetingsFlow(2096,ER),
getScheduledMeetingsForClubFlow(2103,ER), getMeetingById(2110), getMeetingByIdFlow(2121),
getMeetingsForBookFlow(2126,ER), getMeetingsForBookList(2135), updateMeetingStatus(2146,
NLM), deleteMeeting(2155,NLM), deleteRsvpsForMeeting(2163,NLM), insertMeetingRsvp(2173,TRO
insert_meeting_rsvp), getRsvpsForMeetingFlow(2188,ER), getRsvpForMeetingOfUserFlow(2202),
insertMeetingPattern(2220,NLM), generateMeetingsFromPattern(2253→insertMeeting),
getActiveMeetingPatternFlow(2294), getActiveMeetingPattern(2304), deactivateMeetingPatterns
(2314,NLM), insertMeetingMinutes(2325,NLM), getMeetingMinutesFlow(2338,ER),
insertMeetingNote(2353,NLM), getMeetingNoteFlow(2367).

**NOTIFICATIONS (2383-2441):** insertNotification(2387,NLM), markAllNotificationsAsRead
(2406,NLM), markNotificationAsRead(2418,TRO mark_notification_read), getNotificationsFlow
(2428,ER).

**SAVED QUOTES (2443-2502):** insertSavedQuote(2447,TRO insert_saved_quote),
deleteSavedQuote(2465,NLM), getSavedQuotesForUserFlow(2473,ER), getSavedQuotesForBookFlow
(2488).

**SUMMARIES/RATINGS (2504-2599):** insertBookSummary(2508,NLM), getBookSummaryFlow(2523,
ER), insertBookRating(2541,TRO upsert_book_rating), getBookRatingsFlow(2566,ER),
getBookRatingOfUserFlow(2584).

**FAVORITES (2601-2645):** setBookFavorite(2604,TRO insert/delete_book_favorite),
isBookFavoriteFlow(2627), anyClubIdForBook(2630), getFavoriteBooksForUserFlow(2632,ER).

**SEED (2647):** seedDatabase(2651,no-op).

## 3. DTOs nested (privados na classe — mover com o único consumidor)

ProfileUpdateDto(737, insertUser+handler upsert_profile), JoinClubOnly(819),
JoinMemberProfile(995), JoinClubBookFull(1204), StatusOnlyDto(1231),
**IdOnlyDto(1798) — usado por 4 domínios (chapters/reactions/votes×2) → vira tipo
`internal` compartilhado em RemoteDtos.kt**.

## 4. RPCs (12 distintos, 13 call sites)

create_club(849), join_club_with_code(867), leave_club(897), delete_own_account(907),
regenerate_invite_code(913), promote_member(923), demote_admin(930),
transfer_super_admin(937), remove_member(945), close_voting_round(954),
moderate_remove_content(1699 online + 541 handler), dismiss_report(1720).

## 5. Amarras cross-domain

1. insertMeeting(2044) semeia FK books (online 2071-74 + handler 412-419).
2. closeVotingRound(1961) NLM em 3 tabelas (votes/club_books/notifications).
3. deleteVotesForBook(2033) em BOOK SUGGESTIONS mas muta votes.
4. clearVotesForUserInClub(1829)/getVotesForClubFlow(1845) consultam voting_rounds antes.
5. moderateRemoveContent(1676) escreve em 4 tabelas + tableForTarget routing.
6. getReactionsForChapterFlow(1778) consulta comments antes.
7. getCommentsForBookFlow(1526) join em chapters.
8. RPCs físicos em CLUBS pertencem a Members/Admin/Voting.
DTOs compartilhados: BookInsertDto, IdOnlyDto, ProfileDto, ClubDto, BookDto, ProfileUpdateDto.

## 6. Os 25 handlers da fila (init 337-548) — TODOS num registro só

insert_comment(337), insert_reaction(349), delete_reaction(359), insert_meeting_rsvp(369),
insert_vote(379), insert_saved_quote(389), insert_book(403), insert_club_book(406),
insert_book_suggestion(409), insert_meeting(412,+FK books), upsert_user_progress(420),
upsert_book_rating(431), insert_book_favorite(443), delete_book_favorite(449),
upsert_profile(458), delete_vote(470), edit_comment(480), delete_comment(486),
remove_comment(490), restore_comment(498), mark_notification_read(506), insert_report(513),
insert_user_block(527), delete_user_block(533), moderate_remove(539,RPC). Kick drain@551.

**TRAVA DE DRAIN-SAFETY:** tryDrainPendingQueue(267) itera em ordem createdAt e para no
1º transiente pra preservar dependência (insert_reaction depende de insert_comment;
insert_meeting depende do seed de book). TODOS os handlers precisam estar num único
`mutationHandlers` ANTES de qualquer drain — senão kind sem handler é DESCARTADO como
"unknown"(273). Por isso os 25 handlers + drainMutex + isPermanentError + enqueueMutation
+ kickImmediateDrain FICAM na engine, nunca espalhados por repos lazy.
