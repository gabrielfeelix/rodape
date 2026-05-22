// screens-next.jsx — Próximo tab: Encontro, Votação, Estante (+ Sugerir livro)

function NextScreen({ subTab, setSubTab, goTo }) {
  return (
    <ScreenScroll padding="0">
      {/* sub-tabs */}
      <div style={{
        padding: '4px 22px 12px',
        display: 'flex', gap: 8, borderBottom: `0.5px solid ${TB.divider}`,
      }}>
        {[
          { id: 'meeting', label: 'Encontro' },
          { id: 'voting',  label: 'Votação' },
          { id: 'shelf',   label: 'Estante' },
        ].map(t => {
          const active = subTab === t.id;
          return (
            <button key={t.id} onClick={() => setSubTab(t.id)} style={{
              background: 'transparent', border: 'none', cursor: 'pointer',
              padding: '8px 4px', position: 'relative',
              fontFamily: TB.serif, fontSize: 18, fontWeight: 600,
              letterSpacing: -0.3,
              color: active ? TB.ink : TB.muted,
            }}>
              {t.label}
              {active && (
                <div style={{
                  position: 'absolute', bottom: -1, left: 4, right: 4,
                  height: 2, background: TB.primary, borderRadius: 2,
                }} />
              )}
            </button>
          );
        })}
      </div>

      {subTab === 'meeting' && <MeetingTab />}
      {subTab === 'voting'  && <VotingTab goTo={goTo} />}
      {subTab === 'shelf'   && <ShelfTab goTo={goTo} />}
    </ScreenScroll>
  );
}

