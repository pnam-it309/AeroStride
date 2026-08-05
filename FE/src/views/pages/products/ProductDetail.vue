<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import PromotionBar from '@/components/shared/PromotionBar.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import api from '@/services/apiService';
import { useCartStore } from '@/stores/cartStore';
import { useToastStore } from '@/stores/toastStore';
import { useAuthStore } from '@/stores/authStore';
import { useSeoMeta } from '@/composables/useSeoMeta';
import { PATH } from '@/router/routePaths';
import { dichVuFile } from '@/services/core/dichVuFile';
import defaultShoeImg from '@/assets/images/products/s4.jpg';

const DEFAULT_SHOE_IMAGE = defaultShoeImg;

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();
const toastStore = useToastStore();
const authStore = useAuthStore();
const loading = ref(true);
const product = ref(null);
const selectedColor = ref(null);
const selectedSize = ref(null);
const selectedQuantity = ref(1);
const activeSlide = ref(0);
const isFavorite = ref(false);
const addingToCart = ref(false);
const recommendedProducts = ref([]);
const reviews = ref([]);
const totalReviews = ref(0);
const averageRating = ref(0);
const reviewsLoading = ref(false);
const ratingCounts = ref({ 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 });
const selectedFilter = ref('all');

// State cho modal đánh giá trực tiếp
const showReviewModal = ref(false);
const newReview = ref({ rating: 5, comment: '' });
const submittingReview = ref(false);

const handleWriteReview = () => {
    if (!authStore.isLoggedIn) {
        toastStore.showToast('Vui lòng đăng nhập để đánh giá sản phẩm', 'warning');
        router.push(PATH.LOGIN);
        return;
    }
    // Mở modal viết bình luận
    showReviewModal.value = true;
};

const submitDirectReview = async () => {
    if (!newReview.value.comment.trim()) {
        toastStore.showToast('Vui lòng nhập nội dung đánh giá', 'warning');
        return;
    }

    submittingReview.value = true;
    try {
        const payload = {
            idHoaDon: null,
            idSanPham: product.value.id,
            idKhachHang: authStore.user?.id,
            diemDanhGia: newReview.value.rating,
            noiDung: newReview.value.comment
        };

        const response = await api.post('/customer/review/submit', payload);
        if (response.data?.success || response.status === 200) {
            toastStore.showToast('Cảm ơn bạn đã đánh giá sản phẩm!', 'success');
            showReviewModal.value = false;
            newReview.value.comment = '';
            newReview.value.rating = 5;
            fetchReviews(); // Reload reviews
        } else {
            toastStore.showToast(response.data?.message || 'Có lỗi xảy ra', 'error');
        }
    } catch (error) {
        console.error('Lỗi khi gửi đánh giá:', error);
        toastStore.showToast(error.response?.data?.message || 'Có lỗi xảy ra khi gửi đánh giá', 'error');
    } finally {
        submittingReview.value = false;
    }
};

const fetchProduct = async () => {
    loading.value = true;
    try {
        const data = await dichVuSanPhamPublic.layChiTietSanPham(route.params.id);
        product.value = data;
    } catch (error) {
        console.error('Error fetching product:', error);
    } finally {
        loading.value = false;
    }
};

const fetchRecommendations = async () => {
    try {
        const data = await dichVuSanPhamPublic.layDanhSachSanPham({ page: 1, size: 4 });
        recommendedProducts.value = data.content || [];
    } catch (error) {
        console.error('Error fetching recommendations:', error);
    }
};

const fetchReviews = async () => {
    reviewsLoading.value = true;
    try {
        const res = await api.get(`/customer/review/product/${route.params.id}`);
        if (res.data?.success && res.data.data) {
            reviews.value = res.data.data.content || [];
            totalReviews.value = res.data.data.totalElements || 0;

            // Tính toán breakdown
            const counts = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
            let sum = 0;

            if (reviews.value.length > 0) {
                reviews.value.forEach(r => {
                    if (r.rating >= 1 && r.rating <= 5) {
                        counts[Math.floor(r.rating)]++;
                    }
                    sum += r.rating;
                });
                averageRating.value = (sum / reviews.value.length).toFixed(1);
            }
            ratingCounts.value = counts;
        }
    } catch (error) {
        console.error('Error fetching reviews:', error);
    } finally {
        reviewsLoading.value = false;
    }
};

const { setProductSeo } = useSeoMeta();

onMounted(() => {
    fetchProduct();
    fetchRecommendations();
    fetchReviews();
});

watch(
    () => route.params.id,
    (newId) => {
        if (newId) {
            selectedColor.value = null;
            selectedSize.value = null;
            selectedQuantity.value = 1;
            activeSlide.value = 0;
            fetchProduct();
            fetchRecommendations();
            fetchReviews();
        }
    }
);

// Cập nhật SEO và trạng thái Yêu thích khi product data load xong
watch(product, (newProduct) => {
    if (newProduct) {
        setProductSeo(newProduct);
        let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
        isFavorite.value = favorites.includes(newProduct.id);
    }
});

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const isAbsoluteUrl = (v) => 
    typeof v !== 'string' || 
    /^(https?:)?\/\//i.test(v) || 
    v.startsWith('data:') || 
    v.startsWith('blob:') || 
    (v.startsWith('/') && !v.startsWith('/uploads/'));

const isInvalidImage = (v) => {
    if (!v || typeof v !== 'string') return true;
    const lower = v.toLowerCase();
    return (
        lower.includes('via.placeholder.com') || 
        lower.includes('placeholder.com') || 
        lower.includes('dummyimage.com')
    );
};

const resolveImg = (v) => {
    if (!v || isInvalidImage(v)) return '';
    if (typeof v !== 'string') return v;
    if (isAbsoluteUrl(v)) return v;
    return dichVuFile.layUrlFile(v);
};

const getValidImgUrl = (raw) => {
    const resolved = resolveImg(raw);
    return (resolved && !isInvalidImage(resolved)) ? resolved : null;
};

