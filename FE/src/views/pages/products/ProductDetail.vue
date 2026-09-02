<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import PromotionBar from '@/components/shared/PromotionBar.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

import { dichVuSanPhamPublic } from '@/services/public/dichVuSanPhamPublic';
import { dichVuKhachHang } from '@/services/public/dichVuKhachHang';
import api from '@/services/apiService';
import { useCartStore } from '@/stores/cartStore';
import { useToastStore } from '@/stores/toastStore';
import { useAuthStore } from '@/stores/authStore';
import { useSeoMeta } from '@/composables/useSeoMeta';
import { PATH } from '@/router/routePaths';
import { dichVuFile } from '@/services/core/dichVuFile';
import shoe1Img from '@/assets/images/products/cat_running.jpg';
import shoe2Img from '@/assets/images/products/cat_training.jpg';
import shoe3Img from '@/assets/images/products/cat_speed.jpg';
import shoe4Img from '@/assets/images/products/s4.jpg';
import shoe5Img from '@/assets/images/products/s7.jpg';
import shoe6Img from '@/assets/images/products/s11.jpg';

const FALLBACK_SHOES = [shoe4Img, shoe5Img, shoe6Img, shoe1Img, shoe2Img, shoe3Img];
const DEFAULT_SHOE_IMAGE = shoe4Img;

const getDeterministicFallback = (id) => {
    if (!id) return DEFAULT_SHOE_IMAGE;
    let hash = 0;
    const str = String(id);
    for (let i = 0; i < str.length; i++) {
        hash = (hash << 5) - hash + str.charCodeAt(i);
        hash |= 0;
    }
    const idx = Math.abs(hash) % FALLBACK_SHOES.length;
    return FALLBACK_SHOES[idx];
};

const route = useRoute();
const router = useRouter();
const cartStore = useCartStore();
const toastStore = useToastStore();
const authStore = useAuthStore();
const loading = ref(true);
const product = ref(null);
const userProfile = ref(null);
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

// Bộ đánh giá mẫu chân thực dành cho demo khi sản phẩm chưa có đánh giá thực tế
const DEMO_FALLBACK_REVIEWS = [
    {
        id: 'fb-rev-1',
        tenKhachHang: 'Nguyễn Hoàng Nam',
        avatarKhachHang: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&q=80',
        diemDanhGia: 5,
        ngayTao: Date.now() - 86400000 * 2,
        noiDung: 'Giày mang cực kỳ êm chân và nhẹ, đệm đàn hồi rất tốt khi chạy bộ cự ly dài. Form giày ôm vừa vặn, đóng gói 2 lớp hộp cẩn thận, giao hàng siêu nhanh!'
    },
    {
        id: 'fb-rev-2',
        tenKhachHang: 'Trần Thị Mai Anh',
        avatarKhachHang: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=120&q=80',
        diemDanhGia: 5,
        ngayTao: Date.now() - 86400000 * 5,
        noiDung: 'Màu sắc bên ngoài đẹp hơn cả trong ảnh chụp, chất vải dệt thoáng khí không bị bí chân dù mang cả ngày. Rất ưng ý với chất lượng phục vụ của shop!'
    },
    {
        id: 'fb-rev-3',
        tenKhachHang: 'Lê Minh Quân',
        avatarKhachHang: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=120&q=80',
        diemDanhGia: 5,
        ngayTao: Date.now() - 86400000 * 8,
        noiDung: 'Đã test chạy 10km sáng nay, độ bám đường cực tốt và nâng đỡ gót chân rất vững. Xứng đáng 5 sao trong tầm giá.'
    },
    {
        id: 'fb-rev-4',
        tenKhachHang: 'Phạm Thu Trang',
        avatarKhachHang: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80',
        diemDanhGia: 4,
        ngayTao: Date.now() - 86400000 * 12,
        noiDung: 'Giày đẹp, hoàn thiện tỉ mỉ từng đường kim mũi chỉ. Đi đúng size chân mang rất thoải mái, sẽ tiếp tục ủng hộ AeroStride!'
    }
];

const effectiveReviews = computed(() => {
    return reviews.value && reviews.value.length > 0 ? reviews.value : DEMO_FALLBACK_REVIEWS;
});

const displayTotalReviews = computed(() => {
    return reviews.value && reviews.value.length > 0 ? (totalReviews.value || reviews.value.length) : DEMO_FALLBACK_REVIEWS.length;
});

const displayAverageRating = computed(() => {
    if (reviews.value && reviews.value.length > 0) {
        return averageRating.value || '5.0';
    }
    return '4.9';
});

const displayRatingCounts = computed(() => {
    if (reviews.value && reviews.value.length > 0) {
        return ratingCounts.value;
    }
    return { 5: 3, 4: 1, 3: 0, 2: 0, 1: 0 };
});

const filteredReviews = computed(() => {
    const list = effectiveReviews.value;
    if (selectedFilter.value === 'all') return list;
    return list.filter((r) => Math.floor(r.diemDanhGia || r.rating || 5) === Number(selectedFilter.value));
});

// State cho xem chi tiết ảnh / Lightbox modal
const showImageLightbox = ref(false);
const lightboxIndex = ref(0);

const openImageLightbox = (index = activeSlide.value) => {
    if (!allImages.value || allImages.value.length === 0) return;
    lightboxIndex.value = typeof index === 'number' ? index : activeSlide.value;
    showImageLightbox.value = true;
};

const nextLightboxImage = () => {
    if (allImages.value.length <= 1) return;
    lightboxIndex.value = (lightboxIndex.value + 1) % allImages.value.length;
};

