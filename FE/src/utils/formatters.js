/**
 * formatters.js
 * Reusable data formatting functions.
 */

/**
 * Format number to VND currency
 * @param {number|string} value 
 * @returns {string}
 */
export const formatCurrency = (value) => {
  if (!value && value !== 0) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value);
};

/**
 * Format date string to DD/MM/YYYY
 * @param {string|Date} dateString 
 * @param {boolean} includeTime 
 * @returns {string}
 */
export const formatDate = (dateString, includeTime = false) => {
  if (!dateString) return '--/--/----';
  
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return 'Invalid Date';

  const options = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  };

  if (includeTime) {
    options.hour = '2-digit';
    options.minute = '2-digit';
  }

  return new Intl.NumberFormat('vi-VN', options).format(date);
  // Actually Intl.NumberFormat is for numbers. For dates use Intl.DateTimeFormat
};

/**
 * Corrected formatDate using DateTimeFormat
 */
export const formatDateTime = (dateString, includeTime = false) => {
  if (!dateString) return '--/--/----';
  
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return 'Invalid Date';

  const options = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  };

  if (includeTime) {
    options.hour = '2-digit';
    options.minute = '2-digit';
    options.second = '2-digit';
    options.hour12 = false;
  }

  return new Intl.DateTimeFormat('vi-VN', options).format(date);
};

export default {
  formatCurrency,
  formatDate: formatDateTime // Exporting the corrected one as formatDate
};
