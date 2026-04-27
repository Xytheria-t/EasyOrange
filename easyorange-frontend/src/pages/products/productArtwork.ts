const ARTWORK_PALETTES = [
    {
        primary: '#F6792F',
        secondary: '#FFF2E5',
        tertiary: '#F3E6D8',
        accent: '#1F6D5C',
        deep: '#3A2718'
    },
    {
        primary: '#D96A2A',
        secondary: '#FCEBDE',
        tertiary: '#EFE4D9',
        accent: '#5A7B6C',
        deep: '#332419'
    },
    {
        primary: '#B85C34',
        secondary: '#FAE9DF',
        tertiary: '#EEE4DB',
        accent: '#2D6A5C',
        deep: '#34221A'
    }
];

export function getCuratedPlaceholderImage(seed: number, label = 'EasyOrange'): string {
    const palette = ARTWORK_PALETTES[Math.abs(seed) % ARTWORK_PALETTES.length];
    const token = escapeXml(getArtworkToken(label, seed));
    const edition = String((Math.abs(seed) % 36) + 1).padStart(2, '0');
    const svg = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" fill="none">
  <defs>
    <linearGradient id="bg" x1="48" y1="32" x2="348" y2="372" gradientUnits="userSpaceOnUse">
      <stop stop-color="${palette.secondary}"/>
      <stop offset="0.54" stop-color="#FFF9F2"/>
      <stop offset="1" stop-color="${palette.tertiary}"/>
    </linearGradient>
    <linearGradient id="panel" x1="84" y1="84" x2="322" y2="324" gradientUnits="userSpaceOnUse">
      <stop stop-color="#FFFFFF" stop-opacity="0.92"/>
      <stop offset="1" stop-color="#FFF8F0" stop-opacity="0.78"/>
    </linearGradient>
    <linearGradient id="orb" x1="250" y1="68" x2="354" y2="182" gradientUnits="userSpaceOnUse">
      <stop stop-color="${palette.primary}" stop-opacity="0.34"/>
      <stop offset="1" stop-color="${palette.accent}" stop-opacity="0.08"/>
    </linearGradient>
  </defs>
  <rect width="400" height="400" rx="40" fill="url(#bg)"/>
  <circle cx="298" cy="102" r="72" fill="url(#orb)"/>
  <rect x="38" y="38" width="324" height="324" rx="34" fill="#FFFFFF" fill-opacity="0.34" stroke="#FFFFFF" stroke-opacity="0.56"/>
  <rect x="74" y="72" width="252" height="252" rx="28" fill="#FFFFFF" fill-opacity="0.62" stroke="#FFFFFF" stroke-opacity="0.72"/>
  <path d="M94 236C126 202 165 184 212 184C244 184 274 192 302 210" stroke="${palette.accent}" stroke-opacity="0.2" stroke-width="3" stroke-linecap="round"/>
  <path d="M96 260L152 214L202 246L258 182L304 226" stroke="${palette.primary}" stroke-opacity="0.24" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  <rect x="94" y="92" width="126" height="34" rx="17" fill="#FFFFFF" fill-opacity="0.76"/>
  <text x="112" y="114" fill="${palette.deep}" font-size="17" font-weight="700" font-family="'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif" letter-spacing="1.2">CURATED COVER</text>
  <text x="94" y="188" fill="${palette.deep}" font-size="74" font-weight="700" font-family="'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif">${token}</text>
  <text x="96" y="220" fill="${palette.deep}" fill-opacity="0.78" font-size="17" font-family="'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif">EasyOrange campus selection</text>
  <rect x="96" y="266" width="128" height="10" rx="5" fill="${palette.primary}" fill-opacity="0.34"/>
  <rect x="96" y="286" width="182" height="10" rx="5" fill="${palette.deep}" fill-opacity="0.1"/>
  <rect x="96" y="306" width="142" height="10" rx="5" fill="${palette.deep}" fill-opacity="0.08"/>
  <text x="270" y="320" fill="${palette.deep}" fill-opacity="0.52" font-size="14" font-weight="700" font-family="'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif" letter-spacing="2.4">EO-${edition}</text>
</svg>`.trim();

    return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function getArtworkToken(label: string, seed: number): string {
    const compact = Array.from(label.replace(/\s+/g, '').trim());
    if (compact.length >= 2) {
        return compact.slice(0, 2).join('').toUpperCase();
    }
    if (compact.length === 1) {
        return compact[0].toUpperCase();
    }

    return String((Math.abs(seed) % 89) + 11);
}

function escapeXml(value: string): string {
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&apos;');
}