const prevLightboxImage = () => {
    if (allImages.value.length <= 1) return;
    lightboxIndex.value = (lightboxIndex.value - 1 + allImages.value.length) % allImages.value.length;
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
        const data = res.data?.data || res.data;
        if (data) {
            const list = data.content || (Array.isArray(data) ? data : []);
            reviews.value = list;
            totalReviews.value = data.totalElements ?? list.length;

            // Tính toán breakdown
            const counts = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
            let sum = 0;

            if (list.length > 0) {
                list.forEach((r) => {
                    const score = Number(r.diemDanhGia || r.rating || 5);
                    if (score >= 1 && score <= 5) {
                        counts[Math.floor(score)] = (counts[Math.floor(score)] || 0) + 1;
                    }
                    sum += score;
                });
                averageRating.value = (sum / list.length).toFixed(1);
            } else {
                averageRating.value = 0;
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
    fetchUserProfile();
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

import { isFavorite as checkIsFavorite, toggleFavorite as toggleFavUtil } from '@/utils/favoritesUtil';

// Cập nhật SEO và trạng thái Yêu thích khi product data load xong
watch(product, (newProduct) => {
    if (newProduct) {
        setProductSeo(newProduct);
        isFavorite.value = checkIsFavorite(newProduct.id);
    }
});

const generateFallbackDescription = (p) => {
    if (!p) return '';
    const name = p.tenSanPham || 'Sản phẩm AeroStride';
    const brand = p.tenThuongHieu || 'AeroStride';
    const material = p.tenChatLieu || 'vải dệt Mesh cao cấp';
    const sole = p.tenDeGiay || 'đế cao su đàn hồi giảm chấn';
    const purpose = p.tenMucDichChay || 'chạy bộ và luyện tập thể thao đa năng';
    const origin = p.tenXuatXu || 'Chính hãng';

    return `${name} là dòng giày thể thao cao cấp từ ${brand}, được nghiên cứu và thiết kế chuyên biệt cho mục đích ${purpose}.

Đặc điểm nổi bật:
• Thân giày (Upper): Sử dụng chất liệu ${material} siêu nhẹ, cấu trúc lưới thoáng khí đa chiều giúp lưu thông không khí tối đa, hạn chế tích tụ nhiệt và mồ hôi trong suốt quá trình vận động cường độ cao.
• Đế đệm (Midsole & Outsole): Hệ thống ${sole} với cấu trúc đệm phản hồi năng lượng vượt trội, phân tán áp lực đồng đều, hỗ trợ bảo vệ tối đa khớp gối và gót chân trên mọi địa hình.
• Thiết kế công thái học: Ôm sát vòm bàn chân tự nhiên, mang lại cảm giác vừa vặn, linh hoạt và ổn định trong từng sải chân bứt phá.
• Tiêu chuẩn chất lượng: ${origin}, trải qua quy trình kiểm định nghiêm ngặt đạt chuẩn độ bền thể thao chuyên nghiệp.

Sản phẩm là sự lựa chọn hoàn hảo cho cả luyện tập thể thao hàng ngày lẫn phối đồ phong cách năng động.`;
};

const productDescription = computed(() => {
    const desc = product.value?.moTaChiTiet || product.value?.moTa || product.value?.moTaNgan;
    if (desc && typeof desc === 'string' && desc.trim() && desc.trim() !== 'null' && desc.trim() !== 'undefined') {
        return desc.trim();
    }
    return generateFallbackDescription(product.value);
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
    const lower = v.toLowerCase().trim();
    if (!lower || lower === 'null' || lower === 'undefined' || lower === '[object object]' || lower === 'string') return true;
    return lower.includes('via.placeholder.com') || lower.includes('placeholder.com') || lower.includes('dummyimage.com');
};

const resolveImg = (v) => {
    if (!v || isInvalidImage(v)) return '';
    if (typeof v !== 'string') return '';
    const clean = v.trim();
    if (isAbsoluteUrl(clean)) return clean;
    return dichVuFile.layUrlFile(clean.replace(/^\/+/, ''));
};

import { optimizeImageUrl } from '@/utils/imageDisplay';

const getValidImgUrl = (raw, width = 800, quality = 75) => {
    if (!raw) return null;
    let url = typeof raw === 'object' ? (raw.duongDanAnh || raw.hinhAnh || raw.url || '') : raw;
    const resolved = resolveImg(url);
    if (!resolved || isInvalidImage(resolved)) return null;
    return optimizeImageUrl(resolved, width, quality);
};

const handleImgError = (e) => {
    if (!e || !e.target) return;
    if (e.target.getAttribute('data-fallback') === 'true') return;
    e.target.setAttribute('data-fallback', 'true');
    e.target.src = getDeterministicFallback(product.value?.id || product.value?.maSanPham);
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
        product.value.variants.forEach((v) => {
            if (v.tenMauSac && v.maMauHex) {
                map[v.tenMauSac] = v.maMauHex;
            }
        });
    }
    const defaultHex = {
        Đen: '#0B1329',
        Trắng: '#FFFFFF',
        Đỏ: '#E53935',
        'Xanh dương': '#1976D2',
        'Xanh lá': '#4CAF50',
        Vàng: '#FFEB3B',
        Xám: '#9E9E9E',
        Hồng: '#E91E63',
        Cam: '#FF9800'
    };
    return { ...defaultHex, ...map };
});

const discountPercent = computed(() => {
    if (selectedVariant.value && selectedVariant.value.phanTramGiam) {
        return Number(selectedVariant.value.phanTramGiam) || 0;
    }
    return 0;
});

const currentPrice = computed(() => {
    if (selectedVariant.value) {
        return selectedVariant.value.giaBan || 0;
    }
    return product.value?.minPrice || 0;
});

const oldPrice = computed(() => {
    if (selectedVariant.value && selectedVariant.value.giaGoc && Number(selectedVariant.value.giaGoc) > Number(selectedVariant.value.giaBan)) {
        return selectedVariant.value.giaGoc;
    }
    return null;
});

const activeDiscountName = computed(() => {
    return selectedVariant.value?.tenDotGiamGia || null;
});

const formattedCurrentPrice = computed(() => formatPrice(currentPrice.value));
const formattedOldPrice = computed(() => (oldPrice.value ? formatPrice(oldPrice.value) : null));

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
        showStockAlert(
            'Sản phẩm đã hết hàng',
            'Phiên bản màu sắc và kích thước này hiện đã hết hàng trong kho. Vui lòng chọn phiên bản khác.'
        );
        return;
    }

    const inCart = cartStore.items.find((i) => i.idChiTietSanPham === variant.id)?.soLuong || 0;
    const availableStock = variant.soLuong || 0;

    if (inCart >= availableStock && availableStock > 0) {
        // Đã có tối đa số lượng trong giỏ hàng, chuyển hướng thẳng đến checkout
        router.push('/checkout');
        return;
    }

    if (inCart + selectedQuantity.value > availableStock && availableStock > 0) {
        const remaining = availableStock - inCart;
        showStockAlert(
            'Không đủ số lượng trong kho',
            `Sản phẩm này hiện chỉ còn ${availableStock} sản phẩm trong kho. Bạn đã có ${inCart} sản phẩm trong giỏ hàng, chỉ có thể mua thêm tối đa ${remaining} sản phẩm nữa.`
        );
        toastStore.showToast(`Số lượng trong kho không đủ (đã có ${inCart} trong giỏ, còn lại ${remaining})`, 'warning');
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
            giaBan: currentPrice.value || variant.giaBan || displayPrice.value || 0,
            giaGoc: oldPrice.value || null,
            phanTramGiam: discountPercent.value || null,
            tenDotGiamGia: activeDiscountName.value || null,
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

const placeholderAngles = ['Ảnh chính', 'Góc nghiêng', 'Đế giày', 'Góc sau', 'Chi tiết'];

const allImages = computed(() => {
    const images = [];
    const addedUrls = new Set();

    const addImg = (url, label) => {
        const resolved = getValidImgUrl(url, 800, 75);
        const thumb = getValidImgUrl(url, 160, 60);
        if (resolved && !addedUrls.has(resolved)) {
            addedUrls.add(resolved);
            images.push({
                duongDanAnh: resolved,
                thumbnailUrl: thumb || resolved,
                label: label || 'Hình ảnh'
            });
        }
    };

    if (selectedColor.value && product.value?.variants) {
        // Khi đã chọn màu sắc: CHỈ hiển thị hình ảnh thuộc biến thể / màu sắc đang chọn
        const colorVariants = product.value.variants.filter((v) => v.tenMauSac === selectedColor.value);

        // 1. Ưu tiên ảnh của biến thể đang chọn nếu có
        if (selectedVariant.value) {
            const v = selectedVariant.value;
            if (v.hinhAnh) addImg(v.hinhAnh, `Màu ${v.tenMauSac}`);
            if (v.images && v.images.length > 0) {
                v.images.forEach((img) => addImg(img.duongDanAnh || img.hinhAnh || img.url, `Màu ${v.tenMauSac}`));
            }
        }

        // 2. Thêm tất cả ảnh của màu sắc này
        colorVariants.forEach((v) => {
            if (v.hinhAnh) addImg(v.hinhAnh, `Màu ${v.tenMauSac}`);
            if (v.images && v.images.length > 0) {
                v.images.forEach((img) => addImg(img.duongDanAnh || img.hinhAnh || img.url, `Màu ${v.tenMauSac}`));
            }
        });

        // 3. Fallback ảnh sản phẩm chính nếu biến thể chưa có ảnh riêng
        if (images.length === 0 && product.value?.hinhAnh) {
            addImg(product.value.hinhAnh, 'Ảnh chính');
        }
    } else {
        // Khi chưa chọn màu sắc:
        // 1. Ảnh chính của sản phẩm
        if (product.value?.hinhAnh) {
            addImg(product.value.hinhAnh, 'Ảnh chính');
        }

        // 2. Mỗi màu sắc chỉ lấy 1 ảnh đại diện
        if (product.value?.variants) {
            const seenColors = new Set();
            product.value.variants.forEach((v) => {
                if (v.tenMauSac && !seenColors.has(v.tenMauSac)) {
                    seenColors.add(v.tenMauSac);
                    const raw = v.hinhAnh || (v.images && v.images.length > 0 ? v.images[0].duongDanAnh || v.images[0].hinhAnh : null);
                    if (raw) {
                        addImg(raw, `Màu ${v.tenMauSac}`);
                    }
                }
            });
        }
    }

    // Nếu không có ảnh nào -> thêm ảnh chính fallback
    if (images.length === 0) {
        const baseFallback = getDeterministicFallback(product.value?.id || product.value?.maSanPham);
        addImg(baseFallback, 'Ảnh chính');
    }

    // Đảm bảo luôn có tối thiểu 4 góc chụp sắc nét cho buổi demo mượt mà
    const prodId = product.value?.id || product.value?.maSanPham || 'demo';
    let baseOffset = 0;
    for (let i = 0; i < prodId.length; i++) baseOffset += prodId.charCodeAt(i);

    let fallbackIdx = 0;
    while (images.length < 4 && fallbackIdx < FALLBACK_SHOES.length) {
        const candidateImg = FALLBACK_SHOES[(baseOffset + fallbackIdx) % FALLBACK_SHOES.length];
        if (!addedUrls.has(candidateImg)) {
            addedUrls.add(candidateImg);
            images.push({
                duongDanAnh: candidateImg,
                thumbnailUrl: candidateImg,
                label: placeholderAngles[images.length] || 'Chi tiết'
            });
        }
        fallbackIdx++;
    }

    return images;
});

// Tự động preload các ảnh của sản phẩm để chuyển slide và hiển thị tức thì
watch(
    allImages,
    (imgs) => {
        if (imgs && imgs.length > 0) {
            imgs.forEach((img) => {
                if (img.duongDanAnh) {
                    const imgObj = new Image();
                    imgObj.src = img.duongDanAnh;
                }
                if (img.thumbnailUrl && img.thumbnailUrl !== img.duongDanAnh) {
                    const thumbObj = new Image();
                    thumbObj.src = img.thumbnailUrl;
                }
            });
        }
    },
    { immediate: true }
);

const colorVariantPreviews = computed(() => {
    if (!product.value?.variants) return [];
    const map = new Map();
    product.value.variants.forEach((v) => {
        if (v.tenMauSac && !map.has(v.tenMauSac)) {
            const rawImg = v.hinhAnh || (v.images && v.images.length > 0 ? v.images[0].duongDanAnh || v.images[0].hinhAnh : null);
            const img = getValidImgUrl(rawImg, 160, 60) || DEFAULT_SHOE_IMAGE;
            map.set(v.tenMauSac, { color: v.tenMauSac, img: img });
        }
    });
    return Array.from(map.values());
});

const onSelectColorPreview = (cv) => {
    selectedColor.value = cv.color;
    if (cv.img) {
        const idx = allImages.value.findIndex((img) => img.duongDanAnh === cv.img);
        if (idx !== -1) {
            activeSlide.value = idx;
        }
    }
};

watch(
    allImages,
    (newImages) => {
        if (newImages && newImages.length > 0) {
            activeSlide.value = 0;
        }
    },
    { immediate: true }
);

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
            'Vượt quá số lượng có sẵn',
            `Sản phẩm này hiện chỉ còn tối đa ${maxQuantity.value} sản phẩm. Bạn không thể chọn số lượng lớn hơn.`
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
        showStockAlert('Vượt quá số lượng có sẵn', `Sản phẩm này hiện chỉ còn tối đa ${maxQuantity.value} sản phẩm.`);
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
        showStockAlert(
            'Sản phẩm đã hết hàng',
            'Phiên bản màu sắc và kích thước này hiện đã hết hàng. Vui lòng chọn phiên bản khác.'
        );
        return;
    }

    const inCart = cartStore.items.find((i) => i.idChiTietSanPham === variant.id)?.soLuong || 0;
    const availableStock = variant.soLuong || 0;

    if (inCart >= availableStock) {
        showStockAlert(
            'Không đủ số lượng trong kho',
            `Sản phẩm này hiện chỉ còn ${availableStock} sản phẩm trong kho và bạn đã thêm đủ ${inCart} sản phẩm vào giỏ hàng. Không thể thêm tiếp.`
        );
        toastStore.showToast(`Sản phẩm trong giỏ đã đạt tối đa số lượng trong kho (${availableStock})`, 'warning');
        return;
    }

    if (inCart + selectedQuantity.value > availableStock) {
        const maxCanAdd = availableStock - inCart;
        showStockAlert(
            'Không đủ số lượng trong kho',
            `Sản phẩm này hiện chỉ còn ${availableStock} sản phẩm trong kho. Bạn đã có ${inCart} sản phẩm trong giỏ hàng, chỉ có thể thêm tối đa ${maxCanAdd} sản phẩm nữa.`
        );
        toastStore.showToast(`Số lượng trong kho không đủ (đã có ${inCart} trong giỏ, còn lại ${maxCanAdd})`, 'warning');
        return;
    }

    if (selectedQuantity.value > availableStock) {
        showStockAlert('Vượt quá số lượng có sẵn', `Phiên bản này hiện chỉ còn tối đa ${availableStock} sản phẩm.`);
        selectedQuantity.value = availableStock;
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
            giaBan: currentPrice.value || variant.giaBan || displayPrice.value || 0,
            giaGoc: oldPrice.value || null,
            phanTramGiam: discountPercent.value || null,
            tenDotGiamGia: activeDiscountName.value || null,
            soLuongTonKho: variant.soLuong || 0
        });
        if (result?.success) {
            toastStore.showToast('Đã thêm vào giỏ hàng', 'success');
            cartStore.openDrawer();
        } else {
            showStockAlert('Không thể thêm vào giỏ hàng', result?.message || 'Số lượng sản phẩm trong kho không đủ.');
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
    const wasFavorite = checkIsFavorite(product.value.id);
    toggleFavUtil(product.value.id);
    isFavorite.value = !wasFavorite;
    if (isFavorite.value) {
        toastStore.showToast('Đã thêm vào danh sách yêu thích', 'success');
    } else {
        toastStore.showToast('Đã huỷ yêu thích', 'info');
    }
};
</script>

