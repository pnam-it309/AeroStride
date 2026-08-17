/**
 * Error Utility Functions
 * Centralized helpers for developer console logging and user-friendly Vietnamese error formatting
 */

/**
 * Log technical error details to console for developer inspection
 */
export const logDevError = (context, error) => {
    try {
        const errorInfo = {
            context: context || 'Application Error',
            status: error?.response?.status,
            statusText: error?.response?.statusText,
            url: error?.config?.url,
            method: error?.config?.method?.toUpperCase(),
            params: error?.config?.params,
            data: error?.config?.data,
            responseData: error?.response?.data,
            errorMessage: error?.message,
            stack: error?.stack
        };
        console.error(`[Dev Error - ${context || 'API'}]:`, errorInfo, error);
    } catch (e) {
        console.error('[Dev Error]:', error);
    }
};

/**
 * Common translation dictionary for technical/English error messages to friendly Vietnamese
 */
const ERROR_TRANSLATIONS = [
    // HTTP Status Regexes
    { pattern: /Request failed with status code 401/i, message: 'Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.' },
    { pattern: /Request failed with status code 403/i, message: 'Bạn không có quyền thực hiện thao tác này.' },
    { pattern: /Request failed with status code 404/i, message: 'Không tìm thấy dữ liệu yêu cầu hoặc tài nguyên đã bị xóa.' },
    { pattern: /Request failed with status code 400/i, message: 'Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại.' },
    { pattern: /Request failed with status code 409/i, message: 'Dữ liệu đã tồn tại hoặc xảy ra xung đột trong hệ thống.' },
    { pattern: /Request failed with status code 422/i, message: 'Dữ liệu không đúng định dạng. Vui lòng kiểm tra lại.' },
    { pattern: /Request failed with status code 429/i, message: 'Hệ thống đang quá tải hoặc bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.' },
    { pattern: /Request failed with status code 50\d/i, message: 'Hệ thống máy chủ đang gặp sự cố. Vui lòng thử lại sau.' },
    { pattern: /Request failed with status code (\d+)/i, message: 'Yêu cầu xử lý thất bại (Mã lỗi $1). Vui lòng thử lại.' },

    // Network / Timeout
    { pattern: /Network Error/i, message: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng của bạn.' },
    { pattern: /timeout of \d+ms exceeded/i, message: 'Yêu cầu xử lý quá thời gian quy định. Vui lòng thử lại sau.' },
    { pattern: /ECONNABORTED/i, message: 'Yêu cầu đã bị ngắt kết nối. Vui lòng thử lại.' },
    { pattern: /Failed to fetch/i, message: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra đường truyền mạng.' },

    // Spring Security / Auth
    { pattern: /^Unauthorized$/i, message: 'Vui lòng đăng nhập để tiếp tục.' },
    { pattern: /Full authentication is required/i, message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.' },
    { pattern: /Bad credentials/i, message: 'Tài khoản hoặc mật khẩu không chính xác.' },
    { pattern: /^Forbidden$/i, message: 'Bạn không có quyền truy cập chức năng này.' },
    { pattern: /Access is denied|Access Denied/i, message: 'Bạn không có quyền thực hiện thao tác này.' },
    { pattern: /User not found|Tài khoản.*không tồn tại/i, message: 'Tài khoản không tồn tại trong hệ thống.' },
    { pattern: /User is disabled|Account is locked/i, message: 'Tài khoản đã bị tạm khóa hoặc ngừng hoạt động.' },
    { pattern: /JWT expired|Token expired/i, message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.' },
    { pattern: /Invalid token|Token is invalid/i, message: 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.' },

    // Validation / Server Errors
    { pattern: /Validation failed/i, message: 'Dữ liệu nhập vào chưa đúng định dạng. Vui lòng kiểm tra lại.' },
    { pattern: /Duplicate resource|Duplicate entry/i, message: 'Dữ liệu đã tồn tại trong hệ thống.' },
    { pattern: /An unexpected server error occurred/i, message: 'Đã xảy ra lỗi máy chủ. Vui lòng thử lại sau.' },
    { pattern: /Internal Server Error/i, message: 'Hệ thống máy chủ đang gặp sự cố. Vui lòng thử lại sau.' }
];

/**
 * Backend error code dictionary
 */
const ERROR_CODE_TRANSLATIONS = {
    ERR_UNAUTHORIZED: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
    ERR_FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
    ERR_NOT_FOUND: 'Không tìm thấy dữ liệu yêu cầu.',
    ERR_DUPLICATE_RESOURCE: 'Dữ liệu đã tồn tại trong hệ thống.',
    ERR_RATE_LIMIT: 'Bạn đã thao tác quá nhanh hoặc gửi quá nhiều yêu cầu. Vui lòng đợi trong giây lát.',
    ERR_INVALID_ARGUMENT: 'Dữ liệu nhập vào không hợp lệ.',
    ERR_WRONG_PASSWORD: 'Mật khẩu không chính xác. Vui lòng kiểm tra lại.',
    ERR_USER_NOT_FOUND: 'Tài khoản không tồn tại trong hệ thống.',
    ERR_INTERNAL_SERVER: 'Lỗi máy chủ nội bộ. Vui lòng thử lại sau.',
    ERR_JSON_PARSE: 'Dữ liệu gửi lên sai định dạng.'
};

/**
 * Convert any error message or string into user-friendly Vietnamese
 * @param {string} rawMsg
 * @param {string} fallbackMessage
 * @returns {string}
 */
export const formatUserErrorMessage = (rawMsg, fallbackMessage = 'Thao tác thất bại') => {
    if (!rawMsg) return fallbackMessage;
    if (typeof rawMsg !== 'string') return String(rawMsg);

    const trimmed = rawMsg.trim();
    if (!trimmed) return fallbackMessage;

    // Check known error codes
    if (ERROR_CODE_TRANSLATIONS[trimmed]) {
        return ERROR_CODE_TRANSLATIONS[trimmed];
    }

    // Check regex patterns
    for (const item of ERROR_TRANSLATIONS) {
        if (item.pattern.test(trimmed)) {
            return trimmed.replace(item.pattern, item.message);
        }
    }

    // If the message is just a status code like "401", "403", "500"
    if (/^\d{3}$/.test(trimmed)) {
        const code = Number(trimmed);
        if (code === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
        if (code === 403) return 'Bạn không có quyền thực hiện thao tác này.';
        if (code === 404) return 'Không tìm thấy dữ liệu yêu cầu.';
        if (code === 429) return 'Hệ thống đang quá tải. Vui lòng thử lại sau.';
        if (code >= 500) return 'Hệ thống máy chủ đang gặp sự cố. Vui lòng thử lại sau.';
        return `Thao tác thất bại (Mã lỗi ${code}). Vui lòng thử lại.`;
    }

    return trimmed;
};

/**
 * Extract human-readable, friendly Vietnamese error message from any backend error response or Error object
 * Also logs detailed debug info in the developer console.
 * @param {Error|object|string} error - Error object from catch block or error string
 * @param {string} fallbackMessage - Fallback if no message can be extracted
 * @param {string} context - Optional context name for dev logging
 * @returns {string}
 */
export const getBackendErrorMessage = (error, fallbackMessage = 'Thao tác thất bại', context = '') => {
    if (!error) return fallbackMessage;

    // Log to console for devs
    logDevError(context || 'Error Handler', error);

    // If it's a string, format it
    if (typeof error === 'string') {
        return formatUserErrorMessage(error, fallbackMessage);
    }

    // Check HTTP status directly from response
    const status = error?.response?.status;
    const errorCode = error?.response?.data?.errorCode || error?.response?.data?.code;

    if (errorCode && ERROR_CODE_TRANSLATIONS[errorCode]) {
        return ERROR_CODE_TRANSLATIONS[errorCode];
    }

    // Check response body message
    const rawBackendMsg =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.response?.data?.errors?.[0]?.defaultMessage ||
        error?.response?.data?.errors?.[0]?.message;

    if (rawBackendMsg && typeof rawBackendMsg === 'string') {
        const formatted = formatUserErrorMessage(rawBackendMsg);
        // If it got translated or is meaningful Vietnamese, return it
        if (formatted !== rawBackendMsg || /[\u00C0-\u1EF9]/.test(rawBackendMsg)) {
            return formatted;
        }
    }

    // Handle status codes if no specific Vietnamese message was returned
    if (status === 401) {
        return 'Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.';
    }
    if (status === 403) {
        return 'Bạn không có quyền thực hiện thao tác này.';
    }
    if (status === 404) {
        return 'Không tìm thấy dữ liệu yêu cầu hoặc tài nguyên đã bị xóa.';
    }
    if (status === 429) {
        return 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.';
    }
    if (status && status >= 500) {
        return 'Hệ thống máy chủ đang gặp sự cố. Vui lòng thử lại sau.';
    }

    // Check error.message (e.g. Axios error or JS runtime error)
    if (error.message) {
        return formatUserErrorMessage(error.message, fallbackMessage);
    }

    return fallbackMessage;
};
