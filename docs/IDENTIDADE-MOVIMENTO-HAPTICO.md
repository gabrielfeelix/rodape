# Identidade de movimento & háptico — Rodapé

> Mapa curto (Fase 4 · item 5). Fonte da verdade para *quando* o app vibra e
> *como* o app se move. Objetivo: háptico é raro e significativo — se tudo vibra,
> nada vibra. Movimento serve à leitura, nunca à distração.

## Háptico — quando vibrar

O háptico marca **confirmações de intenção do usuário** que mudam estado
compartilhado ou celebram um marco. Nunca em navegação, scroll ou toque comum.

| Evento | Tipo | Onde | Status |
|---|---|---|---|
| RSVP salvo (Vou / Talvez / Não vou) | `LongPress` | `NextTabScreen` (pílula de RSVP) | ✅ implementado |
| Marco de leitura cruzado (25/50/75/100%) | `LongPress` | `MainTabsScreen` (Marcar progresso) | ✅ implementado |
| Voto registrado na votação | `LongPress` | `NextTabScreen` (votar em livro) | 💡 proposta |
| Encontro concluído (admin) | `LongPress` | `ManageClub` / `MeetingDetail` | 💡 proposta |
| Erro destrutivo confirmado (excluir conta, arquivar clube) | `Reject`/`LongPress` | diálogos RodapeDialog | 💡 proposta |

**Regra:** só `HapticFeedbackType.LongPress` por enquanto (a paleta do Compose
é curta; `LongPress` é o "confirmado" universal). Não usar háptico em: troca de
aba, pull-to-refresh, abertura de sheet, digitação, swipe de notificação
(o tap já é a confirmação).

**Marco de leitura — detalhe:** vibra só quando a marcação **cruza** o marco
(`oldPct < m && newPct >= m`), não em toda marcação de capítulo. A cópia do
snackbar acompanha ("Metade do livro! 📖", "Reta final — 75%! 🏁",
"Livro terminado! 🎉"). Confete/partículas ficam como enriquecimento futuro
(exige overlay + validação visual em device) — o háptico + copy já entregam a
recompensa hoje.

## Movimento — vocabulário

Tudo passa por `RodapeMotion` (`rodapeTween` / `rodapeSpring`), que degrada para
`snap()` quando `ANIMATOR_DURATION_SCALE == 0` (reduced-motion de graça). Não
usar `tween`/`spring` do Compose direto em UI.

| Gesto | Especifica | Sensação |
|---|---|---|
| Entrada de tela / stagger | `rodapeTween` emphasizedDecelerate | conteúdo "pousa" |
| Pílula de navbar, pop de ícone | `rodapeSpring` (guard de 1ª composição) | vivo, mas contido |
| Barra de progresso | emphasizedDecelerate | preenche junto com o par |
| Escala animada | sempre via `graphicsLayer` | nunca relayout |
| Transição de nav (avançar/voltar) | slide direcional; `None` sob reduced-motion | direção = hierarquia |

**Regra:** `transitionSpec` não é composable (specs hoisteados); punch de mola
sempre com guard de 1ª composição pra não "pular" ao entrar na tela.

## Princípio

Movimento e háptico são **assinatura**, não enfeite. Um leitor com pouco
letramento digital não deve precisar entender a animação pra usar o app — ela
confirma o que já aconteceu. Quando em dúvida: menos.
