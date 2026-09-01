<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';

import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { useToastStore } from '@/stores/toastStore';
import apiService from '@/services/apiService';
import { dichVuDatHang } from '@/services/public/dichVuDatHang';
import { dichVuKhachHang } from '@/services/public/dichVuKhachHang';
import { dichVuVnPay } from '@/views/modules/banhang/composables/dichVuVnPay';
import { PATH } from '@/router/routePaths';
import { useLocation } from '@/composables/useLocation';
import defaultShoeImg from '@/assets/images/products/cat_running.jpg';
import SafeProductImage from '@/views/modules/san-pham/components/SafeProductImage.vue';
import { findBestVoucherUpsell } from '@/views/modules/banhang/voucherUpsell';

const DEFAULT_SHOE_IMAGE = defaultShoeImg || new URL('/src/assets/images/products/cat_running.jpg', import.meta.url).href;

const router = useRouter();
const cartStore = useCartStore();
const authStore = useAuthStore();
const { provinces, districts, wards, loadingLocations, fetchProvinces, fetchDistricts, fetchWards, cleanName } = useLocation();

const loading = ref(false);
const voucherLoading = ref(false);
const toast = useToastStore();

const shippingInfo = ref({
    tenNguoiNhan: '',
    soDienThoai: '',
    email: '',
    tinhThanh: null,
    quanHuyen: null,
    phuongXa: null,
    diaChi: '',
    isInitializing: false
});

const paymentMethod = ref('COD');
const ghiChu = ref('');

const vnpayDialog = ref({
    show: false,
    loading: false,
    checking: false,
    orderId: '',
    orderCode: '',
    amount: 0,
    qrUrl: '',
    paymentUrl: '',
    pollInterval: null
});

const closeVnPayDialog = () => {
    if (vnpayDialog.value.pollInterval) {
        clearInterval(vnpayDialog.value.pollInterval);
        vnpayDialog.value.pollInterval = null;
    }
    vnpayDialog.value.show = false;
};

const handlePayLater = () => {
    const orderId = vnpayDialog.value.orderId;
    const orderCode = vnpayDialog.value.orderCode;
    const phone = vnpayDialog.value.phone || shippingInfo.value.soDienThoai;
    closeVnPayDialog();
    if (orderId) {
        cartStore.clearCart();
        const query = {};
        if (orderCode) query.code = orderCode;
        if (phone) query.phone = phone;
        router.push({ path: `/my-orders/${orderId}`, query });
    }
};

const checkOnlineOrderStatus = async (orderId) => {
    try {
        const orderCode = vnpayDialog.value.orderCode;
        const phone = vnpayDialog.value.phone || shippingInfo.value.soDienThoai;
        let order = null;
        if (orderCode && phone) {
            order = await dichVuDatHang.traCuuDonHang(orderCode, phone);
        } else {
            order = await dichVuDatHang.layChiTietDonHang(orderId);
        }

        if (order && (order.trangThai === 'XAC_NHAN' || order.trangThaiDisplay === 'Đã xác nhận')) {
            closeVnPayDialog();
            cartStore.clearCart();
            const query = {};
            if (orderCode) query.code = orderCode;
            if (phone) query.phone = phone;
            router.push({ path: `${PATH.ORDER_SUCCESS}/${orderId}`, query });
        }
    } catch (e) {
        console.error('Poll order status error:', e);
    }
};

const onConfirmPaidOnline = async () => {
    vnpayDialog.value.checking = true;
    try {
        await dichVuDatHang.xacNhanDonHangQr(vnpayDialog.value.orderId);
        closeVnPayDialog();
        cartStore.clearCart();
        router.push(`${PATH.ORDER_SUCCESS}/${vnpayDialog.value.orderId}`);
    } catch (error) {
        alert('Xác nhận thanh toán thất bại hoặc chưa nhận được tiền.');
    } finally {
        vnpayDialog.value.checking = false;
    }
};

const openVnPayGateway = () => {
    if (vnpayDialog.value.paymentUrl) {
        window.open(vnpayDialog.value.paymentUrl, 'vnpay_popup', 'width=850,height=650,top=100,left=200');
    }
};

const availableVouchers = ref([]);
const selectedVoucher = ref(null);
const showVoucherDialog = ref(false);

import { watch } from 'vue';

watch(
    () => shippingInfo.value.tinhThanh,
    (newVal) => {
        if (!shippingInfo.value.isInitializing) {
            shippingInfo.value.quanHuyen = null;
            shippingInfo.value.phuongXa = null;
        }
        if (newVal) fetchDistricts(newVal);
    }
);

watch(
    () => shippingInfo.value.quanHuyen,
    (newVal) => {
        if (!shippingInfo.value.isInitializing) {
            shippingInfo.value.phuongXa = null;
        }
        if (newVal) fetchWards(newVal);
    }
);

const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const FREE_SHIP_THRESHOLD = ref(500000);
const baseShippingFee = ref(35000);
const calculatedShippingFee = ref(35000);
const ghnShippingFee = ref(null);
const shippingFeeLoading = ref(false);

const fetchShippingConfig = async () => {
    try {
        const response = await apiService.get('/config/shipping');
        if (response.data?.success) {
            FREE_SHIP_THRESHOLD.value = response.data.data.freeShipThreshold;
            baseShippingFee.value = response.data.data.baseFee || 35000;
            calculatedShippingFee.value = response.data.data.baseFee || 35000;
        }
    } catch (e) {
        console.error('Lỗi khi lấy cấu hình phí vận chuyển', e);
        baseShippingFee.value = 35000;
        calculatedShippingFee.value = 35000;
    }
};

const calculateGhnShippingFee = async () => {
    if (!shippingInfo.value.quanHuyen || !shippingInfo.value.phuongXa) {
        ghnShippingFee.value = null;
        return;
    }

    const selectedDistrict = districts.value.find((d) => String(d.code) === String(shippingInfo.value.quanHuyen));
    const selectedWard = wards.value.find((w) => String(w.code) === String(shippingInfo.value.phuongXa));

    // Trường hợp địa chỉ là nguồn dự phòng (OpenAPI/Local) -> Không có mã GHN chuẩn -> Áp dụng phí tiêu chuẩn 35k
    if (selectedDistrict?.source === 'OPEN_API' || selectedWard?.source === 'OPEN_API') {
        ghnShippingFee.value = baseShippingFee.value || 35000;
        return;
    }

    shippingFeeLoading.value = true;
    const totalItems = cartStore.cartItems.reduce((acc, item) => acc + (item.soLuong || 1), 0);
    const weight = Math.max(200, 200 * totalItems);

    try {
        const res = await apiService.get('/admin/ghn/fee', {
            params: {
                toDistrictId: shippingInfo.value.quanHuyen,
                toWardCode: shippingInfo.value.phuongXa,
                weight
            },
            timeout: 5000
        });

        // Kiểm tra cờ lỗi hoặc GHN không khả dụng từ server
        if (res?.data?.unavailable || res?.data?.success === false) {
            ghnShippingFee.value = baseShippingFee.value || 35000;
            return;
        }

        const total = Number(res?.data?.data?.total || res?.data?.total || res?.data?.data || 0);
        if (total > 0) {
            ghnShippingFee.value = total;
            calculatedShippingFee.value = total;
        } else {
            // GHN trả về 0đ hoặc không tính được -> Fallback về phí mặc định 35k
            ghnShippingFee.value = baseShippingFee.value || 35000;
        }
    } catch (e) {
        // Bắt mọi lỗi mạng, timeout, HTTP 500/502 -> Fallback an toàn về 35k
        console.warn('GHN API lỗi hoặc mất kết nối, áp dụng phí mặc định 35k:', e);
        ghnShippingFee.value = baseShippingFee.value || 35000;
    } finally {
        shippingFeeLoading.value = false;
    }
};

watch([() => shippingInfo.value.quanHuyen, () => shippingInfo.value.phuongXa], async ([district, ward]) => {
    if (district && ward) {
        await calculateGhnShippingFee();
    } else {
        ghnShippingFee.value = null;
    }
});

const shippingFee = computed(() => {
    if (!shippingInfo.value.tinhThanh || !shippingInfo.value.quanHuyen || !shippingInfo.value.phuongXa) {
        return null;
    }
    return ghnShippingFee.value !== null ? ghnShippingFee.value : calculatedShippingFee.value;
});

// cartStore.cartTotal đã là tổng SAU đợt giảm giá (server trả về giaBan đã giảm).
// Tính tổng theo GIÁ GỐC và số tiền đợt giảm giá để hiển thị minh bạch trên phần tạm tính.
const originalSubtotal = computed(() =>
    cartStore.cartItems.reduce((sum, item) => sum + (item.giaGoc || item.giaBan || 0) * item.soLuong, 0)
);
const campaignDiscount = computed(() => Math.max(0, originalSubtotal.value - cartStore.cartTotal));

const campaignDiscountPercent = computed(() => {
    if (originalSubtotal.value === 0) return 0;
    return Math.round((campaignDiscount.value / originalSubtotal.value) * 100);
});

const calcVoucherDiscount = (v) => {
    if (!v) return 0;
    const isPercentage = v.loaiPhieu === 'PHAN_TRAM' || v.loaiPhieu === 'PERCENTAGE' || v.loaiPhieu === 1 || v.loaiPhieu === '1';
    if (isPercentage && (v.phanTramGiamGia || v.discountPercent || v.giamGia)) {
        const pct = Number(v.phanTramGiamGia || v.discountPercent || v.giamGia || 0);
        let discount = (cartStore.cartTotal * pct) / 100;
        const maxDisc = Number(v.giamToiDa || v.maxDiscount || 0);
        if (maxDisc > 0 && discount > maxDisc) discount = maxDisc;
        return Math.floor(discount);
    }
    return Number(v.soTienGiam || v.discountAmount || v.giamGia || 0);
};

const voucherDiscount = computed(() => calcVoucherDiscount(selectedVoucher.value));

