import useAuthStore from '@/stores/authStore'

/**
 * Route Guards
 * Navigation guards to protect routes and handle authorization.
 */
export const setupRouteGuards = (router) => {
  router.beforeEach((to) => {
    const authStore = useAuthStore()

    // Check if the route requires authentication
    // Default: routes under /admin are protected
    const requiresAuth =
      to.matched.some((record) => record.meta.requiresAuth) || to.path.startsWith('/admin')
    const isGuestOnly =
      to.matched.some((record) => record.meta.guestOnly) || to.path.startsWith('/auth')

    if (requiresAuth && !authStore.isLogin) {
      // Not logged in, redirect to login
      return { path: '/auth/login', query: { redirect: to.fullPath } }
    } else if (isGuestOnly && authStore.isLogin) {
      // Already logged in, redirect to dashboard if trying to access auth pages
      return { path: '/admin/dashboard' }
    } else {
      // Set page title if available
      if (to.meta.title) {
        document.title = `${to.meta.title} | AeroStride`
      }
    }
  })
}

export default setupRouteGuards
