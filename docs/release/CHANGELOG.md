# Changelog — Rodapé

## v1.1.3 — 2026-07-12 (em progresso) · Onda 1 do plano de pendências

Correções priorizadas a partir do estudo de 15 personas (`docs/PERSONAS-2026-07-12.md`)
e do plano em `docs/PLANO-PENDENCIAS-2026-07-12.md`. Onda 1 = "destrava quem importa".

### ✨ Intro de primeiro uso (novo)
- Antes do welcome, uma **introdução de 4 telas** ("como funciona o Rodapé") explica o
  produto — clube privado, ler no ritmo do grupo, discutir sem spoiler, encontros de
  verdade. **Pulável** a qualquer momento; aparece uma vez por device (DataStore
  `intro_seen`). UI fiel ao design (Literata/Inter, terracota/oliva), sem assets
  externos.

### 🧭 Checklist guiado agora é POR PAPEL (A1)
- O admin conduz o ciclo: **Abrir votação → Encerrar votação → Cadastrar capítulos →
  Agendar encontro**. O passo "Encerrar votação" (que faltava) fecha o vão entre
  "votaram" e "tem livro atual".
- O membro vê só o que consegue fazer: **Sugerir um livro → Votar → Ler e marcar
  progresso** (antes via tarefas de admin que não podia executar).

### 🗳️ Aba "Encontros" virou "Clube" + badge de votação (A2)
- A votação vivia numa aba chamada "Encontros" — ninguém achava. Agora a aba é
  **"Clube"** (hub de votação + encontros) e ganha um **ponto de destaque** quando há
  **votação aberta**.

### 📖 "Li vários — escolher o capítulo" (D1)
- Além do "Marcar progresso" (+1), dá pra **pular direto pro capítulo lido** — fim do
  toque-toque-toque pra quem leu em bloco ou voltou atrasado.

### 🔊 Acessibilidade: ações agora são faladas (C1)
- Live regions no **voto** ("Seu voto"), no **RSVP**, no **envio de comentário**
  ("Comentário enviado") e na **pill de sync** — quem usa leitor de tela deixa de
  "agir às cegas".
- Bônus: o rótulo das abas parou de ser lido 2× no TalkBack.

### ✅ Pill de sync com fim explícito (G1)
- Depois de sincronizar, mostra **"Tudo salvo ✓"** por 2s em vez de a rodinha só
  sumir (que parecia "travado" pra leigo).

### Onda 2 — acessibilidade e entrada

- **B1 · "Tenho um convite" no Welcome** — o convidado cola o código **antes** de
  criar conta; o código é retido e o **ingresso no clube acontece automaticamente**
  depois da auth. Fim do "tive que criar conta pra só então achar onde colar o código".
- **B2 · Checklist de senha na redefinição** — a tela de nova senha agora mostra o
  mesmo checklist ao vivo do cadastro (✓/○ por regra), em vez de um botão cinza mudo.
  Regra de senha extraída pra um componente único (`PasswordChecklist`).
- **B4 · Timeout em criar/entrar clube** — "Criando…"/"Entrando…" não travam mais pra
  sempre em sinal ruim: 15s → mensagem "Demorou demais. Verifique a conexão".
- **C2 · Reagir sem abrir sem querer** — reagir a um comentário virou **long-press**
  (ou toque no ícone de reação, agora um alvo real de 48dp); tocar a bolha pra reler
  não abre mais o seletor de emoji.
- **C4 · Botões do Welcome anunciados** — "Criar conta"/"Entrar" agora anunciam
  "botão" no TalkBack (eram `Box` clicáveis mudos).
- **C5 · Alvos de toque ≥ 48dp** — botão de enviar comentário, ícone de reagir e link
  "Sair do clube".
- **C3 · Menos truncamento em fonte grande** — título do livro na Estante em 2 linhas.
  *(As abas do detalhe do livro em `ScrollableTabRow` ficaram pra um passe com
  verificação visual em emulador.)*
- **I1 · Linguagem mais simples** — "Ata do encontro" → **"Resumo do encontro (ata)"**;
  "possível spoiler" → **"pode contar o que vem (spoiler)"**.
- **B3** — o "salvar perfil falha em silêncio" já estava, na prática, **coberto pela
  camada de dados** (`insertUser` é local-first + fila offline); sem mudança de risco.

