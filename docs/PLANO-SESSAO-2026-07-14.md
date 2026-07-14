# PLANO DA PRÓXIMA SESSÃO — Checkpoint visual + fecho final (2026-07-14)

> **Para o agente:** leia PRIMEIRO `docs/HANDOFF-UI-2026-07-13.md` (regras, comandos,
> segredos, padrões técnicos) e depois este plano. O handoff manda; este doc só
> ordena o trabalho. **Compile a cada batch, commite a cada batch, secret-check
> antes de todo commit** (`git diff --cached --name-only | grep -iE "\.env|adminsdk|secret|key\.json"`
> tem que voltar vazio). Repo é PÚBLICO.

## CONTEXTO EM 5 LINHAS

O plano visual (`docs/PLANO-UI-2026-07-13.md`, Ondas 0–4) foi TODO implementado
nas sessões anteriores: Onda 0/1 (fundação), Onda 2 (movimento, 9 commits),
Onda 3 (13 momentos-assinatura), Onda 4 batch 1 (splash, predictive back,
status bar, crossfade, pull-refresh, snackbar). Tudo compila e está pushado
(`master`, HEAD `14c8ead`+). **NADA disso foi visto rodando** — foi feito por
verificação de compile. Esta sessão existe pra: (1) validar visualmente TUDO em
device e consertar o que quebrou; (2) pagar os débitos que exigiam device; (3)
fechar release.

- Máquina WSL: build funciona (`./gradlew :app:compileDebugKotlin -x processDebugGoogleServices --no-configuration-cache -q`; exit 0 = ok).
- APK atual: `rodape-1.1.4.apk` na Área de Trabalho do Windows (`/mnt/c/Users/gabfe/OneDrive/Área de Trabalho/`). Emulador: ver `docs/BUILD-SETUP.md` (setup turnkey).
- Versão: 1.1.4 (code 18). Se for lançar no fim: bump 1.1.5 (code 19).

---

## FASE 1 — CHECKPOINT VISUAL GUIADO (bloqueia tudo; ~metade da sessão)

Instalar o APK (ou rodar no emulador) e percorrer o roteiro abaixo. Para cada
problema: consertar em batch pequeno, compilar, commitar (`fix: checkpoint —
<tela>: <o quê>`). Prioridade: quebrado > feio > polimento.

### Roteiro tela-a-tela (ordem de navegação real) + riscos conhecidos

1. **Cold start** → splash de marca (fundo paper + ícone). Risco: ícone pequeno/
   deslocado (ajustar `windowSplashScreenAnimatedIcon`).
2. **Intro (4 páginas)** → ⚠️ RISCO MAIS ALTO DA SESSÃO: as 4 spot illustrations
   (livro/prateleira/conversa/calendário em `IntroScreen.kt#IntroArt`) foram
   desenhadas ÀS CEGAS em geometria pura. Conferir proporção/centralização
   dentro do disco de 148dp. Parallax no swipe (arte anda mais rápido que texto).
3. **Welcome** → stagger pílula→headline→subhead→4 lombadas "pousando";
   lombadas com sombra tingida (não pode parecer borrão).
4. **Login/Signup/Forgot** → transições de nav direcionais (avançar desliza da
   direita, voltar devolve).
5. **Home** → greeting+headline em stagger; MeetingTicket com notches ANCORADOS
   na linha picotada (testar tb com fonte grande); checklist "Primeiros passos"
   (anel de progresso, círculos morfando, conector enchendo); pull-to-refresh
   terracota; snackbar novo (ink/cream/dourado).
6. **Navbar** → trocar as 5 abas: NADA pode pular/reflow; pílula pinta suave;
   ícone dá pop; fade-through entre abas preservando scroll de cada uma.
7. **Aba Livro** → ⚠️ RISCO ALTO: backdrop ambiente (capa borrada alpha 0.30
   sobre olivaDeep) — conferir LEGIBILIDADE do texto cream por cima com capas
   claras; se ruim, baixar alpha pra ~0.20 ou adicionar scrim. Anel de progresso
   enche; capa entra em fade+rise; abas internas em fade-through.
