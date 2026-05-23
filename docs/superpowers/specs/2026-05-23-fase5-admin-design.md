# Fase 5 — Admin do clube

**Status:** proposto
**Data:** 2026-05-23
**Autor:** Gabriel (com Claude)
**Substitui parcialmente:** [Spec da Fase 4](2026-05-23-fase4-estante-votacao-design.md) — esta fase **estende** o flag `papel` em `ClubMember` (antes só `admin`/`member`, agora `super_admin`/`admin`/`member`) e adiciona toda a UI/lógica de gestão.
**Precondições:** Fases 1–4 concluídas e mergeadas na `master`. App offline-only, Room v3, `applicationId = app.tramabook`.

---

## 1. Objetivo

Dar ao admin do clube **poder real de organização**. Hoje o flag `papel = "admin"` só libera 2 ações (abrir/encerrar votação, marcar data do encontro). Esta fase introduz:

1. Hierarquia de 3 papéis (`super_admin`/`admin`/`member`) com regras de transição.
2. Tela dedicada **"Gerenciar clube"** com 7 seções (info, código, membros, encontros, livro atual, moderação, zona de risco).
3. Gestão completa de encontros via **padrão recorrente** (dia da semana fixo) + edição pontual de cada ocorrência.
4. Gestão de capítulos com tentativa via APIs externas (Open Library + Google Books) e fallback manual.
5. Moderação: admin remove comentários alheios (placeholder no lugar), remove membro com motivo + notificação.
6. Trocar livro atual manualmente sem precisar de votação.
7. Arquivar clube (super_admin only).

**"Banco de dados depois" = backend remoto/sync depois.** Esta fase usa o Room local, com migração v3 → v4 destrutiva (app ainda não publicado).

## 2. Princípios não-negociáveis

- **3 papéis fixos, sem permissões custom.** Não há combinação de "admin que faz X mas não Y" além das diferenças definidas entre super_admin e admin.
- **Invariante: todo clube ativo tem exatamente 1 super_admin.** Aplicado em código (transições e saída de membro).
- **Reaproveitar UI existente.** Tela "Gerenciar clube" segue o design system do app (TramabookCard, TbButton, Pill, Avatar, paleta atual). Sem novos tokens.
- **Moderação como log, não como apagar.** Comentário removido vira placeholder. Membro removido vira registro em `MemberRemoval`. Histórico não some.
- **YAGNI sobre encontros.** Um único padrão recorrente por clube ativo + edição pontual. Sem múltiplos padrões, sem mensal/quinzenal misturados.
- **Sem chamada de IA, sem scraping.** Capítulos via API tenta Open Library + Google Books; se falhar, manual.

## 3. Hierarquia de papéis

Campo `ClubMember.papel` passa a aceitar 3 valores:

| Papel | Quantidade por clube | Capacidades exclusivas |
|---|---|---|
| `super_admin` | exatamente 1 | promover member→admin, rebaixar admin→member, transferir super, arquivar clube |
| `admin` | 0+ | tudo de "operacional" (ver §6) |
| `member` | 0+ | só consumo/colaboração |

### 3.1 Regras invariantes

1. **Todo clube ativo tem 1 super_admin.** Sem exceção.
2. **Quando super_admin sai do clube:**
   - Se há ≥1 admin: o admin mais antigo (menor `entrouEm`) é promovido automaticamente a super_admin. Sair é permitido.
   - Se há 0 admin: **bloqueia a saída.** Mensagem: "Você é o único administrador. Promova alguém ou arquive o clube antes de sair."
3. **Quando admin (não super) sai:** sem restrição, sai direto.
4. **Quando member sai:** sem restrição.
5. **Super não pode se rebaixar diretamente.** Para mudar, ele **transfere o título** pra outro admin (que vira super), e ele vira admin no mesmo gesto.
6. **Promover member a admin:** qualquer super_admin faz. Não há limite de quantos admins existem.
7. **Rebaixar admin a member:** qualquer super_admin faz. Não pode rebaixar a si mesmo (use transferir).
8. **Membro removido (kick):** registrado em `MemberRemoval`. Pode ser feito por qualquer admin (não só super).

