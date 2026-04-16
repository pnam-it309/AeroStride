const ABSOLUTE_URL_PATTERN = /^(?:https?:)?\/\//i;
const INLINE_URL_PATTERN = /^(?:blob:|data:)/i;

const normalizeSlashes = (value) => String(value || '').trim().replace(/\\/g, '/');
const trimEdgeSlashes = (value) => normalizeSlashes(value).replace(/^\/+|\/+$/g, '');

const getBackendOrigin = () => {
    const explicitOrigin = import.meta.env.VITE_API_ORIGIN || '';
    if (explicitOrigin) {
        return trimEdgeSlashes(explicitOrigin);
    }

    const apiUrl = import.meta.env.VITE_API_URL || '/api/v1';

    if (ABSOLUTE_URL_PATTERN.test(apiUrl)) {
        return new URL(apiUrl).origin;
    }

    if (typeof window === 'undefined') {
        return '';
    }

    if (import.meta.env.DEV) {
        return `http://${window.location.hostname || 'localhost'}:8080`;
    }

    return window.location.origin;
};

export const resolveMediaUrl = (value, folder = '') => {
    const rawValue = normalizeSlashes(value);
    if (!rawValue) return '';

    if (INLINE_URL_PATTERN.test(rawValue) || ABSOLUTE_URL_PATTERN.test(rawValue)) {
        return rawValue;
    }

    const backendOrigin = getBackendOrigin();
    if (!backendOrigin) return rawValue;

    if (rawValue.startsWith('/uploads/')) {
        return `${backendOrigin}${rawValue}`;
    }

    if (rawValue.startsWith('uploads/')) {
        return `${backendOrigin}/${rawValue}`;
    }

    const normalizedFolder = trimEdgeSlashes(folder);
    const normalizedPath = rawValue.replace(/^\/+/, '');

    if (normalizedPath.startsWith('aerostride/')) {
        return `${backendOrigin}/uploads/${normalizedPath}`;
    }

    // Legacy sample values like "sp1.jpg" do not contain a real storage path.
    // In cloud-first mode we let the UI fall back to a placeholder instead of
    // continuously requesting missing local files from the backend.
    if (!normalizedPath.includes('/')) {
        return '';
    }

    if (!normalizedFolder) {
        return `${backendOrigin}/uploads/${normalizedPath}`;
    }

    return `${backendOrigin}/uploads/${normalizedFolder}/${normalizedPath}`;
};
