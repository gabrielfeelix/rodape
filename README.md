# Rodapé

App de **clube de leitura privado** para Android (iOS planejado). Reúna a galera
num clube só de vocês: sugiram livros, votem no próximo, leiam no ritmo do grupo
e discutam capítulo por capítulo sem estragar a leitura de quem está atrasado.

> Nome oficial e definitivo do produto: **Rodapé**.

---

## ⚠️ Situação atual (para agentes/devs — leia antes de mexer)

_Atualizado 2026-07-14._

**1. Chave de upload do Play foi RECRIADA (a original se perdeu).** Play App Signing está
ligado → a chave de assinatura do app (do Google) está intacta; só a chave de UPLOAD
(`my-upload-key.jks`, gitignored) foi resetada via Play Console. **Reset ativa ~16/07/2026**
(trava automática de 48h). Antes disso NÃO dá pra subir AAB novo.
- ⏳ **Ação humana pendente:** backup da `my-upload-key.jks` + senhas (`.env`) num gerenciador.
- ⏳ **Dia 16:** subir o AAB `versionCode 20` em Teste interno → baixar no cel.
- Detalhe + fingerprints: [`docs/release/PLAYSTORE-CHECKLIST.md`](docs/release/PLAYSTORE-CHECKLIST.md).

**2. Refactor de arquitetura em andamento (strangler-fig).** F1→F4a + F3c feitos: kernel
offline extraído (`SyncEngine`), Hilt scaffolding, e os 10 repos de domínio fatiados da
god-repo (`RemoteRepository` foi de 2654→574 linhas, virou fachada). **Próximo: F4b
SessionManager + F5 (VMs por tela).**
- ⚠️ **Backbone offline (F3b) + F3c ainda NÃO testados em device** — o vc20 (F3b+F4a) é o
  teste do dia 16; F3c entra em vc21+ depois. NÃO empilhar mudança no backbone assumindo
  que está validado.
- Ponto de partida obrigatório pro próximo agente: [`docs/arch/HANDOFF-refactor.md`](docs/arch/HANDOFF-refactor.md).

**3. Build (RAM apertada, 11Gi WSL):** compilar com `--no-daemon -Pkotlin.compiler.execution.strategy=in-process`
(o daemon separado crashava). `gradle.jvmargs=-Xmx3g`. Limpar daemons após cada build.

---

## Funcionalidades

- Cadastro/login por email+senha ou Google Sign-In
- Criação e administração de clubes privados (convite por código)
- Estante coletiva: lidos, atual e fila
- Votação no próximo livro
- Comentários por capítulo com barreira de spoiler
- Frases salvas com exportação
- Reuniões com RSVP
- Avatar ilustrado (sem foto/rede social obrigatória)

## Stack

- **UI:** Jetpack Compose + Material 3, design system próprio em
  `app/src/main/java/com/example/ui/theme/` (tokens fiéis ao protótipo em
  `claude-design/`)
- **Backend:** Supabase (Auth + Postgres + RLS + Realtime + Storage)
- **Local/offline:** Room como single source of truth, SWR com TTL, fila de
  mutações offline drenada via WorkManager
- **Min SDK:** 24 · **Target/Compile SDK:** 36 · **Kotlin** 2.2 · **AGP** 9.1

## Rodando localmente

Pré-requisito: Android Studio (versão recente).

1. Abra o projeto no Android Studio e deixe sincronizar.
2. Crie um arquivo `.env` na raiz (veja `.env.example`) com as chaves do
   Supabase e do Google OAuth. **Nunca comite o `.env`** — ele é gitignored e
   as chaves sensíveis são barradas do `BuildConfig` pela `ignoreList` em
   `app/build.gradle.kts`.
3. Rode em um emulador ou aparelho físico.

## Estrutura

```
app/src/main/java/com/example/
├── data/            # Room, Supabase, repositórios, sync offline
├── ui/
│   ├── theme/       # Design system: Color, Type, Tokens, Shadows, RodapeIcons
│   ├── components/  # Cards, Buttons, Cover, Avatar, Chips…
│   ├── screens/     # Telas (auth, tabs, discussão, gestão de clube…)
│   └── viewmodel/   # MainViewModel
└── util/            # DateFormat, CoverFiles, voting…
claude-design/       # Protótipo original (fonte de verdade visual)
docs/                # Auditoria, release, privacidade
```

## Documentação

- `docs/AUDITORIA-2026-07.md` — estado atual, achados e plano por fases
- `docs/release/` — checklist Play Store, setup Supabase, changelog
- `docs/privacy/privacy-policy.md` — política de privacidade

## Testes

```bash
./gradlew testDebugUnitTest
```

Testes de screenshot (Roborazzi) e Robolectric estão marcados `@Ignore` porque
exigem o runtime `android-all`, que precisa de rede na primeira execução. Rode
`./gradlew recordRoborazziDebug` num ambiente com rede para reativá-los.
