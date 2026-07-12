# Revisão de UX — Rodapé

**Data:** 2026-07-12 · **Método:** leitura página-a-página de todas as telas (5 auditorias paralelas), avaliadas contra heurísticas de Nielsen, estados vazios/primeiro uso, fricção de fluxo, affordances/hierarquia, consistência de copy PT-BR e descoberta de features.

**Como ler a severidade:**
- **P0** — quebra o fluxo ou **engana** o usuário (copy mentirosa, beco sem saída, ação destrutiva sem aviso). Corrigir primeiro.
- **P1** — fricção séria / confusão real.
- **P2** — polimento / refinamento.

> Este é o relatório. Nada foi alterado no código. Você decide item a item o que corrigir. A **exceção** já combinada: o "favorito de verdade" (coração) será construído à parte — e ele conserta os dois P0 da Estante como efeito colateral.

---

## Resumo executivo — 6 temas transversais

Antes das telas, os padrões que se repetem no app inteiro (corrigir o padrão vale mais que corrigir caso a caso):

1. **Voz PT-BR oscila entre "tu" e "você"** na mesma jornada — "TUA LEITURA" vs "Sua leitura", "Teus clubes" vs "Você deixa de ver…", "Teu voto" vs "Você vai participar?", "Conta pro pessoal por que tu sugere". Soa como feito por pessoas diferentes. **Recomendação: fixar "você" e revisar tudo com um mini guia de copy.**
2. **Ações destrutivas / de toggle sem verbo e sem confirmação** — remover voto ("Teu voto"), descartar rascunho de capítulos, apagar ata (toque fora do diálogo), sobrescrever índice inteiro. O usuário perde trabalho num toque.
3. **Copy que promete ação que não existe** — "toque na estrela pra favoritar" (a estrela avalia), "📚 populares — toque pra sugerir" (só seleciona), "toque em marcar frase" (o botão é "Voltar").
4. **Botões desabilitados sem dizer o que falta** — login, cadastro, criar clube, "Usar" URL de capa ficam cinza sem explicar a regra que falta cumprir.
5. **Estados vazios sem CTA (ou com CTA que não leva à ação)** — Estante, Frases, capítulos no primeiro uso, "0.0 de 5" quando ninguém avaliou.
6. **Features centrais escondidas** — a busca automática de capítulos vive atrás de um ícone "Refresh"; a aba "Próximo" (nome vago) esconde votação + encontros; o card-herói do "Próximo encontro" parece clicável mas não é.

---

## Prioridade P0 (engana ou trava — corrigir primeiro)

| # | Tela | Problema | Correção |
|---|------|----------|----------|
| P0-1 | Estante · filtro "Favoritos" | O pill "Favoritos" **não** mostra favoritos pessoais — mostra livros com **média do clube ≥ 4.5**, que o usuário não controla. (ShelfTabScreen.kt:74-76) | Resolvido pelo **coração de verdade** (o filtro passa a ser favorito pessoal). Enquanto isso: renomear pra "Bem avaliados". |
| P0-2 | Estante · empty de "Favoritos" | Copy manda *"Toque na estrela pra marcar como favorito"*, mas a estrela é **avaliação**, não favoritar. Instrui ação inexistente. (ShelfTabScreen.kt:108) | Coração de verdade torna a instrução verdadeira; até lá, copy honesta. |
| P0-3 | Votação · card do livro votado | Botão **"Teu voto"** (mesma cara do selo não-clicável acima) **remove o voto** ao toque — sem verbo, sem confirmação, sem "desfazer". (NextTabScreen.kt:938, 1004-1018) | Rotular a ação ("Remover voto" / "tocar p/ desfazer") e dar estado visual de toggle; deixar "Teu voto" só como selo. |
| P0-4 | Cadastro · muro de confirmação de email | Depois de cadastrar, a tela exige confirmar o email mas **não mostra qual email**, não tem "Reenviar" nem "Corrigir" — único botão é "Voltar para login". Quem errou 1 letra fica travado fora do app. (SignUpScreen.kt:106-121) | Mostrar o email enviado + botões "Reenviar" e "Corrigir email". |
| P0-5 | Capítulos · "Buscar online" | O ícone de busca **substitui a lista inteira sem confirmação nem undo** — admin que digitou/reordenou 40 capítulos perde tudo num toque. (ManageChaptersScreen.kt:92) | Confirmar antes de sobrescrever quando já há capítulos ("Isso substitui os atuais?"). |

