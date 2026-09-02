<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { dichVuKhachHang } from '@/services/public/dichVuKhachHang';
import { dichVuFile } from '@/services/core/dichVuFile';
import { useAuthStore } from '@/stores/authStore';
import { useUIStore } from '@/stores/ui';
import { PATH } from '@/router/routePaths';
import { useNotifications } from '@/services/notificationService';
import { useLocation } from '@/composables/useLocation';
import defaultAvatarUrl from '@/assets/images/profile/default-avatar.svg';

const router = useRouter();
const authStore = useAuthStore();
const uiStore = useUIStore();
const { addNotification } = useNotifications();

const loading = ref(true);
const isEditing = ref(false);
const passwordDialog = ref(false);
const passwordFormRef = ref(null);
const profileFormRef = ref(null);
const avatarFileInput = ref(null);
const avatarPreview = ref('');

// Location Composable for Province / District / Ward comboboxes
const {
    provinces,
    districts,
    wards,
    loadingLocations,
    fetchProvinces,
    fetchDistricts,
    fetchWards,
    cleanName
} = useLocation({ allowFallback: true });

const profileInfo = ref({
    tenTaiKhoan: '',
    email: '',
    ten: '',
    sdt: '',
    ngaySinh: '',
    gioiTinh: true,
    hinhAnh: ''
});

const addressForm = ref({
    tinh: null,
    thanhPho: null,
    phuongXa: null,
    diaChiChiTiet: ''
});

const passwordData = ref({
    matKhauCu: '',
    matKhauMoi: '',
    xacNhanMatKhau: ''
});

const phoneRegex = /^0[3|5|7|8|9][0-9]{8}$/;

const nameRules = [
    (v) => !isEditing.value || !!v?.trim() || 'Vui lòng nhập họ và tên',
    (v) => !isEditing.value || (v && v.trim().length >= 2 && v.trim().length <= 100) || 'Họ và tên từ 2 đến 100 ký tự'
];
const phoneRules = [
    (v) => !isEditing.value || !!v?.trim() || 'Vui lòng nhập số điện thoại',
    (v) => !isEditing.value || phoneRegex.test(v?.trim() || '') || 'Số điện thoại 10 số không hợp lệ (VD: 0912345678)'
];
const passwordRules = [
    (v) => !!v || 'Vui lòng nhập mật khẩu',
    (v) => (v && v.length >= 6) || 'Mật khẩu phải chứa ít nhất 6 ký tự',
    (v) => (v && v.length <= 50) || 'Mật khẩu tối đa 50 ký tự'
];

