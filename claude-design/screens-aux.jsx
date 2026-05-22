// screens-aux.jsx — Profile, Edit profile, Notifications, Bottom sheets

// ─────────────────────────────────────────────────────────────
// 12. Perfil
// ─────────────────────────────────────────────────────────────
function ProfileScreen({ goTo, theme, setTheme }) {
  const user = TB_DATA.user;
  const clubs = TB_DATA.clubs;

  return (
    <ScreenScroll>
      <div style={{ padding: '4px 22px 14px', display: 'flex', alignItems: 'center', gap: 16 }}>
        <Avatar name={user.name} size={72} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <h1 style={{ fontFamily: TB.serif, fontSize: 22, fontWeight: 600, color: TB.ink, margin: '0 0 2px', letterSpacing: -0.4 }}>
            {user.fullName}
          </h1>
          <div style={{ fontFamily: TB.sans, fontSize: 13, color: TB.tertiary }}>{user.email}</div>
        </div>
        <button onClick={() => goTo('edit-profile')} style={{
          background: TB.cream, border: `1px solid ${TB.divider}`,
          width: 40, height: 40, borderRadius: '50%', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <I.edit size={16} stroke={TB.tertiary} />
        </button>
      </div>

      {/* stats */}
      <div style={{ padding: '8px 22px', display: 'flex', gap: 8 }}>
        <Card padding={14} style={{ flex: 1, background: TB.card }}>
          <div style={{ fontFamily: TB.serif, fontSize: 28, fontWeight: 600, color: TB.ink, letterSpacing: -0.6, lineHeight: 1 }}>
            {user.booksRead}
          </div>
          <div style={{ fontFamily: TB.sans, fontSize: 11, color: TB.tertiary, marginTop: 4 }}>
            livros<br/>lidos
          </div>
        </Card>
        <Card padding={14} style={{ flex: 1, background: TB.card }}>
          <div style={{ fontFamily: TB.serif, fontSize: 28, fontWeight: 600, color: TB.ink, letterSpacing: -0.6, lineHeight: 1 }}>
            {user.activeClubs}
          </div>
          <div style={{ fontFamily: TB.sans, fontSize: 11, color: TB.tertiary, marginTop: 4 }}>
            clubes<br/>ativos
          </div>
        </Card>
        <Card padding={14} style={{ flex: 1, background: TB.secondary, cursor: 'pointer' }} onClick={() => goTo('frases')}>
          <div style={{ fontFamily: TB.serif, fontSize: 28, fontWeight: 600, color: TB.cream, letterSpacing: -0.6, lineHeight: 1 }}>
            42
          </div>
          <div style={{ fontFamily: TB.sans, fontSize: 11, color: TB.cream, opacity: 0.85, marginTop: 4 }}>
            frases<br/>guardadas
          </div>
        </Card>
      </div>

      {/* Clubs */}
      <SectionHeader title="Teus clubes" />
      <div style={{ padding: '0 22px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        {clubs.map(c => (
          <div key={c.id} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            background: TB.cream, border: `1px solid ${c.isActive ? TB.primary : TB.divider}`,
            borderRadius: 16, padding: '12px 14px',
          }}>
            <div style={{
              width: 40, height: 40, borderRadius: '50%', background: c.color,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FBF6EC', fontFamily: TB.serif, fontSize: 20, fontWeight: 600,
              flexShrink: 0,
            }}>{c.letter}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontFamily: TB.serif, fontSize: 15, fontWeight: 600, color: TB.ink, letterSpacing: -0.2 }}>
                  {c.name}
                </span>
                {c.isActive && <Pill variant="terra" size="sm">Atual</Pill>}
              </div>
              <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.tertiary, marginTop: 2 }}>
                {c.lastActivity}
              </div>
            </div>
            <I.chevR size={16} stroke={TB.tertiary} />
          </div>
        ))}
        <button onClick={() => goTo('join')} style={{
          background: 'transparent', border: `1.5px dashed ${TB.divider}`,
          borderRadius: 16, padding: '14px', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          fontFamily: TB.sans, fontSize: 14, fontWeight: 600, color: TB.tertiary,
          marginTop: 4,
        }}>
          <I.plus size={16} stroke={TB.tertiary} sw={2.2} />
          Entrar em outro clube
        </button>
      </div>

      {/* Logout */}
      <div style={{ padding: '24px 22px 20px' }}>
        <button style={{
          width: '100%', padding: '14px', borderRadius: 14,
          background: 'transparent', border: `1px solid ${TB.divider}`,
          color: TB.primary, cursor: 'pointer',
          fontFamily: TB.sans, fontSize: 14, fontWeight: 600,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        }}>
          <I.exit size={16} stroke={TB.primary} />
          Sair da conta
        </button>
      </div>
    </ScreenScroll>
  );
}

