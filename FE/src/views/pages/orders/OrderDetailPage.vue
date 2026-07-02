<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { ORDER_STATUS, ORDER_STATUS_LABELS, ORDER_STATUS_COLORS, ORDER_STATUS_ICONS } from '@/constants/hoaDonConstants';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const cancelLoading = ref(false);
const showCancelDialog = ref(false);

const showEditDialog = ref(false);
const editLoading = ref(false);
const editForm = ref({ tenNguoiNhan: '', soDienThoaiNguoiNhan: '', diaChiNguoiNhan: '', ghiChu: '' });

const editingItems = ref(false);
const itemsLoading = ref(false);
const editQuantities = ref({});

const statusColor = (status) => ORDER_STATUS_COLORS[status] || '#757575';
const statusIcon = (status) => ORDER_STATUS_ICONS[status] || 'mdi-help-circle-outline';
const statusLabel = (status) => ORDER_STATUS_LABELS[status] || status;

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const formatDate = (timestamp) => {
    if (!timestamp) return '';
    return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    }).format(new Date(timestamp));
};

const formatDateFull = (timestamp) => {
    if (!timestamp) return '';
    return new Intl.DateTimeFormat('vi-VN', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    }).format(new Date(timestamp));
};

const canCancel = computed(() => {
    return order.value?.trangThai === 'CHO_XAC_NHAN';
});

// Các bước tiến trình đơn hàng (luồng bình thường, bỏ qua Đã hủy / Hoàn đơn)
const timelineSteps = [
    ORDER_STATUS.CHO_XAC_NHAN,
    ORDER_STATUS.XAC_NHAN,
    ORDER_STATUS.CHO_GIAO,
    ORDER_STATUS.DANG_GIAO,
    ORDER_STATUS.HOAN_THANH
];

// Đơn ở trạng thái bất thường -> không hiển thị thanh ngang mà báo riêng
const isCancelled = computed(() =>
    order.value?.trangThai === ORDER_STATUS.DA_HUY || order.value?.trangThai === ORDER_STATUS.HOAN_DON
);

// Vị trí bước hiện tại trong luồng (-1 nếu không thuộc luồng bình thường)
const currentStepIndex = computed(() => timelineSteps.indexOf(order.value?.trangThai));

const isPriceChanged = (item) => item.giaHienTai != null && Number(item.giaHienTai) !== Number(item.donGia);

const repayLoading = ref(false);

const handleRepay = async () => {
    repayLoading.value = true;
    try {
        const returnUrl = `${window.location.origin}/my-orders/${route.params.id}`;
        const url = await dichVuDatHang.taoUrlThanhToanLai(route.params.id, returnUrl);
        if (url) {
            window.location.href = url;
        } else {
            alert('Không lấy được URL thanh toán. Vui lòng thử lại.');
        }
    } catch (error) {
        console.error('Error creating VNPay URL:', error);
        alert(error.response?.data?.message || 'Tạo thanh toán thất bại');
    } finally {
        repayLoading.value = false;
    }
};

const handleVnPayReturn = async () => {
    const params = route.query || {};
    if (!params.vnp_ResponseCode) return;
    try {
        // Gửi callback về backend để verify chữ ký và lưu trạng thái đã thanh toán
        const res = await dichVuDatHang.xacNhanThanhToanVnPay(params);
        if (res?.success) {
            alert('Thanh toán VNPay thành công! Đơn hàng của bạn đã được ghi nhận thanh toán.');
            await fetchOrder();
        } else {
            alert(res?.message || 'Thanh toán VNPay chưa hoàn tất. Bạn có thể thử thanh toán lại.');
        }
    } catch (error) {
        console.error('VNPay verify error:', error);
        alert('Không xác nhận được kết quả thanh toán. Vui lòng kiểm tra lại đơn hàng.');
    } finally {
        // Dọn query params khỏi URL
        router.replace({ path: `/my-orders/${route.params.id}` });
    }
};

const fetchOrder = async () => {
    loading.value = true;
    try {
        order.value = await dichVuDatHang.layChiTietDonHang(route.params.id);
    } catch (error) {
        console.error('Error fetching order:', error);
    } finally {
        loading.value = false;
    }
};

