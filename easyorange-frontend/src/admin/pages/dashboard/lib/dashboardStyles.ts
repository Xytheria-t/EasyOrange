export const sectionCard: React.CSSProperties = {
    background: 'rgba(255,255,255,0.65)',
    backdropFilter: 'blur(24px)',
    WebkitBackdropFilter: 'blur(24px)',
    border: '1px solid rgba(255,255,255,0.55)',
    borderRadius: 24,
    overflow: 'hidden',
    transition: 'all 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
    position: 'relative',
};

export const sectionHeader: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '1.25rem 1.65rem',
    borderBottom: '1px solid rgba(229,224,219,0.3)',
    position: 'relative',
};

export const sectionTitle: React.CSSProperties = {
    fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
    fontSize: '1.05rem',
    fontWeight: 700,
    color: '#2A2520',
    letterSpacing: '-0.02em',
    display: 'flex',
    alignItems: 'center',
    gap: '0.55rem',
};

export function sectionIconWrap(background: string, color: string): React.CSSProperties {
    return {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 28,
        height: 28,
        borderRadius: 8,
        background,
        color,
        marginRight: '0.15rem',
    };
}

export const linkStyle: React.CSSProperties = {
    fontSize: '0.78rem',
    fontWeight: 500,
    textDecoration: 'none',
    display: 'flex',
    alignItems: 'center',
    gap: '0.25rem',
    transition: 'gap 0.2s ease',
    fontFamily: "'LXGW WenKai', sans-serif",
};

export const emptyStateBox: React.CSSProperties = {
    textAlign: 'center',
    padding: '2.75rem 1rem',
};

export function emptyStateIcon(background: string): React.CSSProperties {
    return {
        width: 64,
        height: 64,
        margin: '0 auto 0.85rem',
        borderRadius: 18,
        background,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '1.6rem',
    };
}

export const emptyStateTitle: React.CSSProperties = {
    fontFamily: "'Playfair Display', serif",
    fontSize: '0.98rem',
    fontWeight: 600,
    color: '#8B857E',
    marginBottom: '0.3rem',
};

export const emptyStateSubtitle: React.CSSProperties = {
    fontSize: '0.82rem',
    color: '#B5AEA8',
    fontFamily: "'LXGW WenKai', sans-serif",
};

export const listItemStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0.72rem 0.4rem',
    gap: '1rem',
    transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
    borderRadius: 10,
};

export const listColumn: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    minWidth: 0,
};

export const nameText: React.CSSProperties = {
    fontSize: '0.9rem',
    fontWeight: 500,
    color: '#4A4540',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    display: 'block',
    fontFamily: "'LXGW WenKai', sans-serif",
};

export const metaText: React.CSSProperties = {
    fontSize: '0.72rem',
    color: '#B5AEA8',
    fontFamily: "'LXGW WenKai', sans-serif",
};

export const timeBadge: React.CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    padding: '0.2rem 0.6rem',
    borderRadius: 9999,
    fontSize: '0.73rem',
    fontWeight: 500,
    background: 'linear-gradient(135deg, rgba(249,115,22,0.06), rgba(195,155,211,0.04))',
    color: '#9B9590',
    letterSpacing: '0.01em',
    whiteSpace: 'nowrap',
    flexShrink: 0,
    fontFamily: "'LXGW WenKai', sans-serif",
};
