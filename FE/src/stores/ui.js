import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

/**
 * UI Store - Quản lý trạng thái giao diện toàn cục
 * Mục đích: Cache trạng thái UI để tránh render lại không cần thiết
 * và đồng bộ trạng thái giữa các trang mà không cần load lại.
 */
export const useUIStore = defineStore('ui', () => {
  // ==========================================
  // SIDEBAR STATE
  // ==========================================
  const sidebarCollapsed = ref(
    localStorage.getItem('sidebar-collapsed') === 'true' || window.innerWidth < 768
  );

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value;
    localStorage.setItem('sidebar-collapsed', sidebarCollapsed.value);
  };

  const setSidebarCollapsed = (value) => {
    sidebarCollapsed.value = value;
    localStorage.setItem('sidebar-collapsed', value);
  };

  // ==========================================
  // GLOBAL LOADING / OVERLAY STATE
  // ==========================================
  const globalLoading = ref(false); // Map to 'overlay'
  const progressBar = ref(false);
  const loadingMessage = ref('Đang tải...');

  /**
   * Bật overlay toàn màn hình (thường dùng cho các tác vụ quan trọng)
   */
  const showLoading = (message = 'Đang tải...') => {
    loadingMessage.value = message;
    globalLoading.value = true;
  };

  /**
   * Tắt overlay toàn màn hình
   */
  const hideLoading = () => {
    globalLoading.value = false;
    loadingMessage.value = 'Đang tải...';
  };

  /**
   * Tương thích với loader.js: showOverlay
   */
  const showOverlay = (msg = 'Hệ thống đang khởi động lại...') => {
    showLoading(msg);
  };

  /**
   * Tương thích với loader.js: hideOverlay
   */
  const hideOverlay = () => {
    hideLoading();
  };

  const startProgress = () => {
    progressBar.value = true;
  };

  const stopProgress = () => {
    setTimeout(() => {
      progressBar.value = false;
    }, 500);
  };

  // ==========================================
  // PAGE DATA CACHE (tránh gọi API lại khi quay về trang và khi F5)
  // ==========================================
  const loadCacheFromSession = () => {
    try {
      const stored = sessionStorage.getItem('page-cache');
      if (!stored) return new Map();
      return new Map(JSON.parse(stored));
    } catch (e) {
      console.error('Lỗi load cache:', e);
      return new Map();
    }
  };

  const pageCache = ref(loadCacheFromSession());

  const saveCacheToSession = () => {
    try {
      sessionStorage.setItem('page-cache', JSON.stringify([...pageCache.value.entries()]));
    } catch (e) {
      console.warn('Lỗi save cache (có thể do dung lượng):', e);
    }
  };

  const getCachedData = (key) => {
    const cached = pageCache.value.get(key);
    if (!cached) return null;

    const now = Date.now();
    const ttl = cached.ttl || 60_000; // Default: 1 phút
    if (now - cached.timestamp > ttl) {
      pageCache.value.delete(key);
      saveCacheToSession();
      return null; // Cache đã hết hạn
    }
    return cached.data;
  };

  const setCachedData = (key, data, ttlMs = 60_000) => {
    pageCache.value.set(key, {
      data,
      timestamp: Date.now(),
      ttl: ttlMs,
    });
    saveCacheToSession();
  };

  const clearCache = (key) => {
    if (key) {
      pageCache.value.delete(key);
    } else {
      pageCache.value.clear();
    }
    saveCacheToSession();
  };

  // ==========================================
  // PAGE PREFERENCES (filter/sort/pagination)
  // ==========================================
  const pagePreferences = ref(
    JSON.parse(sessionStorage.getItem('page-preferences') || '{}')
  );

  const getPagePreference = (pageName, key, defaultValue = null) => {
    return pagePreferences.value[pageName]?.[key] ?? defaultValue;
  };

  const setPagePreference = (pageName, key, value) => {
    if (!pagePreferences.value[pageName]) {
      pagePreferences.value[pageName] = {};
    }
    pagePreferences.value[pageName][key] = value;
    // Lưu vào sessionStorage để không mất khi F5
    sessionStorage.setItem('page-preferences', JSON.stringify(pagePreferences.value));
  };

  // ==========================================
  // NOTIFICATION BADGE
  // ==========================================
  const notificationCount = ref(0);
  const hasNotifications = computed(() => notificationCount.value > 0);

  // ==========================================
  // BREADCRUMBS STATE
  // ==========================================
  const breadcrumbs = ref([]);

  const setBreadcrumbs = (items) => {
    breadcrumbs.value = items;
  };

  return {
    // Sidebar
    sidebarCollapsed,
    toggleSidebar,
    setSidebarCollapsed,

    // Loading / Overlay
    globalLoading,
    progressBar,
    loadingMessage,
    showLoading,
    hideLoading,
    showOverlay, // Alias for compatibility
    hideOverlay, // Alias for compatibility
    startProgress,
    stopProgress,

    // Cache
    getCachedData,
    setCachedData,
    clearCache,

    // Preferences
    getPagePreference,
    setPagePreference,

    // Notifications
    notificationCount,
    hasNotifications,

    // Breadcrumbs
    breadcrumbs,
    setBreadcrumbs,
  };
});

