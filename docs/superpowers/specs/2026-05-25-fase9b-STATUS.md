# Fase 9B — Status (2026-05-25)

## TL;DR

**Fase 9B: ✅ COMPLETA.** App Android Compose totalmente ligado no Supabase.
RodapeRepository (Room) substituido por RemoteRepository (Postgrest + RPCs).
MainViewModel arrancou seed/auto-login fake. Build + lint verdes. Instalado
no device. 3 commits atomicos depois da 9A.

**Fase 9C: ⏸️ PROXIMA.** Apagar Room/AppDatabase/RodapeDao/Entities + DataStore-session,
migrar font_scale pra profiles, upload de capas pro bucket book-covers.

## O que muda na pratica (smoke test no device agora)

1. **Login** (email/senha ou Google): ja funcionava. Tela vazia agora vira tela
   vazia *real* (sem dados de Marina/Beatriz/Lucas/seed do Room).
2. **Bom dia, Gabriel** + **email correto** no perfil: corrigido (le do JWT).
3. **Criar clube**: chama RPC `create_club` no servidor.
   - Servidor cria row em `clubs`, te insere em `club_members` como `super_admin`,
     gera codigo de convite unico — atomicamente.
   - App auto-seleciona o novo clube.
4. **Entrar em clube com codigo**: chama RPC `join_club_with_code`.
   - Servidor valida codigo, verifica que voce nao e ja membro, te insere.
   - Tab "Com link" sumiu (so codigo de 6 chars).
5. **Sugerir livro / votar / fechar round**: inserts em `books`/`club_books`/
   `book_suggestions`/`votes`. Fechar usa RPC `close_voting_round` que faz tudo
   no servidor (marca current->finished, promove vencedor, notifica todos).
6. **Comentar capitulo / reagir**: inserts em `comments`/`reactions`. Tudo
   passa por RLS (so membros do clube veem/escrevem).
7. **Promover/rebaixar/transferir super_admin/remover membro/sair**: todos via
   RPC com invariantes garantidas no servidor (1 super_admin por clube, etc).
8. **Encontros + RSVPs + ata + notas**: ligados ao banco.
9. **Frases salvas + avaliacao de livros + resumo wiki**: idem.

## Decisoes pragmaticas tomadas

- **Mesma interface publica**: RemoteRepository tem as 93 funcoes com nomes
  identicos ao RodapeRepository (Room). Resultado: zero refator nas 22 telas
  Compose. So o `MainViewModel` mudou nome de uma instancia.
- **Sem Realtime WebSocket ainda**: caches sao MutableStateFlow refreshadas
  via polling sob demanda. UI atualiza quando a tela e reaberta ou quando
  uma acao trigga refresh. Realtime fica pra fase futura — simplicidade > vivacidade.
- **DTOs internos** (mesmo arquivo do repo): conversao bidirecional
  snake_case <-> camelCase + Long epoch ms <-> ISO 8601 timestamptz. `Entities.kt`
  fica intacto (UI nao mudou).
- **activeClubId vira state em memoria** (nao persiste cold-start). Auto-seleciona
  primeiro clube do usuario ao logar.
- **createClub/joinClubWithCode/promote/demote/transfer/remove/leave** via RPCs
  SECURITY DEFINER (todas ja existiam no banco da fase 8).
- **seedDatabase() = no-op**: app nasce vazio. Sem mais Marina/Beatriz/Lucas.
- **login(name, email, ...) virou stub no-op** pra preservar interface (algumas
  telas antigas ainda chamam por compat).
- **Vote schema**: dominio usa `clubBookId`, banco usa `book_id`. Mapeio no DTO.
- **Meeting.data + .hora (Strings)** -> banco `data timestamptz`: faco parse
  "DD/MM/YYYY" + "HH:MM" no insert; no read formato de volta como
  "DOMINGO, 25 DE MAIO DE 2026" + "19:00".

## Riscos conhecidos / a validar no smoke

- **Schema divergente Meeting.data**: parser de "DD/MM/YYYY" pode falhar com
  formatos antigos (ex: "Próxima semana", "DOMINGO, 24 DE OUTUBRO" como string
  livre). Esses caem no fallback `now()`. Verificar criacao de encontro.