### Onda 3 — push notifications (F1), backend pronto

- **Backend completo e seguro** (não quebra o build): `supabase/migrations/0007_push_device_tokens.sql`
  (tabela `device_tokens` + RLS + trigger que chama a função a cada notificação) e a
  Edge Function `supabase/functions/send-push/index.ts` (resolve tokens + FCM HTTP v1).
- **Falta ligar** com suas credenciais Firebase — passo-a-passo em
  `docs/release/push-fcm-setup.md`. Enquanto não liga, o trigger é **no-op seguro**.
  O cliente Android (FirebaseMessagingService + registro de token + permissão) fica
  como código pronto pra colar no runbook, **fora do build** de propósito (sem
  `google-services.json` o build quebraria).

### Onda 4 — polimento e regras de negócio

- **H1 · Busca não esconde livro sem capa** — o Open Library deixou de derrubar
  resultados válidos só por não terem capa (o app já mostra capa-placeholder).
- **B5 · Regra de senha coerente** — o login parou de impor um "mín. 6" que divergia
  do cadastro ("8 forte") e podia barrar senha legítima; agora só pede a senha.
- **G2 · Excluir conta pede confirmação por digitação** — numa ação irreversível, o
  botão só habilita quando você digita **"EXCLUIR"** (o "Pedir por email" continua).
- **A4 · Salvar só o avatar** — trocar a foto não depende mais do nome estar "válido"
  (só exige nome válido quando o nome está sendo de fato alterado).
- **E2 · Mais reações** — a paleta foi de 5 pra 10 emojis (2 linhas de 5).
- **F2 · Deep-link de notificação robusto** — passou a ler o payload pelo JSON
  parseado em vez de recortar string (não quebra se o servidor reordenar campos).

### Adiado com critério (decisão de produto)

- **J1 · Dark mode** — aprovado como **opção em Configurações**, mas o app hoje usa
  **constantes de cor fixas** (Ink/Cream/Paper…) em vez de tokens de tema, então é um
  **rework** (reescrever as referências de cor pra serem theme-aware), não um toggle.
  Shipar meia-boca deixaria "buracos brancos". Fica pra um passe dedicado com
  verificação visual em emulador.
- **C6 · Avatar neutro + pronome** — exige um **asset de ilustração** novo e uma
  **coluna de pronome** no banco; escopo de design + migração, adiado.
- **C3 (abas do livro em `ScrollableTabRow`)**, **B6 (código morto do JoinClub)** e
  **I2 (unificar fallback de nome)** — mudanças visuais/cosméticas que ficam melhor com
  checagem visual; anotadas em `docs/PLANO-PENDENCIAS-2026-07-12.md`.

## v1.0.12 — 2026-07-12 (build 13)

### 📅 Agendar encontro ficou fácil de achar
Antes, criar/agendar o próximo encontro estava **enterrado** em Gerenciar clube →
Encontros → "+ Novo encontro" — ninguém achava. Agora:
- Na aba **Próximo › Encontro** (onde todo mundo procura o encontro), o admin tem um
  botão grande **"Agendar encontro"** no estado vazio, e um **"+ Agendar outro
  encontro"** quando já existe encontro. Abre o mesmo editor (data, hora, local,
  capítulos), com data/hora/local já pré-preenchidos pelo padrão do clube e a faixa de
  capítulos sugerida.
- Na **Home**, o card "Nenhum próximo encontro" agora tem **"Agendar encontro"** (admin)
  que leva direto pra lá.

## v1.0.11 — 2026-07-12 (build 12)

### ✨ Capítulos automáticos: comunidade + EPUB (feature completa)
Como as APIs não dão índice de capítulos de ficção, o app agora tem uma **cascata de
fontes** no "buscar online" (Gerenciar clube → Capítulos), na ordem:

1. **Índice compartilhado pela comunidade (crowdsourcing por ISBN)** — quando um admin
   cadastra os capítulos de um livro e marca "compartilhar", aquele índice fica
   disponível **pra todos os clubes** que lerem o mesmo ISBN depois. Um cadastro serve o
   app inteiro. É a melhor fonte pra ficção em português. (Nova tabela `chapter_templates`
   no Supabase, com RLS: qualquer membro lê; contribuições registram o autor.)