8. **Discussion** → barreira de spoiler com cadeado; "Revelar" dissolve o debate;
   "Marcar que li" é TbButton Terra.
9. **NextTab / Encontro** → ⚠️ RISCO ALTO: ticket físico novo (canhoto oliva +
   day-stamp + picote + notches — notches têm que sentar EXATOS na linha, cor
   igual ao fundo da tela nos 2 temas); RSVP semântico (Vou=oliva, Talvez=
   dourado, Não vou=neutro) + carimbo com háptico; "Quem vai?" (barra empilhada
   + avatares espiando nas linhas colapsadas).
10. **NextTab / Votação** → líder com aro dourado + selo "Na frente"; votar em
    outro livro REORDENA com animação; countdown pulsando; sheet de abrir
    votação (stepper com ícones −/+, PillToggles).
11. **MeetingDetail** → header = canhoto expandido do ticket (concluído fica
    neutro); RSVP com mesmas cores.
12. **Estante** → ⚠️ RISCO MÉDIO: cover-first (capa 128×192 SEM card branco —
    conferir em tela estreita se 2 colunas cabem; se apertado, reduzir pra 120);
    sombra de prateleira; sem estrelas vazias em livro sem rating; filtro
    "♥ Favoritos (N)" como segmented; empty state com prateleira geométrica;
    stagger das capas ao rolar.
13. **Frases** → ⚠️ TESTAR DE VERDADE o share-as-image: tap/long-press no card →
    sheet → "Compartilhar como imagem" → o PNG no chooser tem que ser o card
    exato (gradiente + aspas). Se crashar, o suspeito é o FileProvider
    (authority `${applicationId}.fileprovider` — no DEBUG o id tem sufixo
    `.debug`; o código usa `context.packageName`, então deve funcionar — mas é
    exatamente o tipo de coisa que só o runtime prova). QuoteCard keepsake:
    aspas emoldurando sem colidir com texto longo/curto; acento variando.
14. **Notificações** → trilho terracota no unread; swipe marca lida (fundo
    olivaSoft "✓ Lida", item volta); cores (celebração dourada, removido
    neutro); empty "Tudo em dia".
15. **Moderação** (admin) → timeline com rail + nós; bloco citado riscado.
16. **ManageClub** → hero dashboard (nome + pulso + capa); dialogs admin
    (RodapeDialog: raio 20, cream, radio/checkbox oliva).
17. **ManageChapters** → linha "Cap.N · título · ⋮" com menu; subir/descer ANIMA.
18. **Perfil** → dialogs (sair, excluir conta, feedback) com cara RodapeDialog.
19. **DARK MODE inteiro** → repetir passos 5–16 no escuro. Atenção: backdrop do
    hero, warning tokens do RSVP (Talvez), notches do ticket, QuoteCard.
20. **Reduced-motion** (Config. do sistema → remover animações) → TUDO vira
    instantâneo: navbar, shimmer (fill estático), stagger, carimbo, countdown
    parado, nav sem slide.

---

## FASE 2 — ROBUSTEZ DE DEVICE (4.2/4.4 do plano-mestre)

1. **Dynamic type**: fonte do sistema no MÁXIMO + escala interna A++ → varrer
   ticket (Home e NextTab), checklist, navbar, chapter-row, RSVP pickers,
   QuoteCard. Notches são ancorados (devem sobreviver) — confirmar.
2. **Contraste AA dark**: labels tint-sobre-tint (ticket, hero, dourado sobre
   olivaDeep). Accessibility Scanner ajuda.
3. **TalkBack sweep**: navbar (1 nome por aba, sem leitura dupla), ticket lê data
   completa, votação anuncia líder/voto (liveRegions), swipe de notificação tem
   ação alternativa (o tap já marca lida — ok).
