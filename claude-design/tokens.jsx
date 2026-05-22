// tokens.jsx — Tramabook design system

const TB = {
  // Colors — fresh, light, "cozy reading nook"
  primary:   '#B85838',  // terracotta — sparing accent
  primaryDark: '#8E3F25',
  primarySoft: '#FBE5DA',
  secondary: '#5C7349',  // sage / olive — hero color
  secondaryDark: '#3E5230',
  secondaryDeep: '#293820',
  secondarySoft: '#E5EBDA',
  secondaryMid:  '#92A57F',
  tertiary:  '#5B5B53',  // neutral ink-gray
  tertiarySoft: '#D9D9CF',
  ink:       '#1B1F1A',  // primary text (slight green tint)
  inkSoft:   '#383C36',
  muted:     '#8A8A80',  // muted text

  // Surfaces — bright, fresh, almost white
  paper:     '#F7F5EE',  // app bg
  paperDeep: '#F0EEE5',  // alt bg
  card:      '#FFFFFF',  // card surface
  cardSoft:  '#F9F8F2',  // alt card
  cream:     '#FBFAF4',  // ivory
  surface:   '#FFFFFF',
  divider:   '#E9E7DD',
  dividerSoft: '#F1EFE6',

  // Type
  serif: '"Literata", "Source Serif Pro", Georgia, serif',
  sans:  '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif',

  // Radii
  r4: 4, r8: 8, r12: 12, r16: 16, r20: 20, r24: 24, r999: 999,
};

// Club color presets (the 5 in briefing)
const CLUB_COLORS = [
  { id: 'olive',    bg: '#4F653F', soft: '#E1E7D7', ink: '#fff' },
  { id: 'terracotta', bg: '#934528', soft: '#F3DCD0', ink: '#fff' },
  { id: 'plum',     bg: '#6E3A52', soft: '#EBDCE4', ink: '#fff' },
  { id: 'mustard',  bg: '#A6802B', soft: '#F1E3BE', ink: '#fff' },
  { id: 'ink',      bg: '#2E3A47', soft: '#D7DCE2', ink: '#fff' },
];