// Voucher tốt nhất ĐÃ ĐỦ ĐIỀU KIỆN áp dụng ngay cho đơn hàng hiện tại
const bestVoucher = computed(() => {
    if (!availableVouchers.value?.length) return null;
    let best = null;
    let bestValue = 0;
    for (const v of availableVouchers.value) {
        const minOrder = Number(v.donHangToiThieu ?? v.minOrderValue ?? 0);
        if (minOrder > 0 && cartStore.cartTotal < minOrder) {
            continue;
        }
        const d = calcVoucherDiscount(v);
        if (d > bestValue) {
            bestValue = d;
            best = v;
        }
    }
    return best;
});

const bestVoucherDiscount = computed(() => calcVoucherDiscount(bestVoucher.value));

const isBestSelected = computed(
    () =>
        bestVoucher.value &&
        selectedVoucher.value &&
        (bestVoucher.value.id === selectedVoucher.value.id || (bestVoucher.value.ma && bestVoucher.value.ma === selectedVoucher.value.ma))
);

// Gợi ý mua thêm để áp dụng voucher giá trị cao hơn
const upsellSuggestion = computed(() => {
    return findBestVoucherUpsell(availableVouchers.value, cartStore.cartTotal, Date.now(), selectedVoucher.value);
});

const totalAmount = computed(() => {
    const fee = shippingFee.value !== null ? shippingFee.value : 0;
    const total = cartStore.cartTotal + fee - voucherDiscount.value;
    return total > 0 ? total : 0;
});

const isShippingValid = computed(() => {
    return (
        shippingInfo.value.tenNguoiNhan &&
        shippingInfo.value.tenNguoiNhan.trim() &&
        shippingInfo.value.soDienThoai &&
        shippingInfo.value.soDienThoai.trim() &&
        shippingInfo.value.email &&
        shippingInfo.value.email.trim() &&
        /.+@.+\..+/.test(shippingInfo.value.email.trim()) &&
        shippingInfo.value.tinhThanh &&
        shippingInfo.value.quanHuyen &&
        shippingInfo.value.phuongXa &&
        shippingInfo.value.diaChi &&
        shippingInfo.value.diaChi.trim()
    );
});

const estimatedDelivery = computed(() => {
    if (!shippingInfo.value.tinhThanh || !shippingInfo.value.quanHuyen || !shippingInfo.value.phuongXa) {
        return null;
    }
    const now = new Date();
    const est = new Date(now);
    est.setDate(est.getDate() + 3);
    return est.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
});

const fetchUserProfile = async () => {
    if (authStore.isLoggedIn) {
        try {
            const res = await dichVuKhachHang.layThongTinCaNhan();
            if (res.success && res.data) {
                const profile = res.data;
                shippingInfo.value.tenNguoiNhan = profile.ten || profile.tenTaiKhoan || '';
                shippingInfo.value.soDienThoai = profile.sdt || '';
                shippingInfo.value.email = profile.email || '';
                if (profile.diaChiChiTiet) shippingInfo.value.diaChi = profile.diaChiChiTiet;

                if (profile.tinhThanh) {
                    shippingInfo.value.isInitializing = true;
                    await fetchProvinces();
                    const p = provinces.value.find((x) => cleanName(x.name) === cleanName(profile.tinhThanh));
                    if (p) {
                        shippingInfo.value.tinhThanh = p.code;
                        await fetchDistricts(p.code);
                        const d = districts.value.find((x) => cleanName(x.name) === cleanName(profile.quanHuyen));
                        if (d) {
                            shippingInfo.value.quanHuyen = d.code;
                            await fetchWards(d.code);
                            const w = wards.value.find((x) => cleanName(x.name) === cleanName(profile.phuongXa));
                            if (w) {
                                shippingInfo.value.phuongXa = w.code;
                            }
                        }
                    }
                    setTimeout(() => {
                        shippingInfo.value.isInitializing = false;
                    }, 500);
                }
            }
        } catch (e) {
            console.error('Không thể lấy thông tin người dùng', e);
        }
    }
};

const voucherChangedInfo = ref(null);    // lưu thông tin voucher bị thay đổi điều kiện
const showVoucherChangedModal = ref(false);

// Các field so sánh điều kiện voucher
const VOUCHER_CONDITION_FIELDS = ['donHangToiThieu', 'soTienGiam', 'phanTramGiamGia', 'giamToiDa', 'loaiPhieu'];

/**
 * Sau khi fetch xong danh sách mới, kiểm tra xem selectedVoucher có còn hợp lệ không.
 * - Không còn trong danh sách → gỡ + toast
 * - Điều kiện thay đổi + không đủ điều kiện mới → gỡ + showVoucherChangedModal
 * - Đủ điều kiện → cập nhật selectedVoucher với data mới nhất từ server
 */
const revalidateSelectedVoucher = () => {
    if (!selectedVoucher.value) return;

    const freshData = availableVouchers.value.find((v) => v.id === selectedVoucher.value.id);

    // Voucher không còn trong danh sách
    if (!freshData) {
        const removed = selectedVoucher.value;
        selectedVoucher.value = null;
        toast.warning(`Phiếu giảm giá "${removed.ten || removed.ma}" không còn khả dụng và đã được gỡ bỏ.`);
        return;
    }

    // Phát hiện điều kiện thay đổi
    const conditionsChanged = VOUCHER_CONDITION_FIELDS.some(
        (f) => String(freshData[f] ?? '') !== String(selectedVoucher.value[f] ?? '')
    );

    const minOrder = freshData.donHangToiThieu || 0;
    const nowIneligible = minOrder > 0 && cartStore.cartTotal < minOrder;

    if (conditionsChanged && nowIneligible) {
        // Điều kiện bị thay đổi VÀ không còn đủ điều kiện
        const shortfall = minOrder - cartStore.cartTotal;
        voucherChangedInfo.value = {
            voucher: freshData,
            oldVoucher: { ...selectedVoucher.value },
            minOrder,
            currentTotal: cartStore.cartTotal,
            shortfall,
            conditionsChanged: true
        };
        selectedVoucher.value = null;
        showVoucherChangedModal.value = true;
    } else if (nowIneligible) {
        // Không thay đổi nhưng vẫn không đủ (ví dụ cart giảm)
        const shortfall = minOrder - cartStore.cartTotal;
        ineligibleVoucherInfo.value = {
            voucher: freshData,
            minOrder,
            currentTotal: cartStore.cartTotal,
            shortfall
        };
        selectedVoucher.value = null;
        showVoucherIneligibleModal.value = true;
    } else {
        // Hợp lệ: cập nhật data mới nhất
        selectedVoucher.value = freshData;
    }
};

const fetchVouchers = async () => {
    voucherLoading.value = true;
    try {
        const res = await dichVuDatHang.layVoucherKhaDung(cartStore.cartTotal);
        availableVouchers.value = (res || []).sort((a, b) => calcVoucherDiscount(b) - calcVoucherDiscount(a));
        // Sau khi fetch, kiểm tra lại voucher đang chọn
        revalidateSelectedVoucher();
    } catch (error) {
        console.error('Error fetching vouchers:', error);
    } finally {
        voucherLoading.value = false;
    }
};

const openVoucherModal = () => {
    showVoucherDialog.value = true;
    fetchVouchers();
};

const selectVoucher = (voucher) => {
    if (voucher.donHangToiThieu && cartStore.cartTotal < voucher.donHangToiThieu) {
        const diff = voucher.donHangToiThieu - cartStore.cartTotal;
        // Đóng dialog trước khi hiện modal điều kiện
        showVoucherDialog.value = false;
        ineligibleVoucherInfo.value = {
            voucher,
            minOrder: voucher.donHangToiThieu,
            currentTotal: cartStore.cartTotal,
            shortfall: diff
        };
        showVoucherIneligibleModal.value = true;
        return;
    }
    selectedVoucher.value = voucher;
    showVoucherDialog.value = false;
};

const removeVoucher = () => {
    selectedVoucher.value = null;
};


const showConfirmDialog = ref(false);

const fullAddressString = computed(() => {
    const p = provinces.value.find((x) => x.code === shippingInfo.value.tinhThanh);
    const d = districts.value.find((x) => x.code === shippingInfo.value.quanHuyen);
    const w = wards.value.find((x) => x.code === shippingInfo.value.phuongXa);
    const pName = p ? p.name : shippingInfo.value.tinhThanh || '';
    const dName = d ? d.name : shippingInfo.value.quanHuyen || '';
    const wName = w ? w.name : shippingInfo.value.phuongXa || '';
    return [shippingInfo.value.diaChi, wName, dName, pName].filter(Boolean).join(', ');
});

const openConfirmModal = () => {
    if (!isShippingValid.value) {
        alert('Vui lòng điền đầy đủ thông tin giao hàng trước khi tiếp tục.');
        return;
    }
    showConfirmDialog.value = true;
};

const confirmAndExecuteCheckout = async () => {
    showConfirmDialog.value = false;
    await handleCheckout();
};

const showVoucherIneligibleModal = ref(false);
const ineligibleVoucherInfo = ref(null);

const checkVoucherEligibilityBeforeCheckout = async () => {
    if (!selectedVoucher.value) return true;

    await fetchVouchers();
    const currentVoucherInList = availableVouchers.value.find((v) => v.id === selectedVoucher.value.id);
    if (!currentVoucherInList) {
        const removed = selectedVoucher.value;
        selectedVoucher.value = null;
        alert(`Phiếu giảm giá "${removed.ten || removed.ma}" không còn khả dụng hoặc đã hết lượt và đã được gỡ bỏ.`);
        return false;
    }

    const minOrder = currentVoucherInList.donHangToiThieu || 0;
    if (minOrder > 0 && cartStore.cartTotal < minOrder) {
        const shortfall = minOrder - cartStore.cartTotal;
        ineligibleVoucherInfo.value = {
            voucher: currentVoucherInList,
            minOrder,
            currentTotal: cartStore.cartTotal,
            shortfall
        };
        showVoucherIneligibleModal.value = true;
        return false;
    }

    selectedVoucher.value = currentVoucherInList;
    return true;
};

