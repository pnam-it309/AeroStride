<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { dichVuFile } from '@/services/core/dichVuFile';
import { APP_ROLES } from '@/constants/appConstants';
import { AdminBreadcrumbs } from '@/components/common';
import { useLocation } from '@/composables/useLocation';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const profile = ref(null);
const activeTab = ref('profile');

// Location composable for address selection
const {
    provinces,
    districts,
    wards,
    fetchProvinces,
    fetchDistricts,
    fetchWards
} = useLocation({ allowFallback: true });

// Edit Profile State
const isEditing = ref(false);
const saveLoading = ref(false);
const saveMessage = ref({ type: '', text: '' });
const editForm = ref({
    ten: '',
    email: '',
    sdt: '',
    gioiTinh: true,
    ngaySinh: '',
    tinh: '',
    thanhPho: '',
    phuongXa: '',
    diaChiChiTiet: '',
    hinhAnh: ''
});
const avatarPreview = ref('');
const avatarFileInput = ref(null);

// Password Form State
const pwForm = ref({ matKhauCu: '', matKhauMoi: '', xacNhanMatKhau: '' });
const pwLoading = ref(false);
const pwMessage = ref({ type: '', text: '' });
const showOldPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

const roleLabels = {
    [APP_ROLES.ADMIN]: 'Quản lý',
    [APP_ROLES.STAFF]: 'Nhân viên',
    [APP_ROLES.CUSTOMER]: 'Khách hàng'
};

const chucVu = computed(() => {
    if (profile.value?.chucVu) return profile.value.chucVu;
    return roleLabels[profile.value?.role] || 'Nhân viên';
});

const avatarUrl = computed(() => {
    if (avatarPreview.value) return avatarPreview.value;
    const v = profile.value?.hinhAnh || profile.value?.avatar;
    if (!v) return '';
    if (/^(https?:)?\/\//i.test(v) || v.startsWith('data:') || v.startsWith('blob:')) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
});

const diaChiDayDu = computed(() => {
    if (!profile.value) return '';
    return [profile.value.diaChiChiTiet, profile.value.phuongXa, profile.value.thanhPho, profile.value.tinh]
        .filter((x) => x && x.trim())
        .join(', ');
});

const gioiTinhLabel = computed(() => {
    if (profile.value?.gioiTinh === true) return 'Nam';
    if (profile.value?.gioiTinh === false) return 'Nữ';
    return 'Chưa cập nhật';
});

const fetchProfile = async () => {
    loading.value = true;
    try {
        const data = await dichVuXacThuc.layThongTinCaNhan();
        profile.value = data;
        initEditForm(data);
    } catch (e) {
        console.error('Lỗi tải hồ sơ:', e);
    } finally {
        loading.value = false;
    }
};

const initEditForm = (data) => {
    if (!data) return;
    editForm.value = {
        ten: data.ten || '',
        email: data.email || '',
        sdt: data.sdt || '',
        gioiTinh: data.gioiTinh !== undefined ? data.gioiTinh : true,
        ngaySinh: data.ngaySinh || '',
        tinh: data.tinh || '',
        thanhPho: data.thanhPho || '',
        phuongXa: data.phuongXa || '',
        diaChiChiTiet: data.diaChiChiTiet || '',
        hinhAnh: data.hinhAnh || ''
    };
    avatarPreview.value = '';
};

const startEditing = () => {
    saveMessage.value = { type: '', text: '' };
    initEditForm(profile.value);
    isEditing.value = true;
    if (provinces.value.length === 0) {
        fetchProvinces();
    }
};

const cancelEditing = () => {
    isEditing.value = false;
    saveMessage.value = { type: '', text: '' };
    avatarPreview.value = '';
    initEditForm(profile.value);
};

const triggerAvatarPick = () => {
    if (avatarFileInput.value) {
        avatarFileInput.value.click();
    }
};

const handleAvatarChange = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        saveMessage.value = { type: 'error', text: 'Vui lòng chọn file hình ảnh hợp lệ.' };
        return;
    }
    try {
        const compressed = await dichVuFile.nenAnh(file, 800, 800, 0.85);
        const reader = new FileReader();
        reader.onload = (e) => {
            avatarPreview.value = e.target.result;
            editForm.value.hinhAnh = e.target.result;
        };
        reader.readAsDataURL(compressed);
    } catch (e) {
        const reader = new FileReader();
        reader.onload = (e) => {
            avatarPreview.value = e.target.result;
            editForm.value.hinhAnh = e.target.result;
        };
        reader.readAsDataURL(file);
    }
};

