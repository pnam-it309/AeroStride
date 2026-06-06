/**
 * Home Screen - Hero + Featured Products
 */

import React, { useEffect, useState, useCallback } from 'react';
import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  Pressable,
  RefreshControl,
  Dimensions,
} from 'react-native';
import { Image } from 'expo-image';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Animated, { FadeInDown, FadeInUp } from 'react-native-reanimated';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useCart } from '@/context/CartContext';
import { productService, type LandingProduct, type Product } from '@/services/productService';
import { fileService } from '@/services/fileService';
import { formatPriceRange } from '@/utils/format';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export default function HomeScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { cartCount } = useCart();
  const [featured, setFeatured] = useState<LandingProduct[]>([]);
  const [trending, setTrending] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const [featuredData, trendingData] = await Promise.all([
        productService.getLandingProducts(6),
        productService.getProducts({ page: 0, size: 8, sortBy: 'ngayTao', sortDirection: 'desc' }),
      ]);
      setFeatured(featuredData || []);
      setTrending(trendingData?.content || []);
    } catch (error) {
      console.warn('Failed to load home data:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  }, [loadData]);

  if (loading) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.background }]}>
        <LoadingSpinner fullScreen />
      </View>
    );
  }

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: theme.background }]}
      showsVerticalScrollIndicator={false}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Brand.primary} />
      }
    >
      {/* Hero Section */}
      <LinearGradient
        colors={['#0B1120', '#152238', '#1E3A5F']}
        style={[styles.hero, { paddingTop: insets.top + Spacing.three }]}
      >
        {/* Header bar */}
        <View style={styles.headerBar}>
          <View>
            <Text style={styles.heroGreeting}>Xin chào 👋</Text>
            <Text style={styles.heroBrand}>AeroStride</Text>
          </View>
          <Pressable
            onPress={() => router.push('/cart' as any)}
            style={({ pressed }) => [styles.cartButton, { opacity: pressed ? 0.7 : 1 }]}
          >
            <Ionicons name="bag-outline" size={24} color="#FFFFFF" />
            {cartCount > 0 && (
              <View style={styles.cartBadge}>
                <Text style={styles.cartBadgeText}>{cartCount}</Text>
              </View>
            )}
          </Pressable>
        </View>

        {/* Search bar */}
        <Pressable
          style={styles.searchBar}
          onPress={() => router.push('/shop' as any)}
        >
          <Ionicons name="search" size={18} color="#94A3B8" />
          <Text style={styles.searchPlaceholder}>Tìm kiếm giày...</Text>
        </Pressable>

        {/* Hero Banner */}
        <Animated.View entering={FadeInUp.delay(200).duration(600)} style={styles.heroBanner}>
          <Text style={styles.heroTitle}>Bộ Sưu Tập{'\n'}Mới Nhất</Text>
          <Text style={styles.heroSubtitle}>
            Khám phá những đôi giày hot nhất{'\n'}với giá ưu đãi đặc biệt
          </Text>
          <Pressable
            style={({ pressed }) => [styles.heroButton, { opacity: pressed ? 0.8 : 1 }]}
            onPress={() => router.push('/shop' as any)}
          >
            <Text style={styles.heroButtonText}>Khám phá ngay</Text>
            <Ionicons name="arrow-forward" size={16} color="#FFFFFF" />
          </Pressable>
        </Animated.View>
      </LinearGradient>

      {/* Featured Products - Horizontal Scroll */}
      {featured.length > 0 && (
        <Animated.View entering={FadeInDown.delay(300).duration(500)} style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={[styles.sectionTitle, { color: theme.text }]}>⭐ Nổi bật</Text>
            <Pressable onPress={() => router.push('/shop' as any)}>
              <Text style={[styles.seeAll, { color: Brand.primary }]}>Xem tất cả</Text>
            </Pressable>
          </View>

          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.horizontalList}
          >
            {featured.map((product, index) => (
              <Pressable
                key={product.id}
                style={({ pressed }) => [
                  styles.featuredCard,
                  { backgroundColor: theme.surface, borderColor: theme.border, opacity: pressed ? 0.9 : 1 },
                ]}
                onPress={() => router.push(`/product/${product.id}`)}
              >
                <View style={[styles.featuredImageContainer, { backgroundColor: theme.backgroundElement }]}>
                  {product.hinhAnh ? (
                    <Image
                      source={fileService.getImageSource(product.hinhAnh)}
                      style={styles.featuredImage}
                      contentFit="cover"
                      transition={300}
                    />
                  ) : (
                    <Text style={{ fontSize: 40 }}>👟</Text>
                  )}
                </View>
                <View style={styles.featuredInfo}>
                  <Text style={[styles.featuredBrand, { color: theme.textTertiary }]} numberOfLines={1}>
                    {product.tenThuongHieu}
                  </Text>
                  <Text style={[styles.featuredName, { color: theme.text }]} numberOfLines={2}>
                    {product.tenSanPham}
                  </Text>
                  <Text style={[styles.featuredPrice, { color: Brand.primary }]}>
                    {formatPriceRange(product.giaBanThapNhat, product.giaBanCaoNhat)}
                  </Text>
                </View>
              </Pressable>
            ))}
          </ScrollView>
        </Animated.View>
      )}

      {/* Categories */}
      <Animated.View entering={FadeInDown.delay(400).duration(500)} style={styles.section}>
        <Text style={[styles.sectionTitle, { color: theme.text, paddingHorizontal: Spacing.three }]}>
          📁 Danh mục
        </Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.categoriesContainer}
        >
          {[
            { icon: '👟', label: 'Giày chạy', key: 'chay' },
            { icon: '👞', label: 'Giày da', key: 'da' },
            { icon: '🏀', label: 'Thể thao', key: 'thethao' },
            { icon: '👠', label: 'Thời trang', key: 'thoitrang' },
            { icon: '🥾', label: 'Boots', key: 'boots' },
          ].map((cat) => (
            <Pressable
              key={cat.key}
              style={({ pressed }) => [
                styles.categoryChip,
                { backgroundColor: theme.surfaceElevated, borderColor: theme.border, opacity: pressed ? 0.7 : 1 },
              ]}
              onPress={() => router.push('/shop' as any)}
            >
              <Text style={styles.categoryIcon}>{cat.icon}</Text>
              <Text style={[styles.categoryLabel, { color: theme.text }]}>{cat.label}</Text>
            </Pressable>
          ))}
        </ScrollView>
      </Animated.View>

      {/* Trending Products - Grid */}
      {trending.length > 0 && (
        <Animated.View entering={FadeInDown.delay(500).duration(500)} style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={[styles.sectionTitle, { color: theme.text }]}>🔥 Xu hướng</Text>
            <Pressable onPress={() => router.push('/shop' as any)}>
              <Text style={[styles.seeAll, { color: Brand.primary }]}>Xem tất cả</Text>
            </Pressable>
          </View>

          <View style={styles.trendingGrid}>
            {trending.slice(0, 4).map((product) => (
              <Pressable
                key={product.id}
                style={({ pressed }) => [
                  styles.trendingCard,
                  { backgroundColor: theme.surface, borderColor: theme.border, opacity: pressed ? 0.9 : 1 },
                ]}
                onPress={() => router.push(`/product/${product.id}`)}
              >
                <View style={[styles.trendingImageContainer, { backgroundColor: theme.backgroundElement }]}>
                  {product.hinhAnh ? (
                    <Image
                      source={fileService.getImageSource(product.hinhAnh)}
                      style={styles.trendingImage}
                      contentFit="cover"
                      transition={300}
                    />
                  ) : (
                    <Text style={{ fontSize: 36 }}>👟</Text>
                  )}
                </View>
                <View style={styles.trendingInfo}>
                  <Text style={[styles.trendingBrand, { color: theme.textTertiary }]} numberOfLines={1}>
                    {product.tenThuongHieu}
                  </Text>
                  <Text style={[styles.trendingName, { color: theme.text }]} numberOfLines={2}>
                    {product.tenSanPham}
                  </Text>
                  <Text style={[styles.trendingPrice, { color: Brand.primary }]}>
                    {formatPriceRange(product.giaBanThapNhat, product.giaBanCaoNhat)}
                  </Text>
                </View>
              </Pressable>
            ))}
          </View>
        </Animated.View>
      )}

      {/* Bottom spacer */}
      <View style={{ height: 100 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  loadingContainer: {
    flex: 1,
  },
  // Hero
  hero: {
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.five,
  },
  headerBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.three,
  },
  heroGreeting: {
    color: '#94A3B8',
    fontSize: FontSizes.sm,
  },
  heroBrand: {
    color: '#FFFFFF',
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.extrabold,
    letterSpacing: -0.5,
  },
  cartButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255,255,255,0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  cartBadge: {
    position: 'absolute',
    top: -2,
    right: -2,
    backgroundColor: Brand.error,
    borderRadius: 8,
    minWidth: 16,
    height: 16,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 3,
  },
  cartBadgeText: {
    color: '#FFFFFF',
    fontSize: 9,
    fontWeight: FontWeights.bold,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
    gap: Spacing.two,
    marginBottom: Spacing.four,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.06)',
  },
  searchPlaceholder: {
    color: '#64748B',
    fontSize: FontSizes.base,
  },
  heroBanner: {
    gap: Spacing.two,
  },
  heroTitle: {
    color: '#FFFFFF',
    fontSize: FontSizes['3xl'],
    fontWeight: FontWeights.extrabold,
    lineHeight: 44,
    letterSpacing: -1,
  },
  heroSubtitle: {
    color: '#94A3B8',
    fontSize: FontSizes.base,
    lineHeight: 22,
  },
  heroButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Brand.primary,
    alignSelf: 'flex-start',
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.full,
    gap: Spacing.two,
    marginTop: Spacing.two,
  },
  heroButtonText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
  // Sections
  section: {
    marginTop: Spacing.four,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    marginBottom: Spacing.three,
  },
  sectionTitle: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  seeAll: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
  },
  // Featured horizontal
  horizontalList: {
    paddingHorizontal: Spacing.three,
    gap: Spacing.two + 2,
  },
  featuredCard: {
    width: 160,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
  },
  featuredImageContainer: {
    width: '100%',
    height: 140,
    justifyContent: 'center',
    alignItems: 'center',
  },
  featuredImage: {
    width: '100%',
    height: '100%',
  },
  featuredInfo: {
    padding: Spacing.two + 2,
    gap: 2,
  },
  featuredBrand: {
    fontSize: 10,
    fontWeight: FontWeights.medium,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  featuredName: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
    lineHeight: 18,
  },
  featuredPrice: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
    marginTop: 2,
  },
  // Categories
  categoriesContainer: {
    paddingHorizontal: Spacing.three,
    gap: Spacing.two,
    paddingTop: Spacing.two,
  },
  categoryChip: {
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    gap: 6,
    minWidth: 80,
  },
  categoryIcon: {
    fontSize: 24,
  },
  categoryLabel: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.medium,
  },
  // Trending grid
  trendingGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: Spacing.three,
    gap: Spacing.two,
  },
  trendingCard: {
    width: (SCREEN_WIDTH - Spacing.three * 2 - Spacing.two) / 2,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
  },
  trendingImageContainer: {
    width: '100%',
    aspectRatio: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  trendingImage: {
    width: '100%',
    height: '100%',
  },
  trendingInfo: {
    padding: Spacing.two + 2,
    gap: 2,
  },
  trendingBrand: {
    fontSize: 10,
    fontWeight: FontWeights.medium,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  trendingName: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
    lineHeight: 18,
  },
  trendingPrice: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
    marginTop: 2,
  },
});
