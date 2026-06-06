/**
 * Formatting utilities for AeroStride Mobile
 */

/**
 * Format number as Vietnamese currency (VND)
 * @example formatCurrency(1500000) => "1.500.000₫"
 */
export function toNumber(value: number | string | undefined | null, fallback = 0): number {
  if (value == null || value === '') return fallback;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function formatCurrency(amount: number | string | undefined | null): string {
  const value = toNumber(amount, NaN);
  if (!Number.isFinite(value)) return '0₫';
  return value.toLocaleString('vi-VN') + '₫';
}

/**
 * Format price range
 * @example formatPriceRange(100000, 500000) => "100.000₫ - 500.000₫"
 */
export function formatPriceRange(
  min: number | string | undefined | null,
  max: number | string | undefined | null
): string {
  if (min == null && max == null) return 'Liên hệ';
  const minValue = min == null ? null : toNumber(min, NaN);
  const maxValue = max == null ? null : toNumber(max, NaN);
  if (minValue === maxValue || maxValue == null) return formatCurrency(minValue);
  if (min == null) return formatCurrency(max);
  return `${formatCurrency(minValue)} - ${formatCurrency(maxValue)}`;
}

/**
 * Format timestamp (epoch ms) to Vietnamese date string
 * @example formatDate(1717680000000) => "06/06/2024"
 */
export function formatDate(timestamp: number | undefined | null): string {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

/**
 * Format timestamp to full date time
 * @example formatDateTime(1717680000000) => "06/06/2024 14:00"
 */
export function formatDateTime(timestamp: number | undefined | null): string {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Truncate text with ellipsis
 */
export function truncateText(text: string, maxLength: number): string {
  if (!text || text.length <= maxLength) return text || '';
  return text.substring(0, maxLength).trim() + '...';
}

/**
 * Map order status to display text (matches BE CustomerOrderResponse)
 */
export function mapOrderStatus(status: string): string {
  const statusMap: Record<string, string> = {
    CHO_XAC_NHAN: 'Chờ xác nhận',
    XAC_NHAN: 'Đã xác nhận',
    CHO_GIAO: 'Chờ giao hàng',
    DANG_GIAO: 'Đang giao hàng',
    HOAN_THANH: 'Hoàn thành',
    DA_HUY: 'Đã hủy',
    HOAN_DON: 'Hoàn đơn',
  };
  return statusMap[status] || status;
}

/**
 * Get status color for order badges
 */
export function getOrderStatusColor(status: string): string {
  const colorMap: Record<string, string> = {
    CHO_XAC_NHAN: '#F59E0B',
    XAC_NHAN: '#3B82F6',
    CHO_GIAO: '#8B5CF6',
    DANG_GIAO: '#06B6D4',
    HOAN_THANH: '#10B981',
    DA_HUY: '#EF4444',
    HOAN_DON: '#F97316',
  };
  return colorMap[status] || '#6B7280';
}

export function formatVoucherValue(voucher: {
  loaiPhieu?: string | null;
  phanTramGiamGia?: number | string | null;
  soTienGiam?: number | string | null;
  giamToiDa?: number | string | null;
}): string {
  if (voucher.loaiPhieu === 'PHAN_TRAM' && voucher.phanTramGiamGia) {
    const cap = toNumber(voucher.giamToiDa, 0);
    return cap > 0
      ? `Giảm ${voucher.phanTramGiamGia}% tối đa ${formatCurrency(cap)}`
      : `Giảm ${voucher.phanTramGiamGia}%`;
  }
  return `Giảm ${formatCurrency(voucher.soTienGiam)}`;
}

export function calculateVoucherDiscount(
  voucher: {
    loaiPhieu?: string | null;
    phanTramGiamGia?: number | string | null;
    soTienGiam?: number | string | null;
    donHangToiThieu?: number | string | null;
    giamToiDa?: number | string | null;
  } | null,
  subtotal: number
): number {
  if (!voucher) return 0;

  const minimum = toNumber(voucher.donHangToiThieu, 0);
  if (minimum > 0 && subtotal < minimum) return 0;

  if (voucher.loaiPhieu === 'PHAN_TRAM') {
    const percent = toNumber(voucher.phanTramGiamGia, 0);
    const cap = toNumber(voucher.giamToiDa, 0);
    const discount = Math.floor((subtotal * percent) / 100);
    return cap > 0 ? Math.min(discount, cap) : discount;
  }

  return Math.min(toNumber(voucher.soTienGiam, 0), subtotal);
}
