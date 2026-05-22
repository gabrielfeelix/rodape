// logo.jsx — Tramabook chat-bubble mark

function TramabookLogo({ size = 32, color = '#B85838' }) {
  // Speech bubble with peeled-back corner — orange/terracotta
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" style={{ flexShrink: 0 }}>
      <defs>
        <filter id={`tb-shadow-${size}`} x="-10%" y="-10%" width="120%" height="120%">
          <feDropShadow dx="0" dy="0.6" stdDeviation="0.8" floodOpacity="0.18"/>
        </filter>
      </defs>
      <g filter={`url(#tb-shadow-${size})`}>
        {/* Main bubble */}
        <path d="M22 14 H72 Q88 14 88 30 V62 Q88 78 72 78 H56 L48 92 L40 78 H22 Q12 78 12 62 V30 Q12 14 22 14 Z"
          fill={color} />
        {/* Folded corner (darker) */}
        <path d="M88 56 Q88 78 72 78 L72 64 Q72 56 80 56 Z"
          fill={color} opacity="0.7" />
        <path d="M88 56 L80 56 Q72 56 72 64 L72 78 Z"
          fill="none" stroke="rgba(0,0,0,0.18)" strokeWidth="0.6" />
      </g>
    </svg>
  );
}

function TramabookWordmark({ size = 22, color = '#1B1F1A', mark }) {
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: 8,
      fontFamily: TB.serif, fontWeight: 600, color, letterSpacing: -0.6,
      fontSize: size,
    }}>
      <TramabookLogo size={size * 1.1} />
      <span>tramabook</span>
    </div>
  );
}

Object.assign(window, { TramabookLogo, TramabookWordmark });
