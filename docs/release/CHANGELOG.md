# Changelog — Rodapé

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
