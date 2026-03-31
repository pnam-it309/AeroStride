/**
 * Pagination types and utilities for AeroStride.
 */

/**
 * @typedef {Object} PageResponse
 * @property {Array} content - The list of items in the current page.
 * @property {number} pageNumber - Current page number (0-indexed or 1-indexed depending on backend implementation).
 * @property {number} pageSize - Number of items per page.
 * @property {number} totalElements - Total number of items across all pages.
 * @property {number} totalPages - Total number of pages.
 * @property {boolean} last - Whether this is the last page.
 */

/**
 * @typedef {Object} PageRequest
 * @property {number} page - Page number.
 * @property {number} size - Page size.
 * @property {string} [sortBy] - Field to sort by.
 * @property {string} [sortDirection] - Sort direction ('asc' or 'desc').
 */

export const DEFAULT_PAGE_REQUEST = {
  page: 1,
  size: 10,
  sortBy: '',
  sortDirection: 'asc',
  limitOptions: [5, 10, 20, 50],
}

/**
 * Helper to convert backend PageResponse to frontend friendly state.
 * @param {PageResponse} response
 * @returns {Object}
 */
export const mapPageResponse = (response) => {
  if (!response) return null
  return {
    items: response.content || [],
    currentPage: response.pageNumber + 1, // backend is usually 0-indexed
    pageSize: response.pageSize,
    totalItems: response.totalElements,
    totalPages: response.totalPages,
    isLast: response.last,
  }
}
