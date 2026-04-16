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
import { Menu2Icon } from 'vue-tabler-icons';

const uiStore = useUIStore();
const { sidebarCollapsed } = storeToRefs(uiStore);
const { toggleSidebar } = uiStore;

const sidebarMenu = shallowRef(sidebarItems);
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
        :width="236"
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
        <div class="sidebar-logo-wrap d-flex justify-center border-b" v-else>
            <v-avatar size="32" color="primary" class="rounded-0 text-white font-weight-black">A</v-avatar>
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
                    <v-btn class="text-muted mr-1" @click="toggleSidebar" icon variant="flat">
                        <Menu2Icon size="22" stroke-width="2" />
                    </v-btn>
                </div>
                <div class="d-flex align-center">
                    <NotificationDD />
                    <ProfileDD />
                </div>
            </div>
        </div>
    </v-app-bar>
</template>

<style scoped>
/* TRIỆT TIÊU THANH CUỘN THỨ 2 CỦA VUETIFY */
:deep(.v-navigation-drawer__content) {
    overflow: hidden !important;
}

:deep(.v-navigation-drawer) {
    overflow: hidden !important;
}

.sidebar-logo-wrap {
    padding: 12px 8px;
    min-height: 128px;
    align-items: center;
}

/* HIỆU ỨNG DI CHUỘT VÀ ĐÓNG MỞ CAO CẤP */
:deep(.v-list-item) {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
    position: relative;
    overflow: hidden;
    margin-bottom: 4px !important;
}

/* Hiệu ứng hover cho menu item */
:deep(.v-list-item:hover) {
    background: rgba(var(--v-theme-primary), 0.08) !important;
    transform: translateX(4px);
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
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1) !important;
    overflow: hidden;
    background: rgba(0, 0, 0, 0.02);
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
</style>
