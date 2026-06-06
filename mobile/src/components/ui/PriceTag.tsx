/**
 * PriceTag Component - displays formatted VND prices
 */

import React from 'react';
import { StyleSheet, View, Text, ViewStyle } from 'react-native';
import { Brand, FontSizes, FontWeights } from '@/constants/theme';
import { formatCurrency, formatPriceRange } from '@/utils/format';
import { useTheme } from '@/hooks/use-theme';

interface PriceTagProps {
  price?: number;
  minPrice?: number;
  maxPrice?: number;
  originalPrice?: number;
  discountPercent?: number;
  size?: 'sm' | 'md' | 'lg';
  style?: ViewStyle;
}

export function PriceTag({
  price,
  minPrice,
  maxPrice,
  originalPrice,
  discountPercent,
  size = 'md',
  style,
}: PriceTagProps) {
  const theme = useTheme();

  const fontSize =
    size === 'lg' ? FontSizes.xl : size === 'md' ? FontSizes.md : FontSizes.sm;
  const smallFontSize =
    size === 'lg' ? FontSizes.sm : size === 'md' ? FontSizes.xs : 10;

  const displayPrice =
    price != null
      ? formatCurrency(price)
      : formatPriceRange(minPrice ?? null, maxPrice ?? null);

  const hasDiscount = discountPercent != null && discountPercent > 0;

  return (
    <View style={[styles.container, style]}>
      <Text
        style={[
          styles.price,
          { fontSize, color: Brand.primary },
        ]}
        numberOfLines={1}
      >
        {displayPrice}
      </Text>

      {hasDiscount && originalPrice != null && (
        <View style={styles.discountRow}>
          <Text
            style={[
              styles.originalPrice,
              { fontSize: smallFontSize, color: theme.textTertiary },
            ]}
          >
            {formatCurrency(originalPrice)}
          </Text>
          <View style={styles.discountBadge}>
            <Text style={styles.discountText}>-{discountPercent}%</Text>
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 2,
  },
  price: {
    fontWeight: FontWeights.bold,
  },
  discountRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  originalPrice: {
    textDecorationLine: 'line-through',
  },
  discountBadge: {
    backgroundColor: Brand.error + '18',
    paddingHorizontal: 5,
    paddingVertical: 1,
    borderRadius: 4,
  },
  discountText: {
    color: Brand.error,
    fontSize: 10,
    fontWeight: FontWeights.bold,
  },
});
