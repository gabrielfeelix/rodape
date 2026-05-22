// screens-book-detail.jsx — Book detail page (clicking a shelf book)
//                          + Frases (saved quotes) screen + sheet

// ─────────────────────────────────────────────────────────────
// Sample data for shelf books — quotes, club summary, etc.
// ─────────────────────────────────────────────────────────────
const BOOK_DETAILS = {
  'Olhos d\'água': {
    title: 'Olhos d\'água', author: 'Conceição Evaristo', year: 2014,
    pages: 116, genre: 'Contos', rating: 4.8, readers: 7, myRating: 5,
    finished: 'set/25', club: 'Leituras de domingo',
    summary: 'Quinze contos sobre mulheres negras brasileiras. Da memória da mãe ao banzo de outras vidas — Evaristo escreve com olho clínico e amor por aqueles a quem a história raramente concede personagem.',
    clubReview: 'Saiu um papo riquíssimo sobre o conto "Lumbiá" e a forma como Evaristo trata violência sem espetacularizar. Voltamos várias vezes ao texto. Conseguimos chegar à conclusão de que nenhum conto sobrava — cada um servia ao livro.',
    quotes: [
      { text: 'A vida é tecida de fios sem nome, e a gente vai se enovelando.', chapter: 'Lumbiá',     by: 'Marina', when: 'set/25' },
      { text: 'O choro de minha mãe não desaguava em rio. Empoçava nos olhos.',  chapter: 'Olhos d\'água', by: 'Bia',    when: 'set/25' },
      { text: 'A gente queria pouco. Queria vida. E vida, naquela hora, era luxo.', chapter: 'Beijo na face', by: 'Júlia',  when: 'set/25' },
    ],
    timeline: [
      { label: 'Sugerido por Bia', date: '12 ago' },
      { label: 'Votação fechou',   date: '20 ago' },
      { label: 'Leitura começou',  date: '01 set' },
      { label: 'Encontro do clube',date: '28 set' },
    ],
  },
  'Pedro Páramo': {
    title: 'Pedro Páramo', author: 'Juan Rulfo', year: 1955,
    pages: 124, genre: 'Romance', rating: 4.4, readers: 6, myRating: 4,
    finished: 'ago/25', club: 'Leituras de domingo',
    summary: 'Juan Preciado promete à mãe moribunda procurar o pai em Comala, uma vila onde os mortos cochicham. Rulfo dissolve tempo, memória e voz num livro que cabe num suspiro mas se estende para a eternidade.',
    clubReview: 'Ninguém entendeu na primeira leitura. Foi por isso que ficou inesquecível. Voltamos a Comala já como fantasmas também — leitores que perderam o referencial e gostaram.',
    quotes: [
      { text: 'Vim a Comala porque me disseram que aqui vivia meu pai, um tal de Pedro Páramo.', chapter: 'Início', by: 'Rafael', when: 'ago/25' },
      { text: 'O céu é alto e mesquinho, e bem podia chover.',                                    chapter: 'Capítulo 14', by: 'Bia',    when: 'ago/25' },
    ],
    timeline: [
      { label: 'Sugerido por Leo', date: '01 jul' },
      { label: 'Leitura começou',  date: '15 jul' },
      { label: 'Encontro do clube',date: '24 ago' },
    ],
  },
};

function getBookDetail(title) {
  return BOOK_DETAILS[title] || {
    title, author: '—', year: 2024, pages: 200, genre: 'Romance',
    rating: 4.2, readers: 5, myRating: 4, finished: 'mai/25',
    club: 'Leituras de domingo',
    summary: 'Resumo curtinho do livro — três a quatro linhas sobre o que é a história, sem spoiler, escrito como se fosse um amigo te contando.',
    clubReview: 'Foi um livro que dividiu o clube. Metade amou, metade não embarcou. Acabou virando uma das discussões mais boas do ano.',
    quotes: [
      { text: 'Uma frase boa que a gente quis guardar dessa leitura.', chapter: 'Cap. 3', by: 'Marina', when: 'mai/25' },
    ],
    timeline: [
      { label: 'Leitura começou', date: '03 abr' },
      { label: 'Encontro do clube', date: '22 mai' },
    ],
  };
}