### 3.2 Quem foi inicialmente cada papel

- Quem **cria** o clube vira `super_admin` automaticamente.
- Quem entra **via código** vira `member` automaticamente.
- Migração do seed: `Mariana` (criadora) vira `super_admin`. `user_voce` continua `admin` (pra você poder testar fluxos admin).

## 4. Schema de dados (Room v3 → v4)

### 4.1 Alterações em tabelas existentes

```kotlin
// ClubMember.papel agora aceita 3 valores: "super_admin" | "admin" | "member"
// Sem mudança estrutural — só a semântica do campo.

@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey val id: String,
    val nome: String,
    val descricao: String,
    val codigo: String,
    val cor: String,
    val privacidade: String,
    val criadorId: String,
    val criadoEm: Long,
    val arquivado: Boolean            // NOVO
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String,
    val chapterId: String,
    val clubId: String,
    val userId: String,
    val texto: String,
    val criadoEm: Long,
    val removido: Boolean,            // NOVO (default false)
    val removidoPor: String?,         // NOVO
    val motivoRemocao: String?        // NOVO
)
```

### 4.2 Novas tabelas

```kotlin
@Entity(tableName = "meeting_patterns")
data class MeetingPattern(
    @PrimaryKey val id: String,
    val clubId: String,
    val diaSemana: Int,           // 1=DOM ... 7=SAB (Calendar.SUNDAY..SATURDAY)
    val hora: String,             // "19:00"
    val local: String,
    val agendaTemplate: String,
    val ativo: Boolean            // pattern desativado quando admin escolhe "sem recorrência"
)

@Entity(tableName = "member_removals")
data class MemberRemoval(
    @PrimaryKey val id: String,
    val clubId: String,
    val userId: String,
    val removedByUserId: String,
    val motivo: String,           // pode ser ""
    val removedAt: Long
)
```

### 4.3 Migração

`AppDatabase` versão 3 → 4. Manter `fallbackToDestructiveMigration()`. Seed recria o estado consistente.

**Seed novo:**
- `Mariana` vira `super_admin` em `club_mari` (no seed atual está como `admin`).
- `user_voce` continua como `admin` (já está).
- Demais membros permanecem `member`.
- Adicionar 1 `MeetingPattern` ativo no `club_mari`: `(diaSemana=DOMINGO, hora="19:00", local="Café Lispector, Vila Madalena", agendaTemplate="Discussão do livro atual")`.
- O `Meeting` existente (`meet_1`) continua, com data fixa do seed.
- Comentários do seed têm `removido=false`, `removidoPor=null`, `motivoRemocao=null`.

## 5. Notificações novas

Adicionar 3 tipos novos (no campo `DbNotification.tipo`):

- `member_removed` — payload `{motivo: string, clubName: string}`. Disparado quando alguém é removido do clube. Vai pro `userId` removido.
- `promoted_to_admin` — payload `{clubName: string, promotedBy: string}`. Disparado quando member vira admin.
- `super_admin_transferred` — payload `{clubName: string, fromUser: string}`. Disparado quando o super transfere o título.

Renderização dessas notificações na tela de Notificações é **fora de escopo** desta fase (caem como notificações genéricas). Próxima fase pega isso.

## 6. Matriz de capacidades (definitiva)

