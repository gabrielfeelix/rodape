// screens-main.jsx — Home, Current Book, Chapter discussion, Next (3 tabs)

// ─────────────────────────────────────────────────────────────
// 5. Início (Home) — completely different from Livro atual
//    Meeting is the hero (big ticket card), book is a small strip.
//    "Onde a galera tá" matches the photo-avatar reference.
// ─────────────────────────────────────────────────────────────
function HomeScreen({ goTo, openClubSheet }) {
  const data = TB_DATA;
  const club = data.clubs[0];
  const book = data.currentBook;
  const meeting = data.meeting;
  const pct = Math.round(book.userChapter / book.chapters * 100);
  const daysUntil = 3;
  const activities = [
    { who: 'Marina', what: 'comentou no', target: 'Capítulo 8', when: '2h', kind: 'comment' },
    { who: 'Júlia',  what: 'terminou',     target: 'A Hora da Estrela', when: 'ontem', kind: 'done' },
    { who: 'Rafael', what: 'salvou uma frase em', target: 'Cap. 7', when: 'ontem', kind: 'quote' },
  ];

  return (
    <ScreenScroll padding="0 0 24px">
      {/* Greeting */}
      <div style={{ padding: '0 22px 4px' }}>
        <div style={{ fontFamily: TB.sans, fontSize: 13, color: TB.muted, fontWeight: 500 }}>
          Boa noite, {data.user.name}.
        </div>
        <h1 style={{
          fontFamily: TB.serif, fontSize: 28, fontWeight: 600, letterSpacing: -0.7,
          color: TB.ink, margin: '2px 0 0', lineHeight: 1.1,
        }}>
          A galera tá <span style={{ fontStyle: 'italic', color: TB.secondary }}>esperando.</span>
        </h1>
      </div>

      {/* ── Próximo encontro: TICKET ─────────────────────────── */}
      <div style={{ padding: '20px 18px 4px' }}>
        <MeetingTicket meeting={meeting} daysUntil={daysUntil} onTap={() => goTo('next-meeting')} />
      </div>

      {/* ── Reading strip (compact, no green) ────────────────── */}
      <div style={{ padding: '14px 18px 0' }}>
        <button onClick={() => goTo('book')} style={{
          width: '100%', background: TB.card, border: `1px solid ${TB.divider}`,
          borderRadius: 20, padding: 14, cursor: 'pointer', textAlign: 'left',
          display: 'flex', alignItems: 'center', gap: 14,
        }}>
          <Cover title={book.title} author={book.author} w={48} h={72} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{
              fontFamily: TB.sans, fontSize: 10.5, fontWeight: 700, color: TB.muted,
              textTransform: 'uppercase', letterSpacing: 1, marginBottom: 3,
            }}>Tua leitura · cap. {book.userChapter}/{book.chapters}</div>
            <div style={{
              fontFamily: TB.serif, fontSize: 16, fontWeight: 600, color: TB.ink,
              letterSpacing: -0.2, lineHeight: 1.15, marginBottom: 8,
              whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
            }}>{book.title}</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ flex: 1, height: 4, background: TB.dividerSoft, borderRadius: 999, overflow: 'hidden' }}>
                <div style={{ height: '100%', width: `${pct}%`, background: TB.primary, borderRadius: 999 }} />
              </div>
              <span style={{ fontFamily: TB.sans, fontSize: 11, fontWeight: 700, color: TB.primary, fontVariantNumeric: 'tabular-nums' }}>{pct}%</span>
            </div>
          </div>
          <I.chevR size={16} stroke={TB.muted} />
        </button>
      </div>

      {/* ── Onde a galera tá (matching reference) ────────────── */}
      <SectionHeader title="Onde a galera tá"
        action={<span style={{ fontFamily: TB.sans, fontSize: 12, color: TB.muted, fontWeight: 500 }}>{data.members.length} leitores</span>} />
      <div style={{
        display: 'flex', gap: 22, overflowX: 'auto',
        padding: '4px 22px 12px', scrollbarWidth: 'none',
      }}>
        {data.members.map(m => <ReaderChip key={m.name} member={m} />)}
      </div>

      {/* ── Atividade do clube ──────────────────────────────── */}
      <SectionHeader title="No clube hoje" />
      <div style={{ padding: '0 22px' }}>
        <Card padding="6px 6px" style={{ background: TB.card }}>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {activities.map((a, i) => (
              <div key={i} style={{
                display: 'flex', alignItems: 'center', gap: 12,
                padding: '12px 12px',
                borderBottom: i < activities.length - 1 ? `1px solid ${TB.dividerSoft}` : 'none',
              }}>
                <Avatar name={a.who} size={36} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontFamily: TB.sans, fontSize: 13.5, color: TB.ink, lineHeight: 1.35 }}>
                    <b style={{ fontWeight: 600 }}>{a.who}</b>{' '}
                    <span style={{ color: TB.tertiary }}>{a.what}</span>{' '}
                    <span style={{ color: TB.secondary, fontWeight: 600 }}>{a.target}</span>
                  </div>
                  <div style={{ fontFamily: TB.sans, fontSize: 11, color: TB.muted, marginTop: 2 }}>
                    {a.when}
                  </div>
                </div>
                <ActivityIcon kind={a.kind} />
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* ── Sobre o clube ───────────────────────────────────── */}
      <SectionHeader title="Sobre o clube" />
      <div style={{ padding: '0 22px' }}>
        <Card padding="18px 20px" style={{ background: TB.card }}>
          <p style={{
            fontFamily: TB.serif, fontSize: 15.5, lineHeight: 1.5, color: TB.inkSoft,
            margin: '0 0 14px', fontStyle: 'italic', textWrap: 'pretty',
          }}>
            "{club.description}"
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {club.rules.map((r, i) => (
              <div key={i} style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                <div style={{
                  width: 18, height: 18, borderRadius: '50%', background: TB.secondarySoft,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, marginTop: 1,
                }}>
                  <I.check size={11} stroke={TB.secondaryDark} sw={2.6} />
                </div>
                <span style={{ fontFamily: TB.sans, fontSize: 13, color: TB.tertiary, lineHeight: 1.45 }}>{r}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div style={{ height: 18 }} />
    </ScreenScroll>
  );
}

// ─────────────────────────────────────────────────────────────
// MeetingTicket — perforated ticket card (the home hero)
// ─────────────────────────────────────────────────────────────
function MeetingTicket({ meeting, daysUntil, onTap }) {
  return (
    <div onClick={onTap} style={{
      cursor: 'pointer', borderRadius: 24, overflow: 'hidden',
      background: TB.secondaryDeep, color: TB.cream,
      position: 'relative',
      boxShadow: '0 8px 28px rgba(41,56,32,0.20), 0 2px 6px rgba(0,0,0,0.08)',
    }}>
      {/* Top — overline + countdown */}
      <div style={{
        padding: '16px 20px 12px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        borderBottom: '1px dashed rgba(251,250,244,0.25)',
      }}>
        <div style={{
          fontFamily: TB.sans, fontSize: 10.5, fontWeight: 700, letterSpacing: 1.6,
          textTransform: 'uppercase', opacity: 0.7,
        }}>Próximo encontro</div>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 6,
          background: 'rgba(251,250,244,0.12)', padding: '3px 9px', borderRadius: 999,
          fontFamily: TB.sans, fontSize: 11, fontWeight: 600, letterSpacing: 0.2,
        }}>
          <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#E6BF6B' }} />
          em {daysUntil} dias
        </div>
      </div>

      {/* Body */}
      <div style={{ display: 'flex', alignItems: 'stretch' }}>
        {/* Date stamp column */}
        <div style={{
          width: 110, padding: '22px 16px 24px',
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          position: 'relative',
        }}>
          <div style={{ fontFamily: TB.sans, fontSize: 10, fontWeight: 700, letterSpacing: 2, opacity: 0.65 }}>
            {meeting.weekday.toUpperCase()}
          </div>
          <div style={{
            fontFamily: TB.serif, fontSize: 64, fontWeight: 500, color: TB.cream,
            letterSpacing: -2, lineHeight: 1, fontStyle: 'italic',
            margin: '4px 0 2px',
          }}>{meeting.day}</div>
          <div style={{ fontFamily: TB.serif, fontSize: 14, fontStyle: 'italic', opacity: 0.85 }}>
            outubro
          </div>
        </div>

        {/* Perforation */}
        <div style={{
          width: 1, background: 'transparent',
          backgroundImage: `linear-gradient(rgba(251,250,244,0.3) 50%, transparent 50%)`,
          backgroundSize: '1px 8px',
        }} />

        {/* Content column */}
        <div style={{ flex: 1, padding: '20px 18px 22px' }}>
          <div style={{
            fontFamily: TB.serif, fontSize: 17, fontWeight: 600, lineHeight: 1.15,
            letterSpacing: -0.3, marginBottom: 12, textWrap: 'pretty',
          }}>{meeting.title}</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontFamily: TB.sans, fontSize: 12, opacity: 0.85 }}>
              <I.clock size={13} stroke={TB.cream} /> {meeting.timeStart} — {meeting.timeEnd}
            </div>
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8, fontFamily: TB.sans, fontSize: 12, opacity: 0.85 }}>
              <I.pin size={13} stroke={TB.cream} style={{ marginTop: 1, flexShrink: 0 }} />
              <span style={{ lineHeight: 1.35 }}>{meeting.place}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom stub */}
      <div style={{
        background: 'rgba(0,0,0,0.18)', padding: '12px 20px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14,
      }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          {meeting.confirmed.slice(0, 3).map((n, i) => (
            <Avatar key={n} name={n} size={26} style={{ marginLeft: i ? -8 : 0, boxShadow: `0 0 0 2px ${TB.secondaryDeep}` }} />
          ))}
          {meeting.confirmed.length > 3 && (
            <span style={{
              marginLeft: 10, fontFamily: TB.sans, fontSize: 12, opacity: 0.8, fontWeight: 500,
            }}>+{meeting.confirmed.length - 3} vão</span>
          )}
        </div>
        <div style={{
          background: TB.cream, color: TB.secondaryDeep,
          padding: '7px 14px', borderRadius: 999,
          fontFamily: TB.sans, fontSize: 12, fontWeight: 700, letterSpacing: -0.1,
          display: 'flex', alignItems: 'center', gap: 5,
        }}>
          Eu vou
          <I.chevR size={12} stroke={TB.secondaryDeep} sw={2.4} />
        </div>
      </div>

      {/* notch left */}
      <div style={{
        position: 'absolute', left: -8, top: 70,
        width: 16, height: 16, borderRadius: '50%', background: TB.paper,
      }} />
      {/* notch right */}
      <div style={{
        position: 'absolute', right: -8, top: 70,
        width: 16, height: 16, borderRadius: '50%', background: TB.paper,
      }} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// ReaderChip — large avatar with status pill (matches reference)
// ─────────────────────────────────────────────────────────────
function ReaderChip({ member }) {
  const m = member;
  // realistic-feeling avatar: gradient bg + initial
  const palettes = [
    ['#FDE3CF', '#F0A878'], ['#E5EBDA', '#92A57F'],
    ['#FBE5DA', '#E89A77'], ['#D9D9CF', '#9E9C8A'],
    ['#E6D4B5', '#B8965B'], ['#E1D8E9', '#A589B5'],
  ];
  const hash = [...m.name].reduce((s, c) => s + c.charCodeAt(0), 0);
  const p = palettes[hash % palettes.length];

  const ringStyle = m.late
    ? { border: `1.5px dashed ${TB.muted}`, padding: 3, opacity: 0.85 }
    : m.done
    ? { border: `1.5px solid ${TB.secondary}`, padding: 3 }
    : m.ahead
    ? { border: `1.5px solid ${TB.ink}`, padding: 3 }
    : { border: `1.5px solid transparent`, padding: 3 };

  return (
    <div style={{ flexShrink: 0, width: 76, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      {/* Avatar with ring */}
      <div style={{
        width: 72, height: 72, borderRadius: '50%',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        position: 'relative',
        ...ringStyle, boxSizing: 'border-box',
      }}>
        <div style={{
          width: '100%', height: '100%', borderRadius: '50%',
          background: `linear-gradient(135deg, ${p[0]} 0%, ${p[1]} 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontFamily: TB.serif, fontSize: 26, fontWeight: 500,
          textShadow: '0 1px 2px rgba(0,0,0,0.15)',
        }}>
          {m.name[0]}
        </div>
        {/* status pill — overlaps the bottom of the avatar */}
        <div style={{
          position: 'absolute', bottom: -10, left: '50%', transform: 'translateX(-50%)',
          padding: '3px 10px', borderRadius: 999,
          fontFamily: TB.sans, fontSize: 10.5, fontWeight: 700,
          letterSpacing: 0.2, whiteSpace: 'nowrap',
          background: m.done ? TB.secondary
                   : m.late ? TB.dividerSoft
                   : m.ahead ? TB.ink
                   : TB.cream,
          color: m.done ? TB.cream
               : m.late ? TB.muted
               : m.ahead ? TB.cream
               : TB.inkSoft,
          boxShadow: '0 1px 4px rgba(0,0,0,0.10)',
          border: m.done || m.ahead ? 'none' : `1px solid ${TB.divider}`,
        }}>{m.status}</div>
      </div>
      {/* Name */}
      <div style={{
        fontFamily: TB.sans, fontSize: 12.5, fontWeight: 500, color: TB.inkSoft,
        marginTop: 16, textAlign: 'center',
        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 76,
      }}>{m.name}</div>
    </div>
  );
}

function ActivityIcon({ kind }) {
  const map = {
    comment: { bg: TB.secondarySoft, color: TB.secondaryDark, icon: I.book },
    done:    { bg: TB.secondary,     color: TB.cream,         icon: I.check },
    quote:   { bg: TB.primarySoft,   color: TB.primaryDark,   icon: I.starFill },
    vote:    { bg: TB.primarySoft,   color: TB.primaryDark,   icon: I.vote },
  };
  const m = map[kind] || map.comment;
  const Cmp = m.icon;
  return (
    <div style={{
      width: 30, height: 30, borderRadius: 10, background: m.bg,
      display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
    }}>
      <Cmp size={14} stroke={m.color} sw={2} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 6. Livro atual (Current book)
// ─────────────────────────────────────────────────────────────
function CurrentBookScreen({ goTo, openProgressSheet }) {
  const book = TB_DATA.currentBook;
  const pct = Math.round(book.userChapter / book.chapters * 100);

  return (
    <ScreenScroll padding="0">
      {/* Hero — olive section with floating cover */}
      <div style={{
        background: TB.secondaryDeep,
        padding: '20px 22px 80px',
        position: 'relative',
        borderRadius: '0 0 36px 36px',
        color: TB.cream,
      }}>
        {/* subtle pattern */}
        <svg width="220" height="220" viewBox="0 0 220 220" style={{
          position: 'absolute', left: -40, top: -20, opacity: 0.08,
        }}>
          <circle cx="110" cy="110" r="108" fill="none" stroke={TB.cream} strokeWidth="0.5" />
          <circle cx="110" cy="110" r="78" fill="none" stroke={TB.cream} strokeWidth="0.5" />
        </svg>

        <div style={{
          fontFamily: TB.sans, fontSize: 11, fontWeight: 700, letterSpacing: 1.2,
          opacity: 0.7, textTransform: 'uppercase', marginBottom: 4, position: 'relative',
        }}>Livro atual · {TB_DATA.clubs[0].name}</div>

        <div style={{ display: 'flex', gap: 18, alignItems: 'flex-start', marginTop: 14, position: 'relative' }}>
          <Cover title={book.title} author={book.author} w={108} h={162}
            style={{ boxShadow: '0 14px 32px rgba(0,0,0,0.4), 0 2px 6px rgba(0,0,0,0.25)', flexShrink: 0 }} />
          <div style={{ flex: 1, minWidth: 0, paddingTop: 6 }}>
            <h1 style={{
              fontFamily: TB.serif, fontSize: 26, fontWeight: 600, letterSpacing: -0.6,
              color: TB.cream, margin: '0 0 6px', lineHeight: 1.05, textWrap: 'balance',
            }}>{book.title}</h1>
            <div style={{ fontFamily: TB.sans, fontSize: 13, opacity: 0.7, marginBottom: 14 }}>
              {book.author}
            </div>
            <div style={{
              display: 'flex', flexDirection: 'column', gap: 6,
              fontFamily: TB.sans, fontSize: 12, opacity: 0.85,
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <I.starFill size={13} stroke="#E6BF6B" /> <b style={{ fontWeight: 600 }}>{book.rating}</b>
                <span style={{ opacity: 0.6 }}>· {book.genre}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <I.book size={13} stroke={TB.cream} /> {book.chapterList.length} capítulos
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <I.groups size={14} stroke={TB.cream} /> {book.readers} lendo
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Progress card — overlaps the hero curve */}
      <div style={{ padding: '0 22px', marginTop: -54, position: 'relative', zIndex: 2 }}>
        <Card padding="16px 18px" style={{ background: TB.card, boxShadow: '0 8px 28px rgba(0,0,0,0.08), 0 2px 6px rgba(0,0,0,0.04)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
            <div>
              <div style={{ fontFamily: TB.sans, fontSize: 11, fontWeight: 700, color: TB.muted, letterSpacing: 0.6, textTransform: 'uppercase' }}>
                Tua leitura
              </div>
              <div style={{ fontFamily: TB.serif, fontSize: 22, fontWeight: 600, color: TB.ink, marginTop: 2, letterSpacing: -0.4 }}>
                Cap. {book.userChapter} <span style={{ color: TB.muted, fontWeight: 400 }}>de {book.chapters}</span>
              </div>
            </div>
            <div style={{
              width: 56, height: 56, borderRadius: '50%',
              background: `conic-gradient(${TB.primary} ${pct * 3.6}deg, ${TB.dividerSoft} 0)`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <div style={{
                width: 44, height: 44, borderRadius: '50%', background: TB.card,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontFamily: TB.serif, fontSize: 14, fontWeight: 600, color: TB.ink,
              }}>{pct}%</div>
            </div>
          </div>
          <PillButton variant="primary" size="md" full onClick={openProgressSheet}
            iconLeft={<I.check size={16} stroke={TB.cream} sw={2.4} />}>
            Marcar progresso
          </PillButton>
        </Card>
      </div>

      {/* Chapter list */}
      <div style={{ padding: '24px 22px 8px' }}>
        <div style={{
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
          padding: '0 0 14px',
        }}>
          <h2 style={{ fontFamily: TB.serif, fontSize: 19, fontWeight: 600, color: TB.ink, margin: 0, letterSpacing: -0.3 }}>
            Capítulos
          </h2>
          <span style={{ fontFamily: TB.sans, fontSize: 12, color: TB.muted, fontWeight: 500 }}>
            {book.chapterList.filter(c => c.state === 'read').length} lidos
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {book.chapterList.map(ch => (
            <ChapterRow key={ch.n} ch={ch} onTap={() => {
              if (ch.state !== 'locked') goTo('chapter', { chapter: ch });
            }} />
          ))}
        </div>
      </div>
      <div style={{ height: 24 }} />
    </ScreenScroll>
  );
}

function ChapterRow({ ch, onTap }) {
  const locked = ch.state === 'locked';
  const current = ch.state === 'current';

  return (
    <button onClick={onTap} disabled={locked} style={{
      display: 'flex', alignItems: 'center', gap: 14,
      background: current ? TB.cream : 'transparent',
      border: current ? `1.5px solid ${TB.primary}` : `0.5px solid ${TB.divider}`,
      borderRadius: 16, padding: '12px 14px', cursor: locked ? 'default' : 'pointer',
      textAlign: 'left', width: '100%',
      opacity: locked ? 0.65 : 1,
    }}>
      {/* indicator */}
      <div style={{
        width: 36, height: 36, borderRadius: '50%', flexShrink: 0,
        background: locked ? TB.dividerSoft : current ? TB.primary : TB.secondarySoft,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: locked ? TB.muted : current ? TB.cream : TB.secondaryDark,
        fontFamily: TB.serif, fontSize: 14, fontWeight: 600,
      }}>
        {locked ? <I.lock size={16} stroke={TB.muted} /> :
          current ? <I.book size={18} stroke={TB.cream} /> :
          <I.check size={18} stroke={TB.secondaryDark} sw={2.4} />}
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary, letterSpacing: 0.4, textTransform: 'uppercase' }}>
            Cap. {ch.n}
          </span>
          {current && <Pill variant="terra" size="sm">Atual</Pill>}
        </div>
        <div style={{ fontFamily: TB.serif, fontSize: 15, fontWeight: 500, color: locked ? TB.muted : TB.ink, marginTop: 2 }}>
          {ch.title}
        </div>
      </div>

      <div style={{ textAlign: 'right', flexShrink: 0 }}>
        {locked ? (
          <div style={{ fontFamily: TB.sans, fontSize: 11, color: TB.muted }}>Chega aqui<br/>pra liberar</div>
        ) : (
          <div style={{ fontFamily: TB.sans, fontSize: 13, color: TB.tertiary, fontWeight: 500 }}>
            {ch.comments} {ch.comments === 1 ? 'comentário' : 'comentários'}
          </div>
        )}
      </div>
    </button>
  );
}

// ─────────────────────────────────────────────────────────────
// 7. Chapter discussion
// ─────────────────────────────────────────────────────────────
function ChapterScreen({ chapter, onBack }) {
  const [comments, setComments] = React.useState(TB_DATA.comments);
  const [input, setInput] = React.useState('');
  const [userReactions, setUserReactions] = React.useState({}); // commentIdx -> emoji
  const scrollRef = React.useRef(null);

  const toggleReaction = (i, emoji) => {
    const next = comments.slice();
    const c = { ...next[i], reactions: { ...next[i].reactions } };
    const was = userReactions[i] === emoji;
    if (userReactions[i] && userReactions[i] !== emoji) {
      c.reactions[userReactions[i]] = Math.max(0, c.reactions[userReactions[i]] - 1);
    }
    c.reactions[emoji] = (c.reactions[emoji] || 0) + (was ? -1 : 1);
    next[i] = c;
    setComments(next);
    setUserReactions({ ...userReactions, [i]: was ? null : emoji });
  };

  const send = () => {
    if (!input.trim()) return;
    setComments([...comments, {
      author: 'Bia', when: 'agora', own: true, text: input.trim(),
      reactions: { '❤': 0, '🤯': 0, '💀': 0, '✨': 0 },
    }]);
    setInput('');
    setTimeout(() => {
      if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }, 50);
  };

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />

      {/* Header */}
      <div style={{
        padding: '8px 18px 14px', display: 'flex', alignItems: 'center', gap: 6,
        borderBottom: `0.5px solid ${TB.divider}`, background: TB.paper,
      }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={22} stroke={TB.ink} sw={2} />
        </button>
        <div style={{ flex: 1, textAlign: 'center' }}>
          <div style={{ fontFamily: TB.sans, fontSize: 11, fontWeight: 700, color: TB.tertiary, textTransform: 'uppercase', letterSpacing: 0.6 }}>
            Capítulo {chapter.n}
          </div>
          <div style={{ fontFamily: TB.serif, fontSize: 16, fontWeight: 600, color: TB.ink, letterSpacing: -0.2 }}>
            {chapter.title}
          </div>
        </div>
        <button style={{ background: 'transparent', border: 'none', padding: 8, marginRight: -8, cursor: 'pointer' }}>
          <I.more size={20} stroke={TB.ink} />
        </button>
      </div>

      <div ref={scrollRef} style={{ flex: 1, overflowY: 'auto', padding: '14px 18px 18px' }}>
        {/* spoiler clearance pill */}
        <div style={{
          background: TB.secondarySoft, padding: '12px 16px', borderRadius: 14,
          display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16,
        }}>
          <div style={{
            width: 28, height: 28, borderRadius: '50%', background: TB.secondary,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <I.check size={16} stroke={TB.cream} sw={2.6} />
          </div>
          <div>
            <div style={{ fontFamily: TB.serif, fontSize: 14, fontWeight: 600, color: TB.secondaryDark }}>
              Tu já passou daqui.
            </div>
            <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.secondaryDark, opacity: 0.85 }}>
              Tá liberado.
            </div>
          </div>
        </div>

        {/* comment list */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {comments.map((c, i) => (
            <CommentBubble key={i} comment={c} idx={i}
              userReaction={userReactions[i]}
              onReact={(emoji) => toggleReaction(i, emoji)} />
          ))}
        </div>
      </div>

      {/* Input footer */}
      <div style={{
        padding: '10px 14px 14px',
        background: TB.cream,
        borderTop: `0.5px solid ${TB.divider}`,
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <div style={{
          flex: 1, background: TB.paper, borderRadius: 999,
          display: 'flex', alignItems: 'center', padding: '4px 6px 4px 16px',
          border: `1px solid ${TB.divider}`,
        }}>
          <input value={input} onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') send(); }}
            placeholder="Comenta esse capítulo..."
            style={{
              flex: 1, background: 'transparent', border: 'none',
              padding: '10px 0', outline: 'none',
              fontFamily: TB.sans, fontSize: 14, color: TB.ink,
            }} />
          <button style={{ background: 'transparent', border: 'none', padding: 6, cursor: 'pointer' }}>
            <I.smile size={20} stroke={TB.tertiary} />
          </button>
        </div>
        <button onClick={send} disabled={!input.trim()} style={{
          width: 44, height: 44, borderRadius: '50%',
          background: input.trim() ? TB.primary : TB.dividerSoft,
          border: 'none', cursor: input.trim() ? 'pointer' : 'default',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <I.send size={18} stroke={input.trim() ? TB.cream : TB.muted} sw={2} style={{ transform: 'translateX(-1px)' }} />
        </button>
      </div>
    </div>
  );
}

function CommentBubble({ comment, idx, userReaction, onReact }) {
  const own = comment.own;
  const REACTIONS = ['❤', '🤯', '💀', '✨'];

  return (
    <div style={{ display: 'flex', gap: 10 }}>
      <Avatar name={comment.author} size={36} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          background: own ? TB.primarySoft : TB.cream,
          borderRadius: 14, padding: '10px 14px',
          border: own ? 'none' : `0.5px solid ${TB.divider}`,
        }}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 4 }}>
            <span style={{ fontFamily: TB.sans, fontSize: 13, fontWeight: 600, color: own ? TB.primaryDark : TB.ink }}>
              {own ? 'Tu' : comment.author}
            </span>
            <span style={{ fontFamily: TB.sans, fontSize: 11, color: TB.muted }}>
              {comment.when}
            </span>
          </div>
          <div style={{ fontFamily: TB.serif, fontSize: 14.5, lineHeight: 1.45, color: TB.ink, textWrap: 'pretty' }}>
            {comment.text}
          </div>
        </div>

        {/* reactions */}
        <div style={{ display: 'flex', gap: 6, marginTop: 6, alignItems: 'center', flexWrap: 'wrap' }}>
          {REACTIONS.map(e => {
            const count = comment.reactions[e] || 0;
            const active = userReaction === e;
            return (
              <button key={e} onClick={() => onReact(e)} style={{
                background: active ? TB.primarySoft : TB.cardSoft,
                border: `1px solid ${active ? TB.primary : TB.divider}`,
                borderRadius: 999, padding: '3px 8px',
                fontFamily: TB.sans, fontSize: 11, color: TB.ink,
                cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4,
                minWidth: count ? 'auto' : 28, justifyContent: 'center',
              }}>
                <span style={{ fontSize: 12 }}>{e}</span>
                {count > 0 && <span style={{ fontWeight: 600, color: active ? TB.primaryDark : TB.tertiary }}>{count}</span>}
              </button>
            );
          })}
          <button style={{
            background: 'transparent', border: 'none', color: TB.muted, cursor: 'pointer',
            fontFamily: TB.sans, fontSize: 11, fontWeight: 600, padding: '3px 6px',
            display: 'flex', alignItems: 'center', gap: 4,
          }}>
            <I.reply size={12} stroke={TB.muted} /> Responder
          </button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { HomeScreen, CurrentBookScreen, ChapterScreen, ChapterRow, CommentBubble, MeetingTicket, ReaderChip });