const handleImgError = (e) => {
    if (!e || !e.target) return;
    if (e.target.getAttribute('data-fallback') === 'true') return;
    e.target.setAttribute('data-fallback', 'true');
    e.target.src = DEFAULT_SHOE_IMAGE;
};

const colors = computed(() => product.value?.availableColors || []);

const sizes = computed(() => {
    if (!product.value?.availableSizesByColor || !selectedColor.value) return [];
    return product.value.availableSizesByColor[selectedColor.value] || [];
});

watch(selectedColor, () => {
    selectedSize.value = null; // Reset size when color changes
});

const colorHexMap = computed(() => {
    const map = {};
    if (product.value?.variants) {
        product.value.variants.forEach(v => {
            if (v.tenMauSac && v.maMauHex) {
                map[v.tenMauSac] = v.maMauHex;
            }
        });
    }
    const defaultHex = {
        'Đen': '#0B1329',
        'Trắng': '#FFFFFF',
        'Đỏ': '#E53935',
        'Xanh dương': '#1976D2',
        'Xanh lá': '#4CAF50',
        'Vàng': '#FFEB3B',
        'Xám': '#9E9E9E',
        'Hồng': '#E91E63',
        'Cam': '#FF9800'
    };
    return { ...defaultHex, ...map };
});

const discountPercent = computed(() => {
    if (selectedVariant.value && selectedVariant.value.phanTramGiam) {
        const val = Number(selectedVariant.value.phanTramGiam);
        if (val > 0 && val < 100) return val;
        if (val > 0 && selectedVariant.value.giaBan) {
            return Math.round((val / selectedVariant.value.giaBan) * 100);
        }
    }
    return 0;
});

const currentPrice = computed(() => {
    if (selectedVariant.value) {
        const basePrice = selectedVariant.value.giaBan || 0;
        const discountAmt = Number(selectedVariant.value.phanTramGiam) || 0;
        if (discountAmt > 0) {
            if (discountAmt < 100) {
                return basePrice * (1 - discountAmt / 100);
            } else {
                return basePrice - discountAmt;
            }
        }
        return basePrice;
    }
    return product.value?.minPrice || 0;
});

const oldPrice = computed(() => {
    if (selectedVariant.value && Number(selectedVariant.value.phanTramGiam) > 0) {
        return selectedVariant.value.giaBan;
    }
    return null;
});

const formattedCurrentPrice = computed(() => formatPrice(currentPrice.value));
const formattedOldPrice = computed(() => oldPrice.value ? formatPrice(oldPrice.value) : null);

const deliveryDateText = computed(() => {
    const today = new Date();
    const fromDate = new Date(today);
    fromDate.setDate(today.getDate() + 2);
    const toDate = new Date(today);
    toDate.setDate(today.getDate() + 4);
    
    const formatDayMonth = (date) => {
        const d = String(date.getDate()).padStart(2, '0');
        const m = String(date.getMonth() + 1).padStart(2, '0');
        return `${d}/${m}`;
    };
    
    return `Còn hàng — Giao dự kiến ${formatDayMonth(fromDate)}–${formatDayMonth(toDate)}`;
});

const buyNow = async () => {
    if (!product.value) return;

    if (!selectedColor.value) {
        toastStore.showToast('Vui lòng chọn màu sắc', 'warning');
        return;
    }

    if (!selectedSize.value) {
        toastStore.showToast('Vui lòng chọn kích thước', 'warning');
        return;
    }

    const variant = selectedVariant.value;
    if (!variant || variant.soLuong <= 0) {
        showStockAlert('Sản phẩm đã hết hàng', 'Phiên bản màu sắc và kích thước này hiện đã hết hàng trong kho. Vui lòng chọn phiên bản khác.');
        return;
    }

    try {
        const result = await cartStore.addToCart({
            idChiTietSanPham: variant.id,
            soLuong: selectedQuantity.value,
            tenSanPham: product.value?.ten || product.value?.tenSanPham || '',
            hinhAnh: variant.images?.[0]?.duongDanAnh || product.value?.hinhAnh || '',
            tenMauSac: variant.tenMauSac || selectedColor.value || '',
            tenKichThuoc: variant.tenKichThuoc || selectedSize.value || '',
            giaBan: variant.giaBan || displayPrice.value || 0,
            soLuongTonKho: variant.soLuong || 0
        });
        if (result?.success) {
            router.push('/checkout');
        } else {
            toastStore.showToast(result?.message || 'Không thể tiến hành thanh toán', 'warning');
        }
    } catch (e) {
        toastStore.showToast('Có lỗi xảy ra, vui lòng thử lại', 'error');
    }
};

const placeholderAngles = ['Ảnh chính', 'Mặt bên', 'Đế giày', 'Góc sau'];

const allImages = computed(() => {
    const images = [];

    // Thêm ảnh đại diện của sản phẩm
    const mainImg = getValidImgUrl(product.value?.hinhAnh);
    if (mainImg) {
        images.push({ duongDanAnh: mainImg, label: 'Ảnh chính' });
    }

    // Thêm ảnh của các biến thể
    if (product.value?.variants) {
        product.value.variants.forEach((v) => {
            if (v.images && v.images.length > 0) {
                v.images.forEach((img) => {
                    const url = getValidImgUrl(img.duongDanAnh || img.hinhAnh || img.url);
                    if (url && !images.find((i) => i.duongDanAnh === url)) {
                        images.push({ duongDanAnh: url, label: v.tenMauSac ? `Màu ${v.tenMauSac}` : 'Ảnh biến thể' });
                    }
                });
            }
            if (v.hinhAnh) {
                const url = getValidImgUrl(v.hinhAnh);
                if (url && !images.find((i) => i.duongDanAnh === url)) {
                    images.push({ duongDanAnh: url, label: v.tenMauSac ? `Màu ${v.tenMauSac}` : 'Ảnh biến thể' });
                }
            }
        });
    }

    if (images.length === 0) {
        images.push({ duongDanAnh: DEFAULT_SHOE_IMAGE, label: 'Ảnh chính' });
    }
    return images;
});