| Capacidade | member | admin | super_admin |
|---|---|---|---|
| Ver livros, capítulos, comentários, frases, resumos, encontros | ✓ | ✓ | ✓ |
| Comentar em capítulo | ✓ | ✓ | ✓ |
| Reagir a comentário | ✓ | ✓ | ✓ |
| Deletar PRÓPRIO comentário | ✓ | ✓ | ✓ |
| RSVP em encontro | ✓ | ✓ | ✓ |
| Votar em sugestão | ✓ | ✓ | ✓ |
| Sugerir livro (com justificativa) | ✓ | ✓ | ✓ |
| Salvar/editar/deletar PRÓPRIAS frases | ✓ | ✓ | ✓ |
| Escrever/editar resumo (wiki) | ✓ | ✓ | ✓ |
| Adicionar/editar PRÓPRIA avaliação | ✓ | ✓ | ✓ |
| Sair do clube | ✓ | ✓ | bloqueado se 0 admin (ver §3.1) |
| Editar info do clube (nome/descr/cor/privacidade) | — | ✓ | ✓ |
| Regenerar código de convite | — | ✓ | ✓ |
| Remover membro (com motivo) | — | ✓ | ✓ |
| Editar/criar encontros (padrão e avulso) | — | ✓ | ✓ |
| Editar/criar capítulos do livro atual | — | ✓ | ✓ |
| Marcar livro atual manualmente (sem votação) | — | ✓ | ✓ |
| Marcar livro como finalizado | — | ✓ | ✓ |
| Marcar/limpar data do encontro de livro lido | — | ✓ | ✓ |
| Abrir/encerrar rodada de votação | — | ✓ | ✓ |
| Remover sugestão de livro alheia | — | ✓ | ✓ |
| Remover comentário alheio (vira placeholder + log) | — | ✓ | ✓ |
| Ver log de moderação | — | ✓ | ✓ |
| Promover member → admin | — | — | ✓ |
| Rebaixar admin → member | — | — | ✓ |
| Transferir super_admin (e virar admin) | — | — | ✓ |
| Arquivar clube | — | — | ✓ |

## 7. Tela "Gerenciar clube"

### 7.1 Acesso

Adicionar **ícone de engrenagem (`Icons.Outlined.Settings`)** no header do `MainTabsScreen`, ao lado do nome do clube. Visível apenas quando `isCurrentUserAdmin` é true (qualquer um dos 2 níveis de admin). Tocar abre `ManageClubScreen`.

### 7.2 Estrutura geral

`ManageClubScreen.kt` — Scaffold com `TopAppBar` "Gerenciar clube" + back button. Conteúdo em `LazyColumn` com 7 seções, todas abertas por padrão (sem colapse). Cada seção é um `TramabookCard` com título + conteúdo + ações.

```
[← voltar]    Gerenciar clube

┌───────────────────────────────────────────┐
│ ℹ️ Informações do clube                    │
│ Leituras de domingo                       │
│ Um clubinho clássico...                   │
│ Cor: ●  Privacidade: Só convidados        │
│                              [Editar]     │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐
│ 🔑 Código de convite                       │
│       XK7M2P                              │
│        [Copiar]  [Gerar novo]             │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐
│ 👥 Membros (6)                             │
│  [👤] Mariana       Super admin           │
│  [👤] Você          Admin           [⋮]   │
│  [👤] Rafael        Membro          [⋮]   │
│  [👤] Júlia         Membro          [⋮]   │
│  ...                                       │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐
│ 📅 Encontros                               │
│ Padrão: Domingos, 19h, Café Lispector     │
│                              [Editar]     │
│ Próximo encontro: 24/mai                  │
│                   [Editar]  [Cancelar]    │
│              [+ Encontro avulso]          │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐
│ 📖 Livro atual                             │
│ A Hora da Estrela — Clarice Lispector     │
│ Capítulos: 13                             │
│ [Trocar livro]  [Gerenciar capítulos]     │
│ [Marcar como finalizado]                  │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐
│ 🛡️ Moderação                              │
│ 0 comentários removidos                   │
│                            [Ver log]      │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐ (só super_admin)
│ ⚠️ Zona de risco                          │
│ Arquivar este clube remove ele da lista   │
│ ativa. Histórico é preservado.            │
│                       [Arquivar clube]    │
└───────────────────────────────────────────┘
```

### 7.3 Seção: Informações do clube

Mostra nome, descrição, swatch da cor, label de privacidade ("Só convidados" / "Aberto a quem tem link").

**[Editar]** abre `EditClubInfoDialog`:
- Campos: nome (max 40), descrição (max 140), color picker (mesmas 6 cores de `ClubColors`), radio "Só convidados" / "Aberto a quem tem link".
- Salvar atualiza `Club` no Room.

### 7.4 Seção: Código de convite

