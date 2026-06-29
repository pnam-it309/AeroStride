/**
 * Checkout Screen - Order placement form
 */

import React, { useEffect, useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  ScrollView,
  Pressable,
  Alert,
  Modal,
  FlatList,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as WebBrowser from 'expo-web-browser';
import * as Linking from 'expo-linking';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useCart } from '@/context/CartContext';
import { useAuth } from '@/context/AuthContext';
import { orderService, type CheckoutRequest, type Voucher } from '@/services/orderService';
import { profileService } from '@/services/profileService';
import {
  formatCurrency,
  formatVoucherValue,
  calculateVoucherDiscount,
} from '@/utils/format';

const FREE_SHIP_THRESHOLD = 5_000_000;
const SHIPPING_FEE = 30_000;

export default function CheckoutScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { items, cartTotal, clearCart } = useCart();
  const { isAuthenticated, isLoading: authLoading } = useAuth();

  const [tenNguoiNhan, setTenNguoiNhan] = useState('');
  const [soDienThoai, setSoDienThoai] = useState('');
  const [diaChi, setDiaChi] = useState('');
  const [ghiChu, setGhiChu] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const [loading, setLoading] = useState(false);

  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [selectedVoucher, setSelectedVoucher] = useState<Voucher | null>(null);
  const [voucherModal, setVoucherModal] = useState(false);

  // Require authentication for checkout (BE requires ROLE_KHACH_HANG)
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      Alert.alert('Cần đăng nhập', 'Vui lòng đăng nhập để đặt hàng', [
        { text: 'Để sau', style: 'cancel', onPress: () => router.back() },
        { text: 'Đăng nhập', onPress: () => router.replace('/login') },
      ]);
    }
  }, [authLoading, isAuthenticated, router]);

  // Prefill delivery info from profile
  useEffect(() => {
    if (!isAuthenticated) return;
    profileService
      .getMyProfile()
      .then((p) => {
        setTenNguoiNhan((prev) => prev || p.ten || '');
        setSoDienThoai((prev) => prev || p.sdt || '');
        const addr = [p.diaChiChiTiet, p.phuongXa, p.quanHuyen, p.tinhThanh]
          .filter(Boolean)
          .join(', ');
        setDiaChi((prev) => prev || addr);
      })
      .catch(() => {});
  }, [isAuthenticated]);

  // Load available vouchers for the current cart total
  useEffect(() => {
    if (!isAuthenticated || cartTotal <= 0) return;
    orderService
      .getAvailableVouchers(cartTotal)
      .then(setVouchers)
      .catch(() => setVouchers([]));
  }, [isAuthenticated, cartTotal]);

  const shippingFee = cartTotal >= FREE_SHIP_THRESHOLD ? 0 : SHIPPING_FEE;
  const discount = calculateVoucherDiscount(selectedVoucher, cartTotal);
  const total = Math.max(cartTotal + shippingFee - discount, 0);

  const handleSelectVoucher = (v: Voucher | null) => {
    setSelectedVoucher(v);
    setVoucherModal(false);
  };

  const handleCheckout = async () => {
    if (!tenNguoiNhan.trim()) {
      Alert.alert('Lỗi', 'Vui lòng nhập tên người nhận');
      return;
    }
    if (!soDienThoai.trim()) {
      Alert.alert('Lỗi', 'Vui lòng nhập số điện thoại');
      return;
    }
    if (!diaChi.trim()) {
      Alert.alert('Lỗi', 'Vui lòng nhập địa chỉ giao hàng');
      return;
    }

    setLoading(true);
    try {
      const request: CheckoutRequest = {
        items: items.map((item) => ({
          idChiTietSanPham: item.idChiTietSanPham,
          soLuong: item.soLuong,
          giaDuKien: item.giaBan,
        })),
        tenNguoiNhan: tenNguoiNhan.trim(),
        soDienThoai: soDienThoai.trim(),
        diaChi: diaChi.trim(),
        idPhieuGiamGia: selectedVoucher?.id,
        phuongThucThanhToan: paymentMethod,
        ghiChu: ghiChu.trim() || undefined,
      };

      const order = await orderService.checkout(request);
      clearCart();

      if (paymentMethod === 'VNPAY') {
        await handleVnPay(order.id);
      } else {
        showSuccess();
      }
    } catch (error: any) {
      const message = error?.response?.data?.message || 'Không thể đặt hàng. Vui lòng thử lại.';
      Alert.alert('Lỗi', message);
    } finally {
      setLoading(false);
    }
  };

  const handleVnPay = async (orderId: string) => {
    try {
      const returnUrl = Linking.createURL('/orders');
      const payUrl = await orderService.createVnPayUrl(orderId, returnUrl);
      await WebBrowser.openBrowserAsync(payUrl);
    } catch {
      Alert.alert(
        'Thanh toán',
        'Đơn hàng đã được tạo nhưng chưa hoàn tất thanh toán VNPay. Bạn có thể thanh toán lại trong chi tiết đơn hàng.'
      );
    } finally {
      router.dismissAll();
      router.push('/orders');
    }
  };

  const showSuccess = () => {
    Alert.alert('🎉 Đặt hàng thành công', 'Đơn hàng của bạn đã được ghi nhận!', [
      {
        text: 'Xem đơn hàng',
        onPress: () => {
          router.dismissAll();
          router.push('/orders');
        },
      },
      {
        text: 'Tiếp tục mua sắm',
        onPress: () => router.dismissAll(),
      },
    ]);
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        <ScrollView
          contentContainerStyle={[styles.scrollContent, { paddingTop: insets.top + Spacing.two }]}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          {/* Header */}
          <View style={styles.header}>
            <Pressable onPress={() => router.back()} hitSlop={12}>
              <Ionicons name="close" size={24} color={theme.text} />
            </Pressable>
            <Text style={[styles.title, { color: theme.text }]}>Đặt hàng</Text>
            <View style={{ width: 24 }} />
          </View>

          {/* Delivery Info */}
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <View style={styles.sectionHeader}>
              <Ionicons name="location-outline" size={20} color={Brand.primary} />
              <Text style={[styles.sectionTitle, { color: theme.text }]}>
                Thông tin giao hàng
              </Text>
            </View>

            <View style={styles.inputGroup}>
              <Text style={[styles.label, { color: theme.textSecondary }]}>Tên người nhận *</Text>
              <TextInput
                style={[styles.input, { color: theme.text, backgroundColor: theme.backgroundElement, borderColor: theme.border }]}
                placeholder="Nguyễn Văn A"
                placeholderTextColor={theme.textTertiary}
                value={tenNguoiNhan}
                onChangeText={setTenNguoiNhan}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={[styles.label, { color: theme.textSecondary }]}>Số điện thoại *</Text>
              <TextInput
                style={[styles.input, { color: theme.text, backgroundColor: theme.backgroundElement, borderColor: theme.border }]}
                placeholder="0901234567"
                placeholderTextColor={theme.textTertiary}
                value={soDienThoai}
                onChangeText={setSoDienThoai}
                keyboardType="phone-pad"
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={[styles.label, { color: theme.textSecondary }]}>Địa chỉ *</Text>
              <TextInput
                style={[
                  styles.input,
                  styles.multilineInput,
                  { color: theme.text, backgroundColor: theme.backgroundElement, borderColor: theme.border },
                ]}
                placeholder="Số nhà, đường, phường, quận, thành phố"
                placeholderTextColor={theme.textTertiary}
                value={diaChi}
                onChangeText={setDiaChi}
                multiline
                numberOfLines={2}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={[styles.label, { color: theme.textSecondary }]}>Ghi chú</Text>
              <TextInput
                style={[
                  styles.input,
                  styles.multilineInput,
                  { color: theme.text, backgroundColor: theme.backgroundElement, borderColor: theme.border },
                ]}
                placeholder="Ghi chú cho đơn hàng (tùy chọn)"
                placeholderTextColor={theme.textTertiary}
                value={ghiChu}
                onChangeText={setGhiChu}
                multiline
                numberOfLines={2}
              />
            </View>
          </View>

          {/* Voucher */}
          <Pressable
            style={[styles.section, styles.voucherRow, { backgroundColor: theme.surface, borderColor: theme.border }]}
            onPress={() => setVoucherModal(true)}
          >
            <Ionicons name="pricetag-outline" size={20} color={Brand.accent} />
            <View style={{ flex: 1 }}>
              <Text style={[styles.sectionTitle, { color: theme.text }]}>Mã giảm giá</Text>
              {selectedVoucher ? (
                <Text style={[styles.voucherSelected, { color: Brand.success }]} numberOfLines={1}>
                  {selectedVoucher.ma} · {formatVoucherValue(selectedVoucher)}
                </Text>
              ) : (
                <Text style={[styles.voucherHint, { color: theme.textTertiary }]}>
                  {vouchers.length > 0 ? `${vouchers.length} mã khả dụng` : 'Không có mã khả dụng'}
                </Text>
              )}
            </View>
            <Ionicons name="chevron-forward" size={18} color={theme.textTertiary} />
          </Pressable>

          {/* Payment Method */}
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <View style={styles.sectionHeader}>
              <Ionicons name="card-outline" size={20} color={Brand.primary} />
              <Text style={[styles.sectionTitle, { color: theme.text }]}>
                Phương thức thanh toán
              </Text>
            </View>

            {[
              { key: 'COD', label: 'Thanh toán khi nhận hàng', icon: 'cash-outline' as const },
              { key: 'VNPAY', label: 'VNPay', icon: 'card-outline' as const },
            ].map((method) => {
              const isActive = paymentMethod === method.key;
              return (
                <Pressable
                  key={method.key}
                  style={[
                    styles.paymentOption,
                    {
                      borderColor: isActive ? Brand.primary : theme.border,
                      backgroundColor: isActive ? Brand.primary + '08' : 'transparent',
                    },
                  ]}
                  onPress={() => setPaymentMethod(method.key)}
                >
                  <Ionicons
                    name={method.icon}
                    size={20}
                    color={isActive ? Brand.primary : theme.textSecondary}
                  />
                  <Text
                    style={[
                      styles.paymentLabel,
                      { color: isActive ? Brand.primary : theme.text },
                    ]}
                  >
                    {method.label}
                  </Text>
                  <View
                    style={[
                      styles.radio,
                      {
                        borderColor: isActive ? Brand.primary : theme.textTertiary,
                      },
                    ]}
                  >
                    {isActive && <View style={styles.radioInner} />}
                  </View>
                </Pressable>
              );
            })}
          </View>

          {/* Order Summary */}
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <View style={styles.sectionHeader}>
              <Ionicons name="receipt-outline" size={20} color={Brand.primary} />
              <Text style={[styles.sectionTitle, { color: theme.text }]}>
                Tổng quan đơn hàng ({items.length} sản phẩm)
              </Text>
            </View>

            <View style={styles.summaryRow}>
              <Text style={[styles.summaryLabel, { color: theme.textSecondary }]}>Tạm tính</Text>
              <Text style={[styles.summaryValue, { color: theme.text }]}>
                {formatCurrency(cartTotal)}
              </Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={[styles.summaryLabel, { color: theme.textSecondary }]}>
                Phí vận chuyển
              </Text>
              <Text style={[styles.summaryValue, { color: shippingFee === 0 ? Brand.success : theme.text }]}>
                {shippingFee === 0 ? 'Miễn phí' : formatCurrency(shippingFee)}
              </Text>
            </View>
            {discount > 0 && (
              <View style={styles.summaryRow}>
                <Text style={[styles.summaryLabel, { color: theme.textSecondary }]}>Giảm giá</Text>
                <Text style={[styles.summaryValue, { color: Brand.success }]}>
                  -{formatCurrency(discount)}
                </Text>
              </View>
            )}
            <View style={[styles.totalRow, { borderTopColor: theme.border }]}>
              <Text style={[styles.totalLabel, { color: theme.text }]}>Tổng cộng</Text>
              <Text style={[styles.totalValue, { color: Brand.primary }]}>
                {formatCurrency(total)}
              </Text>
            </View>
          </View>

          <View style={{ height: 100 }} />
        </ScrollView>
      </KeyboardAvoidingView>

      {/* Bottom Submit */}
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
        <View style={styles.bottomInfo}>
          <Text style={[styles.bottomLabel, { color: theme.textSecondary }]}>Tổng thanh toán</Text>
          <Text style={[styles.bottomTotal, { color: Brand.primary }]}>{formatCurrency(total)}</Text>
        </View>
        <Pressable
          style={({ pressed }) => [
            styles.submitBtn,
            { opacity: loading || pressed ? 0.7 : 1 },
          ]}
          onPress={handleCheckout}
          disabled={loading}
        >
          <Text style={styles.submitText}>
            {loading ? 'Đang xử lý...' : 'Xác nhận đặt hàng'}
          </Text>
        </Pressable>
      </View>

      {/* Voucher Picker Modal */}
      <Modal
        visible={voucherModal}
        transparent
        animationType="slide"
        onRequestClose={() => setVoucherModal(false)}
      >
        <Pressable style={styles.modalOverlay} onPress={() => setVoucherModal(false)}>
          <Pressable
            style={[styles.modalSheet, { backgroundColor: theme.surfaceElevated, paddingBottom: insets.bottom + Spacing.three }]}
            onPress={(e) => e.stopPropagation()}
          >
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: theme.text }]}>Chọn mã giảm giá</Text>
              <Pressable onPress={() => setVoucherModal(false)} hitSlop={12}>
                <Ionicons name="close" size={22} color={theme.textSecondary} />
              </Pressable>
            </View>

            <FlatList
              data={vouchers}
              keyExtractor={(item) => item.id}
              style={{ maxHeight: 360 }}
              ListHeaderComponent={
                <Pressable
                  style={[styles.voucherItem, { borderColor: theme.border }]}
                  onPress={() => handleSelectVoucher(null)}
                >
                  <Ionicons
                    name={selectedVoucher ? 'ellipse-outline' : 'checkmark-circle'}
                    size={20}
                    color={selectedVoucher ? theme.textTertiary : Brand.primary}
                  />
                  <Text style={[styles.voucherItemText, { color: theme.text }]}>Không dùng mã</Text>
                </Pressable>
              }
              renderItem={({ item }) => {
                const active = selectedVoucher?.id === item.id;
                return (
                  <Pressable
                    style={[styles.voucherItem, { borderColor: active ? Brand.primary : theme.border }]}
                    onPress={() => handleSelectVoucher(item)}
                  >
                    <Ionicons
                      name={active ? 'checkmark-circle' : 'ellipse-outline'}
                      size={20}
                      color={active ? Brand.primary : theme.textTertiary}
                    />
                    <View style={{ flex: 1 }}>
                      <Text style={[styles.voucherItemText, { color: theme.text }]}>
                        {item.ma} · {formatVoucherValue(item)}
                      </Text>
                      {item.donHangToiThieu ? (
                        <Text style={[styles.voucherItemCond, { color: theme.textTertiary }]}>
                          Đơn tối thiểu {formatCurrency(item.donHangToiThieu)}
                        </Text>
                      ) : null}
                    </View>
                  </Pressable>
                );
              }}
              ListEmptyComponent={
                <Text style={[styles.voucherEmpty, { color: theme.textSecondary }]}>
                  Không có mã giảm giá khả dụng cho đơn này
                </Text>
              }
            />
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scrollContent: { paddingBottom: 20 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.three,
  },
  title: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  section: {
    marginHorizontal: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
    marginBottom: Spacing.two,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    marginBottom: Spacing.three,
  },
  sectionTitle: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  voucherRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  voucherSelected: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
    marginTop: 2,
  },
  voucherHint: {
    fontSize: FontSizes.xs,
    marginTop: 2,
  },
  inputGroup: {
    marginBottom: Spacing.three,
    gap: Spacing.one + 2,
  },
  label: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  input: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 2,
    fontSize: FontSizes.base,
  },
  multilineInput: {
    minHeight: 60,
    textAlignVertical: 'top',
  },
  paymentOption: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.three,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    marginBottom: Spacing.two,
    gap: Spacing.two,
  },
  paymentLabel: {
    flex: 1,
    fontSize: FontSizes.base,
    fontWeight: FontWeights.medium,
  },
  radio: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
  },
  radioInner: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: Brand.primary,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: Spacing.two,
  },
  summaryLabel: {
    fontSize: FontSizes.sm,
  },
  summaryValue: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    paddingTop: Spacing.two + 2,
    marginTop: Spacing.one,
  },
  totalLabel: {
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
  },
  totalValue: {
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.extrabold,
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    paddingTop: Spacing.three,
    borderTopWidth: 1,
    gap: Spacing.three,
  },
  bottomInfo: {
    flex: 1,
    gap: 2,
  },
  bottomLabel: {
    fontSize: FontSizes.xs,
  },
  bottomTotal: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.extrabold,
  },
  submitBtn: {
    backgroundColor: Brand.primary,
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.lg,
  },
  submitText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'flex-end',
  },
  modalSheet: {
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    padding: Spacing.three,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.three,
  },
  modalTitle: {
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
  },
  voucherItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    padding: Spacing.three,
    marginBottom: Spacing.two,
  },
  voucherItemText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  voucherItemCond: {
    fontSize: FontSizes.xs,
    marginTop: 2,
  },
  voucherEmpty: {
    fontSize: FontSizes.sm,
    textAlign: 'center',
    paddingVertical: Spacing.four,
  },
});
