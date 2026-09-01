<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { PATH } from '@/router/routePaths';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { dichVuFile } from '@/services/core/dichVuFile';
import defaultAvatar from '@/assets/images/profile/default-avatar.svg';
import { APP_ROLES } from '@/constants/appConstants';

const router = useRouter();
const profile = ref(null);
const avatarError = ref(false);

const roleLabels = {
    [APP_ROLES.ADMIN]: 'Quản lý',
    [APP_ROLES.STAFF]: 'Nhân viên',
    [APP_ROLES.CUSTOMER]: 'Khách hàng'
};

const displayName = computed(() => profile.value?.ten || profile.value?.tenTaiKhoan || profile.value?.username || 'Người dùng');
const chucVu = computed(() => {
    if (profile.value?.chucVu) return profile.value.chucVu;
    return roleLabels[profile.value?.role] || '';
});

const avatarUrl = computed(() => {
    const v = profile.value?.hinhAnh || profile.value?.avatar;
    if (!v) return defaultAvatar;
    if (/^(https?:)?\/\//i.test(v) || v.startsWith('data:') || v.startsWith('blob:')) return v;
    return dichVuFile.layUrlFile(v.replace(/^\/+/, ''));
});

watch(avatarUrl, () => {
    avatarError.value = false;
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

const fetchProfile = async () => {
    try {
        const data = await dichVuXacThuc.layThongTinCaNhan();
        if (data) {
            profile.value = data;
            avatarError.value = false;
        }
    } catch (e) {
        profile.value = dichVuXacThuc.layUserHienTai();
    }
};

// Tối ưu: Chỉ gọi fetchProfile khi mounted và khi nhận sự kiện profile-updated (tránh gọi lại liên tục trên mọi lần đổi route)


onMounted(() => {
    fetchProfile();
    window.addEventListener('profile-updated', fetchProfile);
});

onUnmounted(() => {
    window.removeEventListener('profile-updated', fetchProfile);
});
</script>

<template>
    <v-menu :close-on-content-click="false">
        <template v-slot:activator="{ props }">
            <v-btn class="profileBtn px-2" variant="text" v-bind="props" height="48">
                <div class="d-flex align-center">
                    <div class="d-none d-sm-block text-right mr-3">
                        <div class="text-body-2 font-weight-bold text-slate-800" style="line-height: 1.2">{{ displayName }}</div>
                        <div class="text-caption" style="color: #1e257c; line-height: 1.2">{{ chucVu }}</div>
                    </div>
                    <v-avatar size="38" color="grey-lighten-3" class="border">
                        <v-img v-if="avatarUrl" :src="avatarUrl" cover alt="user">
                            <template #placeholder>
                                <div class="d-flex align-center justify-center fill-height">
                                    <v-icon size="22" color="grey-darken-1">mdi-account</v-icon>
                                </div>
                            </template>
                            <template #error>
                                <div class="d-flex align-center justify-center fill-height bg-grey-lighten-3">
                                    <v-icon size="22" color="grey-darken-1">mdi-account</v-icon>
                                </div>
                            </template>
                        </v-img>
                        <v-icon v-else size="22" color="grey-darken-1">mdi-account</v-icon>
                    </v-avatar>
                </div>
            </v-btn>
        </template>
        <v-sheet rounded="lg" width="240" elevation="10" class="mt-2 border">
            <div class="pa-4 d-flex align-center border-b bg-slate-50">
                <v-avatar size="42" class="mr-3 border" color="grey-lighten-3">
                    <v-img v-if="avatarUrl" :src="avatarUrl" cover alt="user">
                        <template #placeholder>
                            <div class="d-flex align-center justify-center fill-height">
                                <v-icon size="24" color="grey-darken-1">mdi-account</v-icon>
                            </div>
                        </template>
                        <template #error>
                            <div class="d-flex align-center justify-center fill-height bg-grey-lighten-3">
                                <v-icon size="24" color="grey-darken-1">mdi-account</v-icon>
                            </div>
                        </template>
                    </v-img>
                    <v-icon v-else size="24" color="grey-darken-1">mdi-account</v-icon>
                </v-avatar>
                <div style="min-width: 0">
                    <div class="text-subtitle-2 font-weight-bold text-truncate text-slate-800">{{ displayName }}</div>
                    <div class="text-caption text-truncate font-weight-medium" style="color: #1e257c">{{ chucVu }}</div>
                </div>
            </div>
            <v-list class="py-0" lines="one" density="compact">
                <v-list-item
                    v-for="(item, i) in profileDD"
                    :key="i"
                    :value="item"
                    color="primary"
                    class="py-2 px-4 elevation-0"
                    @click="goTo(item)"
                >
                    <template v-slot:prepend>
                        <v-icon :icon="item.avatar" size="20" class="mr-3"></v-icon>
                    </template>
                    <v-list-item-title class="text-subtitle-2 font-weight-bold">{{ item.title }}</v-list-item-title>
                </v-list-item>
            </v-list>
            <div class="pt-4 pb-4 px-5 text-center">
                <v-btn color="primary" variant="outlined" block @click="handleLogout" class="rounded-pill font-weight-bold"
                    >Đăng xuất</v-btn
                >
            </div>
        </v-sheet>
    </v-menu>
</template>