Mostra código atual em fonte grande.

- **[Copiar]** copia pro clipboard via `ClipboardManager`, mostra `Snackbar("Código copiado")`.
- **[Gerar novo]** abre dialog: "Gerar um novo código vai invalidar o atual. Quem ainda não entrou precisa receber o código novo. Continuar?" → confirma → gera novo `codigo` aleatório (6 chars, mesma fórmula da criação) e salva no `Club`.

### 7.5 Seção: Membros

Lista de membros com Avatar + nome + label do papel ("Super admin" / "Admin" / "Membro").

- **Super admin** não tem menu (não pode ser modificado a partir daqui).
- **Admin** tem menu `[⋮]` (visível só para super_admin). Opções:
  - "Rebaixar a membro" → confirma → vira `member`.
  - "Transferir super_admin pra este admin" → confirma → este vira super, atual super vira admin.
  - "Remover do clube" → abre `RemoveMemberDialog` (qualquer admin pode).
- **Membro** tem menu `[⋮]` (visível para admin e super_admin). Opções:
  - "Promover a admin" (só super_admin vê) → confirma → vira `admin`.
  - "Remover do clube" → abre `RemoveMemberDialog`.

**`RemoveMemberDialog`:**
- Título: "Remover {nome}?"
- Campo: "Motivo (opcional)" — textarea de 2 linhas
- Aviso: "A pessoa recebe uma notificação. Comentários e frases dela ficam no histórico."
- Botões: Cancelar / Remover (vermelho)
- Submeter: cria `MemberRemoval`, deleta `ClubMember`, cria `DbNotification` tipo `member_removed` pro userId removido.

**`MemberActionSheet`** — `ModalBottomSheet` com as ações acima. Implementado como sheet pra ficar mais polido que dialog.

### 7.6 Seção: Encontros

Bloco superior: **Padrão recorrente**.
- Se ativo: "Domingos, 19h, {local}" + **[Editar]**.
- Se inativo: "Sem padrão recorrente definido." + **[Definir padrão]**.

Bloco inferior: **Próximo encontro**.
- Se existe `Meeting` futuro: mostra data + hora + local + **[Editar]** + **[Cancelar]**.
- Se não existe: "Nenhum encontro agendado." + **[+ Criar encontro]**.

Plus button: **[+ Encontro avulso]** — cria um Meeting fora do padrão (data específica que não segue o padrão).

**`EditMeetingPatternDialog`:**
- Dropdown de dia da semana (7 opções)
- TimePicker de hora
- Campo de local (texto livre)
- Campo de agenda template (texto livre, max 140)
- Toggle "Padrão ativo" (default ON)
- Salvar atualiza/cria `MeetingPattern`.

**`EditSingleMeetingDialog`:**
- DatePicker de data
- TimePicker de hora
- Campo de local
- Campo de agenda (max 280)
- Salvar atualiza/cria `Meeting`.

**`CancelMeetingDialog`:**
- "Cancelar este encontro? Os RSVPs serão descartados."
- Cancelar / Cancelar encontro (vermelho)
- Submeter: deleta `Meeting` + `MeetingRsvp`s associados.

### 7.7 Seção: Livro atual

Mostra título + autor + número de capítulos.

- **[Trocar livro]** abre `ChangeCurrentBookDialog`:
  - Lista de livros com status `suggested` ou `next` no clube.
  - Botão "Buscar outro livro" (abre `SuggestScreen` em modo "selecionar pra atual").
  - Selecionar → confirma → o livro atual vira `finished` (com `dataEncontro = now`), o escolhido vira `current`. Mesma lógica do `closeRoundInternal`.
- **[Gerenciar capítulos]** abre `ManageChaptersScreen`.
- **[Marcar como finalizado]** → confirma → livro atual vira `finished` (com `dataEncontro = now`). Não promove ninguém — admin pode usar [Trocar livro] depois.

### 7.8 Seção: Moderação

Contador de comentários removidos no clube + **[Ver log]** → abre `ModerationLogScreen`.