const colorVariantPreviews = computed(() => {
    if (!product.value?.variants) return [];
    const map = new Map();
    product.value.variants.forEach(v => {
        if (v.tenMauSac && !map.has(v.tenMauSac)) {
            const rawImg = v.hinhAnh || (v.images && v.images.length > 0 ? (v.images[0].duongDanAnh || v.images[0].hinhAnh) : null);
            const img = getValidImgUrl(rawImg) || DEFAULT_SHOE_IMAGE;
            map.set(v.tenMauSac, { color: v.tenMauSac, img: img });
        }
    });
    return Array.from(map.values());
});

const onSelectColorPreview = (cv) => {
    selectedColor.value = cv.color;
    if (cv.img) {
        const idx = allImages.value.findIndex(img => img.duongDanAnh === cv.img);
        if (idx !== -1) {
            activeSlide.value = idx;
        }
    }
};

watch(allImages, (newImages) => {
    if (newImages && newImages.length > 0) {
        activeSlide.value = 0;
    }
}, { immediate: true });

// Tìm variant phù hợp với color và size đã chọn
const selectedVariant = computed(() => {
    if (!product.value?.variants || !selectedColor.value || !selectedSize.value) return null;
    return product.value.variants.find((v) => v.tenMauSac === selectedColor.value && v.tenKichThuoc === selectedSize.value);
});

const displayPrice = computed(() => {
    if (selectedVariant.value && selectedVariant.value.giaBan) return selectedVariant.value.giaBan;
    return product.value?.minPrice || 0;
});

const formattedDisplayPrice = computed(() => {
    if (selectedVariant.value && selectedVariant.value.giaBan) {
        return formatPrice(selectedVariant.value.giaBan);
    }
    if (product.value?.minPrice && product.value?.maxPrice) {
        if (product.value.minPrice === product.value.maxPrice) {
            return formatPrice(product.value.minPrice);
        }
        return `${formatPrice(product.value.minPrice)} - ${formatPrice(product.value.maxPrice)}`;
    }
    return '0 ₫';
});

const stockAlertModal = ref({
    show: false,
    title: '',
    message: ''
});

const showStockAlert = (title, message) => {
    stockAlertModal.value = {
        show: true,
        title,
        message
    };
};

const maxQuantity = computed(() => {
    if (selectedVariant.value) return selectedVariant.value.soLuong || 0;
    if (selectedColor.value && product.value?.stockByColor) {
        return product.value.stockByColor[selectedColor.value] || 0;
    }
    return product.value?.totalStock || 0;
});

watch(maxQuantity, (newMax) => {
    if (selectedQuantity.value > newMax && newMax > 0) {
        selectedQuantity.value = newMax;
    }
});

const handleQuantityInput = (val) => {
    let num = parseInt(val, 10);
    if (isNaN(num) || num <= 0) {
        selectedQuantity.value = 1;
        return;
    }
    if (maxQuantity.value > 0 && num > maxQuantity.value) {
        showStockAlert(
            'Vượt quá số lượng tồn kho',
            `Sản phẩm này hiện chỉ còn tối đa ${maxQuantity.value} sản phẩm trong kho. Bạn không thể chọn số lượng lớn hơn.`
        );
        selectedQuantity.value = maxQuantity.value;
        return;
    }
    selectedQuantity.value = num;
};

const onlyNumbers = (e) => {
    if (!/[0-9]/.test(e.key)) {
        e.preventDefault();
    }
};

const onQuantityInput = (e) => {
    const val = e.target.value.replace(/\D/g, '');
    e.target.value = val;
    if (val !== '') {
        handleQuantityInput(val);
    }
};

const onQuantityBlur = (e) => {
    const val = e.target.value.replace(/\D/g, '');
    if (!val || parseInt(val, 10) < 1) {
        selectedQuantity.value = 1;
        e.target.value = '1';
    } else {
        handleQuantityInput(val);
    }
};

const handleIncrement = () => {
    if (maxQuantity.value > 0 && selectedQuantity.value >= maxQuantity.value) {
        showStockAlert(
            'Vượt quá số lượng tồn kho',
            `Sản phẩm này hiện chỉ còn tối đa ${maxQuantity.value} sản phẩm trong kho.`
        );
        return;
    }
    selectedQuantity.value++;
};

// Thêm vào giỏ hàng
const addToCart = async () => {
    if (!product.value) return;

    if (!selectedColor.value) {
        toastStore.showToast('Vui lòng chọn màu sắc', 'warning');
        return;
    }

    if (!selectedSize.value) {
        toastStore.showToast('Vui lòng chọn kích thước', 'warning');
        return;
    }

    const variant = selectedVariant.value;
    if (!variant || variant.soLuong <= 0) {
        showStockAlert('Sản phẩm đã hết hàng', 'Phiên bản màu sắc và kích thước này hiện đã hết hàng trong kho. Vui lòng chọn phiên bản khác.');
        return;
    }

    if (selectedQuantity.value > variant.soLuong) {
        showStockAlert('Vượt quá số lượng tồn kho', `Phiên bản này hiện chỉ còn tối đa ${variant.soLuong} sản phẩm trong kho.`);
        selectedQuantity.value = variant.soLuong;
        return;
    }

    addingToCart.value = true;
    try {
        const result = await cartStore.addToCart({
            idChiTietSanPham: variant.id,
            soLuong: selectedQuantity.value,
            // Truyền metadata để drawer hiển thị ngay (không cần chờ sync)
            tenSanPham: product.value?.ten || product.value?.tenSanPham || '',
            hinhAnh: variant.images?.[0]?.duongDanAnh || product.value?.hinhAnh || '',
            tenMauSac: variant.tenMauSac || selectedColor.value || '',
            tenKichThuoc: variant.tenKichThuoc || selectedSize.value || '',
            giaBan: variant.giaBan || displayPrice.value || 0,
            soLuongTonKho: variant.soLuong || 0
        });
        if (result?.success) {
            toastStore.showToast('Đã thêm vào giỏ hàng', 'success');
            cartStore.openDrawer();
        } else {
            toastStore.showToast(result?.message || 'Không thể thêm vào giỏ hàng', 'warning');
        }
    } catch (e) {
        toastStore.showToast('Có lỗi xảy ra, vui lòng thử lại', 'error');
    } finally {
        addingToCart.value = false;
    }
};