const handleRemoveIneligibleVoucher = () => {
    selectedVoucher.value = null;
    showVoucherIneligibleModal.value = false;
};

const handleContinueShopping = () => {
    showVoucherIneligibleModal.value = false;
    router.push(PATH.SHOES);
};

const handleCheckout = async () => {
    if (!isShippingValid.value) {
        return;
    }
    loading.value = true;
    try {
        await cartStore.syncWithBackend();
        if (cartStore.isEmpty) {
            alert('Giỏ hàng của bạn đã thay đổi hoặc hết hàng. Vui lòng kiểm tra lại.');
            router.push(PATH.SHOES);
            return;
        }
        
        const voucherOk = await checkVoucherEligibilityBeforeCheckout();
        if (!voucherOk) {
            loading.value = false;
            return;
        }

        const p = provinces.value.find((x) => x.code === shippingInfo.value.tinhThanh);
        const d = districts.value.find((x) => x.code === shippingInfo.value.quanHuyen);
        const w = wards.value.find((x) => x.code === shippingInfo.value.phuongXa);

        const checkoutData = {
            items: cartStore.cartItems.map((item) => ({
                idChiTietSanPham: item.idChiTietSanPham,
                soLuong: item.soLuong,
                giaDuKien: item.giaBan || 0
            })),
            tenNguoiNhan: shippingInfo.value.tenNguoiNhan,
            soDienThoai: shippingInfo.value.soDienThoai,
            email: shippingInfo.value.email,
            tinhThanh: p ? p.name : shippingInfo.value.tinhThanh,
            quanHuyen: d ? d.name : shippingInfo.value.quanHuyen || '',
            phuongXa: w ? w.name : shippingInfo.value.phuongXa || '',
            diaChi: shippingInfo.value.diaChi,
            phiVanChuyen: shippingFee.value !== null ? shippingFee.value : 0,
            idPhieuGiamGia: selectedVoucher.value?.id || null,
            phuongThucThanhToan: paymentMethod.value,
            ghiChu: ghiChu.value
        };

        const response = await dichVuDatHang.datHang(checkoutData);
        if (response.success) {
            const createdOrder = response.data;
            if (paymentMethod.value === 'VNPAY') {
                try {
                    const payload = {
                        amount: totalAmount.value,
                        orderId: createdOrder.id,
                        orderInfo: 'Thanh toan hoa don ' + (createdOrder.maHoaDon || createdOrder.id),
                        returnUrl: `${window.location.origin}${PATH.ORDER_SUCCESS}/${createdOrder.id}`
                    };
                    const vnpData = await dichVuVnPay.createPaymentUrl(payload);
                    if (vnpData && vnpData.paymentUrl) {
                        vnpayDialog.value = {
                            show: true,
                            loading: false,
                            checking: false,
                            orderId: createdOrder.id,
                            orderCode: createdOrder.maHoaDon || createdOrder.id,
                            amount: totalAmount.value,
                            qrUrl: `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(vnpData.paymentUrl)}`,
                            paymentUrl: vnpData.paymentUrl,
                            pollInterval: setInterval(() => checkOnlineOrderStatus(createdOrder.id), 3000)
                        };
                        loading.value = false;
                        return;
                    }
                } catch (e) {
                    console.error('Lỗi tạo mã QR VNPay:', e);
                }
            }
            cartStore.clearCart();
            router.push(`${PATH.ORDER_SUCCESS}/${createdOrder.id}`);
        }
    } catch (error) {
        console.error('Checkout error:', error);
        alert(error.response?.data?.message || 'Đặt hàng thất bại. Vui lòng thử lại.');
        await cartStore.syncWithBackend();
    } finally {
        loading.value = false;
    }
};

const userAddresses = ref([]);
const showAddressSelectorModal = ref(false);
const showNewAddressModal = ref(false);
const newAddressLoading = ref(false);

const newAddressForm = ref({
    tenNguoiNhan: '',
    sdtNguoiNhan: '',
    tinhThanh: null,
    quanHuyen: null,
    phuongXa: null,
    diaChiChiTiet: '',
    laMacDinh: false
});

const fetchUserAddresses = async () => {
    if (authStore.isLoggedIn) {
        try {
            const res = await dichVuKhachHang.layDanhSachDiaChi();
            const list = res.data || res;
            if (Array.isArray(list)) {
                userAddresses.value = list;
            }
        } catch (e) {
            console.error('Không thể lấy sổ địa chỉ:', e);
        }
    }
};

const formatFullAddressString = (addr) => {
    if (!addr) return '';
    const parts = [addr.diaChiChiTiet, addr.phuongXa, addr.thanhPho, addr.tinh].filter(Boolean);
    return parts.join(', ');
};

const applySavedAddress = async (addr) => {
    shippingInfo.value.isInitializing = true;
    shippingInfo.value.tenNguoiNhan = addr.tenNguoiNhan || shippingInfo.value.tenNguoiNhan;
    shippingInfo.value.soDienThoai = addr.sdtNguoiNhan || shippingInfo.value.soDienThoai;
    shippingInfo.value.diaChi = addr.diaChiChiTiet || '';

    if (addr.tinh) {
        await fetchProvinces();
        const p = provinces.value.find((x) => cleanName(x.name) === cleanName(addr.tinh) || String(x.code) === String(addr.tinh));
        if (p) {
            shippingInfo.value.tinhThanh = p.code;
            await fetchDistricts(p.code);
            const d = districts.value.find(
                (x) => cleanName(x.name) === cleanName(addr.thanhPho) || String(x.code) === String(addr.thanhPho)
            );
            if (d) {
                shippingInfo.value.quanHuyen = d.code;
                await fetchWards(d.code);
                const w = wards.value.find(
                    (x) => cleanName(x.name) === cleanName(addr.phuongXa) || String(x.code) === String(addr.phuongXa)
                );
                if (w) {
                    shippingInfo.value.phuongXa = w.code;
                }
            }
        }
    }
    setTimeout(() => {
        shippingInfo.value.isInitializing = false;
    }, 400);
    showAddressSelectorModal.value = false;
};

const openAddAddressModal = () => {
    newAddressForm.value = {
        tenNguoiNhan: shippingInfo.value.tenNguoiNhan || '',
        sdtNguoiNhan: shippingInfo.value.soDienThoai || '',
        tinhThanh: null,
        quanHuyen: null,
        phuongXa: null,
        diaChiChiTiet: '',
        laMacDinh: false
    };
    showNewAddressModal.value = true;
};

const handleSaveNewAddress = async () => {
    if (
        !newAddressForm.value.tenNguoiNhan ||
        !newAddressForm.value.sdtNguoiNhan ||
        !newAddressForm.value.tinhThanh ||
        !newAddressForm.value.quanHuyen ||
        !newAddressForm.value.phuongXa ||
        !newAddressForm.value.diaChiChiTiet
    ) {
        alert('Vui lòng điền đầy đủ thông tin địa chỉ mới');
        return;
    }

    const p = provinces.value.find((x) => String(x.code) === String(newAddressForm.value.tinhThanh));
    const d = districts.value.find((x) => String(x.code) === String(newAddressForm.value.quanHuyen));
    const w = wards.value.find((x) => String(x.code) === String(newAddressForm.value.phuongXa));

    newAddressLoading.value = true;
    try {
        const payload = {
            tenNguoiNhan: newAddressForm.value.tenNguoiNhan,
            sdtNguoiNhan: newAddressForm.value.sdtNguoiNhan,
            tinh: p ? p.name : '',
            thanhPho: d ? d.name : '',
            phuongXa: w ? w.name : '',
            diaChiChiTiet: newAddressForm.value.diaChiChiTiet,
            laMacDinh: newAddressForm.value.laMacDinh
        };

        const res = await dichVuKhachHang.themDiaChiMoi(payload);
        const newAddr = res.data || res;
        if (newAddr) {
            await fetchUserAddresses();
            await applySavedAddress(newAddr);
            showNewAddressModal.value = false;
        }
    } catch (e) {
        console.error('Lỗi khi thêm địa chỉ:', e);
        alert(e.response?.data?.message || 'Không thể thêm địa chỉ mới');
    } finally {
        newAddressLoading.value = false;
    }
};

onMounted(async () => {
    if (cartStore.isEmpty) {
        router.push(PATH.SHOES);
        return;
    }
    await cartStore.syncWithBackend();
    fetchProvinces();
    fetchShippingConfig();
    await fetchUserProfile();
    await fetchUserAddresses();
    fetchVouchers();
});

onUnmounted(() => {
    closeVnPayDialog();
});
</script>

