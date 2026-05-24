# Changelog — Tramabook

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