// ─────────────────────────────────────────────────────────────
// Book cover placeholder — colored block w/ serif title
// ─────────────────────────────────────────────────────────────
function Cover({ title = 'Livro', author = 'Autor', palette, w = 92, h = 138, style }) {
  // Deterministic palette from title hash so the same book gets the same look
  const palettes = [
    { bg: '#3E5230', fg: '#F0EEE5', accent: '#D8C9B0' },   // deep olive
    { bg: '#B85838', fg: '#FBFAF4', accent: '#F5D8C5' },   // terracotta
    { bg: '#1F2A1A', fg: '#E5EBDA', accent: '#92A57F' },   // forest
    { bg: '#7A4F2B', fg: '#FBFAF4', accent: '#E5EBDA' },   // walnut
    { bg: '#D8C9B0', fg: '#3E5230', accent: '#B85838' },   // sand
    { bg: '#5C7349', fg: '#FBFAF4', accent: '#F5D8C5' },   // sage
    { bg: '#E5EBDA', fg: '#3E5230', accent: '#B85838' },   // mint
    { bg: '#FBE5DA', fg: '#8E3F25', accent: '#5C7349' },   // peach
  ];
  const hash = [...title].reduce((s, c) => s + c.charCodeAt(0), 0);
  const p = palette || palettes[hash % palettes.length];

  // tiny ornament patterns chosen by hash
  const ornament = hash % 4;
  const fontSize = Math.max(9, Math.min(15, w / Math.max(8, title.length) * 1.4));
  return (
    <div style={{
      width: w, height: h, background: p.bg, color: p.fg,
      borderRadius: 3,
      boxShadow: '0 1px 2px rgba(0,0,0,0.18), 0 6px 14px rgba(0,0,0,0.10), inset 1px 0 0 rgba(255,255,255,0.06), inset -2px 0 4px rgba(0,0,0,0.18)',
      position: 'relative', overflow: 'hidden',
      display: 'flex', flexDirection: 'column',
      padding: w * 0.09,
      fontFamily: TB.serif,
      ...style,
    }}>
      {/* spine highlight */}
      <div style={{
        position: 'absolute', left: 0, top: 0, bottom: 0, width: 3,
        background: 'linear-gradient(90deg, rgba(0,0,0,0.25), rgba(0,0,0,0))',
      }} />
      {/* ornament */}
      {ornament === 0 && (
        <div style={{
          position: 'absolute', top: '38%', left: '50%', transform: 'translate(-50%, -50%)',
          width: w * 0.45, height: w * 0.45, borderRadius: '50%',
          border: `1px solid ${p.accent}`, opacity: 0.5,
        }} />
      )}
      {ornament === 1 && (
        <div style={{
          position: 'absolute', top: '46%', left: '12%', right: '12%', height: 1,
          background: p.accent, opacity: 0.6,
        }} />
      )}
      {ornament === 2 && (
        <div style={{
          position: 'absolute', bottom: '12%', left: '12%', right: '12%', height: w * 0.3,
          background: `repeating-linear-gradient(45deg, ${p.accent}22 0 4px, transparent 4px 8px)`,
          opacity: 0.7,
        }} />
      )}
      {ornament === 3 && (
        <>
          <div style={{ position: 'absolute', left: '12%', right: '12%', top: '38%', height: 1, background: p.fg, opacity: 0.3 }} />
          <div style={{ position: 'absolute', left: '12%', right: '12%', top: '62%', height: 1, background: p.fg, opacity: 0.3 }} />
        </>
      )}
      <div style={{
        fontSize: fontSize, lineHeight: 1.05, fontWeight: 600, letterSpacing: -0.2,
        position: 'relative', zIndex: 1, textWrap: 'pretty',
      }}>{title}</div>
      <div style={{ flex: 1 }} />
      <div style={{
        fontSize: Math.max(7, fontSize * 0.55), opacity: 0.75,
        fontFamily: TB.sans, fontWeight: 500, letterSpacing: 0.3, textTransform: 'uppercase',
        position: 'relative', zIndex: 1,
      }}>{author}</div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Avatar — initials over colored bg
// ─────────────────────────────────────────────────────────────
function Avatar({ name = '?', size = 32, color, style, ring }) {
  const palettes = ['#5C7349','#B85838','#3E5230','#7A4F2B','#1F2A1A','#92A57F','#8E3F25','#5B5B53'];
  const hash = [...name].reduce((s, c) => s + c.charCodeAt(0), 0);
  const bg = color || palettes[hash % palettes.length];
  const initials = name.split(' ').filter(Boolean).slice(0, 2).map(s => s[0]).join('').toUpperCase();
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      background: bg, color: '#FBF6EC',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: TB.sans, fontSize: size * 0.4, fontWeight: 600,
      flexShrink: 0,
      boxShadow: ring ? `0 0 0 2px ${ring}` : 'none',
      letterSpacing: -0.2,
      ...style,
    }}>{initials}</div>
  );
}