// ─────────────────────────────────────────────────────────────
// 13. Edit profile
// ─────────────────────────────────────────────────────────────
function EditProfileScreen({ onBack, onSave }) {
  const user = TB_DATA.user;
  const [name, setName] = React.useState(user.fullName);
  const [email, setEmail] = React.useState(user.email);
  const [picked, setPicked] = React.useState(user.name);

  const presets = ['Bia', 'Marina', 'Rafael', 'Júlia', 'Leo', 'Helena'];

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />
      <div style={{
        padding: '8px 18px 12px', display: 'flex', alignItems: 'center',
      }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={22} stroke={TB.ink} sw={2} />
        </button>
        <div style={{
          flex: 1, fontFamily: TB.serif, fontSize: 18, fontWeight: 600, color: TB.ink, textAlign: 'center', letterSpacing: -0.2,
        }}>Editar perfil</div>
        <div style={{ width: 38 }} />
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '6px 28px 20px' }}>
        {/* big avatar */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 18, position: 'relative' }}>
          <div style={{ position: 'relative' }}>
            <Avatar name={picked} size={100} />
            <div style={{
              position: 'absolute', bottom: 0, right: 0,
              width: 32, height: 32, borderRadius: '50%', background: TB.primary,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: `0 0 0 3px ${TB.paper}`,
            }}>
              <I.edit size={14} stroke={TB.cream} />
            </div>
          </div>
        </div>

        {/* preset avatars */}
        <div style={{ marginBottom: 22 }}>
          <div style={{
            fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary,
            textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 10,
          }}>Escolher um avatar</div>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'space-between' }}>
            {presets.map(p => (
              <button key={p} onClick={() => setPicked(p)} style={{ border: 'none', background: 'transparent', padding: 0, cursor: 'pointer' }}>
                <Avatar name={p} size={44} ring={picked === p ? TB.ink : undefined} />
              </button>
            ))}
          </div>
        </div>

        {/* fields */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 24 }}>
          <div>
            <div style={{ fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary, textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 6 }}>Nome</div>
            <input value={name} onChange={e => setName(e.target.value)} style={{
              width: '100%', padding: '14px 16px', borderRadius: 14,
              background: TB.cream, border: `1px solid ${TB.divider}`,
              fontFamily: TB.sans, fontSize: 15, color: TB.ink, outline: 'none',
              boxSizing: 'border-box',
            }} />
          </div>
          <div>
            <div style={{ fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary, textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 6 }}>Email</div>
            <input value={email} onChange={e => setEmail(e.target.value)} style={{
              width: '100%', padding: '14px 16px', borderRadius: 14,
              background: TB.cream, border: `1px solid ${TB.divider}`,
              fontFamily: TB.sans, fontSize: 15, color: TB.ink, outline: 'none',
              boxSizing: 'border-box',
            }} />
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <PillButton variant="outline" size="lg" full onClick={onBack}>Cancelar</PillButton>
          <PillButton variant="primary" size="lg" full onClick={onSave}>Salvar</PillButton>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 14. Notificações