**`ModerationLogScreen.kt`** — lista de cards:
- Avatar + nome do autor original
- "Comentário em Cap. X" (link pro capítulo)
- Texto original (riscado, cor `Muted`)
- "Removido por {nome}" + "Motivo: {motivo}" + timestamp relativo
- Botão **[Restaurar]** (só super_admin) — desfaz a remoção.

### 7.9 Seção: Zona de risco (super_admin only)

Card de fundo `Terracota.copy(alpha = 0.05f)` ou similar, borda `Terracota`.
- **[Arquivar clube]** → dialog: "Arquivar 'Leituras de domingo'? Você e os membros não verão mais este clube na lista. Você pode reativar depois nos Arquivados. Histórico é preservado."
- Confirma → `Club.arquivado = true`. Repository filtra clubes arquivados de `getClubsForUser`. Se este era o `activeClubId`, troca pro próximo disponível ou volta pra Welcome se não houver clube.
- **Reativar:** seção "Clubes arquivados" no Perfil (super_admin vê seus arquivados). Toca → reativa.

## 8. In-context admin actions

Ações pequenas que não exigem tela nova, mas vivem dentro de telas existentes:

### 8.1 Menu de comentário (DiscussionScreen + ChatTab)

Cada comentário ganha um ícone `Icons.Outlined.MoreVert` no canto direito quando:
- Autor próprio: aparece "Deletar"
- Admin (de qualquer nível): aparece "Remover (moderação)"

Tocar "Remover (moderação)" abre dialog: campo motivo opcional + Confirmar. Submeter: marca `removido=true`, `removidoPor=currentUserId`, `motivoRemocao=motivo`. O comentário fica como placeholder no UI: `Card cinza claro, texto "[mensagem removida pela moderação]"`, mostrando ainda avatar+nome do autor original e timestamp original.

### 8.2 Menu de sugestão (VotacaoTab)

Cada card de sugestão ganha menu `[⋮]` (admin only):
- "Remover sugestão" → confirma → deleta `ClubBook` + `BookSuggestion`. Votos vão junto (cascade lógico).

### 8.3 Ícone de engrenagem no header

Adicionar `Icons.Outlined.Settings` no `MainTabsScreen` topbar, à direita do nome do clube (antes do sino de notificações). Visível só pra admin/super_admin do clube ativo. Tap navega pra `ManageClubScreen`.

## 9. Capítulos via API

Quando admin abre `ManageChaptersScreen` em **modo "popular"** (livro acabou de virar `current` e não tem capítulos):

1. **Tenta Open Library:** chama `/works/{olid}/editions.json` se `Book.openlibraryId` existir. Procura por `table_of_contents` em qualquer edição.
2. **Tenta Google Books:** `https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}` ou `q=intitle:{title}+inauthor:{author}` se sem ISBN. Procura por `volumeInfo.tableOfContents` ou similar.
3. **Valida:** considera "sucesso" se retornou ≥3 entradas com títulos não vazios.
4. **Se sucesso:** abre `ManageChaptersScreen` com a lista pré-populada em modo "Revisar e confirmar" (admin pode editar/remover entradas antes de salvar).
5. **Se falha:** abre `ManageChaptersScreen` em modo "vazio" com banner: "Não encontramos os capítulos deste livro. Adicione manualmente ou use 'Cap. 1, 2, 3...' como base." + botão "Gerar N capítulos" (admin escolhe N, cria `Cap. 1` até `Cap. N` sem títulos).

**Sem chamada de IA, sem scraping.** Adicionar `GoogleBooksApi.kt` análogo ao `OpenLibraryApi.kt` existente. Sem auth.

### 9.1 `ManageChaptersScreen`

- Tabela editável: cada linha é "Cap. N" + campo de título (editável inline)
- Botão "+ Adicionar capítulo" no final
- Cada linha tem ícone de lixeira pra remover
- Reordenação: setas pra cima/baixo em cada linha (drag-to-reorder fora de escopo)
- Botão "Salvar" persiste todas as mudanças de uma vez

## 10. Sair do clube — comportamento

A ação "Sair do clube" (no Perfil, na lista de clubes) já existe? Verificar — se não existir, adicionar como **member action** (não admin). Comportamento:

- Se `member`: sai direto.
- Se `admin` (não super): sai direto.
- Se `super_admin`:
  - Se existe ≥1 outro admin no clube: super é transferido automaticamente pro admin mais antigo. Notificação `super_admin_transferred` enviada ao novo super. Saída prossegue.
  - Se 0 admin: bloqueia com dialog: "Você é o único administrador. Promova alguém a admin antes de sair, ou arquive o clube."

## 11. Mapa de arquivos

### Novos
- `app/src/main/java/com/example/ui/screens/ManageClubScreen.kt` — tela hub principal de admin.
- `app/src/main/java/com/example/ui/screens/ManageChaptersScreen.kt` — gerenciar capítulos do livro atual.
- `app/src/main/java/com/example/ui/screens/ModerationLogScreen.kt` — log de moderação.
- `app/src/main/java/com/example/ui/admin/AdminDialogs.kt` — diálogos compartilhados (EditClubInfo, RemoveMember, EditMeetingPattern, EditSingleMeeting, CancelMeeting, ChangeCurrentBook).
- `app/src/main/java/com/example/data/api/GoogleBooksApi.kt` — wrapper Retrofit pra Google Books API.
- `app/src/main/java/com/example/voting/ChapterFetcher.kt` — orquestra OL + GB e valida resultado.

### Alterados
- `app/src/main/java/com/example/data/model/Entities.kt` — `Club.arquivado`, `Comment.removido/removidoPor/motivoRemocao`, novas entities `MeetingPattern` e `MemberRemoval`.
- `app/src/main/java/com/example/data/db/AppDatabase.kt` — version 3 → 4, entities atualizadas.
- `app/src/main/java/com/example/data/db/TramabookDao.kt` — DAOs novos pra MeetingPattern, MemberRemoval, e queries de admin (update papel, soft-delete comment, archive club, etc).
- `app/src/main/java/com/example/data/repository/TramabookRepository.kt` — passthrough + seed atualizado (Mariana vira super, MeetingPattern do clube demo).
- `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` — flows novos: `currentUserPapel`, `meetingPatternForActiveClub`, `memberRemovalsForActiveClub`. Ações novas: `editClubInfo`, `regenerateInviteCode`, `promoteMember`, `demoteAdmin`, `transferSuperAdmin`, `removeMember`, `upsertMeetingPattern`, `upsertMeeting`, `cancelMeeting`, `changeCurrentBook`, `markCurrentBookFinished`, `upsertChapters`, `removeComment`, `restoreComment`, `removeSuggestion`, `archiveClub`, `unarchiveClub`, `leaveClub`. Plus `isCurrentUserSuperAdmin` flow.
- `app/src/main/java/com/example/ui/screens/MainTabsScreen.kt` — ícone de engrenagem no header (admin only), navegação pra ManageClubScreen.
- `app/src/main/java/com/example/ui/screens/DiscussionScreen.kt` — menu de 3 pontos em cada comentário; renderização de placeholder pra `removido=true`.
- `app/src/main/java/com/example/ui/screens/BookDetailScreen.kt` — mesma coisa no ChatTab.
- `app/src/main/java/com/example/ui/screens/NextTabScreen.kt` — menu de 3 pontos em cada sugestão (admin).
- `app/src/main/java/com/example/MainActivity.kt` — rotas novas (`manage_club`, `manage_chapters`, `moderation_log`).

## 12. Critérios de aceite (UAT)