- **Notification.payloadJson como String <-> jsonb**: parsing pode falhar se
  algum legacy notification tem JSON malformado. Use `coerceInputValues` no
  Json config pra tolerar.
- **clearVotesForUserInClub** agora precisa fazer 2 queries (round_ids + delete).
  Pequeno overhead, aceitavel.
- **insertClubMember e insertMemberRemoval viraram no-op**. Se algum codigo do
  MainViewModel ainda chama esses metodos pensando que fazem algo, vai falhar
  silenciosamente. Validar smoke.
- **JoinClubScreen mostra so "Com codigo"** mas a variavel `activeTabIsCode`
  ainda existe — os branches `if (activeTabIsCode)` continuam, branch else fica
  morto. Funciona, so e codigo zumbi. Limpar depois.

## Roteiro de smoke test (faca isto no device)

```
1. Logout (perfil -> sair).
2. Fazer login com Google. ✓ ver email/nome corretos no perfil.
3. Welcome -> "Criar clube".
   - Preencher nome "Teste 9B" + descricao + cor + privacidade.
   - Confirmar -> deve cair em main_tabs com o clube criado vazio.
4. Em outra conta (ou pedir pra alguem): entrar com o codigo gerado.
   - Voltar pra primeira conta -> ver o membro novo em "Onde a galera ta".
5. Sugerir 1-2 livros via Open Library na aba Indicar.
6. Abrir votacao (admin so).
7. Votar nos livros sugeridos.
8. Fechar votacao -> ver vencedor virar "lendo agora".
9. Comentar num capitulo. Reagir em comentario de outro membro.
10. Editar resumo do livro atual.
11. Avaliar livro com estrelas + comentario.
12. Criar encontro presencial.
13. Dar RSVP "Vou".
14. Salvar uma frase via Frases.
15. Verificar notificacoes recebidas.
16. Promover membro a admin. Depois rebaixar.
17. Transferir super_admin pra esse admin promovido. Validar invariante.
18. Logout. Login de novo. Estado preserva.
```

Se 1-15 funcionam, **a 9B esta funcionalmente completa**. Se 16-18 funcionam,
RPCs SECURITY DEFINER tao OK.

## Bugs esperados (admito que podem aparecer)

- Telas vazias ate o primeiro Flow refreshar (latencia HTTP). Se incomodar,
  adicionar shimmer/loading.
- Erros de RLS aparecendo no Logcat com prefixo `PostgrestRestException`.
- Datas de encontro com formatos antigos quebrando — usar criador de encontro
  novo que vai usar parser dd/MM/yyyy.

## Migracao de dados do usuario antigo

Nao se aplica — voce e o unico usuario, e o profile Gabriel Felix ja foi
criado na primeira tentativa de login Google. Banco Supabase tem 1 row em
`auth.users` e 1 em `profiles`. Tudo virgem dali pra frente.

## O que falta pra 9C (proxima sessao)

1. Apagar `data/db/AppDatabase.kt`, `data/db/RodapeDao.kt`,
   `data/repository/RodapeRepository.kt`, `data/model/Entities.kt`.
   (DTOs internos do RemoteRepository ja independem disso — vou ter que
   mover algumas data class do Entities pra outro lugar acessivel.)
2. Remover deps Room + ksp Room do `app/build.gradle.kts` e versions.toml.
3. Remover `USER_ID_KEY`, `USER_NAME_KEY`, `USER_EMAIL_KEY`, `ACTIVE_CLUB_ID_KEY`
   do `DataStoreManager`. Manter `RATED_APP_KEY`, `ENGAGEMENT_COUNT_KEY`,
   `FONT_SCALE_KEY` (`font_scale` migra pra `profiles.font_scale`, mas guarda
   cache local).
4. `AddBookManualScreen`: trocar `file://` local por upload pro bucket
   `book-covers` (path `<club_id>/<book_id>/<uuid>.jpg`).
5. Limpar variavel zumbi `activeTabIsCode` no JoinClubScreen + branch else morto.
6. Limpar fun `login(name, email, ...)` stub se ninguem mais chama.
