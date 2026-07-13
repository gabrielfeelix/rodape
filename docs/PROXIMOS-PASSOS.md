# Próximos passos — features maiores adiadas

> Adiadas de propósito na 1.1.1 (revisão de fluxo do ciclo do clube). São
> **features**, não bugs — por isso ficaram fora do lote de correções, pra não
> serem feitas meia-boca. Contexto completo em [REVISAO-UX-2026-07-12.md](REVISAO-UX-2026-07-12.md).

---

## 1. Checklist guiado de clube novo (onboarding do ciclo)

**Problema (achado #8 da auditoria de fluxo):** um clube recém-criado mostra 4 telas
vazias desconexas (Home, Livro, Encontro, Votação), cada uma com seu CTA, mas nada
encadeia a **ordem real** do ciclo. O organizador não sabe por onde começar.

**Ordem real do ciclo a guiar:**
1. Abrir votação (admin)
2. Sugerir livros (todos)
3. Votar
4. Encerrar votação → livro atual definido
5. Cadastrar/gerar índice de capítulos
6. Membros leem e marcam progresso
7. Agendar encontro (faixa de capítulos)
8. Encontro acontece → concluir
9. Finalizar livro → (agora já promove o próximo da fila automaticamente, 1.1.1)

**O que construir:** um **checklist de progresso do clube** (ex.: card na Home ou uma
faixa no topo) que mostra em que passo o clube está e qual é o próximo, marcando os
concluídos. Some quando o clube já está "rodando" (tem livro atual + capítulos +
encontro agendado).

**Onde:** `MainTabsScreen.kt` (Home/estado inicial ~949; aba Livro vazia ~1399),
`NextTabScreen.kt` (~172/862). Estado derivável do que já existe no `MainViewModel`:
`activeVotingRound`, `suggestedBooks`, `currentBook`, `currentChapters`, `latestMeeting`.
Nenhum dado novo — é composição de UI sobre estados existentes.

**Por que vale:** transforma 4 telas vazias confusas num caminho óbvio; é o maior
ganho de UX pro primeiro clube.

---

## 2. Buscar/adicionar livro dentro do diálogo "Trocar livro"

**Problema (achados #7 e #12):** o diálogo "Trocar livro" (Gerenciar clube) só lista
livros já `suggested`/`next`. Se não há nenhum, ele diz *"Sugira um livro primeiro na
aba Votação"* — um beco que dependia da sugestão estar visível.

**Estado atual (mitigado na 1.1.1):** as sugestões agora ficam **visíveis fora da
rodada** ("Sugestões na fila"), então o beco circular foi bastante reduzido — o admin
consegue sugerir e depois trocar. Mas o ideal continua sendo **buscar/adicionar um
livro ali mesmo** no diálogo, sem sair pra aba Votação.

**O que construir:** embutir a busca de livro (o mesmo `BookSearchService` /
`SuggestScreen`) dentro do diálogo "Trocar livro", pra definir a leitura atual em um
lugar só — inclusive um caminho canônico "definir leitura atual" direto na página do
livro (achado #7: hoje há dois caminhos divergentes — encerrar votação vs. trocar
manual — sem sinalização de qual usar).

**Onde:** `ManageClubScreen.kt` (~760, ~885), reaproveitando
`data/search/BookSearchService.kt` e o fluxo de `SuggestScreen.kt`.

**Por que vale:** unifica os dois caminhos de "definir livro atual" e tira a última
dependência de ordem escondida.