---

## Por tela

### 1. Onboarding · Boas-vindas · Cadastro · Recuperar senha
*(primeiro contato — o momento mais crítico)*

- **[P0]** Cadastro — muro de confirmação de email sem email visível / sem reenviar / sem corrigir (ver P0-4). *(SignUpScreen.kt:106-121)*
- **[P1]** Welcome — **hierarquia de CTA invertida**: "Entrar" é o botão primário preenchido e "Criar conta" é outline secundário, mas o recém-chegado ainda **não tem conta** → é empurrado pro caminho que não consegue concluir. *Correção: no primeiro uso dar peso primário a "Criar conta" (ou igualar).* *(WelcomeScreen.kt:168-182)*
- **[P1]** Recuperar senha — título **"Te mandamos um link por email"** aparece **antes** de o usuário digitar/enviar → parece que já foi enviado. *Correção: título instrucional ("Digite seu email e enviamos o link").* *(ForgotPasswordScreen.kt:83-89)*
- **[P1]** Auth + criar clube — **botão cinza sem dizer o que falta** (senha 8+ com maiúscula/minúscula/dígito/símbolo; nome do clube ≥3). *Correção: validação inline por campo (checklist de senha) e/ou "falta X" ao tocar.* *(SignUpScreen.kt:61-68,191-207; WelcomeScreen.kt:317-319)*
- **[P1]** Onboarding — **submit final sem caminho de erro**: "Pronto!" seta `submitting=true` e chama `onComplete` sem callback de falha → num app offline-first, uma falha deixa spinner infinito barrando a entrada. *Correção: resetar estado + snackbar/retry.* *(OnboardingScreen.kt:59-60, 290-305)*
- **[P2]** Welcome — **proposta de valor rasa**: só a tagline explica o app antes de pedir conta; não deixa claro que é clube **presencial** com encontros. *Correção: 2-3 bullets acima dos CTAs.* *(WelcomeScreen.kt:147-154)*
- **[P2]** AuthErrors — mensagens de erro **sem acento** ("invalido", "conexao", "seguranca") enquanto o resto do app é acentuado — logo no momento de maior atenção. *Correção: acentuar todas.* *(AuthErrors.kt:22,27,45,54,72)*
- **[P2]** Entrar com código — o Box "Com código" tem **visual de aba/pill mas não é clicável** (resquício de segmented control removido) → toque morto. *Correção: virar título/label simples.* *(WelcomeScreen.kt:827-842)*
- **[P2]** Onboarding — 3 passos obrigatórios (avatar, apelido, fonte) **sem "Pular"**. *Correção: "Pular por agora" com defaults.* *(OnboardingScreen.kt:106-265)*

