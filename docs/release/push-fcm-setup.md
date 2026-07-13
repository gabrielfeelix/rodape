# Ativar push notifications (F1) — runbook

O **backend do push já está no repo** (não requer app rodando):
- `supabase/migrations/0007_push_device_tokens.sql` — tabela `device_tokens` + trigger
  `trg_dispatch_push` que, a cada `notifications` inserida, chama a Edge Function.
- `supabase/functions/send-push/index.ts` — resolve os tokens e dispara no **FCM HTTP v1**.

O que **falta pra ligar** depende de credenciais que são suas (projeto Firebase +
segredo FCM). Este documento é o passo-a-passo. Enquanto não for feito, o trigger é um
**no-op seguro** — nada quebra, só não sai push. O gatilho de conteúdo já existe (todo
evento do clube cai em `notifications`, incluindo o cron de lembrete 24h).

> Decisão de produto: push é **essencial** num clube **presencial** (é o que faz a
> pessoa não perder o encontro). Por isso ficou como Onda 3 à parte — só precisa das
> suas credenciais.

---

## 1. Firebase

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/).
2. Adicione um app **Android** com o package **`app.rodape`** (e, se quiser cobrir o
   debug, **`app.rodape.debug`**).
3. Baixe o **`google-services.json`** e coloque em `app/google-services.json`
   (já está no `.gitignore`? senão, adicione — **não commitar**).
4. Em *Project settings → Service accounts*, gere uma **chave privada** (JSON). Dela
   você usa `project_id`, `client_email` e `private_key`.

## 2. App Android (cliente)

**`gradle/libs.versions.toml`** — adicione:
```toml
[versions]
googleServices = "4.4.2"
[libraries]
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }
[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

**`build.gradle.kts` (raiz)** — registre o plugin:
```kotlin
plugins {
  // …
  alias(libs.plugins.google.services) apply false
}
```

**`app/build.gradle.kts`** — aplique o plugin e a dependência:
```kotlin
plugins {
  // …
  alias(libs.plugins.google.services)
}
dependencies {
  // …
  implementation(libs.firebase.messaging)
}
```

**`AndroidManifest.xml`** — permissão + serviço:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- dentro de <application> -->
<service
    android:name=".push.RodapeMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

**`app/src/main/java/com/example/push/RodapeMessagingService.kt`** (novo):
```kotlin
package com.example.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.remote.Supabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RodapeMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Registra o token pro usuário logado (RLS garante que é o dono).
        val uid = Supabase.client.auth.currentUserOrNull()?.id ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                Supabase.client.postgrest["device_tokens"].upsert(
                    mapOf("token" to token, "user_id" to uid, "platform" to "android")
                )
            }
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val n = msg.notification ?: return
        val ch = "rodape_default"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(
            NotificationChannel(ch, "Clube", NotificationManager.IMPORTANCE_HIGH)
        )
        val notif = NotificationCompat.Builder(this, ch)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setAutoCancel(true)
            .build()
        mgr.notify(System.currentTimeMillis().toInt(), notif)
    }
}
```

**Registrar o token no login** (ex.: em `MainViewModel` após autenticar, ou num
`LaunchedEffect` no `main_tabs`):
```kotlin
com.google.firebase.messaging.FirebaseMessaging.getInstance().token
    .addOnSuccessListener { token -> /* upsert em device_tokens (igual onNewToken) */ }
```

**Pedir permissão (Android 13+)** — no momento certo (ex.: depois de agendar o 1º
encontro, não no onboarding): `POST_NOTIFICATIONS` via `rememberLauncherForActivityResult`.

## 3. Supabase

1. **Aplique a migration**: `supabase db push` (ou cole `0007_...sql` no SQL Editor).
2. **Deploy da função**: `supabase functions deploy send-push`.
3. **Segredos da função**:
   ```
   supabase secrets set FCM_PROJECT_ID="<project_id>" \
     FCM_CLIENT_EMAIL="<client_email>" \
     FCM_PRIVATE_KEY="<private_key com \n>"
   ```
4. **Service role no Vault** (migration `0009` — em Supabase HOSPEDADO as GUCs
   `alter database ... set app.x` dão `permission denied`; por isso a URL é inline na
   função e a service_role vem do Vault). Uma vez, no SQL Editor:
   ```sql
   select vault.create_secret('<SERVICE_ROLE_KEY>', 'push_service_role_key');
   ```
   Sem esse secret, o trigger é no-op seguro. Para trocar depois:
   `select vault.update_secret((select id from vault.secrets where name='push_service_role_key'), '<NOVA_KEY>');`

## 4. Testar

1. Logue no app num aparelho real → confirme uma linha em `device_tokens`.
2. Dispare uma notificação (ex.: encerrar votação, ou insira em `notifications` à mão).
3. O aparelho deve **apitar** com o app fechado.

## Checklist

- [x] `google-services.json` em `app/` (não commitado) — projeto `aplicativos-497303`
- [ ] **`app.rodape.debug` registrado no Firebase** + `google-services.json` re-baixado
      (o build DEBUG usa `applicationIdSuffix=".debug"` → sem esse client o plugin
      google-services falha com "No matching client found")
- [x] plugin google-services + dep firebase-messaging (via BoM)
- [x] `RodapeMessagingService` + serviço no Manifest + permissão POST_NOTIFICATIONS
- [x] registro de token no login (`MainViewModel.syncPushToken` / `PushTokens`) + pedido de permissão
- [x] migration 0007 aplicada (prod)
- [x] função `send-push` deployada (ACTIVE) + segredos FCM setados
- [x] migration 0009 (dispatch via Vault, substitui as GUCs que não funcionam no hospedado)
- [ ] **`vault.create_secret('<SERVICE_ROLE>', 'push_service_role_key')`** (você roda)
- [ ] teste ponta-a-ponta ok