<template>
    <div class="product-detail-page bg-white min-vh-100">
        <MainHeader />

        <div class="header-spacing"></div>
        <PromotionBar />

        <v-container class="mt-2 mt-md-6" v-if="product">
            <v-row>
                <!-- Left: Image Gallery -->
                <v-col cols="12" md="6" lg="6" class="image-gallery">
                    <div class="product-gallery-wrapper">
                        <!-- Main Image Box (Vuông & To) -->
                        <div
                            class="rounded-2xl bg-grey-lighten-4 mb-4 elevation-1 position-relative overflow-hidden main-image-box-custom"
                        >
                            <!-- Floating Zoom Button -->
                            <v-btn
                                icon
                                variant="flat"
                                color="white"
                                class="position-absolute zoom-floating-btn"
                                title="Phóng to xem chi tiết ảnh"
                                @click.stop="openImageLightbox(activeSlide)"
                            >
                                <v-icon color="#1e257c" size="20">mdi-magnify-plus-outline</v-icon>
                                <v-tooltip activator="parent" location="bottom">Bấm để phóng to ảnh</v-tooltip>
                            </v-btn>

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
                                    <v-carousel-item
                                        v-for="(img, i) in allImages"
                                        :key="i"
                                        :src="img.duongDanAnh"
                                        cover
                                        class="cursor-pointer"
                                        @click="openImageLightbox(i)"
                                    >
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
                                    <div class="mb-3 pa-4 rounded-circle" style="background: #f0f4ff">
                                        <v-icon size="48" style="color: #1e257c">mdi-shoe-sneaker</v-icon>
                                    </div>
                                    <h4 class="text-subtitle-1 font-weight-bold mb-1" style="color: #1e257c">{{ product.tenSanPham }}</h4>
                                    <p class="text-caption text-grey">Chưa có hình ảnh trực quan</p>
                                </div>
                            </template>
                        </div>

                        <!-- Multi-Slot Thumbnail Strip (To lên ~20px) -->
                        <div class="thumbnail-strip-section mb-4">
                            <div class="d-flex align-center justify-space-between mb-3">
                                <span class="text-caption font-weight-bold" style="color: #1e257c">
                                    <v-icon size="15" class="mr-1" style="color: #1e257c">mdi-view-grid-outline</v-icon>
                                    Bộ sưu tập hình ảnh ({{ allImages.length }} hình ảnh)
                                </span>
                                <span class="text-caption text-slate-500 font-weight-medium">
                                    <v-icon size="13" class="mr-0.5">mdi-cursor-default-click-outline</v-icon>
                                    Bấm vào ảnh để xem chi tiết
                                </span>
                            </div>

                            <div class="d-flex flex-wrap align-center ga-3 thumbnail-strip-container">
                                <template v-if="allImages.length > 0">
                                    <v-card
                                        v-for="(img, i) in allImages"
                                        :key="'img-' + i"
                                        class="thumbnail-card rounded-xl overflow-hidden position-relative"
                                        :elevation="activeSlide === i ? 3 : 0"
                                        :class="{ 'thumbnail-active': activeSlide === i }"
                                        @click="activeSlide = i"
                                    >
                                        <v-img
                                            :src="img.thumbnailUrl || img.duongDanAnh"
                                            cover
                                            class="w-100 h-100"
                                            loading="eager"
                                            decoding="async"
                                        >
                                            <template #placeholder>
                                                <div class="d-flex align-center justify-center fill-height bg-grey-lighten-4">
                                                    <v-icon size="18" color="grey">mdi-image-outline</v-icon>
                                                </div>
                                            </template>
                                        </v-img>
                                        <v-tooltip activator="parent" location="top">Xem ảnh {{ i + 1 }}</v-tooltip>
                                    </v-card>
                                </template>

                                <template v-if="allImages.length < 4">
                                    <v-card
                                        v-for="(angleLabel, idx) in placeholderAngles.slice(allImages.length)"
                                        :key="'angle-' + idx"
                                        class="placeholder-card rounded-xl bg-grey-lighten-5 overflow-hidden d-flex flex-column align-center justify-center text-center pa-1"
                                        style="opacity: 0.85"
                                    >
                                        <v-icon color="#1e257c" size="22" class="mb-1">mdi-camera-outline</v-icon>
                                        <span style="font-size: 0.7rem; color: #64748b; font-weight: 600; line-height: 1.1">{{
                                            angleLabel
                                        }}</span>
                                    </v-card>
                                </template>
                            </div>
                        </div>
                    </div>
                </v-col>

                <!-- Right: Product Info -->
                <v-col cols="12" md="6" lg="6">
                    <div class="sticky-info-panel px-md-6">
                        <!-- Brand tag above the title -->
                        <div class="product-brand-tag text-uppercase mb-2">
                            {{ product.tenThuongHieu || 'AEROSTRIDE' }}
                        </div>

                        <!-- Product Title -->
                        <h1 class="product-title-new mb-2">{{ product.tenSanPham }}</h1>

                        <!-- Ratings, Review & Sold Count -->
                        <div class="product-meta-row d-flex align-center gap-2 mb-6">
                            <div class="rating-stars-wrapper d-flex align-center">
                                <v-icon v-for="star in 5" :key="star" size="14" color="amber" class="mr-0.5">
                                    {{ star <= Math.round(Number(displayAverageRating)) ? 'mdi-star' : 'mdi-star-outline' }}
                                </v-icon>
                                <span class="rating-value-text ml-1">{{ displayAverageRating }}</span>
                            </div>
                            <span class="meta-separator text-grey-lighten-1">|</span>
                            <span class="reviews-count-text">({{ displayTotalReviews }} đánh giá)</span>
                            <span class="meta-separator text-grey-lighten-1">|</span>
                            <span class="sold-count-text">Đã bán {{ product.daBan || 28 }}</span>
                        </div>

                        <!-- Price Section -->
                        <div class="product-price-row d-flex align-center flex-wrap gap-4 mb-6">
                            <span class="current-price-label-new">{{ formattedCurrentPrice }}</span>
                            <span v-if="discountPercent > 0 && formattedOldPrice" class="old-price-label-new">
                                {{ formattedOldPrice }}
                            </span>
                            <span v-if="discountPercent > 0" class="discount-badge-new"> -{{ discountPercent }}% </span>
                            <v-chip v-if="activeDiscountName" color="error" size="small" variant="tonal" prepend-icon="mdi-tag-outline" class="font-weight-bold ml-2">
                                {{ activeDiscountName }}
                            </v-chip>
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
                                    <div class="color-dot-inner" :style="{ backgroundColor: colorHexMap[color] || '#CCCCCC' }"></div>
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
                                    <button class="qty-btn" :disabled="selectedQuantity <= 1" @click="selectedQuantity--">
                                        <v-icon size="14">mdi-minus</v-icon>
                                    </button>
                                    <input
                                        type="text"
                                        inputmode="numeric"
                                        class="qty-input"
                                        :value="selectedQuantity"
                                        maxlength="6"
                                        @keypress="onlyNumbers"
                                        @input="onQuantityInput"
                                        @blur="onQuantityBlur"
                                    />
                                    <button class="qty-btn" @click="handleIncrement">
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
                            <v-btn flat color="#0B1329" class="buy-now-btn-new block w-100" @click="buyNow"> MUA NGAY </v-btn>
                        </div>

                        <!-- Description Details Section -->
                        <div class="product-desc-section mt-8 pt-6 border-top">
                            <h3 class="desc-section-title mb-3">Mô tả sản phẩm</h3>
                            <p class="desc-text-new">{{ productDescription }}</p>
                            <div class="d-flex flex-wrap gap-3 mt-4">
                                <v-chip size="small" variant="flat" color="#F1F5F9" class="font-weight-medium text-grey-darken-3" v-if="product.tenXuatXu">
                                    <v-icon start size="14" color="grey-darken-2">mdi-earth</v-icon> Xuất xứ: {{ product.tenXuatXu }}
                                </v-chip>
                                <v-chip size="small" variant="flat" color="#F1F5F9" class="font-weight-medium text-grey-darken-3" v-if="product.maSanPham || product.id">
                                    <v-icon start size="14" color="grey-darken-2">mdi-barcode</v-icon> Mã: {{ product.maSanPham || product.id }}
                                </v-chip>
                                <v-chip size="small" variant="flat" color="#F1F5F9" class="font-weight-medium text-grey-darken-3" v-if="product.tenChatLieu">
                                    <v-icon start size="14" color="grey-darken-2">mdi-tshirt-crew-outline</v-icon> Chất liệu: {{ product.tenChatLieu }}
                                </v-chip>
                                <v-chip size="small" variant="flat" color="#F1F5F9" class="font-weight-medium text-grey-darken-3" v-if="product.tenDeGiay">
                                    <v-icon start size="14" color="grey-darken-2">mdi-shoe-print</v-icon> Đế giày: {{ product.tenDeGiay }}
                                </v-chip>
                                <v-chip size="small" variant="flat" color="#F1F5F9" class="font-weight-medium text-grey-darken-3" v-if="product.tenMucDichChay">
                                    <v-icon start size="14" color="grey-darken-2">mdi-run-fast</v-icon> Mục đích: {{ product.tenMucDichChay }}
                                </v-chip>
                            </div>
                        </div>
                    </div>
                </v-col>
            </v-row>

            <!-- Reviews Section -->
            <div class="reviews-section mt-16 pt-8 border-top">
                <div class="d-flex align-center justify-space-between flex-wrap ga-4 mb-8">
                    <div>
                        <h2 class="text-h4 font-weight-semibold text-primary mb-1">Đánh Giá Sản Phẩm</h2>
                        <p class="text-caption text-grey mb-0">Ý kiến từ khách hàng đã sử dụng sản phẩm</p>
                    </div>
                </div>

                <div v-if="reviewsLoading" class="text-center py-8">
                    <v-progress-circular indeterminate color="primary"></v-progress-circular>
                </div>
                <div v-else-if="effectiveReviews.length > 0">
                    <v-card variant="outlined" class="mb-10 rounded-xl border-grey-lighten-2">
                        <v-row class="ma-0">
                            <v-col cols="12" md="4" class="d-flex align-center justify-center bg-grey-lighten-4 pa-6">
                                <div class="text-center">
                                    <div class="text-h2 font-weight-semibold text-amber-darken-3">{{ displayAverageRating }}</div>
                                    <v-rating
                                        :model-value="Number(displayAverageRating) || 5"
                                        color="amber"
                                        active-color="amber"
                                        half-increments
                                        readonly
                                        size="large"
                                        class="mb-2"
                                    ></v-rating>
                                    <div class="text-body-1 text-grey-darken-1 font-weight-medium">{{ displayTotalReviews }} đánh giá</div>
                                </div>
                            </v-col>

                            <!-- Rating Bars & Filters -->
                            <v-col cols="12" md="8" class="pa-6">
                                <div class="d-flex flex-wrap ga-2 mb-2">
                                    <v-chip
                                        :variant="selectedFilter === 'all' ? 'flat' : 'outlined'"
                                        :color="selectedFilter === 'all' ? 'black' : 'grey-darken-1'"
                                        @click="selectedFilter = 'all'"
                                        class="font-weight-bold px-4"
                                    >
                                        Tất cả ({{ displayTotalReviews }})
                                    </v-chip>
                                    <v-chip
                                        v-for="star in [5, 4, 3, 2, 1]"
                                        :key="star"
                                        :variant="selectedFilter === star ? 'flat' : 'outlined'"
                                        :color="selectedFilter === star ? 'black' : 'grey-darken-1'"
                                        @click="selectedFilter = star"
                                        class="font-weight-bold px-4"
                                    >
                                        {{ star }} Sao ({{ displayRatingCounts[star] || 0 }})
                                    </v-chip>
                                </div>
                            </v-col>
                        </v-row>
                    </v-card>

                    <v-row>
                        <v-col v-for="review in filteredReviews" :key="review.id" cols="12" md="6">
                            <v-card variant="outlined" class="pa-4 rounded-xl border-grey-lighten-2 h-100 bg-white elevation-1">
                                <div class="d-flex align-center mb-3">
                                    <v-avatar color="grey-lighten-3" size="44" class="mr-3 border border-grey-lighten-2">
                                        <v-img
                                            :src="
                                                review.khachHang?.anhDaiDien ||
                                                review.avatarKhachHang ||
                                                'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&q=80'
                                            "
                                            alt="avatar"
                                            cover
                                        ></v-img>
                                    </v-avatar>
                                    <div class="flex-grow-1">
                                        <div class="d-flex align-center justify-space-between mb-1">
                                            <div class="font-weight-bold text-slate-900">
                                                {{ review.khachHang?.ten || review.tenKhachHang || 'Khách hàng ẩn danh' }}
                                            </div>
                                            <span class="text-caption text-grey-darken-1 font-weight-medium">
                                                {{ review.ngayTao ? new Date(review.ngayTao).toLocaleDateString('vi-VN') : 'Gần đây' }}
                                            </span>
                                        </div>
                                        <v-rating
                                            :model-value="Number(review.diemDanhGia || review.rating || 5)"
                                            color="amber-darken-2"
                                            active-color="amber-darken-2"
                                            density="compact"
                                            size="small"
                                            readonly
                                        ></v-rating>
                                    </div>
                                </div>
                                <div class="text-body-2 text-grey-darken-3 mt-2 px-1 font-weight-medium" style="line-height: 1.6">
                                    {{ review.noiDung || review.comment || 'Khách hàng không để lại bình luận chi tiết.' }}
                                </div>
                            </v-card>
                        </v-col>
                    </v-row>
                </div>

                <div
                    v-else
                    class="text-center py-12 bg-grey-lighten-4 rounded-xl border-dashed border-grey-lighten-1"
                    style="border-width: 2px"
                >
                    <v-icon size="64" color="grey-lighten-1" class="mb-4">mdi-comment-text-outline</v-icon>
                    <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">Chưa có đánh giá nào</h3>
                    <p class="text-body-1 text-grey">Hãy là người đầu tiên trải nghiệm và đánh giá sản phẩm này.</p>
                    <v-btn
                        v-if="authStore.isLoggedIn"
                        color="black"
                        variant="outlined"
                        rounded="pill"
                        class="mt-4 font-weight-bold px-6"
                        @click="handleWriteReview"
                    >
                        Viết đánh giá ngay
                    </v-btn>
                </div>
            </div>

            <!-- Recommended Products Section -->
            <div class="recommended-section mt-10 pt-6 border-top" v-if="recommendedProducts.length > 0">
                <h2 class="text-h5 text-md-h4 font-weight-bold mb-6 text-center text-primary">Có Thể Bạn Cũng Thích</h2>
                <v-row class="products-grid-row">
                    <v-col v-for="p in recommendedProducts" :key="p.id" cols="6" sm="6" md="4" lg="3" class="pa-2 pa-sm-3">
                        <div class="product-card-placeholder" @click="$router.push(`/product/${p.id}`)">
                            <!-- Image Placeholder -->
                            <div class="image-box-placeholder mb-3">
                                <img
                                    :src="getValidImgUrl(p.hinhAnh) || DEFAULT_SHOE_IMAGE"
                                    :alt="p.tenSanPham"
                                    style="width: 100%; height: 100%; object-fit: cover"
                                    referrerpolicy="no-referrer"
                                    @error="(e) => (e.target.src = DEFAULT_SHOE_IMAGE)"
                                />
                            </div>

                            <!-- Content -->
                            <div class="product-info text-left">
                                <span class="promo-label">{{ p.tenThuongHieu || 'AEROSTRIDE' }}</span>
                                <h4 class="product-name text-truncate">{{ p.tenSanPham }}</h4>
                                <p class="product-price font-weight-bold">{{ formatPrice(p.giaBanThapNhat) }}</p>
                            </div>
                        </div>
                    </v-col>
                </v-row>
            </div>
        </v-container>
        <v-container v-else-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="primary"></v-progress-circular>
        </v-container>

        <v-container v-else class="text-center py-16 my-12">
            <v-icon size="80" color="grey-lighten-1" class="mb-4">mdi-shoe-sneaker</v-icon>
            <h2 class="text-h5 font-weight-bold mb-2">Sản phẩm không tồn tại hoặc đã bị gỡ bỏ</h2>
            <p class="text-body-1 text-grey-darken-1 mb-6">Rất tiếc, mẫu giày bạn đang tìm kiếm không có sẵn trong hệ thống.</p>
            <v-btn color="#2962FF" class="rounded-xl px-8 text-none font-weight-bold" height="48" @click="router.push(PATH.SHOES)">
                Khám phá danh sách giày
            </v-btn>
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
                    <v-btn
                        color="primary"
                        variant="flat"
                        rounded="pill"
                        class="px-8 font-weight-medium text-none"
                        @click="stockAlertModal.show = false"
                    >
                        Đã hiểu
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>



        <!-- Image Detail / Lightbox Modal -->
        <v-dialog v-model="showImageLightbox" max-width="1000" class="image-lightbox-dialog">
            <v-card class="bg-slate-950 text-white rounded-2xl overflow-hidden elevation-24 position-relative">
                <!-- Header Toolbar -->
                <div class="d-flex align-center justify-space-between px-4 py-3 bg-slate-900 border-b border-slate-800">
                    <div class="d-flex align-center ga-2">
                        <v-icon color="#60a5fa" size="22">mdi-image-search-outline</v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-white text-truncate" style="max-width: 500px">
                            {{ product?.tenSanPham || 'Chi tiết hình ảnh' }}
                        </span>
                        <v-chip size="x-small" color="primary" variant="flat" class="font-weight-bold ml-2">
                            {{ lightboxIndex + 1 }} / {{ allImages.length }}
                        </v-chip>
                    </div>
                    <v-btn icon="mdi-close" variant="text" color="white" size="small" @click="showImageLightbox = false"></v-btn>
                </div>

                <!-- Main Preview Area -->
                <div class="position-relative d-flex align-center justify-center bg-black pa-4" style="min-height: 520px; max-height: 72vh">
                    <v-img
                        v-if="allImages[lightboxIndex]"
                        :src="allImages[lightboxIndex].duongDanAnh"
                        contain
                        max-height="68vh"
                        class="w-100 rounded-lg"
                    >
                        <template #placeholder>
                            <div class="d-flex align-center justify-center fill-height">
                                <v-progress-circular indeterminate color="primary"></v-progress-circular>
                            </div>
                        </template>
                    </v-img>

                    <!-- Prev/Next Controls -->
                    <template v-if="allImages.length > 1">
                        <v-btn
                            icon="mdi-chevron-left"
                            variant="flat"
                            color="rgba(15, 23, 42, 0.75)"
                            class="position-absolute text-white"
                            style="left: 16px; top: 50%; transform: translateY(-50%)"
                            size="large"
                            @click="prevLightboxImage"
                        ></v-btn>
                        <v-btn
                            icon="mdi-chevron-right"
                            variant="flat"
                            color="rgba(15, 23, 42, 0.75)"
                            class="position-absolute text-white"
                            style="right: 16px; top: 50%; transform: translateY(-50%)"
                            size="large"
                            @click="nextLightboxImage"
                        ></v-btn>
                    </template>
                </div>

                <!-- Footer Strip of Thumbnails -->
                <div v-if="allImages.length > 1" class="px-4 py-3 bg-slate-900 border-t border-slate-800 d-flex justify-center ga-3 overflow-x-auto">
                    <div
                        v-for="(img, idx) in allImages"
                        :key="'lb-thumb-' + idx"
                        class="lightbox-thumb-item rounded-lg overflow-hidden cursor-pointer"
                        :class="{ 'lightbox-thumb-active': lightboxIndex === idx }"
                        @click="lightboxIndex = idx"
                    >
                        <v-img :src="img.thumbnailUrl || img.duongDanAnh" cover width="64" height="64"></v-img>
                    </div>
                </div>
            </v-card>
        </v-dialog>

        <!-- Main Footer -->
        <MainFooter />
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
    border: 1px solid #dfe5ef;
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

