// icons.jsx — lightweight SVG icons (1.6px stroke, rounded)

const Icon = ({ d, size = 20, stroke = 'currentColor', fill = 'none', sw = 1.6, style }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={fill} stroke={stroke} strokeWidth={sw}
    strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, ...style }}>
    {typeof d === 'string' ? <path d={d} /> : d}
  </svg>
);

const I = {
  home:    (p) => <Icon {...p} d="M3 11l9-8 9 8M5 10v10h14V10" />,
  book:    (p) => <Icon {...p} d="M4 4a2 2 0 0 1 2-2h12v18H6a2 2 0 0 1-2-2zM4 18a2 2 0 0 1 2-2h12" />,
  calendar:(p) => <Icon {...p} d="M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14H4zM4 10h16M9 2v4M15 2v4" />,
  user:    (p) => <Icon {...p} d="M4 21c0-4 3.5-7 8-7s8 3 8 7M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" />,
  bell:    (p) => <Icon {...p} d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6zM10 19a2 2 0 0 0 4 0" />,
  chevD:   (p) => <Icon {...p} d="M6 9l6 6 6-6" />,
  chevR:   (p) => <Icon {...p} d="M9 6l6 6-6 6" />,
  chevL:   (p) => <Icon {...p} d="M15 6l-6 6 6 6" />,
  chevU:   (p) => <Icon {...p} d="M6 15l6-6 6 6" />,
  plus:    (p) => <Icon {...p} d="M12 5v14M5 12h14" />,
  check:   (p) => <Icon {...p} d="M5 13l4 4L19 7" />,
  checkCircle: (p) => <Icon {...p} d={<><circle cx="12" cy="12" r="10" /><path d="M8 12l3 3 5-6" /></>} />,
  lock:    (p) => <Icon {...p} d={<><rect x="5" y="11" width="14" height="10" rx="2" /><path d="M8 11V8a4 4 0 1 1 8 0v3" /></>} />,
  search:  (p) => <Icon {...p} d={<><circle cx="11" cy="11" r="7" /><path d="M20 20l-4-4" /></>} />,
  send:    (p) => <Icon {...p} d="M5 12l14-7-5 16-3-7z" />,
  smile:   (p) => <Icon {...p} d={<><circle cx="12" cy="12" r="10" /><path d="M8 14s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01" /></>} />,
  more:    (p) => <Icon {...p} d={<><circle cx="5" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="19" cy="12" r="1.5" fill="currentColor" stroke="none"/></>} />,
  arrow:   (p) => <Icon {...p} d="M5 12h14M13 6l6 6-6 6" />,
  edit:    (p) => <Icon {...p} d="M4 20h4l10-10-4-4L4 16zM14 6l4 4" />,
  pin:     (p) => <Icon {...p} d={<><path d="M12 22s7-7 7-12a7 7 0 1 0-14 0c0 5 7 12 7 12z" /><circle cx="12" cy="10" r="2.5" /></>} />,
  clock:   (p) => <Icon {...p} d={<><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>} />,
  star:    (p) => <Icon {...p} d="M12 3l2.6 5.6 6 .6-4.5 4.2 1.3 6L12 16.7 6.6 19.4l1.3-6L3.4 9.2l6-.6z" />,
  starFill:(p) => <Icon {...p} fill="currentColor" stroke="none" d="M12 3l2.6 5.6 6 .6-4.5 4.2 1.3 6L12 16.7 6.6 19.4l1.3-6L3.4 9.2l6-.6z" />,
  vote:    (p) => <Icon {...p} d="M4 12l5 5L20 6" />,
  shelf:   (p) => <Icon {...p} d="M4 5v14M20 5v14M4 9h16M4 14h16M8 5v4M14 5v4M11 14v5M17 14v5" />,
  groups:  (p) => <Icon {...p} d={<><circle cx="9" cy="9" r="3.5" /><path d="M2 20c0-3.5 3-6 7-6s7 2.5 7 6" /><circle cx="17" cy="7" r="2.5" /><path d="M22 18c0-2.7-2-4.5-5-4.5" /></>} />,
  log:     (p) => <Icon {...p} d="M4 6h16M4 12h16M4 18h10" />,
  google:  (p) => (
    <svg width={p?.size || 20} height={p?.size || 20} viewBox="0 0 24 24">
      <path fill="#4285F4" d="M22 12.2c0-.8-.1-1.5-.2-2.2H12v4.3h5.6c-.2 1.3-1 2.4-2 3.1v2.6h3.3c1.9-1.8 3.1-4.4 3.1-7.8z"/>
      <path fill="#34A853" d="M12 22c2.7 0 5-.9 6.6-2.4l-3.3-2.6c-.9.6-2.1 1-3.3 1-2.5 0-4.7-1.7-5.5-4H3.2v2.5C4.8 19.7 8.1 22 12 22z"/>
      <path fill="#FBBC04" d="M6.5 14c-.2-.6-.3-1.3-.3-2s.1-1.4.3-2V7.5H3.2C2.4 9 2 10.4 2 12s.4 3 1.2 4.5L6.5 14z"/>
      <path fill="#EA4335" d="M12 6c1.4 0 2.7.5 3.7 1.4l2.8-2.8C16.9 3 14.7 2 12 2 8.1 2 4.8 4.3 3.2 7.5L6.5 10c.8-2.3 3-4 5.5-4z"/>
    </svg>
  ),
  heart:   (p) => <Icon {...p} d="M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.5-7 10-7 10z" />,
  reply:   (p) => <Icon {...p} d="M9 14l-5-5 5-5M4 9h9a7 7 0 0 1 7 7v2" />,
  exit:    (p) => <Icon {...p} d="M9 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M16 17l5-5-5-5M21 12H9" />,
  trophy:  (p) => <Icon {...p} d="M8 5h8v4a4 4 0 0 1-8 0zM8 5H5v2a3 3 0 0 0 3 3M16 5h3v2a3 3 0 0 1-3 3M10 14h4l-1 5h-2z" />,
  bars:    (p) => <Icon {...p} d="M4 18V10M10 18V4M16 18v-6M22 18v-9" />,
};

Object.assign(window, { I, Icon });
