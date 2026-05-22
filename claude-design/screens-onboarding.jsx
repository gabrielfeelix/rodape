// screens-onboarding.jsx — Welcome, Login, Create club, Join club

// ─────────────────────────────────────────────────────────────
// 1. Welcome (Boas-vindas)
// ─────────────────────────────────────────────────────────────
function WelcomeScreen({ onCreate, onJoin, onLogin }) {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: TB.paper,
      display: 'flex', flexDirection: 'column',
      position: 'relative', overflow: 'hidden',
    }}>
      <StatusBar />

      {/* logo wordmark — top left */}
      <div style={{ padding: '18px 28px 0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{
          fontFamily: TB.serif, fontSize: 20, fontWeight: 600,
          color: TB.ink, letterSpacing: -0.4,
          display: 'flex', alignItems: 'center', gap: 8,
        }}>
          <TramabookLogo size={28} color={TB.primary} />
          tramabook
        </div>
        <button onClick={onLogin} style={{
          background: 'transparent', border: 'none', cursor: 'pointer',
          fontFamily: TB.sans, fontSize: 13, fontWeight: 600, color: TB.tertiary,
          padding: '8px 4px',
        }}>Entrar</button>
      </div>

      {/* Hero text */}
      <div style={{ padding: '40px 28px 28px', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Pill variant="oliveDeep" size="lg" style={{ alignSelf: 'flex-start', marginBottom: 18 }}>
          Clubes de leitura
        </Pill>

        <h1 style={{
          fontFamily: TB.serif, fontSize: 54, fontWeight: 600, letterSpacing: -1.8,
          color: TB.ink, lineHeight: 0.98, margin: '0 0 18px', textWrap: 'balance',
        }}>
          Leituras<br/>
          <span style={{ fontStyle: 'italic', color: TB.secondary }}>juntas</span>
          <span style={{ color: TB.primary }}>.</span>
        </h1>

        <p style={{
          fontFamily: TB.sans, fontSize: 16, lineHeight: 1.4, color: TB.tertiary,
          margin: 0, maxWidth: 290, textWrap: 'pretty',
        }}>
          Um clube. Um livro. Conversa que <br/>não dá spoiler.
        </p>
      </div>

      {/* Curved olive section with illustration + CTAs */}
      <div style={{
        background: TB.secondaryDeep,
        borderRadius: '40px 40px 0 0',
        padding: '0 22px 40px',
        position: 'relative',
      }}>
        {/* books illustration scene — sits across the curve */}
        <div style={{
          position: 'absolute', top: -68, right: 24,
          display: 'flex', alignItems: 'flex-end', gap: 5,
        }}>
          <BookSpine h={80} color="#D9C9B0" />
          <BookSpine h={110} color={TB.primary} />
          <BookSpine h={92} color={TB.cream} stroke />
          <BookSpine h={72} color={TB.secondary} />
        </div>

        {/* shelf line */}
        <div style={{
          position: 'absolute', top: 12, left: 22, right: 22,
          height: 1, background: 'rgba(255,255,255,0.1)',
        }} />

        <div style={{ paddingTop: 38, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <PillButton variant="terra" size="lg" full onClick={onCreate}
            iconRight={<I.arrow size={18} stroke={TB.cream} sw={2} />}>
            Criar um clube
          </PillButton>
          <button onClick={onJoin} style={{
            background: 'transparent', color: TB.cream, cursor: 'pointer',
            border: `1px solid rgba(251,250,244,0.25)`, padding: '14px 24px',
            borderRadius: 999, fontFamily: TB.sans, fontSize: 15, fontWeight: 600,
            height: 54,
          }}>Entrar num clube</button>
        </div>
      </div>
    </div>
  );
}

// Helper book spine for illustrations
function BookSpine({ h = 90, color = '#D9C9B0', stroke }) {
  return (
    <div style={{
      width: 24, height: h,
      background: color,
      borderRadius: '3px 3px 4px 4px',
      boxShadow: 'inset -3px 0 0 rgba(0,0,0,0.18), inset 1px 1px 0 rgba(255,255,255,0.15)',
      border: stroke ? `1px solid rgba(0,0,0,0.06)` : 'none',
      position: 'relative',
    }}>
      <div style={{
        position: 'absolute', left: 4, right: 4, top: '40%',
        height: 1, background: 'rgba(0,0,0,0.15)',
      }} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 2. Login / Signup
// ─────────────────────────────────────────────────────────────
function LoginScreen({ onBack, onSubmit, defaultMode = 'login' }) {
  const [mode, setMode] = React.useState(defaultMode);
  const [email, setEmail] = React.useState('');
  const [pwd, setPwd] = React.useState('');
  const [name, setName] = React.useState('');

  const Input = ({ label, value, onChange, type = 'text', placeholder }) => (
    <label style={{ display: 'block' }}>
      <div style={{
        fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary,
        textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 6,
      }}>{label}</div>
      <input value={value} onChange={e => onChange(e.target.value)} type={type} placeholder={placeholder} style={{
        width: '100%', padding: '14px 16px', borderRadius: 14,
        background: TB.cream, border: `1px solid ${TB.divider}`,
        fontFamily: TB.sans, fontSize: 15, color: TB.ink, outline: 'none',
        boxSizing: 'border-box',
      }} onFocus={e => e.target.style.borderColor = TB.primary}
         onBlur={e => e.target.style.borderColor = TB.divider} />
    </label>
  );

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />
      <div style={{ padding: '12px 22px 4px' }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={24} stroke={TB.ink} sw={2} />
        </button>
      </div>

      <div style={{ padding: '8px 28px 24px', flex: 1, overflow: 'auto' }}>
        <h1 style={{ fontFamily: TB.serif, fontSize: 36, fontWeight: 600, letterSpacing: -0.8, color: TB.ink, margin: '4px 0 6px' }}>
          {mode === 'login' ? 'Oi de novo.' : 'Vamos criar.'}
        </h1>
        <p style={{ fontFamily: TB.sans, fontSize: 14, color: TB.tertiary, margin: '0 0 24px' }}>
          {mode === 'login' ? 'Tua leitura tá te esperando.' : 'Em poucos segundos tu tá lendo junto.'}
        </p>

        {/* segmented control */}
        <div style={{
          display: 'flex', background: TB.paperDeep, borderRadius: 999, padding: 4, marginBottom: 22,
        }}>
          {[{ id: 'login', label: 'Entrar' }, { id: 'signup', label: 'Criar conta' }].map(t => {
            const active = mode === t.id;
            return (
              <button key={t.id} onClick={() => setMode(t.id)} style={{
                flex: 1, padding: '10px 0', borderRadius: 999, border: 'none',
                background: active ? TB.cream : 'transparent',
                color: active ? TB.ink : TB.tertiary,
                fontFamily: TB.sans, fontSize: 14, fontWeight: 600, cursor: 'pointer',
                boxShadow: active ? '0 1px 3px rgba(0,0,0,0.06)' : 'none',
                transition: 'background .2s',
              }}>{t.label}</button>
            );
          })}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {mode === 'signup' && <Input label="Nome" value={name} onChange={setName} placeholder="Como vão te chamar?" />}
          <Input label="Email" value={email} onChange={setEmail} type="email" placeholder="tu@email.com" />
          <Input label="Senha" value={pwd} onChange={setPwd} type="password" placeholder="Pelo menos 8 caracteres" />
        </div>

        {mode === 'login' && (
          <button style={{
            background: 'transparent', border: 'none', color: TB.primary, fontFamily: TB.sans,
            fontSize: 13, fontWeight: 600, padding: '14px 0', cursor: 'pointer',
          }}>Esqueci minha senha</button>
        )}

        <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <PillButton variant="primary" size="lg" full onClick={onSubmit}>
            {mode === 'login' ? 'Entrar' : 'Criar conta'}
          </PillButton>

          {/* divider */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '6px 0' }}>
            <div style={{ flex: 1, height: 1, background: TB.divider }} />
            <span style={{ fontFamily: TB.sans, fontSize: 12, color: TB.muted, fontWeight: 500 }}>ou</span>
            <div style={{ flex: 1, height: 1, background: TB.divider }} />
          </div>

          <PillButton variant="outline" size="lg" full onClick={onSubmit} iconLeft={<I.google size={18} />}>
            Continuar com Google
          </PillButton>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 3. Create club
// ─────────────────────────────────────────────────────────────
function CreateClubScreen({ onBack, onCreate }) {
  const [name, setName] = React.useState('');
  const [desc, setDesc] = React.useState('');
  const [color, setColor] = React.useState(0);
  const [privacy, setPrivacy] = React.useState('invite');

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />
      <div style={{ padding: '12px 22px 4px' }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={24} stroke={TB.ink} sw={2} />
        </button>
      </div>

      <div style={{ padding: '4px 28px', flex: 1, overflow: 'auto' }}>
        <h1 style={{ fontFamily: TB.serif, fontSize: 32, fontWeight: 600, letterSpacing: -0.8, color: TB.ink, margin: '4px 0 4px' }}>
          Um clube novo.
        </h1>
        <p style={{ fontFamily: TB.sans, fontSize: 14, color: TB.tertiary, margin: '0 0 26px' }}>
          Depois tu chama a galera.
        </p>

        {/* Name */}
        <div style={{ marginBottom: 18 }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between',
            fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary,
            textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 6,
          }}>
            <span>Nome do clube</span>
            <span style={{ color: name.length > 36 ? TB.primary : TB.muted, fontWeight: 500 }}>{name.length}/40</span>
          </div>
          <input value={name} maxLength={40} onChange={e => setName(e.target.value)}
            placeholder="Leituras de domingo"
            style={{
              width: '100%', padding: '14px 16px', borderRadius: 14,
              background: TB.cream, border: `1px solid ${TB.divider}`,
              fontFamily: TB.serif, fontSize: 17, color: TB.ink, outline: 'none',
              boxSizing: 'border-box',
            }} />
        </div>

        {/* Description */}
        <div style={{ marginBottom: 22 }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between',
            fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary,
            textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 6,
          }}>
            <span>Descrição <span style={{ textTransform: 'none', color: TB.muted, fontWeight: 500 }}>(opcional)</span></span>
            <span style={{ color: TB.muted, fontWeight: 500 }}>{desc.length}/140</span>
          </div>
          <textarea value={desc} maxLength={140} onChange={e => setDesc(e.target.value)}
            placeholder="Algumas linhas sobre o clube."
            rows={3}
            style={{
              width: '100%', padding: '14px 16px', borderRadius: 14,
              background: TB.cream, border: `1px solid ${TB.divider}`,
              fontFamily: TB.sans, fontSize: 14, color: TB.ink, outline: 'none',
              boxSizing: 'border-box', resize: 'none',
            }} />
        </div>

        {/* Color */}
        <div style={{ marginBottom: 22 }}>
          <div style={{
            fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary,
            textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 10,
          }}>Cor do clube</div>
          <div style={{ display: 'flex', gap: 12 }}>
            {CLUB_COLORS.map((c, i) => (
              <button key={c.id} onClick={() => setColor(i)} style={{
                width: 44, height: 44, borderRadius: '50%', border: 'none', cursor: 'pointer',
                background: c.bg, position: 'relative',
                boxShadow: color === i ? `0 0 0 2px ${TB.paper}, 0 0 0 4px ${TB.ink}` : 'none',
                transition: 'box-shadow .15s',
              }}>
                {color === i && (
                  <I.check size={20} stroke={c.ink} sw={2.4} style={{ position: 'absolute', top: 12, left: 12 }} />
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Privacy */}
        <div style={{ marginBottom: 18 }}>
          <div style={{
            fontFamily: TB.sans, fontSize: 12, fontWeight: 600, color: TB.tertiary,
            textTransform: 'uppercase', letterSpacing: 0.4, marginBottom: 10,
          }}>Privacidade</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {[
              { id: 'invite', label: 'Só convidados', sub: 'Entram com código ou link' },
              { id: 'open',   label: 'Aberto a quem tem link', sub: 'Sem aprovação' },
            ].map(opt => {
              const active = privacy === opt.id;
              return (
                <button key={opt.id} onClick={() => setPrivacy(opt.id)} style={{
                  display: 'flex', alignItems: 'center', gap: 12,
                  background: active ? TB.cream : 'transparent',
                  border: `1.5px solid ${active ? TB.ink : TB.divider}`,
                  borderRadius: 14, padding: '12px 14px', cursor: 'pointer',
                  textAlign: 'left',
                }}>
                  <div style={{
                    width: 20, height: 20, borderRadius: '50%',
                    border: `1.5px solid ${active ? TB.ink : TB.divider}`,
                    background: active ? TB.ink : 'transparent',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    {active && <div style={{ width: 8, height: 8, borderRadius: '50%', background: TB.cream }} />}
                  </div>
                  <div>
                    <div style={{ fontFamily: TB.sans, fontSize: 15, fontWeight: 600, color: TB.ink, marginBottom: 2 }}>{opt.label}</div>
                    <div style={{ fontFamily: TB.sans, fontSize: 12, color: TB.tertiary }}>{opt.sub}</div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        <div style={{ height: 18 }} />
        <PillButton variant="primary" size="lg" full disabled={!name} onClick={onCreate}>Criar clube</PillButton>
        <div style={{ height: 16 }} />
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// 4. Join club
// ─────────────────────────────────────────────────────────────
function JoinClubScreen({ onBack, onJoin }) {
  const [mode, setMode] = React.useState('code');
  const [code, setCode] = React.useState(['L', 'I', 'V', 'R', '', '']);
  const [link, setLink] = React.useState('');

  const setDigit = (i, v) => {
    const next = code.slice();
    next[i] = (v || '').slice(-1).toUpperCase();
    setCode(next);
  };
  const valid = mode === 'code' ? code.every(c => c) : link.length > 6;

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', background: TB.paper }}>
      <StatusBar />
      <div style={{ padding: '12px 22px 4px' }}>
        <button onClick={onBack} style={{ background: 'transparent', border: 'none', padding: 8, marginLeft: -8, cursor: 'pointer' }}>
          <I.chevL size={24} stroke={TB.ink} sw={2} />
        </button>
      </div>

      <div style={{ padding: '4px 28px', flex: 1, overflow: 'auto' }}>
        <h1 style={{ fontFamily: TB.serif, fontSize: 32, fontWeight: 600, letterSpacing: -0.8, color: TB.ink, margin: '4px 0 4px' }}>
          Que clube tu vai entrar?
        </h1>
        <p style={{ fontFamily: TB.sans, fontSize: 14, color: TB.tertiary, margin: '0 0 26px' }}>
          Cola o código que mandaram, ou o link.
        </p>

        <div style={{ display: 'flex', background: TB.paperDeep, borderRadius: 999, padding: 4, marginBottom: 24 }}>
          {[{ id: 'code', label: 'Com código' }, { id: 'link', label: 'Com link' }].map(t => {
            const active = mode === t.id;
            return (
              <button key={t.id} onClick={() => setMode(t.id)} style={{
                flex: 1, padding: '10px 0', borderRadius: 999, border: 'none',
                background: active ? TB.cream : 'transparent',
                color: active ? TB.ink : TB.tertiary,
                fontFamily: TB.sans, fontSize: 14, fontWeight: 600, cursor: 'pointer',
              }}>{t.label}</button>
            );
          })}
        </div>

        {mode === 'code' ? (
          <>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'space-between', margin: '12px 0 18px' }}>
              {code.map((c, i) => (
                <input key={i} value={c} onChange={e => setDigit(i, e.target.value)} maxLength={1}
                  style={{
                    width: 46, height: 56, borderRadius: 12,
                    border: `1.5px solid ${c ? TB.ink : TB.divider}`,
                    background: TB.cream, textAlign: 'center',
                    fontFamily: TB.serif, fontSize: 22, fontWeight: 600, color: TB.ink,
                    outline: 'none', boxSizing: 'border-box',
                  }} />
              ))}
            </div>
            <p style={{ fontFamily: TB.sans, fontSize: 13, color: TB.muted, margin: 0 }}>
              6 caracteres, sem distinção de maiúsculas.
            </p>
          </>
        ) : (
          <>
            <input value={link} onChange={e => setLink(e.target.value)}
              placeholder="tramabook.app/c/..."
              style={{
                width: '100%', padding: '16px', borderRadius: 14,
                background: TB.cream, border: `1px solid ${TB.divider}`,
                fontFamily: TB.sans, fontSize: 15, color: TB.ink, outline: 'none',
                boxSizing: 'border-box',
              }} />
            <p style={{ fontFamily: TB.sans, fontSize: 13, color: TB.muted, margin: '12px 0 0' }}>
              Cola a URL que mandaram pelo grupo.
            </p>
          </>
        )}

        <div style={{ height: 32 }} />
        <PillButton variant="primary" size="lg" full disabled={!valid} onClick={onJoin}>Confirmar</PillButton>
      </div>
    </div>
  );
}

Object.assign(window, { WelcomeScreen, LoginScreen, CreateClubScreen, JoinClubScreen });