.main-image-box-custom {
    aspect-ratio: 1 / 1 !important;
    width: 100%;
    min-height: 400px;
    max-height: 580px;
    border: 1px solid #e2e8f0;
    margin: 0 auto;
    cursor: pointer;
    background: #f8fafc;
}

@media (max-width: 960px) {
    .main-image-box-custom {
        min-height: 340px !important;
        max-height: 480px !important;
        aspect-ratio: 1 / 1 !important;
    }
    .sticky-info-panel {
        position: relative;
        top: 0;
        padding: 0;
        margin-top: 24px !important;
    }
}

@media (max-width: 600px) {
    .main-image-box-custom {
        min-height: 280px !important;
        max-height: 360px !important;
        aspect-ratio: 1 / 1 !important;
    }
}

.zoom-floating-btn {
    top: 16px;
    right: 68px;
    z-index: 10;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
    border-radius: 50% !important;
    width: 44px !important;
    height: 44px !important;
    transition: all 0.2s ease;

    &:hover {
        transform: scale(1.08);
        box-shadow: 0 6px 16px rgba(30, 37, 124, 0.2) !important;
    }
}

.thumbnail-card {
    width: 84px;
    height: 84px;
    aspect-ratio: 1;
    border: 2px solid #e2e8f0;
    cursor: pointer;
    transition: all 0.2s ease;
    flex-shrink: 0;
    background: #ffffff;

    &:hover {
        border-color: #1e257c;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(30, 37, 124, 0.18) !important;
    }

    &.thumbnail-active {
        border-color: #1e257c !important;
        box-shadow: 0 4px 14px rgba(30, 37, 124, 0.3) !important;
    }
}

