<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { dichVuFile } from '@/services/core/dichVuFile';

const route = useRoute();
const loading = ref(true);
const profile = ref(null);

const pwForm = ref({ matKhauCu: '', matKhauMoi: '', xacNhanMatKhau: '' });
const pwLoading = ref(false);
const pwMessage = ref({ type: '', text: '' });

const roleLabels = {
    ROLE_QUAN_TRI_VIEN: 'Quản trị viên',
    ROLE_NHAN_VIEN: 'Nhân viên'
};

const chucVu = computed(() => {
    if (profile.value?.chucVu) return profile.value.chucVu;
    return roleLabels[profile.value?.role] || 'Nhân viên';
});

const avatarUrl = computed(() => {
    const v = profile.value?.hinhAnh;
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

onMounted(() => {
    fetchProfile();
    // Nếu mở từ mục "Tài khoản của tôi" (?tab=account) thì cuộn tới phần đổi mật khẩu
    if (route.query.tab === 'account') {
        setTimeout(() => {
            document.getElementById('account-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 300);
    }
});
</script>

<template>
    <v-container class="py-6" style="max-width: 1000px">
        <div v-if="loading" class="text-center py-16">
            <v-progress-circular indeterminate color="#1e257c" size="44" width="4" />
            <p class="text-body-2 text-grey mt-4">Đang tải hồ sơ...</p>
        </div>

        <div v-else-if="profile">
            <!-- Profile card -->
            <v-card elevation="0" class="mb-6" style="border: 1px solid #e2e8f0; border-radius: 16px;">
                <div class="pa-6 d-flex align-center flex-wrap ga-6"
                    style="background: linear-gradient(135deg, #1e257c 0%, #2d379e 100%); border-radius: 16px 16px 0 0;">
                    <v-avatar size="96" class="border-4" style="border: 4px solid rgba(255,255,255,0.35);">
                        <v-img v-if="avatarUrl" :src="avatarUrl" cover />
                        <v-icon v-else size="56" color="white">mdi-account</v-icon>
                    </v-avatar>
                    <div>
                        <h2 class="text-h5 font-weight-bold text-white mb-1">{{ profile.ten || profile.tenTaiKhoan }}</h2>
                        <v-chip color="white" variant="flat" size="small" class="font-weight-bold" style="color:#1e257c;">
                            <v-icon size="16" start>mdi-shield-account</v-icon>{{ chucVu }}
                        </v-chip>
                    </div>
                </div>

                <div class="pa-6">
                    <h3 class="text-subtitle-1 font-weight-bold mb-4" style="color:#1e257c;">Thông tin cá nhân</h3>
                    <v-row dense>
                        <v-col cols="12" md="6">
                            <div class="info-item">
                                <div class="lbl">Mã nhân viên</div>
                                <div class="val"><v-icon size="16" class="mr-2" color="#1e257c">mdi-card-account-details-outline</v-icon>{{ profile.ma || '—' }}</div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-item">
                                <div class="lbl">Tên tài khoản</div>
                                <div class="val"><v-icon size="16" class="mr-2" color="#1e257c">mdi-account-circle-outline</v-icon>{{ profile.tenTaiKhoan || '—' }}</div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-item">
                                <div class="lbl">Email</div>
                                <div class="val"><v-icon size="16" class="mr-2" color="#1e257c">mdi-email-outline</v-icon>{{ profile.email || '—' }}</div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-item">
                                <div class="lbl">Số điện thoại</div>
                                <div class="val"><v-icon size="16" class="mr-2" color="#1e257c">mdi-phone-outline</v-icon>{{ profile.sdt || '—' }}</div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-item">
                                <div class="lbl">Giới tính</div>
                                <div class="val"><v-icon size="16" class="mr-2" color="#1e257c">mdi-gender-male-female</v-icon>{{ gioiTinhLabel }}</div>
                            </div>
                        </v-col>
                        <v-col cols="12" md="6">
                            <div class="info-item">
                                <div class="lbl">Ngày sinh</div>
                                <div class="val"><v-icon size="16" class="mr-2" color="#1e257c">mdi-calendar-outline</v-icon>{{ profile.ngaySinh || '—' }}</div>
                            </div>
                        </v-col>
                        <v-col cols="12">
                            <div class="info-item">
                                <div class="lbl">Địa chỉ</div>
                                <div class="val"><v-icon size="16" class="mr-2" color="#1e257c">mdi-map-marker-outline</v-icon>{{ diaChiDayDu || '—' }}</div>
                            </div>
                        </v-col>
                    </v-row>
                </div>
            </v-card>

            <!-- Account / change password -->
            <v-card id="account-section" elevation="0" style="border: 1px solid #e2e8f0; border-radius: 16px;">
                <div class="pa-6">
                    <h3 class="text-subtitle-1 font-weight-bold mb-1" style="color:#1e257c;">
                        <v-icon size="20" class="mr-2" color="#1e257c">mdi-lock-outline</v-icon>Tài khoản của tôi
                    </h3>
                    <p class="text-caption text-grey mb-5">Đổi mật khẩu đăng nhập của bạn.</p>

                    <v-alert v-if="pwMessage.text" :type="pwMessage.type === 'success' ? 'success' : 'error'"
                        variant="tonal" density="compact" class="mb-4">{{ pwMessage.text }}</v-alert>

                    <v-row dense style="max-width: 520px;">
                        <v-col cols="12">
                            <v-text-field v-model="pwForm.matKhauCu" label="Mật khẩu hiện tại" type="password"
                                variant="outlined" density="comfortable" hide-details="auto"
                                prepend-inner-icon="mdi-lock-outline" class="mb-3" />
                        </v-col>
                        <v-col cols="12">
                            <v-text-field v-model="pwForm.matKhauMoi" label="Mật khẩu mới" type="password"
                                variant="outlined" density="comfortable" hide-details="auto"
                                prepend-inner-icon="mdi-lock-plus-outline" class="mb-3" />
                        </v-col>
                        <v-col cols="12">
                            <v-text-field v-model="pwForm.xacNhanMatKhau" label="Xác nhận mật khẩu mới" type="password"
                                variant="outlined" density="comfortable" hide-details="auto"
                                prepend-inner-icon="mdi-lock-check-outline" class="mb-4" />
                        </v-col>
                    </v-row>
                    <v-btn :loading="pwLoading" style="background:#1e257c;color:#fff;" class="font-weight-bold text-none"
                        rounded="pill" @click="handleChangePassword">
                        <v-icon start size="18">mdi-content-save</v-icon>Cập nhật mật khẩu
                    </v-btn>
                </div>
            </v-card>
        </div>

        <div v-else class="text-center py-16">
            <v-icon size="48" color="grey">mdi-account-alert-outline</v-icon>
            <p class="text-body-2 text-grey mt-4">Không tải được thông tin hồ sơ.</p>
        </div>
    </v-container>
</template>

<style scoped>
.info-item {
    padding: 10px 12px;
    border-radius: 10px;
    transition: background 0.2s;
}
.info-item:hover {
    background: #f8fafc;
}
.info-item .lbl {
    font-size: 0.75rem;
    color: #94a3b8;
    margin-bottom: 4px;
}
.info-item .val {
    font-size: 0.9rem;
    font-weight: 600;
    color: #1e293b;
    display: flex;
    align-items: center;
}
</style>
