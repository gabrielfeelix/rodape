# Plano de refactor de arquitetura — rodapé

**Data:** 2026-07-14 · **Contexto:** app é offline-first Compose sólido, mas com
duas dívidas reais de manutenção (god-ViewModel e god-Repository) + alguns ganhos
de eficiência/testabilidade. Auditado contra a skill `app-android` (padrões
NowInAndroid do Google).

> **Veredito da auditoria:** app NÃO é ruim. Acerta o principal (offline-first real,
> tudo Flow, build com R8). As "reprovações" contra o NowInAndroid são em critérios
> de time grande / multi-módulo — over-engineering pro tamanho do app. Este plano
> cobre só o que importa **no nosso tamanho**, e ignora de propósito:
> multi-módulo, use-cases/domain layer, convention plugins.

## ⚠️ BLOQUEIO DE COORDENAÇÃO (2026-07-14)

Há **outro agente** executando `PLANO-BETA-COMPLIANCE-2026-07-14.md` (moderação,
Termos, Crashlytics, etc.) na **mesma working tree / branch master**. O W1
(moderação) reescreve AGORA: `RemoteRepository`, `MainViewModel`, `Entities`,
`AppDatabase`, `RodapeDao`, `DiscussionScreen`, `ManageClubScreen` + novo
`Moderation.kt`.

**Sobreposição com este plano é quase total no núcleo** (RemoteRepository +
MainViewModel + telas). Regra: **NÃO iniciar F1–F7 enquanto o W1 do compliance não
fechar.** Refatorar estrutura durante feature-work nos mesmos arquivos = merge hell.

**Gatilho pra começar:** compliance ter commitado ao menos o W1 (core rewrite).
Idealmente todo o Onda 1. Confirmar working tree limpa antes de iniciar.

---

## Decisões travadas

- **DI:** adotar **Hilt** no F4 (quando surgem ~8 VMs + repos scoped, a fiação
  manual passa a doer — é onde o ROI do Hilt vira positivo).
- **Ordem de início:** F1 + F2 primeiro (baratos, seguros), depois F3 → F4/F5.
- **Estratégia:** strangler-fig. Cada fase compila, commita, vai pro device,
  reverte fácil. App nunca quebrado no meio.

## Dependências

```
F1 lifecycle ─┐ (independentes)
F2 nav ───────┘
F3 repo interface + fatiar ──┐
                             ├─► F4 VMs scoped + F5 Route/Screen+UiState (juntos, por tela)
                             │        └─► F6 Previews
                             └────────────► F7 Testes
```

## Fases

### F1 — `collectAsStateWithLifecycle` · 🟢 baixo · ~1 sessão
- Add dep `androidx.lifecycle:lifecycle-runtime-compose` (checar se já vem transitivo).
- Trocar 121× `collectAsState()` → `collectAsStateWithLifecycle()`.
- **Cuidado:** `collectAsState(initial = X)` (Flow puro) vira
  `collectAsStateWithLifecycle(initialValue = X)` — o parâmetro **renomeia**.
  StateFlow (`.value`) usa sem argumento.
- Ganho: para de coletar em background → bateria/CPU.

### F2 — Nav type-safe · 🟢 baixo · ~1 sessão
- `@Serializable` route objects/data classes; `NavController.navigateToX()`;
  `composable<Route>()`. Trocar 19 rotas string + call sites.
- Contido em MainActivity + call sites.

### F3 — Repository: interface + fatiar · 🟡 médio · 2-3 sessões
1. Extrair `interface RodapeRepository` das assinaturas atuais; `RemoteRepository`
   implementa. Callers inalterados. Compila. Commita.
2. Fatiar por domínio (`ClubRepositoryImpl`, `BookRepositoryImpl`,
   `MeetingRepositoryImpl`, `DiscussionRepositoryImpl`, `VotingRepositoryImpl`…),
   um domínio por commit.
- Habilita test doubles (fakes) e VMs scoped.

### F4 — ViewModels scoped (strangler) · 🟡 médio · 4-6 sessões
- Setup Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, módulos).
- Extrair VM por tela, **ordem folha→hub** pra risco baixo:
  `Moderation → Frases → About → Notifications → Suggest → BookDetail → NextTab
  → MainTabs/Home (por último — é o hub)`.
- Cada VM: repo scoped injetado, expõe `sealed UiState`. Tira a tela do
  MainViewModel. MainViewModel encolhe até sumir.

### F5 — Route/Screen split + UiState selado · 🟡 médio · (dentro do F4)
- Junto do F4, por tela: `sealed interface XUiState { Loading/Success/Error }` +
  split `XRoute` (VM + nav) / `XScreen` (burro, recebe UiState + callbacks).

### F6 — Previews Compose · 🟢 baixo · 1-2 sessões
- Depois do F5. `@ThemePreviews` (light/dark) + `@DevicePreviews` (phone/tablet) +
  preview data por tela.

### F7 — Testes · 🟡 médio · 2-3 sessões
- Fakes das interfaces (sem lib de mock, per skill). Testar VM (state dado fake
  repo) + fluxos críticos: fila offline/sync, tally de voto, marco de leitura, auth.

## Progresso

- [ ] F1 — lifecycle
- [ ] F2 — nav type-safe
- [ ] F3 — repository interface + fatiar
- [ ] F4 — VMs scoped + Hilt
- [ ] F5 — Route/Screen + UiState
- [ ] F6 — previews
- [ ] F7 — testes
