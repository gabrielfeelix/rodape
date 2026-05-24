# Decisões arquiteturais — refactors adiados pra v1.1+

Auditorias externas (em 2026-05-24) levantaram pontos legítimos sobre
arquitetura que **conscientemente** não estão na v1.0.0. Este documento
registra o porquê e quando reabrir.

## 1. `MainViewModel` monolítico (1.200+ linhas)

**Crítica:** viola SRP, dificulta testes unitários, qualquer mudança
arrisca todo o app.

**Por que não agora:**
- App funciona. Tela por tela, fluxo por fluxo, validado em smoke test.
- Cobertura de testes unitários atual do ViewModel é zero — "dificulta
  testes" é teórico, não tem teste pra quebrar.
- Refactor pra `AuthVM/ClubVM/BookVM/ChatVM` toca 100% das telas
  (cada `viewModel: MainViewModel` vira N injections). Risco de
  regressão alto, valor pré-launch baixo.

**Quando reabrir:** v1.1, quando aparecer a primeira feature nova que
naturalmente vive em outro domínio (ex: stories de leitura, gamificação).
Aí o split é orientado a feature, não big bang.

---

## 2. Acoplamento direto com Supabase SDK

**Crítica:** `RemoteRepository` usa `SupabaseClient`, `postgresChangeFlow`
direto. Sem interface = trocar de backend = reescrever tudo.

**Por que não agora:**
- Escolha de Supabase foi **deliberada**: Postgres + RLS + Realtime +
  Storage + Auth num pacote só, é o que faz o app existir com 1 dev.
- Probabilidade de trocar de backend em 12 meses: ~2%.
- Adicionar interfaces = 20+ interfaces, 20+ impls fake pra testes que
  ainda não escrevemos. YAGNI.

**Quando reabrir:** quando aparecer segundo backend real (provavelmente
nunca). Se for só pra "facilitar testes", escrever fakes específicos
quando o primeiro teste real for criado.

---

## 3. 19 telas planas em `ui/screens/`

**Crítica:** `OnboardingScreen.kt`, `AdminLogScreen.kt`, `ReadScreen.kt`
e `SignUpScreen.kt` na mesma pasta dificulta navegação por feature.

**Por que não agora:**
- Mesmo argumento do #1: refactor toca dezenas de imports sem ganho de
  feature.
- IDE faz Cmd+P / Quick Open — ninguém navega por árvore.

**Quando reabrir:** quando o app tiver 30+ telas. Aí a dor passa a ser
real. Reorganizar pra `features/auth/`, `features/clube/`,
`features/leitura/`, `features/admin/`.

---

## 4. `Tokens.kt` vs M3 `ColorScheme`

**Crítica:** uso de `RodapeTokens.Terracota` direto em vez de
`colorScheme.primary` prejudica dark mode/alto contraste futuro.

**Por que não agora:**
- Spec original (2026-05-21-rodape-redesign-design.md) decidiu
  **só tema claro**, fidelidade ao protótipo React. Dark mode/alto
  contraste estão **explicitamente fora de escopo** da v1.0.
- Quando dark mode entrar, vai exigir redesign de todas as cores —
  não é só trocar tokens.

**Quando reabrir:** quando dark mode entrar como feature (provavelmente
v1.2+). Aí migra tokens pra um `ColorScheme` próprio que muda com
`isSystemInDarkTheme()`.

---

## 5. Cache Room por usuário (`rodape-cache-{userId}.db`)

**Crítica:** banco global é compartilhado entre contas; se logout
falhar parcialmente, próximo usuário no mesmo device pode ver lixo.

**Por que escolhi defesa em profundidade em vez de banco por usuário:**
- Banco por usuário cria N arquivos, complica WorkManager (qual user
  drenar?), complica clearLocalCache em login fresco em device novo.
- O problema fundamental não é "banco compartilhado" — é "logout
  pode ser interrompido". A solução cobre os dois cenários:
  1. Logout limpa cache (já existia)
  2. **Login compara userId atual com último persistido; se diferente,
     limpa cache ANTES de qualquer fluxo ler dele** (novo)

Implementado em [MainViewModel.kt:280-296](app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt#L280-L296).