const toggleFavorite = () => {
    if (!authStore.isLoggedIn) {
        toastStore.showToast('Vui lòng đăng nhập để thêm vào yêu thích', 'warning');
        router.push(PATH.LOGIN);
        return;
    }
    isFavorite.value = !isFavorite.value;
    let favorites = JSON.parse(localStorage.getItem('aerostride_favorites') || '[]');
    if (isFavorite.value) {
        if (!favorites.includes(product.value.id)) {
            favorites.push(product.value.id);
        }
        toastStore.showToast('Đã thêm vào danh sách yêu thích', 'success');
    } else {
        favorites = favorites.filter(id => id !== product.value.id);
        toastStore.showToast('Đã huỷ yêu thích', 'info');
    }
    localStorage.setItem('aerostride_favorites', JSON.stringify(favorites));
    window.dispatchEvent(new Event('favorites-updated'));
};
</script>

<template>
    <div class="product-detail-page bg-white min-vh-100">
        <MainHeader />

        <div class="header-spacing" style="height: 104px"></div>
        <PromotionBar />

        <v-container class="mt-12" v-if="product">
            <v-row>
                <!-- Left: Image Gallery -->
                <v-col cols="12" md="6" lg="5" class="image-gallery">
                    <div class="product-gallery-wrapper">
                        <!-- Main Image Box -->
                        <div class="rounded-xl bg-grey-lighten-4 mb-4 elevation-1 position-relative overflow-hidden"
                            style="aspect-ratio: 1; max-height: 480px; border: 1px solid #e2e8f0;">
                            <!-- Floating Favorite Button -->
                            <v-btn
                                icon
                                variant="flat"
                                color="white"
                                class="position-absolute favorite-floating-btn"
                                @click="toggleFavorite"
                            >
                                <v-icon :color="isFavorite ? 'red' : 'grey-darken-1'" size="20">
                                    {{ isFavorite ? 'mdi-heart' : 'mdi-heart-outline' }}
                                </v-icon>
                            </v-btn>

                            <template v-if="allImages.length > 0">
                                <v-carousel v-model="activeSlide" cycle interval="4000" hide-delimiters show-arrows="hover" height="100%">
                                    <v-carousel-item v-for="(img, i) in allImages" :key="i" :src="img.duongDanAnh" cover>
                                        <template #placeholder>
                                            <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                                                <v-progress-circular indeterminate color="#1e257c"></v-progress-circular>
                                            </div>
                                        </template>
                                    </v-carousel-item>
                                </v-carousel>
                            </template>

                            <template v-else>
                                <div class="d-flex flex-column align-center justify-center fill-height text-center pa-6">
                                    <div class="mb-3 pa-4 rounded-circle" style="background: #f0f4ff;">
                                        <v-icon size="48" style="color: #1e257c;">mdi-shoe-sneaker</v-icon>
                                    </div>
                                    <h4 class="text-subtitle-1 font-weight-bold mb-1" style="color: #1e257c;">{{ product.tenSanPham }}</h4>
                                    <p class="text-caption text-grey">Chưa có hình ảnh trực quan</p>
                                </div>
                            </template>
                        </div>

                        <!-- Multi-Slot Thumbnail Strip -->
                        <div class="thumbnail-strip-section mb-4">
                            <div class="d-flex align-center justify-space-between mb-2">
                                <span class="text-caption font-weight-bold" style="color: #1e257c;">
                                    <v-icon size="14" class="mr-1" style="color: #1e257c;">mdi-view-grid-outline</v-icon>
                                    Bộ sưu tập hình ảnh ({{ allImages.length }} hình ảnh)
                                </span>
                            </div>

                            <v-row class="g-2">
                                <template v-if="allImages.length > 0">
                                    <v-col v-for="(img, i) in allImages" :key="'img-' + i" cols="3" sm="2" class="mb-2">
                                        <v-card class="rounded-lg overflow-hidden"
                                            :elevation="activeSlide === i ? 4 : 0"
                                            :style="activeSlide === i ? 'border: 2px solid #1e257c; box-shadow: 0 4px 10px rgba(30, 37, 124, 0.25);' : 'border: 1px solid #e2e8f0; cursor: pointer;'"
                                            @click="activeSlide = i">
                                            <v-img :src="img.duongDanAnh" cover class="aspect-square">
                                                <template #placeholder>
                                                    <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                                                        <v-icon size="18" color="grey">mdi-image-outline</v-icon>
                                                    </div>
                                                </template>
                                            </v-img>
                                        </v-card>
                                    </v-col>
                                </template>

                                <template v-if="allImages.length < 4">
                                    <v-col v-for="(angleLabel, idx) in placeholderAngles.slice(allImages.length)" :key="'angle-' + idx" cols="3" sm="2" class="mb-2">
                                        <v-card class="rounded-lg bg-grey-lighten-5 overflow-hidden d-flex flex-column align-center justify-center aspect-square text-center pa-1"
                                            style="border: 1px dashed #cbd5e1; opacity: 0.85;">
                                            <v-icon color="#1e257c" size="20" class="mb-1">mdi-camera-outline</v-icon>
                                            <span style="font-size: 0.65rem; color: #64748b; font-weight: 600; line-height: 1;">{{ angleLabel }}</span>
                                        </v-card>
                                    </v-col>
                                </template>
                            </v-row>
                        </div>
                    </div>
                </v-col>

                <!-- Right: Product Info -->
                <v-col cols="12" md="6" lg="7">
                    <div class="sticky-info-panel px-md-8">
                        <!-- Brand tag above the title -->
                        <div class="product-brand-tag text-uppercase mb-2">
                            {{ product.tenThuongHieu || 'AEROSTRIDE' }}
                        </div>
                        
                        <!-- Product Title -->
                        <h1 class="product-title-new mb-2">{{ product.tenSanPham }}</h1>
                        
                        <!-- Ratings, Review & Sold Count -->
                        <div class="product-meta-row d-flex align-center gap-2 mb-6">
                            <template v-if="totalReviews > 0">
                                <div class="rating-stars-wrapper d-flex align-center">
                                    <v-icon v-for="star in 5" :key="star" size="14" color="amber" class="mr-0.5">
                                        {{ star <= Math.round(Number(averageRating)) ? 'mdi-star' : 'mdi-star-outline' }}
                                    </v-icon>
                                    <span class="rating-value-text ml-1">{{ averageRating }}</span>
                                </div>
                                <span class="meta-separator text-grey-lighten-1">|</span>
                                <span class="reviews-count-text">({{ totalReviews }} đánh giá)</span>
                            </template>
                            <template v-else>
                                <span class="no-reviews-text text-grey-darken-1">Chưa có đánh giá</span>
                            </template>
                            <span class="meta-separator text-grey-lighten-1">|</span>
                            <span class="sold-count-text">Đã bán {{ product.daBan || 0 }}</span>
                        </div>

                        <!-- Price Section -->
                        <div class="product-price-row d-flex align-center gap-4 mb-6">
                            <span class="current-price-label-new">{{ formattedCurrentPrice }}</span>
                            <span v-if="discountPercent > 0 && formattedOldPrice" class="old-price-label-new">
                                {{ formattedOldPrice }}
                            </span>
                            <span v-if="discountPercent > 0" class="discount-badge-new">
                                -{{ discountPercent }}%
                            </span>
                        </div>

                        <!-- Color Selection -->
                        <div class="color-selection-section mb-6" v-if="colors.length > 0">
                            <div class="selection-label-row mb-3">
                                <span class="label-title">Màu sắc:</span>
                                <span class="label-selected-value font-weight-bold ml-1">
                                    {{ selectedColor || 'Chưa chọn' }}
                                </span>
                            </div>
                            <div class="d-flex flex-wrap ga-3">
                                <div 
                                    v-for="color in colors" 
                                    :key="color" 
                                    class="color-dot-wrapper"
                                    :class="{ active: selectedColor === color }"
                                    @click="selectedColor = color"
                                >
                                    <div 
                                        class="color-dot-inner" 
                                        :style="{ backgroundColor: colorHexMap[color] || '#CCCCCC' }"
                                    ></div>
                                </div>
                            </div>
                        </div>

                        <!-- Size Selection -->
                        <div class="size-selection-section mb-6">
                            <div class="d-flex justify-space-between align-center mb-3">
                                <div class="selection-label-row">
                                    <span class="label-title">Chọn kích cỡ</span>
                                    <span v-if="selectedSize" class="label-selected-value font-weight-bold ml-1">
                                        {{ selectedSize.replace('Size ', '') }}
                                    </span>
                                </div>
                            </div>
                            
                            <div class="d-flex flex-wrap ga-2" v-if="sizes.length > 0">
                                <div 
                                    v-for="size in sizes" 
                                    :key="size" 
                                    class="size-box-item" 
                                    :class="{ active: selectedSize === size }"
                                    @click="selectedSize = size"
                                >
                                    {{ size.replace('Size ', '') }}
                                </div>
                            </div>
                            <p v-else class="text-caption text-grey-darken-1 bg-grey-lighten-4 pa-3 rounded-lg">
                                <v-icon size="16" class="mr-1">mdi-information-outline</v-icon>
                                Vui lòng chọn màu sắc để hiển thị các kích cỡ còn hàng.
                            </p>
                        </div>

                        <!-- Action Buttons and Quantity -->
                        <div class="actions-section-wrapper mb-6">
                            <!-- Quantity + Add to Cart Row -->
                            <div class="quantity-cart-row d-flex align-center gap-4 mb-4">
                                <!-- Modern Rounded Quantity Selector -->
                                <div class="quantity-selector-pill d-flex align-center">
                                    <button 
                                        class="qty-btn" 
                                        :disabled="selectedQuantity <= 1" 
                                        @click="selectedQuantity--"
                                    >
                                        <v-icon size="14">mdi-minus</v-icon>
                                    </button>
                                    <input 
                                        type="text" 
                                        inputmode="numeric" 
                                        class="qty-input" 
                                        :value="selectedQuantity" 
                                        @keypress="onlyNumbers" 
                                        @input="onQuantityInput" 
                                        @blur="onQuantityBlur"
                                    />
                                    <button 
                                        class="qty-btn" 
                                        @click="handleIncrement"
                                    >
                                        <v-icon size="14">mdi-plus</v-icon>
                                    </button>
                                </div>

                                <!-- Add to Cart Button -->
                                <v-btn 
                                    flat
                                    color="#2962FF" 
                                    class="add-to-cart-btn-new flex-grow-1" 
                                    :loading="addingToCart" 
                                    @click="addToCart"
                                >
                                    THÊM VÀO GIỎ HÀNG
                                </v-btn>
                            </div>

                            <!-- Buy Now Button -->
                            <v-btn 
                                flat
                                color="#0B1329" 
                                class="buy-now-btn-new block w-100" 
                                @click="buyNow"
                            >
                                MUA NGAY
                            </v-btn>
                        </div>

                        <!-- Description Details Section -->
                        <div class="product-desc-section mt-8 pt-6 border-top">
                            <h3 class="desc-section-title mb-3">Mô tả sản phẩm</h3>
                            <p class="desc-text-new">{{ product.moTa }}</p>
                            <div class="d-flex flex-wrap gap-4 mt-4">
                                <v-chip size="small" variant="flat" color="#F1F5F9" class="font-weight-medium text-grey-darken-3">
                                    <v-icon start size="14" color="grey-darken-2">mdi-earth</v-icon> Xuất xứ: {{ product.tenXuatXu }}
                                </v-chip>
                                <v-chip size="small" variant="flat" color="#F1F5F9" class="font-weight-medium text-grey-darken-3">
                                    <v-icon start size="14" color="grey-darken-2">mdi-barcode</v-icon> Mã: {{ product.maSanPham }}
                                </v-chip>
                            </div>
                        </div>
                    </div>
                </v-col>
            </v-row>

            <!-- Reviews Section -->
            <div class="reviews-section mt-16 pt-8 border-top">
                <h2 class="text-h4 font-weight-semibold mb-8 text-center text-primary">Đánh Giá Sản Phẩm</h2>

                <div v-if="reviewsLoading" class="text-center py-8">
                    <v-progress-circular indeterminate color="primary"></v-progress-circular>
                </div>
                <div v-else-if="reviews.length > 0">
                    <v-card variant="outlined" class="mb-10 rounded-xl border-grey-lighten-2">
                        <v-row class="ma-0">
                            <v-col cols="12" md="4" class="d-flex align-center justify-center bg-grey-lighten-4 pa-6">
                                <div class="text-center">
                                    <div class="text-h2 font-weight-semibold text-amber-darken-3">{{ averageRating }}</div>
                                    <v-rating :model-value="Number(averageRating) || 5" color="amber" active-color="amber"
                                        half-increments readonly size="large" class="mb-2"></v-rating>
                                    <div class="text-body-1 text-grey-darken-1 font-weight-medium">{{ totalReviews }} đánh giá
                                    </div>
                                </div>
                            </v-col>

                            <!-- Rating Bars & Filters -->
                            <v-col cols="12" md="8" class="pa-6">
                                <div class="d-flex flex-wrap ga-2 mb-2">
                                    <v-chip :variant="selectedFilter === 'all' ? 'flat' : 'outlined'"
                                        :color="selectedFilter === 'all' ? 'black' : 'grey-darken-1'"
                                        @click="selectedFilter = 'all'" class="font-weight-bold px-4">
                                        Tất cả ({{ totalReviews }})
                                    </v-chip>
                                    <v-chip v-for="star in [5, 4, 3, 2, 1]" :key="star"
                                        :variant="selectedFilter === star ? 'flat' : 'outlined'"
                                        :color="selectedFilter === star ? 'black' : 'grey-darken-1'"
                                        @click="selectedFilter = star" class="font-weight-bold px-4">
                                        {{ star }} Sao ({{ ratingCounts[star] }})
                                    </v-chip>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card>

                    <v-row>
                        <v-col v-for="review in reviews" :key="review.id" cols="12" md="6">
                            <v-card variant="outlined" class="pa-4 rounded-xl border-grey-lighten-2 h-100">
                                <div class="d-flex align-center mb-3">
                                    <v-avatar color="grey-lighten-3" size="40" class="mr-3 border border-grey-lighten-2">
                                        <v-img :src="review.avatarKhachHang || 'https://i.pinimg.com/736x/c0/74/9b/c0749b7cc401421662ae901ec8f9f660.jpg'" alt="avatar" cover></v-img>
                                    </v-avatar>
                                    <div>
                                        <div class="font-weight-medium text-grey-darken-4">{{
                                            review.tenKhachHang || 'Khách hàng ẩn danh' }}</div>
                                        <div class="text-caption text-grey">{{ new
                                            Date(review.ngayTao).toLocaleDateString('vi-VN') }}</div>
                                    </div>
                                </div>
                            </v-card>
                        </v-col>
                    </v-row>
                </div>

                <div v-else
                    class="text-center py-12 bg-grey-lighten-4 rounded-xl border-dashed border-grey-lighten-1"
                    style="border-width: 2px;">
                    <v-icon size="64" color="grey-lighten-1" class="mb-4">mdi-comment-text-outline</v-icon>
                    <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">Chưa có đánh giá nào</h3>
                    <p class="text-body-1 text-grey">Hãy là người đầu tiên trải nghiệm và đánh giá sản phẩm
                        này.</p>
                    <v-btn color="black" variant="outlined" rounded="pill"
                        class="mt-4 font-weight-bold px-6" @click="handleWriteReview">
                        Viết đánh giá ngay
                    </v-btn>
                </div>
            </div>

            <!-- Recommended Products Section -->
                        <div class="recommended-section mt-16 pt-8 border-top" v-if="recommendedProducts.length > 0">
                            <h2 class="text-h4 font-weight-semibold mb-8 text-center text-primary">Có Thể Bạn Cũng Thích
                            </h2>
                            <v-row>
                                <v-col v-for="p in recommendedProducts" :key="p.id" cols="12" sm="6" md="4" lg="3">
                                    <div class="product-card-placeholder" @click="$router.push(`/product/${p.id}`)">
                                        <!-- Image Placeholder -->
                                        <div class="image-box-placeholder mb-4">
                                            <img 
                                                :src="getValidImgUrl(p.hinhAnh) || DEFAULT_SHOE_IMAGE" 
                                                :alt="p.tenSanPham" 
                                                style="width: 100%; height: 100%; object-fit: cover;"
                                                referrerpolicy="no-referrer"
                                                @error="(e) => e.target.src = DEFAULT_SHOE_IMAGE"
                                            />
                                        </div>

                                        <!-- Content -->
                                        <div class="product-info text-left">
                                            <span class="promo-label">{{ p.tenThuongHieu || 'NIKE' }}</span>
                                            <h4 class="product-name text-truncate">{{ p.tenSanPham }}</h4>
                                            <p class="product-price">{{ formatPrice(p.giaBanThapNhat) }}</p>
                                        </div>
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

        </v-container>
        <v-container v-else-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="primary"></v-progress-circular>
        </v-container>

        <!-- Landing style Footer -->
        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t mt-16">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride. All rights reserved.</p>
        </footer>

        <!-- Cart Drawer -->

        <!-- Global Chat System -->
        <CustomerChat />

        <!-- Stock Alert Modal -->
        <v-dialog v-model="stockAlertModal.show" max-width="450">
            <v-card class="rounded-xl pa-4 text-center">
                <div class="d-flex justify-center mt-2 mb-3">
                    <v-avatar color="amber-lighten-4" size="64">
                        <v-icon color="amber-darken-3" size="36">mdi-alert-circle-outline</v-icon>
                    </v-avatar>
                </div>
                <v-card-title class="text-h6 font-weight-semibold pt-0 pb-2">{{ stockAlertModal.title }}</v-card-title>
                <v-card-text class="text-body-2 text-grey-darken-2 px-4 pb-4">
                    {{ stockAlertModal.message }}
                </v-card-text>
                <v-card-actions class="justify-center pb-2">
                    <v-btn color="primary" variant="flat" rounded="pill" class="px-8 font-weight-medium text-none"
                        @click="stockAlertModal.show = false">
                        Đã hiểu
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Direct Review Modal -->
        <v-dialog v-model="showReviewModal" max-width="500" persistent>
            <v-card class="rounded-xl overflow-hidden">
                <v-card-title class="d-flex align-center py-3 bg-black text-white">
                    <v-icon icon="mdi-star-circle-outline" class="mr-2"></v-icon>
                    Viết đánh giá
                    <v-spacer></v-spacer>
                    <v-btn icon="mdi-close" variant="text" color="white" @click="showReviewModal = false"
                        density="compact" :disabled="submittingReview"></v-btn>
                </v-card-title>

                <v-card-text class="pa-4">
                    <div class="d-flex align-center mb-4 pa-2 bg-grey-lighten-4 rounded-lg pa-3" v-if="product">
                        <v-avatar rounded size="48" class="mr-3 bg-white elevation-1">
                            <img 
                                :src="getValidImgUrl(product.hinhAnh) || DEFAULT_SHOE_IMAGE" 
                                style="width: 100%; height: 100%; object-fit: cover;"
                                @error="handleImgError"
                            />
                        </v-avatar>
                        <div>
                            <div class="font-weight-bold text-body-2 text-truncate" style="max-width: 300px;">{{
                                product.tenSanPham }}</div>
                            <div class="text-caption text-grey">{{ product.tenThuongHieu }}</div>
                        </div>
                    </div>

                    <div class="text-center mb-4">
                        <p class="text-subtitle-2 font-weight-bold mb-1">Chất lượng sản phẩm</p>
                        <v-rating v-model="newReview.rating" color="amber" active-color="amber" hover
                            size="x-large"></v-rating>
                    </div>

                    <v-textarea v-model="newReview.comment" label="Nhận xét của bạn"
                        placeholder="Hãy chia sẻ cảm nhận của bạn về sản phẩm này nhé..." variant="outlined" rows="4"
                        auto-grow hide-details="auto" bg-color="grey-lighten-5"></v-textarea>
                </v-card-text>

                <v-card-actions class="pa-4 pt-0">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-bold" @click="showReviewModal = false"
                        :disabled="submittingReview">Hủy</v-btn>
                    <v-btn color="black" variant="flat" class="text-none font-weight-bold px-6 rounded-pill"
                        :loading="submittingReview" @click="submitDirectReview">
                        Gửi đánh giá
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </div>
</template>