2. **Open Library** (`table_of_contents`) — técnico/inglês.
3. **EPUB do Project Gutenberg** — clássicos de domínio público. Testado ao vivo:
   **Dom Casmurro = 153 capítulos**, Frankenstein = 32, Sherlock Holmes = 18. Baixa o
   EPUB, lê o índice (nav.xhtml / toc.ncx) e preenche.
4. **Descrição do Google Books** — último recurso.

O primeiro que acertar vence; o admin revisa e salva. Ao salvar um livro com ISBN, um
checkbox oferece **compartilhar o índice com a comunidade** (marcado por padrão).

## v1.0.10 — 2026-07-12 (build 11)

### ✨ "Buscar online" agora usa o Open Library (índice de capítulos)
Pesquisa a fundo (com testes ao vivo): a ÚNICA fonte pública/grátis/sem-chave com
lista de capítulos é o `table_of_contents` do **Open Library**. Cobertura é enviesada:
ótima pra técnico/inglês (ex.: Clean Code = 358 entradas, Design Patterns = 10), mas
**romances e livros em português quase sempre vêm vazios**. O botão "buscar online" em
Gerenciar clube → Capítulos agora consulta o Open Library por ISBN (filtrando só
entradas de capítulo reais), com fallback pra descrição do Google Books. Quando não
acha, o admin usa o **gerar N capítulos**. (Nenhuma API resolve capítulos de ficção de
forma confiável — é uma limitação dos dados, não do app.)

## v1.0.9 — 2026-07-12 (build 10)

Verificado no banco ao vivo que o servidor **aceita** troca de voto e progresso (upsert
HTTP 200) — os bugs restantes eram client-side.

### 🔴 "Trocar pra esse" virava "teu voto" e voltava, em loop
Causa: a PK LOCAL de votos permite várias linhas por usuário, e o fluxo fazia
delete-remoto + insert-remoto separados; um reload com estado antigo do servidor
re-adicionava o voto anterior e podava o novo. **Correção:** troca ATÔMICA — apaga os
votos do usuário na rodada localmente e faz **um único upsert** com `onConflict` na PK
(round,user), que substitui no servidor numa operação só; o reload de reconciliação só
roda no sucesso. Sem loop.

### 🔴 "Marcar progresso" não fazia nada
Causa: o botão só agia se `próximo_capítulo <= nº_de_capítulos`. Como capítulos não
sincronizavam (bug anterior) e muitos livros estão com **0 capítulos**, o clique era
silenciosamente ignorado. **Correção:** agora avisa "Cadastre os capítulos do livro" em
vez de não fazer nada; com capítulos, funciona normalmente.

### ✨ Gerar capítulos de uma vez
As APIs de livro (Google Books/Open Library) **não fornecem a lista de capítulos** de
forma confiável. Em vez de raspar a descrição (falhava quase sempre), agora dá pra
**informar o número de capítulos e gerar todos** de uma vez em Gerenciar clube →
Capítulos (títulos continuam opcionais). Isso destrava o "marcar progresso".

## v1.0.8 — 2026-07-12 (build 9)

### 🔴 "Clico e não vai" — CAUSA RAIZ REAL (não era rede nem conexão)
Investigação a fundo (código + banco ao vivo + medição de latência + docs supabase):
o banco responde em ~200ms e as escritas **funcionam** no servidor. O problema era
**reatividade da UI** — dois bugs de arquitetura:

- **Votar / "trocar pra esse" não aparecia:** a tela de votação lia os votos de
  `getVotesForClubFlow`, um `MutableStateFlow` preenchido por **um único fetch de
  rede** e que **nunca observava o banco local**. O voto era gravado (local + servidor),
  mas a UI lia de um snapshot congelado — só atualizava se você saísse e voltasse.
  **Correção:** a UI agora usa `votesForActiveRound`, um flow **Room-reativo** escopado
  à rodada ativa. O voto otimista aparece na hora.
- **"Marcar progresso" avançava e revertia:** `insertUserProgress` disparava um reload
  do servidor **antes** de a escrita remota confirmar, sobrescrevendo o valor otimista
  com o antigo ("pisca e some"). Esse fluxo tinha ficado de fora da correção anterior
  dos 12 fluxos. **Correção:** o reload agora só roda no sucesso da escrita.

