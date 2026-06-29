/**
 * ProductCard - grid card for product listing
 */

import React from 'react';
import { StyleSheet, View, Text, Pressable, Dimensions } from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Brand, Colors, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { PriceTag } from '@/components/ui/PriceTag';
import { truncateText } from '@/utils/format';
import { fileService } from '@/services/fileService';
import { useTheme } from '@/hooks/use-theme';
import type { Product } from '@/services/productService';

const SCREEN_WIDTH = Dimensions.get('window').width;
const CARD_GAP = Spacing.two;
const CARD_PADDING = Spacing.three;
const CARD_WIDTH = (SCREEN_WIDTH - CARD_PADDING * 2 - CARD_GAP) / 2;

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const router = useRouter();
  const theme = useTheme();

  const imageSource = fileService.getImageSource(product.hinhAnh);

  return (
    <Pressable
      style={({ pressed }) => [
        styles.card,
        {
          backgroundColor: theme.surface,
          borderColor: theme.border,
          opacity: pressed ? 0.92 : 1,
          transform: [{ scale: pressed ? 0.97 : 1 }],
        },
      ]}
      onPress={() => router.push(`/product/${product.id}`)}
    >
      <View style={[styles.imageContainer, { backgroundColor: theme.backgroundElement }]}>
        {imageSource ? (
          <Image
            source={imageSource}
            style={styles.image}
            contentFit="cover"
            transition={300}
          />
        ) : (
          <View style={styles.placeholderImage}>
            <Ionicons name="footsteps-outline" size={40} color={theme.textTertiary} />
          </View>
        )}

        {product.tongSoLuongTon != null && product.tongSoLuongTon <= 5 && product.tongSoLuongTon > 0 && (
          <View style={styles.lowStockBadge}>
            <Text style={styles.lowStockText}>Sắp hết</Text>
          </View>
        )}
      </View>

      <View style={styles.info}>
        <Text style={[styles.brand, { color: theme.textTertiary }]} numberOfLines={1}>
          {product.tenThuongHieu}
        </Text>
        <Text style={[styles.name, { color: theme.text }]} numberOfLines={2}>
          {truncateText(product.tenSanPham, 40)}
        </Text>
        <PriceTag
          minPrice={product.giaBanThapNhat}
          maxPrice={product.giaBanCaoNhat}
          size="sm"
        />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    width: CARD_WIDTH,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
    marginBottom: Spacing.two,
  },
  imageContainer: {
    width: '100%',
    aspectRatio: 1,
    position: 'relative',
  },
  image: {
    width: '100%',
    height: '100%',
  },
  placeholderImage: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    fontSize: 40,
  },
  lowStockBadge: {
    position: 'absolute',
    top: Spacing.two,
    left: Spacing.two,
    backgroundColor: Brand.warning + 'DD',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: BorderRadius.sm,
  },
  lowStockText: {
    color: '#FFFFFF',
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.semibold,
  },
  info: {
    padding: Spacing.two + 2,
    gap: 3,
  },
  brand: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.medium,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  name: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
    lineHeight: 18,
  },
});
