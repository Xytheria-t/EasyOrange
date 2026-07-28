export function DashboardBackground() {
    return (
        <div
            style={{
                position: 'fixed',
                inset: 0,
                zIndex: 0,
                pointerEvents: 'none',
                overflow: 'hidden',
                background: 'linear-gradient(175deg, #FFF9F5 0%, #FAF6F1 30%, #FDF8F4 60%, #FAF8F5 100%)',
            }}
        >
            {/* Blob 1 — warm orange top-left */}
            <div
                style={{
                    position: 'absolute',
                    top: '-8%',
                    left: '-6%',
                    width: '55vw',
                    height: '55vw',
                    maxWidth: '700px',
                    maxHeight: '700px',
                    borderRadius: '50%',
                    background:
                        'radial-gradient(circle, rgba(249,115,22,0.09) 0%, rgba(249,115,22,0.04) 40%, transparent 70%)',
                    filter: 'blur(40px)',
                    animation: 'dashBlobFloat1 20s ease-in-out infinite',
                }}
            />
            {/* Blob 2 — rose mid-right */}
            <div
                style={{
                    position: 'absolute',
                    top: '20%',
                    right: '-10%',
                    width: '50vw',
                    height: '50vw',
                    maxWidth: '650px',
                    maxHeight: '650px',
                    borderRadius: '50%',
                    background:
                        'radial-gradient(circle, rgba(251,113,133,0.07) 0%, rgba(251,113,133,0.03) 45%, transparent 70%)',
                    filter: 'blur(45px)',
                    animation: 'dashBlobFloat2 25s ease-in-out infinite',
                }}
            />
            {/* Blob 3 — purple bottom-left */}
            <div
                style={{
                    position: 'absolute',
                    bottom: '-5%',
                    left: '15%',
                    width: '45vw',
                    height: '45vw',
                    maxWidth: '580px',
                    maxHeight: '580px',
                    borderRadius: '50%',
                    background:
                        'radial-gradient(circle, rgba(195,155,211,0.08) 0%, rgba(195,155,211,0.03) 45%, transparent 70%)',
                    filter: 'blur(50px)',
                    animation: 'dashBlobFloat3 22s ease-in-out infinite reverse',
                }}
            />
            {/* Blob 4 — gold accent center-right */}
            <div
                style={{
                    position: 'absolute',
                    top: '45%',
                    right: '25%',
                    width: '30vw',
                    height: '30vw',
                    maxWidth: '400px',
                    maxHeight: '400px',
                    borderRadius: '50%',
                    background: 'radial-gradient(circle, rgba(251,191,36,0.05) 0%, transparent 65%)',
                    filter: 'blur(35px)',
                    animation: 'dashBlobFloat4 18s ease-in-out infinite',
                }}
            />
            {/* Aurora conic-gradient */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    background:
                        'conic-gradient(from 180deg at 50% 50%, rgba(249,115,22,0.04), rgba(251,113,133,0.03), rgba(195,155,211,0.04), rgba(251,191,36,0.02), rgba(249,115,22,0.04))',
                    opacity: 0.6,
                    mixBlendMode: 'screen',
                }}
            />
            {/* Noise texture overlay */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E")`,
                    opacity: 0.025,
                }}
            />
            {/* Subtle grid pattern */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    backgroundImage:
                        'linear-gradient(rgba(249,115,22,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(249,115,22,0.03) 1px, transparent 1px)',
                    backgroundSize: '64px 64px',
                    maskImage: 'radial-gradient(ellipse 80% 60% at 50% 40%, black 20%, transparent 70%)',
                    WebkitMaskImage: 'radial-gradient(ellipse 80% 60% at 50% 40%, black 20%, transparent 70%)',
                }}
            />
            {/* Vignette */}
            <div
                style={{
                    position: 'absolute',
                    inset: 0,
                    background:
                        'radial-gradient(ellipse 120% 100% at 50% 0%, transparent 50%, rgba(26,22,18,0.04) 100%)',
                }}
            />
        </div>
    );
}
