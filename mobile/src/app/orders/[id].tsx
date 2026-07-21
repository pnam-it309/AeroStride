/**
 * Order Detail Screen
 */

import React, { useEffect, useState } from 'react';
import { StyleSheet, View, Text, ScrollView, Pressable, Alert } from 'react-native';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { orderService, type Order } from '@/services/orderService';
import { fileService } from '@/services/fileService';
import { formatCurrency, formatDate, getOrderStatusColor } from '@/utils/format';
import { StatusBadge } from '@/components/ui/Badge';
import { StatusTimeline } from '@/components/StatusTimeline';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

export default function OrderDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!id) return;
    orderService
      .getOrderDetail(id)
      .then(setOrder)
      .catch((err) => {
        console.warn('Failed to load order:', err);
        Alert.alert('Lỗi', 'Không thể tải đơn hàng');
      })
      .finally(() => setLoading(false));
  }, [id]);

  const handleCancel = () => {
    if (!order) return;
    Alert.alert('Hủy đơn hàng', 'Bạn có chắc muốn hủy đơn hàng này?', [
      { text: 'Không', style: 'cancel' },
      {
        text: 'Hủy đơn',
        style: 'destructive',
        onPress: async () => {
          setCancelling(true);
          try {
            await orderService.cancelOrder(order.id);
            setOrder((prev) =>
              prev
                ? { ...prev, trangThai: 'DA_HUY', trangThaiDisplay: 'Đã hủy' }
                : null
            );
            Alert.alert('Thành công', 'Đơn hàng đã được hủy');
          } catch (err: any) {
            Alert.alert('Lỗi', err?.response?.data?.message || 'Không thể hủy đơn hàng');
          } finally {
            setCancelling(false);
          }
        },
      },
    ]);
  };

  if (loading) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.background }]}>
        <LoadingSpinner fullScreen />
      </View>
    );
  }

  if (!order) return null;

  const canCancel = order.trangThai === 'CHO_XAC_NHAN';
  const statusColor = getOrderStatusColor(order.trangThai);

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
        {/* Header */}
        <View style={[styles.header, { paddingTop: insets.top + Spacing.two }]}>
          <Pressable onPress={() => router.back()} hitSlop={12}>
            <Ionicons name="arrow-back" size={24} color={theme.text} />
          </Pressable>
          <Text style={[styles.title, { color: theme.text }]}>Chi tiết đơn hàng</Text>
          <View style={{ width: 24 }} />
        </View>

        {/* Order Status */}
        <View style={[styles.statusCard, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <View style={styles.statusHeader}>
            <View>
              <Text style={[styles.orderCode, { color: theme.text }]}>{order.maHoaDon}</Text>
              <Text style={[styles.orderDate, { color: theme.textTertiary }]}>
                {formatDate(order.ngayTao)}
              </Text>
            </View>
            <StatusBadge
              status={order.trangThai}
              label={order.trangThaiDisplay}
              color={statusColor}
            />
          </View>
        </View>

        {/* Status Timeline */}
        {order.lichSuTrangThai && order.lichSuTrangThai.length > 0 && (
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <Text style={[styles.sectionTitle, { color: theme.text }]}>Trạng thái đơn hàng</Text>
            <StatusTimeline history={order.lichSuTrangThai} />
          </View>
        )}

        {/* Delivery Info */}
        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>Thông tin giao hàng</Text>
          <View style={styles.infoRow}>
            <Ionicons name="person-outline" size={16} color={theme.textSecondary} />
            <Text style={[styles.infoText, { color: theme.text }]}>{order.tenNguoiNhan}</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="call-outline" size={16} color={theme.textSecondary} />
            <Text style={[styles.infoText, { color: theme.text }]}>
              {order.soDienThoaiNguoiNhan}
            </Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="location-outline" size={16} color={theme.textSecondary} />
            <Text style={[styles.infoText, { color: theme.text }]}>
              {order.diaChiNguoiNhan}
            </Text>
          </View>
        </View>

        {/* Order Items */}
        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>
            Sản phẩm ({order.items?.length || 0})
          </Text>
          {order.items?.map((item) => (
            <View key={item.id} style={[styles.itemRow, { borderTopColor: theme.borderLight }]}>
              <View style={[styles.itemImage, { backgroundColor: theme.backgroundElement }]}>
                {item.hinhAnh ? (
                  <Image
                    source={fileService.getImageSource(item.hinhAnh)}
                    style={{ width: '100%', height: '100%' }}
                    contentFit="cover"
                  />
                ) : (
                  <Ionicons name="footsteps-outline" size={20} color={theme.textTertiary} />
                )}
              </View>
              <View style={styles.itemInfo}>
                <Text style={[styles.itemName, { color: theme.text }]} numberOfLines={2}>
                  {item.tenSanPham}
                </Text>
                <Text style={[styles.itemVariant, { color: theme.textSecondary }]}>
                  {[item.tenMauSac, item.tenKichThuoc].filter(Boolean).join(' · ')}
                </Text>
                <View style={styles.itemPriceRow}>
                  <Text style={[styles.itemPrice, { color: Brand.primary }]}>
                    {formatCurrency(item.donGia)}
                  </Text>
                  <Text style={[styles.itemQty, { color: theme.textTertiary }]}>
                    x{item.soLuong}
                  </Text>
                </View>
              </View>
            </View>
          ))}
        </View>

        {/* Price Summary */}
        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>Thanh toán</Text>
          <View style={styles.priceRow}>
            <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Tạm tính</Text>
            <Text style={[styles.priceValue, { color: theme.text }]}>
              {formatCurrency(order.tongTien)}
            </Text>
          </View>
          {order.phiVanChuyen > 0 && (
            <View style={styles.priceRow}>
              <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Phí vận chuyển</Text>
              <Text style={[styles.priceValue, { color: theme.text }]}>
                {formatCurrency(order.phiVanChuyen)}
              </Text>
            </View>
          )}
          {order.tienGiam > 0 && (
            <View style={styles.priceRow}>
              <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Giảm giá</Text>
              <Text style={[styles.priceValue, { color: Brand.success }]}>
                -{formatCurrency(order.tienGiam)}
              </Text>
            </View>
          )}
          {order.maVoucher && (
            <View style={styles.priceRow}>
              <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Voucher</Text>
              <Text style={[styles.priceValue, { color: Brand.accent }]}>{order.maVoucher}</Text>
            </View>
          )}
          <View style={[styles.totalRow, { borderTopColor: theme.border }]}>
            <Text style={[styles.totalLabel, { color: theme.text }]}>Tổng cộng</Text>
            <Text style={[styles.totalValue, { color: Brand.primary }]}>
              {formatCurrency(order.tongTienSauGiam ?? order.tongTien)}
            </Text>
          </View>
          <View style={styles.priceRow}>
            <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Thanh toán</Text>
            <Text style={[styles.priceValue, { color: theme.text }]}>
              {order.phuongThucThanhToan === 'COD' ? 'Tiền mặt khi nhận' : order.phuongThucThanhToan}
            </Text>
          </View>
        </View>
      </ScrollView>

      {/* Bottom Cancel Button */}
      {canCancel && (
        <View
          style={[
            styles.bottomBar,
            {
              backgroundColor: theme.surface,
              borderTopColor: theme.border,
              paddingBottom: insets.bottom + Spacing.two,
            },
          ]}
        >
          <Pressable
            style={({ pressed }) => [styles.cancelBtn, { opacity: cancelling || pressed ? 0.7 : 1 }]}
            onPress={handleCancel}
            disabled={cancelling}
          >
            <Text style={styles.cancelText}>
              {cancelling ? 'Đang xử lý...' : 'Hủy đơn hàng'}
            </Text>
          </Pressable>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  loadingContainer: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.two + 4,
  },
  title: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  statusCard: {
    marginHorizontal: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
    marginBottom: Spacing.two,
  },
  statusHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  orderCode: {
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
  },
  orderDate: {
    fontSize: FontSizes.xs,
    marginTop: 2,
  },
  section: {
    marginHorizontal: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
    marginBottom: Spacing.two,
  },
  sectionTitle: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
    marginBottom: Spacing.three,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    marginBottom: Spacing.two,
  },
  infoText: {
    fontSize: FontSizes.sm,
    flex: 1,
  },
  itemRow: {
    flexDirection: 'row',
    gap: Spacing.two + 2,
    paddingVertical: Spacing.two + 2,
    borderTopWidth: 1,
  },
  itemImage: {
    width: 60,
    height: 60,
    borderRadius: BorderRadius.sm,
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
  },
  itemInfo: {
    flex: 1,
    gap: 2,
  },
  itemName: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
    lineHeight: 18,
  },
  itemVariant: {
    fontSize: FontSizes.xs,
  },
  itemPriceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 2,
  },
  itemPrice: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  itemQty: {
    fontSize: FontSizes.xs,
  },
  priceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  priceLabel: {
    fontSize: FontSizes.sm,
  },
  priceValue: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopWidth: 1,
    paddingTop: Spacing.two + 2,
    marginTop: Spacing.one,
    marginBottom: Spacing.two,
  },
  totalLabel: {
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
  },
  totalValue: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.extrabold,
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    paddingHorizontal: Spacing.three,
    paddingTop: Spacing.three,
    borderTopWidth: 1,
  },
  cancelBtn: {
    backgroundColor: Brand.error,
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
  },
  cancelText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
});