const handleSaveProfile = async () => {
    saveMessage.value = { type: '', text: '' };
    if (!editForm.value.ten || !editForm.value.ten.trim()) {
        saveMessage.value = { type: 'error', text: 'Họ và tên không được để trống.' };
        return;
    }
    if (editForm.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editForm.value.email.trim())) {
        saveMessage.value = { type: 'error', text: 'Email không đúng định dạng.' };
        return;
    }
    if (editForm.value.sdt && !/^[0-9]{10,11}$/.test(editForm.value.sdt.trim().replace(/\D/g, ''))) {
        saveMessage.value = { type: 'error', text: 'Số điện thoại không hợp lệ (10-11 số).' };
        return;
    }

    saveLoading.value = true;
    try {
        const res = await dichVuXacThuc.capNhatThongTin(editForm.value);
        if (res?.data) {
            profile.value = res.data;
            initEditForm(res.data);
        } else {
            await fetchProfile();
        }
        saveMessage.value = { type: 'success', text: 'Cập nhật thông tin cá nhân thành công!' };
        isEditing.value = false;
    } catch (e) {
        saveMessage.value = {
            type: 'error',
            text: e.response?.data?.message || 'Cập nhật thông tin thất bại. Vui lòng thử lại.'
        };
    } finally {
        saveLoading.value = false;
    }
};

const handleChangePassword = async () => {
    pwMessage.value = { type: '', text: '' };
    if (!pwForm.value.matKhauCu || !pwForm.value.matKhauMoi || !pwForm.value.xacNhanMatKhau) {
        pwMessage.value = { type: 'error', text: 'Vui lòng nhập đầy đủ các trường mật khẩu.' };
        return;
    }
    if (pwForm.value.matKhauMoi.length < 6) {
        pwMessage.value = { type: 'error', text: 'Mật khẩu mới phải có ít nhất 6 ký tự.' };
        return;
    }
    if (pwForm.value.matKhauMoi !== pwForm.value.xacNhanMatKhau) {
        pwMessage.value = { type: 'error', text: 'Mật khẩu mới và xác nhận mật khẩu không khớp.' };
        return;
    }
    pwLoading.value = true;
    try {
        await dichVuXacThuc.doiMatKhau({ ...pwForm.value });
        pwMessage.value = { type: 'success', text: 'Đổi mật khẩu thành công.' };
        pwForm.value = { matKhauCu: '', matKhauMoi: '', xacNhanMatKhau: '' };
    } catch (e) {
        pwMessage.value = { type: 'error', text: e.response?.data?.message || 'Đổi mật khẩu thất bại.' };
    } finally {
        pwLoading.value = false;
    }
};

const onTabChange = (tab) => {
    activeTab.value = tab;
    router.replace({ query: { ...route.query, tab } });
};

watch(
    () => route.query.tab,
    (newTab) => {
        if (newTab === 'account') {
            activeTab.value = 'account';
        } else {
            activeTab.value = 'profile';
        }
    },
    { immediate: true }
);

onMounted(() => {
    fetchProfile();
});
</script>

