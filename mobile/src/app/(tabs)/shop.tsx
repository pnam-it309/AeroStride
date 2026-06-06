/**
 * Shop Screen - Product Listing with Search & Filter
 */

import React, { useEffect, useState, useCallback } from 'react';
import {
  StyleSheet,
  View,
  Text,
  FlatList,
  Pressable,
  TextInput,
  RefreshControl,
  Dimensions,
  Modal,
  ScrollView,
} from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { productService, type Product, type ProductFilters, type ProductSearchParams } from '@/services/productService';
import { fileService } from '@/services/fileService';
import { formatPriceRange } from '@/utils/format';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const CARD_WIDTH = (SCREEN_WIDTH - Spacing.three * 2 - Spacing.two) / 2;

export default function ShopScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filters, setFilters] = useState<ProductFilters | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [selectedFilters, setSelectedFilters] = useState<ProductSearchParams>({});
  const [sortBy, setSortBy] = useState('ngayTao');
  const [sortDir, setSortDir] = useState('desc');

  const sortProductsForUi = useCallback(
    (items: Product[]) => {
      if (sortBy !== 'giaBanThapNhat') return items;
      return [...items].sort((a, b) => {
        const left = Number(a.giaBanThapNhat ?? 0);
        const right = Number(b.giaBanThapNhat ?? 0);
        return sortDir === 'asc' ? left - right : right - left;
      });
    },
    [sortBy, sortDir]
  );

  const loadProducts = useCallback(
    async (page: number = 0, append: boolean = false) => {
      try {
        const params: ProductSearchParams = {
          page,
          size: 12,
          keyword: keyword || undefined,
          sortBy,
          sortDirection: sortDir,
          ...selectedFilters,
        };
        const data = await productService.getProducts(params);
        const nextProducts = sortProductsForUi(data.content || []);
        setProducts((prev) => (append ? [...prev, ...nextProducts] : nextProducts));
        setTotalPages(data.totalPages || 1);
        setCurrentPage(data.pageNumber ?? page);
      } catch (error) {
        console.warn('Failed to load products:', error);
      } finally {
        setLoading(false);
        setRefreshing(false);
        setLoadingMore(false);
      }
    },
    [keyword, sortBy, sortDir, selectedFilters, sortProductsForUi]
  );

  useEffect(() => {
    setLoading(true);
    loadProducts(1);
  }, [loadProducts]);

  useEffect(() => {
    productService.getFilters().then(setFilters).catch(console.warn);
  }, []);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadProducts(1);
  }, [loadProducts]);

  const onEndReached = useCallback(() => {
    if (!loadingMore && currentPage < totalPages - 1) {
      setLoadingMore(true);
      loadProducts(currentPage + 1, true);
    }
  }, [loadingMore, currentPage, totalPages, loadProducts]);

  const renderProduct = ({ item }: { item: Product }) => {
    const imageSource = fileService.getImageSource(item.hinhAnh);
    return (
      <Pressable
        style={({ pressed }) => [
          styles.productCard,
          { backgroundColor: theme.surface, borderColor: theme.border, opacity: pressed ? 0.9 : 1 },
        ]}
        onPress={() => router.push(`/product/${item.id}`)}
      >
        <View style={[styles.imageContainer, { backgroundColor: theme.backgroundElement }]}>
          {imageSource ? (
            <Image source={imageSource} style={styles.image} contentFit="cover" transition={300} />
          ) : (
            <Text style={{ fontSize: 36 }}>👟</Text>
          )}
        </View>
        <View style={styles.info}>
          <Text style={[styles.brand, { color: theme.textTertiary }]} numberOfLines={1}>
            {item.tenThuongHieu}
          </Text>
          <Text style={[styles.name, { color: theme.text }]} numberOfLines={2}>
            {item.tenSanPham}
          </Text>
          <Text style={[styles.price, { color: Brand.primary }]}>
            {formatPriceRange(item.giaBanThapNhat, item.giaBanCaoNhat)}
          </Text>
        </View>
      </Pressable>
    );
  };

  const sortOptions = [
    { label: 'Mới nhất', sortBy: 'ngayTao', sortDir: 'desc' },
    { label: 'Giá tăng', sortBy: 'giaBanThapNhat', sortDir: 'asc' },
    { label: 'Giá giảm', sortBy: 'giaBanThapNhat', sortDir: 'desc' },
    { label: 'Tên A-Z', sortBy: 'ten', sortDir: 'asc' },
  ];

  return (
    <View style={[styles.container, { backgroundColor: theme.background, paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.text }]}>Sản phẩm</Text>
      </View>

      {/* Search */}
      <View style={styles.searchRow}>
        <View style={[styles.searchInput, { backgroundColor: theme.surfaceElevated, borderColor: theme.border }]}>
          <Ionicons name="search" size={18} color={theme.textTertiary} />
          <TextInput
            style={[styles.searchText, { color: theme.text }]}
            placeholder="Tìm kiếm giày..."
            placeholderTextColor={theme.textTertiary}
            value={keyword}
            onChangeText={setKeyword}
            onSubmitEditing={() => {
              setLoading(true);
              loadProducts(1);
            }}
            returnKeyType="search"
          />
          {keyword.length > 0 && (
            <Pressable onPress={() => { setKeyword(''); }}>
              <Ionicons name="close-circle" size={18} color={theme.textTertiary} />
            </Pressable>
          )}
        </View>
        <Pressable
          style={[styles.filterButton, { backgroundColor: theme.surfaceElevated, borderColor: theme.border }]}
          onPress={() => setShowFilters(true)}
        >
          <Ionicons name="options-outline" size={20} color={theme.text} />
        </Pressable>
      </View>

      {/* Sort pills */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.sortRow}
      >
        {sortOptions.map((opt) => {
          const isActive = sortBy === opt.sortBy && sortDir === opt.sortDir;
          return (
            <Pressable
              key={opt.label}
              style={[
                styles.sortPill,
                {
                  backgroundColor: isActive ? Brand.primary : theme.surfaceElevated,
                  borderColor: isActive ? Brand.primary : theme.border,
                },
              ]}
              onPress={() => {
                setSortBy(opt.sortBy);
                setSortDir(opt.sortDir);
              }}
            >
              <Text
                style={[
                  styles.sortPillText,
                  { color: isActive ? '#FFFFFF' : theme.textSecondary },
                ]}
              >
                {opt.label}
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>

      {/* Product Grid */}
      {loading ? (
        <LoadingSpinner fullScreen />
      ) : (
        <FlatList
          data={products}
          renderItem={renderProduct}
          keyExtractor={(item) => item.id}
          numColumns={2}
          columnWrapperStyle={styles.gridRow}
          contentContainerStyle={styles.gridContainer}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Brand.primary} />
          }
          onEndReached={onEndReached}
          onEndReachedThreshold={0.3}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={{ fontSize: 48 }}>🔍</Text>
              <Text style={[styles.emptyText, { color: theme.textSecondary }]}>
                Không tìm thấy sản phẩm
              </Text>
            </View>
          }
          ListFooterComponent={
            loadingMore ? (
              <View style={styles.loadingMore}>
                <LoadingSpinner size={24} />
              </View>
            ) : null
          }
        />
      )}

      {/* Filter Modal */}
      <Modal visible={showFilters} animationType="slide" transparent>
        <View style={[styles.modalOverlay]}>
          <View style={[styles.modalContent, { backgroundColor: theme.background }]}>
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: theme.text }]}>Bộ lọc</Text>
              <Pressable onPress={() => setShowFilters(false)}>
                <Ionicons name="close" size={24} color={theme.text} />
              </Pressable>
            </View>

            <ScrollView style={styles.modalBody}>
              {filters?.thuongHieus && filters.thuongHieus.length > 0 && (
                <View style={styles.filterSection}>
                  <Text style={[styles.filterLabel, { color: theme.text }]}>Thương hiệu</Text>
                  <View style={styles.filterChips}>
                    {filters.thuongHieus.map((item) => {
                      const isActive = selectedFilters.thuongHieuId === item.id;
                      return (
                        <Pressable
                          key={item.id}
                          style={[
                            styles.filterChip,
                            {
                              backgroundColor: isActive ? Brand.primary + '20' : theme.surfaceElevated,
                              borderColor: isActive ? Brand.primary : theme.border,
                            },
                          ]}
                          onPress={() =>
                            setSelectedFilters((prev) => ({
                              ...prev,
                              thuongHieuId: isActive ? undefined : item.id,
                            }))
                          }
                        >
                          <Text
                            style={[
                              styles.filterChipText,
                              { color: isActive ? Brand.primary : theme.textSecondary },
                            ]}
                          >
                            {item.ten}
                          </Text>
                        </Pressable>
                      );
                    })}
                  </View>
                </View>
              )}

              {filters?.danhMucs && filters.danhMucs.length > 0 && (
                <View style={styles.filterSection}>
                  <Text style={[styles.filterLabel, { color: theme.text }]}>Danh mục</Text>
                  <View style={styles.filterChips}>
                    {filters.danhMucs.map((item) => {
                      const isActive = selectedFilters.danhMucId === item.id;
                      return (
                        <Pressable
                          key={item.id}
                          style={[
                            styles.filterChip,
                            {
                              backgroundColor: isActive ? Brand.primary + '20' : theme.surfaceElevated,
                              borderColor: isActive ? Brand.primary : theme.border,
                            },
                          ]}
                          onPress={() =>
                            setSelectedFilters((prev) => ({
                              ...prev,
                              danhMucId: isActive ? undefined : item.id,
                            }))
                          }
                        >
                          <Text
                            style={[
                              styles.filterChipText,
                              { color: isActive ? Brand.primary : theme.textSecondary },
                            ]}
                          >
                            {item.ten}
                          </Text>
                        </Pressable>
                      );
                    })}
                  </View>
                </View>
              )}
            </ScrollView>

            <View style={[styles.modalFooter, { borderTopColor: theme.border }]}>
              <Pressable
                style={[styles.resetButton, { borderColor: theme.border }]}
                onPress={() => setSelectedFilters({})}
              >
                <Text style={[styles.resetText, { color: theme.textSecondary }]}>Đặt lại</Text>
              </Pressable>
              <Pressable
                style={styles.applyButton}
                onPress={() => {
                  setShowFilters(false);
                  setLoading(true);
                  loadProducts(1);
                }}
              >
                <Text style={styles.applyText}>Áp dụng</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
  },
  title: {
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.bold,
  },
  searchRow: {
    flexDirection: 'row',
    paddingHorizontal: Spacing.three,
    gap: Spacing.two,
    marginBottom: Spacing.two,
  },
  searchInput: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    paddingHorizontal: Spacing.two + 4,
    paddingVertical: Spacing.two,
    gap: Spacing.two,
  },
  searchText: {
    flex: 1,
    fontSize: FontSizes.base,
  },
  filterButton: {
    width: 44,
    height: 44,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sortRow: {
    paddingHorizontal: Spacing.three,
    gap: Spacing.two,
    marginBottom: Spacing.three,
  },
  sortPill: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one + 2,
    borderRadius: BorderRadius.full,
    borderWidth: 1,
  },
  sortPillText: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.medium,
  },
  gridContainer: {
    paddingHorizontal: Spacing.three,
    paddingBottom: 100,
  },
  gridRow: {
    gap: Spacing.two,
    marginBottom: Spacing.two,
  },
  productCard: {
    width: CARD_WIDTH,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
  },
  imageContainer: {
    width: '100%',
    aspectRatio: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  image: { width: '100%', height: '100%' },
  info: {
    padding: Spacing.two + 2,
    gap: 2,
  },
  brand: {
    fontSize: 10,
    fontWeight: FontWeights.medium,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  name: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
    lineHeight: 18,
  },
  price: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
    marginTop: 2,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.seven,
    gap: Spacing.three,
  },
  emptyText: {
    fontSize: FontSizes.base,
  },
  loadingMore: {
    paddingVertical: Spacing.four,
  },
  // Modal
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    maxHeight: '80%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.three,
  },
  modalTitle: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  modalBody: {
    paddingHorizontal: Spacing.three,
  },
  filterSection: {
    marginBottom: Spacing.four,
  },
  filterLabel: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
    marginBottom: Spacing.two,
  },
  filterChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.two,
  },
  filterChip: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one + 2,
    borderRadius: BorderRadius.full,
    borderWidth: 1,
  },
  filterChipText: {
    fontSize: FontSizes.sm,
  },
  modalFooter: {
    flexDirection: 'row',
    padding: Spacing.three,
    gap: Spacing.two,
    borderTopWidth: 1,
  },
  resetButton: {
    flex: 1,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    alignItems: 'center',
  },
  resetText: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
  applyButton: {
    flex: 2,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.lg,
    backgroundColor: Brand.primary,
    alignItems: 'center',
  },
  applyText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
});
