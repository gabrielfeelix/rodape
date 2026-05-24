# Checklist Play Store — Rodapé v1.0.0

> Tudo que **eu (Claude)** automatizei já está marcado ✅. O que sobrou exige
> ação humana (conta Google Play, ativos visuais, painéis externos). Cada item
> abaixo explica o que fazer, por quê, e onde clicar.

---

## ✅ Já pronto no repo

- [x] `applicationId = app.rodape`
- [x] `versionCode = 1`, `versionName = "1.0.0"`
- [x] App label "Rodapé" em `strings.xml`
- [x] AAB assinado em `app/build/outputs/bundle/release/app-release.aab` (~21 MB)
- [x] APK release pra smoke test em `app/build/outputs/apk/release/app-release.apk` (~15 MB)
- [x] R8 ativo: minify + shrinkResources + crunchPngs
- [x] ProGuard rules pra Supabase/Ktor/Moshi/Room/WorkManager/Credential Manager/Coil
- [x] `allowBackup=false` + backup rules excluem dados sensíveis
- [x] Crash logger local (sem Crashlytics; nada sai do device sem permissão)
- [x] Privacy Policy em `docs/privacy/privacy-policy.md`
- [x] Changelog em `docs/release/CHANGELOG.md`
- [x] Upload keystore gerado em `my-upload-key.jks` (gitignored)
- [x] Senhas em `.env` (gitignored) + `ignoreList` impede vazamento via BuildConfig

---

## 🔴 Crítico antes do upload

### 1. Hospedar a Privacy Policy num URL público

A Play Store **exige um link público pra política**. Não aceita arquivo
anexado.

**Opções:**

- **GitHub Pages (mais rápido):** habilitar Pages no repo → URL fica
  `https://gabrielfeelix.github.io/rodape/privacy/privacy-policy.html`
  (precisa converter `.md` pra `.html` ou usar Jekyll, que renderiza
  Markdown automaticamente).
- **Permalink raw do GitHub:** `https://raw.githubusercontent.com/gabrielfeelix/rodape/master/docs/privacy/privacy-policy.md` — funciona, mas é cru e a Google às vezes recusa.
- **Subdomínio próprio:** `https://rodape.app/privacy` se você comprar o
  domínio. Mais profissional.

**Recomendado:** GitHub Pages. Tempo: 5 min.

### 2. Confirmar redirect URLs no painel do Supabase

Sem isso o magic link de confirmação de email não consegue voltar pro app.

**Onde:** Supabase Dashboard → Authentication → URL Configuration →
**Redirect URLs**

**Deve conter:**
- `app.rodape://login-callback`
- `app.rodape.debug://login-callback` (pra builds debug)

### 3. Cadastrar SHA-1 do upload keystore no Google Cloud Console

Pro Google Sign-In funcionar em release, o **fingerprint SHA-1 do upload
keystore** precisa estar registrado no OAuth Client.

**Fingerprints do `my-upload-key.jks` (extraídos automaticamente):**

```
SHA1:   8D:E8:B5:3C:8A:5B:D8:B1:9B:43:B5:2F:C8:57:EA:61:9F:1E:15:A3
SHA256: DF:FE:36:E8:17:4B:C4:46:43:A1:1A:3F:6B:AC:66:9F:42:EE:32:76:A0:6A:36:44:04:10:59:B1:83:D6:29:CD
```

Pra extrair de novo (se trocar de keystore):

```bash
keytool -list -v -keystore my-upload-key.jks -alias upload | grep -E "SHA1:|SHA256:"
```

**Onde colar:** Google Cloud Console → APIs & Services → Credentials →
Android Client ID de produção → adicionar SHA-1 fingerprint + applicationId
`app.rodape`.

Se ainda não existe Android Client de produção, criar um novo:
- Tipo: Android
- Package name: `app.rodape`
- SHA-1: (do keystore acima)

E adicionar o novo ID ao `.env` como `GOOGLE_ANDROID_CLIENT_ID_PROD=...`

### 4. SHA-1 do certificado de assinatura **final** da Play Store

Quando você sobe o AAB pela 1ª vez, o Google Play **gera** uma chave de
assinatura própria (Play App Signing). O SHA-1 dessa chave é DIFERENTE da
chave de upload. Você precisa cadastrar **AMBAS** no Google Cloud Console
(uma pra o app funcionar antes da Play distribuir, outra pra depois).

**Onde encontrar o SHA-1 da Play:** Play Console → seu app → Test and
release → Setup → App integrity → "App signing key certificate" → copiar
SHA-1. Cadastrar no Google Cloud Console igual ao passo 3.

---

## 🟡 Conta Google Play + listagem da loja

### 5. Criar conta de desenvolvedor Google Play

- Custo: **US$ 25** (taxa única).
- https://play.google.com/console
- Pode levar até 48h pra aprovar identidade.

### 6. Criar o app no console

- App name: **Rodapé**
- Default language: **Português (Brasil) — pt-BR**
- App or game: **App**
- Free or paid: **Free**
- Categoria sugerida: **Livros e referência** ou **Social**

### 7. Listagem da loja (Store listing)

Conteúdo já pronto pra colar:

**Título curto (30 chars):**
> Rodapé — clube de leitura