<style scoped lang="scss">
.product-detail-page {
    padding-top: 64px;
}

.image-placeholder-large {
    width: 100%;
    aspect-ratio: 1;
    background: #f6f6f6;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    border-radius: 4px;

    .index-label {
        position: absolute;
        bottom: 20px;
        left: 20px;
        font-size: 0.65rem;
        font-weight: 900;
        color: #ccc;
        letter-spacing: 2px;
    }
}

.sticky-info-panel {
    position: sticky;
    top: 140px;
}

.product-title {
    font-size: 2.25rem !important;
}

.product-cat {
    color: #4b5563;
}

.product-price {
    color: #1e257c;
}

.size-guide {
    font-size: 0.9rem;
    color: #4b5563;
    text-decoration: none;
    font-weight: 500;
    border-bottom: 1px solid transparent;

    &:hover {
        border-bottom-color: #1e257c;
        color: #1e257c;
    }
}

.size-box {
    border: 1px solid #DFE5EF;
    border-radius: 8px;
    padding: 12px;
    text-align: center;
    font-size: 0.95rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        border-color: #1e257c;
        color: #1e257c;
    }

    &.active {
        border-color: #1e257c;
        background: #1e257c;
        color: #fff;
    }
}

.desc-text {
    font-size: 1.05rem;
    line-height: 1.8;
    color: #374151;
}