const openEditDialog = () => {
    editForm.value = {
        tenNguoiNhan: order.value.tenNguoiNhan || '',
        soDienThoaiNguoiNhan: order.value.soDienThoaiNguoiNhan || '',
        diaChiNguoiNhan: order.value.diaChiNguoiNhan || '',
        ghiChu: order.value.ghiChu || ''
    };
    showEditDialog.value = true;
};

const handleSaveShipping = async () => {
    if (!editForm.value.tenNguoiNhan?.trim() || !editForm.value.soDienThoaiNguoiNhan?.trim() || !editForm.value.diaChiNguoiNhan?.trim()) {
        alert('Vui lòng nhập đầy đủ tên, số điện thoại và địa chỉ nhận hàng');
        return;
    }
    editLoading.value = true;
    try {
        await dichVuDatHang.capNhatThongTinNhanHang(route.params.id, editForm.value);
        showEditDialog.value = false;
        await fetchOrder();
    } catch (error) {
        console.error('Error updating shipping info:', error);
        alert(error.response?.data?.message || 'Cập nhật thông tin thất bại');
    } finally {
        editLoading.value = false;
    }
};

const startEditItems = () => {
    const map = {};
    (order.value.items || []).forEach((it) => {
        map[it.idChiTietSanPham] = it.soLuong;
    });
    editQuantities.value = map;
    editingItems.value = true;
};

const cancelEditItems = () => {
    editingItems.value = false;
    editQuantities.value = {};
};

const changeQty = (idChiTietSanPham, delta) => {
    const current = editQuantities.value[idChiTietSanPham] || 1;
    const next = current + delta;
    if (next < 1) return;
    editQuantities.value[idChiTietSanPham] = next;
};

const handleSaveItems = async () => {
    const items = Object.entries(editQuantities.value).map(([idChiTietSanPham, soLuong]) => ({
        idChiTietSanPham,
        soLuong
    }));
    if (!items.length || items.some((i) => !i.soLuong || i.soLuong < 1)) {
        alert('Đơn hàng phải còn ít nhất 1 sản phẩm với số lượng tối thiểu là 1');
        return;
    }
    itemsLoading.value = true;
    try {
        await dichVuDatHang.capNhatSanPham(route.params.id, items);
        editingItems.value = false;
        await fetchOrder();
    } catch (error) {
        console.error('Error updating items:', error);
        alert(error.response?.data?.message || 'Cập nhật sản phẩm thất bại');
    } finally {
        itemsLoading.value = false;
    }
};

const handleCancel = async () => {
    cancelLoading.value = true;
    try {
        await dichVuDatHang.huyDonHang(route.params.id);
        showCancelDialog.value = false;
        await fetchOrder();
    } catch (error) {
        console.error('Error cancelling order:', error);
        alert(error.response?.data?.message || 'Hủy đơn hàng thất bại');
    } finally {
        cancelLoading.value = false;
    }
};

onMounted(async () => {
    await fetchOrder();
    await handleVnPayReturn();
});
</script>

