import { API_COMMON, API_DEFAULTS } from '@/constants/apiPaths';

const ABSOLUTE_HTTP_URL_PATTERN = /^https?:\/\//i;
const CLOUDINARY_HOST_PATTERN = /^res\.cloudinary\.com$/i;

const normalizeApiBaseUrl = () => {
    const apiBaseUrl = String(import.meta.env.VITE_API_URL || API_DEFAULTS.PREFIX).trim();
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

    // Optimization: Do NOT proxy Cloudinary URLs.
    // Cloudinary is a high-performance CDN, proxying through our Backend is much slower.
    return false;
};

/**
 * Injects Cloudinary optimization parameters (f_auto, q_auto) into the URL.
 * @param {string} url The original Cloudinary URL
 * @returns {string} Optimized URL
 */
const optimizeCloudinaryUrl = (url) => {
    if (!url || !url.includes('res.cloudinary.com')) return url;
    
    // Check if it's an upload URL and doesn't already have transformations
    const uploadPath = '/image/upload/';
    if (url.includes(uploadPath) && !url.includes(uploadPath + 'f_auto')) {
        return url.replace(uploadPath, `${uploadPath}f_auto,q_auto/`);
    }
    return url;
};

export const getDisplayImageUrl = (value) => {
    if (!value || typeof value !== 'string') {
        return value || '';
    }

    const optimizedValue = optimizeCloudinaryUrl(value);

    if (!shouldProxyImageUrl(optimizedValue)) {
        return optimizedValue;
    }

    return `${API_BASE_URL}${API_COMMON.STORAGE}/proxy?url=${encodeURIComponent(optimizedValue)}`;
};