const currentAvatarUrl = computed(() => {
    if (avatarPreview.value) return avatarPreview.value;
    const v = profileInfo.value.hinhAnh;
    if (!v) return defaultAvatarUrl;
    if (/^(https?:)?\/\//i.test(v) || v.startsWith('data:') || v.startsWith('blob:')) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
});

// Watchers for address cascade
watch(
    () => addressForm.value.tinh,
    (newVal, oldVal) => {
        if (oldVal !== undefined && isEditing.value) {
            addressForm.value.thanhPho = null;
            addressForm.value.phuongXa = null;
        }
        if (newVal) fetchDistricts(newVal);
    }
);

watch(
    () => addressForm.value.thanhPho,
    (newVal, oldVal) => {
        if (oldVal !== undefined && isEditing.value) {
            addressForm.value.phuongXa = null;
        }
        if (newVal) fetchWards(newVal);
    }
);

const fetchProfile = async (forceRefresh = false) => {
    if (!authStore.isLoggedIn) {
        router.push(PATH.LOGIN);
        return;
    }

    try {
        await fetchProvinces();
        const res = await dichVuKhachHang.layThongTinCaNhan(forceRefresh);
        if (res.success && res.data) {
            const data = res.data;
            profileInfo.value = {
                tenTaiKhoan: data.tenTaiKhoan || '',
                email: data.email || '',
                ten: data.ten || '',
                sdt: data.sdt || '',
                ngaySinh: data.ngaySinh || '',
                gioiTinh: data.gioiTinh !== undefined && data.gioiTinh !== null ? data.gioiTinh : true,
                hinhAnh: data.hinhAnh || ''
            };

            // Map address codes / names
            let matchedProvinceCode = null;
            let matchedDistrictCode = null;
            let matchedWardCode = null;

            if (data.tinhThanh || data.tinh) {
                const provName = data.tinhThanh || data.tinh;
                const prov = provinces.value.find(
                    (p) => cleanName(p.name) === cleanName(provName) || String(p.code) === String(provName)
                );
                if (prov) {
                    matchedProvinceCode = prov.code;
                    await fetchDistricts(prov.code);
                    const distName = data.quanHuyen || data.thanhPho;
                    if (distName) {
                        const dist = districts.value.find(
                            (d) => cleanName(d.name) === cleanName(distName) || String(d.code) === String(distName)
                        );
                        if (dist) {
                            matchedDistrictCode = dist.code;
                            await fetchWards(dist.code);
                            if (data.phuongXa) {
                                const w = wards.value.find(
                                    (x) => cleanName(x.name) === cleanName(data.phuongXa) || String(x.code) === String(data.phuongXa)
                                );
                                if (w) matchedWardCode = w.code;
                            }
                        }
                    }
                }
            }

            addressForm.value = {
                tinh: matchedProvinceCode,
                thanhPho: matchedDistrictCode,
                phuongXa: matchedWardCode,
                diaChiChiTiet: data.diaChiChiTiet || ''
            };
        }
    } catch (error) {
        console.error('Lỗi khi tải thông tin hồ sơ:', error);
    } finally {
        loading.value = false;
    }
};

const fullAddressDisplay = computed(() => {
    const provObj = provinces.value.find((p) => p.code === addressForm.value.tinh || String(p.code) === String(addressForm.value.tinh));
    const distObj = districts.value.find((d) => d.code === addressForm.value.thanhPho || String(d.code) === String(addressForm.value.thanhPho));
    const wardObj = wards.value.find((w) => w.code === addressForm.value.phuongXa || String(w.code) === String(addressForm.value.phuongXa));

    const parts = [
        addressForm.value.diaChiChiTiet,
        wardObj ? wardObj.name : '',
        distObj ? distObj.name : '',
        provObj ? provObj.name : ''
    ].filter((p) => p && p.trim() !== '');

    return parts.length > 0 ? parts.join(', ') : 'Chưa cập nhật địa chỉ';
});

const triggerAvatarPick = () => {
    if (avatarFileInput.value) {
        avatarFileInput.value.click();
    }
};

const handleAvatarChange = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        addNotification({ title: 'Lỗi', subtitle: 'Vui lòng chọn file hình ảnh hợp lệ (PNG, JPG, JPEG).', color: 'error' });
        return;
    }

    try {
        const compressed = await dichVuFile.nenAnh(file, 800, 800, 0.85);
        const reader = new FileReader();
        reader.onload = (e) => {
            avatarPreview.value = e.target.result;
            profileInfo.value.hinhAnh = e.target.result;
        };
        reader.readAsDataURL(compressed);
        addNotification({ title: 'Thành công', subtitle: 'Đã chọn ảnh đại diện mới. Bấm "Lưu thay đổi" để cập nhật.', color: 'info' });
    } catch (e) {
        console.error('Lỗi nén ảnh avatar:', e);
    }
};

const startEditing = async () => {
    isEditing.value = true;
    if (provinces.value.length === 0) {
        await fetchProvinces();
    }
};

const cancelEditing = async () => {
    isEditing.value = false;
    avatarPreview.value = '';
    await fetchProfile(true);
};