<template>
    <div class="pa-4 font-body hoso-container">
        <!-- Breadcrumbs Cố Định, Không Nhảy Khi Đổi Tab -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Tài khoản', disabled: false, href: '#' },
                { title: 'Hồ sơ của tôi', disabled: true }
            ]"
        />

        <div v-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="#1e257c" size="44" width="4" />
            <p class="text-body-2 text-grey mt-4">Đang tải thông tin hồ sơ...</p>
        </div>

        <div v-else-if="profile" class="mt-3">
            <!-- UNIFIED SEAMLESS CARD LAYOUT (Cố định chiều rộng 100%, không co giật) -->
            <v-card elevation="0" class="profile-unified-card border">
                <!-- 1. Header Banner with Navy Gradient -->
                <div class="profile-header-banner">
                    <div class="d-flex align-center justify-space-between flex-wrap ga-4 pa-6">
                        <div class="d-flex align-center ga-5 flex-wrap">
                            <!-- Avatar with Upload Button -->
                            <div class="avatar-wrapper">
                                <v-avatar size="88" class="profile-avatar">
                                    <v-img v-if="avatarUrl" :src="avatarUrl" cover alt="avatar">
                                        <template #placeholder>
                                            <div class="d-flex align-center justify-center fill-height bg-primary">
                                                <v-icon size="48" color="white">mdi-account</v-icon>
                                            </div>
                                        </template>
                                    </v-img>
                                    <div v-else class="d-flex align-center justify-center fill-height bg-primary-dark">
                                        <v-icon size="48" color="white">mdi-account</v-icon>
                                    </div>
                                </v-avatar>

                                <v-btn
                                    icon="mdi-camera"
                                    size="small"
                                    color="white"
                                    class="avatar-edit-btn"
                                    title="Thay đổi ảnh đại diện"
                                    @click="triggerAvatarPick"
                                />
                                <input
                                    ref="avatarFileInput"
                                    type="file"
                                    accept="image/*"
                                    style="display: none"
                                    @change="handleAvatarChange"
                                />
                            </div>

                            <!-- Staff Name & Info -->
                            <div class="profile-info-text">
                                <h2 class="text-h5 font-weight-bold text-white mb-2 profile-name">
                                    {{ profile.ten || profile.tenTaiKhoan }}
                                </h2>
                                <div class="d-flex align-center ga-2 flex-wrap">
                                    <span class="role-badge">
                                        <v-icon size="14" class="mr-1" color="#1e257c">mdi-shield-check</v-icon>
                                        {{ chucVu }}
                                    </span>
                                    <span class="code-badge" v-if="profile.ma">
                                        Mã: {{ profile.ma }}
                                    </span>
                                    <span class="code-badge" v-if="profile.tenTaiKhoan">
                                        @{{ profile.tenTaiKhoan }}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <!-- Single Action Button on Header (1 Nút chỉnh sửa chung duy nhất) -->
                        <div class="d-flex align-center ga-2">
                            <v-btn
                                v-if="activeTab === 'profile' && !isEditing"
                                color="white"
                                variant="flat"
                                class="text-none font-weight-bold px-4 rounded-lg edit-header-btn"
                                prepend-icon="mdi-account-edit-outline"
                                @click="startEditing"
                            >
                                Chỉnh sửa thông tin
                            </v-btn>
                            <v-btn
                                v-if="activeTab === 'profile' && isEditing"
                                color="white"
                                variant="outlined"
                                class="text-none font-weight-medium px-4 rounded-lg cancel-header-btn"
                                prepend-icon="mdi-close"
                                @click="cancelEditing"
                            >
                                Hủy sửa
                            </v-btn>
                        </div>
                    </div>

                    <!-- Modern Seamless Integrated Tab Bar -->
                    <div class="profile-nav-tabs">
                        <button
                            type="button"
                            :class="['profile-tab-item', { 'active': activeTab === 'profile' }]"
                            @click="onTabChange('profile')"
                        >
                            <v-icon start size="18">mdi-account-circle-outline</v-icon>
                            Hồ sơ của tôi
                        </button>
                        <button
                            type="button"
                            :class="['profile-tab-item', { 'active': activeTab === 'account' }]"
                            @click="onTabChange('account')"
                        >
                            <v-icon start size="18">mdi-shield-lock-outline</v-icon>
                            Tài khoản & Đổi mật khẩu
                        </button>
                    </div>
                </div>

                <!-- 2. Content Body -->
                <div class="profile-body-content pa-6">
                    <!-- Global Save Message Alert -->
                    <v-alert
                        v-if="saveMessage.text"
                        :type="saveMessage.type === 'success' ? 'success' : 'error'"
                        variant="tonal"
                        density="compact"
                        closable
                        class="mb-5 rounded-lg"
                        @click:close="saveMessage.text = ''"
                    >
                        {{ saveMessage.text }}
                    </v-alert>

                    <!-- TAB 1: THÔNG TIN HỒ SƠ -->
                    <div v-show="activeTab === 'profile'" class="tab-content-panel">
                        <!-- 1.1 Chế độ XEM (Read-only View) -->
                        <div v-if="!isEditing">
                            <div class="d-flex align-center mb-4">
                                <div class="section-icon-box mr-3">
                                    <v-icon color="#1e257c" size="20">mdi-card-account-details-outline</v-icon>
                                </div>
                                <div>
                                    <h3 class="text-subtitle-1 font-weight-bold text-slate-800 mb-0">Thông tin cá nhân chi tiết</h3>
                                    <p class="text-caption text-slate-500 mb-0">Xem và quản lý thông tin tài khoản của bạn</p>
                                </div>
                            </div>

                            <v-row dense class="mt-1">
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Mã nhân viên</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-pound</v-icon>
                                            <span class="font-weight-semibold">{{ profile.ma || 'Chưa cập nhật' }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Tên tài khoản</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-account-outline</v-icon>
                                            <span>{{ profile.tenTaiKhoan || 'Chưa cập nhật' }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Họ và tên</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-card-text-outline</v-icon>
                                            <span class="font-weight-semibold">{{ profile.ten || 'Chưa cập nhật' }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Email</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-email-outline</v-icon>
                                            <span>{{ profile.email || 'Chưa cập nhật' }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Số điện thoại</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-phone-outline</v-icon>
                                            <span>{{ profile.sdt || 'Chưa cập nhật' }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Giới tính</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-gender-male-female</v-icon>
                                            <span>{{ gioiTinhLabel }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Ngày sinh</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-calendar-outline</v-icon>
                                            <span>{{ profile.ngaySinh || 'Chưa cập nhật' }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="info-field-box">
                                        <div class="field-label">Chức vụ / Quyền hạn</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#1e257c" class="mr-2">mdi-shield-account-outline</v-icon>
                                            <span class="font-weight-bold" style="color: #1e257c">{{ chucVu }}</span>
                                        </div>
                                    </div>
                                </v-col>
                                <v-col cols="12">
                                    <div class="info-field-box">
                                        <div class="field-label">Địa chỉ liên hệ</div>
                                        <div class="field-value">
                                            <v-icon size="16" color="#64748b" class="mr-2">mdi-map-marker-outline</v-icon>
                                            <span>{{ diaChiDayDu || 'Chưa cập nhật địa chỉ' }}</span>
                                        </div>
                                    </div>
                                </v-col>
                            </v-row>
                        </div>

                        <!-- 1.2 Chế độ SỬA (Edit Profile Mode) -->
                        <div v-else class="edit-profile-form">
                            <div class="d-flex align-center justify-space-between mb-4">
                                <div class="d-flex align-center">
                                    <div class="section-icon-box mr-3">
                                        <v-icon color="#1e257c" size="20">mdi-account-edit</v-icon>
                                    </div>
                                    <div>
                                        <h3 class="text-subtitle-1 font-weight-bold text-slate-800 mb-0">Chỉnh sửa thông tin</h3>
                                        <p class="text-caption text-slate-500 mb-0">Cập nhật thông tin cá nhân của bạn</p>
                                    </div>
                                </div>
                            </div>

                            <v-row dense>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Họ và tên <span class="text-error">*</span></label>
                                        <v-text-field
                                            v-model="editForm.ten"
                                            placeholder="Nhập họ và tên"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Email</label>
                                        <v-text-field
                                            v-model="editForm.email"
                                            placeholder="example@aerostride.com"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Số điện thoại</label>
                                        <v-text-field
                                            v-model="editForm.sdt"
                                            placeholder="0987654321"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Giới tính</label>
                                        <v-radio-group v-model="editForm.gioiTinh" inline hide-details density="compact" class="gender-radio-group">
                                            <v-radio label="Nam" :value="true" color="primary" />
                                            <v-radio label="Nữ" :value="false" color="primary" />
                                        </v-radio-group>
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Ngày sinh</label>
                                        <v-text-field
                                            v-model="editForm.ngaySinh"
                                            type="date"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Tỉnh / Thành phố</label>
                                        <v-select
                                            v-model="editForm.tinh"
                                            :items="provinces"
                                            item-title="name"
                                            item-value="name"
                                            placeholder="Chọn Tỉnh / Thành"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                            @update:model-value="fetchDistricts(editForm.tinh)"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Quận / Huyện</label>
                                        <v-select
                                            v-model="editForm.thanhPho"
                                            :items="districts"
                                            item-title="name"
                                            item-value="name"
                                            placeholder="Chọn Quận / Huyện"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                            :disabled="!editForm.tinh"
                                            @update:model-value="fetchWards(editForm.thanhPho)"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Phường / Xã</label>
                                        <v-select
                                            v-model="editForm.phuongXa"
                                            :items="wards"
                                            item-title="name"
                                            item-value="name"
                                            placeholder="Chọn Phường / Xã"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                            :disabled="!editForm.thanhPho"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12">
                                    <div class="mb-3">
                                        <label class="form-input-label">Địa chỉ chi tiết</label>
                                        <v-text-field
                                            v-model="editForm.diaChiChiTiet"
                                            placeholder="Số nhà, tên đường..."
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                        />
                                    </div>
                                </v-col>
                            </v-row>

                            <div class="d-flex justify-end ga-3 mt-4">
                                <v-btn
                                    variant="outlined"
                                    color="grey-darken-1"
                                    class="text-none px-5 rounded-lg"
                                    @click="cancelEditing"
                                >
                                    Hủy
                                </v-btn>
                                <v-btn
                                    :loading="saveLoading"
                                    color="primary"
                                    variant="flat"
                                    class="text-none font-weight-bold px-6 rounded-lg save-btn"
                                    style="background-color: #1e257c !important"
                                    @click="handleSaveProfile"
                                >
                                    Lưu thay đổi
                                </v-btn>
                            </div>
                        </div>
                    </div>

                    <!-- TAB 2: TÀI KHOẢN & ĐỔI MẬT KHẨU (Giao diện liền mạch, gọn gàng) -->
                    <div v-show="activeTab === 'account'" class="tab-content-panel">
                        <div class="d-flex align-center mb-4">
                            <div class="section-icon-box mr-3">
                                <v-icon color="#1e257c" size="20">mdi-lock-reset</v-icon>
                            </div>
                            <div>
                                <h3 class="text-subtitle-1 font-weight-bold text-slate-800 mb-0">Đổi mật khẩu tài khoản</h3>
                                <p class="text-caption text-slate-500 mb-0">
                                    Bảo vệ tài khoản với mật khẩu mạnh và an toàn
                                </p>
                            </div>
                        </div>

                        <div class="pa-5 rounded-xl border bg-white mt-1">
                            <v-alert
                                v-if="pwMessage.text"
                                :type="pwMessage.type === 'success' ? 'success' : 'error'"
                                variant="tonal"
                                density="compact"
                                class="mb-4 rounded-lg"
                            >
                                {{ pwMessage.text }}
                            </v-alert>

                            <v-row dense>
                                <v-col cols="12">
                                    <div class="mb-3">
                                        <label class="form-input-label">Mật khẩu hiện tại <span class="text-error">*</span></label>
                                        <v-text-field
                                            v-model="pwForm.matKhauCu"
                                            :type="showOldPassword ? 'text' : 'password'"
                                            placeholder="Nhập mật khẩu đang dùng"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                            prepend-inner-icon="mdi-lock-outline"
                                            :append-inner-icon="showOldPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                                            @click:append-inner="showOldPassword = !showOldPassword"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-3">
                                        <label class="form-input-label">Mật khẩu mới <span class="text-error">*</span></label>
                                        <v-text-field
                                            v-model="pwForm.matKhauMoi"
                                            :type="showNewPassword ? 'text' : 'password'"
                                            placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                            prepend-inner-icon="mdi-lock-plus-outline"
                                            :append-inner-icon="showNewPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                                            @click:append-inner="showNewPassword = !showNewPassword"
                                        />
                                    </div>
                                </v-col>
                                <v-col cols="12" md="6">
                                    <div class="mb-4">
                                        <label class="form-input-label">Xác nhận mật khẩu mới <span class="text-error">*</span></label>
                                        <v-text-field
                                            v-model="pwForm.xacNhanMatKhau"
                                            :type="showConfirmPassword ? 'text' : 'password'"
                                            placeholder="Nhập lại mật khẩu mới"
                                            variant="outlined"
                                            density="compact"
                                            hide-details="auto"
                                            rounded="lg"
                                            prepend-inner-icon="mdi-lock-check-outline"
                                            :append-inner-icon="showConfirmPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                                            @click:append-inner="showConfirmPassword = !showConfirmPassword"
                                        />
                                    </div>
                                </v-col>
                            </v-row>

                            <div class="d-flex justify-end mt-3">
                                <v-btn
                                    :loading="pwLoading"
                                    color="primary"
                                    variant="flat"
                                    class="text-none font-weight-bold px-6 rounded-lg save-btn"
                                    height="42"
                                    style="background-color: #1e257c !important"
                                    @click="handleChangePassword"
                                >
                                    <v-icon start size="18">mdi-content-save-check-outline</v-icon>
                                    Cập nhật mật khẩu
                                </v-btn>
                            </div>
                        </div>
                    </div>
                </div>
            </v-card>
        </div>

        <div v-else class="text-center py-16">
            <v-icon size="48" color="grey">mdi-account-alert-outline</v-icon>
            <p class="text-body-2 text-grey mt-4">Không tải được thông tin hồ sơ.</p>
        </div>
    </div>
</template>

<style scoped>
.hoso-container {
    max-width: 980px;
    margin: 0 auto;
}

/* UNIFIED PROFILE CARD */
.profile-unified-card {
    background-color: #ffffff;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    border: 1px solid #e2e8f0;
}

/* HEADER BANNER */
.profile-header-banner {
    background: linear-gradient(135deg, #1e257c 0%, #151b5c 100%);
    position: relative;
}

.avatar-wrapper {
    position: relative;
    display: inline-block;
}

.profile-avatar {
    border: 3px solid rgba(255, 255, 255, 0.6);
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
    background-color: #2a358c;
}

.avatar-edit-btn {
    position: absolute;
    bottom: -2px;
    right: -2px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
    background-color: #ffffff !important;
    color: #1e257c !important;
    width: 30px !important;
    height: 30px !important;
}

.profile-name {
    color: #ffffff !important;
    letter-spacing: -0.3px;
}

.role-badge {
    display: inline-flex;
    align-items: center;
    background-color: #ffffff;
    color: #1e257c;
    font-weight: 700;
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 6px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.code-badge {
    display: inline-flex;
    align-items: center;
    background-color: rgba(255, 255, 255, 0.15);
    color: #ffffff;
    font-weight: 500;
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 6px;
    border: 1px solid rgba(255, 255, 255, 0.2);
}

.edit-header-btn {
    color: #1e257c !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.cancel-header-btn {
    color: #ffffff !important;
    border-color: rgba(255, 255, 255, 0.6) !important;
}

/* SEAMLESS TAB BAR */
.profile-nav-tabs {
    display: flex;
    gap: 4px;
    padding: 0 20px;
    background-color: rgba(0, 0, 0, 0.18);
    border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.profile-tab-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 12px 20px;
    color: rgba(255, 255, 255, 0.75);
    font-weight: 600;
    font-size: 13px;
    border: none;
    background: transparent;
    cursor: pointer;
    position: relative;
    transition: all 0.2s ease;
    outline: none;
}

.profile-tab-item:hover {
    color: #ffffff;
    background-color: rgba(255, 255, 255, 0.08);
}

.profile-tab-item.active {
    color: #ffffff;
    font-weight: 700;
    background-color: rgba(255, 255, 255, 0.15);
}

.profile-tab-item.active::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 3px;
    background-color: #60a5fa;
    border-radius: 3px 3px 0 0;
}

/* SECTION ICONS */
.section-icon-box {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    background-color: #eef2ff;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* READ-ONLY INFO BOXES */
.info-field-box {
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 12px 16px;
    margin-bottom: 8px;
    transition: all 0.2s ease;
}

.info-field-box:hover {
    background-color: #f1f5f9;
    border-color: #cbd5e1;
}

.field-label {
    font-size: 11px;
    font-weight: 700;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 4px;
}

.field-value {
    font-size: 14px;
    font-weight: 500;
    color: #1e293b;
    display: flex;
    align-items: center;
}

/* FORM LABELS & INPUTS */
.form-input-label {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: #334155;
    margin-bottom: 6px;
}

.gender-radio-group {
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 2px 12px;
}

.profile-body-content {
    min-height: 460px;
}

.save-btn {
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.25) !important;
}

.tab-content-panel {
    /* Đứng yên tĩnh, không dùng translateY gây giật layout */
    width: 100%;
}
</style>
