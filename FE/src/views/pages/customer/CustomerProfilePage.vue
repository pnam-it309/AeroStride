<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import { dichVuKhachHang } from '@/services/public/dichVuKhachHang';
import { useAuthStore } from '@/stores/authStore';
import { useUIStore } from '@/stores/ui';
import { PATH } from '@/router/routePaths';

import { useNotifications } from '@/services/notificationService';

const router = useRouter();
const authStore = useAuthStore();
const uiStore = useUIStore();
const { addNotification } = useNotifications();
const loading = ref(true);
const isEditing = ref(false);
const passwordDialog = ref(false);
const passwordFormRef = ref(null);

const profileInfo = ref({
    tenTaiKhoan: '',
    email: '',
    ten: '',
    sdt: '',
    ngaySinh: '',
    gioiTinh: true,
    hinhAnh: ''
});

const defaultAddress = ref({
    diaChiChiTiet: '',
    phuongXa: '',
    quanHuyen: '',
    tinhThanh: ''
});

const passwordData = ref({
    matKhauCu: '',
    matKhauMoi: '',
    xacNhanMatKhau: ''
});

import defaultAvatarUrl from '@/assets/images/profile/default-avatar.svg';
const phoneRegex = /^0[3|5|7|8|9][0-9]{8}$/;
const profileFormRef = ref(null);

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

const fetchProfile = async () => {
    if (!authStore.isLoggedIn) {
        router.push(PATH.LOGIN);
        return;
    }

    try {
        const res = await dichVuKhachHang.layThongTinCaNhan();
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
            defaultAddress.value = {
                diaChiChiTiet: data.diaChiChiTiet || '',
                phuongXa: data.phuongXa || '',
                quanHuyen: data.quanHuyen || '',
                tinhThanh: data.tinhThanh || ''
            };
        }
    } catch (error) {
        console.error('Lỗi khi tải thông tin hồ sơ:', error);
    } finally {
        loading.value = false;
    }
};

const fullAddress = () => {
    if (!defaultAddress.value) return 'Chưa thiết lập địa chỉ';
    const parts = [
        defaultAddress.value.diaChiChiTiet,
        defaultAddress.value.phuongXa,
        defaultAddress.value.quanHuyen,
        defaultAddress.value.tinhThanh
    ].filter((p) => p && p.trim() !== '');

    return parts.length > 0 ? parts.join(', ') : 'Chưa cập nhật địa chỉ';
};

const submitUpdateProfile = async () => {
    if (profileFormRef.value) {
        const { valid } = await profileFormRef.value.validate();
        if (!valid) return;
    }

    try {
        uiStore.showLoading('Đang cập nhật...');
        await dichVuKhachHang.capNhatHoSo({
            ten: profileInfo.value.ten?.trim(),
            sdt: profileInfo.value.sdt?.trim(),
            ngaySinh: profileInfo.value.ngaySinh || null,
            gioiTinh: profileInfo.value.gioiTinh,
            hinhAnh: profileInfo.value.hinhAnh
        });
        addNotification({ title: 'Thành công', subtitle: 'Cập nhật hồ sơ thành công', color: 'success' });
        isEditing.value = false;
        await fetchProfile();
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
        uiStore.showSnackbar('Đổi mật khẩu thành công', 'success');
        passwordDialog.value = false;
        passwordData.value = { matKhauCu: '', matKhauMoi: '', xacNhanMatKhau: '' };
    } catch (error) {
        uiStore.showSnackbar(error.response?.data?.message || 'Lỗi đổi mật khẩu', 'error');
    } finally {
        uiStore.hideLoading();
    }
};

onMounted(() => {
    fetchProfile();
});
</script>

