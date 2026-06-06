/**
 * Product Detail Screen - with variant selection
 */

import React, { useEffect, useState, useMemo } from 'react';
import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  Pressable,
  Dimensions,
  Alert,
} from 'react-native';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useCart } from '@/context/CartContext';
import { productService, type ProductDetail, type ProductVariant } from '@/services/productService';
import { fileService } from '@/services/fileService';
import { formatCurrency } from '@/utils/format';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export default function ProductDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { addToCart } = useCart();

  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedColor, setSelectedColor] = useState<string | null>(null);
  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  useEffect(() => {
    if (!id) return;
    productService
      .getProductDetail(id)
      .then((data) => {
        setProduct(data);
        // Auto-select first available color
        const colors = [...new Set(data.variants?.map((v) => v.idMauSac))];
        if (colors.length > 0) setSelectedColor(colors[0]);
      })
      .catch((err) => {
        console.warn('Failed to load product:', err);
        Alert.alert('Lỗi', 'Không thể tải sản phẩm');
      })
      .finally(() => setLoading(false));
  }, [id]);

  // Unique colors from variants
  const availableColors = useMemo(() => {
    if (!product?.variants) return [];
    const colorMap = new Map<string, { id: string; name: string; hex: string }>();
    product.variants.forEach((v) => {
      if (v.idMauSac && !colorMap.has(v.idMauSac)) {
        colorMap.set(v.idMauSac, { id: v.idMauSac, name: v.tenMauSac, hex: v.maMauHex });
      }
    });
    return Array.from(colorMap.values());
  }, [product]);

  // Sizes available for selected color
  const availableSizes = useMemo(() => {
    if (!product?.variants || !selectedColor) return [];
    const sizeMap = new Map<string, { id: string; name: string; value: string }>();
    product.variants
      .filter((v) => v.idMauSac === selectedColor && v.soLuong > 0)
      .forEach((v) => {
        if (v.idKichThuoc && !sizeMap.has(v.idKichThuoc)) {
          sizeMap.set(v.idKichThuoc, {
            id: v.idKichThuoc,
            name: v.tenKichThuoc,
            value: v.giaTriKichThuoc,
          });
        }
      });
    return Array.from(sizeMap.values());
  }, [product, selectedColor]);

  // Selected variant
  const selectedVariant = useMemo((): ProductVariant | null => {
    if (!product?.variants || !selectedColor || !selectedSize) return null;
    return (
      product.variants.find(
        (v) => v.idMauSac === selectedColor && v.idKichThuoc === selectedSize
      ) ?? null
    );
  }, [product, selectedColor, selectedSize]);

  // Images for current selection
  const images = useMemo(() => {
    if (!product?.variants) return [];
    const colorVariants = selectedColor
      ? product.variants.filter((v) => v.idMauSac === selectedColor)
      : product.variants;
    const allImages = colorVariants.flatMap((v) => v.images || []);
    if (allImages.length === 0 && product.hinhAnh) {
      return [{ id: 'main', duongDanAnh: product.hinhAnh, hinhAnhDaiDien: true }];
    }
    const seen = new Set<string>();
    return allImages
      .sort(
        (a, b) =>
          Number(Boolean(b.hinhAnhDaiDien || b.isMain)) -
          Number(Boolean(a.hinhAnhDaiDien || a.isMain))
      )
      .filter((image) => {
        const imageUrl = image.duongDanAnh || image.url || '';
        if (!imageUrl || seen.has(imageUrl)) return false;
        seen.add(imageUrl);
        return true;
      });
  }, [product, selectedColor]);

  const handleAddToCart = () => {
    if (!selectedVariant || !product) {
      Alert.alert('Thông báo', 'Vui lòng chọn màu sắc và kích thước');
      return;
    }

    const result = addToCart({
      idChiTietSanPham: selectedVariant.id,
      tenSanPham: product.tenSanPham,
      hinhAnh: images[0]?.duongDanAnh || images[0]?.url || product.hinhAnh || '',
      tenMauSac: selectedVariant.tenMauSac,
      tenKichThuoc: selectedVariant.tenKichThuoc,
      giaBan: selectedVariant.giaBan,
      soLuong: quantity,
      soLuongTonKho: selectedVariant.soLuong,
    });

    if (result.success) {
      Alert.alert('✅ Thành công', result.message);
    } else {
      Alert.alert('⚠️ Lưu ý', result.message);
    }
  };

  if (loading) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.background }]}>
        <LoadingSpinner fullScreen />
      </View>
    );
  }

  if (!product) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.background }]}>
        <Text style={{ color: theme.textSecondary, fontSize: FontSizes.base }}>
          Không tìm thấy sản phẩm
        </Text>
      </View>
    );
  }

  const displayPrice = selectedVariant
    ? formatCurrency(selectedVariant.giaBan)
    : 'Chọn phân loại';

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScrollView showsVerticalScrollIndicator={false}>
        {/* Image Gallery */}
        <View style={[styles.imageSection, { backgroundColor: theme.backgroundElement }]}>
          <Pressable
            style={[styles.backButton, { top: insets.top + Spacing.two }]}
            onPress={() => router.back()}
          >
            <Ionicons name="arrow-back" size={22} color={theme.text} />
          </Pressable>

          {images.length > 0 ? (
            <ScrollView
              horizontal
              pagingEnabled
              showsHorizontalScrollIndicator={false}
              onMomentumScrollEnd={(e) => {
                const idx = Math.round(e.nativeEvent.contentOffset.x / SCREEN_WIDTH);
                setCurrentImageIndex(idx);
              }}
            >
              {images.map((img, idx) => (
                <Image
                  key={img.id || idx}
                  source={fileService.getImageSource(img.duongDanAnh || img.url)}
                  style={{ width: SCREEN_WIDTH, height: SCREEN_WIDTH }}
                  contentFit="cover"
                  transition={300}
                />
              ))}
            </ScrollView>
          ) : (
            <View style={styles.placeholderImage}>
              <Text style={{ fontSize: 80 }}>👟</Text>
            </View>
          )}

          {/* Image dots */}
          {images.length > 1 && (
            <View style={styles.imageDots}>
              {images.map((_, idx) => (
                <View
                  key={idx}
                  style={[
                    styles.dot,
                    {
                      backgroundColor:
                        idx === currentImageIndex ? Brand.primary : 'rgba(255,255,255,0.4)',
                    },
                  ]}
                />
              ))}
            </View>
          )}
        </View>

        {/* Product Info */}
        <Animated.View entering={FadeInDown.duration(400)} style={styles.infoSection}>
          <View style={styles.brandRow}>
            <Text style={[styles.brandName, { color: Brand.primary }]}>
              {product.tenThuongHieu}
            </Text>
            <Text style={[styles.productCode, { color: theme.textTertiary }]}>
              {product.maSanPham}
            </Text>
          </View>

          <Text style={[styles.productName, { color: theme.text }]}>{product.tenSanPham}</Text>

          <Text style={[styles.displayPrice, { color: Brand.primary }]}>{displayPrice}</Text>

          {/* Color Selection */}
          {availableColors.length > 0 && (
            <View style={styles.optionSection}>
              <Text style={[styles.optionLabel, { color: theme.text }]}>
                Màu sắc: <Text style={{ color: theme.textSecondary }}>
                  {availableColors.find((c) => c.id === selectedColor)?.name || ''}
                </Text>
              </Text>
              <View style={styles.colorOptions}>
                {availableColors.map((color) => (
                  <Pressable
                    key={color.id}
                    style={[
                      styles.colorSwatch,
                      {
                        backgroundColor: color.hex || '#CCC',
                        borderColor:
                          selectedColor === color.id ? Brand.primary : 'transparent',
                        borderWidth: selectedColor === color.id ? 2.5 : 0,
                      },
                    ]}
                    onPress={() => {
                      setSelectedColor(color.id);
                      setSelectedSize(null);
                      setCurrentImageIndex(0);
                    }}
                  />
                ))}
              </View>
            </View>
          )}

          {/* Size Selection */}
          {availableSizes.length > 0 && (
            <View style={styles.optionSection}>
              <Text style={[styles.optionLabel, { color: theme.text }]}>Kích thước</Text>
              <View style={styles.sizeOptions}>
                {availableSizes.map((size) => {
                  const isActive = selectedSize === size.id;
                  return (
                    <Pressable
                      key={size.id}
                      style={[
                        styles.sizeChip,
                        {
                          backgroundColor: isActive ? Brand.primary : theme.surfaceElevated,
                          borderColor: isActive ? Brand.primary : theme.border,
                        },
                      ]}
                      onPress={() => setSelectedSize(size.id)}
                    >
                      <Text
                        style={[
                          styles.sizeText,
                          { color: isActive ? '#FFFFFF' : theme.text },
                        ]}
                      >
                        {size.value || size.name}
                      </Text>
                    </Pressable>
                  );
                })}
              </View>
            </View>
          )}

          {/* Quantity */}
          <View style={styles.optionSection}>
            <Text style={[styles.optionLabel, { color: theme.text }]}>Số lượng</Text>
            <View style={[styles.quantityRow, { borderColor: theme.border }]}>
              <Pressable
                style={styles.qtyBtn}
                onPress={() => setQuantity(Math.max(1, quantity - 1))}
              >
                <Ionicons name="remove" size={18} color={theme.text} />
              </Pressable>
              <Text style={[styles.qtyValue, { color: theme.text }]}>{quantity}</Text>
              <Pressable
                style={styles.qtyBtn}
                onPress={() =>
                  setQuantity(Math.min(selectedVariant?.soLuong ?? 99, quantity + 1))
                }
              >
                <Ionicons name="add" size={18} color={theme.text} />
              </Pressable>
            </View>
            {selectedVariant && (
              <Text style={[styles.stockInfo, { color: theme.textTertiary }]}>
                Còn {selectedVariant.soLuong} sản phẩm
              </Text>
            )}
          </View>

          {/* Description */}
          {product.moTaNgan && (
            <View style={styles.optionSection}>
              <Text style={[styles.optionLabel, { color: theme.text }]}>Mô tả</Text>
              <Text style={[styles.description, { color: theme.textSecondary }]}>
                {product.moTaNgan}
              </Text>
            </View>
          )}

          {/* Product details */}
          <View style={[styles.detailsCard, { backgroundColor: theme.surfaceElevated, borderColor: theme.border }]}>
            {[
              { label: 'Danh mục', value: product.tenDanhMuc },
              { label: 'Chất liệu', value: product.tenChatLieu },
              { label: 'Đế giày', value: product.tenDeGiay },
              { label: 'Cổ giày', value: product.tenCoGiay },
              { label: 'Xuất xứ', value: product.tenXuatXu },
            ]
              .filter((d) => d.value)
              .map((detail, idx) => (
                <View
                  key={detail.label}
                  style={[
                    styles.detailRow,
                    idx > 0 && { borderTopWidth: 1, borderTopColor: theme.borderLight },
                  ]}
                >
                  <Text style={[styles.detailLabel, { color: theme.textSecondary }]}>
                    {detail.label}
                  </Text>
                  <Text style={[styles.detailValue, { color: theme.text }]}>
                    {detail.value}
                  </Text>
                </View>
              ))}
          </View>
        </Animated.View>

        <View style={{ height: 120 }} />
      </ScrollView>

      {/* Bottom Add to Cart */}
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
        <View style={styles.bottomPrice}>
          <Text style={[styles.bottomPriceLabel, { color: theme.textSecondary }]}>Tổng</Text>
          <Text style={[styles.bottomPriceValue, { color: Brand.primary }]}>
            {selectedVariant ? formatCurrency(selectedVariant.giaBan * quantity) : '—'}
          </Text>
        </View>
        <Pressable
          style={({ pressed }) => [
            styles.addToCartBtn,
            { opacity: !selectedVariant || pressed ? 0.7 : 1 },
          ]}
          onPress={handleAddToCart}
          disabled={!selectedVariant}
        >
          <Ionicons name="bag-add-outline" size={20} color="#FFFFFF" />
          <Text style={styles.addToCartText}>Thêm vào giỏ</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  imageSection: {
    width: SCREEN_WIDTH,
    height: SCREEN_WIDTH,
    position: 'relative',
  },
  backButton: {
    position: 'absolute',
    left: Spacing.three,
    zIndex: 10,
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderImage: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  imageDots: {
    flexDirection: 'row',
    position: 'absolute',
    bottom: Spacing.three,
    alignSelf: 'center',
    gap: 6,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  infoSection: {
    padding: Spacing.three,
    gap: Spacing.one,
  },
  brandRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  brandName: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  productCode: {
    fontSize: FontSizes.xs,
  },
  productName: {
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.bold,
    lineHeight: 30,
    marginTop: Spacing.one,
  },
  displayPrice: {
    fontSize: FontSizes['2xl'],
    fontWeight: FontWeights.extrabold,
    marginTop: Spacing.two,
  },
  discountRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 2,
  },
  originalPrice: {
    fontSize: FontSizes.base,
    textDecorationLine: 'line-through',
  },
  discountBadge: {
    backgroundColor: Brand.error + '18',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  discountText: {
    color: Brand.error,
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.bold,
  },
  optionSection: {
    marginTop: Spacing.four,
    gap: Spacing.two,
  },
  optionLabel: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
  colorOptions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.two + 2,
  },
  colorSwatch: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },
  sizeOptions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.two,
  },
  sizeChip: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    minWidth: 52,
    alignItems: 'center',
  },
  sizeText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
  },
  quantityRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    alignSelf: 'flex-start',
  },
  qtyBtn: {
    padding: Spacing.two + 2,
  },
  qtyValue: {
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
    minWidth: 36,
    textAlign: 'center',
  },
  stockInfo: {
    fontSize: FontSizes.xs,
    marginTop: 2,
  },
  description: {
    fontSize: FontSizes.sm,
    lineHeight: 22,
  },
  detailsCard: {
    marginTop: Spacing.four,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
  },
  detailLabel: {
    fontSize: FontSizes.sm,
  },
  detailValue: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
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
  bottomPrice: {
    flex: 1,
    gap: 2,
  },
  bottomPriceLabel: {
    fontSize: FontSizes.xs,
  },
  bottomPriceValue: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.extrabold,
  },
  addToCartBtn: {
    flexDirection: 'row',
    backgroundColor: Brand.primary,
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    gap: Spacing.two,
  },
  addToCartText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
});
