import { ref, shallowRef } from 'vue';
import api from '@/services/api';

/**
 * Enhanced composable for making API calls with "Soft Fail" logic.
 * @param {Function} apiCall - A function that returns a promise from the 'api' service.
 * @param {Object} options - Configuration options.
 */
export function useSafeApi(apiCall, options = {}) {
  const { 
    immediate = false, 
    initialData = null, 
    cacheKey = null,
    silent = false, // If true, don't show global toasts (handled by caller)
    retryCount = 3,
    fallbackValue = null
  } = options;

  const data = ref(initialData);
  const error = shallowRef(null);
  const loading = ref(immediate);
  const isRetrying = ref(false);

  // Load from cache initially (Graceful Degradation)
  if (cacheKey) {
    const cached = localStorage.getItem(`api_cache_${cacheKey}`);
    if (cached) {
      try {
        data.value = JSON.parse(cached);
      } catch (e) {
        console.warn('Silent fail: Cache parsing error', e);
      }
    }
  }

  const execute = async (...args) => {
    loading.value = true;
    error.value = null;
    let attempts = 0;

    const performRequest = async () => {
      try {
        const result = await apiCall(...args);
        data.value = result;
        
        if (cacheKey) {
          localStorage.setItem(`api_cache_${cacheKey}`, JSON.stringify(result));
        }
        return result;
      } catch (err) {
        if (attempts < retryCount) {
          attempts++;
          isRetrying.value = true;
          // Exponential backoff
          await new Promise(r => setTimeout(r, 1000 * attempts));
          return performRequest();
        }
        
        error.value = err;
        // Graceful Degradation: If we have fallback or cache, it stays. 
        // If we want a strict fallback on error:
        if (fallbackValue !== null && data.value === initialData) {
            data.value = fallbackValue;
        }

        if (!silent) {
           // Global interceptor already shows toast, but we can add secondary handling here
        }
        throw err;
      } finally {
        isRetrying.value = false;
      }
    };

    try {
      return await performRequest();
    } finally {
      loading.value = false;
    }
  };

  if (immediate) {
    execute();
  }

  return {
    data,
    error,
    loading,
    execute,
    isRetrying
  };
}
