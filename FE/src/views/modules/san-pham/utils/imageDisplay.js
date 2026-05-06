import { API_COMMON } from '@/constants/apiPaths';

const ABSOLUTE_HTTP_URL_PATTERN = /^https?:\/\//i;
const CLOUDINARY_HOST_PATTERN = /^res\.cloudinary\.com$/i;

const normalizeApiBaseUrl = () => {
    const apiBaseUrl = String(import.meta.env.VITE_API_URL || '').trim();
    if (!apiBaseUrl) {
        return '';
    }

    const sanitizedBaseUrl = apiBaseUrl.replace(/\/+$/, '');
    if (ABSOLUTE_HTTP_URL_PATTERN.test(sanitizedBaseUrl)) {
        return sanitizedBaseUrl;
    }

    return sanitizedBaseUrl.startsWith('/')
        ? sanitizedBaseUrl
        : `/${sanitizedBaseUrl}`;
};

const API_BASE_URL = normalizeApiBaseUrl();

const parseUrlSafely = (value) => {
    if (typeof window === 'undefined' || !value) {
        return null;
    }

    try {
        return new URL(value, window.location.origin);
    } catch (error) {
        return null;
    }
};

const isProxyUrl = (parsedUrl) => parsedUrl?.pathname?.includes(`${API_COMMON.STORAGE}/proxy`);

const shouldProxyImageUrl = (value) => {
    if (!value || typeof value !== 'string') {
        return false;
    }

    if (value.startsWith('blob:') || value.startsWith('data:')) {
        return false;
    }

    const parsedUrl = parseUrlSafely(value);
    if (!parsedUrl) {
        return false;
    }

    if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
        return false;
    }

    if (isProxyUrl(parsedUrl)) {
        return false;
    }

    return CLOUDINARY_HOST_PATTERN.test(parsedUrl.hostname);
};

export const getDisplayImageUrl = (value) => {
    if (!value || typeof value !== 'string') {
        return value || '';
    }

    if (!shouldProxyImageUrl(value)) {
        return value;
    }

    return `${API_BASE_URL}${API_COMMON.STORAGE}/proxy?url=${encodeURIComponent(value)}`;
};
