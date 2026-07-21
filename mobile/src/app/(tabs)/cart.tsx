/**
 * Cart Screen - Shopping bag with items and checkout
 */

import React from 'react';
import { StyleSheet, View, Text, FlatList, Pressable } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useCart } from '@/context/CartContext';
import { useAuth } from '@/context/AuthContext';
import { CartItemCard } from '@/components/CartItemCard';
import { formatCurrency } from '@/utils/format';

export default function CartScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { items, cartCount, cartTotal, isEmpty, clearCart } = useCart();
  const { isAuthenticated } = useAuth();

  const handleCheckout = () => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    router.push('/checkout');
  };

  if (isEmpty) {
    return (
      <View style={[styles.container, { backgroundColor: theme.background, paddingTop: insets.top }]}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.text }]}>Giỏ hàng</Text>
        </View>
        <View style={styles.emptyContainer}>
          <Ionicons name="cart-outline" size={64} color={theme.textTertiary} />
          <Text style={[styles.emptyTitle, { color: theme.text }]}>Giỏ hàng trống</Text>
          <Text style={[styles.emptySubtitle, { color: theme.textSecondary }]}>
            Hãy thêm sản phẩm yêu thích vào giỏ hàng
          </Text>
          <Pressable
            style={({ pressed }) => [styles.shopButton, { opacity: pressed ? 0.8 : 1 }]}
            onPress={() => router.push('/shop' as any)}
          >
            <Text style={styles.shopButtonText}>Mua sắm ngay</Text>
          </Pressable>
        </View>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: theme.background, paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.text }]}>
          Giỏ hàng ({cartCount})
        </Text>
        <Pressable onPress={clearCart}>
          <Text style={[styles.clearText, { color: Brand.error }]}>Xoá tất cả</Text>
        </Pressable>
      </View>

      {/* Cart Items */}
      <FlatList
        data={items}
        keyExtractor={(item) => item.idChiTietSanPham}
        renderItem={({ item, index }) => (
          <Animated.View entering={FadeInDown.delay(index * 80).duration(400)}>
            <CartItemCard item={item} />
          </Animated.View>
        )}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
      />

      {/* Checkout Footer */}
      <View
        style={[
          styles.footer,
          {
            backgroundColor: theme.surface,
            borderTopColor: theme.border,
            paddingBottom: insets.bottom + Spacing.two,
          },
        ]}
      >
        <View style={styles.totalRow}>
          <Text style={[styles.totalLabel, { color: theme.textSecondary }]}>Tạm tính</Text>
          <Text style={[styles.totalPrice, { color: theme.text }]}>{formatCurrency(cartTotal)}</Text>
        </View>
        <Pressable
          style={({ pressed }) => [styles.checkoutButton, { opacity: pressed ? 0.85 : 1 }]}
          onPress={handleCheckout}
        >
          <Ionicons name="card-outline" size={20} color="#FFFFFF" />
          <Text style={styles.checkoutText}>Đặt hàng</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
  },
  title: {
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.bold,
  },
  clearText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  listContent: {
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.four,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.two,
    paddingBottom: Spacing.seven,
  },
  emptyTitle: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
    marginTop: Spacing.two,
  },
  emptySubtitle: {
    fontSize: FontSizes.sm,
    textAlign: 'center',
  },
  shopButton: {
    backgroundColor: Brand.primary,
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.full,
    marginTop: Spacing.three,
  },
  shopButtonText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
  footer: {
    borderTopWidth: 1,
    paddingHorizontal: Spacing.three,
    paddingTop: Spacing.three,
    gap: Spacing.three,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  totalLabel: {
    fontSize: FontSizes.base,
  },
  totalPrice: {
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.bold,
  },
  checkoutButton: {
    flexDirection: 'row',
    backgroundColor: Brand.primary,
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.lg,
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.two,
  },
  checkoutText: {
    color: '#FFFFFF',
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
  },
});
