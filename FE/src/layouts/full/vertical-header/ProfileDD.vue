<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { PATH } from '@/router/routePaths';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { dichVuFile } from '@/services/core/dichVuFile';
import defaultAvatar from '@/assets/images/profile/user-1.jpg';

const router = useRouter();
const profile = ref(null);

const roleLabels = {
    ROLE_QUAN_TRI_VIEN: 'Quản trị viên',
    ROLE_NHAN_VIEN: 'Nhân viên'
};

const displayName = computed(() =>
    profile.value?.ten || profile.value?.tenTaiKhoan || profile.value?.username || 'Người dùng'
);
const chucVu = computed(() => {
    if (profile.value?.chucVu) return profile.value.chucVu;
    return roleLabels[profile.value?.role] || '';
});

const avatarUrl = computed(() => {
    const v = profile.value?.hinhAnh;
    if (!v) return defaultAvatar;
    if (/^(https?:)?\/\//i.test(v) || v.startsWith('data:') || v.startsWith('blob:')) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
});

const profileDD = [
    {
        avatar: 'mdi-account-outline',
        title: 'Hồ sơ của tôi',
        to: { path: PATH.HO_SO }
    },
    {
        avatar: 'mdi-lock-outline',
        title: 'Tài khoản của tôi',
        to: { path: PATH.HO_SO, query: { tab: 'account' } }
    }
];

const goTo = (item) => {
    router.push(item.to);
};

const handleLogout = async () => {
    await dichVuXacThuc.dangXuat();
    window.location.href = PATH.ADMIN_LOGIN;
};

onMounted(async () => {
    try {
        profile.value = await dichVuXacThuc.layThongTinCaNhan();
    } catch (e) {
        // Nếu không lấy được (vd token hết hạn) thì fallback dữ liệu session
        profile.value = dichVuXacThuc.layUserHienTai();
    }
});
</script>

<template>
    <v-menu :close-on-content-click="false">
        <template v-slot:activator="{ props }">
            <v-btn class="profileBtn px-2" variant="text" v-bind="props" height="48">
                <div class="d-flex align-center">
                    <div class="d-none d-sm-block text-right mr-3">
                        <div class="text-body-2 font-weight-bold text-slate-800" style="line-height:1.2;">{{ displayName }}</div>
                        <div class="text-caption" style="color:#1e257c; line-height:1.2;">{{ chucVu }}</div>
                    </div>
                    <v-avatar size="38">
                        <img :src="avatarUrl" height="38" alt="user" style="object-fit:cover;" />
                    </v-avatar>
                </div>
            </v-btn>
        </template>
        <v-sheet rounded="md" width="230" elevation="10" class="mt-2">
            <div class="pa-4 d-flex align-center border-b">
                <v-avatar size="40" class="mr-3">
                    <img :src="avatarUrl" height="40" alt="user" style="object-fit:cover;" />
                </v-avatar>
                <div style="min-width:0;">
                    <div class="text-subtitle-2 font-weight-bold text-truncate">{{ displayName }}</div>
                    <div class="text-caption text-truncate" style="color:#1e257c;">{{ chucVu }}</div>
                </div>
            </div>
            <v-list class="py-0" lines="one" density="compact">
                <v-list-item v-for="(item, i) in profileDD" :key="i" :value="item" color="primary"
                    class="py-2 px-4 shadow-none" @click="goTo(item)">
                    <template v-slot:prepend>
                        <v-icon :icon="item.avatar" size="20" class="mr-3"></v-icon>
                    </template>
                    <v-list-item-title class="text-subtitle-2 font-weight-bold">{{ item.title }}</v-list-item-title>
                </v-list-item>
            </v-list>
            <div class="pt-4 pb-4 px-5 text-center">
                <v-btn color="primary" variant="outlined" block @click="handleLogout"
                    class="rounded-pill font-weight-bold">Đăng
                    xuất</v-btn>
            </div>
        </v-sheet>
    </v-menu>
</template>