1. **Ícone de engrenagem:** logado como admin/super, ícone aparece no header do clube ativo. Não aparece pra member.
2. **Tela Gerenciar clube:** abre com 7 seções (admin) ou 6 seções (super_admin vê Zona de risco extra... wait — super vê 7, admin vê 6). Admin não vê Zona de risco.
3. **Editar info do clube:** mudança aparece imediatamente em todas as telas (nome no topbar, descrição na Home → "Sobre o clube").
4. **Regenerar código:** novo código gerado, antigo invalidado (tentar entrar com antigo retorna erro padrão).
5. **Promover/rebaixar:** super promove um member, label muda. Super rebaixa um admin, label muda. Promovido recebe notificação `promoted_to_admin`.
6. **Transferir super:** super transfere pra outro admin. Quem recebeu vira super, quem transferiu vira admin. Notificação `super_admin_transferred` pra novo super.
7. **Remover membro:** admin remove um member, dialog pede motivo, member é removido, recebe notificação `member_removed`, perde acesso (não aparece em allClubs na próxima abertura).
8. **Sair do clube (super, único admin):** bloqueia com dialog explicativo.
9. **Sair do clube (super, com outros admins):** transfere automaticamente, sai.
10. **Padrão de encontro:** admin edita dia/hora/local, salva. Próximo encontro reflete o padrão.
11. **Encontro avulso:** admin cria com data específica. Aparece como "próximo encontro" se for o mais próximo no futuro.
12. **Trocar livro atual:** admin escolhe um suggested. Vira current. Antigo vira finished com dataEncontro=now.
13. **Capítulos via API:** ao trocar livro pra um clássico (ex: "1984"), tela ManageChapters abre com lista pré-populada. Pra livro obscuro, abre vazia com banner explicativo.
14. **Editar capítulos:** admin adiciona "Cap. 1", "Cap. 2"... salva. Aparecem na tela Livro atual.
15. **Remover comentário:** admin remove. Placeholder aparece pra todos. Log em Moderação registra.
16. **Restaurar comentário:** super_admin clica em "Restaurar" no log. Placeholder some, comentário original volta.
17. **Remover sugestão:** admin remove uma sugestão da votação. Some da lista de sugestões + votos.
18. **Arquivar clube:** super arquiva. Clube some da lista de allClubs. Visível em "Arquivados" no Perfil. Reativar funciona.

## 13. Pontos abertos e assumptions

- **Renderização de notificações novas (`member_removed`, `promoted_to_admin`, `super_admin_transferred`):** fora de escopo. Tela de Notificações renderiza como genérica. Próxima fase pega.
- **Frases/avaliações/resumo de membro removido:** continuam visíveis com avatar+nome originais. Não anonimiza. (Decisão explícita: histórico do clube é coletivo.)
- **Comentário removido com reações:** reações são mantidas no DB mas não renderizadas no placeholder.
- **Recuperar membro removido:** fora de escopo. Pessoa removida só volta entrando de novo pelo código.
- **Múltiplos padrões de encontro:** fora de escopo. Um único padrão ativo por vez. Trocar do "Domingos" pra "Quartas" requer editar o existente.
- **Drag-to-reorder de capítulos:** fora de escopo. Usa setas cima/baixo.
- **Edição em lote de papéis:** fora de escopo. Um membro de cada vez.

## 14. Riscos

- **Migração v3→v4 apaga dados locais.** Aceitável porque app não foi publicado.
- **Loops de transição de papel:** se super_admin é rebaixado por engano (não deve acontecer por design — super só rebaixa via transfer), checks defensivos no ViewModel garantem que toda transição mantém invariante de 1 super por clube.
- **APIs externas instáveis:** Google Books ou Open Library podem ter rate limit ou ficar fora do ar. Fallback é manual, então degrada graciosamente.
- **Tela Gerenciar clube fica longa:** 7 seções num scroll só. Se ficar cansativo, próxima fase pode introduzir tabs internas. Por enquanto YAGNI.
- **Notificações novas sem renderização específica:** vão aparecer feias na lista de notificações. Aceitável temporariamente.

## 15. Fora de escopo (declarado)

- Renderização polida das notificações novas (member_removed, promoted_to_admin, super_admin_transferred).
- Backend remoto / sync entre dispositivos.
- Push notifications reais.
- Permissões custom (granular).
- Auditoria completa (só log de remoção de comentário e remoção de membro).
- Comentários fixados (pinned).
- Encontros recorrentes além do padrão único.
- Drag-to-reorder de capítulos.
- Edição em lote.
- Recuperar membro removido sem novo código.
- Transferência de "propriedade" de conteúdo ao remover membro (frases ficam dele mesmo).
