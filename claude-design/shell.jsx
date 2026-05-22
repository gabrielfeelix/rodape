// shell.jsx — Tramabook phone shell: status bar, header, bottom nav

// ─────────────────────────────────────────────────────────────
// Status bar (warm-paper styled, not iOS default)
// ─────────────────────────────────────────────────────────────
function StatusBar({ time = '9:41', color = TB.ink }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 28px 8px', position: 'relative', zIndex: 20, color,
      fontFamily: TB.sans,
    }}>
      <div style={{ fontSize: 15, fontWeight: 700, letterSpacing: -0.2 }}>{time}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, opacity: 0.95 }}>
        <svg width="17" height="11" viewBox="0 0 17 11">
          <rect x="0" y="6" width="2.5" height="4" rx="0.6" fill={color}/>
          <rect x="4" y="4" width="2.5" height="6" rx="0.6" fill={color}/>
          <rect x="8" y="2" width="2.5" height="8" rx="0.6" fill={color}/>
          <rect x="12" y="0" width="2.5" height="10" rx="0.6" fill={color}/>
        </svg>
        <svg width="15" height="11" viewBox="0 0 15 11">
          <path d="M7.5 3C9.5 3 11.3 3.8 12.7 5L13.6 4.1C11.9 2.5 9.8 1.5 7.5 1.5C5.2 1.5 3.1 2.5 1.4 4.1L2.3 5C3.7 3.8 5.5 3 7.5 3Z" fill={color}/>
          <path d="M7.5 6C8.7 6 9.8 6.4 10.6 7.2L11.5 6.3C10.4 5.3 9 4.7 7.5 4.7C6 4.7 4.6 5.3 3.5 6.3L4.4 7.2C5.2 6.4 6.3 6 7.5 6Z" fill={color}/>
          <circle cx="7.5" cy="9.3" r="1.3" fill={color}/>
        </svg>
        <svg width="24" height="11" viewBox="0 0 24 11">
          <rect x="0.5" y="0.5" width="20" height="10" rx="3" stroke={color} strokeOpacity="0.4" fill="none"/>
          <rect x="2" y="2" width="17" height="7" rx="1.6" fill={color}/>
          <path d="M22 4v3c.7-.2 1.2-1 1.2-1.5S22.7 4.2 22 4z" fill={color} fillOpacity="0.5"/>
        </svg>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Global header — avatar · club name + chevron · bell
