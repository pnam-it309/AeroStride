import { ref, shallowRef } from 'vue'
import api from '@/services/api'

/**
 * Custom composable for making API calls with structured state.
 * @param {Function} apiCall - A function that returns a promise from the 'api' service.
 * @param {Object} options - Configuration options.
 * @param {boolean} options.immediate - Whether to execute the call immediately.
 * @param {any} options.initialData - Initial value for the data ref.
 * @param {string} options.cacheKey - Key for localStorage caching.
 */
export function useApi(apiCall, options = {}) {
  const { immediate = false, initialData = null, cacheKey = null } = options

  const data = ref(initialData)
  const error = shallowRef(null)
  const loading = ref(immediate)

  // Load from cache if cacheKey is provided
  if (cacheKey) {
    const cached = localStorage.getItem(`api_cache_${cacheKey}`)
    if (cached) {
      try {
        data.value = JSON.parse(cached)
      } catch (e) {
        console.error('Failed to parse cached data', e)
      }
    }
  }

  const execute = async (...args) => {
    loading.value = true
    error.value = null

    try {
      const result = await apiCall(...args)
      data.value = result

      // Save to cache
      if (cacheKey) {
        localStorage.setItem(`api_cache_${cacheKey}`, JSON.stringify(result))
      }

      return result
    } catch (err) {
      error.value = err

      // If we have cached data, we might want to keep it
      // otherwise, handled by global axios interceptor for alerts
      throw err
    } finally {
      loading.value = false
    }
  }

  if (immediate) {
    execute()
  }

  return {
    data,
    error,
    loading,
    execute,
  }
}