Isso explica por que os ajustes de rede da 1.0.7 não resolveram: o defeito estava na
camada de estado da UI, não na conexão.

## v1.0.7 — 2026-07-12 (build 8)

### ⚡ Ações de DB agora resolvem na hora (fim do "1 alteração aguardando conexão")
- **Diagnóstico (medido):** o banco/rede está rápido — round-trip ~200ms. O problema
  NÃO era o servidor. Era o caminho de recuperação: quando uma ação falhava por um
  tropeço **transitório** (rádio frio / conexão fria no primeiro toque), ela caía na
  fila offline e o **único** gatilho pra re-sincronizar era o `DrainQueueWorker`
  (WorkManager, não-expedited) — que só roda **minutos** depois. Por isso o
  "1 alteração aguardando conexão" ficava preso.
- **Correção 1 — retry inline:** antes de desistir pra fila, a ação tenta de novo
  2–3× com 350–700ms de intervalo. Como a rede volta em <1s, a maioria das ações
  agora nem chega a mostrar "aguardando".
- **Correção 2 — drenagem imediata em processo:** se ainda assim cair na fila, o app
  drena na hora (retries de 0,25s→5s) no próprio processo, em vez de esperar o
  WorkManager. O worker vira só fallback pra quando o app está fechado.
- Resultado: votar, marcar progresso, RSVP, avaliar, comentar etc. sincronizam em
  ~1s. A UI já era otimista (aparece na hora); agora o selo de sincronização some
  quase imediatamente.

## v1.0.6 — 2026-07-12 (build 7)

Correção dos P0 de dados que estavam pendentes na 1.0.5 — agora **validados contra o
banco Supabase ao vivo** (Management API).

### 🔴 Capítulos e comentários agora sincronizam entre dispositivos (P0-1 + B2)
- **Causa raiz (provada no banco):** o app gerava id de capítulo em TEXTO
  (`ch_<bookId>_<numero>`) e mandava pra coluna `uuid` do servidor → Postgres
  rejeitava com `22P02` (erro engolido) → capítulos **nunca** sincronizavam e os
  comentários iam pra dead-letter. Num aparelho só o Room mascarava; entre membros,
  a discussão por capítulo não aparecia.
- **Correção:** identidade de capítulo agora é um `uuid` estável, gerado na tela e
  aceito pelo servidor. O vínculo comentário→capítulo é o id (uuid), então
  **reordenar/renumerar capítulos não remaneja mais os comentários** (bug B2).
- Validado ao vivo: id de texto → rejeitado (22P02); id uuid + comentário via FK →
  aceitos.

### 🔴 Criação offline não se perde mais (P0-2)
- Criar livro/livro-do-clube/sugestão/encontro **offline** (ou com o servidor
  falhando) agora entra na **fila de sincronização** em vez de virar uma linha
  local órfã que o próximo sync apagava silenciosamente. 4 novos handlers de
  replay drenam essas criações quando a rede volta (encontro empurra o livro antes,
  respeitando a FK).

### 🔴 Votação da 2ª rodada corrigida (B1)
- A votação lia votos do **clube inteiro** (todas as rodadas), então numa segunda
  votação os botões travavam em "Limite de votos atingido" e tocar em "Teu voto"
  (de rodada antiga) criava voto novo. Agora é **voto único por rodada** (como o
  servidor exige, PK round+usuário), escopado à rodada ativa: tocar no próprio voto
  desfaz, votar em outro troca. Sem falso "limite atingido".

## v1.0.5 — 2026-07-12 (build 6)

Revisão de app completa (6 auditorias paralelas do código real) → correções de
sensação de lentidão, bugs de comportamento e telas sem ação. Plano completo em
`docs/PLANO-CORRECOES-2026-07-12.md`.

### Performance (fim do "engasga")
- Discussão: Flows memoizados por capítulo (fim do refetch de rede em loop a cada
  recomposição), listas com `key` estável, `isCurrentUserAdmin` içado pra fora do
  `items{}`, reações/membros memoizados (`groupBy`/`associateBy`)
- Votação e avaliações: contagens/filtros memoizados em vez de recalcular por item
- Capas e avatares: `AsyncImage` (sem subcomposição) — rolagem mais leve em grids
- Realtime: canais desregistrados no `close()` (fim do acúmulo por rotação) e race
  de dedup fechada com `compute` atômico