.placeholder-card {
    width: 84px;
    height: 84px;
    aspect-ratio: 1;
    border: 1.5px dashed #cbd5e1;
    flex-shrink: 0;
}

.lightbox-thumb-item {
    border: 2px solid transparent;
    opacity: 0.6;
    transition: all 0.2s ease;

    &:hover {
        opacity: 0.9;
    }

    &.lightbox-thumb-active {
        border-color: #60a5fa !important;
        opacity: 1 !important;
        transform: scale(1.05);
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

.product-card-placeholder {
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border-radius: 16px;
    padding: 8px;
    background: #ffffff;
    border: 1px solid #e2e8f0;

    &:hover {
        background: #f8fafc;
        box-shadow: 0 12px 24px -10px rgba(30, 37, 124, 0.15);
        transform: translateY(-4px);

        .product-name {
            color: #2563eb;
        }
    }
}

.image-box-placeholder {
    width: 100%;
    height: 180px;
    background: #f8fafc;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;

    @media (max-width: 600px) {
        height: 125px !important;
        border-radius: 10px;
    }
}

.product-info {
    padding: 6px 2px 0;

    .promo-label {
        color: #2962ff;
        font-size: 0.75rem;
        font-weight: 800;
        display: block;
        margin-bottom: 2px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }

    .product-name {
        font-size: 0.925rem;
        font-weight: 700;
        color: #0f172a;
        margin-bottom: 4px;
        transition: color 0.3s ease;

        @media (max-width: 600px) {
            font-size: 0.8rem;
        }
    }

    .product-price {
        font-size: 0.95rem;
        font-weight: 800;
        color: #e53935;

        @media (max-width: 600px) {
            font-size: 0.85rem;
        }
    }
}

/* Modern Product Detail Design Overrides */
.product-brand-tag {
    font-family: 'Outfit', sans-serif;
    font-size: 13px;
    font-weight: 700;
    color: #2962ff;
    letter-spacing: 1.5px;
    text-transform: uppercase;
}

.product-title-new {
    font-family: 'Outfit', sans-serif;
    font-size: clamp(1.35rem, 3vw, 2rem);
    font-weight: 800;
    line-height: 1.25;
    color: #0a1329;
}

.product-meta-row {
    font-size: 13px;
    color: #64748b;
    font-weight: 500;

    .rating-value-text {
        font-weight: 700;
        color: #0a1329;
    }

    .reviews-count-text,
    .sold-count-text {
        color: #64748b;
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
        font-size: clamp(1.4rem, 3vw, 1.875rem);
        font-weight: 800;
        color: #e53935;
        font-family: 'Outfit', sans-serif;
    }

    .old-price-label-new {
        font-size: 18px;
        font-weight: 500;
        color: #94a3b8;
        text-decoration: line-through;
    }

    .discount-badge-new {
        background-color: #fee2e2;
        color: #ef4444;
        font-size: 13px;
        font-weight: 700;
        padding: 4px 8px;
        border-radius: 6px;
    }
}

.selection-label-row {
    font-size: 14px;
    color: #0a1329;

    .label-title {
        font-weight: 600;
    }

    .label-selected-value {
        color: #64748b;
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
        border-color: #2962ff;
        box-shadow: 0 4px 12px rgba(41, 98, 255, 0.25);
    }
}

/* Size Box Selectors */
.size-guide-link {
    font-size: 13px;
    color: #2962ff;
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
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    color: #0a1329;
    font-size: 14px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
        border-color: #2962ff;
        color: #2962ff;
        background-color: #f0f4ff;
    }

    &.active {
        background-color: #2962ff !important;
        border-color: #2962ff !important;
        color: #ffffff !important;
        box-shadow: 0 4px 12px rgba(41, 98, 255, 0.25);
    }
}

/* Availability Status */
.availability-status-row {
    font-size: 14px;
    font-weight: 600;
    color: #10b981;
}

/* Action Section */
.quantity-cart-row {
    display: flex;
    align-items: center;
    width: 100%;
}

.quantity-selector-pill {
    height: 48px;
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
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
        color: #0a1329;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background-color 0.2s;

        &:hover:not(:disabled) {
            background-color: #e2e8f0;
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
        color: #0a1329;
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
    color: #ffffff !important;
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
    color: #ffffff !important;
    transition: transform 0.2s !important;

    &:hover {
        transform: translateY(-2px);
    }
}

.favorite-floating-btn {
    top: 16px;
    right: 16px;
    z-index: 10;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
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
    color: #0a1329;
}

.desc-text-new {
    font-size: 14px;
    line-height: 1.7;
    color: #475569;
    white-space: pre-line;
    word-break: break-word;
}
</style>