// ─────────────────────────────────────────────────────────────
function GlobalHeader({ club, user, onAvatar, onClubTap, onBell, unread = 3 }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '6px 18px 12px', position: 'relative', zIndex: 5,
    }}>
      <button onClick={onAvatar} style={{ border: 'none', background: 'transparent', padding: 0, cursor: 'pointer' }}>
        <Avatar name={user.name} size={40} />
      </button>

      <button onClick={onClubTap} style={{
        flex: 1, minWidth: 0,
        background: TB.card, border: `1px solid ${TB.divider}`,
        padding: '8px 14px 8px 8px', cursor: 'pointer',
        display: 'flex', alignItems: 'center', gap: 10, borderRadius: 999,
      }}>
        <div style={{
          width: 26, height: 26, borderRadius: '50%', background: club.color,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: TB.cream, fontFamily: TB.serif, fontSize: 13, fontWeight: 600,
          flexShrink: 0,
        }}>{club.letter}</div>
        <div style={{ flex: 1, minWidth: 0, textAlign: 'left' }}>
          <div style={{
            fontFamily: TB.sans, fontSize: 10, fontWeight: 600, color: TB.muted,
            textTransform: 'uppercase', letterSpacing: 0.6, lineHeight: 1,
          }}>Clube</div>
          <div style={{
            fontFamily: TB.serif, fontSize: 14, fontWeight: 600, color: TB.ink,
            letterSpacing: -0.2, lineHeight: 1.2, marginTop: 1,
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}>{club.name}</div>
        </div>
        <I.chevD size={14} stroke={TB.tertiary} sw={2} />
      </button>

      <button onClick={onBell} style={{
        position: 'relative', background: TB.card, border: `1px solid ${TB.divider}`,
        width: 40, height: 40, borderRadius: '50%', cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <I.bell size={18} stroke={TB.ink} sw={1.8} />
        {unread > 0 && (
          <span style={{
            position: 'absolute', top: -1, right: -1, minWidth: 16, height: 16, borderRadius: 999,
            padding: '0 4px', background: TB.primary, color: TB.cream,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: TB.sans, fontSize: 10, fontWeight: 700,
            border: `2px solid ${TB.paper}`, boxSizing: 'content-box',
          }}>{unread}</span>
        )}
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Bottom navigation
// ─────────────────────────────────────────────────────────────
function BottomNav({ tab, setTab }) {
  const items = [
    { id: 'home',    label: 'Início',   Icon: I.home },
    { id: 'book',    label: 'Livro',    Icon: I.book },
    { id: 'next',    label: 'Próximo',  Icon: I.calendar },
    { id: 'profile', label: 'Perfil',   Icon: I.user },
  ];
  return (
    <div style={{
      padding: '8px 18px 26px',
      background: 'linear-gradient(180deg, rgba(247,245,238,0) 0%, ' + TB.paper + ' 50%)',
      position: 'relative',
    }}>
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        background: TB.secondaryDeep, borderRadius: 999, padding: 6,
        boxShadow: '0 8px 24px rgba(41,56,32,0.25), 0 2px 6px rgba(41,56,32,0.15)',
      }}>
        {items.map(({ id, label, Icon }) => {
          const active = tab === id;
          return (
            <button key={id} onClick={() => setTab(id)} style={{
              background: active ? TB.primary : 'transparent',
              border: 'none', cursor: 'pointer',
              display: 'flex', alignItems: 'center', gap: 6,
              padding: active ? '10px 16px' : '10px 12px',
              borderRadius: 999,
              color: active ? TB.cream : 'rgba(251,250,244,0.7)',
              transition: 'all .2s ease',
            }}>
              <Icon size={20} stroke={active ? TB.cream : 'rgba(251,250,244,0.7)'} sw={active ? 2 : 1.8} />
              {active && (
                <span style={{
                  fontFamily: TB.sans, fontSize: 13, fontWeight: 600, color: TB.cream,
                  letterSpacing: -0.1, whiteSpace: 'nowrap',
                }}>{label}</span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Phone — outer device frame with home indicator
// ─────────────────────────────────────────────────────────────
function Phone({ children, width = 390, height = 844, bg = TB.paper, style }) {
  return (
    <div style={{
      width, height, position: 'relative',
      background: bg, overflow: 'hidden',
      fontFamily: TB.sans, color: TB.ink,
      WebkitFontSmoothing: 'antialiased',
      ...style,
    }}>
      {/* dynamic island */}
      <div style={{
        position: 'absolute', top: 10, left: '50%', transform: 'translateX(-50%)',
        width: 110, height: 32, borderRadius: 22, background: '#0c0c0a', zIndex: 50,
      }} />
      {children}
      {/* home indicator */}
      <div style={{
        position: 'absolute', bottom: 8, left: 0, right: 0, zIndex: 60,
        display: 'flex', justifyContent: 'center', pointerEvents: 'none',
      }}>
        <div style={{ width: 130, height: 5, borderRadius: 999, background: 'rgba(27,28,26,0.35)' }} />
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Common screen container (with scrollable content area)
// ─────────────────────────────────────────────────────────────
function ScreenScroll({ children, padding = '0 0 24px', style }) {
  return (
    <div style={{
      flex: 1, overflow: 'auto', padding,
      WebkitOverflowScrolling: 'touch',
      ...style,
    }}>{children}</div>
  );
}

// ─────────────────────────────────────────────────────────────
// Section header inside a screen
// ─────────────────────────────────────────────────────────────
function SectionHeader({ title, action, style }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
      padding: '18px 22px 10px', ...style,
    }}>
      <h2 style={{
        fontFamily: TB.serif, fontSize: 20, fontWeight: 600, letterSpacing: -0.4,
        color: TB.ink, margin: 0,
      }}>{title}</h2>
      {action}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Card surface
// ─────────────────────────────────────────────────────────────
function Card({ children, style, onClick, padding = 16 }) {
  return (
    <div onClick={onClick} style={{
      background: TB.card, borderRadius: 20, padding,
      border: `0.5px solid ${TB.divider}`,
      boxShadow: '0 1px 2px rgba(45,30,15,0.04), 0 4px 14px rgba(45,30,15,0.04)',
      cursor: onClick ? 'pointer' : undefined,
      ...style,
    }}>{children}</div>
  );
}

// ─────────────────────────────────────────────────────────────
// Toast
// ─────────────────────────────────────────────────────────────
function Toast({ text, visible }) {
  return (
    <div style={{
      position: 'absolute', bottom: 100, left: 16, right: 16, zIndex: 100,
      background: TB.ink, color: TB.cream,
      padding: '14px 18px', borderRadius: 14,
      fontFamily: TB.sans, fontSize: 14, fontWeight: 500,
      boxShadow: '0 8px 24px rgba(0,0,0,0.18)',
      transform: visible ? 'translateY(0)' : 'translateY(20px)',
      opacity: visible ? 1 : 0,
      pointerEvents: visible ? 'auto' : 'none',
      transition: 'opacity .25s ease, transform .25s ease',
      textAlign: 'center',
    }}>{text}</div>
  );
}

Object.assign(window, { StatusBar, GlobalHeader, BottomNav, Phone, ScreenScroll, SectionHeader, Card, Toast });
