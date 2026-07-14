# Política de Privacidade — Rodapé

**Última atualização:** 14 de julho de 2026

O Rodapé (`app.rodape`) é um aplicativo Android para clubes de leitura
privados. Esta política descreve quais dados coletamos, como usamos e quais são
seus direitos. Ao usar o app você também concorda com os
[Termos de Uso](../legal/termos-de-uso.md).

> **Versão curta:** coletamos só o necessário pra você participar do clube
> (nome, email, avatar opcional e o que você posta dentro do clube). Não
> vendemos dados, não rastreamos você fora do app, não enviamos para
> publicidade.

---

## 1. Quem somos

- **Nome do app:** Rodapé
- **Desenvolvedor:** Gabriel Felix
- **Contato:** [feedback@rodape.app](mailto:feedback@rodape.app)

## 2. Dados que coletamos

### 2.1 Conta

Quando você cria uma conta (email/senha ou Google Sign-In), coletamos:

- **Email** — identifica sua conta, recebe redefinição de senha e notificações
  por email.
- **Nome de exibição** — escolhido por você, visível aos outros membros do(s)
  seu(s) clube(s).
- **Avatar** — uma das ilustrações pré-fabricadas do app. Você pode trocar
  a qualquer momento. **Não usamos foto sua.**

### 2.2 Conteúdo de clube

Dentro de um clube de leitura, você pode criar conteúdo que é visível para os
outros membros do mesmo clube:

- Livros sugeridos, votos em livros, capas de livros
- Comentários por capítulo, reações em comentários
- Frases salvas, reuniões, RSVPs
- Mensagens da sua agenda de leitura

Esses dados são compartilhados **apenas com membros do clube em que foram
postados** (garantido por Row Level Security no banco).

### 2.5 Dados de moderação

Para manter os clubes seguros, também tratamos:

- **Denúncias:** ao denunciar um conteúdo, guardamos quem denunciou, o conteúdo
  denunciado, o motivo e um detalhe opcional. Visível apenas aos administradores
  do clube e a nós, para análise.
- **Bloqueios:** guardamos a relação de quem você bloqueou, para esconder o
  conteúdo entre vocês. Essa lista é privada — só você a vê.
- **Remoções:** quando um conteúdo é removido por moderação, registramos quem
  removeu e o motivo (log de moderação do clube).

### 2.3 Dados técnicos do app

- **Cache local:** o app guarda no seu aparelho uma cópia dos dados do clube
  pra funcionar offline e abrir rápido. Esse cache é apagado quando você
  desinstala o app ou faz logout.
- **Crash logs locais:** se o app crashar, salvamos o stack trace num arquivo
  local (em `filesDir/crashes/`). **Esse arquivo nunca é enviado pra ninguém
  automaticamente.** Você decide se quer compartilhar conosco em caso de bug.

### 2.4 Dados que NÃO coletamos

- Localização (GPS, IP, etc.)
- Lista de contatos, fotos, microfone
- Identificadores de publicidade
- Histórico de uso fora do app
- Analytics de terceiros (não usamos Google Analytics, Facebook SDK, Mixpanel,
  etc.)

## 3. Como usamos seus dados

- **Operar o serviço:** autenticar você, mostrar o conteúdo do clube,
  sincronizar entre seus aparelhos.
- **Comunicação transacional:** confirmação de email, redefinição de senha,
  notificações de clube (futuramente — opt-in).
- **Suporte:** responder seu email se você nos contatar.

**Não usamos seus dados para:** publicidade, marketing externo, treinamento
de modelos de IA, venda a terceiros, ou enriquecimento de perfis.

## 4. Onde os dados ficam armazenados

- **Backend:** [Supabase](https://supabase.com) (Postgres gerenciado), hospedado
  em data center da AWS. O Supabase é nosso processador de dados.
- **Autenticação:** Supabase Auth + Google OAuth (quando você escolhe Google
  Sign-In).
- **Imagens (capas, avatares):** Supabase Storage (buckets privados, acessíveis
  apenas via URLs assinadas com expiração).

## 5. Compartilhamento com terceiros

Compartilhamos dados apenas com nossos processadores estritamente necessários
pra operar o app:

| Terceiro | Para quê | Política de privacidade |
|---|---|---|
| Supabase, Inc. | Banco, autenticação, storage | https://supabase.com/privacy |
| Google LLC | Login com Google (opcional) | https://policies.google.com/privacy |

**Não vendemos seus dados** e **não enviamos para parceiros de publicidade**.

## 6. Retenção e exclusão

- Seu conteúdo fica armazenado enquanto sua conta estiver ativa.
- Você pode **excluir sua conta e todos os seus dados** direto no app
  (Perfil › Excluir conta) — a exclusão é imediata. Alternativamente, envie um
  email para [feedback@rodape.app](mailto:feedback@rodape.app) com o assunto
  "Excluir minha conta" e processamos em até 30 dias.
- Conteúdo que você postou em clubes pode permanecer visível pros outros
  membros (ex: comentários em capítulos), mas será anonimizado.
- **Denúncias e logs de moderação** são retidos enquanto necessário para a
  segurança do clube e para cumprir obrigações legais, mesmo após o conteúdo
  original ser removido.

## 7. Seus direitos (LGPD/GDPR)

Você tem direito a:

- **Acessar** seus dados pessoais
- **Corrigir** dados incorretos
- **Excluir** sua conta e dados pessoais
- **Exportar** seus dados em formato legível
- **Revogar consentimento** a qualquer momento

Para exercer qualquer direito, envie email pra
[feedback@rodape.app](mailto:feedback@rodape.app).

## 8. Segurança

- **HTTPS em todas as requisições** com o backend.
- **Row Level Security (RLS)** garante que você só vê dados dos clubes em que
  é membro — validado no servidor, não dá pra burlar pelo app.
- **Senhas** seguem o padrão NIST: mínimo 8 caracteres com maiúscula,
  minúscula, número e símbolo. Armazenadas com hash bcrypt pelo Supabase Auth.
- **Tokens de sessão** ficam no Android Credential Storage (criptografado pelo
  sistema). Não fazemos backup deles pro Google Drive.
- **Backup automático desligado:** o cache local do app não vai pro auto-backup
  do Android pra evitar vazamento de dados de clube em backups pessoais.

## 9. Crianças

O Rodapé não é direcionado a menores de 13 anos. Não coletamos
deliberadamente dados de crianças. Se você é responsável por um menor que
criou conta no app, entre em contato pra remoção.

## 10. Mudanças nesta política

Vamos avisar dentro do app antes de qualquer mudança material. A versão
sempre fica neste link: https://github.com/gabrielfeelix/rodape/blob/master/docs/privacy/privacy-policy.md

## 11. Contato

Dúvidas, sugestões, exclusão de conta:
[feedback@rodape.app](mailto:feedback@rodape.app)
