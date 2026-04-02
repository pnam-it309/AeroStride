import axios from 'axios'
import useAuthStore from '@/stores/authStore'
import { useToast } from '@/composable/useToast'
import router from '@/router'
import { ROUTES } from '@/constants'
import { useLoadingStore } from '@/stores/loadingStore'

/**
 * Global Axios Instance
 * Configured with base URL, request/response interceptors for Auth and Error handling.
 */
const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${import.meta.env.VITE_API_PREFIX || '/api/v1'}`,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

const toast = useToast()

/**
 * Request Interceptor
 * Automatically attaches the JWT token to the Authorization header if available.
 */
api.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    const loadingStore = useLoadingStore()

    // Start progress bar for every request
    loadingStore.startProgress()

    // Show full overlay if requested in config
    if (config.showOverlay) {
      loadingStore.showOverlay(true)
    }

    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => {
    const loadingStore = useLoadingStore()
    loadingStore.failProgress()
    loadingStore.showOverlay(false)
    return Promise.reject(error)
  },
)

/**
 * Response Interceptor
 * Globally handles status codes like 401 (Unauthorized) and 400 (Bad Request).
 */
api.interceptors.response.use(
  (response) => {
    const loadingStore = useLoadingStore()
    loadingStore.finishProgress()
    loadingStore.showOverlay(false)

    const apiResponse = response.data
    // Extract data if it follows the ApiResponse wrapper
    if (apiResponse && Object.prototype.hasOwnProperty.call(apiResponse, 'success')) {
      if (!apiResponse.success) {
        return Promise.reject(apiResponse)
      }
      return apiResponse.data
    }

    return apiResponse
  },
  (error) => {
    const authStore = useAuthStore()
    const loadingStore = useLoadingStore()
    const { response } = error

    loadingStore.failProgress()
    loadingStore.showOverlay(false)

    if (response) {
      const { status, data } = response
      const message = data?.message || 'Có lỗi xảy ra, vui lòng thử lại.'
      const errorTitle = data?.error || 'Lỗi'

      switch (status) {
        case 401:
          toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
          authStore.logout()
          router.push(ROUTES.AUTH.LOGIN)
          break

        case 403:
          toast.error('Bạn không có quyền truy cập tính năng này.')
          router.push(ROUTES.ADMIN.ERRORS.E403)
          break

        case 400:
          toast.error(message)
          break

        case 404:
          // Optional: Only redirect to 404 page for page-level 404s,
          // usually API 404s just show a toast.
          toast.error(message)
          break

        case 500:
          toast.error(`Lỗi hệ thống: ${message}`)
          if (!error.config?.skipErrorRedirect) {
            router.push(ROUTES.ADMIN.ERRORS.E500)
          }
          break

        default:
          toast.error(message)
      }
    } else if (error.request) {
      toast.error('Không thể kết nối tới máy chủ. Vui lòng kiểm tra internet.')
    } else {
      toast.error('Lỗi cấu hình yêu cầu.')
    }

    return Promise.reject(error)
  },
)

export default api
