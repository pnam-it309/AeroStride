import api from './api'

/**
 * Authentication Service
 * API calls for login, logout, and token refresh.
 */
export const authService = {
  /**
   * Login with username/password
   * Note: The standard system uses username but the UI uses email as input.
   * We will pass whatever provided in the email field as username to the API.
   */
  login: async (credentials) => {
    return api.post('/auth/login', {
      username: credentials.email, // In your system, it looks like email is username
      password: credentials.password,
    })
  },

  /**
   * Refresh the access token
   */
  refreshToken: async (token) => {
    return api.post('/auth/refresh-token', { refreshToken: token })
  },

  /**
   * Logout from the system
   */
  logout: async () => {
    return api.post('/auth/logout')
  },
}

export default authService
