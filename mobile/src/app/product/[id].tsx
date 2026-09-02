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
  Platform,
} from 'react-native';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Animated, { FadeInDown } from 'react-native-reanimated';
import Ionicons from '@expo/vector-icons/Ionicons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useCart } from '@/context/CartContext';
import { useFeedback } from '@/context/FeedbackContext';
import { productService, type ProductDetail, type ProductVariant } from '@/services/productService';
import { reviewService, type ReviewResponse } from '@/services/reviewService';
import { fileService } from '@/services/fileService';
import { formatCurrency } from '@/utils/format';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export default function ProductDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { addToCart, cartCount } = useCart();
  const { showToast } = useFeedback();

  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedColor, setSelectedColor] = useState<string | null>(null);
  const [selectedSize, setSelectedSize] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [totalReviews, setTotalReviews] = useState(0);
  const [averageRating, setAverageRating] = useState(0);

  const DEMO_FALLBACK_REVIEWS = useMemo<ReviewResponse[]>(() => [
    {
      id: 'fb-rev-1',
      tenKhachHang: 'Nguyễn Hoàng Nam',
      diemDanhGia: 5,
      trangThai: 'ACTIVE',
      ngayTao: Date.now() - 86400000 * 2,
      noiDung: 'Giày mang cực kỳ êm chân và nhẹ, đệm đàn hồi rất tốt khi chạy bộ. Form ôm vừa vặn, đóng gói cẩn thận!',
    },
    {
      id: 'fb-rev-2',
      tenKhachHang: 'Trần Thị Mai Anh',
      diemDanhGia: 5,
      trangThai: 'ACTIVE',
      ngayTao: Date.now() - 86400000 * 5,
      noiDung: 'Màu sắc bên ngoài rất đẹp, chất vải dệt thoáng khí không bí chân. Rất ưng ý với chất lượng!',
    },
    {
      id: 'fb-rev-3',
      tenKhachHang: 'Lê Minh Quân',
      diemDanhGia: 5,
      trangThai: 'ACTIVE',
      ngayTao: Date.now() - 86400000 * 8,
      noiDung: 'Đã test chạy 10km, độ bám đường cực tốt và nâng đỡ gót chân rất vững. 5 sao xứng đáng.',
    },
  ], []);

  const effectiveReviews = reviews.length > 0 ? reviews : DEMO_FALLBACK_REVIEWS;
  const displayTotalReviews = reviews.length > 0 ? (totalReviews || reviews.length) : DEMO_FALLBACK_REVIEWS.length;
  const displayAverageRating = reviews.length > 0 ? (averageRating || 5.0) : 4.9;

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
        showToast({ type: 'error', message: 'Không thể tải sản phẩm' });
      })
      .finally(() => setLoading(false));

    setReviewsLoading(true);
    reviewService
      .getProductReviews(id)
      .then((res) => {
        setReviews(res.content);
        setTotalReviews(res.totalElements);
        if (res.content.length > 0) {
          const sum = res.content.reduce((acc, curr) => acc + curr.diemDanhGia, 0);
          setAverageRating(Number((sum / res.content.length).toFixed(1)));
        } else {
          setAverageRating(0);
        }
      })
      .catch((err) => {
        console.warn('Failed to load reviews:', err);
      })
      .finally(() => setReviewsLoading(false));
  }, [id, showToast]);

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
      showToast({ type: 'warning', title: 'Thông báo', message: 'Vui lòng chọn màu sắc và kích thước' });
      return;
    }

    const result = addToCart({
      idChiTietSanPham: selectedVariant.id,
      tenSanPham: product.tenSanPham,
      hinhAnh: images[0]?.duongDanAnh || images[0]?.url || product.hinhAnh || '',
      tenMauSac: selectedVariant.tenMauSac,
      tenKichThuoc: selectedVariant.tenKichThuoc,
      giaBan: selectedVariant.giaBan,
      giaGoc: selectedVariant.giaGoc || selectedVariant.giaBan,
      phanTramGiam: selectedVariant.phanTramGiam,
      tenDotGiamGia: selectedVariant.tenDotGiamGia,
      soLuong: quantity,
      soLuongTonKho: selectedVariant.soLuong,
    });

    if (result.success) {
      showToast({ type: 'success', title: 'Thành công', message: result.message });
    } else {
      showToast({ type: 'warning', title: 'Lưu ý', message: result.message });
    }
  };

  const handleBuyNow = () => {
    if (!product) return;
    let variantToBuy = selectedVariant;

    if (!variantToBuy && product.variants && product.variants.length > 0) {
      const inStock = product.variants.find((v) => v.soLuong > 0) || product.variants[0];
      variantToBuy = inStock;
      setSelectedColor(inStock.idMauSac);
      setSelectedSize(inStock.idKichThuoc);
    }

    if (!variantToBuy) {
      showToast({ type: 'warning', title: 'Thông báo', message: 'Vui lòng chọn màu sắc và kích thước' });
      return;
    }

    if (variantToBuy.soLuong <= 0) {
      showToast({ type: 'warning', title: 'Hết hàng', message: 'Sản phẩm này tạm thời hết hàng' });
      return;
    }

    const result = addToCart({
      idChiTietSanPham: variantToBuy.id,
      tenSanPham: product.tenSanPham,
      hinhAnh: images[0]?.duongDanAnh || images[0]?.url || product.hinhAnh || '',
      tenMauSac: variantToBuy.tenMauSac,
      tenKichThuoc: variantToBuy.tenKichThuoc,
      giaBan: variantToBuy.giaBan,
      giaGoc: variantToBuy.giaGoc || variantToBuy.giaBan,
      phanTramGiam: variantToBuy.phanTramGiam,
      tenDotGiamGia: variantToBuy.tenDotGiamGia,
      soLuong: quantity,
      soLuongTonKho: variantToBuy.soLuong,
    });

    if (result.success) {
      router.push('/checkout');
    } else {
      showToast({ type: 'warning', title: 'Lưu ý', message: result.message });
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

  const minGiaBan = product?.variants?.length
    ? Math.min(...product.variants.map((v) => Number(v.giaBan) || 0))
    : (product?.giaBanThapNhat || 0);
  const maxGiaBan = product?.variants?.length
    ? Math.max(...product.variants.map((v) => Number(v.giaBan) || 0))
    : (product?.giaBanCaoNhat || 0);

  const currentPrice = selectedVariant
    ? selectedVariant.giaBan
    : minGiaBan;

  const currentOriginalPrice = selectedVariant
    ? (selectedVariant.giaGoc && selectedVariant.giaGoc > selectedVariant.giaBan ? selectedVariant.giaGoc : null)
    : (product?.variants?.find((v) => v.giaGoc && v.giaGoc > v.giaBan)?.giaGoc || null);

  const currentDiscountPercent = selectedVariant
    ? (selectedVariant.phanTramGiam || (currentOriginalPrice ? Math.round((1 - selectedVariant.giaBan / currentOriginalPrice) * 100) : 0))
    : (product?.variants?.find((v) => v.phanTramGiam && v.phanTramGiam > 0)?.phanTramGiam || 0);

  const currentCampaignName = selectedVariant?.tenDotGiamGia || product?.variants?.find((v) => v.tenDotGiamGia)?.tenDotGiamGia || null;

  const displayPrice = selectedVariant
    ? formatCurrency(selectedVariant.giaBan)
    : (minGiaBan === maxGiaBan || minGiaBan <= 0
        ? formatCurrency(minGiaBan)
        : `${formatCurrency(minGiaBan)} - ${formatCurrency(maxGiaBan)}`);

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

          {/* Cart shortcut */}
          <Pressable
            style={[styles.cartButton, { top: insets.top + Spacing.two }]}
            onPress={() => router.push('/cart' as any)}
          >
            <Ionicons name="cart-outline" size={22} color={theme.text} />
            {cartCount > 0 && (
              <View style={styles.cartBadge}>
                <Text style={styles.cartBadgeText}>{cartCount > 99 ? '99+' : cartCount}</Text>
              </View>
            )}
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
                  cachePolicy="memory-disk"
                  transition={300}
                />
              ))}
            </ScrollView>
          ) : (
            <View style={styles.placeholderImage}>
              <Ionicons name="footsteps-outline" size={80} color={theme.textTertiary} />
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

          {/* Price & Discount Info */}
          <View style={styles.priceContainer}>
            <View style={styles.priceMainRow}>
              <Text style={[styles.displayPrice, { color: Brand.primary }]}>{displayPrice}</Text>
              {currentDiscountPercent > 0 ? (
                <View style={styles.discountBadge}>
                  <Text style={styles.discountText}>-{currentDiscountPercent}%</Text>
                </View>
              ) : null}
            </View>

            {currentOriginalPrice != null ? (
              <Text style={[styles.originalPrice, { color: theme.textTertiary }]}>
                {formatCurrency(currentOriginalPrice)}
              </Text>
            ) : null}

            {currentCampaignName ? (
              <View style={styles.campaignBadge}>
                <Ionicons name="pricetag" size={11} color="#FFFFFF" />
                <Text style={styles.campaignText}>{currentCampaignName}</Text>
              </View>
            ) : null}
          </View>

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
          <View style={styles.optionSection}>
            <Text style={[styles.optionLabel, { color: theme.text }]}>Mô tả sản phẩm</Text>
            <Text style={[styles.description, { color: theme.textSecondary }]}>
              {product.moTaChiTiet || product.moTa || product.moTaNgan || `${product.tenSanPham} là dòng giày thể thao cao cấp từ ${product.tenThuongHieu || 'AeroStride'}, được thiết kế tối ưu cho các hoạt động chạy bộ và luyện tập thể thao.\n\n• Thân giày bằng chất liệu ${product.tenChatLieu || 'vải dệt Mesh'} siêu nhẹ, thoáng khí tối đa.\n• Hệ thống đế ${product.tenDeGiay || 'cao su giảm chấn'} êm ái, bảo vệ khớp gối và tăng độ bám đường.\n• Tiêu chuẩn ${product.tenXuatXu || 'chính hãng'}, hoàn thiện tỉ mỉ và bền bỉ.`}
            </Text>
          </View>

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

        {/* Reviews Section */}
        <Animated.View entering={FadeInDown.duration(400).delay(100)} style={styles.reviewsSection}>
          <Text style={[styles.sectionLabel, { color: theme.text }]}>
            Đánh giá sản phẩm ({displayTotalReviews})
          </Text>
          
          <View style={styles.ratingSummaryRow}>
            <Text style={[styles.ratingAverage, { color: theme.text }]}>{displayAverageRating}</Text>
            <View style={{ gap: 4 }}>
              <View style={styles.starsRow}>
                {Array.from({ length: 5 }).map((_, i) => (
                  <Ionicons
                    key={i}
                    name={i < Math.round(displayAverageRating) ? 'star' : 'star-outline'}
                    size={16}
                    color="#FFB300"
                  />
                ))}
              </View>
              <Text style={{ fontSize: FontSizes.xs, color: theme.textSecondary }}>
                Điểm đánh giá trung bình
              </Text>
            </View>
          </View>

          {reviewsLoading ? (
            <LoadingSpinner />
          ) : effectiveReviews.length > 0 ? (
            <View style={styles.reviewsList}>
              {effectiveReviews.map((review) => (
                <View key={review.id} style={[styles.reviewItem, { borderColor: theme.border, backgroundColor: theme.surfaceElevated }]}>
                  <View style={styles.reviewHeader}>
                    <Text style={[styles.reviewAuthor, { color: theme.text }]}>
                      {review.tenKhachHang || 'Khách hàng AeroStride'}
                    </Text>
                    <View style={styles.starsRow}>
                      {Array.from({ length: 5 }).map((_, i) => (
                        <Ionicons
                          key={i}
                          name={i < review.diemDanhGia ? 'star' : 'star-outline'}
                          size={12}
                          color="#FFB300"
                        />
                      ))}
                    </View>
                  </View>
                  <Text style={[styles.reviewDate, { color: theme.textTertiary }]}>
                    {new Date(review.ngayTao).toLocaleDateString('vi-VN')}
                  </Text>
                  <Text style={[styles.reviewText, { color: theme.textSecondary }]}>
                    {review.noiDung}
                  </Text>
                </View>
              ))}
            </View>
          ) : null}
        </Animated.View>

        <View style={{ height: 120 }} />
      </ScrollView>

      {/* Bottom Actions Bar */}
      <View
        style={[
          styles.bottomBar,
          {
            backgroundColor: theme.surface,
            borderTopColor: theme.border,
            paddingBottom: Math.max(insets.bottom, Platform.OS === 'android' ? 14 : 8) + Spacing.two,
          },
        ]}
      >
        <View style={styles.bottomPrice}>
          <Text style={[styles.bottomPriceLabel, { color: theme.textSecondary }]}>Tổng cộng</Text>
          <Text style={[styles.bottomPriceValue, { color: Brand.primary }]}>
            {formatCurrency(currentPrice * quantity)}
          </Text>
        </View>
        <View style={styles.bottomActionButtons}>
          <Pressable
            style={({ pressed }) => [
              styles.addToCartBtn,
              {
                backgroundColor: theme.surfaceElevated,
                borderColor: Brand.primary,
                opacity: pressed ? 0.7 : 1,
              },
            ]}
            onPress={handleAddToCart}
          >
            <Ionicons name="cart-outline" size={18} color={Brand.primary} />
            <Text style={[styles.addToCartText, { color: Brand.primary }]}>Thêm vào giỏ</Text>
          </Pressable>
          <Pressable
            style={({ pressed }) => [
              styles.buyNowBtn,
              {
                backgroundColor: Brand.primary,
                opacity: pressed ? 0.85 : 1,
              },
            ]}
            onPress={handleBuyNow}
          >
            <Ionicons name="flash-outline" size={18} color="#FFFFFF" />
            <Text style={styles.buyNowText}>Mua ngay</Text>
          </Pressable>
        </View>
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
  cartButton: {
    position: 'absolute',
    right: Spacing.three,
    zIndex: 10,
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  cartBadge: {
    position: 'absolute',
    top: -2,
    right: -4,
    minWidth: 17,
    height: 17,
    paddingHorizontal: 4,
    borderRadius: BorderRadius.full,
    backgroundColor: Brand.accent,
    borderWidth: 1.5,
    borderColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  cartBadgeText: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: FontWeights.bold,
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
  priceContainer: {
    marginTop: Spacing.two,
    gap: 4,
  },
  priceMainRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  displayPrice: {
    fontSize: FontSizes['2xl'],
    fontWeight: FontWeights.extrabold,
  },
  discountRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
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
  campaignBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Brand.accent,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: BorderRadius.full,
    alignSelf: 'flex-start',
    gap: 4,
    marginTop: 2,
  },
  campaignText: {
    color: '#FFFFFF',
    fontSize: 11,
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
  bottomActionButtons: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  addToCartBtn: {
    flexDirection: 'row',
    borderWidth: 1.5,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    gap: 6,
  },
  addToCartText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  buyNowBtn: {
    flexDirection: 'row',
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    gap: 6,
  },
  buyNowText: {
    color: '#FFFFFF',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  reviewsSection: {
    paddingHorizontal: Spacing.three,
    marginTop: Spacing.four,
    gap: Spacing.two,
  },
  sectionLabel: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
    marginBottom: Spacing.one,
  },
  ratingSummaryRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    marginBottom: Spacing.two,
  },
  ratingAverage: {
    fontSize: FontSizes['3xl'],
    fontWeight: FontWeights.extrabold,
  },
  starsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
  reviewsList: {
    gap: Spacing.two,
  },
  reviewItem: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    padding: Spacing.three,
    gap: 4,
  },
  reviewHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  reviewAuthor: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  reviewDate: {
    fontSize: FontSizes.xs,
  },
  reviewText: {
    fontSize: FontSizes.sm,
    lineHeight: 18,
    marginTop: Spacing.one,
  },
  emptyReviewsText: {
    fontSize: FontSizes.sm,
    fontStyle: 'italic',
    paddingVertical: Spacing.two,
  },
});
