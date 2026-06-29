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
  Platform,
} from 'react-native';
import { BlurView } from 'expo-blur';
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
      {/* Hero Section with abstract shapes */}
      <View style={[styles.hero, { paddingTop: insets.top + Spacing.three, backgroundColor: theme.surface }]}>
        {/* Background glow elements */}
        <View style={styles.glowCircle1} />
        <View style={styles.glowCircle2} />

        {/* Header bar */}
        <View style={styles.headerBar}>
          <View>
            <Text style={[styles.heroBrand, { color: theme.text }]}>AeroStride</Text>
            <Text style={[styles.heroGreeting, { color: theme.textSecondary }]}>Khám phá phong cách của bạn</Text>
          </View>
          <Pressable
            onPress={() => router.push('/cart' as any)}
            style={({ pressed }) => [styles.cartButton, { opacity: pressed ? 0.7 : 1, backgroundColor: theme.surfaceElevated, borderColor: theme.border, borderWidth: 1 }]}
          >
            <Ionicons name="bag-handle-outline" size={24} color={theme.text} />
            {cartCount > 0 && (
              <View style={styles.cartBadge}>
                <Text style={styles.cartBadgeText}>{cartCount}</Text>
              </View>
            )}
          </Pressable>
        </View>

        {/* Search bar */}
        <Pressable
          style={[styles.searchBar, { backgroundColor: theme.backgroundElement, borderColor: theme.border }]}
          onPress={() => router.push('/shop' as any)}
        >
          <Ionicons name="search-outline" size={20} color={theme.textTertiary} />
          <Text style={[styles.searchPlaceholder, { color: theme.textSecondary }]}>Tìm kiếm giày mơ ước...</Text>
          <View style={styles.searchFilterBtn}>
            <Ionicons name="options-outline" size={16} color="#FFFFFF" />
          </View>
        </Pressable>

        {/* Hero Banner Card */}
        <Animated.View entering={FadeInUp.delay(200).duration(600)}>
          <LinearGradient
            colors={['rgba(32, 138, 239, 0.8)', 'rgba(32, 138, 239, 0.5)']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={[styles.heroBannerCard, { borderWidth: 1, borderColor: theme.border }]}
          >
            <View style={styles.heroBannerContent}>
              <View style={styles.heroBannerBadgeContainer}>
                <Text style={styles.heroBannerBadge}>MỚI RA MẮT</Text>
              </View>
              <Text style={[styles.heroTitle, { color: '#FFFFFF' }]}>Bộ Sưu Tập{'\n'}Thể Thao 2026</Text>
              <Pressable
                style={({ pressed }) => [styles.heroButton, { opacity: pressed ? 0.8 : 1 }]}
                onPress={() => router.push('/shop' as any)}
              >
                <Text style={styles.heroButtonText}>Khám phá ngay</Text>
                <Ionicons name="arrow-forward" size={16} color="#1E293B" />
              </Pressable>
            </View>
            <Ionicons name="flash" size={160} color="rgba(255,255,255,0.2)" style={styles.heroBannerIcon} />
          </LinearGradient>
        </Animated.View>
      </View>

      <View style={styles.mainContent}>
        {/* Categories */}
        <Animated.View entering={FadeInDown.delay(300).duration(500)} style={styles.section}>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.categoriesContainer}
          >
            {[
              { icon: 'footsteps-outline', label: 'Giày chạy', key: 'chay' },
              { icon: 'business-outline', label: 'Giày da', key: 'da' },
              { icon: 'basketball-outline', label: 'Thể thao', key: 'thethao' },
              { icon: 'color-palette-outline', label: 'Thời trang', key: 'thoitrang' },
              { icon: 'snow-outline', label: 'Boots', key: 'boots' },
            ].map((cat) => (
              <Pressable
                key={cat.key}
                style={({ pressed }) => [
                  styles.categoryItem,
                  { opacity: pressed ? 0.7 : 1 },
                ]}
                onPress={() => router.push('/shop' as any)}
              >
                <View style={[styles.categoryIconContainer, { backgroundColor: theme.surface, borderWidth: 1, borderColor: theme.border }]}>
                  <Ionicons name={cat.icon as any} size={24} color={Brand.primary} />
                </View>
                <Text style={[styles.categoryLabel, { color: theme.textSecondary }]}>{cat.label}</Text>
              </Pressable>
            ))}
          </ScrollView>
        </Animated.View>

        {/* Featured Products - Horizontal Scroll */}
        {featured.length > 0 && (
          <Animated.View entering={FadeInDown.delay(400).duration(500)} style={styles.section}>
            <View style={styles.sectionHeader}>
              <Text style={[styles.sectionTitle, { color: theme.text }]}>Sản phẩm nổi bật</Text>
              <Pressable onPress={() => router.push('/shop' as any)}>
                <Text style={[styles.seeAll, { color: Brand.primary }]}>Xem thêm</Text>
              </Pressable>
            </View>

            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.horizontalList}
              snapToInterval={172} // width + gap
              decelerationRate="fast"
            >
              {featured.map((product) => (
                <Pressable
                  key={product.id}
                  style={({ pressed }) => [
                    styles.featuredCard,
                    { backgroundColor: theme.surface, borderColor: theme.border, borderWidth: 1, opacity: pressed ? 0.95 : 1 },
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
                      <Ionicons name="footsteps-outline" size={40} color="#CBD5E1" />
                    )}
                    <Pressable style={[styles.favoriteBtn, { backgroundColor: theme.surfaceElevated, borderColor: theme.border, borderWidth: 1 }]}>
                      <Ionicons name="heart-outline" size={18} color="#94A3B8" />
                    </Pressable>
                  </View>
                  <View style={styles.featuredInfo}>
                    <Text style={[styles.featuredBrand, { color: Brand.primary }]} numberOfLines={1}>
                      {product.tenThuongHieu}
                    </Text>
                    <Text style={[styles.featuredName, { color: theme.text }]} numberOfLines={1}>
                      {product.tenSanPham}
                    </Text>
                    <Text style={[styles.featuredPrice, { color: theme.text }]}>
                      {formatPriceRange(product.giaBanThapNhat, product.giaBanCaoNhat)}
                    </Text>
                  </View>
                </Pressable>
              ))}
            </ScrollView>
          </Animated.View>
        )}

        {/* Trending Products - Grid */}
        {trending.length > 0 && (
          <Animated.View entering={FadeInDown.delay(500).duration(500)} style={[styles.section, { paddingBottom: Spacing.six }]}>
            <View style={styles.sectionHeader}>
              <Text style={[styles.sectionTitle, { color: theme.text }]}>Xu hướng mới</Text>
            </View>

            <View style={styles.trendingGrid}>
              {trending.slice(0, 4).map((product) => (
                <Pressable
                  key={product.id}
                  style={({ pressed }) => [
                    styles.trendingCard,
                    { backgroundColor: theme.surface, borderColor: theme.border, borderWidth: 1, opacity: pressed ? 0.95 : 1 },
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
                      <Ionicons name="footsteps-outline" size={36} color="#CBD5E1" />
                    )}
                    <Pressable style={[styles.favoriteBtn, { backgroundColor: theme.surfaceElevated, borderColor: theme.border, borderWidth: 1 }]}>
                      <Ionicons name="heart-outline" size={16} color="#94A3B8" />
                    </Pressable>
                  </View>
                  <View style={styles.trendingInfo}>
                    <Text style={[styles.trendingBrand, { color: Brand.primary }]} numberOfLines={1}>
                      {product.tenThuongHieu}
                    </Text>
                    <Text style={[styles.trendingName, { color: theme.text }]} numberOfLines={2}>
                      {product.tenSanPham}
                    </Text>
                    <Text style={[styles.trendingPrice, { color: theme.text }]}>
                      {formatPriceRange(product.giaBanThapNhat, product.giaBanCaoNhat)}
                    </Text>
                  </View>
                </Pressable>
              ))}
            </View>
          </Animated.View>
        )}
      </View>
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
  mainContent: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    marginTop: -16,
  },
  // Hero
  hero: {
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.six,
    borderBottomLeftRadius: 24,
    borderBottomRightRadius: 24,
    position: 'relative',
    overflow: 'hidden',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.05,
        shadowRadius: 10,
      },
      android: {
        elevation: 4,
      },
    }),
  },
  glowCircle1: {
    position: 'absolute',
    top: -50,
    right: -20,
    width: 250,
    height: 250,
    borderRadius: 125,
    backgroundColor: 'rgba(32, 138, 239, 0.12)',
  },
  glowCircle2: {
    position: 'absolute',
    top: 100,
    left: -80,
    width: 200,
    height: 200,
    borderRadius: 100,
    backgroundColor: 'rgba(32, 138, 239, 0.08)',
  },
  headerBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.four,
  },
  heroGreeting: {
    color: '#64748B',
    fontSize: FontSizes.sm,
    marginTop: 2,
  },
  heroBrand: {
    fontSize: FontSizes['2xl'],
    fontWeight: FontWeights.extrabold,
    letterSpacing: -0.5,
  },
  cartButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 8,
      },
      android: {
        elevation: 2,
      },
    }),
  },
  cartBadge: {
    position: 'absolute',
    top: -2,
    right: -2,
    backgroundColor: Brand.error,
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 4,
    borderWidth: 2,
    borderColor: '#FFFFFF',
  },
  cartBadgeText: {
    color: '#FFFFFF',
    fontSize: 9,
    fontWeight: FontWeights.bold,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.three,
    gap: Spacing.two,
    marginBottom: Spacing.four,
    borderWidth: 1,
  },
  searchPlaceholder: {
    flex: 1,
    color: '#94A3B8',
    fontSize: FontSizes.sm,
  },
  searchFilterBtn: {
    backgroundColor: Brand.primary,
    width: 28,
    height: 28,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
  },
  heroBannerCard: {
    borderRadius: 20,
    padding: Spacing.four,
    position: 'relative',
    overflow: 'hidden',
    minHeight: 160,
  },
  heroBannerContent: {
    zIndex: 1,
    width: '60%',
  },
  heroBannerBadgeContainer: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    alignSelf: 'flex-start',
    marginBottom: Spacing.two,
  },
  heroBannerBadge: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: FontWeights.bold,
    letterSpacing: 1,
  },
  heroTitle: {
    color: '#FFFFFF',
    fontSize: 22,
    fontWeight: FontWeights.extrabold,
    lineHeight: 30,
    letterSpacing: -0.5,
  },
  heroButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    alignSelf: 'flex-start',
    paddingHorizontal: Spacing.three,
    paddingVertical: 8,
    borderRadius: BorderRadius.full,
    gap: 6,
    marginTop: Spacing.three,
  },
  heroButtonText: {
    color: '#1E293B',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  heroBannerIcon: {
    position: 'absolute',
    right: -20,
    bottom: -30,
    transform: [{ rotate: '-15deg' }],
  },
  // Sections
  section: {
    marginTop: Spacing.five,
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
    letterSpacing: -0.5,
  },
  seeAll: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
  },
  // Categories
  categoriesContainer: {
    paddingHorizontal: Spacing.three,
    gap: Spacing.four,
    paddingTop: Spacing.one,
  },
  categoryItem: {
    alignItems: 'center',
    gap: 8,
  },
  categoryIconContainer: {
    width: 56,
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 8,
      },
      android: {
        elevation: 2,
      },
    }),
  },
  categoryLabel: {
    fontSize: 11,
    fontWeight: FontWeights.medium,
  },
  // Featured horizontal
  horizontalList: {
    paddingHorizontal: Spacing.three,
    gap: Spacing.three,
    paddingBottom: Spacing.two,
  },
  featuredCard: {
    width: 160,
    borderRadius: 16,
    overflow: 'hidden',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.08,
        shadowRadius: 12,
      },
      android: {
        elevation: 3,
      },
    }),
  },
  featuredImageContainer: {
    width: '100%',
    height: 160,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  featuredImage: {
    width: '100%',
    height: '100%',
  },
  favoriteBtn: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(255,255,255,0.9)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  featuredInfo: {
    padding: Spacing.three,
    gap: 4,
  },
  featuredBrand: {
    fontSize: 10,
    fontWeight: FontWeights.bold,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  featuredName: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
  },
  featuredPrice: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
    marginTop: 2,
  },
  // Trending grid
  trendingGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: Spacing.three,
    gap: Spacing.three,
  },
  trendingCard: {
    width: (SCREEN_WIDTH - Spacing.three * 2 - Spacing.three) / 2,
    borderRadius: 16,
    overflow: 'hidden',
    marginBottom: Spacing.two,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.08,
        shadowRadius: 12,
      },
      android: {
        elevation: 3,
      },
    }),
  },
  trendingImageContainer: {
    width: '100%',
    aspectRatio: 1,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  trendingImage: {
    width: '100%',
    height: '100%',
  },
  trendingInfo: {
    padding: Spacing.three,
    gap: 4,
  },
  trendingBrand: {
    fontSize: 10,
    fontWeight: FontWeights.bold,
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