### Skeletons (sensação de carregamento)
- Placeholders shimmer em todas as telas que carregam dados: Home, aba Livro,
  Estante, Votação/Encontro, Discussão, Detalhe do livro, Frases, Encontro,
  Notificações, Log de moderação, Cadastrar capítulos

### Telas vazias agora têm ação (CTA por papel)
- Aba "Livro" sem livro: ícone + orientação + botões (admin: gerenciar/abrir
  votação; membro: sugerir/ver votação)
- Estante: copy correta pro filtro "Favoritos" vazio + ícone/microcopy
- Capítulos não cadastrados: botão "Gerenciar clube" pro admin
- Votação sem sugestões e encontro sem agenda: CTA/microcopy por papel
- Frases vazias: ícone + explicação do fluxo + saída

### Bugs corrigidos
- Entrar em clube por código não empilha mais `main_tabs` duplicado (voltar sai
  do app em vez de reabrir a tela de código)
- Promover/rebaixar/transferir admin agora mostram erro real (antes: "sucesso"
  falso e nada mudava)
- Formulários (cadastro manual de livro, etc.) não se perdem mais na rotação
- Chat de capítulo rola pro comentário recém-enviado
- Barreira de spoiler não vaza mais numa corrida de carregamento
- Câmera: aviso de permissão não pisca mais atrás do diálogo do sistema
- Erros de login/cadastro nunca mais aparecem crus em inglês
- `MainActivity` com `launchMode=singleTop` (deep link de auth não duplica a tela)

### Pendente (exige validação em 2 dispositivos — ver plano)
- P0 de sincronização de capítulos/comentários (id de texto × uuid), criação
  offline sem fila e modelo de voto da 2ª rodada — corretos por diagnóstico, mas
  só se validam com o app rodando em contas/aparelhos diferentes

## Não lançado — 2026-07-11

### Visual (fidelidade ao Claude Design)
- Fontes Literata/Inter embarcadas (corrige queda silenciosa pra Roboto)
- Sombras suaves tingidas em vez da elevação Material cinza
- Header assinatura (avatar + pill de clube + sino), ícones próprios,
  ticket de encontro perfurado, capas com lombada, chips de leitores com anéis

### UX
- Estados de loading reais (fim dos falsos "não encontrado")
- Feedback de ações (snackbar), indicador de sincronização offline
- Pull-to-refresh, confirmações de ações destrutivas, desfazer exclusão de frase
- Voltar capítulo marcado sem querer

### Correções / robustez
- Testes voltaram a rodar; `GEMINI_API_KEY` não vai mais pro APK
- Texto de privacidade corrigido (condiz com o backend Supabase)
- Progresso de leitura não se perde mais offline (local-first + fila)
- Fila offline não é mais descartada no logout; poison messages têm dead-letter

### Produto
- **Exclusão de conta** dentro do app (requisito da Play Store)
- **Sair do clube** para membro comum

## v1.0.0 — 2026-05-24 (build 1)

**Primeira versão pública.**

### Funcionalidades principais
- Cadastro/login por email+senha ou Google Sign-In
- Criação e administração de clubes de leitura privados
- Convite por código + ingresso em clube
- Estante: livros lidos, atual, fila, sugestões
- Votação no próximo livro do clube
- Comentários por capítulo (sem spoiler pra quem está atrasado)
- Reações em comentários
- Frases salvas e exportação por share sheet
- Reuniões com RSVP
- Tela de perfil com avatar ilustrado (escolha automática por gênero do nome)

### Arquitetura
- Backend Supabase (Postgres + RLS + Realtime + Storage + Auth)
- Cache local Room com Single Source of Truth + Stale-While-Revalidate
- Fila offline de mutations com retry via WorkManager
- 73 policies de RLS validadas, 14 RPCs com `auth.uid()` + `SET search_path`
- R8 ativo no release (encolhe + ofusca + remove recursos)
- Backup automático Android desligado (privacidade)
- Crash logs persistidos localmente (nunca enviados sem autorização)

### Limitações conhecidas
- Sem push notifications (FCM) ainda — futuras versões
- HIBP (proteção contra senhas vazadas no Supabase) requer plano Pro, não
  habilitado nesta versão
- Captcha desabilitado (sem hCaptcha secret configurado)
- Sem dark mode (decisão de design)