// ─────────────────────────────────────────────────────────────
// 8. Encontro
// ─────────────────────────────────────────────────────────────
function MeetingTab() {
  const meeting = TB_DATA.meeting;
  const [rsvp, setRsvp] = React.useState('confirmed'); // confirmed | maybe | no | null
  const [openGroup, setOpenGroup] = React.useState({ confirmed: true, maybe: false, no: false });

  return (
    <div>
      {/* Meeting card */}
      <div style={{ padding: '16px 22px 12px' }}>
        <Card style={{ padding: 0, overflow: 'hidden', background: TB.cream }}>
          <div style={{
            background: `linear-gradient(135deg, ${TB.secondary} 0%, ${TB.secondaryDark} 100%)`,
            color: TB.cream, padding: '20px 22px 18px',
          }}>
            <div style={{
              fontFamily: TB.sans, fontSize: 12, fontWeight: 700, letterSpacing: 1.2,
              opacity: 0.85, marginBottom: 8, textTransform: 'uppercase',
            }}>{meeting.weekday} · {meeting.day} {meeting.monthShort.toLowerCase()}</div>
            <div style={{ fontFamily: TB.serif, fontSize: 24, fontWeight: 600, letterSpacing: -0.5, lineHeight: 1.15, marginBottom: 14 }}>
              {meeting.title}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontFamily: TB.sans, fontSize: 13, opacity: 0.9 }}>
                <I.clock size={15} stroke={TB.cream} /> {meeting.timeStart} – {meeting.timeEnd}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontFamily: TB.sans, fontSize: 13, opacity: 0.9 }}>
                <I.pin size={15} stroke={TB.cream} /> {meeting.place}
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* RSVP */}
      <div style={{ padding: '8px 22px' }}>
        <div style={{ fontFamily: TB.sans, fontSize: 12, fontWeight: 700, color: TB.tertiary, textTransform: 'uppercase', letterSpacing: 0.8, marginBottom: 10 }}>
          Tua participação
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {[
            { id: 'confirmed', label: 'Vou' },
            { id: 'maybe',     label: 'Talvez' },
            { id: 'no',        label: 'Não vou' },
          ].map(opt => {
            const active = rsvp === opt.id;
            return (
              <button key={opt.id} onClick={() => setRsvp(opt.id)} style={{
                flex: 1, padding: '12px 0', borderRadius: 14,
                background: active ? TB.ink : TB.cream,
                color: active ? TB.cream : TB.ink,
                border: `1px solid ${active ? TB.ink : TB.divider}`,
                fontFamily: TB.sans, fontSize: 14, fontWeight: 600, cursor: 'pointer',
                transition: 'all .15s',
              }}>{opt.label}</button>
            );
          })}
        </div>
      </div>

      {/* Quem vai */}
      <div style={{ padding: '20px 22px 8px' }}>
        <div style={{
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 12,
        }}>
          <h3 style={{ fontFamily: TB.serif, fontSize: 18, fontWeight: 600, color: TB.ink, margin: 0, letterSpacing: -0.3 }}>
            Quem vai?
          </h3>
          <div style={{
            display: 'flex', gap: 12, fontFamily: TB.sans, fontSize: 12, color: TB.tertiary,
          }}>
            <span><b style={{ color: TB.secondaryDark }}>{meeting.confirmed.length}</b> vão</span>
            <span><b style={{ color: TB.ink }}>{meeting.maybe.length}</b> talvez</span>
            <span><b style={{ color: TB.muted }}>{meeting.no.length}</b> não</span>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <RSVPGroup label="Confirmados" people={meeting.confirmed} open={openGroup.confirmed}
            onToggle={() => setOpenGroup({ ...openGroup, confirmed: !openGroup.confirmed })} accent={TB.secondary} />
          <RSVPGroup label="Talvez" people={meeting.maybe} open={openGroup.maybe}
            onToggle={() => setOpenGroup({ ...openGroup, maybe: !openGroup.maybe })} accent={TB.tertiary} />
          <RSVPGroup label="Não vão" people={meeting.no} open={openGroup.no}
            onToggle={() => setOpenGroup({ ...openGroup, no: !openGroup.no })} accent={TB.muted} />
        </div>
      </div>

      {/* Agenda */}
      <div style={{ padding: '16px 22px 24px' }}>
        <h3 style={{ fontFamily: TB.serif, fontSize: 18, fontWeight: 600, color: TB.ink, margin: '0 0 12px', letterSpacing: -0.3 }}>
          Pauta
        </h3>
        <Card padding="14px 16px" style={{ background: TB.cream }}>
          <ol style={{ margin: 0, padding: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 10 }}>
            {meeting.agenda.map((item, i) => (
              <li key={i} style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                <div style={{
                  width: 22, height: 22, borderRadius: '50%', flexShrink: 0,
                  background: TB.secondarySoft, color: TB.secondaryDark,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontFamily: TB.sans, fontSize: 11, fontWeight: 700,
                }}>{i + 1}</div>
                <span style={{ fontFamily: TB.sans, fontSize: 14, color: TB.ink, lineHeight: 1.4 }}>{item}</span>
              </li>
            ))}
          </ol>
        </Card>
      </div>
    </div>
  );
}

