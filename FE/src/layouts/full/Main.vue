<script setup>
import { ref, shallowRef } from 'vue';
import { useUIStore } from '@/stores/ui';
import { storeToRefs } from 'pinia';

// Menu & Header Components
import sidebarItems from './vertical-sidebar/sidebarItem';
import NavGroup from './vertical-sidebar/NavGroup/index.vue';
import NavItem from './vertical-sidebar/NavItem/index.vue';
import NavCollapse from './vertical-sidebar/NavCollapse/index.vue';
import Logo from './logo/Logo.vue';
import NotificationDD from './vertical-header/NotificationDD.vue';
import ProfileDD from './vertical-header/ProfileDD.vue';
import GiaoCaModal from '@/components/common/GiaoCaModal.vue';
import { dichVuGiaoCa } from '@/services/admin/dichVuGiaoCa';
import { Menu2Icon } from 'vue-tabler-icons';
import { useNotificationStore } from '@/stores/notificationStore';
import { computed, onMounted } from 'vue';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

const uiStore = useUIStore();
const notificationStore = useNotificationStore();
const { sidebarCollapsed, breadcrumbs } = storeToRefs(uiStore);
const { unreadChatCount } = storeToRefs(notificationStore);
const { toggleSidebar } = uiStore;

// Giao Ca
const showGiaoCaModal = ref(false);
const giaoCaMode = ref('open');
const currentShift = ref(null);

const handleGiaoCaClick = async () => {
    try {
        const res = await dichVuGiaoCa.getCaHienTai();
        const data = res?.data || res;
        if (data && data.id) {
            currentShift.value = data;
            giaoCaMode.value = 'close';
        } else {
            currentShift.value = null;
            giaoCaMode.value = 'open';
        }
    } catch (e) {
        currentShift.value = null;
        giaoCaMode.value = 'open';
    } finally {
        showGiaoCaModal.value = true;
    }
};

const sidebarMenu = computed(() => {
    const userRole = dichVuXacThuc.layUserHienTai()?.role;

    return sidebarItems
        .filter(item => !item.roles || item.roles.includes(userRole))
        .map((item) => {
            let filteredItem = { ...item };
            
            if (filteredItem.children) {
                filteredItem.children = filteredItem.children.filter(child => !child.roles || child.roles.includes(userRole));
            }

            if (filteredItem.title === 'Quản lý tin nhắn' && unreadChatCount.value > 0) {
                return {
                    ...filteredItem,
                    chip: unreadChatCount.value.toString(),
                    chipColor: 'error',
                    chipVariant: 'flat'
                };
            }
            return filteredItem;
        });
});

onMounted(() => {
    notificationStore.init();
});
</script>

<template>
    <!-- BIẾN SIDEBAR THÀNH CHẾ ĐỘ RAIL (MINI) KHI TẮT -->
    <v-navigation-drawer
        left
        :rail="sidebarCollapsed"
        permanent
        app
        class="leftSidebar bg-containerBg"
        elevation="0"
        :width="300"
        rail-width="68"
        style="
            overflow: hidden !important;
            margin: 0;
            height: 100vh !important;
            border-radius: 0 !important;
            border-right: 1px solid rgba(0, 0, 0, 0.08) !important;
        "
    >
        <div class="sidebar-logo-wrap d-flex justify-center border-b" v-if="!sidebarCollapsed">
            <Logo />
        </div>
        <div class="sidebar-logo-wrap d-flex justify-center border-b" v-else style="min-height: 64px; padding: 16px 0">
            <img src="@/assets/images/logos/logo.jpg" alt="logo" style="width: 44px; height: 44px; object-fit: contain" />
        </div>

        <perfect-scrollbar class="scrollnavbar bg-containerBg" :style="{ height: 'calc(100vh - 136px)' }">
            <v-list class="py-4 px-2 bg-containerBg">
                <template v-for="(item, i) in sidebarMenu">
                    <NavGroup :item="item" v-if="item.header && !sidebarCollapsed" :key="item.title" />
                    <NavCollapse :item="item" v-else-if="item.children" :hide-title="sidebarCollapsed" />
                    <NavItem :item="item" v-else class="leftPadding" :hide-title="sidebarCollapsed" />
                </template>
            </v-list>
        </perfect-scrollbar>
    </v-navigation-drawer>

    <!-- HEADER: TỰ ĐỘNG CĂN THEO SIDEBAR VÀ NỘI DUNG -->
    <v-app-bar
        flat
        color="white"
        height="64"
        app
        class="px-sm-4 px-3"
        style="box-shadow: none !important; border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important; background: white !important"
    >
        <div class="maxWidth mx-auto w-100 d-flex align-center px-1 py-0" style="height: 56px">
            <div class="d-flex align-center justify-space-between w-100">
                <div class="d-flex align-center">
                    <v-btn class="mr-3 header-toggle-btn" @click="toggleSidebar" icon variant="text" color="slate-600">
                        <Menu2Icon size="22" stroke-width="2" />
                    </v-btn>

                    <v-breadcrumbs v-if="breadcrumbs.length" :items="breadcrumbs" class="pa-0 breadcrumbs-header">
                        <template v-slot:divider>
                            <v-icon size="small" color="grey-lighten-1">mdi-chevron-right</v-icon>
                        </template>
                    </v-breadcrumbs>
                </div>
                <div class="d-flex align-center">
                    <!-- <v-btn
                        variant="tonal"
                        color="primary"
                        class="mr-2 rounded-pill font-weight-bold px-4"
                        prepend-icon="mdi-store-clock"
                        @click="handleGiaoCaClick"
                        height="40"
                    >
                        Giao Ca
                    </v-btn> -->
                    <NotificationDD />
                    <ProfileDD />
                </div>
            </div>
        </div>
    </v-app-bar>

    <!-- <GiaoCaModal v-model="showGiaoCaModal" :mode="giaoCaMode" :current-shift="currentShift" /> -->