const submitUpdateProfile = async () => {
    if (profileFormRef.value) {
        const { valid } = await profileFormRef.value.validate();
        if (!valid) return;
    }

    try {
        uiStore.showLoading('Đang cập nhật hồ sơ...');

        const provObj = provinces.value.find((p) => p.code === addressForm.value.tinh || String(p.code) === String(addressForm.value.tinh));
        const distObj = districts.value.find((d) => d.code === addressForm.value.thanhPho || String(d.code) === String(addressForm.value.thanhPho));
        const wardObj = wards.value.find((w) => w.code === addressForm.value.phuongXa || String(w.code) === String(addressForm.value.phuongXa));

        let formattedDob = profileInfo.value.ngaySinh;
        if (formattedDob instanceof Date) {
            const y = formattedDob.getFullYear();
            const m = String(formattedDob.getMonth() + 1).padStart(2, '0');
            const d = String(formattedDob.getDate()).padStart(2, '0');
            formattedDob = `${y}-${m}-${d}`;
        } else if (typeof formattedDob === 'string' && formattedDob.includes('/')) {
            const parts = formattedDob.split('/');
            if (parts.length === 3) {
                formattedDob = `${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}`;
            }
        }

        const res = await dichVuKhachHang.capNhatHoSo({
            ten: profileInfo.value.ten?.trim(),
            sdt: profileInfo.value.sdt?.trim(),
            ngaySinh: formattedDob || null,
            gioiTinh: profileInfo.value.gioiTinh,
            hinhAnh: profileInfo.value.hinhAnh,
            tinh: provObj ? provObj.name : null,
            thanhPho: distObj ? distObj.name : null,
            phuongXa: wardObj ? wardObj.name : null,
            diaChiChiTiet: addressForm.value.diaChiChiTiet?.trim() || null
        });

        if (res?.data) {
            authStore.userProfile = res.data;
            sessionStorage.setItem('userProfile', JSON.stringify(res.data));
            window.dispatchEvent(new CustomEvent('profile-updated', { detail: res.data }));
        }

        addNotification({ title: 'Thành công', subtitle: 'Cập nhật hồ sơ thành công', color: 'success' });
        isEditing.value = false;
        avatarPreview.value = '';
        await fetchProfile(true);
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: error.response?.data?.message || 'Lỗi cập nhật hồ sơ', color: 'error' });
    } finally {
        uiStore.hideLoading();
    }
};

const handleLogout = async () => {
    await authStore.logout();
    addNotification({
        title: 'Đăng xuất',
        subtitle: 'Bạn đã đăng xuất tài khoản thành công.',
        color: 'info'
    });
    router.push(PATH.LANDING);
};

const submitChangePassword = async () => {
    const { valid } = await passwordFormRef.value.validate();
    if (!valid) return;

    try {
        uiStore.showLoading('Đang đổi mật khẩu...');
        await dichVuKhachHang.doiMatKhau(passwordData.value);
        addNotification({ title: 'Thành công', subtitle: 'Đổi mật khẩu thành công', color: 'success' });
        passwordDialog.value = false;
        passwordData.value = { matKhauCu: '', matKhauMoi: '', xacNhanMatKhau: '' };
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: error.response?.data?.message || 'Lỗi đổi mật khẩu', color: 'error' });
    } finally {
        uiStore.hideLoading();
    }
};

onMounted(() => {
    fetchProfile();
});
</script>

