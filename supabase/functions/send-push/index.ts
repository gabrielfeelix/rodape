// Edge Function: send-push
// -------------------------------------------------------------------------
// Disparada pelo trigger `trg_dispatch_push` (migration 0007) a cada linha nova
// em public.notifications. Resolve os device_tokens do destinatário e envia via
// FCM HTTP v1.
//
// Segredos necessários (supabase secrets set ...):
//   SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY  (injetados pelo runtime)
//   FCM_PROJECT_ID            — id do projeto Firebase
//   FCM_CLIENT_EMAIL          — service account client_email
//   FCM_PRIVATE_KEY           — service account private_key (com \n reais)
//
// Deploy: supabase functions deploy send-push
// Ver docs/release/push-fcm-setup.md pro passo-a-passo completo.
// -------------------------------------------------------------------------
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

interface NotificationRow {
  id: string;
  user_id: string;
  tipo: string;
  payload: Record<string, unknown> | null;
}

// Humaniza a notificação (espelha a lógica do NotificationsScreen no app).
function humanize(tipo: string, p: Record<string, unknown>): { title: string; body: string } {
  const actor = (p.actorName as string) || "Alguém";
  const club = (p.clubName as string) || "seu clube";
  const book = (p.bookTitle as string) || "o livro";
  const titulos = Array.isArray(p.titulos) ? (p.titulos as string[]).join(", ") : book;
  switch (tipo) {
    case "member_finished": return { title: club, body: `${actor} terminou "${book}" 🎉` };
    case "voting_open": return { title: club, body: "A votação do próximo livro está aberta 🗳️" };
    case "voting_closed": return { title: club, body: `O clube escolheu: ${titulos}.` };
    case "next_book_decided": return { title: club, body: `Próxima leitura: ${book} 📖` };
    case "meeting_reminder": return { title: club, body: "Seu encontro do clube é amanhã 📅" };
    case "comment_on_chapter": return { title: club, body: `${actor} comentou um capítulo que você já leu 💬` };
    default: return { title: "Rodapé", body: "Você tem uma novidade no clube." };
  }
}

// ---- Google OAuth (service account → access token pra FCM v1) ----
function pemToArrayBuffer(pem: string): ArrayBuffer {
  const b64 = pem.replace(/-----[^-]+-----/g, "").replace(/\s/g, "");
  const bin = atob(b64);
  const buf = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
  return buf.buffer;
}
function b64url(data: ArrayBuffer | string): string {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
async function getAccessToken(clientEmail: string, privateKeyPem: string): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claim = {
    iss: clientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };
  const unsigned = `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(claim))}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKeyPem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const jwt = `${unsigned}.${b64url(sig)}`;
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });
  const json = await res.json();
  if (!json.access_token) throw new Error("Falha no OAuth do FCM: " + JSON.stringify(json));
  return json.access_token as string;
}

Deno.serve(async (req) => {
  try {
    const { notification_id } = await req.json();
    if (!notification_id) return new Response("missing notification_id", { status: 400 });

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    const { data: notif } = await supabase
      .from("notifications")
      .select("id,user_id,tipo,payload")
      .eq("id", notification_id)
      .single<NotificationRow>();
    if (!notif) return new Response("notification not found", { status: 404 });

    const { data: tokens } = await supabase
      .from("device_tokens")
      .select("token")
      .eq("user_id", notif.user_id);
    if (!tokens || tokens.length === 0) return new Response("no tokens", { status: 200 });

    const { title, body } = humanize(notif.tipo, notif.payload ?? {});
    const projectId = Deno.env.get("FCM_PROJECT_ID")!;
    const accessToken = await getAccessToken(
      Deno.env.get("FCM_CLIENT_EMAIL")!,
      (Deno.env.get("FCM_PRIVATE_KEY") ?? "").replace(/\\n/g, "\n"),
    );

    const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
    for (const { token } of tokens) {
      const msg = {
        message: {
          token,
          notification: { title, body },
          data: { tipo: notif.tipo, notification_id: notif.id },
          android: { priority: "high", notification: { channel_id: "rodape_default" } },
        },
      };
      const r = await fetch(url, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify(msg),
      });
      // Token inválido/expirado (404/UNREGISTERED) → limpa pra não reenviar.
      if (r.status === 404 || r.status === 400) {
        await supabase.from("device_tokens").delete().eq("token", token);
      }
    }
    return new Response("ok", { status: 200 });
  } catch (e) {
    console.error("send-push error", e);
    return new Response("error", { status: 500 });
  }
});
