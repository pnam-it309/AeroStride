/**
 * Validation Utility for Frontend
 * Provides common validation rules for forms and data integrity.
 */

export const validationUtil = {
  /**
   * Check if a value is empty (null, undefined, empty string, or empty array/object)
   */
  isEmpty: (value) => {
    if (value === null || value === undefined) return true
    if (typeof value === 'string') return value.trim().length === 0
    if (Array.isArray(value)) return value.length === 0
    if (typeof value === 'object') return Object.keys(value).length === 0
    return false
  },

  /**
   * Validate Email format
   */
  isEmail: (email) => {
    const re = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    return re.test(String(email).toLowerCase())
  },

  /**
   * Validate Vietnamese Phone Number
   * Supports common formats: 09x, 08x, 07x, 05x, 03x (10 digits)
   */
  isPhoneNumber: (phone) => {
    const re = /^(0|84)(3|5|7|8|9)([0-9]{8})$/
    return re.test(String(phone))
  },

  /**
   * Check minimum length
   */
  isMinLength: (value, min) => {
    if (!value) return false
    return value.length >= min
  },

  /**
   * Check maximum length
   */
  isMaxLength: (value, max) => {
    if (!value) return true
    return value.length <= max
  },

  /**
   * Check if value is numeric
   */
  isNumeric: (value) => {
    return /^\d+$/.test(String(value))
  },

  /**
   * Validate Password
   * Requirements: Min 8 chars, at least 1 uppercase, 1 lowercase, 1 number
   */
  isValidPassword: (password) => {
    const minLength = password.length >= 8
    const hasUpperCase = /[A-Z]/.test(password)
    const hasLowerCase = /[a-z]/.test(password)
    const hasNumber = /[0-9]/.test(password)
    return minLength && hasUpperCase && hasLowerCase && hasNumber
  },

  /**
   * Validate URL
   */
  isValidUrl: (url) => {
    try {
      new URL(url)
      return true
    } catch (_) {
      return false
    }
  },
}

export default validationUtil