.spec-list {
    list-style: disc;

    li {
        font-size: 0.9rem;
        margin-bottom: 8px;
        color: #374151;
        font-weight: 500;
    }
}

:deep(.v-expansion-panel-title) {
    font-weight: 600;
    font-size: 1.1rem;
    padding: 24px 0;
    color: #1e257c;
}

:deep(.v-expansion-panel-text__wrapper) {
    padding: 0 0 24px 0;
}

.gap-4 {
    gap: 16px;
}

.aspect-square {
    aspect-ratio: 1;
}

@media (max-width: 960px) {
    .sticky-info-panel {
        position: relative;
        top: 0;
        padding: 0;
        margin-top: 40px;
    }
}

.quantity-input {
    width: 60px;
    border: none;
    outline: none;
    background: transparent;
    appearance: textfield;
    -moz-appearance: textfield;
}

.quantity-input::-webkit-outer-spin-button,
.quantity-input::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

.product-badge-chip {
    border: 1px solid rgba(30, 37, 124, 0.25) !important;
}

.shipping-returns-alert {
    background-color: #fffbeb !important;
    /* Pastel yellow background (amber-50) */
    border-color: #f59e0b !important;
    /* Warm yellow/amber border (amber-500) */
    color: #78350f !important;
    /* Elegant deep warm amber-brown text (amber-900) */

    :deep(.v-alert__prepend .v-icon) {
        color: #f59e0b !important;
    }

    .title-text {
        color: #78350f !important;
        font-weight: 600 !important;
    }

    .desc-text {
        color: #92400e !important;
        /* Slightly lighter body text (amber-800) */
        opacity: 0.95;
    }
}

