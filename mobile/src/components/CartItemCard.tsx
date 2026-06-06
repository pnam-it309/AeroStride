/**
 * CartItemCard - cart item with quantity controls
 */

import React from 'react';
import { StyleSheet, View, Text, Pressable } from 'react-native';
import { Image } from 'expo-image';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { formatCurrency } from '@/utils/format';
import { fileService } from '@/services/fileService';
import { useTheme } from '@/hooks/use-theme';
import { useCart, CartItem } from '@/context/CartContext';

interface CartItemCardProps {
  item: CartItem;
}

export function CartItemCard({ item }: CartItemCardProps) {
  const theme = useTheme();
  const { updateQuantity, removeFromCart } = useCart();
  const imageSource = fileService.getImageSource(item.hinhAnh);

  return (
    <View style={[styles.card, { backgroundColor: theme.surface, borderColor: theme.border }]}>
      <View style={[styles.imageContainer, { backgroundColor: theme.backgroundElement }]}>
        {imageSource ? (
          <Image source={imageSource} style={styles.image} contentFit="cover" transition={200} />
        ) : (
          <View style={styles.placeholder}>
            <Text style={{ fontSize: 28 }}>👟</Text>
          </View>
        )}
      </View>

      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={[styles.name, { color: theme.text }]} numberOfLines={2}>
            {item.tenSanPham}
          </Text>
          <Pressable
            onPress={() => removeFromCart(item.idChiTietSanPham)}
            hitSlop={8}
            style={({ pressed }) => ({ opacity: pressed ? 0.5 : 1 })}
          >
            <Ionicons name="close-circle" size={22} color={theme.textTertiary} />
          </Pressable>
        </View>

        {(item.tenMauSac || item.tenKichThuoc) && (
          <Text style={[styles.variant, { color: theme.textSecondary }]}>
            {[item.tenMauSac, item.tenKichThuoc].filter(Boolean).join(' · ')}
          </Text>
        )}

        <View style={styles.footer}>
          <Text style={[styles.price, { color: Brand.primary }]}>
            {formatCurrency(item.giaBan)}
          </Text>

          <View style={[styles.quantityControl, { borderColor: theme.border }]}>
            <Pressable
              onPress={() => updateQuantity(item.idChiTietSanPham, item.soLuong - 1)}
              style={({ pressed }) => [styles.qtyButton, { opacity: pressed ? 0.5 : 1 }]}
            >
              <Ionicons name="remove" size={16} color={theme.text} />
            </Pressable>
            <Text style={[styles.qtyText, { color: theme.text }]}>{item.soLuong}</Text>
            <Pressable
              onPress={() => updateQuantity(item.idChiTietSanPham, item.soLuong + 1)}
              style={({ pressed }) => [styles.qtyButton, { opacity: pressed ? 0.5 : 1 }]}
            >
              <Ionicons name="add" size={16} color={theme.text} />
            </Pressable>
          </View>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
    marginBottom: Spacing.two,
  },
  imageContainer: {
    width: 100,
    height: 100,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  placeholder: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    flex: 1,
    padding: Spacing.two + 2,
    gap: 4,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 8,
  },
  name: {
    flex: 1,
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
    lineHeight: 18,
  },
  variant: {
    fontSize: FontSizes.xs,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 'auto',
  },
  price: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  quantityControl: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.sm,
  },
  qtyButton: {
    padding: 6,
  },
  qtyText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
    minWidth: 28,
    textAlign: 'center',
  },
});