<template>
    <div class="order-detail-page min-vh-100 pb-12 bg-white">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <v-container class="py-8" style="max-width: 1100px">
            <!-- Back -->
            <v-btn variant="text" class="font-weight-bold text-none mb-6 back-btn" 
                style="color: #1e257c;" @click="router.push('/my-orders')">
                <v-icon class="mr-2" size="18" style="color: #1e257c;">mdi-arrow-left</v-icon>
                Quay lại danh sách đơn hàng
            </v-btn>

            <div v-if="loading" class="text-center py-16">
                <v-progress-circular indeterminate color="#1e257c" size="48" width="4"></v-progress-circular>
                <p class="text-body-2 text-grey mt-4">Đang tải thông tin đơn hàng...</p>
            </div>

            <div v-else-if="order">
                <!-- Order Header -->
                <div class="order-header mb-8 pb-6" style="border-bottom: 2px solid #1e257c;">
                    <div class="d-flex align-center justify-space-between flex-wrap gap-4">
                        <div>
                            <p class="text-overline mb-1 font-weight-bold" style="color: #1e257c;">MÃ ĐƠN HÀNG</p>
                            <h1 class="text-h4 font-weight-bold mb-2" style="color: #1e257c;">{{ order.maHoaDon || 'Đang tạo...' }}</h1>
                            <div class="d-flex align-center flex-wrap gap-4">
                                <div class="d-flex align-center">
                                    <v-icon size="16" class="mr-2" style="color: #1e257c;">mdi-calendar-clock</v-icon>
                                    <span class="text-body-2">Đặt lúc {{ formatDateFull(order.ngayTao) }}</span>
                                </div>
                                <div v-if="order.ngayDuKienNhan" class="d-flex align-center">
                                    <v-icon size="16" class="mr-2" style="color: #1e257c;">mdi-truck-fast-outline</v-icon>
                                    <span class="text-body-2">Dự kiến nhận: {{ formatDate(order.ngayDuKienNhan) }}</span>
                                </div>
                            </div>
                        </div>
                        <div class="text-right">
                            <v-chip
                                style="background: #1e257c; color: white;"
                                variant="flat"
                                size="large"
                                class="font-weight-bold px-4"
                            >
                                <v-icon :icon="statusIcon(order.trangThai)" class="mr-2" size="22"></v-icon>
                                {{ statusLabel(order.trangThai) }}
                            </v-chip>
                        </div>
                    </div>
                </div>

                <!-- Horizontal Progress Stepper -->
                <div v-if="!isCancelled && currentStepIndex >= 0" class="progress-stepper section-block mb-8">
                    <div class="stepper-track">
                        <template v-for="(step, i) in timelineSteps" :key="step">
                            <div class="stepper-step" :class="{ 'is-done': i <= currentStepIndex, 'is-current': i === currentStepIndex }">
                                <div class="stepper-circle">
                                    <v-icon size="18" :color="i <= currentStepIndex ? 'white' : '#b0b5e0'">{{ statusIcon(step) }}</v-icon>
                                </div>
                                <span class="stepper-label">{{ statusLabel(step) }}</span>
                            </div>
                            <div v-if="i < timelineSteps.length - 1" class="stepper-line" :class="{ 'is-done': i < currentStepIndex }"></div>
                        </template>
                    </div>
                </div>
                <!-- Trạng thái bất thường -->
                <div v-else-if="isCancelled" class="cancelled-banner section-block mb-8">
                    <v-icon size="22" class="mr-3" color="#991b1b">{{ statusIcon(order.trangThai) }}</v-icon>
                    <span>Đơn hàng đã {{ statusLabel(order.trangThai).toLowerCase() }}</span>
                </div>

                <v-row class="content-grid">
                    <!-- Left Column -->
                    <v-col cols="12" lg="8" md="7" class="d-flex flex-column gap-6">
                        <!-- Products -->
                        <div class="section-block pa-6">
                            <div class="d-flex align-center justify-space-between mb-6">
                                <div class="d-flex align-center">
                                    <div class="card-icon mr-4" style="background: #f0f4ff;">
                                        <v-icon style="color: #1e257c;" size="22">mdi-package-variant</v-icon>
                                    </div>
                                    <div>
                                        <h3 class="text-subtitle-1 font-weight-bold mb-0" style="color: #1e257c;">Sản phẩm đã đặt</h3>
                                        <p class="text-caption mb-0">{{ order.items?.length || 0 }} sản phẩm</p>
                                    </div>
                                </div>
                                <div v-if="order.choPhepSuaSanPham">
                                    <v-btn v-if="!editingItems" variant="tonal" size="small"
                                        rounded="pill" class="text-none font-weight-bold" 
                                        style="color: #1e257c; background: #f0f4ff;" @click="startEditItems">
                                        <v-icon size="16" class="mr-1">mdi-pencil</v-icon>Sửa số lượng
                                    </v-btn>
                                    <template v-else>
                                        <v-btn variant="text" size="small" rounded="pill"
                                            class="text-none font-weight-bold mr-1" :disabled="itemsLoading"
                                            style="color: #1e257c;"
                                            @click="cancelEditItems">Hủy</v-btn>
                                        <v-btn variant="flat" size="small" rounded="pill"
                                            class="text-none font-weight-bold" :loading="itemsLoading"
                                            style="background: #1e257c; color: white;"
                                            @click="handleSaveItems">
                                            <v-icon size="16" class="mr-1">mdi-content-save</v-icon>Lưu
                                        </v-btn>
                                    </template>
                                </div>
                            </div>
                            <div v-for="(item, i) in order.items" :key="i"
                                class="detail-product d-flex align-center gap-4 py-4"
                                :class="{ 'border-top': i > 0 }">
                                <div class="detail-thumb-wrapper">
                                    <v-img :src="item.hinhAnh || 'https://via.placeholder.com/150?text=Sản+Phẩm'"
                                        cover width="76" height="76" class="rounded-xl"></v-img>
                                </div>
                                <div class="flex-grow-1 min-w-0">
                                    <p class="text-body-1 font-weight-bold mb-1 text-truncate">{{ item.tenSanPham }}</p>
                                    <div class="d-flex align-center flex-wrap gap-3">
                                        <v-chip size="x-small" variant="tonal" class="font-weight-medium"
                                            style="color: #1e257c; background: #f0f4ff;">
                                            <v-icon size="12" class="mr-1">mdi-palette</v-icon>{{ item.tenMauSac }}
                                        </v-chip>
                                        <v-chip size="x-small" variant="tonal" class="font-weight-medium"
                                            style="color: #1e257c; background: #f0f4ff;">
                                            <v-icon size="12" class="mr-1">mdi-ruler</v-icon>{{ item.tenKichThuoc }}
                                        </v-chip>
                                        <div v-if="editingItems" class="qty-stepper d-flex align-center">
                                            <v-btn icon size="x-small" variant="tonal"
                                                :disabled="(editQuantities[item.idChiTietSanPham] || 1) <= 1"
                                                style="color: #1e257c; background: #f0f4ff;"
                                                @click="changeQty(item.idChiTietSanPham, -1)">
                                                <v-icon size="16">mdi-minus</v-icon>
                                            </v-btn>
                                            <span class="qty-value font-weight-bold mx-2">{{ editQuantities[item.idChiTietSanPham] }}</span>
                                            <v-btn icon size="x-small" variant="tonal"
                                                style="color: #1e257c; background: #f0f4ff;"
                                                @click="changeQty(item.idChiTietSanPham, 1)">
                                                <v-icon size="16">mdi-plus</v-icon>
                                            </v-btn>
                                        </div>
                                        <span v-else class="text-caption text-grey">x{{ item.soLuong }}</span>
                                    </div>
                                    <p class="text-caption text-grey mt-1">{{ formatPrice(item.donGia) }} / sản phẩm</p>
                                    <div v-if="isPriceChanged(item)"
                                        class="price-change-note d-flex align-center gap-2 mt-1 pa-2 rounded-lg"
                                        style="background: #f5f7ff; border: 1px dashed #1e257c;">
                                        <v-icon size="14" style="color: #1e257c;">mdi-alert-outline</v-icon>
                                        <span class="text-caption" style="color: #1e257c;">
                                            Giá đã đổi: <s>{{ formatPrice(item.donGia) }}</s> → <b>{{ formatPrice(item.giaHienTai) }}</b>
                                        </span>
                                    </div>
                                </div>
                                <span class="text-body-1 font-weight-bold text-right flex-shrink-0">{{ formatPrice(item.thanhTien) }}</span>
                            </div>
                        </div>

                        <!-- Status Timeline -->
                        <div v-if="order.lichSuTrangThai?.length" class="section-block pa-6">
                            <div class="d-flex align-center mb-6">
                                <div class="card-icon mr-4" style="background: #f0f4ff;">
                                    <v-icon style="color: #1e257c;" size="22">mdi-timeline-clock-outline</v-icon>
                                </div>
                                <div>
                                    <h3 class="text-subtitle-1 font-weight-bold mb-0" style="color: #1e257c;">Lịch sử đơn hàng</h3>
                                    <p class="text-caption mb-0">Các cập nhật mới nhất về đơn hàng</p>
                                </div>
                            </div>
                            <v-timeline density="compact" side="end" class="timeline-modern">
                                <v-timeline-item
                                    v-for="(ls, i) in order.lichSuTrangThai"
                                    :key="i"
                                    size="small"
                                    class="timeline-item-custom"
                                >
                                    <template v-slot:icon>
                                        <div class="timeline-dot" style="background: #1e257c;">
                                            <v-icon size="14" color="white">{{ statusIcon(ls.trangThai) }}</v-icon>
                                        </div>
                                    </template>
                                    <div>
                                        <p class="text-body-2 font-weight-bold mb-0">{{ statusLabel(ls.trangThai) }}</p>
                                        <p class="text-caption text-grey mb-1 d-flex align-center">
                                            <v-icon size="12" class="mr-1">mdi-clock-outline</v-icon>
                                            {{ formatDateFull(ls.thoiGian) }}
                                        </p>
                                        <p v-if="ls.ghiChu" class="text-caption text-grey-darken-1 mb-0 pa-3 rounded-lg mt-1" style="background: #f5f7ff;">
                                            <v-icon size="12" class="mr-1">mdi-comment-text-outline</v-icon>
                                            {{ ls.ghiChu }}
                                        </p>
                                    </div>
                                </v-timeline-item>
                            </v-timeline>
                        </div>
                    </v-col>

                    <!-- Right Column -->
                    <v-col cols="12" lg="4" md="5" class="d-flex flex-column gap-6">
                        <!-- Shipping Info -->
                        <div class="section-block pa-6">
                            <div class="d-flex align-center justify-space-between mb-6">
                                <div class="d-flex align-center">
                                    <div class="card-icon mr-4" style="background: #f0f4ff;">
                                        <v-icon style="color: #1e257c;" size="22">mdi-truck-outline</v-icon>
                                    </div>
                                    <h3 class="text-subtitle-1 font-weight-bold mb-0" style="color: #1e257c;">Thông tin giao hàng</h3>
                                </div>
                                <v-btn v-if="order.choPhepSuaThongTin" variant="tonal" size="small"
                                    rounded="pill" class="text-none font-weight-bold" 
                                    style="color: #1e257c; background: #f0f4ff;" @click="openEditDialog">
                                    <v-icon size="16" class="mr-1">mdi-pencil</v-icon>Sửa
                                </v-btn>
                            </div>
                            <div class="shipping-details">
                                <div class="shipping-row">
                                    <div class="shipping-label text-caption text-grey mb-1">
                                        <v-icon size="14" class="mr-1">mdi-account-outline</v-icon>Người nhận
                                    </div>
                                    <p class="text-body-2 font-weight-bold mb-0">{{ order.tenNguoiNhan }}</p>
                                </div>
                                <div class="shipping-row">
                                    <div class="shipping-label text-caption text-grey mb-1">
                                        <v-icon size="14" class="mr-1">mdi-phone-outline</v-icon>Điện thoại
                                    </div>
                                    <p class="text-body-2 font-weight-bold mb-0">{{ order.soDienThoaiNguoiNhan }}</p>
                                </div>
                                <div class="shipping-row">
                                    <div class="shipping-label text-caption text-grey mb-1">
                                        <v-icon size="14" class="mr-1">mdi-map-marker-outline</v-icon>Địa chỉ
                                    </div>
                                    <p class="text-body-2 font-weight-medium mb-0">{{ order.diaChiNguoiNhan }}</p>
                                </div>
                                <div v-if="order.ngayDuKienNhan" class="shipping-row">
                                    <div class="shipping-label text-caption text-grey mb-1">
                                        <v-icon size="14" class="mr-1">mdi-calendar-check-outline</v-icon>Dự kiến nhận
                                    </div>
                                    <p class="text-body-2 font-weight-bold mb-0" style="color: #1e257c;">{{ formatDateFull(order.ngayDuKienNhan) }}</p>
                                </div>
                            </div>
                        </div>

                        <!-- Payment Summary -->
                        <div class="section-block pa-6">
                            <div class="d-flex align-center mb-6">
                                <div class="card-icon mr-4" style="background: #f0f4ff;">
                                    <v-icon style="color: #1e257c;" size="22">mdi-receipt-text-outline</v-icon>
                                </div>
                                <h3 class="text-subtitle-1 font-weight-bold mb-0" style="color: #1e257c;">Chi tiết thanh toán</h3>
                            </div>
                            <div class="payment-details pa-4" style="background: #f8f9fa;">
                                <div class="d-flex justify-space-between mb-3">
                                    <span class="text-body-2 text-grey-darken-1">Tạm tính</span>
                                    <span class="text-body-2 font-weight-medium">{{ formatPrice(order.tongTien) }}</span>
                                </div>
                                <div class="d-flex justify-space-between mb-3">
                                    <span class="text-body-2 text-grey-darken-1">Phí vận chuyển</span>
                                    <span class="text-body-2 font-weight-medium" :class="!order.phiVanChuyen || order.phiVanChuyen == 0 ? 'text-blue-darken-4' : ''">
                                        <v-icon v-if="!order.phiVanChuyen || order.phiVanChuyen == 0" size="14" color="#1e257c" class="mr-1">mdi-check-circle</v-icon>
                                        {{ !order.phiVanChuyen || order.phiVanChuyen == 0 ? 'Miễn phí' : formatPrice(order.phiVanChuyen) }}
                                    </span>
                                </div>
                                <div v-if="order.tienGiam > 0" class="d-flex justify-space-between mb-3">
                                    <span class="text-body-2" style="color: #1e257c;">
                                        <v-icon size="14" class="mr-1" style="color: #1e257c;">mdi-ticket-percent</v-icon>Giảm giá
                                    </span>
                                    <span class="text-body-2 font-weight-medium" style="color: #1e257c;">-{{ formatPrice(order.tienGiam) }}</span>
                                </div>
                                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 12px 0;" />
                                <div class="d-flex justify-space-between align-center mb-2">
                                    <span class="text-body-1 font-weight-bold">Tổng cộng</span>
                                    <span class="text-h6 font-weight-bold" style="color: #1e257c;">{{ formatPrice(order.tongTienSauGiam) }}</span>
                                </div>
                                <div class="d-flex align-center mt-3">
                                    <v-chip variant="tonal" size="small" class="font-weight-bold"
                                        style="color: #1e257c; background: #f0f4ff;">
                                        <v-icon size="14" class="mr-1">mdi-credit-card-outline</v-icon>
                                        {{ order.phuongThucThanhToan === 'COD' ? 'Thanh toán khi nhận hàng' : order.phuongThucThanhToan }}
                                    </v-chip>
                                    <v-chip v-if="order.maVoucher" variant="tonal" size="small" class="font-weight-bold ml-2"
                                        style="color: #1e257c; background: #f0f4ff;">
                                        <v-icon size="14" class="mr-1">mdi-ticket-percent</v-icon>
                                        {{ order.maVoucher }}
                                    </v-chip>
                                </div>
                            </div>
                        </div>

                        <!-- Repay VNPay Button -->
                        <div v-if="order.choPhepThanhToanLai" class="section-block pa-6 text-center">
                            <div class="repay-icon-wrapper mx-auto mb-4">
                                <v-icon size="32" style="color: #1e257c;">mdi-credit-card-sync-outline</v-icon>
                            </div>
                            <h4 class="text-body-1 font-weight-bold mb-2" style="color: #1e257c;">Hoàn tất thanh toán</h4>
                            <p class="text-caption mb-4">Đơn hàng chuyển khoản chưa được thanh toán. Bạn có thể thanh toán lại qua VNPay.</p>
                            <v-btn style="background: #1e257c; color: white;" variant="flat" rounded="pill"
                                class="font-weight-bold text-none" block :loading="repayLoading"
                                @click="handleRepay">
                                <v-icon class="mr-2" size="18">mdi-qrcode-scan</v-icon>
                                Thanh toán lại bằng VNPay
                            </v-btn>
                        </div>

                        <!-- Cancel Button -->
                        <div v-if="canCancel" class="section-block pa-6 text-center">
                            <div class="cancel-icon-wrapper mx-auto mb-4">
                                <v-icon size="32" style="color: #1e257c;">mdi-alert-circle-outline</v-icon>
                            </div>
                            <h4 class="text-body-1 font-weight-bold mb-2" style="color: #1e257c;">Cần hủy đơn hàng?</h4>
                            <p class="text-caption mb-4">Đơn hàng của bạn đang chờ xác nhận, bạn có thể hủy nếu cần.</p>
                            <v-btn style="color: #1e257c; border-color: #1e257c;" variant="outlined" rounded="pill"
                                class="font-weight-bold text-none" block
                                @click="showCancelDialog = true">
                                <v-icon class="mr-2" size="18">mdi-close-circle-outline</v-icon>
                                Hủy đơn hàng
                            </v-btn>
                        </div>
                    </v-col>
                </v-row>
            </div>
        </v-container>

        <!-- Edit Shipping Info Dialog -->
        <v-dialog v-model="showEditDialog" max-width="520">
            <div class="dialog-content pa-6 bg-white">
                <div class="d-flex align-center mb-5">
                    <div class="card-icon mr-3" style="background: #f0f4ff;">
                        <v-icon style="color: #1e257c;" size="22">mdi-truck-outline</v-icon>
                    </div>
                    <h3 class="text-h6 font-weight-bold mb-0" style="color: #1e257c;">Sửa thông tin nhận hàng</h3>
                </div>

                <v-text-field v-model="editForm.tenNguoiNhan" label="Tên người nhận"
                    variant="outlined" density="comfortable" class="mb-3" hide-details="auto"
                    prepend-inner-icon="mdi-account-outline" />
                <v-text-field v-model="editForm.soDienThoaiNguoiNhan" label="Số điện thoại người nhận"
                    variant="outlined" density="comfortable" class="mb-3" hide-details="auto"
                    prepend-inner-icon="mdi-phone-outline" />
                <v-textarea v-model="editForm.diaChiNguoiNhan" label="Địa chỉ nhận hàng" variant="outlined"
                    density="comfortable" rows="2" auto-grow class="mb-3" hide-details="auto"
                    prepend-inner-icon="mdi-map-marker-outline" />
                <v-textarea v-model="editForm.ghiChu" label="Ghi chú (tùy chọn)" variant="outlined"
                    density="comfortable" rows="2" auto-grow class="mb-2" hide-details="auto"
                    prepend-inner-icon="mdi-note-text-outline" />

                <v-alert v-if="!order?.laTienMat" type="info" variant="tonal" density="compact"
                    class="mb-4 text-caption" icon="mdi-information-outline">
                    Đổi địa chỉ không làm thay đổi phí vận chuyển đã chốt của đơn.
                </v-alert>

                <div class="d-flex gap-3 mt-2">
                    <v-btn variant="outlined" rounded="pill" class="font-weight-bold text-none flex-grow-1"
                        :disabled="editLoading" @click="showEditDialog = false">
                        Hủy
                    </v-btn>
                    <v-btn rounded="pill" class="font-weight-bold text-none flex-grow-1 text-white"
                        style="background: #1e257c;"
                        :loading="editLoading" @click="handleSaveShipping">
                        Lưu thay đổi
                    </v-btn>
                </div>
            </div>
        </v-dialog>

        <!-- Cancel Dialog -->
        <v-dialog v-model="showCancelDialog" max-width="440">
            <div class="dialog-content pa-6 bg-white">
                <div class="text-center mb-6">
                    <div class="cancel-dialog-icon mx-auto mb-4">
                        <v-icon size="40" style="color: #1e257c;">mdi-alert-circle-outline</v-icon>
                    </div>
                    <h3 class="text-h6 font-weight-bold mb-2">Xác nhận hủy đơn hàng</h3>
                    <p class="text-body-2 text-grey">
                        Bạn có chắc chắn muốn hủy đơn hàng
                        <strong class="text-black">{{ order?.maHoaDon }}</strong>?
                        <br>Thao tác này không thể hoàn tác.
                    </p>
                </div>
                <div class="d-flex gap-3">
                    <v-btn variant="outlined" rounded="pill" class="font-weight-bold text-none flex-grow-1"
                        @click="showCancelDialog = false">
                        Giữ lại đơn hàng
                    </v-btn>
                    <v-btn rounded="pill" class="font-weight-bold text-none flex-grow-1 text-white"
                        style="background: #1e257c;"
                        :loading="cancelLoading" @click="handleCancel">
                        Xác nhận hủy
                    </v-btn>
                </div>
            </div>
        </v-dialog>

        
        <CustomerChat />
    </div>