/* Product Card Styling (Synced with ShoeListing.vue) */
.product-card-placeholder {
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border-radius: 20px;
    padding: 10px;
    background: transparent;
    border: 1px solid transparent;

    &:hover {
        background: #f8fafc;
        box-shadow: 0 12px 24px -10px rgba(30, 37, 124, 0.15);
        transform: translateY(-6px);

        .image-box-placeholder :deep(.v-img__img) {
            transform: scale(1.08);
        }

        .product-name {
            color: #2563eb;
        }
    }
}

.image-box-placeholder {
    width: 100%;
    aspect-ratio: 1;
    background: transparent;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;

    :deep(.v-img__img) {
        transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    }
}

.product-info {
    padding: 8px 4px 0;

    .promo-label {
        color: #ff3d00;
        font-size: 0.85rem;
        font-weight: 800;
        display: block;
        margin-bottom: 4px;
    }

    .product-name {
        font-size: 1rem;
        font-weight: 700;
        color: #1e257c;
        margin-bottom: 2px;
        transition: color 0.3s ease;
    }

    .product-price {
        font-size: 1.05rem;
        font-weight: 800;
        color: #1e257c;
    }
}

/* Modern Product Detail Design Overrides */
.product-brand-tag {
    font-family: 'Outfit', sans-serif;
    font-size: 13px;
    font-weight: 700;
    color: #2962FF;
    letter-spacing: 1.5px;
    text-transform: uppercase;
}