**Descrição curta (80 chars):**
> Leiam juntos no seu clube. Sugira livros, vote, comente capítulos e marquem encontros.

**Descrição completa (4000 chars):**
> 📚 **Rodapé** é o app feito pra clubes de leitura privados.
>
> Reúna os amigos da leitura num clube só seu. Sugiram livros, votem no próximo, leiam no ritmo de vocês e discutam capítulo por capítulo sem medo de spoiler — quem ainda não chegou no capítulo X simplesmente não vê o que rolou lá.
>
> **O que dá pra fazer:**
>
> 📖 **Estante coletiva** — o que o clube já leu, o que está lendo agora e a fila do que vem aí.
>
> 🗳️ **Votação democrática** — cada um sugere e vota no próximo livro. Quem comanda o clube fecha a rodada quando bate a hora.
>
> 💬 **Discussão por capítulo** — comente livremente sem estragar a leitura pra quem está atrasado.
>
> ✨ **Frases salvas** — guarde aquele trecho marcante e exporte pro WhatsApp ou notas.
>
> 📅 **Reuniões com RSVP** — marque o encontro presencial ou online e veja quem confirma.
>
> 🌱 **Avatar ilustrado** — sem foto, sem rede social. Você escolhe um personagem (ou deixamos a gente sugerir um baseado no seu nome).
>
> **Privacidade em primeiro lugar:**
> - Conteúdo do clube é visível só pros membros do clube
> - Sem rastreamento de terceiros, sem analytics invasivo
> - Sem publicidade
> - Backup automático do Android desligado pra não vazar conteúdo do clube em backups pessoais
>
> Política de privacidade: https://github.com/gabrielfeelix/rodape/blob/master/docs/privacy/privacy-policy.md

### 8. Ativos gráficos necessários

Tudo o que a Play Store **exige**:

| Asset | Tamanho | Quantidade | Status |
|---|---|---|---|
| Ícone do app | 512×512 PNG | 1 | ⚠️ extrair do `ic_launcher` adaptativo |
| Banner / feature graphic | 1024×500 PNG | 1 | ❌ criar (Figma/Canva) |
| Screenshots phone | mín 1080×1920 | 2 a 8 | ❌ tirar do app rodando |
| Screenshots tablet 7" (opcional) | 1024×600 | 0 a 8 | ⏭️ pular se app é só phone |
| Vídeo (opcional) | YouTube URL | 0 a 1 | ⏭️ pular pra v1 |

**Screenshots sugeridos pra capturar (rodar app, Vol- + Power):**
1. Tela inicial do clube (Home tab com livro atual)
2. Lista de capítulos / discussão
3. Comentários em um capítulo
4. Estante coletiva
5. Votação no próximo livro
6. Tela de criar/escolher clube

### 9. Classificação de conteúdo (Content rating)

Preencher o questionário do IARC:
- **Faixa esperada:** 12+ (conteúdo gerado por usuário, sem moderação automática de palavrões)
- Marcar: "User-generated content" SIM, "Comunicação com outros usuários" SIM (dentro do clube)

### 10. Data Safety form

Declarar exatamente o que coletamos. Modelo:

- **Dados coletados:**
  - Nome e endereço de email (obrigatório, função do app, criptografado em trânsito, usuário pode pedir exclusão)
  - Conteúdo gerado pelo usuário: textos, fotos (capas de livros), reações (obrigatório, função do app)
- **Dados compartilhados:** Nenhum (Supabase é processador, não terceiro)
- **Práticas de segurança:**
  - ✅ Dados criptografados em trânsito (HTTPS)
  - ✅ Usuário pode solicitar exclusão
  - ❌ Política de retenção: enviado por email pra `feedback@rodape.app`
- **Atende guidelines de Family policy:** não direcionado a crianças <13

---

## 🟢 Pós-upload (track interno → produção)

### 11. Subir o AAB

- Play Console → Test and release → **Internal testing** primeiro
- Upload `app/build/outputs/bundle/release/app-release.aab`
- Adicionar testers por email
- Validar end-to-end no aparelho de teste antes de promover pra Production

### 12. Promover pra Produção

- Test and release → **Production** → New release
- Promote from Internal testing
- Notas da versão (copiar de `docs/release/CHANGELOG.md`)
- Submeter pra review

### 13. Backup do keystore (CRÍTICO)

⚠️ **Perder `my-upload-key.jks` = não consegue mais atualizar o app na Play
Store, ponto final.** Não tem recovery.

Fazer:
1. Copiar `my-upload-key.jks` pra um pendrive criptografado
2. Anotar as senhas (KEYSTORE_PATH / STORE_PASSWORD / KEY_PASSWORD do `.env`)
   num gerenciador de senhas (1Password, Bitwarden, etc.)
3. Idealmente, fazer SEGUNDA cópia num lugar separado (cofre familiar, etc.)

---

## Limitações conhecidas (pra incluir na descrição se quiser ser transparente)

- Sem push notifications (FCM) — planejado pra v1.1
- Sem dark mode (decisão de design)
- HIBP (proteção contra senhas vazadas no Supabase) requer plano Pro
- Captcha desabilitado (sem hCaptcha secret)
