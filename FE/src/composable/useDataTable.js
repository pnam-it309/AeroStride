import { reactive, onMounted, computed } from 'vue'
import api from '@/services/api'
import { useSafeApi } from './useSafeApi'

/**
 * useDataTable
 * Reusable logic for paginated data tables, powered by useSafeApi for resilience.
 * @param {string} endpoint - The API endpoint to fetch data from.
 * @param {Object} options - Additional options like initial filters.
 */
export function useDataTable(endpoint, options = {}) {
  const pagination = reactive({
    page: 1,
    size: 10,
    sortBy: options.defaultSortBy || 'id',
    sortDirection: options.defaultSortDirection || 'desc',
  })

  const filters = reactive(options.initialFilters || {})

  // Define the API call function for useSafeApi
  const apiCall = () => {
    return api.get(endpoint, {
      params: {
        page: pagination.page,
        size: pagination.size,
        sortBy: pagination.sortBy,
        sortDirection: pagination.sortDirection,
        ...filters,
      },
    })
  }

  const {
    data: responseData,
    loading,
    error,
    execute: fetchData,
    isRetrying,
  } = useSafeApi(apiCall, {
    cacheKey: options.cacheKey || `table_${endpoint.replace(/\//g, '_')}`,
    initialData: { content: [], totalElements: 0 },
  })

  // Computed properties to match the expected interface
  const items = computed(() => responseData.value?.content || [])
  const total = computed(() => responseData.value?.totalElements || 0)

  const handlePageChange = (newPage) => {
    pagination.page = newPage
    fetchData()
  }

  const handlePageSizeChange = (newSize) => {
    pagination.size = newSize
    pagination.page = 1
    fetchData()
  }

  const handleSort = (key) => {
    if (pagination.sortBy === key) {
      pagination.sortDirection = pagination.sortDirection === 'asc' ? 'desc' : 'asc'
    } else {
      pagination.sortBy = key
      pagination.sortDirection = 'desc'
    }
    fetchData()
  }

  const applyFilters = (newFilters) => {
    Object.assign(filters, newFilters)
    pagination.page = 1
    fetchData()
  }

  onMounted(() => {
    if (options.immediate !== false) {
      fetchData()
    }
  })

  return {
    items,
    loading,
    total,
    pagination,
    filters,
    error,
    isRetrying,
    fetchData,
    handlePageChange,
    handlePageSizeChange,
    handleSort,
    applyFilters,
  }
}
