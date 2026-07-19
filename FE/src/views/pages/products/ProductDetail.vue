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

        const response = await api.post('/api/v1/customer/review/submit', payload);
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
        const res = await api.get(`/api/v1/customer/review/product/${route.params.id}`);
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

const colors = computed(() => product.value?.availableColors || []);

const sizes = computed(() => {
    if (!product.value?.availableSizesByColor || !selectedColor.value) return [];
    return product.value.availableSizesByColor[selectedColor.value] || [];
});

watch(selectedColor, () => {
    selectedSize.value = null; // Reset size when color changes
});

const allImages = computed(() => {
    const images = [];
    
    // Thêm ảnh đại diện của sản phẩm
    if (product.value?.hinhAnh) {
        images.push({ duongDanAnh: product.value.hinhAnh });
    }
    
    // Thêm ảnh của các biến thể
    if (product.value?.variants) {
        product.value.variants.forEach((v) => {
            if (v.images) {
                v.images.forEach((img) => {
                    if (!images.find((i) => i.duongDanAnh === img.duongDanAnh)) {
                        images.push({ duongDanAnh: img.duongDanAnh });
                    }
                });
            }
        });
    }
    return images;
});

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
                    <div v-if="allImages.length > 0">
                        <!-- Main Auto Slider -->
                        <div class="rounded-lg bg-grey-lighten-4 mb-4" style="aspect-ratio: 1; max-height: 600px; overflow: hidden;">
                            <v-carousel
                                v-model="activeSlide"
                                cycle
                                interval="3000"
                                hide-delimiters
                                show-arrows="hover"
                                height="100%"
                            >
                                <v-carousel-item
                                    v-for="(img, i) in allImages"
                                    :key="i"
                                    :src="img.duongDanAnh"
                                    cover
                                ></v-carousel-item>
                            </v-carousel>
                        </div>

                        <!-- Thumbnail Strip -->
                        <v-row class="g-2">
                            <v-col v-for="(img, i) in allImages" :key="'img-'+i" cols="3" sm="2" class="mb-2">
                                <v-card class="rounded-lg bg-grey-lighten-4 overflow-hidden"
                                    :elevation="activeSlide === i ? 4 : 0"
                                    :style="activeSlide === i ? 'border: 2px solid #e53935;' : 'border: 1px solid #eee; cursor: pointer;'"
                                    @click="activeSlide = i">
                                    <v-img :src="img.duongDanAnh" cover class="aspect-square"></v-img>
                                </v-card>
                            </v-col>
                            <v-col v-for="i in Math.max(0, 4 - allImages.length)" :key="'empty-'+i" cols="3" sm="2" class="mb-2">
                                <v-card class="rounded-lg bg-grey-lighten-4 overflow-hidden d-flex align-center justify-center aspect-square"
                                    style="border: 1px dashed #ccc; opacity: 0.6">
                                    <v-icon color="grey-lighten-1" size="24">mdi-image-outline</v-icon>
                                </v-card>
                            </v-col>
                        </v-row>
                    </div>
                    <div v-else class="image-placeholder-large">
                        <v-icon size="64" color="grey-lighten-2">mdi-image-outline</v-icon>
                    </div>
                </v-col>

                <!-- Right: Product Info -->
                <v-col cols="12" md="6" lg="7">
                    <div class="sticky-info-panel px-md-8">
                        <div class="header-info mb-8">
                            <h1 class="product-title text-h4 font-weight-black mb-1">{{ product.tenSanPham }}</h1>
                            <p class="product-cat text-subtitle-1 font-weight-bold grey--text">
                                {{ product.tenThuongHieu }}
                            </p>
                            <div class="product-price mt-4 text-h5 font-weight-black text-blue-darken-4">{{ formattedDisplayPrice }}</div>
                        </div>

                        <!-- Description Info moved to top -->
                        <div class="product-description mb-8">
                            <p class="desc-text text-body-1">{{ product.moTa }}</p>
                            <div class="d-flex flex-wrap gap-4 mt-3">
                                <v-chip size="small" variant="flat" color="grey-lighten-4"
                                    class="font-weight-bold"><v-icon start size="14">mdi-domain</v-icon> {{
                                    product.tenThuongHieu }}</v-chip>
                                <v-chip size="small" variant="flat" color="grey-lighten-4"
                                    class="font-weight-bold"><v-icon start size="14">mdi-earth</v-icon> Xuất xứ: {{
                                    product.tenXuatXu }}</v-chip>
                                <v-chip size="small" variant="flat" color="grey-lighten-4"
                                    class="font-weight-bold"><v-icon start size="14">mdi-barcode</v-icon> Mã: {{
                                    product.maSanPham }}</v-chip>
                            </div>
                        </div>

                        <!-- Color Selection -->
                        <div class="color-selection-section mb-6" v-if="colors.length > 0">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="font-weight-black">Chọn màu sắc</span>
                            </div>
                            <v-row class="g-2">
                                <v-col v-for="color in colors" :key="color" cols="4">
                                    <div class="size-box" :class="{ active: selectedColor === color }"
                                        @click="selectedColor = color">
                                        {{ color }}
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

                        <!-- Size Selection -->
                        <div class="size-selection-section mb-10">
                            <div class="d-flex justify-space-between align-center mb-4">
                                <span class="font-weight-black">Chọn kích thước</span>
                                <a href="#" class="size-guide">Bảng kích thước</a>
                            </div>
                            <v-row class="g-2" v-if="sizes.length > 0">
                                <v-col v-for="size in sizes" :key="size" cols="4">
                                    <div class="size-box" :class="{ active: selectedSize === size }"
                                        @click="selectedSize = size">
                                        {{ size.replace('Size ', '') }}
                                    </div>
                                </v-col>
                            </v-row>
                            <p v-else class="text-grey">Vui lòng chọn màu sắc để xem các kích thước.</p>
                        </div>

                        <!-- Quantity Selection -->
                        <div class="quantity-selection-section mb-10">
                            <div class="d-flex align-center justify-space-between mb-4">
                                <span class="font-weight-black">Số lượng</span>
                                <span class="text-caption text-grey" v-if="maxQuantity > 0">
                                    Còn lại: {{ maxQuantity }}
                                </span>
                            </div>
                            <div class="d-flex align-center border rounded-lg py-1 px-3" style="width: 150px;">
                                <v-btn icon="mdi-minus" variant="plain" density="compact"
                                    :disabled="selectedQuantity <= 1" @click="selectedQuantity--"></v-btn>
                                <input type="number"
                                    class="quantity-input flex-grow-1 text-center font-weight-bold text-body-1"
                                    :value="selectedQuantity"
                                    @change="handleQuantityInput($event.target.value)"
                                    @blur="handleQuantityInput($event.target.value)"
                                />
                                <v-btn icon="mdi-plus" variant="plain" density="compact"
                                    @click="handleIncrement"></v-btn>
                            </div>
                        </div>

                        <!-- Action Buttons -->
                        <div class="action-buttons d-flex flex-column gap-4">
                            <v-btn block size="x-large" color="black" rounded="pill" class="font-weight-black py-6"
                                :loading="addingToCart" @click="addToCart">
                                <v-icon class="mr-2">mdi-bag-plus-outline</v-icon>
                                Thêm vào giỏ hàng
                            </v-btn>
                            <v-btn v-if="authStore.isLoggedIn" block size="x-large" variant="outlined" rounded="pill" class="font-weight-black py-6"
                                :class="isFavorite ? 'text-red' : ''"
                                :color="isFavorite ? 'red' : 'black'" @click="toggleFavorite">
                                {{ isFavorite ? 'Đã yêu thích' : 'Yêu thích' }}
                                <v-icon class="ml-2" :color="isFavorite ? 'red' : ''">{{ isFavorite ? 'mdi-heart' :
                                    'mdi-heart-outline' }}</v-icon>
                            </v-btn>
                        </div>

                        <!-- Shipping & Returns Beautiful Alert -->
                        <div class="shipping-returns mt-8">
                            <v-alert color="blue-lighten-5" border="start" border-color="blue-darken-3"
                                icon="mdi-truck-fast-outline" class="rounded-lg" elevation="0">
                                <div class="font-weight-bold text-blue-darken-4 mb-1">Giao hàng & Trả hàng</div>
                                <div class="text-caption text-blue-darken-4">Miễn phí giao hàng tiêu chuẩn cho đơn hàng
                                    trên 5.000.000₫. Miễn phí đổi trả trong vòng 30 ngày.</div>
                            </v-alert>
                        </div>
                    </div>
                </v-col>
            </v-row>
            
            <!-- Reviews Section -->
            <div class="reviews-section mt-16 pt-8 border-top">
                <div class="d-flex align-center justify-space-between mb-8">
                    <h2 class="text-h4 font-weight-black mb-0">Đánh Giá Sản Phẩm</h2>
                    <v-btn color="black" variant="flat" rounded="pill" class="font-weight-bold px-6" prepend-icon="mdi-pencil-outline" @click="handleWriteReview">
                        Viết đánh giá
                    </v-btn>
                </div>
                
                <v-card variant="outlined" class="rounded-xl border-grey-lighten-2 mb-8 overflow-hidden">
                    <v-row no-gutters class="align-center">
                        <!-- Rating Summary -->
                        <v-col cols="12" md="4" class="pa-6 text-center border-e-md border-grey-lighten-2 bg-grey-lighten-4">
                            <div class="text-h2 font-weight-black text-amber-darken-3 mb-1">{{ averageRating || '5.0' }}</div>
                            <v-rating
                                :model-value="Number(averageRating) || 5"
                                color="amber"
                                active-color="amber"
                                half-increments
                                readonly
                                size="large"
                                class="mb-2"
                            ></v-rating>
                            <div class="text-body-1 text-grey-darken-1 font-weight-medium">{{ totalReviews }} đánh giá</div>
                        </v-col>
                        
                        <!-- Rating Bars & Filters -->
                        <v-col cols="12" md="8" class="pa-6">
                            <div class="d-flex flex-wrap gap-2 mb-2">
                                <v-chip
                                    :variant="selectedFilter === 'all' ? 'flat' : 'outlined'"
                                    :color="selectedFilter === 'all' ? 'black' : 'grey-darken-1'"
                                    @click="selectedFilter = 'all'"
                                    class="font-weight-bold px-4"
                                >
                                    Tất cả ({{ totalReviews }})
                                </v-chip>
                                <v-chip
                                    v-for="star in [5,4,3,2,1]"
                                    :key="star"
                                    :variant="selectedFilter === star ? 'flat' : 'outlined'"
                                    :color="selectedFilter === star ? 'black' : 'grey-darken-1'"
                                    @click="selectedFilter = star"
                                    class="font-weight-bold px-4"
                                >
                                    {{ star }} Sao ({{ ratingCounts[star] }})
                                </v-chip>
                            </div>
                        </v-col>
                    </v-row>
                </v-card>

                <div v-if="reviewsLoading" class="text-center py-8">
                    <v-progress-circular indeterminate color="black"></v-progress-circular>
                </div>
                
                <div v-else-if="reviews.length > 0">
                    <v-row>
                        <v-col v-for="review in reviews.filter(r => selectedFilter === 'all' || Math.floor(r.rating) === selectedFilter)" :key="review.id" cols="12">
                            <v-card variant="flat" class="pa-4 border-bottom rounded-0">
                                <div class="d-flex mb-3">
                                    <v-avatar color="grey-lighten-3" size="48" class="mr-4 mt-1">
                                        <span class="text-grey-darken-3 font-weight-bold text-h6">{{ review.tenKhachHang ? review.tenKhachHang.charAt(0) : 'U' }}</span>
                                    </v-avatar>
                                    <div class="flex-grow-1">
                                        <div class="d-flex align-center justify-space-between mb-1">
                                            <div class="font-weight-bold text-body-1">{{ review.tenKhachHang || 'Khách hàng ẩn danh' }}</div>
                                            <div class="text-caption text-grey">{{ new Date(review.ngayTao).toLocaleDateString('vi-VN') }}</div>
                                        </div>
                                        <v-rating
                                            :model-value="review.rating"
                                            color="amber"
                                            active-color="amber"
                                            readonly
                                            size="small"
                                            density="compact"
                                            class="mb-2"
                                        ></v-rating>
                                        <p class="text-body-1 text-grey-darken-3 mb-2" style="line-height: 1.6;">{{ review.comment }}</p>
                                        <div class="text-caption d-flex align-center font-weight-medium" :class="review.hoaDon ? 'text-success' : 'text-blue-grey'">
                                            <v-icon size="14" class="mr-1">{{ review.hoaDon ? 'mdi-check-circle' : 'mdi-account-check' }}</v-icon>
                                            {{ review.hoaDon ? 'Đã mua hàng' : 'Thành viên' }}
                                        </div>
                                    </div>
                                </div>
                            </v-card>
                        </v-col>
                    </v-row>
                </div>
                
                <div v-else class="text-center py-12 bg-grey-lighten-4 rounded-xl border-dashed border-grey-lighten-1" style="border-width: 2px;">
                    <v-icon size="64" color="grey-lighten-1" class="mb-4">mdi-comment-text-outline</v-icon>
                    <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">Chưa có đánh giá nào</h3>
                    <p class="text-body-1 text-grey">Hãy là người đầu tiên trải nghiệm và đánh giá sản phẩm này.</p>
                    <v-btn color="black" variant="outlined" rounded="pill" class="mt-4 font-weight-bold px-6" @click="handleWriteReview">
                        Viết đánh giá ngay
                    </v-btn>
                </div>
            </div>
            
            <!-- Recommended Products Section -->
            <div class="recommended-section mt-16 pt-8 border-top" v-if="recommendedProducts.length > 0">
                <h2 class="text-h4 font-weight-black mb-8 text-center">Có Thể Bạn Cũng Thích</h2>
                <v-row>
                    <v-col v-for="p in recommendedProducts" :key="p.id" cols="12" sm="6" md="4" lg="3">
                        <div class="cursor-pointer" @click="$router.push(`/product/${p.id}`)">
                            <div class="rounded-xl bg-grey-lighten-4 mb-4 overflow-hidden d-flex align-center justify-center" style="aspect-ratio: 1">
                                <v-img :src="p.hinhAnh || ''" cover class="fill-height"></v-img>
                            </div>
                            
                            <div class="px-1 text-left">
                                <div class="text-caption font-weight-bold text-uppercase text-grey-darken-2 mb-1">{{ p.tenThuongHieu || 'NIKE' }}</div>
                                <h3 class="text-subtitle-1 font-weight-black text-black mb-1 text-truncate">{{ p.tenSanPham }}</h3>
                                <div class="text-subtitle-1 font-weight-black text-black">{{ formatPrice(p.giaBanThapNhat) }}</div>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </div>
            
        </v-container>
        <v-container v-else-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="black"></v-progress-circular>
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
            <v-card class="rounded-2xl pa-4 text-center">
                <div class="d-flex justify-center mt-2 mb-3">
                    <v-avatar color="amber-lighten-4" size="64">
                        <v-icon color="amber-darken-3" size="36">mdi-alert-circle-outline</v-icon>
                    </v-avatar>
                </div>
                <v-card-title class="text-h6 font-weight-black pt-0 pb-2">{{ stockAlertModal.title }}</v-card-title>
                <v-card-text class="text-body-2 text-grey-darken-2 px-4 pb-4">
                    {{ stockAlertModal.message }}
                </v-card-text>
                <v-card-actions class="justify-center pb-2">
                    <v-btn color="black" variant="flat" rounded="pill" class="px-8 font-weight-bold text-none"
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
                    <v-btn icon="mdi-close" variant="text" color="white" @click="showReviewModal = false" density="compact" :disabled="submittingReview"></v-btn>
                </v-card-title>
                
                <v-card-text class="pa-4">
                    <div class="d-flex align-center mb-4 p-2 bg-grey-lighten-4 rounded-lg pa-3" v-if="product">
                        <v-avatar rounded size="48" class="mr-3 bg-white elevation-1">
                            <v-img :src="product.hinhAnh" cover></v-img>
                        </v-avatar>
                        <div>
                            <div class="font-weight-bold text-body-2 text-truncate" style="max-width: 300px;">{{ product.tenSanPham }}</div>
                            <div class="text-caption text-grey">{{ product.tenThuongHieu }}</div>
                        </div>
                    </div>

                    <div class="text-center mb-4">
                        <p class="text-subtitle-2 font-weight-bold mb-1">Chất lượng sản phẩm</p>
                        <v-rating
                            v-model="newReview.rating"
                            color="amber"
                            active-color="amber"
                            hover
                            size="x-large"
                        ></v-rating>
                    </div>

                    <v-textarea
                        v-model="newReview.comment"
                        label="Nhận xét của bạn"
                        placeholder="Hãy chia sẻ cảm nhận của bạn về sản phẩm này nhé..."
                        variant="outlined"
                        rows="4"
                        auto-grow
                        hide-details="auto"
                        bg-color="grey-lighten-5"
                    ></v-textarea>
                </v-card-text>

                <v-card-actions class="pa-4 pt-0">
                    <v-spacer></v-spacer>
                    <v-btn variant="text" class="text-none font-weight-bold" @click="showReviewModal = false" :disabled="submittingReview">Hủy</v-btn>
                    <v-btn 
                        color="black" 
                        variant="flat" 
                        class="text-none font-weight-bold px-6 rounded-pill" 
                        :loading="submittingReview"
                        @click="submitDirectReview"
                    >
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

.product-cat {
    color: #111;
}

.product-price {
    color: #111;
}

.size-guide {
    font-size: 0.9rem;
    color: #757575;
    text-decoration: none;
    font-weight: 600;
    border-bottom: 1px solid transparent;

    &:hover {
        border-bottom-color: #757575;
    }
}

.size-box {
    border: 1px solid #e5e5e5;
    border-radius: 4px;
    padding: 12px;
    text-align: center;
    font-size: 0.95rem;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        border-color: #000;
    }

    &.active {
        border-color: #000;
        background: #000;
        color: #fff;
    }
}

.desc-text {
    font-size: 1.05rem;
    line-height: 1.8;
    color: #111;
}

.spec-list {
    list-style: disc;

    li {
        font-size: 0.9rem;
        margin-bottom: 8px;
        color: #111;
        font-weight: 500;
    }
}

:deep(.v-expansion-panel-title) {
    font-weight: 800;
    font-size: 1.1rem;
    padding: 24px 0;
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
</style>