<template>
    <div class="checkout-page bg-white min-vh-100 pb-16">
        <MainHeader />
        <div class="header-spacing"></div>

        <v-container style="max-width: 1480px" class="pt-2">
            <!-- Breadcrumb -->
            <div class="d-flex align-center mb-6 pt-4">
                <router-link to="/" class="breadcrumb-link">Trang chủ</router-link>
                <v-icon size="14" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <router-link :to="PATH.SHOES" class="breadcrumb-link">Sản phẩm</router-link>
                <v-icon size="14" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <span class="text-body-1 font-weight-bold text-black">Thanh toán</span>
            </div>

            <!-- Page Title -->
            <div class="d-flex align-center mb-8">
                <div class="section-icon mr-3">
                    <v-icon size="18" style="color: #ffffff !important">mdi-credit-card-check-outline</v-icon>
                </div>
                <div>
                    <h1 class="text-h4 font-weight-black mb-1">Thanh toán đơn hàng</h1>
                    <p class="text-body-2 text-grey-darken-1 mb-0">Kiểm tra và xác nhận thông tin đặt hàng</p>
                </div>
            </div>

            <v-row>
                <!-- Left Column -->
                <v-col cols="12" md="7">
                    <!-- Shipping Info -->
                    <div class="section-block mb-6">
                        <div class="pa-4 pa-sm-6 pa-md-8">
                            <div class="d-flex align-center justify-space-between flex-wrap ga-3 mb-6">
                                <div class="d-flex align-center">
                                    <div class="step-number mr-4 font-weight-bold">
                                        <v-icon color="white">mdi-map-marker-outline</v-icon>
                                    </div>
                                    <div>
                                        <h2 class="text-h5 font-weight-bold mb-1">Thông tin giao hàng</h2>
                                        <p class="text-caption text-grey mb-0">Nhập địa chỉ nơi bạn muốn nhận hàng</p>
                                    </div>
                                </div>
                                <div v-if="authStore.isLoggedIn" class="d-flex flex-wrap ga-2">
                                    <v-btn
                                        v-if="userAddresses.length > 0"
                                        variant="tonal"
                                        size="small"
                                        color="#1e257c"
                                        class="rounded-pill text-none font-weight-bold"
                                        @click="showAddressSelectorModal = true"
                                    >
                                        <v-icon class="mr-1" size="16">mdi-book-account-outline</v-icon>
                                        Chọn từ sổ địa chỉ ({{ userAddresses.length }})
                                    </v-btn>
                                    <v-btn
                                        variant="outlined"
                                        size="small"
                                        color="#1e257c"
                                        class="rounded-pill text-none font-weight-bold"
                                        @click="openAddAddressModal"
                                    >
                                        <v-icon class="mr-1" size="16">mdi-plus-circle-outline</v-icon>
                                        + Thêm địa chỉ mới
                                    </v-btn>
                                </div>
                            </div>

                            <v-row>
                                <v-col cols="12" sm="6">
                                    <v-text-field
                                        v-model="shippingInfo.tenNguoiNhan"
                                        label="Tên người nhận *"
                                        variant="outlined"
                                        hide-details="auto"
                                        prepend-inner-icon="mdi-account-outline"
                                        maxlength="100"
                                        counter="100"
                                        :rules="[(v) => !!v?.trim() || 'Vui lòng nhập tên người nhận', (v) => (v && v.trim().length >= 2) || 'Tên tối thiểu 2 ký tự']"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6">
                                    <v-text-field
                                        v-model="shippingInfo.soDienThoai"
                                        label="Số điện thoại *"
                                        variant="outlined"
                                        hide-details="auto"
                                        prepend-inner-icon="mdi-phone-outline"
                                        maxlength="10"
                                        counter="10"
                                        :rules="[(v) => !!v?.trim() || 'Vui lòng nhập SĐT', (v) => /^0[3|5|7|8|9][0-9]{8}$/.test(v?.trim() || '') || 'SĐT 10 số không hợp lệ']"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12">
                                    <v-text-field
                                        v-model="shippingInfo.email"
                                        label="Email nhận thông báo đơn hàng *"
                                        variant="outlined"
                                        hide-details="auto"
                                        prepend-inner-icon="mdi-email-outline"
                                        maxlength="100"
                                        :rules="[(v) => !!v?.trim() || 'Vui lòng nhập Email', (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v?.trim() || '') || 'Email không hợp lệ']"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-autocomplete
                                        v-model="shippingInfo.tinhThanh"
                                        :items="provinces"
                                        item-title="name"
                                        item-value="code"
                                        label="Tỉnh/Thành phố *"
                                        variant="outlined"
                                        hide-details="auto"
                                        prepend-inner-icon="mdi-map-marker-outline"
                                        :loading="loadingLocations.provinces"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Tỉnh/Thành']"
                                    ></v-autocomplete>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-autocomplete
                                        v-model="shippingInfo.quanHuyen"
                                        :items="districts"
                                        item-title="name"
                                        item-value="code"
                                        label="Quận/Huyện *"
                                        variant="outlined"
                                        hide-details="auto"
                                        :disabled="!shippingInfo.tinhThanh"
                                        :loading="loadingLocations.districts"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Quận/Huyện']"
                                    ></v-autocomplete>
                                </v-col>
                                <v-col cols="12" sm="4">
                                    <v-autocomplete
                                        v-model="shippingInfo.phuongXa"
                                        :items="wards"
                                        item-title="name"
                                        item-value="code"
                                        label="Phường/Xã *"
                                        variant="outlined"
                                        hide-details="auto"
                                        :disabled="!shippingInfo.quanHuyen"
                                        :loading="loadingLocations.wards"
                                        :rules="[(v) => !!v || 'Vui lòng chọn Phường/Xã']"
                                    ></v-autocomplete>
                                </v-col>
                                <v-col cols="12">
                                    <v-text-field
                                        v-model="shippingInfo.diaChi"
                                        label="Địa chỉ chi tiết (số nhà, tên đường) *"
                                        variant="outlined"
                                        hide-details="auto"
                                        prepend-inner-icon="mdi-home-outline"
                                        maxlength="255"
                                        counter="255"
                                        :rules="[(v) => !!v?.trim() || 'Vui lòng nhập địa chỉ', (v) => (v && v.trim().length >= 5) || 'Địa chỉ tối thiểu 5 ký tự']"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12">
                                    <v-textarea
                                        v-model="ghiChu"
                                        label="Ghi chú cho đơn hàng (không bắt buộc)"
                                        variant="outlined"
                                        density="comfortable"
                                        rows="2"
                                        hide-details="auto"
                                        maxlength="500"
                                        counter="500"
                                        prepend-inner-icon="mdi-note-text-outline"
                                    ></v-textarea>
                                </v-col>
                            </v-row>
                        </div>
                    </div>

                    <!-- Payment Method -->
                    <div class="section-block mb-6">
                        <div class="pa-4 pa-sm-6 pa-md-8">
                            <div class="d-flex align-center mb-6">
                                <div class="step-number mr-4 font-weight-bold">
                                    <v-icon color="white">mdi-credit-card-outline</v-icon>
                                </div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Phương thức thanh toán</h2>
                                    <p class="text-caption text-grey mb-0">Chọn cách bạn muốn thanh toán cho đơn hàng</p>
                                </div>
                            </div>

                            <div class="payment-options">
                                <div
                                    class="payment-option d-flex align-center pa-5 mb-4"
                                    :class="{ selected: paymentMethod === 'COD' }"
                                    @click="paymentMethod = 'COD'"
                                >
                                    <div class="radio-indicator mr-4">
                                        <div v-if="paymentMethod === 'COD'" class="radio-inner"></div>
                                    </div>
                                    <div class="flex-grow-1">
                                        <h4 class="font-weight-bold text-body-1 mb-1">Thanh toán khi nhận hàng (COD)</h4>
                                        <p class="text-caption text-grey mb-0">Thanh toán bằng tiền mặt khi nhận hàng</p>
                                    </div>
                                    <v-chip
                                        v-if="paymentMethod === 'COD'"
                                        size="x-small"
                                        color="#1e257c"
                                        variant="flat"
                                        class="font-weight-bold text-white"
                                        >ĐÃ CHỌN</v-chip
                                    >
                                </div>

                                <div
                                    class="payment-option d-flex align-center pa-5"
                                    :class="{ selected: paymentMethod === 'VNPAY' }"
                                    @click="paymentMethod = 'VNPAY'"
                                >
                                    <div class="radio-indicator mr-4">
                                        <div v-if="paymentMethod === 'VNPAY'" class="radio-inner"></div>
                                    </div>
                                    <div class="flex-grow-1">
                                        <h4 class="font-weight-bold text-body-1 mb-1">Thanh toán trực tuyến VNPay</h4>
                                        <p class="text-caption text-grey mb-0">Hỗ trợ Thẻ ATM, Visa, MasterCard, QR Code</p>
                                    </div>
                                    <v-chip
                                        v-if="paymentMethod === 'VNPAY'"
                                        size="x-small"
                                        color="#1e257c"
                                        variant="flat"
                                        class="font-weight-bold text-white"
                                        >ĐÃ CHỌN</v-chip
                                    >
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Estimated Delivery -->
                    <div class="section-block">
                        <div class="pa-4 pa-sm-6 pa-md-8">
                            <div class="d-flex align-center mb-4">
                                <div class="step-number mr-4 font-weight-bold">
                                    <v-icon color="white">mdi-truck-fast-outline</v-icon>
                                </div>
                                <div>
                                    <h2 class="text-h5 font-weight-bold mb-1">Thông tin giao hàng dự kiến</h2>
                                </div>
                            </div>
                            <div class="delivery-estimate pa-5">
                                <div v-if="estimatedDelivery" class="d-flex align-center">
                                    <div class="delivery-icon mr-4">
                                        <v-icon size="32" color="#1e257c">mdi-truck-fast-outline</v-icon>
                                    </div>
                                    <div>
                                        <p class="text-body-2 font-weight-bold mb-1">Dự kiến giao hàng vào</p>
                                        <p class="text-h6 font-weight-bold mb-1" style="color: #1e257c">{{ estimatedDelivery }}</p>
                                        <p class="text-caption text-grey-darken-1 mb-0 d-flex align-center">
                                            <v-icon size="14" class="mr-1">mdi-clock-outline</v-icon>
                                            Thời gian giao hàng từ 8:00 - 20:00 tất cả các ngày trong tuần
                                        </p>
                                    </div>
                                </div>
                                <div v-else class="d-flex align-center">
                                    <div class="delivery-icon mr-4">
                                        <v-icon size="32" color="grey">mdi-map-marker-question-outline</v-icon>
                                    </div>
                                    <div>
                                        <p class="text-body-2 font-weight-bold text-grey-darken-2 mb-1">Chưa có thông tin giao hàng</p>
                                        <p class="text-caption text-grey-darken-1 mb-0">
                                            Vui lòng chọn Tỉnh/Thành phố, Quận/Huyện, Phường/Xã để tính thời gian và phí giao hàng.
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </v-col>

                <!-- Right Column: Order Summary -->
                <v-col cols="12" md="5">
                    <div class="order-summary-sticky">
                        <div class="section-block">
                            <div class="pa-4 pa-sm-6">
                                <div class="d-flex align-center mb-6">
                                    <div class="summary-icon mr-3">
                                        <v-icon size="20" style="color: #ffffff !important">mdi-receipt-text-outline</v-icon>
                                    </div>
                                    <h3 class="text-h6 font-weight-bold mb-0">Tóm tắt đơn hàng</h3>
                                </div>

                                <!-- Product List -->
                                <div class="product-list mb-6">
                                    <div
                                        v-for="item in cartStore.cartItems"
                                        :key="item.idChiTietSanPham"
                                        class="product-item d-flex ga-4 py-4"
                                    >
                                        <div class="product-img-wrapper position-relative flex-shrink-0 rounded-lg overflow-hidden" style="width: 72px; height: 72px">
                                            <SafeProductImage
                                                :src="item.hinhAnh"
                                                :fallbackSrc="DEFAULT_SHOE_IMAGE"
                                                :alt="item.tenSanPham"
                                                :iconSize="28"
                                            />
                                            <span class="product-qty-badge">{{ item.soLuong }}</span>
                                            <div v-if="item.phanTramGiam > 0" class="cart-discount-badge">-{{ item.phanTramGiam }}%</div>
                                        </div>
                                        <div class="flex-grow-1 d-flex flex-column">
                                            <p class="text-body-2 font-weight-bold mb-1">{{ item.tenSanPham }}</p>
                                            <p class="text-caption text-grey-darken-1 mb-1">
                                                {{ item.tenMauSac }} / {{ item.tenKichThuoc }}
                                            </p>
                                            <div class="mt-auto d-flex flex-column ga-1">
                                                <div v-if="item.phanTramGiam > 0" class="text-caption text-error font-weight-medium mb-1">
                                                    Đợt giảm giá: -{{ item.phanTramGiam }}%
                                                </div>
                                                <div class="d-flex justify-space-between align-center">
                                                    <span class="text-caption text-grey">Đơn giá: {{ formatPrice(item.giaBan) }}</span>
                                                    <span class="text-body-2 font-weight-bold">{{
                                                        formatPrice(item.giaBan * item.soLuong)
                                                    }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <hr class="my-4" style="border: none; border-top: 1px solid #e0e0e0" />

                                <!-- Voucher -->
                                <div class="voucher-section mb-4">
                                    <div v-if="selectedVoucher" class="voucher-applied d-flex align-center justify-space-between pa-4 mb-3">
                                        <div class="d-flex align-center ga-3">
                                            <div class="voucher-icon-small">
                                                <v-icon size="20" color="white">mdi-ticket-percent</v-icon>
                                            </div>
                                            <div>
                                                <span class="text-body-2 font-weight-bold d-block" style="color: #1e257c">{{
                                                    selectedVoucher.ten ||
                                                    selectedVoucher.ma ||
                                                    selectedVoucher.maPhieu ||
                                                    selectedVoucher.maPhieuGiamGia ||
                                                    'Mã Voucher'
                                                }}</span>
                                                <div class="text-caption" style="color: #1e257c">
                                                    Giảm {{ formatPrice(voucherDiscount) }}
                                                    <span v-if="selectedVoucher.donHangToiThieu" class="text-grey-darken-1">
                                                        • Đơn tối thiểu: {{ formatPrice(selectedVoucher.donHangToiThieu) }}
                                                    </span>
                                                    <span v-else class="text-grey-darken-1"> • Mọi đơn hàng</span>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="d-flex align-center ga-1">
                                            <v-btn
                                                variant="text"
                                                size="small"
                                                class="text-none font-weight-bold"
                                                style="color: #1e257c"
                                                @click="openVoucherModal"
                                            >
                                                Đổi mã
                                            </v-btn>
                                            <v-btn icon variant="text" size="small" color="grey" @click="removeVoucher">
                                                <v-icon size="18">mdi-close-circle</v-icon>
                                            </v-btn>
                                        </div>
                                    </div>
                                    <v-btn
                                        v-else
                                        variant="outlined"
                                        block
                                        class="voucher-btn text-none"
                                        color="#1e257c"
                                        @click="openVoucherModal"
                                    >
                                        <span class="font-weight-bold d-flex align-center flex-grow-1">
                                            <v-icon class="mr-2" size="20">mdi-ticket-percent-outline</v-icon>
                                            Chọn hoặc nhập mã giảm giá
                                        </span>
                                        <v-icon size="18">mdi-chevron-right</v-icon>
                                    </v-btn>

                                    <!-- Gợi ý voucher tốt nhất đã đủ điều kiện áp dụng ngay -->
                                    <div
                                        v-if="bestVoucher && !isBestSelected && bestVoucherDiscount > 0"
                                        class="best-voucher-suggest d-flex align-center justify-space-between pa-3 mt-3 rounded-lg"
                                        style="border: 1px dashed #1e257c; background: #f5f7ff"
                                    >
                                        <div class="d-flex align-center ga-2 flex-grow-1 pr-2">
                                            <v-icon size="20" style="color: #1e257c" class="flex-shrink-0">mdi-lightbulb-on-outline</v-icon>
                                            <div class="flex-grow-1">
                                                <div class="d-flex align-center flex-wrap ga-1">
                                                    <span class="text-caption font-weight-bold" style="color: #1e257c">
                                                        Gợi ý tốt nhất: {{ bestVoucher.ten || bestVoucher.ma }}
                                                    </span>
                                                    <v-chip
                                                        v-if="bestVoucher.donHangToiThieu"
                                                        size="x-small"
                                                        variant="tonal"
                                                        color="primary"
                                                        class="font-weight-medium px-1"
                                                        style="font-size: 10.5px; height: 18px"
                                                    >
                                                        Đơn từ {{ formatPrice(bestVoucher.donHangToiThieu) }}
                                                    </v-chip>
                                                </div>
                                                <div class="text-caption text-grey-darken-2 mt-0.5">
                                                    <span class="font-weight-bold text-success">Tiết kiệm {{ formatPrice(bestVoucherDiscount) }}</span>
                                                    <span v-if="bestVoucher.donHangToiThieu" class="text-grey-darken-1">
                                                        • Đơn tối thiểu: {{ formatPrice(bestVoucher.donHangToiThieu) }}
                                                    </span>
                                                    <span v-else class="text-grey-darken-1"> • Không giới hạn đơn tối thiểu</span>
                                                </div>
                                            </div>
                                        </div>
                                        <v-btn
                                            size="small"
                                            variant="flat"
                                            style="background: #1e257c; color: white"
                                            rounded="pill"
                                            class="text-none font-weight-bold flex-shrink-0"
                                            @click="selectVoucher(bestVoucher)"
                                        >
                                            Áp dụng
                                        </v-btn>
                                    </div>

                                    <!-- Gợi ý mua thêm để áp dụng voucher giá trị cao hơn -->
                                    <div
                                        v-if="upsellSuggestion"
                                        class="upsell-voucher-suggest d-flex align-start ga-2 pa-3 mt-3 rounded-lg"
                                        style="border: 1px dashed #f59e0b; background: #fffbeb"
                                    >
                                        <v-icon size="18" style="color: #d97706" class="mt-0.5 flex-shrink-0">mdi-sparkles</v-icon>
                                        <div class="text-caption text-slate-800" style="line-height: 1.45">
                                            <span>Mua thêm </span>
                                            <strong style="color: #d97706">{{ formatPrice(upsellSuggestion.remainingAmount) }}</strong>
                                            <span> để áp dụng mã </span>
                                            <strong style="color: #1e257c">{{ upsellSuggestion.voucher?.ten || upsellSuggestion.voucher?.ma }}</strong>
                                            <span> (giảm <strong>{{ formatPrice(upsellSuggestion.discountAmount) }}</strong> cho đơn từ <strong>{{ formatPrice(upsellSuggestion.minimumOrderValue) }}</strong>)</span>
                                            <span v-if="selectedVoucher && upsellSuggestion.extraDiscountAmount > 0" class="font-weight-bold text-success">
                                                — Lợi hơn {{ formatPrice(upsellSuggestion.extraDiscountAmount) }}
                                            </span>
                                        </div>
                                    </div>
                                </div>

                                <!-- Price Breakdown -->
                                <div class="price-breakdown pa-4 mb-4">
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1">Tạm tính ({{ cartStore.cartCount }} sản phẩm)</span>
                                        <span class="text-body-2 font-weight-bold">{{ formatPrice(originalSubtotal) }}</span>
                                    </div>
                                    <div v-if="campaignDiscount > 0" class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-error">Đợt giảm giá</span>
                                        <span class="text-body-2 font-weight-bold text-error">-{{ campaignDiscountPercent }}%</span>
                                    </div>
                                    <div class="d-flex justify-space-between mb-3">
                                        <span class="text-body-2 text-grey-darken-1 d-flex align-center">
                                            Phí vận chuyển
                                            <svg
                                                width="45"
                                                height="15"
                                                viewBox="0 0 45 15"
                                                fill="none"
                                                xmlns="http://www.w3.org/2000/svg"
                                                style="display: inline-block; vertical-align: middle; margin-left: 6px"
                                            >
                                                <path
                                                    d="M1 2.5 L7 2.5 L4.5 6.5 L7 6.5 L3.5 10.5 L1 10.5 L3.5 6.5 L1 6.5 Z"
                                                    fill="#0C2A46"
                                                />
                                                <path
                                                    d="M5.5 2.5 L11.5 2.5 L9 6.5 L11.5 6.5 L8 10.5 L5.5 10.5 L8 6.5 L5.5 6.5 Z"
                                                    fill="#FA6400"
                                                />
                                                <text
                                                    x="13.5"
                                                    y="11"
                                                    fill="#FA6400"
                                                    font-family="'Inter', sans-serif"
                                                    font-weight="900"
                                                    font-style="italic"
                                                    font-size="10.5"
                                                    letter-spacing="-0.5px"
                                                >
                                                    GHN
                                                </text>
                                            </svg>
                                        </span>
                                        <span
                                            class="text-body-2 font-weight-medium"
                                            :class="{
                                                'text-blue-darken-4': shippingFee === 0,
                                                'text-grey-darken-1': shippingFee === null || shippingFeeLoading
                                            }"
                                        >
                                            <template v-if="shippingFeeLoading">
                                                <v-progress-circular
                                                    indeterminate
                                                    size="12"
                                                    width="2"
                                                    color="primary"
                                                    class="mr-1"
                                                ></v-progress-circular>
                                                Đang tính phí GHN...
                                            </template>
                                            <template v-else-if="shippingFee === null"> Chưa tính </template>
                                            <template v-else>
                                                <v-icon v-if="shippingFee === 0" size="14" color="#1e257c" class="mr-1"
                                                    >mdi-check-circle</v-icon
                                                >
                                                {{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}
                                            </template>
                                        </span>
                                    </div>
                                    <div v-if="voucherDiscount > 0" class="d-flex justify-space-between mb-1">
                                        <span class="text-body-2" style="color: #1e257c">
                                            <v-icon size="14" class="mr-1" style="color: #1e257c">mdi-ticket-percent</v-icon>
                                            Giảm giá
                                        </span>
                                        <span class="text-body-2 font-weight-bold" style="color: #1e257c">
                                            -{{ formatPrice(voucherDiscount) }}
                                        </span>
                                    </div>
                                </div>

                                <!-- Total -->
                                <div class="d-flex justify-space-between align-center mb-4 pa-4 total-bar">
                                    <span class="text-body-1 font-weight-bold total-label">Tổng thanh toán</span>
                                    <span class="text-h5 font-weight-bold total-label">{{ formatPrice(totalAmount) }}</span>
                                </div>

                                <v-btn
                                    style="background: #1e257c; color: white"
                                    rounded="pill"
                                    size="x-large"
                                    block
                                    class="font-weight-bold text-none place-order-btn"
                                    :loading="loading"
                                    :disabled="!isShippingValid"
                                    @click="openConfirmModal"
                                >
                                    <v-icon class="mr-2">{{ paymentMethod === 'VNPAY' ? 'mdi-qrcode-scan' : 'mdi-lock-outline' }}</v-icon>
                                    {{ paymentMethod === 'VNPAY' ? 'Thanh toán bằng VNPay' : 'Hoàn tất đặt hàng' }}
                                </v-btn>
                            </div>
                        </div>
                    </div>
                </v-col>
            </v-row>
        </v-container>

        <!-- Order Confirmation Modal -->
        <v-dialog v-model="showConfirmDialog" max-width="520" class="confirm-order-dialog">
            <div class="modal-content overflow-hidden rounded-xl bg-white elevation-10" style="border: 1px solid #e2e8f0">
                <div class="confirm-dialog-header pa-5 d-flex align-center justify-space-between" style="background: #1e257c !important; color: #ffffff !important">
                    <div class="d-flex align-center">
                        <v-icon size="24" class="mr-3" style="color: #ffffff !important">mdi-clipboard-check-outline</v-icon>
                        <h3 class="text-h6 font-weight-bold mb-0" style="color: #ffffff !important">Xác nhận thông tin đặt hàng</h3>
                    </div>
                    <v-btn icon variant="text" size="small" @click="showConfirmDialog = false" style="color: #ffffff !important">
                        <v-icon style="color: #ffffff !important">mdi-close</v-icon>
                    </v-btn>
                </div>

                <div class="pa-6 bg-white">
                    <!-- Delivery Info Block -->
                    <div class="mb-4 pa-4 rounded-lg" style="background: #f8fafc; border: 1px solid #f1f5f9">
                        <div class="d-flex align-center mb-2">
                            <v-icon size="18" style="color: #1e257c" class="mr-2">mdi-account-outline</v-icon>
                            <span class="text-body-2 font-weight-bold" style="color: #1e257c">Người nhận:</span>
                            <span class="text-body-2 font-weight-bold ml-2 text-dark"
                                >{{ shippingInfo.tenNguoiNhan }} ({{ shippingInfo.soDienThoai }})</span
                            >
                        </div>
                        <div class="d-flex align-start mb-2">
                            <v-icon size="18" style="color: #1e257c" class="mr-2 mt-1">mdi-map-marker-outline</v-icon>
                            <div class="text-body-2 text-grey-darken-2">
                                {{ fullAddressString }}
                            </div>
                        </div>
                        <div class="d-flex align-center">
                            <v-icon size="18" style="color: #1e257c" class="mr-2">mdi-wallet-outline</v-icon>
                            <span class="text-body-2 font-weight-medium text-grey-darken-2">Thanh toán:</span>
                            <v-chip size="x-small" class="ml-2 font-weight-bold" style="background: #1e257c; color: white">
                                {{ paymentMethod === 'VNPAY' ? 'VNPay (Thanh toán trực tuyến)' : 'COD (Thanh toán khi nhận hàng)' }}
                            </v-chip>
                        </div>
                    </div>

                    <!-- Order Total Breakdown -->
                    <div class="pa-4 rounded-lg border mb-6" style="border-color: #e2e8f0 !important">
                        <div class="d-flex justify-space-between text-body-2 mb-2 text-grey-darken-1">
                            <span>Sản phẩm trong đơn:</span>
                            <span class="font-weight-bold text-black">{{ cartStore.cartCount }} sản phẩm</span>
                        </div>
                        <div class="d-flex justify-space-between text-body-2 mb-2 text-grey-darken-1">
                            <span>Tạm tính:</span>
                            <span>{{ formatPrice(originalSubtotal) }}</span>
                        </div>
                        <div v-if="voucherDiscount > 0" class="d-flex justify-space-between text-body-2 mb-2 text-error font-weight-medium">
                            <span>Giảm giá Voucher:</span>
                            <span>-{{ formatPrice(voucherDiscount) }}</span>
                        </div>
                        <div class="d-flex justify-space-between text-body-2 mb-3 text-grey-darken-1">
                            <span>Phí vận chuyển (GHN):</span>
                            <span>{{ shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee) }}</span>
                        </div>
                        <v-divider class="mb-3"></v-divider>
                        <div class="d-flex justify-space-between align-center">
                            <span class="text-subtitle-1 font-weight-bold" style="color: #1e257c">Tổng thanh toán:</span>
                            <span class="text-h5 font-weight-bold" style="color: #1e257c">{{ formatPrice(totalAmount) }}</span>
                        </div>
                    </div>

                    <!-- Actions -->
                    <div class="d-flex ga-3">
                        <v-btn
                            variant="outlined"
                            color="grey-darken-1"
                            rounded="pill"
                            class="font-weight-bold text-none flex-grow-1"
                            height="46"
                            @click="showConfirmDialog = false"
                        >
                            Quay lại sửa
                        </v-btn>
                        <v-btn
                            style="background: #1e257c; color: white"
                            rounded="pill"
                            class="font-weight-bold text-none flex-grow-1"
                            height="46"
                            :loading="loading"
                            @click="confirmAndExecuteCheckout"
                        >
                            Xác nhận đặt hàng
                        </v-btn>
                    </div>
                </div>
            </div>
        </v-dialog>

        <!-- Voucher Dialog -->
        <v-dialog v-model="showVoucherDialog" max-width="520" class="voucher-dialog">
            <div class="modal-content">
                <div class="pa-6" style="background: #1e257c">
                    <div class="d-flex align-center justify-space-between">
                        <div class="d-flex align-center">
                            <v-icon size="24" class="mr-3" style="color: #ffffff !important">mdi-ticket-percent</v-icon>
                            <h3 class="text-h6 font-weight-bold mb-0" style="color: #e0f7fa !important">Chọn mã giảm giá</h3>
                        </div>
                        <v-btn icon variant="text" size="small" @click="showVoucherDialog = false" style="color: #ffffff !important">
                            <v-icon>mdi-close</v-icon>
                        </v-btn>
                    </div>
                </div>
                <div class="pa-6 bg-white">
                    <div v-if="voucherLoading" class="text-center py-10">
                        <v-progress-circular indeterminate color="#1e257c" size="40"></v-progress-circular>
                        <p class="text-body-2 text-grey mt-4">Đang tải mã giảm giá...</p>
                    </div>
                    <div v-else-if="availableVouchers.length === 0" class="text-center py-10">
                        <div class="empty-voucher-icon mx-auto mb-4">
                            <v-icon size="48" color="grey-lighten-2">mdi-ticket-outline</v-icon>
                        </div>
                        <p class="text-body-1 text-grey-darken-1 font-weight-medium mb-2">Không có mã giảm giá</p>
                        <p class="text-caption text-grey">Bạn chưa có mã giảm giá nào phù hợp với đơn hàng này.</p>
                    </div>
                    <div v-else class="voucher-list">
                        <div
                            v-for="v in availableVouchers"
                            :key="v.id"
                            class="voucher-item d-flex pa-4 mb-3 rounded-lg border"
                            :class="{ 
                                selected: selectedVoucher?.id === v.id,
                                'opacity-60': v.donHangToiThieu && cartStore.cartTotal < v.donHangToiThieu 
                            }"
                            style="cursor: pointer; transition: all 0.2s ease"
                            @click="selectVoucher(v)"
                        >
                            <div class="voucher-badge mr-4 px-2">
                                <span class="voucher-badge-text text-center" style="font-size: 0.75rem; white-space: nowrap">{{
                                    v.ma || v.maPhieu || v.maPhieuGiamGia || 'Mã'
                                }}</span>
                            </div>
                            <div class="flex-grow-1">
                                <div class="d-flex align-center justify-space-between mb-1">
                                    <h4 class="text-body-2 font-weight-bold">
                                        {{ v.ten || v.ma || v.maPhieu || v.maPhieuGiamGia || 'Mã Voucher' }}
                                    </h4>
                                    <v-icon v-if="selectedVoucher?.id === v.id" style="color: #1e257c" size="22">mdi-check-circle</v-icon>
                                    <div v-else class="unselected-radio"></div>
                                </div>
                                <p class="text-caption text-grey-darken-1 mb-1">
                                    <span v-if="v.loaiPhieu === 'PHAN_TRAM' || v.loaiPhieu === 'PERCENTAGE' || v.loaiPhieu === 1 || v.loaiPhieu === '1'">
                                        Giảm {{ v.phanTramGiamGia || v.discountPercent || v.giamGia }}%
                                        <span v-if="v.giamToiDa || v.maxDiscount" class="font-weight-medium" style="color: #ef4444"
                                            >(Tối đa {{ formatPrice(v.giamToiDa || v.maxDiscount) }})</span
                                        >
                                    </span>
                                    <span v-else>Giảm {{ formatPrice(v.soTienGiam || v.discountAmount || v.giamGia) }} trực tiếp</span>
                                </p>
                                <div class="d-flex align-center justify-space-between flex-wrap ga-1 mt-1">
                                    <p v-if="v.donHangToiThieu" class="text-caption font-weight-bold mb-0" style="color: #1e257c">
                                        <v-icon size="13" class="mr-1" style="color: #1e257c">mdi-cart-outline</v-icon>
                                        Đơn tối thiểu: {{ formatPrice(v.donHangToiThieu) }}
                                    </p>
                                    <p v-else class="text-caption font-weight-bold mb-0 text-success">
                                        <v-icon size="13" class="mr-1" color="success">mdi-cart-outline</v-icon>
                                        Áp dụng cho mọi đơn hàng
                                    </p>

                                    <span v-if="v.donHangToiThieu && cartStore.cartTotal < v.donHangToiThieu" class="text-caption font-weight-bold text-warning">
                                        Cần mua thêm {{ formatPrice(v.donHangToiThieu - cartStore.cartTotal) }}
                                    </span>
                                    <span v-else class="text-caption font-weight-bold text-success">
                                        Đủ điều kiện
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </v-dialog>

        <!-- VNPay QR Dialog for Online Checkout -->
        <v-dialog v-model="vnpayDialog.show" max-width="450" persistent>
            <v-card class="rounded-xl overflow-hidden pb-4">
                <v-card-text class="pt-6 text-center d-flex flex-column align-center">
                    <div class="vnpay-logo-wrapper mb-4">
                        <v-img src="https://vnpay.vn/assets/images/logo-icon/logo-primary.svg" width="60" />
                    </div>

                    <h3 class="text-h6 font-weight-bold mb-1">Thanh toán VNPay</h3>
                    <p class="text-subtitle-2 text-grey-darken-1 mb-6">Mã đơn: {{ vnpayDialog.orderCode }}</p>

                    <div class="pa-2 bg-white rounded-lg elevation-2 mb-4 d-inline-block">
                        <v-img :src="vnpayDialog.qrUrl" width="220" height="220" />
                    </div>
                    <div class="text-h5 font-weight-bold text-error mb-1">
                        {{
                            new Intl.NumberFormat('vi-VN', {
                                style: 'currency',
                                currency: 'VND'
                            }).format(vnpayDialog.amount)
                        }}
                    </div>
                    <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                        Sử dụng ứng dụng ngân hàng hoặc ví VNPay để quét mã.
                    </div>

                    <div class="w-100 px-2">
                        <v-btn
                            variant="outlined"
                            color="#005BAA"
                            block
                            class="mb-3 rounded-lg font-weight-bold"
                            height="44"
                            @click="openVnPayGateway"
                        >
                            Mở cổng thanh toán VNPay
                        </v-btn>

                        <v-btn
                            block
                            color="#005BAA"
                            class="mb-3 rounded-lg text-white font-weight-bold"
                            height="48"
                            :loading="vnpayDialog.checking"
                            @click="onConfirmPaidOnline"
                        >
                            XÁC NHẬN ĐÃ THANH TOÁN
                        </v-btn>

                        <v-btn
                            variant="text"
                            color="grey-darken-1"
                            block
                            class="rounded-lg font-weight-medium"
                            height="40"
                            @click="handlePayLater"
                        >
                            Đóng và thanh toán sau
                        </v-btn>
                    </div>
                </v-card-text>
            </v-card>
        </v-dialog>

        <!-- Address Selector Modal -->
        <v-dialog v-model="showAddressSelectorModal" max-width="650">
            <v-card class="rounded-xl overflow-hidden">
                <v-card-title class="d-flex align-center py-4 px-6 bg-indigo-darken-4 text-white" style="background: #1e257c">
                    <v-icon icon="mdi-book-account-outline" class="mr-3" color="white"></v-icon>
                    Sổ địa chỉ đã lưu
                    <v-spacer></v-spacer>
                    <v-btn
                        icon="mdi-close"
                        variant="text"
                        color="white"
                        density="compact"
                        @click="showAddressSelectorModal = false"
                    ></v-btn>
                </v-card-title>

                <v-card-text class="pa-6">
                    <div v-if="userAddresses.length === 0" class="text-center py-8">
                        <v-icon size="48" color="grey">mdi-map-marker-off-outline</v-icon>
                        <p class="text-body-1 text-grey mt-2">Bạn chưa lưu địa chỉ nào trong sổ địa chỉ</p>
                        <v-btn
                            style="background: #1e257c; color: white"
                            class="rounded-pill mt-2 font-weight-bold"
                            @click="
                                showAddressSelectorModal = false;
                                openAddAddressModal();
                            "
                        >
                            + Thêm địa chỉ mới ngay
                        </v-btn>
                    </div>
                    <div v-else class="d-flex flex-column ga-3">
                        <v-card
                            v-for="addr in userAddresses"
                            :key="addr.id"
                            variant="outlined"
                            class="pa-4 rounded-lg cursor-pointer"
                            style="border: 1.5px solid #e2e8f0; transition: all 0.2s ease"
                            @click="applySavedAddress(addr)"
                        >
                            <div class="d-flex align-center justify-space-between mb-1">
                                <div class="d-flex align-center">
                                    <span class="font-weight-bold text-subtitle-1 text-slate-900 mr-2">{{ addr.tenNguoiNhan }}</span>
                                    <span class="text-body-2 text-grey-darken-1">({{ addr.sdtNguoiNhan }})</span>
                                </div>
                                <v-chip v-if="addr.laMacDinh" color="amber-darken-3" size="x-small" variant="flat" class="font-weight-bold">
                                    Mặc định
                                </v-chip>
                            </div>
                            <p class="text-body-2 text-grey-darken-3 mb-0">
                                <v-icon size="16" color="#1e257c" class="mr-1">mdi-map-marker</v-icon>
                                {{ formatFullAddressString(addr) }}
                            </p>
                        </v-card>
                    </div>
                </v-card-text>

                <v-card-actions class="pa-4 bg-grey-lighten-4 d-flex justify-space-between">
                    <v-btn
                        variant="text"
                        class="text-none font-weight-bold"
                        style="color: #1e257c"
                        @click="
                            showAddressSelectorModal = false;
                            openAddAddressModal();
                        "
                    >
                        + Thêm địa chỉ mới
                    </v-btn>
                    <v-btn variant="outlined" class="text-none font-weight-bold rounded-pill" @click="showAddressSelectorModal = false">
                        Đóng
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Quick Add Address Modal -->
        <v-dialog v-model="showNewAddressModal" max-width="600" persistent>
            <v-card class="rounded-xl overflow-hidden">
                <v-card-title class="d-flex align-center py-4 px-6 text-white" style="background: #1e257c">
                    <v-icon icon="mdi-map-marker-plus-outline" class="mr-3" color="white"></v-icon>
                    Thêm nhanh địa chỉ giao hàng mới
                    <v-spacer></v-spacer>
                    <v-btn
                        icon="mdi-close"
                        variant="text"
                        color="white"
                        density="compact"
                        :disabled="newAddressLoading"
                        @click="showNewAddressModal = false"
                    ></v-btn>
                </v-card-title>

                <v-card-text class="pa-6">
                    <v-row dense>
                        <v-col cols="12" sm="6">
                            <v-text-field
                                v-model="newAddressForm.tenNguoiNhan"
                                label="Tên người nhận *"
                                variant="outlined"
                                density="comfortable"
                                prepend-inner-icon="mdi-account-outline"
                                maxlength="100"
                                counter="100"
                                :rules="[(v) => !!v?.trim() || 'Vui lòng nhập tên người nhận', (v) => (v && v.trim().length >= 2) || 'Tên tối thiểu 2 ký tự']"
                            ></v-text-field>
                        </v-col>
                        <v-col cols="12" sm="6">
                            <v-text-field
                                v-model="newAddressForm.sdtNguoiNhan"
                                label="Số điện thoại *"
                                variant="outlined"
                                density="comfortable"
                                prepend-inner-icon="mdi-phone-outline"
                                maxlength="10"
                                counter="10"
                                :rules="[(v) => !!v?.trim() || 'Vui lòng nhập SĐT', (v) => /^0[3|5|7|8|9][0-9]{8}$/.test(v?.trim() || '') || 'SĐT 10 số không hợp lệ']"
                            ></v-text-field>
                        </v-col>

                        <v-col cols="12" sm="4">
                            <v-autocomplete
                                v-model="newAddressForm.tinhThanh"
                                :items="provinces"
                                item-title="name"
                                item-value="code"
                                label="Tỉnh/Thành phố *"
                                variant="outlined"
                                density="comfortable"
                                :rules="[(v) => !!v || 'Vui lòng chọn Tỉnh/Thành']"
                                @update:model-value="
                                    (val) => {
                                        newAddressForm.quanHuyen = null;
                                        newAddressForm.phuongXa = null;
                                        if (val) fetchDistricts(val);
                                    }
                                "
                            ></v-autocomplete>
                        </v-col>
                        <v-col cols="12" sm="4">
                            <v-autocomplete
                                v-model="newAddressForm.quanHuyen"
                                :items="districts"
                                item-title="name"
                                item-value="code"
                                label="Quận/Huyện *"
                                variant="outlined"
                                density="comfortable"
                                :disabled="!newAddressForm.tinhThanh"
                                :rules="[(v) => !!v || 'Vui lòng chọn Quận/Huyện']"
                                @update:model-value="
                                    (val) => {
                                        newAddressForm.phuongXa = null;
                                        if (val) fetchWards(val);
                                    }
                                "
                            ></v-autocomplete>
                        </v-col>
                        <v-col cols="12" sm="4">
                            <v-autocomplete
                                v-model="newAddressForm.phuongXa"
                                :items="wards"
                                item-title="name"
                                item-value="code"
                                label="Phường/Xã *"
                                variant="outlined"
                                density="comfortable"
                                :disabled="!newAddressForm.quanHuyen"
                                :rules="[(v) => !!v || 'Vui lòng chọn Phường/Xã']"
                            ></v-autocomplete>
                        </v-col>

                        <v-col cols="12">
                            <v-text-field
                                v-model="newAddressForm.diaChiChiTiet"
                                label="Địa chỉ chi tiết (Số nhà, đường...) *"
                                variant="outlined"
                                density="comfortable"
                                prepend-inner-icon="mdi-home-city-outline"
                                maxlength="255"
                                counter="255"
                                :rules="[(v) => !!v?.trim() || 'Vui lòng nhập địa chỉ', (v) => (v && v.trim().length >= 5) || 'Địa chỉ tối thiểu 5 ký tự']"
                            ></v-text-field>
                        </v-col>

                        <v-col cols="12">
                            <v-checkbox
                                v-model="newAddressForm.laMacDinh"
                                label="Đặt làm địa chỉ mặc định"
                                color="#1e257c"
                                hide-details
                            ></v-checkbox>
                        </v-col>
                    </v-row>
                </v-card-text>

                <v-card-actions class="pa-4 bg-grey-lighten-4 d-flex justify-end ga-2">
                    <v-btn
                        variant="text"
                        class="text-none font-weight-bold"
                        :disabled="newAddressLoading"
                        @click="showNewAddressModal = false"
                    >
                        Hủy
                    </v-btn>
                    <v-btn
                        style="background: #1e257c; color: white"
                        variant="flat"
                        class="text-none font-weight-bold px-6 rounded-pill"
                        :loading="newAddressLoading"
                        @click="handleSaveNewAddress"
                    >
                        Lưu & Áp dụng
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Modal Cảnh báo Voucher Chưa đủ điều kiện tối thiểu -->
        <v-dialog v-model="showVoucherIneligibleModal" max-width="480" persistent>
            <v-card class="rounded-xl overflow-hidden shadow-2xl">
                <div class="pa-4 d-flex align-center justify-space-between text-white" style="background: #1e257c">
                    <div class="d-flex align-center">
                        <v-icon class="mr-2" color="amber-lighten-2">mdi-alert-circle</v-icon>
                        <span class="font-weight-bold text-subtitle-1">Chưa đạt đơn hàng tối thiểu</span>
                    </div>
                    <v-btn icon="mdi-close" variant="text" size="small" color="white" @click="showVoucherIneligibleModal = false"></v-btn>
                </div>
                <v-card-text class="pa-6">
                    <div class="pa-4 rounded-xl mb-4" style="background: #fffbeb; border: 1px solid #fde68a">
                        <div class="d-flex align-center mb-2">
                            <v-icon color="#b45309" size="20" class="mr-2">mdi-ticket-percent</v-icon>
                            <strong style="color: #92400e">{{ ineligibleVoucherInfo?.voucher?.ten || ineligibleVoucherInfo?.voucher?.ma }}</strong>
                        </div>
                        <p class="text-body-2 mb-0" style="color: #b45309">
                            Phiếu giảm giá yêu cầu giá trị đơn hàng tối thiểu <strong>{{ formatPrice(ineligibleVoucherInfo?.minOrder) }}</strong>.
                        </p>
                    </div>

                    <div class="py-2 px-3 bg-slate-50 rounded-lg mb-4 text-body-2">
                        <div class="d-flex justify-space-between py-1">
                            <span class="text-slate-500">Tiền hàng hiện tại:</span>
                            <strong class="text-slate-800">{{ formatPrice(ineligibleVoucherInfo?.currentTotal) }}</strong>
                        </div>
                        <div class="d-flex justify-space-between py-1">
                            <span class="text-slate-500">Đơn hàng tối thiểu:</span>
                            <strong class="text-slate-800">{{ formatPrice(ineligibleVoucherInfo?.minOrder) }}</strong>
                        </div>
                        <v-divider class="my-2"></v-divider>
                        <div class="d-flex justify-space-between py-1">
                            <span class="text-amber-800 font-weight-bold">Cần mua thêm:</span>
                            <strong style="color: #b45309; font-size: 1.05rem">+{{ formatPrice(ineligibleVoucherInfo?.shortfall) }}</strong>
                        </div>
                    </div>

                    <p class="text-caption text-slate-500 mb-0">
                        Bạn có thể gỡ phiếu giảm giá để tiếp tục đặt hàng ngay hoặc mua thêm sản phẩm để được áp dụng giảm giá.
                    </p>
                </v-card-text>
                <v-card-actions class="pa-4 bg-slate-50 border-t d-flex ga-2">
                    <v-btn
                        variant="outlined"
                        color="error"
                        class="flex-grow-1 font-weight-bold rounded-pill text-none"
                        @click="handleRemoveIneligibleVoucher"
                    >
                        <v-icon start size="16">mdi-ticket-remove</v-icon>
                        Gỡ phiếu giảm giá
                    </v-btn>
                    <v-btn
                        style="background: #1e257c; color: white !important"
                        variant="flat"
                        class="flex-grow-1 font-weight-bold rounded-pill text-none"
                        @click="handleContinueShopping"
                    >
                        <v-icon start size="16">mdi-cart-plus</v-icon>
                        Mua thêm sản phẩm
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Modal: Phiếu đã được admin thay đổi điều kiện & không còn đủ điều kiện -->
        <v-dialog v-model="showVoucherChangedModal" max-width="480" persistent>
            <v-card class="rounded-xl overflow-hidden shadow-2xl">
                <div class="pa-4 d-flex align-center justify-space-between text-white" style="background: #92400e">
                    <div class="d-flex align-center">
                        <v-icon class="mr-2" color="amber-lighten-3">mdi-alert-decagram</v-icon>
                        <span class="font-weight-bold text-subtitle-1">Phiếu giảm giá đã được cập nhật</span>
                    </div>
                    <v-btn icon="mdi-close" variant="text" size="small" color="white" @click="showVoucherChangedModal = false"></v-btn>
                </div>
                <v-card-text class="pa-6">
                    <div class="pa-4 rounded-xl mb-4" style="background: #fffbeb; border: 1px solid #fde68a">
                        <div class="d-flex align-center mb-2">
                            <v-icon color="#b45309" size="20" class="mr-2">mdi-ticket-percent</v-icon>
                            <strong style="color: #92400e">{{ voucherChangedInfo?.voucher?.ten || voucherChangedInfo?.voucher?.ma }}</strong>
                        </div>
                        <p class="text-body-2 mb-0" style="color: #b45309">
                            Phiếu giảm giá này <strong>vừa được cập nhật điều kiện</strong> bởi quản trị viên.
                            Bạn hiện chưa đủ điều kiện để áp dụng.
                        </p>
                    </div>

                    <div class="py-2 px-3 bg-slate-50 rounded-lg mb-4 text-body-2">
                        <div class="d-flex justify-space-between py-1">
                            <span class="text-slate-500">Giá trị đơn hàng hiện tại:</span>
                            <strong class="text-slate-800">{{ formatPrice(voucherChangedInfo?.currentTotal) }}</strong>
                        </div>
                        <div class="d-flex justify-space-between py-1">
                            <span class="text-slate-500">Đơn tối thiểu mới (sau cập nhật):</span>
                            <strong style="color: #b45309">{{ formatPrice(voucherChangedInfo?.minOrder) }}</strong>
                        </div>
                        <v-divider class="my-2"></v-divider>
                        <div class="d-flex justify-space-between py-1">
                            <span class="text-amber-800 font-weight-bold">Cần mua thêm để đủ điều kiện:</span>
                            <strong style="color: #b45309; font-size: 1.05rem">+{{ formatPrice(voucherChangedInfo?.shortfall) }}</strong>
                        </div>
                    </div>

                    <p class="text-caption text-slate-500 mb-0">
                        Phiếu giảm giá đã được gỡ bỏ. Bạn có thể mua thêm để đáp ứng điều kiện mới hoặc chọn mã khác phù hợp hơn.
                    </p>
                </v-card-text>
                <v-card-actions class="pa-4 bg-slate-50 border-t d-flex ga-2">
                    <v-btn
                        variant="outlined"
                        color="warning"
                        class="flex-grow-1 font-weight-bold rounded-pill text-none"
                        @click="showVoucherChangedModal = false; openVoucherModal()"
                    >
                        <v-icon start size="16">mdi-ticket-search</v-icon>
                        Chọn phiếu khác
                    </v-btn>
                    <v-btn
                        style="background: #92400e; color: white !important"
                        variant="flat"
                        class="flex-grow-1 font-weight-bold rounded-pill text-none"
                        @click="showVoucherChangedModal = false; handleContinueShopping()"
                    >
                        <v-icon start size="16">mdi-cart-plus</v-icon>
                        Mua thêm sản phẩm
                    </v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Main Footer -->
        <MainFooter class="mt-12" />

        <CustomerChat />
    </div>
</template>

<style scoped>
:deep(.v-field) {
    font-size: 0.95rem;
}

.confirm-dialog-header,
.confirm-dialog-header h3,
.confirm-dialog-header .text-white,
.confirm-dialog-header :deep(.v-icon),
.confirm-dialog-header :deep(i),
.confirm-dialog-header :deep(svg) {
    color: #ffffff !important;
    fill: #ffffff !important;
}

@media (max-width: 768px) {
    .order-summary-sticky {
        position: relative !important;
        top: 0 !important;
    }
}
</style>