// ─────────────────────────────────────────────────────────────
function NotificationsScreen({ onBack }) {
  const sections = TB_DATA.notifications;

  const Icon = ({ kind }) => {
    const map = {
      comment: { bg: TB.secondarySoft, color: TB.secondaryDark, icon: I.book },
      vote:    { bg: TB.primarySoft,   color: TB.primaryDark,   icon: I.vote },
      meet:    { bg: '#F1E3BE',        color: '#6E5316',        icon: I.calendar },
      done:    { bg: TB.ink,           color: TB.cream,         icon: I.checkCircle },
    };
    const m = map[kind] || map.comment;
    const Cmp = m.icon;
    return (
      <div style={{
        width: 40, height: 40, borderRadius: '50%', background: m.bg,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>
        <Cmp size={18} stroke={m.color} sw={1.8} />
      </div>
    );
  };

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />
      <div style={{
        padding: '8px 18px 8px', display: 'flex', alignItems: 'center',
      }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={22} stroke={TB.ink} sw={2} />
        </button>
        <div style={{ flex: 1, fontFamily: TB.serif, fontSize: 18, fontWeight: 600, color: TB.ink, textAlign: 'center' }}>Avisos</div>
        <button style={{
          background: 'transparent', border: 'none', padding: 8, marginRight: -8, cursor: 'pointer',
          fontFamily: TB.sans, fontSize: 13, fontWeight: 600, color: TB.primary,
        }}>Ler todas</button>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '4px 22px 20px' }}>
        {Object.entries(sections).map(([group, items]) => (
          <div key={group} style={{ marginBottom: 20 }}>
            <div style={{
              fontFamily: TB.sans, fontSize: 11, fontWeight: 700, color: TB.muted,
              textTransform: 'uppercase', letterSpacing: 1.2, padding: '8px 4px 10px',
            }}>{group}</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {items.map((n, i) => (
                <div key={i} style={{
                  display: 'flex', gap: 12, alignItems: 'flex-start', padding: '12px 12px', borderRadius: 14,
                  background: n.unread ? TB.cream : 'transparent',
                  position: 'relative',
                }}>
                  {n.who ? <Avatar name={n.who} size={40} /> : <Icon kind={n.kind} />}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: TB.sans, fontSize: 14, color: TB.ink, lineHeight: 1.35 }}>
                      {n.who && <b style={{ fontWeight: 600 }}>{n.who} </b>}
                      {n.text}
                    </div>
                    <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.muted, marginTop: 3 }}>
                      {n.context}
                    </div>
                  </div>
                  {n.unread && (
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: TB.primary, marginTop: 6, flexShrink: 0 }} />
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}

        <div style={{
          textAlign: 'center', padding: '20px 0',
          fontFamily: TB.serif, fontSize: 15, color: TB.muted, fontStyle: 'italic',
        }}>Tudo em dia.</div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 15. Bottom sheet — club switcher
// ─────────────────────────────────────────────────────────────
function ClubSheet({ open, onClose, onSelect, onJoin }) {
  return (
    <Sheet open={open} onClose={onClose}>
      <h3 style={{ fontFamily: TB.serif, fontSize: 22, fontWeight: 600, color: TB.ink, margin: '0 0 16px', letterSpacing: -0.4 }}>
        Trocar de clube
      </h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {TB_DATA.clubs.map(c => (
          <button key={c.id} onClick={() => { onSelect(c.id); onClose(); }} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            background: 'transparent',
            border: `1px solid ${c.isActive ? TB.ink : TB.divider}`,
            borderRadius: 14, padding: '12px 14px', cursor: 'pointer', textAlign: 'left',
          }}>
            <div style={{
              width: 36, height: 36, borderRadius: '50%', background: c.color,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#FBF6EC', fontFamily: TB.serif, fontSize: 16, fontWeight: 600,
            }}>{c.letter}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontFamily: TB.serif, fontSize: 15, fontWeight: 600, color: TB.ink }}>{c.name}</span>
                {c.isActive && <Pill variant="terra" size="sm">Atual</Pill>}
              </div>
              <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.tertiary }}>{c.lastActivity}</div>
            </div>
          </button>
        ))}
        <button onClick={onJoin} style={{
          background: 'transparent', border: `1.5px dashed ${TB.divider}`,
          borderRadius: 14, padding: '14px', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          fontFamily: TB.sans, fontSize: 14, fontWeight: 600, color: TB.tertiary,
          marginTop: 4,
        }}>
          <I.plus size={16} stroke={TB.tertiary} sw={2.2} />
          Entrar em outro clube
        </button>
      </div>
    </Sheet>
  );
}

