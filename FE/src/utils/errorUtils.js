/**
 * Error Utility Functions
 * Shared helpers for extracting error messages from backend responses
 */

/**
 * Extract human-readable error message from backend error response
 * Supports Spring Boot validation errors, standard error responses, and generic JS errors
 * @param {Error|object} error - Error object from catch block
 * @param {string} fallbackMessage - Fallback if no message can be extracted
 * @returns {string}
 */
export const getBackendErrorMessage = (error, fallbackMessage = 'Thao tác thất bại') =>
    error?.response?.data?.message ||
    error?.response?.data?.errors?.[0]?.defaultMessage ||
    error?.message ||
    fallbackMessage;
