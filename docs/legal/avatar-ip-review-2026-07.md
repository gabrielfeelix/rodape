# Parecer de propriedade intelectual — avatares (2026-07-11)

> Não é aconselhamento jurídico formal. É uma triagem de risco de IP feita a
> partir da inspeção visual de cada asset. Para publicação comercial, confirme
> com um advogado de PI.

## Contexto

O app usava 12 avatares ilustrados (renders 3D estilo "Pixar") de personagens.
Personagens de contos de fada / clássicos são de **domínio público**, MAS a
**representação específica de uma empresa** (ex: o design da Disney) continua
protegida por direitos autorais e/ou trade dress. O risco não é o personagem —
é a *cópia de um design protegido*.

## Decisão por avatar

| Avatar | Personagem | Veredito | Motivo |
|---|---|---|---|
| `avatar_alice` | Alice | 🔴 **REMOVIDO** | Design **da Disney** (vestido azul, avental branco, laço preto, coelho branco). Cópia clara de obra protegida. |
| `avatar_petalas` | Mulan | 🔴 **REMOVIDO** | **Mulan da Disney** (armadura, espada vertical no rosto, pétalas de cerejeira — composição icônica do pôster). |
| `avatar_pequeno_principe` | O Pequeno Príncipe | 🔴 **REMOVIDO** | Espólio Saint-Exupéry (`Le Petit Prince™`) é litigioso; direitos estendidos na França até ~2032. |
| `avatar_fantasma` | Fantasma da Ópera | 🟡 **REMOVIDO (precaução)** | Novela é domínio público, mas "fantasma + rosa + máscara" evoca o musical de A. L. Webber (marca da Really Useful Group). Máscara inteira reduzia o risco, mas removi por segurança. |
| `avatar_emilia` | Emília (Lobato) | 🟢 Mantido | Monteiro Lobato em domínio público no Brasil desde 2019 (morte 1948 + 70). Representação de boneca de pano genérica. |
| `avatar_leitor` | (Frankenstein/genérico) | 🟢 Mantido | Criatura de Mary Shelley é domínio público; sem os elementos protegidos da Universal (cabeça chata, parafusos no pescoço). |
| `avatar_detetive` | Sherlock Holmes | 🟢 Mantido | Personagem 100% em domínio público (EUA desde 2023). Iconografia (deerstalker, cachimbo) também é PD. |
| `avatar_don_quixote` | Dom Quixote | 🟢 Mantido | Cervantes, 1605. Domínio público. Representação original. |
| `avatar_joana_darc` | Joana d'Arc | 🟢 Mantido | Figura histórica. Domínio público. |
| `avatar_indigena` | (folclore/genérico) | 🟢 Mantido | Não é personagem protegido. Evoca folclore brasileiro (domínio público). |
| `avatar_leitora` | (genérico) | 🟢 Mantido | Personagem original genérico. |
| `avatar_mago` | (genérico) | 🟢 Mantido | Arquétipo de mago genérico (não é Gandalf/Merlin específico). |

## O que foi feito

- Removidos do app os 4 de risco (`alice`, `petalas`, `pequeno_principe`,
  `fantasma`): PNGs deletados + retirados de `Avatar.kt`, `AvatarPicker.kt`,
  `OnboardingScreen.kt` e `EditProfileView`.
- Lista de avatares **centralizada** em `Avatar.kt` (`presetAvatarKeys` /
  `presetDisplayName`) — antes havia 4 listas duplicadas, fonte de bugs.
- Fallback seguro: usuário que já tinha escolhido um avatar removido passa a ver
  as **iniciais** (o app não tenta mais carregar o preset removido).
- Removidos da raiz do repo: `pequenoprincipe.png` (IP) e `logotramabook.png`
  (marca Tramabook defunta).

## Observação sobre o estilo dos assets

Todos os avatares mantidos são renders 3D estilo "Pixar/Disney". **O estilo em
si não infringe** (estilo não é protegido por copyright) — o que infringia era
reproduzir o *design de um personagem específico*. Mesmo assim, se quiser
eliminar qualquer ambiguidade de marca, considere no futuro trocar por
ilustrações com estética própria (flat/editorial combinando com o design
Rodapé), o que também ficaria mais coeso visualmente.

## Recomendação de baixo risco remanescente (opcional)

- `emilia` e `leitor` são defensáveis, mas se o app for distribuído fora do
  Brasil, reavalie `emilia` (adaptações modernas de Lobato têm proteção).