### 2. Navegação & Home
- **[P1]** Bottom nav — 4 das 5 abas ficam **só com ícone** quando inativas, e **"Próximo" (ícone de calendário)** é nome vago pra um hub de votação+encontros → reconhecimento vira decoreba. *Correção: rótulo sempre visível + renomear "Próximo" (ex: "Encontros" ou "Votação").* *(MainTabsScreen.kt:698-703, 760-782)*
- **[P1]** Home vs aba Livro — **copy contraditória pro mesmo estado**: a Home manda todo usuário "Sugerir o primeiro livro · comece a votação", mas a aba Livro só deixa admin "Abrir votação" (membro vê "Ver votação"). *Correção: unificar a regra por papel nos dois lugares.* *(MainTabsScreen.kt:966-975 vs 1399-1431)*
- **[P2]** Header (pill de clube) — com **1 clube só**, o chevron e a sheet dizem "Trocar de clube" (troca que não existe). *Correção: rotular "Meus clubes" e destacar criar/entrar.* *(MainTabsScreen.kt:574, 363)*
- **[P2]** Home ("Onde a galera tá") — pill diz "Na frente"/"No seu ritmo" mas o `contentDescription` do TalkBack lê "adiantado"/"no seu ritmo" (divergência). *Correção: mesmo texto no pill e na acessibilidade.* *(MainTabsScreen.kt:1204-1209 vs 1262-1272)*

### 3. Perfil
- **[P1]** EditProfileView — **nome de uma palavra só trava tudo**: `canSave` exige nome completo (2 palavras), então quem tem conta Google "Ana" **não consegue nem trocar o avatar**. *Correção: desacoplar avatar do nome e aceitar nome único.* *(MainTabsScreen.kt:2853, 2942-2946)*
- **[P2]** Stat "livros lidos" — conta só o **clube ativo**, mas o rótulo genérico sugere total do usuário → parece bug pra quem tem vários clubes. *Correção: "livros lidos neste clube" até haver agregação cross-clube.* *(MainTabsScreen.kt:2007-2024)*
- **[P2]** 3 stat cards — só "frases guardadas" navega (e **sem chevron**); os outros dois usam o mesmo card mas não são clicáveis → affordance inconsistente. *Correção: chevron no card navegável e/ou tornar os três consultáveis.* *(MainTabsScreen.kt:2009-2088)*
- **[P2]** Rodapé — **"Sair do clube X"** e **"Sair da conta"** são dois botões idênticos e colados (consequências muito diferentes) → convida ao toque errado. *Correção: "Sair do clube" como link discreto e afastar os dois.* *(MainTabsScreen.kt:2510-2534)*
- **[P2]** EditProfileView — edição ocupa a tela como modal, mas a **bottom bar continua ativa** e não há voltar no topo (só "Cancelar" lá embaixo) → trocar de aba abandona a edição sem aviso. *Correção: header com seta de voltar / rota dedicada que atenua a bottom bar.* *(MainTabsScreen.kt:2755-2762)*
- 🆕 **Nova seção "Meus livros favoritos"** entra aqui (ver design dos favoritos abaixo).

### 4. Próximo (votação + encontros) · Detalhe do encontro
- **[P0]** Votação — botão "Teu voto" remove o voto sem aviso (ver P0-3). *(NextTabScreen.kt:938, 1004-1018)*
- **[P1]** Encontro — **card-herói "Próximo encontro" não é clicável**, e o detalhe (ata, anotações, concluir) só abre pelos itens do cronograma, que só existem com `meetingsForBook.size > 1` → clube com **um** encontro nunca chega na ata. *Correção: tornar o herói clicável → `onNavigateToMeetingDetail`.* *(NextTabScreen.kt:337-430, 241, 261-269)*
- **[P1]** Encontro — o card-herói **omite a faixa de capítulos** ("Caps X–Y"), a informação mais acionável de um clube de leitura (aparece só nos cards secundários). *Correção: incluir "📖 Caps N–M" no herói.* *(NextTabScreen.kt:386-407)*
- **[P1]** Detalhe do encontro — **editor de ata dentro de AlertDialog**: tocar fora do diálogo (scrim) apaga a ata inteira digitada sem aviso. *Correção: confirmar descarte quando há rascunho, ou usar sheet/tela full-screen.* *(MeetingDetailScreen.kt:432-468)*
- **[P2]** Votação — estado vazio mostra **dois CTAs pra mesma ação** ("Sugerir o primeiro livro" primário + "Sugerir livro" outline no rodapé). *Correção: suprimir o do rodapé enquanto o empty exibe o primário.* *(NextTabScreen.kt:879 vs 1073)*
- **[P2]** Consistência — mesma seção com tratamentos diferentes entre telas irmãs: Title Case (`TbSectionHeader`) vs CAIXA ALTA (`labelSmall`); pauta chamada "Programação / pauta" numa tela e "AGENDA" na outra; RSVP "Vou/Talvez/Não vou" vs contadores "Confirmados/Talvez/Não vão". *Correção: padronizar componente de cabeçalho e termos.* *(NextTabScreen.kt:433/715/451 vs MeetingDetailScreen.kt:204/183; 510-668)*
- **[P2]** Detalhe do encontro — gate de loading fixa `hasData=false`, então um encontro válido que ainda sincroniza pode piscar **"Encontro não encontrado."** *Correção: derivar `hasData` de `meeting != null`.* *(MeetingDetailScreen.kt:93-97)*