</template>

<style scoped>
/* TRIỆT TIÊU THANH CUỘN THỨ 2 CỦA VUETIFY */
:deep(.v-navigation-drawer__content) {
    overflow: hidden !important;
}

:deep(.v-navigation-drawer) {
    overflow: hidden !important;
}

.header-toggle-btn {
    transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), color 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.header-toggle-btn:hover {
    color: #475569 !important;
    background: transparent !important;
}

.header-toggle-btn :deep(.v-btn__overlay),
.header-toggle-btn :deep(.v-btn__underlay),
.header-toggle-btn :deep(.v-ripple__container) {
    display: none !important;
}

.header-toggle-btn:focus,
.header-toggle-btn:active,
.header-toggle-btn:focus-visible {
    background: transparent !important;
    background-color: transparent !important;
    box-shadow: none !important;
    outline: none !important;
}

.sidebar-logo-wrap {
    padding: 12px 8px;
    min-height: 128px;
    align-items: center;
}

/* HIỆU ỨNG DI CHUỘT VÀ ĐÓNG MỞ CAO CẤP */
:deep(.v-list-item) {
    transition: background-color 0.3s cubic-bezier(0.4, 0, 0.2, 1), transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), color 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
    position: relative;
    overflow: hidden;
    margin-bottom: 4px !important;
}

/* Hiệu ứng hover cho menu item */
:deep(.v-list-item:hover) {
    background: rgba(var(--v-theme-primary), 0.08) !important;
}

/* Thêm vạch accent bên trái khi hover */
:deep(.v-list-item::before) {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 0;
    height: 60%;
    background: rgb(var(--v-theme-primary));
    border-top-right-radius: 4px;
    border-bottom-right-radius: 4px;
    transition: width 0.3s ease;
}

:deep(.v-list-item:hover::before) {
    width: 4px;
}

/* Icon rotation khi mở/đóng sub-menu */
:deep(.v-list-group--active > .v-list-item .v-list-item__append i) {
    transform: rotate(180deg);
}

/* Hiệu ứng mượt cho sub-menu (Accordion) */
:deep(.v-list-group__items) {
    transition: height 0.4s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.4s cubic-bezier(0.4, 0, 0.2, 1) !important;
    overflow: hidden;
    background: transparent;
    border-radius: 12px;
    margin: 4px 8px !important;
}

/* Active state highlight: Light blue background with black text/icon */
:deep(.v-list-item--active) {
    background: rgba(var(--v-theme-primary), 0.1) !important;
    color: rgb(var(--v-theme-primary)) !important;
    box-shadow: none !important;
}

:deep(.icon-box) {
    color: #475569 !important; /* Default slate icon */
}

:deep(.v-list-item--active .icon-box) {
    color: #000000 !important; /* Black icon on active */
}

:deep(.v-list-item--active .v-list-item-title) {
    color: #000000 !important; /* Black text on active */
    font-weight: 700 !important;
}

/* Animation cho text menu khi rail đổi trạng thái */
:deep(.v-list-item-title) {
    transition: opacity 0.3s ease;
}
/* Breadcrumbs in Header */
:deep(.breadcrumbs-header) {
    font-size: 14px;
    font-weight: 700;
}

:deep(.breadcrumbs-header .v-breadcrumbs-item) {
    padding: 0;
    color: #475569 !important; /* Slate 600 */
    opacity: 1 !important;
}

:deep(.breadcrumbs-header .v-breadcrumbs-item--disabled) {
    opacity: 1 !important;
    color: #0f172a !important; /* Slate 900 for current page */
}

:deep(.breadcrumbs-header .v-breadcrumbs-divider) {
    padding: 0 4px;
    color: #94a3b8;
}
</style>