// ─────────────────────────────────────────────────────────────
// BookDetailScreen
// ─────────────────────────────────────────────────────────────
function BookDetailScreen({ title, onBack, onOpenQuotes }) {
  const b = getBookDetail(title);
  const [tab, setTab] = React.useState('resumo'); // resumo, frases, timeline

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />

      {/* Hero — soft sage with cover */}
      <div style={{
        background: `linear-gradient(180deg, ${TB.secondarySoft} 0%, ${TB.paper} 100%)`,
        padding: '0 22px 24px', position: 'relative',
      }}>
        <div style={{
          padding: '4px 0 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <button onClick={onBack} style={{
            background: TB.card, border: `1px solid ${TB.divider}`, borderRadius: '50%',
            width: 38, height: 38, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <I.chevL size={18} stroke={TB.ink} sw={2} />
          </button>
          <button style={{
            background: TB.card, border: `1px solid ${TB.divider}`, borderRadius: '50%',
            width: 38, height: 38, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <I.more size={18} stroke={TB.ink} />
          </button>
        </div>

        <div style={{ display: 'flex', justifyContent: 'center', marginTop: 8 }}>
          <Cover title={b.title} author={b.author} w={150} h={224}
            style={{ boxShadow: '0 18px 38px rgba(41,56,32,0.22), 0 4px 10px rgba(0,0,0,0.10)' }} />
        </div>

        <h1 style={{
          fontFamily: TB.serif, fontSize: 26, fontWeight: 600, letterSpacing: -0.6,
          color: TB.ink, margin: '18px 0 4px', textAlign: 'center',
          lineHeight: 1.1, textWrap: 'balance',
        }}>{b.title}</h1>
        <div style={{ fontFamily: TB.sans, fontSize: 14, color: TB.tertiary, textAlign: 'center', marginBottom: 14 }}>
          {b.author} · {b.year}
        </div>

        {/* Meta row */}
        <div style={{
          display: 'flex', justifyContent: 'center', gap: 18,
          fontFamily: TB.sans, fontSize: 12, color: TB.inkSoft,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <I.starFill size={13} stroke="#E6BF6B" />
            <b style={{ fontWeight: 600 }}>{b.rating}</b>
            <span style={{ color: TB.muted }}>do clube</span>
          </div>
          <div style={{ width: 3, height: 3, background: TB.tertiarySoft, borderRadius: '50%', alignSelf: 'center' }} />
          <span>{b.pages} págs</span>
          <div style={{ width: 3, height: 3, background: TB.tertiarySoft, borderRadius: '50%', alignSelf: 'center' }} />
          <span>{b.genre}</span>
        </div>
      </div>

      {/* Read status banner */}
      <div style={{ padding: '12px 22px 0' }}>
        <div style={{
          background: TB.secondary, color: TB.cream,
          borderRadius: 14, padding: '10px 14px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              width: 28, height: 28, borderRadius: '50%',
              background: 'rgba(251,250,244,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <I.check size={16} stroke={TB.cream} sw={2.6} />
            </div>
            <div>
              <div style={{ fontFamily: TB.sans, fontSize: 13, fontWeight: 600 }}>Tu leu em {b.finished}</div>
              <div style={{ fontFamily: TB.sans, fontSize: 11, opacity: 0.8 }}>com {b.club}</div>
            </div>
          </div>
          {/* my rating */}
          <div style={{ display: 'flex', gap: 2 }}>
            {[1,2,3,4,5].map(i => (
              <span key={i} style={{ color: i <= b.myRating ? '#E6BF6B' : 'rgba(251,250,244,0.3)' }}>
                <I.starFill size={14} stroke={i <= b.myRating ? '#E6BF6B' : 'rgba(251,250,244,0.3)'} />
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div style={{
        padding: '14px 22px 0', display: 'flex', gap: 4,
        borderBottom: `1px solid ${TB.divider}`,
      }}>
        {[
          { id: 'resumo',   label: 'Resumo' },
          { id: 'frases',   label: 'Frases', badge: b.quotes.length },
          { id: 'timeline', label: 'Histórico' },
        ].map(t => {
          const active = tab === t.id;
          return (
            <button key={t.id} onClick={() => setTab(t.id)} style={{
              background: 'transparent', border: 'none', padding: '10px 14px 12px',
              cursor: 'pointer', position: 'relative',
              fontFamily: TB.sans, fontSize: 13, fontWeight: 600,
              color: active ? TB.ink : TB.muted,
              display: 'inline-flex', alignItems: 'center', gap: 6,
            }}>
              {t.label}
              {t.badge != null && (
                <span style={{
                  background: active ? TB.primary : TB.dividerSoft,
                  color: active ? TB.cream : TB.muted,
                  fontSize: 10, fontWeight: 700, padding: '1px 6px', borderRadius: 999,
                }}>{t.badge}</span>
              )}
              {active && (
                <div style={{ position: 'absolute', bottom: -1, left: 8, right: 8, height: 2, background: TB.primary, borderRadius: 2 }} />
              )}
            </button>
          );
        })}
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '16px 22px 24px' }}>
        {tab === 'resumo' && (
          <>
            <p style={{
              fontFamily: TB.serif, fontSize: 15.5, lineHeight: 1.55, color: TB.inkSoft,
              margin: '0 0 22px', textWrap: 'pretty',
            }}>{b.summary}</p>

            <div style={{
              fontFamily: TB.sans, fontSize: 11, fontWeight: 700, color: TB.tertiary,
              textTransform: 'uppercase', letterSpacing: 0.8, marginBottom: 8,
            }}>O que o clube achou</div>
            <Card padding="16px 18px" style={{ background: TB.card, borderLeft: `3px solid ${TB.secondary}`, borderRadius: 14 }}>
              <p style={{
                fontFamily: TB.serif, fontSize: 14, lineHeight: 1.5, color: TB.inkSoft,
                margin: 0, fontStyle: 'italic', textWrap: 'pretty',
              }}>"{b.clubReview}"</p>
              <div style={{
                marginTop: 12, paddingTop: 10, borderTop: `1px solid ${TB.dividerSoft}`,
                display: 'flex', alignItems: 'center', gap: 8,
                fontFamily: TB.sans, fontSize: 11, color: TB.muted,
              }}>
                <I.calendar size={12} stroke={TB.muted} />
                Encontro de {b.finished}
              </div>
            </Card>
          </>
        )}

        {tab === 'frases' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {b.quotes.map((q, i) => <QuoteCard key={i} q={q} />)}
            <button style={{
              padding: 14, borderRadius: 14,
              background: 'transparent', border: `1.5px dashed ${TB.divider}`,
              cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
              fontFamily: TB.sans, fontSize: 13, fontWeight: 600, color: TB.tertiary,
            }}>
              <I.plus size={14} stroke={TB.tertiary} sw={2.2} />
              Salvar uma frase
            </button>
          </div>
        )}

        {tab === 'timeline' && (
          <div style={{ paddingLeft: 8 }}>
            {b.timeline.map((t, i) => (
              <div key={i} style={{ display: 'flex', gap: 14, paddingBottom: i < b.timeline.length - 1 ? 18 : 0, position: 'relative' }}>
                <div style={{ position: 'relative', width: 12, flexShrink: 0, paddingTop: 6 }}>
                  <div style={{
                    width: 12, height: 12, borderRadius: '50%',
                    background: i === b.timeline.length - 1 ? TB.secondary : TB.card,
                    border: `2px solid ${TB.secondary}`,
                  }} />
                  {i < b.timeline.length - 1 && (
                    <div style={{
                      position: 'absolute', top: 22, bottom: -18, left: 5,
                      width: 2, background: TB.dividerSoft,
                    }} />
                  )}
                </div>
                <div style={{ flex: 1, paddingTop: 2 }}>
                  <div style={{ fontFamily: TB.sans, fontSize: 13.5, fontWeight: 600, color: TB.ink }}>{t.label}</div>
                  <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.muted, marginTop: 2 }}>{t.date}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// QuoteCard — used both in BookDetail and Frases screen
// ─────────────────────────────────────────────────────────────
function QuoteCard({ q, showBook }) {
  return (
    <div style={{
      background: TB.card, borderRadius: 16, padding: '18px 20px',
      border: `1px solid ${TB.divider}`,
      position: 'relative',
    }}>
      <div style={{
        position: 'absolute', top: 8, left: 16,
        fontFamily: TB.serif, fontSize: 54, color: TB.secondarySoft,
        lineHeight: 1, fontWeight: 600,
      }}>"</div>
      <p style={{
        fontFamily: TB.serif, fontSize: 16, fontStyle: 'italic', lineHeight: 1.5,
        color: TB.inkSoft, margin: '14px 0 12px', textWrap: 'pretty',
        position: 'relative', zIndex: 1,
      }}>{q.text}</p>
      <div style={{
        paddingTop: 10, borderTop: `1px solid ${TB.dividerSoft}`,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, flex: 1 }}>
          <Avatar name={q.by} size={22} />
          <div style={{
            fontFamily: TB.sans, fontSize: 12, color: TB.muted,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>
            <b style={{ color: TB.inkSoft, fontWeight: 600 }}>{q.by}</b> · {q.chapter}
          </div>
        </div>
        <button style={{
          background: 'transparent', border: 'none', padding: 4, cursor: 'pointer',
          color: TB.muted, display: 'flex', alignItems: 'center', gap: 4,
        }}>
          <I.heart size={14} stroke={TB.muted} />
        </button>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// FrasesScreen — all saved quotes across all books read
// ─────────────────────────────────────────────────────────────
function FrasesScreen({ onBack }) {
  const all = [];
  Object.values(BOOK_DETAILS).forEach(b => {
    b.quotes.forEach(q => all.push({ ...q, book: b.title, author: b.author }));
  });

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />
      <div style={{ padding: '8px 18px 8px', display: 'flex', alignItems: 'center' }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={22} stroke={TB.ink} sw={2} />
        </button>
        <div style={{ flex: 1, fontFamily: TB.serif, fontSize: 18, fontWeight: 600, color: TB.ink, textAlign: 'center' }}>Frases</div>
        <button style={{ background: 'transparent', border: 'none', padding: 8, marginRight: -8, cursor: 'pointer' }}>
          <I.search size={20} stroke={TB.ink} />
        </button>
      </div>

      <div style={{ padding: '4px 22px 6px' }}>
        <div style={{
          fontFamily: TB.sans, fontSize: 13, color: TB.muted, marginBottom: 14, lineHeight: 1.4,
        }}>
          As frases que tu e o clube guardaram. {all.length} no total.
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '4px 22px 24px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {all.map((q, i) => (
          <div key={i}>
            <div style={{
              fontFamily: TB.serif, fontSize: 13, fontStyle: 'italic', color: TB.muted, marginBottom: 6, padding: '0 4px',
              display: 'flex', alignItems: 'center', gap: 6,
            }}>
              <I.book size={11} stroke={TB.muted} />
              <span>{q.book} · {q.author}</span>
            </div>
            <QuoteCard q={q} />
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { BookDetailScreen, QuoteCard, FrasesScreen, getBookDetail });