### 5. Discussão & Capítulos
- **[P0]** Capítulos · "Buscar online" sobrescreve a lista inteira sem confirmação (ver P0-5). *(ManageChaptersScreen.kt:92)*
- **[P1]** Capítulos · TopBar — **toda a cascata automática** (comunidade → Open Library → Gutenberg → descrição) está atrás de um **ícone Refresh** com rótulo só de acessibilidade → lê como "recarregar", o recurso principal fica indescobrível. *Correção: botão com texto "Buscar capítulos online", em destaque no estado vazio.* *(ManageChaptersScreen.kt:84-106)*
- **[P1]** Capítulos · primeiro uso — livro sem capítulos cai em tela quase em branco (só "+ Adicionar" e campo "Nº"), **sem explicar os 3 caminhos** (buscar online / gerar N / manual). *Correção: estado vazio com contexto + "Buscar online" como ação primária.* *(ManageChaptersScreen.kt:185-285)*
- **[P1]** Capítulos · rodapé — "Cancelar" e o back **descartam o rascunho inteiro sem "descartar alterações?"**. *(ManageChaptersScreen.kt:78, 291-296)*
- **[P1]** Discussão · bolha de comentário — **reagir depende de tocar a bolha inteira, sem affordance visível** (só `contentDescription`) → ninguém descobre que dá pra reagir. *Correção: ícone visível de "reagir" na bolha ou ao segurar.* *(DiscussionScreen.kt:333-352)*
- **[P2]** Capítulos · banner de **falha** ("Não encontramos os capítulos") aparece num surface **verde** (cor de sucesso) → contradiz o conteúdo. *Correção: tom neutro/terracota na falha.* *(ManageChaptersScreen.kt:97-99,152-164)*
- **[P2]** Capítulos · gerador "Gerar N" é só um campo "Nº" + botão, **sem rótulo/explicação**. *Correção: microcopy "Não sabe os títulos? Gere N capítulos numerados".* *(ManageChaptersScreen.kt:253-283)*
- **[P2]** Discussão · autoria — o mesmo usuário é "Você" no avatar mas **"Tu" na bolha**, e desconhecido vira **"Iniciante"** (soa pejorativo; outras telas usam "Membro"). *Correção: padronizar pronome e usar "Membro" como fallback.* *(DiscussionScreen.kt:309, 357)*
- **[P2]** Discussão · barreira de spoiler — **"Revelar debate mesmo assim" é o botão dominante** e "Voltar" é link fraco (ação arriscada pesa mais que a segura). *Correção: inverter ênfase.* *(DiscussionScreen.kt:166-174)*