</template>

<style scoped>
.order-detail-page {
    padding-top: 64px;
}

.back-btn {
    transition: all 0.2s;
}
.back-btn:hover {
    background: #f0f0f0;
}

.section-block {
    border: 1px solid #e0e0e0;
    background: #fff;
}

.card-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.detail-product {
    transition: all 0.2s;
    border-radius: 12px;
    padding: 12px;
}
.detail-product:hover {
    background: #f8fbff;
}
.detail-product.border-top {
    border-top: 1px solid #f0f0f0;
}

.detail-thumb-wrapper {
    width: 76px;
    height: 76px;
    border-radius: 12px;
    overflow: hidden;
    flex-shrink: 0;
}

/* Horizontal progress stepper */
.progress-stepper {
    padding: 28px 24px 22px;
    overflow-x: auto;
}
.stepper-track {
    display: flex;
    align-items: flex-start;
    min-width: 520px;
}
.stepper-step {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 0 0 auto;
    width: 96px;
}
.stepper-circle {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border: 2px solid #d8daf0;
    transition: all 0.3s ease;
}
.stepper-step.is-done .stepper-circle {
    background: #1e257c;
    border-color: #1e257c;
}
.stepper-step.is-current .stepper-circle {
    box-shadow: 0 0 0 4px rgba(30, 37, 124, 0.15);
}
.stepper-label {
    margin-top: 10px;
    font-size: 0.8rem;
    font-weight: 600;
    color: #9aa0c0;
    text-align: center;
    line-height: 1.2;
}
.stepper-step.is-done .stepper-label {
    color: #1e257c;
}
.stepper-line {
    flex: 1 1 auto;
    height: 3px;
    background: #d8daf0;
    margin-top: 23px;
    border-radius: 2px;
    transition: all 0.3s ease;
}
.stepper-line.is-done {
    background: #1e257c;
}
.cancelled-banner {
    display: flex;
    align-items: center;
    padding: 18px 24px;
    font-weight: 700;
    color: #991b1b;
    background: #fef2f2;
    border-color: #fecaca !important;
}

.timeline-modern :deep(.v-timeline-item__body) {
    padding-top: 4px;
}

.timeline-dot {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.shipping-details .shipping-row {
    padding: 10px 12px;
    border-radius: 10px;
    transition: background 0.2s;
    margin-bottom: 4px;
}
.shipping-details .shipping-row:hover {
    background: #f8fbff;
}
.shipping-details .shipping-row .shipping-label {
    display: flex;
    align-items: center;
}

.cancel-icon-wrapper {
    width: 56px;
    height: 56px;
    background: #f5f7ff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.repay-icon-wrapper {
    width: 56px;
    height: 56px;
    background: #f5f7ff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.cancel-dialog-icon {
    width: 64px;
    height: 64px;
    background: #f5f7ff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.dialog-content {
    border: 1px solid #e0e0e0;
}

.content-grid {
    gap: 0;
}

.gap-6 {
    gap: 24px;
}

.qty-stepper {
    background: #f8fbff;
    border: 1px solid #e0e0e0;
    border-radius: 999px;
    padding: 2px 6px;
}

.qty-value {
    min-width: 24px;
    text-align: center;
    font-size: 0.9rem;
}
</style>