4. **RTL rápido** (pseudo-locale ar): chevrons/Back do RodapeIcons NÃO auto-
   espelham — conferir onde importa; baixo risco pra pt-BR, registrar o que achar.
5. **Landscape/tablet**: ao menos nada quebrado; `WindowSizeClass` só se sobrar tempo.

## FASE 3 — DÉBITOS CONTÁVEIS (medidos no código em 2026-07-14)

1. **23 `AlertDialog` Material → `RodapeDialog`** em 8 arquivos:
   ManageClub (5), Discussion (4), BookDetail (4), MeetingDetail (3),
   ManageChapters (3), NextTab (2), Welcome (1), Suggest (1).
   Mecânica já provada (ver commits `0d1bda9`, `dcec544`). Radios/checkboxes
   dentro deles → `ThemedRadio`/`ThemedCheckbox`.
2. **3 `.copy(fontSize)` de headline** no NextTabScreen (linhas ~1261/1405/1598
   — headlineLarge espremido pra 16/20sp): trocar pelo token certo
   (`titleMedium`/`headlineMedium`) CONFERINDO o render.
3. **Ícones Material restantes** (intencionais até existir vetor): Visibility/
   VisibilityOff (3+3), Refresh (2), VerifiedUser, Settings, FormatQuote,
   Favorite/FavoriteBorder. Se desenhar: geometria simples + conferir visual
   (regra do handoff). Settings (engrenagem) é o mais visível (header admin).
4. **ChatTab do BookDetail** reusar a bolha Literata do DiscussionScreen (3.6).
5. **Estados de erro/offline padronizados** (4.3): retry consistente; pill de
   sync com tom/posição únicos.

## FASE 4 — UPGRADES QUE EXIGIAM DEVICE (fazer o que couber, nesta ordem)

1. **Drag-to-reorder real** no ManageChapters (lib `sh.calvin.reorderable` ou
   detectDragGesturesAfterLongPress) — validar NO DEVICE, é gesto.
2. **Shared-element ticket→detail** (`SharedTransitionLayout` no NavHost +
   `Modifier.sharedElement` no day-stamp). Experimental — só com tempo.
3. **Recompensa de marco de leitura** (25/50/100%: mola/confete + háptico ao
   "Marcar progresso" — `MainTabsScreen` ~2260).
4. **Onboarding**: corpo do step com AnimatedContent + footer fixo (reestrutura
   da LazyColumn); unificar indicador first-run (stretch-bar do Intro no
   Onboarding, no topo); preview de fonte animando ao trocar chip.
5. **Identidade de movimento/háptico**: mapa curto em doc (quando vibrar: RSVP ✔,
   marco ✔ — o resto é proposta).

## FASE 5 — FECHO E RELEASE

1. Atualizar `docs/HANDOFF-UI-2026-07-13.md` com o que foi validado/consertado.
2. Bump `versionName = "1.1.5"` / `versionCode = 19` em `app/build.gradle.kts`.
3. `./gradlew :app:assembleRelease --no-configuration-cache` (assinatura vem do
   `.env` + `my-upload-key.jks` — NUNCA ecoar valores).
4. Verificar assinatura (`apksigner verify --print-certs`), copiar pra Área de
   Trabalho como `rodape-1.1.5.apk` (apagar o 1.1.4), commit + push.
5. Se for subir na Play: `bundleRelease` (.aab) também.

## REGRAS INEGOCIÁVEIS (resumo do handoff)

- Compile após CADA batch; commit pt-BR (`fix:`/`feat:` + trailer Co-Authored-By).
- `git add` explícito SEMPRE; secret-check antes de todo commit; repo público.
- Cor por `RodapeTheme.colors.*`; raio por `RodapeRadii`; motion por
  `rodapeTween`/`rodapeSpring` (reduced-motion de graça); `transitionSpec` não é
  composable (specs hoisteados); punch de mola com guard de 1ª composição;
  escala animada sempre via `graphicsLayer`.
- Não inventar vetor às cegas; não mexer em `.env`/`google-services.json`.