<template>
    <div class="customer-profile-page bg-slate-50 min-h-screen">
        <MainHeader />

        <v-container class="py-10">
            <!-- Breadcrumbs -->
            <div class="d-flex align-center text-body-2 text-grey-darken-1 mb-8">
                <span class="cursor-pointer hover:text-blue-darken-4 font-weight-medium" @click="router.push(PATH.LANDING)">Trang chủ</span>
                <v-icon size="16" class="mx-2">mdi-chevron-right</v-icon>
                <span class="font-weight-bold text-blue-darken-4">Tài khoản của tôi</span>
            </div>

            <v-row>
                <!-- Sidebar Menu -->
                <v-col cols="12" md="3">
                    <v-card class="profile-card rounded-xl text-center pa-6" elevation="0">
                        <div class="d-flex flex-column align-center justify-center mb-4 position-relative">
                            <div class="avatar-container position-relative">
                                <v-avatar size="110" color="grey-lighten-4" class="elevation-3 cursor-pointer" @click="triggerAvatarPick">
                                    <v-img :src="currentAvatarUrl" cover></v-img>
                                </v-avatar>
                                <v-btn
                                    icon
                                    size="small"
                                    color="blue-darken-4"
                                    class="avatar-camera-btn elevation-2"
                                    @click="triggerAvatarPick"
                                >
                                    <v-icon size="16" color="white">mdi-camera</v-icon>
                                    <v-tooltip activator="parent" location="top">Đổi ảnh đại diện</v-tooltip>
                                </v-btn>
                            </div>
                            <input
                                ref="avatarFileInput"
                                type="file"
                                accept="image/*"
                                class="d-none"
                                @change="handleAvatarChange"
                            />
                        </div>

                        <div class="mb-6 border-bottom pb-4">
                            <h3 class="text-h5 font-weight-black text-blue-darken-4 mb-1">{{ profileInfo.ten || profileInfo.tenTaiKhoan || 'Khách hàng' }}</h3>
                            <p class="text-body-2 text-grey-darken-1 mb-0">{{ profileInfo.email || 'Chưa cập nhật Email' }}</p>
                        </div>

                        <v-list class="bg-transparent" density="compact" nav>
                            <v-list-item
                                color="blue-darken-4"
                                :active="true"
                                prepend-icon="mdi-account-outline"
                                title="Hồ sơ cá nhân"
                                class="rounded-lg mb-1 font-weight-bold"
                            ></v-list-item>
                            <v-list-item
                                prepend-icon="mdi-package-variant-closed"
                                title="Đơn mua của tôi"
                                class="rounded-lg mb-1 text-grey-darken-2"
                                @click="router.push(PATH.ORDERS)"
                            ></v-list-item>
                            <v-list-item
                                prepend-icon="mdi-lock-outline"
                                title="Đổi mật khẩu"
                                class="rounded-lg mb-1 text-grey-darken-2"
                                @click="passwordDialog = true"
                            ></v-list-item>
                            <v-list-item
                                prepend-icon="mdi-logout"
                                title="Đăng xuất"
                                class="rounded-lg mt-4 text-red-darken-2 font-weight-bold"
                                @click="handleLogout"
                            ></v-list-item>
                        </v-list>
                    </v-card>
                </v-col>

                <!-- Profile Content -->
                <v-col cols="12" md="9">
                    <v-card class="profile-card rounded-xl pa-8 pa-md-10" elevation="0">
                        <div class="d-flex align-center justify-space-between mb-6 pb-6 border-bottom flex-wrap ga-3">
                            <div>
                                <h2 class="text-h4 font-weight-black text-blue-darken-4 mb-1">Hồ sơ cá nhân</h2>
                                <p class="text-body-2 text-grey-darken-1 mb-0">Quản lý thông tin hồ sơ và địa chỉ giao nhận</p>
                            </div>
                            <div class="d-flex align-center ga-3">
                                <v-btn
                                    v-if="!isEditing"
                                    color="blue-darken-4"
                                    size="large"
                                    rounded="pill"
                                    class="text-none font-weight-bold elevation-2 px-6"
                                    @click="startEditing"
                                >
                                    <v-icon start size="18">mdi-account-edit-outline</v-icon>
                                    Cập nhật hồ sơ
                                </v-btn>
                                <template v-else>
                                    <v-btn
                                        color="grey-lighten-1"
                                        size="large"
                                        rounded="pill"
                                        class="text-none font-weight-bold px-5"
                                        @click="cancelEditing"
                                    >
                                        Hủy
                                    </v-btn>
                                    <v-btn
                                        color="blue-darken-4"
                                        size="large"
                                        rounded="pill"
                                        class="text-none font-weight-bold elevation-2 px-6"
                                        @click="submitUpdateProfile"
                                    >
                                        <v-icon start size="18">mdi-check</v-icon>
                                        Lưu thay đổi
                                    </v-btn>
                                </template>
                            </div>
                        </div>

                        <div v-if="loading" class="text-center py-12">
                            <v-progress-circular indeterminate color="blue-darken-4" size="48"></v-progress-circular>
                        </div>

                        <v-form ref="profileFormRef" v-else class="mt-4" :readonly="!isEditing">
                            <v-row>
                                <!-- Tên tài khoản -->
                                <v-col cols="12" sm="6" class="py-2">
                                    <label class="text-subtitle-2 font-weight-bold text-blue-darken-4 mb-2 d-block">Tên đăng nhập</label>
                                    <v-text-field
                                        v-model="profileInfo.tenTaiKhoan"
                                        variant="outlined"
                                        density="comfortable"
                                        prepend-inner-icon="mdi-account-circle-outline"
                                        hide-details="auto"
                                        class="profile-input"
                                        readonly
                                    ></v-text-field>
                                </v-col>

                                <!-- Họ và Tên -->
                                <v-col cols="12" sm="6" class="py-2">
                                    <label class="text-subtitle-2 font-weight-bold text-blue-darken-4 mb-2 d-block">Họ và Tên *</label>
                                    <v-text-field
                                        v-model="profileInfo.ten"
                                        variant="outlined"
                                        density="comfortable"
                                        prepend-inner-icon="mdi-card-account-details-outline"
                                        color="blue-darken-4"
                                        hide-details="auto"
                                        class="profile-input"
                                        maxlength="100"
                                        :readonly="!isEditing"
                                        :rules="nameRules"
                                    ></v-text-field>
                                </v-col>

                                <!-- Email -->
                                <v-col cols="12" sm="6" class="py-2">
                                    <label class="text-subtitle-2 font-weight-bold text-blue-darken-4 mb-2 d-block">Email</label>
                                    <v-text-field
                                        v-model="profileInfo.email"
                                        variant="outlined"
                                        density="comfortable"
                                        prepend-inner-icon="mdi-email-outline"
                                        hide-details="auto"
                                        class="profile-input"
                                        readonly
                                    ></v-text-field>
                                </v-col>

                                <!-- Số điện thoại -->
                                <v-col cols="12" sm="6" class="py-2">
                                    <label class="text-subtitle-2 font-weight-bold text-blue-darken-4 mb-2 d-block">Số điện thoại *</label>
                                    <v-text-field
                                        v-model="profileInfo.sdt"
                                        variant="outlined"
                                        density="comfortable"
                                        prepend-inner-icon="mdi-phone-outline"
                                        color="blue-darken-4"
                                        hide-details="auto"
                                        class="profile-input"
                                        maxlength="10"
                                        :readonly="!isEditing"
                                        :rules="phoneRules"
                                        @input="(e) => { profileInfo.sdt = String(e.target.value || '').replace(/\D/g, '').slice(0, 10); }"
                                    ></v-text-field>
                                </v-col>

                                <!-- Ngày sinh -->
                                <v-col cols="12" sm="6" class="py-2">
                                    <label class="text-subtitle-2 font-weight-bold text-blue-darken-4 mb-2 d-block">Ngày sinh</label>
                                    <AppDatePicker
                                        v-model="profileInfo.ngaySinh"
                                        :disabled="!isEditing"
                                        placeholder="Chọn ngày sinh"
                                        input-class="profile-input"
                                        :text-field-props="{
                                            variant: 'outlined',
                                            density: 'comfortable',
                                            color: 'blue-darken-4',
                                            'hide-details': 'auto'
                                        }"
                                    />
                                </v-col>

                                <!-- Giới tính -->
                                <v-col cols="12" sm="6" class="py-2">
                                    <label class="text-subtitle-2 font-weight-bold text-blue-darken-4 mb-2 d-block">Giới tính</label>
                                    <v-select
                                        v-model="profileInfo.gioiTinh"
                                        :items="[
                                            { title: 'Nam', value: true },
                                            { title: 'Nữ', value: false }
                                        ]"
                                        item-title="title"
                                        item-value="value"
                                        variant="outlined"
                                        density="comfortable"
                                        prepend-inner-icon="mdi-gender-male-female"
                                        color="blue-darken-4"
                                        hide-details="auto"
                                        class="profile-input"
                                        :readonly="!isEditing"
                                    ></v-select>
                                </v-col>

                                <!-- ĐỊA CHỈ PHẦN -->
                                <v-col cols="12" class="pt-4 pb-1">
                                    <div class="text-subtitle-1 font-weight-black text-blue-darken-4 border-b pb-2">
                                        <v-icon size="20" class="mr-1">mdi-map-marker-outline</v-icon>
                                        Địa chỉ mặc định
                                    </div>
                                </v-col>

                                <!-- Khi đang ở chế độ xem: hiển thị text đầy đủ -->
                                <template v-if="!isEditing">
                                    <v-col cols="12" class="py-2">
                                        <v-textarea
                                            :model-value="fullAddressDisplay"
                                            variant="outlined"
                                            density="comfortable"
                                            prepend-inner-icon="mdi-home-outline"
                                            rows="2"
                                            hide-details="auto"
                                            auto-grow
                                            class="profile-input"
                                            readonly
                                        ></v-textarea>
                                    </v-col>
                                </template>

                                <!-- Khi ở chế độ chỉnh sửa: hiển thị các combobox Tỉnh, Huyện, Xã, Chi tiết -->
                                <template v-else>
                                    <!-- Tỉnh / Thành phố -->
                                    <v-col cols="12" sm="4" class="py-2">
                                        <label class="text-caption font-weight-bold text-blue-darken-4 mb-1 d-block">Tỉnh / Thành phố</label>
                                        <v-autocomplete
                                            v-model="addressForm.tinh"
                                            :items="provinces"
                                            item-title="name"
                                            item-value="code"
                                            placeholder="Chọn Tỉnh/Thành phố"
                                            variant="outlined"
                                            density="comfortable"
                                            hide-details
                                            :loading="loadingLocations.provinces"
                                            class="profile-input"
                                        ></v-autocomplete>
                                    </v-col>

                                    <!-- Quận / Huyện -->
                                    <v-col cols="12" sm="4" class="py-2">
                                        <label class="text-caption font-weight-bold text-blue-darken-4 mb-1 d-block">Quận / Huyện</label>
                                        <v-autocomplete
                                            v-model="addressForm.thanhPho"
                                            :items="districts"
                                            item-title="name"
                                            item-value="code"
                                            placeholder="Chọn Quận/Huyện"
                                            variant="outlined"
                                            density="comfortable"
                                            hide-details
                                            :disabled="!addressForm.tinh"
                                            :loading="loadingLocations.districts"
                                            class="profile-input"
                                        ></v-autocomplete>
                                    </v-col>

                                    <!-- Phường / Xã -->
                                    <v-col cols="12" sm="4" class="py-2">
                                        <label class="text-caption font-weight-bold text-blue-darken-4 mb-1 d-block">Phường / Xã</label>
                                        <v-autocomplete
                                            v-model="addressForm.phuongXa"
                                            :items="wards"
                                            item-title="name"
                                            item-value="code"
                                            placeholder="Chọn Phường/Xã"
                                            variant="outlined"
                                            density="comfortable"
                                            hide-details
                                            :disabled="!addressForm.thanhPho"
                                            :loading="loadingLocations.wards"
                                            class="profile-input"
                                        ></v-autocomplete>
                                    </v-col>

                                    <!-- Địa chỉ chi tiết (Số nhà, tên đường...) -->
                                    <v-col cols="12" class="py-2">
                                        <label class="text-caption font-weight-bold text-blue-darken-4 mb-1 d-block">Địa chỉ chi tiết (Số nhà, tên đường, ngõ xóm...)</label>
                                        <v-text-field
                                            v-model="addressForm.diaChiChiTiet"
                                            placeholder="VD: Số 123 Đường Nguyễn Trãi..."
                                            variant="outlined"
                                            density="comfortable"
                                            prepend-inner-icon="mdi-home-outline"
                                            hide-details
                                            maxlength="255"
                                            class="profile-input"
                                        ></v-text-field>
                                    </v-col>
                                </template>
                            </v-row>
                        </v-form>
                    </v-card>
                </v-col>
            </v-row>
        </v-container>

        <!-- Đổi mật khẩu dialog -->
        <v-dialog v-model="passwordDialog" max-width="500">
            <v-card class="rounded-xl pa-4">
                <v-card-title class="text-h5 font-weight-bold text-center text-blue-darken-4 pt-4"> Đổi Mật Khẩu </v-card-title>
                <v-card-text>
                    <v-form ref="passwordFormRef">
                        <label class="text-body-2 font-weight-bold text-blue-darken-4 mb-1 d-block">Mật khẩu hiện tại</label>
                        <v-text-field
                            v-model="passwordData.matKhauCu"
                            type="password"
                            variant="outlined"
                            density="comfortable"
                            prepend-inner-icon="mdi-lock-outline"
                            color="blue-darken-4"
                            class="mb-3"
                            :rules="passwordRules"
                        ></v-text-field>

                        <label class="text-body-2 font-weight-bold text-blue-darken-4 mb-1 d-block">Mật khẩu mới</label>
                        <v-text-field
                            v-model="passwordData.matKhauMoi"
                            type="password"
                            variant="outlined"
                            density="comfortable"
                            prepend-inner-icon="mdi-lock-reset"
                            color="blue-darken-4"
                            class="mb-3"
                            :rules="passwordRules"
                        ></v-text-field>

                        <label class="text-body-2 font-weight-bold text-blue-darken-4 mb-1 d-block">Xác nhận mật khẩu mới</label>
                        <v-text-field
                            v-model="passwordData.xacNhanMatKhau"
                            type="password"
                            variant="outlined"
                            density="comfortable"
                            prepend-inner-icon="mdi-lock-check-outline"
                            color="blue-darken-4"
                            :rules="[
                                ...passwordRules,
                                (v) => v === passwordData.matKhauMoi || 'Mật khẩu xác nhận không khớp'
                            ]"
                        ></v-text-field>
                    </v-form>
                </v-card-text>
                <v-card-actions class="pa-4 justify-end">
                    <v-btn variant="text" color="grey-darken-1" class="text-none font-weight-bold" @click="passwordDialog = false">Hủy</v-btn>
                    <v-btn color="blue-darken-4" variant="flat" class="text-none font-weight-bold px-5 rounded-pill" @click="submitChangePassword">Xác nhận đổi</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>

        <!-- Main Footer -->
        <MainFooter class="mt-12" />

        <CustomerChat />
    </div>
</template>

<style scoped>
.customer-profile-page {
    font-family: inherit;
}

.profile-card {
    background: #ffffff !important;
    border: 1px solid rgba(0, 0, 0, 0.06);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04) !important;
}

.avatar-container {
    display: inline-block;
}

.avatar-camera-btn {
    position: absolute;
    bottom: 0;
    right: 0;
    border-radius: 50% !important;
    width: 32px !important;
    height: 32px !important;
    min-width: 32px !important;
}

.profile-input :deep(.v-field) {
    border-radius: 10px !important;
    background-color: #f8fafc !important;
}

.profile-input :deep(.v-field--focused) {
    background-color: #ffffff !important;
}
</style>
