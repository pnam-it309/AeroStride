<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { dichVuFile } from '@/services/core/dichVuFile';
import { APP_ROLES } from '@/constants/appConstants';
import { AdminBreadcrumbs } from '@/components/common';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const profile = ref(null);
const activeTab = ref('profile');

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
        profile.value = await dichVuXacThuc.layThongTinCaNhan();
    } catch (e) {
        console.error('Lỗi tải hồ sơ:', e);
    } finally {
        loading.value = false;
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
        <!-- Breadcrumbs -->
        <AdminBreadcrumbs
            :items="[
                { title: 'Tài khoản', disabled: false, href: '#' },
                { title: activeTab === 'account' ? 'Tài khoản của tôi' : 'Hồ sơ của tôi', disabled: true }
            ]"
        />

        <div v-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="#1e257c" size="44" width="4" />
            <p class="text-body-2 text-grey mt-4">Đang tải thông tin hồ sơ...</p>
        </div>

        <div v-else-if="profile" class="mt-3">
            <!-- Header Profile Summary Banner -->
            <div class="profile-header-banner mb-4">
                <div class="d-flex align-center flex-wrap ga-4 pa-5">
                    <v-avatar size="84" class="profile-avatar border">
                        <v-img v-if="avatarUrl" :src="avatarUrl" cover alt="avatar">
                            <template #placeholder>
                                <div class="d-flex align-center justify-center fill-height">
                                    <v-icon size="44" color="white">mdi-account</v-icon>
                                </div>
                            </template>
                        </v-img>
                        <v-icon v-else size="44" color="white">mdi-account</v-icon>
                    </v-avatar>

                    <div>
                        <h2 class="text-h6 font-weight-bold text-white mb-1">
                            {{ profile.ten || profile.tenTaiKhoan }}
                        </h2>
                        <div class="d-flex align-center ga-2 flex-wrap">
                            <span class="role-badge">
                                <v-icon size="14" class="mr-1" color="#1e257c">mdi-shield-check</v-icon>
                                {{ chucVu }}
                            </span>
                            <span class="text-caption text-white opacity-80" v-if="profile.ma">
                                Mã: {{ profile.ma }}
                            </span>
                        </div>
                    </div>
                </div>

                <!-- Modern Tabs Navigation -->
                <div class="profile-nav-tabs px-3">
                    <v-btn
                        variant="text"
                        :class="['profile-tab-btn', { 'profile-tab-active': activeTab === 'profile' }]"
                        @click="onTabChange('profile')"
                    >
                        <v-icon start size="18">mdi-account-circle-outline</v-icon>
                        Hồ sơ của tôi
                    </v-btn>
                    <v-btn
                        variant="text"
                        :class="['profile-tab-btn', { 'profile-tab-active': activeTab === 'account' }]"
                        @click="onTabChange('account')"
                    >
                        <v-icon start size="18">mdi-shield-lock-outline</v-icon>
                        Tài khoản & Đổi mật khẩu
                    </v-btn>
                </div>
            </div>

            <!-- TAB 1: THÔNG TIN HỒ SƠ CÁ NHÂN -->
            <v-card v-if="activeTab === 'profile'" elevation="0" class="profile-content-card border">
                <div class="pa-5">
                    <div class="d-flex align-center mb-4">
                        <v-icon color="#1e257c" size="20" class="mr-2">mdi-card-account-details-outline</v-icon>
                        <h3 class="text-subtitle-1 font-weight-bold text-slate-800 mb-0">Thông tin cá nhân chi tiết</h3>
                    </div>

                    <v-row dense>
                        <v-col cols="12" md="6">
                            <div class="info-field-box">
                                <div class="field-label">Mã nhân viên</div>
                                <div class="field-value">
                                    <v-icon size="16" class="mr-2" color="#1e257c">mdi-identifier</v-icon>
                                    {{ profile.ma || '—' }}
                                </div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-field-box">
                                <div class="field-label">Tên tài khoản</div>
                                <div class="field-value">
                                    <v-icon size="16" class="mr-2" color="#1e257c">mdi-account-outline</v-icon>
                                    {{ profile.tenTaiKhoan || '—' }}
                                </div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-field-box">
                                <div class="field-label">Email</div>
                                <div class="field-value">
                                    <v-icon size="16" class="mr-2" color="#1e257c">mdi-email-outline</v-icon>
                                    {{ profile.email || '—' }}
                                </div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-field-box">
                                <div class="field-label">Số điện thoại</div>
                                <div class="field-value">
                                    <v-icon size="16" class="mr-2" color="#1e257c">mdi-phone-outline</v-icon>
                                    {{ profile.sdt || '—' }}
                                </div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-field-box">
                                <div class="field-label">Giới tính</div>
                                <div class="field-value">
                                    <v-icon size="16" class="mr-2" color="#1e257c">mdi-gender-male-female</v-icon>
                                    {{ gioiTinhLabel }}
                                </div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-field-box">
                                <div class="field-label">Ngày sinh</div>
                                <div class="field-value">
                                    <v-icon size="16" class="mr-2" color="#1e257c">mdi-calendar-outline</v-icon>
                                    {{ profile.ngaySinh || '—' }}
                                </div>
                            </div>
                        </v-col>
                        <v-col cols="12">
                            <div class="info-field-box">
                                <div class="field-label">Địa chỉ liên hệ</div>
                                <div class="field-value">
                                    <v-icon size="16" class="mr-2" color="#1e257c">mdi-map-marker-outline</v-icon>
                                    {{ diaChiDayDu || '—' }}
                                </div>
                            </div>
                        </v-col>
                    </v-row>
                </div>
            </v-card>

            <!-- TAB 2: TÀI KHOẢN & ĐỔI MẬT KHẨU -->
            <v-card v-else-if="activeTab === 'account'" elevation="0" class="profile-content-card border">
                <div class="pa-5" style="max-width: 580px">
                    <div class="d-flex align-center mb-1">
                        <v-icon color="#1e257c" size="20" class="mr-2">mdi-lock-reset</v-icon>
                        <h3 class="text-subtitle-1 font-weight-bold text-slate-800 mb-0">Đổi mật khẩu tài khoản</h3>
                    </div>
                    <p class="text-caption text-slate-500 mb-4">
                        Để đảm bảo an toàn cho tài khoản, vui lòng sử dụng mật khẩu có ít nhất 6 ký tự.
                    </p>

                    <v-alert
                        v-if="pwMessage.text"
                        :type="pwMessage.type === 'success' ? 'success' : 'error'"
                        variant="tonal"
                        density="compact"
                        class="mb-4 rounded-lg"
                    >
                        {{ pwMessage.text }}
                    </v-alert>

                    <div class="mb-3">
                        <label class="form-input-label">Mật khẩu hiện tại</label>
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

                    <div class="mb-3">
                        <label class="form-input-label">Mật khẩu mới</label>
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

                    <div class="mb-4">
                        <label class="form-input-label">Xác nhận mật khẩu mới</label>
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

                    <v-btn
                        :loading="pwLoading"
                        color="primary"
                        variant="flat"
                        class="text-none font-weight-medium px-6 rounded-lg"
                        height="40"
                        style="background-color: #1e257c !important"
                        @click="handleChangePassword"
                    >
                        <v-icon start size="18">mdi-content-save-outline</v-icon>
                        Cập nhật mật khẩu
                    </v-btn>
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
    max-width: 960px;
    margin: 0 auto;
}