// ─────────────────────────────────────────────────────────────
// Progress bottom sheet
// ─────────────────────────────────────────────────────────────
function ProgressSheet({ open, onClose, current, total, onSet, doneToast }) {
  return (
    <Sheet open={open} onClose={onClose}>
      <h3 style={{ fontFamily: TB.serif, fontSize: 22, fontWeight: 600, color: TB.ink, margin: '0 0 6px', letterSpacing: -0.4 }}>
        Onde tu tá?
      </h3>
      <p style={{ fontFamily: TB.sans, fontSize: 13, color: TB.tertiary, margin: '0 0 16px' }}>
        Quanto mais perto, mais comentário tu pode ver.
      </p>
      <div style={{
        display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 6,
        maxHeight: 320, overflowY: 'auto',
      }}>
        {Array.from({ length: total }, (_, i) => {
          const n = i + 1;
          const active = n === current;
          return (
            <button key={n} onClick={() => { onSet(n); onClose(); doneToast(`Cap. ${n} marcado.`); }} style={{
              padding: '14px 0', borderRadius: 12,
              background: active ? TB.primary : TB.cream,
              color: active ? TB.cream : TB.ink,
              border: `1px solid ${active ? TB.primary : TB.divider}`,
              fontFamily: TB.serif, fontSize: 16, fontWeight: 600, cursor: 'pointer',
            }}>{n}</button>
          );
        })}
      </div>
      <button onClick={() => { onSet(total); onClose(); doneToast('Acabou. Próximo?'); }} style={{
        marginTop: 16, width: '100%', padding: '14px', borderRadius: 14,
        background: TB.secondarySoft, border: 'none',
        color: TB.secondaryDark, cursor: 'pointer',
        fontFamily: TB.sans, fontSize: 14, fontWeight: 600,
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
      }}>
        <I.check size={16} stroke={TB.secondaryDark} sw={2.4} />
        Terminei o livro
      </button>
    </Sheet>
  );
}

// ─────────────────────────────────────────────────────────────
// Generic Sheet
// ─────────────────────────────────────────────────────────────
function Sheet({ open, onClose, children }) {
  return (
    <>
      {/* backdrop */}
      <div onClick={onClose} style={{
        position: 'absolute', inset: 0, background: 'rgba(20,18,15,0.5)',
        zIndex: 80, opacity: open ? 1 : 0, pointerEvents: open ? 'auto' : 'none',
        transition: 'opacity .25s',
      }} />
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 81,
        background: TB.paper, borderRadius: '24px 24px 0 0',
        padding: '12px 22px 28px',
        transform: open ? 'translateY(0)' : 'translateY(100%)',
        transition: 'transform .3s cubic-bezier(.2,.7,.3,1)',
        boxShadow: '0 -8px 32px rgba(20,18,15,0.18)',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 14 }}>
          <div style={{ width: 38, height: 4, background: TB.dividerSoft, borderRadius: 999 }} />
        </div>
        {children}
      </div>
    </>
  );
}

Object.assign(window, { ProfileScreen, EditProfileScreen, NotificationsScreen, ClubSheet, ProgressSheet, Sheet });