<template>
    <div class="profile-page bg-grey-lighten-4 min-vh-100 pb-12">
        <MainHeader />
        <div class="header-spacing" style="height: 104px"></div>

        <v-container style="max-width: 1200px">
            <!-- Breadcrumb -->
            <div class="d-flex align-center mb-8">
                <router-link to="/" class="breadcrumb-link">Trang chủ</router-link>
                <v-icon size="16" class="mx-2 text-grey">mdi-chevron-right</v-icon>
                <span class="text-body-1 font-weight-bold text-blue-darken-4">Hồ sơ của tôi</span>
            </div>

            <v-row>
                <!-- Sidebar Menu -->
                <v-col cols="12" md="3">
                    <v-card class="profile-card rounded-xl text-center pa-6" elevation="0">
                        <div class="d-flex justify-center mb-4">
                            <v-avatar size="100" color="grey-lighten-4" class="elevation-2">
                                <v-img
                                    v-if="profileInfo.hinhAnh || defaultAvatarUrl"
                                    :src="profileInfo.hinhAnh || defaultAvatarUrl"
                                    cover
                                ></v-img>
                                <span v-else class="text-h3 font-weight-bold text-blue-darken-4">
                                    {{ profileInfo.tenTaiKhoan ? profileInfo.tenTaiKhoan.charAt(0).toUpperCase() : 'N' }}
                                </span>
                            </v-avatar>
                        </div>
                        <div class="mb-6 border-bottom">
                            <h3 class="text-h5 font-weight-black text-blue-darken-4 mb-1">{{ profileInfo.tenTaiKhoan || 'Người dùng' }}</h3>
                            <p class="text-body-2 text-grey-darken-1">{{ profileInfo.email || 'Chưa cập nhật Email' }}</p>
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
                                class="rounded-lg mt-4 text-red-darken-2"
                                @click="handleLogout"
                            ></v-list-item>
                        </v-list>
                    </v-card>
                </v-col>

                <!-- Profile Content -->
                <v-col cols="12" md="9">
                    <v-card class="profile-card rounded-xl pa-10" elevation="0">
                        <div class="d-flex align-center justify-space-between mb-8 pb-6 border-bottom">
                            <div>
                                <h2 class="text-h4 font-weight-black text-blue-darken-4 mb-2">Hồ sơ cá nhân</h2>
                                <p class="text-body-1 text-grey-darken-1 mb-0">Quản lý thông tin bảo mật cho tài khoản của bạn</p>
                            </div>
                            <div class="d-flex align-center ga-4">
                                <v-btn
                                    v-if="!isEditing"
                                    color="blue-darken-4"
                                    size="x-large"
                                    rounded="pill"
                                    class="text-none font-weight-bold elevation-3"
                                    @click="isEditing = true"
                                >
                                    Cập nhật hồ sơ
                                </v-btn>
                                <template v-else>
                                    <v-btn
                                        color="grey-lighten-1"
                                        size="x-large"
                                        rounded="pill"
                                        class="text-none font-weight-bold"
                                        @click="isEditing = false"
                                    >
                                        Hủy
                                    </v-btn>
                                    <v-btn
                                        color="blue-darken-4"
                                        size="x-large"
                                        rounded="pill"
                                        class="text-none font-weight-bold elevation-3"
                                        @click="submitUpdateProfile"
                                    >
                                        Lưu thay đổi
                                    </v-btn>
                                </template>
                            </div>
                        </div>

                        <div v-if="loading" class="text-center py-12">
                            <v-progress-circular indeterminate color="blue-darken-4" size="48"></v-progress-circular>
                        </div>

                        <v-form ref="profileFormRef" v-else class="mt-8" :readonly="!isEditing">
                            <v-row class="ma-n3">
                                <v-col cols="12" sm="6" class="pa-3">
                                    <label class="text-body-1 font-weight-bold text-blue-darken-4 mb-2 d-block">Tên đăng nhập</label>
                                    <v-text-field
                                        v-model="profileInfo.tenTaiKhoan"
                                        variant="outlined"
                                        prepend-inner-icon="mdi-account"
                                        hide-details="auto"
                                        class="profile-input"
                                        readonly
                                        maxlength="50"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6" class="pa-3">
                                    <label class="text-body-1 font-weight-bold text-blue-darken-4 mb-2 d-block">Họ và Tên</label>
                                    <v-text-field
                                        v-model="profileInfo.ten"
                                        variant="outlined"
                                        prepend-inner-icon="mdi-card-account-details"
                                        color="blue-darken-4"
                                        hide-details="auto"
                                        class="profile-input"
                                        maxlength="100"
                                        counter="100"
                                        :rules="nameRules"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6" class="pa-3">
                                    <label class="text-body-1 font-weight-bold text-blue-darken-4 mb-2 d-block">Email</label>
                                    <v-text-field
                                        v-model="profileInfo.email"
                                        variant="outlined"
                                        prepend-inner-icon="mdi-email"
                                        hide-details="auto"
                                        class="profile-input"
                                        readonly
                                        maxlength="100"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6" class="pa-3">
                                    <label class="text-body-1 font-weight-bold text-blue-darken-4 mb-2 d-block">Số điện thoại</label>
                                    <v-text-field
                                        v-model="profileInfo.sdt"
                                        variant="outlined"
                                        prepend-inner-icon="mdi-phone"
                                        color="blue-darken-4"
                                        hide-details="auto"
                                        class="profile-input"
                                        maxlength="10"
                                        counter="10"
                                        :rules="phoneRules"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6" class="pa-3">
                                    <label class="text-body-1 font-weight-bold text-blue-darken-4 mb-2 d-block">Ngày sinh</label>
                                    <v-text-field
                                        v-model="profileInfo.ngaySinh"
                                        type="date"
                                        variant="outlined"
                                        prepend-inner-icon="mdi-calendar"
                                        color="blue-darken-4"
                                        hide-details="auto"
                                        class="profile-input"
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" sm="6" class="pa-3">
                                    <label class="text-body-1 font-weight-bold text-blue-darken-4 mb-2 d-block">Giới tính</label>
                                    <v-select
                                        v-model="profileInfo.gioiTinh"
                                        :items="[
                                            { title: 'Nam', value: true },
                                            { title: 'Nữ', value: false }
                                        ]"
                                        item-title="title"
                                        item-value="value"
                                        variant="outlined"
                                        prepend-inner-icon="mdi-gender-male-female"
                                        color="blue-darken-4"
                                        hide-details="auto"
                                        class="profile-input"
                                    ></v-select>
                                </v-col>
                                <v-col cols="12" class="pa-3">
                                    <label class="text-body-1 font-weight-bold text-blue-darken-4 mb-2 d-block">Địa chỉ mặc định</label>
                                    <v-textarea
                                        :model-value="fullAddress()"
                                        variant="outlined"
                                        prepend-inner-icon="mdi-map-marker"
                                        color="blue-darken-4"
                                        rows="3"
                                        hide-details="auto"
                                        auto-grow
                                        class="profile-input"
                                        readonly
                                        maxlength="255"
                                    ></v-textarea>
                                </v-col>
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
                        <label class="text-body-2 font-weight-bold mb-2 d-block">Mật khẩu cũ</label>
                        <v-text-field
                            v-model="passwordData.matKhauCu"
                            type="password"
                            variant="outlined"
                            density="comfortable"
                            color="blue-darken-4"
                            maxlength="50"
                            :rules="passwordRules"
                        ></v-text-field>

                        <label class="text-body-2 font-weight-bold mb-2 d-block">Mật khẩu mới</label>
                        <v-text-field
                            v-model="passwordData.matKhauMoi"
                            type="password"
                            variant="outlined"
                            density="comfortable"
                            color="blue-darken-4"
                            maxlength="50"
                            :rules="passwordRules"
                        ></v-text-field>

                        <label class="text-body-2 font-weight-bold mb-2 d-block">Xác nhận mật khẩu mới</label>
                        <v-text-field
                            v-model="passwordData.xacNhanMatKhau"
                            type="password"
                            variant="outlined"
                            density="comfortable"
                            color="blue-darken-4"
                            maxlength="50"
                            :rules="[
                                (v) => !!v || 'Vui lòng xác nhận mật khẩu mới',
                                (v) => v === passwordData.matKhauMoi || 'Xác nhận mật khẩu không khớp'
                            ]"
                        ></v-text-field>
                    </v-form>
                </v-card-text>
                <v-card-actions class="pb-6 justify-center">
                    <v-btn color="grey-darken-1" variant="text" class="text-none px-6 rounded-pill" @click="passwordDialog = false"
                        >Hủy</v-btn
                    >
                    <v-btn
                        color="blue-darken-4"
                        variant="flat"
                        class="text-none px-8 rounded-pill font-weight-bold"
                        @click="submitChangePassword"
                        >Đổi mật khẩu</v-btn
                    >
                </v-card-actions>
            </v-card>
        </v-dialog>

        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.breadcrumb-link {
    text-decoration: none;
    font-size: 0.9rem;
    font-weight: 600;
    color: #757575;

    &:hover {
        color: #0d47a1;
    }
}

.border-bottom {
    border-bottom: 2px solid #f0f0f0;
}

.profile-card {
    border: 1px solid #e0e0e0;
    background: #fff;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04) !important;
}

/* Custom styling for inputs to make them look larger and softer */
:deep(.profile-input .v-field) {
    border-radius: 12px;
    background: #fafafa;
    border: 1px solid transparent;
    transition: all 0.3s ease;
}

:deep(.profile-input:hover .v-field) {
    background: #fff;
    border-color: #bbdefb;
}

:deep(.v-list-item) {
    padding: 12px 16px;
}
</style>