### 6. Detalhe do livro
- **[P1]** Avaliações — sem nenhuma avaliação o cabeçalho mostra **"0.0 de 5"** com 5 estrelas vazias → parece nota zero, não "ainda não avaliado". *Correção: estado vazio distinto ("Ninguém avaliou ainda — seja o primeiro").* *(BookDetailScreen.kt:638-649)*
- **[P2]** Salvar frase — capítulo em branco vira o texto solto **"Cap."** pendurado. *Correção: omitir a referência quando vazia.* *(BookDetailScreen.kt:305)*
- **[P2]** Abas — 5 abas de texto com `weight(1f)` igual; "Avaliações"/"Histórico" **truncam** em telas estreitas e as inativas têm baixa affordance. *Correção: `ScrollableTabRow` ou rótulos menores + reforçar estado inativo.* *(BookDetailScreen.kt:195-239)*
- 🆕 **Toggle ♥ Favoritar** entra no cabeçalho (ver design dos favoritos abaixo).

### 7. Estante · Frases · Sugerir · Cadastro manual
- **[P0]** Estante · filtro e empty enganosos (ver P0-1 e P0-2). *(ShelfTabScreen.kt:74-76, 108)*
- **[P1]** Sugerir · busca — durante o debounce de 400ms a tela pisca **"Não achamos nada — verifique a conexão"** antes de a busca rodar (o skeleton pensado pra isso foi calculado mas **nunca ligado no render**). *Correção: gatear o render por `searching` e mostrar o skeleton; erro só quando a busca termina.* *(SuggestScreen.kt:117, 235, 290-306)*
- **[P1]** Frases · empty — copy diz "Abra o livro atual e toque em marcar frase", mas o único botão é **"Voltar"** (não leva à ação). *Correção: CTA "Abrir livro atual" navegando pro detalhe do livro em leitura.* *(FrasesScreen.kt:168, 174-179)*
- **[P1]** Estante · empty geral — "Nenhum livro lido ainda…" **sem nenhum CTA** (o próprio código admite que "Sugerir uma leitura" foi omitido por falta de callback). *Correção: passar `onNavigateToSuggest` + botão.* *(ShelfTabScreen.kt:127-134)*
- **[P2]** Sugerir · populares — "📚 toque pra sugerir um" mas tocar só **seleciona**; sugerir exige 2º toque em "Adicionar" longe do item. *Correção: copy "toque pra selecionar" e/ou barra sticky de confirmar.* *(SuggestScreen.kt:268, 283-285)*
- **[P2]** Adicionar livro · terminologia — 4 verbos pro mesmo objetivo: "Sugerir"+"Adicionar" (Suggest) vs "Cadastrar"+"Salvar livro" (Manual). *Correção: padronizar o verbo do domínio.* *(SuggestScreen.kt:124,169; AddBookManualScreen.kt:150,451)*
- **[P2]** Frases · card — quando o livro não está em `clubBooks`, o rótulo cai pra `capituloRef` e mostra **"Cap. 3" no lugar do título**. *Correção: fallback "Livro removido".* *(FrasesScreen.kt:211-212)*
- **[P2]** Cadastro manual · capa por URL — "Usar" fica **desabilitado sem explicação** quando a URL não é `https://`. *Correção: validar no clique com "Use um endereço https://".* *(AddBookManualScreen.kt:292-298)*

---

## Sugestão de sequência (quando você decidir corrigir)

1. **Quick wins de honestidade** (baratos, alto impacto): P0-2/P0-1 (via coração), P0-3 rótulo do voto, "0.0 de 5", banner verde de erro, empty da Estante/Frases com CTA. Corrigem a sensação de "app enganoso".
2. **Segurança contra perda de dado**: confirmação de descarte em capítulos e ata (P0-5, P1 ata).
3. **Primeiro uso**: hierarquia "Criar conta", muro de email (P0-4), estado vazio de capítulos, guiar "primeiro livro".
4. **Consistência**: guia de copy "você" + termos padronizados (pauta, cabeçalhos, RSVP), rótulos da bottom nav.
5. **Descoberta**: renomear "Próximo", "Buscar capítulos online" com texto, card-herói clicável.