function RSVPGroup({ label, people, open, onToggle, accent }) {
  return (
    <div style={{
      background: TB.cream, borderRadius: 14, border: `0.5px solid ${TB.divider}`, overflow: 'hidden',
    }}>
      <button onClick={onToggle} style={{
        width: '100%', background: 'transparent', border: 'none', padding: '12px 14px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ width: 8, height: 8, borderRadius: '50%', background: accent }} />
          <span style={{ fontFamily: TB.sans, fontSize: 14, fontWeight: 600, color: TB.ink }}>{label}</span>
          <span style={{ fontFamily: TB.sans, fontSize: 13, color: TB.muted }}>· {people.length}</span>
        </div>
        <div style={{ transform: open ? 'rotate(180deg)' : 'none', transition: 'transform .15s' }}>
          <I.chevD size={16} stroke={TB.tertiary} />
        </div>
      </button>
      {open && (
        <div style={{ padding: '4px 14px 14px', display: 'flex', flexDirection: 'column', gap: 8 }}>
          {people.length === 0 && (
            <div style={{ fontFamily: TB.sans, fontSize: 13, color: TB.muted, fontStyle: 'italic' }}>Ninguém ainda.</div>
          )}
          {people.map(p => (
            <div key={p} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <Avatar name={p} size={28} />
              <span style={{ fontFamily: TB.sans, fontSize: 14, color: TB.ink }}>{p}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 9. Votação
// ─────────────────────────────────────────────────────────────
function VotingTab({ goTo }) {
  const [books, setBooks] = React.useState(TB_DATA.votingBooks);
  const votedIdx = books.findIndex(b => b.mine);
  const totalVotes = books.reduce((s, b) => s + b.votes, 0);

  const vote = (i) => {
    setBooks(books.map((b, j) => ({
      ...b,
      mine: j === i,
      votes: b.votes + (j === i ? (b.mine ? 0 : 1) : 0) - (j !== i && b.mine ? 1 : 0),
    })));
  };

  return (
    <div>
      <div style={{ padding: '14px 22px 4px' }}>
        <h2 style={{ fontFamily: TB.serif, fontSize: 22, fontWeight: 600, color: TB.ink, margin: '0 0 4px', letterSpacing: -0.4 }}>
          Próximo livro
        </h2>
        <p style={{ fontFamily: TB.sans, fontSize: 13, color: TB.tertiary, margin: 0, lineHeight: 1.4 }}>
          Vota num. Tu pode trocar até a votação fechar.
        </p>
      </div>

      <div style={{ padding: '14px 22px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        {books.map((b, i) => (
          <VotingCard key={b.id} book={b} onVote={() => vote(i)} total={totalVotes} />
        ))}
      </div>

      <div style={{ padding: '4px 22px 18px' }}>
        <PillButton variant="outline" size="md" full onClick={() => goTo('suggest')}
          iconLeft={<I.plus size={16} stroke={TB.ink} sw={2.2} />}>
          Sugerir livro
        </PillButton>
        <div style={{
          textAlign: 'center', marginTop: 14,
          fontFamily: TB.sans, fontSize: 12, color: TB.muted,
        }}>
          5 de 7 já votaram · fecha em <b style={{ color: TB.ink, fontWeight: 600 }}>2 dias</b>
        </div>
      </div>
    </div>
  );
}

function VotingCard({ book, onVote, total }) {
  const pct = total ? Math.round((book.votes / total) * 100) : 0;
  return (
    <Card padding={16} style={{ background: TB.cream }}>
      <div style={{ display: 'flex', gap: 14 }}>
        <Cover title={book.title} author={book.author} w={68} h={102} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <h3 style={{ fontFamily: TB.serif, fontSize: 17, fontWeight: 600, color: TB.ink, margin: '0 0 2px', letterSpacing: -0.3, lineHeight: 1.15 }}>
            {book.title}
          </h3>
          <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.tertiary, marginBottom: 8 }}>
            {book.author}
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 6,
            fontFamily: TB.sans, fontSize: 12, color: TB.muted, marginBottom: 6,
          }}>
            <Avatar name={book.suggester} size={18} />
            <span>Sugerido por <b style={{ color: TB.ink, fontWeight: 600 }}>{book.suggester}</b></span>
          </div>
        </div>
      </div>

      {book.reason && (
        <p style={{
          fontFamily: TB.serif, fontSize: 14, lineHeight: 1.4, color: TB.inkSoft,
          fontStyle: 'italic', margin: '10px 0 12px',
          padding: '8px 12px', borderLeft: `2px solid ${TB.tertiarySoft}`,
          textWrap: 'pretty',
        }}>"{book.reason}"</p>
      )}

      <div style={{ marginTop: 10 }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 5 }}>
          <span style={{ fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary }}>
            {book.votes} {book.votes === 1 ? 'voto' : 'votos'} · {pct}%
          </span>
        </div>
        <Progress value={pct} color={book.mine ? TB.primary : TB.secondary} />
      </div>

      <div style={{ marginTop: 12 }}>
        <PillButton variant={book.mine ? 'soft' : 'dark'} size="sm" full onClick={onVote}
          iconLeft={book.mine ? <I.check size={14} stroke={TB.primaryDark} sw={2.4} /> : null}>
          {book.mine ? 'Teu voto' : 'Votar nesse'}
        </PillButton>
      </div>
    </Card>
  );
}

// ─────────────────────────────────────────────────────────────
// 10. Sugerir livro
// ─────────────────────────────────────────────────────────────
function SuggestBookScreen({ onBack, onAdd }) {
  const [q, setQ] = React.useState('Conceição');
  const [sel, setSel] = React.useState(null);
  const [modal, setModal] = React.useState(false);
  const [reason, setReason] = React.useState('');

  const results = [
    { title: 'Olhos d\'água',        author: 'Conceição Evaristo', year: '2014' },
    { title: 'Becos da memória',     author: 'Conceição Evaristo', year: '2006' },
    { title: 'Insubmissas lágrimas de mulheres', author: 'Conceição Evaristo', year: '2011' },
    { title: 'Ponciá Vicêncio',      author: 'Conceição Evaristo', year: '2003' },
    { title: 'Histórias de leves enganos e parecenças', author: 'Conceição Evaristo', year: '2016' },
  ].filter(r => !q || r.title.toLowerCase().includes(q.toLowerCase()) || r.author.toLowerCase().includes(q.toLowerCase()));

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />
      <div style={{
        padding: '8px 18px 12px', display: 'flex', alignItems: 'center', gap: 6,
      }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={22} stroke={TB.ink} sw={2} />
        </button>
        <div style={{
          flex: 1, fontFamily: TB.serif, fontSize: 18, fontWeight: 600, color: TB.ink, textAlign: 'center', letterSpacing: -0.2,
        }}>Sugerir livro</div>
        <button disabled={!sel} onClick={() => setModal(true)} style={{
          background: 'transparent', border: 'none', padding: 8, marginRight: -8, cursor: sel ? 'pointer' : 'default',
          color: sel ? TB.primary : TB.muted,
          fontFamily: TB.sans, fontSize: 14, fontWeight: 600,
        }}>Adicionar</button>
      </div>

      {/* search */}
      <div style={{ padding: '4px 22px 14px' }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '12px 16px', borderRadius: 14,
          background: TB.cream, border: `1px solid ${TB.divider}`,
        }}>
          <I.search size={18} stroke={TB.tertiary} />
          <input value={q} onChange={e => setQ(e.target.value)} placeholder="Buscar por título, autor ou ISBN"
            style={{ flex: 1, border: 'none', background: 'transparent', outline: 'none', fontFamily: TB.sans, fontSize: 14, color: TB.ink }} />
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 22px 20px' }}>
        {q ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {results.map((r, i) => {
              const selected = sel?.title === r.title;
              return (
                <button key={i} onClick={() => setSel(r)} style={{
                  display: 'flex', gap: 12, alignItems: 'center',
                  background: selected ? TB.cream : 'transparent',
                  border: `1.5px solid ${selected ? TB.primary : TB.divider}`,
                  borderRadius: 14, padding: 10, cursor: 'pointer', textAlign: 'left',
                }}>
                  <Cover title={r.title} author={r.author} w={48} h={72} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: TB.serif, fontSize: 15, fontWeight: 600, color: TB.ink, lineHeight: 1.15, marginBottom: 2 }}>{r.title}</div>
                    <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.tertiary }}>{r.author} · {r.year}</div>
                  </div>
                  {selected && (
                    <div style={{
                      width: 24, height: 24, borderRadius: '50%', background: TB.primary,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                      <I.check size={14} stroke={TB.cream} sw={2.8} />
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: TB.muted, fontFamily: TB.sans, fontSize: 14 }}>
            Comece a digitar pra encontrar livros.
          </div>
        )}
      </div>

      {/* modal */}
      {modal && (
        <div onClick={() => setModal(false)} style={{
          position: 'absolute', inset: 0, background: 'rgba(20,18,15,0.55)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          padding: 24, zIndex: 100,
        }}>
          <div onClick={e => e.stopPropagation()} style={{
            background: TB.paper, borderRadius: 22, padding: 22, width: '100%', maxWidth: 320,
          }}>
            <h3 style={{ fontFamily: TB.serif, fontSize: 22, fontWeight: 600, color: TB.ink, margin: '0 0 10px', letterSpacing: -0.4 }}>
              Por que esse livro?
            </h3>
            <p style={{ fontFamily: TB.sans, fontSize: 13, color: TB.tertiary, margin: '0 0 14px', lineHeight: 1.4 }}>
              Opcional. Ajuda a galera a votar.
            </p>
            <textarea value={reason} onChange={e => setReason(e.target.value)} rows={4}
              placeholder="Tipo: queria a gente lendo brasileiras esse semestre..."
              style={{
                width: '100%', padding: 12, borderRadius: 12,
                background: TB.cream, border: `1px solid ${TB.divider}`,
                fontFamily: TB.sans, fontSize: 14, color: TB.ink, outline: 'none',
                boxSizing: 'border-box', resize: 'none',
              }} />
            <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
              <PillButton variant="outline" size="md" full onClick={() => setModal(false)}>Voltar</PillButton>
              <PillButton variant="primary" size="md" full onClick={() => { setModal(false); onAdd(); }}>Adicionar</PillButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 11. Estante
// ─────────────────────────────────────────────────────────────
function ShelfTab({ goTo }) {
  const [filter, setFilter] = React.useState('year');
  const books = TB_DATA.shelf;

  return (
    <div>
      <div style={{ padding: '16px 22px 12px', display: 'flex', gap: 8, overflowX: 'auto' }}>
        {[
          { id: 'year',   label: 'Por ano' },
          { id: 'rating', label: 'Por nota' },
          { id: 'read',   label: 'Mais lidos' },
        ].map(o => {
          const active = filter === o.id;
          return (
            <button key={o.id} onClick={() => setFilter(o.id)} style={{
              padding: '7px 14px', borderRadius: 999,
              background: active ? TB.ink : TB.cream,
              color: active ? TB.cream : TB.tertiary,
              border: `1px solid ${active ? TB.ink : TB.divider}`,
              fontFamily: TB.sans, fontSize: 13, fontWeight: 600, cursor: 'pointer',
              flexShrink: 0,
            }}>{o.label}</button>
          );
        })}
      </div>

      <div style={{
        display: 'grid', gridTemplateColumns: '1fr 1fr',
        gap: 18, padding: '8px 22px 24px',
      }}>
        {books.map((b, i) => (
          <button key={i} onClick={() => goTo && goTo('book-detail', { title: b.title })} style={{
            display: 'flex', flexDirection: 'column', alignItems: 'flex-start', textAlign: 'left',
            background: 'transparent', border: 'none', padding: 0, cursor: 'pointer',
          }}>
            <Cover title={b.title} author={b.author} w={146} h={216} style={{ marginBottom: 10 }} />
            <h4 style={{ fontFamily: TB.serif, fontSize: 14, fontWeight: 600, color: TB.ink, margin: '0 0 2px', letterSpacing: -0.2, lineHeight: 1.15 }}>
              {b.title}
            </h4>
            <div style={{ fontFamily: TB.sans, fontSize: 11, color: TB.tertiary, marginBottom: 4 }}>
              {b.author}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontFamily: TB.sans, fontSize: 11, color: TB.muted }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                <I.starFill size={11} stroke="#A6802B" /> <b style={{ color: TB.ink, fontWeight: 600 }}>{b.rating}</b>
              </span>
              <span style={{ width: 2, height: 2, background: TB.tertiarySoft, borderRadius: '50%' }} />
              <span>{b.when}</span>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { NextScreen, MeetingTab, VotingTab, SuggestBookScreen, ShelfTab });