.product-title-new {
    font-family: 'Outfit', sans-serif;
    font-size: 32px;
    font-weight: 800;
    line-height: 1.25;
    color: #0A1329;
}

.product-meta-row {
    font-size: 13px;
    color: #64748B;
    font-weight: 500;
    
    .rating-value-text {
        font-weight: 700;
        color: #0A1329;
    }
    
    .reviews-count-text, .sold-count-text {
        color: #64748B;
    }
    
    .meta-separator {
        margin: 0 4px;
    }
}

.product-price-row {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .current-price-label-new {
        font-size: 30px;
        font-weight: 800;
        color: #E53935;
        font-family: 'Outfit', sans-serif;
    }
    
    .old-price-label-new {
        font-size: 18px;
        font-weight: 500;
        color: #94A3B8;
        text-decoration: line-through;
    }
    
    .discount-badge-new {
        background-color: #FEE2E2;
        color: #EF4444;
        font-size: 13px;
        font-weight: 700;
        padding: 4px 8px;
        border-radius: 6px;
    }
}

.selection-label-row {
    font-size: 14px;
    color: #0A1329;
    
    .label-title {
        font-weight: 600;
    }
    
    .label-selected-value {
        color: #64748B;
        font-weight: 500;
    }
}

/* Color Dot Selectors */
.color-dot-wrapper {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    border: 2px solid transparent;
    padding: 2px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

    .color-dot-inner {
        width: 100%;
        height: 100%;
        border-radius: 50%;
        border: 1px solid rgba(0, 0, 0, 0.08);
        transition: transform 0.2s;
    }
    
    &:hover {
        transform: scale(1.05);
        .color-dot-inner {
            transform: scale(0.95);
        }
    }
    
    &.active {
        border-color: #2962FF;
        box-shadow: 0 4px 12px rgba(41, 98, 255, 0.25);
    }
}

/* Size Box Selectors */
.size-guide-link {
    font-size: 13px;
    color: #2962FF;
    font-weight: 600;
    text-decoration: none;
    transition: opacity 0.2s;
    
    &:hover {
        opacity: 0.8;
        text-decoration: underline;
    }
}

.size-box-item {
    min-width: 48px;
    height: 48px;
    padding: 0 12px;
    border-radius: 12px;
    background-color: #F8FAFC;
    border: 1px solid #E2E8F0;
    color: #0A1329;
    font-size: 14px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    
    &:hover {
        border-color: #2962FF;
        color: #2962FF;
        background-color: #F0F4FF;
    }
    
    &.active {
        background-color: #2962FF !important;
        border-color: #2962FF !important;
        color: #FFFFFF !important;
        box-shadow: 0 4px 12px rgba(41, 98, 255, 0.25);
    }
}

/* Availability Status */
.availability-status-row {
    font-size: 14px;
    font-weight: 600;
    color: #10B981;
}

/* Action Section */
.quantity-cart-row {
    display: flex;
    align-items: center;
    width: 100%;
}

.quantity-selector-pill {
    height: 48px;
    background-color: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 0 8px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 130px;
    
    .qty-btn {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        border: none;
        background: transparent;
        color: #0A1329;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background-color 0.2s;
        
        &:hover:not(:disabled) {
            background-color: #E2E8F0;
        }
        
        &:disabled {
            opacity: 0.35;
            cursor: not-allowed;
        }
    }
    
    .qty-number,
    .qty-input {
        font-size: 16px;
        font-weight: 700;
        color: #0A1329;
        width: 48px;
        text-align: center;
        border: none;
        outline: none;
        background: transparent;
        appearance: textfield;
        -moz-appearance: textfield;
    }
    
    .qty-input::-webkit-outer-spin-button,
    .qty-input::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
    }
}

.add-to-cart-btn-new {
    height: 48px !important;
    border-radius: 999px !important;
    font-size: 14px !important;
    font-weight: 700 !important;
    letter-spacing: 0.5px !important;
    box-shadow: 0 4px 14px rgba(41, 98, 255, 0.25) !important;
    color: #FFFFFF !important;
    transition: transform 0.2s !important;
    
    &:hover {
        transform: translateY(-2px);
    }
}

.buy-now-btn-new {
    height: 48px !important;
    border-radius: 999px !important;
    font-size: 14px !important;
    font-weight: 700 !important;
    letter-spacing: 0.5px !important;
    box-shadow: 0 4px 14px rgba(11, 19, 41, 0.2) !important;
    color: #FFFFFF !important;
    transition: transform 0.2s !important;
    
    &:hover {
        transform: translateY(-2px);
    }
}

.favorite-floating-btn {
    top: 16px;
    right: 16px;
    z-index: 10;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
    border-radius: 50% !important;
    width: 44px !important;
    height: 44px !important;
    
    &:hover {
        transform: scale(1.05);
    }
}

.desc-section-title {
    font-size: 16px;
    font-weight: 700;
    color: #0A1329;
}

.desc-text-new {
    font-size: 14px;
    line-height: 1.6;
    color: #475569;
}
</style>
