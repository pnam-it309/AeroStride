/**
 * Checkout Screen - Order placement form
 */

import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  ScrollView,
  Pressable,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useCart } from '@/context/CartContext';
import { orderService, type CheckoutRequest } from '@/services/orderService';
import { formatCurrency } from '@/utils/format';

export default function CheckoutScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { items, cartTotal, clearCart } = useCart();

  const [tenNguoiNhan, setTenNguoiNhan] = useState('');
  const [soDienThoai, setSoDienThoai] = useState('');
  const [diaChi, setDiaChi] = useState('');
  const [ghiChu, setGhiChu] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const [loading, setLoading] = useState(false);

  const shippingFee = 30000;
  const total = cartTotal + shippingFee;

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
        })),
        tenNguoiNhan: tenNguoiNhan.trim(),
        soDienThoai: soDienThoai.trim(),
        diaChi: diaChi.trim(),
        phuongThucThanhToan: paymentMethod,
        ghiChu: ghiChu.trim() || undefined,
      };

      await orderService.checkout(request);
      clearCart();
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
    } catch (error: any) {
      const message = error?.response?.data?.message || 'Không thể đặt hàng. Vui lòng thử lại.';
      Alert.alert('Lỗi', message);
    } finally {
      setLoading(false);
    }
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
              <Text style={[styles.summaryValue, { color: theme.text }]}>
                {formatCurrency(shippingFee)}
              </Text>
            </View>
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
});