// ─────────────────────────────────────────────────────────────
// Pill button — primary, outline, ghost
// ─────────────────────────────────────────────────────────────
function PillButton({ children, variant = 'primary', onClick, size = 'md', full, style, disabled, iconLeft, iconRight }) {
  const sizes = {
    sm: { p: '8px 14px', fs: 13, h: 32 },
    md: { p: '13px 22px', fs: 15, h: 46 },
    lg: { p: '16px 28px', fs: 16, h: 54 },
  };
  const s = sizes[size];
  const variants = {
    primary: {
      background: disabled ? '#D8C9B8' : TB.secondary, color: TB.cream,
      border: 'none',
    },
    terra: {
      background: disabled ? '#D8C9B8' : TB.primary, color: TB.cream,
      border: 'none',
    },
    outline: {
      background: TB.card, color: TB.ink,
      border: `1px solid ${TB.divider}`,
    },
    soft: {
      background: TB.primarySoft, color: TB.primaryDark,
      border: 'none',
    },
    ghost: {
      background: 'transparent', color: TB.primary, border: 'none',
    },
    olive: {
      background: TB.secondary, color: TB.cream, border: 'none',
    },
    oliveSoft: {
      background: TB.secondarySoft, color: TB.secondaryDark, border: 'none',
    },
    dark: {
      background: TB.ink, color: TB.cream, border: 'none',
    },
    light: {
      background: TB.card, color: TB.ink, border: 'none',
      boxShadow: '0 1px 2px rgba(0,0,0,0.06), 0 2px 8px rgba(0,0,0,0.04)',
    },
  };
  const v = variants[variant];
  return (
    <button onClick={onClick} disabled={disabled} style={{
      ...v,
      padding: s.p, height: s.h, minHeight: s.h,
      borderRadius: 999,
      fontFamily: TB.sans, fontSize: s.fs, fontWeight: 600,
      letterSpacing: -0.1, cursor: disabled ? 'default' : 'pointer',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
      width: full ? '100%' : undefined,
      transition: 'transform .12s ease, opacity .12s ease, background .12s ease',
      WebkitTapHighlightColor: 'transparent',
      ...style,
    }}
    onMouseDown={e => { if (!disabled) e.currentTarget.style.transform = 'scale(0.98)'; }}
    onMouseUp={e => { e.currentTarget.style.transform = 'scale(1)'; }}
    onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)'; }}
    >
      {iconLeft}
      <span>{children}</span>
      {iconRight}
    </button>
  );
}

// ─────────────────────────────────────────────────────────────
// Pill (tag/chip)
// ─────────────────────────────────────────────────────────────
function Pill({ children, variant = 'default', size = 'md', style }) {
  const sizes = { sm: { p: '3px 8px', fs: 10 }, md: { p: '4px 10px', fs: 11 }, lg: { p: '6px 14px', fs: 13 } };
  const variants = {
    default: { background: TB.cardSoft, color: TB.tertiary, border: `1px solid ${TB.divider}` },
    olive:   { background: TB.secondarySoft, color: TB.secondaryDark, border: 'none' },
    oliveDeep:{ background: TB.secondary,     color: TB.cream,         border: 'none' },
    terra:   { background: TB.primarySoft, color: TB.primaryDark, border: 'none' },
    mustard: { background: '#F1E3BE', color: '#6E5316', border: 'none' },
    ink:     { background: TB.ink, color: TB.cream, border: 'none' },
    cream:   { background: TB.card, color: TB.ink, border: `1px solid ${TB.divider}` },
    warning: { background: '#F5D5B8', color: '#7A3820', border: 'none' },
    outline: { background: 'transparent', color: TB.tertiary, border: `1px solid ${TB.divider}` },
  };
  const s = sizes[size]; const v = variants[variant];
  return (
    <span style={{
      ...v, padding: s.p, borderRadius: 999, display: 'inline-flex', alignItems: 'center', gap: 4,
      fontFamily: TB.sans, fontSize: s.fs, fontWeight: 600,
      textTransform: 'uppercase', letterSpacing: 0.4, whiteSpace: 'nowrap',
      ...style,
    }}>{children}</span>
  );
}

// ─────────────────────────────────────────────────────────────
// Progress bar
// ─────────────────────────────────────────────────────────────
function Progress({ value = 0, color = TB.secondary, bg = TB.dividerSoft, h = 6, style }) {
  return (
    <div style={{
      width: '100%', height: h, background: bg, borderRadius: 999, overflow: 'hidden', ...style,
    }}>
      <div style={{
        width: `${Math.max(0, Math.min(100, value))}%`, height: '100%',
        background: color, borderRadius: 999, transition: 'width .4s ease',
      }} />
    </div>
  );
}

Object.assign(window, { TB, CLUB_COLORS, Cover, Avatar, PillButton, Pill, Progress });
