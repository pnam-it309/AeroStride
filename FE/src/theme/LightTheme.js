// Neutral ramp.
//
// The UI was authored against Tailwind's `slate` scale - the class names appear
// ~445 times across 48 components and the same hex values another ~479 times as
// literals - but Tailwind was never installed, so none of those class names
// resolved and elements simply inherited whatever colour an ancestor set. That
// is why the same card looked different on every screen.
//
// Declaring the ramp as Vuetify theme colours makes it native: Vuetify emits
// `.text-slate-500` / `.bg-slate-50` for every key below, so the existing markup
// works unchanged and there is one colour system instead of two.
//
// `on-*` pairs are explicit rather than left to Vuetify's auto-contrast, because
// `.bg-{key}` also sets `color: rgb(var(--v-theme-on-{key}))` - leaving them out
// makes the text colour of 91 `bg-slate-50` call sites depend on a computed
// guess. Light shades take dark text, dark shades take light text.
const SLATE = {
    'slate-50': '#f8fafc',
    'slate-100': '#f1f5f9',
    'slate-200': '#e2e8f0',
    'slate-300': '#cbd5e1',
    'slate-400': '#94a3b8',
    'slate-500': '#64748b',
    'slate-600': '#475569',
    'slate-700': '#334155',
    'slate-800': '#1e293b',
    'slate-900': '#0f172a'
};

const SLATE_ON = {
    'on-slate-50': '#0f172a',
    'on-slate-100': '#0f172a',
    'on-slate-200': '#0f172a',
    'on-slate-300': '#0f172a',
    'on-slate-400': '#0f172a',
    'on-slate-500': '#ffffff',
    'on-slate-600': '#ffffff',
    'on-slate-700': '#ffffff',
    'on-slate-800': '#ffffff',
    'on-slate-900': '#ffffff'
};

const BLUE_THEME = {
    name: 'BLUE_THEME',
    dark: false,
    variables: {
        'border-color': '#e5eaef'
    },
    colors: {
        primary: '#1e257c',
        secondary: '#707a82',
        info: '#46caeb',
        success: '#4bd08b',
        warning: '#f8c076',
        error: '#fb977d',
        indigo: '#8763da',
        purple: '#9c27b0',
        orange: '#ff9800',
        pink: '#e91e63',
        teal: '#009688',
        grey: '#9e9e9e',
        lightprimary: '#f0f1ff',
        lightinfo: '#e1f5fa',
        lightsecondary: '#e7ecf0',
        lightsuccess: '#dffff3',
        lighterror: '#ffede9',
        lightwarning: '#fff6ea',
        lightindigo: '#f1ebff',

        // These four were all '#000000'. They are the neutral text/grey slots the
        // sidebar reads through --v-theme-*, so section labels, menu text and the
        // sublink dots were rendering pure black instead of muted. Mapped onto the
        // slate ramp above, keeping grey100 lighter than grey200.
        textPrimary: '#0f172a',
        textSecondary: '#64748b',
        grey100: '#cbd5e1',
        grey200: '#94a3b8',

        borderColor: '#e5eaef',
        inputBorder: '#DFE5EF',
        containerBg: '#ffffff',
        background: '#f0f5f9',
        hoverColor: '#f6f9fc',
        surface: '#fff',
        darkbg: '#2a3447',
        bglight: '#f5f8fb',
        bgdark: '#111c2d',

        ...SLATE,
        ...SLATE_ON
    }
};

export { BLUE_THEME };