.profile-header-banner {
    background: linear-gradient(135deg, #1e257c 0%, #2a358c 100%);
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.12);
    overflow: hidden;
}

.profile-avatar {
    border: 3px solid rgba(255, 255, 255, 0.4);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.role-badge {
    display: inline-flex;
    align-items: center;
    background-color: #ffffff;
    color: #1e257c;
    font-weight: 600;
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 6px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.profile-nav-tabs {
    display: flex;
    gap: 8px;
    background-color: rgba(0, 0, 0, 0.15);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.profile-tab-btn {
    color: rgba(255, 255, 255, 0.8) !important;
    font-weight: 500;
    font-size: 13px;
    border-radius: 0;
    border-bottom: 2px solid transparent;
    padding: 10px 16px;
    transition: all 0.2s ease;
}

.profile-tab-btn:hover {
    color: #ffffff !important;
    background-color: rgba(255, 255, 255, 0.08);
}

.profile-tab-active {
    color: #ffffff !important;
    font-weight: 600 !important;
    border-bottom: 2px solid #ffffff !important;
    background-color: rgba(255, 255, 255, 0.12);
}

.profile-content-card {
    background-color: #ffffff;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
}

.info-field-box {
    background-color: #f8fafc;
    border: 1px solid #edf2f7;
    border-radius: 8px;
    padding: 10px 14px;
    margin-bottom: 6px;
    transition: all 0.2s;
}

.info-field-box:hover {
    background-color: #f1f5f9;
    border-color: #cbd5e1;
}

.field-label {
    font-size: 11px;
    font-weight: 600;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 3px;
}

.field-value {
    font-size: 14px;
    font-weight: 500;
    color: #1e293b;
    display: flex;
    align-items: center;
}

.form-input-label {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: #475569;
    margin-bottom: 4px;
}
</style>

